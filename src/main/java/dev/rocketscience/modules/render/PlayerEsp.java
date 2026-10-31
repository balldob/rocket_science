package dev.rocketscience.modules.render;

import dev.rocketscience.RocketScience;
import dev.rocketscience.gui.Theme;
import dev.rocketscience.module.Category;
import dev.rocketscience.module.Module;
import dev.rocketscience.render.FontRenderer;
import dev.rocketscience.render.Projection;
import dev.rocketscience.render.Render2D;
import dev.rocketscience.render.Render3D;
import dev.rocketscience.setting.*;
import dev.rocketscience.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Highlights other players through walls.
 * <ul>
 *   <li>BOX - 3D wireframe box (+ faint fill)</li>
 *   <li>OUTLINE - 2D screen-space rectangle around the player</li>
 *   <li>GLOW - vanilla entity outline shader, recoloured (see EntityMixin / MinecraftClientMixin)</li>
 * </ul>
 * Optional crosshair tracers and nametags with health and distance. Friends are blue, others red,
 * or colour by distance (red = close, blue = far).
 */
public class PlayerEsp extends Module {
    public enum Mode { BOX, OUTLINE, GLOW }

    public enum ColorMode { FRIEND_STATUS, DISTANCE }

    private final EnumSetting<Mode> mode = enumSetting("Mode", "How players are highlighted", Mode.BOX);
    private final EnumSetting<ColorMode> colorMode = enumSetting("Color Mode", "Friend status or distance based colours", ColorMode.FRIEND_STATUS);
    private final ColorSetting friendColor = color("Friend Color", "Colour for friends", Theme.BLUE);
    private final ColorSetting enemyColor = color("Enemy Color", "Colour for everyone else", Theme.RED);
    private final BoolSetting tracers = bool("Tracers", "Lines from the crosshair to players", true);
    private final BoolSetting nametags = bool("Nametags", "Name, health and distance above players", true);
    private final BoolSetting showHealth = bool("Health", "Show health in nametags", true).visibleWhen(nametags::get);
    private final BoolSetting showDistance = bool("Distance", "Show distance in nametags", true).visibleWhen(nametags::get);
    private final IntSetting range = intSetting("Range", "Maximum distance in blocks", 128, 8, 512);
    private final DoubleSetting lineWidth = doubleSetting("Line Width", "Thickness of boxes and tracers", 1.5, 0.5, 5.0, 0.5);

    public PlayerEsp() {
        super("Player ESP", "See players through walls", Category.RENDER);
    }

    @Override
    public String getInfo() {
        return mode.displayValue();
    }

