package dev.sentinel.gui.component;

import dev.sentinel.gui.Animation;
import dev.sentinel.gui.GuiState;
import dev.sentinel.gui.Theme;
import dev.sentinel.render.FontRenderer;
import dev.sentinel.render.Render2D;
import dev.sentinel.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

/**
 * A draggable, collapsible window with a thin blue header bar and a red accent line.
 * Subclasses provide the body via {@link #contentHeight()} and {@link #renderContent}.
 */
public abstract class Panel {
    public static final int HEADER = Theme.HEADER_HEIGHT;

    protected final String title;
    protected final GuiState.PanelState state;
    public int width;
    private final Animation expand;
    private boolean dragging;
    private int dragOffsetX, dragOffsetY;

    protected Panel(String title, int width, int defaultX, int defaultY) {
        this.title = title;
        this.width = width;
        this.state = GuiState.get(title, defaultX, defaultY);
        this.expand = new Animation(state.expanded ? 1 : 0, 14);
    }

    public int x() {
        return state.x;
    }

    public int y() {
        return state.y;
    }

    /** Moves the panel (used by mouse wheel scrolling). */
    public void scroll(int dy) {
        state.y += dy;
    }

    /** Full (un-animated) content height in pixels. */
    protected abstract int contentHeight();

    /** Renders the body starting at {@code top}; already scissored to the visible area. */
    protected abstract void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta, int top);

    /** Body click. */
    protected boolean contentClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    protected void contentReleased(double mouseX, double mouseY, int button) {
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        return false;
    }

    /** Small grey text on the right side of the header. */
    protected String headerInfo() {
        return "";
    }

    /** Colour of the short accent line under the header. */
    protected int accent() {
        return Theme.RED;
    }

    /** Whether this panel should be drawn at all (e.g. hidden by search). */
    public boolean isVisible() {
        return true;
    }

    private int bodyHeight() {
        int full = contentHeight();
        return full <= 0 ? 0 : (int) Math.round((full + 4) * expand.get());
    }

    public int totalHeight() {
        return HEADER + bodyHeight();
    }

    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (dragging) {
            state.x = mouseX - dragOffsetX;
            state.y = mouseY - dragOffsetY;
        }
        expand.setTarget(state.expanded ? 1 : 0);
        expand.update();

        int x = state.x, y = state.y, w = width;
        int body = bodyHeight();
        int h = HEADER + body;
        int r = Theme.RADIUS;

        // Drop shadow + translucent body
        Render2D.shadow(ctx, x, y, w, h, r, Theme.SHADOW, 4);
        Render2D.roundedRect(ctx, x, y, w, h, r, Theme.PANEL);

        // Header: rounded top, thin blue bar along the top edge, red accent under the title
        if (body > 0) Render2D.roundedTop(ctx, x, y, w, HEADER, r, Theme.HEADER);
        else Render2D.roundedRect(ctx, x, y, w, HEADER, r, Theme.HEADER);
        ctx.fill(x + r, y, x + w - r, y + 1, Theme.BLUE);
        ctx.fill(x + r - 1, y + 1, x + w - r + 1, y + 2, ColorUtil.withAlpha(Theme.BLUE, 0.35));
        if (body > 0) {
            ctx.fill(x + 1, y + HEADER - 1, x + w - 1, y + HEADER, ColorUtil.withAlpha(Theme.BLUE, 0.18));
            ctx.fill(x + 6, y + HEADER - 1, x + 22, y + HEADER, accent());
        }

        boolean headerHover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + HEADER;
        float ty = y + (HEADER - FontRenderer.height(Theme.TEXT_SCALE)) / 2f + 1;
        FontRenderer.draw(ctx, title.toUpperCase(), x + 7, ty, headerHover ? Theme.TEXT : ColorUtil.lerp(Theme.TEXT, Theme.MUTED, 0.15), Theme.TEXT_SCALE, false);
        String info = headerInfo();
        if (!info.isEmpty()) {
            float iw = FontRenderer.width(info, Theme.SMALL_SCALE);
            FontRenderer.draw(ctx, info, x + w - 7 - iw, y + (HEADER - FontRenderer.height(Theme.SMALL_SCALE)) / 2f + 1, Theme.MUTED, Theme.SMALL_SCALE);
        }

        Render2D.roundedOutline(ctx, x, y, w, h, r, Theme.BORDER);

        if (body > 0) {
            ctx.enableScissor(x + 1, y + HEADER, x + w - 1, y + h - 1);
            renderContent(ctx, mouseX, mouseY, delta, y + HEADER + 2);
            ctx.disableScissor();
        }
    }

    public boolean isOver(double mx, double my) {
        return mx >= state.x && mx < state.x + width && my >= state.y && my < state.y + totalHeight();
    }

    /** @return true if the click hit this panel */
    public boolean mouseClicked(double mx, double my, int button) {
        if (!isOver(mx, my)) return false;
        if (my < state.y + HEADER) {
            if (button == 0) {
                dragging = true;
                dragOffsetX = (int) mx - state.x;
                dragOffsetY = (int) my - state.y;
            } else if (button == 1) {
                state.expanded = !state.expanded;
            }
            return true;
        }
        if (state.expanded) contentClicked(mx, my, button);
        return true;
    }

    public void mouseReleased(double mx, double my, int button) {
        dragging = false;
        contentReleased(mx, my, button);
    }
}
