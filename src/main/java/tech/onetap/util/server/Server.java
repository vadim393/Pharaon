package tech.onetap.util.server;

import lombok.experimental.UtilityClass;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import tech.onetap.Onetap;
import tech.onetap.module.list.misc.ScoreboardHealth;
import tech.onetap.util.IMinecraft;

@UtilityClass
public class Server implements IMinecraft {
    public int getPing(PlayerEntity entity) {
        PlayerListEntry list = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
        return list != null ? list.getLatency() : 0;
    }

    public float getHealth(LivingEntity entity, boolean gapple) {
        ScoreboardHealth scoreboardHealth = Onetap.getInstance().getModuleStorage().get(ScoreboardHealth.class);
        if (scoreboardHealth != null) {
            return scoreboardHealth.getHealth(entity, gapple);
        }
        return entity.getHealth() + (gapple ? entity.getAbsorptionAmount() : 0f);
    }
}
