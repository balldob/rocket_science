package dev.rocketscience.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * An editable list of strings (used e.g. for Base Finder's block whitelist). Entries are
 * validated with an optional predicate before being accepted.
 */
public class StringListSetting extends Setting<List<String>> {
    private final Predicate<String> validator;

    public StringListSetting(String name, String description, List<String> defaults, Predicate<String> validator) {
        super(name, description, Collections.unmodifiableList(new ArrayList<>(defaults)));
        this.validator = validator == null ? s -> true : validator;
    }

    public boolean isValid(String entry) {
        return entry != null && !entry.isBlank() && validator.test(entry.trim());
    }

    /** Adds an entry if valid and not already present. Returns whether it was added. */
    public boolean add(String entry) {
        if (!isValid(entry)) return false;
        String clean = entry.trim();
        if (value.contains(clean)) return false;
        List<String> copy = new ArrayList<>(value);
        copy.add(clean);
        set(copy);
        return true;
    }

    public boolean remove(String entry) {
        if (!value.contains(entry)) return false;
        List<String> copy = new ArrayList<>(value);
        copy.remove(entry);
        set(copy);
        return true;
    }

    @Override
    protected List<String> sanitize(List<String> v) {
        return Collections.unmodifiableList(new ArrayList<>(v));
    }

    @Override
    public String displayValue() {
        return String.join(", ", value);
    }

    @Override
    public JsonElement toJson() {
        JsonArray arr = new JsonArray();
        value.forEach(arr::add);
        return arr;
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element == null || !element.isJsonArray()) return;
        List<String> list = new ArrayList<>();
        for (JsonElement e : element.getAsJsonArray()) {
            String s = e.getAsString();
            if (isValid(s)) list.add(s.trim());
        }
        set(list);
    }

    /** Accepts {@code add <entry>}, {@code remove <entry>}, or a comma separated replacement list. */
    @Override
    public boolean parse(String input) {
        if (input.startsWith("add ")) return add(input.substring(4));
        if (input.startsWith("remove ")) return remove(input.substring(7).trim());
        List<String> list = new ArrayList<>();
        for (String s : input.split(",")) {
            if (!isValid(s)) return false;
            list.add(s.trim());
        }
        set(list);
        return true;
    }
}
