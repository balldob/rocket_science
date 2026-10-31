package dev.sentinel.mixin;

import dev.sentinel.Sentinel;
import dev.sentinel.modules.render.FreeCam;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Free Cam: after vanilla positions the camera, move it to the free camera's position and
 * rotation. Setting {@code thirdPerson} makes the world renderer draw your own body.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private boolean thirdPerson;

    @Shadow
    protected abstract void setPos(Vec3d pos);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void sentinel$freecam(BlockView area, Entity focusedEntity, boolean isThirdPerson, boolean inverseView,
                                  float tickDelta, CallbackInfo ci) {
        FreeCam freeCam = Sentinel.modules().get(FreeCam.class);
        if (freeCam == null || !freeCam.isActive()) return;
        setPos(freeCam.getLerpedPos(tickDelta));
        setRotation(freeCam.getYaw(), freeCam.getPitch());
        thirdPerson = true;
    }
}
