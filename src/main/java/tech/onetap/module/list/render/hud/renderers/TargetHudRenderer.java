package tech.onetap.module.list.render.hud.renderers;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import tech.onetap.mixin.IPlayerListHudAccessor;
import tech.onetap.module.list.combat.KillAura;
import tech.onetap.module.list.misc.ScoreboardHealth;
import tech.onetap.module.list.misc.NameProtect;
import tech.onetap.module.list.render.hud.Interface;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.impl.ThemeManager;
import tech.onetap.ui.clickgui.ClickGuiUtil;
import tech.onetap.util.base.Instance;
import tech.onetap.util.draggable.Draggable;
import tech.onetap.util.math.StopWatch;
import tech.onetap.util.render.builders.Builder;
import tech.onetap.util.render.builders.states.QuadColorState;
import tech.onetap.util.render.builders.states.QuadRadiusState;
import tech.onetap.util.render.builders.states.SizeState;
import tech.onetap.util.render.math.Animation;
import tech.onetap.util.render.math.AnimationUtils;
import tech.onetap.util.render.math.Easing;
import tech.onetap.util.render.math.Scissor;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.msdf.MsdfFont;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;
import tech.onetap.util.render.stencil.StencilUtil;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TargetHudRenderer {
    private static final Identifier TARGET_HUD_GLOW_TEXTURE = Identifier.of("mre", "images/glow.png");

    private final Interface owner;
    private final Animation animation = new Animation(Easing.EXPO_OUT, 300);
    private final Animation armorAnim = new Animation(Easing.EXPO_OUT, 300);
    private final Animation hpAnimation = new Animation(Easing.EXPO_OUT, 550);
    private final Animation outdatedHpAnimation = new Animation(Easing.EXPO_OUT, 600);
    private final Animation absorptionAnimation = new Animation(Easing.EXPO_OUT, 550);
    private final Animation secondaryHpAnimation = new Animation(Easing.EXPO_OUT, 2200);
    private LivingEntity lastTarget;
    private float lastHpPercent = -1f;
    private float trailHealthPercent = 1f;
    private float lastHealthPercent = 1f;
    private float lastAbsorptionPercent = 0f;
    private float lastAbsorptionReveal = 0f;
    private float lastHpRaw = -1f;
    private final List<HeadParticle> headParticles = new ArrayList<>();
    private boolean particlesSpawnedThisHit = false;
    private final List<DamageParticle> damageParticles = new ArrayList<>();
    private float exp4DisplayedHealth = 0f;
    private float exp4AnimatedHeight = 36f;
    private long exp4LastUpdateTime = System.currentTimeMillis();

    private final Animation dimaHpAnim = new Animation(Easing.EXPO_OUT, 340);
    private final Animation dimaSecHpAnim = new Animation(Easing.EXPO_OUT, 340);
    private final Animation dimaAbsAnim = new Animation(Easing.EXPO_OUT, 340);
    private int dimaTargetId = Integer.MIN_VALUE;
    private float dimaLastHurtTime = 0f;
    private final List<DimaParticle> dimaParticles = new ArrayList<>();

    public TargetHudRenderer(Interface owner) {
        this.owner = owner;
    }

    public void render(DrawContext context) {
        ModeSetting style = owner.getTargetHudStyleSetting();
        if (style == null) {
            renderClassic(context);
            return;
        }
        if (style.is("DLC")) {
            renderDlc(context);
        } else if (style.is("Pharaon")) {
            renderMoonward(context);
        } else if (style.is("PharaonBETA")) {
            renderMoonwardBeta(context);
        } else if (style.is("Pharaon2")) {
            renderMoonward2(context);
        } else if (style.is("Akrein")) {
            renderAkrein(context);
        } else if (style.is("Exp4.0")) {
            renderExp4_0(context);
        } else if (style.is("CelkalOld")) {
            renderCelkalOld(context);
        } else if (style.is("Wex16.5")) {
            renderWex16(context);
        } else if (style.is("Dima")) {
            renderDima(context);
        } else {
            renderClassic(context);
        }
    }

    private final Animation dlcAnimation = new Animation(Easing.SMOOTH_STEP, 400);
    private final StopWatch dlcStopWatch = new StopWatch();
    private LivingEntity dlcTarget;
    private boolean dlcAllow;
    private float dlcHealthAnimation = 0f;
    private float dlcAbsorptionAnimation = 0f;

    private void renderDlc(DrawContext context) {
        dlcTarget = getDlcTarget(dlcTarget);
        if (dlcTarget == null) {
            return;
        }

        boolean out = !dlcAllow || dlcStopWatch.isReached(1000);
        dlcAnimation.setDuration(out ? 400 : 300);
        dlcAnimation.run(out ? 0f : 1f);
        if (dlcAnimation.getValue() <= 0f) {
            dlcTarget = null;
            return;
        }

        MatrixStack matrix = context.getMatrices();
        String name = dlcTarget.getName().getString();

        Draggable drag = owner.getTargetHUDDrag();
        float posX = drag.getX();
        float posY = drag.getY();

        float pad = 3f;
        float headSize = 30f;
        float gap = 6f;
        float width = 160 / 1.5f;
        float height = 36f;
        drag.setWidth(width);
        drag.setHeight(height);

        float hp = dlcTarget.getHealth();
        float maxHp = dlcTarget.getMaxHealth();

        if (isDlcFunTimeAnarchy() && dlcTarget instanceof PlayerEntity player) {
            ScoreboardObjective objective = owner.mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (objective != null) {
                ReadableScoreboardScore score = owner.mc.world.getScoreboard().getScore(player, objective);
                if (score != null) {
                    hp = score.getScore();
                    maxHp = 20;
                }
            }
        }

        dlcHealthAnimation = ClickGuiUtil.fast(dlcHealthAnimation, MathHelper.clamp(hp / maxHp, 0, 1), 10);
        dlcAbsorptionAnimation = ClickGuiUtil.fast(dlcAbsorptionAnimation, MathHelper.clamp(dlcTarget.getAbsorptionAmount() / maxHp, 0, 1), 10);

        float animationValue = (float) dlcAnimation.getValue();
        float halfAnimationValueRest = (1 - animationValue) / 2f;
        float testX = posX + (width * halfAnimationValueRest);
        float testY = posY + (height * halfAnimationValueRest);
        float testW = width * animationValue;
        float testH = height * animationValue;

        float headX = posX + pad;
        float headY = posY + (height - headSize) / 2f;
        float textX = headX + headSize + gap;
        float contentW = width - (textX - posX) - pad;
        float barY = posY + height - pad - 7f;

        matrix.push();
        dlcSizeAnimation(matrix, posX + (width / 2), posY + (height / 2), animationValue);
        drawDlcStyledRect(posX, posY, width, height);
        float hurtPercent = (dlcTarget.hurtTime - (dlcTarget.hurtTime != 0 ? owner.mc.getRenderTickCounter().getTickDelta(true) : 0.0f)) / 10.0f;
        drawDlcHead(matrix, dlcTarget, headX, headY, headSize, headSize, 4f, 1, hurtPercent);

        Scissor.push();
        Scissor.setFromComponentCoordinates(testX, testY, testW - 6, testH);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), name, matrix.peek().getPositionMatrix(), textX, posY + 4f, -1, 9);
        float hpTextY = posY + 17f;
        int brightGold = ColorProvider.rgba(255, 230, 50, 255);
        DrawUtil.drawText(Fonts.ICONS2.get(), "D", matrix.peek().getPositionMatrix(), textX, hpTextY - 1f, ColorProvider.rgba(255, 255, 255, 255), 7f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), String.valueOf((int) hp), matrix.peek().getPositionMatrix(), textX + 8f, hpTextY, ColorProvider.rgba(255, 255, 255, 255), 8);
        float absorptionHp = dlcTarget.getAbsorptionAmount();
        if (absorptionHp > 0.5f) {
            float hpTextW = Fonts.SFMEDIUM.get().getWidth(String.valueOf((int) hp), 8);
            float goldX = textX + 8f + hpTextW + 4f;
            DrawUtil.drawText(Fonts.ICONS2.get(), "D", matrix.peek().getPositionMatrix(), goldX, hpTextY - 1f, brightGold, 7f);
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), String.valueOf((int) absorptionHp), matrix.peek().getPositionMatrix(), goldX + 8f, hpTextY, brightGold, 8);
        }
        Scissor.unset();
        Scissor.pop();

        DrawUtil.drawRound(textX, barY, contentW * dlcHealthAnimation, 6, new Vector4f(3, 3, 3, 3), ColorProvider.getThemeColor());
        DrawUtil.drawRound(textX, barY, contentW * dlcHealthAnimation, 6, new Vector4f(3, 3, 3, 3), ColorProvider.rgba(0, 0, 0, 165), ColorProvider.rgba(0, 0, 0, 165), ColorProvider.rgba(0, 0, 0, 0), ColorProvider.rgba(0, 0, 0, 0));

        float dlcAbsBarWidth = contentW * dlcAbsorptionAnimation;
        if (dlcAbsBarWidth > 0.5f) {
            DrawUtil.drawRound(textX, barY, dlcAbsBarWidth, 6, new Vector4f(3, 3, 3, 3),
                    ColorProvider.rgba(255, 225, 40, 255),
                    ColorProvider.rgba(255, 255, 160, 255),
                    ColorProvider.rgba(255, 255, 160, 255),
                    ColorProvider.rgba(255, 225, 40, 255));
        }

        if (owner.getShowItemsSetting().getValue()) {
            List<ItemStack> handItems = new ArrayList<>(2);
            if (!dlcTarget.getMainHandStack().isEmpty()) handItems.add(dlcTarget.getMainHandStack());
            if (!dlcTarget.getOffHandStack().isEmpty()) handItems.add(dlcTarget.getOffHandStack());

            List<ItemStack> armorItems = new ArrayList<>(4);
            armorItems.add(dlcTarget.getEquippedStack(EquipmentSlot.HEAD));
            armorItems.add(dlcTarget.getEquippedStack(EquipmentSlot.CHEST));
            armorItems.add(dlcTarget.getEquippedStack(EquipmentSlot.LEGS));
            armorItems.add(dlcTarget.getEquippedStack(EquipmentSlot.FEET));
            armorItems.removeIf(ItemStack::isEmpty);

            int totalCount = handItems.size() + armorItems.size();
            if (totalCount > 0) {
                float itemScale = 0.8f;
                float slotSize = 14f * itemScale;
                float padding = 2f;
                float handGap = 7f;
                float trailingGap = 4f;
                boolean twoGroups = !handItems.isEmpty() && !armorItems.isEmpty();
                float totalWidth = totalCount * slotSize + (totalCount - 1) * padding + (twoGroups ? handGap : 0f) + trailingGap;
                float itemX = posX + (width - totalWidth) / 2f;
                float itemY = posY - slotSize - 3f;
                context.getMatrices().push();
                context.getMatrices().translate(0, 0, 100);
                TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
                for (int i = 0; i < totalCount; i++) {
                    if (twoGroups && i == handItems.size()) {
                        itemX += handGap - padding;
                    }
                    ItemStack stack = i < handItems.size() ? handItems.get(i) : armorItems.get(i - handItems.size());
                    context.getMatrices().push();
                    context.getMatrices().translate(itemX, itemY, 0);
                    context.getMatrices().scale(animationValue * itemScale, animationValue * itemScale, 1f);
                    context.drawItem(stack, 0, 0);
                    context.drawStackOverlay(textRenderer, stack, 0, 0);
                    context.getMatrices().pop();
                    itemX += slotSize + padding;
                }
                context.getMatrices().pop();
            }
        }

        matrix.pop();
    }

    private LivingEntity getDlcTarget(LivingEntity nullTarget) {
        KillAura killAura = Instance.get(KillAura.class);
        LivingEntity auraTarget = killAura != null && killAura.isEnabled() ? killAura.getTarget() : null;
        LivingEntity target = nullTarget;
        if (auraTarget != null) {
            dlcStopWatch.reset();
            dlcAllow = true;
            target = auraTarget;
        } else if (owner.mc.currentScreen instanceof ChatScreen) {
            dlcStopWatch.reset();
            dlcAllow = true;
            target = owner.mc.player;
        } else {
            dlcAllow = false;
        }
        return target;
    }

    private String getDlcTabHeader() {
        try {
            Text header = ((IPlayerListHudAccessor) owner.mc.inGameHud.getPlayerListHud()).onetap$getHeader();
            return header == null ? " " : header.getString().toLowerCase();
        } catch (Exception ignored) {
            return " ";
        }
    }

    private boolean isDlcFunTimeAnarchy() {
        if (owner.mc.getCurrentServerEntry() == null || owner.mc.getCurrentServerEntry().address == null) {
            return false;
        }
        String header = getDlcTabHeader();
        return owner.mc.getCurrentServerEntry().address.contains("funtime")
                && (header.contains("анархия") || header.contains("гриферский"));
    }

    private static void dlcSizeAnimation(MatrixStack matrix, float width, float height, float scale) {
        matrix.translate(width, height, 0);
        matrix.scale(scale, scale, 1f);
        matrix.translate(-width, -height, 0);
    }

    private void drawDlcStyledRect(float x, float y, float width, float height) {
        DrawUtil.drawRound(x, y, width, height, 5f, ColorProvider.rgba(0, 0, 0, 235));
    }

    private void drawDlcHead(MatrixStack matrix, LivingEntity entity, float x, float y, float width, float height, float radius, float alpha, float hurtPercent) {
        Identifier skin;
        if (entity instanceof AbstractClientPlayerEntity player) {
            skin = player.getSkinTextures().texture();
        } else {
            skin = Identifier.of("textures/entity/" + EntityType.getId(entity.getType()).getPath() + ".png");
        }
        int tint = (int) (255 * (1f - hurtPercent));
        int color = ColorProvider.rgba(255, tint, tint, (int) (255 * alpha));
        try {
            int texId = owner.mc.getTextureManager().getTexture(skin).getGlId();
            Builder.texture().size(new SizeState(width, height)).radius(new QuadRadiusState(radius))
                    .color(new QuadColorState(color)).texture(8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, texId)
                    .smoothness(1f).build().render(matrix.peek().getPositionMatrix(), x, y);
        } catch (Exception ignored) {
            DrawUtil.drawRound(x, y, width, height, radius, ColorProvider.rgba(35, 35, 35, (int) (255 * alpha)));
        }
    }

    private void renderPouchAkrein(DrawContext context) {
        LivingEntity target = resolveTarget();
        animateTarget(target);
        float animAlpha = (float) animation.getValue();
        if (animAlpha <= 0.05f || lastTarget == null) return;

        AbstractClientPlayerEntity playerEntity = lastTarget instanceof AbstractClientPlayerEntity p ? p : null;
        Draggable drag = owner.getTargetHUDDrag();
        float x = drag.getX();
        float y = drag.getY();
        float width = 130f;
        float height = 36f;
        int alphaInt = (int) (255 * animAlpha);

        DrawUtil.drawRoundBlur(x, y, width, height, 4f, ColorProvider.rgba(0, 0, 0, alphaInt), 25f);
        Builder.border()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(4f))
                .color(new QuadColorState(ColorProvider.rgba(60, 60, 60, (int) (180 * animAlpha))))
                .thickness(0.7f)
                .smoothness(1f, 0.5f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), x, y);

        float headSize = 26f;
        float headX = x + 4f;
        float headY = y + (height - headSize) / 2f;
        int hurtTint = (int) (255 * (1f - lastTarget.hurtTime / 10f));
        int headColor = ColorProvider.rgba(255, hurtTint, hurtTint, alphaInt);

        if (playerEntity != null) {
            try {
                int texId = owner.mc.getTextureManager().getTexture(playerEntity.getSkinTextures().texture()).getGlId();
                Builder.texture().size(new SizeState(headSize, headSize)).radius(new QuadRadiusState(2.5f))
                        .color(new QuadColorState(headColor)).texture(8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, texId)
                        .smoothness(1f).build().render(context.getMatrices().peek().getPositionMatrix(), headX, headY);
            } catch (Exception ignored) {
                DrawUtil.drawRound(headX, headY, headSize, headSize, 2.5f, ColorProvider.rgba(45, 45, 45, alphaInt));
            }
        } else {
            DrawUtil.drawRound(headX, headY, headSize, headSize, 2.5f, ColorProvider.rgba(45, 45, 45, alphaInt));
        }

        float textX = headX + headSize + 7f;
        NameProtect nameProtect = Instance.get(NameProtect.class);
        String name = nameProtect.isEnabled() ? nameProtect.getCustomName(lastTarget.getName().getString()) : lastTarget.getName().getString();

        Scissor.push();
        Scissor.setFromComponentCoordinates(textX, y, width - (textX - x), height - 2f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), name, textX, y + 6f, PouchHud.TEXT, 8.5f, 0.7f, 0.98f, width - (textX - x) - 4f);
        Scissor.unset();
        Scissor.pop();

        float currentHp = getCurrentHp(lastTarget);
        float maxHealth = Math.max(1f, lastTarget.getMaxHealth());
        float hpPercent = MathHelper.clamp(currentHp / maxHealth, 0f, 1f);

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), String.format(java.util.Locale.US, "HP: %.1f", currentHp),
                textX, y + 17.5f, ColorProvider.rgba(245, 245, 245, alphaInt), 7f);

        float barX = textX;
        float barY = y + height - 7f;
        float barWidth = width - (textX - x) - 6f;
        float barHeight = 2.5f;

        DrawUtil.drawRound(barX, barY, barWidth, barHeight, 1f, ColorProvider.rgba(20, 20, 20, Math.max(8, (int) (80 * animAlpha))));

        hpAnimation.run(barWidth * hpPercent);
        float hpW = (float) hpAnimation.getValue();
        if (hpW > 0.4f) {
            java.awt.Color c = getHealthBarColor(currentHp, maxHealth);
            int left = ColorProvider.rgba((int) (c.getRed() * 0.7f), (int) (c.getGreen() * 0.7f), (int) (c.getBlue() * 0.7f), alphaInt);
            int right = ColorProvider.rgba(c.getRed(), c.getGreen(), c.getBlue(), alphaInt);
            DrawUtil.drawRound(barX, barY, hpW, barHeight, 1f, left, left, right, right);
        }

        drag.setWidth(width);
        drag.setHeight(height);
    }

    private void renderPouchCircle(DrawContext context) {
        LivingEntity target = resolveTarget();
        animateTarget(target);
        float animAlpha = (float) animation.getValue();
        if (animAlpha <= 0.05f || lastTarget == null) return;

        AbstractClientPlayerEntity playerEntity = lastTarget instanceof AbstractClientPlayerEntity p ? p : null;
        Draggable drag = owner.getTargetHUDDrag();
        float x = drag.getX();
        float y = drag.getY();
        float width = 140f;
        float height = 52f;
        int alphaInt = (int) (255 * animAlpha);

        DrawUtil.drawRoundBlur(x, y, width, height, 3f, ColorProvider.rgba(10, 12, 16, alphaInt), 20f);

        float radius = 24f;
        float circleX = x + radius + 14f;
        float circleY = y + (height / 2f);
        float headSize = radius * 2f - 6f;

        // круг здоровья: фон-кольцо + заполнение по HP
        float currentHp = getCurrentHp(lastTarget);
        float maxHealth = Math.max(1f, lastTarget.getMaxHealth());
        float hpPercent = MathHelper.clamp(currentHp / maxHealth, 0f, 1f);
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        java.awt.Color c = getHealthBarColor(currentHp, maxHealth);
        int ringTrack = ColorProvider.setAlpha(PouchHud.BORDER, (int) (alphaInt * 0.5f));
        int ringFill = ColorProvider.rgba(c.getRed(), c.getGreen(), c.getBlue(), alphaInt);
        if (owner.isTargetHudThemeMode()) {
            ringFill = ColorProvider.setAlpha(ColorProvider.getThemeColor(), alphaInt);
        }
        drawPouchRing(matrix, circleX, circleY, radius, 2f, 360f, ringTrack);
        drawPouchRing(matrix, circleX, circleY, radius, 2f, hpPercent * 360f, ringFill);
        int absCol = ColorProvider.rgba(255, 215, 0, alphaInt);
        if (lastTarget.getAbsorptionAmount() > 0f) {
            float absPct = MathHelper.clamp(lastTarget.getAbsorptionAmount() / maxHealth, 0f, 1f);
            drawPouchRing(matrix, circleX, circleY, radius + 2.5f, 1.6f, absPct * 360f, absCol);
        }

        float headSize2 = headSize - 3f;
        float headX = circleX - headSize2 / 2f;
        float headY = circleY - headSize2 / 2f;
        int hurtTint = (int) (255 * (1f - lastTarget.hurtTime / 10f));
        int headColor = ColorProvider.rgba(255, hurtTint, hurtTint, alphaInt);

        DrawUtil.drawRound(headX, headY, headSize2, headSize2, headSize2 / 2f, ColorProvider.rgba(30, 25, 40, alphaInt));
        if (playerEntity != null) {
            try {
                int texId = owner.mc.getTextureManager().getTexture(playerEntity.getSkinTextures().texture()).getGlId();
                Builder.texture().size(new SizeState(headSize2, headSize2)).radius(new QuadRadiusState(headSize2 / 2f))
                        .color(new QuadColorState(headColor)).texture(8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, texId)
                        .smoothness(1f).build().render(context.getMatrices().peek().getPositionMatrix(), headX, headY);
            } catch (Exception ignored) {
            }
        }

        float textX = circleX + radius + 12f;
        NameProtect nameProtect = Instance.get(NameProtect.class);
        String name = nameProtect.isEnabled() ? nameProtect.getCustomName(lastTarget.getName().getString()) : lastTarget.getName().getString();

        Scissor.push();
        Scissor.setFromComponentCoordinates(textX, y, width - (textX - x), height);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), name, textX, y + 12f, ColorProvider.rgba(245, 245, 245, alphaInt), 8.5f, 0.6f, 0.98f, width - (textX - x) - 2f);
        Scissor.unset();
        Scissor.pop();

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), String.format(java.util.Locale.US, "HP: %.1f", currentHp), textX, y + 24f, ColorProvider.rgba(255, 255, 255, alphaInt), 7f);
        float hpW = Fonts.SFMEDIUM.get().getWidth(String.format(java.util.Locale.US, "HP: %.1f", currentHp), 7f);
        if (lastTarget.getAbsorptionAmount() > 0f) {
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), String.format(java.util.Locale.US, " (+%.1f)", lastTarget.getAbsorptionAmount()), textX + hpW, y + 24f, absCol, 7f);
        }
        float distance = owner.mc.player == null ? 0f : owner.mc.player.distanceTo(lastTarget);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), String.format(java.util.Locale.US, "Distance: %.1fm", distance), textX, y + 35f, ColorProvider.rgba(200, 200, 200, (int) (alphaInt * 0.85f)), 6f);

        drag.setWidth(width);
        drag.setHeight(height);
    }

    private void drawPouchRing(Matrix4f matrix, float cx, float cy, float radius, float thickness, float degrees, int color) {
        if (degrees <= 0f) return;
        float start = 90f;
        float end = start + Math.min(360f, degrees);
        int segments = Math.max(8, (int) (degrees / 4f));
        float inner = Math.max(0.01f, radius - thickness / 2f);
        float outer = radius + thickness / 2f;

        DrawUtil.drawSetup();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            float angle = (float) Math.toRadians(start + (end - start) * i / segments);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            builder.vertex(matrix, cx + cos * inner, cy + sin * inner, 0).color(color);
            builder.vertex(matrix, cx + cos * outer, cy + sin * outer, 0).color(color);
        }
        BufferRenderer.drawWithGlobalProgram(builder.end());
        DrawUtil.drawEnd();
    }

    private LivingEntity resolveTarget() {
        return resolveTarget(true);
    }

    private LivingEntity resolveTarget(boolean allowCrosshairTarget) {
        KillAura killAura = Instance.get(KillAura.class);
        boolean chatOpen = owner.mc.currentScreen instanceof ChatScreen;
        if (killAura.isEnabled() && killAura.getTarget() != null && killAura.getTarget().isAlive()) return killAura.getTarget();
        if (allowCrosshairTarget && owner.mc.targetedEntity instanceof LivingEntity living && living.isAlive()) return living;
        if (chatOpen) return owner.mc.player;
        return null;
    }

    private float getCurrentHp(LivingEntity entity) {
        if (entity instanceof PlayerEntity) {
            ScoreboardHealth scoreboardHealth = Instance.get(ScoreboardHealth.class);
            if (scoreboardHealth != null) {
                return scoreboardHealth.getHealth(entity);
            }
        }
        return Math.max(0f, entity.getHealth());
    }

    private float getArmorPiecesPercent(LivingEntity entity) {
        int equippedPieces = 0;
        for (ItemStack stack : entity.getArmorItems()) {
            if (!stack.isEmpty()) equippedPieces++;
        }
        return MathHelper.clamp(equippedPieces / 4f, 0f, 1f);
    }

    private void renderMoonwardBeta(DrawContext context) {
        LivingEntity target = resolveTarget();
        animateTarget(target);
        float animAlpha = (float) animation.getValue();
        if (animAlpha <= 0.05f || lastTarget == null) return;

        AbstractClientPlayerEntity playerEntity = lastTarget instanceof AbstractClientPlayerEntity p ? p : null;
        Draggable drag = owner.getTargetHUDDrag();
        float x = drag.getX();
        float y = drag.getY();
        float width = 118f;
        float height = 30f;
        float radius = 3f;
        int alphaInt = (int) (255 * animAlpha);
        int textColor = ColorProvider.rgba(222, 222, 222, alphaInt);
        int panelColor = ColorProvider.rgba(125, 125, 125, alphaInt);

        float headSize = 22f;
        float headX = x + 4f;
        float headY = y + 4f;
        float headRadius = 3f;
        int headColor = ColorProvider.rgba(255, (int) (255 * (1 - lastTarget.hurtTime / 10f)), (int) (255 * (1 - lastTarget.hurtTime / 10f)), alphaInt);

        NameProtect nameProtect = Instance.get(NameProtect.class);
        String rawName = nameProtect.isEnabled() ? nameProtect.getCustomName(lastTarget.getName().getString()) : lastTarget.getName().getString();
        String name = transliterate(rawName);

        float currentHp = getCurrentHp(lastTarget);
        float absorptionHP = Math.max(0f, lastTarget.getAbsorptionAmount());
        float maxHealth = Math.max(1f, lastTarget.getMaxHealth());
        String hpText = String.format(java.util.Locale.US, "%.1f HP", currentHp);

        float textX = headX + headSize + 5f;
        float textY = y + 6f;
        float contentWidth = width - (textX - x) - 6f;


        DrawUtil.drawRoundBlur(x, y, width, height, radius, panelColor, 45f);

        if (playerEntity != null) {
            try {
                int texId = owner.mc.getTextureManager().getTexture(playerEntity.getSkinTextures().texture()).getGlId();
                Builder.texture().size(new SizeState(headSize, headSize)).radius(new QuadRadiusState(headRadius))
                        .color(new QuadColorState(headColor)).texture(8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, texId)
                        .smoothness(1f).build().render(context.getMatrices().peek().getPositionMatrix(), headX, headY);
            } catch (Exception ignored) {
                DrawUtil.drawRound(headX, headY, headSize, headSize, headRadius, ColorProvider.rgba(35, 35, 35, alphaInt));
            }
        } else {
            DrawUtil.drawRound(headX, headY, headSize, headSize, headRadius, ColorProvider.rgba(35, 35, 35, alphaInt));
        }

        Scissor.push();
        Scissor.setFromComponentCoordinates(textX, y + 4f, contentWidth, 10f);
        DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), name, textX, textY, textColor, 8.6f, 0.72f, 0.98f, contentWidth);
        Scissor.unset();
        Scissor.pop();
        DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), hpText, textX, textY + 9.5f, textColor, 7.7f);

        float barSpacing = 2f;
        float barRectY = y + height + barSpacing;
        float barRectHeight = 8f;
        float barInnerHeight = 4f;
        float barWidth = width;
        float hpPercent = MathHelper.clamp(currentHp / maxHealth, 0f, 1f);

        lastHealthPercent += (hpPercent - lastHealthPercent) * 0.125f;
        trailHealthPercent += (lastHealthPercent - trailHealthPercent) * 0.0025f;

        float hpWidth = (barWidth - 4f) * lastHealthPercent;
        float trailWidth = (barWidth - 4f) * trailHealthPercent;


        DrawUtil.drawRoundBlur(x, barRectY, barWidth, barRectHeight, 2f, panelColor, 45f);

        // Сама полоска
        float barX = x + 2f;
        float barY = barRectY + (barRectHeight - barInnerHeight) / 2f;

        DrawUtil.drawRound(barX, barY, barWidth - 4f, barInnerHeight, 1f, ColorProvider.rgba(40, 40, 40, alphaInt));

        int hpLeft, hpRight;
        if (owner.isTargetHudThemeMode()) {
            hpLeft = ColorProvider.setAlpha(ColorProvider.getThemeColorTwo(), alphaInt);
            hpRight = ColorProvider.setAlpha(ColorProvider.getThemeColor(), alphaInt);
        } else {
            Color hpColor = getHealthBarColor(currentHp, maxHealth);
            hpLeft = ColorProvider.rgba((int) (hpColor.getRed() * 0.7f), (int) (hpColor.getGreen() * 0.7f), (int) (hpColor.getBlue() * 0.7f), alphaInt);
            hpRight = ColorProvider.rgba(hpColor.getRed(), hpColor.getGreen(), hpColor.getBlue(), alphaInt);
        }

        if (trailWidth > hpWidth) {
            DrawUtil.drawRound(barX, barY, trailWidth, barInnerHeight, 1f, ColorProvider.rgba(170, 45, 45, (int) (150 * animAlpha)));
        }
        if (hpWidth > 0.5f) {
            DrawUtil.drawRound(barX, barY, hpWidth, barInnerHeight, 1f, hpLeft, hpLeft, hpRight, hpRight);
        }

        drag.setWidth(width);
        drag.setHeight(height + barSpacing + barRectHeight);
    }

    private void renderAkrein(DrawContext context) {
        LivingEntity target = resolveTarget();
        animateTarget(target);
        float animAlpha = (float) animation.getValue();
        if (animAlpha <= 0.05f || lastTarget == null) return;

        AbstractClientPlayerEntity playerEntity = lastTarget instanceof AbstractClientPlayerEntity p ? p : null;
        Draggable drag = owner.getTargetHUDDrag();
        float x = drag.getX();
        float y = drag.getY();
        float width = 116f;
        float height = 37f;
        float radius = 5f;
        int alphaInt = (int) (255 * animAlpha);
        int themeA = ColorProvider.getThemeColor();
        int themeB = ColorProvider.getThemeColorTwo();

        // свечение позади панели в цвет темы
        Builder.glow()
                .size(new SizeState(width + 22f, height + 22f))
                .radius(new QuadRadiusState(radius))
                .color(new QuadColorState(ColorProvider.setAlpha(themeA, (int) (50 * animAlpha))))
                .glowRadius(8f)
                .softness(2.5f)
                .intensity(1.05f)
                .additive(false)
                .build()
                .render(x - 11f, y - 11f, 0);

        // тёмная основа + тинт темы
        DrawUtil.drawRound(x, y, width, height, radius, ColorProvider.rgba(18, 20, 28, (int) (205 * animAlpha)));
        DrawUtil.drawRound(x, y, width, height, radius,
                ColorProvider.setAlpha(themeA, (int) (16 * animAlpha)),
                ColorProvider.setAlpha(themeB, (int) (16 * animAlpha)),
                ColorProvider.setAlpha(themeB, (int) (8 * animAlpha)),
                ColorProvider.setAlpha(themeA, (int) (8 * animAlpha)));
        // обводка в цвет темы
        Builder.border()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(radius))
                .color(new QuadColorState(ColorProvider.setAlpha(themeB, (int) (150 * animAlpha))))
                .thickness(0.7f)
                .smoothness(1f, 0.5f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), x, y);

        // акцентная полоса слева (градиент темы)
        DrawUtil.drawRound(x + 3f, y + 6f, 2.2f, height - 12f, 1.1f,
                ColorProvider.setAlpha(themeA, alphaInt),
                ColorProvider.setAlpha(themeA, alphaInt),
                ColorProvider.setAlpha(themeB, alphaInt),
                ColorProvider.setAlpha(themeB, alphaInt));

        float headSize = 26f;
        float headX = x + 8f;
        float headY = y + (height - headSize) / 2f;
        float headRadius = 4f;
        int hurtTint = (int) (255 * (1f - lastTarget.hurtTime / 10f));
        int headColor = ColorProvider.rgba(255, hurtTint, hurtTint, alphaInt);

        if (playerEntity != null) {
            try {
                int texId = owner.mc.getTextureManager().getTexture(playerEntity.getSkinTextures().texture()).getGlId();
                Builder.texture().size(new SizeState(headSize, headSize)).radius(new QuadRadiusState(headRadius))
                        .color(new QuadColorState(headColor)).texture(8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, texId)
                        .smoothness(1f).build().render(context.getMatrices().peek().getPositionMatrix(), headX, headY);
            } catch (Exception ignored) {
                DrawUtil.drawRound(headX, headY, headSize, headSize, headRadius, ColorProvider.rgba(35, 38, 48, alphaInt));
            }
        } else {
            DrawUtil.drawRound(headX, headY, headSize, headSize, headRadius, ColorProvider.rgba(35, 38, 48, alphaInt));
        }
        Builder.border()
                .size(new SizeState(headSize, headSize))
                .radius(new QuadRadiusState(headRadius))
                .color(new QuadColorState(ColorProvider.setAlpha(themeB, (int) (120 * animAlpha))))
                .thickness(0.6f)
                .smoothness(1f, 0.5f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), headX, headY);

        float textX = headX + headSize + 6f;
        NameProtect nameProtect = Instance.get(NameProtect.class);
        String name = nameProtect.isEnabled() ? nameProtect.getCustomName(lastTarget.getName().getString()) : lastTarget.getName().getString();
        float currentHp = getCurrentHp(lastTarget);
        float maxHealth = Math.max(1f, lastTarget.getMaxHealth());
        float distance = owner.mc.player == null ? 0f : owner.mc.player.distanceTo(lastTarget);
        float armorPercent = getArmorPiecesPercent(lastTarget);

        int nameColor = ColorProvider.rgba(245, 245, 245, alphaInt);
        int infoColor = ColorProvider.setAlpha(themeB, (int) (235 * animAlpha));
        float textWidth = width - (textX - x) - 5f;

        Scissor.push();
        Scissor.setFromComponentCoordinates(textX, y + 2f, textWidth, height - 8f);
        DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), name, textX, y + 4.5f, nameColor, 8f, 0.72f, 0.98f, textWidth);
        Scissor.unset();
        Scissor.pop();

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), String.format(java.util.Locale.US, "HP: %.1f", currentHp), textX, y + 14.5f, nameColor, 6.5f);
        if (distance > 0.1f) {
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), String.format(java.util.Locale.US, "%.1fm", distance), textX + 45f, y + 14.5f, infoColor, 6.5f);
        }

        float barX = textX;
        float barY = y + height - 6.5f;
        float barWidth = width - (textX - x) - 5f;
        float barHeight = 2.6f;
        int barBackColor = ColorProvider.setAlpha(themeB, (int) (28 * animAlpha));
        DrawUtil.drawRound(barX, barY, barWidth, barHeight, 1f, barBackColor);
        DrawUtil.drawRound(barX, barY + barHeight + 2f, barWidth, 1.4f, 0.7f, barBackColor);

        float hpPercent = MathHelper.clamp(currentHp / maxHealth, 0f, 1f);
        hpAnimation.run(barWidth * hpPercent);
        float hpWidth = (float) hpAnimation.getValue();
        if (hpWidth > 0.1f) {
            int hpLeft = ColorProvider.setAlpha(themeA, alphaInt);
            int hpRight = ColorProvider.setAlpha(themeB, alphaInt);
            if (!owner.isTargetHudThemeMode()) {
                Color hpColor = getHealthBarColor(currentHp, maxHealth);
                hpLeft = ColorProvider.rgba((int) (hpColor.getRed() * 0.7f), (int) (hpColor.getGreen() * 0.7f), (int) (hpColor.getBlue() * 0.7f), alphaInt);
                hpRight = ColorProvider.rgba(hpColor.getRed(), hpColor.getGreen(), hpColor.getBlue(), alphaInt);
            }
            DrawUtil.drawRound(barX, barY, hpWidth, barHeight, 1f, hpLeft, hpLeft, hpRight, hpRight);
        }

        absorptionAnimation.run(barWidth * armorPercent);
        float armorWidth = (float) absorptionAnimation.getValue();
        if (armorWidth > 0.1f) {
            DrawUtil.drawRound(barX, barY + barHeight + 2f, armorWidth, 1.4f, 0.7f,
                    ColorProvider.setAlpha(themeB, (int) (210 * animAlpha)),
                    ColorProvider.setAlpha(themeB, (int) (210 * animAlpha)),
                    ColorProvider.setAlpha(themeA, (int) (220 * animAlpha)),
                    ColorProvider.setAlpha(themeA, (int) (220 * animAlpha)));
        }

        drag.setWidth(width);
        drag.setHeight(height);
    }

    private float celkaHealth = 0f;
    private PlayerEntity celkaTarget = null;

    private void renderCelkalOld(DrawContext context) {
        float posX = owner.getTargetHUDDrag().getX();
        float posY = owner.getTargetHUDDrag().getY();
        owner.getTargetHUDDrag().setWidth(140);
        owner.getTargetHUDDrag().setHeight(36);

        this.celkaTarget = getCelkaTarget(this.celkaTarget);
        float scale = (float) animation.getValue();
        if (scale == 0.0F) celkaTarget = null;
        if (celkaTarget == null) return;

        final String targetName = celkaTarget.getName().getString();
        String substring = targetName.substring(0, Math.min(targetName.length(), 10));

        float healthFromScoreboard = getCelkaHealthFromScoreboard(celkaTarget);
        float targetHealth = healthFromScoreboard >= 0 ? healthFromScoreboard : celkaTarget.getHealth() / celkaTarget.getMaxHealth();
        this.celkaHealth += (targetHealth - this.celkaHealth) * 0.2f;
        this.celkaHealth = MathHelper.clamp(this.celkaHealth, 0, 1);

        context.getMatrices().push();
        context.getMatrices().translate(posX + 70f, posY + 18f, 0f);
        context.getMatrices().scale(scale, scale, 1f);
        context.getMatrices().translate(-(posX + 70f), -(posY + 18f), 0f);
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        int alphaInt = (int) (255 * MathHelper.clamp(scale, 0f, 1f));

        DrawUtil.drawRound(matrix, posX, posY, 126, 36, 0f, ColorProvider.rgba(30, 30, 30, 220));

        float healthBarWidth = celkaHealth * 80;
        int barC0 = ColorProvider.setAlpha(ColorProvider.getThemeColor(), alphaInt);
        int barC1 = ColorProvider.setAlpha(ColorProvider.getThemeColorTwo(), alphaInt);
        int barC2 = ColorProvider.setAlpha(ColorProvider.interpolateColor(ColorProvider.getThemeColor(), ColorProvider.getThemeColorTwo(), 0.35f), alphaInt);
        int barC3 = ColorProvider.setAlpha(ColorProvider.interpolateColor(ColorProvider.getThemeColorTwo(), ColorProvider.getThemeColor(), 0.35f), alphaInt);
        if (healthBarWidth > 0.5f) {
            DrawUtil.drawRound(matrix, posX + 9 + 30, posY + 13, healthBarWidth, 6, 0f, barC0, barC2, barC3, barC1);
        }

        int faceColor = (celkaTarget.hurtTime > 0) ? ColorProvider.rgba(255, 0, 0, 180) : ColorProvider.rgba(255, 255, 255, 255);
        if (celkaTarget instanceof AbstractClientPlayerEntity playerEntity) {
            try {
                int texId = owner.mc.getTextureManager().getTexture(playerEntity.getSkinTextures().texture()).getGlId();
                Builder.texture().size(new SizeState(36, 36)).radius(new QuadRadiusState(0f))
                        .color(new QuadColorState(faceColor)).texture(8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, texId)
                        .smoothness(1f).build().render(matrix, posX, posY);
            } catch (Exception ignored) {
                DrawUtil.drawRound(matrix, posX, posY, 36, 36, 0f, ColorProvider.rgba(45, 45, 45, alphaInt));
            }
        } else {
            DrawUtil.drawRound(matrix, posX, posY, 36, 36, 0f, ColorProvider.rgba(45, 45, 45, alphaInt));
        }

        DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), substring, matrix, posX + 38, posY + 3, ColorProvider.rgba(255, 255, 255, alphaInt), 8.5f);

        drawCelkaCompactEquipment(context, posX + 37, posY + 23);

        context.getMatrices().pop();
    }

    private void drawCelkaCompactEquipment(DrawContext context, float x, float y) {
        List<ItemStack> equipment = new ArrayList<>();
        equipment.add(celkaTarget.getMainHandStack());
        equipment.add(celkaTarget.getOffHandStack());
        for (ItemStack armor : celkaTarget.getArmorItems()) {
            if (!armor.isEmpty()) equipment.add(armor);
        }

        float startX = x;
        float itemSize = 8;
        for (ItemStack stack : equipment) {
            if (!stack.isEmpty()) {
                context.getMatrices().push();
                context.getMatrices().translate(startX, y - 2f, 0f);
                context.getMatrices().scale(0.75f, 0.75f, 1f);
                context.drawItem(stack, 0, 0);
                context.drawStackOverlay(owner.mc.textRenderer, stack, 0, 0);
                context.getMatrices().pop();
            }
            startX += 6 + itemSize;
        }
    }

    private PlayerEntity getCelkaTarget(PlayerEntity nullTarget) {
        PlayerEntity target = nullTarget;

        KillAura killAura = Instance.get(KillAura.class);
        if (killAura.isEnabled() && killAura.getTarget() instanceof PlayerEntity playerTarget) {
            target = playerTarget;
            animation.run(1f);
        } else if (owner.mc.currentScreen instanceof ChatScreen) {
            target = owner.mc.player;
            animation.run(1f);
        } else {
            animation.run(0f);
        }
        return target;
    }

    private float getCelkaHealthFromScoreboard(PlayerEntity player) {
        ScoreboardHealth scoreboardHealth = Instance.get(ScoreboardHealth.class);
        if (scoreboardHealth != null && scoreboardHealth.isEnabled()) {
            float hp = scoreboardHealth.getHealth(player);
            return hp / Math.max(1f, player.getMaxHealth());
        }
        return -1;
    }

    private void renderWex16(DrawContext context) {
        LivingEntity target = resolveTarget();
        animateTarget(target);
        float animAlpha = (float) animation.getValue();
        if (animAlpha <= 0.05f || lastTarget == null) return;

        AbstractClientPlayerEntity playerEntity = lastTarget instanceof AbstractClientPlayerEntity p ? p : null;
        Draggable drag = owner.getTargetHUDDrag();
        float x = drag.getX();
        float y = drag.getY();
        float width = 122f;
        float height = 38f;
        float radius = 2f;
        int alphaInt = (int) (255 * animAlpha);

        int bgColor = ColorProvider.rgba(10, 10, 10, Math.max(8, (int) (130 * animAlpha)));
        DrawUtil.drawRound(x, y, width, height, radius, bgColor);

        Builder.border()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(radius))
                .color(new QuadColorState(ColorProvider.rgba(90, 90, 90, (int) (120 * animAlpha))))
                .thickness(0.5f)
                .smoothness(1f, 0.5f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), x, y);

        float headSize = 22f;
        float headX = x + 3f;
        float headY = y + 3f;
        int hurtTint = (int) (255 * (1f - lastTarget.hurtTime / 10f));
        int headColor = ColorProvider.rgba(255, hurtTint, hurtTint, alphaInt);

        if (playerEntity != null) {
            try {
                int texId = owner.mc.getTextureManager().getTexture(playerEntity.getSkinTextures().texture()).getGlId();
                Builder.texture().size(new SizeState(headSize, headSize)).radius(new QuadRadiusState(2f))
                        .color(new QuadColorState(headColor)).texture(8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, texId)
                        .smoothness(1f).build().render(context.getMatrices().peek().getPositionMatrix(), headX, headY);
            } catch (Exception ignored) {
                DrawUtil.drawRound(headX, headY, headSize, headSize, 2f, ColorProvider.rgba(35, 35, 35, alphaInt));
            }
        } else {
            DrawUtil.drawRound(headX, headY, headSize, headSize, 2f, ColorProvider.rgba(35, 35, 35, alphaInt));
        }

        float textX = headX + headSize + 6f;
        float textY = y + 3f;
        NameProtect nameProtect = Instance.get(NameProtect.class);
        String name = nameProtect.isEnabled() ? nameProtect.getCustomName(lastTarget.getName().getString()) : lastTarget.getName().getString();

        float currentHp = getCurrentHp(lastTarget);
        float maxHealth = Math.max(1f, lastTarget.getMaxHealth());
        float distance = owner.mc.player == null ? 0f : owner.mc.player.distanceTo(lastTarget);

        int textColor = ColorProvider.rgba(245, 245, 245, alphaInt);
        int subColor = ColorProvider.rgba(180, 180, 180, (int) (alphaInt * 0.85f));
        float nameSize = 8.5f;
        float infoSize = 6.5f;

        Scissor.push();
        Scissor.setFromComponentCoordinates(textX, y, width - (textX - x), height - 3f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), name, textX, textY, textColor, nameSize, 0.7f, 0.95f, width - (textX - x) - 4f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), String.format(java.util.Locale.US, "Health: %.1f  Distance: %.1fm", currentHp, distance),
                textX, textY + 11f, subColor, infoSize);
        Scissor.unset();
        Scissor.pop();

        float barX = textX;
        float barY = y + height - 6f;
        float barWidth = width - (textX - x) - 4f;
        float barHeight = 2.5f;
        float hpPercent = MathHelper.clamp(currentHp / maxHealth, 0f, 1f);

        DrawUtil.drawRound(barX, barY, barWidth, barHeight, 1f, ColorProvider.rgba(20, 20, 20, Math.max(8, (int) (80 * animAlpha))));

        hpAnimation.run(barWidth * hpPercent);
        float hpWidth = (float) hpAnimation.getValue();
        if (hpWidth > 0.1f) {
            Color hpColor = getHealthBarColor(currentHp, maxHealth);
            int hpLeft = ColorProvider.rgba((int) (hpColor.getRed() * 0.6f), (int) (hpColor.getGreen() * 0.6f), (int) (hpColor.getBlue() * 0.6f), alphaInt);
            int hpRight = ColorProvider.rgba(hpColor.getRed(), hpColor.getGreen(), hpColor.getBlue(), alphaInt);
            DrawUtil.drawRound(barX, barY, hpWidth, barHeight, 1f, hpLeft, hpLeft, hpRight, hpRight);
        }

        float armorPercent = getArmorPiecesPercent(lastTarget);
        absorptionAnimation.run(barWidth * armorPercent);
        float armorWidth = (float) absorptionAnimation.getValue();
        if (armorWidth > 0.1f && armorWidth < barWidth) {
            DrawUtil.drawRound(barX, barY + barHeight + 1.5f, armorWidth, 1f, 0.5f, ColorProvider.rgba(130, 170, 255, alphaInt));
        }

        drag.setWidth(width);
        drag.setHeight(height);
    }

    private void renderDima(DrawContext context) {
        LivingEntity target = resolveTarget();
        animateTarget(target);
        float animAlpha = (float) animation.getValue();
        if (animAlpha <= 0.05f || lastTarget == null) return;

        Draggable drag = owner.getTargetHUDDrag();
        float x = drag.getX();
        float y = drag.getY();
        float width = 100f;
        float height = 38f;
        float radius = 5.5f;
        int bgA = (int) (255 * animAlpha);

        DrawUtil.drawRoundBlur(x, y, width, height, radius, ColorProvider.rgba(0, 0, 0, bgA), 15f);

        float headSize = 32f;
        float headX = x + 3f;
        float headY = y + 3f;
        float headRadius = 4f;
        renderDimaHead(context, headX, headY, headSize, headRadius, bgA);

        float currentHp = getCurrentHp(lastTarget);
        float absorption = Math.max(0f, lastTarget.getAbsorptionAmount());
        float maxHp = Math.max(1f, lastTarget.getMaxHealth());

        int newId = lastTarget.getId();
        if (newId != dimaTargetId) {
            dimaTargetId = newId;
            dimaHpAnim.setValue(currentHp);
            dimaSecHpAnim.setValue(currentHp);
            dimaAbsAnim.setValue(absorption);
        }
        dimaHpAnim.run(currentHp);
        dimaSecHpAnim.run(currentHp);
        dimaAbsAnim.run(absorption);
        float hp = dimaHpAnim.getValue();
        float secHp = dimaSecHpAnim.getValue();
        float abs = dimaAbsAnim.getValue();

        int whiteCol = ColorProvider.rgba(255, 255, 255, bgA);
        NameProtect nameProtect = Instance.get(NameProtect.class);
        String name = nameProtect.isEnabled() ? nameProtect.getCustomName(lastTarget.getName().getString()) : lastTarget.getName().getString();
        Scissor.push();
        Scissor.setFromComponentCoordinates(x + 37.5f, y + 2.5f, width - 42.5f, 10f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), name, x + 37.5f, y + 4.3f, whiteCol, 7f);
        Scissor.unset();
        Scissor.pop();

        int themeCol = ColorProvider.getThemeColor();
        if (!owner.isTargetHudThemeMode()) {
            Color hpBase = getHealthBarColor(currentHp, maxHp);
            themeCol = ColorProvider.rgba(hpBase.getRed(), hpBase.getGreen(), hpBase.getBlue(), 255);
        }

        drawDimaHpBar(context, x + 37.0f, y + 27.6f, 58f, 4f, 2f, hp, secHp, abs, maxHp, bgA, themeCol);

        float hurtTime = lastTarget.hurtTime > 0 ? Math.min(0.5f, lastTarget.hurtTime / 10f) : 0f;
        if (hurtTime > dimaLastHurtTime) {
            for (int i = 0; i < 5; i++) {
                dimaParticles.add(new DimaParticle(x + 14f, y + 16.5f));
            }
        }
        dimaLastHurtTime = hurtTime;
        tickDimaParticles(x, y, animAlpha);

        drag.setWidth(width);
        drag.setHeight(height);
    }

    private void renderDimaHead(DrawContext context, float headX, float headY, float headSize, float headRadius, int bgA) {
        if (lastTarget instanceof AbstractClientPlayerEntity playerEntity) {
            try {
                int texId = owner.mc.getTextureManager().getTexture(playerEntity.getSkinTextures().texture()).getGlId();
                int color = ColorProvider.rgba(255, 255, 255, bgA);
                Builder.texture().size(new SizeState(headSize, headSize)).radius(new QuadRadiusState(headRadius))
                        .color(new QuadColorState(color)).texture(8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, texId)
                        .smoothness(1f).build().render(context.getMatrices().peek().getPositionMatrix(), headX, headY);
                Builder.texture().size(new SizeState(headSize, headSize)).radius(new QuadRadiusState(headRadius))
                        .color(new QuadColorState(color)).texture(40f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, texId)
                        .smoothness(1f).build().render(context.getMatrices().peek().getPositionMatrix(), headX, headY);
            } catch (Exception ignored) {
                DrawUtil.drawRound(headX, headY, headSize, headSize, headRadius, ColorProvider.rgba(30, 25, 40, bgA));
            }
        } else {
            DrawUtil.drawRound(headX, headY, headSize, headSize, headRadius, ColorProvider.rgba(30, 25, 40, bgA));
        }
    }

    private void drawDimaHpBar(DrawContext context, float barX, float barY, float barW, float barH, float radius,
                               float hp, float secHp, float absorption, float maxHp, int bgA, int themeCol) {
        float hpPct = Math.min(hp / maxHp, 1f);
        float secPct = Math.min(secHp / maxHp, 1f);

        int inactiveColor = ColorProvider.interpolateColor(themeCol, ColorProvider.rgba(0, 0, 0, 255), 0.7f);
        int bgBarCol = ColorProvider.setAlpha(inactiveColor, (int) (255 * 0.3f));
        DrawUtil.drawRound(barX, barY, barW, barH, radius, bgBarCol);

        if (hpPct > 0.001f) {
            int mainCol = ColorProvider.setAlpha(themeCol, bgA);
            DrawUtil.drawRound(barX, barY, barW * hpPct, barH, radius, mainCol);
        }
        if (secPct > 0.001f && secPct > hpPct) {
            int secCol = ColorProvider.setAlpha(themeCol, (int) (bgA * 0.75f));
            DrawUtil.drawRound(barX, barY, barW * secPct, barH, radius, secCol);
        }
        if (absorption > 0.01f) {
            float absPct = Math.min(absorption / maxHp, 1f);
            if (absPct > 0.001f) {
                DrawUtil.drawRound(barX, barY, barW * absPct, barH, radius, ColorProvider.rgba(255, 210, 0, bgA));
            }
        }

        float minFillEnd = barX + barW * Math.min(2f / maxHp, 1f);
        float fillEndX = Math.max(barX + barW * hpPct, minFillEnd) - 1.5f;
        float arrowSize = 12f;
        float arrowY = barY - arrowSize - 1f;
        float arrowW = Fonts.TARGET.get().getWidth("f", arrowSize);
        DrawUtil.drawText(Fonts.TARGET.get(), "f", fillEndX - arrowW * 0.5f, arrowY, ColorProvider.setAlpha(themeCol, bgA), arrowSize);

        int hpInt = Math.round(hp);
        String hpStr = String.valueOf(hpInt);
        float hpNumW = Fonts.SFMEDIUM.get().getWidth(hpStr, 5.5f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), hpStr, fillEndX - hpNumW * 0.5f, arrowY - 5.5f - 1f + 4.8f, ColorProvider.rgba(255, 255, 255, bgA), 5.5f);
    }

    private void tickDimaParticles(float hudX, float hudY, float animAlpha) {
        dimaParticles.removeIf(p -> System.currentTimeMillis() - p.startTime > p.lifetime);
        int themeCol = ColorProvider.getThemeColor();
        for (DimaParticle p : dimaParticles) {
            p.update(hudX, hudY);
            float sz = 1f - (float) (System.currentTimeMillis() - p.startTime) / (float) p.lifetime;
            float rad = 2.3f;
            int col = ColorProvider.setAlpha(themeCol, (int) (255 * p.progress * sz * animAlpha));
            DrawUtil.drawRound(p.originX - rad, p.originY - rad, rad * 2f, rad * 2f, rad - 1f, col);
        }
    }

    private static class DimaParticle {
        float posX, posY;
        float originX, originY;
        final float velX, velY;
        final long startTime;
        float progress;
        final long lifetime;

        DimaParticle(float x, float y) {
            this.posX = 0f;
            this.posY = 0f;
            this.originX = x;
            this.originY = y;
            this.velX = ThreadLocalRandom.current().nextFloat(-2f, 2f);
            this.velY = ThreadLocalRandom.current().nextFloat(-2f, 2f);
            this.startTime = System.currentTimeMillis();
            this.lifetime = 1250L + ThreadLocalRandom.current().nextLong(750L);
        }

        void update(float hudX, float hudY) {
            progress = Math.min(1f, progress + 0.1f);
            posX += velX * 0.75f;
            posY += velY * 0.75f;
            originX = hudX + 14f + posX;
            originY = hudY + 16.5f + posY;
        }
    }

    private void renderMoonward2(DrawContext context) {
        LivingEntity target = resolveTarget();
        animateTarget(target);
        if (animation.getValue() <= 0.05f || lastTarget == null) return;

        AbstractClientPlayerEntity playerEntity = lastTarget instanceof AbstractClientPlayerEntity p ? p : null;
        float animAlpha = (float) animation.getValue();
        int alphaInt = (int) (255 * animAlpha);
        Draggable drag = owner.getTargetHUDDrag();
        float x = drag.getX();
        float y = drag.getY();
        float width = 125f;
        float height = 38f;
        float radius = 10f;




        DrawUtil.drawRoundBlur(x, y, width, height, radius, ColorProvider.rgba(175, 175, 175, (int)(255 * animAlpha)), 55);

        Builder.border()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(10))
                .color(new QuadColorState(ColorProvider.rgba(222, 222, 222, (int)(155 * animAlpha))))
                .thickness(0.25f)
                .smoothness(1f, 0.5f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), x, y);


        float headSize = 28f;
        float headX = x + 5f;
        float headY = y + 5f;
        float headRadius = 4f;
        
        int headColor = ColorProvider.rgba(255, (int) (255 * (1 - lastTarget.hurtTime / 10f)), (int) (255 * (1 - lastTarget.hurtTime / 10f)), alphaInt);
        
        if (playerEntity != null) {
            try {
                int texId = owner.mc.getTextureManager().getTexture(playerEntity.getSkinTextures().texture()).getGlId();
                Builder.texture().size(new SizeState(headSize, headSize)).radius(new QuadRadiusState(headRadius))
                        .color(new QuadColorState(headColor)).texture(8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, texId)
                        .smoothness(1f).build().render(context.getMatrices().peek().getPositionMatrix(), headX, headY);
            } catch (Exception ignored) { }
        } else {
            DrawUtil.drawRound(headX, headY, headSize, headSize, headRadius, ColorProvider.rgba(50, 50, 50, alphaInt));
        }

        Builder.border()
            .size(new SizeState(headSize, headSize))
            .radius(new QuadRadiusState(headRadius))
            .color(new QuadColorState(ColorProvider.rgba(222, 222, 222, (int)(155 * animAlpha))))
            .thickness(0.25f)
            .smoothness(1f, 0.5f)
            .build()
            .render(context.getMatrices().peek().getPositionMatrix(), headX, headY);

        float textX = headX + headSize + 3f;
        
        NameProtect nameProtect = Instance.get(NameProtect.class);
        String rawName = nameProtect.isEnabled() ? nameProtect.getCustomName(lastTarget.getName().getString()) : lastTarget.getName().getString();
        
        float textY = y + 5f;
        float nameLimit = width - headSize - 16f;
        Scissor.push();
        Scissor.setFromComponentCoordinates(textX, textY + 1.5f, nameLimit, 12f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), rawName, textX, textY + 2.5f, ColorProvider.rgba(255, 255, 255, alphaInt), 8.5f);
        Scissor.unset();
        Scissor.pop();
        

        float currentHp = getCurrentHp(lastTarget);
        float absorptionHP = Math.max(0f, lastTarget.getAbsorptionAmount());
        float maxHealth = Math.max(1f, lastTarget.getMaxHealth());
        
        String hpText = String.format(java.util.Locale.US, "HP: %.1f", currentHp);
        float hpTextX = textX + 0.5f;
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), hpText, hpTextX, textY + 12f, ColorProvider.rgba(255, 255, 255, alphaInt), 7.5f);
        if (absorptionHP > 0f) {
            float hpW = Fonts.SFMEDIUM.get().getWidth(hpText, 7.5f);
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), "  (+" + String.format(java.util.Locale.US, "%.1f", absorptionHP) + ")", hpTextX + hpW, textY + 12f, ColorProvider.rgba(255, 215, 0, alphaInt), 7.5f);
        }

        float armorAlpha = (float) armorAnim.getValue();
        if (armorAlpha > 0.05f) {
            List<ItemStack> handItems = new ArrayList<>(2);
            if (!lastTarget.getMainHandStack().isEmpty()) handItems.add(lastTarget.getMainHandStack());
            if (!lastTarget.getOffHandStack().isEmpty()) handItems.add(lastTarget.getOffHandStack());

            List<ItemStack> armorItems = new ArrayList<>(4);
            armorItems.add(lastTarget.getEquippedStack(EquipmentSlot.HEAD));
            armorItems.add(lastTarget.getEquippedStack(EquipmentSlot.CHEST));
            armorItems.add(lastTarget.getEquippedStack(EquipmentSlot.LEGS));
            armorItems.add(lastTarget.getEquippedStack(EquipmentSlot.FEET));
            armorItems.removeIf(ItemStack::isEmpty);

            int totalCount = handItems.size() + armorItems.size();
            if (totalCount > 0) {
                float itemScale = 0.8f;
                float slotSize = 14f * itemScale;
                float padding = 2f;
                float handGap = 6f;
                boolean twoGroups = !handItems.isEmpty() && !armorItems.isEmpty();
                float totalWidth = totalCount * slotSize + (totalCount - 1) * padding + (twoGroups ? handGap : 0f);
                float itemX = x + (width - totalWidth) / 2f - 20;
                float itemY = y - slotSize - 4f;
                context.getMatrices().push();
                context.getMatrices().translate(0, 0, 100);
                TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
                for (int i = 0; i < totalCount; i++) {
                    if (twoGroups && i == handItems.size()) {
                        itemX += handGap - padding;
                    }
                    ItemStack stack = i < handItems.size() ? handItems.get(i) : armorItems.get(i - handItems.size());
                    context.getMatrices().push();
                    context.getMatrices().translate(itemX, itemY, 0);
                    context.getMatrices().scale(armorAlpha * itemScale, armorAlpha * itemScale, 1f);
                    context.drawItem(stack, 0, 0);
                    context.drawStackOverlay(textRenderer, stack, 0, 0);
                    context.getMatrices().pop();
                    itemX += slotSize + padding;
                }
                context.getMatrices().pop();
            }
        }

        float barX = textX;
        float barY = textY + 22f;
        float barWidth = width - (headSize + 20f);
        float barHeight = 3.75f;
        
        DrawUtil.drawRound(barX, barY, barWidth, barHeight, 2f, ColorProvider.rgba(22, 25, 32, alphaInt));
        
        float hpPercent = MathHelper.clamp(currentHp / maxHealth, 0, 1);
        float absPercent = MathHelper.clamp(absorptionHP / maxHealth, 0, 1);
        hpAnimation.run(barWidth * hpPercent);
        absorptionAnimation.run(barWidth * absPercent);
        float hpWNow = (float) hpAnimation.getValue();
        float absWNow = (float) absorptionAnimation.getValue();

        if (hpWNow > 0.5f) {
            int themeColor1 = ColorProvider.rgba(182, 142, 255, alphaInt);
            int themeColor2 = ColorProvider.rgba(76, 127, 255, alphaInt);
            if (owner.isTargetHudThemeMode()) {
                themeColor1 = ColorProvider.setAlpha(ColorProvider.getThemeColor(), alphaInt);
                themeColor2 = ColorProvider.setAlpha(ColorProvider.getThemeColorTwo(), alphaInt);
            }
            Builder.rectangle().size(new SizeState(hpWNow, barHeight)).radius(new QuadRadiusState(0.5f))
                    .color(new QuadColorState(themeColor1, themeColor1, themeColor2, themeColor2)).build()
                    .render(context.getMatrices().peek().getPositionMatrix(), barX, barY);
        }
        if (absWNow > 0.5f) {
            int absLeft = ColorProvider.rgba(180, 155, 0, (int) (255 * animAlpha));
            int absRight = ColorProvider.rgba(255, 215, 0, alphaInt);
            DrawUtil.drawRound(barX - 0.25f, barY, absWNow, barHeight, 0.5f, absLeft, absLeft, absRight, absRight);
        }

        drag.setWidth(width);
        drag.setHeight(height);
    }

    private void renderExp4_0(DrawContext context) {
        LivingEntity target = resolveTarget();
        animateTarget(target);
        float animAlpha = (float) animation.getValue();
        if (animAlpha <= 0.05f || lastTarget == null) return;

        AbstractClientPlayerEntity playerEntity = lastTarget instanceof AbstractClientPlayerEntity p ? p : null;
        Draggable drag = owner.getTargetHUDDrag();
        float x = drag.getX();
        float y = drag.getY();
        final float width = 118f;
        final float heightFull = 36f;
        final float heightCompact = 26f;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - exp4LastUpdateTime) / 1000.0f;
        exp4LastUpdateTime = currentTime;
        deltaTime = Math.min(deltaTime, 0.1f);

        float absorptionHP = Math.max(0f, lastTarget.getAbsorptionAmount());
        boolean fullLayout = exp4HasItems(lastTarget) || absorptionHP > 0.1f;
        float targetHeight = fullLayout ? heightFull : heightCompact;
        exp4AnimatedHeight += (targetHeight - exp4AnimatedHeight) * exp4LerpFactor(deltaTime, 10f);
        float height = Math.max(exp4AnimatedHeight, 1f);

        int alphaInt = (int) (255 * animAlpha);
        int bgAlpha = (int) (230 * animAlpha);

        DrawUtil.drawRound(x, y, width, height, 6f,
                ColorProvider.rgba(20, 20, 20, bgAlpha),
                ColorProvider.rgba(15, 15, 15, bgAlpha),
                ColorProvider.rgba(20, 20, 20, bgAlpha),
                ColorProvider.rgba(15, 15, 15, bgAlpha));
        owner.drawExp4Border(x, y, width, height, 6f, bgAlpha);

        float faceSize = fullLayout ? 26f : 18f;
        float faceX = x + 5f;
        float faceY = y + (height - faceSize) / 2f;
        float hurtPercent = lastTarget.hurtTime > 0 ? lastTarget.hurtTime / 10.0f : 0.0f;
        int faceColor = ColorProvider.rgba(255, (int) (255 * (1.0f - hurtPercent)), (int) (255 * (1.0f - hurtPercent)), alphaInt);

        if (playerEntity != null) {
            try {
                int texId = owner.mc.getTextureManager().getTexture(playerEntity.getSkinTextures().texture()).getGlId();
                Builder.texture().size(new SizeState(faceSize, faceSize)).radius(new QuadRadiusState(4f))
                        .color(new QuadColorState(faceColor)).texture(8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, texId)
                        .smoothness(1f).build().render(context.getMatrices().peek().getPositionMatrix(), faceX, faceY);
            } catch (Exception ignored) {
                DrawUtil.drawRound(faceX, faceY, faceSize, faceSize, 4f, ColorProvider.rgba(35, 35, 35, alphaInt));
            }
        } else {
            DrawUtil.drawRound(faceX, faceY, faceSize, faceSize, 4f, ColorProvider.rgba(35, 35, 35, alphaInt));
        }

        float currentHp = getCurrentHp(lastTarget);
        float maxHp = lastTarget.getMaxHealth();
        exp4DisplayedHealth += (currentHp - exp4DisplayedHealth) * exp4LerpFactor(deltaTime, 5f);
        float displayedHealth = Math.max(0f, exp4DisplayedHealth);
        float displayedAbsorption = absorptionHP;

        String rawName = lastTarget.getName().getString();
        if (rawName == null || rawName.trim().isEmpty()) rawName = lastTarget.getDisplayName() != null ? lastTarget.getDisplayName().getString() : "";
        String cleanName = rawName.replaceAll("(?i)§[0-9A-FK-OR]", "");
        if (cleanName == null || cleanName.trim().isEmpty()) cleanName = "Target";

        NameProtect nameProtect = Instance.get(NameProtect.class);
        if (nameProtect.isEnabled()) cleanName = nameProtect.getCustomName(cleanName);

        int accentColor = ColorProvider.rgba(130, 140, 255, alphaInt);
        int absorptionColor = ColorProvider.rgba(255, 200, 50, alphaInt);
        int whiteColor = ColorProvider.rgba(255, 255, 255, alphaInt);

        float contentX = x + (fullLayout ? 36f : 28f);
        float nameY = y + (fullLayout ? 17f : 8f);
        float hpXBase = x + width - 6f;

        boolean showAbsorption = absorptionHP > 0.1f || displayedAbsorption > 0.1f;
        float hpLineY = y + 6f;
        if (showAbsorption) {
            // обычное HP, а справа от него — золотые сердца (не прибавляются к HP)
            String hpStr = String.valueOf((int) displayedHealth);
            String absStr = String.valueOf((int) displayedAbsorption);
            float hpW = Fonts.SFBOLD.get().getWidth(hpStr, 6f);
            float absW = Fonts.SFBOLD.get().getWidth(absStr, 6f);
            float heartW = Fonts.ICONS2.get().getWidth("D", 7f);
            float gap = 2f;
            float absNumX = hpXBase - absW;
            float absHeartX = absNumX - heartW - gap;
            float hpNumX = absHeartX - hpW - gap;
            int brightGold = ColorProvider.rgba(255, 225, 30, alphaInt);
            DrawUtil.drawText(Fonts.SFBOLD.get(), hpStr, hpNumX, hpLineY + 0.5f, accentColor, 6f);
            DrawUtil.drawText(Fonts.ICONS2.get(), "D", absHeartX, hpLineY - 0.5f, brightGold, 7f);
            DrawUtil.drawText(Fonts.SFBOLD.get(), absStr, absNumX, hpLineY + 0.5f, brightGold, 6f);
        } else {
            String hpStr = String.format(java.util.Locale.US, "%.1f", displayedHealth);
            float hpWidth = Fonts.SFBOLD.get().getWidth(hpStr, 6f);
            float hpX = hpXBase - hpWidth;
            float hpY = nameY + 0.5f;
            DrawUtil.drawText(Fonts.ICONS2.get(), "D", hpX - 9f, hpY - 0.5f, accentColor, 7f);
            DrawUtil.drawText(Fonts.SFBOLD.get(), hpStr, hpX, nameY + 0.5f, accentColor, 6f);
        }

        float hpWidthSample = Fonts.SFBOLD.get().getWidth("20.0", 6f);
        float hpXSample = hpXBase - hpWidthSample;
        float maxNameWidth = (showAbsorption ? hpXBase : hpXSample) - contentX - 10f;
        float nameWidth = Fonts.SFBOLD.get().getWidth(cleanName, 7f);

        if (nameWidth > maxNameWidth) {
            Scissor.push();
            Scissor.setFromComponentCoordinates(contentX, nameY - 2f, maxNameWidth, 12f);
            DrawUtil.drawText(Fonts.SFBOLD.get(), cleanName, contentX, nameY, whiteColor, 7f, 0.3f, 0.7f, maxNameWidth);
            Scissor.unset();
            Scissor.pop();

            int fadeColor = ColorProvider.rgba(15, 15, 15, bgAlpha);
            int transparentFade = ColorProvider.rgba(15, 15, 15, 0);
            DrawUtil.drawRound(contentX + maxNameWidth - 15f, nameY - 2f, 15f, 12f, 0f,
                    transparentFade, fadeColor, transparentFade, fadeColor);
        } else {
            DrawUtil.drawText(Fonts.SFBOLD.get(), cleanName, contentX, nameY, whiteColor, 7f);
        }

        float barX = contentX;
        float barY = y + (fullLayout ? 29f : 18f);
        float barWidth = width - (fullLayout ? 36f : 28f) - 6f;
        float barHeight = 1.5f;

        DrawUtil.drawRound(barX, barY, barWidth, barHeight, 1f, ColorProvider.rgba(30, 30, 35, (int) (180 * animAlpha)));

        float targetHealth = MathHelper.clamp(currentHp / Math.max(1f, maxHp), 0f, 1f);
        hpAnimation.run(barWidth * Math.min(1.0f, targetHealth));
        float hpBarWidth = (float) hpAnimation.getValue();

        if (hpBarWidth > 0.01f) {
            int color1 = ColorProvider.rgba(130, 140, 255, alphaInt);
            int color2 = ColorProvider.rgba(100, 110, 230, alphaInt);
            DrawUtil.drawRound(barX, barY, hpBarWidth, barHeight, 1f, color1, color1, color2, color2);
        }

        // золотые сердца в начале полоски HP
        if (absorptionHP > 0.1f) {
            float absPct = MathHelper.clamp(absorptionHP / Math.max(1f, maxHp), 0f, 1f);
            float absBarWidth = Math.min(barWidth, absPct * barWidth);
            if (absBarWidth > 0.01f) {
                DrawUtil.drawRound(barX, barY, absBarWidth, barHeight, 1f,
                        ColorProvider.rgba(255, 215, 0, alphaInt),
                        ColorProvider.rgba(255, 250, 100, alphaInt),
                        ColorProvider.rgba(255, 215, 0, alphaInt),
                        ColorProvider.rgba(255, 250, 100, alphaInt));
            }
        }

        if (fullLayout) {
            drawExp4Armor(context, x, y, animAlpha);
        }

        drag.setWidth(width);
        drag.setHeight(height);
    }

    private boolean exp4HasItems(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity player)) return false;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR || slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
                if (!player.getEquippedStack(slot).isEmpty()) return true;
            }
        }
        return false;
    }

    private float exp4LerpFactor(float deltaTime, float speed) {
        return (float) (1.0 - Math.pow(0.001, deltaTime * speed));
    }

    private void drawExp4Armor(DrawContext context, float x, float y, float animAlpha) {
        if (!(lastTarget instanceof PlayerEntity player)) return;

        List<ItemStack> handItems = new ArrayList<>(2);
        if (!player.getMainHandStack().isEmpty()) handItems.add(player.getMainHandStack());
        if (!player.getOffHandStack().isEmpty()) handItems.add(player.getOffHandStack());

        List<ItemStack> armorItems = new ArrayList<>(4);
        armorItems.add(player.getEquippedStack(EquipmentSlot.HEAD));
        armorItems.add(player.getEquippedStack(EquipmentSlot.CHEST));
        armorItems.add(player.getEquippedStack(EquipmentSlot.LEGS));
        armorItems.add(player.getEquippedStack(EquipmentSlot.FEET));
        armorItems.removeIf(ItemStack::isEmpty);

        int totalCount = handItems.size() + armorItems.size();
        if (totalCount == 0) return;

        float iconSize = 6f;
        float spacing = 1.5f;
        float handGap = 4f;
        boolean twoGroups = !handItems.isEmpty() && !armorItems.isEmpty();
        float armorX = x + 36f;
        float armorY = y + 6f;

        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 100);
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        for (int i = 0; i < totalCount; i++) {
            if (twoGroups && i == handItems.size()) {
                armorX += handGap - spacing;
            }
            ItemStack stack = i < handItems.size() ? handItems.get(i) : armorItems.get(i - handItems.size());
            context.getMatrices().push();
            context.getMatrices().translate(armorX + iconSize / 2f, armorY + iconSize / 2f, 0);
            context.getMatrices().scale(0.45f * animAlpha, 0.45f * animAlpha, 1f);
            context.drawItem(stack, 0, 0);
            context.drawStackOverlay(textRenderer, stack, 0, 0);
            context.getMatrices().pop();
            armorX += iconSize + spacing;
        }
        context.getMatrices().pop();
    }

    private void renderClassic(DrawContext context) {
        LivingEntity previousTarget = lastTarget;
        LivingEntity target = resolveTarget();
        animateTarget(target);
        if (animation.getValue() <= 0.05f || lastTarget == null) return;

        AbstractClientPlayerEntity playerEntity = lastTarget instanceof AbstractClientPlayerEntity p ? p : null;
        float anim = (float) animation.getValue();
        int alphaInt = (int) (255 * anim);
        Draggable drag = owner.getTargetHUDDrag();
        float width = 96, height = 37f, x = drag.getX(), y = drag.getY();

        owner.drawBackground(x, y, width, height, 6.25f, alphaInt);

        float headSize = 31.75f, headX = x + 2.5f, headY = y + (height - headSize) / 2f + 0.25f;
        float currentHpRaw = getCurrentHp(lastTarget);
        if (lastHpRaw == -1f || (target != null && previousTarget != target)) {
            lastHpRaw = currentHpRaw;
            headParticles.clear();
        }

        if (lastTarget.hurtTime > 0 && !particlesSpawnedThisHit) {
            float headPX = headX + headSize / 2f;
            float headPY = headY + headSize / 2f;
            for (int i = 0; i < 9; i++) headParticles.add(new HeadParticle(headPX, headPY, ColorProvider.getThemeColor()));
            particlesSpawnedThisHit = true;
        } else if (lastTarget.hurtTime == 0) {
            particlesSpawnedThisHit = false;
        }

        headParticles.removeIf(HeadParticle::isDead);
        for (HeadParticle p : headParticles) {
            p.update();
            float size = p.getLifePct();
            float particleSize = size * 6.0f;
            int color = ColorProvider.setAlpha(p.color, (int) (255 * p.getAlpha() * size * anim * 0.72f));
            DrawUtil.drawRound(p.x, p.y, particleSize, particleSize, particleSize / 2.5f, color);
        }

        float hurtPercent = lastTarget.hurtTime / 10f;
        int headColor = ColorProvider.rgba(255, (int) (255 * (1 - hurtPercent)), (int) (255 * (1 - hurtPercent)), alphaInt);

        if (playerEntity != null) {
            try {
                Identifier skin = playerEntity.getSkinTextures().texture();
                int texId = owner.mc.getTextureManager().getTexture(skin).getGlId();
                Builder.texture().size(new SizeState(headSize, headSize)).radius(new QuadRadiusState(5))
                        .color(new QuadColorState(headColor)).texture(8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, texId)
                        .smoothness(1f).build().render(context.getMatrices().peek().getPositionMatrix(), headX, headY);
            } catch (Exception ignored) { }
        } else {
            DrawUtil.drawText(Fonts.ICONS_NURIK.get(), "N", headX + 1, headY + 8, headColor, 26f);
        }

        float textX = x + 36.5f;
        NameProtect nameProtect = Instance.get(NameProtect.class);
        String name = nameProtect.isEnabled() ? nameProtect.getCustomName(lastTarget.getName().getString()) : lastTarget.getName().getString();
        Scissor.push();
        Scissor.setFromComponentCoordinates(textX, y, width - 42, height);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), name, textX + 1, y + 7, ColorProvider.rgba(255, 255, 255, alphaInt), 8.25f,0.3f, 0.7f,width);
        Scissor.unset();
        Scissor.pop();

        LivingEntity livingEntity = lastTarget;
        float currentHp = getCurrentHp(livingEntity);
        String hpText = String.format(java.util.Locale.US, "HP: %.1f", currentHp);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), hpText, textX + 1, y + 15.5f, ColorProvider.rgba(255, 255, 255, alphaInt), 6.75f);

        float absorption = livingEntity.getAbsorptionAmount();
        if (absorption > 0) {
            String absText = String.format(java.util.Locale.US, "(%.1f)", absorption);
            float offset = Fonts.SFMEDIUM.get().getWidth(hpText, 6.5f) + 3;
            //DrawUtil.drawText(Fonts.SFMEDIUM.get(), absText, textX + offset + 3, y + 15.5f, ColorProvider.rgba(255, 215, 0, alphaInt), 6.5f);
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), absText, textX + offset + 2, y + 15.5f, ColorProvider.rgba(255, 255, 255, alphaInt), 6.5f);

        }

        float barX = textX - 0.25f, barY = y + 25.5f, barWidth = width - 41, barHeight = 7.75f, barRadius = 2f, barSmooth = 1.25f;
        float maxHealth = livingEntity.getMaxHealth();
        float hpPercent = MathHelper.clamp(currentHp / maxHealth, 0, 1);
        hpAnimation.run(barWidth * hpPercent);
        secondaryHpAnimation.run(barWidth * hpPercent);

        if (hpPercent < lastHpPercent) {
            outdatedHpAnimation.run(barWidth * hpPercent);
        } else {
            outdatedHpAnimation.setValue(hpAnimation.getValue());
        }
        lastHpPercent = hpPercent;

        int hpLeftFull, hpRightFull, hpLeftGhost, hpRightGhost;
        if (owner.isTargetHudThemeMode()) {
            int c1 = ColorProvider.getThemeColor();
            int r = (c1 >> 16) & 0xFF;
            int g = (c1 >> 8) & 0xFF;
            int b = c1 & 0xFF;

            hpLeftFull = ColorProvider.rgba((int) (r * 0.55f), (int) (g * 0.55f), (int) (b * 0.55f), alphaInt);
            hpRightFull = ColorProvider.setAlpha(c1, alphaInt);

            hpLeftGhost = ColorProvider.rgba(r, g, b, (int) (110 * anim));
            hpRightGhost = ColorProvider.rgba((int) (r * 0.55f), (int) (g * 0.55f), (int) (b * 0.55f), (int) (110 * anim));
        } else {
            Color baseColor = getHealthBarColor(currentHp, maxHealth);
            int br = baseColor.getRed();
            int bg = baseColor.getGreen();
            int bb = baseColor.getBlue();

            hpLeftFull = ColorProvider.rgba(MathHelper.clamp((int) (br * 0.5f), 0, 255), MathHelper.clamp((int) (bg * 0.5f), 0, 255), MathHelper.clamp((int) (bb * 0.5f), 0, 255), alphaInt);
            hpRightFull = ColorProvider.rgba(br, bg, bb, alphaInt);

            hpLeftGhost = ColorProvider.rgba(br, bg, bb, (int) (110 * anim));
            hpRightGhost = ColorProvider.rgba(MathHelper.clamp((int) (br * 0.5f), 0, 255), MathHelper.clamp((int) (bg * 0.5f), 0, 255), MathHelper.clamp((int) (bb * 0.5f), 0, 255), (int) (110 * anim));
        }

        int backColor;
        if (owner.isTargetHudThemeMode()) {
            int c1 = ColorProvider.getThemeColor();
            int r = (c1 >> 16) & 0xFF;
            int g = (c1 >> 8) & 0xFF;
            int b = c1 & 0xFF;
            backColor = ColorProvider.rgba((int) (r * 0.45f), (int) (g * 0.45f), (int) (b * 0.45f), (int) (120 * anim));
        } else {
            Color baseColor = getHealthBarColor(currentHp, maxHealth);
            backColor = ColorProvider.rgba((int) (baseColor.getRed() * 0.45f), (int) (baseColor.getGreen() * 0.45f), (int) (baseColor.getBlue() * 0.45f), (int) (120 * anim));
        }

        DrawUtil.drawRound(barX, barY, barWidth, barHeight, barRadius,barSmooth, backColor);
        float hpWOld = (float) outdatedHpAnimation.getValue();
