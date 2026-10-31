package dev.sentinel.hud;

import dev.sentinel.Sentinel;
import dev.sentinel.gui.Animation;
import dev.sentinel.gui.Theme;
import dev.sentinel.module.Module;
import dev.sentinel.modules.misc.Hud;
import dev.sentinel.render.FontRenderer;
import dev.sentinel.render.Render2D;
import dev.sentinel.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.*;

/**
 * Draws the HUD overlay: gradient watermark (top-left), animated ArrayList (top-right),
 * coordinates/FPS/facing (bottom-left) and toasts (bottom-right).
 */
public final class HudRenderer {
    private static final Map<Module, Animation> SLIDE = new HashMap<>();

    private HudRenderer() {
    }

    public static void render(DrawContext ctx, Hud hud) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null) return;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        boolean debug = mc.getDebugHud().shouldShowDebugHud();

        if (hud.watermark.get() && !debug) watermark(ctx);
        if (hud.arrayList.get() && !debug) arrayList(ctx, sw, hud.textScale.get().floatValue());
        info(ctx, mc, hud, sh);
        if (hud.toasts.get()) Notifications.render(ctx, sw, sh);
    }

    private static void watermark(DrawContext ctx) {
        String name = Sentinel.NAME.toUpperCase();
        float scale = 1.25f;
        float nameW = FontRenderer.width(name, scale);
        String ver = "v" + Sentinel.VERSION;
        float verW = FontRenderer.width(ver, 0.7f);
        int x = 4, y = 4, w = (int) (nameW + verW + 18), h = 17;
        Render2D.roundedRect(ctx, x, y, w, h, 4, ColorUtil.withAlpha(Theme.BG, 0.72));
        Render2D.roundedOutline(ctx, x, y, w, h, 4, Theme.BORDER);
        ctx.fill(x + 4, y + h - 1, x + 18, y + h, Theme.RED);
        FontRenderer.drawGradient(ctx, name, x + 6, y + 4, Theme.BLUE, Theme.RED, scale, true);
        FontRenderer.draw(ctx, ver, x + 10 + nameW, y + 7, Theme.MUTED, 0.7f);
    }

    private static void arrayList(DrawContext ctx, int sw, float scale) {
        List<Module> shown = new ArrayList<>();
        for (Module m : Sentinel.modules().getAll()) {
            if (!m.isShownInArrayList()) continue;
            Animation a = SLIDE.computeIfAbsent(m, k -> new Animation(m.isEnabled() ? 1 : 0, 10));
            a.setTarget(m.isEnabled() ? 1 : 0);
            if (a.update() > 0.01) shown.add(m);
        }
        // Widest first gives the classic stepped look
        shown.sort(Comparator.comparingDouble((Module m) -> FontRenderer.width(label(m), scale)).reversed());

        float y = 4;
        int rowH = (int) Math.ceil(FontRenderer.height(scale)) + 4;
        for (int i = 0; i < shown.size(); i++) {
            Module m = shown.get(i);
            double a = SLIDE.get(m).get();
            float textW = FontRenderer.width(label(m), scale);
            int w = (int) textW + 8;
            int x = (int) Math.round(sw - 3 - w * a);
            int h = (int) Math.round(rowH * a);
            int accent = ColorUtil.lerp(Theme.BLUE, Theme.RED, shown.size() <= 1 ? 0 : i / (double) (shown.size() - 1));
            ctx.fill(x, (int) y, x + w, (int) y + h, ColorUtil.withAlpha(Theme.BG, 0.65 * a));
            ctx.fill(sw - 3, (int) y, sw - 1, (int) y + h, ColorUtil.withAlpha(accent, a));
            if (h >= rowH - 1) {
                FontRenderer.draw(ctx, m.getName(), x + 4, y + 2.5f, ColorUtil.withAlpha(Theme.TEXT, a), scale, true);
                String info = m.getInfo();
                if (info != null) {
                    float nx = x + 4 + FontRenderer.width(m.getName() + " ", scale);
                    FontRenderer.draw(ctx, info, nx, y + 2.5f, ColorUtil.withAlpha(Theme.MUTED, a), scale, true);
                }
            }
            y += h;
        }
    }

    private static String label(Module m) {
        String info = m.getInfo();
        return info == null ? m.getName() : m.getName() + " " + info;
    }

    private static void info(DrawContext ctx, MinecraftClient mc, Hud hud, int sh) {
        List<String[]> lines = new ArrayList<>(); // {label, value}
        if (hud.coords.get()) {
            var p = mc.player;
            lines.add(new String[]{"XYZ", String.format("%.1f  %.1f  %.1f", p.getX(), p.getY(), p.getZ())});
            if (mc.world != null) {
                var dim = mc.world.getRegistryKey();
                if (dim == World.OVERWORLD) lines.add(new String[]{"NETHER", String.format("%.1f  %.1f", p.getX() / 8, p.getZ() / 8)});
                else if (dim == World.NETHER) lines.add(new String[]{"OVERWORLD", String.format("%.1f  %.1f", p.getX() * 8, p.getZ() * 8)});
            }
        }
        if (hud.facing.get()) {
            Direction d = mc.player.getHorizontalFacing();
            String axis = switch (d) {
                case NORTH -> "-Z";
                case SOUTH -> "+Z";
                case EAST -> "+X";
                case WEST -> "-X";
                default -> "";
            };
            String name = d.asString();
            lines.add(new String[]{"FACING", Character.toUpperCase(name.charAt(0)) + name.substring(1) + " (" + axis + ")"});
        }
        if (hud.fps.get()) lines.add(new String[]{"FPS", String.valueOf(mc.getCurrentFps())});
        if (lines.isEmpty()) return;

        float scale = hud.textScale.get().floatValue();
        int lineH = (int) Math.ceil(FontRenderer.height(scale)) + 3;
        // Sit above the chat input line
        int y = sh - 4 - lines.size() * lineH - (mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen ? 14 : 0);
        for (String[] l : lines) {
            FontRenderer.draw(ctx, l[0], 4, y, Theme.BLUE, scale, true);
            FontRenderer.draw(ctx, l[1], 4 + FontRenderer.width(l[0] + " ", scale), y, Theme.TEXT, scale, true);
            y += lineH;
        }
    }
}
