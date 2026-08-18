package tech.onetap.ui.punch.feature;

import com.google.common.eventbus.Subscribe;
import tech.onetap.Onetap;
import tech.onetap.event.list.EventKeyInput;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleStorage;
import tech.onetap.util.config.ConfigManager;
import tech.onetap.ui.punch.feature.setting.BindSetting;

import java.util.ArrayList;
import java.util.List;

public final class FeatureManager {
    public static final FeatureManager INSTANCE = new FeatureManager();

    private final ModuleStorage storage;
    private final List<Feature> features = new ArrayList<>();
    private boolean initialized;

    private FeatureManager() {
        this.storage = Onetap.getInstance().getModuleStorage();
    }

    public static void init() {
        INSTANCE.initialize();
    }

    private void initialize() {
        if (this.initialized) {
            return;
        }
        this.initialized = true;
        for (Module module : this.storage.getModules()) {
            this.features.add(new Feature(module));
        }
        Onetap.getInstance().getEventBus().register(this);
    }

    public List<Feature> getFeatures() {
        ensureInitialized();
        return new ArrayList<>(this.features);
    }

    public List<Feature> getFeatures(FeatureCategory category) {
        ensureInitialized();
        ModuleCategory moduleCategory = toModuleCategory(category);
        List<Feature> result = new ArrayList<>();
        for (Feature feature : this.features) {
            if (feature.module().getCategory() == moduleCategory) {
                result.add(feature);
            }
        }
        return result;
    }

    public List<String> listNamedConfigs() {
        return ConfigManager.getConfigs();
    }

    public void loadNamedConfig(String name) {
        ConfigManager.load(stripExtension(normalizeName(name)));
    }

    public void saveNamedConfig(String name) {
        ConfigManager.save(stripExtension(normalizeName(name)));
    }

    public void deleteNamedConfig(String name) {
        ConfigManager.delete(stripExtension(normalizeName(name)));
    }

    @Subscribe
    public void onKey(EventKeyInput event) {
        if (tech.onetap.ui.punch.core.MenuOverlay.isOpen()) {
            return;
        }
        int pressed = event.getKey() >= 0 && event.getKey() < 8 ? ~event.getKey() : event.getKey();
        if (BindSetting.isForbiddenMouse(pressed)) {
            return;
        }
        for (Feature feature : this.features) {
            if (!feature.isToggleable() || feature.getBind().isEmpty()) {
                continue;
            }
            int index = feature.getBind().indexOfCode(pressed);
            if (index < 0 || !feature.isBindVisibleAt(index)) {
                continue;
            }
            if (feature.module().getKey() == pressed) {
                continue;
            }
            switch (feature.getBindModeAt(index)) {
                case TOGGLE -> {
                    if (event.getAction() == 1) {
                        feature.toggle();
                    }
                }
                case HOLD -> feature.setEnabled(event.getAction() == 1);
            }
        }
    }

    private static ModuleCategory toModuleCategory(FeatureCategory category) {
        return switch (category) {
            case COMBAT -> ModuleCategory.COMBAT;
            case MOVEMENT -> ModuleCategory.MOVEMENT;
            case VISUAL -> ModuleCategory.RENDER;
            case PLAYER -> ModuleCategory.PLAYER;
            case MISC -> ModuleCategory.MISC;
        };
    }

    private void ensureInitialized() {
        if (!this.initialized) {
            initialize();
        }
    }

    private static String normalizeName(String value) {
        return ConfigManager.normalizeName(value);
    }

    private static String stripExtension(String name) {
        return name.endsWith(".json") ? name.substring(0, name.length() - ".json".length()) : name;
    }
}