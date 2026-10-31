package dev.rocketscience.gui.component.setting;

import dev.rocketscience.gui.Animation;
import dev.rocketscience.gui.Theme;
import dev.rocketscience.gui.component.TextField;
import dev.rocketscience.render.FontRenderer;
import dev.rocketscience.setting.StringListSetting;
import dev.rocketscience.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/** Expandable list editor: click an entry to remove it, type in the field and press Enter to add. */
public class StringListComponent extends SettingComponent<StringListSetting> {
    private static final int ENTRY = 10;
    private final Animation open = new Animation(0, 16);
    private final TextField input;
    private boolean expanded;
    private long invalidUntil;

    public StringListComponent(StringListSetting setting) {
        super(setting);
        input = new TextField("add id + Enter", 64, false).size(12, 0.75f);
        input.onSubmit(text -> {
            if (setting.add(text)) input.setText("");
            else invalidUntil = System.currentTimeMillis() + 800;
        });
    }

    private int bodyHeight() {
        return setting.get().size() * ENTRY + 16;
    }

    @Override
    public int height() {
        return LINE + (int) Math.round(bodyHeight() * open.get());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        open.setTarget(expanded ? 1 : 0);
        double o = open.update();
        boolean hover = within(mouseX, mouseY, x, y, width, LINE);
        drawLabel(ctx, hover ? Theme.TEXT : ColorUtil.lerp(Theme.MUTED, Theme.TEXT, 0.6));
        drawRight(ctx, setting.get().size() + (expanded ? " ▴" : " ▾"), Theme.BLUE, 2);
        if (o <= 0.01) return;

        int top = y + LINE;
        ctx.enableScissor(x, top, x + width, top + (int) Math.round(bodyHeight() * o));
        int ey = top;
        for (String entry : setting.get()) {
            boolean hov = within(mouseX, mouseY, x, ey, width, ENTRY);
            if (hov) ctx.fill(x + 2, ey, x + width - 2, ey + ENTRY, ColorUtil.withAlpha(Theme.RED, 0.18));
            String shown = entry.startsWith("minecraft:") ? entry.substring(10) : entry;
            FontRenderer.draw(ctx, shown, x + 5, ey + 2, hov ? Theme.TEXT : Theme.MUTED, 0.75f);
            if (hov) FontRenderer.draw(ctx, "✕", x + width - 9, ey + 2, Theme.RED, 0.75f);
            ey += ENTRY;
        }
        input.x = x + 2;
        input.y = ey + 2;
        input.width = width - 4;
        input.render(ctx, mouseX, mouseY, delta);
        if (System.currentTimeMillis() < invalidUntil) {
            ctx.fill(input.x, input.y + input.height() - 1, input.x + input.width, input.y + input.height(), Theme.RED);
        }
        ctx.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (within(mouseX, mouseY, x, y, width, LINE)) {
            expanded = !expanded;
            return true;
        }
        if (!expanded || !isHovered(mouseX, mouseY)) {
            input.mouseClicked(mouseX, mouseY, button); // unfocus
            return false;
        }
        if (input.mouseClicked(mouseX, mouseY, button)) return true;
        int index = (int) ((mouseY - (y + LINE)) / ENTRY);
        List<String> list = setting.get();
        if (index >= 0 && index < list.size()) setting.remove(list.get(index));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return expanded && input.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return expanded && input.charTyped(chr, modifiers);
    }
}
