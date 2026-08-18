package tech.onetap.module.list.combat;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.FishEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import tech.onetap.Onetap;
import tech.onetap.event.EventGameUpdate;
import tech.onetap.event.list.EventChangeSprint;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.combat.rotation.FTRotation;
import tech.onetap.module.list.player.FreeCamera;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeListSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.friend.FriendRepository;
import tech.onetap.util.math.BestPoint;
import tech.onetap.util.math.RotationUtil;
import tech.onetap.util.math.StopWatch;
import tech.onetap.util.packet.NetworkUtils;
import tech.onetap.util.player.combat.PredictUtils;
import tech.onetap.util.player.combat.RaytraceUtil;
import tech.onetap.util.player.simulate.SimulatedPlayer;
import tech.onetap.util.render.math.GCDFixer;
import tech.onetap.util.render.math.MathUtil;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.rotation.Rotation;
import tech.onetap.util.rotation.RotationComponent;
import tech.onetap.util.text.ValueUnit;

import java.util.Random;

@ModuleInformation(moduleName = "KillAura", moduleCategory = ModuleCategory.COMBAT)
public class KillAura extends Module {
    private static final String ATTACK_RANGE_MODE_NORMAL = "Обычный";
    private static final String ATTACK_RANGE_MODE_DASHED = "Пунктирный";

    public final ModeSetting rotation = new ModeSetting("Ротация", "FunSky", "FunSky", "FT");

    private final ModeListSetting targets = new ModeListSetting("Таргеты",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Голые", true),
            new BooleanSetting("Монстры", true),
            new BooleanSetting("Животные", true)
    );

    public final SliderSetting distance = new SliderSetting("Дистанция", ValueUnit.countable("блок", "блока", "блоков"), 3, 2, 6, 0.1f);
    private final SliderSetting preRotation = new SliderSetting("Пре дистанция", ValueUnit.countable("блок", "блока", "блоков"), 1.5f, 0, 3, 0.1f);
    public final BooleanSetting raycastCheck = new BooleanSetting("Проверка на наведение", true);
    public static double distanceBoost = 0.0;
    private static double forcedAttackRange = -1.0;
    public final BooleanSetting wallBypass = new BooleanSetting("Обход через стены", true);

    public final BooleanSetting visualElytraRotation = new BooleanSetting("Визуал. ротка Элитры", true);

    public final ModeSetting movementCorrection = new ModeSetting("Коррекция движения", "Сфокусированная", "Свободная", "Сфокусированная", "Таргет");

    public final BooleanSetting onlySpace = new BooleanSetting("Только с пробелом", true);
    public final BooleanSetting clientLook = new BooleanSetting("Клиент лук", true);
    public final BooleanSetting showPredictPoint = new BooleanSetting("Показать предикт точку", true);
    public final BooleanSetting renderRotations = new BooleanSetting("Рендер ротации", true);

    public final BooleanSetting showAttackRange = new BooleanSetting("Отображать радиус удара", false);
    public final ModeSetting attackRangeCircleMode = new ModeSetting("Режим круга", ATTACK_RANGE_MODE_NORMAL, ATTACK_RANGE_MODE_NORMAL, ATTACK_RANGE_MODE_DASHED).setVisible(showAttackRange::getValue);
    public boolean isResolving = false;
    public Vec3d resolverPoint = null;

    public static boolean isSlowdownActive = false;
    private static StopWatch stopWatch = new StopWatch();
    @Getter
    private LivingEntity target;
    public static LivingEntity lastTarget;
    public int ticksToAttack;

    public static long lastPhysicalMoveTime;

    public float preddict;
    public float lastYaw;
    public float lastPitch;
    private int lastElytraLegitPitchChangeDirection;
    private Rotation lastElytraLegitSentAngle = new Rotation(0.0f, 0.0f);
    private final Random elytraLegitRandom = new Random();
    @Getter
    @Setter
    private boolean returningToCamera;
    private Vec3d cameraReturnRotationPoint = Vec3d.ZERO;
    private Vec3d cameraReturnRotationMotion = Vec3d.ZERO;

    private final FTRotation ftRotation = new FTRotation();

    private float attackRangeReachAnimation = 0.0f;

    private boolean renderListenerRegistered = false;
    private final WorldRenderEvents.Last renderListener = context -> {
        if (isEnabled()) {
            if (showAttackRange.getValue()) {
                renderAttackRangeCircle(context.matrixStack(), context.camera(), context.tickCounter().getTickDelta(true));
            }
            if (showPredictPoint.getValue()) {
                renderPredictPoint(context.matrixStack(), context.camera(), context.tickCounter().getTickDelta(true));
            }
            if (renderRotations.getValue()) {
                renderRotationPoint(context.matrixStack(), context.camera(), context.tickCounter().getTickDelta(true));
            }
        }
    };
    private record RotationPlan(Rotation rotation, Box targetBox, boolean targetGliding, int jitterSeed, boolean readyToAttack, boolean ignoreAssistMoveGate) {
    }