    /** Players we should draw this frame. */
    private Iterable<? extends PlayerEntity> targets() {
        return mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && !p.isSpectator() && p.distanceTo(mc.player) <= range.get())
                .toList();
    }

    /** Colour for a given player based on settings. */
    public int colorFor(PlayerEntity p) {
        if (RocketScience.friends().isFriend(p)) return friendColor.get();
        if (colorMode.get() == ColorMode.FRIEND_STATUS) return enemyColor.get();
        double t = MathHelper.clamp(p.distanceTo(mc.player) / 64.0, 0, 1);
        return ColorUtil.lerp(enemyColor.get(), friendColor.get(), t);
    }

    @Override
    public void onRender3D(float tickDelta) {
        if (mc.world == null || mc.player == null) return;
        float lw = lineWidth.get().floatValue();
        for (PlayerEntity p : targets()) {
            int c = colorFor(p);
            Box box = lerpedBox(p, tickDelta);
            if (mode.get() == Mode.BOX) Render3D.box(box, ColorUtil.withAlpha(c, 0.12), c, lw);
            if (tracers.get()) Render3D.tracer(box.getCenter(), ColorUtil.withAlpha(c, 0.85), lw);
        }
    }

    @Override
    public void onRender2D(DrawContext ctx, float tickDelta) {
        if (mc.world == null || mc.player == null || mc.options.hudHidden) return;
        for (PlayerEntity p : targets()) {
            int c = colorFor(p);
            Box box = lerpedBox(p, tickDelta);
            if (mode.get() == Mode.OUTLINE) drawOutline(ctx, box, c);
            if (nametags.get()) drawNametag(ctx, p, box, c);
        }
    }

    /** Projects the 8 box corners and draws their 2D bounding rectangle. */
    private void drawOutline(DrawContext ctx, Box b, int color) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            Vec3d corner = new Vec3d((i & 1) == 0 ? b.minX : b.maxX, (i & 2) == 0 ? b.minY : b.maxY, (i & 4) == 0 ? b.minZ : b.maxZ);
            double[] s = Projection.toScreen(corner);
            if (s == null) return;
            minX = Math.min(minX, s[0]);
            minY = Math.min(minY, s[1]);
            maxX = Math.max(maxX, s[0]);
            maxY = Math.max(maxY, s[1]);
        }
        int x = (int) minX, y = (int) minY, w = (int) (maxX - minX), h = (int) (maxY - minY);
        Render2D.border(ctx, x - 1, y - 1, w + 2, h + 2, 0xAA000000);
        Render2D.border(ctx, x, y, w, h, color);
        Render2D.border(ctx, x + 1, y + 1, w - 2, h - 2, 0xAA000000);
    }

    private void drawNametag(DrawContext ctx, PlayerEntity p, Box box, int color) {
        double[] s = Projection.toScreen(new Vec3d((box.minX + box.maxX) / 2, box.maxY + 0.45, (box.minZ + box.maxZ) / 2));
        if (s == null) return;
        String name = p.getGameProfile().getName();
        String hp = "";
        if (showHealth.get()) hp = String.format(" %.1f", p.getHealth() + p.getAbsorptionAmount());
        String dist = showDistance.get() ? String.format(" %.0fm", p.distanceTo(mc.player)) : "";
        float scale = 0.85f;
        float w = FontRenderer.width(name + hp + dist, scale) + 8;
        int x = (int) (s[0] - w / 2), y = (int) s[1] - 12;
        Render2D.roundedRect(ctx, x, y, (int) w, 11, 3, ColorUtil.withAlpha(Theme.BG, 0.75));
        ctx.fill(x + 2, y + 10, x + (int) w - 2, y + 11, color);
        float tx = x + 4;
        FontRenderer.draw(ctx, name, tx, y + 2, Theme.TEXT, scale);
        tx += FontRenderer.width(name, scale);
        if (!hp.isEmpty()) {
            float ratio = (p.getHealth() + p.getAbsorptionAmount()) / Math.max(1f, p.getMaxHealth());
            FontRenderer.draw(ctx, hp, tx, y + 2, ColorUtil.lerp(Theme.RED, 0xFF4CD964, ratio), scale);
            tx += FontRenderer.width(hp, scale);
        }
        if (!dist.isEmpty()) FontRenderer.draw(ctx, dist, tx, y + 2, Theme.MUTED, scale);
    }

    private static Box lerpedBox(Entity e, float tickDelta) {
        Vec3d lerped = e.getLerpedPos(tickDelta);
        return e.getBoundingBox().offset(lerped.subtract(e.getPos()));
    }

    /**
     * Glow colour used by {@code EntityMixin#getTeamColorValue}; null when this entity should not
     * be affected.
     */
    public Integer glowColor(Entity entity) {
        if (!isEnabled() || mode.get() != Mode.GLOW || !(entity instanceof PlayerEntity p) || p == mc.player) return null;
        if (mc.player == null || p.distanceTo(mc.player) > range.get()) return null;
        return colorFor(p) & 0xFFFFFF;
    }

    /** Whether vanilla should draw the outline shader for this entity. */
    public boolean shouldGlow(Entity entity) {
        return glowColor(entity) != null;
    }
}
