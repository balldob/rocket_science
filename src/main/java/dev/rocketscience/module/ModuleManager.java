package dev.rocketscience.module;

import dev.rocketscience.RocketScience;
import dev.rocketscience.modules.combat.AimAssist;
import dev.rocketscience.modules.misc.ClickGuiModule;
import dev.rocketscience.modules.misc.Hud;
import dev.rocketscience.modules.misc.Performance;
import dev.rocketscience.modules.player.AutoSprint;
import dev.rocketscience.modules.player.Reach;
import dev.rocketscience.modules.render.FreeCam;
import dev.rocketscience.modules.render.PlayerEsp;
import dev.rocketscience.modules.render.Tracers;
import dev.rocketscience.modules.world.BaseFinder;
import dev.rocketscience.modules.world.PlayerFinder;
import dev.rocketscience.modules.world.StorageEsp;
import net.minecraft.client.gui.DrawContext;

import java.util.*;
import java.util.function.Consumer;

/**
 * Owns every module instance. Registers them, looks them up, dispatches lifecycle hooks and
 * handles keybind toggles.
 */
public final class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    private final Map<Class<? extends Module>, Module> byClass = new HashMap<>();

    /** Registers all built-in modules. Add new modules here. */
    public void init() {
        // World / detection
        register(new BaseFinder());
        register(new PlayerFinder());
        register(new StorageEsp());
        // Render
        register(new PlayerEsp());
        register(new FreeCam());
        register(new Tracers());
        // Combat
        register(new AimAssist());
        // Player
        register(new AutoSprint());
        register(new Reach());
        // Misc
        register(new Hud());
        register(new Performance());
        register(new ClickGuiModule());

        modules.sort(Comparator.comparing(Module::getName));
    }

    public void register(Module module) {
        modules.add(module);
        byClass.put(module.getClass(), module);
    }

    /** Typed lookup, e.g. {@code get(FreeCam.class)}. */
    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) {
        return (T) byClass.get(type);
    }

    /** Case and space insensitive lookup by name. */
    public Module getByName(String name) {
        String id = name.replace(" ", "").toLowerCase(Locale.ROOT);
        for (Module m : modules) if (m.getId().equals(id)) return m;
        return null;
    }

    public List<Module> getAll() {
        return Collections.unmodifiableList(modules);
    }

    public List<Module> getByCategory(Category category) {
        List<Module> list = new ArrayList<>();
        for (Module m : modules) if (m.getCategory() == category) list.add(m);
        return list;
    }

    /* ------------------------------------------------------------------ */
    /*  Dispatch                                                          */
    /* ------------------------------------------------------------------ */

    /** Toggles every module bound to {@code key}. Called from the keyboard mixin. */
    public void onKeyPressed(int key) {
        for (Module m : modules) {
            if (m.getBind().get() == key) safely(m, Module::toggle);
        }
    }

    public void onPreTick() {
        forEnabled(Module::onPreTick);
    }

    public void onTick() {
        forEnabled(Module::onTick);
    }

    public void onRender3D(float tickDelta) {
        forEnabled(m -> m.onRender3D(tickDelta));
    }

    public void onRender2D(DrawContext ctx, float tickDelta) {
        forEnabled(m -> m.onRender2D(ctx, tickDelta));
    }

    public void onWorldLeave() {
        for (Module m : modules) safely(m, Module::onWorldLeave);
    }

    private void forEnabled(Consumer<Module> action) {
        for (Module m : modules) {
            if (m.isEnabled()) safely(m, action);
        }
    }

    /** Runs a hook, logging (and disabling the module) instead of crashing the game on error. */
    private void safely(Module m, Consumer<Module> action) {
        try {
            action.accept(m);
        } catch (Throwable t) {
            RocketScience.LOG.error("Module '{}' threw an exception and was disabled", m.getName(), t);
            m.setEnabled(false, false);
        }
    }
}
