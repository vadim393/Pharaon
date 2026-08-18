package tech.onetap.module.list.render;

import org.lwjgl.glfw.GLFW;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.ui.CsGuiScreen;

@ModuleInformation(moduleName = "CS Gui", moduleCategory = ModuleCategory.RENDER, moduleKeybind = GLFW.GLFW_KEY_LEFT_SHIFT)
public class CsGui extends Module {
    private CsGuiScreen csGuiScreen;

    @Override
    public void onEnable() {
        try {
            if (csGuiScreen == null) {
                csGuiScreen = new CsGuiScreen();
            }
            csGuiScreen.prepareForOpen();
            mc.setScreen(csGuiScreen);
            logDirect("CS Gui opened");
        } catch (Throwable throwable) {
            System.err.println("[Pharaon] Failed to open CS Gui.");
            throwable.printStackTrace();
            logDirect("CS Gui error: " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
        toggle();
    }
}