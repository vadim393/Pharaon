package tech.onetap.ui.punch.menu;

import com.google.common.eventbus.Subscribe;
import tech.onetap.event.list.EventKeyInput;
import tech.onetap.ui.punch.context.MinecraftContext;
import tech.onetap.ui.punch.core.MenuOverlay;
import tech.onetap.ui.punch.ui.controls.MenuClipboard;
import org.lwjgl.glfw.GLFW;

public final class MenuKeyHandler implements MinecraftContext {

    @Subscribe
    public void onKeyInput(EventKeyInput event) {
        if (mc.currentScreen != null) {
            return;
        }

        int key = event.getKey();
        int action = event.getAction();

        if (key == GLFW.GLFW_KEY_F11) {
            return;
        }

        boolean mouseButton = key >= 0 && key < 8;
        if (mouseButton) {
            if (MenuOverlay.handleMouseButton(mc, key, action)) {
                event.cancelEvent();
            }
            return;
        }

        if (action == GLFW.GLFW_PRESS && key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            if (!MenuOverlay.isOpen() || !MenuOverlay.isCapturingBind()) {
                if (MenuOverlay.toggle(mc)) {
                    event.cancelEvent();
                }
            }
            return;
        }

        if (!MenuOverlay.isOpen()) {
            return;
        }

        boolean pressOrRepeat = action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT;
        if (pressOrRepeat && MenuOverlay.handleKey(key)) {
            event.cancelEvent();
            return;
        }
        if (action == GLFW.GLFW_PRESS && key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            event.cancelEvent();
            return;
        }
        if (pressOrRepeat && key == GLFW.GLFW_KEY_ESCAPE) {
            MenuOverlay.closePageOrOverlay(mc);
            event.cancelEvent();
            return;
        } else if (pressOrRepeat && key == GLFW.GLFW_KEY_BACKSPACE && MenuOverlay.isSearchFocused()) {
            MenuOverlay.backspaceSearch();
            event.cancelEvent();
            return;
        } else if (action == GLFW.GLFW_PRESS
                && key == GLFW.GLFW_KEY_F
                && MenuClipboard.shortcutDown()) {
            MenuOverlay.openSearch();
            event.cancelEvent();
            return;
        } else if (action == GLFW.GLFW_PRESS && (key == GLFW.GLFW_KEY_TAB || key == GLFW.GLFW_KEY_RIGHT)) {
            MenuOverlay.focusNextHeaderAction(1);
            event.cancelEvent();
            return;
        } else if (action == GLFW.GLFW_PRESS && key == GLFW.GLFW_KEY_LEFT) {
            MenuOverlay.focusNextHeaderAction(-1);
            event.cancelEvent();
            return;
        } else if (action == GLFW.GLFW_PRESS && key == GLFW.GLFW_KEY_ENTER) {
            MenuOverlay.activateFocusedHeaderAction();
            event.cancelEvent();
            return;
        }

        if (MenuOverlay.isSearchFocused() || MenuOverlay.isCapturingBind()) {
            event.cancelEvent();
        }
    }
}