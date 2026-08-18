package tech.onetap.module.list.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import tech.onetap.event.list.EventKeyInput;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BindSetting;
import tech.onetap.module.settings.BooleanSetting;

@ModuleInformation(moduleName = "Crystal Spammer", moduleCategory = ModuleCategory.COMBAT)
public class CrystalSpammer extends Module {
    private final BindSetting key = new BindSetting("Клавиша спама", -1);
    private final BooleanSetting placeObsidian = new BooleanSetting("Ставить обсидиан", true);

    private boolean active;
    private int previousSlot = -1;

    @Subscribe
    private void onUpdate(EventTick e) {
        if (!active || mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) active = false;

        ClientPlayerEntity player = mc.player;
        if (placeObsidian.getValue() && mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult) mc.crosshairTarget;
            BlockPos targetPos = blockHitResult.getBlockPos().offset(blockHitResult.getSide());
            
            if (mc.world.getBlockState(targetPos).isAir()) {
                BlockPos supportPos = targetPos.down();
                if (!mc.world.getBlockState(supportPos).isOf(Blocks.OBSIDIAN) && !mc.world.getBlockState(supportPos).isOf(Blocks.BEDROCK)) {
                    if (placeObsidianAt(supportPos)) {
                        return;
                    }
                }
            }
        }

        if (!holdCrystal(player)) return;

        placeCrystal();

        if (mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            if (mc.crosshairTarget instanceof EntityHitResult hitResult) {
                mc.interactionManager.attackEntity(mc.player, hitResult.getEntity());
                player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    @Subscribe
    private void onKey(EventKeyInput e) {
        if (e.getKey() == key.getValue()) active = e.getAction() == 1 || e.getAction() == 2;
    }

    private boolean holdCrystal(ClientPlayerEntity player) {
        ItemStack main = player.getMainHandStack();
        if (main.getItem() instanceof EndCrystalItem) return true;

        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() instanceof EndCrystalItem) {
                player.getInventory().selectedSlot = i;
                return true;
            }
        }
        return false;
    }

    boolean placed;

    private void placeCrystal() {
        if (mc.interactionManager == null || mc.player == null) return;

        if (mc.crosshairTarget != null) {
            if (mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHitResult = (BlockHitResult)mc.crosshairTarget;
                ItemStack itemStack = mc.player.getStackInHand(Hand.MAIN_HAND);
                int i = itemStack.getCount();
                ActionResult actionResult2 = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult);
                if (actionResult2 instanceof ActionResult.Success) {
                    ActionResult.Success success2 = (ActionResult.Success)actionResult2;
                    if (success2.swingSource() == ActionResult.SwingSource.CLIENT) {
                        mc.player.swingHand(Hand.MAIN_HAND);
                        if (!itemStack.isEmpty() && (itemStack.getCount() != i || mc.interactionManager.hasCreativeInventory())) {
                            mc.gameRenderer.firstPersonRenderer.resetEquipProgress(Hand.MAIN_HAND);
                        }
                    }
                }
                placed = true;
            }
        }
    }

    private boolean placeObsidianAt(BlockPos pos) {
        if (mc.player == null || mc.interactionManager == null) return false;

        int obsidianSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == Blocks.OBSIDIAN) {
                obsidianSlot = i;
                break;
            }
        }

        if (obsidianSlot == -1) return false;
        previousSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = obsidianSlot;
        Direction side = Direction.UP;
        BlockPos placeOnPos = pos;

        if (mc.world.getBlockState(placeOnPos).isAir() || !mc.world.getBlockState(placeOnPos).isSolid()) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.offset(dir);
                if (!mc.world.getBlockState(neighbor).isAir() && mc.world.getBlockState(neighbor).isSolid()) {
                    side = dir.getOpposite();
                    placeOnPos = neighbor;
                    break;
                }
            }
        }

        Vec3d hitVec = Vec3d.ofCenter(placeOnPos);
        BlockHitResult placeResult = new BlockHitResult(hitVec, side, placeOnPos, false);

        ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, placeResult);
        
        if (result instanceof ActionResult.Success success) {
            if (success.swingSource() == ActionResult.SwingSource.CLIENT) {
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }

        if (previousSlot != -1) {
            mc.player.getInventory().selectedSlot = previousSlot;
            previousSlot = -1;
        }

        return result instanceof ActionResult.Success;
    }
}