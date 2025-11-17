package com.aiplayer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration for AI Minecraft Player mod
 *
 * This class handles loading and saving mod configuration from JSON files.
 */
public class AIPlayerConfig {
    public String username = "AISteve";
    public String personality = "helpful and friendly assistant who loves exploring Minecraft";

    public LLMConfig llm = new LLMConfig();
    public BehaviorConfig behavior = new BehaviorConfig();
    public GoalsConfig goals = new GoalsConfig();
    public MemoryConfig memory = new MemoryConfig();

    /**
     * LLM (Large Language Model) configuration
     */
    public static class LLMConfig {
        public String provider = "openai"; // openai, claude, local
        public String model = "gpt-4";
        public String apiKey = "";
        public String baseUrl = "https://api.openai.com/v1";
        public int maxTokens = 1000;
        public double temperature = 0.7;
        public int timeoutSeconds = 30;
    }

    /**
     * Behavior configuration
     */
    public static class BehaviorConfig {
        public int reactionTimeMs = 200;
        public boolean movementHumanization = true;
        public boolean chatEnabled = true;
        public boolean autoRespawn = true;
        public int actionCacheSize = 100;
        public boolean debugMode = false;
    }

    /**
     * Goals configuration
     */
    public static class GoalsConfig {
        public String defaultGoal = "explore and gather resources";
        public boolean acceptPlayerRequests = true;
        public int maxActiveGoals = 3;
        public boolean autonomousGoalGeneration = true;
    }

    /**
     * Memory configuration
     */
    public static class MemoryConfig {
        public String storageType = "json"; // json, sqlite, vector
        public int maxEpisodicMemories = 1000;
        public boolean enableSemanticSearch = false;
        public String vectorDbUrl = "";
        public int workingMemorySize = 10;
    }

    /**
     * Load configuration from file
     *
     * @param configPath Path to configuration file
     * @return Loaded configuration
     * @throws IOException If file cannot be read
     */
    public static AIPlayerConfig load(Path configPath) throws IOException {
        if (!Files.exists(configPath)) {
            // Create default configuration
            AIPlayerConfig defaultConfig = new AIPlayerConfig();
            defaultConfig.save(configPath);
            return defaultConfig;
        }

        String json = Files.readString(configPath);
        Gson gson = new Gson();

        try {
            return gson.fromJson(json, AIPlayerConfig.class);
        } catch (Exception e) {
            throw new IOException("Invalid configuration JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Save configuration to file
     *
     * @param configPath Path to save configuration
     * @throws IOException If file cannot be written
     */
    public void save(Path configPath) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(this);

        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath, json);
    }

    /**
     * Validate configuration
     *
     * @throws IllegalStateException If configuration is invalid
     */
    public void validate() {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalStateException("Username cannot be empty");
        }

        if (username.length() > 16) {
            throw new IllegalStateException("Username cannot be longer than 16 characters");
        }

        if (llm.provider == null || llm.provider.trim().isEmpty()) {
            throw new IllegalStateException("LLM provider cannot be empty");
        }

        if (!llm.provider.equals("local") && (llm.apiKey == null || llm.apiKey.trim().isEmpty())) {
            throw new IllegalStateException("API key is required for provider: " + llm.provider);
        }

        if (llm.maxTokens <= 0) {
            throw new IllegalStateException("Max tokens must be positive");
        }

        if (llm.temperature < 0 || llm.temperature > 2) {
            throw new IllegalStateException("Temperature must be between 0 and 2");
        }

        if (behavior.reactionTimeMs < 0) {
            throw new IllegalStateException("Reaction time cannot be negative");
        }

        if (goals.maxActiveGoals <= 0) {
            throw new IllegalStateException("Max active goals must be positive");
        }

        if (memory.maxEpisodicMemories <= 0) {
            throw new IllegalStateException("Max episodic memories must be positive");
        }
    }

    /**
     * Get a summary of this configuration
     */
    public String getSummary() {
        return String.format("AIPlayerConfig{username='%s', llm='%s/%s', chatEnabled=%s}",
                username, llm.provider, llm.model, behavior.chatEnabled);
    }
}
