package com.aiplayer.learning;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * World knowledge persistence system - save and load world data to/from disk (Phase 6).
 *
 * Saves:
 * - Landmarks (villages, structures, points of interest)
 * - Resource locations (ores, trees, water sources)
 * - Explored chunks
 * - World facts
 *
 * File format: config/aiplayer/world/{aiPlayerUUID}/world_knowledge.json
 *
 * Phase 6: Optimization & Polish
 */
public class WorldKnowledgePersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldKnowledgePersistence.class);

    private static final String WORLD_DIR = "config/aiplayer/world";
    private final Gson gson;

    public WorldKnowledgePersistence() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    }

    /**
     * Save world knowledge to disk.
     *
     * @param aiPlayerUUID The AI player's UUID
     * @param worldKnowledge The world knowledge to save
     * @return true if successful
     */
    public boolean saveWorldKnowledge(UUID aiPlayerUUID, WorldKnowledge worldKnowledge) {
        try {
            // Create directory
            Path worldPath = Paths.get(WORLD_DIR, aiPlayerUUID.toString());
            Files.createDirectories(worldPath);

            // Build JSON
            JsonObject json = new JsonObject();

            // Save landmarks
            JsonArray landmarksArray = new JsonArray();
            for (WorldKnowledge.Landmark landmark : worldKnowledge.getAllLandmarks()) {
                landmarksArray.add(serializeLandmark(landmark));
            }
            json.add("landmarks", landmarksArray);

            // Save explored chunks
            JsonArray chunksArray = new JsonArray();
            // Note: Would need to expose explored chunks from WorldKnowledge
            // For now, just save count
            json.addProperty("exploredChunkCount", worldKnowledge.getExploredChunkCount());

            // Write to file
            File worldFile = worldPath.resolve("world_knowledge.json").toFile();
            try (FileWriter writer = new FileWriter(worldFile)) {
                gson.toJson(json, writer);
            }

            LOGGER.info("Saved world knowledge for AI player {}", aiPlayerUUID);
            return true;

        } catch (IOException e) {
            LOGGER.error("Failed to save world knowledge for AI player {}", aiPlayerUUID, e);
            return false;
        }
    }

    /**
     * Load world knowledge from disk.
     *
     * @param aiPlayerUUID The AI player's UUID
     * @param worldKnowledge The world knowledge object to populate
     * @return true if successful
     */
    public boolean loadWorldKnowledge(UUID aiPlayerUUID, WorldKnowledge worldKnowledge) {
        try {
            Path worldFilePath = Paths.get(WORLD_DIR, aiPlayerUUID.toString(), "world_knowledge.json");
            File worldFile = worldFilePath.toFile();

            if (!worldFile.exists()) {
                LOGGER.debug("No saved world knowledge found for AI player {}", aiPlayerUUID);
                return false;
            }

            // Read JSON
            try (FileReader reader = new FileReader(worldFile)) {
                JsonObject json = gson.fromJson(reader, JsonObject.class);

                // Load landmarks
                if (json.has("landmarks")) {
                    JsonArray landmarksArray = json.getAsJsonArray("landmarks");
                    for (int i = 0; i < landmarksArray.size(); i++) {
                        JsonObject landmarkObj = landmarksArray.get(i).getAsJsonObject();
                        WorldKnowledge.Landmark landmark = deserializeLandmark(landmarkObj);
                        if (landmark != null) {
                            worldKnowledge.addLandmark(landmark);
                        }
                    }
                }
            }

            LOGGER.info("Loaded world knowledge for AI player {}", aiPlayerUUID);
            return true;

        } catch (IOException e) {
            LOGGER.error("Failed to load world knowledge for AI player {}", aiPlayerUUID, e);
            return false;
        }
    }

    /**
     * Serialize a landmark to JSON.
     */
    private JsonObject serializeLandmark(WorldKnowledge.Landmark landmark) {
        JsonObject json = new JsonObject();

        json.addProperty("name", landmark.getName());
        json.addProperty("type", landmark.getType().name());
        json.addProperty("description", landmark.getDescription());

        // Position
        JsonObject posJson = new JsonObject();
        posJson.addProperty("x", landmark.getPosition().x);
        posJson.addProperty("y", landmark.getPosition().y);
        posJson.addProperty("z", landmark.getPosition().z);
        json.add("position", posJson);

        // Stats
        json.addProperty("discoveredTimestamp", landmark.getDiscoveredTimestamp());
        json.addProperty("visits", landmark.getVisits());

        return json;
    }

    /**
     * Deserialize a landmark from JSON.
     */
    private WorldKnowledge.Landmark deserializeLandmark(JsonObject json) {
        try {
            String name = json.get("name").getAsString();
            String typeStr = json.get("type").getAsString();
            String description = json.get("description").getAsString();

            JsonObject posJson = json.getAsJsonObject("position");
            WorldKnowledge.Position position = new WorldKnowledge.Position(
                posJson.get("x").getAsDouble(),
                posJson.get("y").getAsDouble(),
                posJson.get("z").getAsDouble()
            );

            WorldKnowledge.Landmark.LandmarkType type =
                WorldKnowledge.Landmark.LandmarkType.valueOf(typeStr);

            WorldKnowledge.Landmark landmark = new WorldKnowledge.Landmark(
                name, position, type, description
            );

            // Restore visit count
            if (json.has("visits")) {
                int visits = json.get("visits").getAsInt();
                for (int i = 0; i < visits; i++) {
                    landmark.recordVisit();
                }
            }

            return landmark;

        } catch (Exception e) {
            LOGGER.error("Failed to deserialize landmark from JSON", e);
            return null;
        }
    }

    /**
     * Delete world knowledge for an AI player.
     *
     * @param aiPlayerUUID The AI player's UUID
     * @return true if successful
     */
    public boolean deleteWorldKnowledge(UUID aiPlayerUUID) {
        try {
            Path worldPath = Paths.get(WORLD_DIR, aiPlayerUUID.toString());
            File worldDir = worldPath.toFile();

            if (worldDir.exists()) {
                File[] files = worldDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
                worldDir.delete();
                LOGGER.info("Deleted world knowledge for AI player {}", aiPlayerUUID);
                return true;
            }

            return false;

        } catch (Exception e) {
            LOGGER.error("Failed to delete world knowledge for AI player {}", aiPlayerUUID, e);
            return false;
        }
    }

    /**
     * Check if world knowledge exists for an AI player.
     *
     * @param aiPlayerUUID The AI player's UUID
     * @return true if world knowledge file exists
     */
    public boolean worldKnowledgeExists(UUID aiPlayerUUID) {
        Path worldFilePath = Paths.get(WORLD_DIR, aiPlayerUUID.toString(), "world_knowledge.json");
        return worldFilePath.toFile().exists();
    }
}
