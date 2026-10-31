package dev.sentinel;

import dev.sentinel.command.CommandManager;
import dev.sentinel.config.ConfigManager;
import dev.sentinel.friend.FriendManager;
import dev.sentinel.hud.HudRenderer;
import dev.sentinel.module.ModuleManager;
import dev.sentinel.modules.misc.Hud;
import dev.sentinel.modules.world.BaseFinder;
import dev.sentinel.render.Projection;
import dev.sentinel.render.Render3D;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entrypoint. Creates the managers and wires Fabric API events to them. Mixins are only
 * used for things Fabric API has no event for (camera override, input, glow outlines, raw keys).
 */
public final class Sentinel implements ClientModInitializer {
    public static final String NAME = "Sentinel";
    public static final String MOD_ID = "sentinel";
    public static final String VERSION = "1.0";
    public static final Logger LOG = LoggerFactory.getLogger(NAME);

    private static ModuleManager modules;
    private static CommandManager commands;
    private static FriendManager friends;
    private static ConfigManager config;

    @Override
    public void onInitializeClient() {
        modules = new ModuleManager();
        modules.init();
        commands = new CommandManager();
        friends = new FriendManager();
        config = new ConfigManager();

        friends.load();
        config.load();

        // --- ticks -------------------------------------------------------
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player != null) modules.onPreTick();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) modules.onTick();
        });

        // --- rendering ---------------------------------------------------
        WorldRenderEvents.LAST.register(context -> {
            float tickDelta = context.tickCounter().getTickDelta(true);
            Projection.update(context.camera(), context.projectionMatrix());
            modules.onRender3D(tickDelta);
            Render3D.flush(context.camera());
        });
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            float tickDelta = tickCounter.getTickDelta(true);
            modules.onRender2D(ctx, tickDelta);
            if (modules.get(Hud.class).isEnabled()) HudRenderer.render(ctx, modules.get(Hud.class));
        });

        // --- chat commands -----------------------------------------------
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (message.startsWith(commands.getPrefix())) {
                commands.dispatch(message.substring(commands.getPrefix().length()));
                return false; // never send commands to the server
            }
            return true;
        });

        // --- world lifecycle ---------------------------------------------
        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> modules.get(BaseFinder.class).onChunkLoad(chunk));
        ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> modules.get(BaseFinder.class).onChunkUnload(chunk));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(modules::onWorldLeave));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            config.save();
            friends.save();
        });

        LOG.info("{} {} initialised with {} modules", NAME, VERSION, modules.getAll().size());
    }

    public static ModuleManager modules() {
        return modules;
    }

    public static CommandManager commands() {
        return commands;
    }

    public static FriendManager friends() {
        return friends;
    }

    public static ConfigManager config() {
        return config;
    }
}
