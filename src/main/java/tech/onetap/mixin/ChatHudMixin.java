package tech.onetap.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.OrderedText;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.onetap.util.chat.ChatHighlightController;

import java.util.Map;
import java.util.WeakHashMap;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    @Unique
    private static final long CHAT_LINE_ANIMATION_MS = 210L;
    @Unique
    private static final float CHAT_LINE_ANIMATION_OFFSET = 8.0f;
    @Unique
    private static final Map<OrderedText, Long> onetap$lineAnimationTimes = new WeakHashMap<>();

    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "addVisibleMessage", at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V"))
    private void onetap$rememberLineHighlight(ChatHudLine line, CallbackInfo ci, @Local(ordinal = 0) OrderedText orderedText) {
        ChatHighlightController.rememberVisibleLine(orderedText, line.content());
        if (orderedText == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (this.client.inGameHud != null && this.client.inGameHud.getTicks() - line.creationTick() <= 1) {
            onetap$lineAnimationTimes.put(orderedText, now);
            return;
        }

        onetap$lineAnimationTimes.put(orderedText, now - CHAT_LINE_ANIMATION_MS);
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V", ordinal = 0), index = 4)
    private int onetap$replaceChatBackground(int originalColor, @Local(ordinal = 0) ChatHudLine.Visible visibleLine) {
        int highlightedColor = ChatHighlightController.getBackgroundColor(visibleLine.content(), originalColor);
        return onetap$scaleAlpha(highlightedColor, onetap$getLineAnimationProgress(visibleLine == null ? null : visibleLine.content()));
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I", ordinal = 0), index = 4)
    private int onetap$animateChatTextColor(int originalColor, @Local(ordinal = 0) ChatHudLine.Visible visibleLine) {
        return onetap$scaleAlpha(originalColor, onetap$getLineAnimationProgress(visibleLine == null ? null : visibleLine.content()));
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V", ordinal = 0))
    private void onetap$pushChatLineAnimation(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci, @Local(ordinal = 0) ChatHudLine.Visible visibleLine) {
        float slide = (1.0f - onetap$getLineAnimationProgress(visibleLine == null ? null : visibleLine.content())) * CHAT_LINE_ANIMATION_OFFSET;
        if (slide <= 0.01f) {
            return;
        }

        context.getMatrices().push();
        context.getMatrices().translate(-slide, 0.0f, 0.0f);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V", ordinal = 0, shift = At.Shift.AFTER))
    private void onetap$popChatLineAnimation(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci, @Local(ordinal = 0) ChatHudLine.Visible visibleLine) {
        float slide = (1.0f - onetap$getLineAnimationProgress(visibleLine == null ? null : visibleLine.content())) * CHAT_LINE_ANIMATION_OFFSET;
        if (slide <= 0.01f) {
            return;
        }

        context.getMatrices().pop();
    }

    @Unique
    private float onetap$getLineAnimationProgress(OrderedText orderedText) {
        if (orderedText == null) {
            return 1.0f;
        }

        Long startedAt = onetap$lineAnimationTimes.get(orderedText);
        if (startedAt == null) {
            return 1.0f;
        }

        float progress = MathHelper.clamp((System.currentTimeMillis() - startedAt) / (float) CHAT_LINE_ANIMATION_MS, 0.0f, 1.0f);
        return 1.0f - (float) Math.pow(1.0f - progress, 3.0f);
    }

    @Unique
    private int onetap$scaleAlpha(int color, float factor) {
        int alpha = color >>> 24;
        int scaledAlpha = MathHelper.clamp((int) (alpha * factor), 0, 255);
        return (color & 0x00FFFFFF) | (scaledAlpha << 24);
    }
}
