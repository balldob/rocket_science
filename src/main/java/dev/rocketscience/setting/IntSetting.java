package dev.rocketscience.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Integer slider clamped to {@code [min, max]}. */
public class IntSetting extends NumberSetting<Integer> {
    private final int min;
    private final int max;

    public IntSetting(String name, String description, int defaultValue, int min, int max) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
    }

    @Override
    protected Integer sanitize(Integer v) {
        return Math.max(min, Math.min(max, v));
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
        set((int) Math.round(v));
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement element) {
        try {
            set(element.getAsInt());
        } catch (Exception ignored) {
            // keep current value on malformed config
        }
    }

    @Override
    public boolean parse(String input) {
        try {
            set(Integer.parseInt(input));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
