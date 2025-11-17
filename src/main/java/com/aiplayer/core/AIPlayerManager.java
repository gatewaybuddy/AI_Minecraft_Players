package com.aiplayer.core;

import com.aiplayer.util.AILogger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
     * Spawn a new AI player
     *
     * @param name Name for the AI player
     * @return The spawned AI player entity
     */
    public AIPlayerEntity spawnPlayer(String name) {
        // TODO: Implement in Phase 1, Task 1.5
        AILogger.info("Spawning AI player: {}", name);
        throw new UnsupportedOperationException("AI player spawning not yet implemented - coming in Phase 1!");
    }

    /**
     * Despawn an AI player
     *
     * @param playerId UUID of the player to despawn
     */
    public void despawnPlayer(UUID playerId) {
        AIPlayerEntity player = activePlayers.remove(playerId);
        if (player != null) {
            playersByName.remove(player.getName());
            AILogger.info("Despawned AI player: {}", player.getName());
        }
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
        playersByName.put(player.getName(), player.getUuid());
        AILogger.info("Registered AI player: {} (UUID: {})", player.getName(), player.getUuid());
    }

    /**
     * Tick all AI players
     * Called from server tick
     */
    public void tick() {
        for (AIPlayerEntity player : activePlayers.values()) {
            try {
                player.tick();
            } catch (Exception e) {
                AILogger.logError(player.getName(), "Error during tick", e);
            }
        }
    }
}
