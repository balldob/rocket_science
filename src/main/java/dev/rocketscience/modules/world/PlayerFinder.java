package dev.rocketscience.modules.world;

import dev.rocketscience.gui.Theme;
import dev.rocketscience.hud.Notifications;
import dev.rocketscience.module.Category;
import dev.rocketscience.module.Module;
import dev.rocketscience.render.FontRenderer;
import dev.rocketscience.render.Projection;
import dev.rocketscience.render.Render2D;
import dev.rocketscience.render.Render3D;
import dev.rocketscience.setting.BoolSetting;
import dev.rocketscience.setting.ColorSetting;
import dev.rocketscience.setting.IntSetting;
import dev.rocketscience.RocketScience;
import dev.rocketscience.util.ChatUtil;
import dev.rocketscience.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Logs players entering and leaving render distance (name, coordinates, distance, time).
 * The log is shown in the ClickGUI "Players" panel and via {@code .players}. A waypoint marks
 * each player's last known position after they leave.
 */
public class PlayerFinder extends Module {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX_LOG = 200;

    /** One sighting. {@code entered=false} means the player left render distance. */
    public record LogEntry(String name, UUID uuid, boolean entered, Vec3d pos, double distance, LocalTime time) {
        public String timeString() {
            return time.format(TIME);
        }

        public String format() {
            return String.format("[%s] %s %s at %d, %d, %d (%.0fm)", timeString(), name, entered ? "entered" : "left",
                    (int) pos.x, (int) pos.y, (int) pos.z, distance);
        }
    }

    /** Last known position, used for waypoints. */
    private record LastSeen(String name, Vec3d pos, long time) {
    }

    private final BoolSetting toast = bool("Toast", "Toast when a player appears", true);
    private final BoolSetting sound = bool("Sound", "Play a ping when a player appears", false);
    private final BoolSetting chat = bool("Chat Log", "Print sightings in chat", false);
    private final BoolSetting logLeave = bool("Log Leaving", "Also log players leaving render distance", true);
    private final BoolSetting ignoreFriends = bool("Ignore Friends", "Don't alert for friends", false);
    private final BoolSetting waypoints = bool("Waypoints", "Mark last known positions", true);
    private final IntSetting waypointMinutes = intSetting("Waypoint Time", "Minutes to keep waypoints", 5, 1, 60).visibleWhen(waypoints::get);
    private final ColorSetting waypointColor = color("Waypoint Color", "Waypoint colour", Theme.RED).visibleWhen(waypoints::get);

    private final List<LogEntry> log = new ArrayList<>();
    private final Map<UUID, AbstractClientPlayerEntity> visible = new HashMap<>();
    private final Map<UUID, LastSeen> lastSeen = new LinkedHashMap<>();

    public PlayerFinder() {
        super("Player Finder", "Log players entering render distance", Category.WORLD);
    }

    /** Newest first. */
    public List<LogEntry> getLog() {
        return Collections.unmodifiableList(log);
    }

    public void clearLog() {
        log.clear();
    }

    @Override
    public String getInfo() {
        return String.valueOf(visible.size());
    }

    @Override
    protected void onDisable() {
        visible.clear();
    }

    @Override
    public void onWorldLeave() {
        visible.clear();
        lastSeen.clear();
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) return;
        Map<UUID, AbstractClientPlayerEntity> now = new HashMap<>();
        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p != mc.player) now.put(p.getUuid(), p);
        }

        // New arrivals
        for (var e : now.entrySet()) {
            if (visible.containsKey(e.getKey())) continue;
            AbstractClientPlayerEntity p = e.getValue();
            LogEntry entry = record(p.getGameProfile().getName(), p.getUuid(), true, p.getPos());
            boolean quiet = ignoreFriends.get() && RocketScience.friends().isFriend(p);
            if (!quiet) alert(entry);
        }
        // Departures
        for (var e : visible.entrySet()) {
            if (now.containsKey(e.getKey())) continue;
            AbstractClientPlayerEntity p = e.getValue();
            String name = p.getGameProfile().getName();
            if (logLeave.get()) {
                LogEntry entry = record(name, p.getUuid(), false, p.getPos());
                if (chat.get()) ChatUtil.info(entry.format());
            }
            lastSeen.put(p.getUuid(), new LastSeen(name, p.getPos(), System.currentTimeMillis()));
        }
        // Visible players aren't "last seen" yet
        now.keySet().forEach(lastSeen::remove);
        visible.clear();
        visible.putAll(now);

        long expiry = waypointMinutes.get() * 60_000L;
        lastSeen.values().removeIf(l -> System.currentTimeMillis() - l.time > expiry);
    }

    private LogEntry record(String name, UUID uuid, boolean entered, Vec3d pos) {
        LogEntry entry = new LogEntry(name, uuid, entered, pos, pos.distanceTo(mc.player.getPos()), LocalTime.now());
        log.add(0, entry);
        if (log.size() > MAX_LOG) log.remove(log.size() - 1);
        return entry;
    }

    private void alert(LogEntry e) {
        if (toast.get()) {
            Notifications.push("Player spotted", String.format("%s  %d, %d, %d  (%.0fm)", e.name(),
                    (int) e.pos().x, (int) e.pos().y, (int) e.pos().z, e.distance()), Notifications.Type.WARNING, 4000);
        }
        if (sound.get()) mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.6f));
        if (chat.get()) ChatUtil.info(e.format());
    }

    @Override
    public void onRender3D(float tickDelta) {
        if (!waypoints.get()) return;
        int c = waypointColor.get();
        for (LastSeen l : lastSeen.values()) {
            Vec3d p = l.pos;
            Render3D.box(new Box(p.x - 0.3, p.y, p.z - 0.3, p.x + 0.3, p.y + 1.8, p.z + 0.3), ColorUtil.withAlpha(c, 0.15), c, 1.5f);
            Render3D.line(new Vec3d(p.x, p.y + 1.8, p.z), new Vec3d(p.x, p.y + 40, p.z), ColorUtil.withAlpha(c, 0.5), 1.5f);
        }
    }

    @Override
    public void onRender2D(DrawContext ctx, float tickDelta) {
        if (!waypoints.get() || mc.player == null || mc.options.hudHidden) return;
        long now = System.currentTimeMillis();
        for (LastSeen l : lastSeen.values()) {
            double[] s = Projection.toScreen(l.pos.add(0, 2.3, 0));
            if (s == null) continue;
            long secs = (now - l.time) / 1000;
            String label = String.format("%s  %.0fm  %s ago", l.name, l.pos.distanceTo(mc.player.getPos()),
                    secs < 60 ? secs + "s" : (secs / 60) + "m");
            float w = FontRenderer.width(label, 0.8f) + 8;
            int x = (int) (s[0] - w / 2), y = (int) s[1];
            Render2D.roundedRect(ctx, x, y, (int) w, 11, 3, ColorUtil.withAlpha(Theme.BG, 0.75));
            Render2D.roundedOutline(ctx, x, y, (int) w, 11, 3, ColorUtil.withAlpha(waypointColor.get(), 0.8));
            FontRenderer.draw(ctx, label, x + 4, y + 2, Theme.TEXT, 0.8f);
        }
    }
}
