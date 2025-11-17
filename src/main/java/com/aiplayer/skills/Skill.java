package com.aiplayer.skills;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Skill - A learned behavior or capability.
 *
 * Inspired by Voyager's skill library system.
 *
 * Skills are reusable patterns that the AI learns:
 * - "Find and mine nearest tree"
 * - "Build a shelter"
 * - "Craft wooden tools"
 * - "Escape from hostile mob"
 *
 * Each skill has:
 * - Description: What the skill does
 * - Prerequisites: Required items/conditions
 * - Steps: Sequence of actions
 * - Success rate: Track how often it works
 * - Complexity: How difficult (1-10)
 *
 * Phase 3: Basic skill structure
 * Phase 5: LLM-generated skills, refinement, composition
 */
public class Skill {

    private final UUID id;
    private final String name;
    private final String description;
    private final List<String> prerequisites;
    private final List<String> steps;
    private final SkillCategory category;
    private final int complexity;

    // Learning metrics
    private int timesUsed;
    private int timesSucceeded;
    private int timesFailed;
    private long lastUsedTimestamp;

    public Skill(String name, String description, SkillCategory category, int complexity) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.prerequisites = new ArrayList<>();
        this.steps = new ArrayList<>();
        this.category = category;
        this.complexity = Math.max(1, Math.min(10, complexity));
        this.timesUsed = 0;
        this.timesSucceeded = 0;
        this.timesFailed = 0;
        this.lastUsedTimestamp = 0;
    }

    /**
     * Skill categories.
     */
    public enum SkillCategory {
        MINING,        // Mining blocks, ores
        BUILDING,      // Placing blocks, structures
        CRAFTING,      // Crafting items
        COMBAT,        // Fighting mobs
        SURVIVAL,      // Food, health, safety
        EXPLORATION,   // Navigation, discovery
        SOCIAL,        // Interacting with players
        FARMING,       // Growing crops, breeding animals
        UTILITY        // General purpose
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getPrerequisites() {
        return new ArrayList<>(prerequisites);
    }

    public List<String> getSteps() {
        return new ArrayList<>(steps);
    }

    public SkillCategory getCategory() {
        return category;
    }

    public int getComplexity() {
        return complexity;
    }

    public int getTimesUsed() {
        return timesUsed;
    }

    public int getTimesSucceeded() {
        return timesSucceeded;
    }

    public int getTimesFailed() {
        return timesFailed;
    }

    public long getLastUsedTimestamp() {
        return lastUsedTimestamp;
    }

    /**
     * Get success rate (0.0 to 1.0).
     */
    public double getSuccessRate() {
        if (timesUsed == 0) {
            return 0.5; // Unknown - assume 50%
        }
        return (double) timesSucceeded / timesUsed;
    }

    /**
     * Check if skill has been used recently (within last hour).
     */
    public boolean isRecentlyUsed() {
        long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
        return lastUsedTimestamp > oneHourAgo;
    }

    /**
     * Check if skill is well-practiced (used 10+ times).
     */
    public boolean isWellPracticed() {
        return timesUsed >= 10;
    }

    /**
     * Check if skill is reliable (success rate >= 70%).
     */
    public boolean isReliable() {
        return getSuccessRate() >= 0.7 && timesUsed >= 3;
    }

    // Modifiers

    /**
     * Add prerequisite.
     */
    public void addPrerequisite(String prerequisite) {
        if (!prerequisites.contains(prerequisite)) {
            prerequisites.add(prerequisite);
        }
    }

    /**
     * Add step.
     */
    public void addStep(String step) {
        steps.add(step);
    }

    /**
     * Record skill usage.
     */
    public void recordUse(boolean success) {
        timesUsed++;
        if (success) {
            timesSucceeded++;
        } else {
            timesFailed++;
        }
        lastUsedTimestamp = System.currentTimeMillis();
    }

    /**
     * Get skill quality score (0-100).
     *
     * Based on:
     * - Success rate (50%)
     * - Experience - times used (30%)
     * - Recency (20%)
     */
    public int getQualityScore() {
        double successScore = getSuccessRate() * 50;
        double experienceScore = Math.min(timesUsed / 20.0, 1.0) * 30;
        double recencyScore = isRecentlyUsed() ? 20 : 0;
        return (int) (successScore + experienceScore + recencyScore);
    }

    @Override
    public String toString() {
        return String.format("Skill{name='%s', category=%s, complexity=%d, successRate=%.1f%%, used=%d}",
            name, category, complexity, getSuccessRate() * 100, timesUsed);
    }

    /**
     * Format skill for LLM context.
     */
    public String toContextString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" (").append(category).append(")\n");
        sb.append("  ").append(description).append("\n");

        if (!prerequisites.isEmpty()) {
            sb.append("  Prerequisites: ").append(String.join(", ", prerequisites)).append("\n");
        }

        if (timesUsed > 0) {
            sb.append(String.format("  Success rate: %.0f%% (%d uses)\n",
                getSuccessRate() * 100, timesUsed));
        }

        return sb.toString();
    }
}
