package tech.onetap.module.list.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeListSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.math.RotationUtil;
import tech.onetap.util.rotation.Rotation;
import tech.onetap.util.rotation.RotationComponent;

@ModuleInformation(moduleName = "ItemPilot", moduleDesc = "Наводит прицел на слезу гаста, головы игроков и кастомные мечи", moduleCategory = ModuleCategory.COMBAT)
public class ItemPilot extends Module {

    private final ModeListSetting targets = new ModeListSetting("Таргеты",
            new BooleanSetting("Осколок", true),
            new BooleanSetting("Шар", true),
            new BooleanSetting("Кастом. Меч", true));
    private final SliderSetting range = new SliderSetting("Дальность", 8f, 5f, 50f, 1f);
    private final BooleanSetting clientLook = new BooleanSetting("Клиент лук", true);
    private final BooleanSetting onlyFov = new BooleanSetting("Только в ФОВ", false);

    private RegistryEntry<Enchantment> sharpnessEntry;

    @Subscribe
    private void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        Entity target = findTarget();
        if (target == null) return;

        Vec3d aimPoint = getAimPoint(target);
        if (aimPoint == null) return;

        if (onlyFov.getValue()) {
            Rotation current = new Rotation(mc.player);
            Rotation targetRotation = new Rotation(RotationUtil.calculate(aimPoint));
            if (current.getDelta(targetRotation) > 60f) return;
        }

        Rotation targetRotation = new Rotation(RotationUtil.calculate(aimPoint));
        RotationComponent.update(targetRotation, 360f, 360f, 360f, 360f, 0, 1, clientLook.getValue());
    }

    private Entity findTarget() {
        if (sharpnessEntry == null && mc.world != null) {
            sharpnessEntry = mc.world.getRegistryManager()
                    .getOptional(RegistryKeys.ENCHANTMENT)
                    .flatMap(reg -> reg.getEntry(Enchantments.SHARPNESS.getValue()))
                    .orElse(null);
        }

        Entity best = null;
        double bestDist = range.getValue();
        Vec3d eye = mc.player.getEyePos();

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player || !entity.isAlive()) continue;

            Vec3d point = getAimPoint(entity);
            if (point == null) continue;

            boolean matched = false;
            if (targets.isEnabled("Шар") && isPlayerHead(entity)) {
                matched = true;
            } else if (targets.isEnabled("Осколок") && isGhastTear(entity)) {
                matched = true;
            } else if (targets.isEnabled("Кастом. Меч") && isCustomSword(entity)) {
                matched = true;
            }
            if (!matched) continue;

            double dist = eye.squaredDistanceTo(point);
            if (dist < bestDist * bestDist) {
                bestDist = Math.sqrt(dist);
                best = entity;
            }
        }
        return best;
    }

    private boolean isGhastTear(Entity entity) {
        if (!(entity instanceof ItemEntity item)) return false;
        return item.getStack().isOf(Items.GHAST_TEAR);
    }

    private boolean isPlayerHead(Entity entity) {
        if (!(entity instanceof ItemEntity item)) return false;
        return item.getStack().isOf(Items.PLAYER_HEAD);
    }

    private boolean isCustomSword(Entity entity) {
        if (!(entity instanceof ItemEntity item)) return false;
        ItemStack stack = item.getStack();
        if (!(stack.getItem() instanceof SwordItem)) return false;
        if (sharpnessEntry == null) return false;
        return EnchantmentHelper.getLevel(sharpnessEntry, stack) >= 6;
    }

    private Vec3d getAimPoint(Entity entity) {
        if (entity instanceof PlayerEntity player) {
            return player.getPos().add(0, player.getHeight(), 0);
        }
        if (entity instanceof ItemEntity item) {
            return item.getBoundingBox().getCenter();
        }
        return null;
    }
}