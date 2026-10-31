package dev.sentinel.module;

/** Module categories; each one becomes a draggable panel in the ClickGUI. */
public enum Category {
    RENDER("Render"),
    COMBAT("Combat"),
    WORLD("World"),
    PLAYER("Player"),
    MISC("Misc");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
