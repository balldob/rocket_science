package dev.rocketscience.mixin;

import dev.rocketscience.RocketScience;
import dev.rocketscience.modules.player.Reach;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reach: raise the local player's interaction ranges. Vanilla reads these getters for crosshair
 * targeting (GameRenderer) and for attacking/breaking, so one hook covers both.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "getEntityInteractionRange", at = @At("RETURN"), cancellable = true)
    private void rocketscience$entityReach(CallbackInfoReturnable<Double> cir) {
        Reach reach = activeReach();
        if (reach != null) cir.setReturnValue(reach.entityRange(cir.getReturnValue()));
    }

    @Inject(method = "getBlockInteractionRange", at = @At("RETURN"), cancellable = true)
    private void rocketscience$blockReach(CallbackInfoReturnable<Double> cir) {
        Reach reach = activeReach();
        if (reach != null) cir.setReturnValue(reach.blockRange(cir.getReturnValue()));
    }

    /** The Reach module if it applies to this entity (only the local player, only when on). */
    private Reach activeReach() {
        if (RocketScience.modules() == null || (Object) this != MinecraftClient.getInstance().player) return null;
        Reach reach = RocketScience.modules().get(Reach.class);
        return reach != null && reach.isEnabled() ? reach : null;
    }
}
