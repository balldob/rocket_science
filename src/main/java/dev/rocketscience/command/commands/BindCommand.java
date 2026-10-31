package dev.rocketscience.command.commands;

import dev.rocketscience.RocketScience;
import dev.rocketscience.command.Command;
import dev.rocketscience.module.Module;
import dev.rocketscience.util.ChatUtil;
import dev.rocketscience.util.KeyUtil;

/** {@code .bind <module> <key|none>} */
public class BindCommand extends Command {
    public BindCommand() {
        super("bind", "Binds a module to a key (use 'none' to unbind)", "bind <module> <key|none>", "b");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        requireArgs(args, 2);
        Module m = RocketScience.modules().getByName(args[0]);
        if (m == null) throw new CommandException("No module named '" + args[0] + "'.");
        int key = KeyUtil.parse(args[1]);
        if (key == KeyUtil.INVALID) throw new CommandException("Unknown key '" + args[1] + "'.");
        m.getBind().set(key);
        ChatUtil.info(m.getName() + " bound to " + KeyUtil.name(key) + ".");
    }
}
