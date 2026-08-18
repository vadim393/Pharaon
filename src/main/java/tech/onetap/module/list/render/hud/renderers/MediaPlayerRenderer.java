package tech.onetap.module.list.render.hud.renderers;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import tech.onetap.module.list.render.hud.Interface;
import tech.onetap.module.list.render.hud.media.MediaInfo;
import tech.onetap.module.list.render.hud.media.MediaInfoProvider;
import tech.onetap.module.list.render.hud.media.MediaPlayerInfoLibraryProvider;
import tech.onetap.util.draggable.Draggable;
import tech.onetap.util.render.builders.Builder;
import tech.onetap.util.render.builders.states.QuadColorState;
import tech.onetap.util.render.builders.states.QuadRadiusState;
import tech.onetap.util.render.builders.states.SizeState;
import tech.onetap.util.render.math.Animation;
import tech.onetap.util.render.math.Easing;
import tech.onetap.util.render.math.Scissor;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Optional;

public class MediaPlayerRenderer {
    private static final Identifier COVER_TEXTURE_ID = Identifier.of("onetap", "media_cover");
    private static final float COVER_SIZE = 30f;
    private static final float COVER_OFFSET_X = 1f;
    private static final float COVER_OFFSET_Y = 0f;
    private static final float PAD = 2f;
    private static final float PANEL_HEIGHT = 10f;
    private static final float PROGRESS_BAR_HEIGHT = 3f;
    private static final float TITLE_FONT = 7f;
    private static final float ARTIST_FONT = 6f;

    private static final boolean LAYOUT_TWO_LINES = true;
    private static final float TITLE_OFFSET_Y = 3f;
    private static final float TITLE_ARTIST_GAP = 2f;
    private static final boolean SHOW_TIME = true;

    private static final float TIME_FONT = 5.5f;
    private static final float TIME_OFFSET_Y = 22f;

    private final Interface owner;
    private final MediaInfoProvider provider;
    private final Animation alphaAnim = new Animation(Easing.EXPO_OUT, 220);
    private final Animation progressAnim = new Animation(Easing.LINEAR, 180);
    private byte[] lastCoverBytes = null;
    private float smoothDurationWidth = 1f;
    private boolean iconPulseWasPlaying = false;
    private long iconPulseStartedAt = 0L;
    private float smoothedIconAlpha = 100f;

    public MediaPlayerRenderer(Interface owner) {
        this.owner = owner;
        this.provider = createProvider();
    }

    private static MediaInfoProvider createProvider() {
        try {
            var lib = new MediaPlayerInfoLibraryProvider();
            if (lib.isAvailable()) return lib;
        } catch (Throwable ignored) {}
        return new MediaInfoProvider() {
            @Override
            public MediaInfo getCurrentMedia() { return MediaInfo.empty(); }
            @Override
            public boolean isAvailable() { return true; }
        };
    }

