package dev.rocketscience.gui.component.setting;

import dev.rocketscience.gui.Animation;
import dev.rocketscience.gui.Theme;
import dev.rocketscience.render.FontRenderer;
import dev.rocketscience.render.Render2D;
import dev.rocketscience.setting.EnumSetting;
import dev.rocketscience.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

/** Dropdown: click to expand the option list, click an option to select it. */
public class EnumComponent<E extends Enum<E>> extends SettingComponent<EnumSetting<E>> {
    private static final int OPTION = 11;
    private final Animation open = new Animation(0, 16);
    private boolean expanded;

    public EnumComponent(EnumSetting<E> setting) {
        super(setting);
    }

    private int optionsHeight() {
        return setting.getConstants().length * OPTION + 2;
    }

    @Override
    public int height() {
        return LINE + (int) Math.round(optionsHeight() * open.get());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        open.setTarget(expanded ? 1 : 0);
        double o = open.update();
        boolean hover = within(mouseX, mouseY, x, y, width, LINE);

        drawLabel(ctx, hover ? Theme.TEXT : ColorUtil.lerp(Theme.MUTED, Theme.TEXT, 0.6));
        drawRight(ctx, setting.displayValue() + (expanded ? "  ▴" : "  ▾"), Theme.BLUE, 2);

        if (o <= 0.01) return;
        int top = y + LINE;
        int h = (int) Math.round(optionsHeight() * o);
        ctx.enableScissor(x, top, x + width, top + h);
        Render2D.roundedRect(ctx, x + 2, top, width - 4, optionsHeight() - 1, 3, Theme.SURFACE);
        int oy = top + 1;
        for (E c : setting.getConstants()) {
            boolean selected = c == setting.get();
            boolean hov = within(mouseX, mouseY, x + 2, oy, width - 4, OPTION);
            if (selected) ctx.fill(x + 2, oy, x + 4, oy + OPTION, Theme.BLUE);
            if (hov) ctx.fill(x + 4, oy, x + width - 2, oy + OPTION, ColorUtil.withAlpha(Theme.BLUE, 0.15));
            FontRenderer.draw(ctx, EnumSetting.pretty(c), x + 8, oy + (OPTION - FontRenderer.height(SCALE)) / 2f,
                    selected ? Theme.TEXT : Theme.MUTED, SCALE);
            oy += OPTION;
        }
        ctx.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (within(mouseX, mouseY, x, y, width, LINE)) {
            if (button == 0) expanded = !expanded;
            else if (button == 1) setting.cycle(); // right click: quick cycle
            return true;
        }
        if (expanded && isHovered(mouseX, mouseY)) {
            int index = (int) ((mouseY - (y + LINE + 1)) / OPTION);
            E[] values = setting.getConstants();
            if (index >= 0 && index < values.length) {
                setting.set(values[index]);
                expanded = false;
            }
            return true;
        }
        return false;
    }
}
