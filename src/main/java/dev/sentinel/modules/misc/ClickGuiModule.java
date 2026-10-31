package dev.sentinel.modules.misc;

import dev.sentinel.gui.ClickGuiScreen;
import dev.sentinel.module.Category;
import dev.sentinel.module.Module;
import dev.sentinel.render.FontRenderer;
import dev.sentinel.setting.BoolSetting;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;

/**
 * Opens the ClickGUI. It never stays "enabled": {@link #toggle()} just opens the screen.
 * Default bind is Right Shift (rebind in its settings or with {@code .bind clickgui <key>}).
 */
public class ClickGuiModule extends Module {
    public final BoolSetting customFont = bool("Custom Font", "Use the bundled sans-serif font for all client text", true)
            .onChange(v -> FontRenderer.useCustomFont = v);

    public ClickGuiModule() {
        super("ClickGUI", "Opens the Command Panel", Category.MISC, GLFW.GLFW_KEY_RIGHT_SHIFT);
        hideFromArrayList();
    }

    @Override
    public void toggle() {
        // Deferred so it also works from the chat ".toggle clickgui" (chat closes first).
        mc.send(() -> {
            if (mc.currentScreen == null || mc.currentScreen instanceof ChatScreen) mc.setScreen(new ClickGuiScreen());
        });
    }

    @Override
    public void setEnabled(boolean enabled, boolean notify) {
        // Intentionally stateless; ignore enable requests from configs/commands except to open.
        if (enabled) toggle();
    }
}
