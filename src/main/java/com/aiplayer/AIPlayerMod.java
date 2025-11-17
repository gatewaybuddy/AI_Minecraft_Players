package com.aiplayer;

import com.aiplayer.config.AIPlayerConfig;
import com.aiplayer.core.AIPlayerManager;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main entry point for the AI Minecraft Player mod.
 *
 * This mod creates autonomous AI players that can:
 * - Join Minecraft servers as regular players
 * - Perform all player actions (mining, building, crafting, combat)
 * - Communicate naturally via chat using LLMs
 * - Remember experiences and learn from them
 * - Accept and complete tasks from human players
 * - Collaborate on shared goals
 */
public class AIPlayerMod implements ModInitializer {

    public static final String MOD_ID = "aiplayer";
    public static final String MOD_NAME = "AI Minecraft Player";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private static AIPlayerConfig config;
    private static AIPlayerManager playerManager;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing {} mod...", MOD_NAME);

        try {
            // Load configuration
            loadConfiguration();

            // Initialize AI player manager
            playerManager = new AIPlayerManager();

            // Register commands
            registerCommands();

            // Register event listeners
            registerEvents();

            LOGGER.info("{} mod initialized successfully!", MOD_NAME);
            LOGGER.info("AI Player: {} | LLM Provider: {}",
                config.getUsername(),
                config.getLlm().getProvider());

        } catch (Exception e) {
            LOGGER.error("Failed to initialize {} mod", MOD_NAME, e);
            throw new RuntimeException("Failed to initialize AI Player mod", e);
        }
    }

    /**
     * Load configuration from file or create default.
     */
    private void loadConfiguration() {
        Path configPath = Paths.get("config", "aiplayer.json");

        try {
            config = AIPlayerConfig.load(configPath);
            LOGGER.info("Configuration loaded from: {}", configPath);
        } catch (Exception e) {
            LOGGER.warn("Failed to load config, using defaults: {}", e.getMessage());
            config = new AIPlayerConfig();

            // Try to save default config
            try {
                config.save(configPath);
                LOGGER.info("Default configuration saved to: {}", configPath);
            } catch (Exception saveError) {
                LOGGER.error("Failed to save default configuration", saveError);
            }
        }
    }

    /**
     * Register mod commands.
     */
    private void registerCommands() {
        // Commands will be registered in Phase 1, Task 1.8
        LOGGER.debug("Command registration placeholder - will be implemented in Task 1.8");
    }

    /**
     * Register event listeners.
     */
    private void registerEvents() {
        // Event listeners will be registered in later phases
        LOGGER.debug("Event registration placeholder - will be implemented in later phases");
    }

    /**
     * Get the current configuration.
     */
    public static AIPlayerConfig getConfig() {
        return config;
    }

    /**
     * Get the AI player manager.
     */
    public static AIPlayerManager getPlayerManager() {
        return playerManager;
    }

    /**
     * Reload configuration from file.
     */
    public static void reloadConfig() {
        Path configPath = Paths.get("config", "aiplayer.json");
        try {
            config = AIPlayerConfig.load(configPath);
            LOGGER.info("Configuration reloaded successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to reload configuration", e);
        }
    }
}
