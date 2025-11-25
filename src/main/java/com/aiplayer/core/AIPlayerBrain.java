package com.aiplayer.core;

import com.aiplayer.llm.LLMProvider;
import com.aiplayer.memory.Memory;
import com.aiplayer.memory.MemorySystem;
import com.aiplayer.perception.WorldState;
import com.aiplayer.planning.Goal;
import com.aiplayer.planning.PlanningEngine;
import com.aiplayer.skills.SkillLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Random;

/**
 * The "brain" of an AI player - handles decision making and behavior.
 *
 * This class implements the main AI loop:
 * 1. Perceive the world state
 * 2. Decide what to do based on goals and observations
 * 3. Execute actions through controllers
 *
 * Phase 3 Update: Now uses intelligent planning with:
 * - Memory system (episodic + semantic + working memory)
 * - LLM-based goal planning (ReAct framework)
 * - Skill library (learned behaviors)
 * - Goal-based decision making
 *
 * Falls back to simple random walk if LLM is unavailable.
 */
public class AIPlayerBrain {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPlayerBrain.class);

    private final AIPlayerEntity player;
    private final Random random;

    // Phase 3: Intelligence systems
    private final MemorySystem memorySystem;
    private final PlanningEngine planningEngine;
    private final SkillLibrary skillLibrary;
    private final boolean intelligentMode;

    // Simple state for fallback mode
    private Vec3dSimple currentMovementTarget;
    private int ticksSinceLastDecision;
    private final int decisionIntervalTicks; // Configurable decision interval (default 8 = 400ms)

    // Memory cleanup tracking
    private int ticksSinceLastCleanup = 0;
    private static final int CLEANUP_INTERVAL_TICKS = 1200; // Every minute (60 seconds * 20 ticks)

    // Stuck detection
    private net.minecraft.util.math.Vec3d lastPosition;
    private int ticksStuck = 0;
    private static final int MAX_STUCK_TICKS = 60; // 3 seconds
    private static final double STUCK_DISTANCE_THRESHOLD = 0.5; // Moved less than 0.5 blocks

    /**
     * Create AI brain with intelligent planning (Phase 3+).
     */
    public AIPlayerBrain(AIPlayerEntity player, LLMProvider llmProvider) {
        this.player = player;
        this.random = new Random();
        this.ticksSinceLastDecision = 0;

        // Load decision interval from config
        this.decisionIntervalTicks = com.aiplayer.AIPlayerMod.getConfig()
            .getBehavior()
            .getBrainDecisionIntervalTicks();

        LOGGER.info("Brain decision interval set to {} ticks (~{}ms)",
            decisionIntervalTicks, decisionIntervalTicks * 50);

        // Initialize intelligence systems
        this.memorySystem = new MemorySystem();
        this.skillLibrary = new SkillLibrary();

        // Check if LLM is available
        if (llmProvider != null && llmProvider.isAvailable()) {
            this.planningEngine = new PlanningEngine(llmProvider, memorySystem);
            this.intelligentMode = true;
            LOGGER.info("AI brain initialized in INTELLIGENT mode with {} ({}))",
                llmProvider.getProviderName(), llmProvider.getModelName());
        } else {
            this.planningEngine = null;
            this.intelligentMode = false;
            LOGGER.warn("AI brain initialized in SIMPLE mode (LLM unavailable)");
        }
    }

    /**
     * Create AI brain without LLM (simple mode).
     */
    public AIPlayerBrain(AIPlayerEntity player) {
        this(player, null);
    }

    /**
     * Main update loop - called every tick (or every 0.5 seconds).
     *
     * @param worldState Current perception of the world
     */
    public void update(WorldState worldState) {
        ticksSinceLastDecision++;

        // Only make decisions periodically to reduce CPU usage (configurable interval)
        if (ticksSinceLastDecision < decisionIntervalTicks) {
            return;
        }

        LOGGER.info("[BRAIN] {} - Update cycle starting | Health: {}, Hunger: {}, Pos: {}",
            player.getName().getString(),
            worldState.getHealth(),
            worldState.getHunger(),
            worldState.getPlayerPosition());

        ticksSinceLastDecision = 0;

        try {
            // Store perception in memory
            int memoriesStored = memorySystem.getWorkingMemory().size();
            storePerceptionMemories(worldState);
            int newMemories = memorySystem.getWorkingMemory().size() - memoriesStored;
            LOGGER.debug("[BRAIN] Stored {} new perception memories", newMemories);

            // Use intelligent planning if available, otherwise fall back to simple mode
            if (intelligentMode) {
                LOGGER.debug("[BRAIN] Using INTELLIGENT mode");
                makeIntelligentDecision(worldState);
            } else {
                LOGGER.debug("[BRAIN] Using SIMPLE mode");
                makeSimpleDecision(worldState);
            }

            // Periodic memory cleanup (separate counter!)
            ticksSinceLastCleanup++;
            if (ticksSinceLastCleanup >= CLEANUP_INTERVAL_TICKS) {
                LOGGER.debug("[BRAIN] Running periodic memory cleanup");
                memorySystem.cleanup();
                ticksSinceLastCleanup = 0;
            }
        } catch (Exception e) {
            LOGGER.error("[BRAIN] Error in AI brain update for {}", player.getName().getString(), e);
        }
    }

    /**
     * Phase 3: Intelligent decision making using LLM planning.
     */
    private void makeIntelligentDecision(WorldState worldState) {
        LOGGER.debug("[BRAIN] Updating planning engine...");
        // Update planning engine
        planningEngine.update(worldState);

        // Get current goal
        Optional<Goal> currentGoal = planningEngine.getCurrentGoal();

        if (currentGoal.isPresent()) {
            Goal goal = currentGoal.get();
            LOGGER.info("[BRAIN] Executing goal: Type={}, Status={}, Desc='{}'",
                goal.getType(), goal.getStatus(), goal.getDescription());
            executeGoal(goal, worldState);
        } else {
            // No active goal - request new plan from LLM
            LOGGER.warn("[BRAIN] No active goals - requesting new plan from LLM");
            planningEngine.replan(worldState);

            // Fall back to simple behavior while waiting
            LOGGER.debug("[BRAIN] Falling back to simple behavior while waiting for plan");
            makeSimpleDecision(worldState);
        }
    }

    /**
     * Execute actions for current goal.
     *
     * Phase 3: Basic goal execution
     * Phase 4: Will use proper task decomposition and skill execution
     */
    private void executeGoal(Goal goal, WorldState worldState) {
        LOGGER.debug("[BRAIN] executeGoal() called - Type: {}", goal.getType());

        // Update goal status
        if (goal.getStatus() == Goal.GoalStatus.PENDING) {
            goal.setStatus(Goal.GoalStatus.IN_PROGRESS);
            LOGGER.debug("[BRAIN] Goal status changed: PENDING → IN_PROGRESS");
        }

        // For Phase 3, we'll execute based on goal type
        // Phase 4 will decompose into proper tasks
        switch (goal.getType()) {
            case SURVIVAL:
                LOGGER.info("[BRAIN] Executing SURVIVAL goal");
                executeSurvivalGoal(worldState);
                break;

            case RESOURCE_GATHERING:
                LOGGER.info("[BRAIN] Executing RESOURCE_GATHERING goal: {}", goal.getDescription());
                executeResourceGoal(goal, worldState);
                break;

            case EXPLORATION:
                LOGGER.info("[BRAIN] Executing EXPLORATION goal");
                executeExplorationGoal(worldState);
                break;

            case COMBAT:
                LOGGER.info("[BRAIN] Executing COMBAT goal");
                executeCombatGoal(worldState);
                break;

            case BUILD:
                LOGGER.info("[BRAIN] Executing BUILD goal");
                executeBuildGoal(worldState);
                break;

            case SOCIAL:
                LOGGER.info("[BRAIN] Executing SOCIAL goal: {}", goal.getDescription());
                executeSocialGoal(goal, worldState);
                break;

            default:
                LOGGER.warn("[BRAIN] Unknown goal type: {} - falling back to simple behavior", goal.getType());
                makeSimpleDecision(worldState);
                break;
        }
    }

    /**
     * Execute survival goal (find food, heal, etc.).
     */
    private void executeSurvivalGoal(WorldState worldState) {
        // CRITICAL: Check if under attack (low health)
        if (worldState.getHealth() < 10 && worldState.getHealth() > 0) {
            LOGGER.warn("[BRAIN] SURVIVAL MODE: Low health detected - seeking safety");

            // Check for nearby hostiles
            Optional<WorldState.EntityInfo> nearestHostile = worldState.findNearestEntity(
                e -> e.getName().contains("zombie") || e.getName().contains("skeleton") ||
                     e.getName().contains("creeper") || e.getName().contains("spider") ||
                     e.getName().contains("player")
            );

            if (nearestHostile.isPresent()) {
                // Extract once to avoid multiple .get() calls
                WorldState.EntityInfo hostile = nearestHostile.get();

                // FLEE from hostile
                Vec3dSimple hostilePos = new Vec3dSimple(
                    hostile.getPosition().x,
                    hostile.getPosition().y,
                    hostile.getPosition().z
                );

                // Move AWAY from hostile (opposite direction)
                double dx = worldState.getPlayerPosition().x - hostilePos.x;
                double dz = worldState.getPlayerPosition().z - hostilePos.z;
                double distance = Math.sqrt(dx * dx + dz * dz);

                if (distance > 0.1) {
                    dx /= distance;
                    dz /= distance;

                    // Set flee target 20 blocks away
                    currentMovementTarget = new Vec3dSimple(
                        worldState.getPlayerPosition().x + dx * 20,
                        worldState.getPlayerPosition().y,
                        worldState.getPlayerPosition().z + dz * 20
                    );

                    LOGGER.warn("[BRAIN] FLEEING from {} at distance {}", hostile.getName(), distance);

                    memorySystem.store(new Memory(
                        Memory.MemoryType.OBSERVATION,
                        "FLEEING from " + hostile.getName() + " - health critical!",
                        1.0
                    ));
                }
            } else {
                // No visible threat - explore to find safety
                LOGGER.info("[BRAIN] Low health but no visible threat - exploring for safety");
                executeExplorationGoal(worldState);
            }
            return;
        }

        // Check if we need food
        if (worldState.getHunger() < 10) {
            // Look for food sources
            Optional<WorldState.EntityInfo> nearestAnimal = worldState.findNearestEntity(
                e -> e.getName().contains("cow") || e.getName().contains("pig") || e.getName().contains("sheep")
            );

            if (nearestAnimal.isPresent()) {
                // Extract once to avoid multiple .get() calls
                WorldState.EntityInfo animal = nearestAnimal.get();

                // Move towards animal (using ActionController would be better)
                Vec3dSimple target = new Vec3dSimple(
                    animal.getPosition().x,
                    animal.getPosition().y,
                    animal.getPosition().z
                );
                currentMovementTarget = target;
                moveTowardsTarget(worldState);

                memorySystem.store(new Memory(
                    Memory.MemoryType.OBSERVATION,
                    "Moving towards " + animal.getName() + " for food",
                    0.6
                ));
            } else {
                // Explore to find food
                executeExplorationGoal(worldState);
            }
        } else {
            // Not hungry, not hurt - just explore
            executeExplorationGoal(worldState);
        }
    }

    /**
     * Execute resource gathering goal.
     */
    private void executeResourceGoal(Goal goal, WorldState worldState) {
        String goalDesc = goal.getDescription().toLowerCase();

        LOGGER.info("[BRAIN] Resource gathering - Goal: {}", goalDesc);

        // Determine what resource to gather from goal description
        String targetResource = null;
        if (goalDesc.contains("wood") || goalDesc.contains("log")) {
            targetResource = "log";
        } else if (goalDesc.contains("coal")) {
            targetResource = "coal_ore";
        } else if (goalDesc.contains("iron")) {
            targetResource = "iron_ore";
        } else if (goalDesc.contains("stone") || goalDesc.contains("cobblestone")) {
            targetResource = "stone";
        } else if (goalDesc.contains("diamond")) {
            targetResource = "diamond_ore";
        } else if (goalDesc.contains("gold")) {
            targetResource = "gold_ore";
        }

        // If no specific resource identified, look for any valuable resource
        if (targetResource == null) {
            LOGGER.info("[BRAIN] No specific resource in goal, searching for any valuable blocks");
            targetResource = "any_valuable";
        }

        // Search visible blocks for target resource
        net.minecraft.util.math.BlockPos targetBlock = findNearestResourceBlock(worldState, targetResource);

        if (targetBlock != null) {
            LOGGER.info("[BRAIN] Found {} at {}", targetResource, targetBlock);

            // Check distance
            double distance = player.getPos().distanceTo(targetBlock.toCenterPos());

            if (distance > 6.0) {
                // Too far - move closer
                LOGGER.info("[BRAIN] Resource too far ({} blocks), moving closer", String.format("%.1f", distance));
                currentMovementTarget = new Vec3dSimple(
                    targetBlock.getX(),
                    targetBlock.getY(),
                    targetBlock.getZ()
                );
                moveTowardsTarget(worldState);
            } else {
                // In range - mine it!
                LOGGER.info("[BRAIN] Mining {} at distance {}", targetResource, String.format("%.1f", distance));

                // Stop movement while mining
                player.stopMovement();

                // Check inventory before mining
                final String finalTargetResource = targetResource; // Make final for lambda
                int itemCountBefore = getResourceCount(finalTargetResource);

                // Use MiningController to mine the block
                player.getActionController().mining().mineBlock(targetBlock)
                    .thenAccept(result -> {
                        if (result.isSuccess()) {
                            LOGGER.info("[BRAIN] Successfully mined block: {}", result.getMessage());

                            // Check inventory after mining
                            int itemCountAfter = getResourceCount(finalTargetResource);
                            int gained = itemCountAfter - itemCountBefore;

                            if (gained > 0) {
                                LOGGER.info("[BRAIN] Gained {} items! Total: {}", gained, itemCountAfter);

                                // Store memory of successful mining
                                memorySystem.store(new Memory(
                                    Memory.MemoryType.OBSERVATION,
                                    "Successfully mined " + finalTargetResource + " - now have " + itemCountAfter,
                                    0.7
                                ));
                            }
                        } else {
                            LOGGER.warn("[BRAIN] Failed to mine block: {}", result.getMessage());
                        }
                    })
                    .exceptionally(e -> {
                        LOGGER.error("[BRAIN] Mining error: {}", e.getMessage());
                        return null;
                    });
            }
        } else {
            // No resource found nearby - explore to find some
            LOGGER.info("[BRAIN] No {} found nearby, exploring to find resources", targetResource);
            executeExplorationGoal(worldState);
        }
    }

    /**
     * Find nearest resource block of specified type.
     */
    private net.minecraft.util.math.BlockPos findNearestResourceBlock(WorldState worldState, String resourceType) {
        net.minecraft.util.math.BlockPos nearestPos = null;
        double nearestDistance = Double.MAX_VALUE;

        for (java.util.Map.Entry<net.minecraft.util.math.BlockPos, net.minecraft.block.BlockState> entry :
                worldState.getVisibleBlocks().entrySet()) {
            net.minecraft.util.math.BlockPos pos = entry.getKey();
            net.minecraft.block.BlockState state = entry.getValue();
            String blockName = state.getBlock().getName().getString().toLowerCase();

            boolean matches = false;

            if (resourceType.equals("any_valuable")) {
                // Look for any valuable resource
                matches = blockName.contains("log") || blockName.contains("oak") ||
                         blockName.contains("coal") || blockName.contains("iron") ||
                         blockName.contains("diamond") || blockName.contains("gold");
            } else if (resourceType.equals("log")) {
                // Match any log type (oak, birch, spruce, etc.)
                matches = blockName.contains("log") || blockName.contains("oak_wood") ||
                         blockName.contains("birch_wood") || blockName.contains("spruce_wood");
            } else {
                // Direct match
                matches = blockName.contains(resourceType);
            }

            if (matches) {
                double distance = player.getPos().distanceTo(pos.toCenterPos());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestPos = pos;
                }
            }
        }

        return nearestPos;
    }

    /**
     * Get count of a resource in inventory.
     */
    private int getResourceCount(String resourceType) {
        if (resourceType.equals("log")) {
            // Count all log types
            return player.getActionController().inventory().countItem("log") +
                   player.getActionController().inventory().countItem("wood");
        } else if (resourceType.equals("coal_ore")) {
            return player.getActionController().inventory().countItem("coal");
        } else if (resourceType.equals("iron_ore")) {
            return player.getActionController().inventory().countItem("iron");
        } else if (resourceType.equals("stone")) {
            return player.getActionController().inventory().countItem("cobblestone") +
                   player.getActionController().inventory().countItem("stone");
        } else {
            return player.getActionController().inventory().countItem(resourceType);
        }
    }

    /**
     * Execute exploration goal.
     */
    private void executeExplorationGoal(WorldState worldState) {
        // Use simple random walk for exploration
        makeSimpleDecision(worldState);
    }

    /**
     * Execute combat goal.
     */
    private void executeCombatGoal(WorldState worldState) {
        // Find hostile mobs
        Optional<WorldState.EntityInfo> nearestHostile = worldState.findNearestEntity(
            e -> e.getName().contains("zombie") || e.getName().contains("skeleton") ||
                 e.getName().contains("creeper") || e.getName().contains("spider")
        );

        if (nearestHostile.isPresent()) {
            // Extract once to avoid multiple .get() calls
            WorldState.EntityInfo hostile = nearestHostile.get();

            // Engage (simplified - Phase 4 will use CombatController)
            Vec3dSimple target = new Vec3dSimple(
                hostile.getPosition().x,
                hostile.getPosition().y,
                hostile.getPosition().z
            );
            currentMovementTarget = target;
            moveTowardsTarget(worldState);
        } else {
            // No hostiles - switch to exploration
            executeExplorationGoal(worldState);
        }
    }

    /**
     * Execute build goal.
     */
    private void executeBuildGoal(WorldState worldState) {
        // Building will be implemented in Phase 4
        makeSimpleDecision(worldState);
    }

    /**
     * Execute social goal (follow player, interact, etc.).
     */
    private void executeSocialGoal(Goal goal, WorldState worldState) {
        String goalDesc = goal.getDescription().toLowerCase();

        // Check if this is a "follow" goal
        if (goalDesc.contains("follow")) {
            // Extract player name from goal description (e.g., "Follow ColoradoFeingold")
            Optional<String> requestedBy = goal.getRequestedBy();

            if (requestedBy.isPresent()) {
                String targetPlayerName = requestedBy.get();

                // Find the target player in nearby players (not entities!)
                Optional<WorldState.PlayerInfo> targetPlayer = worldState.getNearbyPlayers().stream()
                    .filter(p -> p.getName().equalsIgnoreCase(targetPlayerName))
                    .findFirst();

                if (targetPlayer.isPresent()) {
                    WorldState.PlayerInfo target = targetPlayer.get();
                    Vec3dSimple targetPos = new Vec3dSimple(
                        target.getPosition().x,
                        target.getPosition().y,
                        target.getPosition().z
                    );

                    // Calculate distance to player
                    double dx = targetPos.x - worldState.getPlayerPosition().x;
                    double dz = targetPos.z - worldState.getPlayerPosition().z;
                    double distance = Math.sqrt(dx * dx + dz * dz);

                    // Follow if they're more than 3 blocks away
                    if (distance > 3.0) {
                        currentMovementTarget = targetPos;
                        moveTowardsTarget(worldState);
                        LOGGER.info("[BRAIN] Following {} - Distance: {} blocks", targetPlayerName, (int)distance);
                    } else {
                        // Close enough - just stay near them
                        player.stopMovement();
                        LOGGER.debug("[BRAIN] Close to {} - staying nearby", targetPlayerName);
                    }
                } else {
                    LOGGER.warn("[BRAIN] Cannot find player {} to follow - {} players nearby",
                        targetPlayerName, worldState.getNearbyPlayers().size());
                    // Don't explore - just stop moving
                    player.stopMovement();
                }
            } else {
                LOGGER.warn("[BRAIN] No target specified for follow goal");
                executeExplorationGoal(worldState);
            }
        } else {
            // Other social goals - implement as needed
            LOGGER.info("[BRAIN] Generic social goal - staying idle");
            player.stopMovement();
        }
    }

    /**
     * Store important observations in memory.
     */
    private void storePerceptionMemories(WorldState worldState) {
        // Store critical health warning (being attacked!)
        if (worldState.getHealth() < 10 && worldState.getHealth() > 0) {
            memorySystem.store(new Memory(
                Memory.MemoryType.OBSERVATION,
                String.format("CRITICAL: Low health: %.1f/20 - May be under attack!", worldState.getHealth()),
                1.0 // Maximum importance
            ));
            LOGGER.warn("[BRAIN] CRITICAL HEALTH: {}/20 - Player may be under attack!", worldState.getHealth());
        } else if (worldState.getHealth() < 15) {
            memorySystem.store(new Memory(
                Memory.MemoryType.OBSERVATION,
                String.format("Low health: %.1f/20", worldState.getHealth()),
                0.9 // High importance
            ));
        }

        // Store low hunger warning
        if (worldState.getHunger() < 6) {
            memorySystem.store(new Memory(
                Memory.MemoryType.OBSERVATION,
                String.format("Low hunger: %.1f/20", worldState.getHunger()),
                0.8
            ));
        }

        // Store nearby hostile mobs with high priority
        worldState.getNearbyEntities().stream()
            .filter(e -> e.getName().contains("zombie") || e.getName().contains("skeleton") ||
                        e.getName().contains("creeper") || e.getName().contains("spider"))
            .forEach(hostile -> {
                double distance = worldState.getPlayerPosition().distanceTo(hostile.getPosition());
                memorySystem.store(new Memory(
                    Memory.MemoryType.OBSERVATION,
                    String.format("DANGER: Hostile %s nearby (%.1f blocks)",
                        hostile.getName(), distance),
                    0.9 // Very high importance
                ));
                LOGGER.warn("[BRAIN] HOSTILE DETECTED: {} at {} blocks", hostile.getName(), distance);
            });
    }

    /**
     * Phase 1 simple decision making: Random walk with obstacle avoidance.
     */
    private void makeSimpleDecision(WorldState worldState) {
        LOGGER.info("[BRAIN] Using simple decision making (random walk)");

        // If we don't have a target or reached it, pick a new random target
        if (currentMovementTarget == null || hasReachedTarget(worldState)) {
            LOGGER.debug("[BRAIN] No movement target or reached target - picking new target");
            pickRandomMovementTarget(worldState);
        }

        // Move towards target
        if (currentMovementTarget != null) {
            LOGGER.debug("[BRAIN] Moving towards target: {}", currentMovementTarget);
            moveTowardsTarget(worldState);
        } else {
            LOGGER.warn("[BRAIN] No movement target available after picking");
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

        LOGGER.info("[BRAIN] Picked new random target: {} (distance: {} blocks, angle: {}°)",
            currentMovementTarget, (int)distance, (int)Math.toDegrees(angle));
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

        // Use larger threshold to prevent back-and-forth oscillation
        boolean reached = distance < 3.0; // Within 3 blocks

        if (reached) {
            LOGGER.info("[BRAIN] Reached target - Distance: {} blocks", distance);
        }

        return reached;
    }

    /**
     * Move towards the current target.
     */
    private void moveTowardsTarget(WorldState worldState) {
        // Stuck detection - check if we've moved since last tick
        net.minecraft.util.math.Vec3d currentPos = worldState.getPlayerPosition();

        if (lastPosition != null) {
            double movedDistance = Math.sqrt(
                Math.pow(currentPos.x - lastPosition.x, 2) +
                Math.pow(currentPos.z - lastPosition.z, 2)
            );

            if (movedDistance < STUCK_DISTANCE_THRESHOLD) {
                ticksStuck++;
                if (ticksStuck >= MAX_STUCK_TICKS) {
                    LOGGER.warn("[BRAIN] STUCK DETECTED! Haven't moved {} blocks in {} ticks. Picking new target.",
                        STUCK_DISTANCE_THRESHOLD, ticksStuck);
                    currentMovementTarget = null; // Force new target selection
                    ticksStuck = 0;
                    player.stopMovement();
                    return;
                }
            } else {
                // We're moving, reset stuck counter
                ticksStuck = 0;
            }
        }

        lastPosition = currentPos;

        // Calculate direction to target
        double dx = currentMovementTarget.x - currentPos.x;
        double dz = currentMovementTarget.z - currentPos.z;

        double distance = Math.sqrt(dx * dx + dz * dz);

        LOGGER.debug("[BRAIN] Moving towards target - Current pos: {}, Target: {}, Distance: {}",
            currentPos, currentMovementTarget, distance);

        if (distance < 0.1) {
            LOGGER.debug("[BRAIN] Already at target - distance too small: {}", distance);
            return; // Already there
        }

        // Normalize direction
        dx /= distance;
        dz /= distance;

        LOGGER.info("[BRAIN] Setting movement direction - dx: {}, dz: {} (distance: {} blocks)",
            dx, dz, (int)distance);

        // Tell the player entity to move in this direction
        player.setAIMovementDirection(dx, dz);
    }

    /**
     * Get current goal description (for status display).
     */
    public String getCurrentGoalDescription() {
        if (intelligentMode && planningEngine != null) {
            Optional<Goal> currentGoal = planningEngine.getCurrentGoal();
            if (currentGoal.isPresent()) {
                return currentGoal.get().getDescription();
            }
        }

        if (currentMovementTarget == null) {
            return "Idle";
        }
        return String.format("Walking to %.1f, %.1f, %.1f",
            currentMovementTarget.x, currentMovementTarget.y, currentMovementTarget.z);
    }

    /**
     * Get memory system (for debugging/commands).
     */
    public MemorySystem getMemorySystem() {
        return memorySystem;
    }

    /**
     * Get skill library (for debugging/commands).
     */
    public SkillLibrary getSkillLibrary() {
        return skillLibrary;
    }

    /**
     * Get planning engine (for debugging/commands).
     */
    public PlanningEngine getPlanningEngine() {
        return planningEngine;
    }

    /**
     * Check if in intelligent mode.
     */
    public boolean isIntelligentMode() {
        return intelligentMode;
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
