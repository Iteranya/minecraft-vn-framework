// Suggested package location
package com.artesparadox.vn.vnEngine.dataclass;

import com.google.gson.annotations.SerializedName;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single Visual Novel-like script (FSM) loaded from a JSON file.
 * This class is designed to be deserialized from JSON using Gson.
 */
public class Script {

    private String name;
    private String description;
    private Map<String, String> triggers;
    private Map<String, String> metadata;

    @SerializedName("required_mods")
    private List<String> requiredMods;

    @SerializedName("asset_dir")
    private String assetDir;

    private Map<String, Object> fsm;

    /**
     * Post-deserialization initialization.
     * This method should be called by the ScriptManager after a script is loaded
     * to ensure all fields are in a valid state (not null) and a UUID is present.
     *
     * @param originalFilename The filename from which the script was loaded, used as a fallback for the name.
     */
    public void postLoad(String originalFilename) {
        // Ensure collections are not null to prevent NullPointerExceptions
        if (this.name == null || this.name.isEmpty()) {
            this.name = originalFilename.endsWith(".json")
                    ? originalFilename.substring(0, originalFilename.length() - 5)
                    : originalFilename;
        }
        if (this.description == null) this.description = "";
        if (this.triggers == null) this.triggers = new HashMap<>();
        if (this.requiredMods == null) this.requiredMods = new ArrayList<>();
        if (this.fsm == null) this.fsm = new HashMap<>();

        // Ensure metadata and UUID exist, mirroring the Python logic
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        if (!this.metadata.containsKey("uuid") || this.metadata.get("uuid") == null || this.metadata.get("uuid").isEmpty()) {
            this.metadata.put("uuid", UUID.randomUUID().toString());
        }
        // Provide default metadata if missing
        this.metadata.putIfAbsent("creator", "unknown");
        this.metadata.putIfAbsent("version", "0.0.1");
    }

    /**
     * Unique identifier (UUID) for the script.
     * @return The script's UUID string.
     */
    public String getId() {
        return this.metadata.get("uuid");
    }

    // --- Standard Getters ---

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getTriggers() {
        return triggers;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public List<String> getRequiredMods() {
        return requiredMods;
    }

    public String getAssetDir() {
        return assetDir;
    }

    public Map<String, Object> getFsm() {
        return fsm;
    }
}
