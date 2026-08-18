package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.onetap.event.list.EventHUD;
import tech.onetap.event.list.EventTick;
import tech.onetap.mixin.IFireworkRocketEntityAccessor;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.util.render.math.ProjectionUtil;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.msdf.MsdfFont;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@ModuleInformation(moduleName = "FireWorkESP", moduleDesc = "Рисует тег на фейрверк", moduleCategory = ModuleCategory.RENDER)
public class FireWorkESP extends Module {
    private static final long MAX_MARK_LIFETIME_MS = 4000L;
    private static final long FADE_OUT_DURATION_MS = 1300L;
    private static final int MAX_TRACKED_MARKS = 128;
    private static final ItemStack FIREWORK_ICON = new ItemStack(Items.FIREWORK_ROCKET);

    private final Set<Integer> seenFireworks = new HashSet<>();
    private final List<FireworkMark> marks = new ArrayList<>();

    @Override
    public void onDisable() {
        super.onDisable();
        seenFireworks.clear();
        marks.clear();
    }

    @Subscribe
    private void onTick(EventTick ignored) {
        if (!isEnabled() || mc.world == null || mc.player == null) {
            seenFireworks.clear();
            marks.clear();
            return;
        }

        long now = System.currentTimeMillis();
        marks.removeIf(mark -> now - mark.launchMillis() > MAX_MARK_LIFETIME_MS);

        Set<Integer> aliveIds = new HashSet<>();
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof FireworkRocketEntity firework)) continue;

            int fireworkId = firework.getId();
            aliveIds.add(fireworkId);
            if (seenFireworks.contains(fireworkId)) continue;

            seenFireworks.add(fireworkId);
            IFireworkRocketEntityAccessor accessor = (IFireworkRocketEntityAccessor) firework;
            LivingEntity shooter = accessor.getShooter();
            if (shooter != null && !(shooter instanceof PlayerEntity)) continue;

            marks.add(new FireworkMark(firework.getPos(), now));
        }

        seenFireworks.retainAll(aliveIds);
        trimMarks();
    }

    @Subscribe
    private void onHud(EventHUD e) {
        if (!isEnabled() || mc.world == null || mc.player == null || marks.isEmpty()) return;

        long now = System.currentTimeMillis();
        MsdfFont font = Fonts.SFMEDIUM.get();
        DrawContext drawContext = e.getDrawContext();
        MatrixStack matrices = drawContext.getMatrices();

        Iterator<FireworkMark> iterator = marks.iterator();
        while (iterator.hasNext()) {
            FireworkMark mark = iterator.next();
            long ageMs = now - mark.launchMillis();
            if (ageMs > MAX_MARK_LIFETIME_MS) {
                iterator.remove();
                continue;
            }

            Vector2f projected = ProjectionUtil.project(mark.position().x, mark.position().y + 0.2, mark.position().z);
            if (projected.getX() == Float.MAX_VALUE || projected.getY() == Float.MAX_VALUE) continue;

            float ageSeconds = ageMs / 1000.0f;
            float alphaFactor = getAlphaFactor(ageMs);
            if (alphaFactor <= 0.01f) continue;
            int alpha = (int) (255.0f * alphaFactor);

            String timerText = String.format(Locale.US, "%.1f сек. ", ageSeconds);
            float textSize = 8.0f;
            float iconSize = 8.0f;
            float padding = 2.0f;
            float gap = 2.0f;
            float tagHeight = 13f;
            float textWidth = font.getWidth(timerText, textSize);
            float tagWidth = padding + iconSize + gap + textWidth + padding;

            float x = projected.getX() - tagWidth / 2.0f;
            float y = projected.getY() - 10.0f - (1.0f - alphaFactor) * 2.0f;

            // Same visual feel as item/hand tags: dark flat rect, icon left, text right.
            int bgColor = ColorProvider.rgba(0, 0, 0, (int) (125 * alphaFactor));

            DrawUtil.drawRound(x, y, tagWidth, tagHeight, 0.0f, bgColor);

            float iconX = x + padding;
            float iconY = y + (tagHeight - iconSize) / 2.0f;
            float iconAnim = MathHelper.clamp(alphaFactor, 0.0f, 1.0f);
            float animatedIconScale = 0.5f * iconAnim;
            if (animatedIconScale > 0.001f) {
                float iconRenderSize = 16.0f * animatedIconScale;
                float animatedIconX = iconX + (iconSize - iconRenderSize) * 0.5f;
                float animatedIconY = iconY + (iconSize - iconRenderSize) * 0.5f;

                matrices.push();
                matrices.translate(animatedIconX, animatedIconY, 0.0f);
                matrices.scale(animatedIconScale, animatedIconScale, 1.0f);
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, iconAnim);
                drawContext.drawItem(FIREWORK_ICON, 0, 0);
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                matrices.pop();
            }

            DrawUtil.drawText(font, timerText, x + padding + iconSize + gap, y + 1.8f, ColorProvider.rgba(255, 255, 255, alpha), textSize);
        }
    }

    private float getAlphaFactor(long ageMs) {
        long fadeStart = MAX_MARK_LIFETIME_MS - FADE_OUT_DURATION_MS;
        if (ageMs <= fadeStart) {
            return 1.0f;
        }

        float t = MathHelper.clamp((ageMs - fadeStart) / (float) FADE_OUT_DURATION_MS, 0.0f, 1.0f);
        float smooth = t * t * (3.0f - 2.0f * t);
        return 1.0f - smooth;
    }

    private void trimMarks() {
        if (marks.size() <= MAX_TRACKED_MARKS) return;
        int remove = marks.size() - MAX_TRACKED_MARKS;
        marks.subList(0, remove).clear();
    }

    private record FireworkMark(Vec3d position, long launchMillis) {}
}
