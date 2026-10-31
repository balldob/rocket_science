package dev.sentinel.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** An ARGB colour. Serialised as {@code #AARRGGBB}. */
public class ColorSetting extends Setting<Integer> {
    public ColorSetting(String name, String description, int defaultArgb) {
        super(name, description, defaultArgb);
    }

    public int alpha() {
        return (value >>> 24) & 0xFF;
    }

    /** The colour with its alpha replaced by {@code alpha} (0..255). */
    public int withAlpha(int alpha) {
        return (value & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    @Override
    public String displayValue() {
        return String.format("#%08X", value);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(displayValue());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive()) parse(element.getAsString());
    }

    @Override
    public boolean parse(String input) {
        String hex = input.startsWith("#") ? input.substring(1) : input;
        try {
            if (hex.length() == 6) {
                set(0xFF000000 | Integer.parseUnsignedInt(hex, 16));
                return true;
            }
            if (hex.length() == 8) {
                set((int) Long.parseLong(hex, 16));
                return true;
            }
        } catch (NumberFormatException ignored) {
            // fall through
        }
        return false;
    }
}
