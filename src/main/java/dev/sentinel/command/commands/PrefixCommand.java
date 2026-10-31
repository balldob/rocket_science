package dev.sentinel.command.commands;

import dev.sentinel.Sentinel;
import dev.sentinel.command.Command;
import dev.sentinel.util.ChatUtil;

/** {@code .prefix <new>} - changes the command prefix. */
public class PrefixCommand extends Command {
    public PrefixCommand() {
        super("prefix", "Changes the command prefix", "prefix <symbol>");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        requireArgs(args, 1);
        Sentinel.commands().setPrefix(args[0]);
        Sentinel.config().save();
        ChatUtil.info("Prefix is now '" + Sentinel.commands().getPrefix() + "'.");
    }
}
