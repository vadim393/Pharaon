package tech.onetap.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.onetap.util.friend.FriendRepository;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.msdf.MsdfFont;
import tech.onetap.util.render.msdf.MsdfRenderer;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    @Unique
    private static final int TAB_PING_WIDTH = 28;
    @Unique
    private static final int SELF_ROW_COLOR = 0x553E92FF;
    @Unique
    private static final int FRIEND_ROW_COLOR = 0x553FD45F;
    @Unique
    private static final int GOOD_PING_COLOR = 0x67FF77;
    @Unique
    private static final int MEDIUM_PING_COLOR = 0xFFD95E;
    @Unique
    private static final int BAD_PING_COLOR = 0xFF6767;
    @Unique
    private static final int UNKNOWN_PING_COLOR = 0xAFAFAF;

    @Shadow
    @Final
    private MinecraftClient client;

    @ModifyConstant(method = "render", constant = @Constant(intValue = 13))
    private int widenPingColumn(int original) {
        return TAB_PING_WIDTH;
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I"
            )
    )
    private void onRenderPlayerRowHighlight(
            DrawContext context,
            int scaledWindowWidth,
            Scoreboard scoreboard,
            ScoreboardObjective objective,
            CallbackInfo ci,
            @Local(index = 15) int columnWidth,
            @Local(index = 25) int x,
            @Local(index = 26) int y,
            @Local(index = 27) PlayerListEntry entry
    ) {
        Integer color = this.onetap$getRowColor(entry);
        if (color == null) {
            return;
        }

        context.fill(x, y, x + columnWidth, y + 8, color);
    }

    @Inject(method = "renderLatencyIcon", at = @At("HEAD"), cancellable = true)
    private void onRenderLatencyIcon(DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        String pingText = this.onetap$getPingText(entry);
        int color = this.onetap$getPingColor(entry.getLatency()) | 0xFF000000;

        MsdfFont font = Fonts.SFREGULAR.get();
        float size = 6.0f;
        int textX = x + width - (int) Math.ceil(font.getWidth(pingText, size)) - 2;
        MsdfRenderer.renderText(font, pingText, size, color, context.getMatrices().peek().getPositionMatrix(), textX, y + 2.0f, 0);
        ci.cancel();
    }

    @Unique
    private Integer onetap$getRowColor(PlayerListEntry entry) {
        if (entry == null || entry.getProfile() == null || this.client.player == null) {
            return null;
        }

        String playerName = this.client.player.getGameProfile().getName();
        String entryName = entry.getProfile().getName();

        if (entry.getProfile().getId() != null && entry.getProfile().getId().equals(this.client.player.getUuid())) {
            return SELF_ROW_COLOR;
        }
        if (entryName != null && playerName != null && entryName.equalsIgnoreCase(playerName)) {
            return SELF_ROW_COLOR;
        }

        return FriendRepository.isFriend(entryName) ? FRIEND_ROW_COLOR : null;
    }

    @Unique
    private String onetap$getPingText(PlayerListEntry entry) {
        int latency = entry.getLatency();
        if (latency < 0) {
            return "?";
        }

        return latency > 999 ? "999+" : Integer.toString(latency);
    }

    @Unique
    private int onetap$getPingColor(int latency) {
        if (latency < 0) {
            return UNKNOWN_PING_COLOR;
        }
        if (latency > 180) {
            return BAD_PING_COLOR;
        }
        if (latency > 85) {
            return MEDIUM_PING_COLOR;
        }

        return GOOD_PING_COLOR;
    }

}
