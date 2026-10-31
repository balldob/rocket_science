package dev.rocketscience.gui.component;

import dev.rocketscience.RocketScience;
import dev.rocketscience.gui.Theme;
import dev.rocketscience.hud.Notifications;
import dev.rocketscience.modules.world.PlayerFinder;
import dev.rocketscience.render.FontRenderer;
import dev.rocketscience.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/**
 * The Player Finder "tab": a log of players entering/leaving render distance.
 * Click a row to toggle that player's friend status. "clear" in the header wipes the log.
 */
public class PlayersPanel extends Panel {
    private static final int ROW = 20;
    private static final int MAX_ROWS = 12;

    public PlayersPanel() {
        super("Players", 168);
    }

    private List<PlayerFinder.LogEntry> entries() {
        List<PlayerFinder.LogEntry> log = RocketScience.modules().get(PlayerFinder.class).getLog();
        return log.subList(0, Math.min(MAX_ROWS, log.size()));
    }

    @Override
    protected String headerInfo() {
        return "clear";
    }

    @Override
    protected int accent() {
        return Theme.BLUE;
    }

    @Override
    protected int contentHeight() {
        return Math.max(1, entries().size()) * ROW;
    }

    @Override
    protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta, int top) {
        List<PlayerFinder.LogEntry> list = entries();
        if (list.isEmpty()) {
            String msg = RocketScience.modules().get(PlayerFinder.class).isEnabled() ? "no sightings yet" : "enable Player Finder";
            FontRenderer.draw(ctx, msg, x() + 7, top + 5, Theme.MUTED, Theme.SMALL_SCALE);
            return;
        }
        int y = top;
        for (PlayerFinder.LogEntry e : list) {
            boolean hover = mouseX >= x() && mouseX < x() + width && mouseY >= y && mouseY < y + ROW;
            boolean friend = RocketScience.friends().isFriend(e.name());
            if (hover) ctx.fill(x() + 1, y, x() + width - 1, y + ROW, ColorUtil.withAlpha(Theme.BLUE, 0.10));
            // enter = blue marker, leave = red marker
            ctx.fill(x() + 3, y + 3, x() + 5, y + ROW - 3, e.entered() ? Theme.BLUE : Theme.RED);
            FontRenderer.draw(ctx, e.name(), x() + 9, y + 2, friend ? Theme.BLUE : Theme.TEXT, 0.85f);
            String time = e.timeString();
            FontRenderer.draw(ctx, time, x() + width - 6 - FontRenderer.width(time, 0.7f), y + 3, Theme.MUTED, 0.7f);
            String detail = String.format("%s %d, %d, %d  %.0fm", e.entered() ? "in" : "out",
                    (int) e.pos().x, (int) e.pos().y, (int) e.pos().z, e.distance());
            FontRenderer.draw(ctx, detail, x() + 9, y + 11, Theme.MUTED, 0.7f);
            y += ROW;
        }
    }

    @Override
    protected boolean contentClicked(double mx, double my, int button) {
        int index = (int) ((my - (y() + HEADER + 2)) / ROW);
        List<PlayerFinder.LogEntry> list = entries();
        if (index < 0 || index >= list.size()) return false;
        String name = list.get(index).name();
        boolean now = RocketScience.friends().toggle(name);
        Notifications.push("Friends", (now ? "Added " : "Removed ") + name, now ? Notifications.Type.ON : Notifications.Type.OFF);
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // "clear" label in the header
        if (button == 0 && my >= y() && my < y() + HEADER && mx > x() + width - 36 && mx < x() + width) {
            RocketScience.modules().get(PlayerFinder.class).clearLog();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }
}
