package com.aiplayer.core;

import com.aiplayer.action.ActionController;
import com.aiplayer.knowledge.WorldKnowledge;
import com.aiplayer.llm.LLMProvider;
import com.aiplayer.memory.Memory;
import com.aiplayer.memory.MemorySystem;
import com.aiplayer.perception.WorldState;
import com.aiplayer.planning.Goal;
import com.aiplayer.planning.PlanningEngine;
import com.aiplayer.skills.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
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
 * Phase 3: Intelligent planning with memory, LLM-based goals, and skill library
 * Phase 4: Natural language communication and chat integration
 * Phase 5: Advanced AI with self-improvement capabilities:
 * - Skill generation from experiences (success and failure)
 * - World knowledge tracking (landmarks, resources, dangers)
 * - Experience-based learning and adaptation
 * - Skill execution and refinement
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

    // Phase 5: Advanced AI systems
    private final SkillExecutor skillExecutor;
    private final SkillGenerator skillGenerator;
    private final ExperienceLearner experienceLearner;
    private final WorldKnowledge worldKnowledge;

    // Controllers (will be properly initialized when ActionController is available)
    private final ActionController actionController;

    // Simple state for fallback mode
    private Vec3dSimple currentMovementTarget;
    private int ticksSinceLastDecision;
    private static final int DECISION_INTERVAL_TICKS = 20; // Decide every second

    /**
     * Create AI brain with intelligent planning (Phase 3+) and advanced AI (Phase 5).
     */
    public AIPlayerBrain(AIPlayerEntity player, LLMProvider llmProvider) {
        this.player = player;
        this.random = new Random();
        this.ticksSinceLastDecision = 0;

        // Initialize Phase 3 intelligence systems
        this.memorySystem = new MemorySystem();
        this.skillLibrary = new SkillLibrary();

        // Initialize action controller (placeholder for now)
        this.actionController = null; // Will be properly initialized when available

        // Check if LLM is available
        if (llmProvider != null && llmProvider.isAvailable()) {
            this.planningEngine = new PlanningEngine(llmProvider, memorySystem);
            this.intelligentMode = true;

            // Initialize Phase 5 advanced AI systems (requires LLM)
            this.skillExecutor = new SkillExecutor(player, actionController, memorySystem);
            this.skillGenerator = new SkillGenerator(llmProvider, memorySystem, skillLibrary);
            this.experienceLearner = new ExperienceLearner(
                player, memorySystem, skillGenerator, skillLibrary, skillExecutor);
            this.worldKnowledge = new WorldKnowledge(memorySystem);

            LOGGER.info("AI brain initialized in INTELLIGENT mode with {} ({})",
                llmProvider.getProviderName(), llmProvider.getModelName());
            LOGGER.info("Phase 5 Advanced AI systems enabled: skill learning, world knowledge");
        } else {
            this.planningEngine = null;
            this.intelligentMode = false;

            // Phase 5 systems disabled in simple mode
            this.skillExecutor = null;
            this.skillGenerator = null;
            this.experienceLearner = null;
            this.worldKnowledge = null;

            LOGGER.warn("AI brain initialized in SIMPLE mode (LLM unavailable)");
            LOGGER.warn("Phase 5 Advanced AI features disabled");
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

        // Store world state for access by other systems (Phase 4)
        storeWorldState(worldState);

        // Only make decisions periodically to reduce CPU usage
        if (ticksSinceLastDecision < DECISION_INTERVAL_TICKS) {
            return;
        }

        ticksSinceLastDecision = 0;

        try {
            // Store perception in memory
            storePerceptionMemories(worldState);

            // Phase 5: Update world knowledge from observations
            if (intelligentMode && worldKnowledge != null) {
                updateWorldKnowledge(worldState);
            }

            // Use intelligent planning if available, otherwise fall back to simple mode
            if (intelligentMode) {
                makeIntelligentDecision(worldState);
            } else {
                makeSimpleDecision(worldState);
            }

            // Phase 5: Process learning experiences periodically
            if (intelligentMode && experienceLearner != null && ticksSinceLastDecision % 200 == 0) {
                experienceLearner.processExperiences(worldState);
            }

            // Periodic memory cleanup
            if (ticksSinceLastDecision % 1200 == 0) { // Every minute
                memorySystem.cleanup();
            }
        } catch (Exception e) {
            LOGGER.error("Error in AI brain update for {}", player.getName().getString(), e);
        }
    }

    /**
     * Phase 3: Intelligent decision making using LLM planning.
     */
    private void makeIntelligentDecision(WorldState worldState) {
        // Update planning engine
        planningEngine.update(worldState);

        // Get current goal
        Optional<Goal> currentGoal = planningEngine.getCurrentGoal();

        if (currentGoal.isPresent()) {
            executeGoal(currentGoal.get(), worldState);
        } else {
            // No active goal - request new plan from LLM
            LOGGER.debug("No active goals - requesting new plan");
            planningEngine.replan(worldState);

            // Fall back to simple behavior while waiting
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
        LOGGER.debug("Executing goal: {}", goal.getDescription());

        // Update goal status
        if (goal.getStatus() == Goal.GoalStatus.PENDING) {
            goal.setStatus(Goal.GoalStatus.IN_PROGRESS);
        }

        // For Phase 3, we'll execute based on goal type
        // Phase 4 will decompose into proper tasks
        switch (goal.getType()) {
            case SURVIVAL:
                executeSurvivalGoal(worldState);
                break;

            case RESOURCE_GATHERING:
                executeResourceGoal(worldState);
                break;

            case EXPLORATION:
                executeExplorationGoal(worldState);
                break;

            case COMBAT:
                executeCombatGoal(worldState);
                break;

            case BUILD:
                executeBuildGoal(worldState);
                break;

            default:
                // Fall back to simple behavior
                makeSimpleDecision(worldState);
                break;
        }
    }

    /**
     * Execute survival goal (find food, heal, etc.).
     */
    private void executeSurvivalGoal(WorldState worldState) {
        // Check if we need food
        if (worldState.getHunger() < 10) {
            // Look for food sources
            Optional<WorldState.EntityInfo> nearestAnimal = worldState.findNearestEntity(
                e -> e.getName().contains("cow") || e.getName().contains("pig") || e.getName().contains("sheep")
            );

            if (nearestAnimal.isPresent()) {
                // Move towards animal (using ActionController would be better)
                Vec3dSimple target = new Vec3dSimple(
                    nearestAnimal.get().getPosition().x,
                    nearestAnimal.get().getPosition().y,
                    nearestAnimal.get().getPosition().z
                );
                currentMovementTarget = target;
                moveTowardsTarget(worldState);

                memorySystem.store(new Memory(
                    Memory.MemoryType.OBSERVATION,
                    "Moving towards " + nearestAnimal.get().getName() + " for food",
                    0.6
                ));
            } else {
                // Explore to find food
                executeExplorationGoal(worldState);
            }
        }
    }

    /**
     * Execute resource gathering goal.
     */
    private void executeResourceGoal(WorldState worldState) {
        // Look for resources (trees, ores, etc.)
        // For now, just explore
        executeExplorationGoal(worldState);
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
            // Engage (simplified - Phase 4 will use CombatController)
            Vec3dSimple target = new Vec3dSimple(
                nearestHostile.get().getPosition().x,
                nearestHostile.get().getPosition().y,
                nearestHostile.get().getPosition().z
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
     * Store important observations in memory.
     */
    private void storePerceptionMemories(WorldState worldState) {
        // Store low health warning
        if (worldState.getHealth() < 6) {
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

        // Store nearby hostile mobs
        worldState.getNearbyEntities().stream()
            .filter(e -> e.getName().contains("zombie") || e.getName().contains("skeleton") ||
                        e.getName().contains("creeper") || e.getName().contains("spider"))
            .forEach(hostile -> {
                memorySystem.store(new Memory(
                    Memory.MemoryType.OBSERVATION,
                    String.format("Hostile %s nearby (%.1f blocks)",
                        hostile.getName(),
                        worldState.getPlayerPosition().distanceTo(hostile.getPosition())),
                    0.7
                ));
            });
    }

    /**
     * Phase 5: Update world knowledge based on observations.
     */
    private void updateWorldKnowledge(WorldState worldState) {
        if (worldKnowledge == null) {
            return;
        }

        // Mark current region as explored
        worldKnowledge.markExplored(
            player.getBlockPos(),
            16, // 16 block radius
            worldState.getBiome()
        );

        // Discover resources (simplified - in full version, would scan for actual blocks)
        // This is a placeholder for actual resource discovery logic

        // Register danger zones if health is very low in current area
        if (worldState.getHealth() < 4) {
            worldKnowledge.registerDangerZone(
                "Recently took damage here",
                player.getBlockPos(),
                10, // 10 block radius
                0.7 // Moderate threat
            );
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
     * Check if AI can accept a new goal (Phase 4: Chat Integration).
     *
     * @param goal Goal to check
     * @return true if goal can be accepted
     */
    public boolean canAcceptGoal(Goal goal) {
        if (goal == null) {
            return false;
        }

        // Cannot accept goals in simple mode without planning
        if (!intelligentMode || planningEngine == null) {
            LOGGER.debug("Cannot accept goal in simple mode");
            return false;
        }

        // Check if we have too many active goals (max 5)
        if (getActiveGoals().size() >= 5) {
            LOGGER.debug("Cannot accept goal: too many active goals ({})", getActiveGoals().size());
            return false;
        }

        // Basic validation
        if (goal.getStatus() == Goal.GoalStatus.COMPLETED ||
            goal.getStatus() == Goal.GoalStatus.FAILED) {
            return false;
        }

        return true;
    }

    /**
     * Add a new goal for the AI to pursue (Phase 4: Chat Integration).
     *
     * @param goal Goal to add
     * @return true if goal was added successfully
     */
    public boolean addGoal(Goal goal) {
        if (!canAcceptGoal(goal)) {
            return false;
        }

        try {
            planningEngine.addGoal(goal);
            LOGGER.info("Added goal: {}", goal.getDescription());

            // Store in memory
            memorySystem.store(new Memory(
                Memory.MemoryType.GOAL_COMPLETION,
                "Accepted new goal: " + goal.getDescription(),
                0.8
            ));

            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to add goal: " + goal.getDescription(), e);
            return false;
        }
    }

    /**
     * Get the current goal being executed (Phase 4: Chat Integration).
     *
     * @return Current goal, or null if none
     */
    public Goal getCurrentGoal() {
        if (intelligentMode && planningEngine != null) {
            return planningEngine.getCurrentGoal().orElse(null);
        }
        return null;
    }

    /**
     * Get all active goals (Phase 4: Chat Integration).
     *
     * @return List of active goals
     */
    public java.util.List<Goal> getActiveGoals() {
        if (intelligentMode && planningEngine != null) {
            return planningEngine.getActiveGoals();
        }
        return java.util.Collections.emptyList();
    }

    /**
     * Get current world state (Phase 4: Chat Integration).
     * This is used by ResponseGenerator to provide context.
     *
     * @return Current world state, or null if not available
     */
    private WorldState currentWorldState;

    public WorldState getWorldState() {
        return currentWorldState;
    }

    /**
     * Store current world state for access by other systems.
     * Called during update() cycle.
     */
    private void storeWorldState(WorldState worldState) {
        this.currentWorldState = worldState;
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
     * Phase 5: Get skill executor.
     */
    public SkillExecutor getSkillExecutor() {
        return skillExecutor;
    }

    /**
     * Phase 5: Get skill generator.
     */
    public SkillGenerator getSkillGenerator() {
        return skillGenerator;
    }

    /**
     * Phase 5: Get experience learner.
     */
    public ExperienceLearner getExperienceLearner() {
        return experienceLearner;
    }

    /**
     * Phase 5: Get world knowledge.
     */
    public WorldKnowledge getWorldKnowledge() {
        return worldKnowledge;
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
