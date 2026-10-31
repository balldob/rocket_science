package dev.sentinel.gui;

/**
 * "Command Panel" palette and shared metrics. Every widget pulls its colours from here, so the
 * whole client can be re-themed in one place.
 */
public final class Theme {
    private Theme() {
    }

    // Core palette
    /** Deep navy / near black background. */
    public static final int BG = 0xFF0B1020;
    /** Electric blue primary accent. */
    public static final int BLUE = 0xFF2F80FF;
    /** Crimson secondary accent (combat, warnings, "off"). */
    public static final int RED = 0xFFE63946;
    /** Soft white text. */
    public static final int TEXT = 0xFFE8ECF4;
    /** Muted grey-blue for inactive elements. */
    public static final int MUTED = 0xFF6B7894;

    // Derived surfaces (slightly translucent)
    public static final int PANEL = 0xE60B1020;
    public static final int HEADER = 0xF2111A33;
    public static final int SURFACE = 0xFF141D38;
    public static final int TRACK = 0xFF1C2747;
    public static final int BORDER = 0x662F80FF;
    public static final int BORDER_DIM = 0x33FFFFFF;
    public static final int SHADOW = 0x99000000;

    // Metrics
    public static final int RADIUS = 5;
    public static final int HEADER_HEIGHT = 18;
    public static final int ROW_HEIGHT = 14;
    public static final float TEXT_SCALE = 1.0f;
    public static final float SMALL_SCALE = 0.85f;
}
