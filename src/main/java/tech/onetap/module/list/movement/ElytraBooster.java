package tech.onetap.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.onetap.Onetap;
import tech.onetap.event.list.EventTick;
import tech.onetap.event.list.FireworkEvent;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.combat.ElytraTarget;
import tech.onetap.module.list.combat.KillAura;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.player.combat.PredictUtils;

@ModuleInformation(moduleName = "Elytra Booster", moduleDesc = "Усиленные фейерверки на элитре", moduleCategory = ModuleCategory.MOVEMENT)
public class ElytraBooster extends Module {
    public final ModeSetting mode = new ModeSetting("Режим", "Custom", "Bravo Grief", "Bravo FFA", "Bravo Complex", "Rage", "Custom");
    private final SliderSetting customXZ_0_5 = new SliderSetting("XZ 0-5°", 1.61, 0.01, 3.0, 0.01).setVisible(() -> mode.is("Custom"));
    private final SliderSetting customXZ_5_10 = new SliderSetting("XZ 5-10°", 1.61, 0.01, 3.0, 0.01).setVisible(() -> mode.is("Custom"));
    private final SliderSetting customXZ_10_15 = new SliderSetting("XZ 10-15°", 1.61, 0.01, 3.0, 0.01).setVisible(() -> mode.is("Custom"));
    private final SliderSetting customXZ_15_20 = new SliderSetting("XZ 15-20°", 1.61, 0.01, 3.0, 0.01).setVisible(() -> mode.is("Custom"));
    private final SliderSetting customXZ_20_25 = new SliderSetting("XZ 20-25°", 1.61, 0.01, 3.0, 0.01).setVisible(() -> mode.is("Custom"));
    private final SliderSetting customXZ_25_30 = new SliderSetting("XZ 25-30°", 1.61, 0.01, 3.0, 0.01).setVisible(() -> mode.is("Custom"));
    private final SliderSetting customXZ_30_35 = new SliderSetting("XZ 30-35°", 1.61, 0.01, 3.0, 0.01).setVisible(() -> mode.is("Custom"));
    private final SliderSetting customXZ_35_40 = new SliderSetting("XZ 35-40°", 1.61, 0.01, 3.0, 0.01).setVisible(() -> mode.is("Custom"));
    private final SliderSetting customXZ_40_45 = new SliderSetting("XZ 40-45°", 1.61, 0.01, 3.0, 0.01).setVisible(() -> mode.is("Custom"));
    private final SliderSetting customY_0_5 = new SliderSetting("Y 0-5°", 1.61, 0.01, 3.0, 0.01).setVisible(() -> mode.is("Custom"));
    private final SliderSetting customY_5_10 = new SliderSetting("Y 5-10°", 1.61, 0.01, 3.0, 0.01).setVisible(() -> mode.is("Custom"));
    private final SliderSetting customY_10_15 = new SliderSetting("Y 10-15°", 1.61, 0.01, 3.0, 0.01).setVisible(() -> mode.is("Custom"));
    private final SliderSetting customY_15_20 = new SliderSetting("Y 15-20°", 1.61, 0.01, 3.0, 0.01).setVisible(() -> mode.is("Custom"));
    private final SliderSetting customY_20_25 = new SliderSetting("Y 20-25°", 1.61, 0.01, 3.0, 0.01).setVisible(() -> mode.is("Custom"));
    private final SliderSetting customY_25_30 = new SliderSetting("Y 25-30°", 1.61, 0.01, 3.0, 0.01).setVisible(() -> mode.is("Custom"));
    private final SliderSetting customY_30_35 = new SliderSetting("Y 30-35°", 1.61, 0.01, 3.0, 0.01).setVisible(() -> mode.is("Custom"));
    private final SliderSetting customY_35_40 = new SliderSetting("Y 35-40°", 1.61, 0.01, 3.0, 0.01).setVisible(() -> mode.is("Custom"));
    private final SliderSetting customY_40_45 = new SliderSetting("Y 40-45°", 1.61, 0.01, 3.0, 0.01).setVisible(() -> mode.is("Custom"));
    private double[] lastSliderValues = new double[18];
    private boolean firstTick = true;
    private static final float[] BRAVO_GRIEF_XZ = new float[]{1.73F, 1.72F, 1.7F, 1.7F, 1.82F, 1.85F, 1.92F, 2.04F, 2.04F};
    private static final float[] BRAVO_GRIEF_Y = new float[]{1.68F, 1.7F, 1.74F, 1.77F, 1.82F, 1.83F, 1.82F, 2.04F, 2.04F};
    private static final float[] BRAVO_FFA_XZ = new float[]{1.66F, 1.66F, 1.72F, 1.77F, 1.78F, 1.84F, 1.92F, 1.96F, 2.04F};
    private static final float[] BRAVO_FFA_Y = new float[]{1.65F, 1.66F, 1.7F, 1.72F, 1.61F, 1.91F, 1.98F, 1.99F, 2.03F};
    private static final float[] RAGE_XZ = new float[]{1.61F, 1.62F, 1.65F, 1.7F, 1.76F, 1.83F, 1.91F, 2.01F, 2.12F};
    private static final float[] RAGE_Y = new float[]{1.61F, 1.62F, 1.67F, 1.72F, 1.79F, 1.87F, 1.98F, 2.09F, 2.21F};

