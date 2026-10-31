package dev.rocketscience.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

/** A simple on/off toggle. */
public class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String name, String description, boolean defaultValue) {
        super(name, description, defaultValue);
    }

    public void toggle() {
        set(!value);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive()) set(element.getAsBoolean());
    }

    @Override
    public boolean parse(String input) {
        switch (input.toLowerCase(Locale.ROOT)) {
            case "true", "on", "yes", "1" -> set(true);
            case "false", "off", "no", "0" -> set(false);
            case "toggle" -> toggle();
            default -> {
                return false;
            }
        }
        return true;
    }
}
