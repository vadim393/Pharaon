package tech.onetap.module.list.movement;

import com.google.common.eventbus.Subscribe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import tech.onetap.event.list.EventCloseInv;
import tech.onetap.event.list.EventPacket;
import tech.onetap.event.list.EventPlayerUpdate;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.base.Instance;
import tech.onetap.util.packet.NetworkUtils;
import tech.onetap.util.player.move.MoveUtil;
import tech.onetap.util.player.other.SlownessManager;

@ModuleInformation(moduleName = "Gui Move", moduleDesc = "Позволяет взаимодействовать с инвентарем при движении", moduleCategory = ModuleCategory.MOVEMENT)
public class GuiMove extends Module {
   private final BooleanSetting universal = new BooleanSetting("Универсальный", false);
   private final SliderSetting slownessDuration = new SliderSetting("Длительность замедления", 50.0, 1.0, 400.0, 1.0).setVisible(this.universal::getValue);
   private final List<Packet<?>> packets = new ArrayList<>();
   private boolean wasSprinting = false;

   @Override
   public void onDisable() {
      super.onDisable();
      this.packets.clear();
      this.wasSprinting = false;
   }

   @Subscribe
   public void onPacket(EventPacket e) {
      if (this.universal.getValue()) {
         if (this.mc.currentScreen != null && !(this.mc.currentScreen instanceof ChatScreen)) {
            Packet<?> packet = e.getPacket();
            if (packet instanceof ClickSlotC2SPacket && MoveUtil.hasPlayerMovement() && this.mc.currentScreen instanceof InventoryScreen) {
               this.packets.add(packet);
               e.cancelEvent();
            } else if (packet instanceof CloseHandledScreenC2SPacket && MoveUtil.hasPlayerMovement() && this.mc.player.isSprinting()) {
               this.wasSprinting = true;
               this.packets.add(packet);
               e.cancelEvent();
            }
         }
      }
   }

   @Subscribe
   public void onCloseInv(EventCloseInv e) {
      if (this.universal.getValue()) {
         if (this.mc.currentScreen != null && !(this.mc.currentScreen instanceof ChatScreen)) {
            if (!this.packets.isEmpty()) {
               e.cancelEvent();
               if (this.wasSprinting) {
                  this.mc.player.setSprinting(false);
               }

               SlownessManager.addTask(new SlownessManager.SlowTask(this.slownessDuration.getIntValue(), 0L, () -> {
                  this.packets.forEach(NetworkUtils::sendSilentPacket);
                  this.packets.clear();
                  NetworkUtils.sendSilentPacket(new CloseHandledScreenC2SPacket(this.mc.player.currentScreenHandler.syncId));
                  if (this.wasSprinting) {
                     Sprint sprint = Instance.get(Sprint.class);
                     if (sprint != null && sprint.isEnabled()) {
                        this.mc.player.setSprinting(true);
                     }

                     this.wasSprinting = false;
                  }
               }));
            }
         }
      }
   }

   @Subscribe
   public void onUpdate(EventPlayerUpdate e) {
      if (this.mc.player != null) {
         if (this.mc.currentScreen != null && !(this.mc.currentScreen instanceof ChatScreen)) {
            for (KeyBinding key : new KeyBinding[]{
               this.mc.options.forwardKey, this.mc.options.backKey, this.mc.options.leftKey, this.mc.options.rightKey, this.mc.options.jumpKey
            }) {
               key.setPressed(InputUtil.isKeyPressed(this.mc.getWindow().getHandle(), InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey()).getCode()));
            }
         }
      }
   }
}