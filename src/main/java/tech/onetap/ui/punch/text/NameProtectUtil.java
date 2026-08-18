package tech.onetap.ui.punch.text;

import net.minecraft.client.MinecraftClient;
import tech.onetap.Onetap;
import tech.onetap.module.list.misc.NameProtect;
import tech.onetap.util.friend.Friend;
import tech.onetap.util.friend.FriendRepository;

public final class NameProtectUtil {
    public static final String PROTECTED_NAME = "Protected";

    private NameProtectUtil() {
    }

    public static String protect(String name) {
        if (name == null) {
            return "";
        }
        MinecraftClient minecraft = MinecraftClient.getInstance();
        NameProtect module = Onetap.getInstance().getModuleStorage().get(NameProtect.class);
        if (module != null && module.isEnabled()) {
            String replaced = module.getCustomName(name);
            if (replaced != null && !replaced.equals(name)) {
                return replaced;
            }
            return replaceIgnoreCase(name, protectPlayerName(minecraft), PROTECTED_NAME);
        }
        return name;
    }

    public static boolean shouldProtect(String name) {
        NameProtect module = Onetap.getInstance().getModuleStorage().get(NameProtect.class);
        return module != null && module.shouldProtectName(name);
    }

    private static String protectPlayerName(MinecraftClient minecraft) {
        if (minecraft.player != null) {
            return minecraft.player.getNameForScoreboard();
        }
        return minecraft.getSession().getUsername();
    }

    private static String replaceIgnoreCase(String source, String search, String replacement) {
        if (source == null || source.isEmpty() || search == null || search.isEmpty()) {
            return source;
        }
        StringBuilder builder = new StringBuilder(source.length());
        int start = 0;
        int index;
        while ((index = indexOfIgnoreCase(source, search, start)) >= 0) {
            builder.append(source, start, index).append(replacement);
            start = index + search.length();
        }
        if (start == 0) {
            return source;
        }
        builder.append(source, start, source.length());
        return builder.toString();
    }

    private static int indexOfIgnoreCase(String source, String search, int fromIndex) {
        int max = source.length() - search.length();
        for (int i = Math.max(0, fromIndex); i <= max; i++) {
            if (source.regionMatches(true, i, search, 0, search.length())) {
                return i;
            }
        }
        return -1;
    }
}