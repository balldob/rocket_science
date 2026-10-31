package dev.rocketscience.util;

/** Small ARGB colour helpers (no AWT dependency). */
public final class ColorUtil {
    private ColorUtil() {
    }

    public static int alpha(int c) {
        return (c >>> 24) & 0xFF;
    }

    public static int red(int c) {
        return (c >> 16) & 0xFF;
    }

    public static int green(int c) {
        return (c >> 8) & 0xFF;
    }

    public static int blue(int c) {
        return c & 0xFF;
    }

    public static int argb(int a, int r, int g, int b) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    /** Replaces the alpha channel with {@code alpha} in 0..1. */
    public static int withAlpha(int color, double alpha) {
        return (color & 0x00FFFFFF) | (clamp((int) Math.round(alpha * 255)) << 24);
    }

    /** Multiplies the existing alpha by {@code factor}. */
    public static int fade(int color, double factor) {
        return (color & 0x00FFFFFF) | (clamp((int) Math.round(alpha(color) * factor)) << 24);
    }

    /** Linear interpolation between two ARGB colours. */
    public static int lerp(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        return argb(
                (int) Math.round(alpha(a) + (alpha(b) - alpha(a)) * t),
                (int) Math.round(red(a) + (red(b) - red(a)) * t),
                (int) Math.round(green(a) + (green(b) - green(a)) * t),
                (int) Math.round(blue(a) + (blue(b) - blue(a)) * t));
    }

    /** HSV (all 0..1) to opaque RGB. */
    public static int hsv(float h, float s, float v) {
        h = (h % 1f + 1f) % 1f;
        float r, g, b;
        int i = (int) (h * 6f);
        float f = h * 6f - i;
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return argb(255, Math.round(r * 255), Math.round(g * 255), Math.round(b * 255));
    }

    /** RGB to HSV, returning {h, s, v} in 0..1. */
    public static float[] toHsv(int color) {
        float r = red(color) / 255f, g = green(color) / 255f, b = blue(color) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h = 0;
        if (d != 0) {
            if (max == r) h = ((g - b) / d) % 6f;
            else if (max == g) h = (b - r) / d + 2f;
            else h = (r - g) / d + 4f;
            h /= 6f;
            if (h < 0) h += 1f;
        }
        float s = max == 0 ? 0 : d / max;
        return new float[]{h, s, max};
    }
}