    public void render(DrawContext context) {
        if (owner.mc.player == null) return;

        MediaInfo info = provider.getCurrentMedia();
        boolean hasContent = !info.isEmpty();
        boolean hasProgress = info.durationMs().isPresent() && info.positionMs().isPresent();
        alphaAnim.run(hasContent ? 1f : 0f);
        float alpha = (float) alphaAnim.getValue();

        Draggable drag = owner.getMediaPlayerDrag();

        if (owner.getHudStyleSetting().is("DLC")) {
            renderPouchMediaPlayer(context, info, alpha, drag);
            return;
        }

        float width = computeWidth(info);
        float baseHeight = LAYOUT_TWO_LINES ? PANEL_HEIGHT + 14f : PANEL_HEIGHT;
        float height = hasProgress ? baseHeight + PROGRESS_BAR_HEIGHT + PAD + 4f : baseHeight;
        drag.setWidth(width);
        drag.setHeight(height);

        if (alpha <= 0.02f) return;

        int aInt = MathHelper.clamp((int) (255 * alpha), 0, 255);
        float x = drag.getX();
        float y = drag.getY();

        owner.drawBackground(x, y, width, height, 4, aInt);

        float coverX = x + COVER_OFFSET_X;
        float coverY = y + COVER_OFFSET_Y + (height - COVER_SIZE) / 2f;
        float textStartX = coverX + COVER_SIZE + PAD;
        float maxTextW = width - (textStartX - x) - PAD;

        renderCover(context, coverX, coverY, info, aInt);

        int t1 = ColorProvider.setAlpha(ColorProvider.getThemeColor(), aInt);
        int t2 = ColorProvider.setAlpha(ColorProvider.getThemeColorTwo(), aInt);

        Scissor.push();
        Scissor.setFromComponentCoordinates(textStartX, y, maxTextW, height);
        if (LAYOUT_TWO_LINES) {
            String title = truncate(info.getDisplayTitle(), maxTextW, TITLE_FONT, false);
            String artist = truncate(info.getDisplayArtist(), maxTextW, ARTIST_FONT, false);
            float titleY = y + TITLE_OFFSET_Y;
            float artistY = titleY + TITLE_FONT * 1.2f + TITLE_ARTIST_GAP;
            DrawUtil.drawText(Fonts.SFREGULAR.get(), title, textStartX, titleY, ColorProvider.rgba(255, 255, 255, aInt), TITLE_FONT);
            drawGradientText(textStartX, artistY, artist, ARTIST_FONT, t1, t2);
        } else {
            String singleLine = buildSingleLine(info);
            String truncated = truncate(singleLine, maxTextW, TITLE_FONT, false);
            float textY = y + (height - TITLE_FONT * 1.2f) / 2f;
            DrawUtil.drawText(Fonts.SFREGULAR.get(), truncated, textStartX, textY, ColorProvider.rgba(255, 255, 255, aInt), TITLE_FONT);
        }
        Scissor.unset();
        Scissor.pop();

        if (hasProgress) {
            float barY = y + height - PAD - PROGRESS_BAR_HEIGHT - 4f;
            float barW = width - (textStartX - x) - PAD;
            float targetProgress = info.getProgress();
            float progress = MathHelper.clamp((float) progressAnim.run(targetProgress), 0f, 1f);
            int bgColor = ColorProvider.rgba(40, 40, 40, aInt);
            DrawUtil.drawRound(textStartX, barY, barW, PROGRESS_BAR_HEIGHT, 0.5f, bgColor);
            DrawUtil.drawRound(textStartX, barY, barW * progress, PROGRESS_BAR_HEIGHT, 0.5f, t1, t2, t2, t1);
            if (SHOW_TIME) {
                long posMs = info.getEffectivePositionMs();
                long durMs = info.durationMs().orElse(0L);
                String timeText = formatTime(posMs) + " / " + formatTime(durMs);
                int timeColor = ColorProvider.rgba(200, 200, 200, (int) (0.6f * aInt));
                DrawUtil.drawText(Fonts.SFREGULAR.get(), timeText, textStartX + barW - Fonts.SFREGULAR.get().getWidth(timeText, TIME_FONT), barY - TIME_OFFSET_Y, timeColor, TIME_FONT);
            }
        }
    }

