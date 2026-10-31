package dev.rocketscience;

import dev.rocketscience.command.CommandManager;
import dev.rocketscience.config.ConfigManager;
import dev.rocketscience.friend.FriendManager;
import dev.rocketscience.hud.HudRenderer;
import dev.rocketscience.module.ModuleManager;
import dev.rocketscience.modules.misc.Hud;
import dev.rocketscience.modules.world.BaseFinder;
import dev.rocketscience.render.Projection;
import dev.rocketscience.render.Render3D;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client entrypoint. Creates the managers and wires Fabric API events to them. Mixins are only
 * used for things Fabric API has no event for (camera override, input, glow outlines, raw keys).
 */
public final class RocketScience implements ClientModInitializer {
    public static final String NAME = "Rocket Science";
    public static final String MOD_ID = "rocketscience";
    public static final String VERSION = "1.0";
    public static final Logger LOG = LoggerFactory.getLogger(NAME);

    private static ModuleManager modules;
    private static CommandManager commands;
    private static FriendManager friends;
    private static ConfigManager config;

    @Override
    public void onInitializeClient() {
        migrateOldConfig();
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

    /** The client used to be called "Sentinel": move its config folder over once. */
    private static void migrateOldConfig() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path oldDir = gameDir.resolve("sentinel");
        Path newDir = gameDir.resolve(MOD_ID);
        if (Files.isDirectory(oldDir) && !Files.exists(newDir)) {
            try {
                Files.move(oldDir, newDir);
                LOG.info("Migrated config from {} to {}", oldDir, newDir);
            } catch (IOException e) {
                LOG.warn("Could not migrate old config folder", e);
            }
        }
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
