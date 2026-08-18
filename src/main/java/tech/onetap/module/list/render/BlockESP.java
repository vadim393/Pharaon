package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Block;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import tech.onetap.event.list.EventTick;
import tech.onetap.event.list.EventWorldRender;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeListSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.module.settings.impl.ThemeManager;
import tech.onetap.util.blockesp.BlockEspRepository;
import tech.onetap.util.render.providers.ColorProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@ModuleInformation(moduleName = "BlockESP", moduleDesc = "хуйня", moduleCategory = ModuleCategory.RENDER)
public class BlockESP extends Module {
    private final SliderSetting scanRange = new SliderSetting("Дистанция", 64.0f, 8.0f, 256.0f, 1.0f);
    private final SliderSetting scanDelay = new SliderSetting("Обновление", 4.0f, 1.0f, 20.0f, 1.0f);
    private final SliderSetting maxBlocks = new SliderSetting("Лимит блоков", 192.0f, 16.0f, 9999.0f, 1.0f);
    private final SliderSetting outlineAlpha = new SliderSetting("Прозрачность", 1.0f, 0.1f, 1.0f, 0.05f);
    private final BooleanSetting throughWalls = new BooleanSetting("Через стены", true);
    private final ModeListSetting trackedBlocksSetting = new ModeListSetting("Блоки");

    private final Map<String, Block> trackedBlocks = new LinkedHashMap<>();
    private final Map<String, BooleanSetting> trackedBlockToggles = new LinkedHashMap<>();
    private final List<BlockPos> cachedBlocks = new ArrayList<>();

    private int scanCooldown = 0;

    public BlockESP() {
        BlockEspRepository.ensureLoaded();
        for (String blockId : BlockEspRepository.getBlocks()) {
            Identifier id = Identifier.tryParse(blockId);
            if (id == null || !Registries.BLOCK.containsId(id)) {
                continue;
            }
            addBlockInternal(id, true, false);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        scanCooldown = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        cachedBlocks.clear();
    }

    @Subscribe
    private void onTick(EventTick ignored) {
        if (!isEnabled() || mc.player == null || mc.world == null) {
            cachedBlocks.clear();
            return;
        }

        if (trackedBlocks.isEmpty()) {
            cachedBlocks.clear();
            return;
        }

        if (scanCooldown-- > 0) {
            return;
        }

        scanCooldown = Math.max(1, (int) Math.round(scanDelay.getValue()));
        rebuildCache();
    }

    @Subscribe
    private void onRender(EventWorldRender e) {
        if (!isEnabled() || mc.player == null || mc.world == null || cachedBlocks.isEmpty()) return;

        int outlineColor = ThemeManager.getInstance().getCurrentTheme().getColorTheme(0);
        outlineColor = ColorProvider.setAlpha(outlineColor, (int) (outlineAlpha.getValue() * 255));

        renderOutlines(e.getMatrixStack(), outlineColor);
    }

    public boolean addBlock(Identifier id) {
        if (id == null || !Registries.BLOCK.containsId(id)) {
            return false;
        }
        boolean added = addBlockInternal(id, true, true);
        if (added) {
            scanCooldown = 0;
        }
        return added;
    }

    public boolean removeBlock(Identifier id) {
        if (id == null) return false;

        String normalizedId = id.toString();
        Block removed = trackedBlocks.remove(normalizedId);
        BooleanSetting toggle = trackedBlockToggles.remove(normalizedId);
        if (toggle != null) {
            trackedBlocksSetting.removeOption(toggle);
        }

        if (removed == null) {
            return false;
        }

        BlockEspRepository.remove(normalizedId);
        scanCooldown = 0;
        return true;
    }

    public void clearBlocks() {
        trackedBlocks.clear();
        trackedBlockToggles.clear();
        trackedBlocksSetting.clearOptions();
        cachedBlocks.clear();
        BlockEspRepository.clear();
        scanCooldown = 0;
    }

    public boolean hasBlock(Identifier id) {
        if (id == null) return false;
        return trackedBlocks.containsKey(id.toString());
    }

    public List<String> getTrackedBlocks() {
        return new ArrayList<>(trackedBlocks.keySet());
    }

    public boolean isTrackedBlockEnabled(String blockId) {
        BooleanSetting setting = trackedBlockToggles.get(blockId);
        return setting != null && setting.getValue();
    }

    public static Identifier normalizeBlockId(String input) {
        if (input == null || input.isBlank()) return null;

        String normalized = input.trim().toLowerCase(Locale.ROOT);
        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }

        return Identifier.tryParse(normalized);
    }

    public static String toUserBlockName(String idString) {
        Identifier id = Identifier.tryParse(idString);
        if (id == null) return idString;
        if ("minecraft".equals(id.getNamespace())) {
            return id.getPath();
        }
        return id.toString();
    }

