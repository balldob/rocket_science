package dev.rocketscience.modules.render;

import dev.rocketscience.module.Category;
import dev.rocketscience.module.Module;
import dev.rocketscience.setting.BoolSetting;
import dev.rocketscience.setting.DoubleSetting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Detaches the camera for free flight while the player stays put server-side.
 * <p>
 * How it works (three small mixins):
 * <ul>
 *   <li>{@code KeyboardInputMixin} zeroes the player's movement input, so no movement packets
 *       other than "standing still" are sent;</li>
 *   <li>{@code EntityMixin} redirects mouse look to {@link #rotate};</li>
 *   <li>{@code CameraMixin} overrides the camera position/rotation and marks it third-person so
 *       your own body is rendered.</li>
 * </ul>
 * Nothing is permanently changed, so disabling restores the normal camera on the next frame.
 */
public class FreeCam extends Module {
    private final DoubleSetting speed = doubleSetting("Speed", "Flight speed in blocks/tick", 1.0, 0.1, 5.0, 0.1);
    private final BoolSetting interact = bool("Interact", "Allow the (stationary) player to still attack/use items", false);

    private Vec3d pos;
    private Vec3d prevPos;
    private Vec3d velocity = Vec3d.ZERO;
    private float yaw;
    private float pitch;

    public FreeCam() {
        super("Free Cam", "Fly the camera freely while your player stays still", Category.RENDER);
    }

    /** True once the camera has been initialised (i.e. there is a world). */
    public boolean isActive() {
        return isEnabled() && pos != null;
    }

    @Override
    protected void onEnable() {
        initialise();
    }

    private void initialise() {
        if (mc.player == null) return;
        pos = prevPos = mc.gameRenderer.getCamera().getPos();
        velocity = Vec3d.ZERO;
        yaw = mc.player.getYaw();
        pitch = mc.player.getPitch();
    }

    @Override
    protected void onDisable() {
        pos = prevPos = null;
    }

    @Override
    public void onWorldLeave() {
        if (isEnabled()) setEnabled(false, false);
    }

    /** Runs before input is handled: swallow attack/use presses unless interaction is allowed. */
    @Override
    public void onPreTick() {
        if (pos == null || interact.get()) return;
        var o = mc.options;
        while (o.attackKey.wasPressed()) { /* drain */ }
        while (o.useKey.wasPressed()) { /* drain */ }
        o.attackKey.setPressed(false);
        o.useKey.setPressed(false);
    }

    @Override
    public void onTick() {
        if (pos == null) {
            initialise();
            return;
        }
        prevPos = pos;
        var o = mc.options;
        double forward = (o.forwardKey.isPressed() ? 1 : 0) - (o.backKey.isPressed() ? 1 : 0);
        double strafe = (o.rightKey.isPressed() ? 1 : 0) - (o.leftKey.isPressed() ? 1 : 0);
        double vertical = (o.jumpKey.isPressed() ? 1 : 0) - (o.sneakKey.isPressed() ? 1 : 0);

        Vec3d look = Vec3d.fromPolar(0, yaw);
        Vec3d right = Vec3d.fromPolar(0, yaw + 90);
        Vec3d wish = look.multiply(forward).add(right.multiply(strafe)).add(0, vertical, 0);
        if (wish.lengthSquared() > 1e-4) wish = wish.normalize().multiply(speed.get() * (o.sprintKey.isPressed() ? 2 : 1));
        // ease towards the wished velocity for smooth starts/stops
        velocity = velocity.lerp(wish, 0.5);
        pos = pos.add(velocity);
    }

    /** Called by EntityMixin instead of rotating the player. Same 0.15 factor as vanilla. */
    public void rotate(double cursorDeltaX, double cursorDeltaY) {
        yaw += (float) cursorDeltaX * 0.15f;
        pitch = MathHelper.clamp(pitch + (float) cursorDeltaY * 0.15f, -90f, 90f);
    }

    public Vec3d getLerpedPos(float tickDelta) {
        return prevPos.lerp(pos, tickDelta);
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }
}
