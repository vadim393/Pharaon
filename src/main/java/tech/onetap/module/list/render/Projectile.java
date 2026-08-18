package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import tech.onetap.event.list.EventWorldRender;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.util.player.combat.RaytraceUtil;
import tech.onetap.util.friend.FriendRepository;
import tech.onetap.util.player.other.WorldUtils;
import tech.onetap.util.render.math.ProjectionUtil;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;
import tech.onetap.util.rotation.Rotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

@ModuleInformation(
        moduleName = "Projectile",
        moduleDesc = "Показывает куда упадет предмет/стрела/эндер перл",
        moduleCategory = ModuleCategory.RENDER
)
public final class Projectile extends Module {

    private record Point(ItemStack stack, Vec3d pos, int ticks, Vec3d direction, boolean hitsEntity, Vec3d surfaceNormal) {}

    private record Physics(float dragAir, float dragWater, float gravity) {}

    private final List<Point> points = new ArrayList<>();
    private Point hoveredPoint = null;


    @Subscribe
    public void onWorldRender(EventWorldRender e) {
        if (mc.player == null || mc.world == null) return;

        points.clear();
        hoveredPoint = null;

        drawPredictionInHand(e.getMatrixStack(), StreamSupport.stream(mc.player.getHandItems().spliterator(), false).toList(), e.getTickDelta());

        double mouseX = mc.mouse.getX() / mc.getWindow().getScaleFactor();
        double mouseY = mc.mouse.getY() / mc.getWindow().getScaleFactor();

        renderPoints(mouseX, mouseY);
    }

    private void renderPoints(double mouseX, double mouseY) {
        for (Point point : points) {
            Vec3d pos = point.pos;

            var screen = ProjectionUtil.project(pos);
            boolean isHovered = false;
            if (screen.getX() != Float.MAX_VALUE && screen.getY() != Float.MAX_VALUE) {
                double distance = Math.sqrt(Math.pow(screen.getX() - mouseX, 2) + Math.pow(screen.getY() - mouseY, 2));
                isHovered = distance < 20;
                if (isHovered) {
                    hoveredPoint = point;
                }
            }

            int color = point.hitsEntity ? ColorProvider.rgba(0, 255, 0, 255) : ColorProvider.rgba(255, 0, 0, 255);

            drawCircleOnSurface(point.pos, 0.3, color, point.surfaceNormal);

            if (isHovered) {
                Vec3d direction = point.direction.multiply(2.0);
                DrawUtil.drawLine(point.pos, point.pos.add(direction), ColorProvider.rgba(255, 255, 255, 200), 1.5f, false);
                DrawUtil.drawLine(point.pos, point.pos.add(point.direction.multiply(-2.0)), ColorProvider.rgba(255, 255, 255, 200), 1.5f, false);
            }
        }
    }

    private void drawCircleOnSurface(Vec3d center, double radius, int color, Vec3d normal) {
        normal = normal.normalize();
        
        Vec3d u, v;
        if (Math.abs(normal.y) > 0.9) {
            u = new Vec3d(1, 0, 0);
        } else {
            u = new Vec3d(0, 1, 0);
        }
        u = u.crossProduct(normal).normalize();
        v = normal.crossProduct(u).normalize();
        
        int segments = 32;
        for (int i = 0; i < segments; i++) {
            double angle1 = (i / (double) segments) * Math.PI * 2;
            double angle2 = ((i + 1) / (double) segments) * Math.PI * 2;
            
            double cos1 = Math.cos(angle1) * radius;
            double sin1 = Math.sin(angle1) * radius;
            double cos2 = Math.cos(angle2) * radius;
            double sin2 = Math.sin(angle2) * radius;
            
            Vec3d p1 = center.add(u.multiply(cos1)).add(v.multiply(sin1));
            Vec3d p2 = center.add(u.multiply(cos2)).add(v.multiply(sin2));
            
            DrawUtil.drawLine(p1, p2, color, 1.5f, false);
        }
        
        double crossSize = radius;
        DrawUtil.drawLine(center.add(u.multiply(-crossSize)), center.add(u.multiply(crossSize)), color, 1.5f, false);
        DrawUtil.drawLine(center.add(v.multiply(-crossSize)), center.add(v.multiply(crossSize)), color, 1.5f, false);
    }

