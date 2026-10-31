package dev.rocketscience.mixin;

import dev.rocketscience.RocketScience;
import dev.rocketscience.modules.render.FreeCam;
import dev.rocketscience.modules.render.PlayerEsp;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Two tiny client-side hooks on Entity. */
@Mixin(Entity.class)
public abstract class EntityMixin {
    /** Free Cam: mouse movement rotates the free camera instead of the local player. */
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void rocketscience$freecamLook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if ((Object) this != MinecraftClient.getInstance().player) return;
        FreeCam freeCam = RocketScience.modules().get(FreeCam.class);
        if (freeCam != null && freeCam.isActive()) {
            freeCam.rotate(cursorDeltaX, cursorDeltaY);
            ci.cancel();
        }
    }

    /** Player ESP (glow mode): recolour the vanilla outline - friends blue, others red. */
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void rocketscience$espGlowColor(CallbackInfoReturnable<Integer> cir) {
        PlayerEsp esp = RocketScience.modules() == null ? null : RocketScience.modules().get(PlayerEsp.class);
        if (esp == null) return;
        Integer color = esp.glowColor((Entity) (Object) this);
        if (color != null) cir.setReturnValue(color);
    }
}
