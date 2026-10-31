package dev.rocketscience.setting;

import com.google.gson.JsonElement;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Base class for every configurable module option.
 * <p>
 * A setting owns a typed value plus its default, knows how to serialise itself to/from JSON
 * (for profiles) and how to parse a string (for the {@code .set} command). The ClickGUI picks
 * a matching widget for each concrete subclass, so adding a setting to a module is all that is
 * needed for it to show up in the UI and be saved.
 *
 * @param <T> the value type
 */
public abstract class Setting<T> {
    private final String name;
    private final String description;
    protected final T defaultValue;
    protected T value;

    /** Controls whether the setting is shown in the GUI (e.g. only when another option is on). */
    private BooleanSupplier visibility = () -> true;
    /** Optional listener fired after the value actually changes. */
    private Consumer<T> changeListener;

    protected Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public T get() {
        return value;
    }

    public T getDefault() {
        return defaultValue;
    }

    /** Sets a new value (after {@link #sanitize}) and notifies the listener if it changed. */
    public void set(T newValue) {
        T clean = sanitize(newValue);
        if (!Objects.equals(clean, value)) {
            value = clean;
            if (changeListener != null) changeListener.accept(value);
        }
    }

    /** Hook for subclasses to clamp / normalise incoming values. */
    protected T sanitize(T v) {
        return v;
    }

    public void reset() {
        set(defaultValue);
    }

    public boolean isVisible() {
        return visibility.getAsBoolean();
    }

    /**
     * Only show this setting while {@code condition} is true. Returns {@code this} typed as the
     * caller's variable so it can be chained in field initialisers.
     */
    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleWhen(BooleanSupplier condition) {
        this.visibility = condition;
        return (S) this;
    }

    /** Registers a change listener. */
    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S onChange(Consumer<T> listener) {
        this.changeListener = listener;
        return (S) this;
    }

    /** Human readable value used by commands and some widgets. */
    public String displayValue() {
        return String.valueOf(value);
    }

    /** Serialises the current value. */
    public abstract JsonElement toJson();

    /** Restores a value from JSON; implementations must tolerate malformed input. */
    public abstract void fromJson(JsonElement element);

    /** Parses a user supplied string. Returns {@code false} if the input was invalid. */
    public abstract boolean parse(String input);
}
