package tech.onetap.ui.clickgui;

import net.minecraft.util.math.MathHelper;
import tech.onetap.Onetap;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.ui.clickgui.objects.ModuleObject;
import tech.onetap.ui.clickgui.objects.Object;
import tech.onetap.util.IMinecraft;
import tech.onetap.util.render.helper.HoverUtil;
import tech.onetap.util.render.math.Scissor;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Panel implements IMinecraft {
    ModuleCategory category;
    float x;
    float y;
    float width;
    float height;
    float modulesScroll;
    boolean collapsed;
    private final List<ModuleObject> moduleObjects = new ArrayList<>();
    private String lastSearchText = "";

    public Panel(ModuleCategory category, float x, float y, float width, float height) {
        this.category = category;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        List<Module> modules = new ArrayList<>(Onetap.getInstance().getModuleStorage().get(category));
        modules.sort(Comparator.comparing(Module::getName));
        for (Module module : modules) {
            ModuleObject mo = new ModuleObject(module);
            mo.openSettingsAction = () -> {
                if (!mo.object.isEmpty()) {
                    mo.expanded = !mo.expanded;
                }
            };
            this.moduleObjects.add(mo);
        }
    }

    private String getTypeName() {
        return switch (category) {
            case COMBAT -> "Combat";
            case MOVEMENT -> "Movement";
            case RENDER -> "Render";
            case PLAYER -> "Player";
            case MISC -> "Misc";
        };
    }

    private String getIconForType() {
        return switch (category) {
            case COMBAT -> "v";
            case MOVEMENT -> "w";
            case RENDER -> "y";
            case PLAYER -> "z";
            case MISC -> "x";
            default -> "e";
        };
    }

    private boolean searchMatch(ModuleObject m) {
        String query = ClickGuiFrame.searchText.trim().toLowerCase();
        if (query.isEmpty()) return true;
        String name = m.module.getName().toLowerCase();
        String desc = m.module.getDesc() != null ? m.module.getDesc().toLowerCase() : "";
        return name.contains(query) || name.replace(" ", "").contains(query) || desc.contains(query);
    }

    private List<ModuleObject> visibleModules() {
        List<ModuleObject> list = new ArrayList<>();
        for (ModuleObject m : moduleObjects) {
            if (searchMatch(m)) list.add(m);
        }
        return list;
    }

    private float settingsHeight(ModuleObject m) {
        float h = 0.0F;
        for (Object o : m.object) {
            if (o.setting != null && !o.setting.visible.get()) continue;
            h += o.height;
        }
        return h;
    }

    public void render(int mouseX, int mouseY) {
        DrawUtil.drawRoundBlur(this.x - 5.0F, this.y - 5.0F, this.width + 10.0F, this.height + 10.0F,
                ClickGuiUtil.RADIUS_PANEL, ClickGuiUtil.glow(), 16.0F);
        DrawUtil.drawRoundBlur(this.x, this.y, this.width, this.height, ClickGuiUtil.RADIUS_PANEL, ClickGuiUtil.backgroundSoft(), 5.0F);
        DrawUtil.drawRound(this.x, this.y, this.width, this.height, ClickGuiUtil.RADIUS_PANEL, ClickGuiUtil.background());

        String title = getTypeName();
        DrawUtil.drawText(Fonts.SFBOLD.get(), title, this.x + 11.0F, this.y + (ClickGuiUtil.HEADER_HEIGHT - 10.0F) / 2.0F, ClickGuiUtil.textColor(), 10.0F);

        String icon = collapsed ? "m" : getIconForType();
        int iconColor = collapsed ? ClickGuiUtil.textMuted() : ClickGuiUtil.textMuted();
        if (HoverUtil.isHovered(mouseX, mouseY, this.x + this.width - 24.0F, this.y + 3.0F, 18.0F, ClickGuiUtil.HEADER_HEIGHT - 6.0F)) {
            iconColor = ClickGuiUtil.textColor();
        }
        DrawUtil.drawText(Fonts.ICONS.get(), icon, this.x + this.width - 24.0F, this.y + (ClickGuiUtil.HEADER_HEIGHT - 9.0F) / 2.0F + 1.0F, iconColor, 9.0F);

        DrawUtil.drawRound(this.x + 10.0F, this.y + ClickGuiUtil.HEADER_HEIGHT + 2.0F, this.width - 20.0F, 1.0F, 0.5F, ClickGuiUtil.separator());

        if (collapsed) return;

        float contentY = this.y + ClickGuiUtil.HEADER_HEIGHT + 5.0F;
        float contentH = this.height - (ClickGuiUtil.HEADER_HEIGHT + 7.0F);

        Scissor.push();
        Scissor.setFromComponentCoordinates(this.x + 1.0F, contentY, this.width - 2.0F, contentH);
        renderModules(mouseX, mouseY, contentY);
        Scissor.pop();
    }

    private void renderModules(int mouseX, int mouseY, float contentY) {
        float rowH = ClickGuiUtil.ROW_HEIGHT;
        float body = this.height - (ClickGuiUtil.HEADER_HEIGHT + 7.0F);
        List<ModuleObject> visible = visibleModules();

        if (!lastSearchText.equals(ClickGuiFrame.searchText)) {
            modulesScroll = 0.0F;
            lastSearchText = ClickGuiFrame.searchText;
        }

        float contentHeight = 0.0F;
        for (ModuleObject m : visible) {
            contentHeight += rowH;
            if (m.expanded) contentHeight += settingsHeight(m);
        }
        float maxScroll = Math.max(0.0F, contentHeight - body);
        modulesScroll = MathHelper.clamp(modulesScroll, -maxScroll, 0.0F);

        float cursor = contentY + modulesScroll;
        boolean anyHover = false;
        for (ModuleObject m : visible) {
            m.x = this.x + 2.0F;
            m.y = cursor;
            m.width = this.width - 4.0F;
            m.height = rowH;

            if (cursor + rowH >= contentY - 2.0F && cursor <= contentY + body + 2.0F) {
                m.draw(mouseX, mouseY);
                if (m.isHoveredRow(mouseX, mouseY)) {
                    anyHover = true;
                    ClickGuiFrame.hoveredDesc = m.module.getDesc();
                }
            }
            cursor += rowH;

            if (m.expanded) {
                cursor += renderSettings(m, mouseX, mouseY, cursor);
            }
        }
        if (!anyHover) {
            ClickGuiFrame.hoveredDesc = "";
        }
    }

    private float renderSettings(ModuleObject m, int mouseX, int mouseY, float y) {
        float cursor = y;
        float body = this.height - (ClickGuiUtil.HEADER_HEIGHT + 7.0F);
        float contentY = this.y + ClickGuiUtil.HEADER_HEIGHT + 5.0F;
        boolean anyHover = false;
        for (Object o : m.object) {
            if (o.setting != null && !o.setting.visible.get()) continue;
            o.x = this.x + 1.0F + 6.0F;
            o.y = cursor;
            o.width = this.width - 2.0F - 6.0F;
            if (cursor + o.height >= contentY - 2.0F && cursor <= contentY + body + 2.0F) {
                o.draw(mouseX, mouseY);
                if (o.isHovered(mouseX, mouseY) && o.setting != null && o.setting.getDesc() != null && !o.setting.getDesc().isEmpty()) {
                    anyHover = true;
                    ClickGuiFrame.hoveredDesc = o.setting.getDesc();
                }
            }
            cursor += o.height;
        }
        if (!anyHover) {
            ClickGuiFrame.hoveredDesc = "";
        }
        return cursor - y;
    }

    public void onClick(double mouseX, double mouseY, int button) {
        if (!HoverUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height)) return;

        float rowH = ClickGuiUtil.ROW_HEIGHT;
        List<ModuleObject> visible = visibleModules();

        float cursor = this.y + ClickGuiUtil.HEADER_HEIGHT + 5.0F + modulesScroll;
        for (ModuleObject m : visible) {
            m.x = this.x + 2.0F;
            m.y = cursor;
            m.width = this.width - 4.0F;
            m.height = rowH;

            if (m.isHoveredRow((int) mouseX, (int) mouseY)) {
                if (button == 1) {
                    if (!m.object.isEmpty()) {
                        m.expanded = !m.expanded;
                    }
                    return;
                }
                m.mouseClicked((int) mouseX, (int) mouseY, button);
                return;
            }
            cursor += rowH;

            if (m.expanded) {
                for (Object o : m.object) {
                    if (o.setting != null && !o.setting.visible.get()) continue;
                    o.x = this.x + 1.0F + 6.0F;
                    o.y = cursor;
                    o.width = this.width - 2.0F - 6.0F;
                    if (o.isHovered((int) mouseX, (int) mouseY)) {
                        o.mouseClicked((int) mouseX, (int) mouseY, button);
                        return;
                    }
                    cursor += o.height;
                }
            }
        }
    }

    public void onRelease(double mouseX, double mouseY, int button) {
        float rowH = ClickGuiUtil.ROW_HEIGHT;
        List<ModuleObject> visible = visibleModules();

        float cursor = this.y + ClickGuiUtil.HEADER_HEIGHT + 5.0F + modulesScroll;
        for (ModuleObject m : visible) {
            m.x = this.x + 2.0F;
            m.y = cursor;
            m.width = this.width - 4.0F;
            m.height = rowH;

            if (m.isHoveredRow((int) mouseX, (int) mouseY)) {
                m.mouseReleased((int) mouseX, (int) mouseY, button);
                return;
            }
            cursor += rowH;

            if (m.expanded) {
                for (Object o : m.object) {
                    if (o.setting != null && !o.setting.visible.get()) continue;
                    o.x = this.x + 1.0F + 6.0F;
                    o.y = cursor;
                    o.width = this.width - 2.0F - 6.0F;
                    if (o.isHovered((int) mouseX, (int) mouseY)) {
                        o.mouseReleased((int) mouseX, (int) mouseY, button);
                        return;
                    }
                    cursor += o.height;
                }
            }
        }
    }

    public void onScroll(double mouseX, double mouseY, double delta) {
        if (!HoverUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height)) return;
        modulesScroll += delta * 16.0F;
    }

    public void onKey(int keyCode, int scanCode, int modifiers) {
        for (ModuleObject m : moduleObjects) {
            if (m.expanded) {
                m.settingsKey(keyCode, scanCode, modifiers);
            }
        }
    }

    public void onChar(char chr, int modifiers) {
        for (ModuleObject m : moduleObjects) {
            if (m.expanded) {
                m.settingsChar(chr, modifiers);
            }
        }
    }
}
