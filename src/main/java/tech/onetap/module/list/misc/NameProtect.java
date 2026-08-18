package tech.onetap.module.list.misc;

import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.util.friend.Friend;
import tech.onetap.util.friend.FriendRepository;

@ModuleInformation(moduleName = "Streamer Mode", moduleCategory = ModuleCategory.MISC)
public class NameProtect extends Module {
    private static final String PROTECTED_NAME = "Protected";

    public final BooleanSetting hideFriends = new BooleanSetting("Скрыть друзей", false);

    public String getCustomName() {
        return isEnabled() ? PROTECTED_NAME : mc.player.getNameForScoreboard();
    }

    public String getReplacementName() {
        return PROTECTED_NAME;
    }

    public boolean shouldProtectName(String scoreboardName) {
        if (!isEnabled() || mc.player == null || scoreboardName == null || scoreboardName.isEmpty()) {
            return false;
        }

        if (scoreboardName.equalsIgnoreCase(mc.player.getNameForScoreboard())) {
            return true;
        }

        return hideFriends.getValue() && FriendRepository.isFriend(scoreboardName);
    }

    public String getCustomName(String originalName) {
        if (!isEnabled() || mc.player == null || originalName == null || originalName.isEmpty()) {
            return originalName;
        }

        String result = replaceIgnoreCase(originalName, mc.player.getNameForScoreboard(), PROTECTED_NAME);

        if (hideFriends.getValue()) {
            for (Friend friend : FriendRepository.getFriends()) {
                result = replaceIgnoreCase(result, friend.name(), PROTECTED_NAME);
            }
        }

        return result;
    }

    private String replaceIgnoreCase(String source, String search, String replacement) {
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

    private int indexOfIgnoreCase(String source, String search, int fromIndex) {
        int max = source.length() - search.length();
        for (int i = Math.max(0, fromIndex); i <= max; i++) {
            if (source.regionMatches(true, i, search, 0, search.length())) {
                return i;
            }
        }
        return -1;
    }
}
