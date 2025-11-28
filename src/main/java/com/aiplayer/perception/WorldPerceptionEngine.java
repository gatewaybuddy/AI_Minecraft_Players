package com.aiplayer.perception;

import com.aiplayer.core.AIPlayerEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * World Perception Engine - Gathers information about the world for the AI.
 *
 * This class is responsible for:
 * - Scanning nearby blocks
 * - Detecting entities and players
 * - Monitoring inventory state
 * - Building a WorldState snapshot
 *
 * The perception range can be configured to balance performance vs awareness.
 */
public class WorldPerceptionEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldPerceptionEngine.class);

    private final AIPlayerEntity player;

    // Perception ranges (in blocks)
    private static final double ENTITY_DETECTION_RANGE = 32.0;
    private static final int BLOCK_SCAN_RADIUS = 16;
    private static final int BLOCK_SCAN_HEIGHT = 8; // +/- vertical range

    public WorldPerceptionEngine(AIPlayerEntity player) {
        this.player = player;
    }

    /**
     * Perceive the world and create a WorldState snapshot.
     *
     * This is called periodically (every 0.5 seconds) by the AI brain.
     *
     * @return Current world state from AI's perspective
     */
    public WorldState perceiveWorld() {
        World world = player.getWorld();
        Vec3d playerPos = player.getPos();
        Vec3d lookDir = player.getRotationVector();

        // Get player stats
        float health = player.getHealth();
        float hunger = player.getHungerManager().getFoodLevel();
        int experience = player.experienceLevel;
        String dimension = world.getRegistryKey().getValue().toString();

        // Scan for entities
        List<WorldState.EntityInfo> nearbyEntities = detectEntities(world, playerPos);

        // Scan for players
        List<WorldState.PlayerInfo> nearbyPlayers = detectPlayers(world, playerPos);

        // Scan blocks (simplified for Phase 1)
        Map<BlockPos, BlockState> visibleBlocks = scanBlocks(world, playerPos);

        return new WorldState(
            playerPos,
            lookDir,
            health,
            hunger,
            experience,
            dimension,
            nearbyEntities,
            visibleBlocks,
            nearbyPlayers
        );
    }

    /**
     * Detect nearby entities.
     */
    private List<WorldState.EntityInfo> detectEntities(World world, Vec3d playerPos) {
        List<WorldState.EntityInfo> entities = new ArrayList<>();

        // Create bounding box for detection
        Box detectionBox = Box.of(playerPos, ENTITY_DETECTION_RANGE * 2, ENTITY_DETECTION_RANGE * 2, ENTITY_DETECTION_RANGE * 2);

        // Get all entities in range
        List<Entity> nearbyEntities = world.getOtherEntities(player, detectionBox);

        for (Entity entity : nearbyEntities) {
            // Skip non-living entities for Phase 1
            if (!(entity instanceof LivingEntity livingEntity)) {
                continue;
            }

            // Skip other players (handled separately)
            if (entity instanceof PlayerEntity) {
                continue;
            }

            WorldState.EntityInfo info = new WorldState.EntityInfo(
                entity.getUuid(),
                entity.getType().toString(),
                entity.getPos(),
                livingEntity.getHealth(),
                entity instanceof HostileEntity
            );

            entities.add(info);
        }

        return entities;
    }

    /**
     * Detect nearby players.
     */
    private List<WorldState.PlayerInfo> detectPlayers(World world, Vec3d playerPos) {
        List<WorldState.PlayerInfo> players = new ArrayList<>();

        // Get all players in the world
        List<? extends PlayerEntity> allPlayers = world.getPlayers();

        for (PlayerEntity otherPlayer : allPlayers) {
            // Skip self
            if (otherPlayer == player) {
                continue;
            }

            // Check if in range
            double distance = otherPlayer.getPos().distanceTo(playerPos);
            if (distance > ENTITY_DETECTION_RANGE) {
                continue;
            }

            WorldState.PlayerInfo info = new WorldState.PlayerInfo(
                otherPlayer.getUuid(),
                otherPlayer.getName().getString(),
                otherPlayer.getPos(),
                otherPlayer instanceof ServerPlayerEntity serverPlayer && serverPlayer.hasPermissionLevel(2)
            );

            players.add(info);
        }

        return players;
    }

    /**
     * Scan blocks in a radius around the player.
     *
     * For Phase 1, this does a simple scan.
     * Phase 2 will add intelligent scanning (only scan changed areas, etc.)
     */
    private Map<BlockPos, BlockState> scanBlocks(World world, Vec3d playerPos) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();

        BlockPos centerPos = BlockPos.ofFloored(playerPos);

        // Scan in a cube around the player
        for (int x = -BLOCK_SCAN_RADIUS; x <= BLOCK_SCAN_RADIUS; x++) {
            for (int y = -BLOCK_SCAN_HEIGHT; y <= BLOCK_SCAN_HEIGHT; y++) {
                for (int z = -BLOCK_SCAN_RADIUS; z <= BLOCK_SCAN_RADIUS; z++) {
                    BlockPos pos = centerPos.add(x, y, z);

                    // Only scan loaded chunks
                    if (!world.isChunkLoaded(pos)) {
                        continue;
                    }

                    BlockState state = world.getBlockState(pos);

                    // Store non-air blocks
                    if (!state.isAir()) {
                        blocks.put(pos, state);
                    }
                }
            }
        }

        return blocks;
    }

    /**
     * Get perception statistics (for debugging).
     */
    public String getPerceptionStats() {
        WorldState state = perceiveWorld();
        return String.format("Perception: %d entities, %d players, %d blocks",
            state.getNearbyEntities().size(),
            state.getNearbyPlayers().size(),
            state.getVisibleBlocks().size());
    }
}
