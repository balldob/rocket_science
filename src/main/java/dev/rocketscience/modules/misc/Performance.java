package dev.rocketscience.modules.misc;

import dev.rocketscience.module.Category;
import dev.rocketscience.module.Module;
import dev.rocketscience.setting.BoolSetting;
import dev.rocketscience.setting.DoubleSetting;
import dev.rocketscience.setting.IntSetting;
import dev.rocketscience.util.WorldUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FPS boost.
 * <p>
 * <b>Occlusion culling</b>: vanilla only skips entities/block entities outside your view cone,
 * so mobs, chests, signs and banners behind walls are still fully rendered. This module raycasts
 * from the camera to each one through solid opaque blocks and tells the renderer to skip the
 * ones that are completely hidden ({@code EntityRenderDispatcherMixin},
 * {@code BlockEntityRenderDispatcherMixin}).
 * <p>
 * <b>No lag</b>: checks run on a per-frame time budget, round-robin, and results are cached, so
 * a crowded area never spikes a frame. A just-seen object stays visible for a short grace period
 * and its test box is padded, so things don't flicker or pop in at wall edges.
 * <p>
 * <b>Fast Preset</b> (optional): switches the heaviest vanilla video options to their fast values
 * while the module is on and restores your originals when it is turned off.
 */
public class Performance extends Module {
    private static Performance instance;

    private final BoolSetting entityCulling = bool("Entity Culling", "Don't render mobs/players hidden behind walls", true);
    private final BoolSetting blockEntityCulling = bool("Block Entity Culling", "Don't render hidden chests, signs, banners, beds...", true);
    private final BoolSetting cullPlayers = bool("Cull Players", "Also cull players (hides their nametags behind walls)", true)
            .visibleWhen(entityCulling::get);
    private final DoubleSetting budget = doubleSetting("Time Budget", "Max milliseconds per frame spent on visibility checks", 1.0, 0.25, 4.0, 0.25);
    private final IntSetting range = intSetting("Check Range", "Objects further than this are left to vanilla", 96, 32, 256);
    private final BoolSetting fastPreset = bool("Fast Preset", "Clouds off, entity shadows off, minimal particles, fast graphics, biome blend 0", false)
            .onChange(v -> this.applyPreset());

    /** Visibility cache entry. */
    private static final class Entry {
        long checkedAt;
        long visibleUntil;
        boolean culled;
    }

    private static final long GRACE_MS = 300;      // keep things visible briefly after last seen
    private static final long RECHECK_MS = 40;     // don't re-test the same object more often
    private static final double PADDING = 0.6;     // grow test boxes to avoid pop-in at edges

    private final Map<Integer, Entry> entities = new HashMap<>();
    private final Map<Long, Entry> blockEntities = new HashMap<>();
    private final ArrayDeque<Entity> entityQueue = new ArrayDeque<>();
    private final ArrayDeque<BlockEntity> blockQueue = new ArrayDeque<>();
    private final BlockPos.Mutable mutable = new BlockPos.Mutable();
    private long lastBlockRefill;
    private long lastCleanup;
    private int culledEntities, culledBlocks;

    /** Undo actions for the Fast Preset (original vanilla option values). */
    private final List<Runnable> restore = new ArrayList<>();
    private long lastCount;

    public Performance() {
        super("Performance", "FPS boost: skip rendering what you can't see", Category.MISC);
        instance = this;
    }

    @Override
    public String getInfo() {
        return culledEntities + "/" + culledBlocks;
    }

    @Override
    protected void onEnable() {
        applyPreset();
    }

    @Override
    protected void onDisable() {
        clearCaches();
        restorePreset();
    }

    @Override
    public void onWorldLeave() {
        clearCaches();
    }

    private void clearCaches() {
        entities.clear();
        blockEntities.clear();
        entityQueue.clear();
        blockQueue.clear();
        culledEntities = culledBlocks = 0;
    }

    /* ------------------------------------------------------------------ */
    /*  Queries from the render mixins (called for every object, every     */
    /*  frame - must be O(1))                                              */
    /* ------------------------------------------------------------------ */

    public static boolean isCulled(Entity entity) {
        Performance p = instance;
        if (p == null || !p.isEnabled() || !p.entityCulling.get()) return false;
        Entry e = p.entities.get(entity.getId());
        return e != null && e.culled;
    }

