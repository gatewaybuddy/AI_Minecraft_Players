package com.aiplayer.core;

import com.aiplayer.AIPlayerMod;
import com.aiplayer.config.AIPlayerConfig;
import com.aiplayer.llm.LLMFactory;
import com.aiplayer.llm.LLMProvider;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Manages all AI player instances.
 *
 * Responsibilities:
 * - Create and spawn AI players
 * - Track active AI players
 * - Handle AI player lifecycle (spawn, despawn, respawn)
 * - Coordinate multiple AI players
 *
 * This is a singleton class - only one instance exists per server.
 */
public class AIPlayerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPlayerManager.class);

    // Map of player name to AI player entity
    private final Map<String, AIPlayerEntity> aiPlayers = new HashMap<>();

    // Map of UUID to AI player entity for quick lookup
    private final Map<UUID, AIPlayerEntity> aiPlayersByUuid = new HashMap<>();

    // Server reference (set when first AI spawns)
    private MinecraftServer server;

    // LLM provider (shared by all AI players)
    private LLMProvider llmProvider;

    public AIPlayerManager() {
        LOGGER.info("AIPlayerManager initialized");
        initializeLLMProvider();
    }

    /**
     * Initialize LLM provider from config.
     */
    private void initializeLLMProvider() {
        try {
            AIPlayerConfig.LLMConfig llmConfig = AIPlayerMod.getConfig().getLlm();

            // Check if API key is provided (not needed for local)
            String apiKey = llmConfig.getApiKey();
            if ((llmConfig.getProvider().equals("openai") || llmConfig.getProvider().equals("claude"))
                && (apiKey == null || apiKey.trim().isEmpty())) {
                LOGGER.warn("LLM API key not configured - AI players will run in SIMPLE mode");
                LOGGER.info("To enable intelligent mode, set apiKey in aiplayer-config.json");
                this.llmProvider = null;
                return;
            }

            // Create LLM provider
            this.llmProvider = LLMFactory.create(
                llmConfig.getProvider(),
                apiKey,
                llmConfig.getModel(),
                llmConfig.getLocalModelUrl(),
                true // Enable caching
            );

            if (this.llmProvider != null) {
                LOGGER.info("LLM provider initialized: {} ({})",
                    this.llmProvider.getProviderName(),
                    this.llmProvider.getModelName());
            } else {
                LOGGER.warn("Failed to initialize LLM provider - AI players will run in SIMPLE mode");
            }

        } catch (Exception e) {
            LOGGER.error("Error initializing LLM provider - AI players will run in SIMPLE mode", e);
            this.llmProvider = null;
        }
    }

    /**
     * Set the server reference.
     * Called when the first AI player is spawned.
     */
    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    /**
     * Get the number of active AI players.
     */
    public int getActivePlayerCount() {
        return aiPlayers.size();
    }

    /**
     * Check if an AI player exists with the given name.
     */
    public boolean hasPlayer(String username) {
        return aiPlayers.containsKey(username);
    }

    /**
     * Check if an AI player exists with the given UUID.
     */
    public boolean hasPlayer(UUID uuid) {
        return aiPlayersByUuid.containsKey(uuid);
    }

    /**
     * Spawn a new AI player.
     *
     * @param server Minecraft server instance
     * @param username Name for the AI player
     * @param world World to spawn in (null = overworld)
     * @param spawnPos Spawn position (null = world spawn)
     * @return The spawned AI player, or null if failed
     */
    public AIPlayerEntity spawnAIPlayer(MinecraftServer server, String username, ServerWorld world, Vec3d spawnPos) {
        this.server = server;

        // Check if player already exists
        if (hasPlayer(username)) {
            LOGGER.warn("AI player '{}' already exists", username);
            return aiPlayers.get(username);
        }

        try {
            // Use overworld if no world specified
            if (world == null) {
                world = server.getWorld(World.OVERWORLD);
            }

            // Use world spawn if no position specified
            if (spawnPos == null) {
                spawnPos = Vec3d.of(world.getSpawnPos()).add(0.5, 1, 0.5);
            }

            // Create game profile for the AI player
            UUID uuid = UUID.randomUUID();
            GameProfile profile = new GameProfile(uuid, username);

            // Get auto-respawn setting from config
            boolean autoRespawn = AIPlayerMod.getConfig().getBehavior().isAutoRespawn();

            // Create the AI player entity with LLM provider
            AIPlayerEntity aiPlayer = new AIPlayerEntity(server, world, profile, autoRespawn, llmProvider);

            // Set spawn position
            aiPlayer.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, 0, 0);

            // Add to world
            world.spawnEntity(aiPlayer);

            // Add to player manager
            server.getPlayerManager().onPlayerConnect(
                new FakeClientConnection(aiPlayer),
                aiPlayer
            );

            // Track the player
            aiPlayers.put(username, aiPlayer);
            aiPlayersByUuid.put(uuid, aiPlayer);

            LOGGER.info("Spawned AI player '{}' at {}, {}, {} in {}",
                username, spawnPos.x, spawnPos.y, spawnPos.z, world.getRegistryKey().getValue());

            return aiPlayer;

        } catch (Exception e) {
            LOGGER.error("Failed to spawn AI player '{}'", username, e);
            return null;
        }
    }

    /**
     * Simplified spawn method using config defaults.
     */
    public AIPlayerEntity spawnAIPlayer(MinecraftServer server, String username) {
        return spawnAIPlayer(server, username, null, null);
    }

    /**
     * Despawn an AI player by name.
     */
    public boolean despawnAIPlayer(String username) {
        AIPlayerEntity player = aiPlayers.get(username);
        if (player == null) {
            LOGGER.warn("AI player '{}' not found", username);
            return false;
        }

        return despawnAIPlayer(player);
    }

    /**
     * Despawn an AI player by UUID.
     */
    public boolean despawnAIPlayer(UUID uuid) {
        AIPlayerEntity player = aiPlayersByUuid.get(uuid);
        if (player == null) {
            LOGGER.warn("AI player with UUID {} not found", uuid);
            return false;
        }

        return despawnAIPlayer(player);
    }

    /**
     * Despawn an AI player entity.
     */
    private boolean despawnAIPlayer(AIPlayerEntity player) {
        try {
            String username = player.getName().getString();
            UUID uuid = player.getAIPlayerId();

            // Cleanup AI components
            player.cleanup();

            // Remove from world
            player.kill();

            // Remove from tracking
            aiPlayers.remove(username);
            aiPlayersByUuid.remove(uuid);

            LOGGER.info("Despawned AI player '{}'", username);
            return true;

        } catch (Exception e) {
            LOGGER.error("Failed to despawn AI player", e);
            return false;
        }
    }

    /**
     * Get an AI player by name.
     */
    public AIPlayerEntity getPlayer(String username) {
        return aiPlayers.get(username);
    }

    /**
     * Get an AI player by UUID.
     */
    public AIPlayerEntity getPlayer(UUID uuid) {
        return aiPlayersByUuid.get(uuid);
    }

    /**
     * Get all active AI players.
     */
    public Collection<AIPlayerEntity> getAllPlayers() {
        return Collections.unmodifiableCollection(aiPlayers.values());
    }

    /**
     * Get all AI player names.
     */
    public Set<String> getAllPlayerNames() {
        return Collections.unmodifiableSet(aiPlayers.keySet());
    }

    /**
     * Tick all AI players - called every server tick.
     * Note: Individual AI players tick themselves, this is for manager-level coordination.
     */
    public void tick() {
        // Phase 1: No manager-level coordination needed yet
        // Phase 3+: Will add goal coordination, resource sharing, etc.
    }

    /**
     * Cleanup all AI players (called on server shutdown).
     */
    public void cleanupAll() {
        LOGGER.info("Cleaning up all AI players...");

        for (AIPlayerEntity player : new ArrayList<>(aiPlayers.values())) {
            despawnAIPlayer(player);
        }

        aiPlayers.clear();
        aiPlayersByUuid.clear();

        LOGGER.info("All AI players cleaned up");
    }

    /**
     * Get status of all AI players.
     */
    public List<String> getAllPlayerStatus() {
        List<String> status = new ArrayList<>();
        for (AIPlayerEntity player : aiPlayers.values()) {
            status.add(player.getStatusInfo());
        }
        return status;
    }
}
