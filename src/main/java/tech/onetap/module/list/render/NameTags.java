package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.FishEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.onetap.event.list.EventHUD;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeListSetting;
import tech.onetap.util.friend.FriendRepository;
import tech.onetap.util.render.math.ProjectionUtil;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ModuleInformation(moduleName = "NameTags", moduleDesc = "Отображает теги над сущностями", moduleCategory = ModuleCategory.RENDER)
public class NameTags extends Module {

    public ModeListSetting entityType = new ModeListSetting("Отображать",
            new BooleanSetting("Игроков", true),
            new BooleanSetting("Животных", true),
            new BooleanSetting("Монстров", false),
            new BooleanSetting("Предметов", true));
    public BooleanSetting armor = new BooleanSetting("Отображать броню", true);
    public BooleanSetting showMainHand = new BooleanSetting("Правая рука", true);
    public BooleanSetting showOffHand = new BooleanSetting("Левая рука", true);
    public BooleanSetting showDonate = new BooleanSetting("Донаты", true);

    private static final int DONATE_DEFAULT_COLOR = ColorProvider.rgba(255, 170, 0, 255);

    private static final int DARK_BG = ColorProvider.rgba(10, 10, 12, 150);
    private static final int DARK_BG_FRIEND = ColorProvider.rgba(14, 38, 16, 150);
    private static final int TEXT_WHITE = ColorProvider.rgba(255, 255, 255, 255);
    private static final int TEXT_FRIEND = ColorProvider.rgba(85, 255, 85, 255);
    private static final int HP_LOCAL = ColorProvider.rgba(255, 255, 85, 255);
    private static final int HP_ORANGE = ColorProvider.rgba(255, 170, 0, 255);
    private static final int HP_RED = ColorProvider.rgba(255, 85, 85, 255);
    private static final int HP_GREEN = ColorProvider.rgba(85, 255, 85, 255);

    private final Map<Entity, double[]> entityPositions = new HashMap<>();
    private final Map<String, String> donatePrefixCache = new HashMap<>();
    private final Map<String, Integer> donateColorCache = new HashMap<>();
    private long lastDonateRefresh = 0L;

    @Subscribe
    private void onRender(EventHUD event) {
        if (mc.world == null || mc.player == null) return;

        refreshDonateCache();

        float tickDelta = event.getRenderTickCounter().getTickDelta(true);
        updatePositions(tickDelta);
        if (entityPositions.isEmpty()) return;

        DrawContext ctx = event.getDrawContext();

        for (Map.Entry<Entity, double[]> entry : entityPositions.entrySet()) {
            Entity entity = entry.getKey();
            double[] pos = entry.getValue();
            if (pos == null) continue;

            renderTag(ctx, entity, (float) pos[0], (float) pos[1], 1.0F);
        }
    }

    private void updatePositions(float tickDelta) {
        entityPositions.clear();
        if (mc.world == null || mc.player == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player || !shouldRender(entity)) continue;

            double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
            double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
            double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());

            float heightOffset = entity instanceof ItemEntity
                    ? entity.getHeight() * 0.35F
                    : entity.getHeight() + 0.35F;
            Vector2f headScreen = ProjectionUtil.project(x, y + heightOffset, z);
            if (headScreen.getX() == Float.MAX_VALUE || headScreen.getY() == Float.MAX_VALUE) {
                continue;
            }

