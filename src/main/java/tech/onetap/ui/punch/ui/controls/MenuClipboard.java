package tech.onetap.ui.punch.ui.controls;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

public final class MenuClipboard {
    private MenuClipboard() {
    }

    public static boolean shortcutDown() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return false;
        }
        long window = client.getWindow().getHandle();
        if (Util.getOperatingSystem() == Util.OperatingSystem.OSX) {
            return keyDown(window, GLFW.GLFW_KEY_LEFT_SUPER)
                    || keyDown(window, GLFW.GLFW_KEY_RIGHT_SUPER);
        }
        return keyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || keyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    public static String get() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client == null ? "" : client.keyboard.getClipboard();
    }

    public static void set(String value) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.keyboard.setClipboard(value == null ? "" : value);
        }
    }

    private static boolean keyDown(long window, int key) {
        return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }
}
