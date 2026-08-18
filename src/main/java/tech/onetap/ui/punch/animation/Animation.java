package tech.onetap.ui.punch.animation;

import tech.onetap.ui.punch.math.MathUtil;

public final class Animation {
    public enum Easing {
        EASE_IN_QUAD(Animation::easeInQuad),
        EASE_OUT_QUAD(Animation::easeOutQuad),
        EASE_IN_OUT_QUAD(Animation::easeInOutQuad),
        EASE_IN_CUBIC(Animation::easeInCubic),
        EASE_OUT_CUBIC(Animation::easeOutCubic),
        EASE_OUT_BACK(Animation::easeOutBack),
        LINEAR(t -> t);

        private final EasingFunction function;

        Easing(EasingFunction function) {
            this.function = function;
        }

        public float ease(float t) {
            return function.apply(MathUtil.clamp01(t));
        }

        private interface EasingFunction {
            float apply(float t);
        }
    }

    private float value;
    private float from;
    private float to;
    private long startTime;
    private long duration;
    private Easing easing;

    public Animation(long durationMs, Easing easing) {
        this.duration = Math.max(0L, durationMs);
        this.easing = easing;
        this.startTime = System.currentTimeMillis();
        this.from = 0.0F;
        this.to = 0.0F;
        this.value = 0.0F;
    }

    public void animate(float from, float to, long durationMs, Easing easing) {
        this.value = getValue();
        this.from = from;
        this.to = to;
        this.duration = Math.max(0L, durationMs);
        this.easing = easing;
        this.startTime = System.currentTimeMillis();
    }

    public float getValue() {
        if (duration <= 0L) {
            value = to;
            return value;
        }
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= duration) {
            value = to;
            return value;
        }
        float t = elapsed / (float) duration;
        value = MathUtil.lerp(from, to, easing.ease(t));
        return value;
    }

    public boolean isFinished() {
        if (duration <= 0L) {
            return true;
        }
        return System.currentTimeMillis() - startTime >= duration;
    }

    public void setValue(float value) {
        this.value = value;
        this.from = value;
        this.to = value;
        this.startTime = System.currentTimeMillis();
        this.duration = 0L;
    }

    private static float easeInQuad(float t) {
        return t * t;
    }

    private static float easeOutQuad(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t);
    }

    private static float easeInOutQuad(float t) {
        return t < 0.5F ? 2.0F * t * t : 1.0F - (float) Math.pow(-2.0F * t + 2.0F, 2.0F) / 2.0F;
    }

    private static float easeInCubic(float t) {
        return t * t * t;
    }

    private static float easeOutCubic(float t) {
        return 1.0F - (float) Math.pow(1.0F - t, 3.0F);
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        return 1.0F + c3 * (float) Math.pow(t - 1.0F, 3.0F) + c1 * (float) Math.pow(t - 1.0F, 2.0F);
    }
}
