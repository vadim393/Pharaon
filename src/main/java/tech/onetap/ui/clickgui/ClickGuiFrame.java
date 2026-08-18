package tech.onetap.ui.clickgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.Module;
import tech.onetap.util.IMinecraft;
import tech.onetap.util.render.helper.HoverUtil;
import tech.onetap.util.render.math.Animation;
import tech.onetap.util.render.math.Easing;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClickGuiFrame extends Screen implements IMinecraft {
    public static boolean showThemes = false;
    public static String searchText = "";
    public static String hoveredDesc = "";

    private final List<Panel> panels = new ArrayList<>();
    private final float[] panelTargetX;
    private final float[] panelTargetY;
    private float panelWidth = 130.0F;
    private float panelHeight;
    private float panelSpacing = 8.0F;

    private final Animation openAnim = new Animation(Easing.BACK_OUT, 500);
    private boolean closing;
    private boolean pendingClose;
    private boolean searchFocused;
    private long searchBlink = 0L;
    private int draggingPanel = -1;
    private float dragOffsetX;
    private float dragOffsetY;
    private static final Map<String, float[]> themeSquares = new LinkedHashMap<>();

    public ClickGuiFrame() {
        super(Text.literal("Click Gui"));
        showThemes = false;
        searchText = "";
        hoveredDesc = "";
        int count = ModuleCategory.values().length;
        this.panelTargetX = new float[count];
        this.panelTargetY = new float[count];
        updateLayout();
    }

    public boolean isSearchFocused() {
        return searchFocused;
    }

    public void prepareForOpen() {
        showThemes = false;
        searchText = "";
        hoveredDesc = "";
        closing = false;
        pendingClose = false;
        searchFocused = false;
        openAnim.reset(0.0F);
    }

    private void updateLayout() {
        int count = ModuleCategory.values().length;
        float scw = mc.getWindow().getScaledWidth();
        float sch = mc.getWindow().getScaledHeight();
        this.panelHeight = Math.max(120.0F, Math.min(240.0F, sch - 170.0F));
        float total = count * panelWidth + (count - 1) * panelSpacing;
        float startX = (scw - total) / 2.0F;

        for (int i = 0; i < count; i++) {
            panelTargetX[i] = startX + i * (panelWidth + panelSpacing);
            panelTargetY[i] = (sch - panelHeight) / 2.0F;
        }
    }

    @Override
    protected void init() {
        super.init();
    }

    private void rebuildPanels() {
        panels.clear();
        ModuleCategory[] categories = ModuleCategory.values();
        for (int i = 0; i < categories.length; i++) {
            panels.add(new Panel(categories[i], panelTargetX[i], panelTargetY[i], panelWidth, panelHeight));
        }
    }

    @Override
    public void tick() {
        handleMovementKeys();
        if (panels.isEmpty()) {
            rebuildPanels();
        }
        super.tick();
    }

    private void handleMovementKeys() {
        if (mc.player == null || searchFocused) return;
        long handle = mc.getWindow().getHandle();
        KeyBinding[] keys = {mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey};
        for (KeyBinding key : keys) {
            int code = InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey()).getCode();
            key.setPressed(InputUtil.isKeyPressed(handle, code));
        }
        if (mc.player.getAbilities().flying) {
            int code = InputUtil.fromTranslationKey(mc.options.sneakKey.getBoundKeyTranslationKey()).getCode();
            mc.options.sneakKey.setPressed(InputUtil.isKeyPressed(handle, code));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        float scw = mc.getWindow().getScaledWidth();
        float sch = mc.getWindow().getScaledHeight();

        DrawUtil.drawRound(0.0F, 0.0F, scw, sch, 0.0F, ColorProvider.rgba(0, 0, 0, 55));

        openAnim.setDuration(closing ? 250L : 500L);
        openAnim.run(closing ? 0.0F : 1.0F);
        float open = Math.max(0.0F, openAnim.getValue());

        if (closing && openAnim.getValue() < 0.04F && !pendingClose) {
            pendingClose = true;
            mc.setScreen(null);
        }

        if (panels.isEmpty()) {
            rebuildPanels();
        }

        float centerX = scw / 2.0F;
        List<ModuleCategory> categories = List.of(ModuleCategory.values());
        for (int i = 0; i < panels.size(); i++) {
            Panel p = panels.get(i);
            float targetX = panelTargetX[i];
            float targetY = panelTargetY[i];
            float dx = targetX - centerX;
            p.x = centerX + dx * open;
            p.y = targetY + (1.0F - open) * 6.0F;
            p.render(mouseX, mouseY);
        }

        renderSearch(mouseX, mouseY, scw, sch);
        renderThemeBar(mouseX, mouseY, scw, sch, open);
        renderDescription(open, scw, sch);
    }

    private void renderDescription(float open, float scw, float sch) {
        if (hoveredDesc == null || hoveredDesc.isEmpty()) return;
        float size = 9.5F;
        float width = Fonts.SFREGULAR.get().getWidth(hoveredDesc, size);
        int color = ColorProvider.setAlpha(ColorProvider.rgba(255, 255, 255, 255), (int) (200.0F * open));
        float x = Math.max(20.0F, Math.min(scw - width - 20.0F, (scw - width) / 2.0F));
        DrawUtil.drawRound(x - 10.0F, sch - 34.0F, width + 20.0F, 20.0F, 2.0F, ClickGuiUtil.backgroundSoft());
        DrawUtil.drawText(Fonts.SFREGULAR.get(), hoveredDesc, x, sch - 32.0F, color, size);
    }

    private void renderSearch(int mouseX, int mouseY, float scw, float sch) {
        float rectW = 180.0F;
        float rectH = 20.0F;
        float rectX = (scw - rectW) / 2.0F;
        float rectY = Math.min(sch - 70.0F, panelTargetY[0] + panelHeight + 24.0F);

        boolean hovered = HoverUtil.isHovered(mouseX, mouseY, rectX, rectY, rectW, rectH);
        int bg = ClickGuiUtil.background();
        DrawUtil.drawRound(rectX, rectY, rectW, rectH, 2.0F, bg);
        ClickGuiUtil.drawRoundOutline(rectX, rectY, rectW, rectH, 2.0F, 0,
                ColorProvider.setAlpha(ClickGuiUtil.accent(), searchFocused || hovered ? 110 : 45),
                ColorProvider.setAlpha(ClickGuiUtil.accent(), searchFocused || hovered ? 110 : 45),
                ColorProvider.setAlpha(ClickGuiUtil.accent(), searchFocused || hovered ? 110 : 45),
                ColorProvider.setAlpha(ClickGuiUtil.accent(), searchFocused || hovered ? 110 : 45));

        String text = searchText;
        if (text.isEmpty() && !searchFocused) {
            DrawUtil.drawText(Fonts.SFREGULAR.get(), "Поиск...", rectX + 10.0F, rectY + (rectH - 9.5F) / 2.0F, ClickGuiUtil.textMuted(), 9.5F);
        } else {
            if (searchFocused && (System.currentTimeMillis() / 500L) % 2 == 0) {
                text = text + "_";
            }
            DrawUtil.drawText(Fonts.SFREGULAR.get(), text, rectX + 10.0F, rectY + (rectH - 9.5F) / 2.0F, ClickGuiUtil.textColor(), 9.5F);
        }
    }

    private void renderThemeBar(int mouseX, int mouseY, float scw, float sch, float open) {
        float pillW = 64.0F;
        float pillH = 16.0F;
        float pillX = (scw - pillW) / 2.0F;
        float pillY = 8.0F;
        boolean hovered = HoverUtil.isHovered(mouseX, mouseY, pillX, pillY, pillW, pillH);
        DrawUtil.drawRound(pillX, pillY, pillW, pillH, pillH / 2.0F, ClickGuiUtil.backgroundSoft());
        DrawUtil.drawRound(pillX, pillY, pillW, pillH, pillH / 2.0F, ColorProvider.setAlpha(ClickGuiUtil.accent(), hovered || showThemes ? 80 : 35));
        DrawUtil.drawText(Fonts.SFREGULAR.get(), "Темы", pillX + pillW / 2.0F - Fonts.SFREGULAR.get().getWidth("Темы", 8.5F) / 2.0F, pillY + (pillH - 8.5F) / 2.0F, ClickGuiUtil.textColor(), 8.5F);

        if (!showThemes) return;

        themeSquares.clear();
        float sq = 13.0F;
        float spacing = 4.0F;
        int perRow = 7;
        int rows = (int) Math.ceil(ClickGuiUtil.getStyleColors().size() / (double) perRow);
        float windowW = perRow * sq + (perRow - 1) * spacing + 12.0F;
        float windowH = rows * sq + (rows - 1) * spacing + 12.0F;
        float windowX = (scw - windowW) / 2.0F;
        float windowY = pillY + pillH + 6.0F;
        DrawUtil.drawRound(windowX, windowY, windowW, windowH, 2.0F, ClickGuiUtil.background());

        int index = 0;
        for (String mode : ClickGuiUtil.getStyleColors().keySet()) {
            int color = ClickGuiUtil.colorFor(mode);
            int row = index / perRow;
            int col = index % perRow;
            float squareX = windowX + 6.0F + col * (sq + spacing);
            float squareY = windowY + 6.0F + row * (sq + spacing);
            themeSquares.put(mode, new float[]{squareX, squareY, sq, sq});
            DrawUtil.drawRound(squareX, squareY, sq, sq, 1.0F, color);
            if (mode.equals(ClickGuiUtil.theme)) {
                ClickGuiUtil.drawRoundOutline(squareX, squareY, sq, sq, 3.0F, 0,
                        0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF);
            }
            index++;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tech.onetap.ui.clickgui.objects.sets.BindObject.hasActiveListening()
                && tech.onetap.ui.clickgui.objects.sets.BindObject.captureMouseButton(button)) {
            return true;
        }
        float pillW = 64.0F;
        float pillH = 16.0F;
        float pillX = (mc.getWindow().getScaledWidth() - pillW) / 2.0F;
        float pillY = 8.0F;
        if (HoverUtil.isHovered(mouseX, mouseY, pillX, pillY, pillW, pillH)) {
            showThemes = !showThemes;
            return true;
        }
        if (showThemes) {
            for (Map.Entry<String, float[]> entry : themeSquares.entrySet()) {
                float[] pos = entry.getValue();
                if (HoverUtil.isHovered(mouseX, mouseY, pos[0], pos[1], pos[2], pos[3])) {
                    ClickGuiUtil.theme = entry.getKey();
                    applyGlobalTheme(entry.getKey());
                    showThemes = false;
                    return true;
                }
            }
        }

        float rectW = 180.0F;
        float rectH = 20.0F;
        float rectX = (mc.getWindow().getScaledWidth() - rectW) / 2.0F;
        float rectY = Math.min(mc.getWindow().getScaledHeight() - 70.0F, panelTargetY[0] + panelHeight + 24.0F);
        if (HoverUtil.isHovered(mouseX, mouseY, rectX, rectY, rectW, rectH)) {
            searchFocused = !searchFocused;
            return true;
        }

        if (button == 0) {
            float centerX = mc.getWindow().getScaledWidth() / 2.0F;
            float openV = Math.max(0.0F, openAnim.getValue());
            for (int i = 0; i < panels.size(); i++) {
                Panel p = panels.get(i);
                float px = centerX + (panelTargetX[i] - centerX) * openV;
                float py = panelTargetY[i] + (1.0F - openV) * 6.0F;
                if (HoverUtil.isHovered(mouseX, mouseY, px + p.width - 24.0F, py + 3.0F, 18.0F, ClickGuiUtil.HEADER_HEIGHT - 6.0F)) {
                    p.collapsed = !p.collapsed;
                    return true;
                }
                if (HoverUtil.isHovered(mouseX, mouseY, px, py, p.width, ClickGuiUtil.HEADER_HEIGHT)) {
                    draggingPanel = i;
                    dragOffsetX = (float) mouseX - px;
                    dragOffsetY = (float) mouseY - py;
                    return true;
                }
            }
        }

        for (Panel p : panels) {
            p.onClick(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingPanel >= 0 && button == 0) {
            float scw = mc.getWindow().getScaledWidth();
            float sch = mc.getWindow().getScaledHeight();
            panelTargetX[draggingPanel] = MathHelper.clamp((float) mouseX - dragOffsetX, 0.0F, scw - panelWidth);
            panelTargetY[draggingPanel] = MathHelper.clamp((float) mouseY - dragOffsetY, 0.0F, sch - 40.0F);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingPanel = -1;
        for (Panel p : panels) {
            p.onRelease(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (Panel p : panels) {
            p.onScroll(mouseX, mouseY, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (tech.onetap.ui.clickgui.objects.sets.BindObject.hasActiveListening()) {
            for (Panel p : panels) {
                p.onKey(keyCode, scanCode, modifiers);
            }
            return true;
        }
        if (keyCode == 70 && (modifiers & 2) != 0) {
            searchFocused = !searchFocused;
            searchText = "";
            return true;
        }
        if (keyCode == 256) {
            if (searchFocused) {
                searchFocused = false;
                return true;
            }
            if (showThemes) {
                showThemes = false;
                return true;
            }
            if (!closing) {
                closing = true;
            }
            return true;
        }
        if (searchFocused) {
            if (keyCode == 259) {
                if (!searchText.isEmpty()) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                }
            } else if (keyCode == 257) {
                toggleFirstMatch();
                searchText = "";
                searchFocused = false;
            }
            return true;
        }
        for (Panel p : panels) {
            p.onKey(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void toggleFirstMatch() {
        String query = searchText.toLowerCase();
        if (query.isEmpty()) return;
        for (ModuleCategory category : ModuleCategory.values()) {
            for (Module module : tech.onetap.Onetap.getInstance().getModuleStorage().get(category)) {
                if (module.getName().toLowerCase().contains(query)) {
                    module.toggle();
                    return;
                }
            }
        }
    }

    private void applyGlobalTheme(String id) {
        tech.onetap.module.settings.impl.ThemeManager tm = tech.onetap.module.settings.impl.ThemeManager.getInstance();
        int c1;
        int c2;
        if ("13".equals(id)) {
            c1 = 0xFF2CDFB0;
            c2 = 0xFF2C6BDF;
        } else if ("14".equals(id)) {
            c1 = 0xFFFFFFFF;
            c2 = 0xFF9AA0B0;
        } else {
            c1 = ClickGuiUtil.colorFor(id);
            c2 = ColorProvider.interpolateColor(c1, 0xFF0A0C14, 0.35F);
        }
        tm.getCurrentTheme().setColors(c1, c2);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocused && searchText.length() < 20) {
            searchText = searchText + chr;
            return true;
        }
        for (Panel p : panels) {
            p.onChar(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void close() {
        super.close();
        showThemes = false;
        searchText = "";
        searchFocused = false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }
}