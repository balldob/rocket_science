package dev.rocketscience.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;

import java.util.function.Consumer;

/** Helpers for iterating the client's loaded chunks and geometry maths. */
public final class WorldUtil {
    private WorldUtil() {
    }

    /** Calls {@code action} for every loaded chunk around the player (client view distance + 1). */
    public static void forEachLoadedChunk(Consumer<WorldChunk> action) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        int radius = mc.options.getViewDistance().getValue() + 1;
        ChunkPos center = mc.player.getChunkPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                WorldChunk chunk = getChunk(center.x + dx, center.z + dz);
                if (chunk != null) action.accept(chunk);
            }
        }
    }

    /** Returns the loaded chunk at the given chunk coords, or null if not loaded. */
    public static WorldChunk getChunk(int cx, int cz) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return null;
        WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(cx, cz);
        return chunk == null || chunk instanceof EmptyChunk ? null : chunk;
    }

    /** Yaw/pitch (degrees) needed to look from {@code from} to {@code to}. */
    public static float[] anglesTo(Vec3d from, Vec3d to) {
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        return new float[]{yaw, pitch};
    }
}
