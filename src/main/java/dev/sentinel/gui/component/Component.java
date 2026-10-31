package dev.sentinel.gui.component;

import net.minecraft.client.gui.DrawContext;

/**
 * Base for every ClickGUI widget. The parent assigns {@link #x}, {@link #y} and {@link #width}
 * before each render; the component reports its own (possibly animated) height.
 */
public abstract class Component {
    public int x;
    public int y;
    public int width;

    public abstract int height();

    public abstract void render(DrawContext ctx, int mouseX, int mouseY, float delta);

    /** @return true if the click was consumed */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    /** @return true if the key was consumed */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /** @return true if the character was consumed */
    public boolean charTyped(char chr, int modifiers) {
        return false;
    }

    public boolean isHovered(double mx, double my) {
        return within(mx, my, x, y, width, height());
    }

    protected static boolean within(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
