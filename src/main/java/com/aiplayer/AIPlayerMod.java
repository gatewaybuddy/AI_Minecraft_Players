package com.aiplayer;

import com.aiplayer.config.AIPlayerConfig;
import com.aiplayer.core.AIPlayerManager;
import com.aiplayer.util.AILogger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main mod class for AI Minecraft Player
 *
 * This mod adds autonomous AI players that can perform all player actions,
 * communicate naturally, and collaborate with human players.
 *
 * @author AI Minecraft Players Team
 * @version 0.1.0-alpha
 */
public class AIPlayerMod implements ModInitializer {
    public static final String MOD_ID = "aiplayer";
    public static final String MOD_NAME = "AI Minecraft Player";
    public static final String VERSION = "0.1.0-alpha";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static AIPlayerConfig config;
    private static AIPlayerManager playerManager;

    @Override
    public void onInitialize() {
        long startTime = System.currentTimeMillis();

        LOGGER.info("========================================");
        LOGGER.info("Initializing {} v{}", MOD_NAME, VERSION);
        LOGGER.info("========================================");

        try {
            // Load configuration
            initializeConfig();

            // Initialize player manager
            playerManager = new AIPlayerManager();
            LOGGER.info("AI Player Manager initialized");

            // Register commands
            registerCommands();
            LOGGER.info("Commands registered");

            long initTime = System.currentTimeMillis() - startTime;
            LOGGER.info("========================================");
            LOGGER.info("{} initialized successfully in {}ms", MOD_NAME, initTime);
            LOGGER.info("========================================");

        } catch (Exception e) {
            LOGGER.error("Failed to initialize {}", MOD_NAME, e);
            throw new RuntimeException("Failed to initialize AI Player mod", e);
        }
    }

