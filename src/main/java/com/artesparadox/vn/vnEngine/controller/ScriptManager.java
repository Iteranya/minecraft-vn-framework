// Suggested package location
package com.artesparadox.vn.vnEngine.controller;

import com.artesparadox.vn.vnEngine.dataclass.Const;
import com.artesparadox.vn.vnEngine.dataclass.Script;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages multiple scripts by loading them from a directory within the Minecraft world save.
 * Identifies and stores scripts by their unique UUID.
 */
public class ScriptManager {

    private static final Gson GSON = new Gson();
    private final Path scriptDirectory;
    private final Map<String, Script> scripts = new ConcurrentHashMap<>(); // Key = UUID

    /**
     * Initializes the ScriptManager.
     * It determines the script directory path based on the Minecraft world save
     * and ensures the directory exists.
     *
     * @param server The running MinecraftServer instance.
     */
    public ScriptManager(MinecraftServer server) {
        // Assuming you add a SCRIPTS_SUBDIR constant to your Const class
        // e.g., public static final String SCRIPTS_SUBDIR = "vn_scripts";
        Path frameworkPath = Paths.get(Const.FRAMEWORK_ID, Const.SCRIPTS_SUBDIR);
        this.scriptDirectory = server.getWorldPath(LevelResource.ROOT).resolve(frameworkPath);

        try {
            Files.createDirectories(this.scriptDirectory);
            System.out.println(Const.LOG_PREFIX + " Script directory ensured at: " + this.scriptDirectory.toAbsolutePath());
        } catch (IOException e) {
            System.err.println(Const.LOG_PREFIX + " [!] FATAL: Could not create script directory: " + this.scriptDirectory);
            e.printStackTrace();
        }
    }

    /**
     * Scans the script directory for .json files, loads them, and populates the script map.
     * This method can be called to reload all scripts.
     */
    public void loadScripts() {
        this.scripts.clear();
        System.out.println(Const.LOG_PREFIX + " Starting to load scripts from " + this.scriptDirectory);

        if (!Files.isDirectory(this.scriptDirectory)) {
            System.err.println(Const.LOG_PREFIX + " [!] Script directory does not exist or is not a directory.");
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.scriptDirectory, "*.json")) {
            for (Path entry : stream) {
                try {
                    String jsonContent = Files.readString(entry);
                    Script script = GSON.fromJson(jsonContent, Script.class);

                    // Post-load initialization is crucial to set defaults and UUID
                    script.postLoad(entry.getFileName().toString());

                    if (script.getId() == null || script.getId().isEmpty()) {
                        System.err.println("[!] Failed to load " + entry.getFileName() + ": Script has no valid UUID after initialization.");
                        continue;
                    }

                    if (this.scripts.containsKey(script.getId())) {
                        System.out.println("[!] Warning: Duplicate UUID '" + script.getId() + "' found. Overwriting script '" + this.scripts.get(script.getId()).getName() + "' with '" + script.getName() + "'.");
                    }

                    this.scripts.put(script.getId(), script);
                    System.out.println("[+] Loaded script: " + script.getName() + " (UUID: " + script.getId() + ")");

                } catch (JsonSyntaxException e) {
                    System.err.println("[!] Failed to load " + entry.getFileName() + ": Invalid JSON format. " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("[!] Failed to load " + entry.getFileName() + ": " + e);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("[!] Error reading script directory: " + e);
            e.printStackTrace();
        }
        System.out.println(Const.LOG_PREFIX + " Finished loading scripts. Total: " + this.scripts.size());
    }

    /**
     * Retrieves a script by its unique ID (UUID).
     *
     * @param scriptId The UUID of the script to find.
     * @return The Script object, or null if not found.
     */
    public Script getScript(String scriptId) {
        return this.scripts.get(scriptId);
    }

    /**
     * Finds all scripts that match a specific trigger condition.
     *
     * @param triggerKey The key of the trigger (e.g., "on_npc_interact").
     * @param triggerValue The value the trigger key must have (e.g., "minecraft:villager").
     * @return A list of matching scripts. The list will be empty if no matches are found.
     */
    public List<Script> findByTrigger(String triggerKey, String triggerValue) {
        return this.scripts.values().stream()
                .filter(script -> triggerValue.equals(script.getTriggers().get(triggerKey)))
                .collect(Collectors.toList());
    }
}
