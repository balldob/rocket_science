package dev.rocketscience.gui.component;

import dev.rocketscience.gui.Theme;
import dev.rocketscience.render.FontRenderer;
import dev.rocketscience.render.Render2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/** Minimal single-line text input (search bar, block list, friend entry). */
public class TextField extends Component {
    private final String placeholder;
    private final int maxLength;
    private final boolean alwaysFocused;
    private String text = "";
    private boolean focused;
    private Consumer<String> onChange = s -> {
    };
    private Consumer<String> onSubmit = s -> {
    };
    private int height = 14;
    private float scale = Theme.SMALL_SCALE;
    private String icon;

    public TextField(String placeholder, int maxLength, boolean alwaysFocused) {
        this.placeholder = placeholder;
        this.maxLength = maxLength;
        this.alwaysFocused = alwaysFocused;
        this.focused = alwaysFocused;
    }

    public TextField onChange(Consumer<String> c) {
        this.onChange = c;
        return this;
    }

    public TextField onSubmit(Consumer<String> c) {
        this.onSubmit = c;
        return this;
    }

    public TextField size(int height, float scale) {
        this.height = height;
        this.scale = scale;
        return this;
    }

    /** Short prefix glyph drawn before the text (e.g. a search marker). */
    public TextField icon(String icon) {
        this.icon = icon;
        return this;
    }

    public String getText() {
        return text;
    }

    public void setText(String t) {
        text = t;
        onChange.accept(text);
    }

    public boolean isFocused() {
        return focused;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        boolean hover = isHovered(mouseX, mouseY);
        Render2D.roundedRect(ctx, x, y, width, height, 3, Theme.SURFACE);
        Render2D.roundedOutline(ctx, x, y, width, height, 3, focused && !text.isEmpty() || hover ? Theme.BLUE : Theme.BORDER);
        float ty = y + (height - FontRenderer.height(scale)) / 2f;
        float tx = x + 5;
        if (icon != null) {
            FontRenderer.draw(ctx, icon, tx, ty, Theme.BLUE, scale);
            tx += FontRenderer.width(icon + " ", scale);
        }
        if (text.isEmpty()) {
            FontRenderer.draw(ctx, placeholder, tx, ty, Theme.MUTED, scale);
        }
        // Keep the tail of long text visible
        String shown = text;
        float maxW = width - (tx - x) - 6;
        while (!shown.isEmpty() && FontRenderer.width(shown, scale) > maxW) shown = shown.substring(1);
        FontRenderer.draw(ctx, shown, tx, ty, Theme.TEXT, scale);
        if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = (int) (tx + FontRenderer.width(shown, scale)) + 1;
            ctx.fill(cx, (int) ty - 1, cx + 1, (int) (ty + FontRenderer.height(scale)) + 1, Theme.BLUE);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean hit = isHovered(mouseX, mouseY);
        if (!alwaysFocused) focused = hit;
        if (hit && button == 1) setText(""); // right click clears
        return hit;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;
        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!text.isEmpty()) setText(Screen.hasControlDown() ? "" : text.substring(0, text.length() - 1));
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                onSubmit.accept(text);
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                if (alwaysFocused) return false; // let the screen close
                focused = false;
                return true;
            }
            default -> {
                if (keyCode == GLFW.GLFW_KEY_V && Screen.hasControlDown()) {
                    String clip = MinecraftClient.getInstance().keyboard.getClipboard().replaceAll("[\\r\\n]", "");
                    setText((text + clip).substring(0, Math.min(maxLength, text.length() + clip.length())));
                    return true;
                }
                return false;
            }
        }
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!focused || chr < 32 || chr == 127 || text.length() >= maxLength) return false;
        setText(text + chr);
        return true;
    }
}
