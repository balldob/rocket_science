package dev.rocketscience.render;

import dev.rocketscience.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

/**
 * 2D drawing primitives for the Command Panel look: rounded rectangles, 1px rounded outlines,
 * soft drop shadows and horizontal gradients, all built on {@link DrawContext#fill}.
 */
public final class Render2D {
    private Render2D() {
    }

    /** Horizontal inset of row {@code i} (0 = outermost) of a quarter circle of radius {@code r}. */
    private static int inset(int r, int i) {
        double dy = r - i - 0.5;
        return (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
    }

    /** Filled rectangle with rounded corners. */
    public static void roundedRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        ctx.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int in = inset(r, i);
            ctx.fill(x + in, y + i, x + w - in, y + i + 1, color);
            ctx.fill(x + in, y + h - i - 1, x + w - in, y + h - i, color);
        }
    }

    /** Rectangle with only the top corners rounded (used for panel headers). */
    public static void roundedTop(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w / 2, h));
        ctx.fill(x, y + r, x + w, y + h, color);
        for (int i = 0; i < r; i++) {
            int in = inset(r, i);
            ctx.fill(x + in, y + i, x + w - in, y + i + 1, color);
        }
    }

    /** 1px outline following a rounded rectangle. */
    public static void roundedOutline(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w <= 1 || h <= 1) return;
        r = Math.min(r, Math.min(w, h) / 2);
        // straight edges
        ctx.fill(x + r, y, x + w - r, y + 1, color);
        ctx.fill(x + r, y + h - 1, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + 1, y + h - r, color);
        ctx.fill(x + w - 1, y + r, x + w, y + h - r, color);
        // corner arcs: each row draws from its inset to the previous row's inset
        for (int i = 0; i < r; i++) {
            int in = inset(r, i);
            int prev = i == 0 ? r : inset(r, i - 1);
            int len = Math.max(1, prev - in);
            if (i == 0) len = Math.max(1, r - in);
            // top-left / top-right
            ctx.fill(x + in, y + i, x + in + len, y + i + 1, color);
            ctx.fill(x + w - in - len, y + i, x + w - in, y + i + 1, color);
            // bottom-left / bottom-right
            ctx.fill(x + in, y + h - i - 1, x + in + len, y + h - i, color);
            ctx.fill(x + w - in - len, y + h - i - 1, x + w - in, y + h - i, color);
        }
    }

    /** Soft shadow/glow made from a few expanding, fading rounded rectangles. */
    public static void shadow(DrawContext ctx, int x, int y, int w, int h, int r, int color, int layers) {
        int baseAlpha = ColorUtil.alpha(color);
        for (int i = layers; i >= 1; i--) {
            double a = baseAlpha / 255.0 * (1.0 - (double) i / (layers + 1)) / layers;
            roundedRect(ctx, x - i, y - i, w + i * 2, h + i * 2, r + i, ColorUtil.withAlpha(color, a));
        }
    }

    /** Left-to-right gradient, drawn as thin vertical strips. */
    public static void horizontalGradient(DrawContext ctx, int x1, int y1, int x2, int y2, int left, int right) {
        int w = x2 - x1;
        if (w <= 0) return;
        int step = 2;
        for (int x = 0; x < w; x += step) {
            double t = (double) x / Math.max(1, w - 1);
            ctx.fill(x1 + x, y1, Math.min(x2, x1 + x + step), y2, ColorUtil.lerp(left, right, t));
        }
    }

    /** Plain 1px rectangle border. */
    public static void border(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }
}
