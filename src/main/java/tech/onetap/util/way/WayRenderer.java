package tech.onetap.util.way;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import tech.onetap.Onetap;
import tech.onetap.event.list.EventHUD;
import tech.onetap.event.list.EventWorldRender;
import tech.onetap.util.IMinecraft;
import tech.onetap.util.render.math.ProjectionUtil;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WayRenderer implements IMinecraft {
    private static final WayRenderer INSTANCE = new WayRenderer();
    private static final Identifier WAY_TEXTURE = Identifier.of("mre", "images/map.png");
    private static final float ICON_SIZE = 8.0f;
    private static final float ICON_SCALE = 0.08f;
    private static final float ICON_DISTANCE_SCALE_DIVIDER = 48.0f;
    private static final float ICON_DISTANCE_SCALE_MAX = 10.0f;
    private static final double ICON_RENDER_DISTANCE_CAP = 96.0;
    private static final double WALK_SPEED_MPS = 4.317;
    private static final double SPRINT_SPEED_MPS = 5.612;

    private final Map<String, Waypoint> waypointsByName = new LinkedHashMap<>();

    public static WayRenderer get() {
        return INSTANCE;
    }

    private WayRenderer() {
        Onetap.getInstance().getEventBus().register(this);
    }

    public AddResult addWaypoint(String name, double x, double y, double z) {
        String trimmedName = name.trim();
        Waypoint previous = waypointsByName.put(normalize(trimmedName), new Waypoint(trimmedName, x, y, z));
        return previous == null ? AddResult.ADDED : AddResult.UPDATED;
    }

    public boolean removeWaypoint(String name) {
        return waypointsByName.remove(normalize(name)) != null;
    }

    public void clearWaypoints() {
        waypointsByName.clear();
    }

    public List<Waypoint> getWaypoints() {
        return new ArrayList<>(waypointsByName.values());
    }

    @Subscribe
    private void onWorldRender(EventWorldRender event) {
        if (waypointsByName.isEmpty()) {
            return;
        }
        if (mc.player == null || mc.world == null) {
            return;
        }

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        MatrixStack matrixStack = event.getMatrixStack();
        for (Waypoint waypoint : waypointsByName.values()) {
            renderWaypointIcon(matrixStack, cameraPos, waypoint);
        }
    }

    @Subscribe
    private void onHud(EventHUD event) {
        if (waypointsByName.isEmpty()) {
            return;
        }
        if (mc.player == null || mc.world == null) {
            return;
        }

        for (Waypoint waypoint : waypointsByName.values()) {
            Vector2f projected = ProjectionUtil.project(waypoint.x(), waypoint.y() + 0.85, waypoint.z());
            if (!isVisibleOnScreen(projected)) {
                continue;
            }

            String label = buildLabel(waypoint);
            float fontSize = 7.0f;
            float textWidth = Fonts.SFBOLD.get().getWidth(label, fontSize);
            float textX = projected.getX() - textWidth / 2.0f;
            float textY = projected.getY() + 14.0f;

            DrawUtil.drawText(Fonts.SFBOLD.get(), label, textX, textY - 0.5f, ColorProvider.rgba(255, 255, 255, 255), fontSize);
        }
    }

    private void renderWaypointIcon(MatrixStack matrices, Vec3d cameraPos, Waypoint waypoint) {
        Vec3d waypointPos = new Vec3d(waypoint.x(), waypoint.y(), waypoint.z());
        Vec3d cameraToWaypoint = waypointPos.subtract(cameraPos);
        double distance = cameraToWaypoint.length();
        Vec3d renderPos = waypointPos;
        if (distance > ICON_RENDER_DISTANCE_CAP && distance > 0.0) {
            renderPos = cameraPos.add(cameraToWaypoint.normalize().multiply(ICON_RENDER_DISTANCE_CAP));
        }

        float iconDistanceScale = Math.min(ICON_DISTANCE_SCALE_MAX, Math.max(1.0f, (float) (distance / ICON_DISTANCE_SCALE_DIVIDER)));
        float iconScale = ICON_SCALE * iconDistanceScale;

        matrices.push();
        matrices.translate(renderPos.x - cameraPos.x, renderPos.y - cameraPos.y + 0.15, renderPos.z - cameraPos.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
        matrices.scale(-iconScale, -iconScale, iconScale);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, WAY_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int color = ColorProvider.rgba(255, 255, 255, 240);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, -ICON_SIZE, ICON_SIZE, 0.0f).texture(0.0f, 1.0f).color(color);
        buffer.vertex(matrix, ICON_SIZE, ICON_SIZE, 0.0f).texture(1.0f, 1.0f).color(color);
        buffer.vertex(matrix, ICON_SIZE, -ICON_SIZE, 0.0f).texture(1.0f, 0.0f).color(color);
        buffer.vertex(matrix, -ICON_SIZE, -ICON_SIZE, 0.0f).texture(0.0f, 0.0f).color(color);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private String buildLabel(Waypoint waypoint) {
        Vec3d markerPos = new Vec3d(waypoint.x(), waypoint.y(), waypoint.z());
        double distance = mc.player.getPos().distanceTo(markerPos);
        int meters = (int) Math.round(distance);
        int seconds = estimateTravelSeconds(distance);
        return waypoint.name() + " " + meters + "m (" + seconds + "s)";
    }

    private int estimateTravelSeconds(double distance) {
        if (distance <= 0.0) {
            return 0;
        }
        double speed = mc.player != null && mc.player.isSprinting() ? SPRINT_SPEED_MPS : WALK_SPEED_MPS;
        return (int) Math.ceil(distance / speed);
    }

    private static boolean isVisibleOnScreen(Vector2f projected) {
        return projected.getX() != Float.MAX_VALUE
                && projected.getY() != Float.MAX_VALUE
                && !Float.isNaN(projected.getX())
                && !Float.isNaN(projected.getY());
    }

    private static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public enum AddResult {
        ADDED,
        UPDATED
    }

    public record Waypoint(String name, double x, double y, double z) {
    }
}
