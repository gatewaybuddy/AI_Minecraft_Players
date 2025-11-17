package com.aiplayer.core;

import com.aiplayer.AIPlayerMod;
import com.aiplayer.util.AILogger;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

/**
 * Manager for all AI players
 *
 * Handles spawning, despawning, and tracking of AI player entities.
 * This is a singleton that maintains the list of active AI players.
 */
public class AIPlayerManager {
    private final Map<UUID, AIPlayerEntity> activePlayers = new HashMap<>();
    private final Map<String, UUID> playersByName = new HashMap<>();

    public AIPlayerManager() {
        AILogger.info("AIPlayerManager initialized");
    }

    /**
     * Spawn a new AI player in the world
     *
     * @param server The Minecraft server
     * @param name Name for the AI player
     * @return The spawned AI player entity
     */
    public AIPlayerEntity spawnPlayer(MinecraftServer server, String name) {
        return spawnPlayer(server, name, null);
    }

    /**
     * Spawn a new AI player at a specific position
     *
     * @param server The Minecraft server
     * @param name Name for the AI player
     * @param spawnPos Position to spawn at (null for world spawn)
     * @return The spawned AI player entity
     */
    public AIPlayerEntity spawnPlayer(MinecraftServer server, String name, Vec3d spawnPos) {
        AILogger.info("Spawning AI player: {}", name);

        // Validate name
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("AI player name cannot be empty");
        }

        if (name.length() > 16) {
            throw new IllegalArgumentException("AI player name cannot be longer than 16 characters");
        }

        // Check if player already exists
        if (playerExists(name)) {
            AILogger.warn("AI player already exists: {}", name);
            return getPlayerByName(name);
        }

        try {
            // Create game profile with unique UUID
            UUID uuid = UUID.randomUUID();
            GameProfile profile = new GameProfile(uuid, name);

            // Get the overworld (default spawn dimension)
            ServerWorld world = server.getOverworld();

            // Create AI player entity
            AIPlayerEntity aiPlayer = new AIPlayerEntity(server, world, profile);

            // Determine spawn position
            Vec3d finalSpawnPos;
            if (spawnPos != null) {
                finalSpawnPos = spawnPos;
            } else {
                // Use world spawn point
                BlockPos worldSpawn = world.getSpawnPos();
                finalSpawnPos = new Vec3d(
                    worldSpawn.getX() + 0.5,
                    worldSpawn.getY(),
                    worldSpawn.getZ() + 0.5
                );
            }

            // Set spawn position
            aiPlayer.refreshPositionAndAngles(
                finalSpawnPos.x,
                finalSpawnPos.y,
                finalSpawnPos.z,
                0.0f, // yaw
                0.0f  // pitch
            );

            // Add player to the world
            world.spawnEntity(aiPlayer);

            // Initialize the AI player
            aiPlayer.initialize();

            // Register with manager
            registerPlayer(aiPlayer);

            AILogger.info("Successfully spawned AI player: {} at position {}",
                name, aiPlayer.getPositionString());

            return aiPlayer;

        } catch (Exception e) {
            AILogger.error("Failed to spawn AI player: {}", name, e);
            throw new RuntimeException("Failed to spawn AI player: " + name, e);
        }
    }

    /**
     * Despawn an AI player
     *
     * @param playerId UUID of the player to despawn
     * @return True if player was despawned, false if not found
     */
    public boolean despawnPlayer(UUID playerId) {
        AIPlayerEntity player = activePlayers.remove(playerId);
        if (player != null) {
            // Deactivate the player
            player.deactivate();

            // Remove from world
            player.kill();

            // Remove from name lookup
            playersByName.remove(player.getAIName());

            AILogger.info("Despawned AI player: {}", player.getAIName());
            return true;
        }

        return false;
    }

    /**
     * Despawn an AI player by name
     *
     * @param name Name of the player to despawn
     * @return True if player was despawned, false if not found
     */
    public boolean despawnPlayerByName(String name) {
        UUID playerId = playersByName.get(name);
        if (playerId != null) {
            return despawnPlayer(playerId);
        }
        return false;
    }

    /**
     * Despawn all AI players
     *
     * @return Number of players despawned
     */
    public int despawnAll() {
        int count = 0;
        List<UUID> playerIds = new ArrayList<>(activePlayers.keySet());

        for (UUID playerId : playerIds) {
            if (despawnPlayer(playerId)) {
                count++;
            }
        }

        AILogger.info("Despawned {} AI players", count);
        return count;
    }

    /**
     * Get an AI player by UUID
     *
     * @param playerId Player UUID
     * @return AI player entity, or null if not found
     */
    public AIPlayerEntity getPlayer(UUID playerId) {
        return activePlayers.get(playerId);
    }

    /**
     * Get an AI player by name
     *
     * @param name Player name
     * @return AI player entity, or null if not found
     */
    public AIPlayerEntity getPlayerByName(String name) {
        UUID playerId = playersByName.get(name);
        return playerId != null ? activePlayers.get(playerId) : null;
    }

    /**
     * Get the number of active AI players
     *
     * @return Number of active players
     */
    public int getActivePlayerCount() {
        return activePlayers.size();
    }

    /**
     * Get all active AI players
     *
     * @return Map of all active players
     */
    public Map<UUID, AIPlayerEntity> getActivePlayers() {
        return new HashMap<>(activePlayers);
    }

    /**
     * Get all active AI player names
     *
     * @return List of active player names
     */
    public List<String> getActivePlayerNames() {
        return new ArrayList<>(playersByName.keySet());
    }

    /**
     * Check if an AI player exists
     *
     * @param name Player name
     * @return True if player exists
     */
    public boolean playerExists(String name) {
        return playersByName.containsKey(name);
    }

    /**
     * Register a new AI player (internal use)
     *
     * @param player The AI player entity to register
     */
    protected void registerPlayer(AIPlayerEntity player) {
        activePlayers.put(player.getUuid(), player);
        playersByName.put(player.getAIName(), player.getUuid());
        AILogger.info("Registered AI player: {} (UUID: {})", player.getAIName(), player.getUuid());
    }

    /**
     * Unregister an AI player (internal use)
     *
     * @param player The AI player entity to unregister
     */
    protected void unregisterPlayer(AIPlayerEntity player) {
        activePlayers.remove(player.getUuid());
        playersByName.remove(player.getAIName());
        AILogger.info("Unregistered AI player: {}", player.getAIName());
    }

    /**
     * Tick all AI players
     * This is currently not called - AI players tick automatically via ServerPlayerEntity
     * Keeping this for future use if we need manual tick control
     */
    public void tick() {
        // Currently not needed - ServerPlayerEntity ticks automatically
        // This method is here for future use if we need additional tick logic
    }

    /**
     * Get a summary of all active AI players
     *
     * @return String summary
     */
    public String getSummary() {
        if (activePlayers.isEmpty()) {
            return "No active AI players";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Active AI Players (%d):\n", activePlayers.size()));

        for (AIPlayerEntity player : activePlayers.values()) {
            sb.append(String.format("  - %s at %s (ticks: %d)\n",
                player.getAIName(),
                player.getPositionString(),
                player.getTickCounter()));
        }

        return sb.toString();
    }
}
