package tech.onetap.module.list.combat.rotation;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.onetap.util.IMinecraft;
import tech.onetap.util.render.math.MathUtil;
import tech.onetap.util.rotation.Rotation;

import java.util.concurrent.ThreadLocalRandom;

public class FTRotation implements IMinecraft {
    public boolean attack;
    public int attackedTicks;
    public int attackCount;
    public int idAttack;
    public long lastAttack;
    public boolean firstAttack = true;
    private Rotation returnRotation;

    public void reset() {
        attack = false;
        attackedTicks = 0;
        attackCount = 0;
        idAttack = 0;
        lastAttack = 0;
        firstAttack = true;
        if (mc.player != null) {
            returnRotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        }
    }

    public void updateAttackState(boolean attack) {
        this.attack = attack;
    }

    public void onAttack() {
        attackCount++;
        if (firstAttack && attackCount >= 2) {
            firstAttack = false;
            attackCount = 0;
        }

        lastAttack = System.currentTimeMillis();
        if (mc.player != null) {
            returnRotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        }

        idAttack = ThreadLocalRandom.current().nextInt(2);
        attackedTicks = ThreadLocalRandom.current().nextInt(12, 17) * 3;
    }

    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        if (attackedTicks > 0) attackedTicks = Math.max(attackedTicks - 1, 0);

        long timeSinceAttack = System.currentTimeMillis() - lastAttack;
        boolean returnAfterHit = timeSinceAttack > 140 && timeSinceAttack < 620;
        boolean lateAttack = timeSinceAttack > 800;

        float prevPitch = targetRotation.getPitch();

        float swing = (float) (Math.sin(mc.player.age * 0.85f) * MathUtil.random(9.0f, 11.0f)
                + Math.sin(mc.player.age * 0.23f) * MathUtil.random(2.0f, 3.0f));
        if (attack) {
            swing *= 0.18f;
        }

        float yawSpeed = 69f / 3f;
        float pitchSpeed = 15f / 3f;

        if (returnRotation == null) {
            returnRotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        }

        if (!attack && returnAfterHit) {
            targetRotation = returnRotation;
            yawSpeed = 88f / 3f;
            pitchSpeed = 28f / 3f;
        } else if (!attack && lateAttack) {
            firstAttack = true;
            targetRotation = new Rotation(returnRotation.getYaw(), prevPitch);
            yawSpeed *= 0.2f;
        }

        float yawDelta = MathHelper.wrapDegrees(targetRotation.getYaw() - currentRotation.getYaw());
        float pitchDelta = targetRotation.getPitch() - currentRotation.getPitch();

        float finalDeltaYaw = yawDelta + swing;
        float finalDeltaPitch = pitchDelta;

        float finalYaw = currentRotation.getYaw() + MathHelper.clamp(finalDeltaYaw, -yawSpeed, yawSpeed);
        float finalPitch = currentRotation.getPitch() + MathHelper.clamp(finalDeltaPitch, -pitchSpeed, pitchSpeed);

        return new Rotation(finalYaw, finalPitch);
    }
}