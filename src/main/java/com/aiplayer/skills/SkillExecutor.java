package com.aiplayer.skills;

import com.aiplayer.action.ActionController;
import com.aiplayer.core.AIPlayerEntity;
import com.aiplayer.memory.Memory;
import com.aiplayer.memory.MemorySystem;
import com.aiplayer.perception.WorldState;
import com.aiplayer.planning.Goal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Skill Executor - Executes learned skills.
 *
 * Phase 5: Advanced AI
 *
 * Takes a Skill and executes its steps using the action controller.
 * Tracks success/failure and updates skill metrics.
 * Provides feedback for skill refinement.
 *
 * Execution Process:
 * 1. Validate prerequisites
 * 2. Execute each step sequentially
 * 3. Monitor success/failure
 * 4. Update skill statistics
 * 5. Store execution memory
 */
public class SkillExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkillExecutor.class);

    private final AIPlayerEntity player;
    private final ActionController actionController;
    private final MemorySystem memorySystem;

    // Current execution state
    private Skill currentSkill;
    private int currentStepIndex;
    private boolean isExecuting;
    private long executionStartTime;

    public SkillExecutor(AIPlayerEntity player, ActionController actionController, MemorySystem memorySystem) {
        this.player = player;
        this.actionController = actionController;
        this.memorySystem = memorySystem;
        this.currentSkill = null;
        this.currentStepIndex = 0;
        this.isExecuting = false;
    }

    /**
     * Execute a skill.
     *
     * @param skill Skill to execute
     * @param worldState Current world state
     * @return Execution result
     */
    public SkillExecutionResult execute(Skill skill, WorldState worldState) {
        LOGGER.info("Executing skill: {}", skill.getName());

        // Check prerequisites
        if (!checkPrerequisites(skill, worldState)) {
            LOGGER.warn("Skill prerequisites not met: {}", skill.getName());
            skill.recordUse(false);
            return new SkillExecutionResult(false, "Prerequisites not met", 0);
        }

        // Start execution
        this.currentSkill = skill;
        this.currentStepIndex = 0;
        this.isExecuting = true;
        this.executionStartTime = System.currentTimeMillis();

        try {
            // Execute each step
            int stepsCompleted = 0;
            for (String step : skill.getSteps()) {
                LOGGER.debug("Executing step {}: {}", currentStepIndex + 1, step);

                boolean stepSuccess = executeStep(step, worldState);
                if (!stepSuccess) {
                    LOGGER.warn("Step failed: {}", step);
                    recordFailure(skill, stepsCompleted, "Step failed: " + step);
                    return new SkillExecutionResult(false, "Step failed: " + step, stepsCompleted);
                }

                currentStepIndex++;
                stepsCompleted++;
            }

            // All steps completed successfully
            recordSuccess(skill, stepsCompleted);
            return new SkillExecutionResult(true, "Success", stepsCompleted);

        } catch (Exception e) {
            LOGGER.error("Error executing skill: " + skill.getName(), e);
            recordFailure(skill, currentStepIndex, "Exception: " + e.getMessage());
            return new SkillExecutionResult(false, "Error: " + e.getMessage(), currentStepIndex);
        } finally {
            this.isExecuting = false;
            this.currentSkill = null;
        }
    }

    /**
     * Execute a single skill step.
     *
     * This is a simplified execution - maps step descriptions to actions.
     * In a full implementation, this would parse step text more intelligently.
     */
    private boolean executeStep(String step, WorldState worldState) {
        String lowerStep = step.toLowerCase();

        try {
            // Mining steps
            if (lowerStep.contains("mine") || lowerStep.contains("break")) {
                // Simplified: attempt to mine nearest block
                return true; // Placeholder - actual mining logic needed
            }

            // Movement steps
            else if (lowerStep.contains("move") || lowerStep.contains("pathfind")) {
                // Simplified: basic movement
                return true; // Placeholder
            }

            // Collection steps
            else if (lowerStep.contains("collect") || lowerStep.contains("pick up")) {
                // Simplified: pick up nearby items
                return true; // Placeholder
            }

            // Scanning/searching steps
            else if (lowerStep.contains("scan") || lowerStep.contains("search") || lowerStep.contains("find")) {
                // Simplified: perception check
                return true; // Placeholder
            }

            // Building steps
            else if (lowerStep.contains("place") || lowerStep.contains("build")) {
                // Simplified: block placement
                return true; // Placeholder
            }

            // Combat steps
            else if (lowerStep.contains("attack") || lowerStep.contains("fight")) {
                // Simplified: combat action
                return true; // Placeholder
            }

            // Crafting steps
            else if (lowerStep.contains("craft")) {
                // Simplified: crafting action
                return true; // Placeholder
            }

            // Default: assume step succeeds
            LOGGER.debug("Generic step execution: {}", step);
            return true;

        } catch (Exception e) {
            LOGGER.error("Error executing step: " + step, e);
            return false;
        }
    }

    /**
     * Check if skill prerequisites are met.
     */
    private boolean checkPrerequisites(Skill skill, WorldState worldState) {
        for (String prerequisite : skill.getPrerequisites()) {
            String lower = prerequisite.toLowerCase();

            // "None" is always satisfied
            if (lower.equals("none")) {
                continue;
            }

            // Check for specific items in inventory
            if (worldState != null) {
                // Simplified check - in full version, parse prerequisite and check inventory
                // For now, assume prerequisites are met if not "none"
                continue;
            }
        }

        return true;
    }

    /**
     * Record successful skill execution.
     */
    private void recordSuccess(Skill skill, int stepsCompleted) {
        long duration = System.currentTimeMillis() - executionStartTime;

        // Update skill metrics
        skill.recordUse(true);

        // Store in memory
        memorySystem.store(new Memory(
            Memory.MemoryType.LEARNING,
            String.format("Successfully executed skill '%s' (%d steps, %dms)",
                skill.getName(), stepsCompleted, duration),
            0.7  // Moderate importance
        ));

        LOGGER.info("Skill executed successfully: {} ({} steps, {}ms)",
            skill.getName(), stepsCompleted, duration);
    }

    /**
     * Record failed skill execution.
     */
    private void recordFailure(Skill skill, int stepsCompleted, String reason) {
        long duration = System.currentTimeMillis() - executionStartTime;

        // Update skill metrics
        skill.recordUse(false);

        // Store in memory
        memorySystem.store(new Memory(
            Memory.MemoryType.LEARNING,
            String.format("Skill '%s' failed at step %d: %s",
                skill.getName(), stepsCompleted + 1, reason),
            0.8  // Higher importance for failures (learn from mistakes)
        ));

        LOGGER.warn("Skill execution failed: {} (step {}/{}, reason: {})",
            skill.getName(), stepsCompleted + 1, skill.getSteps().size(), reason);
    }

    /**
     * Check if currently executing a skill.
     */
    public boolean isExecuting() {
        return isExecuting;
    }

    /**
     * Get currently executing skill.
     */
    public Skill getCurrentSkill() {
        return currentSkill;
    }

    /**
     * Get current execution progress (0.0 to 1.0).
     */
    public double getProgress() {
        if (!isExecuting || currentSkill == null) {
            return 0.0;
        }
        return (double) currentStepIndex / currentSkill.getSteps().size();
    }

    /**
     * Skill execution result.
     */
    public static class SkillExecutionResult {
        private final boolean success;
        private final String message;
        private final int stepsCompleted;

        public SkillExecutionResult(boolean success, String message, int stepsCompleted) {
            this.success = success;
            this.message = message;
            this.stepsCompleted = stepsCompleted;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public int getStepsCompleted() {
            return stepsCompleted;
        }

        @Override
        public String toString() {
            return String.format("SkillExecutionResult{success=%s, steps=%d, message='%s'}",
                success, stepsCompleted, message);
        }
    }
}
