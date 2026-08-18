package tech.onetap.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.onetap.Onetap;
import tech.onetap.event.list.EventHUD;
import tech.onetap.module.list.render.Crosshair;
import tech.onetap.module.list.render.hud.Interface;
import tech.onetap.util.render.math.Animation;
import tech.onetap.util.render.math.Easing;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    @Final
    private PlayerListHud playerListHud;

    @Unique
    private final Animation onetap$tabAnimation = new Animation(Easing.BACK_OUT, 300L);
    @Unique
    private boolean onetap$tabShownLastFrame;

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void onRenderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        try {
            Crosshair crosshair = Onetap.getInstance().getModuleStorage().get(Crosshair.class);
            if (crosshair != null && crosshair.shouldHideVanillaCrosshair()) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void onRenderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        try {
            Interface interfaceModule = getInterfaceModule();
            if (interfaceModule != null
                    && interfaceModule.isEnabled()
                    && interfaceModule.isHotbarEnabled()
                    && MinecraftClient.getInstance().player != null
                    && !MinecraftClient.getInstance().player.isSpectator()) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        new EventHUD(context, tickCounter).post();
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true)
    private void onRenderScoreboardSidebar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        try {
            Interface interfaceModule = getInterfaceModule();
            if (interfaceModule != null
                    && interfaceModule.isEnabled()
                    && interfaceModule.isScoreboardElementEnabled()) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "renderPlayerList", at = @At("HEAD"), cancellable = true)
    private void onRenderPlayerList(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ci.cancel();
        if (this.client.world == null) {
            return;
        }

        Scoreboard scoreboard = this.client.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.LIST);
        boolean shouldShow = this.client.options.playerListKey.isPressed()
                && (!this.client.isInSingleplayer()
                || this.client.player != null && this.client.player.networkHandler.getListedPlayerListEntries().size() > 1
                || objective != null);

        this.playerListHud.setVisible(shouldShow);
        if (shouldShow) {
            if (!this.onetap$tabShownLastFrame) {
                this.onetap$tabAnimation.setValue(0f);
            }
            this.onetap$tabAnimation.setEasing(Easing.BACK_OUT);
            this.onetap$tabAnimation.run(1f);
            this.onetap$tabShownLastFrame = true;
        } else {
            this.onetap$tabAnimation.setEasing(Easing.EXPO_IN);
            this.onetap$tabAnimation.run(0f);
            if (this.onetap$tabAnimation.isDone() && this.onetap$tabAnimation.getTargetValue() == 0f) {
                this.onetap$tabShownLastFrame = false;
            }
        }

        if (this.onetap$tabAnimation.getValue() > 1.25f) {
            this.onetap$tabAnimation.setValue(1f);
        }

        float popup = Math.max(0f, this.onetap$tabAnimation.getValue());
        if (popup <= 0.001f) {
            return;
        }

        float centerX = context.getScaledWindowWidth() / 2f;
        float centerY = 18f;
        context.getMatrices().push();
        context.getMatrices().translate(centerX, centerY, 0.0f);
        context.getMatrices().scale(popup, popup, 1.0f);
        context.getMatrices().translate(-centerX, -centerY, 0.0f);

        this.playerListHud.render(context, context.getScaledWindowWidth(), scoreboard, objective);
        context.getMatrices().pop();
    }

    private Interface getInterfaceModule() {
        return Onetap.getInstance().getModuleStorage().get(Interface.class);
    }
}
