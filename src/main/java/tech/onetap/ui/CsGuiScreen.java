package tech.onetap.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import tech.onetap.Onetap;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.util.auth.DiscordAvatarTexture;
import tech.onetap.util.cursor.CursorManager;
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
import tech.onetap.ui.csgui.component.ModuleComponent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class CsGuiScreen extends Screen {
    private static final Identifier CS_GUI_DISCORD_AVATAR_ID = Identifier.of("mre", "images/cs_gui_discord_avatar");

    private final Animation openAnimation = new Animation(Easing.BACK_OUT, 300);
    private boolean closing;
    private final String[] categories = {"Combat", "Movement", "Visuals", "Player", "Misc"};
    private final String[] categoryIcons = {"\ue902", "\ue910", "\ue90b", "\ue904", "\ue90a"};
    private final float[] categoryAnimations = new float[categories.length];
    private final DiscordAvatarTexture discordAvatarTexture = new DiscordAvatarTexture(CS_GUI_DISCORD_AVATAR_ID);
    private final Map<ModuleCategory, List<ModuleComponent>> moduleComponents = new EnumMap<>(ModuleCategory.class);
    private final Animation modulesAnimation = new Animation(Easing.CUBIC_OUT, 220);
    private final Animation scrollBarAnimation = new Animation(Easing.CUBIC_OUT, 180);
    private int selectedCategory = 0;
    private String searchText = "";
    private boolean searchFocused;
    private float moduleScroll;
    private float moduleTargetScroll;

    public CsGuiScreen() {
        super(Text.of("CS Gui"));
        buildModuleComponents();
    }

    public void prepareForOpen() {
        closing = false;
        openAnimation.setValue(0f);
        openAnimation.reset();
        modulesAnimation.setValue(0f);
        modulesAnimation.reset();
        searchFocused = false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        openAnimation.run(closing ? 0f : 1f);
        modulesAnimation.run(1f);
        float animationValue = (float) Math.max(0.0, Math.min(1.0, openAnimation.getValue()));
        if (closing && animationValue <= 0.01f) {
            if (client != null) {
                client.setScreen(null);
            }
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        CursorManager.resetAll();

        float width = 135 * (0.85f + animationValue * 0.15f);
        float height = 265 * (0.85f + animationValue * 0.15f);
        if (client == null) {
            return;
        }

        float x = client.getWindow().getScaledWidth() / 2f - width / 2f - 160;
        float y = client.getWindow().getScaledHeight() / 2f - height / 2f - 10f;
        float contentX = x + width + 8.0f;
        float searchHeight = 22.0f;
        float contentY = y + searchHeight + 6.0f;
        float contentWidth = 300.0f;
        float contentHeight = height - searchHeight - 6.0f;
        float maxScroll = getMaxModuleScroll(contentX, contentY, contentWidth, contentHeight);
        scrollBarAnimation.run(maxScroll > 0.0f ? 1f : 0f);
        moduleTargetScroll = Math.max(0.0f, Math.min(maxScroll, moduleTargetScroll));
        moduleScroll += (moduleTargetScroll - moduleScroll) * 0.18f;
        if (Math.abs(moduleTargetScroll - moduleScroll) < 0.15f) {
            moduleScroll = moduleTargetScroll;
        }
        int blurAlpha = (int) (255 * animationValue);
        int fillAlpha = (int) (52 * animationValue);
        int borderAlpha = (int) (100 * animationValue);
        int[] syncedColumnGlowColors = ColorProvider.getOrbitalRect(
                ColorProvider.getThemeColor(),
                ColorProvider.getThemeColorTwo(),
                500.0,
                (int) (115 * animationValue)
        );
        renderColumnGlow(context, x, y, width, height, 8.0f, syncedColumnGlowColors);

        DrawUtil.drawRoundBlur(x, y, width, height, 8, ColorProvider.rgba(125, 125, 125, blurAlpha), 35f);
        DrawUtil.drawRound(x, y, width, height, 8, ColorProvider.rgba(8, 8, 10, fillAlpha));

        Builder.border()
                .size(new SizeState(width + 1f, height + 1f))
                .radius(new QuadRadiusState(8 + 0.5f))
                .color(new QuadColorState(ColorProvider.rgba(255, 255, 255, borderAlpha)))
                .thickness(0.2f)
                .smoothness(1f, 0.5f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), x - 0.5f, y - 0.5f);

        renderSearch(context, contentX, y, contentWidth, searchHeight, animationValue, mouseX, mouseY);

        renderColumnGlow(context, contentX, contentY, contentWidth, contentHeight, 8.0f, syncedColumnGlowColors);
        DrawUtil.drawRoundBlur(contentX, contentY, contentWidth, contentHeight, 8, ColorProvider.rgba(125, 125, 125, blurAlpha), 35f);
        DrawUtil.drawRound(contentX, contentY, contentWidth, contentHeight, 8, ColorProvider.rgba(8, 8, 10, fillAlpha));

        Builder.border()
                .size(new SizeState(contentWidth + 1f, contentHeight + 1f))
                .radius(new QuadRadiusState(8 + 0.5f))
                .color(new QuadColorState(ColorProvider.rgba(255, 255, 255, borderAlpha)))
                .thickness(0.2f)
                .smoothness(1f, 0.5f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), contentX - 0.5f, contentY - 0.5f);

        float innerX = x + 4.5f;
        float innerY = y + 12.0f;
        float innerWidth = width - 15.0f;
        float innerHeight = 26.0f;
        float innerSpacing = 6.0f;
        int innerFillAlpha = (int) (42 * animationValue);
        int innerBorderAlpha = (int) (85 * animationValue);
        int textAlpha = (int) (255 * animationValue);

        for (int i = 0; i < categories.length; i++) {
            float categoryY = innerY + i * (innerHeight + innerSpacing);
            categoryAnimations[i] += (((selectedCategory == i) ? 1.0f : 0.0f) - categoryAnimations[i]) * 0.18f;
            float animatedInnerX = innerX + 2.5f * categoryAnimations[i];

            int inactiveCategoryColor = ColorProvider.rgba(15, 15, 15, innerFillAlpha);
            int activeCategoryColor = ColorProvider.setAlpha(ColorProvider.brighter(ColorProvider.getThemeColor(), 1.35f), innerFillAlpha);
            int categoryColor = ColorProvider.interpolateColor(inactiveCategoryColor, activeCategoryColor, categoryAnimations[i]);

            DrawUtil.drawRound(animatedInnerX, categoryY, innerWidth, innerHeight, 5.0f, categoryColor);

            Builder.border()
                    .size(new SizeState(innerWidth + 1f, innerHeight + 1f))
                    .radius(new QuadRadiusState(5.5f))
                    .color(new QuadColorState(ColorProvider.rgba(255, 255, 255, innerBorderAlpha)))
                    .thickness(0.2f)
                    .smoothness(1f, 0.5f)
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), animatedInnerX - 0.5f, categoryY - 0.5f);

            DrawUtil.drawText(Fonts.MOONWARD_ICONS.get(), categoryIcons[i], animatedInnerX + 4.5f, categoryY + 9f, ColorProvider.rgba(255, 255, 255, textAlpha), 12);
            DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), categories[i], animatedInnerX + 18.0f, categoryY + 8.55f, ColorProvider.rgba(255, 255, 255, textAlpha), 10);
        }

        renderModules(context, mouseX, mouseY, delta, contentX, contentY, contentWidth, contentHeight);
        renderScrollBar(context, contentX, contentY, contentWidth, contentHeight, animationValue, maxScroll);
        renderProfileState(context, x, y, width, height, animationValue);

        long window = client.getWindow().getHandle();
        CursorManager.forceArrow(window);
        CursorManager.applyRequested(window);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (client != null) {
            float animationValue = (float) Math.max(0.0, Math.min(1.0, openAnimation.getValue()));
            float width = 135 * (0.85f + animationValue * 0.15f);
            float height = 265 * (0.85f + animationValue * 0.15f);
            float x = client.getWindow().getScaledWidth() / 2f - width / 2f - 160;
            float y = client.getWindow().getScaledHeight() / 2f - height / 2f - 10f;
            float contentX = x + width + 8.0f;
            float searchHeight = 22.0f;
            float contentY = y + searchHeight + 6.0f;
            float contentWidth = 300.0f;
            float contentHeight = height - searchHeight - 6.0f;

            searchFocused = tech.onetap.util.render.helper.HoverUtil.isHovered(mouseX, mouseY, contentX, y, contentWidth, searchHeight);
            if (searchFocused) {
                return true;
            }

            float innerX = x + 4.5f;
            float innerY = y + 12.0f;
            float innerWidth = width - 15.0f;
            float innerHeight = 26.0f;
            float innerSpacing = 6.0f;

            for (int i = 0; i < categories.length; i++) {
                float categoryY = innerY + i * (innerHeight + innerSpacing);
                float animatedInnerX = innerX + 2.5f * categoryAnimations[i];
                if (tech.onetap.util.render.helper.HoverUtil.isHovered(mouseX, mouseY, animatedInnerX, categoryY, innerWidth, innerHeight)) {
                    if (selectedCategory != i) {
                        selectedCategory = i;
                        moduleScroll = 0.0f;
                        moduleTargetScroll = 0.0f;
                        modulesAnimation.setValue(0f);
                        modulesAnimation.reset();
                    }
                    return true;
                }
            }

            layoutModuleComponents(contentX, contentY, contentWidth, contentHeight);
            for (ModuleComponent moduleComponent : getFilteredModuleComponents()) {
                if (moduleComponent.getY() + moduleComponent.getHeight() < contentY + 4.0f || moduleComponent.getY() > contentY + contentHeight - 8.0f) {
                    continue;
                }
                if (moduleComponent.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (ModuleComponent moduleComponent : getCurrentModuleComponents()) {
            moduleComponent.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (!searchText.isEmpty()) {
                searchText = "";
                searchFocused = false;
                moduleScroll = 0.0f;
                moduleTargetScroll = 0.0f;
                modulesAnimation.setValue(0f);
                modulesAnimation.reset();
                return true;
            }
            close();
            return true;
        }
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
                moduleScroll = 0.0f;
                moduleTargetScroll = 0.0f;
                modulesAnimation.setValue(0f);
                modulesAnimation.reset();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocused && !Character.isISOControl(chr) && searchText.length() < 24) {
            searchText += chr;
            moduleScroll = 0.0f;
            moduleTargetScroll = 0.0f;
            modulesAnimation.setValue(0f);
            modulesAnimation.reset();
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (client == null) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        float animationValue = (float) Math.max(0.0, Math.min(1.0, openAnimation.getValue()));
        float width = 135 * (0.85f + animationValue * 0.15f);
        float height = 265 * (0.85f + animationValue * 0.15f);
        float x = client.getWindow().getScaledWidth() / 2f - width / 2f - 160;
        float y = client.getWindow().getScaledHeight() / 2f - height / 2f - 10f;
        float contentX = x + width + 8.0f;
        float searchHeight = 22.0f;
        float contentY = y + searchHeight + 6.0f;
        float contentWidth = 300.0f;
        float contentHeight = height - searchHeight - 6.0f;

        if (!tech.onetap.util.render.helper.HoverUtil.isHovered(mouseX, mouseY, contentX, contentY, contentWidth, contentHeight)) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        float maxScroll = getMaxModuleScroll(contentX, contentY, contentWidth, contentHeight);
        if (maxScroll <= 0.0f) {
            moduleScroll = 0.0f;
            moduleTargetScroll = 0.0f;
            return true;
        }

        moduleTargetScroll = Math.max(0.0f, Math.min(maxScroll, moduleTargetScroll - (float) verticalAmount * 30.0f));
        return true;
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
            searchFocused = false;
            openAnimation.run(0f);
        }
    }

    @Override
    public void removed() {
        searchFocused = false;
        super.removed();
    }

    
    private void renderProfileState(DrawContext context, float x, float y, float width, float height, float animationValue) {
        discordAvatarTexture.requestLoad();
        discordAvatarTexture.updateTexture(client);

        String discordName = "Unknown";
        String uidText = "UID: 0";
        try {
            var auth = Onetap.getInstance().getAuthManager();
            if (auth != null) {
                if (auth.getDiscordUsername() != null && !auth.getDiscordUsername().isEmpty()) {
                    discordName = auth.getDiscordUsername();
                }
                uidText = "UID: " + Math.max(auth.getUid(), 0);
            }
        } catch (Throwable ignored) {
        }

        float avatarSize = 24.0f;
        float avatarX = x + 10.0f;
        float avatarY = y + height - 36.0f;
        int avatarBorderAlpha = (int) (125 * animationValue);
        int avatarAlpha = (int) (255 * animationValue);
        int nameAlpha = (int) (255 * animationValue);
        int uidAlpha = (int) (175 * animationValue);

        Builder.border()
                .size(new SizeState(avatarSize + 2.0f, avatarSize + 2.0f))
                .radius(new QuadRadiusState(avatarSize / 2.0f + 1.0f))
                .color(new QuadColorState(ColorProvider.rgba(255, 255, 255, avatarBorderAlpha)))
                .thickness(0.45f)
                .smoothness(1f, 0.5f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), avatarX - 1.0f, avatarY - 1.0f);

        Builder.texture()
                .size(new SizeState(avatarSize, avatarSize))
                .radius(new QuadRadiusState(avatarSize / 2.0f))
                .color(new QuadColorState(ColorProvider.rgba(255, 255, 255, avatarAlpha)))
                .texture(0f, 0f, 1f, 1f, resolveProfileTextureId())
                .smoothness(1f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), avatarX, avatarY, 0f);

        float textX = avatarX + avatarSize + 5.0f;
        String displayName = trimToFit(discordName, width - 56.0f, 7.5f);

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), displayName, textX, avatarY + 1.5f, ColorProvider.rgba(255, 255, 255, nameAlpha), 8.5f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), uidText, textX, avatarY + 13.2f, ColorProvider.rgba(185, 185, 185, uidAlpha), 7.2f);
    }

    private int resolveProfileTextureId() {
        if (client == null) {
            return 0;
        }
        return discordAvatarTexture.getTextureId(() -> client.getTextureManager().getTexture(DefaultSkinHelper.getSteve().texture()).getGlId());
    }

    private void buildModuleComponents() {
        if (Onetap.getInstance() == null || Onetap.getInstance().getModuleStorage() == null) {
            return;
        }

        moduleComponents.clear();
        for (ModuleCategory category : ModuleCategory.values()) {
            List<ModuleComponent> components = new ArrayList<>();
            for (Module module : Onetap.getInstance().getModuleStorage().get(category)) {
                components.add(new ModuleComponent(module));
            }
            moduleComponents.put(category, components);
        }
    }

    private void renderModules(DrawContext context, int mouseX, int mouseY, float delta, float x, float y, float width, float height) {
        float modulesAlpha = (float) Math.max(0.0, Math.min(1.0, modulesAnimation.getValue()));
        float maxY = y + height - 8.0f;
        List<ModuleComponent> components = layoutModuleComponents(x, y, width, height);

        Scissor.push();
        Scissor.setFromComponentCoordinates(x + 4.0f, y + 4.0f, width - 8.0f, height - 8.0f);

        for (ModuleComponent component : components) {
            component.setAlpha((float) Math.max(0.0, Math.min(1.0, openAnimation.getValue())) * modulesAlpha);

            if (component.getY() + component.getHeight() >= y + 4.0f && component.getY() <= maxY) {
                component.render(context, mouseX, mouseY, delta);
            }
        }

        Scissor.unset();
        Scissor.pop();
    }

    private List<ModuleComponent> layoutModuleComponents(float x, float y, float width, float height) {
        List<ModuleComponent> components = getFilteredModuleComponents();
        float componentX = x + 8.0f;
        float componentY = y + 8.0f;
        float spacing = 6.0f;
        float componentWidth = (width - 16.0f - spacing) / 2.0f;
        float modulesAlpha = (float) Math.max(0.0, Math.min(1.0, modulesAnimation.getValue()));
        float modulesSlide = (1.0f - modulesAlpha) * 8.0f;
        float maxScroll = getMaxModuleScroll(x, y, width, height);
        if (moduleTargetScroll > maxScroll) {
            moduleTargetScroll = maxScroll;
        }
        if (moduleScroll > maxScroll) {
            moduleScroll = maxScroll;
        }
        float[] columnHeights = {componentY + modulesSlide - moduleScroll, componentY + modulesSlide - moduleScroll};

        for (int i = 0; i < components.size(); i++) {
            ModuleComponent component = components.get(i);
            int column = i % 2;
            float renderX = componentX + column * (componentWidth + spacing);
            float renderY = columnHeights[column];
            float componentHeight = component.getHeight();

            component.setBounds(renderX, renderY, componentWidth, componentHeight);
            columnHeights[column] = renderY + component.getHeight() + spacing;
        }

        return components;
    }

    private List<ModuleComponent> getCurrentModuleComponents() {
        return moduleComponents.getOrDefault(getSelectedModuleCategory(), List.of());
    }

    private List<ModuleComponent> getFilteredModuleComponents() {
        if (searchText.isBlank()) {
            return getCurrentModuleComponents();
        }

        String query = searchText.toLowerCase();
        List<ModuleComponent> filtered = new ArrayList<>();
        for (List<ModuleComponent> categoryComponents : moduleComponents.values()) {
            for (ModuleComponent component : categoryComponents) {
                if (component.getModule().getName().toLowerCase().contains(query)) {
                    filtered.add(component);
                }
            }
        }
        return filtered;
    }

    private ModuleCategory getSelectedModuleCategory() {
        return switch (selectedCategory) {
            case 0 -> ModuleCategory.COMBAT;
            case 1 -> ModuleCategory.MOVEMENT;
            case 2 -> ModuleCategory.RENDER;
            case 3 -> ModuleCategory.PLAYER;
            default -> ModuleCategory.MISC;
        };
    }

    private float getMaxModuleScroll(float x, float y, float width, float height) {
        List<ModuleComponent> components = getFilteredModuleComponents();
        float componentY = y + 8.0f;
        float spacing = 6.0f;
        float[] columnHeights = {componentY, componentY};

        for (int i = 0; i < components.size(); i++) {
            ModuleComponent component = components.get(i);
            int column = i % 2;
            columnHeights[column] += component.getHeight() + spacing;
        }

        float contentBottom = Math.max(columnHeights[0], columnHeights[1]) - spacing;
        float visibleBottom = y + height - 8.0f;
        return Math.max(0.0f, contentBottom - visibleBottom);
    }

    private void renderScrollBar(DrawContext context, float x, float y, float width, float height, float animationValue, float maxScroll) {
        float scrollBarValue = (float) scrollBarAnimation.getValue();
        if (scrollBarValue <= 0.01f) {
            return;
        }

        float trackX = x + width + 6.0f;
        float trackY = y + 8.0f;
        float trackWidth = 2.5f;
        float trackHeight = height - 16.0f;
        float thumbHeight = Math.max(18.0f, trackHeight * (trackHeight / (trackHeight + maxScroll)));
        float scrollProgress = maxScroll <= 0.0f ? 0.0f : moduleScroll / maxScroll;
        float thumbY = trackY + (trackHeight - thumbHeight) * scrollProgress;

        DrawUtil.drawRound(trackX, trackY, trackWidth, trackHeight, 1.25f, ColorProvider.rgba(255, 255, 255, (int) (28 * animationValue * scrollBarValue)));
        DrawUtil.drawRound(trackX - 0.25f, thumbY, trackWidth + 0.5f, thumbHeight, 1.25f, ColorProvider.rgba(255, 255, 255, (int) (130 * animationValue * scrollBarValue)));
    }

    private String trimToFit(String text, float maxWidth, float fontSize) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (Fonts.SFMEDIUM.get().getWidth(text, fontSize) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        while (!text.isEmpty() && Fonts.SFMEDIUM.get().getWidth(text + ellipsis, fontSize) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }

    private void renderColumnGlow(DrawContext context, float x, float y, float width, float height, float radius, int[] glowColors) {
        float glow = 8.0f;

        Builder.glow()
                .size(new SizeState(width + glow * 2f - 6f, height + glow * 2f - 6f))
                .radius(new QuadRadiusState(radius))
                .color(new QuadColorState(glowColors[0], glowColors[1], glowColors[2], glowColors[3]))
                .glowRadius(glow)
                .softness(0f)
                .intensity(2.0f)
                .additive(true)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), x - glow + 3f, y - glow + 3f, 0f);
    }

    private void renderSearch(DrawContext context, float x, float y, float width, float height, float animationValue, int mouseX, int mouseY) {
        int blurAlpha = (int) (255 * animationValue);
        int fillAlpha = (int) (42 * animationValue);
        int borderAlpha = (int) (85 * animationValue);
        int textAlpha = (int) (255 * animationValue);
        boolean hovered = tech.onetap.util.render.helper.HoverUtil.isHovered(mouseX, mouseY, x, y, width, height);
        if (hovered) {
            CursorManager.requestHand();
        }

        int[] glowColors = ColorProvider.getOrbitalRect(
                ColorProvider.getThemeColor(),
                ColorProvider.getThemeColorTwo(),
                500.0,
                (int) (110 * animationValue)
        );
        float glow = 6.0f;
        Builder.glow()
                .size(new SizeState(width + glow * 2f - 4f, height + glow * 2f - 4f))
                .radius(new QuadRadiusState(5.0f))
                .color(new QuadColorState(glowColors[0], glowColors[1], glowColors[2], glowColors[3]))
                .glowRadius(glow)
                .softness(0f)
                .intensity(1.85f)
                .additive(true)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), x - glow + 2f, y - glow + 2f, 0f);

        DrawUtil.drawRoundBlur(x, y, width, height, 5.0f, ColorProvider.rgba(125, 125, 125, blurAlpha), 35f);
        DrawUtil.drawRound(x, y, width, height, 5.0f, ColorProvider.rgba(15, 15, 15, fillAlpha));
        Builder.border()
                .size(new SizeState(width + 1f, height + 1f))
                .radius(new QuadRadiusState(5.5f))
                .color(new QuadColorState(ColorProvider.rgba(255, 255, 255, borderAlpha)))
                .thickness(0.2f)
                .smoothness(1f, 0.5f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), x - 0.5f, y - 0.5f);

        String displayText = searchText.isEmpty() && !searchFocused ? "Поиск..." : searchText;
        int displayColor = searchText.isEmpty() && !searchFocused
                ? ColorProvider.rgba(160, 160, 160, (int) (180 * animationValue))
                : ColorProvider.rgba(255, 255, 255, textAlpha);
        String cursor = searchFocused && System.currentTimeMillis() % 1000 > 500 ? "_" : "";
        float iconX = x + 8.0f;
        float iconY = y + 6.0f;
        context.getMatrices().push();
        context.getMatrices().translate(iconX + 8.0f, 0.0f, 0.0f);
        context.getMatrices().scale(-1.0f, 1.0f, 1.0f);
        DrawUtil.drawText(Fonts.LUPA.get(), "\uF002", context.getMatrices().peek().getPositionMatrix(), 0, iconY, ColorProvider.rgba(255, 255, 255, textAlpha), 9.0f);
        context.getMatrices().pop();
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), displayText + (searchFocused ? cursor : ""), x + 20.0f, y + 6.0f, displayColor, 9.0f);
    }
}
