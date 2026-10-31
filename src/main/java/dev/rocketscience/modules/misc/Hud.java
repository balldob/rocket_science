package dev.rocketscience.modules.misc;

import dev.rocketscience.hud.Notifications;
import dev.rocketscience.module.Category;
import dev.rocketscience.module.Module;
import dev.rocketscience.setting.BoolSetting;
import dev.rocketscience.setting.DoubleSetting;

/** HUD overlay settings. Drawing happens in {@link dev.rocketscience.hud.HudRenderer}. */
public class Hud extends Module {
    public final BoolSetting watermark = bool("Watermark", "Client name top-left", true);
    public final BoolSetting arrayList = bool("ArrayList", "Active modules top-right", true);
    public final BoolSetting coords = bool("Coordinates", "Position (and nether/overworld conversion)", true);
    public final BoolSetting fps = bool("FPS", "Frames per second", true);
    public final BoolSetting facing = bool("Facing", "Cardinal direction", true);
    public final BoolSetting toasts = bool("Notifications", "Show toast notifications", true);
    public final BoolSetting toggleToasts = bool("Toggle Toasts", "Toast when a module is toggled", true)
            .onChange(v -> Notifications.toggleToasts = v);
    public final DoubleSetting textScale = doubleSetting("Text Scale", "HUD text size", 1.0, 0.5, 2.0, 0.05);

    public Hud() {
        super("HUD", "Watermark, module list, info lines and toasts", Category.MISC);
        hideFromArrayList();
        setEnabled(true, false);
    }
}
