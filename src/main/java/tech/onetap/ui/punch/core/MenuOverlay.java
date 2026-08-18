package tech.onetap.ui.punch.core;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public final class MenuOverlay {
    private static final MenuOverlayState STATE = new MenuOverlayState();
    private static final MenuDragController DRAG = new MenuDragController();
    private static final MenuOverlayRenderer RENDERER = new MenuOverlayRenderer();

    private MenuOverlay() {
    }

    public static boolean toggle(MinecraftClient minecraft) {
        if (STATE.isInteractive()) {
            close(minecraft);
            return true;
        }
        open(minecraft);
        return true;
    }

    public static void close(MinecraftClient minecraft) {
        if (!STATE.isOpen() || STATE.isClosing()) {
            return;
        }

        boolean restoreMouse = shouldReturnMouseToGame(minecraft);
        STATE.beginClose();
        DRAG.reset();
        RENDERER.releasePointer();
        if (restoreMouse) {
            minecraft.mouse.lockCursor();
        }
    }

    public static boolean suspendForReload(MinecraftClient minecraft) {
        if (!STATE.isInteractive()) {
            return false;
        }
        boolean restoreMouse = STATE.grabbedMouseBeforeOpen() && shouldReturnMouseToGame(minecraft);
        STATE.suspend();
        DRAG.reset();
        RENDERER.releasePointer();
        if (restoreMouse) {
            minecraft.mouse.lockCursor();
        }
        return true;
    }

    public static void resumeAfterReload(MinecraftClient minecraft) {
        if (STATE.isOpen()) {
            return;
        }
        STATE.resume();
        DRAG.reset();
        KeyBinding.unpressAll();
        if (minecraft.mouse.isCursorLocked()) {
            minecraft.mouse.unlockCursor();
        }
    }

    public static boolean isOpen() {
        return STATE.isInteractive();
    }

    public static boolean isVisible() {
        return STATE.isOpen();
    }

    public static boolean blocksInput() {
        return STATE.isInteractive();
    }

    public static void focusNextHeaderAction(int direction) {
        STATE.focusNextHeaderAction(direction);
    }

    public static void activateFocusedHeaderAction() {
        STATE.activateFocusedHeaderAction();
    }

    public static void closePageOrOverlay(MinecraftClient minecraft) {
        if (STATE.page() != MenuPage.NONE) {
            STATE.openPage(MenuPage.NONE);
            return;
        }
        close(minecraft);
    }

    public static void openSearch() {
        if (STATE.isInteractive()) {
            if (STATE.page() != MenuPage.NONE) {
                STATE.openPage(MenuPage.NONE);
            }
            RENDERER.focusSearch();
        }
    }

    public static boolean isSearchOpen() {
        return STATE.isInteractive() && RENDERER.isSearchOpen();
    }

    public static boolean isSearchFocused() {
        return STATE.isInteractive() && RENDERER.isSearchFocused();
    }

    public static boolean isCapturingBind() {
        return STATE.isInteractive() && RENDERER.isCapturingBind();
    }

    public static void backspaceSearch() {
        RENDERER.backspaceSearch();
    }

    public static boolean handleKey(int key) {
        return STATE.isInteractive() && RENDERER.handleKey(key);
    }

    public static boolean handleCharacter(int codePoint) {
        if (!STATE.isInteractive()) {
            return false;
        }
        if (RENDERER.handleCharacter(codePoint)) {
            return true;
        }
        if (!isSearchFocused()) {
            return false;
        }
        RENDERER.appendSearchCodePoint(codePoint);
        return true;
    }

    public static void handleScroll(double vertical) {
        if (!STATE.isInteractive()) return;
        RENDERER.handleScroll(mouseX(MinecraftClient.getInstance()), mouseY(MinecraftClient.getInstance()), vertical);
    }

    public static boolean handleMouseButton(MinecraftClient minecraft, int button, int action) {
        if (!STATE.isInteractive()) {
            return false;
        }

        int screenWidth = minecraft.getWindow().getScaledWidth();
        int screenHeight = minecraft.getWindow().getScaledHeight();
        int mouseX = mouseX(minecraft);
        int mouseY = mouseY(minecraft);
        RENDERER.layout(minecraft, STATE, screenWidth, screenHeight, mouseX, mouseY);

        if (action == GLFW.GLFW_PRESS) {
            if (RENDERER.handleMouseButton(mouseX, mouseY, button, STATE)) {
                DRAG.onRelease();
                return true;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                DRAG.onPress(mouseX, mouseY, STATE, RENDERER);
            }
        } else if (action == GLFW.GLFW_RELEASE) {
            RENDERER.releasePointer();
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    DRAG.onRelease();
            }
        }
        return true;
    }

    public static void render(MinecraftClient minecraft, DrawContext context, int screenWidth, int screenHeight) {
        if (!STATE.isOpen()) {
            return;
        }
        if (STATE.isClosing() && STATE.openProgress() <= 0.001F) {
            STATE.close();
            return;
        }

        MenuDimensions dimensions = MenuDimensions.resolve(minecraft, STATE);
        int mouseX = mouseX(minecraft);
        int mouseY = mouseY(minecraft);
        DRAG.update(mouseX, mouseY, STATE, dimensions, screenWidth, screenHeight);
        RENDERER.layout(minecraft, STATE, screenWidth, screenHeight, mouseX, mouseY);
        RENDERER.drag(mouseX, mouseY);
        RENDERER.render(minecraft, context);
    }

    private static void open(MinecraftClient minecraft) {
        boolean grabbedMouseBeforeOpen = minecraft.mouse.isCursorLocked();
        STATE.open(grabbedMouseBeforeOpen);
        DRAG.reset();
        KeyBinding.unpressAll();
        if (grabbedMouseBeforeOpen) {
            minecraft.mouse.unlockCursor();
        }
    }

    private static int mouseX(MinecraftClient minecraft) {
        return (int) Math.round(minecraft.mouse.getX() / minecraft.getWindow().getScaleFactor());
    }

    private static int mouseY(MinecraftClient minecraft) {
        return (int) Math.round(minecraft.mouse.getY() / minecraft.getWindow().getScaleFactor());
    }

    private static boolean shouldReturnMouseToGame(MinecraftClient minecraft) {
        return minecraft.world != null && minecraft.player != null && minecraft.currentScreen == null;
    }
}
