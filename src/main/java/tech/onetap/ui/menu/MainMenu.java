package tech.onetap.ui.menu;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;
import tech.onetap.util.cursor.CursorManager;
import tech.onetap.util.config.ChangelogManager;
import tech.onetap.util.render.helper.HoverUtil;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.CustomDrawContext;
import tech.onetap.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class MainMenu extends Screen {
    private static final Identifier STAR_ID = Identifier.of("mre", "images/star.png");
    private final Identifier backmenu = Identifier.of("mre", "images/backmenu.png");
    private final List<MenuButton> buttons = new ArrayList<>();
    private static final CopyOnWriteArrayList<Particle> particles = new CopyOnWriteArrayList<>();
    private long lastParticleSpawn = 0L;
    private float animationProgress = 0f;
    private boolean isWindowMode = false;
    private long lastAnimationTime = 0L;
    private boolean shouldBeWindowMode = false;
    private boolean isAccountManagerOpen = false;
    private final AltWidget altWidget = new AltWidget();

    public MainMenu() {
        super(Text.of("Main Menu"));
    }

    @Override
    protected void init() {
        buttons.clear();

        float buttonWidth = 70f;
        float buttonHeight = 60f;
        float spacing = 5f;
        float windowModeButtonSize = 60f;

        shouldBeWindowMode = client.getWindow().getWidth() <= 1050 || client.getWindow().getHeight() <= 625;

        float xFullscreen = width - buttonWidth;
        float totalHeightFullscreen = 5 * buttonHeight + 4 * spacing;
        float startYFullscreen = height / 2f - totalHeightFullscreen / 2f;

        float totalWidthWindow = 5 * windowModeButtonSize + 4 * spacing;
        float startXWindow = width / 2f - totalWidthWindow / 2f;
        float yWindow = height / 2f - windowModeButtonSize / 2f + 80f;

        buttons.add(new MenuButton(xFullscreen, startYFullscreen, startXWindow, yWindow, buttonWidth, buttonHeight, windowModeButtonSize, "\u041E\u0434\u0438\u043D\u043E\u0447\u043D\u0430\u044F \u0438\u0433\u0440\u0430", "I", false, () -> {
            client.setScreen(new SelectWorldScreen(this));
        }));
        buttons.add(new MenuButton(xFullscreen, startYFullscreen + buttonHeight + spacing, startXWindow + windowModeButtonSize + spacing, yWindow, buttonWidth, buttonHeight, windowModeButtonSize, "\u0421\u0435\u0442\u0435\u0432\u0430\u044F \u0438\u0433\u0440\u0430", "W", false, () -> {
            client.setScreen(new MultiplayerScreen(this));
        }));
        buttons.add(new MenuButton(xFullscreen, startYFullscreen + 2 * (buttonHeight + spacing), startXWindow + 2 * (windowModeButtonSize + spacing), yWindow, buttonWidth, buttonHeight, windowModeButtonSize, "\u041C\u0435\u043D\u0435\u0434\u0436\u0435\u0440 \u0430\u043A\u043A\u0430\u0443\u043D\u0442\u043E\u0432", "D", false, () -> {
            altWidget.open = !altWidget.open;
        }));
        buttons.add(new MenuButton(xFullscreen, startYFullscreen + 3 * (buttonHeight + spacing), startXWindow + 3 * (windowModeButtonSize + spacing), yWindow, buttonWidth, buttonHeight, windowModeButtonSize, "\u041D\u0430\u0441\u0442\u0440\u043E\u0439\u043A\u0438", "F", false, () -> {
            client.setScreen(new OptionsScreen(this, client.options));
        }));
        buttons.add(new MenuButton(xFullscreen, startYFullscreen + 4 * (buttonHeight + spacing), startXWindow + 4 * (windowModeButtonSize + spacing), yWindow, buttonWidth, buttonHeight, windowModeButtonSize, "\u0412\u044B\u0445\u043E\u0434", "Q", true, () -> {
            client.scheduleStop();
        }));

        particles.forEach(particle -> particle.y = ThreadLocalRandom.current().nextInt(-5, height));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isAccountManagerOpen) {
            altWidget.updateScroll(mouseX, mouseY, (float) verticalAmount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        CustomDrawContext ctx = CustomDrawContext.of(context);
        super.render(context, mouseX, mouseY, delta);

        if (System.currentTimeMillis() - lastParticleSpawn > 100) {
            particles.add(new Particle());
            lastParticleSpawn = System.currentTimeMillis();
        }

        int windowWidth = width;
        int windowHeight = height;

        boolean newShouldBeWindowMode = client.getWindow().getWidth() <= 1050 || client.getWindow().getHeight() <= 625;
        if (newShouldBeWindowMode != shouldBeWindowMode) {
            shouldBeWindowMode = newShouldBeWindowMode;
        }

        float targetProgress = shouldBeWindowMode ? 1.0f : 0.0f;
        long now = System.currentTimeMillis();
        float frameTime = lastAnimationTime == 0L ? 0.05f : Math.min((now - lastAnimationTime) / 1000f, 0.1f);
        lastAnimationTime = now;
        animationProgress = approach(animationProgress, targetProgress, frameTime * 4f);
        if (Math.abs(animationProgress - targetProgress) < 0.01f) {
            animationProgress = targetProgress;
        }
        isWindowMode = animationProgress > 0.5f;

        CursorManager.resetAll();

        DrawUtil.drawImage(ctx.getMatrices(), backmenu, 0, 0, width, height, -1);
        drawEdgeGlow(width, height);

        int themeA = ColorProvider.getThemeColor();
        int themeB = ColorProvider.getThemeColorTwo();

        for (Particle particle : particles) {
            particle.update();
            particle.render(ctx);
        }

        isAccountManagerOpen = altWidget.open;
        buttons.forEach(b -> b.render(ctx, mouseX, mouseY, animationProgress, isAccountManagerOpen));

        if (isAccountManagerOpen) {
            altWidget.render(ctx, mouseX, mouseY);
        }

        renderChangelog(ctx, themeA, themeB);

        renderPlaque(ctx, windowWidth, windowHeight, themeA, themeB);

        long window = client.getWindow().getHandle();
        CursorManager.applyRequested(window);
    }

    private static float approach(float current, float target, float maxStep) {
        if (current < target) {
            return Math.min(target, current + maxStep);
        } else if (current > target) {
            return Math.max(target, current - maxStep);
        }
        return target;
    }

    private void drawEdgeGlow(int width, int height) {
        int color = ColorProvider.rgba(46, 82, 150, 190);
        float blur = 48f;

        DrawUtil.drawRoundBlur(-blur, -18, width + blur * 2, 26, 0f, color, blur);
        DrawUtil.drawRoundBlur(-blur, height - 8, width + blur * 2, 26, 0f, color, blur);
        DrawUtil.drawRoundBlur(-18, -blur, 26, height + blur * 2, 0f, color, blur);
        DrawUtil.drawRoundBlur(width - 8, -blur, 26, height + blur * 2, 0f, color, blur);
    }

    private void renderChangelog(CustomDrawContext ctx, int themeA, int themeB) {
        List<String> lines = ChangelogManager.getLines();
        if (lines.isEmpty()) return;

        float panelW = 220f;
        float padding = 10f;
        float headerH = 24f;
        float lineH = 12f;
        float fontSize = 8f;

        float panelX = 14f;
        float panelY = 24f;
        float panelH = Math.min(height - 48f, headerH + padding * 2f + lines.size() * (lineH + 2f));

        int panelBase = ColorProvider.rgba(6, 14, 34, 160);
        int panelFill = ColorProvider.rgba(9, 20, 46, 150);
        int panelBorder = ColorProvider.rgba(38, 58, 110, 170);
        int textColor = ColorProvider.rgba(255, 255, 255, 210);

        DrawUtil.drawRoundBlur(panelX, panelY, panelW, panelH, 8f, panelBase, 14f);
        DrawUtil.drawRound(panelX - 0.7f, panelY - 0.7f, panelW + 1.4f, panelH + 1.4f, 8f, panelBorder);
        DrawUtil.drawRound(panelX, panelY, panelW, panelH, 8f, panelFill);
        DrawUtil.drawRound(panelX + 8f, panelY + 1f, panelW - 16f, 2f, 1f, ColorProvider.setAlpha(themeA, 200));

        DrawUtil.drawText(Fonts.SFBOLD.get(), "ChangeLog", panelX + 10, panelY + 8, textColor, 10f);

        float y = panelY + headerH + padding;
        for (String line : lines) {
            if (y + lineH > panelY + panelH - padding) break;
            DrawUtil.drawText(Fonts.SFREGULAR.get(), line.isEmpty() ? " " : line, panelX + padding, y, textColor, fontSize);
            y += lineH + 2f;
        }
    }

    private void renderPlaque(CustomDrawContext ctx, int windowWidth, int windowHeight, int themeA, int themeB) {
        float plaqueHeight = 20f;
        float padding = 8f;
        float fontSize = 9f;

        String clientName = "pharaonBeta";
        String usernameText = client.getSession().getUsername();

        float clientNameWidth = Fonts.SFREGULAR.get().getWidth(clientName, fontSize);
        float usernameWidth = Fonts.SFREGULAR.get().getWidth(usernameText, fontSize);
        float plaqueWidth = clientNameWidth + usernameWidth + padding * 3;

        float plaqueX = (windowWidth - plaqueWidth) / 2f;
        float plaqueY = 20f;

        float textY = plaqueY + (plaqueHeight - fontSize) / 2f;
        float x = plaqueX + padding;
        x = drawText(ctx, clientName, x, textY, ColorProvider.colorLerp(themeA, themeB, 8.0f, 0), fontSize);
        drawText(ctx, usernameText, x, textY, ColorProvider.rgba(255, 255, 255, 255), fontSize);
    }

    private float drawText(CustomDrawContext ctx, String text, float x, float y, int color, float size) {
        float width = Fonts.SFREGULAR.get().getWidth(text, size);
        DrawUtil.drawText(Fonts.SFREGULAR.get(), text, x, y + 2, color, size);
        return x + width + 5f;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (isAccountManagerOpen) {
            altWidget.onChar(chr);
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (altWidget.open) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                altWidget.close();
                return true;
            }
            altWidget.onKey(keyCode);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        isAccountManagerOpen = altWidget.open;
        boolean altWidgetClicked = false;

        if (isAccountManagerOpen) {
            altWidget.click((int) mouseX, (int) mouseY, button);
            altWidgetClicked = true;
        }

        buttons.forEach(b -> b.click(mouseX, mouseY, button, animationProgress));

        if (altWidgetClicked) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private class Particle {
        private final float x;
        private float y;
        private float size;

        public Particle() {
            x = ThreadLocalRandom.current().nextInt(0, client.getWindow().getScaledWidth());
            y = 0;
            size = 0;
        }

        public void update() {
            y += 1f;
        }

        public void render(CustomDrawContext ctx) {
            size += 0.1f;
            float sway = (float) (x + Math.sin(System.nanoTime() / 1_000_000_000f) * 5);
            float multi = 1 - MathHelper.clamp(y / (float) client.getWindow().getScaledHeight(), 0, 1);
            y += 1;
            int alpha = (int) (255 * MathHelper.clamp(size * multi, 0, 5) / 5f);
            int scaledSize = (int) MathHelper.clamp(size * multi * 4.0f, 0, 20);
            if (scaledSize > 0.5f) {
                DrawUtil.drawImage(ctx.getMatrices(), STAR_ID, sway, y, scaledSize, scaledSize,
                        ColorProvider.setAlpha(ColorProvider.rgba(255, 255, 255, 255), alpha));
            }
            if (y >= client.getWindow().getScaledHeight()) {
                particles.remove(this);
            }
        }
    }

    private class MenuButton {
        private final float xFullscreen, yFullscreen, xWindow, yWindow, widthFullscreen, heightFullscreen, sizeWindow;
        private final String text;
        private final String icon;
        private final boolean crossLabel;
        private final Runnable action;
        private float hoverProgress = 0f;
        private long lastHoverUpdate = 0L;

        public MenuButton(float xFullscreen, float yFullscreen, float xWindow, float yWindow,
                          float width, float height, float sizeWindow,
                          String text, String icon, boolean crossLabel, Runnable action) {
            this.xFullscreen = xFullscreen;
            this.yFullscreen = yFullscreen;
            this.xWindow = xWindow;
            this.yWindow = yWindow;
            this.widthFullscreen = width;
            this.heightFullscreen = height;
            this.sizeWindow = sizeWindow;
            this.text = text;
            this.icon = icon;
            this.crossLabel = crossLabel;
            this.action = action;
        }

        public void render(CustomDrawContext ctx, int mouseX, int mouseY, float animationProgress, boolean accountManagerOpen) {
            boolean currentlyWindowMode = animationProgress > 0.5f;
            float textWidth = currentlyWindowMode ? 0f : Fonts.SFREGULAR.get().getWidth(text, 16f);
            float maxExtension = (textWidth + 2f + 5f);
            float currentWidth = currentlyWindowMode ? sizeWindow : widthFullscreen + maxExtension * hoverProgress;
            float currentHeight = currentlyWindowMode ? sizeWindow : heightFullscreen;
            float currentX = currentlyWindowMode ? MathHelper.lerp(animationProgress, xFullscreen, xWindow) : MathHelper.lerp(animationProgress, xFullscreen, xWindow) - maxExtension * hoverProgress;
            float currentY = MathHelper.lerp(animationProgress, yFullscreen, yWindow);

            // стабильный хитбокс: полная расширенная зона (исключает осцилляцию при анимации hover)
            float hitX = currentlyWindowMode ? currentX : currentX + maxExtension * hoverProgress - maxExtension;
            float hitY = currentY;
            float hitW = currentWidth + (currentlyWindowMode ? 0f : maxExtension * (1f - hoverProgress));
            float hitH = currentHeight;

            boolean isHovered = !accountManagerOpen && HoverUtil.isHovered(mouseX, mouseY, hitX, hitY, hitW, hitH);

            long now = System.currentTimeMillis();
            float frameTime = lastHoverUpdate == 0L ? 0.05f : Math.min((now - lastHoverUpdate) / 1000f, 0.1f);
            lastHoverUpdate = now;
            float targetHover = isHovered ? 1.0f : 0.0f;
            hoverProgress = accountManagerOpen ? 0f : approach(hoverProgress, targetHover, frameTime * 8f);
            if (Math.abs(hoverProgress - targetHover) < 0.01f) {
                hoverProgress = targetHover;
            }

            if (isHovered) {
                CursorManager.requestHand();
            }

            int themeA = ColorProvider.getThemeColor();
            int themeB = ColorProvider.getThemeColorTwo();

            if (hoverProgress > 0) {
                DrawUtil.drawRoundBlur(currentX, currentY, currentWidth, currentHeight, 5f,
                        ColorProvider.setAlpha(ColorProvider.interpolateColor(themeA, themeB, hoverProgress), (int) (150 * hoverProgress)), 12f);
            }

            DrawUtil.drawRound(currentX, currentY, currentWidth, currentHeight, 5f, ColorProvider.rgba(0, 0, 0, 120));

            float iconSize = currentlyWindowMode ? 35f : 40f;
            float iconX = currentX + (currentlyWindowMode ? (sizeWindow - iconSize) / 2f : 10f);
            float iconY = currentY + (currentHeight - iconSize) / 2f;
            int iconColor = ColorProvider.rgba(255, 255, 255, 255);
            if (crossLabel) {
                if (!currentlyWindowMode) {
                    float wordSize = 16f;
                    float crossSize = 26f;
                    drawCrossIcon(ctx.getMatrices(), iconX + crossSize * 0.5f, iconY + iconSize * 0.5f, crossSize, 3f, iconColor);
                    float wordX = iconX + crossSize + 6f;
                    DrawUtil.drawText(Fonts.SFREGULAR.get(), text, wordX, currentY + (currentHeight - wordSize) / 2f, iconColor, wordSize);
                } else {
                    float crossSize = 22f;
                    drawCrossIcon(ctx.getMatrices(), iconX + iconSize * 0.5f, iconY + iconSize * 0.5f, crossSize, 3f, iconColor);
                }
            } else {
                ctx.drawText(Fonts.ICONS2.get(), icon, iconX, iconY, iconColor, iconSize);
            }

            if (!currentlyWindowMode && hoverProgress > 0) {
                float textX = currentX + widthFullscreen + 2f;
                float textY = currentY + (heightFullscreen - 16f) / 2f;
                int textAlpha = (int) (255 * hoverProgress);
                int textColor = ColorProvider.rgba(255, 255, 255, textAlpha);
                if (!crossLabel) {
                    DrawUtil.drawText(Fonts.SFREGULAR.get(), text, textX, textY, textColor, 16f);
                }
            }
        }

        private void drawCrossIcon(MatrixStack matrices, float cx, float cy, float size, float thickness, int color) {
            matrices.push();
            matrices.translate(cx, cy, 0.0f);
            float half = size / 2f;
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45f));
            DrawUtil.drawRound(matrices.peek().getPositionMatrix(), -half, -thickness / 2f, size, thickness, thickness / 2f, color);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90f));
            DrawUtil.drawRound(matrices.peek().getPositionMatrix(), -half, -thickness / 2f, size, thickness, thickness / 2f, color);
            matrices.pop();
        }

        public void click(double mouseX, double mouseY, int button, float animationProgress) {
            boolean currentlyWindowMode = animationProgress > 0.5f;
            float textWidth = currentlyWindowMode ? 0f : Fonts.SFREGULAR.get().getWidth(text, 16f);
            float maxExtension = (textWidth + 2f + 5f);
            float currentX = MathHelper.lerp(animationProgress, xFullscreen, xWindow) - (currentlyWindowMode ? 0f : maxExtension * hoverProgress);
            float currentY = MathHelper.lerp(animationProgress, yFullscreen, yWindow);
            float currentWidth = currentlyWindowMode ? sizeWindow : widthFullscreen + maxExtension * hoverProgress;
            float currentHeight = currentlyWindowMode ? sizeWindow : heightFullscreen;

            float hitX = currentlyWindowMode ? currentX : currentX + maxExtension * hoverProgress - maxExtension;
            float hitW = currentWidth + (currentlyWindowMode ? 0f : maxExtension * (1f - hoverProgress));
            float hitH = currentHeight;

            if (HoverUtil.isHovered(mouseX, mouseY, hitX, currentY, hitW, hitH)) {
                action.run();
            }
        }
    }
}