    private Vec3d getSurfaceNormal(HitResult result, Vec3d fallback) {
        if (result instanceof BlockHitResult blockHit) {
            Direction side = blockHit.getSide();
            return new Vec3d(side.getOffsetX(), side.getOffsetY(), side.getOffsetZ());
        }
        return fallback;
    }

    private void drawCircle(Vec3d center, double radius, int color) {
        int segments = 32;
        for (int i = 0; i < segments; i++) {
            double angle1 = (i / (double) segments) * Math.PI * 2;
            double angle2 = ((i + 1) / (double) segments) * Math.PI * 2;

            Vec3d p1 = center.add(Math.cos(angle1) * radius, 0, Math.sin(angle1) * radius);
            Vec3d p2 = center.add(Math.cos(angle2) * radius, 0, Math.sin(angle2) * radius);

            DrawUtil.drawLine(p1, p2, color, 2, false);
        }
    }

    private void drawPredictionInHand(net.minecraft.client.util.math.MatrixStack matrix, List<ItemStack> stacks, float tickDelta) {
        Item activeItem = mc.player.getActiveItem().getItem();

        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;

            List<HitResult> results = switch (stack.getItem()) {
                case ExperienceBottleItem item -> checkTrajectory(stack, 0.8, getPhysics(stack.getItem()), tickDelta);
                case SplashPotionItem item -> checkTrajectory(stack, 0.55, getPhysics(stack.getItem()), tickDelta);
                case LingeringPotionItem item -> checkTrajectory(stack, 0.55, getPhysics(stack.getItem()), tickDelta);
                case TridentItem item when item.equals(activeItem) && mc.player.getItemUseTime() >= 10 ->
                        checkTrajectory(stack, 2.5, getPhysics(stack.getItem()), tickDelta);
                case SnowballItem item -> checkTrajectory(stack, 1.5, getPhysics(stack.getItem()), tickDelta);
                case EggItem item -> checkTrajectory(stack, 1.5, getPhysics(stack.getItem()), tickDelta);
                case EnderPearlItem item -> checkTrajectory(stack, 1.5, getPhysics(stack.getItem()), tickDelta);
                case BowItem item when item.equals(activeItem) && mc.player.isUsingItem() -> {
                    float pull = BowItem.getPullProgress((int) (mc.player.getItemUseTime() + mc.getRenderTickCounter().getTickDelta(false)));
                    double velocity = 3 * MathHelper.clamp(pull, 0F, 1F);
                    yield checkTrajectory(stack, velocity, getPhysics(stack.getItem()), tickDelta);
                }
                case CrossbowItem item when CrossbowItem.isCharged(stack) -> {
                    ChargedProjectilesComponent component = stack.get(DataComponentTypes.CHARGED_PROJECTILES);
                    List<HitResult> list = new ArrayList<>();
                    if (component != null && !component.getProjectiles().isEmpty()) {
                        float velocity = component.getProjectiles().getFirst().isOf(Items.FIREWORK_ROCKET) ? 100 : 3;


                        list.add(checkTrajectoryWithLook(stack, mc.player.getRotationVec(tickDelta), velocity, getPhysics(stack.getItem()), true));


                        if (component.getProjectiles().size() > 2) {
                            float pitchAbs = mc.player.getPitch() / 90;
                            float delta = pitchAbs * pitchAbs * pitchAbs * pitchAbs * pitchAbs;
                            float yaw = MathHelper.lerp(Math.abs(delta), 10, 90);
                            float pitch = MathHelper.lerp(delta, 0, 10);
                            list.add(checkTrajectoryWithLook(stack, new Rotation(mc.player.getYaw() - yaw, mc.player.getPitch() - pitch).toVector(), velocity, getPhysics(stack.getItem()), true));
                            list.add(checkTrajectoryWithLook(stack, new Rotation(mc.player.getYaw() + yaw, mc.player.getPitch() - pitch).toVector(), velocity, getPhysics(stack.getItem()), true));
                        }
                    }
                    yield list;
                }
                default -> null;
            };

            if (results != null) {
                results = results.stream().filter(Objects::nonNull).toList();
                if (!results.isEmpty()) {
                    renderProjectileResults(results, mc.player.getRotationVec(tickDelta));
                }
            }
        }
    }

    private void renderProjectileResults(List<HitResult> results, Vec3d motion) {
        for (HitResult result : results) {
            Vec3d pos = result.getPos();

            double mouseX = mc.mouse.getX() / mc.getWindow().getScaleFactor();
            double mouseY = mc.mouse.getY() / mc.getWindow().getScaleFactor();
            var screen = ProjectionUtil.project(pos);
            boolean isHovered = false;
            if (screen.getX() != Float.MAX_VALUE && screen.getY() != Float.MAX_VALUE) {
                double distance = Math.sqrt(Math.pow(screen.getX() - mouseX, 2) + Math.pow(screen.getY() - mouseY, 2));
                isHovered = distance < 20;
            }

            boolean hitsEntity = result.getType() == HitResult.Type.ENTITY;
            int color = hitsEntity ? ColorProvider.rgba(0, 255, 0, 255) : ColorProvider.rgba(255, 0, 0, 255);
            
            Vec3d normal = getSurfaceNormal(result, motion.normalize());
            drawCircleOnSurface(pos, 0.3, color, normal);

            if (isHovered) {
                Vec3d direction = motion.normalize().multiply(2.0);
                DrawUtil.drawLine(pos, pos.add(direction), ColorProvider.rgba(255, 255, 255, 200), 1.5f, false);
                DrawUtil.drawLine(pos, pos.add(motion.normalize().multiply(-2.0)), ColorProvider.rgba(255, 255, 255, 200), 1.5f, false);
            }
        }
    }

    public List<Entity> getProjectiles() {
        return StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                .filter(e -> (e instanceof PersistentProjectileEntity || e instanceof ThrownItemEntity || e instanceof ItemEntity) && !visible(e))
                .toList();
    }

    private List<HitResult> checkTrajectory(ItemStack stack, double velocity, Physics physics, float tickDelta) {
        return new ArrayList<>(Collections.singleton(checkTrajectoryWithLook(stack, mc.player.getRotationVec(tickDelta), velocity, physics, false)));
    }

    private HitResult checkTrajectoryWithLook(ItemStack stack, Vec3d lookVec, double velocity, Physics physics, boolean zeroMotion) {
        double distance = lookVec.length();
        if (distance == 0) return null;

        Vec3d motion = mc.player.getPos().subtract(mc.player.prevX, mc.player.prevY, mc.player.prevZ);
        if (zeroMotion) motion = Vec3d.ZERO;

        Vec3d start = mc.player.getEyePos();
        return traceTrajectory(stack, start, lookVec.multiply(velocity / distance).add(motion), physics);
    }

    private HitResult traceTrajectory(ItemStack stack, Vec3d start, Vec3d motion, Physics physics) {
        List<Vec3d> trajectoryPoints = new ArrayList<>();
        trajectoryPoints.add(start);
        
        Vec3d pos = start;
        Vec3d prevPos;
        Vec3d currentMotion = motion;
        
        HitResult finalResult = null;
        int finalTicks = 0;
        Vec3d finalMotion = currentMotion;
        boolean hitsEntity = false;
        
        for (int i = 0; i < 300; i++) {
            prevPos = pos;
            pos = pos.add(currentMotion);
            trajectoryPoints.add(pos);

            currentMotion = calculateMotion(prevPos, currentMotion, physics);

            HitResult result = RaytraceUtil.raycast(prevPos, pos, RaycastContext.ShapeType.COLLIDER, mc.player);
            
            if (result.getType() != HitResult.Type.MISS) {
                finalResult = result;
                finalTicks = i;
                finalMotion = currentMotion;
                hitsEntity = false;
                break;
            }

            Vec3d finalPrevPos = prevPos, finalPos = pos;
            boolean hitEntity = StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                    .filter(ent -> ent instanceof LivingEntity living && living != mc.player && living.isAlive())
                    .anyMatch(ent -> canHitEntityOnTrajectory(ent, finalPrevPos, finalPos));
            if (hitEntity) {
                Vec3d p = pos;
                finalResult = new HitResult(p) {
                    @Override
                    public Type getType() {
                        return Type.ENTITY;
                    }
                };
                finalTicks = i;
                finalMotion = currentMotion;
                hitsEntity = true;
                break;
            }

            if (pos.y < -128) {
                finalResult = null;
                break;
            }
        }
        
        int color = hitsEntity ? ColorProvider.rgba(0, 255, 0, 200) : ColorProvider.rgba(255, 0, 0, 200);
        
        for (int i = 0; i < trajectoryPoints.size() - 1; i++) {
            DrawUtil.drawLine(trajectoryPoints.get(i), trajectoryPoints.get(i + 1), color, 1.5f, false);
        }
        
        if (finalResult != null) {
            points.add(new Point(stack.copy(), finalResult.getPos(), finalTicks, finalMotion.normalize(), hitsEntity, getSurfaceNormal(finalResult, finalMotion.normalize())));
        }
        
        return finalResult;
    }

    private boolean canHitEntityOnTrajectory(Entity entity, Vec3d start, Vec3d end) {
        if (entity instanceof PlayerEntity player && !FriendRepository.shouldAttack(player)) {
            return false;
        }
        if (!entity.getBoundingBox().expand(0.3).intersects(start, end)) {
            return false;
        }
        HitResult visibilityCheck = mc.world.raycast(new RaycastContext(
                start,
                entity.getBoundingBox().getCenter(),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return visibilityCheck.getType() == HitResult.Type.MISS;
    }

    private Vec3d calculateMotion(Vec3d prevPos, Vec3d motion, Physics physics) {
        boolean isInWater = Objects.requireNonNull(mc.world)
                .getBlockState(BlockPos.ofFloored(prevPos))
                .getFluidState()
                .isIn(FluidTags.WATER);

        float drag = isInWater ? physics.dragWater : physics.dragAir;
        return motion.multiply(drag).add(0, -physics.gravity, 0);
    }

    public Vec3d calculateMotion(Entity entity, Vec3d prevPos, Vec3d motion) {
        boolean isInWater = Objects.requireNonNull(mc.world).getBlockState(BlockPos.ofFloored(prevPos)).getFluidState().isIn(FluidTags.WATER);

        float multiply = switch (entity) {
            case TridentEntity i -> 0.99F;
            case PersistentProjectileEntity i when isInWater -> 0.6F;
            default -> isInWater ? 0.8F : 0.99F;
        };

        return motion.multiply(multiply).add(0, -entity.getFinalGravity(), 0);
    }

    private void breakingBad(Entity entity, Vec3d pos, int ticks, Vec3d direction, boolean hitsEntity, Vec3d surfaceNormal) {
        switch (entity) {
            case ItemEntity item -> points.add(new Point(item.getStack(), pos, ticks, direction, hitsEntity, surfaceNormal));
            case ThrownItemEntity thrown -> points.add(new Point(thrown.getStack(), pos, ticks, direction, hitsEntity, surfaceNormal));
            case PersistentProjectileEntity persistent -> points.add(new Point(persistent.getItemStack(), pos, ticks, direction, hitsEntity, surfaceNormal));
            default -> {
            }
        }
    }


    private boolean visible(Entity entity) {
        boolean posChange = entity.getX() == entity.prevX && entity.getY() == entity.prevY && entity.getZ() == entity.prevZ;
        boolean itemEntityCheck = entity instanceof ItemEntity && (entity.isOnGround() || WorldUtils.isBoxInBlock(entity.getBoundingBox().expand(2), Blocks.WATER));
        return posChange || itemEntityCheck;
    }

    private Physics getPhysics(Item item) {
        if (item instanceof ExperienceBottleItem) return new Physics(0.99f, 0.8f, 0.07f);
        if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) return new Physics(0.99f, 0.8f, 0.05f);
        if (item instanceof EnderPearlItem) return new Physics(0.99f, 0.8f, 0.03f);
        if (item instanceof SnowballItem || item instanceof EggItem) return new Physics(0.99f, 0.8f, 0.03f);
        if (item instanceof BowItem || item instanceof CrossbowItem) return new Physics(0.99f, 0.6f, 0.05f);
        if (item instanceof TridentItem) return new Physics(0.99f, 0.8f, 0.05f);
        return new Physics(0.99f, 0.8f, 0.03f);
    }
}
