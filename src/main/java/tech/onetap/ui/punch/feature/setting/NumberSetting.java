package tech.onetap.ui.punch.feature.setting;

import java.util.function.Supplier;

public final class NumberSetting extends Setting<Double> {
    private final Supplier<Double> getter;
    private final java.util.function.DoubleConsumer setter;
    private final double min;
    private final double max;
    private final double step;
    private final String suffix;

    public NumberSetting(String name, Supplier<Double> getter, java.util.function.DoubleConsumer setter,
                         double min, double max, double step, String suffix) {
        super(name);
        this.getter = getter;
        this.setter = setter;
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix == null ? "" : suffix;
    }

    public double getMin() {
        return this.min;
    }

    public double getMax() {
        return this.max;
    }

    public double getStep() {
        return this.step;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public double getValue() {
        return this.getter.get();
    }

    public void setValue(double value) {
        this.setter.accept(value);
    }

    public double getProgress() {
        double range = this.max - this.min;
        if (range <= 0.0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (this.getValue() - this.min) / range));
    }
}