    @Subscribe
    public void onTick(EventTick event) {
        checkSliderChanges();
    }

    private void checkSliderChanges() {
        double[] currentValues = new double[]{
                customXZ_0_5.getValue(),
                customXZ_5_10.getValue(),
                customXZ_10_15.getValue(),
                customXZ_15_20.getValue(),
                customXZ_20_25.getValue(),
                customXZ_25_30.getValue(),
                customXZ_30_35.getValue(),
                customXZ_35_40.getValue(),
                customXZ_40_45.getValue(),
                customY_0_5.getValue(),
                customY_5_10.getValue(),
                customY_10_15.getValue(),
                customY_15_20.getValue(),
                customY_20_25.getValue(),
                customY_25_30.getValue(),
                customY_30_35.getValue(),
                customY_35_40.getValue(),
                customY_40_45.getValue()
        };
        if (firstTick) {
            System.arraycopy(currentValues, 0, lastSliderValues, 0, currentValues.length);
            firstTick = false;
        } else {
            for (int i = 0; i < currentValues.length; i++) {
                if (Math.abs(currentValues[i] - lastSliderValues[i]) > 1.0E-4) {
                    break;
                }
            }

            System.arraycopy(currentValues, 0, lastSliderValues, 0, currentValues.length);
        }
    }

