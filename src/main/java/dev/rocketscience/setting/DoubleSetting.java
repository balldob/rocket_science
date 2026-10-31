package dev.rocketscience.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Decimal slider clamped to {@code [min, max]} and snapped to {@code step}. */
public class DoubleSetting extends NumberSetting<Double> {
    private final double min;
    private final double max;
    private final double step;

    public DoubleSetting(String name, String description, double defaultValue, double min, double max, double step) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    @Override
    protected Double sanitize(Double v) {
        double clamped = Math.max(min, Math.min(max, v));
        if (step > 0) clamped = Math.round(clamped / step) * step;
        // Re-clamp and trim floating point noise (e.g. 0.30000000000000004)
        clamped = Math.max(min, Math.min(max, clamped));
        return Math.round(clamped * 1e6) / 1e6;
    }

    @Override
    public double min() {
        return min;
    }

    @Override
    public double max() {
        return max;
    }

    @Override
    public void setDouble(double v) {
        set(v);
    }

    @Override
    public String displayValue() {
        // Show as many decimals as the step implies (max 2)
        int decimals = step >= 1 ? 0 : step >= 0.1 ? 1 : 2;
        return String.format(java.util.Locale.ROOT, "%." + decimals + "f", value);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement element) {
        try {
            set(element.getAsDouble());
        } catch (Exception ignored) {
            // keep current value on malformed config
        }
    }

    @Override
    public boolean parse(String input) {
        try {
            set(Double.parseDouble(input));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
