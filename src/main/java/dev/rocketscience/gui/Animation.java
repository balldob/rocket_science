package dev.rocketscience.gui;

/**
 * Frame-rate independent easing. The value approaches its target exponentially, which gives a
 * natural ease-out curve for hovers, toggles and expansions.
 */
public final class Animation {
    private final double speed;
    private double value;
    private double target;
    private long lastNanos = System.nanoTime();

    /**
     * @param initial starting value
     * @param speed   higher is snappier (roughly 1/seconds to cover ~63% of the distance)
     */
    public Animation(double initial, double speed) {
        this.value = initial;
        this.target = initial;
        this.speed = speed;
    }

    public void setTarget(double target) {
        this.target = target;
    }

    public double getTarget() {
        return target;
    }

    /** Advances the animation by the time since the last call and returns the new value. */
    public double update() {
        long now = System.nanoTime();
        double dt = Math.min((now - lastNanos) / 1e9, 0.1);
        lastNanos = now;
        value += (target - value) * (1.0 - Math.exp(-speed * dt));
        if (Math.abs(target - value) < 0.002) value = target;
        return value;
    }

    public double get() {
        return value;
    }

    public void snap(double v) {
        value = v;
        target = v;
    }

    /** Cubic ease-out for time-based (0..1) animations. */
    public static double easeOutCubic(double t) {
        t = Math.max(0, Math.min(1, t));
        return 1 - Math.pow(1 - t, 3);
    }
}
