package dev.rocketscience.command;

import java.util.List;

/**
 * Base class for chat commands. Subclasses implement {@link #execute(String[])} and throw
 * {@link CommandException} with a user facing message when input is wrong.
 */
public abstract class Command {
    private final String name;
    private final String description;
    private final String usage;
    private final List<String> aliases;

    protected Command(String name, String description, String usage, String... aliases) {
        this.name = name;
        this.description = description;
        this.usage = usage;
        this.aliases = List.of(aliases);
    }

    public abstract void execute(String[] args) throws CommandException;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    public List<String> getAliases() {
        return aliases;
    }

    /** Throws a usage error if fewer than {@code n} arguments were supplied. */
    protected void requireArgs(String[] args, int n) throws CommandException {
        if (args.length < n) throw new CommandException("Usage: " + usage);
    }

    /** Signals a user error; the message is printed in red. */
    public static class CommandException extends Exception {
        public CommandException(String message) {
            super(message);
        }
    }
}
