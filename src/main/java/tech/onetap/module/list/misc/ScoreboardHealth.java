package tech.onetap.module.list.misc;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;

@ModuleInformation(moduleName = "Scoreboard Health", moduleCategory = ModuleCategory.MISC)
public class ScoreboardHealth extends Module {

    public ScoreboardHealth() {
        setEnabled(true);
    }

    public float getHealth(LivingEntity entity) {
        return getHealth(entity, false);
    }

    public float getHealth(LivingEntity entity, boolean includeAbsorption) {
        float health = getBaseHealth(entity);
        if (includeAbsorption) {
            health += Math.max(0f, entity.getAbsorptionAmount());
        }
        return Math.max(0f, health);
    }

    private float getBaseHealth(LivingEntity entity) {
        if (!isEnabled()) return entity.getHealth();
        if (!(entity instanceof PlayerEntity player)) return entity.getHealth();

        ScoreboardObjective objective = player.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
        if (objective == null) return entity.getHealth();

        ReadableScoreboardScore score = player.getScoreboard().getScore(player, objective);
        if (score == null) return entity.getHealth();

        String value = ReadableScoreboardScore.getFormattedScore(score, objective.getNumberFormatOr(StyledNumberFormat.EMPTY))
                .getString()
                .replaceAll("[^0-9.,-]", "")
                .replace(',', '.');
        if (value.isEmpty()) return entity.getHealth();

        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ignored) {
            return entity.getHealth();
        }
    }
}
