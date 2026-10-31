package dev.rocketscience.gui.component.setting;

import dev.rocketscience.gui.Animation;
import dev.rocketscience.gui.Theme;
import dev.rocketscience.render.Render2D;
import dev.rocketscience.setting.BoolSetting;
import dev.rocketscience.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

/** Pill-shaped switch with an eased knob. */
public class BoolComponent extends SettingComponent<BoolSetting> {
    private final Animation knob;
    private final Animation hover = new Animation(0, 16);

    public BoolComponent(BoolSetting setting) {
        super(setting);
        knob = new Animation(setting.get() ? 1 : 0, 14);
    }

    @Override
    public int height() {
        return LINE;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        knob.setTarget(setting.get() ? 1 : 0);
        hover.setTarget(isHovered(mouseX, mouseY) ? 1 : 0);
        double k = knob.update();
        double h = hover.update();

        drawLabel(ctx, ColorUtil.lerp(Theme.MUTED, Theme.TEXT, Math.max(k, h * 0.6)));

        int tw = 14, th = 7;
        int tx = x + width - tw - 2, ty = y + (LINE - th) / 2;
        Render2D.roundedRect(ctx, tx, ty, tw, th, 3, ColorUtil.lerp(Theme.TRACK, ColorUtil.withAlpha(Theme.BLUE, 0.85), k));
        if (k > 0.05) Render2D.shadow(ctx, tx, ty, tw, th, 3, ColorUtil.withAlpha(Theme.BLUE, 0.5 * k), 2);
        int knobX = (int) Math.round(tx + 1 + (tw - 7) * k);
        Render2D.roundedRect(ctx, knobX, ty + 1, 5, 5, 2, Theme.TEXT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isHovered(mouseX, mouseY) || button != 0) return false;
        setting.toggle();
        return true;
    }
}
