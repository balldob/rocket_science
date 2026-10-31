package dev.rocketscience.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

/**
 * A choice between the constants of an enum, shown as a dropdown in the GUI.
 *
 * @param <E> the enum type
 */
public class EnumSetting<E extends Enum<E>> extends Setting<E> {
    private final E[] constants;

    public EnumSetting(String name, String description, E defaultValue) {
        super(name, description, defaultValue);
        this.constants = defaultValue.getDeclaringClass().getEnumConstants();
    }

    public E[] getConstants() {
        return constants;
    }

    /** Advances to the next constant, wrapping around. */
    public void cycle() {
        set(constants[(value.ordinal() + 1) % constants.length]);
    }

    /** Turns {@code FRIEND_STATUS} into {@code Friend Status}. */
    public static String pretty(Enum<?> e) {
        String[] parts = e.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    @Override
    public String displayValue() {
        return pretty(value);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value.name());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive()) parse(element.getAsString());
    }

    @Override
    public boolean parse(String input) {
        String normalized = input.replace(" ", "_");
        for (E c : constants) {
            if (c.name().equalsIgnoreCase(normalized)) {
                set(c);
                return true;
            }
        }
        return false;
    }
}