//        if (hpWOld > 0.5f) {
//            DrawUtil.drawRound(barX, barY, hpWOld, barHeight, barRadius,barSmooth, hpLeftGhost, hpLeftGhost, hpRightGhost, hpRightGhost);
//        }

        float hpWSecondary = (float) secondaryHpAnimation.getValue();
        if (hpWSecondary > 0.5f) {
            int secColorLeft = ColorProvider.setAlpha(hpLeftFull, (int) (160 * anim));
            int secColorRight = ColorProvider.setAlpha(hpRightFull, (int) (160 * anim));
            //DrawUtil.drawRound(barX, barY, hpWSecondary, barHeight, barRadius,barSmooth, secColorLeft, secColorLeft, secColorRight, secColorRight);
//            DrawUtil.drawRound(barX, barY, hpWSecondary, barHeight, barRadius,barSmooth,
//                    ColorProvider.rgba(0, 0, 0, (int) (58 * anim)),
//                    ColorProvider.rgba(0, 0, 0, (int) (44 * anim)),
//                    ColorProvider.rgba(0, 0, 0, 0),
//                    ColorProvider.rgba(0, 0, 0, 0));
        }

        float hpWNow = (float) hpAnimation.getValue();
        if (hpWNow > 0.5f) {
            DrawUtil.drawRound(barX, barY, hpWNow, barHeight, barRadius,barSmooth, hpLeftFull, hpLeftFull, hpRightFull, hpRightFull);
            DrawUtil.drawRound(barX, barY, hpWNow, barHeight, barRadius,barSmooth,
                    ColorProvider.rgba(0, 0, 0, (int) (95 * anim)),
                    ColorProvider.rgba(0, 0, 0, (int) (66 * anim)),
                    ColorProvider.rgba(0, 0, 0, 0),
                    ColorProvider.rgba(0, 0, 0, 0));
        }

        float absPercent = MathHelper.clamp(livingEntity.getAbsorptionAmount() / maxHealth, 0, 1);
        absorptionAnimation.run(barWidth * absPercent);
        float abWNow = (float) absorptionAnimation.getValue();

        if (abWNow > 0.5f) {
            int absLeftColor = ColorProvider.rgba(66, 55, 8, (int) (255 * anim));
            int absRightColor = ColorProvider.rgba(235, 230, 0, (int) (255 * anim));
            DrawUtil.drawRound(barX - 0.25f, barY - 0.25f, abWNow + 0.5f, barHeight + 0.5f, barRadius,barSmooth, absLeftColor, absLeftColor, absRightColor, absRightColor);
        }

        float armorAlpha = (float) armorAnim.getValue();
        if (armorAlpha > 0.05f) {
            List<ItemStack> items = new ArrayList<>(6);
            items.add(livingEntity.getMainHandStack());
            items.add(livingEntity.getOffHandStack());
            items.add(livingEntity.getEquippedStack(EquipmentSlot.HEAD));
            items.add(livingEntity.getEquippedStack(EquipmentSlot.CHEST));
            items.add(livingEntity.getEquippedStack(EquipmentSlot.LEGS));
            items.add(livingEntity.getEquippedStack(EquipmentSlot.FEET));

            float itemScale = 0.65f;
            float slotSize = 16 * itemScale;
            float handGap = 4f;
            float itemX = x + width - (slotSize * 6) - 5;
            float itemY = y - slotSize - 2;

            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 100);
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            for (int i = 0; i < items.size(); i++) {
                ItemStack stack = items.get(i);
                if (i == 2) {
                    itemX += handGap;
                }
                if (stack.isEmpty()) {
                    itemX += slotSize;
                    continue;
                }
                context.getMatrices().push();
                context.getMatrices().translate(itemX, itemY, 0);
                context.getMatrices().scale(armorAlpha * itemScale, armorAlpha * itemScale, 1f);
                context.drawItem(stack, 0, 0);
                context.drawStackOverlay(textRenderer, stack, 0, 0);
                context.getMatrices().pop();
                itemX += slotSize;
            }
            context.getMatrices().pop();
        }

        drag.setWidth(width);
        drag.setHeight(height);
    }

    private void renderMoonward(DrawContext context) {
        LivingEntity target = resolveTarget();
        animateTarget(target);
        float animAlpha = (float) animation.getValue();
        if (animAlpha <= 0.05f || lastTarget == null) return;

        Draggable drag = owner.getTargetHUDDrag();
        float x = drag.getX();
        float y = drag.getY();
        float width = 105f;
        float height = 36.5f;
        float panelRadius = 6f;

        owner.drawBackground(x, y, width, height, panelRadius, (int) (255 * animAlpha));

        float headSize = 28f;
        float headX = x + width - headSize - 4f;
        float headY = y + (height - headSize) / 2f;
        float headRadius = headSize / 2f;

        renderMoonward$extracted$0(context, headX, headY, headSize, headRadius);
        renderMoonward$extracted$0(context, headSize, headRadius, animAlpha, headX, headY);

        float textX = x + 6f;
        float textY = y + 7f;
        NameProtect nameProtect = Instance.get(NameProtect.class);
        String rawName = nameProtect.isEnabled() ? nameProtect.getCustomName(lastTarget.getName().getString()) : lastTarget.getName().getString();
        String name = transliterate(rawName);
        int textColor = ColorProvider.rgba(222, 222, 222, (int) (255 * animAlpha));
        float rightTextLimit = headX - 3f;

        renderMoonward$extracted$0(textX, y, rightTextLimit, height, name, textY, textColor);

        float currentHp = getCurrentHp(lastTarget);
        float absorptionHP = Math.max(0f, lastTarget.getAbsorptionAmount());
        float maxHealth = Math.max(1f, lastTarget.getMaxHealth());

        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "HP: " + String.format(java.util.Locale.US, "%.1f", currentHp), textX, textY + 10f, textColor, 7.5f);
        if (absorptionHP > 0f) {
            int absColor = ColorProvider.rgba(222, 222, 0, (int) (255 * animAlpha));
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), "(+" + String.format(java.util.Locale.US, "%.1f", absorptionHP) + ")", textX + 35f, textY + 10f, absColor, 7.5f);
        }

        renderMoonward$extracted$0 renderMoonward$extracted$0 = renderMoonward$extracted$0(currentHp, absorptionHP, animAlpha);
        float topTextWidth = Fonts.SFMEDIUM.get().getWidth(renderMoonward$extracted$0.topText(), 7.0f);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), renderMoonward$extracted$0.topText(), x + (width / 2f) - (topTextWidth / 2f), y - 22f, renderMoonward$extracted$0.topColor(), 8.0f);

        float barX = textX - 1f;
        float barY = y + 27f;
        float barWidth = width - headSize - 12f;
        float barHeight = 5f;

        renderMoonward$extracted$0(target, currentHp, maxHealth, barWidth, barX, barY, barHeight);
        renderMoonward$extracted$0(context, animAlpha);

        DrawUtil.drawRound(barX, barY, barWidth, barHeight, 1.5f, ColorProvider.rgba(60, 60, 60, (int) (255 * animAlpha)));
        float hpPercent = MathHelper.clamp(currentHp / maxHealth, 0f, 1f);
        float absorptionPercent = MathHelper.clamp(absorptionHP / maxHealth, 0f, 1f);
        lastHealthPercent += (hpPercent - lastHealthPercent) * 0.25f;
        lastAbsorptionPercent += (absorptionPercent - lastAbsorptionPercent) * 0.15f;
        trailHealthPercent += (lastHealthPercent - trailHealthPercent) * 0.008f;

        float hpWidth = barWidth * lastHealthPercent;
        float trailWidth = barWidth * trailHealthPercent;
        float absWidth = barWidth * lastAbsorptionPercent;

        int hpLeft;
        int hpRight;
        if (owner.isTargetHudThemeMode()) {
            hpRight = ColorProvider.setAlpha(ColorProvider.getThemeColor(), (int) (255 * animAlpha));
            hpLeft = ColorProvider.setAlpha(ColorProvider.getThemeColorTwo(), (int) (255 * animAlpha));
        } else {
            Color hpCol = getHealthBarColor(currentHp, maxHealth);
            hpLeft = ColorProvider.rgba((int) (hpCol.getRed() * 0.5), (int) (hpCol.getGreen() * 0.5), (int) (hpCol.getBlue() * 0.5), (int) (255 * animAlpha));
            hpRight = ColorProvider.rgba(hpCol.getRed(), hpCol.getGreen(), hpCol.getBlue(), (int) (255 * animAlpha));
        }

        if (trailWidth > hpWidth) {
            DrawUtil.drawRound(barX, barY, trailWidth, barHeight, 1.5f, ColorProvider.setAlpha(ColorProvider.getThemeColor(), (int) (135 * animAlpha)));
        }
        if (hpWidth > 0) {
            DrawUtil.drawRound(barX, barY, hpWidth, barHeight, 1.5f, hpLeft, hpLeft, hpRight, hpRight);
        }
        if (absWidth > 0) {
            int absBase = ColorProvider.rgba(255, 222, 0, (int) (255 * animAlpha));
            int absLeft = ColorProvider.rgba(180, 155, 0, (int) (255 * animAlpha));
            DrawUtil.drawRound(barX, barY, absWidth, barHeight, 1.5f, absLeft, absLeft, absBase, absBase);
        }

        float armorAlpha = (float) armorAnim.getValue();
        if (armorAlpha > 0.05f) {
            items$extracted$0(context, x, width, y, armorAlpha);
        }

        drag.setWidth(width);
        drag.setHeight(height);
    }

    private static void renderMoonward$extracted$0(float textX, float y, float rightTextLimit, float height, String name, float textY, int textColor) {
        Scissor.push();
        Scissor.setFromComponentCoordinates(textX, y, rightTextLimit - textX, height);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), name, textX, textY - 2f, textColor, 8.25f);
        Scissor.unset();
        Scissor.pop();
    }

    private static void renderMoonward$extracted$0(DrawContext context, float headSize, float headRadius, float animAlpha, float headX, float headY) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        DiffuseLighting.disableGuiDepthLighting();

        Builder.border()
                .size(new SizeState(headSize + 1.5f, headSize + 1.5f))
                .radius(new QuadRadiusState(headRadius))
                .color(new QuadColorState(ColorProvider.rgba(60, 60, 60, (int) (255 * animAlpha))))
                .thickness(1f)
                .smoothness(1f, 0.5f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), headX - 0.75f, headY - 0.75f);
    }

    private void renderMoonward$extracted$0(DrawContext context, float headX, float headY, float headSize, float headRadius) {
        context.draw();
        tech.onetap.util.render.stencil.StencilUtil.push();
        DrawUtil.drawRound(headX, headY, headSize, headSize, headRadius, -1);
        tech.onetap.util.render.stencil.StencilUtil.read(1);

        float currentAnimScale = (float) armorAnim.getValue();
        float entityScale = (headSize / 1.3f) * currentAnimScale;
        if (entityScale > 0.1f) {
            float entityX = headX + headSize / 2f;
            float entityY = headY + headSize + 15f * currentAnimScale;
            float elytra = lastTarget.isGliding() ? -10f : 0f;
            if (lastTarget.isGliding()) entityY -= 20f * currentAnimScale;
            drawEntity(entityX - elytra, entityY + elytra, entityScale, -33.0F, 0.0F, lastTarget);
        }

        context.draw();
        tech.onetap.util.render.stencil.StencilUtil.pop();
    }

    private void renderMoonward$extracted$0(LivingEntity target, float currentHp, float maxHealth, float barWidth, float barX, float barY, float barHeight) {
        if (lastHpRaw == -1f || lastTarget != target) {
            lastHpRaw = currentHp;
            damageParticles.clear();
        }
        if (currentHp < lastHpRaw) {
            int count = MathHelper.clamp((int) ((lastHpRaw - currentHp) * 4), 10, 25);
            Color pColor = getHealthBarColor(currentHp, maxHealth);
            float lostHpWidth = barWidth * MathHelper.clamp((lastHpRaw - currentHp) / maxHealth, 0f, 1f);
            float currentHpWidth = barWidth * MathHelper.clamp(currentHp / maxHealth, 0f, 1f);
            for (int i = 0; i < count; i++) {
                float spawnX = barX + currentHpWidth + (float) (Math.random() * lostHpWidth);
                float spawnY = barY + barHeight / 2f;
                damageParticles.add(new DamageParticle(spawnX, spawnY, pColor.getRGB()));
            }
            lastHpRaw = currentHp;
        } else if (currentHp > lastHpRaw) {
            lastHpRaw = currentHp;
        }
    }

    private @NotNull TargetHudRenderer.renderMoonward$extracted$0 renderMoonward$extracted$0(float currentHp, float absorptionHP, float animAlpha) {
        float myTotalHp = getCurrentHp(owner.mc.player) + owner.mc.player.getAbsorptionAmount();
        float targetTotalHp = currentHp + absorptionHP;
        float damage = 1.0f;
        ItemStack weapon = owner.mc.player.getMainHandStack();

        if (weapon != null && !weapon.isEmpty()) {
            String itemName = net.minecraft.registry.Registries.ITEM.getId(weapon.getItem()).getPath();
            if (itemName.contains("netherite_sword")) damage += 7.0f;
            else if (itemName.contains("diamond_sword")) damage += 6.0f;
            else if (itemName.contains("iron_sword")) damage += 5.0f;
            else if (itemName.contains("stone_sword")) damage += 4.0f;
            else if (itemName.contains("golden_sword") || itemName.contains("wooden_sword")) damage += 3.0f;
            else if (itemName.contains("netherite_axe")) damage += 9.0f;
            else if (itemName.contains("diamond_axe") || itemName.contains("iron_axe") || itemName.contains("stone_axe")) damage += 8.0f;
            else if (itemName.contains("golden_axe") || itemName.contains("wooden_axe")) damage += 6.0f;
            if (weapon.hasGlint()) damage += 3.0f;
        }

        if (owner.mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.STRENGTH)) {
            damage += 3.0f * (owner.mc.player.getStatusEffect(net.minecraft.entity.effect.StatusEffects.STRENGTH).getAmplifier() + 1);
        }
        if (owner.mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.WEAKNESS)) {
            damage -= 4.0f * (owner.mc.player.getStatusEffect(net.minecraft.entity.effect.StatusEffects.WEAKNESS).getAmplifier() + 1);
        }

        float potentialDamage = damage * 1.5f;
        float targetArmor = lastTarget.getArmor();
        float targetToughness = (float) lastTarget.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.ARMOR_TOUGHNESS);
        float f = 2.0F + targetToughness / 4.0F;
        float g = MathHelper.clamp(targetArmor - potentialDamage / f, targetArmor * 0.2F, 20.0F);
        potentialDamage = potentialDamage * (1.0F - g / 25.0F);

        int epf = 0;
        for (ItemStack armorPiece : lastTarget.getArmorItems()) {
            if (!armorPiece.isEmpty() && armorPiece.hasGlint()) epf += 4;
        }
        epf = Math.min(20, epf);
        if (epf > 0) potentialDamage = potentialDamage * (1.0F - (epf * 0.04F));

        String topText;
        int topColor;
        if (targetTotalHp <= potentialDamage - 1 && targetTotalHp > 0) {
            topText = "ONETAP";
            topColor = ColorProvider.rgba(255, 75, 75, (int) (255 * animAlpha));
        } else {
            topText = myTotalHp >= targetTotalHp ? "WINNING" : "LOSING";
            topColor = ColorProvider.rgba(255, 255, 255, (int) (255 * animAlpha));
        }
        renderMoonward$extracted$0 renderMoonward$extracted$0 = new renderMoonward$extracted$0(topText, topColor);
        return renderMoonward$extracted$0;
    }

    private record renderMoonward$extracted$0(String topText, int topColor) {}

    private void items$extracted$0(DrawContext context, float x, float width, float y, float armorAlpha) {
        List<ItemStack> handItems = new ArrayList<>(2);
        if (!lastTarget.getMainHandStack().isEmpty()) handItems.add(lastTarget.getMainHandStack());
        if (!lastTarget.getOffHandStack().isEmpty()) handItems.add(lastTarget.getOffHandStack());

        List<ItemStack> armorItems = new ArrayList<>(4);
        armorItems.add(lastTarget.getEquippedStack(EquipmentSlot.HEAD));
        armorItems.add(lastTarget.getEquippedStack(EquipmentSlot.CHEST));
        armorItems.add(lastTarget.getEquippedStack(EquipmentSlot.LEGS));
        armorItems.add(lastTarget.getEquippedStack(EquipmentSlot.FEET));
        armorItems.removeIf(ItemStack::isEmpty);

        int totalCount = handItems.size() + armorItems.size();
        if (totalCount == 0) return;

        float itemScale = 0.7f;
        float slotSize = 14f * itemScale;
        float padding = 2f;
        float handGap = 6f;
        boolean twoGroups = !handItems.isEmpty() && !armorItems.isEmpty();
        float totalWidth = totalCount * slotSize + (totalCount - 1) * padding + (twoGroups ? handGap : 0f);
        float itemX = x + (width - totalWidth) / 2f - 14f;
        float itemY = y - slotSize - 2f;

        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 100);
        for (int i = 0; i < totalCount; i++) {
            if (twoGroups && i == handItems.size()) {
                itemX += handGap - padding;
            }
            ItemStack stack = i < handItems.size() ? handItems.get(i) : armorItems.get(i - handItems.size());
            context.getMatrices().push();
            context.getMatrices().translate(itemX, itemY, 0);
            context.getMatrices().scale(armorAlpha * itemScale, armorAlpha * itemScale, 1f);
            context.drawItem(stack, 0, 0);
            context.drawStackOverlay(owner.mc.textRenderer, stack, 0, 0);
            context.getMatrices().pop();
            itemX += slotSize + padding;
        }
        context.getMatrices().pop();
    }

    private void renderMoonward$extracted$0(DrawContext context, float animAlpha) {
        damageParticles.removeIf(p -> p.getAlpha() <= 0);
        if (!damageParticles.isEmpty()) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, TARGET_HUD_GLOW_TEXTURE);

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
            for (DamageParticle p : damageParticles) {
                p.update();
                float pAlpha = p.getAlpha() * animAlpha;
                int c = ColorProvider.setAlpha(p.color, (int) (pAlpha * 255));
                float half = p.getSize() / 2f;
                buffer.vertex(matrix, p.x - half, p.y - half, 0).texture(0, 0).color(c);
                buffer.vertex(matrix, p.x - half, p.y + half, 0).texture(0, 1).color(c);
                buffer.vertex(matrix, p.x + half, p.y + half, 0).texture(1, 1).color(c);
                buffer.vertex(matrix, p.x + half, p.y - half, 0).texture(1, 0).color(c);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableBlend();
        }
    }

    private void animateTarget(LivingEntity target) {
        if (target != null) {
            lastTarget = target;
            animation.run(1);
            armorAnim.run(1);
        } else {
            animation.run(0);
            armorAnim.run(0);
            if (animation.isDone() && animation.getTargetValue() == 0f) {
                lastTarget = null;
            }
        }
    }

    private String transliterate(String text) {
        if (text == null) return "";
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            String replacement = switch (c) {
                case 'а', 'А' -> c == 'А' ? "A" : "a";
                case 'б', 'Б' -> c == 'Б' ? "B" : "b";
                case 'в', 'В' -> c == 'В' ? "V" : "v";
                case 'г', 'Г' -> c == 'Г' ? "G" : "g";
                case 'д', 'Д' -> c == 'Д' ? "D" : "d";
                case 'е', 'Е' -> c == 'Е' ? "E" : "e";
                case 'ё', 'Ё' -> c == 'Ё' ? "Yo" : "yo";
                case 'ж', 'Ж' -> c == 'Ж' ? "Zh" : "zh";
                case 'з', 'З' -> c == 'З' ? "Z" : "z";
                case 'и', 'И' -> c == 'И' ? "I" : "i";
                case 'й', 'Й' -> c == 'Й' ? "Y" : "y";
                case 'к', 'К' -> c == 'К' ? "K" : "k";
                case 'л', 'Л' -> c == 'Л' ? "L" : "l";
                case 'м', 'М' -> c == 'М' ? "M" : "m";
                case 'н', 'Н' -> c == 'Н' ? "N" : "n";
                case 'о', 'О' -> c == 'О' ? "O" : "o";
                case 'п', 'П' -> c == 'П' ? "P" : "p";
                case 'р', 'Р' -> c == 'Р' ? "R" : "r";
                case 'с', 'С' -> c == 'С' ? "S" : "s";
                case 'т', 'Т' -> c == 'Т' ? "T" : "t";
                case 'у', 'У' -> c == 'У' ? "U" : "u";
                case 'ф', 'Ф' -> c == 'Ф' ? "F" : "f";
                case 'х', 'Х' -> c == 'Х' ? "Kh" : "kh";
                case 'ц', 'Ц' -> c == 'Ц' ? "Ts" : "ts";
                case 'ч', 'Ч' -> c == 'Ч' ? "Ch" : "ch";
                case 'ш', 'Ш' -> c == 'Ш' ? "Sh" : "sh";
                case 'щ', 'Щ' -> c == 'Щ' ? "Shch" : "shch";
                case 'ъ', 'Ъ', 'ь', 'Ь' -> "";
                case 'ы', 'Ы' -> c == 'Ы' ? "Y" : "y";
                case 'э', 'Э' -> c == 'Э' ? "E" : "e";
                case 'ю', 'Ю' -> c == 'Ю' ? "Yu" : "yu";
                case 'я', 'Я' -> c == 'Я' ? "Ya" : "ya";
                default -> String.valueOf(c);
            };
            result.append(replacement);
        }
        return result.toString();
    }

    private java.awt.Color getHealthBarColor(float currentHp, float maxHp) {
        float ratio = MathHelper.clamp(currentHp / maxHp, 0.0f, 1.0f);
        java.awt.Color colorAtMax = new java.awt.Color(44, 246, 53);
        java.awt.Color colorAt56  = new java.awt.Color(160, 228, 69);
        java.awt.Color colorAt38  = new java.awt.Color(222, 191, 79);
        java.awt.Color colorAt32  = new java.awt.Color(233, 150, 87);
        java.awt.Color colorAt11  = new java.awt.Color(255, 125, 98);

        if (ratio >= 0.56f) {
            float t = MathHelper.clamp((1.0f - ratio) / (1.0f - 0.56f), 0.0f, 1.0f);
            return lerpColor(colorAtMax, colorAt56, t);
        } else if (ratio >= 0.38f) {
            float t = MathHelper.clamp((0.56f - ratio) / (0.56f - 0.38f), 0.0f, 1.0f);
            return lerpColor(colorAt56, colorAt38, t);
        } else if (ratio >= 0.32f) {
            float t = MathHelper.clamp((0.38f - ratio) / (0.38f - 0.32f), 0.0f, 1.0f);
            return lerpColor(colorAt38, colorAt32, t);
        } else if (ratio >= 0.11f) {
            float t = MathHelper.clamp((0.32f - ratio) / (0.32f - 0.11f), 0.0f, 1.0f);
            return lerpColor(colorAt32, colorAt11, t);
        } else {
            return colorAt11;
        }
    }
    private java.awt.Color lerpColor(java.awt.Color a, java.awt.Color b, float t) {
        return new java.awt.Color(
                (int) (a.getRed() + t * (b.getRed() - a.getRed())),
                (int) (a.getGreen() + t * (b.getGreen() - a.getGreen())),
                (int) (a.getBlue() + t * (b.getBlue() - a.getBlue()))
        );
    }
    public void drawEntity(float x, float y, float scale, float yawAngle, float pitchAngle, LivingEntity entity) {
        MatrixStack matrices = new MatrixStack();
        matrices.push();
        matrices.translate(x, y, 50.0);
        matrices.scale(-scale, scale, scale);
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(yawAngle));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(pitchAngle));

        float bodyYaw = entity.bodyYaw;
        float prevBodyYaw = entity.prevBodyYaw;
        float headYaw = entity.headYaw;
        float prevHeadYaw = entity.prevHeadYaw;
        float yaw = entity.getYaw();
        float prevYaw = entity.prevYaw;
        float pitch = entity.getPitch();
        float prevPitch = entity.prevPitch;

        entity.bodyYaw = 0;
        entity.prevBodyYaw = 0;
        entity.headYaw = 0;
        entity.prevHeadYaw = 0;
        entity.setYaw(0);
        entity.prevYaw = 0;
        entity.setPitch(0);
        entity.prevPitch = 0;

        DiffuseLighting.disableGuiDepthLighting();
        VertexConsumerProvider.Immediate immediate = owner.mc.getBufferBuilders().getEntityVertexConsumers();
        float tickDelta = owner.mc.getRenderTickCounter().getTickDelta(true);
        owner.mc.getEntityRenderDispatcher().render(entity, 0.0, 0.0, 0.0, tickDelta, matrices, immediate, 0x00F000F0);
        immediate.draw();
        DiffuseLighting.enableGuiDepthLighting();

        entity.bodyYaw = bodyYaw;
        entity.prevBodyYaw = prevBodyYaw;
        entity.headYaw = headYaw;
        entity.prevHeadYaw = prevHeadYaw;
        entity.setYaw(yaw);
        entity.prevYaw = prevYaw;
        entity.setPitch(pitch);
        entity.prevPitch = prevPitch;

        matrices.pop();
    }

    private static class HeadParticle {
        float x, y, endX, endY;
        long spawnTime;
        int color;
        float alpha = 0;

        HeadParticle(float startX, float startY, int color) {
            this.x = startX;
            this.y = startY;
            this.endX = startX + (float) (Math.random() * 80.0 - 40.0);
            this.endY = startY + (float) (Math.random() * 80.0 - 40.0);
            this.spawnTime = System.currentTimeMillis();
            this.color = color;
        }

        void update() {
            this.alpha = MathHelper.lerp(this.alpha, 1.0F, 0.1F);
            this.x = MathHelper.lerp(0.01f, this.x, this.endX);
            this.y = MathHelper.lerp(0.01f, this.y, this.endY);
        }

        boolean isDead() {
            return System.currentTimeMillis() - spawnTime > 2500L;
        }

        float getLifePct() {
            return 1.0f - (float) (System.currentTimeMillis() - spawnTime) / 4500.0f;
        }

        float getAlpha() {
            return alpha;
        }
    }

    private static class DamageParticle {
        float x, y;
        float motionX, motionY;
        int color;
        float alpha;
        float size;

        DamageParticle(float x, float y, int color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.alpha = 1f;
            this.size = 9.5f + (float) (Math.random() * 2.0f);
            this.motionX = (float) ((Math.random() - 0.5f) * 2.5f);
            this.motionY = (float) ((Math.random() - 0.5f) * 2.5f);
        }

        void update() {
            x += motionX;
            y += motionY;
            motionX *= 0.95f;
            motionY *= 0.95f;
            alpha -= 0.028f;
            if (alpha < 0f) alpha = 0f;
            size *= 0.985f;
        }

        float getAlpha() {
            return alpha;
        }

        float getSize() {
            return size;
        }
    }
}
