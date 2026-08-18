package tech.onetap.module.list.combat;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.Last;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import tech.onetap.Onetap;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.math.StopWatch;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.rotation.ElytraComponent;

@ModuleInformation(moduleName = "Elytra Target", moduleDesc = "Преследует таргета на элитре", moduleCategory = ModuleCategory.COMBAT)
public class ElytraTarget extends Module {
    public final BooleanSetting target = new BooleanSetting("Перегонять", false);
    public static SliderSetting pursuitDistance = new SliderSetting("Дистанция преследования", 30.0, 10.0, 100.0, 5.0)
            .setVisible(() -> getInstance().target.getValue());
    public final BooleanSetting adaptivePredict = new BooleanSetting("Адаптивный перегон", true).setVisible(() -> target.getValue());
    public final SliderSetting adaptiveMinSpeed = new SliderSetting("Мин. скорость (БПС)", 30.0, 10.0, 100.0, 1.0)
            .setVisible(() -> target.getValue() && adaptivePredict.getValue());
    public final SliderSetting adaptiveMaxSpeed = new SliderSetting("Макс. скорость (БПС)", 85.0, 30.0, 200.0, 1.0)
            .setVisible(() -> target.getValue() && adaptivePredict.getValue());
    public final SliderSetting adaptiveMinPredict = new SliderSetting("Мин. предикт", 2.7F, 1.0, 20.0, 0.1F)
            .setVisible(() -> target.getValue() && adaptivePredict.getValue());
    public final SliderSetting adaptiveMaxPredict = new SliderSetting("Макс. предикт", 10.0, 2.0, 30.0, 0.1F)
            .setVisible(() -> target.getValue() && adaptivePredict.getValue());
    public final SliderSetting distance = new SliderSetting("Сила предикта", 2.7F, 1.0, 5.0, 0.1F)
            .setVisible(() -> target.getValue() && !adaptivePredict.getValue());
    public final ModeSetting predictMode = new ModeSetting("Режим предикта", "ReallyWorld", "ReallyWorld", "ReallyWorld - 2", "Default")
            .setVisible(() -> target.getValue());
    public final BooleanSetting predictCube = new BooleanSetting("Рисовать предикт", true)
            .setVisible(() -> target.getValue() && supportsPredictCube());
    public final SliderSetting predictFillAlpha = new SliderSetting("Прозрачность", 40.0, 0.0, 255.0, 1.0)
            .setVisible(() -> target.getValue() && predictCube.getValue() && supportsPredictCube());
    public final BooleanSetting predictFromTheme = new BooleanSetting("От темы", true)
            .setVisible(() -> target.getValue() && predictCube.getValue() && supportsPredictCube());
    public final ModeSetting predictBoxMode = new ModeSetting("Вид квадрата", "Обычный", "Обычный", "Пунктир", "Диагонали")
            .setVisible(() -> target.getValue() && predictCube.getValue() && supportsPredictCube());
    public final BooleanSetting visualReverse = new BooleanSetting("Разворот на 180", false).setVisible(() -> target.getValue());
    public final BooleanSetting stopOnHurt = new BooleanSetting("Стоп при уроне", false).setVisible(() -> target.getValue());
    public final SliderSetting stopDuration = new SliderSetting("Длительность стопа (мс)", 500.0, 100.0, 2000.0, 50.0)
            .setVisible(() -> target.getValue() && stopOnHurt.getValue());
    public final BooleanSetting antiBlink = new BooleanSetting("Анти-блинк", false).setVisible(() -> target.getValue());
    public boolean status = true;
    public boolean disableForward = false;

