package tech.onetap.util.cursor;

import org.lwjgl.glfw.GLFW;

public class CursorManager {
    private static boolean hand = false;
    private static boolean iBeam = false;
    private static boolean click = false;
    private static long arrowCursor;
    private static long handCursor;
    private static long iBeamCursor;
    private static long clickCursor;

    public static void requestHand() {
        hand = true;
    }
    public static void requestIBeam() {
        iBeam = true;
    }
    public static void requestClick() {
        click = true;
    }

    public static boolean shouldBeHand() {
        return hand;
    }
    public static boolean shouldIBeam() {
        return iBeam;
    }
    public static boolean shouldClick() {
        return click;
    }

    public static void reset() {
        hand = false;
    }
    public static void resetIBeam() {
        iBeam = false;
    }
    public static void resetClick() {
        click = false;
    }

    public static void resetAll() {
        hand = false;
        iBeam = false;
        click = false;
    }

    public static void applyRequested(long window) {
        if (shouldBeHand()) {
            GLFW.glfwSetCursor(window, getHandCursor());
            return;
        }

        if (shouldIBeam()) {
            GLFW.glfwSetCursor(window, getIBeamCursor());
            return;
        }

        if (shouldClick()) {
            GLFW.glfwSetCursor(window, getClickCursor());
            return;
        }

        GLFW.glfwSetCursor(window, getArrowCursor());
    }

    public static void forceArrow(long window) {
        resetAll();
        GLFW.glfwSetCursor(window, getArrowCursor());
    }

    private static long getArrowCursor() {
        if (arrowCursor == 0L) {
            arrowCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR);
        }
        return arrowCursor;
    }

    private static long getHandCursor() {
        if (handCursor == 0L) {
            handCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
        }
        return handCursor;
    }

    private static long getIBeamCursor() {
        if (iBeamCursor == 0L) {
            iBeamCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR);
        }
        return iBeamCursor;
    }

    private static long getClickCursor() {
        if (clickCursor == 0L) {
            clickCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_POINTING_HAND_CURSOR);
        }
        return clickCursor;
    }
}
