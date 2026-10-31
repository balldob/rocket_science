package dev.sentinel.gui.component.setting;

import dev.sentinel.gui.Theme;
import dev.sentinel.render.FontRenderer;
import dev.sentinel.render.Render2D;
import dev.sentinel.setting.KeybindSetting;
import dev.sentinel.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

/** Click to listen, then press a key. ESC cancels; Backspace/Delete unbinds. */
public class KeybindComponent extends SettingComponent<KeybindSetting> {
    private boolean listening;

    public KeybindComponent(KeybindSetting setting) {
        super(setting);
    }

    @Override
    public int height() {
        return LINE;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        boolean hover = isHovered(mouseX, mouseY);
        drawLabel(ctx, hover ? Theme.TEXT : ColorUtil.lerp(Theme.MUTED, Theme.TEXT, 0.6));
        String label = listening ? "press a key..." : setting.displayValue();
        float w = FontRenderer.width(label, SCALE) + 6;
        int bx = (int) (x + width - w - 2);
        Render2D.roundedRect(ctx, bx, y + 1, (int) w, LINE - 2, 2, listening ? ColorUtil.withAlpha(Theme.BLUE, 0.3) : Theme.SURFACE);
        Render2D.roundedOutline(ctx, bx, y + 1, (int) w, LINE - 2, 2, listening ? Theme.BLUE : Theme.BORDER_DIM);
        FontRenderer.draw(ctx, label, bx + 3, y + (LINE - FontRenderer.height(SCALE)) / 2f,
                listening ? Theme.TEXT : setting.isBound() ? Theme.BLUE : Theme.MUTED, SCALE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isHovered(mouseX, mouseY)) {
            listening = false;
            return false;
        }
        if (button == 0) listening = !listening;
        else if (button == 1) {
            setting.set(KeybindSetting.NONE);
            listening = false;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!listening) return false;
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) setting.set(KeybindSetting.NONE);
        else if (keyCode != GLFW.GLFW_KEY_ESCAPE) setting.set(keyCode);
        listening = false;
        return true;
    }

    /** Swallow typed characters while listening so they don't leak into the search bar. */
    @Override
    public boolean charTyped(char chr, int modifiers) {
        return listening;
    }
}