    private void renderPouchMediaPlayer(DrawContext context, MediaInfo info, float alpha, Draggable drag) {
        boolean visible = !info.isEmpty() && alpha > 0.02f;
        if (!visible) {
            drag.setWidth(0);
            drag.setHeight(0);
            return;
        }

        float x = drag.getX();
        float y = drag.getY();
        float w = 132f;
        float h = 42f;
        float artworkSize = 32.5f;
        float textX = x + 42f;
        float textWidth = w - 50f;
        int duration = Math.max(1, info.durationMs().map(ms -> (int) (ms / 1000)).orElse(0));
        int position = Math.max(0, Math.min((int) (info.getEffectivePositionMs() / 1000), duration));
        int iconAlpha = resolveMusicIconAlpha(info.isPlaying(), alpha);
        float targetWidth = duration > 0 ? (position / (float) duration) * textWidth : 0f;
        String title = truncate(info.getDisplayTitle(), textWidth, 6f, true);
        String artist = truncate(info.getDisplayArtist(), textWidth, 5.5f, false);
        String posText = formatTimeSec(position);
        String durText = formatTimeSec(duration);
        float durWidth = Fonts.SFREGULAR.get().getWidth(durText, 5f);
        float controlsCenterX = textX + textWidth * 0.5f;
        float controlsY = y + 26f;
        int aInt = (int) (255 * alpha);

        int background = ColorProvider.rgba(0, 0, 0, (int) (255 * alpha));
        int coverColor = ColorProvider.rgba(80, 80, 80, (int) (50 * alpha));
        int white = ColorProvider.rgba(255, 255, 255, aInt);
        int muted = ColorProvider.rgba(187, 187, 187, aInt);
        int barBack = ColorProvider.rgba(58, 58, 58, aInt);
        int purple = ColorProvider.rgba(158, 107, 255, aInt);

        smoothDurationWidth += (targetWidth - smoothDurationWidth) * 0.2f;
        smoothDurationWidth = MathHelper.clamp(smoothDurationWidth, 1f, textWidth);

        DrawUtil.drawRoundBlur(x, y, w, h, 5f, background, 15f);

        DrawUtil.drawRound(x + 5f, y + 5f, artworkSize, artworkSize, 4f, coverColor);
        float iconSize = 20f;
        float iconTextW = Fonts.ICONS_NURIK.get().getWidth("u", iconSize);
        DrawUtil.drawText(Fonts.ICONS_NURIK.get(), "u", x + 21f - iconTextW * 0.5f, y + 12.5f,
                ColorProvider.rgba(255, 255, 255, iconAlpha), iconSize);

        DrawUtil.drawText(Fonts.SFBOLD.get(), title, textX, y + 8f, white, 6f);
        DrawUtil.drawText(Fonts.SFREGULAR.get(), artist, textX, y + 17f, muted, 5.5f);

        DrawUtil.drawText(Fonts.SFREGULAR.get(), posText, textX, y + 28f, white, 5f);
        DrawUtil.drawText(Fonts.SFREGULAR.get(), durText, x + w - durWidth - 8f, y + 28f, white, 5f);

        DrawUtil.drawRound(textX, y + h - 7f, textWidth, 2f, 1f, barBack);
        DrawUtil.drawRound(textX, y + h - 7f, smoothDurationWidth, 2f, 1f, purple);

        drawControlIcon(x, controlsCenterX - 9f, controlsY + 0.5f, "s", 6f, white);
        drawControlIcon(x, controlsCenterX, controlsY, info.isPlaying() ? "p" : "o", 7.5f, white);
        drawControlIcon(x, controlsCenterX + 9f, controlsY + 0.75f, "n", 6f, white);

        drag.setWidth(w);
        drag.setHeight(h);
    }

    private void drawControlIcon(float panelX, float centerX, float y, String icon, float size, int color) {
        float width = Fonts.MOONWARD.get().getWidth(icon, size);
        DrawUtil.drawText(Fonts.MOONWARD.get(), icon, centerX - width * 0.5f, y, color, size);
    }

    private String formatTimeSec(int seconds) {
        int safe = Math.max(0, seconds);
        return String.format("%d:%02d", safe / 60, safe % 60);
    }

    private int resolveMusicIconAlpha(boolean playing, float widgetAlpha) {
        long now = System.currentTimeMillis();
        if (playing != iconPulseWasPlaying) {
            iconPulseWasPlaying = playing;
            iconPulseStartedAt = now;
        }

        float targetAlpha;
        if (!playing) {
            targetAlpha = 100f;
        } else {
            long elapsed = Math.max(0L, now - iconPulseStartedAt);
            double phase = ((elapsed % 1800L) / 1800.0D) * (Math.PI * 2.0D);
            double wave = Math.sin(phase - Math.PI / 2.0D);
            targetAlpha = 150f + (float) (50.0D * wave);
        }

        smoothedIconAlpha += (targetAlpha - smoothedIconAlpha) * 0.14f;
        if (Math.abs(targetAlpha - smoothedIconAlpha) < 0.5f) {
            smoothedIconAlpha = targetAlpha;
        }
        return Math.round(MathHelper.clamp(smoothedIconAlpha * widgetAlpha, 0f, 255f));
    }

    private String formatTime(long ms) {
        long s = ms / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    private void drawGradientText(float x, float y, String text, float size, int c1, int c2) {
        if (text == null || text.isEmpty()) return;
        var font = Fonts.SFREGULAR.get();
        float cursorX = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            int color = ColorProvider.interpolateColor(c1, c2, (float) i / Math.max(1, text.length() - 1));
            DrawUtil.drawText(font, ch, cursorX, y, color, size);
            cursorX += font.getWidth(ch, size);
        }
    }

