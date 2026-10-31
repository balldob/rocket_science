package dev.rocketscience.gui;

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

    /** Stored state for a panel, or null if it has never been placed (or the layout was reset). */
    public static PanelState find(String id) {
        return PANELS.get(id);
    }

    /** Creates (or replaces) the state for a panel at the given position. */
    public static PanelState place(String id, int x, int y) {
        PanelState old = PANELS.get(id);
        PanelState s = new PanelState(x, y);
        if (old != null) s.expanded = old.expanded;
        PANELS.put(id, s);
        return s;
    }

    /** Forgets all positions; panels are re-flowed to fit the screen next time the GUI lays out. */
    public static void reset() {
        PANELS.clear();
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
