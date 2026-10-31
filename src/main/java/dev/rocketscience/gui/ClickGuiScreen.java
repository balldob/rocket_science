package dev.rocketscience.gui;

import dev.rocketscience.RocketScience;
import dev.rocketscience.gui.component.*;
import dev.rocketscience.module.Category;
import dev.rocketscience.render.FontRenderer;
import dev.rocketscience.render.Render2D;
import dev.rocketscience.util.ColorUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The Command Panel ClickGUI: draggable category panels, a Players log tab, a Friends tab and a
 * search bar. Panels slide in with a short stagger whenever the screen opens.
 */
public class ClickGuiScreen extends Screen {
    private static final long INTRO_MS = 320;
    private static final long STAGGER_MS = 45;

    private final List<Panel> panels = new ArrayList<>();
    private final TextField search = new TextField("search modules", 32, true).size(16, 0.9f).icon("⌕");
    private final long openedAt = System.currentTimeMillis();
    private final Animation fade = new Animation(0, 9);

    public ClickGuiScreen() {
        super(Text.literal(RocketScience.NAME));
        for (Category c : Category.values()) panels.add(new CategoryPanel(c, search::getText));
        panels.add(new PlayersPanel());
        panels.add(new FriendsPanel());
        fade.setTarget(1);
    }

    /** Called on open and on window resize: place new panels and pull everything on screen. */
    @Override
    protected void init() {
        layout(false);
    }

    /**
     * Flows panels left-to-right, wrapping to a new row when the next one would not fit the
     * screen width. With {@code all=false} only panels without a saved position are placed.
     */
    private void layout(boolean all) {
        int margin = 8, gap = 8;
        int x = margin, y = Panel.TOP_MARGIN, rowHeight = 0;
        for (Panel p : panels) {
            if (!all && p.isPlaced()) continue;
            if (x + p.width > width - margin && x > margin) {
                x = margin;
                y += rowHeight + gap;
                rowHeight = 0;
            }
            p.place(x, y);
            x += p.width + gap;
            rowHeight = Math.max(rowHeight, p.totalHeight());
        }
        for (Panel p : panels) p.clamp(width, height);
    }

    /** Forgets saved positions and re-flows every panel to fit the current screen. */
    public void resetLayout() {
        GuiState.reset();
        layout(true);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** Replaces the vanilla blur/dirt background with our own tint (see {@link #render}). */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        double a = fade.update();

        // Background: navy tint + faint tactical grid
        ctx.fillGradient(0, 0, width, height, ColorUtil.withAlpha(Theme.BG, 0.45 * a), ColorUtil.withAlpha(Theme.BG, 0.78 * a));
        int grid = ColorUtil.withAlpha(Theme.BLUE, 0.035 * a);
        for (int gx = 0; gx < width; gx += 32) ctx.fill(gx, 0, gx + 1, height, grid);
        for (int gy = 0; gy < height; gy += 32) ctx.fill(0, gy, width, gy + 1, grid);

        // Search bar, top centre
        search.width = 200;
        search.x = (width - search.width) / 2;
        search.y = 8;
        search.render(ctx, mouseX, mouseY, delta);

        // Panels with staggered slide-in
        long elapsed = System.currentTimeMillis() - openedAt;
        for (int i = 0; i < panels.size(); i++) {
            Panel p = panels.get(i);
            if (!p.isVisible()) continue;
            p.clamp(width, height);
            double t = Animation.easeOutCubic((elapsed - i * STAGGER_MS) / (double) INTRO_MS);
            if (t <= 0) continue;
            ctx.draw(); // flush so the alpha below only affects this panel
            RenderSystem.setShaderColor(1f, 1f, 1f, (float) t);
            ctx.getMatrices().push();
            ctx.getMatrices().translate(0, (1 - t) * -18, 0);
            p.render(ctx, mouseX, mouseY, delta);
            ctx.getMatrices().pop();
            ctx.draw();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }

        // Footer hints
        String hint = "LMB toggle  •  RMB settings / collapse  •  drag headers  •  scroll to pan";
        float hw = FontRenderer.width(hint, 0.75f);
        FontRenderer.draw(ctx, hint, (width - hw) / 2f, height - 12, ColorUtil.withAlpha(Theme.MUTED, 0.9 * a), 0.75f);
        Render2D.horizontalGradient(ctx, 0, height - 1, width, height, ColorUtil.withAlpha(Theme.BLUE, 0.6 * a), ColorUtil.withAlpha(Theme.RED, 0.6 * a));
    }

    private boolean isOverReset(double mx, double my) {
        float w = FontRenderer.width("\u21BA reset layout", 0.75f);
        return my >= height - 14 && my < height - 2 && mx >= width - 8 - w && mx < width - 2;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverReset(mouseX, mouseY)) {
            resetLayout();
            return true;
        }
        if (search.mouseClicked(mouseX, mouseY, button)) return true;
        // top-most panel first (last drawn)
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);
            if (p.isVisible() && p.mouseClicked(mouseX, mouseY, button)) {
                panels.remove(i);
                panels.add(p); // bring to front
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (Panel p : panels) p.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int dy = (int) Math.round(verticalAmount * 16);
        // Scroll the panel under the cursor; over empty space, scroll them all
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);
            if (p.isVisible() && p.isOver(mouseX, mouseY)) {
                p.scroll(dy);
                p.clamp(width, height);
                return true;
            }
        }
        for (Panel p : panels) {
            p.scroll(dy);
            p.clamp(width, height);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (Panel p : panels) if (p.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (keyCode != GLFW.GLFW_KEY_ESCAPE && search.keyPressed(keyCode, scanCode, modifiers)) return true;
        // Close with the ClickGUI bind as well as ESC
        int bind = RocketScience.modules().get(dev.rocketscience.modules.misc.ClickGuiModule.class).getBind().get();
        if (keyCode == bind && search.getText().isEmpty()) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        for (Panel p : panels) if (p.charTyped(chr, modifiers)) return true;
        return search.charTyped(chr, modifiers);
    }

    @Override
    public void removed() {
        RocketScience.config().save();
    }
}
