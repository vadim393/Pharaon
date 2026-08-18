package tech.onetap.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.onetap.event.list.EventPlayerUpdate;
import tech.onetap.event.list.MoveInputEvent;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.combat.KillAura;
import tech.onetap.util.base.Instance;
import tech.onetap.util.packet.NetworkUtils;
import tech.onetap.util.player.other.InventoryUtil;
import tech.onetap.util.rotation.Rotation;
import tech.onetap.util.rotation.RotationComponent;

@ModuleInformation(moduleName = "ElytraJump", moduleDesc = "неработает хуйня ебаная ПЕРЕДЕЛАТЬ!", moduleCategory = ModuleCategory.MOVEMENT)
public class ElytraJump extends Module {

    private static final boolean AUTO_SWAP = true;

    private float rotationYaw = Float.NaN;

    @Subscribe
    private void onMoveInput(MoveInputEvent e) {
        if (mc.player == null) return;

        if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA
                && e.jump
                && !mc.player.isGliding()) {
            e.jump = false;
        }

        if (mc.player.isGliding() && !Float.isNaN(rotationYaw)) {
            RotationComponent.fixMovement(e, rotationYaw);
        }
    }

    @Subscribe
    private void onUpdate(EventPlayerUpdate event) {
        if (mc.player == null || mc.world == null) return;
        handleJump();
    }

    private void handleJump() {
        if (mc.player.hurtTime > 0) {
            if (AUTO_SWAP && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
                swapElytra(false);
            }
            rotationYaw = Float.NaN;
            return;
        }

        if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
            handleElytra();
        } else if (AUTO_SWAP) {
            swapElytra(false);
        }

        if (mc.player.isGliding()) {
            lookAtTarget();
        } else {
            rotationYaw = Float.NaN;
        }
    }

    private void handleElytra() {
        if (!mc.player.isGliding()) {
            if (mc.player.isOnGround()) {
                mc.player.jump();
                return;
            }

            NetworkUtils.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            mc.player.startGliding();
            return;
        }

        mc.player.setVelocity(0.0, mc.player.getVelocity().y, 0.0);

        Vec3d velocity = mc.player.getVelocity();
        mc.player.setVelocity(velocity.x, velocity.y + 0.06, velocity.z);
    }

    private void swapElytra(boolean chestplate) {
        if (mc.interactionManager == null) return;

        int slot = chestplate ? InventoryUtil.findBestChestplateSlot() : InventoryUtil.findBestElytraSlot();
        if (slot == -1) return;

        if (slot >= 0 && slot <= 8) {
            mc.interactionManager.clickSlot(0, 6, slot, SlotActionType.SWAP, mc.player);
            return;
        }

        mc.interactionManager.clickSlot(0, slot, 8, SlotActionType.SWAP, mc.player);
        mc.interactionManager.clickSlot(0, 6, 8, SlotActionType.SWAP, mc.player);
        mc.interactionManager.clickSlot(0, slot, 8, SlotActionType.SWAP, mc.player);
    }

    private void lookAtTarget() {
        KillAura aura = Instance.get(KillAura.class);
        LivingEntity target = aura != null ? aura.getTarget() : null;
        if (target == null) {
            rotationYaw = Float.NaN;
            return;
        }

        Vec3d targetPos = target.getPos().add(0.0, MathHelper.clamp(target.getHeight() * 0.8, 0.0, target.getHeight()), 0.0);
        Rotation rotation = new Rotation(targetPos);
        rotationYaw = rotation.getYaw();
        RotationComponent.update(rotation, 360, 360, 360, 360, 0, 3, false);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        rotationYaw = Float.NaN;

        if (AUTO_SWAP
                && mc.player != null
                && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
            swapElytra(true);
        }
    }
}
