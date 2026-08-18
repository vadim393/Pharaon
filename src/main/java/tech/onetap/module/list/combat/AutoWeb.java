package tech.onetap.module.list.combat;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import tech.onetap.Onetap;
import tech.onetap.event.list.EventTick;
import tech.onetap.event.list.EventWorldRender;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ColorSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.friend.FriendRepository;
import tech.onetap.util.math.StopWatch;
import tech.onetap.util.player.other.InventoryUtil;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.rotation.Rotation;
import tech.onetap.util.rotation.RotationComponent;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ModuleInformation(moduleName = "Auto Web", moduleCategory = ModuleCategory.COMBAT)
public final class AutoWeb extends Module {
    private final SliderSetting range = new SliderSetting("Дистанция", 5, 1, 7, 1);
    private final SliderSetting placeWallRange = new SliderSetting("Дитанция сквозь стены", 5, 1, 7, 1);
    private final ModeSetting placeTiming = new ModeSetting("Тайминг установки", "Default", "Default", "Vanilla");
    private final SliderSetting blocksPerTick = new SliderSetting("Блоки/Тик", 8, 1, 12, 1)
            .setVisible(() -> placeTiming.is("Default"));
    private final SliderSetting placeDelay = new SliderSetting("Задержка/Установка", 3, 0, 10, 1);
    private final ModeSetting interact = new ModeSetting("Взаимодействие", "Strict", "Strict", "Default");
    private final ModeSetting placeMode = new ModeSetting("Мод установки", "Normal", "Normal");
    private final ModeSetting rotate = new ModeSetting("Ротейт", "None", "None", "Normal");

    private final BooleanSetting head = new BooleanSetting("Голова", true);
    private final BooleanSetting legs = new BooleanSetting("Ножки", true);
    private final BooleanSetting surround = new BooleanSetting("Писька", true);
    private final BooleanSetting upperSurround = new BooleanSetting("Выше письки", false);

    private final BooleanSetting render = new BooleanSetting("Рендер", true);
    private final ModeSetting renderMode = new ModeSetting("Рендер Мод", "Fade", "Fade", "Decrease")
            .setVisible(render::getValue);
    private final ColorSetting renderFillColor = new ColorSetting("Рендер Fill Color", ColorProvider.rgba(60, 110, 255, 75))
            .setVisible(render::getValue);
    private final ColorSetting renderLineColor = new ColorSetting("Рендер Line Color", ColorProvider.rgba(60, 110, 255, 185))
            .setVisible(render::getValue);
    private final SliderSetting renderLineWidth = new SliderSetting("Рендер Line Width", 2, 1, 5, 1)
            .setVisible(render::getValue);
    private final SliderSetting effectDurationMs = new SliderSetting("Длительность эффекта в мс", 500, 0, 10000, 10)
            .setVisible(render::getValue);

    private final ArrayList<BlockPos> sequentialBlocks = new ArrayList<>();
    public static final StopWatch inactivityTimer = new StopWatch();
    private final Map<BlockPos, Long> renderPoses = new ConcurrentHashMap<>();
    private int delay;

    @Override
    public void onEnable() {
        sequentialBlocks.clear();
        renderPoses.clear();
        delay = 0;
        super.onEnable();
    }

    @Subscribe
    private void onTick(EventTick event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }

        cleanupRenderPoses();

        BlockPos probe = getSequentialPos();
        if (probe == null) {
            return;
        }

        if (delay > 0) {
            delay--;
            return;
        }

        int previousSlot = mc.player.getInventory().selectedSlot;
        int slot = getSlot();
        if (slot == -1) {
            return;
        }

        if (previousSlot != slot) {
            mc.player.getInventory().selectedSlot = slot;
            mc.interactionManager.syncSelectedSlot();
        }

        if (placeTiming.is("Default")) {
            int placed = 0;
            int perTick = Math.max(1, blocksPerTick.getIntValue());
            while (placed < perTick) {
                BlockPos targetBlock = getSequentialPos();
                if (targetBlock == null || !placeAt(targetBlock)) {
                    break;
                }

                placed++;
                renderPoses.put(targetBlock, System.currentTimeMillis());
                delay = placeDelay.getIntValue();
                inactivityTimer.reset();
            }
        } else {
            BlockPos targetBlock = getSequentialPos();
            if (targetBlock != null && placeAt(targetBlock)) {
                sequentialBlocks.add(targetBlock);
                renderPoses.put(targetBlock, System.currentTimeMillis());
                delay = placeDelay.getIntValue();
                inactivityTimer.reset();
            }
        }

