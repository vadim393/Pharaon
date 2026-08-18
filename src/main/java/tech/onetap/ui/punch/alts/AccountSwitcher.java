package tech.onetap.ui.punch.alts;

import net.minecraft.client.MinecraftClient;
import tech.onetap.util.alt.Alt;
import tech.onetap.util.alt.AltManager;

import java.util.List;
import java.util.UUID;

public final class AccountSwitcher {

    public static final String DEFAULT_NAME = MinecraftClient.getInstance().getSession().getUsername();

    private AccountSwitcher() {
    }

    public static void switchTo(String name) {
        if (name == null || name.isEmpty()) {
            return;
        }
        AltManager.login(findOrCreate(name));
    }

    public static void relogin(String name) {
        if (name == null || name.isEmpty()) {
            return;
        }
        AltManager.login(findOrCreate(name));
    }

    private static Alt findOrCreate(String name) {
        List<Alt> alts = AltManager.getAlts();
        for (Alt alt : alts) {
            if (alt.name().equalsIgnoreCase(name)) {
                return alt;
            }
        }
        AltManager.addAlt(name);
        return new Alt(name, UUID.randomUUID().toString(), false);
    }
}