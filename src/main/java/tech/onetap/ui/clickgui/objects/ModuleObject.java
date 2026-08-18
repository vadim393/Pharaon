package tech.onetap.ui.clickgui.objects;

import tech.onetap.module.Module;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.BindSetting;
import tech.onetap.module.settings.ColorSetting;
import tech.onetap.module.settings.ModeListSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.Setting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.module.settings.StringSetting;
import tech.onetap.module.settings.ThemeSetting;
import tech.onetap.ui.clickgui.ClickGuiFrame;
import tech.onetap.ui.clickgui.ClickGuiUtil;
import tech.onetap.ui.clickgui.objects.sets.BindObject;
import tech.onetap.ui.clickgui.objects.sets.BooleanObject;
import tech.onetap.ui.clickgui.objects.sets.ColorObject;
import tech.onetap.ui.clickgui.objects.sets.ModeObject;
import tech.onetap.ui.clickgui.objects.sets.MultiObject;
import tech.onetap.ui.clickgui.objects.sets.SliderObject;
import tech.onetap.ui.clickgui.objects.sets.StringObject;
import tech.onetap.ui.clickgui.objects.sets.ThemObject;
import tech.onetap.util.render.helper.HoverUtil;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.msdf.MsdfFont;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.util.ArrayList;

public class ModuleObject extends Object {
    public Module module;
    public ArrayList<Object> object = new ArrayList<>();
    public float hover_anim;
    public float arrow_anim;
    public boolean expanded = false;
    public Runnable openSettingsAction;

    public ModuleObject(Module module) {
        this.module = module;

        this.object.add(new BindObject(this));

        for (Setting setting : module.getSettings()) {
            if (setting instanceof BooleanSetting option) {
                this.object.add(new BooleanObject(this, option));
            }
            if (setting instanceof BindSetting option) {
                this.object.add(new BindObject(this, option));
            }
            if (setting instanceof ColorSetting option) {
                this.object.add(new ColorObject(this, option));
            }
            if (setting instanceof SliderSetting option) {
                this.object.add(new SliderObject(this, option));
            }
            if (setting instanceof ModeSetting option) {
                this.object.add(new ModeObject(this, option));
            }
            if (setting instanceof ThemeSetting option) {
                this.object.add(new ThemObject(this, option));
            }
            if (setting instanceof ModeListSetting option) {
                this.object.add(new MultiObject(this, option));
            }
            if (setting instanceof StringSetting option) {
                this.object.add(new StringObject(this, option));
            }
        }
    }

    public boolean isHoveredRow(int mouseX, int mouseY) {
        return HoverUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        super.draw(mouseX, mouseY);
        this.hover_anim = ClickGuiUtil.fast(hover_anim, isHoveredRow(mouseX, mouseY) ? 1.0F : 0.0F, 12.0F);

        int alpha = (int) (255.0F * hover_anim);
        if (hover_anim > 0.01F) {
            DrawUtil.drawRound(this.x + 2.0F, this.y + 0.5F, this.width - 4.0F, this.height - 1.0F, ClickGuiUtil.RADIUS_ROW,
                    ColorProvider.setAlpha(ClickGuiUtil.hoverBackground(), alpha));
        }

        float annim = module.getAnimation().getValue();
        int nameColor = ColorProvider.interpolateColor(ClickGuiUtil.textDisabled(), ClickGuiUtil.textColor(), annim);
        if (annim > 0.05F) {
            DrawUtil.drawRound(this.x + 0.5F, this.y + 4.0F, 2.0F, this.height - 8.0F, 1.0F,
                    ColorProvider.setAlpha(ClickGuiUtil.accent(), (int) (150.0F * annim)));
        }

        String name = module.getName();
        if (hover_anim > 0.4F) {
            nameColor = ColorProvider.brighter(nameColor, 1.08F);
        }

        float nameX = this.x + 10.0F;
        String query = ClickGuiFrame.searchText;
        if (!query.isEmpty()) {
            int idx = name.toLowerCase().indexOf(query.toLowerCase());
            if (idx >= 0) {
                MsdfFont font = Fonts.SFREGULAR.get();
                float prefixW = font.getWidth(name.substring(0, idx), ClickGuiUtil.NC14);
                float matchW = font.getWidth(name.substring(idx, idx + query.length()), ClickGuiUtil.NC14);
                DrawUtil.drawRound(nameX + prefixW - 1.0F, this.y + (this.height - 13.0F) / 2.0F, matchW + 2.0F, 13.0F, 3.0F,
                        ColorProvider.setAlpha(ClickGuiUtil.accent(), (int) (80.0F * Math.max(0.1F, hover_anim))));
            }
        }

        DrawUtil.drawText(Fonts.SFREGULAR.get(), name, nameX, this.y + (this.height - ClickGuiUtil.NC14) / 2.0F, nameColor, ClickGuiUtil.NC14);

        if (!object.isEmpty()) {
            float arrowX = this.x + this.width - 14.0F;
            boolean arrowHover = HoverUtil.isHovered(mouseX, mouseY, arrowX - 1.0F, this.y, 13.0F, this.height);
            int arrowColor = (arrowHover || expanded) ? ClickGuiUtil.accent() : ClickGuiUtil.textSecondary();
            this.arrow_anim = ClickGuiUtil.fast(this.arrow_anim, expanded ? 1.0F : 0.0F, 10.0F);
            float centerX = arrowX + ClickGuiUtil.NC14 / 2.0F;
            float centerY = this.y + (this.height - ClickGuiUtil.NC14) / 2.0F + ClickGuiUtil.NC14 / 2.0F;
            org.joml.Matrix4f matrix = new org.joml.Matrix4f();
            matrix.translate(centerX, centerY, 0.0F);
            matrix.rotateZ((float) Math.toRadians(90.0F * this.arrow_anim));
            matrix.translate(-centerX, -centerY, 0.0F);
            DrawUtil.drawText(Fonts.SFREGULAR.get(), ">", matrix, arrowX,
                    this.y + (this.height - ClickGuiUtil.NC14) / 2.0F, arrowColor, ClickGuiUtil.NC14);
        }
    }

    public boolean isHoveredSettingsArrow(int mouseX, int mouseY) {
        if (object.isEmpty()) return false;
        float arrowX = this.x + this.width - 14.0F;
        return HoverUtil.isHovered(mouseX, mouseY, arrowX - 1.0F, this.y, 13.0F, this.height);
    }

    public void openSettings() {
        if (openSettingsAction != null) {
            openSettingsAction.run();
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!isHoveredRow(mouseX, mouseY)) return;
        if (mouseButton == 0) {
            if (isHoveredSettingsArrow(mouseX, mouseY) && !object.isEmpty()) {
                openSettings();
                return;
            }
            module.toggle();
        }
    }

    public void settingsClick(int mouseX, int mouseY, int mouseButton) {
        for (Object object1 : object) {
            object1.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    public void settingsRelease(int mouseX, int mouseY, int mouseButton) {
        for (Object object1 : object) {
            object1.mouseReleased(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        for (Object object1 : object) {
            object1.mouseReleased(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void keyTyped(int keyCode, int scanCode, int modifiers) {
    }

    public void settingsKey(int keyCode, int scanCode, int modifiers) {
        for (Object obj : object) {
            obj.keyTyped(keyCode, scanCode, modifiers);
        }
    }

    public void settingsChar(char codePoint, int modifiers) {
        for (Object obj : object) {
            obj.charTyped(codePoint, modifiers);
        }
    }

    @Override
    public void exit() {
        for (Object object1 : object) {
            object1.exit();
        }
    }
}