package dev.rocketscience.setting;

/**
 * Shared base for numeric sliders so the GUI can treat int and double settings uniformly.
 *
 * @param <T> boxed numeric type
 */
public abstract class NumberSetting<T extends Number> extends Setting<T> {
    protected NumberSetting(String name, String description, T defaultValue) {
        super(name, description, defaultValue);
    }

    public abstract double min();

    public abstract double max();

    /** Sets the value from a double (the slider position), rounding as appropriate. */
    public abstract void setDouble(double v);

    public double getDouble() {
        return value.doubleValue();
    }

    /** Current value normalised to 0..1 for drawing the slider fill. */
    public double fraction() {
        double range = max() - min();
        return range <= 0 ? 0 : (getDouble() - min()) / range;
    }
}
