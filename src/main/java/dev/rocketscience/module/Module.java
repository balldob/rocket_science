package dev.rocketscience.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.rocketscience.hud.Notifications;
import dev.rocketscience.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Base class for all modules.
 * <p>
 * A module has a name, category, an enabled flag, a keybind and a list of settings. Subclasses
 * override only the hooks they need ({@link #onTick()}, {@link #onRender3D(float)}, ...). The
 * {@link ModuleManager} dispatches hooks only to enabled modules, wraps them in error handling
 * and takes care of keybinds, so a new module is just a class plus one {@code register} line.
 */
public abstract class Module {
    protected final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    /** The bind is a normal setting so it is saved and shown in the GUI like everything else. */
    private final KeybindSetting bind;

    private boolean enabled;
    /** Whether the module appears in the HUD ArrayList. */
    private boolean shownInArrayList = true;

    protected Module(String name, String description, Category category) {
        this(name, description, category, KeybindSetting.NONE);
    }

    protected Module(String name, String description, Category category, int defaultKey) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.bind = new KeybindSetting("Bind", "Key that toggles this module", defaultKey);
    }

    /* ------------------------------------------------------------------ */
    /*  Lifecycle hooks - override what you need                          */
    /* ------------------------------------------------------------------ */

    /** Called when the module is switched on. */
    protected void onEnable() {
    }

    /** Called when the module is switched off. */
    protected void onDisable() {
    }

    /** Called at the start of every client tick (before input is processed). */
    public void onPreTick() {
    }

    /** Called at the end of every client tick (20/s). */
    public void onTick() {
    }

    /** Called once per frame while the world renders; queue shapes via {@code Render3D}. */
    public void onRender3D(float tickDelta) {
    }

    /** Called once per frame while the HUD renders. */
    public void onRender2D(DrawContext ctx, float tickDelta) {
    }

    /** Called when leaving a world/server. Clear per-world caches here. */
    public void onWorldLeave() {
    }

    /** Optional short suffix shown next to the name in the ArrayList (e.g. current mode). */
    public String getInfo() {
        return null;
    }

    /* ------------------------------------------------------------------ */
    /*  State                                                             */
    /* ------------------------------------------------------------------ */

    public void toggle() {
        setEnabled(!enabled, true);
    }

    public void setEnabled(boolean enabled) {
        setEnabled(enabled, true);
    }

    /**
     * Changes the enabled state, running the enable/disable hooks.
     *
     * @param notify whether to show a toggle toast (false when loading configs)
     */
    public void setEnabled(boolean enabled, boolean notify) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        try {
            if (enabled) onEnable();
            else onDisable();
        } catch (Throwable t) {
            dev.rocketscience.RocketScience.LOG.error("Module {} failed to {}", name, enabled ? "enable" : "disable", t);
        }
        if (notify && this.enabled == enabled) Notifications.moduleToggled(this);
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Combat modules get red accents in the GUI. */
    public boolean isCombat() {
        return category == Category.COMBAT;
    }

    public String getName() {
        return name;
    }

    /** Lower-case name without spaces, used for command lookup ("Base Finder" -> "basefinder"). */
    public String getId() {
        return name.replace(" ", "").toLowerCase(Locale.ROOT);
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public KeybindSetting getBind() {
        return bind;
    }

    public boolean isShownInArrayList() {
        return shownInArrayList;
    }

    protected void hideFromArrayList() {
        this.shownInArrayList = false;
    }

    /** All settings, with the keybind last. */
    public List<Setting<?>> getSettings() {
        List<Setting<?>> all = new ArrayList<>(settings);
        all.add(bind);
        return Collections.unmodifiableList(all);
    }

    public Setting<?> getSetting(String name) {
        String wanted = name.replace(" ", "").toLowerCase(Locale.ROOT);
        for (Setting<?> s : getSettings()) {
            if (s.getName().replace(" ", "").toLowerCase(Locale.ROOT).equals(wanted)) return s;
        }
        return null;
    }

    /* ------------------------------------------------------------------ */
    /*  Setting helpers for subclasses                                    */
    /* ------------------------------------------------------------------ */

    protected <S extends Setting<?>> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    protected BoolSetting bool(String name, String desc, boolean def) {
        return add(new BoolSetting(name, desc, def));
    }

    protected IntSetting intSetting(String name, String desc, int def, int min, int max) {
        return add(new IntSetting(name, desc, def, min, max));
    }

    protected DoubleSetting doubleSetting(String name, String desc, double def, double min, double max, double step) {
        return add(new DoubleSetting(name, desc, def, min, max, step));
    }

    protected ColorSetting color(String name, String desc, int argb) {
        return add(new ColorSetting(name, desc, argb));
    }

    protected <E extends Enum<E>> EnumSetting<E> enumSetting(String name, String desc, E def) {
        return add(new EnumSetting<>(name, desc, def));
    }

    protected StringListSetting stringList(String name, String desc, List<String> def, Predicate<String> validator) {
        return add(new StringListSetting(name, desc, def, validator));
    }

    /* ------------------------------------------------------------------ */
    /*  Serialisation                                                     */
    /* ------------------------------------------------------------------ */

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("enabled", enabled);
        JsonObject s = new JsonObject();
        for (Setting<?> setting : getSettings()) s.add(setting.getName(), setting.toJson());
        obj.add("settings", s);
        return obj;
    }

    public void fromJson(JsonObject obj) {
        JsonElement s = obj.get("settings");
        if (s != null && s.isJsonObject()) {
            for (Setting<?> setting : getSettings()) {
                JsonElement v = s.getAsJsonObject().get(setting.getName());
                if (v != null) setting.fromJson(v);
            }
        }
        if (obj.has("enabled")) setEnabled(obj.get("enabled").getAsBoolean(), false);
    }
}
