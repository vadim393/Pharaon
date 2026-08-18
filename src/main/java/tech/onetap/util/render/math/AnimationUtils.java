package tech.onetap.util.render.math;

import net.minecraft.util.math.MathHelper;

public class AnimationUtils {

    private float value;
    private final float initValue;
    private float speed;
    private final Easing easing;
    private long lastTime = System.currentTimeMillis();

    public AnimationUtils(float value, float speed, Easing easing) {
        this.initValue = value;
        this.value = value;
        this.speed = speed;
        this.easing = easing;
    }

    public void update(float target) {
        float delta = Math.min(1.0f, (System.currentTimeMillis() - lastTime) / 1000f);
        lastTime = System.currentTimeMillis();
        float factor = 1.0f - (float) Math.exp(-speed * delta);
        float eased = easing.ease(MathHelper.clamp(factor, 0.0f, 1.0f), 0.0f, 1.0f, 1.0f);
        this.value += (target - value) * MathHelper.clamp(eased, 0.0f, 1.0f);
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getValue() {
        return this.value;
    }

    public float getInitValue() {
        return this.initValue;
    }
}