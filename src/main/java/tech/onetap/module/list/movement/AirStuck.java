package tech.onetap.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import tech.onetap.event.list.EventAttack;
import tech.onetap.event.list.EventPacket;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.combat.KillAura;
import tech.onetap.module.list.player.ElytraSwap;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.util.base.Instance;
import tech.onetap.util.chat.ChatUtil;

@ModuleInformation(moduleName = "Air Stuck", moduleCategory = ModuleCategory.MOVEMENT)
public class AirStuck extends Module {

    private final ModeSetting mode = new ModeSetting("Мод", "Vanilla", "Vanilla", "Grim", "Polar");

    private final BooleanSetting autoSwapChest = new BooleanSetting("Свап на нагрудник", true);
    private final BooleanSetting auraBoost = new BooleanSetting("Аура буст", true);
    private final BooleanSetting backElytra = new BooleanSetting("Вернуть при выкл", true)
            .setVisible(autoSwapChest::getValue);
    private final BooleanSetting fallCheck = new BooleanSetting("Проверка на падение", true);

    private Vec3d savedVelocity = Vec3d.ZERO;
    private boolean isElytra;
    private boolean isBoosting = false;
    private boolean waitingFirstDamage;
    private LivingEntity pendingFirstHitTarget;
    private int pendingFirstHitHurtTime;
    private int pendingFirstHitTicks;

    @Subscribe
    private void onPacket(EventPacket e) {
        if (mc.player == null) return;

        if (e.getPacket() instanceof PlayerMoveC2SPacket) e.cancelEvent();
    }

    @Subscribe
    private void onAttack(EventAttack e) {
        if (!auraBoost.getValue() || !waitingFirstDamage || isBoosting) return;
        if (!(e.getEntity() instanceof LivingEntity living)) return;

        pendingFirstHitTarget = living;
        pendingFirstHitHurtTime = living.hurtTime;
        pendingFirstHitTicks = 4;
    }

    @Subscribe
    private void onTick(EventTick e) {
        if (mc.player == null) return;

        if (auraBoost.getValue() && waitingFirstDamage && !isBoosting) {
            tryApplyAuraBoostAfterDamage();
        }

        mc.player.setVelocity(0, 0, 0);
        mc.player.setNoGravity(true);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player == null || mc.world == null) return;

        waitingFirstDamage = false;
        pendingFirstHitTarget = null;
        pendingFirstHitHurtTime = 0;
        pendingFirstHitTicks = 0;
        isBoosting = false;

        if (mc.player.fallDistance == 0 && fallCheck.getValue()) {
            ChatUtil.send("Вам нужно падать");
            setEnabled(false);
            return;
        }

        if (auraBoost.getValue()) {
            KillAura.setForcedAttackRange(3.0);
            waitingFirstDamage = true;
        }

        mc.player.setNoGravity(true);

        savedVelocity = mc.player.getVelocity();

        boolean wearingElytra = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;

        if (!wearingElytra || !autoSwapChest.getValue()) return;
        isElytra = true;

        Instance.get(ElytraSwap.class).swap(mode, true);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.player == null) return;

        waitingFirstDamage = false;
        pendingFirstHitTarget = null;
        pendingFirstHitHurtTime = 0;
        pendingFirstHitTicks = 0;

        if (isBoosting) {
            KillAura.distanceBoost -= 2.75;
            isBoosting = false;
        }

        if (auraBoost.getValue()) {
            KillAura.setForcedAttackRange(-1.0);
        }

        if (mc.player.fallDistance == 0 && fallCheck.getValue()) return;

        if (savedVelocity != null) mc.player.setVelocity(savedVelocity);

        mc.player.setNoGravity(false);

        boolean wearingChestPlate = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof ArmorItem;

        if (!wearingChestPlate || !(autoSwapChest.getValue() && backElytra.getValue()) || !isElytra) return;
        isElytra = false;

        Instance.get(ElytraSwap.class).swap(mode, false);
    }

    private void tryApplyAuraBoostAfterDamage() {
        if (pendingFirstHitTarget == null || !pendingFirstHitTarget.isAlive()) {
            pendingFirstHitTarget = null;
            pendingFirstHitTicks = 0;
            return;
        }

        if (pendingFirstHitTicks <= 0) {
            pendingFirstHitTarget = null;
            return;
        }

        if (pendingFirstHitTarget.hurtTime > pendingFirstHitHurtTime) {
            KillAura.distanceBoost += 2.75;
            KillAura.setForcedAttackRange(5.75);
            isBoosting = true;
            waitingFirstDamage = false;
            pendingFirstHitTarget = null;
            pendingFirstHitTicks = 0;
            return;
        }

        pendingFirstHitTicks--;
        if (pendingFirstHitTicks <= 0) {
            pendingFirstHitTarget = null;
        }
    }
}
