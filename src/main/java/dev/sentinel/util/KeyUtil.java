package dev.sentinel.util;

import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.Map;

/** Converts between GLFW key codes and human readable names. */
public final class KeyUtil {
    /** Returned by {@link #parse} when a name is not recognised. */
    public static final int INVALID = Integer.MIN_VALUE;

    /** Friendly aliases on top of Minecraft's own "key.keyboard.*" names. */
    private static final Map<String, Integer> ALIASES = Map.ofEntries(
            Map.entry("rshift", GLFW.GLFW_KEY_RIGHT_SHIFT),
            Map.entry("lshift", GLFW.GLFW_KEY_LEFT_SHIFT),
            Map.entry("rctrl", GLFW.GLFW_KEY_RIGHT_CONTROL),
            Map.entry("lctrl", GLFW.GLFW_KEY_LEFT_CONTROL),
            Map.entry("ralt", GLFW.GLFW_KEY_RIGHT_ALT),
            Map.entry("lalt", GLFW.GLFW_KEY_LEFT_ALT),
            Map.entry("esc", GLFW.GLFW_KEY_ESCAPE),
            Map.entry("del", GLFW.GLFW_KEY_DELETE),
            Map.entry("ins", GLFW.GLFW_KEY_INSERT)
    );

    private KeyUtil() {
    }

    public static String name(int key) {
        if (key < 0) return "None";
        try {
            return InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString();
        } catch (Exception e) {
            return "Key " + key;
        }
    }

    /** Parses names like "g", "f5", "rshift", "right_shift", "none". */
    public static int parse(String input) {
        String s = input.trim().toLowerCase(Locale.ROOT);
        if (s.equals("none") || s.equals("unbind") || s.equals("-1")) return -1;
        Integer alias = ALIASES.get(s.replace("_", "").replace(".", ""));
        if (alias != null) return alias;
        try {
            InputUtil.Key key = InputUtil.fromTranslationKey("key.keyboard." + s.replace('_', '.'));
            if (key != InputUtil.UNKNOWN_KEY && key.getCode() >= 0) return key.getCode();
        } catch (Exception ignored) {
            // unknown name -> fall through
        }
        return INVALID;
    }
}
