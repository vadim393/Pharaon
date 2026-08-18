package tech.onetap.module.list.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import tech.onetap.event.list.EventPlayerSync;
import tech.onetap.event.list.EventPlayerUpdate;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeListSetting;
import tech.onetap.util.math.StopWatch;

@ModuleInformation(moduleName = "Auto Potion", moduleDesc = "Автоматически кидает выбранные бафы", moduleCategory = ModuleCategory.COMBAT)
public class AutoPotion extends Module {
    private final ModeListSetting throwSettings = new ModeListSetting(
            "Кидать", new BooleanSetting("Зелье силы", true), new BooleanSetting("Зелье скорости", true), new BooleanSetting("Зелье огнестойкости", true)
    );
    private final BooleanSetting autoDisable = new BooleanSetting("Выключать после использования", false);
    private final StopWatch timer = new StopWatch();
    private float savedPitch;
    private boolean throwing = false;
    private static AutoPotion staticInstance;

    @Subscribe
    public void onSync(EventPlayerSync e) {
        if (mc.player != null) {
            if (!canThrow()) {
                throwing = false;
            } else {
                savedPitch = mc.player.getPitch();
                mc.player.setPitch(90.0F);
                throwing = true;
            }
        }
    }

    @Subscribe
    public void onUpdate(EventPlayerUpdate e) {
        if (mc.player != null && mc.world != null) {
            if (throwing) {
                if (timer.isReached(500L)) {
                    for (PotionType type : PotionType.values()) {
                        if (type.isSettingEnabled() && !mc.player.hasStatusEffect(type.getEffect())) {
                            int slot = findPotionSlot(type.getEffect());
                            if (slot != -1) {
                                throwPotion(slot);
                                timer.reset();
                                if (autoDisable.getValue()) {
                                    setEnabled(false);
                                }

                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private void throwPotion(int slot) {
        int previousSlot = mc.player.getInventory().selectedSlot;
        if (slot < 9) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
        } else {
            mc.interactionManager.clickSlot(0, slot, previousSlot, SlotActionType.SWAP, mc.player);
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.interactionManager.clickSlot(0, slot, previousSlot, SlotActionType.SWAP, mc.player);
        }
    }

    private int findPotionSlot(RegistryEntry<StatusEffect> effect) {
        for (int i = 0; i < 45; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.SPLASH_POTION) {
                PotionContentsComponent contents = (PotionContentsComponent) stack.get(DataComponentTypes.POTION_CONTENTS);
                if (contents != null) {
                    for (StatusEffectInstance effectInstance : contents.getEffects()) {
                        if (effectInstance.getEffectType().equals(effect)) {
                            return i;
                        }
                    }
                }
            }
        }

        return -1;
    }

    private boolean canThrow() {
        if (mc.player != null && mc.world != null) {
            boolean onGround = mc.player.isOnGround()
                    || mc.world.getBlockState(BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 0.3, mc.player.getZ())).isSolid();
            if (!onGround) {
                return false;
            }

            if (mc.player.isClimbing()) {
                return false;
            }

            if (mc.player.hasVehicle()) {
                return false;
            }

            if (mc.player.getAbilities().flying) {
                return false;
            }

            if (!mc.player.isTouchingWater() && !mc.player.isInLava()) {
                for (PotionType type : PotionType.values()) {
                    if (type.isSettingEnabled() && !mc.player.hasStatusEffect(type.getEffect()) && findPotionSlot(type.getEffect()) != -1) {
                        return true;
                    }
                }

                return false;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void onDisable() {
        throwing = false;
        super.onDisable();
    }

    public AutoPotion() {
        staticInstance = this;
    }

    public static AutoPotion getStaticInstance() {
        return staticInstance;
    }

    private enum PotionType {
        STRENGTH(StatusEffects.STRENGTH, "Зелье силы"),
        SPEED(StatusEffects.SPEED, "Зелье скорости"),
        FIRE_RESISTANCE(StatusEffects.FIRE_RESISTANCE, "Зелье огнестойкости");

        private final RegistryEntry<StatusEffect> effect;
        private final String settingName;

        public boolean isSettingEnabled() {
            return AutoPotion.getStaticInstance().throwSettings.isEnabled(this.settingName);
        }

        public RegistryEntry<StatusEffect> getEffect() {
            return this.effect;
        }

        public String getSettingName() {
            return this.settingName;
        }

        PotionType(final RegistryEntry<StatusEffect> effect, final String settingName) {
            this.effect = effect;
            this.settingName = settingName;
        }
    }
}