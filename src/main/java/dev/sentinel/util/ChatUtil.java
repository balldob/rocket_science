package dev.sentinel.util;

import dev.sentinel.Sentinel;
import dev.sentinel.gui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Client-side chat output with a themed "[Sentinel]" prefix. Nothing is sent to the server. */
public final class ChatUtil {
    private ChatUtil() {
    }

    private static MutableText prefix() {
        return Text.literal("[").formatted(Formatting.DARK_GRAY)
                .append(Text.literal(Sentinel.NAME).styled(s -> s.withColor(Theme.BLUE & 0xFFFFFF)))
                .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
    }

    public static void send(Text text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.inGameHud == null) return;
        mc.inGameHud.getChatHud().addMessage(prefix().append(text));
    }

    public static void info(String message) {
        send(Text.literal(message).styled(s -> s.withColor(Theme.TEXT & 0xFFFFFF)));
    }

    public static void error(String message) {
        send(Text.literal(message).styled(s -> s.withColor(Theme.RED & 0xFFFFFF)));
    }
}
