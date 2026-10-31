package dev.rocketscience.gui.component.setting;

import dev.rocketscience.gui.Animation;
import dev.rocketscience.gui.Theme;
import dev.rocketscience.render.Render2D;
import dev.rocketscience.setting.NumberSetting;
import dev.rocketscience.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

/** Label + value on the first line, a thin track with an eased fill on the second. */
public class SliderComponent extends SettingComponent<NumberSetting<?>> {
    private final Animation fill;
    private boolean dragging;

    public SliderComponent(NumberSetting<?> setting) {
        super(setting);
        fill = new Animation(setting.fraction(), 18);
    }

    @Override
    public int height() {
        return LINE + 6;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (dragging) updateValue(mouseX);
        boolean hover = isHovered(mouseX, mouseY) || dragging;

        drawLabel(ctx, hover ? Theme.TEXT : ColorUtil.lerp(Theme.MUTED, Theme.TEXT, 0.6));
        drawRight(ctx, setting.displayValue(), Theme.BLUE, 2);

        fill.setTarget(setting.fraction());
        double f = fill.update();
        int tx = x + 2, ty = y + LINE + 1, tw = width - 4;
        Render2D.roundedRect(ctx, tx, ty, tw, 3, 1, Theme.TRACK);
        int fw = (int) Math.round(tw * f);
        if (fw > 0) Render2D.horizontalGradient(ctx, tx, ty, tx + fw, ty + 3, ColorUtil.withAlpha(Theme.BLUE, 0.55), Theme.BLUE);
        // knob
        int kx = tx + fw - 2;
        Render2D.roundedRect(ctx, Math.max(tx, kx), ty - 1, 4, 5, 2, hover ? Theme.TEXT : ColorUtil.lerp(Theme.BLUE, Theme.TEXT, 0.5));
    }

    private void updateValue(double mouseX) {
        double t = Math.max(0, Math.min(1, (mouseX - (x + 2)) / (width - 4)));
        setting.setDouble(setting.min() + (setting.max() - setting.min()) * t);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isHovered(mouseX, mouseY) || button != 0) return false;
        dragging = true;
        updateValue(mouseX);
        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
    }
}
