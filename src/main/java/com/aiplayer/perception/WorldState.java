package com.aiplayer.perception;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.function.Predicate;

/**
 * Represents the AI player's current understanding of the world.
 *
 * Contains all information the AI perceives about its environment:
 * - Own state (position, health, hunger, etc.)
 * - Nearby entities and players
 * - Visible blocks
 * - Inventory contents
 *
 * This is immutable - each update creates a new WorldState.
 */
public class WorldState {

    private final Vec3d playerPosition;
    private final Vec3d lookDirection;
    private final float health;
    private final float hunger;
    private final int experience;
    private final String dimension;

    private final List<EntityInfo> nearbyEntities;
    private final Map<BlockPos, BlockState> visibleBlocks;
    private final List<PlayerInfo> nearbyPlayers;

    private final long timestamp;

    // Constructor
    public WorldState(Vec3d playerPosition, Vec3d lookDirection, float health, float hunger,
                      int experience, String dimension, List<EntityInfo> nearbyEntities,
                      Map<BlockPos, BlockState> visibleBlocks, List<PlayerInfo> nearbyPlayers) {
        this.playerPosition = playerPosition;
        this.lookDirection = lookDirection;
        this.health = health;
        this.hunger = hunger;
        this.experience = experience;
        this.dimension = dimension;
        this.nearbyEntities = new ArrayList<>(nearbyEntities);
        this.visibleBlocks = new HashMap<>(visibleBlocks);
        this.nearbyPlayers = new ArrayList<>(nearbyPlayers);
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public Vec3d getPlayerPosition() {
        return playerPosition;
    }

    public Vec3d getLookDirection() {
        return lookDirection;
    }

    public float getHealth() {
        return health;
    }

    public float getHunger() {
        return hunger;
    }

    public int getExperience() {
        return experience;
    }

    public String getDimension() {
        return dimension;
    }

    public List<EntityInfo> getNearbyEntities() {
        return Collections.unmodifiableList(nearbyEntities);
    }

    public Map<BlockPos, BlockState> getVisibleBlocks() {
        return Collections.unmodifiableMap(visibleBlocks);
    }

    public List<PlayerInfo> getNearbyPlayers() {
        return Collections.unmodifiableList(nearbyPlayers);
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Query methods

    /**
     * Find the nearest entity matching the given predicate.
     */
    public Optional<EntityInfo> findNearestEntity(Predicate<EntityInfo> filter) {
        return nearbyEntities.stream()
            .filter(filter)
            .min(Comparator.comparingDouble(e ->
                playerPosition.distanceTo(e.getPosition())));
    }

    /**
     * Find all blocks of a specific type within visible range.
     */
    public List<BlockPos> findBlocksOfType(String blockId) {
        List<BlockPos> result = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockState> entry : visibleBlocks.entrySet()) {
            if (entry.getValue().getBlock().getTranslationKey().contains(blockId)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Calculate distance to a position.
     */
    public double distanceTo(BlockPos pos) {
        return playerPosition.distanceTo(Vec3d.ofCenter(pos));
    }

    /**
     * Check if a position is within visible range.
     */
    public boolean canSeePosition(BlockPos pos) {
        return visibleBlocks.containsKey(pos);
    }

    /**
     * Get block state at a position, if visible.
     */
    public Optional<BlockState> getBlockAt(BlockPos pos) {
        return Optional.ofNullable(visibleBlocks.get(pos));
    }

    /**
     * Simple data class for entity information.
     */
    public static class EntityInfo {
        private final UUID id;
        private final String type;
        private final Vec3d position;
        private final float health;
        private final boolean isHostile;

        public EntityInfo(UUID id, String type, Vec3d position, float health, boolean isHostile) {
            this.id = id;
            this.type = type;
            this.position = position;
            this.health = health;
            this.isHostile = isHostile;
        }

        public UUID getId() { return id; }
        public String getType() { return type; }
        public String getName() { return type; } // Alias for getType()
        public Vec3d getPosition() { return position; }
        public float getHealth() { return health; }
        public boolean isHostile() { return isHostile; }
    }

    /**
     * Simple data class for player information.
     */
    public static class PlayerInfo {
        private final UUID id;
        private final String name;
        private final Vec3d position;
        private final boolean isOp;

        public PlayerInfo(UUID id, String name, Vec3d position, boolean isOp) {
            this.id = id;
            this.name = name;
            this.position = position;
            this.isOp = isOp;
        }

        public UUID getId() { return id; }
        public String getName() { return name; }
        public Vec3d getPosition() { return position; }
        public boolean isOp() { return isOp; }
    }

    @Override
    public String toString() {
        return String.format("WorldState{pos=%.1f,%.1f,%.1f, health=%.1f, entities=%d, players=%d}",
            playerPosition.x, playerPosition.y, playerPosition.z,
            health, nearbyEntities.size(), nearbyPlayers.size());
    }
}
