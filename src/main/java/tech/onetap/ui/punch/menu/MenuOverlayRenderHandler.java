package tech.onetap.ui.punch.menu;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.MinecraftClient;
import tech.onetap.event.list.EventHUD;
import tech.onetap.ui.punch.core.MenuAppearance;
import tech.onetap.ui.punch.core.MenuOverlay;
import tech.onetap.ui.punch.gui.Render2DUtil;

public final class MenuOverlayRenderHandler {
    @Subscribe
    public void onRender(EventHUD event) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft.currentScreen != null) {
            return;
        }
        int screenWidth = minecraft.getWindow().getScaledWidth();
        int screenHeight = minecraft.getWindow().getScaledHeight();
        Render2DUtil.beginFrame();
        Render2DUtil.setBackdropBlurScale(MenuAppearance.glassBlurScale());
        MenuOverlay.render(minecraft, event.getDrawContext(), screenWidth, screenHeight);
        Render2DUtil.flush();
    }
}