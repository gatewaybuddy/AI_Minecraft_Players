package com.aiplayer.core;

import com.aiplayer.perception.WorldState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * The "brain" of an AI player - handles decision making and behavior.
 *
 * This class implements the main AI loop:
 * 1. Perceive the world state
 * 2. Decide what to do based on goals and observations
 * 3. Execute actions through controllers
 *
 * For Phase 1, this implements simple random movement.
 * Later phases will add:
 * - Goal-based planning (Phase 3)
 * - LLM integration for high-level decisions (Phase 3)
 * - Memory system integration (Phase 3)
 * - Natural language understanding (Phase 4)
 */
public class AIPlayerBrain {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPlayerBrain.class);

    private final AIPlayerEntity player;
    private final Random random;

    // Simple state for Phase 1
    private Vec3dSimple currentMovementTarget;
    private int ticksSinceLastDecision;
    private static final int DECISION_INTERVAL_TICKS = 20; // Decide every second

    public AIPlayerBrain(AIPlayerEntity player) {
        this.player = player;
        this.random = new Random();
        this.ticksSinceLastDecision = 0;
    }

    /**
     * Main update loop - called every tick (or every 0.5 seconds).
     *
     * @param worldState Current perception of the world
     */
    public void update(WorldState worldState) {
        ticksSinceLastDecision++;

        // Only make decisions periodically to reduce CPU usage
        if (ticksSinceLastDecision < DECISION_INTERVAL_TICKS) {
            return;
        }

        ticksSinceLastDecision = 0;

        try {
            // Phase 1: Simple random walk behavior
            makeSimpleDecision(worldState);
        } catch (Exception e) {
            LOGGER.error("Error in AI brain update for {}", player.getName().getString(), e);
        }
    }

    /**
     * Phase 1 simple decision making: Random walk with obstacle avoidance.
     */
    private void makeSimpleDecision(WorldState worldState) {
        // If we don't have a target or reached it, pick a new random target
        if (currentMovementTarget == null || hasReachedTarget(worldState)) {
            pickRandomMovementTarget(worldState);
        }

        // Move towards target
        if (currentMovementTarget != null) {
            moveTowardsTarget(worldState);
        }
    }

    /**
     * Pick a random position nearby to move to.
     */
    private void pickRandomMovementTarget(WorldState worldState) {
        // Pick a point 5-15 blocks away in a random direction
        double distance = 5 + random.nextDouble() * 10;
        double angle = random.nextDouble() * Math.PI * 2;

        double targetX = worldState.getPlayerPosition().x + Math.cos(angle) * distance;
        double targetY = worldState.getPlayerPosition().y;
        double targetZ = worldState.getPlayerPosition().z + Math.sin(angle) * distance;

        currentMovementTarget = new Vec3dSimple(targetX, targetY, targetZ);

        LOGGER.debug("AI {} picking new target: {}", player.getName().getString(), currentMovementTarget);
    }

    /**
     * Check if we've reached the current target.
     */
    private boolean hasReachedTarget(WorldState worldState) {
        if (currentMovementTarget == null) {
            return true;
        }

        double distance = Math.sqrt(
            Math.pow(worldState.getPlayerPosition().x - currentMovementTarget.x, 2) +
            Math.pow(worldState.getPlayerPosition().z - currentMovementTarget.z, 2)
        );

        return distance < 2.0; // Within 2 blocks
    }

    /**
     * Move towards the current target.
     */
    private void moveTowardsTarget(WorldState worldState) {
        // Calculate direction to target
        double dx = currentMovementTarget.x - worldState.getPlayerPosition().x;
        double dz = currentMovementTarget.z - worldState.getPlayerPosition().z;

        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 0.1) {
            return; // Already there
        }

        // Normalize direction
        dx /= distance;
        dz /= distance;

        // Tell the player entity to move in this direction
        player.setAIMovementDirection(dx, dz);
    }

    /**
     * Get current goal description (for status display).
     */
    public String getCurrentGoalDescription() {
        if (currentMovementTarget == null) {
            return "Idle";
        }
        return String.format("Walking to %.1f, %.1f, %.1f",
            currentMovementTarget.x, currentMovementTarget.y, currentMovementTarget.z);
    }

    /**
     * Simple 3D vector class to avoid dependency issues in early implementation.
     * Will use proper Vec3d later.
     */
    private static class Vec3dSimple {
        final double x, y, z;

        Vec3dSimple(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public String toString() {
            return String.format("(%.1f, %.1f, %.1f)", x, y, z);
        }
    }
}
