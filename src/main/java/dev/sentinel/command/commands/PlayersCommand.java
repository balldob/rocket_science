package dev.sentinel.command.commands;

import dev.sentinel.Sentinel;
import dev.sentinel.command.Command;
import dev.sentinel.modules.world.PlayerFinder;
import dev.sentinel.util.ChatUtil;

import java.util.List;

/** {@code .players [clear]} - prints or clears the Player Finder log. */
public class PlayersCommand extends Command {
    public PlayersCommand() {
        super("players", "Shows the Player Finder log", "players [clear]", "log");
    }

    @Override
    public void execute(String[] args) {
        PlayerFinder finder = Sentinel.modules().get(PlayerFinder.class);
        if (args.length > 0 && args[0].equalsIgnoreCase("clear")) {
            finder.clearLog();
            ChatUtil.info("Player log cleared.");
            return;
        }
        List<PlayerFinder.LogEntry> log = finder.getLog();
        if (log.isEmpty()) {
            ChatUtil.info("No players logged yet.");
            return;
        }
        int shown = Math.min(10, log.size());
        ChatUtil.info("Last " + shown + " sightings:");
        for (int i = 0; i < shown; i++) ChatUtil.info("  " + log.get(i).format());
    }
}
