package dev.rocketscience.mixin;

import dev.rocketscience.modules.misc.Performance;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Performance: skip chests, signs, banners etc. that are hidden behind walls. Optional
 * ({@code require = 0}) for the same reason as the entity mixin.
 */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Inject(method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void rocketscience$cullHidden(BlockEntity blockEntity, float tickDelta, MatrixStack matrices,
                                          VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (Performance.isCulled(blockEntity)) ci.cancel();
    }
}
