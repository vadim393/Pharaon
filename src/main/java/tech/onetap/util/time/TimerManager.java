package tech.onetap.util.time;

public final class TimerManager {
    private static volatile float timer = 1.0F;

    private TimerManager() {
    }

    public static void setTimer(float value) {
        timer = Float.isFinite(value) ? Math.max(0.05F, value) : 1.0F;
    }

    public static float getTimer() {
        return timer;
    }

    public static void reset() {
        timer = 1.0F;
    }
}