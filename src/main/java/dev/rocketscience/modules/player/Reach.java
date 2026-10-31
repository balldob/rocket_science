package dev.rocketscience.modules.player;

import dev.rocketscience.module.Category;
import dev.rocketscience.module.Module;
import dev.rocketscience.setting.DoubleSetting;

/**
 * Raises how far you can hit entities and break/place blocks. Applied through
 * {@code PlayerEntityMixin} on the interaction-range getters, which vanilla uses for both
 * crosshair targeting and the actual interaction.
 * <p>
 * Vanilla defaults are 3.0 (entities) and 4.5 (blocks). Servers check distance themselves, so
 * hits far beyond vanilla range are usually rejected or flagged.
 */
public class Reach extends Module {
    private final DoubleSetting entityReach = doubleSetting("Entity Reach", "Attack/interact range for entities", 3.5, 3.0, 6.0, 0.1);
    private final DoubleSetting blockReach = doubleSetting("Block Reach", "Break/place range for blocks", 5.0, 4.5, 7.0, 0.1);

    public Reach() {
        super("Reach", "Extend entity and block reach", Category.PLAYER);
    }

    @Override
    public String getInfo() {
        return entityReach.displayValue();
    }

    /** Returns the boosted range, never lower than the vanilla/attribute value. */
    public double entityRange(double vanilla) {
        return Math.max(vanilla, entityReach.get());
    }

    public double blockRange(double vanilla) {
        return Math.max(vanilla, blockReach.get());
    }
}
