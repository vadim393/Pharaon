package tech.onetap.ui.menu;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import tech.onetap.util.alt.Alt;
import tech.onetap.util.alt.AltManager;
import tech.onetap.util.cursor.CursorManager;
import tech.onetap.util.render.builders.Builder;
import tech.onetap.util.render.builders.states.QuadColorState;
import tech.onetap.util.render.builders.states.QuadRadiusState;
import tech.onetap.util.render.builders.states.SizeState;
import tech.onetap.util.render.helper.HoverUtil;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.CustomDrawContext;
import tech.onetap.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AltManagerScreen extends Screen {
    private static final float ROW_HEIGHT = 27f;
    private static final float ROW_GAP = 4f;
    private static final float MARGIN = 16f;
    private static final Identifier STAR_ID = Identifier.of("mre", "images/star.png");

    private final Screen parent;
    private final List<ClickArea> clickAreas = new ArrayList<>();

    private List<Alt> alts = new ArrayList<>();
    private float panelX;
    private float panelY;
    private float panelW;
    private float panelH;
    private int scrollOffset;

    private String fieldText = "";
    private boolean fieldFocused;
    private float fieldX;
    private float fieldY;
    private float fieldW;
    private float fieldH;

    private String statusText = "";
    private boolean statusError;

    public AltManagerScreen(Screen parent) {
        super(Text.of("Alt Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.alts = AltManager.getAlts();
        this.panelW = Math.min(600, this.width - 40);
        this.panelH = Math.min(480, this.height - 40);
        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = (this.height - this.panelH) / 2;
        this.scrollOffset = 0;

        this.fieldH = 22f;
        this.fieldW = this.panelW - MARGIN * 2f - 8f - 92f - 8f - 118f;
        this.fieldX = this.panelX + MARGIN;
        this.fieldY = this.panelY + 46f;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        CustomDrawContext ctx = CustomDrawContext.of(context);
        clickAreas.clear();

        int themeA = ColorProvider.getThemeColor();
        int themeB = ColorProvider.getThemeColorTwo();

        DrawUtil.drawRect(ctx.getMatrices(), 0f, 0f, width, height, ColorProvider.rgba(0, 0, 0, 190));

        float radius = 10f;
        int panelFill = ColorProvider.rgba(14, 14, 16, 215);
        DrawUtil.drawRoundBlur(panelX, panelY, panelW, panelH, radius, panelFill, 24f);
        DrawUtil.drawRound(panelX - 0.7f, panelY - 0.7f, panelW + 1.4f, panelH + 1.4f, radius, ColorProvider.setAlpha(themeA, 110));
        DrawUtil.drawRound(panelX, panelY, panelW, panelH, radius, panelFill);

        int titleColor = ColorProvider.colorLerp(themeA, themeB, 8.0f, 0);
        DrawUtil.drawText(Fonts.SFBOLD.get(), "Alt Manager", panelX + MARGIN, panelY + 10f, titleColor, 12f);

        String countText = "\u0410\u043A\u043A\u0430\u0443\u043D\u0442\u044B: " + alts.size();
        float countW = Fonts.SFREGULAR.get().getWidth(countText, 7f);
        DrawUtil.drawText(Fonts.SFREGULAR.get(), countText, panelX + panelW - MARGIN - countW, panelY + 18f, ColorProvider.rgba(180, 180, 180, 230), 7f);

        DrawUtil.drawRound(panelX + MARGIN, panelY + 38f, panelW - MARGIN * 2f, 1f, 0.5f, ColorProvider.setAlpha(themeA, 90));

        renderField(mouseX, mouseY);

        float createX = fieldX + fieldW + 8f;
        float randomX = createX + 92f + 8f;
        renderButton("\u0421\u043E\u0437\u0434\u0430\u0442\u044C", createX, fieldY, 92f, fieldH, themeA, false, () -> onCreate(), mouseX, mouseY);
        renderButton("\u0420\u0430\u043D\u0434\u043E\u043C \u0410\u043A\u043A", randomX, fieldY, 118f, fieldH, themeA, false, () -> onRandom(), mouseX, mouseY);

        if (!statusText.isEmpty()) {
            int statusColor = statusError ? ColorProvider.rgba(255, 85, 85, 235) : ColorProvider.rgba(85, 255, 85, 235);
            DrawUtil.drawText(Fonts.SFREGULAR.get(), statusText, fieldX, panelY + 74f, statusColor, 6.8f);
        }

        renderList(ctx, mouseX, mouseY);

        renderButton("\u041D\u0430\u0437\u0430\u0434", panelX + MARGIN, panelY + panelH - 34f, 100f, 20f, themeA, false, this::close, mouseX, mouseY);
        String hint = "\u041A\u043E\u043B\u0435\u0441\u043E \u043C\u044B\u0448\u0438 \u2014 \u043F\u0440\u043E\u043A\u0440\u0443\u0442\u043A\u0430";
        float hintW = Fonts.SFREGULAR.get().getWidth(hint, 6f);
        DrawUtil.drawText(Fonts.SFREGULAR.get(), hint, panelX + panelW - MARGIN - hintW, panelY + panelH - 28f, ColorProvider.rgba(150, 150, 150, 190), 6f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (HoverUtil.isHovered(mouseX, mouseY, fieldX, fieldY, fieldW, fieldH)) {
                fieldFocused = true;
                return true;
            }
            fieldFocused = false;
            for (ClickArea area : clickAreas) {
                if (area.contains(mouseX, mouseY)) {
                    area.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = getMaxScroll();
        this.scrollOffset = MathHelper.clamp(this.scrollOffset - (int) Math.round(verticalAmount), 0, maxScroll);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (fieldFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !fieldText.isEmpty()) {
                fieldText = fieldText.substring(0, fieldText.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                onCreate();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                fieldFocused = false;
                return true;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (fieldFocused && !Character.isISOControl(chr)) {
            if (fieldText.length() < 16) {
                fieldText += chr;
            }
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    private void renderField(int mouseX, int mouseY) {
        boolean hovered = HoverUtil.isHovered(mouseX, mouseY, fieldX, fieldY, fieldW, fieldH);
        if (hovered || fieldFocused) {
            CursorManager.requestIBeam();
        }

        int borderAlpha = fieldFocused ? 150 : (hovered ? 100 : 60);
        DrawUtil.drawRound(fieldX - 0.5f, fieldY - 0.5f, fieldW + 1f, fieldH + 0.5f, 4f, ColorProvider.setAlpha(ColorProvider.getThemeColor(), borderAlpha));
        DrawUtil.drawRoundBlur(fieldX, fieldY, fieldW, fieldH, 4f, ColorProvider.rgba(20, 20, 25, 190), 14f);

        boolean empty = fieldText.isEmpty();
        String display = empty && !fieldFocused
                ? "\u0412\u0432\u0435\u0434\u0438 \u0438\u043C\u044F (\u043C\u0438\u043D. 3 \u0441\u0438\u043C\u0432\u043E\u043B\u0430)"
                : fieldText + (fieldFocused && System.currentTimeMillis() % 1000L > 500L ? "_" : "");
        int textColor = empty && !fieldFocused
                ? ColorProvider.rgba(150, 150, 150, 180)
                : ColorProvider.rgba(235, 235, 235, 235);
        float textSize = empty && !fieldFocused ? 6.8f : 7.5f;
        DrawUtil.drawText(Fonts.SFREGULAR.get(), display, fieldX + 6f, fieldY + (fieldH - 7.5f) / 2f, textColor, textSize, 0.8f, 1f, fieldW - 12f);
    }

    private void renderButton(String label, float x, float y, float w, float h, int themeColor, boolean danger, Runnable action, int mouseX, int mouseY) {
        boolean hovered = HoverUtil.isHovered(mouseX, mouseY, x, y, w, h);
        if (hovered) {
            CursorManager.requestHand();
        }
        int borderAlpha = (int) (70 + (hovered ? 80 : 0));
        int borderColor = danger
                ? ColorProvider.setAlpha(ColorProvider.rgba(255, 90, 90, 255), borderAlpha)
                : ColorProvider.setAlpha(themeColor, borderAlpha);
        DrawUtil.drawRound(x - 0.5f, y - 0.5f, w + 1f, h + 0.5f, 3.5f, borderColor);
        DrawUtil.drawRoundBlur(x, y, w, h, 3.5f, ColorProvider.rgba(20, 20, 25, hovered ? 200 : 150), 14f);

        float textW = Fonts.SFREGULAR.get().getWidth(label, 7f);
        int textColor = ColorProvider.rgba(255, 255, 255, hovered ? 255 : 210);
        DrawUtil.drawText(Fonts.SFREGULAR.get(), label, x + w / 2f - textW / 2f, y + (h - 6.5f) / 2f, textColor, 7f);
        clickAreas.add(new ClickArea(x, y, w, h, action));
    }

    private void renderList(CustomDrawContext ctx, int mouseX, int mouseY) {
        float listTop = panelY + 86f;
        float listBottom = panelY + panelH - 46f;
        float listLeft = panelX + MARGIN;
        float listRight = panelX + panelW - MARGIN;
        float listWidth = listRight - listLeft;
        int visibleRows = Math.max(0, (int) ((listBottom - listTop + ROW_GAP) / (ROW_HEIGHT + ROW_GAP)));
        int maxScroll = getMaxScroll();
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }

        if (alts.isEmpty()) {
            DrawUtil.drawText(Fonts.SFREGULAR.get(), "\u041D\u0435\u0442 \u0430\u043A\u043A\u0430\u0443\u043D\u0442\u043E\u0432. \u0421\u043E\u0437\u0434\u0430\u0439 \u043F\u0435\u0440\u0432\u044B\u0439!", listLeft + 6f, listTop + 8f, ColorProvider.rgba(140, 140, 140, 200), 7f);
        }

        String currentUser = this.client.getSession().getUsername();
        for (int i = 0; i < visibleRows; i++) {
            int index = i + scrollOffset;
            if (index >= alts.size()) {
                break;
            }
            Alt alt = alts.get(index);
            float rowY = listTop + i * (ROW_HEIGHT + ROW_GAP);
            boolean selected = currentUser != null && currentUser.equalsIgnoreCase(alt.name());
            boolean hovered = HoverUtil.isHovered(mouseX, mouseY, listLeft, rowY, listWidth, ROW_HEIGHT);

            int rowFill = selected
                    ? ColorProvider.rgba(255, 255, 255, 235)
                    : ColorProvider.rgba(20, 20, 25, hovered ? 190 : 150);
            DrawUtil.drawRoundBlur(listLeft, rowY, listWidth, ROW_HEIGHT, 4f, rowFill, 14f);
            if (selected) {
                DrawUtil.drawRound(listLeft - 0.5f, rowY - 0.5f, listWidth + 1f, ROW_HEIGHT + 0.5f, 4f, ColorProvider.rgba(255, 255, 255, 200));
            } else if (hovered) {
                DrawUtil.drawRound(listLeft - 0.5f, rowY - 0.5f, listWidth + 1f, ROW_HEIGHT + 0.5f, 4f, ColorProvider.setAlpha(ColorProvider.getThemeColor(), 110));
            }

            float innerX = listLeft + 6f;
            float ctrlY = rowY + (ROW_HEIGHT - 20f) / 2f;

            float pinW = 22f;
            boolean pinHovered = HoverUtil.isHovered(mouseX, mouseY, innerX, ctrlY, pinW, 20f);
            if (pinHovered) {
                CursorManager.requestHand();
            }
            drawPin(ctx, innerX + 5f, rowY + (ROW_HEIGHT - 16f) / 2f, 13f, alt.pinned(), pinHovered);
            clickAreas.add(new ClickArea(innerX, ctrlY, pinW, 20f, () -> onTogglePin(alt)));

            float deleteW = 62f;
            float deleteX = listRight - deleteW;
            boolean deleteHovered = HoverUtil.isHovered(mouseX, mouseY, deleteX, rowY + 3f, deleteW, ROW_HEIGHT - 6f);
            if (deleteHovered) {
                CursorManager.requestHand();
            }
            DrawUtil.drawRoundBlur(deleteX, rowY + 3f, deleteW, ROW_HEIGHT - 6f, 3f, ColorProvider.setAlpha(ColorProvider.rgba(192, 57, 43, 255), deleteHovered ? 215 : 155), 12f);
            drawCenteredText("Удалить", deleteX, rowY + 3f, deleteW, ROW_HEIGHT - 6f, 6.5f, ColorProvider.rgba(255, 255, 255, 245));
            clickAreas.add(new ClickArea(deleteX, rowY + 3f, deleteW, ROW_HEIGHT - 6f, () -> onDelete(alt)));

            float nameX = innerX + pinW + 8f;
            String activeLabel = "Активный";
            float activeW = selected ? Fonts.SFREGULAR.get().getWidth(activeLabel, 6f) + 8f : 0f;
            float nameMax = deleteX - 8f - activeW - nameX;
            float rowBodyX = nameX - 4f;
            float rowBodyW = deleteX - 8f - rowBodyX;
            boolean rowHovered = HoverUtil.isHovered(mouseX, mouseY, rowBodyX, rowY, rowBodyW, ROW_HEIGHT);
            if (rowHovered) {
                CursorManager.requestHand();
            }
            clickAreas.add(new ClickArea(rowBodyX, rowY, rowBodyW, ROW_HEIGHT, () -> onLogin(alt)));

            int nameColor = selected
                    ? ColorProvider.rgba(18, 18, 20, 245)
                    : ColorProvider.rgba(235, 235, 235, 240);
            DrawUtil.drawText(Fonts.SFREGULAR.get(), alt.name(), nameX, rowY + (ROW_HEIGHT - 6.5f) / 2f, nameColor, 7f, 0.8f, 1f, nameMax);
            if (selected) {
                DrawUtil.drawText(Fonts.SFREGULAR.get(), activeLabel, deleteX - 8f - Fonts.SFREGULAR.get().getWidth(activeLabel, 6f), rowY + (ROW_HEIGHT - 6f) / 2f, ColorProvider.rgba(18, 18, 20, 170), 6f);
            }
        }

        if (maxScroll > 0) {
            float trackH = listBottom - listTop;
            float thumbH = Math.max(16f, trackH * (visibleRows / (float) Math.max(1, alts.size())));
            float thumbY = listTop + ((trackH - thumbH) * scrollOffset / (float) maxScroll);
            DrawUtil.drawRound(panelX + panelW - 6f, listTop, 2.5f, trackH, 1f, ColorProvider.rgba(60, 60, 70, 120));
            DrawUtil.drawRound(panelX + panelW - 6f, thumbY, 2.5f, thumbH, 1f, ColorProvider.setAlpha(ColorProvider.getThemeColor(), 200));
        }
    }

    private void drawPin(CustomDrawContext ctx, float x, float y, float size, boolean pinned, boolean hovered) {
        int starTex = this.client.getTextureManager().getTexture(STAR_ID).getGlId();
        int color;
        if (pinned) {
            color = ColorProvider.setAlpha(ColorProvider.rgba(255, 215, 0, 255), 245);
        } else {
            color = ColorProvider.rgba(hovered ? 190 : 130, hovered ? 190 : 130, hovered ? 190 : 130, 210);
        }
        Builder.texture()
                .size(new SizeState(size, size))
                .radius(QuadRadiusState.NO_ROUND)
                .color(new QuadColorState(color))
                .texture(0f, 0f, 1f, 1f, starTex)
                .smoothness(1f)
                .build()
                .render(ctx.getMatrices().peek().getPositionMatrix(), x, y, 0f);
    }

    private void drawCenteredText(String label, float x, float y, float w, float h, float size, int color) {
        float textW = Fonts.SFREGULAR.get().getWidth(label, size);
        DrawUtil.drawText(Fonts.SFREGULAR.get(), label, x + w / 2f - textW / 2f, y + (h - size) / 2f, color, size);
    }

    private void onCreate() {
        String raw = fieldText.trim();
        if (raw.length() < 3) {
            setStatus("\u0418\u043C\u044F \u0434\u043E\u043B\u0436\u043D\u043E \u0441\u043E\u0434\u0435\u0440\u0436\u0430\u0442\u044C \u043C\u0438\u043D\u0438\u043C\u0443\u043C 3 \u0441\u0438\u043C\u0432\u043E\u043B\u0430", true);
            return;
        }
        String name = AltManager.sanitizeName(raw);
        if (name.length() != raw.length() || !AltManager.isValidName(name)) {
            setStatus("\u0420\u0430\u0437\u0440\u0435\u0448\u0435\u043D\u044B \u0442\u043E\u043B\u044C\u043A\u043E \u0431\u0443\u043A\u0432\u044B, \u0446\u0438\u0444\u0440\u044B \u0438 _ (3-16 \u0441\u0438\u043C\u0432\u043E\u043B\u043E\u0432)", true);
            return;
        }
        if (!AltManager.addAlt(name)) {
            setStatus("\u0422\u0430\u043A\u043E\u0439 \u0430\u043A\u043A\u0430\u0443\u043D\u0442 \u0443\u0436\u0435 \u0435\u0441\u0442\u044C", true);
            return;
        }
        fieldText = "";
        refreshAlts();
        setStatus("\u0410\u043A\u043A\u0430\u0443\u043D\u0442 '" + name + "' \u0441\u043E\u0437\u0434\u0430\u043D", false);
    }

    private void onRandom() {
        String name = AltManager.generateRandomName(new Random());
        if (!AltManager.addAlt(name)) {
            setStatus("\u041D\u0435 \u0443\u0434\u0430\u043B\u043E\u0441\u044C \u0441\u043E\u0437\u0434\u0430\u0442\u044C \u0440\u0430\u043D\u0434\u043E\u043C\u043D\u044B\u0439 \u0430\u043A\u043A\u0430\u0443\u043D\u0442", true);
            return;
        }
        refreshAlts();
        setStatus("\u0420\u0430\u043D\u0434\u043E\u043C\u043D\u044B\u0439 \u0430\u043A\u043A\u0430\u0443\u043D\u0442 '" + name + "' \u0441\u043E\u0437\u0434\u0430\u043D", false);
    }

    private void onDelete(Alt alt) {
        AltManager.removeAlt(alt.name());
        refreshAlts();
        setStatus("\u0410\u043A\u043A\u0430\u0443\u043D\u0442 '" + alt.name() + "' \u0443\u0434\u0430\u043B\u0451\u043D", false);
    }

    private void onTogglePin(Alt alt) {
        AltManager.setPinned(alt.name(), !alt.pinned());
        refreshAlts();
        setStatus(alt.pinned() ? "\u0410\u043A\u043A\u0430\u0443\u043D\u0442 '" + alt.name() + "' \u043E\u0442\u043A\u0440\u0435\u043F\u043B\u0435\u043D" : "\u0410\u043A\u043A\u0430\u0443\u043D\u0442 '" + alt.name() + "' \u0437\u0430\u043A\u0440\u0435\u043F\u043B\u0451\u043D", false);
    }

    private void onLogin(Alt alt) {
        AltManager.login(alt);
        setStatus("\u0412\u043E\u0448\u0435\u043B \u043A\u0430\u043A " + alt.name(), false);
    }

    private void refreshAlts() {
        this.alts = AltManager.getAlts();
        this.scrollOffset = Math.min(this.scrollOffset, getMaxScroll());
    }

    private int getMaxScroll() {
        int visibleRows = Math.max(0, (int) ((panelH - 46f - 86f + ROW_GAP) / (ROW_HEIGHT + ROW_GAP)));
        return Math.max(0, alts.size() - visibleRows);
    }

    private void setStatus(String text, boolean error) {
        this.statusText = text;
        this.statusError = error;
    }

    private record ClickArea(float x, float y, float w, float h, Runnable action) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        }
    }
}