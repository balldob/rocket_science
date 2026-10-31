package dev.rocketscience.command.commands;

import dev.rocketscience.RocketScience;
import dev.rocketscience.command.Command;
import dev.rocketscience.gui.ClickGuiScreen;
import dev.rocketscience.gui.GuiState;
import dev.rocketscience.util.ChatUtil;
import net.minecraft.client.MinecraftClient;

/** {@code .gui reset} - puts all ClickGUI panels back into a layout that fits the screen. */
public class GuiCommand extends Command {
    public GuiCommand() {
        super("gui", "ClickGUI layout tools", "gui reset");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length < 1 || !args[0].equalsIgnoreCase("reset")) throw new CommandException("Usage: " + getUsage());
        if (MinecraftClient.getInstance().currentScreen instanceof ClickGuiScreen gui) gui.resetLayout();
        else GuiState.reset(); // panels are re-flowed the next time the GUI opens
        RocketScience.config().save();
        ChatUtil.info("ClickGUI layout reset.");
    }
}
