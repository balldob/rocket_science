package dev.rocketscience.mixin;

import dev.rocketscience.RocketScience;
import dev.rocketscience.modules.render.PlayerEsp;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Player ESP (glow mode): tell the renderer to draw the outline shader for targeted players. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void rocketscience$espGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (RocketScience.modules() == null) return;
        PlayerEsp esp = RocketScience.modules().get(PlayerEsp.class);
        if (esp != null && esp.shouldGlow(entity)) cir.setReturnValue(true);
    }
}
