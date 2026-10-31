package dev.rocketscience.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/**
 * Batched world-space shape renderer used by all ESP-style modules.
 * <p>
 * Modules queue boxes/lines during {@code onRender3D}; {@link #flush(Camera)} draws everything
 * once per frame through walls (depth test off) with alpha blending. Vertices are emitted
 * relative to the camera to keep float precision at large coordinates.
 */
public final class Render3D {
    private record BoxJob(Box box, int fill, int outline, float width) {
    }

    private record LineJob(Vec3d from, Vec3d to, int color, float width) {
    }

    private static final List<BoxJob> BOXES = new ArrayList<>();
    private static final List<LineJob> LINES = new ArrayList<>();

    private Render3D() {
    }

    /**
     * Queues a box.
     *
     * @param fill    ARGB fill colour, or 0 for no fill
     * @param outline ARGB outline colour, or 0 for no outline
     */
    public static void box(Box box, int fill, int outline, float lineWidth) {
        BOXES.add(new BoxJob(box, fill, outline, lineWidth));
    }

    /** Queues a line between two world positions. */
    public static void line(Vec3d from, Vec3d to, int color, float lineWidth) {
        LINES.add(new LineJob(from, to, color, lineWidth));
    }

    /** Queues a tracer from the crosshair (camera look direction) to {@code to}. */
    public static void tracer(Vec3d to, int color, float lineWidth) {
        LINES.add(new LineJob(null, to, color, lineWidth));
    }

    /** Draws and clears everything queued this frame. */
    public static void flush(Camera camera) {
        if (BOXES.isEmpty() && LINES.isEmpty()) return;

        Vec3d cam = camera.getPos();
        // View rotation, identical to the matrix vanilla builds in GameRenderer#renderWorld.
        Matrix4f view = new Matrix4f().rotation(camera.getRotation().conjugate(new Quaternionf()));
        // Tracers start one block in front of the camera, which projects onto the crosshair.
        Vec3d look = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());

        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        modelView.mul(view);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        // 1) translucent fills
        BufferBuilder quads = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (BoxJob job : BOXES) {
            if (job.fill != 0) fillBox(quads, job.box.offset(-cam.x, -cam.y, -cam.z), job.fill);
        }
        draw(quads);

        // 2) lines, grouped by width (line width is GL state, not per vertex)
        for (float width : collectWidths()) {
            RenderSystem.lineWidth(width);
            BufferBuilder lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (BoxJob job : BOXES) {
                if (job.outline != 0 && job.width == width) outlineBox(lines, job.box.offset(-cam.x, -cam.y, -cam.z), job.outline);
            }
            for (LineJob job : LINES) {
                if (job.width != width) continue;
                Vec3d a = job.from == null ? look : job.from.subtract(cam);
                Vec3d b = job.to.subtract(cam);
                vertex(lines, a.x, a.y, a.z, job.color);
                vertex(lines, b.x, b.y, b.z, job.color);
            }
            draw(lines);
        }

        RenderSystem.lineWidth(1f);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        modelView.popMatrix();

        BOXES.clear();
        LINES.clear();
    }

    private static List<Float> collectWidths() {
        List<Float> widths = new ArrayList<>();
        for (BoxJob j : BOXES) if (j.outline != 0 && !widths.contains(j.width)) widths.add(j.width);
        for (LineJob j : LINES) if (!widths.contains(j.width)) widths.add(j.width);
        return widths;
    }

    private static void draw(BufferBuilder builder) {
        BuiltBuffer built = builder.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
    }

    private static void vertex(BufferBuilder b, double x, double y, double z, int color) {
        b.vertex((float) x, (float) y, (float) z).color(color);
    }

    private static void fillBox(BufferBuilder b, Box box, int c) {
        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;
        // bottom, top, north, south, west, east
        quad(b, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, c);
        quad(b, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, c);
        quad(b, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, c);
        quad(b, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, c);
        quad(b, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, c);
        quad(b, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, c);
    }

    private static void quad(BufferBuilder b, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, int c) {
        b.vertex(ax, ay, az).color(c);
        b.vertex(bx, by, bz).color(c);
        b.vertex(cx, cy, cz).color(c);
        b.vertex(dx, dy, dz).color(c);
    }

    private static void outlineBox(BufferBuilder b, Box box, int c) {
        double x1 = box.minX, y1 = box.minY, z1 = box.minZ, x2 = box.maxX, y2 = box.maxY, z2 = box.maxZ;
        // bottom rectangle
        edge(b, x1, y1, z1, x2, y1, z1, c);
        edge(b, x2, y1, z1, x2, y1, z2, c);
        edge(b, x2, y1, z2, x1, y1, z2, c);
        edge(b, x1, y1, z2, x1, y1, z1, c);
        // top rectangle
        edge(b, x1, y2, z1, x2, y2, z1, c);
        edge(b, x2, y2, z1, x2, y2, z2, c);
        edge(b, x2, y2, z2, x1, y2, z2, c);
        edge(b, x1, y2, z2, x1, y2, z1, c);
        // verticals
        edge(b, x1, y1, z1, x1, y2, z1, c);
        edge(b, x2, y1, z1, x2, y2, z1, c);
        edge(b, x2, y1, z2, x2, y2, z2, c);
        edge(b, x1, y1, z2, x1, y2, z2, c);
    }

    private static void edge(BufferBuilder b, double x1, double y1, double z1, double x2, double y2, double z2, int c) {
        vertex(b, x1, y1, z1, c);
        vertex(b, x2, y2, z2, c);
    }
}
