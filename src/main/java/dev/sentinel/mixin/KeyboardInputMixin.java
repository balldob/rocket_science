package dev.sentinel.mixin;

import dev.sentinel.Sentinel;
import dev.sentinel.modules.render.FreeCam;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Free Cam: after vanilla reads WASD/jump/sneak into the player's input, wipe it so the player
 * stays still. Free Cam reads the key bindings itself to move the camera.
 * The handler takes only the CallbackInfo, so it matches {@code tick()} regardless of its
 * parameter list.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void sentinel$freezeForFreecam(CallbackInfo ci) {
        FreeCam freeCam = Sentinel.modules().get(FreeCam.class);
        if (freeCam == null || !freeCam.isActive()) return;
        Input self = (Input) (Object) this;
        self.playerInput = PlayerInput.DEFAULT;
        self.movementForward = 0f;
        self.movementSideways = 0f;
    }
}
