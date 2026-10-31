package dev.sentinel.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** Remembers ClickGUI panel positions and collapsed state; saved inside each profile. */
public final class GuiState {
    /** Position/expanded state of one panel. */
    public static final class PanelState {
        public int x;
        public int y;
        public boolean expanded = true;

        PanelState(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final Map<String, PanelState> PANELS = new LinkedHashMap<>();

    private GuiState() {
    }

    /** Returns the stored state for a panel, creating it at the default position if missing. */
    public static PanelState get(String id, int defaultX, int defaultY) {
        return PANELS.computeIfAbsent(id, k -> new PanelState(defaultX, defaultY));
    }

    public static JsonObject toJson() {
        JsonObject root = new JsonObject();
        PANELS.forEach((id, s) -> {
            JsonObject o = new JsonObject();
            o.addProperty("x", s.x);
            o.addProperty("y", s.y);
            o.addProperty("expanded", s.expanded);
            root.add(id, o);
        });
        return root;
    }

    public static void fromJson(JsonObject root) {
        for (Map.Entry<String, JsonElement> e : root.entrySet()) {
            if (!e.getValue().isJsonObject()) continue;
            JsonObject o = e.getValue().getAsJsonObject();
            PanelState s = new PanelState(o.has("x") ? o.get("x").getAsInt() : 10, o.has("y") ? o.get("y").getAsInt() : 30);
            if (o.has("expanded")) s.expanded = o.get("expanded").getAsBoolean();
            PANELS.put(e.getKey(), s);
        }
    }
}
