package dev.sentinel.modules.player;

import dev.sentinel.module.Category;
import dev.sentinel.module.Module;

/**
 * Minimal example module (see README "Adding a module"): keeps you sprinting while moving forward.
 */
public class AutoSprint extends Module {
    public AutoSprint() {
        super("Auto Sprint", "Sprint automatically when walking forward", Category.PLAYER);
    }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null) return;
        if (mc.options.forwardKey.isPressed() && !p.isSneaking() && !p.horizontalCollision && p.getHungerManager().getFoodLevel() > 6) {
            p.setSprinting(true);
        }
    }
}
