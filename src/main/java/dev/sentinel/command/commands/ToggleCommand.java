package dev.sentinel.command.commands;

import dev.sentinel.Sentinel;
import dev.sentinel.command.Command;
import dev.sentinel.module.Module;
import dev.sentinel.util.ChatUtil;

/** {@code .toggle <module>} */
public class ToggleCommand extends Command {
    public ToggleCommand() {
        super("toggle", "Toggles a module", "toggle <module>", "t");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        requireArgs(args, 1);
        Module m = Sentinel.modules().getByName(String.join("", args));
        if (m == null) throw new CommandException("No module named '" + String.join(" ", args) + "'.");
        m.toggle();
        ChatUtil.info(m.getName() + (m.isEnabled() ? " enabled." : " disabled."));
    }
}
