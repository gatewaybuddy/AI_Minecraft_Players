package com.aiplayer.action;

import com.aiplayer.core.AIPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Movement Controller - Handles AI player movement and navigation.
 *
 * Features:
 * - Path following with A* pathfinding
 * - Smooth movement interpolation
 * - Jump, sprint, sneak control
 * - Obstacle avoidance
 *
 * Phase 1: Basic movement
 * Phase 2: Pathfinding-based movement
 * Phase 3: Advanced movement (parkour, elytra, etc.)
 */
public class MovementController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovementController.class);

    private final AIPlayerEntity player;
    private final PathfindingEngine pathfinder;

    // Current path being followed
    private PathfindingEngine.Path currentPath;
    private int currentPathIndex;

    // Movement state
    private boolean isMoving;
    private Vec3d targetPosition;

    public MovementController(AIPlayerEntity player) {
        this.player = player;
        this.pathfinder = new PathfindingEngine();
        this.currentPathIndex = 0;
        this.isMoving = false;
    }

    /**
     * Move to a target position using pathfinding.
     *
     * @param target Target position
     * @return true if pathfinding succeeded, false if no path found
     */
    public boolean moveTo(BlockPos target) {
        World world = player.getWorld();
        BlockPos start = player.getBlockPos();

        LOGGER.info("[MOVEMENT] Starting pathfinding - From: {} To: {}", start, target);

        // Find path
        Optional<PathfindingEngine.Path> pathOpt = pathfinder.findPath(start, target, world);

        if (pathOpt.isEmpty()) {
            LOGGER.warn("[MOVEMENT] No path found from {} to {}", start, target);
            return false;
        }

        currentPath = pathOpt.get();
        currentPathIndex = 0;
        isMoving = true;

        LOGGER.info("[MOVEMENT] Path found - {} waypoints, distance: ~{} blocks",
            currentPath.getLength(),
            start.getSquaredDistance(target));
        LOGGER.debug("[MOVEMENT] Path waypoints: {}", currentPath.getPositions());
        return true;
    }

    /**
     * Move directly towards a position (no pathfinding).
     *
     * @param target Target position
     */
    public void moveTowardsDirect(Vec3d target) {
        LOGGER.info("[MOVEMENT] Starting direct movement (no pathfinding) - From: {} To: {}",
            player.getPos(), target);

        this.targetPosition = target;
        this.currentPath = null;
        this.isMoving = true;

        // Calculate direction and set AI movement
        Vec3d playerPos = player.getPos();
        double dx = target.x - playerPos.x;
        double dz = target.z - playerPos.z;

        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance > 0.1) {
            dx /= distance;
            dz /= distance;
            LOGGER.debug("[MOVEMENT] Setting movement direction - dx: {}, dz: {}, distance: {}",
                dx, dz, distance);
            player.setAIMovementDirection(dx, dz);
        } else {
            LOGGER.debug("[MOVEMENT] Already at target position");
        }
    }

    /**
     * Update movement (called every tick or periodically).
     */
    public void update() {
        if (!isMoving) {
            return;
        }

        LOGGER.debug("[MOVEMENT] Update - Path: {}, Direct: {}",
            (currentPath != null), (targetPosition != null));

        // Path following
        if (currentPath != null) {
            updatePathFollowing();
        }
        // Direct movement
        else if (targetPosition != null) {
            updateDirectMovement();
        }
    }

    /**
     * Update path following logic.
     */
    private void updatePathFollowing() {
        if (currentPath == null || currentPathIndex >= currentPath.getLength()) {
            LOGGER.debug("[MOVEMENT] Path following ended - Path null: {}, Index: {}/{}",
                (currentPath == null), currentPathIndex,
                (currentPath != null ? currentPath.getLength() : 0));
            stopMovement();
            return;
        }

        // Get current waypoint
        BlockPos waypoint = currentPath.getPositions().get(currentPathIndex);
        Vec3d waypointVec = Vec3d.ofCenter(waypoint);

        // Check if reached waypoint
        double distance = player.getPos().distanceTo(waypointVec);
        if (distance < 1.0) {
            // Move to next waypoint
            currentPathIndex++;
            LOGGER.debug("[MOVEMENT] Reached waypoint {} - Moving to next ({}/{})",
                waypoint, currentPathIndex, currentPath.getLength());

            if (currentPathIndex >= currentPath.getLength()) {
                // Path completed!
                LOGGER.info("[MOVEMENT] Path following completed - Reached destination");
                stopMovement();
                return;
            }

            waypoint = currentPath.getPositions().get(currentPathIndex);
            waypointVec = Vec3d.ofCenter(waypoint);
        }

        // Move towards current waypoint
        Vec3d playerPos = player.getPos();
        double dx = waypointVec.x - playerPos.x;
        double dz = waypointVec.z - playerPos.z;

        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 0.1) {
            dx /= dist;
            dz /= dist;
            LOGGER.debug("[MOVEMENT] Moving towards waypoint {} - Direction: ({}, {}), Distance: {}",
                waypoint, dx, dz, dist);
            player.setAIMovementDirection(dx, dz);

            // Jump if next waypoint is above us
            if (waypoint.getY() > player.getBlockY()) {
                LOGGER.debug("[MOVEMENT] Jumping - waypoint Y {} > player Y {}", waypoint.getY(), player.getBlockY());
                player.jump();
            }
        }
    }

    /**
     * Update direct movement (no pathfinding).
     */
    private void updateDirectMovement() {
        if (targetPosition == null) {
            LOGGER.debug("[MOVEMENT] Direct movement ended - target is null");
            stopMovement();
            return;
        }

        Vec3d playerPos = player.getPos();
        double distance = playerPos.distanceTo(targetPosition);

        // Reached target
        if (distance < 1.0) {
            LOGGER.info("[MOVEMENT] Direct movement completed - Reached target {}", targetPosition);
            stopMovement();
            return;
        }

        // Keep moving towards target
        double dx = targetPosition.x - playerPos.x;
        double dz = targetPosition.z - playerPos.z;

        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 0.1) {
            dx /= dist;
            dz /= dist;
            LOGGER.debug("[MOVEMENT] Moving towards target {} - Direction: ({}, {}), Distance: {}",
                targetPosition, dx, dz, dist);
            player.setAIMovementDirection(dx, dz);
        }
    }

    /**
     * Stop all movement.
     */
    public void stopMovement() {
        LOGGER.info("[MOVEMENT] Stopping movement");
        isMoving = false;
        currentPath = null;
        targetPosition = null;
        player.stopMovement();
        LOGGER.debug("[MOVEMENT] Movement stopped");
    }

    /**
     * Check if currently moving.
     */
    public boolean isMoving() {
        return isMoving;
    }

    /**
     * Get current path (if following one).
     */
    public Optional<PathfindingEngine.Path> getCurrentPath() {
        return Optional.ofNullable(currentPath);
    }

    /**
     * Get remaining waypoints in current path.
     */
    public List<BlockPos> getRemainingWaypoints() {
        if (currentPath == null || currentPathIndex >= currentPath.getLength()) {
            return List.of();
        }
        return currentPath.getPositions().subList(currentPathIndex, currentPath.getLength());
    }

    /**
     * Sprint towards target (increases speed).
     */
    public void setSprinting(boolean sprinting) {
        player.setSprinting(sprinting);
    }

    /**
     * Sneak (reduces speed, prevents falling off edges).
     */
    public void setSneaking(boolean sneaking) {
        player.setSneaking(sneaking);
    }
}
