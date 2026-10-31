package dev.sentinel.render;

import dev.sentinel.Sentinel;
import dev.sentinel.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Text rendering with arbitrary scale and an optional clean sans-serif font.
 * <p>
 * The custom font is a regular Minecraft font resource ({@code assets/sentinel/font/ui.json})
 * backed by the bundled Roboto TTF, so no extra font rasteriser is needed; it is applied with
 * {@link Style#withFont}. When disabled the vanilla font is used.
 */
public final class FontRenderer {
    public static final Identifier UI_FONT = Identifier.of(Sentinel.MOD_ID, "ui");
    private static final Style STYLE = Style.EMPTY.withFont(UI_FONT);

    /** Toggled by the ClickGUI module's "Custom Font" setting. */
    public static boolean useCustomFont = true;

    private FontRenderer() {
    }

    private static TextRenderer tr() {
        return MinecraftClient.getInstance().textRenderer;
    }

    private static Text text(String s) {
        MutableText t = Text.literal(s);
        return useCustomFont ? t.setStyle(STYLE) : t;
    }

    public static float width(String s, float scale) {
        return tr().getWidth(text(s)) * scale;
    }

    public static float width(String s) {
        return width(s, 1f);
    }

    /** Visual line height (without the 1px shadow gap). */
    public static float height(float scale) {
        return (tr().fontHeight - 1) * scale;
    }

    public static void draw(DrawContext ctx, String s, float x, float y, int color, float scale, boolean shadow) {
        if (ColorUtil.alpha(color) < 5) return; // vanilla treats near-zero alpha as opaque
        var m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0);
        if (scale != 1f) m.scale(scale, scale, 1f);
        ctx.drawText(tr(), text(s), 0, 0, color, shadow);
        m.pop();
    }

    public static void draw(DrawContext ctx, String s, float x, float y, int color, float scale) {
        draw(ctx, s, x, y, color, scale, false);
    }

    /** Draws text with each character tinted along a left-to-right gradient. */
    public static void drawGradient(DrawContext ctx, String s, float x, float y, int from, int to, float scale, boolean shadow) {
        float total = width(s, scale);
        float cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            float w = width(ch, scale);
            double t = total <= 0 ? 0 : (cx - x + w / 2) / total;
            draw(ctx, ch, cx, y, ColorUtil.lerp(from, to, t), scale, shadow);
            cx += w;
        }
    }
}
