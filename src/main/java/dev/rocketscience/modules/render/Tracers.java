package dev.rocketscience.modules.render;

import dev.rocketscience.RocketScience;
import dev.rocketscience.gui.Theme;
import dev.rocketscience.module.Category;
import dev.rocketscience.module.Module;
import dev.rocketscience.render.Render3D;
import dev.rocketscience.setting.*;
import dev.rocketscience.util.ColorUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Draws lines from the crosshair to nearby entities, colour coded by type: players (friends in
 * blue), hostile mobs, passive mobs and dropped items. Lines can fade with distance.
 */
public class Tracers extends Module {
    public enum Target { BODY, HEAD, FEET }

    private final BoolSetting players = bool("Players", "Trace other players", true);
    private final ColorSetting playerColor = color("Player Color", "Non-friend players", Theme.RED).visibleWhen(players::get);
    private final ColorSetting friendColor = color("Friend Color", "Friends", Theme.BLUE).visibleWhen(players::get);
    private final BoolSetting hostile = bool("Hostile", "Trace hostile mobs", true);
    private final ColorSetting hostileColor = color("Hostile Color", "Hostile mobs", 0xFFFF8C42).visibleWhen(hostile::get);
    private final BoolSetting passive = bool("Passive", "Trace animals and other passive mobs", false);
    private final ColorSetting passiveColor = color("Passive Color", "Passive mobs", 0xFF4CD964).visibleWhen(passive::get);
    private final BoolSetting items = bool("Items", "Trace dropped items", false);
    private final ColorSetting itemColor = color("Item Color", "Dropped items", 0xFFE8ECF4).visibleWhen(items::get);
    private final EnumSetting<Target> target = enumSetting("Target", "Where on the entity the line ends", Target.BODY);
    private final IntSetting range = intSetting("Range", "Maximum distance in blocks", 128, 8, 512);
    private final BoolSetting fade = bool("Distance Fade", "Fainter lines for far entities", true);
    private final DoubleSetting lineWidth = doubleSetting("Line Width", "Line thickness", 1.5, 0.5, 5.0, 0.5);

    private int count;

    public Tracers() {
        super("Tracers", "Lines from your crosshair to entities", Category.RENDER);
    }

    @Override
    public String getInfo() {
        return String.valueOf(count);
    }

    /** Colour for an entity, or 0 if it shouldn't be traced. */
    private int colorFor(Entity e) {
        if (e instanceof PlayerEntity p) {
            if (!players.get() || p == mc.player || p.isSpectator()) return 0;
            return RocketScience.friends().isFriend(p) ? friendColor.get() : playerColor.get();
        }
        if (e instanceof ItemEntity) return items.get() ? itemColor.get() : 0;
        if (e instanceof LivingEntity && !(e instanceof ArmorStandEntity)) {
            if (e.getType().getSpawnGroup() == SpawnGroup.MONSTER) return hostile.get() ? hostileColor.get() : 0;
            return passive.get() ? passiveColor.get() : 0;
        }
        return 0;
    }

    @Override
    public void onRender3D(float tickDelta) {
        count = 0;
        if (mc.world == null || mc.player == null) return;
        float lw = lineWidth.get().floatValue();
        double max = range.get();
        for (Entity e : mc.world.getEntities()) {
            int color = colorFor(e);
            if (color == 0) continue;
            double dist = e.distanceTo(mc.player);
            if (dist > max) continue;
            Vec3d lerped = e.getLerpedPos(tickDelta);
            Box box = e.getBoundingBox().offset(lerped.subtract(e.getPos()));
            Vec3d point = switch (target.get()) {
                case HEAD -> new Vec3d(lerped.x, box.maxY - 0.1, lerped.z);
                case FEET -> lerped;
                case BODY -> box.getCenter();
            };
            double alpha = fade.get() ? 1.0 - 0.75 * (dist / max) : 0.9;
            Render3D.tracer(point, ColorUtil.withAlpha(color, alpha), lw);
            count++;
        }
    }
}
