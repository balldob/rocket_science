package dev.sentinel.mixin;

import dev.sentinel.Sentinel;
import dev.sentinel.modules.render.PlayerEsp;
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
    private void sentinel$espGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (Sentinel.modules() == null) return;
        PlayerEsp esp = Sentinel.modules().get(PlayerEsp.class);
        if (esp != null && esp.shouldGlow(entity)) cir.setReturnValue(true);
    }
}
