package tech.onetap.util.rotation;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import tech.onetap.module.list.combat.ElytraTarget;
import tech.onetap.util.player.combat.PredictUtils;

@UtilityClass
public class ElytraComponent {

    public Vec3d pos = Vec3d.ZERO;
    private static Vec3d lastPredict = Vec3d.ZERO;

    public void processTargetLogic(ElytraTarget module, LivingEntity target) {
        if (module == null || target == null) {
            pos = Vec3d.ZERO;
            return;
        }

        module.status = module.target.getValue();
        if (module.shouldTarget(target)) {
            lastPredict = PredictUtils.predict(target, module.getAdaptivePredictDistance());
            pos = lastPredict;
        } else {
            pos = Vec3d.ZERO;
        }
    }

    public void smartPredict() {
        if (pos.equals(Vec3d.ZERO)) {
            pos = lastPredict;
        }
    }

    public Vec3d getVector3d(PlayerEntity player, LivingEntity target) {
        if (player == null || target == null) {
            return Vec3d.ZERO;
        }

        Vec3d targetPoint = pos.equals(Vec3d.ZERO) ? target.getPos() : pos;
        Vec3d direction = targetPoint.subtract(player.getPos());
        double length = direction.length();
        return length > 0.0 ? direction.multiply(1.0 / length) : Vec3d.ZERO;
    }

    public void resetState() {
        pos = Vec3d.ZERO;
        lastPredict = Vec3d.ZERO;
    }
}
