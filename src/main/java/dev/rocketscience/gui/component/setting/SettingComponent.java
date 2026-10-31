package dev.rocketscience.gui.component.setting;

import dev.rocketscience.gui.Theme;
import dev.rocketscience.gui.component.Component;
import dev.rocketscience.render.FontRenderer;
import dev.rocketscience.setting.*;
import net.minecraft.client.gui.DrawContext;

/**
 * Base for widgets that edit a {@link Setting}. {@link #create} picks the right widget for a
 * setting type - extend that switch when you add a new setting class.
 */
public abstract class SettingComponent<S extends Setting<?>> extends Component {
    protected static final float SCALE = Theme.SMALL_SCALE;
    protected static final int LINE = 12;
    protected final S setting;

    protected SettingComponent(S setting) {
        this.setting = setting;
    }

    public S getSetting() {
        return setting;
    }

    /** Draws the setting name on the left of the first line. */
    protected void drawLabel(DrawContext ctx, int color) {
        FontRenderer.draw(ctx, setting.getName(), x + 2, y + (LINE - FontRenderer.height(SCALE)) / 2f, color, SCALE);
    }

    /** Draws text right-aligned on the first line. */
    protected void drawRight(DrawContext ctx, String text, int color, int rightPad) {
        float w = FontRenderer.width(text, SCALE);
        FontRenderer.draw(ctx, text, x + width - rightPad - w, y + (LINE - FontRenderer.height(SCALE)) / 2f, color, SCALE);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static SettingComponent<?> create(Setting<?> s) {
        if (s instanceof BoolSetting b) return new BoolComponent(b);
        if (s instanceof NumberSetting<?> n) return new SliderComponent(n);
        if (s instanceof EnumSetting e) return new EnumComponent(e);
        if (s instanceof ColorSetting c) return new ColorComponent(c);
        if (s instanceof KeybindSetting k) return new KeybindComponent(k);
        if (s instanceof StringListSetting l) return new StringListComponent(l);
        throw new IllegalArgumentException("No GUI component for " + s.getClass().getSimpleName());
    }
}