    @Subscribe
    private void onFirework(FireworkEvent event) {
        LivingEntity boosted = event.getBoostedEntity();
        if (mc.player != null && boosted == mc.player) {
            float speed = getSpeed();
            KillAura aura = Onetap.getInstance().getModuleStorage().get(KillAura.class);
            ElytraTarget ev = Onetap.getInstance().getModuleStorage().get(ElytraTarget.class);
            if (aura != null && ev != null && ev.isEnabled()) {
                LivingEntity target = aura.getTarget();
                if (aura.isEnabled() && ev.elytraSlowdown.getValue() && target != null) {
                    if (ev.slowdownMode.is("По радиусу")) {
                        Vec3d predictedPos = PredictUtils.predict(target, ev.predictValue.getValue());
                        double dist = mc.player.getEyePos().distanceTo(predictedPos);
                        double radius = ev.slowdownRadius.getValue();
                        if (dist < radius) {
                            double ratio = MathHelper.clamp(dist / radius, 0.0, 1.0);
                            double smoothCurve = ratio * ratio * (3.0 - 2.0 * ratio);
                            float min = ev.minSpeed.getFloatValue();
                            speed *= (float) (min + (1.0F - min) * smoothCurve);
                        }
                    } else if (ev.slowdownMode.is("Перед ударом") && KillAura.isSlowdownActive) {
                        speed = 1.3F;
                    } else if (ev.slowdownMode.is("Синхронизация с таргетом")) {
                        Vec3d predictedPos = PredictUtils.predict(target, ev.predictValue.getValue());
                        double dist = mc.player.getEyePos().distanceTo(predictedPos);
                        double syncDistance = ev.syncDistance.getValue();
                        if (dist <= syncDistance) {
                            Vec3d targetVelocity = new Vec3d(target.getX() - target.prevX, target.getY() - target.prevY, target.getZ() - target.prevZ);
                            double targetSpeed = targetVelocity.length();
                            Vec3d playerVelocity = mc.player.getVelocity();
                            double playerSpeed = playerVelocity.length();
                            if (playerSpeed > 0.01) {
                                float targetMultiplier = (float) (targetSpeed / playerSpeed);
                                speed = MathHelper.lerp(0.3F, speed, targetMultiplier);
                                speed = Math.max(speed, 0.5F);
                            }
                        }
                    }
                }

                event.setSpeed(speed);
            } else {
                event.setSpeed(speed);
            }
        }
    }

    private float interpolateBravo(float value, float min, float max, float speedMin, float speedMax) {
        float t = (value - min) / (max - min);
        return MathHelper.lerp(t, speedMin, speedMax);
    }

    private float getSpeed() {
        float pitch = Math.abs(mc.player.getPitch());
        float yaw = Math.abs(MathHelper.wrapDegrees(mc.player.getYaw()));
        if (yaw > 180.0F) {
            yaw = 360.0F - yaw;
        }

        if (yaw > 90.0F) {
            yaw = 180.0F - yaw;
        }

        float xzSpeed = getXZSpeed(yaw);
        float ySpeed = getYSpeed(pitch);
        return Math.max(xzSpeed, ySpeed);
    }

    private float getXZSpeed(float yaw) {
        if (mode.is("Bravo Grief")) {
            return getBravoGriefXZSpeed(yaw);
        } else if (mode.is("Bravo FFA")) {
            return getBravoFFAXZSpeed(yaw);
        } else if (mode.is("Bravo Complex")) {
            return getBravoComplexXZSpeed(yaw);
        } else {
            return mode.is("Rage") ? getRageXZSpeed(yaw) : getCustomXZSpeed(yaw);
        }
    }

    private float getYSpeed(float pitch) {
        if (mode.is("Bravo Grief")) {
            return getBravoGriefYSpeed(pitch);
        } else if (mode.is("Bravo FFA")) {
            return getBravoFFAYSpeed(pitch);
        } else if (mode.is("Bravo Complex")) {
            return getBravoComplexYSpeed(pitch);
        } else {
            return mode.is("Rage") ? getRageYSpeed(pitch) : getCustomYSpeed(pitch);
        }
    }

    private float getBravoGriefXZSpeed(float yaw) {
        return getValueFromTable(yaw, BRAVO_GRIEF_XZ);
    }

    private float getBravoGriefYSpeed(float pitch) {
        return getValueFromTable(pitch, BRAVO_GRIEF_Y);
    }

    private float getBravoFFAXZSpeed(float yaw) {
        return getValueFromTable(yaw, BRAVO_FFA_XZ);
    }

    private float getBravoFFAYSpeed(float pitch) {
        return getValueFromTable(pitch, BRAVO_FFA_Y);
    }

    private float getRageXZSpeed(float yaw) {
        return getValueFromTable(yaw, RAGE_XZ);
    }

    private float getRageYSpeed(float pitch) {
        return getValueFromTable(pitch, RAGE_Y);
    }

    private float getBravoComplexXZSpeed(float yaw) {
        float pitch = Math.abs(mc.player.getPitch());
        return getBravoComplexXZ(pitch, yaw);
    }

