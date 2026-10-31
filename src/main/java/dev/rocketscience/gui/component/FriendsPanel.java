package dev.rocketscience.gui.component;

import dev.rocketscience.RocketScience;
import dev.rocketscience.gui.Theme;
import dev.rocketscience.render.FontRenderer;
import dev.rocketscience.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Lists friends (click to remove) with a text field to add new ones. */
public class FriendsPanel extends Panel {
    private static final int ROW = 11;
    private final TextField input = new TextField("add friend + Enter", 16, false).size(13, 0.8f);

    public FriendsPanel() {
        super("Friends", 112);
        input.onSubmit(name -> {
            if (name.matches("[A-Za-z0-9_]{1,16}")) {
                RocketScience.friends().add(name);
                input.setText("");
            }
        });
    }

    private List<String> friends() {
        return new ArrayList<>(RocketScience.friends().getAll());
    }

    @Override
    protected String headerInfo() {
        return String.valueOf(friends().size());
    }

    @Override
    protected int accent() {
        return Theme.BLUE;
    }

    @Override
    protected int contentHeight() {
        return friends().size() * ROW + 18;
    }

    @Override
    protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta, int top) {
        int y = top;
        for (String f : friends()) {
            boolean hover = mouseX >= x() && mouseX < x() + width && mouseY >= y && mouseY < y + ROW;
            if (hover) ctx.fill(x() + 1, y, x() + width - 1, y + ROW, ColorUtil.withAlpha(Theme.RED, 0.15));
            FontRenderer.draw(ctx, f, x() + 7, y + 2, hover ? Theme.TEXT : Theme.BLUE, 0.85f);
            if (hover) FontRenderer.draw(ctx, "✕", x() + width - 11, y + 2, Theme.RED, 0.8f);
            y += ROW;
        }
        input.x = x() + 4;
        input.y = y + 2;
        input.width = width - 8;
        input.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    protected boolean contentClicked(double mx, double my, int button) {
        if (input.mouseClicked(mx, my, button)) return true;
        int index = (int) ((my - (y() + HEADER + 2)) / ROW);
        List<String> list = friends();
        if (index >= 0 && index < list.size()) {
            RocketScience.friends().remove(list.get(index));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!isOver(mx, my)) input.mouseClicked(mx, my, button); // clicking elsewhere unfocuses
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return input.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return input.charTyped(chr, modifiers);
    }
}