    public static boolean isCulled(BlockEntity be) {
        Performance p = instance;
        if (p == null || !p.isEnabled() || !p.blockEntityCulling.get()) return false;
        Entry e = p.blockEntities.get(be.getPos().asLong());
        return e != null && e.culled;
    }

    /* ------------------------------------------------------------------ */
    /*  Budgeted visibility checks                                         */
    /* ------------------------------------------------------------------ */

    @Override
    public void onRender3D(float tickDelta) {
        if (mc.world == null || mc.player == null) return;
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();
        long deadline = System.nanoTime() + (long) (budget.get() * 1_000_000);
        double rangeSq = range.get() * (double) range.get();

        // Entities
        if (entityCulling.get()) {
            if (entityQueue.isEmpty()) for (Entity e : mc.world.getEntities()) entityQueue.add(e);
            while (!entityQueue.isEmpty() && System.nanoTime() < deadline) {
                Entity e = entityQueue.poll();
                if (e.isRemoved()) continue;
                Entry entry = entities.computeIfAbsent(e.getId(), k -> new Entry());
                if (now - entry.checkedAt < RECHECK_MS) continue;
                boolean visible = mustRender(e) || e.squaredDistanceTo(cam) > rangeSq
                        || isVisible(cam, e.getBoundingBox().expand(PADDING));
                update(entry, visible, now);
            }
        } else if (!entities.isEmpty()) {
            entities.clear();
        }

        // Block entities (re-collected at most 5 times a second)
        if (blockEntityCulling.get()) {
            if (blockQueue.isEmpty() && now - lastBlockRefill > 200) {
                lastBlockRefill = now;
                WorldUtil.forEachLoadedChunk(c -> blockQueue.addAll(c.getBlockEntities().values()));
            }
            while (!blockQueue.isEmpty() && System.nanoTime() < deadline) {
                BlockEntity be = blockQueue.poll();
                BlockPos pos = be.getPos();
                Entry entry = blockEntities.computeIfAbsent(pos.asLong(), k -> new Entry());
                if (now - entry.checkedAt < RECHECK_MS) continue;
                boolean visible = pos.toCenterPos().squaredDistanceTo(cam) > rangeSq
                        || isVisible(cam, new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1).expand(PADDING));
                update(entry, visible, now);
            }
        } else if (!blockEntities.isEmpty()) {
            blockEntities.clear();
        }