    private float getBravoComplexYSpeed(float pitch) {
        return getBravoComplexY(pitch);
    }

    private float getBravoComplexXZ(float pitch, float yaw) {
        float absPitch = Math.abs(pitch);
        float absYaw = Math.abs(MathHelper.wrapDegrees(yaw) % 90.0F);
        float speed = absPitch >= 38.0F && absPitch <= 52.0F
                ? 2.0F
                : (absPitch >= 32.0F && absPitch <= 58.0F
                ? 1.96F
                : (absPitch >= 28.0F && absPitch <= 62.0F
                ? 1.95F
                : ((!(absYaw >= 29.0F) || !(absYaw <= 61.0F)) && (!(absPitch >= 29.0F) || !(absPitch <= 61.0F))
                ? ((!(absYaw >= 28.0F) || !(absYaw <= 60.0F)) && (!(absPitch >= 28.0F) || !(absPitch <= 60.0F))
                ? ((!(absYaw >= 26.0F) || !(absYaw <= 64.0F)) && (!(absPitch >= 26.0F) || !(absPitch <= 64.0F))
                ? ((!(absYaw >= 24.0F) || !(absYaw <= 66.0F)) && (!(absPitch >= 24.0F) || !(absPitch <= 66.0F))
                ? ((!(absYaw >= 15.0F) || !(absYaw <= 75.0F)) && (!(absPitch >= 15.0F) || !(absPitch <= 75.0F))
                ? ((!(absYaw >= 13.0F) || !(absYaw <= 77.0F)) && (!(absPitch >= 13.0F) || !(absPitch <= 77.0F))
                ? ((!(absYaw >= 12.0F) || !(absYaw <= 78.0F)) && (!(absPitch >= 12.0F) || !(absPitch <= 78.0F))
                ? ((!(absYaw >= 8.0F) || !(absYaw <= 82.0F)) && (!(absPitch >= 11.0F) || !(absPitch <= 79.0F))
                ? ((!(absYaw >= 5.0F) || !(absYaw <= 85.0F)) && (!(absPitch >= 8.0F) || !(absPitch <= 82.0F))
                ? (!(absYaw <= 90.0F) && !(absPitch <= 90.0F) ? 1.66F : 1.67F)
: 1.67F)
                        : 1.75F)
                        : 1.75F)
                        : 1.75F)
                        : 1.75F)
                        : 1.75F)
                        : 1.874F)
                        : 1.954F)
                        : 1.963F)));
        return pitch > 15.0F ? speed - 0.068F : speed;
    }

    private float getBravoComplexY(float pitch) {
        float absPitch = Math.abs(pitch);
        if (absPitch >= 37.0F && absPitch <= 38.0F) {
            return 2.03F;
        } else if (absPitch >= 25.0F && absPitch <= 30.0F) {
            return 2.0F;
        } else if (absPitch >= 35.0F && absPitch <= 45.0F) {
            return 1.99F;
        } else if (absPitch >= 40.0F && absPitch <= 50.0F) {
            return 1.97F;
        } else if (absPitch >= 50.0F && absPitch <= 60.0F) {
            return 1.96F;
        } else if (absPitch >= 51.0F && absPitch <= 61.0F) {
            return 1.85F;
        } else {
            return absPitch >= 52.0F && absPitch <= 65.0F ? 1.8F : 1.59F;
        }
    }

