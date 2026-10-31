package dev.sentinel.modules.world;

import dev.sentinel.module.Category;
import dev.sentinel.module.Module;
import dev.sentinel.render.Render3D;
import dev.sentinel.setting.*;
import dev.sentinel.util.ColorUtil;
import dev.sentinel.util.WorldUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Highlights containers through walls with boxes and/or tracers, colour coded per type.
 * The container list is rebuilt from loaded chunks every few ticks rather than every frame.
 */
public class StorageEsp extends Module {
    public enum Render { BOX, TRACER, BOTH }

    public enum Style { FILL, OUTLINE, BOTH }

    /** One container type with its own toggle and colour. */
    private final class Type {
        final BoolSetting enabled;
        final ColorSetting color;

        Type(String name, int argb) {
            enabled = bool(name, "Highlight " + name.toLowerCase(), true);
            color = color(name + " Color", "Colour for " + name.toLowerCase(), argb);
            color.visibleWhen(enabled::get);
        }
    }

    private record Target(Box box, int color) {
    }

    private final EnumSetting<Render> render = enumSetting("Render", "Boxes, tracers or both", Render.BOX);
    private final EnumSetting<Style> style = enumSetting("Style", "Fill, outline or both", Style.BOTH);
    private final DoubleSetting opacity = doubleSetting("Fill Opacity", "Fill transparency", 0.25, 0.0, 1.0, 0.05);
    private final IntSetting range = intSetting("Range", "Maximum distance in blocks", 96, 8, 256);
    private final DoubleSetting lineWidth = doubleSetting("Line Width", "Outline/tracer thickness", 1.5, 0.5, 5.0, 0.5);

    private final Type chest = new Type("Chest", 0xFFF2A93B);
    private final Type trapped = new Type("Trapped Chest", 0xFFE63946);
    private final Type barrel = new Type("Barrel", 0xFFB5793A);
    private final Type shulker = new Type("Shulker", 0xFFB06BFF);
    private final Type ender = new Type("Ender Chest", 0xFF2F80FF);
    private final Type hopper = new Type("Hopper", 0xFF8A94A6);
    private final Type furnace = new Type("Furnace", 0xFFE8ECF4);

    private final List<Target> targets = new ArrayList<>();
    private int timer;

    public StorageEsp() {
        super("Storage ESP", "Highlight containers through walls", Category.WORLD);
    }

    @Override
    public String getInfo() {
        return String.valueOf(targets.size());
    }

    @Override
    protected void onDisable() {
        targets.clear();
    }

    @Override
    public void onWorldLeave() {
        targets.clear();
    }

    /** Maps a block entity to its type (order matters: trapped chests are chests too). */
    private Type typeOf(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) return trapped;
        if (be instanceof ChestBlockEntity) return chest;
        if (be instanceof BarrelBlockEntity) return barrel;
        if (be instanceof ShulkerBoxBlockEntity) return shulker;
        if (be instanceof EnderChestBlockEntity) return ender;
        if (be instanceof HopperBlockEntity) return hopper;
        if (be instanceof AbstractFurnaceBlockEntity) return furnace;
        return null;
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null || timer-- > 0) return;
        timer = 10; // rescan twice a second
        targets.clear();
        double maxSq = range.get() * (double) range.get();
        WorldUtil.forEachLoadedChunk(chunk -> {
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                Type t = typeOf(be);
                if (t == null || !t.enabled.get()) continue;
                BlockPos pos = be.getPos();
                if (mc.player.squaredDistanceTo(pos.toCenterPos()) > maxSq) continue;
                targets.add(new Target(shapeBox(pos, be.getCachedState()), t.color.get()));
            }
        });
    }

    /** Uses the block's real outline (chests are smaller than a full block). */
    private Box shapeBox(BlockPos pos, BlockState state) {
        VoxelShape shape = state.getOutlineShape(mc.world, pos);
        if (shape.isEmpty()) return new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        return shape.getBoundingBox().offset(pos);
    }

    @Override
    public void onRender3D(float tickDelta) {
        float lw = lineWidth.get().floatValue();
        boolean boxes = render.get() != Render.TRACER;
        boolean lines = render.get() != Render.BOX;
        for (Target t : targets) {
            if (boxes) {
                int fill = style.get() != Style.OUTLINE ? ColorUtil.withAlpha(t.color, opacity.get() * ColorUtil.alpha(t.color) / 255.0) : 0;
                int outline = style.get() != Style.FILL ? t.color : 0;
                Render3D.box(t.box, fill, outline, lw);
            }
            if (lines) Render3D.tracer(t.box.getCenter(), ColorUtil.withAlpha(t.color, 0.8), lw);
        }
    }
}
