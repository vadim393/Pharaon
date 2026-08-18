package tech.onetap.module.list.render;

import org.lwjgl.glfw.GLFW;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.ui.clickgui.ClickGuiFrame;

@ModuleInformation(moduleName = "Click Gui", moduleCategory = ModuleCategory.RENDER, moduleKeybind = GLFW.GLFW_KEY_RIGHT_SHIFT)
public class ClickGui extends Module {
    private ClickGuiFrame clickGuiFrame;

    @Override
    public void onEnable() {
        try {
            if (clickGuiFrame == null) {
                clickGuiFrame = new ClickGuiFrame();
            }
            clickGuiFrame.prepareForOpen();
            mc.setScreen(clickGuiFrame);
            logDirect("ClickGui opened");
        } catch (Throwable throwable) {
            System.err.println("[Pharaon] Failed to open ClickGui.");
            throwable.printStackTrace();
            logDirect("ClickGui error: " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
        toggle();
    }
}
