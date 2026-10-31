package dev.sentinel.command.commands;

import dev.sentinel.Sentinel;
import dev.sentinel.command.Command;
import dev.sentinel.util.ChatUtil;

import java.util.Locale;

/** {@code .config save|load|delete|list [name]} - manages named profiles. */
public class ConfigCommand extends Command {
    public ConfigCommand() {
        super("config", "Saves/loads named profiles", "config <save|load|delete|list> [name]", "cfg", "profile");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        requireArgs(args, 1);
        var cfg = Sentinel.config();
        String name = args.length > 1 ? args[1] : cfg.getActiveProfile();
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "save" -> {
                if (cfg.saveProfile(name)) ChatUtil.info("Saved profile '" + cfg.getActiveProfile() + "'.");
                else throw new CommandException("Could not save profile '" + name + "'.");
            }
            case "load" -> {
                requireArgs(args, 2);
                if (cfg.loadProfile(name)) ChatUtil.info("Loaded profile '" + name + "'.");
                else throw new CommandException("Profile '" + name + "' not found.");
            }
            case "delete" -> {
                requireArgs(args, 2);
                if (cfg.deleteProfile(name)) ChatUtil.info("Deleted profile '" + name + "'.");
                else throw new CommandException("Can't delete '" + name + "' (missing or active).");
            }
            case "list" -> {
                var names = cfg.listProfiles();
                ChatUtil.info("Profiles (" + names.size() + "): " + String.join(", ", names)
                        + "  [active: " + cfg.getActiveProfile() + "]");
            }
            default -> throw new CommandException("Usage: " + getUsage());
        }
    }
}
