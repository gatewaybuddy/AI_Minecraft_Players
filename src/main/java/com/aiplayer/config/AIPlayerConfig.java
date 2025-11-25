package com.aiplayer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration for the AI Minecraft Player mod.
 *
 * This class defines all configurable settings including:
 * - Player identity (username, personality)
 * - LLM provider settings
 * - Behavior parameters
 * - Goal and task settings
 * - Memory system settings
 */
public class AIPlayerConfig {

    private String username = "AISteve";
    private String personality = "helpful and curious";

    private LLMConfig llm = new LLMConfig();
    private BehaviorConfig behavior = new BehaviorConfig();
    private GoalsConfig goals = new GoalsConfig();
    private MemoryConfig memory = new MemoryConfig();

    // Getters
    public String getUsername() {
        return username;
    }

    public String getPersonality() {
        return personality;
    }

    public LLMConfig getLlm() {
        return llm;
    }

    public BehaviorConfig getBehavior() {
        return behavior;
    }

    public GoalsConfig getGoals() {
        return goals;
    }

    public MemoryConfig getMemory() {
        return memory;
    }

    /**
     * LLM provider configuration.
     */
    public static class LLMConfig {
        private String provider = "openai"; // openai, claude, local
        private String model = "gpt-4";
        private String apiKey = "";
        private String localModelUrl = "http://localhost:11434";
        private int maxTokens = 1000;
        private double temperature = 0.7;

        public String getProvider() {
            return provider;
        }

        public String getModel() {
            return model;
        }

        public String getApiKey() {
            return apiKey;
        }

        public String getLocalModelUrl() {
            return localModelUrl;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public double getTemperature() {
            return temperature;
        }
    }

    /**
     * Behavior configuration.
     */
    public static class BehaviorConfig {
        private int reactionTimeMs = 200;
        private boolean movementHumanization = true;
        private boolean chatEnabled = true;
        private boolean autoRespawn = true;
        private int actionCacheSize = 100;
        private int aiUpdateIntervalTicks = 5; // How often AI updates (5 = 4 times per second, 10 = 2 times per second)
        private int brainDecisionIntervalTicks = 8; // How often brain makes decisions (8 = 400ms, 4 = 200ms, 20 = 1000ms)

        public int getReactionTimeMs() {
            return reactionTimeMs;
        }

        public boolean isMovementHumanization() {
            return movementHumanization;
        }

        public boolean isChatEnabled() {
            return chatEnabled;
        }

        public boolean isAutoRespawn() {
            return autoRespawn;
        }

        public int getActionCacheSize() {
            return actionCacheSize;
        }

        public int getAiUpdateIntervalTicks() {
            return aiUpdateIntervalTicks;
        }

        public int getBrainDecisionIntervalTicks() {
            return brainDecisionIntervalTicks;
        }
    }

    /**
     * Goals and task configuration.
     */
    public static class GoalsConfig {
        private String defaultGoal = "explore and gather resources";
        private boolean acceptPlayerRequests = true;
        private int maxActiveGoals = 3;
        private boolean autonomousGoalGeneration = true;

        public String getDefaultGoal() {
            return defaultGoal;
        }

        public boolean isAcceptPlayerRequests() {
            return acceptPlayerRequests;
        }

        public int getMaxActiveGoals() {
            return maxActiveGoals;
        }

        public boolean isAutonomousGoalGeneration() {
            return autonomousGoalGeneration;
        }
    }

    /**
     * Memory system configuration.
     */
    public static class MemoryConfig {
        private String storageType = "json"; // json, sqlite, vector
        private int maxEpisodicMemories = 1000;
        private boolean enableSemanticSearch = false;
        private String vectorDbUrl = "";

        public String getStorageType() {
            return storageType;
        }

        public int getMaxEpisodicMemories() {
            return maxEpisodicMemories;
        }

        public boolean isEnableSemanticSearch() {
            return enableSemanticSearch;
        }

        public String getVectorDbUrl() {
            return vectorDbUrl;
        }
    }

    /**
     * Load configuration from JSON file.
     *
     * @param configPath Path to the configuration file
     * @return Loaded configuration
     * @throws IOException If file cannot be read
     */
    public static AIPlayerConfig load(Path configPath) throws IOException {
        if (!Files.exists(configPath)) {
            throw new IOException("Configuration file not found: " + configPath);
        }

        String json = Files.readString(configPath);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        AIPlayerConfig config = gson.fromJson(json, AIPlayerConfig.class);

        // Validate configuration
        if (config == null) {
            throw new IOException("Failed to parse configuration file");
        }

        validateConfig(config);

        return config;
    }

    /**
     * Save configuration to JSON file.
     *
     * @param configPath Path to save the configuration
     * @throws IOException If file cannot be written
     */
    public void save(Path configPath) throws IOException {
        // Ensure parent directory exists
        Files.createDirectories(configPath.getParent());

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(this);

        Files.writeString(configPath, json);
    }

    /**
     * Validate configuration values.
     *
     * @param config Configuration to validate
     * @throws IllegalArgumentException If configuration is invalid
     */
    private static void validateConfig(AIPlayerConfig config) {
        if (config.username == null || config.username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        if (config.llm.provider == null || config.llm.provider.trim().isEmpty()) {
            throw new IllegalArgumentException("LLM provider cannot be empty");
        }

        if (config.llm.maxTokens <= 0) {
            throw new IllegalArgumentException("Max tokens must be positive");
        }

        if (config.llm.temperature < 0 || config.llm.temperature > 2) {
            throw new IllegalArgumentException("Temperature must be between 0 and 2");
        }

        if (config.behavior.reactionTimeMs < 0) {
            throw new IllegalArgumentException("Reaction time cannot be negative");
        }

        if (config.goals.maxActiveGoals <= 0) {
            throw new IllegalArgumentException("Max active goals must be positive");
        }

        if (config.memory.maxEpisodicMemories <= 0) {
            throw new IllegalArgumentException("Max episodic memories must be positive");
        }
    }
}
