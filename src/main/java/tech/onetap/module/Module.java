package tech.onetap.module;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import tech.onetap.Onetap;
import tech.onetap.module.list.render.ClientSounds;
import tech.onetap.module.list.render.hud.Interface;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.Setting;
import tech.onetap.util.IMinecraft;
import tech.onetap.util.QuickLogger;
import tech.onetap.util.base.Instance;
import tech.onetap.util.render.math.Animation;
import tech.onetap.util.render.math.Easing;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Module implements IMinecraft, QuickLogger {
    private final String name, desc;
    private final ModuleCategory category;
    private int key;
    private boolean enabled;
    private boolean guiExpanded;
    private final Animation animation = new Animation(Easing.BACK_OUT, 450);

    public final MinecraftClient mc = MinecraftClient.getInstance();

    private final List<Setting> settings = new ArrayList<>();
    private boolean settingsInitialized;

    public Module() {
        ModuleInformation information = getClass().getAnnotation(ModuleInformation.class);

        this.name = information.moduleName();
        this.desc = information.moduleDesc();
        this.category = information.moduleCategory();
        this.key = information.moduleKeybind();
    }

    public List<Setting> getSettings() {
        if (settingsInitialized) {
            return settings;
        }

        settings.clear();
        Class<?> current = this.getClass();
        while (current != null && current != Module.class) {
            for (java.lang.reflect.Field field : current.getDeclaredFields()) {
                if (Setting.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        Object obj = field.get(this);
                        if (obj instanceof Setting setting) {
                            settings.add(setting);
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            current = current.getSuperclass();
        }
        settingsInitialized = true;
        return settings;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                onEnable();
            } else {
                onDisable();
            }
            Interface.NotificationManager.post(this.name, enabled);
        }
        if (name.equals("ClickGui")) return;
        ClientSounds soundsModule = Instance.get(ClientSounds.class);
        if (soundsModule != null && (soundsModule.isEnabled() || this instanceof ClientSounds)) {
            ClientSounds.play(this.isEnabled());
        }
    }

    protected ModeSetting modeCreate() {
        return new ModeSetting("Мод", "Vanilla", "Vanilla", "Grim", "Polar");
    }

    public void onEnable() {
        Onetap.getInstance().getEventBus().register(this);
    }

    public void onDisable() {
        Onetap.getInstance().getEventBus().unregister(this);
    }

    public void toggle() {
        setEnabled(!isEnabled());
    }
}