    private void findResolverPoint() {
        if (mc.player == null || mc.world == null) return;
        Vec3d eye = mc.player.getEyePos();

        float oppositeYaw = mc.player.getYaw() + 180f;
        float searchPitch = -50f;

        int[] yawOffsets = {0, 30, -30, 45, -45, 60, -60, 90, -90};

        for (int offset : yawOffsets) {
            float testYaw = oppositeYaw + offset;

            float radYaw = (float) Math.toRadians(testYaw);
            float radPitch = (float) Math.toRadians(searchPitch);

            double x = -Math.sin(radYaw) * Math.cos(radPitch);
            double y = -Math.sin(radPitch);
            double z = Math.cos(radYaw) * Math.cos(radPitch);

            Vec3d checkVec = new Vec3d(x, y, z).normalize().multiply(8.0);
            Vec3d endPoint = eye.add(checkVec);

            if (mc.world.raycast(new RaycastContext(eye, endPoint, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS) {
                resolverPoint = endPoint;
                return;
            }
        }
        resolverPoint = null;
    }


    @Subscribe
    private void onGameUpdate(EventGameUpdate e) {
        if (mc.player == null || target == null) return;

        if (isResolving) {
            if (stopWatch.isReached(300)) {
                isResolving = false;
            } else if (resolverPoint != null) {
                var rot = new Rotation(RotationUtil.calculate(resolverPoint));
                RotationComponent.update(rot, 360, 360, 360, 360, 0, 1, clientLook.getValue());
                lastYaw = rot.getYaw();
                lastPitch = rot.getPitch();
                return;
            }
        }

        if (returningToCamera) {
            returningToCamera = false;
            resetCameraReturnRotationState();
        }

        if (shouldUseElytraLegitRotation(target)) {
            updateElytraLegitRotation(target);
            return;
        }

        applyRotationPlan(createTargetRotationPlan(target));
    }

    @Subscribe
    private void onChangeSprint(EventChangeSprint e) {
        if (canStopSprinting()) e.setSprinting(false);
    }

    @Subscribe
    private void onUpdate(final EventTick ignored) {
        if (mc.player == null || mc.world == null) return;

        if (ticksToAttack > 0) ticksToAttack--;

        if (rotation.is("FT")) {
            ftRotation.updateAttackState(false);
        }

        updateTarget();
        if (target == null) {
            isSlowdownActive = false;
        }

        if (target != null) {
            lastTarget = target;
            if (getElytraTarget().elytraSlowdown.getValue() && mc.player.isGliding()) {
                double distToPredict = mc.player.getEyePos().distanceTo(getElytraChasePoint(target));
                if (getElytraTarget().slowdownMode.is("Перед ударом")) {
                    isSlowdownActive = ticksToAttack > 0 && ticksToAttack <= 3;
                } else {
                    isSlowdownActive = distToPredict < 2.7 && ticksToAttack <= 2;
                }
            } else {
                isSlowdownActive = false;
            }
            if (canStopSprinting()) mc.player.setSprinting(false);

            if (canAttack()) {
                if (getElytraTarget().useResolver.getValue() && mc.player.isGliding()) {
                    mc.player.setVelocity(0, 0, 0);

                    findResolverPoint();
                    if (resolverPoint != null) {
                        isResolving = true;
                        stopWatch.reset();
                    }
                }
                mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));

                if (wallBypass.getValue()) {
                    if (mc.crosshairTarget instanceof BlockHitResult hitResult) {
                        mc.interactionManager.attackBlock(hitResult.getBlockPos(), hitResult.getSide());
                        NetworkUtils.sendSilentPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, hitResult.getBlockPos(), hitResult.getSide(), 0));
                    }
                }

                applyElytraTurnaround(target);
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);

                if (rotation.is("FT")) {
                    ftRotation.updateAttackState(true);
                    ftRotation.onAttack();
                }

                mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(mc.player.input.playerInput));

                ticksToAttack = 10;
            }
        }
    }

    private boolean isValidEntity(Entity entity) {
        if (!entity.isAlive()) return false;
        FreeCamera freeCamera = Onetap.getInstance().getModuleStorage().get(FreeCamera.class);
        AntiBot antiBot = Onetap.getInstance().getModuleStorage().get(AntiBot.class);
        PlayerEntity fakePlayer = freeCamera != null ? freeCamera.fakePlayer : null;
        PlayerEntity player = fakePlayer != null ? fakePlayer : mc.player;
        if (entity == fakePlayer) return false;
        if (entity instanceof ClientPlayerEntity) return false;
        if (entity instanceof ArmorStandEntity) return false;
        if (entity instanceof PlayerEntity p && p.getArmor() != 0 && !targets.isEnabled("Игроки")) return false;
        if (entity instanceof PlayerEntity p && p.getArmor() == 0 && !targets.isEnabled("Голые")) return false;
        if ((entity instanceof HostileEntity || entity instanceof AmbientEntity) && !targets.isEnabled("Монстры"))
            return false;
        if ((entity instanceof PassiveEntity || entity instanceof FishEntity) && !targets.isEnabled("Животные"))
            return false;
        if (entity instanceof PlayerEntity p) {
            if (antiBot != null && antiBot.isBot(p)) return false;
            if (!FriendRepository.shouldAttack(p)) return false;
        }
        if (player.getEyePos().distanceTo(BestPoint.getNearestPoint(entity)) > (player.isGliding() ? 50 : distance.getValue() + preRotation.getValue() + distanceBoost))
            return false;
        return true;
    }

    public boolean canAttack() {
        if (target == null) return false;

        FreeCamera freeCamera = Onetap.getInstance().getModuleStorage().get(FreeCamera.class);
        PlayerEntity player = freeCamera != null && freeCamera.fakePlayer != null ? freeCamera.fakePlayer : mc.player;

        boolean elytraPredict = shouldUseElytraPredict(target);

        if (elytraPredict) {
            preddict = getElytraTarget().hitAfterOvertake.getValue() ? 2.85f : 4f;
        }

        if (!Onetap.getInstance().getIdealHitUtils().cooldownIsReached(false)) return false;
        if (ticksToAttack > 0) return false;

        if (elytraPredict) {
            double distToPredict = player.getEyePos().distanceTo(getPredictPoint(target, getElytraTarget().predictValue.getValue()));
            if (distToPredict > preddict) return false;
        } else {
            double attackRange = getEffectiveAttackRange();
            if (!RaytraceUtil.rayTrace(player.getRotationVector(), attackRange, target.getBoundingBox()) && raycastCheck.getValue())
                return false;

            if (player.getEyePos().distanceTo(BestPoint.getNearestPoint(target)) > (attackRange - 0.2f))
                return false;
        }

        return Onetap.getInstance().getIdealHitUtils().canCritical();
    }

    private double getEffectiveAttackRange() {

        if (forcedAttackRange > 0) {
            return forcedAttackRange;
        }
        return distance.getValue() + distanceBoost;
    }

    public static void setForcedAttackRange(double attackRange) {
        forcedAttackRange = attackRange;
    }

    private void applyElytraTurnaround(LivingEntity target) {
        if (!shouldApplyElytraTurnaround(target)) {
            return;
        }

        Vec3d snapPoint = BestPoint.getNearestPoint(target);
        if (snapPoint == null) {
            snapPoint = target.getBoundingBox().getCenter();
        }

        Rotation snapRotation = new Rotation(RotationUtil.calculate(snapPoint));
        RotationComponent.update(snapRotation, 360, 360, 360, 360, 0, 2, clientLook.getValue());
        lastYaw = snapRotation.getYaw();
        lastPitch = snapRotation.getPitch();
    }

    private boolean shouldApplyElytraTurnaround(LivingEntity target) {
        ElytraTarget elytraTarget = getElytraTarget();
        return elytraTarget != null
                && elytraTarget.elytraTurnaround.getValue()
                && mc.player != null
                && mc.player.isGliding()
                && shouldUseElytraPredict(target);
    }

    public boolean canStopSprinting() {
        if (target == null) return false;
        if (!Onetap.getInstance().getIdealHitUtils().cooldownIsReached(true)) return false;
        if (ticksToAttack > 1) return false;
        if (SimulatedPlayer.simulateLocalPlayer(1).fallDistance == 0) return false;
        return true;
    }

    private void updateTarget() {
        LivingEntity best = null;
        double bestFovDot = -1;

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0F);

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity) {
                if (!isValidEntity(entity)) continue;

                Vec3d targetVec = BestPoint.getNearestPoint(entity).subtract(eyePos).normalize();
                double dot = lookVec.dotProduct(targetVec);

                if (dot > bestFovDot) {
                    bestFovDot = dot;
                    best = (LivingEntity) entity;
                }
            }
        }

        if (target == null || !isValidEntity(target)) {
            this.target = best;
        }
    }

    private void updateFunSkyRotation(Rotation rotation) {
        RotationComponent.update(rotation, 360, 360, 360, 360, 2, 1, clientLook.getValue());
        lastYaw = rotation.getYaw();
        lastPitch = rotation.getPitch();
    }

    private void updateFTRotation(Rotation targetRotation) {
        Rotation currentRotation = new Rotation(lastYaw, lastPitch);
        Rotation result = ftRotation.process(currentRotation, targetRotation, null, null);
        float newPitch = MathHelper.clamp(result.getPitch(), -89.9f, 89.9f);
        Rotation finalRotation = new Rotation(result.getYaw(), newPitch);
        RotationComponent.update(finalRotation, 360, 360, 360, 360, 0, 1, clientLook.getValue());
        lastYaw = finalRotation.getYaw();
        lastPitch = finalRotation.getPitch();
    }

    private ElytraTarget getElytraTarget() {
        return Onetap.getInstance().getModuleStorage().get(ElytraTarget.class);
    }

    public Vec3d getPredictPoint(LivingEntity target, double ticksAhead) {
        if (target == null) {
            return Vec3d.ZERO;
        }
        return PredictUtils.predict(target, ticksAhead);
    }

    public Vec3d getElytraChasePoint(LivingEntity entity) {
        if (entity == null) {
            return Vec3d.ZERO;
        }
        return getPredictPoint(entity, getElytraTarget().predictValue.getValue());
    }

    private boolean shouldUseElytraPredict(LivingEntity target) {
        ElytraTarget elytraTarget = getElytraTarget();
        return elytraTarget != null
                && mc.player != null
                && mc.player.isGliding()
                && target != null
                && target.isGliding()
                && elytraTarget.predictate.getValue();
    }

    private boolean shouldUseElytraPitchHoldPoint() {
        return mc.player != null && mc.player.isGliding() && getElytraTarget().elytraPitchHold.getValue();
    }

    private Vec3d getElytraPitchHoldPoint(LivingEntity target) {
        if (target == null || mc.player == null) {
            return Vec3d.ZERO;
        }
        return getElytraPitchHoldPoint(mc.player.getEyePos(), target);
    }

    private Vec3d getElytraPitchHoldPoint(Vec3d pos, LivingEntity entity) {
        if (entity == null) {
            return Vec3d.ZERO;
        }

        double safePoint = 0;
        return new Vec3d(
                MathHelper.clamp(pos.x, entity.getBoundingBox().minX + safePoint, entity.getBoundingBox().maxX - safePoint),
                MathHelper.clamp(pos.y, entity.getBoundingBox().minY + safePoint, entity.getBoundingBox().maxY - safePoint),
                MathHelper.clamp(pos.z, entity.getBoundingBox().minZ + safePoint, entity.getBoundingBox().maxZ - safePoint)
        );
    }

    private boolean isReadyToAttackRotation() {
        return mc.player != null && mc.player.getAttackCooldownProgress(1.0f) > 0.9f && ticksToAttack <= 1;
    }

    private RotationPlan createTargetRotationPlan(LivingEntity target) {
        if (target == null || mc.player == null) {
            return null;
        }
        Rotation targetRotation = new Rotation(RotationUtil.calculate(BestPoint.getPoint(target)));

        return new RotationPlan(
                targetRotation,
                target.getBoundingBox(),
                target.isGliding(),
                target.getId(),
                isReadyToAttackRotation(),
                false
        );
    }

    private void applyRotationPlan(RotationPlan plan) {
        if (plan == null) {
            return;
        }

        switch (rotation.getValue()) {
            case "FunSky" -> updateFunSkyRotation(plan.rotation());
            case "FT" -> updateFTRotation(plan.rotation());
            default -> updateFunSkyRotation(plan.rotation());
        }
    }

    public void syncRotationStateToCamera() {
        Rotation cameraRotation = getCameraReturnRotation();
        lastYaw = cameraRotation.getYaw();
        lastPitch = cameraRotation.getPitch();
        resetCameraReturnRotationState();
    }

    public void rotateToCamera() {
        if (mc.player == null) {
            return;
        }

        Rotation cameraRotation = getCameraReturnRotation();
        applyRotationPlan(createCameraReturnPlan(cameraRotation));
    }

    public boolean updateRotationToCamera(float maxDelta) {
        if (mc.player == null) {
            return true;
        }

        Rotation cameraRotation = getCameraReturnRotation();
        float delta = new Rotation(lastYaw, lastPitch).getDelta(cameraRotation);
        if (delta < maxDelta) {
            return true;
        }

        applyRotationPlan(createCameraReturnPlan(cameraRotation));

        return new Rotation(lastYaw, lastPitch).getDelta(cameraRotation) < maxDelta;
    }

    private RotationPlan createCameraReturnPlan(Rotation cameraRotation) {
        Box cameraRotationBox = getCameraRotationBox(cameraRotation);
        Vec3d cameraReturnPoint = resolveCameraReturnRotationPoint(cameraRotationBox);
        int jitterSeed = lastTarget != null ? lastTarget.getId() : mc.player.getId();
        return new RotationPlan(
                new Rotation(RotationUtil.calculate(cameraReturnPoint)),
                cameraRotationBox,
                mc.player.isGliding(),
                jitterSeed,
                isReadyToAttackRotation(),
                true
        );
    }

    private Rotation getCameraReturnRotation() {
        float yaw = mc.gameRenderer.getCamera().getYaw();
        float pitch = mc.gameRenderer.getCamera().getPitch();

        if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
            yaw -= 180.0f;
            pitch = -pitch;
        }

        return new Rotation(yaw, pitch);
    }

    private Box getCameraRotationBox(Rotation rotation) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d point = eyePos.add(rotation.toVector().multiply(6.0));
        return new Box(
                point.x - 0.3,
                point.y - 0.9,
                point.z - 0.3,
                point.x + 0.3,
                point.y + 0.9,
                point.z + 0.3
        );
    }

    private Vec3d resolveCameraReturnRotationPoint(Box box) {
        float minMotionXZ = 0.005f;
        float maxMotionXZ = 0.015f;
        float minMotionY = 0.0015f;
        float maxMotionY = 0.015f;

        double lengthX = box.getLengthX();
        double lengthY = box.getLengthY();
        double lengthZ = box.getLengthZ();

        if (cameraReturnRotationMotion.equals(Vec3d.ZERO)) {
            cameraReturnRotationMotion = new Vec3d(
                    MathUtil.random(-0.02f, 0.02f),
                    MathUtil.random(-0.02f, 0.02f),
                    MathUtil.random(-0.02f, 0.02f)
            );
        }

        if (cameraReturnRotationPoint.equals(Vec3d.ZERO)) {
            cameraReturnRotationPoint = new Vec3d(0.0, lengthY * 0.5, 0.0);
        }

        cameraReturnRotationPoint = cameraReturnRotationPoint.add(cameraReturnRotationMotion);

        double safeX = (lengthX - 0.1) / 2.0f;
        double safeZ = (lengthZ - 0.1) / 2.0f;

        if (cameraReturnRotationPoint.x >= safeX) {
            cameraReturnRotationMotion = new Vec3d(-MathUtil.random(minMotionXZ, maxMotionXZ), cameraReturnRotationMotion.getY(), cameraReturnRotationMotion.getZ());
        } else if (cameraReturnRotationPoint.x <= -safeX) {
            cameraReturnRotationMotion = new Vec3d(MathUtil.random(minMotionXZ, maxMotionXZ), cameraReturnRotationMotion.getY(), cameraReturnRotationMotion.getZ());
        }

        if (cameraReturnRotationPoint.y >= lengthY * 0.75) {
            cameraReturnRotationMotion = new Vec3d(cameraReturnRotationMotion.getX(), -MathUtil.random(minMotionY, maxMotionY), cameraReturnRotationMotion.getZ());
        } else if (cameraReturnRotationPoint.y <= lengthY * 0.3) {
            cameraReturnRotationMotion = new Vec3d(cameraReturnRotationMotion.getX(), MathUtil.random(minMotionY, maxMotionY), cameraReturnRotationMotion.getZ());
        }

        if (cameraReturnRotationPoint.z >= safeZ) {
            cameraReturnRotationMotion = new Vec3d(cameraReturnRotationMotion.getX(), cameraReturnRotationMotion.getY(), -MathUtil.random(minMotionXZ, maxMotionXZ));
        } else if (cameraReturnRotationPoint.z <= -safeZ) {
            cameraReturnRotationMotion = new Vec3d(cameraReturnRotationMotion.getX(), cameraReturnRotationMotion.getY(), MathUtil.random(minMotionXZ, maxMotionXZ));
        }

        return new Vec3d(box.getCenter().x, box.minY, box.getCenter().z).add(cameraReturnRotationPoint);
    }

    private boolean shouldUseElytraLegitRotation(LivingEntity target) {
        ElytraTarget elytraTarget = getElytraTarget();
        return elytraTarget != null
                && mc.player != null
                && target != null
                && mc.player.isGliding()
                && target.isGliding()
                && elytraTarget.rotationMode.is("Legit");
    }

    private void updateElytraLegitRotation(LivingEntity target) {
        if (target == null || mc.player == null) {
            return;
        }

        Vec3d point;
        if (shouldUseElytraPredict(target)) {
            point = getPredictPoint(target, getElytraTarget().predictValue.getValue());
        } else if (shouldUseElytraPitchHoldPoint()) {
            point = getElytraPitchHoldPoint(target);
        } else {
            point = BestPoint.getPoint(target);
        }

        point = point.add(getElytraLegitRandomValue());

        Rotation currentAngle = new Rotation(lastYaw, lastPitch);
        Rotation targetAngle = new Rotation(RotationUtil.calculate(point));
        Rotation finalAngle = limitElytraLegitAngleChange(currentAngle, targetAngle);

        float newYaw = finalAngle.getYaw();
        float newPitch = MathHelper.clamp(finalAngle.getPitch(), -89.9f, 89.9f);

        float gcd = GCDFixer.getGCDValue();
        if (gcd > 0.0f) {
            newYaw = lastYaw + (float) Math.round((newYaw - lastYaw) / gcd) * gcd;
            newPitch = lastPitch + (float) Math.round((newPitch - lastPitch) / gcd) * gcd;
            newPitch = MathHelper.clamp(newPitch, -89.9f, 89.9f);
        }

        Rotation smoothRot = new Rotation(newYaw, newPitch);
        RotationComponent.update(smoothRot, 360, 360, 360, 360, 0, 1, clientLook.getValue());

        lastYaw = smoothRot.getYaw();
        lastPitch = smoothRot.getPitch();
        lastElytraLegitSentAngle = smoothRot;
    }

    private Rotation limitElytraLegitAngleChange(Rotation currentAngle, Rotation targetAngle) {
        if (currentAngle.equals(lastElytraLegitSentAngle) && currentAngle.getDelta(targetAngle) < 0.01f) {
            Rotation micro = getElytraLegitMicroJitter();
            return new Rotation(currentAngle.getYaw() + micro.getYaw(), currentAngle.getPitch() + micro.getPitch());
        }

        float yawDelta = normalizeElytraLegitAngle(MathHelper.wrapDegrees(targetAngle.getYaw() - currentAngle.getYaw()));
        float pitchDelta = normalizeElytraLegitAngle(MathHelper.wrapDegrees(targetAngle.getPitch() - currentAngle.getPitch()));

        yawDelta = applyElytraLegitHumanError(yawDelta);
        pitchDelta = applyElytraLegitHumanError(pitchDelta);

        if (Math.abs(pitchDelta) < 0.01f) {
            pitchDelta += getElytraLegitMicroJitter().getPitch();
        }

        handleElytraLegitPitchDirectionChange(pitchDelta);


        return new Rotation(currentAngle.getYaw() + yawDelta, currentAngle.getPitch() + pitchDelta);
    }

    private float normalizeElytraLegitAngle(float angle) {
        if (angle > 180.25f) {
            return angle - 360.0f;
        }
        if (angle < -180.25f) {
            return angle + 360.0f;
        }
        return angle;
    }

    private float applyElytraLegitHumanError(float value) {
        return value * 0.97f + (elytraLegitRandom.nextFloat() * 0.06f - 0.03f);
    }

    private Rotation getElytraLegitMicroJitter() {
        return new Rotation(
                elytraLegitRandom.nextFloat() * 0.02f - 0.01f,
                elytraLegitRandom.nextFloat() * 0.02f - 0.01f
        );
    }

    private void handleElytraLegitPitchDirectionChange(float pitchDelta) {
        int currentDirection = (int) Math.signum(pitchDelta);
        if (currentDirection != 0 && lastElytraLegitPitchChangeDirection == 0) {
            lastElytraLegitPitchChangeDirection = currentDirection;
            return;
        }
        if (lastElytraLegitPitchChangeDirection != 0 && currentDirection != lastElytraLegitPitchChangeDirection) {
            lastElytraLegitPitchChangeDirection = currentDirection;
        }
    }

    private Vec3d getElytraLegitRandomValue() {
        return new Vec3d(
                elytraLegitRandom.nextDouble() * 0.02 - 0.01,
                elytraLegitRandom.nextDouble() * 0.02 - 0.01,
                elytraLegitRandom.nextDouble() * 0.02 - 0.01
        );
    }

    private void resetElytraLegitRotationState() {
        lastElytraLegitPitchChangeDirection = 0;
        lastElytraLegitSentAngle = new Rotation(0.0f, 0.0f);
    }

    public void resetCameraReturnRotationState() {
        cameraReturnRotationPoint = Vec3d.ZERO;
        cameraReturnRotationMotion = Vec3d.ZERO;
    }


    private void renderRotationPoint(MatrixStack matrices, Camera camera, float tickDelta) {
        if (target == null) return;

        Rotation currentRot = new Rotation(lastYaw, lastPitch);
        Vec3d lookVec = currentRot.toVector();
        Vec3d eyePos = mc.player.getEyePos();
        Box aimBox = target.getBoundingBox();

        Vec3d endPoint = eyePos.add(lookVec.multiply(6.0));
        var raycast = aimBox.raycast(eyePos, endPoint);

        Vec3d renderPoint;
        if (raycast.isPresent()) {
            renderPoint = raycast.get();
        } else {
            renderPoint = BestPoint.getNearestPoint(target);
        }

        Vec3d camPos = camera.getPos();
        double renderX = renderPoint.x - camPos.x;
        double renderY = renderPoint.y - camPos.y;
        double renderZ = renderPoint.z - camPos.z;

        float size = 0.05f;
        int color = ColorProvider.getThemeColor();

        matrices.push();
        matrices.translate(renderX, renderY, renderZ);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = 0.5f;

        buffer.vertex(matrix, -size, -size, -size).color(r, g, b, a);
        buffer.vertex(matrix, size, -size, -size).color(r, g, b, a);
        buffer.vertex(matrix, size, -size, size).color(r, g, b, a);
        buffer.vertex(matrix, -size, -size, size).color(r, g, b, a);

        buffer.vertex(matrix, -size, size, -size).color(r, g, b, a);
        buffer.vertex(matrix, -size, size, size).color(r, g, b, a);
        buffer.vertex(matrix, size, size, size).color(r, g, b, a);
        buffer.vertex(matrix, size, size, -size).color(r, g, b, a);

        buffer.vertex(matrix, -size, -size, -size).color(r, g, b, a);
        buffer.vertex(matrix, -size, size, -size).color(r, g, b, a);
        buffer.vertex(matrix, size, size, -size).color(r, g, b, a);
        buffer.vertex(matrix, size, -size, -size).color(r, g, b, a);

        buffer.vertex(matrix, -size, -size, size).color(r, g, b, a);
        buffer.vertex(matrix, size, -size, size).color(r, g, b, a);
        buffer.vertex(matrix, size, size, size).color(r, g, b, a);
        buffer.vertex(matrix, -size, size, size).color(r, g, b, a);

        buffer.vertex(matrix, -size, -size, -size).color(r, g, b, a);
        buffer.vertex(matrix, -size, -size, size).color(r, g, b, a);
        buffer.vertex(matrix, -size, size, size).color(r, g, b, a);
        buffer.vertex(matrix, -size, size, -size).color(r, g, b, a);

        buffer.vertex(matrix, size, -size, -size).color(r, g, b, a);
        buffer.vertex(matrix, size, size, -size).color(r, g, b, a);
        buffer.vertex(matrix, size, size, size).color(r, g, b, a);
        buffer.vertex(matrix, size, -size, size).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        matrices.pop();
    }

    private void renderPredictPoint(MatrixStack matrices, Camera camera, float tickDelta) {
        if (!shouldUseElytraPredict(target)) return;

        Vec3d camPos = camera.getPos();
        int color = ColorProvider.rgb(255, 255, 255);

        matrices.push();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        Vec3d predictPos = getPredictPoint(target, getElytraTarget().predictValue.getValue());
        float size = 0.35f;
        float x1 = (float) (predictPos.x - camPos.x - size);
        float y1 = (float) (predictPos.y - camPos.y - size);
        float z1 = (float) (predictPos.z - camPos.z - size);
        float x2 = (float) (predictPos.x - camPos.x + size);
        float y2 = (float) (predictPos.y - camPos.y + size);
        float z2 = (float) (predictPos.z - camPos.z + size);
        putBoxOutline(buffer, matrix, x1, y1, z1, x2, y2, z2, color);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        matrices.pop();
    }

    private void putBoxOutline(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        buffer.vertex(matrix, x1, y1, z1).color(color);
        buffer.vertex(matrix, x2, y1, z1).color(color);
        buffer.vertex(matrix, x2, y1, z1).color(color);
        buffer.vertex(matrix, x2, y1, z2).color(color);
        buffer.vertex(matrix, x2, y1, z2).color(color);
        buffer.vertex(matrix, x1, y1, z2).color(color);
        buffer.vertex(matrix, x1, y1, z2).color(color);
        buffer.vertex(matrix, x1, y1, z1).color(color);

        buffer.vertex(matrix, x1, y2, z1).color(color);
        buffer.vertex(matrix, x2, y2, z1).color(color);
        buffer.vertex(matrix, x2, y2, z1).color(color);
        buffer.vertex(matrix, x2, y2, z2).color(color);
        buffer.vertex(matrix, x2, y2, z2).color(color);
        buffer.vertex(matrix, x1, y2, z2).color(color);
        buffer.vertex(matrix, x1, y2, z2).color(color);
        buffer.vertex(matrix, x1, y2, z1).color(color);

        buffer.vertex(matrix, x1, y1, z1).color(color);
        buffer.vertex(matrix, x1, y2, z1).color(color);
        buffer.vertex(matrix, x2, y1, z1).color(color);
        buffer.vertex(matrix, x2, y2, z1).color(color);
        buffer.vertex(matrix, x1, y1, z2).color(color);
        buffer.vertex(matrix, x1, y2, z2).color(color);
        buffer.vertex(matrix, x2, y1, z2).color(color);
        buffer.vertex(matrix, x2, y2, z2).color(color);
    }

    private void renderAttackRangeCircle(MatrixStack matrices, Camera camera, float tickDelta) {
        if (mc.player == null) {
            return;
        }

        float attackRange = (float) getEffectiveAttackRange();
        if (attackRange <= 0.01f) {
            return;
        }

        Vec3d playerPos = getInterpolatedEntityPos(mc.player, tickDelta);
        Vec3d camPos = camera.getPos();

        boolean canHitTarget = isTargetInAttackRangeForVisual(attackRange);

        float targetFactor = canHitTarget ? 1.0f : 0.0f;
        attackRangeReachAnimation += (targetFactor - attackRangeReachAnimation) * 0.12f;
        attackRangeReachAnimation = MathHelper.clamp(attackRangeReachAnimation, 0.0f, 1.0f);

        int baseColorA = ColorProvider.getThemeColor();
        int baseColorB = ColorProvider.getThemeColorTwo();
        int hitColor = ColorProvider.rgba(255, 80, 80, 255);
        int ringColorA = mixColors(baseColorA, hitColor, attackRangeReachAnimation * 0.95f);
        int ringColorB = mixColors(baseColorB, hitColor, attackRangeReachAnimation * 0.95f);

        boolean dashedMode = attackRangeCircleMode.is(ATTACK_RANGE_MODE_DASHED);

        float time = (mc.player.age + tickDelta) * 0.055f;
        float ringY = (float) (mc.player.getEyeHeight(mc.player.getPose()) * 0.34f);

        matrices.push();
        matrices.translate(playerPos.x - camPos.x, playerPos.y - camPos.y, playerPos.z - camPos.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        if (dashedMode) {
            renderDashedRangeRing(matrix, attackRange, ringY, time, ringColorA, ringColorB, canHitTarget);
        } else {
            renderSolidRangeRing(matrix, attackRange, ringY, ringColorA, ringColorB, canHitTarget);
            int sparkThemeColor = ringColorA;
            int sparkHotColor = sparkThemeColor;
            int sparkColdColor = sparkThemeColor;
            renderSnakeRangeSparks(matrix, attackRange, ringY, time, sparkHotColor, sparkColdColor);
        }

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private boolean isTargetInAttackRangeForVisual(float attackRange) {
        if (mc.player == null || target == null || !target.isAlive()) {
            return false;
        }

        Vec3d nearest = BestPoint.getNearestPoint(target);
        if (nearest == null) {
            return false;
        }

        return mc.player.getEyePos().distanceTo(nearest) <= (attackRange - 0.2f);
    }

    private void renderSolidRangeRing(
            Matrix4f matrix,
            float attackRange,
            float ringY,
            int ringColorA,
            int ringColorB,
            boolean canHitTarget
    ) {
        int segments = 132;
        RenderSystem.lineWidth(2.0f);
        BufferBuilder ringBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            float progress = i / (float) segments;
            float angle = progress * ((float) Math.PI * 2.0f);
            int mixedColor = mixColors(ringColorA, ringColorB, progress);
            int color = ColorProvider.setAlpha(mixedColor, canHitTarget ? 230 : 185);

            float x = MathHelper.cos(angle) * attackRange;
            float z = MathHelper.sin(angle) * attackRange;
            ringBuffer.vertex(matrix, x, ringY, z).color(color);
        }
        BufferRenderer.drawWithGlobalProgram(ringBuffer.end());

        RenderSystem.lineWidth(4.0f);
        BufferBuilder glowBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            float progress = i / (float) segments;
            float angle = progress * ((float) Math.PI * 2.0f);
            int mixedColor = mixColors(ringColorA, ringColorB, progress);
            int glowColor = ColorProvider.setAlpha(mixedColor, canHitTarget ? 105 : 75);

            float x = MathHelper.cos(angle) * attackRange;
            float z = MathHelper.sin(angle) * attackRange;
            glowBuffer.vertex(matrix, x, ringY + 0.01f, z).color(glowColor);
        }
        BufferRenderer.drawWithGlobalProgram(glowBuffer.end());
        RenderSystem.lineWidth(1.0f);
    }

    private void renderDashedRangeRing(
            Matrix4f matrix,
            float attackRange,
            float ringY,
            float time,
            int ringColorA,
            int ringColorB,
            boolean canHitTarget
    ) {
        int dashes = 56;
        float dashPart = 0.36f;
        float moveSpeed = 0.28f;
        float move = wrap01(time * moveSpeed);

        RenderSystem.lineWidth(2.2f);
        BufferBuilder dashBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < dashes; i++) {
            float step = 1.0f / dashes;
            float start = wrap01(i * step + move);
            float end = wrap01(start + step * dashPart);

            float angleStart = start * ((float) Math.PI * 2.0f);
            float angleEnd = end * ((float) Math.PI * 2.0f);
            float colorMix = 0.5f + 0.5f * MathHelper.sin(angleStart * 1.7f + time * 2.1f);
            int mixedColor = mixColors(ringColorA, ringColorB, colorMix);
            int color = ColorProvider.setAlpha(mixedColor, canHitTarget ? 240 : 190);

            dashBuffer.vertex(matrix, MathHelper.cos(angleStart) * attackRange, ringY, MathHelper.sin(angleStart) * attackRange).color(color);
            dashBuffer.vertex(matrix, MathHelper.cos(angleEnd) * attackRange, ringY, MathHelper.sin(angleEnd) * attackRange).color(color);
        }
        BufferRenderer.drawWithGlobalProgram(dashBuffer.end());

        RenderSystem.lineWidth(3.2f);
        BufferBuilder glowDashBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < dashes; i++) {
            float step = 1.0f / dashes;
            float start = wrap01(i * step + move);
            float end = wrap01(start + step * dashPart);

            float angleStart = start * ((float) Math.PI * 2.0f);
            float angleEnd = end * ((float) Math.PI * 2.0f);
            int mixedColor = mixColors(ringColorA, ringColorB, start);
            int glowColor = ColorProvider.setAlpha(mixedColor, canHitTarget ? 110 : 80);

            glowDashBuffer.vertex(matrix, MathHelper.cos(angleStart) * attackRange, ringY + 0.01f, MathHelper.sin(angleStart) * attackRange).color(glowColor);
            glowDashBuffer.vertex(matrix, MathHelper.cos(angleEnd) * attackRange, ringY + 0.01f, MathHelper.sin(angleEnd) * attackRange).color(glowColor);
        }
        BufferRenderer.drawWithGlobalProgram(glowDashBuffer.end());
        RenderSystem.lineWidth(1.0f);
    }

    private void renderSnakeRangeSparks(
            Matrix4f matrix,
            float attackRange,
            float ringY,
            float time,
            int sparkHotColor,
            int sparkColdColor
    ) {
        int snakeCount = 3;
        int tailSegments = 22;
        float tailStep = 0.018f;
        float speed = 0.34f;

        RenderSystem.lineWidth(1.7f);
        BufferBuilder sparkBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        for (int snake = 0; snake < snakeCount; snake++) {
            float headProgress = wrap01(time * speed + snake / (float) snakeCount);
            for (int seg = 0; seg < tailSegments; seg++) {
                float p0 = wrap01(headProgress - seg * tailStep);
                float p1 = wrap01(headProgress - (seg + 1) * tailStep);

                float angle0 = p0 * ((float) Math.PI * 2.0f);
                float angle1 = p1 * ((float) Math.PI * 2.0f);

                float wave0 = MathHelper.sin(angle0 * 4.0f + time * 6.2f + snake * 1.3f) * 0.12f;
                float wave1 = MathHelper.sin(angle1 * 4.0f + time * 6.2f + snake * 1.3f) * 0.12f;

                float radius0 = attackRange + wave0;
                float radius1 = attackRange + wave1;
                float y0 = ringY + 0.012f + MathHelper.cos(angle0 * 3.0f + time * 5.0f + snake) * 0.04f;
                float y1 = ringY + 0.012f + MathHelper.cos(angle1 * 3.0f + time * 5.0f + snake) * 0.04f;

                float fade0 = 1.0f - seg / (float) tailSegments;
                float fade1 = 1.0f - (seg + 1) / (float) tailSegments;
                int color0 = ColorProvider.setAlpha(mixColors(sparkHotColor, sparkColdColor, 1.0f - fade0), (int) (220 * fade0));
                int color1 = ColorProvider.setAlpha(mixColors(sparkHotColor, sparkColdColor, 1.0f - fade1), (int) (220 * fade1));

                sparkBuffer.vertex(matrix, MathHelper.cos(angle0) * radius0, y0, MathHelper.sin(angle0) * radius0).color(color0);
                sparkBuffer.vertex(matrix, MathHelper.cos(angle1) * radius1, y1, MathHelper.sin(angle1) * radius1).color(color1);
            }
        }

        BufferRenderer.drawWithGlobalProgram(sparkBuffer.end());
        RenderSystem.lineWidth(1.0f);
    }

    private float wrap01(float value) {
        return value - (float) Math.floor(value);
    }

    private Vec3d getInterpolatedEntityPos(Entity entity, float tickDelta) {
        return new Vec3d(
                MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ())
        );
    }

    private int mixColors(int from, int to, float factor) {
        factor = MathHelper.clamp(factor, 0.0f, 1.0f);

        int fromA = (from >> 24) & 0xFF;
        int fromR = (from >> 16) & 0xFF;
        int fromG = (from >> 8) & 0xFF;
        int fromB = from & 0xFF;

        int toA = (to >> 24) & 0xFF;
        int toR = (to >> 16) & 0xFF;
        int toG = (to >> 8) & 0xFF;
        int toB = to & 0xFF;

        int a = (int) (fromA + (toA - fromA) * factor);
        int r = (int) (fromR + (toR - fromR) * factor);
        int g = (int) (fromG + (toG - fromG) * factor);
        int b = (int) (fromB + (toB - fromB) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public void onEnable() {
        target = null;
        attackRangeReachAnimation = 0.0f;
        resetElytraLegitRotationState();
        returningToCamera = false;
        ftRotation.reset();

        if (!renderListenerRegistered) {
            WorldRenderEvents.LAST.register(renderListener);
            renderListenerRegistered = true;
        }

        super.onEnable();
    }

    @Override
    public void onDisable() {
        target = null;
        ticksToAttack = 0;
        isResolving = false;
        resolverPoint = null;
        attackRangeReachAnimation = 0.0f;
        resetElytraLegitRotationState();
        returningToCamera = false;
        ftRotation.reset();
        super.onDisable();
    }
}