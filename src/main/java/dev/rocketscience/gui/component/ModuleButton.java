package dev.rocketscience.gui.component;

import dev.rocketscience.gui.Animation;
import dev.rocketscience.gui.Theme;
import dev.rocketscience.gui.component.setting.SettingComponent;
import dev.rocketscience.module.Module;
import dev.rocketscience.render.FontRenderer;
import dev.rocketscience.render.Render2D;
import dev.rocketscience.setting.Setting;
import dev.rocketscience.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * One module row. Left click toggles (eased blue glow when on), right click expands its settings.
 * Combat modules carry a red tick on the right edge.
 */
public class ModuleButton extends Component {
    private final Module module;
    private final List<SettingComponent<?>> settings = new ArrayList<>();
    private final Animation toggle;
    private final Animation hover = new Animation(0, 18);
    private final Animation expand = new Animation(0, 14);
    private boolean expanded;

    public ModuleButton(Module module) {
        this.module = module;
        this.toggle = new Animation(module.isEnabled() ? 1 : 0, 12);
        for (Setting<?> s : module.getSettings()) settings.add(SettingComponent.create(s));
    }

    public Module getModule() {
        return module;
    }

    private int settingsHeight() {
        int h = 0;
        for (SettingComponent<?> c : settings) if (c.getSetting().isVisible()) h += c.height() + 1;
        return h + 3;
    }

    @Override
    public int height() {
        return Theme.ROW_HEIGHT + (int) Math.round(settingsHeight() * expand.get());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        toggle.setTarget(module.isEnabled() ? 1 : 0);
        boolean rowHover = within(mouseX, mouseY, x, y, width, Theme.ROW_HEIGHT);
        hover.setTarget(rowHover ? 1 : 0);
        expand.setTarget(expanded ? 1 : 0);
        double t = toggle.update(), hv = hover.update(), e = expand.update();
        int rh = Theme.ROW_HEIGHT;

        // hover highlight
        if (hv > 0.01) ctx.fill(x, y, x + width, y + rh, ColorUtil.withAlpha(Theme.BLUE, 0.08 * hv));
        // enabled glow: gradient fading to the right + bright left bar
        if (t > 0.01) {
            Render2D.horizontalGradient(ctx, x, y, x + width, y + rh,
                    ColorUtil.withAlpha(Theme.BLUE, 0.42 * t), ColorUtil.withAlpha(Theme.BLUE, 0.04 * t));
            ctx.fill(x, y + 1, x + 2, y + rh - 1, ColorUtil.withAlpha(Theme.BLUE, t));
        }

        int textColor = ColorUtil.lerp(ColorUtil.lerp(Theme.MUTED, Theme.TEXT, hv * 0.5), Theme.TEXT, t);
        FontRenderer.draw(ctx, module.getName(), x + 6 + (float) (t * 1.5), y + (rh - FontRenderer.height(0.9f)) / 2f, textColor, 0.9f);

        int right = x + width - 4;
        if (module.isCombat()) {
            ctx.fill(right - 2, y + 4, right, y + rh - 4, ColorUtil.withAlpha(Theme.RED, 0.6 + 0.4 * t));
            right -= 5;
        }
        if (!settings.isEmpty()) {
            String glyph = expanded ? "-" : "+";
            FontRenderer.draw(ctx, glyph, right - FontRenderer.width(glyph, 0.8f), y + (rh - FontRenderer.height(0.8f)) / 2f,
                    ColorUtil.lerp(Theme.MUTED, Theme.BLUE, Math.max(e, hv)), 0.8f);
        }

        if (e <= 0.01) return;
        int top = y + rh;
        int visible = (int) Math.round(settingsHeight() * e);
        ctx.enableScissor(x, top, x + width, top + visible);
        ctx.fill(x, top, x + width, top + visible, 0x33000000);
        ctx.fill(x + 3, top + 1, x + 4, top + visible - 2, ColorUtil.withAlpha(Theme.BLUE, 0.45));
        int sy = top + 1;
        for (SettingComponent<?> c : settings) {
            if (!c.getSetting().isVisible()) continue;
            c.x = x + 6;
            c.y = sy;
            c.width = width - 9;
            c.render(ctx, mouseX, mouseY, delta);
            sy += c.height() + 1;
        }
        ctx.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (within(mx, my, x, y, width, Theme.ROW_HEIGHT)) {
            if (button == 0) module.toggle();
            else if (button == 1 && !settings.isEmpty()) expanded = !expanded;
            return true;
        }
        if (expanded) {
            for (SettingComponent<?> c : settings) {
                if (c.getSetting().isVisible() && c.mouseClicked(mx, my, button)) return true;
            }
        }
        return false;
    }

    @Override
    public void mouseReleased(double mx, double my, int button) {
        for (SettingComponent<?> c : settings) c.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!expanded) return false;
        for (SettingComponent<?> c : settings) if (c.keyPressed(keyCode, scanCode, modifiers)) return true;
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!expanded) return false;
        for (SettingComponent<?> c : settings) if (c.charTyped(chr, modifiers)) return true;
        return false;
    }
}