        // Drop stale entries every few seconds and refresh the counters
        if (now - lastCleanup > 3000) {
            lastCleanup = now;
            entities.values().removeIf(e -> now - e.checkedAt > 5000);
            blockEntities.values().removeIf(e -> now - e.checkedAt > 5000);
        }
        if (now - lastCount > 250) { // HUD counter, refreshed 4x a second
            lastCount = now;
            culledEntities = (int) entities.values().stream().filter(e -> e.culled).count();
            culledBlocks = (int) blockEntities.values().stream().filter(e -> e.culled).count();
        }
    }

    private static void update(Entry entry, boolean visible, long now) {
        entry.checkedAt = now;
        if (visible) entry.visibleUntil = now + GRACE_MS;
        entry.culled = !visible && now > entry.visibleUntil;
    }

    /** Never cull ourselves, what we ride, or anything using the glow outline (ESP). */
    private boolean mustRender(Entity e) {
        if (e == mc.player || e == mc.getCameraEntity() || e == mc.player.getVehicle()) return true;
        if (e instanceof PlayerEntity && !cullPlayers.get()) return true;
        return mc.hasOutline(e);
    }

    /** Visible if a ray reaches the box centre or any of its 8 corners unobstructed. */
    private boolean isVisible(Vec3d cam, Box box) {
        if (box.contains(cam)) return true;
        if (rayClear(cam, box.getCenter())) return true;
        for (int i = 0; i < 8; i++) {
            Vec3d corner = new Vec3d((i & 1) == 0 ? box.minX : box.maxX, (i & 2) == 0 ? box.minY : box.maxY, (i & 4) == 0 ? box.minZ : box.maxZ);
            if (rayClear(cam, corner)) return true;
        }
        return false;
    }

    /**
     * Voxel walk (Amanatides &amp; Woo) from {@code from} to {@code to}. Blocked only by full, solid,
     * opaque blocks (stone, dirt, planks...), so glass, leaves, slabs etc. never hide anything.
     * Solid blocks around the start are ignored so a camera inside a wall (Free Cam) still works.
     */
    private boolean rayClear(Vec3d from, Vec3d to) {
        return rayClear(from.x, from.y, from.z, to.x, to.y, to.z, this::isOccluder);
    }

    /** Answers whether the block at x, y, z hides what is behind it. */
    @FunctionalInterface
    public interface Occluder {
        boolean test(int x, int y, int z);
    }

    /** Pure voxel walk, independent of Minecraft state (unit-testable). */
    public static boolean rayClear(double fx, double fy, double fz, double tx, double ty, double tz, Occluder occluder) {
        double dx = tx - fx, dy = ty - fy, dz = tz - fz;
        int x = (int) Math.floor(fx), y = (int) Math.floor(fy), z = (int) Math.floor(fz);
        int endX = (int) Math.floor(tx), endY = (int) Math.floor(ty), endZ = (int) Math.floor(tz);
        int stepX = (int) Math.signum(dx), stepY = (int) Math.signum(dy), stepZ = (int) Math.signum(dz);
        // t runs 0..1 along the ray; tDelta = t needed to cross one block on each axis
        double tDeltaX = dx == 0 ? Double.MAX_VALUE : Math.abs(1 / dx);
        double tDeltaY = dy == 0 ? Double.MAX_VALUE : Math.abs(1 / dy);
        double tDeltaZ = dz == 0 ? Double.MAX_VALUE : Math.abs(1 / dz);
        double tMaxX = dx == 0 ? Double.MAX_VALUE : tDeltaX * (dx > 0 ? (x + 1 - fx) : (fx - x));
        double tMaxY = dy == 0 ? Double.MAX_VALUE : tDeltaY * (dy > 0 ? (y + 1 - fy) : (fy - y));
        double tMaxZ = dz == 0 ? Double.MAX_VALUE : tDeltaZ * (dz > 0 ? (z + 1 - fz) : (fz - z));

        boolean leavingStart = true;
        for (int i = 0; i < 1024; i++) {
            boolean solid = occluder.test(x, y, z);
            if (leavingStart) {
                if (!solid) leavingStart = false;
            } else if (solid) {
                return false; // the target point itself may be inside a wall - that counts as blocked
            }
            // Reached the target voxel, or the next boundary lies beyond the target
            if ((x == endX && y == endY && z == endZ) || Math.min(tMaxX, Math.min(tMaxY, tMaxZ)) > 1.0) return true;
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                x += stepX;
                tMaxX += tDeltaX;
            } else if (tMaxY < tMaxZ) {
                y += stepY;
                tMaxY += tDeltaY;
            } else {
                z += stepZ;
                tMaxZ += tDeltaZ;
            }
        }
        return true; // extremely long ray: never risk hiding something
    }

    private boolean isOccluder(int x, int y, int z) {
        mutable.set(x, y, z);
        BlockState state = mc.world.getBlockState(mutable);
        return state.isOpaque() && state.isSolidBlock(mc.world, mutable);
    }

    /* ------------------------------------------------------------------ */
    /*  Fast Preset                                                        */
    /* ------------------------------------------------------------------ */

    private void applyPreset() {
        if (!isEnabled() || !fastPreset.get() || mc.options == null) {
            restorePreset();
            return;
        }
        if (!restore.isEmpty()) return; // already applied
        var o = mc.options;
        setEnum(o.getCloudRenderMode(), "OFF");
        setEnum(o.getParticles(), "MINIMAL");
        setEnum(o.getGraphicsMode(), "FAST");
        set(o.getEntityShadows(), false);
        set(o.getBiomeBlendRadius(), 0);
    }

    private void restorePreset() {
        // restore in reverse order
        for (int i = restore.size() - 1; i >= 0; i--) restore.get(i).run();
        restore.clear();
    }

    /** Sets an option, remembering its old value for {@link #restorePreset()}. */
    private <T> void set(SimpleOption<T> option, T value) {
        T old = option.getValue();
        restore.add(() -> option.setValue(old));
        option.setValue(value);
    }

    /** Sets an enum option by constant name (avoids depending on where the enum class lives). */
    private <T extends Enum<T>> void setEnum(SimpleOption<T> option, String constant) {
        for (T c : option.getValue().getDeclaringClass().getEnumConstants()) {
            if (c.name().equals(constant)) {
                set(option, c);
                return;
            }
        }
    }
}
