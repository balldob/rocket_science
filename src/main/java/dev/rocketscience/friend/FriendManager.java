package dev.rocketscience.friend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.rocketscience.RocketScience;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Stores friend names (case-insensitive). Friends are skipped by combat modules and drawn blue
 * in ESP. Friends are global (not per profile) and live in {@code rocketscience/friends.json}.
 */
public final class FriendManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** lower-case name -> name as typed. */
    private final Map<String, String> friends = new LinkedHashMap<>();
    private final Path file = FabricLoader.getInstance().getGameDir().resolve(RocketScience.MOD_ID).resolve("friends.json");

    public boolean add(String name) {
        boolean added = friends.putIfAbsent(name.toLowerCase(Locale.ROOT), name) == null;
        if (added) save();
        return added;
    }

    public boolean remove(String name) {
        boolean removed = friends.remove(name.toLowerCase(Locale.ROOT)) != null;
        if (removed) save();
        return removed;
    }

    /** Adds or removes; returns the new friend state. */
    public boolean toggle(String name) {
        if (isFriend(name)) {
            remove(name);
            return false;
        }
        add(name);
        return true;
    }

    public boolean isFriend(String name) {
        return name != null && friends.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public boolean isFriend(Entity entity) {
        return entity instanceof PlayerEntity p && isFriend(p.getGameProfile().getName());
    }

    public Collection<String> getAll() {
        return Collections.unmodifiableCollection(friends.values());
    }

    public void load() {
        if (!Files.exists(file)) return;
        try {
            JsonElement root = JsonParser.parseString(Files.readString(file));
            friends.clear();
            for (JsonElement e : root.getAsJsonArray()) {
                String n = e.getAsString();
                friends.put(n.toLowerCase(Locale.ROOT), n);
            }
        } catch (Exception e) {
            RocketScience.LOG.error("Failed to load friends", e);
        }
    }

    public void save() {
        try {
            Files.createDirectories(file.getParent());
            JsonArray arr = new JsonArray();
            friends.values().forEach(arr::add);
            Files.writeString(file, GSON.toJson(arr));
        } catch (IOException e) {
            RocketScience.LOG.error("Failed to save friends", e);
        }
    }
}
