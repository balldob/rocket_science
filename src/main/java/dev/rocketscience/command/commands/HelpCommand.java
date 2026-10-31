package dev.rocketscience.command.commands;

import dev.rocketscience.RocketScience;
import dev.rocketscience.command.Command;
import dev.rocketscience.util.ChatUtil;

/** Lists every command with its usage. */
public class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "Lists commands", "help", "?", "commands");
    }

    @Override
    public void execute(String[] args) {
        String p = RocketScience.commands().getPrefix();
        ChatUtil.info("Commands:");
        for (Command c : RocketScience.commands().getAll()) {
            ChatUtil.info("  " + p + c.getUsage() + "  - " + c.getDescription());
        }
    }
}