        if (previousSlot != slot) {
            mc.player.getInventory().selectedSlot = previousSlot;
            mc.interactionManager.syncSelectedSlot();
        }
    }

    @Subscribe
    private void onWorldRender(EventWorldRender event) {
        if (!render.getValue() || renderPoses.isEmpty() || mc.player == null || mc.world == null) {
            return;
        }

        long now = System.currentTimeMillis();
        float duration = Math.max(1f, effectDurationMs.getFloatValue());

        for (Map.Entry<BlockPos, Long> entry : renderPoses.entrySet()) {
            float life = 1.0f - (float) (now - entry.getValue()) / duration;
            if (life <= 0f) {
                continue;
            }

            Box box = new Box(entry.getKey());
            if (renderMode.is("Decrease")) {
                double scale = Math.max(0.05, life);
                double cx = (box.minX + box.maxX) * 0.5;
                double cy = (box.minY + box.maxY) * 0.5;
                double cz = (box.minZ + box.maxZ) * 0.5;
                double hs = 0.5 * scale;
                box = new Box(cx - hs, cy - hs, cz - hs, cx + hs, cy + hs, cz + hs);
            }

            int fill = withLifeAlpha(renderFillColor.getValue(), life);
            int line = withLifeAlpha(renderLineColor.getValue(), life);
            drawWorldBox(event.getMatrixStack(), box, fill, line, renderLineWidth.getFloatValue());
        }
    }

    private BlockPos getSequentialPos() {
        PlayerEntity target = resolveTarget();
        if (target == null) {
            return null;
        }

        BlockPos base = target.getBlockPos();
        ArrayList<BlockPos> positions = new ArrayList<>(10);
        if (legs.getValue()) {
            positions.add(base);
        }
        if (head.getValue()) {
            positions.add(base.up());
        }
        if (surround.getValue()) {
            positions.add(base.north());
            positions.add(base.south());
            positions.add(base.east());
            positions.add(base.west());
        }
        if (upperSurround.getValue()) {
            positions.add(base.north().up());
            positions.add(base.south().up());
            positions.add(base.east().up());
            positions.add(base.west().up());
        }

        for (BlockPos bp : positions) {
            if (!isReplaceableForWeb(bp) || mc.world.getBlockState(bp).isOf(Blocks.COBWEB)) {
                continue;
            }

            Vec3d center = Vec3d.ofCenter(bp);
            double distSq = mc.player.getEyePos().squaredDistanceTo(center);
            if (distSq > range.getValue() * range.getValue()) {
                continue;
            }

            if (isBlockedByWall(bp) && distSq > placeWallRange.getValue() * placeWallRange.getValue()) {
                continue;
            }

            if (!canPlaceAt(bp)) {
                continue;
            }

            return bp;
        }

        return null;
    }

    private boolean placeAt(BlockPos pos) {
        if (!canPlaceAt(pos)) {
            return false;
        }

        BlockHitResult hit = createPlaceHitResult(pos);
        if (hit == null) {
            return false;
        }

        if (rotate.is("Normal")) {
            RotationComponent.update(new Rotation(hit.getPos()), 120, 120, 120, 120, 0, 20, false);
        }

        boolean placed = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit).isAccepted();
        if (placed) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }
        return placed;
    }

    private BlockHitResult createPlaceHitResult(BlockPos target) {
        return interact.is("Strict") ? createStrictHitResult(target) : createDefaultHitResult(target);
    }

    private BlockHitResult createDefaultHitResult(BlockPos target) {
        for (Direction side : Direction.values()) {
            BlockPos supportPos = target.offset(side);
            if (!isSupportBlock(supportPos)) {
                continue;
            }

            Direction clickSide = side.getOpposite();
            return new BlockHitResult(Vec3d.ofCenter(supportPos), clickSide, supportPos, false);
        }
        return null;
    }

    private BlockHitResult createStrictHitResult(BlockPos target) {
        BlockHitResult bestVisible = null;
        BlockHitResult bestBypass = null;
        double bestVisibleDistSq = Double.MAX_VALUE;
        double bestBypassDistSq = Double.MAX_VALUE;

        for (Direction side : Direction.values()) {
            BlockPos supportPos = target.offset(side);
            if (!isSupportBlock(supportPos)) {
                continue;
            }

            Direction clickSide = side.getOpposite();
            Vec3d hitVec = Vec3d.ofCenter(supportPos).add(Vec3d.of(clickSide.getVector()).multiply(0.5));
            double distSq = mc.player.getEyePos().squaredDistanceTo(hitVec);
            BlockHitResult candidate = new BlockHitResult(hitVec, clickSide, supportPos, false);

            if (canUseHitVecDirectly(hitVec, supportPos)) {
                if (distSq < bestVisibleDistSq) {
                    bestVisibleDistSq = distSq;
                    bestVisible = candidate;
                }
            } else if (distSq < bestBypassDistSq) {
                bestBypassDistSq = distSq;
                bestBypass = candidate;
            }
        }

        if (bestVisible != null) {
            return bestVisible;
        }

        if (bestBypass != null && bestBypassDistSq <= placeWallRange.getValue() * placeWallRange.getValue()) {
            return bestBypass;
        }

        return null;
    }

    private boolean canUseHitVecDirectly(Vec3d hitVec, BlockPos supportPos) {
        HitResult result = mc.world.raycast(new RaycastContext(
                mc.player.getEyePos(),
                hitVec,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        if (result.getType() == HitResult.Type.MISS) {
            return true;
        }
        if (!(result instanceof BlockHitResult blockHit)) {
            return false;
        }
        return blockHit.getBlockPos().equals(supportPos);
    }

    private boolean isBlockedByWall(BlockPos pos) {
        HitResult hit = mc.world.raycast(new RaycastContext(
                mc.player.getEyePos(),
                Vec3d.ofCenter(pos).add(0.0, 0.5, 0.0),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        if (hit.getType() == HitResult.Type.MISS) {
            return false;
        }
        if (!(hit instanceof BlockHitResult blockHit)) {
            return true;
        }
        return !blockHit.getBlockPos().equals(pos);
    }

    private boolean canPlaceAt(BlockPos pos) {
        if (!isReplaceableForWeb(pos)) {
            return false;
        }

        for (Direction side : Direction.values()) {
            if (isSupportBlock(pos.offset(side))) {
                return true;
            }
        }
        return false;
    }

    private boolean isReplaceableForWeb(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return state.isAir() || state.isReplaceable();
    }

    private boolean isSupportBlock(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        if (state.isOf(Blocks.COBWEB)) {
            return true;
        }
        return !state.isReplaceable();
    }

    private PlayerEntity resolveTarget() {
        PlayerEntity best = null;
        double bestDistSq = Double.MAX_VALUE;
        double maxDistSq = range.getValue() * range.getValue();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity player) || !isValidTarget(player)) {
                continue;
            }

            double distSq = mc.player.squaredDistanceTo(player);
            if (distSq > maxDistSq || distSq >= bestDistSq) {
                continue;
            }

            bestDistSq = distSq;
            best = player;
        }

        return best;
    }

    private boolean isValidTarget(PlayerEntity player) {
        if (player == mc.player || !player.isAlive()) {
            return false;
        }
        if (FriendRepository.isFriend(player.getNameForScoreboard())) {
            return false;
        }

        AntiBot antiBot = Onetap.getInstance().getModuleStorage().get(AntiBot.class);
        if (antiBot != null && antiBot.isBot(player)) {
            return false;
        }

        double maxDistSq = range.getValue() * range.getValue();
        return mc.player.squaredDistanceTo(player) <= maxDistSq;
    }

    private int getSlot() {
        int selected = mc.player.getInventory().selectedSlot;
        if (mc.player.getInventory().getStack(selected).isOf(Items.COBWEB)) {
            return selected;
        }
        return InventoryUtil.searchItemHotbar(Items.COBWEB);
    }

    private void cleanupRenderPoses() {
        long now = System.currentTimeMillis();
        long duration = Math.max(0, effectDurationMs.getIntValue());
        renderPoses.entrySet().removeIf(entry -> now - entry.getValue() > duration);
    }

    private int withLifeAlpha(int color, float life) {
        int alpha = (color >>> 24) & 0xFF;
        int nextAlpha = (int) (alpha * Math.max(0f, Math.min(1f, life)));
        return ColorProvider.setAlpha(color, nextAlpha);
    }

    private void drawWorldBox(net.minecraft.client.util.math.MatrixStack matrices, Box worldBox, int fillColor, int lineColor, float lineWidth) {
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        float x1 = (float) (worldBox.minX - camPos.x);
        float y1 = (float) (worldBox.minY - camPos.y);
        float z1 = (float) (worldBox.minZ - camPos.z);
        float x2 = (float) (worldBox.maxX - camPos.x);
        float y2 = (float) (worldBox.maxY - camPos.y);
        float z2 = (float) (worldBox.maxZ - camPos.z);

        drawFill(matrices, x1, y1, z1, x2, y2, z2, fillColor);
        drawOutline(matrices, x1, y1, z1, x2, y2, z2, lineColor, lineWidth);
    }

    private void drawFill(net.minecraft.client.util.math.MatrixStack matrices,
                          float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        putFace(buffer, matrix, x1, y1, z1, x2, y2, z2, Direction.UP, color);
        putFace(buffer, matrix, x1, y1, z1, x2, y2, z2, Direction.DOWN, color);
        putFace(buffer, matrix, x1, y1, z1, x2, y2, z2, Direction.NORTH, color);
        putFace(buffer, matrix, x1, y1, z1, x2, y2, z2, Direction.SOUTH, color);
        putFace(buffer, matrix, x1, y1, z1, x2, y2, z2, Direction.WEST, color);
        putFace(buffer, matrix, x1, y1, z1, x2, y2, z2, Direction.EAST, color);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void putFace(BufferBuilder buffer, Matrix4f matrix,
                         float x1, float y1, float z1, float x2, float y2, float z2,
                         Direction face, int color) {
        switch (face) {
            case UP -> {
                buffer.vertex(matrix, x1, y2, z1).color(color);
                buffer.vertex(matrix, x1, y2, z2).color(color);
                buffer.vertex(matrix, x2, y2, z2).color(color);
                buffer.vertex(matrix, x2, y2, z1).color(color);
            }
            case DOWN -> {
                buffer.vertex(matrix, x1, y1, z1).color(color);
                buffer.vertex(matrix, x2, y1, z1).color(color);
                buffer.vertex(matrix, x2, y1, z2).color(color);
                buffer.vertex(matrix, x1, y1, z2).color(color);
            }
            case NORTH -> {
                buffer.vertex(matrix, x1, y1, z1).color(color);
                buffer.vertex(matrix, x1, y2, z1).color(color);
                buffer.vertex(matrix, x2, y2, z1).color(color);
                buffer.vertex(matrix, x2, y1, z1).color(color);
            }
            case SOUTH -> {
                buffer.vertex(matrix, x1, y1, z2).color(color);
                buffer.vertex(matrix, x2, y1, z2).color(color);
                buffer.vertex(matrix, x2, y2, z2).color(color);
                buffer.vertex(matrix, x1, y2, z2).color(color);
            }
            case WEST -> {
                buffer.vertex(matrix, x1, y1, z1).color(color);
                buffer.vertex(matrix, x1, y1, z2).color(color);
                buffer.vertex(matrix, x1, y2, z2).color(color);
                buffer.vertex(matrix, x1, y2, z1).color(color);
            }
            case EAST -> {
                buffer.vertex(matrix, x2, y1, z1).color(color);
                buffer.vertex(matrix, x2, y2, z1).color(color);
                buffer.vertex(matrix, x2, y2, z2).color(color);
                buffer.vertex(matrix, x2, y1, z2).color(color);
            }
        }
    }

    private void drawOutline(net.minecraft.client.util.math.MatrixStack matrices,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             int color, float width) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.lineWidth(width);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        line(buffer, matrix, x1, y1, z1, x2, y1, z1, color);
        line(buffer, matrix, x2, y1, z1, x2, y1, z2, color);
        line(buffer, matrix, x2, y1, z2, x1, y1, z2, color);
        line(buffer, matrix, x1, y1, z2, x1, y1, z1, color);

        line(buffer, matrix, x1, y2, z1, x2, y2, z1, color);
        line(buffer, matrix, x2, y2, z1, x2, y2, z2, color);
        line(buffer, matrix, x2, y2, z2, x1, y2, z2, color);
        line(buffer, matrix, x1, y2, z2, x1, y2, z1, color);

        line(buffer, matrix, x1, y1, z1, x1, y2, z1, color);
        line(buffer, matrix, x2, y1, z1, x2, y2, z1, color);
        line(buffer, matrix, x1, y1, z2, x1, y2, z2, color);
        line(buffer, matrix, x2, y1, z2, x2, y2, z2, color);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private void line(BufferBuilder buffer, Matrix4f matrix,
                      float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        buffer.vertex(matrix, x1, y1, z1).color(color);
        buffer.vertex(matrix, x2, y2, z2).color(color);
    }

    @Override
    public void onDisable() {
        delay = 0;
        sequentialBlocks.clear();
        renderPoses.clear();
        super.onDisable();
    }
}