    /**
     * Initialize configuration system
     */
    private void initializeConfig() {
        try {
            Path configPath = Paths.get("config", "aiplayer.json");
            config = AIPlayerConfig.load(configPath);
            LOGGER.info("Configuration loaded from: {}", configPath);

            // Log configuration summary
            LOGGER.info("Default AI username: {}", config.username);
            LOGGER.info("LLM provider: {}", config.llm.provider);
            LOGGER.info("Chat enabled: {}", config.behavior.chatEnabled);

        } catch (Exception e) {
            LOGGER.error("Failed to load configuration", e);
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

    /**
     * Register mod commands
     */
    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("aiplayer")
                .requires(source -> source.hasPermissionLevel(2)) // Require OP
                .then(CommandManager.literal("spawn")
                    .executes(context -> {
                        // Spawn with default config name
                        return spawnAIPlayer(context, config.username);
                    })
                    .then(CommandManager.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(context -> {
                            // Spawn with custom name
                            String name = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "name");
                            return spawnAIPlayer(context, name);
                        })
                    )
                )
                .then(CommandManager.literal("despawn")
                    .then(CommandManager.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(context -> {
                            String name = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "name");

                            if (playerManager.despawnPlayerByName(name)) {
                                context.getSource().sendFeedback(
                                    () -> Text.literal(String.format("§a[AI Player] §7Despawned: §f%s", name)),
                                    false
                                );
                                return 1;
                            } else {
                                context.getSource().sendError(
                                    Text.literal(String.format("§c[AI Player] §7AI player not found: §f%s", name))
                                );
                                return 0;
                            }
                        })
                    )
                )
                .then(CommandManager.literal("list")
                    .executes(context -> {
                        int count = playerManager.getActivePlayerCount();
                        context.getSource().sendFeedback(
                            () -> Text.literal(String.format("§e[AI Player] §7Active AI players: %d", count)),
                            false
                        );
                        return count;
                    })
                )
                .then(CommandManager.literal("status")
                    .executes(context -> {
                        context.getSource().sendFeedback(
                            () -> Text.literal("§e[AI Player] §7Status:"),
                            false
                        );
                        context.getSource().sendFeedback(
                            () -> Text.literal(String.format("  §7Version: §f%s", VERSION)),
                            false
                        );
                        context.getSource().sendFeedback(
                            () -> Text.literal(String.format("  §7Active AIs: §f%d", playerManager.getActivePlayerCount())),
                            false
                        );
                        context.getSource().sendFeedback(
                            () -> Text.literal(String.format("  §7LLM Provider: §f%s", config.llm.provider)),
                            false
                        );
                        return 1;
                    })
                )
                .then(CommandManager.literal("reload")
                    .executes(context -> {
                        context.getSource().sendFeedback(
                            () -> Text.literal("§e[AI Player] §7Reloading configuration..."),
                            false
                        );

                        try {
                            initializeConfig();
                            context.getSource().sendFeedback(
                                () -> Text.literal("§a[AI Player] §7Configuration reloaded successfully!"),
                                false
                            );
                            return 1;
                        } catch (Exception e) {
                            context.getSource().sendError(
                                Text.literal("§c[AI Player] §7Failed to reload configuration: " + e.getMessage())
                            );
                            return 0;
                        }
                    })
                )
                .then(CommandManager.literal("help")
                    .executes(context -> {
                        context.getSource().sendFeedback(
                            () -> Text.literal("§e[AI Player] §7Commands:"),
                            false
                        );
                        context.getSource().sendFeedback(
                            () -> Text.literal("  §f/aiplayer spawn [name] §7- Spawn an AI player"),
                            false
                        );
                        context.getSource().sendFeedback(
                            () -> Text.literal("  §f/aiplayer despawn <name> §7- Despawn an AI player"),
                            false
                        );
                        context.getSource().sendFeedback(
                            () -> Text.literal("  §f/aiplayer list §7- List active AI players"),
                            false
                        );
                        context.getSource().sendFeedback(
                            () -> Text.literal("  §f/aiplayer status §7- Show mod status"),
                            false
                        );
                        context.getSource().sendFeedback(
                            () -> Text.literal("  §f/aiplayer reload §7- Reload configuration"),
                            false
                        );
                        context.getSource().sendFeedback(
                            () -> Text.literal("  §f/aiplayer help §7- Show this help"),
                            false
                        );
                        return 1;
                    })
                )
                .executes(context -> {
                    // Default command - show help
                    context.getSource().sendFeedback(
                        () -> Text.literal("§e[AI Player] §7Use §f/aiplayer help §7for available commands"),
                        false
                    );
                    return 1;
                })
            );
        });
    }

    /**
     * Helper method to spawn an AI player from command
     *
     * @param context Command context
     * @param name Name of the AI player
     * @return 1 if successful, 0 if failed
     */
    private int spawnAIPlayer(net.minecraft.server.command.ServerCommandSource context, String name) {
        try {
            context.getSource().sendFeedback(
                () -> Text.literal(String.format("§e[AI Player] §7Spawning: §f%s§7...", name)),
                false
            );

            // Spawn the AI player
            com.aiplayer.core.AIPlayerEntity aiPlayer = playerManager.spawnPlayer(
                context.getServer(),
                name
            );

            context.getSource().sendFeedback(
                () -> Text.literal(String.format(
                    "§a[AI Player] §7Successfully spawned: §f%s §7at %s",
                    aiPlayer.getAIName(),
                    aiPlayer.getPositionString()
                )),
                false
            );

            return 1;

        } catch (IllegalArgumentException e) {
            context.getSource().sendError(
                Text.literal(String.format("§c[AI Player] §7Invalid name: §f%s", e.getMessage()))
            );
            return 0;
        } catch (Exception e) {
            context.getSource().sendError(
                Text.literal(String.format("§c[AI Player] §7Failed to spawn: §f%s", e.getMessage()))
            );
            AILogger.error("Failed to spawn AI player: {}", name, e);
            return 0;
        }
    }

    /**
     * Get the mod configuration
     */
    public static AIPlayerConfig getConfig() {
        return config;
    }

    /**
     * Get the player manager
     */
    public static AIPlayerManager getPlayerManager() {
        return playerManager;
    }
}
