package dev.rocketscience.mixin;

import dev.rocketscience.RocketScience;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Module keybinds. Hooking the raw GLFW key callback catches every press precisely (polling once
 * per tick could miss quick taps). Only fires when no screen is open, so typing never toggles.
 */
@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"))
    private void rocketscience$onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (action == GLFW.GLFW_PRESS && key != GLFW.GLFW_KEY_UNKNOWN
                && window == mc.getWindow().getHandle() && mc.currentScreen == null) {
            RocketScience.modules().onKeyPressed(key);
        }
    }
}
