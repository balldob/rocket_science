package dev.sentinel.command.commands;

import dev.sentinel.Sentinel;
import dev.sentinel.command.Command;
import dev.sentinel.module.Module;
import dev.sentinel.util.ChatUtil;
import dev.sentinel.util.KeyUtil;

/** {@code .bind <module> <key|none>} */
public class BindCommand extends Command {
    public BindCommand() {
        super("bind", "Binds a module to a key (use 'none' to unbind)", "bind <module> <key|none>", "b");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        requireArgs(args, 2);
        Module m = Sentinel.modules().getByName(args[0]);
        if (m == null) throw new CommandException("No module named '" + args[0] + "'.");
        int key = KeyUtil.parse(args[1]);
        if (key == KeyUtil.INVALID) throw new CommandException("Unknown key '" + args[1] + "'.");
        m.getBind().set(key);
        ChatUtil.info(m.getName() + " bound to " + KeyUtil.name(key) + ".");
    }
}
