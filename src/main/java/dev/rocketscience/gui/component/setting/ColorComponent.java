package dev.rocketscience.gui.component.setting;

import dev.rocketscience.gui.Animation;
import dev.rocketscience.gui.Theme;
import dev.rocketscience.render.FontRenderer;
import dev.rocketscience.render.Render2D;
import dev.rocketscience.setting.ColorSetting;
import dev.rocketscience.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

/**
 * Colour picker: a swatch that expands into Hue / Saturation / Brightness / Alpha sliders.
 * HSV is kept locally so the hue doesn't jump when saturation hits zero.
 */
public class ColorComponent extends SettingComponent<ColorSetting> {
    private static final int ROW = 10;
    private static final String[] LABELS = {"H", "S", "B", "A"};
    private final Animation open = new Animation(0, 16);
    private boolean expanded;
    private int dragging = -1;
    private float h, s, v;
    private int lastSeen;

    public ColorComponent(ColorSetting setting) {
        super(setting);
        syncFromSetting();
    }

    private void syncFromSetting() {
        float[] hsv = ColorUtil.toHsv(setting.get());
        h = hsv[0];
        s = hsv[1];
        v = hsv[2];
        lastSeen = setting.get();
    }

    @Override
    public int height() {
        return LINE + (int) Math.round((ROW * 4 + 2) * open.get());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (setting.get() != lastSeen && dragging < 0) syncFromSetting(); // changed elsewhere (command/config)
        if (dragging >= 0) drag(dragging, mouseX);
        open.setTarget(expanded ? 1 : 0);
        double o = open.update();
        boolean hover = within(mouseX, mouseY, x, y, width, LINE);

        drawLabel(ctx, hover ? Theme.TEXT : ColorUtil.lerp(Theme.MUTED, Theme.TEXT, 0.6));
        int sw = 16, sx = x + width - sw - 2, sy = y + 2;
        Render2D.roundedRect(ctx, sx, sy, sw, LINE - 4, 2, setting.get() | 0xFF000000);
        Render2D.roundedOutline(ctx, sx, sy, sw, LINE - 4, 2, hover ? Theme.BLUE : Theme.BORDER_DIM);

        if (o <= 0.01) return;
        int top = y + LINE;
        ctx.enableScissor(x, top, x + width, top + (int) Math.round((ROW * 4 + 2) * o));
        for (int i = 0; i < 4; i++) {
            int ry = top + i * ROW + 1;
            FontRenderer.draw(ctx, LABELS[i], x + 3, ry + 1, Theme.MUTED, 0.75f);
            int tx = x + 12, tw = width - 16, ty = ry + 3;
            drawTrack(ctx, i, tx, ty, tw, 4);
            float frac = switch (i) {
                case 0 -> h;
                case 1 -> s;
                case 2 -> v;
                default -> setting.alpha() / 255f;
            };
            int kx = tx + Math.round(frac * (tw - 2));
            ctx.fill(kx - 1, ty - 1, kx + 2, ty + 5, Theme.TEXT);
        }
        ctx.disableScissor();
    }

    /** Draws the gradient behind each slider so the user can see what they're picking. */
    private void drawTrack(DrawContext ctx, int index, int tx, int ty, int tw, int th) {
        int segments = 24;
        for (int i = 0; i < segments; i++) {
            float t = i / (float) (segments - 1);
            int c = switch (index) {
                case 0 -> ColorUtil.hsv(t, 1, 1);
                case 1 -> ColorUtil.hsv(h, t, v);
                case 2 -> ColorUtil.hsv(h, s, t);
                default -> ColorUtil.withAlpha(ColorUtil.hsv(h, s, v), Math.max(0.08, t));
            };
            int x1 = tx + tw * i / segments, x2 = tx + tw * (i + 1) / segments;
            ctx.fill(x1, ty, x2, ty + th, c);
        }
    }

    private void drag(int index, double mouseX) {
        int tx = x + 12, tw = width - 16;
        float t = (float) Math.max(0, Math.min(1, (mouseX - tx) / (tw - 2)));
        int alpha = setting.alpha();
        switch (index) {
            case 0 -> h = t;
            case 1 -> s = t;
            case 2 -> v = t;
            default -> alpha = Math.round(t * 255);
        }
        setting.set((ColorUtil.hsv(h, s, v) & 0x00FFFFFF) | (alpha << 24));
        lastSeen = setting.get();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (within(mouseX, mouseY, x, y, width, LINE)) {
            expanded = !expanded;
            return true;
        }
        if (expanded && isHovered(mouseX, mouseY) && button == 0) {
            int index = (int) ((mouseY - (y + LINE + 1)) / ROW);
            if (index >= 0 && index < 4) {
                dragging = index;
                drag(index, mouseX);
            }
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        dragging = -1;
    }
}