    private float getValueFromTable(float angle, float[] table) {
        if (angle <= 5.0F) {
            return table[0];
        } else if (angle <= 10.0F) {
            return interpolateBravo(angle, 5.0F, 10.0F, table[0], table[1]);
        } else if (angle <= 15.0F) {
            return interpolateBravo(angle, 10.0F, 15.0F, table[1], table[2]);
        } else if (angle <= 20.0F) {
            return interpolateBravo(angle, 15.0F, 20.0F, table[2], table[3]);
        } else if (angle <= 25.0F) {
            return interpolateBravo(angle, 20.0F, 25.0F, table[3], table[4]);
        } else if (angle <= 30.0F) {
            return interpolateBravo(angle, 25.0F, 30.0F, table[4], table[5]);
        } else if (angle <= 35.0F) {
            return interpolateBravo(angle, 30.0F, 35.0F, table[5], table[6]);
        } else {
            return angle <= 40.0F ? interpolateBravo(angle, 35.0F, 40.0F, table[6], table[7]) : table[8];
        }
    }

    private float getCustomXZSpeed(float yaw) {
        if (yaw <= 5.0F) {
            return interpolateBravo(yaw, 0.0F, 5.0F, customXZ_0_5.getFloatValue(), customXZ_5_10.getFloatValue());
        } else if (yaw <= 10.0F) {
            return interpolateBravo(yaw, 5.0F, 10.0F, customXZ_5_10.getFloatValue(), customXZ_10_15.getFloatValue());
        } else if (yaw <= 15.0F) {
            return interpolateBravo(yaw, 10.0F, 15.0F, customXZ_10_15.getFloatValue(), customXZ_15_20.getFloatValue());
        } else if (yaw <= 20.0F) {
            return interpolateBravo(yaw, 15.0F, 20.0F, customXZ_15_20.getFloatValue(), customXZ_20_25.getFloatValue());
        } else if (yaw <= 25.0F) {
            return interpolateBravo(yaw, 20.0F, 25.0F, customXZ_20_25.getFloatValue(), customXZ_25_30.getFloatValue());
        } else if (yaw <= 30.0F) {
            return interpolateBravo(yaw, 25.0F, 30.0F, customXZ_25_30.getFloatValue(), customXZ_30_35.getFloatValue());
        } else if (yaw <= 35.0F) {
            return interpolateBravo(yaw, 30.0F, 35.0F, customXZ_30_35.getFloatValue(), customXZ_35_40.getFloatValue());
        } else {
            return yaw <= 40.0F
                    ? interpolateBravo(yaw, 35.0F, 40.0F, customXZ_35_40.getFloatValue(), customXZ_40_45.getFloatValue())
                    : customXZ_40_45.getFloatValue();
        }
    }

    private float getCustomYSpeed(float pitch) {
        if (pitch <= 5.0F) {
            return interpolateBravo(pitch, 0.0F, 5.0F, customY_0_5.getFloatValue(), customY_5_10.getFloatValue());
        } else if (pitch <= 10.0F) {
            return interpolateBravo(pitch, 5.0F, 10.0F, customY_5_10.getFloatValue(), customY_10_15.getFloatValue());
        } else if (pitch <= 15.0F) {
            return interpolateBravo(pitch, 10.0F, 15.0F, customY_10_15.getFloatValue(), customY_15_20.getFloatValue());
        } else if (pitch <= 20.0F) {
            return interpolateBravo(pitch, 15.0F, 20.0F, customY_15_20.getFloatValue(), customY_20_25.getFloatValue());
        } else if (pitch <= 25.0F) {
            return interpolateBravo(pitch, 20.0F, 25.0F, customY_20_25.getFloatValue(), customY_25_30.getFloatValue());
        } else if (pitch <= 30.0F) {
            return interpolateBravo(pitch, 25.0F, 30.0F, customY_25_30.getFloatValue(), customY_30_35.getFloatValue());
        } else if (pitch <= 35.0F) {
            return interpolateBravo(pitch, 30.0F, 35.0F, customY_30_35.getFloatValue(), customY_35_40.getFloatValue());
        } else {
            return pitch <= 40.0F
                    ? interpolateBravo(pitch, 35.0F, 40.0F, customY_35_40.getFloatValue(), customY_40_45.getFloatValue())
                    : customY_40_45.getFloatValue();
        }
    }
}