    private boolean addBlockInternal(Identifier id, boolean enabledByDefault, boolean saveToRepository) {
        String normalizedId = id.toString();
        if (trackedBlocks.containsKey(normalizedId)) {
            return false;
        }

        Block block = Registries.BLOCK.get(id);
        trackedBlocks.put(normalizedId, block);
        trackedBlockToggles.put(normalizedId, trackedBlocksSetting.addOption(normalizedId, enabledByDefault));

        if (saveToRepository) {
            BlockEspRepository.add(normalizedId);
        }
        return true;
    }

    private void rebuildCache() {
        Set<Block> enabledBlocks = collectEnabledBlocks();
        if (enabledBlocks.isEmpty()) {
            cachedBlocks.clear();
            return;
        }

        int range = Math.max(1, (int) Math.round(scanRange.getValue()));
        int verticalRange = Math.min(range, 24);
        int limit = Math.max(1, (int) Math.round(maxBlocks.getValue()));
        double rangeSq = range * range;

        BlockPos center = mc.player.getBlockPos();
        BlockPos min = center.add(-range, -verticalRange, -range);
        BlockPos max = center.add(range, verticalRange, range);

        List<FoundBlock> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.iterate(min, max)) {
            double distanceSq = mc.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distanceSq > rangeSq) continue;

            Block block = mc.world.getBlockState(pos).getBlock();
            if (enabledBlocks.contains(block)) {
                found.add(new FoundBlock(new BlockPos(pos.getX(), pos.getY(), pos.getZ()), distanceSq));
            }
        }

        found.sort(Comparator.comparingDouble(FoundBlock::distanceSq));

        cachedBlocks.clear();
        int maxIndex = Math.min(limit, found.size());
        for (int i = 0; i < maxIndex; i++) {
            cachedBlocks.add(found.get(i).pos());
        }
    }

    private Set<Block> collectEnabledBlocks() {
        Set<Block> enabled = new HashSet<>();
        for (Map.Entry<String, Block> entry : trackedBlocks.entrySet()) {
            BooleanSetting toggle = trackedBlockToggles.get(entry.getKey());
            if (toggle != null && toggle.getValue()) {
                enabled.add(entry.getValue());
            }
        }
        return enabled;
    }

    private void renderOutlines(net.minecraft.client.util.math.MatrixStack matrices, int color) {
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        boolean xray = throughWalls.getValue();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        if (xray) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
        }

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (BlockPos pos : cachedBlocks) {
            if (mc.world.getBlockState(pos).isAir()) continue;

            float x1 = (float) (pos.getX() - camPos.x + 0.002D);
            float y1 = (float) (pos.getY() - camPos.y + 0.002D);
            float z1 = (float) (pos.getZ() - camPos.z + 0.002D);
            float x2 = x1 + 0.996f;
            float y2 = y1 + 0.996f;
            float z2 = z1 + 0.996f;

            putBoxOutline(buffer, matrix, x1, y1, z1, x2, y2, z2, color);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        if (xray) {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }

        RenderSystem.lineWidth(1.0f);
        RenderSystem.disableBlend();
    }

    private void putBoxOutline(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        buffer.vertex(matrix, x1, y1, z1).color(color); buffer.vertex(matrix, x2, y1, z1).color(color);
        buffer.vertex(matrix, x2, y1, z1).color(color); buffer.vertex(matrix, x2, y1, z2).color(color);
        buffer.vertex(matrix, x2, y1, z2).color(color); buffer.vertex(matrix, x1, y1, z2).color(color);
        buffer.vertex(matrix, x1, y1, z2).color(color); buffer.vertex(matrix, x1, y1, z1).color(color);

        buffer.vertex(matrix, x1, y2, z1).color(color); buffer.vertex(matrix, x2, y2, z1).color(color);
        buffer.vertex(matrix, x2, y2, z1).color(color); buffer.vertex(matrix, x2, y2, z2).color(color);
        buffer.vertex(matrix, x2, y2, z2).color(color); buffer.vertex(matrix, x1, y2, z2).color(color);
        buffer.vertex(matrix, x1, y2, z2).color(color); buffer.vertex(matrix, x1, y2, z1).color(color);

        buffer.vertex(matrix, x1, y1, z1).color(color); buffer.vertex(matrix, x1, y2, z1).color(color);
        buffer.vertex(matrix, x2, y1, z1).color(color); buffer.vertex(matrix, x2, y2, z1).color(color);
        buffer.vertex(matrix, x1, y1, z2).color(color); buffer.vertex(matrix, x1, y2, z2).color(color);
        buffer.vertex(matrix, x2, y1, z2).color(color); buffer.vertex(matrix, x2, y2, z2).color(color);
    }

    private record FoundBlock(BlockPos pos, double distanceSq) {}
}