            entityPositions.put(entity, new double[]{headScreen.getX(), headScreen.getY(), 0, 0});
        }
    }

    private boolean shouldRender(Entity entity) {
        if (entity instanceof PlayerEntity) return entityType.isEnabled("Игроков");
        if (entity instanceof ItemEntity) return entityType.isEnabled("Предметов");
        if (entity instanceof HostileEntity || entity instanceof AmbientEntity) return entityType.isEnabled("Монстров");
        if (entity instanceof PassiveEntity || entity instanceof FishEntity) return entityType.isEnabled("Животных");
        return false;
    }

    private void renderTag(DrawContext ctx, Entity entity, float x, float y, float scale) {
        if (entity instanceof ItemEntity itemEntity) {
            renderItemTag(ctx, itemEntity, x, y, scale);
        } else if (entity instanceof PlayerEntity player) {
            renderPlayerTag(ctx, player, x, y, scale);
        } else if (entity instanceof LivingEntity living) {
            renderLivingTag(ctx, living, x, y, scale);
        }
    }

    private void drawBackground(MatrixStack matrices, float x, float y, float width, float height, boolean friend) {
        int color = friend ? DARK_BG_FRIEND : DARK_BG;
        DrawUtil.drawRect(matrices, x, y, width, height, color);
    }

    private void refreshDonateCache() {
        long now = System.currentTimeMillis();
        if (now - lastDonateRefresh < 1000L) return;
        lastDonateRefresh = now;
        donatePrefixCache.clear();
        donateColorCache.clear();

        if (!isDonateServer()) return;
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler == null) return;

        for (PlayerListEntry entry : handler.getPlayerList()) {
            Text displayName = entry.getDisplayName();
            if (displayName == null) continue;
            String profileName = entry.getProfile().getName();
            if (profileName == null || profileName.isEmpty()) continue;

            String raw = displayName.getString();
            int idx = raw.toLowerCase(Locale.ROOT).indexOf(profileName.toLowerCase(Locale.ROOT));
            if (idx <= 0) continue;

            String prefixRaw = raw.substring(0, idx);
            String clean = prefixRaw.replaceAll("\u00A7[0-9a-fk-or]", "").trim();
            if (clean.isEmpty()) continue;

            donatePrefixCache.put(profileName.toLowerCase(Locale.ROOT), clean);
            donateColorCache.put(profileName.toLowerCase(Locale.ROOT), getDonateColor(prefixRaw));
        }
    }

    private boolean isDonateServer() {
        if (mc.getCurrentServerEntry() == null || mc.getCurrentServerEntry().address == null) return false;
        String address = mc.getCurrentServerEntry().address;
        return address.contains("funtime") || address.contains("rwdonat") || address.contains("reallyworld");
    }

    private int getDonateColor(String raw) {
        for (int i = raw.length() - 2; i >= 0; i--) {
            if (raw.charAt(i) == '\u00A7') {
                int color = parseDonateColorCode(raw.charAt(i + 1));
                if (color != 0) return color;
            }
        }
        return DONATE_DEFAULT_COLOR;
    }

    private int parseDonateColorCode(char code) {
        switch (code) {
            case '0': return 0xFF000000;
            case '1': return 0xFF0000AA;
            case '2': return 0xFF00AA00;
            case '3': return 0xFF00AAAA;
            case '4': return 0xFFAA0000;
            case '5': return 0xFFAA00AA;
            case '6': return 0xFFFFAA00;
            case '7': return 0xFFAAAAAA;
            case '8': return 0xFF555555;
            case '9': return 0xFF5555FF;
            case 'a': return 0xFF55FF55;
            case 'b': return 0xFF55FFFF;
            case 'c': return 0xFFFF5555;
            case 'd': return 0xFFFF55FF;
            case 'e': return 0xFFFFFF55;
            case 'f': return 0xFFFFFFFF;
            default: return 0;
        }
    }

    private void renderPlayerTag(DrawContext ctx, PlayerEntity entity, float x, float y, float scale) {
        boolean friend = FriendRepository.isFriend(entity.getName().getString());
        String name = entity.getName().getString();
        float hp = entity.getHealth();
        String hpText = "[" + (int) hp + "]";
        String friendTag = friend ? "[F] " : "";
        String donatePrefix = null;
        int donateColor = DONATE_DEFAULT_COLOR;
        if (showDonate.getValue()) {
            String key = name.toLowerCase(Locale.ROOT);
            donatePrefix = donatePrefixCache.get(key);
            donateColor = donateColorCache.getOrDefault(key, donateColor);
        }

        float fontSize = 8.5F * scale;
        float donateWidth = donatePrefix != null ? Fonts.SFREGULAR.get().getWidth(donatePrefix, fontSize) + 2.0F * scale : 0.0F;
        float friendTagWidth = friend ? Fonts.SFMEDIUM.get().getWidth(friendTag, fontSize) : 0.0F;
        float nameWidth = Fonts.SFMEDIUM.get().getWidth(name, fontSize);
        float hpWidth = Fonts.SFREGULAR.get().getWidth(hpText, fontSize);
        float totalWidth = donateWidth + friendTagWidth + nameWidth + hpWidth + 2.0F * scale;
        float headSize = 12.0F * scale;

        float width = totalWidth + headSize + 6.0F * scale;
        float height = Math.max(14.0F * scale, headSize + 4.0F * scale);

        float bgX = x - width / 2.0F;
        float bgY = y - height / 2.0F;

        drawBackground(ctx.getMatrices(), bgX, bgY, width, height, friend);

        float iconX = bgX + 3.0F * scale;
        float iconY = bgY + (height - headSize) / 2.0F;
        drawPlayerHead(ctx, entity, iconX, iconY, headSize, friend ? TEXT_FRIEND : TEXT_WHITE);

        float textX = iconX + headSize + 2.0F;
        float textY = bgY + (height - fontSize) / 2.0F - 2.0F;

        if (donatePrefix != null) {
            DrawUtil.drawText(Fonts.SFREGULAR.get(), donatePrefix, textX, textY, friend ? TEXT_FRIEND : donateColor, fontSize);
            textX += donateWidth;
        }

        if (friend) {
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), friendTag, textX, textY, TEXT_FRIEND, fontSize);
            textX += friendTagWidth;
        }

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), name, textX, textY, friend ? TEXT_FRIEND : TEXT_WHITE, fontSize);
        textX += nameWidth + 2.0F * scale;
        DrawUtil.drawText(Fonts.SFREGULAR.get(), hpText, textX, textY, friend ? TEXT_FRIEND : getHealthColor(hp, entity.getMaxHealth()), fontSize);

        if (armor.getValue()) {
            drawArmor(ctx, entity, x, bgY - 14.0F * scale, 1.0F);
        }
    }

    private void renderLivingTag(DrawContext ctx, LivingEntity entity, float x, float y, float scale) {
        String name = entity.getDisplayName().getString();
        float hp = entity.getHealth();
        String hpText = "[" + (int) hp + "]";

        float fontSize = 8.5F * scale;
        float nameWidth = Fonts.SFMEDIUM.get().getWidth(name, fontSize);
        float hpWidth = Fonts.SFREGULAR.get().getWidth(hpText, fontSize);
        float totalWidth = nameWidth + hpWidth + 2.0F * scale;
        float width = totalWidth + 6.0F * scale;
        float height = 14.0F * scale;

        float bgX = x - width / 2.0F;
        float bgY = y - height / 2.0F;

        drawBackground(ctx.getMatrices(), bgX, bgY, width, height, false);

        float textX = bgX + 3.0F * scale;
        float textY = bgY + (height - fontSize) / 2.0F - 2.0F;

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), name, textX, textY, TEXT_WHITE, fontSize);
        textX += nameWidth + 2.0F * scale;
        DrawUtil.drawText(Fonts.SFREGULAR.get(), hpText, textX, textY, getHealthColor(hp, entity.getMaxHealth()), fontSize);
    }

    private void renderItemTag(DrawContext ctx, ItemEntity item, float x, float y, float scale) {
        ItemStack stack = item.getStack();
        if (stack.isEmpty()) return;
        String name = stack.getCount() > 1 ? stack.getName().getString() + " x" + stack.getCount() : stack.getName().getString();

        float fontSize = 8.5F * scale;
        float textWidth = Fonts.SFMEDIUM.get().getWidth(name, fontSize);
        float iconSize = 12.0F * scale;

        float width = textWidth + iconSize + 8.0F * scale;
        float height = Math.max(14.0F * scale, iconSize + 4.0F * scale);

        float bgX = x - width / 2.0F;
        float bgY = y - height / 2.0F;

        drawBackground(ctx.getMatrices(), bgX, bgY, width, height, false);

        float iconX = bgX + 3.0F * scale;
        float iconY = bgY + (height - iconSize) / 2.0F;
        drawItemIcon(ctx, stack, iconX, iconY, iconSize);

        float textX = iconX + iconSize + 2.0F;
        float textY = bgY + (height - fontSize) / 2.0F - 2.0F;

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), name, textX, textY, TEXT_WHITE, fontSize);
    }

    private void drawPlayerHead(DrawContext ctx, PlayerEntity player, float x, float y, float size, int color) {
        if (!(player instanceof AbstractClientPlayerEntity clientPlayer)) {
            DrawUtil.drawRound(x, y, size, size, 1.5F * (size / 12.0F), ColorProvider.rgba(45, 45, 45, 255));
            return;
        }
        try {
            net.minecraft.client.util.math.MatrixStack matrices = ctx.getMatrices();
            int texId = mc.getTextureManager().getTexture(clientPlayer.getSkinTextures().texture()).getGlId();
            tech.onetap.util.render.builders.Builder.texture()
                    .size(new tech.onetap.util.render.builders.states.SizeState(size, size))
                    .radius(new tech.onetap.util.render.builders.states.QuadRadiusState(1.5F * (size / 12.0F)))
                    .color(new tech.onetap.util.render.builders.states.QuadColorState(color))
                    .texture(8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, texId)
                    .smoothness(1f).build().render(matrices.peek().getPositionMatrix(), x, y);
        } catch (Exception ignored) {
            DrawUtil.drawRound(x, y, size, size, 1.5F * (size / 12.0F), ColorProvider.rgba(45, 45, 45, 255));
        }
    }

    private void drawItemIcon(DrawContext ctx, ItemStack stack, float x, float y, float size) {
        float ratio = size / 16.0F;
        MatrixStack matrices = ctx.getMatrices();
        matrices.push();
        matrices.translate(x + size / 2.0F, y + size / 2.0F, 0.0F);
        matrices.scale(ratio, ratio, 1.0F);
        matrices.translate(-8.0F, -8.0F, 0.0F);
        ctx.drawItem(stack, 0, 0);
        matrices.pop();
    }

    private int getHealthColor(float health, float maxHealth) {
        float ratio = MathHelper.clamp(health / maxHealth, 0.0F, 1.0F);
        if (ratio > 0.75F) return HP_GREEN;
        if (ratio > 0.5F) return HP_LOCAL;
        return ratio > 0.25F ? HP_ORANGE : HP_RED;
    }

    private void drawArmor(DrawContext ctx, PlayerEntity player, float x, float y, float animation) {
        float boxSizeItem = 10.0F;
        float paddingItem = 1.0F;
        List<ItemStack> armor = player.getInventory().armor;
        List<ItemStack> stacks = new ArrayList<>(6);
        if (showOffHand.getValue()) {
            stacks.add(player.getOffHandStack());
        }
        stacks.add(armor.get(0));
        stacks.add(armor.get(1));
        stacks.add(armor.get(2));
        stacks.add(armor.get(3));
        if (showMainHand.getValue()) {
            stacks.add(player.getMainHandStack());
        }

        stacks.removeIf(ItemStack::isEmpty);
        if (stacks.isEmpty()) return;
        float totalWidth = stacks.size() * (boxSizeItem + paddingItem) - paddingItem;
        float iconX = x - totalWidth / 2.0F;
        float iconY = y;

        MatrixStack matrices = ctx.getMatrices();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (ItemStack stack : stacks) {
            matrices.push();
            matrices.translate(iconX + (boxSizeItem - 9.6F) / 2.0F, iconY + (boxSizeItem - 9.6F) / 2.0F, 0.0F);
            matrices.scale(0.6F * animation, 0.6F * animation, 0.6F * animation);
            ctx.drawItem(stack, 0, 0);
            matrices.pop();
            iconX += boxSizeItem + paddingItem;
        }
        RenderSystem.disableBlend();
    }

    public boolean shouldHideVanillaLabel(net.minecraft.client.render.entity.state.EntityRenderState state) {
        if (!isEnabled()) return false;
        if (state instanceof net.minecraft.client.render.entity.state.PlayerEntityRenderState playerState) {
            return playerState.name != null && (entityType.isEnabled("Игроков") || entityType.isEnabled("Друзей") || entityType.isEnabled("Голые"));
        }
        return false;
    }

    public boolean shouldRenderEntityGradientShader(net.minecraft.client.render.entity.state.PlayerEntityRenderState state) {
        return false;
    }

    public void renderPlayerGradientOverlay(MatrixStack matrices, net.minecraft.client.render.entity.state.PlayerEntityRenderState state,
                                            net.minecraft.client.render.entity.model.EntityModel<?> rawModel, int light) {
    }

    public boolean shouldGlowEntity(PlayerEntity entity) {
        return false;
    }
}
