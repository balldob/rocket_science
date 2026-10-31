package dev.rocketscience.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

/**
 * Projects world positions onto the 2D HUD. The matrices are captured every frame during
 * world rendering and used afterwards by HUD code (nametags, 2D boxes, waypoint labels).
 */
public final class Projection {
    private static final Matrix4f VIEW = new Matrix4f();
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static Vec3d cameraPos = Vec3d.ZERO;

    private Projection() {
    }

    /** Captures the current camera; called from the world render event. */
    public static void update(Camera camera, Matrix4f projection) {
        cameraPos = camera.getPos();
        VIEW.rotation(camera.getRotation().conjugate(new Quaternionf()));
        PROJECTION.set(projection);
    }

    public static Vec3d getCameraPos() {
        return cameraPos;
    }

    /**
     * Projects a world position to scaled GUI coordinates.
     *
     * @return {x, y} or {@code null} if the point is behind the camera
     */
    public static double[] toScreen(Vec3d pos) {
        Vector4f v = new Vector4f(
                (float) (pos.x - cameraPos.x),
                (float) (pos.y - cameraPos.y),
                (float) (pos.z - cameraPos.z), 1f);
        v.mul(VIEW).mul(PROJECTION);
        if (v.w <= 0.05f) return null;
        double ndcX = v.x / v.w;
        double ndcY = v.y / v.w;
        var window = MinecraftClient.getInstance().getWindow();
        double x = (ndcX + 1.0) * 0.5 * window.getScaledWidth();
        double y = (1.0 - ndcY) * 0.5 * window.getScaledHeight();
        return new double[]{x, y};
    }
}
