package dev.sentinel.config;

import com.google.gson.*;
import dev.sentinel.Sentinel;
import dev.sentinel.gui.GuiState;
import dev.sentinel.module.Module;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Saves and loads named JSON profiles.
 * <pre>
 * .minecraft/sentinel/
 *   client.json            -> { "activeProfile": "default", "prefix": "." }
 *   friends.json           -> ["Alice", "Bob"]
 *   profiles/default.json  -> { "modules": {...}, "gui": {...} }
 * </pre>
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path root = FabricLoader.getInstance().getGameDir().resolve(Sentinel.MOD_ID);
    private final Path profilesDir = root.resolve("profiles");
    private final Path clientFile = root.resolve("client.json");
    private String activeProfile = "default";

    public String getActiveProfile() {
        return activeProfile;
    }

    /** Loads client.json and then the active profile. */
    public void load() {
        try {
            if (Files.exists(clientFile)) {
                JsonObject obj = JsonParser.parseString(Files.readString(clientFile)).getAsJsonObject();
                if (obj.has("activeProfile")) activeProfile = sanitize(obj.get("activeProfile").getAsString());
                if (obj.has("prefix")) Sentinel.commands().setPrefix(obj.get("prefix").getAsString());
            }
        } catch (Exception e) {
            Sentinel.LOG.error("Failed to read client.json", e);
        }
        loadProfile(activeProfile);
    }

    /** Saves the active profile and client.json. */
    public void save() {
        saveProfile(activeProfile);
    }

    public boolean saveProfile(String name) {
        name = sanitize(name);
        if (name.isEmpty()) return false;
        JsonObject root = new JsonObject();
        JsonObject mods = new JsonObject();
        for (Module m : Sentinel.modules().getAll()) mods.add(m.getName(), m.toJson());
        root.add("modules", mods);
        root.add("gui", GuiState.toJson());
        try {
            Files.createDirectories(profilesDir);
            Files.writeString(profilesDir.resolve(name + ".json"), GSON.toJson(root));
            activeProfile = name;
            writeClientFile();
            return true;
        } catch (IOException e) {
            Sentinel.LOG.error("Failed to save profile {}", name, e);
            return false;
        }
    }

    public boolean loadProfile(String name) {
        name = sanitize(name);
        Path file = profilesDir.resolve(name + ".json");
        if (!Files.exists(file)) return false;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            JsonObject mods = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
            for (Module m : Sentinel.modules().getAll()) {
                JsonElement e = mods.get(m.getName());
                if (e != null && e.isJsonObject()) m.fromJson(e.getAsJsonObject());
            }
            if (root.has("gui")) GuiState.fromJson(root.getAsJsonObject("gui"));
            activeProfile = name;
            writeClientFile();
            return true;
        } catch (Exception e) {
            Sentinel.LOG.error("Failed to load profile {}", name, e);
            return false;
        }
    }

    public boolean deleteProfile(String name) {
        name = sanitize(name);
        if (name.equals(activeProfile)) return false;
        try {
            return Files.deleteIfExists(profilesDir.resolve(name + ".json"));
        } catch (IOException e) {
            return false;
        }
    }

    public List<String> listProfiles() {
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(profilesDir)) return names;
        try (Stream<Path> files = Files.list(profilesDir)) {
            files.map(p -> p.getFileName().toString())
                    .filter(f -> f.endsWith(".json"))
                    .map(f -> f.substring(0, f.length() - 5))
                    .sorted()
                    .forEach(names::add);
        } catch (IOException ignored) {
            // empty list
        }
        return names;
    }

    private void writeClientFile() throws IOException {
        Files.createDirectories(root);
        JsonObject obj = new JsonObject();
        obj.addProperty("activeProfile", activeProfile);
        obj.addProperty("prefix", Sentinel.commands().getPrefix());
        Files.writeString(clientFile, GSON.toJson(obj));
    }

    /** Profile names become file names, so only allow a safe character set. */
    private static String sanitize(String name) {
        return name.replaceAll("[^A-Za-z0-9_\\-]", "");
    }
}
