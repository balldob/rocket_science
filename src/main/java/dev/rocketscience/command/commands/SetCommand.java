package dev.rocketscience.command.commands;

import dev.rocketscience.RocketScience;
import dev.rocketscience.command.Command;
import dev.rocketscience.module.Module;
import dev.rocketscience.setting.Setting;
import dev.rocketscience.util.ChatUtil;

import java.util.Arrays;

/** {@code .set <module> <setting> <value>} - edits any setting from chat. */
public class SetCommand extends Command {
    public SetCommand() {
        super("set", "Changes a module setting", "set <module> <setting> <value>", "s");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        requireArgs(args, 3);
        Module m = RocketScience.modules().getByName(args[0]);
        if (m == null) throw new CommandException("No module named '" + args[0] + "'.");
        Setting<?> s = m.getSetting(args[1]);
        if (s == null) throw new CommandException(m.getName() + " has no setting '" + args[1] + "'.");
        String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (!s.parse(value)) throw new CommandException("Invalid value '" + value + "' for " + s.getName() + ".");
        ChatUtil.info(m.getName() + " > " + s.getName() + " = " + s.displayValue());
    }
}