    private String buildSingleLine(MediaInfo info) {
        String artist = info.getDisplayArtist();
        String title = info.getDisplayTitle();
        if ("—".equals(artist) && "—".equals(title)) return "—";
        if ("—".equals(artist)) return title;
        if ("—".equals(title)) return artist;
        return artist + " - " + title;
    }

    private void renderCover(DrawContext context, float cx, float cy, MediaInfo info, int alpha) {
        Optional<Identifier> texIdOpt = provider.getCoverTextureId();
        if (texIdOpt.isPresent()) {
            try {
                int texId = owner.mc.getTextureManager().getTexture(texIdOpt.get()).getGlId();
                int color = ColorProvider.rgba(255, 255, 255, alpha);
                Builder.texture()
                        .size(new SizeState(COVER_SIZE, COVER_SIZE))
                        .radius(new QuadRadiusState(4f))
                        .color(new QuadColorState(color))
                        .texture(0f, 0f, 1f, 1f, texId)
                        .smoothness(1f)
                        .build()
                        .render(context.getMatrices().peek().getPositionMatrix(), cx, cy, 0f);
                return;
            } catch (Throwable ignored) {}
        }

        Optional<byte[]> coverOpt = info.coverImage();
        if (coverOpt.isPresent() && coverOpt.get().length > 0) {
            byte[] coverBytes = coverOpt.get();
            if (!Arrays.equals(coverBytes, lastCoverBytes)) {
                lastCoverBytes = coverBytes;
                try (var stream = new ByteArrayInputStream(coverBytes)) {
                    NativeImage img = NativeImage.read(stream);
                    NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
                    owner.mc.getTextureManager().registerTexture(COVER_TEXTURE_ID, tex);
                } catch (Throwable ignored) {
                }
            }
            try {
                int texId = owner.mc.getTextureManager().getTexture(COVER_TEXTURE_ID).getGlId();
                int color = ColorProvider.rgba(255, 255, 255, alpha);
                Builder.texture()
                        .size(new SizeState(COVER_SIZE, COVER_SIZE))
                        .radius(new QuadRadiusState(3f))
                        .color(new QuadColorState(color))
                        .texture(0f, 0f, 1f, 1f, texId)
                        .smoothness(1f)
                        .build()
                        .render(context.getMatrices().peek().getPositionMatrix(), cx, cy, 0f);
                return;
            } catch (Throwable ignored) {
            }
        } else {
            lastCoverBytes = null;
        }

        float iconSize = 16f;
        float iconX = cx + (COVER_SIZE - iconSize) / 4f;
        float iconY = cy + (COVER_SIZE - iconSize) / 2f;
        int iconColor = ColorProvider.rgba(255, 255, 255, (int) (0.7f * alpha));
        DrawUtil.drawText(Fonts.ICONS_NURIK.get(), "M", iconX, iconY, iconColor, iconSize);
    }

    private float computeWidth(MediaInfo info) {
        float minW = COVER_SIZE + PAD * 3 + 35f;
        float textW = 0f;
        if (!info.isEmpty()) {
            if (LAYOUT_TWO_LINES) {
                textW = Math.max(
                        Fonts.SFREGULAR.get().getWidth(info.getDisplayTitle(), TITLE_FONT),
                        Fonts.SFREGULAR.get().getWidth(info.getDisplayArtist(), ARTIST_FONT)
                );
            } else {
                textW = Fonts.SFREGULAR.get().getWidth(buildSingleLine(info), TITLE_FONT);
            }
        }
        return Math.max(minW, textW + PAD * 2 + 75f);
    }

    private String truncate(String s, float maxW, float fontSize, boolean medium) {
        if (s == null || s.isEmpty()) return "—";
        var font = medium ? Fonts.SFMEDIUM.get() : Fonts.SFREGULAR.get();
        if (font.getWidth(s, fontSize) <= maxW) return s;
        String ellipsis = "...";
        for (int i = s.length() - 1; i > 0; i--) {
            String sub = s.substring(0, i) + ellipsis;
            if (font.getWidth(sub, fontSize) <= maxW) return sub;
        }
        return ellipsis;
    }
}
