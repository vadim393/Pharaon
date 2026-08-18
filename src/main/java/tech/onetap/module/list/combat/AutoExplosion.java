package tech.onetap.module.list.combat;

import com.google.common.eventbus.Subscribe;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import tech.onetap.event.list.EventEntitySpawn;
import tech.onetap.event.list.EventObsidianPlace;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.friend.FriendRepository;
import tech.onetap.util.math.BestPoint;
import tech.onetap.util.math.RotationUtil;
import tech.onetap.util.player.other.InventoryUtil;
import tech.onetap.util.player.other.SlownessManager;
import tech.onetap.util.rotation.Rotation;
import tech.onetap.util.rotation.RotationComponent;

@ModuleInformation(moduleName = "Auto Explosion", moduleDesc = "Автоматически взрывает кристаллы на обсидиане", moduleCategory = ModuleCategory.COMBAT)
public class AutoExplosion extends Module {
    private final BooleanSetting saveSelf = new BooleanSetting("Не бабах себя", false);
    private final BooleanSetting saveFriend = new BooleanSetting("Не бабах друзей", false);
    private final BooleanSetting saveResources = new BooleanSetting("Не бахать ресы", false);
    private final SliderSetting maxSelfDamage = new SliderSetting("Макс урон себе", 6.0, 0.0, 20.0, 0.5).setVisible(() -> saveSelf.getValue());
    private final SliderSetting maxFriendDamage = new SliderSetting("Макс урон другу", 6.0, 0.0, 20.0, 0.5).setVisible(() -> saveFriend.getValue());
    private final BooleanSetting throughWalls = new BooleanSetting("Через стены", false);
    private final SliderSetting delay = new SliderSetting("Задержка (мс)", 50.0, 0.0, 1000.0, 50.0);
    private BlockPos obsidianPos;
    private Entity entityToAttack;
    private int prevSlot = -1;
    @Getter private int ticksToDisableRightClicks;
    private final Set<BlockPos> myCrystalPlaces = new HashSet<>();
    private final Set<BlockPos> pendingCrystalPlacements = new HashSet<>();

    @Subscribe
    private void onObsidianPlace(EventObsidianPlace e) {
        int crystalSlot = InventoryUtil.searchItemHotbar(Items.END_CRYSTAL);
        if (crystalSlot != -1) {
            if (obsidianPos == null && condition(e.getBlockPos())) {
                BlockPos crystalPlacePos = e.getBlockPos().up();
                if (!pendingCrystalPlacements.contains(crystalPlacePos) && !hasCrystalAt(crystalPlacePos)) {
                    obsidianPos = e.getBlockPos();
                    pendingCrystalPlacements.add(crystalPlacePos);
                    ticksToDisableRightClicks = 5;
                }
            }
        }
    }

    @Subscribe
    private void onTick(EventTick e) {
        if (ticksToDisableRightClicks > 0) {
            ticksToDisableRightClicks--;
        }

        if (entityToAttack != null) {
            if (mc.player.getEyePos().distanceTo(BestPoint.getNearestPoint(entityToAttack)) < 3.0) {
                Rotation rotation = new Rotation(RotationUtil.calculate(BestPoint.getNearestPoint(entityToAttack)));
                RotationComponent.update(rotation, 360.0F, 360.0F, 360.0F, 360.0F, 0, 55, false);
                SlownessManager.addTimeTask(new SlownessManager.TimeTask(50L, () -> {
                    if (entityToAttack != null) {
                        mc.interactionManager.attackEntity(mc.player, entityToAttack);
                        mc.player.swingHand(Hand.MAIN_HAND);
                        entityToAttack = null;
                    }
                }, true));
            } else {
                entityToAttack = null;
            }
        }

        if (obsidianPos != null) {
            Rotation rotation = new Rotation(Vec3d.ofCenter(obsidianPos));
            RotationComponent.update(rotation, 360.0F, 360.0F, 360.0F, 360.0F, 0, 55, false);
            int slot = InventoryUtil.searchItemHotbar(Items.END_CRYSTAL);
            SlownessManager.addTimeTask(new SlownessManager.TimeTask((int) delay.getValue(), () -> {
                if (slot != -1 && mc.crosshairTarget instanceof BlockHitResult hitResult) {
                    BlockPos placePos = hitResult.getBlockPos().up();
                    Vec3d crystalVec = Vec3d.ofCenter(placePos);
                    if (saveResources.getValue() && hasItemsNearby(placePos)) {
                        obsidianPos = null;
                        return;
                    }

                    if (saveSelf.getValue()) {
                        double selfDamage = calculateExplosionDamage(crystalVec, mc.player);
                        if (selfDamage > maxSelfDamage.getValue()) {
                            obsidianPos = null;
                            return;
                        }
                    }

                    if (saveFriend.getValue()) {
                        for (Entity entity : mc.world.getEntities()) {
                            if (entity != mc.player && entity instanceof PlayerEntity player && FriendRepository.isFriend(player.getNameForScoreboard())) {
                                double friendDamage = calculateExplosionDamage(crystalVec, player);
                                if (friendDamage > maxFriendDamage.getValue()) {
                                    obsidianPos = null;
                                    return;
                                }
                            }
                        }
                    }

                    if (prevSlot == -1) {
                        prevSlot = mc.player.getInventory().selectedSlot;
                    }

                    mc.player.getInventory().selectedSlot = slot;
                    myCrystalPlaces.add(placePos);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
                    SlownessManager.addTimeTask(new SlownessManager.TimeTask(5L, () -> {
                        if (prevSlot != -1) {
                            mc.player.getInventory().selectedSlot = prevSlot;
                        }

                        prevSlot = -1;
                    }, true));
                    pendingCrystalPlacements.remove(placePos);
                    obsidianPos = null;
                }
            }, true));
        }
    }

