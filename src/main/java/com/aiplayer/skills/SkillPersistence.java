package com.aiplayer.skills;

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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Skill persistence system - save and load skills to/from disk (Phase 6).
 *
 * Saves skills as JSON files for:
 * - Persistence across server restarts
 * - Sharing skills between AI players
 * - Manual skill editing/creation
 * - Backup and versioning
 *
 * File format: skills/{aiPlayerUUID}/skills.json
 *
 * Phase 6: Optimization & Polish
 */
public class SkillPersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillPersistence.class);

    private static final String SKILLS_DIR = "config/aiplayer/skills";
    private final Gson gson;

    public SkillPersistence() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    }

    /**
     * Save skills to disk for a specific AI player.
     *
     * @param aiPlayerUUID The AI player's UUID
     * @param skills List of skills to save
     * @return true if successful
     */
    public boolean saveSkills(UUID aiPlayerUUID, List<Skill> skills) {
        try {
            // Create directory structure
            Path skillsPath = Paths.get(SKILLS_DIR, aiPlayerUUID.toString());
            Files.createDirectories(skillsPath);

            // Build JSON array
            JsonArray skillsArray = new JsonArray();
            for (Skill skill : skills) {
                skillsArray.add(serializeSkill(skill));
            }

            // Write to file
            File skillsFile = skillsPath.resolve("skills.json").toFile();
            try (FileWriter writer = new FileWriter(skillsFile)) {
                gson.toJson(skillsArray, writer);
            }

            LOGGER.info("Saved {} skills for AI player {}", skills.size(), aiPlayerUUID);
            return true;

        } catch (IOException e) {
            LOGGER.error("Failed to save skills for AI player {}", aiPlayerUUID, e);
            return false;
        }
    }

    /**
     * Load skills from disk for a specific AI player.
     *
     * @param aiPlayerUUID The AI player's UUID
     * @return List of loaded skills (empty if file doesn't exist)
     */
    public List<Skill> loadSkills(UUID aiPlayerUUID) {
        List<Skill> skills = new ArrayList<>();

        try {
            Path skillsFilePath = Paths.get(SKILLS_DIR, aiPlayerUUID.toString(), "skills.json");
            File skillsFile = skillsFilePath.toFile();

            if (!skillsFile.exists()) {
                LOGGER.debug("No saved skills found for AI player {}", aiPlayerUUID);
                return skills;
            }

            // Read JSON
            try (FileReader reader = new FileReader(skillsFile)) {
                JsonArray skillsArray = gson.fromJson(reader, JsonArray.class);

                for (int i = 0; i < skillsArray.size(); i++) {
                    JsonObject skillObj = skillsArray.get(i).getAsJsonObject();
                    Skill skill = deserializeSkill(skillObj);
                    if (skill != null) {
                        skills.add(skill);
                    }
                }
            }

            LOGGER.info("Loaded {} skills for AI player {}", skills.size(), aiPlayerUUID);

        } catch (IOException e) {
            LOGGER.error("Failed to load skills for AI player {}", aiPlayerUUID, e);
        }

        return skills;
    }

    /**
     * Save a single skill template (shared across all AI players).
     *
     * @param skill The skill to save
     * @param category Category directory (e.g., "mining", "building")
     * @return true if successful
     */
    public boolean saveSkillTemplate(Skill skill, String category) {
        try {
            Path templatesPath = Paths.get(SKILLS_DIR, "templates", category);
            Files.createDirectories(templatesPath);

            JsonObject skillJson = serializeSkill(skill);

            String filename = skill.getName().toLowerCase().replaceAll("\\s+", "_") + ".json";
            File templateFile = templatesPath.resolve(filename).toFile();

            try (FileWriter writer = new FileWriter(templateFile)) {
                gson.toJson(skillJson, writer);
            }

            LOGGER.info("Saved skill template: {} to {}", skill.getName(), category);
            return true;

        } catch (IOException e) {
            LOGGER.error("Failed to save skill template: {}", skill.getName(), e);
            return false;
        }
    }

    /**
     * Load all skill templates from a category.
     *
     * @param category Category directory (e.g., "mining", "building")
     * @return List of template skills
     */
    public List<Skill> loadSkillTemplates(String category) {
        List<Skill> templates = new ArrayList<>();

        try {
            Path templatesPath = Paths.get(SKILLS_DIR, "templates", category);
            File categoryDir = templatesPath.toFile();

            if (!categoryDir.exists()) {
                LOGGER.debug("No skill templates found in category: {}", category);
                return templates;
            }

            File[] templateFiles = categoryDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (templateFiles != null) {
                for (File templateFile : templateFiles) {
                    try (FileReader reader = new FileReader(templateFile)) {
                        JsonObject skillObj = gson.fromJson(reader, JsonObject.class);
                        Skill skill = deserializeSkill(skillObj);
                        if (skill != null) {
                            templates.add(skill);
                        }
                    }
                }
            }

            LOGGER.info("Loaded {} skill templates from category: {}", templates.size(), category);

        } catch (IOException e) {
            LOGGER.error("Failed to load skill templates from category: {}", category, e);
        }

        return templates;
    }

    /**
     * Serialize a skill to JSON.
     */
    private JsonObject serializeSkill(Skill skill) {
        JsonObject json = new JsonObject();

        json.addProperty("name", skill.getName());
        json.addProperty("description", skill.getDescription());
        json.addProperty("category", skill.getCategory().name());
        json.addProperty("complexity", skill.getComplexity());

        // Prerequisites
        JsonArray prereqs = new JsonArray();
        for (String prereq : skill.getPrerequisites()) {
            prereqs.add(prereq);
        }
        json.add("prerequisites", prereqs);

        // Steps
        JsonArray steps = new JsonArray();
        for (String step : skill.getSteps()) {
            steps.add(step);
        }
        json.add("steps", steps);

        // Statistics
        JsonObject stats = new JsonObject();
        stats.addProperty("timesUsed", skill.getTimesUsed());
        stats.addProperty("timesSucceeded", skill.getTimesSucceeded());
        stats.addProperty("timesFailed", skill.getTimesFailed());
        stats.addProperty("lastUsedTimestamp", skill.getLastUsedTimestamp());
        json.add("statistics", stats);

        return json;
    }

    /**
     * Deserialize a skill from JSON.
     */
    private Skill deserializeSkill(JsonObject json) {
        try {
            String name = json.get("name").getAsString();
            String description = json.get("description").getAsString();
            String categoryStr = json.get("category").getAsString();
            int complexity = json.get("complexity").getAsInt();

            Skill.SkillCategory category = Skill.SkillCategory.valueOf(categoryStr);
            Skill skill = new Skill(name, description, category, complexity);

            // Prerequisites
            if (json.has("prerequisites")) {
                JsonArray prereqs = json.getAsJsonArray("prerequisites");
                for (int i = 0; i < prereqs.size(); i++) {
                    skill.addPrerequisite(prereqs.get(i).getAsString());
                }
            }

            // Steps
            if (json.has("steps")) {
                JsonArray steps = json.getAsJsonArray("steps");
                for (int i = 0; i < steps.size(); i++) {
                    skill.addStep(steps.get(i).getAsString());
                }
            }

            // Statistics
            if (json.has("statistics")) {
                JsonObject stats = json.getAsJsonObject("statistics");
                int timesUsed = stats.get("timesUsed").getAsInt();
                int timesSucceeded = stats.get("timesSucceeded").getAsInt();

                // Reconstruct usage history
                for (int i = 0; i < timesUsed; i++) {
                    skill.recordUse(i < timesSucceeded);
                }
            }

            return skill;

        } catch (Exception e) {
            LOGGER.error("Failed to deserialize skill from JSON", e);
            return null;
        }
    }

    /**
     * Delete all saved skills for an AI player.
     *
     * @param aiPlayerUUID The AI player's UUID
     * @return true if successful
     */
    public boolean deleteSkills(UUID aiPlayerUUID) {
        try {
            Path skillsPath = Paths.get(SKILLS_DIR, aiPlayerUUID.toString());
            File skillsDir = skillsPath.toFile();

            if (skillsDir.exists()) {
                File[] files = skillsDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
                skillsDir.delete();
                LOGGER.info("Deleted skills for AI player {}", aiPlayerUUID);
                return true;
            }

            return false;

        } catch (Exception e) {
            LOGGER.error("Failed to delete skills for AI player {}", aiPlayerUUID, e);
            return false;
        }
    }

    /**
     * Get the directory path for an AI player's skills.
     *
     * @param aiPlayerUUID The AI player's UUID
     * @return Path to skills directory
     */
    public Path getSkillsDirectory(UUID aiPlayerUUID) {
        return Paths.get(SKILLS_DIR, aiPlayerUUID.toString());
    }

    /**
     * Check if skills exist for an AI player.
     *
     * @param aiPlayerUUID The AI player's UUID
     * @return true if skills file exists
     */
    public boolean skillsExist(UUID aiPlayerUUID) {
        Path skillsFilePath = Paths.get(SKILLS_DIR, aiPlayerUUID.toString(), "skills.json");
        return skillsFilePath.toFile().exists();
    }

    /**
     * Get count of saved skills for an AI player.
     *
     * @param aiPlayerUUID The AI player's UUID
     * @return Number of saved skills
     */
    public int getSkillCount(UUID aiPlayerUUID) {
        List<Skill> skills = loadSkills(aiPlayerUUID);
        return skills.size();
    }
}
