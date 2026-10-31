package dev.rocketscience.command.commands;

import dev.rocketscience.RocketScience;
import dev.rocketscience.command.Command;
import dev.rocketscience.util.ChatUtil;

/** {@code .prefix <new>} - changes the command prefix. */
public class PrefixCommand extends Command {
    public PrefixCommand() {
        super("prefix", "Changes the command prefix", "prefix <symbol>");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        requireArgs(args, 1);
        RocketScience.commands().setPrefix(args[0]);
        RocketScience.config().save();
        ChatUtil.info("Prefix is now '" + RocketScience.commands().getPrefix() + "'.");
    }
}