    public void handleRightClickBlock(Hand hand, BlockHitResult hitResult) {
        if (mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL) return;

        BlockPos placePos = hitResult.getBlockPos().up();
        myCrystalPlaces.add(placePos);
    }

    @Subscribe
    private void onEntitySpawn(EventEntitySpawn e) {
        if (!(e.getEntity() instanceof EndCrystalEntity crystal)) return;

        BlockPos var10 = crystal.getBlockPos();
        if (myCrystalPlaces.contains(var10)) {
            Vec3d crystalVec = crystal.getPos();
            if (saveSelf.getValue()) {
                double selfDamage = calculateExplosionDamage(crystalVec, mc.player);
                if (selfDamage > maxSelfDamage.getValue()) {
                    myCrystalPlaces.remove(var10);
                    return;
                }
            }

            if (saveFriend.getValue()) {
                for (Entity entity : mc.world.getEntities()) {
                    if (entity != mc.player && entity instanceof PlayerEntity player && FriendRepository.isFriend(player.getNameForScoreboard())) {
                        double friendDamage = calculateExplosionDamage(crystalVec, player);
                        if (friendDamage > maxFriendDamage.getValue()) {
                            myCrystalPlaces.remove(var10);
                            return;
                        }
                    }
                }
            }

            if (saveResources.getValue() && hasItemsNearby(var10)) {
                myCrystalPlaces.remove(var10);
            } else {
                entityToAttack = crystal;
                myCrystalPlaces.remove(var10);
            }
        }
    }

    private boolean condition(BlockPos blockPos) {
        Vec3d crystalPos = Vec3d.ofCenter(blockPos.up());
        if (!throughWalls.getValue() && !canSeePosition(crystalPos)) {
            return false;
        }

        if (saveSelf.getValue()) {
            double selfDamage = calculateExplosionDamage(crystalPos, mc.player);
            if (selfDamage > maxSelfDamage.getValue()) {
                return false;
            }
        }

        if (saveFriend.getValue()) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity != mc.player && entity instanceof PlayerEntity player && FriendRepository.isFriend(player.getNameForScoreboard())) {
                    double friendDamage = calculateExplosionDamage(crystalPos, player);
                    if (friendDamage > maxFriendDamage.getValue()) {
                        return false;
                    }
                }
            }
        }

        return !saveResources.getValue() || !hasItemsNearby(blockPos.up());
    }

    private boolean canSeePosition(Vec3d pos) {
        Vec3d eyePos = mc.player.getEyePos();
        RaycastContext context = new RaycastContext(eyePos, pos, ShapeType.COLLIDER, FluidHandling.NONE, mc.player);
        HitResult result = mc.world.raycast(context);
        return result.getType() == Type.MISS;
    }

    private double calculateExplosionDamage(Vec3d explosionPos, PlayerEntity target) {
        double distance = target.getPos().distanceTo(explosionPos);
        if (distance > 12.0) {
            return 0.0;
        }

        double damage = (1.0 - distance / 12.0) * 12.0;
        if (target.getArmor() > 0) {
            damage *= 1.0 - Math.min(target.getArmor() * 0.04, 0.8);
        }

        return Math.max(0.0, damage);
    }

    private boolean hasItemsNearby(BlockPos pos) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ItemEntity item) {
                double dx = Math.abs(item.getX() - pos.getX());
                double dy = Math.abs(item.getY() - pos.getY());
                double dz = Math.abs(item.getZ() - pos.getZ());
                if (dx <= 12.0 && dy <= 12.0 && dz <= 12.0) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasCrystalAt(BlockPos pos) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity crystal) {
                BlockPos crystalPos = crystal.getBlockPos();
                if (crystalPos.equals(pos)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void onDisable() {
        obsidianPos = null;
        entityToAttack = null;
        ticksToDisableRightClicks = 0;
        pendingCrystalPlacements.clear();
        super.onDisable();
    }
}