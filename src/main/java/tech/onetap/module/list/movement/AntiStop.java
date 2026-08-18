package tech.onetap.module.list.movement;

import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.SliderSetting;

@ModuleInformation(moduleName = "AntiStop", moduleCategory = ModuleCategory.MOVEMENT)
public class AntiStop extends Module {
    private final BooleanSetting bpsLimit = new BooleanSetting("BPS лимит", true);
    private final SliderSetting maxBPS = new SliderSetting("Макс BPS", 30f, 1f, 100f, 0.5f);
    private final BooleanSetting includeVertical = new BooleanSetting("Вертикаль", true);
    private final SliderSetting smoothness = new SliderSetting("Сглаживание", 0.95f, 0.5f, 1f, 0.01f);

    public boolean isBpsLimitEnabled() {
        return bpsLimit.getValue();
    }

    public double getMaxBPS() {
        return maxBPS.getValue();
    }

    public boolean isIncludeVertical() {
        return includeVertical.getValue();
    }

    public double getSmoothnessMultiplier() {
        return smoothness.getValue();
    }
}