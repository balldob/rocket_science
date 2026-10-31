package dev.rocketscience.command.commands;

import dev.rocketscience.RocketScience;
import dev.rocketscience.command.Command;
import dev.rocketscience.util.ChatUtil;

import java.util.Locale;

/** {@code .friend add|remove|list [name]} */
public class FriendCommand extends Command {
    public FriendCommand() {
        super("friend", "Manages friends", "friend <add|remove|list> [name]", "f", "friends");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        requireArgs(args, 1);
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add" -> {
                requireArgs(args, 2);
                if (RocketScience.friends().add(args[1])) ChatUtil.info("Added " + args[1] + " as a friend.");
                else ChatUtil.error(args[1] + " is already a friend.");
            }
            case "remove", "del", "rm" -> {
                requireArgs(args, 2);
                if (RocketScience.friends().remove(args[1])) ChatUtil.info("Removed " + args[1] + ".");
                else ChatUtil.error(args[1] + " is not a friend.");
            }
            case "list" -> {
                var all = RocketScience.friends().getAll();
                ChatUtil.info(all.isEmpty() ? "No friends yet." : "Friends: " + String.join(", ", all));
            }
            default -> throw new CommandException("Usage: " + getUsage());
        }
    }
}
