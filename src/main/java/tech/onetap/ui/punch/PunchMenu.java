package tech.onetap.ui.punch;

import tech.onetap.Onetap;
import tech.onetap.ui.punch.feature.FeatureManager;
import tech.onetap.ui.punch.friends.FriendManager;
import tech.onetap.ui.punch.menu.MenuKeyHandler;
import tech.onetap.ui.punch.menu.MenuOverlayRenderHandler;

public final class PunchMenu {
    private static boolean initialized;

    private PunchMenu() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        Onetap.getInstance().getEventBus().register(new MenuKeyHandler());
        Onetap.getInstance().getEventBus().register(new MenuOverlayRenderHandler());
        FeatureManager.init();
        FriendManager.init();
    }
}