package com.aiplayer.skills;

import com.aiplayer.core.AIPlayerEntity;
import com.aiplayer.memory.Memory;
import com.aiplayer.memory.MemorySystem;
import com.aiplayer.perception.WorldState;
import com.aiplayer.planning.Goal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Experience Learner - Learns from successes and failures.
 *
 * Phase 5: Advanced AI
 *
 * Tracks AI experiences and generates new skills or refines existing ones.
 * Implements a continuous learning loop:
 * 1. Track action sequences
 * 2. Detect goal completion (success) or failure
 * 3. Generate new skills from successful patterns
 * 4. Refine failed skills
 * 5. Learn from observing other players
 *
 * Inspired by Voyager's self-improvement approach.
 */
public class ExperienceLearner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperienceLearner.class);

    private final AIPlayerEntity player;
    private final MemorySystem memorySystem;
    private final SkillGenerator skillGenerator;
    private final SkillLibrary skillLibrary;
    private final SkillExecutor skillExecutor;

    // Experience tracking
    private final Map<Goal, ExperienceTracker> activeExperiences;
    private final List<String> currentActionSequence;
    private Goal currentGoal;
    private long experienceStartTime;

    // Learning parameters
    private static final int MIN_ACTIONS_FOR_SKILL = 3;
    private static final int MAX_ACTIONS_TO_TRACK = 20;
    private static final long EXPERIENCE_TIMEOUT_MS = 300000; // 5 minutes

    // Learning statistics
    private int skillsGenerated;
    private int skillsRefined;
    private int experiencesTracked;

    public ExperienceLearner(
            AIPlayerEntity player,
            MemorySystem memorySystem,
            SkillGenerator skillGenerator,
            SkillLibrary skillLibrary,
            SkillExecutor skillExecutor) {

        this.player = player;
        this.memorySystem = memorySystem;
        this.skillGenerator = skillGenerator;
        this.skillLibrary = skillLibrary;
        this.skillExecutor = skillExecutor;

        this.activeExperiences = new HashMap<>();
        this.currentActionSequence = new ArrayList<>();
        this.currentGoal = null;
        this.skillsGenerated = 0;
        this.skillsRefined = 0;
        this.experiencesTracked = 0;
    }

    /**
     * Start tracking a new goal experience.
     *
     * @param goal Goal being pursued
     */
    public void startExperience(Goal goal) {
        if (goal == null) {
            return;
        }

        LOGGER.debug("Starting experience tracking for goal: {}", goal.getDescription());

        this.currentGoal = goal;
        this.currentActionSequence.clear();
        this.experienceStartTime = System.currentTimeMillis();

        ExperienceTracker tracker = new ExperienceTracker(goal);
        activeExperiences.put(goal, tracker);
        experiencesTracked++;
    }

    /**
     * Record an action taken toward the current goal.
     *
     * @param action Description of action taken
     */
    public void recordAction(String action) {
        if (currentGoal == null || action == null || action.trim().isEmpty()) {
            return;
        }

        currentActionSequence.add(action);

        // Limit tracking to prevent memory issues
        if (currentActionSequence.size() > MAX_ACTIONS_TO_TRACK) {
            currentActionSequence.remove(0);
        }

        LOGGER.debug("Recorded action: {} (total: {})", action, currentActionSequence.size());
    }

    /**
     * Mark current goal as successfully completed and learn from it.
     *
     * @param worldState Current world state
     * @return Future that completes when learning is done
     */
    public CompletableFuture<Void> completeSuccess(WorldState worldState) {
        if (currentGoal == null) {
            return CompletableFuture.completedFuture(null);
        }

        Goal completedGoal = currentGoal;
        List<String> actions = new ArrayList<>(currentActionSequence);
        long duration = System.currentTimeMillis() - experienceStartTime;

        LOGGER.info("Goal completed successfully: {} ({} actions, {}ms)",
            completedGoal.getDescription(), actions.size(), duration);

        // Store success in memory
        memorySystem.store(new Memory(
            Memory.MemoryType.EPISODIC,
            String.format("Successfully completed goal '%s' in %d actions",
                completedGoal.getDescription(), actions.size()),
            0.8
        ));

        // Clean up tracking
        activeExperiences.remove(completedGoal);
        currentGoal = null;

        // Learn from this success
        if (actions.size() >= MIN_ACTIONS_FOR_SKILL) {
            return learnFromSuccessfulExperience(actions, completedGoal.getDescription(), worldState);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Mark current goal as failed and learn from the failure.
     *
     * @param reason Why the goal failed
     * @param worldState Current world state
     * @return Future that completes when learning is done
     */
    public CompletableFuture<Void> completeFailure(String reason, WorldState worldState) {
        if (currentGoal == null) {
            return CompletableFuture.completedFuture(null);
        }

        Goal failedGoal = currentGoal;
        List<String> actions = new ArrayList<>(currentActionSequence);

        LOGGER.warn("Goal failed: {} - Reason: {}", failedGoal.getDescription(), reason);

        // Store failure in memory (higher importance - learn from mistakes)
        memorySystem.store(new Memory(
            Memory.MemoryType.EPISODIC,
            String.format("Failed goal '%s': %s (attempted %d actions)",
                failedGoal.getDescription(), reason, actions.size()),
            0.9
        ));

        // Clean up tracking
        activeExperiences.remove(failedGoal);
        currentGoal = null;

        // Learn from this failure
        return learnFromFailedExperience(failedGoal, reason, worldState);
    }

    /**
     * Learn from a successful experience by generating a new skill.
     */
    private CompletableFuture<Void> learnFromSuccessfulExperience(
            List<String> actions,
            String goalDescription,
            WorldState worldState) {

        LOGGER.info("Learning from successful experience: {}", goalDescription);

        // Check if we already have a similar skill
        List<Skill> similar = skillLibrary.findRelevantSkills(goalDescription, 3);
        if (!similar.isEmpty()) {
            LOGGER.debug("Similar skills already exist, skipping generation");
            return CompletableFuture.completedFuture(null);
        }

        // Generate new skill
        return skillGenerator.learnFromSuccess(actions, goalDescription, worldState)
                .thenAccept(success -> {
                    if (success) {
                        skillsGenerated++;
                        LOGGER.info("Generated new skill from success (total: {})", skillsGenerated);
                    }
                })
                .exceptionally(ex -> {
                    LOGGER.error("Failed to learn from success", ex);
                    return null;
                });
    }

    /**
     * Learn from a failed experience by refining related skills.
     */
    private CompletableFuture<Void> learnFromFailedExperience(
            Goal failedGoal,
            String reason,
            WorldState worldState) {

        LOGGER.info("Learning from failed experience: {}", failedGoal.getDescription());

        // Find skills that might be related to this failure
        List<Skill> relatedSkills = skillLibrary.findRelevantSkills(failedGoal.getDescription(), 1);

        if (relatedSkills.isEmpty()) {
            LOGGER.debug("No related skills found to refine");
            return CompletableFuture.completedFuture(null);
        }

        // Refine the most relevant skill
        Skill skillToRefine = relatedSkills.get(0);

        // Only refine if the skill has failed before (not just first-time bad luck)
        if (skillToRefine.getTimesFailed() < 2) {
            LOGGER.debug("Skill hasn't failed enough times to warrant refinement");
            return CompletableFuture.completedFuture(null);
        }

        return skillGenerator.improveSkill(skillToRefine, reason, worldState)
                .thenAccept(success -> {
                    if (success) {
                        skillsRefined++;
                        LOGGER.info("Refined skill from failure (total: {})", skillsRefined);
                    }
                })
                .exceptionally(ex -> {
                    LOGGER.error("Failed to refine skill", ex);
                    return null;
                });
    }

    /**
     * Learn from observing another player's actions.
     *
     * @param playerName Player being observed
     * @param actions Actions observed
     * @param outcome What the player achieved
     */
    public CompletableFuture<Void> learnFromObservation(
            String playerName,
            List<String> actions,
            String outcome) {

        if (actions.size() < MIN_ACTIONS_FOR_SKILL) {
            return CompletableFuture.completedFuture(null);
        }

        LOGGER.info("Learning from observing player: {}", playerName);

        return skillGenerator.generateFromObservation(actions, playerName, outcome)
                .thenAccept(skill -> {
                    if (skill != null) {
                        skillLibrary.addSkill(skill);
                        skillsGenerated++;

                        memorySystem.store(new Memory(
                            Memory.MemoryType.LEARNING,
                            String.format("Learned skill '%s' by observing %s",
                                skill.getName(), playerName),
                            0.7
                        ));

                        LOGGER.info("Learned skill from observation: {}", skill.getName());
                    }
                })
                .exceptionally(ex -> {
                    LOGGER.error("Failed to learn from observation", ex);
                    return null;
                });
    }

    /**
     * Automatically identify and learn skills from completed experiences.
     *
     * Called periodically by AIPlayerBrain to process accumulated experiences.
     */
    public void processExperiences(WorldState worldState) {
        // Check for timed-out experiences
        long currentTime = System.currentTimeMillis();

        List<Goal> timedOut = new ArrayList<>();
        for (Map.Entry<Goal, ExperienceTracker> entry : activeExperiences.entrySet()) {
            ExperienceTracker tracker = entry.getValue();
            if (currentTime - tracker.startTime > EXPERIENCE_TIMEOUT_MS) {
                timedOut.add(entry.getKey());
            }
        }

        // Clean up timed-out experiences
        for (Goal goal : timedOut) {
            LOGGER.debug("Experience timed out: {}", goal.getDescription());
            activeExperiences.remove(goal);
        }
    }

    /**
     * Get learning statistics.
     */
    public LearningStats getStats() {
        return new LearningStats(
            skillsGenerated,
            skillsRefined,
            experiencesTracked,
            activeExperiences.size()
        );
    }

    /**
     * Reset learning statistics.
     */
    public void resetStats() {
        skillsGenerated = 0;
        skillsRefined = 0;
        experiencesTracked = 0;
    }

    /**
     * Check if currently tracking an experience.
     */
    public boolean isTrackingExperience() {
        return currentGoal != null;
    }

    /**
     * Get current goal being tracked.
     */
    public Goal getCurrentGoal() {
        return currentGoal;
    }

    /**
     * Tracks an ongoing experience.
     */
    private static class ExperienceTracker {
        final Goal goal;
        final long startTime;
        final List<String> actions;

        ExperienceTracker(Goal goal) {
            this.goal = goal;
            this.startTime = System.currentTimeMillis();
            this.actions = new ArrayList<>();
        }
    }

    /**
     * Learning statistics.
     */
    public static class LearningStats {
        private final int skillsGenerated;
        private final int skillsRefined;
        private final int experiencesTracked;
        private final int activeExperiences;

        public LearningStats(int skillsGenerated, int skillsRefined,
                           int experiencesTracked, int activeExperiences) {
            this.skillsGenerated = skillsGenerated;
            this.skillsRefined = skillsRefined;
            this.experiencesTracked = experiencesTracked;
            this.activeExperiences = activeExperiences;
        }

        public int getSkillsGenerated() {
            return skillsGenerated;
        }

        public int getSkillsRefined() {
            return skillsRefined;
        }

        public int getExperiencesTracked() {
            return experiencesTracked;
        }

        public int getActiveExperiences() {
            return activeExperiences;
        }

        @Override
        public String toString() {
            return String.format("LearningStats{generated=%d, refined=%d, tracked=%d, active=%d}",
                skillsGenerated, skillsRefined, experiencesTracked, activeExperiences);
        }
    }
}
