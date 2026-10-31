package dev.sentinel.gui.component;

import dev.sentinel.Sentinel;
import dev.sentinel.gui.Theme;
import dev.sentinel.module.Category;
import dev.sentinel.module.Module;
import dev.sentinel.render.FontRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** Panel listing the modules of one category, filtered by the search bar. */
public class CategoryPanel extends Panel {
    private final Category category;
    private final List<ModuleButton> buttons = new ArrayList<>();
    private final Supplier<String> search;

    public CategoryPanel(Category category, int x, int y, Supplier<String> search) {
        super(category.getDisplayName(), 112, x, y);
        this.category = category;
        this.search = search;
        for (Module m : Sentinel.modules().getByCategory(category)) buttons.add(new ModuleButton(m));
    }

    private List<ModuleButton> visibleButtons() {
        String q = search.get().trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return buttons;
        List<ModuleButton> list = new ArrayList<>();
        for (ModuleButton b : buttons) {
            if (b.getModule().getName().toLowerCase(Locale.ROOT).contains(q)) list.add(b);
        }
        return list;
    }

    @Override
    public boolean isVisible() {
        return search.get().isBlank() || !visibleButtons().isEmpty();
    }

    @Override
    protected int accent() {
        return category == Category.COMBAT ? Theme.RED : super.accent();
    }

    @Override
    protected String headerInfo() {
        long on = buttons.stream().filter(b -> b.getModule().isEnabled()).count();
        return on + "/" + buttons.size();
    }

    @Override
    protected int contentHeight() {
        List<ModuleButton> vis = visibleButtons();
        if (vis.isEmpty()) return Theme.ROW_HEIGHT;
        int h = 0;
        for (ModuleButton b : vis) h += b.height();
        return h;
    }

    @Override
    protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta, int top) {
        List<ModuleButton> vis = visibleButtons();
        if (vis.isEmpty()) {
            FontRenderer.draw(ctx, "no modules", x() + 7, top + 3, Theme.MUTED, Theme.SMALL_SCALE);
            return;
        }
        int y = top;
        for (ModuleButton b : vis) {
            b.x = x() + 1;
            b.y = y;
            b.width = width - 2;
            b.render(ctx, mouseX, mouseY, delta);
            y += b.height();
        }
    }

    @Override
    protected boolean contentClicked(double mx, double my, int button) {
        for (ModuleButton b : visibleButtons()) if (b.mouseClicked(mx, my, button)) return true;
        return false;
    }

    @Override
    protected void contentReleased(double mx, double my, int button) {
        for (ModuleButton b : buttons) b.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (ModuleButton b : buttons) if (b.keyPressed(keyCode, scanCode, modifiers)) return true;
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        for (ModuleButton b : buttons) if (b.charTyped(chr, modifiers)) return true;
        return false;
    }
}