    public final BooleanSetting predictate = new BooleanSetting("Предикт на элитрах", true);
    public final SliderSetting predictValue = new SliderSetting("Предикт значение", 2.5f, 1, 5, 0.1f).setVisible(() -> predictate.getValue());
    public final ModeSetting rotationMode = new ModeSetting("Rotation mode", "Killaura", "Killaura", "Legit");
    public final BooleanSetting elytraTurnaround = new BooleanSetting("Снап на цель", true).setVisible(() -> predictate.getValue());
    public final BooleanSetting elytraPitchHold = new BooleanSetting("Удержание питча на элитрах", false);
    public final BooleanSetting elytraSlowdown = new BooleanSetting("Замедление на элитрах", true);
    public final ModeSetting slowdownMode = new ModeSetting(
            "Режим замедления", "По радиусу", "По радиусу", "Перед ударом", "33 BPS", "Синхронизация с таргетом"
    ).setVisible(() -> elytraSlowdown.getValue());
    public final SliderSetting slowdownRadius = new SliderSetting("Радиус замедления", 3.0f, 1.0f, 6.0f, 0.1f)
            .setVisible(() -> elytraSlowdown.getValue() && slowdownMode.is("По радиусу"));
    public final SliderSetting minSpeed = new SliderSetting("Мин. скорость", 0.3f, 0.1f, 0.9f, 0.05f)
            .setVisible(() -> elytraSlowdown.getValue() && slowdownMode.is("По радиусу"));
    public final SliderSetting syncDistance = new SliderSetting("Дистанция синхронизации", 10.0, 1.0, 30.0, 1.0)
            .setVisible(() -> elytraSlowdown.getValue() && slowdownMode.is("Синхронизация с таргетом"));
    public final BooleanSetting hitAfterOvertake = new BooleanSetting("Бить токо после перегона", true);
    public final BooleanSetting useResolver = new BooleanSetting("Резольвер на элитрах", true);

    private boolean visualReverseWasActive = false;
    private final StopWatch hurtTimer = new StopWatch();
    private static ElytraTarget instance;
    private Vec3d lastTargetPosition = null;
    private Vec3d lastLastTargetPosition = null;
    private final List<BlinkData> blinkHistory = new ArrayList<>();
    private Vec3d predictedBlinkPosition = null;
    private long lastBlinkTime = 0L;
    private int consecutiveNormalMoves = 0;
    private boolean renderListenerRegistered = false;
    private final Last renderListener = context -> {
        if (isEnabled() && predictCube.getValue() && supportsPredictCube()) {
            renderPredictCube(context.matrixStack(), context.camera(), context.tickCounter().getTickDelta(true));
        }
    };

    public ElytraTarget() {
        instance = this;
    }

    public static ElytraTarget getInstance() {
        return instance;
    }

    public boolean isReallyworldPredict() {
        return false;
    }

    public boolean isTestPredict() {
        return predictMode.is("ReallyWorld");
    }

    public boolean isPedikPredict() {
        return predictMode.is("ReallyWorld - 2");
    }

    public boolean usesVisualReverseLogic() {
        return isTestPredict() || isPedikPredict();
    }

    public boolean supportsPredictCube() {
        return isReallyworldPredict() || isTestPredict() || isPedikPredict();
    }

