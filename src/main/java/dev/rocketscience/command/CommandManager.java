package dev.rocketscience.command;

import dev.rocketscience.RocketScience;
import dev.rocketscience.command.commands.*;
import dev.rocketscience.util.ChatUtil;

import java.util.*;

/** Registers commands and dispatches chat input that starts with the prefix (default "."). */
public final class CommandManager {
    private final List<Command> commands = new ArrayList<>();
    private String prefix = ".";

    public CommandManager() {
        register(new HelpCommand());
        register(new ToggleCommand());
        register(new BindCommand());
        register(new SetCommand());
        register(new FriendCommand());
        register(new ConfigCommand());
        register(new PlayersCommand());
        register(new PrefixCommand());
        register(new GuiCommand());
    }

    public void register(Command command) {
        commands.add(command);
    }

    public List<Command> getAll() {
        return Collections.unmodifiableList(commands);
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        if (prefix != null && !prefix.isBlank() && !prefix.contains(" ")) this.prefix = prefix;
    }

    /** Parses and runs a command line (without the prefix). */
    public void dispatch(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            ChatUtil.error("Type " + prefix + "help for a list of commands.");
            return;
        }
        String name = parts[0].toLowerCase(Locale.ROOT);
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);
        for (Command c : commands) {
            if (c.getName().equals(name) || c.getAliases().contains(name)) {
                try {
                    c.execute(args);
                } catch (Command.CommandException e) {
                    ChatUtil.error(e.getMessage());
                } catch (Exception e) {
                    RocketScience.LOG.error("Command {} failed", name, e);
                    ChatUtil.error("Command failed: " + e.getMessage());
                }
                return;
            }
        }
        ChatUtil.error("Unknown command '" + name + "'. Type " + prefix + "help.");
    }
}
