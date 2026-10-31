package dev.sentinel.modules.combat;

import dev.sentinel.Sentinel;
import dev.sentinel.module.Category;
import dev.sentinel.module.Module;
import dev.sentinel.modules.render.FreeCam;
import dev.sentinel.setting.BoolSetting;
import dev.sentinel.setting.DoubleSetting;
import dev.sentinel.setting.EnumSetting;
import dev.sentinel.util.WorldUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;

/**
 * Gently eases your view toward the best target inside an FOV cone. Rotation is applied per
 * frame with exponential smoothing (never snapped) through vanilla's own
 * {@code changeLookDirection}, exactly as if the mouse had moved.
 */
public class AimAssist extends Module {
    public enum Priority { DISTANCE, HEALTH, ANGLE }

    private final DoubleSetting fov = doubleSetting("FOV", "Cone (degrees) in which targets are considered", 60, 5, 180, 1);
    private final DoubleSetting range = doubleSetting("Range", "Maximum target distance", 4.5, 1, 8, 0.1);
    private final DoubleSetting speed = doubleSetting("Speed", "Higher = faster, lower = smoother", 5, 0.5, 20, 0.5);
    private final EnumSetting<Priority> priority = enumSetting("Priority", "Which target to prefer", Priority.ANGLE);
    private final BoolSetting playersOnly = bool("Players Only", "Ignore mobs", true);
    private final BoolSetting ignoreFriends = bool("Ignore Friends", "Never aim at friends", true);
    private final BoolSetting ignoreInvisible = bool("Ignore Invisible", "Skip invisible entities", true);
    private final BoolSetting vertical = bool("Vertical", "Also adjust pitch", true);
    private final BoolSetting requireClick = bool("Require Click", "Only assist while holding attack", false);

    private long lastFrame;

    public AimAssist() {
        super("Aim Assist", "Smoothly nudges your aim toward targets", Category.COMBAT);
    }

    @Override
    public String getInfo() {
        return priority.displayValue();
    }

    @Override
    protected void onEnable() {
        lastFrame = 0;
    }

    /** Runs every frame (from the world render hook) for smooth, frame-rate independent easing. */
    @Override
    public void onRender3D(float tickDelta) {
        long now = System.nanoTime();
        double dt = lastFrame == 0 ? 0 : Math.min((now - lastFrame) / 1e9, 0.05);
        lastFrame = now;

        var player = mc.player;
        if (player == null || mc.world == null || mc.currentScreen != null || dt <= 0) return;
        if (Sentinel.modules().get(FreeCam.class).isActive()) return;
        if (requireClick.get() && !mc.options.attackKey.isPressed()) return;

        LivingEntity target = findTarget(tickDelta);
        if (target == null) return;

        float[] want = WorldUtil.anglesTo(player.getEyePos(), aimPoint(target, tickDelta));
        float dYaw = MathHelper.wrapDegrees(want[0] - player.getYaw());
        float dPitch = vertical.get() ? want[1] - player.getPitch() : 0;

        // Exponential smoothing: covers (1 - e^-speed*dt) of the remaining angle this frame
        double f = 1.0 - Math.exp(-speed.get() * dt);
        double stepYaw = dYaw * f, stepPitch = dPitch * f;
        // changeLookDirection multiplies by 0.15 (degrees per cursor unit), so divide it back out
        player.changeLookDirection(stepYaw / 0.15, stepPitch / 0.15);
    }

    /** Aims at the upper body, clamped into the hitbox, which looks natural. */
    private Vec3d aimPoint(LivingEntity e, float tickDelta) {
        Vec3d base = e.getLerpedPos(tickDelta);
        return base.add(0, e.getHeight() * 0.7, 0);
    }

    private LivingEntity findTarget(float tickDelta) {
        Comparator<LivingEntity> order = switch (priority.get()) {
            case DISTANCE -> Comparator.comparingDouble(e -> e.distanceTo(mc.player));
            case HEALTH -> Comparator.comparingDouble(LivingEntity::getHealth);
            case ANGLE -> Comparator.comparingDouble(e -> angleTo(e, tickDelta));
        };
        LivingEntity best = null;
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity e) || !isValid(e)) continue;
            if (angleTo(e, tickDelta) > fov.get() / 2) continue;
            if (best == null || order.compare(e, best) < 0) best = e;
        }
        return best;
    }

    private boolean isValid(LivingEntity e) {
        if (e == mc.player || !e.isAlive() || e instanceof ArmorStandEntity) return false;
        if (playersOnly.get() && !(e instanceof PlayerEntity)) return false;
        if (e instanceof PlayerEntity p && p.isSpectator()) return false;
        if (ignoreFriends.get() && Sentinel.friends().isFriend(e)) return false;
        if (ignoreInvisible.get() && e.isInvisible()) return false;
        return mc.player.distanceTo(e) <= range.get();
    }

    /** Angular distance between the crosshair and the target in degrees. */
    private double angleTo(LivingEntity e, float tickDelta) {
        float[] want = WorldUtil.anglesTo(mc.player.getEyePos(), aimPoint(e, tickDelta));
        double dy = MathHelper.wrapDegrees(want[0] - mc.player.getYaw());
        double dp = want[1] - mc.player.getPitch();
        return Math.sqrt(dy * dy + dp * dp);
    }
}
