package tech.onetap.module.list.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.friend.FriendRepository;

@ModuleInformation(moduleName = "HitBox", moduleCategory = ModuleCategory.COMBAT)
public class HitBox extends Module {
    public final SliderSetting expand = new SliderSetting("Расширение", 0.15, 0.0, 1.5, 0.01);
    public final BooleanSetting onlyPlayers = new BooleanSetting("Только игроки", true);
    public final BooleanSetting affectFriends = new BooleanSetting("Расширять друзей", false);

    public boolean shouldExpand(Entity entity) {
        if (mc.player == null || entity == null) return false;
        if (entity == mc.player) return false;
        if (!entity.isAlive()) return false;
        if (onlyPlayers.getValue() && !(entity instanceof PlayerEntity)) return false;
        if (entity instanceof PlayerEntity p && !affectFriends.getValue() && FriendRepository.isFriend(p.getNameForScoreboard())) {
            return false;
        }
        return expand.getValue() > 0.0;
    }
}
