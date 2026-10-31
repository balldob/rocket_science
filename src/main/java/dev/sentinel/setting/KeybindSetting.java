package dev.sentinel.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.sentinel.util.KeyUtil;

/** A GLFW key code; {@code -1} means unbound. */
public class KeybindSetting extends Setting<Integer> {
    public static final int NONE = -1;

    public KeybindSetting(String name, String description, int defaultKey) {
        super(name, description, defaultKey);
    }

    public boolean isBound() {
        return value != NONE;
    }

    @Override
    public String displayValue() {
        return KeyUtil.name(value);
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
            // keep current bind on malformed config
        }
    }

    @Override
    public boolean parse(String input) {
        int key = KeyUtil.parse(input);
        if (key == KeyUtil.INVALID) return false;
        set(key);
        return true;
    }
}