    public float getAdaptivePredictDistance() {
        if (adaptivePredict.getValue() && mc.player != null) {
            Vec3d velocity = mc.player.getVelocity();
            double speed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z) * 20.0;
            float minSpeed = adaptiveMinSpeed.getFloatValue();
            float maxSpeed = adaptiveMaxSpeed.getFloatValue();
            float minPredict = adaptiveMinPredict.getFloatValue();
            float maxPredict = adaptiveMaxPredict.getFloatValue();
            if (speed <= minSpeed) {
                return minPredict;
            }

            if (speed >= maxSpeed) {
                return maxPredict;
            }

            float speedRatio = (float) ((speed - minSpeed) / (maxSpeed - minSpeed));
            return minPredict + (maxPredict - minPredict) * speedRatio;
        } else {
            return distance.getFloatValue();
        }
    }

    private void processAntiBlink(LivingEntity target) {
        if (antiBlink.getValue() && target != null) {
            Vec3d currentPos = target.getPos();
            long currentTime = System.currentTimeMillis();
            if (lastTargetPosition == null) {
                lastTargetPosition = currentPos;
            } else if (lastLastTargetPosition == null) {
                lastLastTargetPosition = lastTargetPosition;
                lastTargetPosition = currentPos;
            } else {
                double moveDistance = lastTargetPosition.distanceTo(currentPos);
                double prevMoveDistance = lastLastTargetPosition.distanceTo(lastTargetPosition);
                Vec3d velocity = target.getVelocity();
                double expectedMovePerTick = velocity.length();
                boolean isBlinking;
                if (expectedMovePerTick < 0.5 && moveDistance > 2.0) {
                    isBlinking = true;
                } else if (moveDistance > expectedMovePerTick * 3.0 && moveDistance > 1.5) {
                    isBlinking = true;
                } else {
                    isBlinking = moveDistance > 2.5 && prevMoveDistance < 1.0;
                }

                if (isBlinking) {
                    Vec3d blinkDirection = currentPos.subtract(lastTargetPosition).normalize();
                    BlinkData blink = new BlinkData(currentPos, currentTime, moveDistance, blinkDirection);
                    blinkHistory.add(blink);
                    lastBlinkTime = currentTime;
                    consecutiveNormalMoves = 0;
                    blinkHistory.removeIf(data -> currentTime - data.timestamp > 4000L);
                    calculateSmartBlinkPrediction(target);
                } else {
                    consecutiveNormalMoves++;
                    if (consecutiveNormalMoves > 40) {
                        predictedBlinkPosition = null;
                    }
                }

                lastLastTargetPosition = lastTargetPosition;
                lastTargetPosition = currentPos;
            }
        } else {
            predictedBlinkPosition = null;
        }
    }

    private void calculateSmartBlinkPrediction(LivingEntity target) {
        if (blinkHistory.size() < 2) {
            predictedBlinkPosition = null;
        } else {
            long currentTime = System.currentTimeMillis();
            Vec3d currentPos = target.getPos();
            List<BlinkData> recentBlinks = blinkHistory.stream()
                    .filter(blink -> currentTime - blink.timestamp <= 3000L)
                    .collect(Collectors.toList());
            if (recentBlinks.size() < 2) {
                predictedBlinkPosition = null;
            } else {
                double totalDistance = 0.0;
                long totalInterval = 0L;
                Vec3d avgDirection = Vec3d.ZERO;

                for (int i = 1; i < recentBlinks.size(); i++) {
                    BlinkData current = recentBlinks.get(i);
                    BlinkData previous = recentBlinks.get(i - 1);
                    totalDistance += current.distance;
                    totalInterval += current.timestamp - previous.timestamp;
                    avgDirection = avgDirection.add(current.direction);
                }

                double avgDistance = totalDistance / (recentBlinks.size() - 1);
                long avgInterval = totalInterval / (recentBlinks.size() - 1);
                avgDirection = avgDirection.multiply(1.0 / (recentBlinks.size() - 1)).normalize();
                double distanceVariance = 0.0;

                for (int i = 1; i < recentBlinks.size(); i++) {
                    double diff = recentBlinks.get(i).distance - avgDistance;
                    distanceVariance += diff * diff;
                }

                distanceVariance /= recentBlinks.size() - 1;
                boolean stablePattern = distanceVariance < avgDistance * 0.5;
                long timeSinceLastBlink = currentTime - lastBlinkTime;
                if (stablePattern && timeSinceLastBlink >= avgInterval * 0.7) {
                    Vec3d predictedDirection = avgDirection;
                    Vec3d currentVelocity = target.getVelocity();
                    if (currentVelocity.length() > 0.1) {
                        Vec3d velocityDirection = currentVelocity.normalize();
                        predictedDirection = avgDirection.multiply(0.7).add(velocityDirection.multiply(0.3)).normalize();
                    }

                    predictedBlinkPosition = currentPos.add(predictedDirection.multiply(avgDistance));
                } else if (!stablePattern && recentBlinks.size() >= 3) {
                    BlinkData lastBlink = recentBlinks.get(recentBlinks.size() - 1);
                    Vec3d trendDirection = lastBlink.direction;
                    double trendDistance = lastBlink.distance;
                    if (timeSinceLastBlink >= 300L) {
                        predictedBlinkPosition = currentPos.add(trendDirection.multiply(trendDistance));
                    }
                } else {
                    predictedBlinkPosition = null;
                }
            }
        }
    }

    public Vec3d getAntiBlinkPredictPosition() {
        return predictedBlinkPosition;
    }

    @Subscribe
    public void onUpdate(EventTick event) {
        if (mc.player != null && mc.world != null) {
            KillAura aura = Onetap.getInstance().getModuleStorage().get(KillAura.class);
            if (aura != null) {
                status = target.getValue();
                if (aura.getTarget() == null) {
                    disableForward = false;
                    lastTargetPosition = null;
                    lastLastTargetPosition = null;
                    blinkHistory.clear();
                    predictedBlinkPosition = null;
                    consecutiveNormalMoves = 0;
                } else {
                    processAntiBlink(aura.getTarget());
                    ElytraComponent.processTargetLogic(this, aura.getTarget());
                    ElytraComponent.smartPredict();
                    if (stopOnHurt.getValue()) {
                        if (mc.player.hurtTime > 0 && aura.getTarget() != null) {
                            disableForward = true;
                            hurtTimer.reset();
                        }

                        if (hurtTimer.isReached((long) stopDuration.getValue())) {
                            disableForward = false;
                        }
                    } else {
                        disableForward = false;
                    }
                }
            }
        }
    }

    private void renderPredictCube(MatrixStack matrices, Camera camera, float tickDelta) {
        KillAura aura = Onetap.getInstance().getModuleStorage().get(KillAura.class);
        if (aura != null && aura.getTarget() != null) {
            LivingEntity target = aura.getTarget();
            if (target.isGliding()) {
                Vec3d renderPos;
                int color = -16711936;
                if (antiBlink.getValue() && predictedBlinkPosition != null) {
                    renderPos = predictedBlinkPosition;
                    color = -65536;
                } else {
                    renderPos = ElytraComponent.pos;
                    color = predictFromTheme.getValue() ? ColorProvider.getThemeColor() : -16711936;
                }

                if (renderPos != null && !renderPos.equals(Vec3d.ZERO)) {
                    Vec3d camPos = camera.getPos();
                    double renderX = renderPos.x - camPos.x;
                    double renderY = renderPos.y - camPos.y;
                    double renderZ = renderPos.z - camPos.z;
                    float size = 0.5F;
                    matrices.push();
                    matrices.translate(renderX, renderY, renderZ);
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.disableDepthTest();
                    RenderSystem.disableCull();
                    RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
                    Matrix4f matrix = matrices.peek().getPositionMatrix();
                    Tessellator tessellator = Tessellator.getInstance();
                    float r = (color >> 16 & 0xFF) / 255.0F;
                    float g = (color >> 8 & 0xFF) / 255.0F;
                    float b = (color & 0xFF) / 255.0F;
                    float fillAlpha = (float) (predictFillAlpha.getValue() / 255.0);
                    float lineAlpha = 1.0F;
                    if (fillAlpha > 0.0F) {
                        renderPredictFill(matrix, tessellator, size, r, g, b, fillAlpha);
                    }

                    BufferBuilder lineBuffer = tessellator.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
                    if (predictBoxMode.is("Обычный")) {
                        renderBoxLines(matrix, lineBuffer, size, r, g, b, lineAlpha, false);
                    } else if (predictBoxMode.is("Диагонали")) {
                        renderBoxLines(matrix, lineBuffer, size, r, g, b, lineAlpha, true);
                    } else if (predictBoxMode.is("Пунктир")) {
                        float step = size / 3.0F;
                        for (int i = 0; i < 3; i++) {
                            float start = -size + i * (size * 2.0F / 3.0F);
                            float end = start + step;
                            lineBuffer.vertex(matrix, start, -size, -size).color(r, g, b, lineAlpha);
                            lineBuffer.vertex(matrix, end, -size, -size).color(r, g, b, lineAlpha);
                        }
                    }

                    BufferRenderer.drawWithGlobalProgram(lineBuffer.end());
                    RenderSystem.enableDepthTest();
                    RenderSystem.enableCull();
                    RenderSystem.disableBlend();
                    matrices.pop();
                }
            }
        }
    }

    private void renderPredictFill(Matrix4f matrix, Tessellator tessellator, float size, float r, float g, float b, float fillAlpha) {
        BufferBuilder fillBuffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        fillBuffer.vertex(matrix, -size, -size, -size).color(r, g, b, fillAlpha);
        fillBuffer.vertex(matrix, size, -size, -size).color(r, g, b, fillAlpha);
        fillBuffer.vertex(matrix, size, -size, size).color(r, g, b, fillAlpha);
        fillBuffer.vertex(matrix, -size, -size, size).color(r, g, b, fillAlpha);
        fillBuffer.vertex(matrix, -size, size, -size).color(r, g, b, fillAlpha);
        fillBuffer.vertex(matrix, -size, size, size).color(r, g, b, fillAlpha);
        fillBuffer.vertex(matrix, size, size, size).color(r, g, b, fillAlpha);
        fillBuffer.vertex(matrix, size, size, -size).color(r, g, b, fillAlpha);
        fillBuffer.vertex(matrix, -size, -size, -size).color(r, g, b, fillAlpha);
        fillBuffer.vertex(matrix, -size, size, -size).color(r, g, b, fillAlpha);
        fillBuffer.vertex(matrix, size, size, -size).color(r, g, b, fillAlpha);
        fillBuffer.vertex(matrix, size, -size, -size).color(r, g, b, fillAlpha);
        fillBuffer.vertex(matrix, size, -size, -size).color(r, g, b, fillAlpha);
        fillBuffer.vertex(matrix, size, size, -size).color(r, g, b, fillAlpha);
        fillBuffer.vertex(matrix, size, size, size).color(r, g, b, fillAlpha);
        fillBuffer.vertex(matrix, size, -size, size).color(r, g, b, fillAlpha);
        fillBuffer.vertex(matrix, size, -size, size).color(r, g, b, fillAlpha);
        fillBuffer.vertex(matrix, size, size, size).color(r, g, b, fillAlpha);
        fillBuffer.vertex(matrix, -size, size, size).color(r, g, b, fillAlpha);
        fillBuffer.vertex(matrix, -size, -size, size).color(r, g, b, fillAlpha);
        fillBuffer.vertex(matrix, -size, -size, size).color(r, g, b, fillAlpha);
        fillBuffer.vertex(matrix, -size, size, size).color(r, g, b, fillAlpha);
        fillBuffer.vertex(matrix, -size, size, -size).color(r, g, b, fillAlpha);
        fillBuffer.vertex(matrix, -size, -size, -size).color(r, g, b, fillAlpha);
        BufferRenderer.drawWithGlobalProgram(fillBuffer.end());
    }

    private void renderBoxLines(Matrix4f matrix, BufferBuilder lineBuffer, float size, float r, float g, float b, float lineAlpha, boolean diagonals) {
        if (diagonals) {
            lineBuffer.vertex(matrix, -size, -size, -size).color(r, g, b, lineAlpha);
            lineBuffer.vertex(matrix, size, size, size).color(r, g, b, lineAlpha);
            lineBuffer.vertex(matrix, size, -size, -size).color(r, g, b, lineAlpha);
            lineBuffer.vertex(matrix, -size, size, size).color(r, g, b, lineAlpha);
            lineBuffer.vertex(matrix, -size, -size, size).color(r, g, b, lineAlpha);
            lineBuffer.vertex(matrix, size, size, -size).color(r, g, b, lineAlpha);
            lineBuffer.vertex(matrix, size, -size, size).color(r, g, b, lineAlpha);
            lineBuffer.vertex(matrix, -size, size, -size).color(r, g, b, lineAlpha);
            return;
        }

        lineBuffer.vertex(matrix, -size, -size, -size).color(r, g, b, lineAlpha);
        lineBuffer.vertex(matrix, size, -size, -size).color(r, g, b, lineAlpha);
        lineBuffer.vertex(matrix, size, -size, -size).color(r, g, b, lineAlpha);
        lineBuffer.vertex(matrix, size, -size, size).color(r, g, b, lineAlpha);
        lineBuffer.vertex(matrix, size, -size, size).color(r, g, b, lineAlpha);
        lineBuffer.vertex(matrix, -size, -size, size).color(r, g, b, lineAlpha);
        lineBuffer.vertex(matrix, -size, -size, size).color(r, g, b, lineAlpha);
        lineBuffer.vertex(matrix, -size, -size, -size).color(r, g, b, lineAlpha);
        lineBuffer.vertex(matrix, -size, size, -size).color(r, g, b, lineAlpha);
        lineBuffer.vertex(matrix, size, size, -size).color(r, g, b, lineAlpha);
        lineBuffer.vertex(matrix, size, size, -size).color(r, g, b, lineAlpha);
        lineBuffer.vertex(matrix, size, size, size).color(r, g, b, lineAlpha);
        lineBuffer.vertex(matrix, size, size, size).color(r, g, b, lineAlpha);
        lineBuffer.vertex(matrix, -size, size, size).color(r, g, b, lineAlpha);
        lineBuffer.vertex(matrix, -size, size, size).color(r, g, b, lineAlpha);
        lineBuffer.vertex(matrix, -size, size, -size).color(r, g, b, lineAlpha);
        lineBuffer.vertex(matrix, -size, -size, -size).color(r, g, b, lineAlpha);
        lineBuffer.vertex(matrix, -size, size, -size).color(r, g, b, lineAlpha);
        lineBuffer.vertex(matrix, size, -size, -size).color(r, g, b, lineAlpha);
        lineBuffer.vertex(matrix, size, size, -size).color(r, g, b, lineAlpha);
        lineBuffer.vertex(matrix, size, -size, size).color(r, g, b, lineAlpha);
        lineBuffer.vertex(matrix, size, size, size).color(r, g, b, lineAlpha);
        lineBuffer.vertex(matrix, -size, -size, size).color(r, g, b, lineAlpha);
        lineBuffer.vertex(matrix, -size, size, size).color(r, g, b, lineAlpha);
    }

    public boolean shouldTarget(LivingEntity livingEntity) {
        if (!isEnabled() || livingEntity == null || disableForward) {
            return false;
        }

        if (mc.player == null) {
            return false;
        }

        boolean isTargetValid = livingEntity.isGliding();
        return status && mc.player.isGliding() && isTargetValid;
    }

    public boolean isReverseActive() {
        if (usesVisualReverseLogic()
                && isEnabled()
                && target.getValue()
                && visualReverse.getValue()
                && !disableForward
                && mc.player != null) {
            KillAura aura = Onetap.getInstance().getModuleStorage().get(KillAura.class);
            if (aura == null) {
                visualReverseWasActive = false;
                return false;
            }

            LivingEntity auraTarget = aura.getTarget();
            if (auraTarget != null && shouldTarget(auraTarget)) {
                float distanceToTarget = mc.player.distanceTo(auraTarget);
                float enableDistance = 2.5F;
                float predictDistance = Math.max(enableDistance, (float) distance.getValue());
                float disableDistance = predictDistance + 3.0F;
                if (!visualReverseWasActive && distanceToTarget <= enableDistance) {
                    visualReverseWasActive = true;
                }

                if (visualReverseWasActive && distanceToTarget >= disableDistance) {
                    visualReverseWasActive = false;
                }

                return visualReverseWasActive;
            } else {
                visualReverseWasActive = false;
                return false;
            }
        } else {
            visualReverseWasActive = false;
            return false;
        }
    }

    public Vec3d getTargetVector(LivingEntity target) {
        return mc.player != null && target != null ? ElytraComponent.getVector3d(mc.player, target) : Vec3d.ZERO;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (!renderListenerRegistered) {
            WorldRenderEvents.LAST.register(renderListener);
            renderListenerRegistered = true;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        disableForward = false;
        visualReverseWasActive = false;
        lastTargetPosition = null;
        lastLastTargetPosition = null;
        blinkHistory.clear();
        predictedBlinkPosition = null;
        lastBlinkTime = 0L;
        consecutiveNormalMoves = 0;
        ElytraComponent.resetState();
    }

    public BooleanSetting getTarget() {
        return target;
    }

    public BooleanSetting getAdaptivePredict() {
        return adaptivePredict;
    }

    public SliderSetting getAdaptiveMinSpeed() {
        return adaptiveMinSpeed;
    }

    public SliderSetting getAdaptiveMaxSpeed() {
        return adaptiveMaxSpeed;
    }

    public SliderSetting getAdaptiveMinPredict() {
        return adaptiveMinPredict;
    }

    public SliderSetting getAdaptiveMaxPredict() {
        return adaptiveMaxPredict;
    }

    public SliderSetting getDistance() {
        return distance;
    }

    public ModeSetting getPredictMode() {
        return predictMode;
    }

    public BooleanSetting getPredictCube() {
        return predictCube;
    }

    public SliderSetting getPredictFillAlpha() {
        return predictFillAlpha;
    }

    public BooleanSetting getPredictFromTheme() {
        return predictFromTheme;
    }

    public ModeSetting getPredictBoxMode() {
        return predictBoxMode;
    }

    public BooleanSetting getVisualReverse() {
        return visualReverse;
    }

    public BooleanSetting getStopOnHurt() {
        return stopOnHurt;
    }

    public SliderSetting getStopDuration() {
        return stopDuration;
    }

    public BooleanSetting getAntiBlink() {
        return antiBlink;
    }

    public boolean isStatus() {
        return status;
    }

    public boolean isDisableForward() {
        return disableForward;
    }

    public boolean isVisualReverseWasActive() {
        return visualReverseWasActive;
    }

    public StopWatch getHurtTimer() {
        return hurtTimer;
    }

    public Vec3d getLastTargetPosition() {
        return lastTargetPosition;
    }

    public Vec3d getLastLastTargetPosition() {
        return lastLastTargetPosition;
    }

    public List<BlinkData> getBlinkHistory() {
        return blinkHistory;
    }

    public Vec3d getPredictedBlinkPosition() {
        return predictedBlinkPosition;
    }

    public long getLastBlinkTime() {
        return lastBlinkTime;
    }

    public int getConsecutiveNormalMoves() {
        return consecutiveNormalMoves;
    }

    public boolean isRenderListenerRegistered() {
        return renderListenerRegistered;
    }

    public Last getRenderListener() {
        return renderListener;
    }

    private static class BlinkData {
        final Vec3d position;
        final long timestamp;
        final double distance;
        final Vec3d direction;

        BlinkData(Vec3d position, long timestamp, double distance, Vec3d direction) {
            this.position = position;
            this.timestamp = timestamp;
            this.distance = distance;
            this.direction = direction;
        }
    }
}