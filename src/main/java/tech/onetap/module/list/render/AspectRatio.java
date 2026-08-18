package tech.onetap.module.list.render;

import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.SliderSetting;

@ModuleInformation(moduleName = "AspectRatio", moduleDesc = "Растягивает изображение", moduleCategory = ModuleCategory.RENDER)
public class AspectRatio extends Module {
    private final SliderSetting stretch = new SliderSetting("Растяжение", 1.0f, 0.1f, 2f, 0.01f);

    public float getAspectRatio() {
        int width = mc.getWindow().getFramebufferWidth();
        int height = mc.getWindow().getFramebufferHeight();

        if (width <= 0 || height <= 0) {
            return stretch.getFloatValue();
        }

        return (width / (float) height) * stretch.getFloatValue();
    }
}
