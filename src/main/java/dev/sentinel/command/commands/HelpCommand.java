package dev.sentinel.command.commands;

import dev.sentinel.Sentinel;
import dev.sentinel.command.Command;
import dev.sentinel.util.ChatUtil;

/** Lists every command with its usage. */
public class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "Lists commands", "help", "?", "commands");
    }

    @Override
    public void execute(String[] args) {
        String p = Sentinel.commands().getPrefix();
        ChatUtil.info("Commands:");
        for (Command c : Sentinel.commands().getAll()) {
            ChatUtil.info("  " + p + c.getUsage() + "  - " + c.getDescription());
        }
    }
}
