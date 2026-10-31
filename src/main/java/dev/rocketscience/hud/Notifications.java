package dev.rocketscience.hud;

import dev.rocketscience.gui.Animation;
import dev.rocketscience.gui.Theme;
import dev.rocketscience.module.Module;
import dev.rocketscience.render.FontRenderer;
import dev.rocketscience.render.Render2D;
import dev.rocketscience.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Small toasts in the bottom-right corner. Blue border = on/info, red border = off/warning.
 * Each toast slides in from the right, lingers, then slides out.
 */
public final class Notifications {
    public enum Type {
        ON, OFF, INFO, WARNING;

        int color() {
            return this == ON || this == INFO ? Theme.BLUE : Theme.RED;
        }
    }

    private static final class Toast {
        final String title;
        final String message;
        final Type type;
        final long created = System.currentTimeMillis();
        final long duration;
        final Animation slide = new Animation(0, 12);
        final Animation slot = new Animation(-1, 14);

        Toast(String title, String message, Type type, long duration) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.duration = duration;
            slide.setTarget(1);
        }
    }

    private static final List<Toast> TOASTS = new ArrayList<>();
    private static final int MAX = 6;

    /** Whether toasts for module toggles are shown (HUD setting). */
    public static boolean toggleToasts = true;

    private Notifications() {
    }

    public static void push(String title, String message, Type type) {
        push(title, message, type, 2500);
    }

    public static void push(String title, String message, Type type, long durationMs) {
        synchronized (TOASTS) {
            TOASTS.add(0, new Toast(title, message, type, durationMs));
            while (TOASTS.size() > MAX) TOASTS.remove(TOASTS.size() - 1);
        }
    }

    public static void moduleToggled(Module m) {
        if (!toggleToasts) return;
        push(m.getName(), m.isEnabled() ? "Enabled" : "Disabled", m.isEnabled() ? Type.ON : Type.OFF, 1600);
    }

    public static void render(DrawContext ctx, int screenW, int screenH) {
        synchronized (TOASTS) {
            long now = System.currentTimeMillis();
            int w = 150, h = 26, gap = 4;
            int index = 0;
            Iterator<Toast> it = TOASTS.iterator();
            while (it.hasNext()) {
                Toast t = it.next();
                if (now - t.created > t.duration) t.slide.setTarget(0);
                double s = t.slide.update();
                if (t.slide.getTarget() == 0 && s <= 0.01) {
                    it.remove();
                    continue;
                }
                if (t.slot.get() < 0) t.slot.snap(index);
                t.slot.setTarget(index);
                double slot = t.slot.update();

                int x = (int) Math.round(screenW - (w + 6) * s);
                int y = (int) Math.round(screenH - 6 - h - slot * (h + gap));
                int border = t.type.color();

                Render2D.shadow(ctx, x, y, w, h, 4, ColorUtil.withAlpha(0xFF000000, 0.5), 3);
                Render2D.roundedRect(ctx, x, y, w, h, 4, ColorUtil.withAlpha(Theme.BG, 0.92));
                Render2D.roundedOutline(ctx, x, y, w, h, 4, ColorUtil.withAlpha(border, 0.9));
                ctx.fill(x + 1, y + 4, x + 3, y + h - 4, border);
                FontRenderer.draw(ctx, t.title, x + 8, y + 5, Theme.TEXT, 0.9f);
                FontRenderer.draw(ctx, t.message, x + 8, y + 15, ColorUtil.lerp(Theme.MUTED, border, 0.5), 0.75f);
                // lifetime bar
                double life = Math.max(0, 1 - (now - t.created) / (double) t.duration);
                ctx.fill(x + 4, y + h - 2, x + 4 + (int) ((w - 8) * life), y + h - 1, ColorUtil.withAlpha(border, 0.6));
                index++;
            }
        }
    }
}
