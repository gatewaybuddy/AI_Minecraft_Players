package com.aiplayer.skills;

import com.aiplayer.llm.LLMProvider;
import com.aiplayer.perception.WorldState;
import com.aiplayer.planning.Goal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Skill Library - Manages all learned skills.
 *
 * Inspired by Voyager's self-improving skill system.
 *
 * The skill library:
 * - Stores all skills the AI has learned
 * - Retrieves relevant skills for current task
 * - Tracks skill usage and success rates
 * - Suggests skills for new situations
 *
 * Skills are indexed by:
 * - Category (MINING, BUILDING, etc.)
 * - Complexity (1-10)
 * - Success rate
 *
 * Phase 3: Basic skill storage and retrieval
 * Phase 5: LLM-generated skills, composition, refinement
 */
public class SkillLibrary {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkillLibrary.class);

    private final Map<UUID, Skill> skills;
    private final Map<Skill.SkillCategory, List<Skill>> skillsByCategory;

    // Phase 5: Skill generation
    private SkillGenerator skillGenerator;

    public SkillLibrary() {
        this.skills = new HashMap<>();
        this.skillsByCategory = new EnumMap<>(Skill.SkillCategory.class);
        this.skillGenerator = null; // Set when LLM provider is available

        // Initialize category indices
        for (Skill.SkillCategory category : Skill.SkillCategory.values()) {
            skillsByCategory.put(category, new ArrayList<>());
        }

        // Initialize with basic skills
        initializeBasicSkills();
    }

    /**
     * Set LLM provider for skill generation (Phase 5).
     */
    public void setLLMProvider(LLMProvider llmProvider) {
        if (llmProvider != null && llmProvider.isAvailable()) {
            this.skillGenerator = new SkillGenerator(llmProvider);
            LOGGER.info("Skill generation enabled with LLM provider");
        }
    }

    /**
     * Generate a new skill for a goal (Phase 5).
     *
     * @param goal The goal to achieve
     * @param worldState Current world state
     * @return Future containing the generated skill
     */
    public CompletableFuture<Skill> generateSkillForGoal(Goal goal, WorldState worldState) {
        if (skillGenerator == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Skill generation not enabled - no LLM provider")
            );
        }

        // Check if similar skill already exists
        List<Skill> relevantSkills = findRelevantSkills(goal.getDescription(), 3);
        int attempts = relevantSkills.isEmpty() ? 0 : relevantSkills.size();

        return skillGenerator.generateSkill(goal, worldState, attempts)
            .thenApply(skill -> {
                addSkill(skill);
                return skill;
            });
    }

    /**
     * Refine a skill based on failure (Phase 5).
     *
     * @param skill The skill that failed
     * @param failureReason Why it failed
     * @param worldState Current world state
     * @return Future containing refined skill
     */
    public CompletableFuture<Skill> refineSkill(Skill skill, String failureReason, WorldState worldState) {
        if (skillGenerator == null) {
            return CompletableFuture.completedFuture(skill);
        }

        return skillGenerator.refineSkill(skill, failureReason, worldState)
            .thenApply(refinedSkill -> {
                // Replace old skill with refined version
                removeSkill(skill.getId());
                addSkill(refinedSkill);
                return refinedSkill;
            });
    }

    /**
     * Check if skill generation is enabled.
     */
    public boolean isSkillGenerationEnabled() {
        return skillGenerator != null;
    }

    /**
     * Add a skill to the library.
     */
    public void addSkill(Skill skill) {
        skills.put(skill.getId(), skill);
        skillsByCategory.get(skill.getCategory()).add(skill);
        LOGGER.info("Added skill: {} (category: {}, complexity: {})",
            skill.getName(), skill.getCategory(), skill.getComplexity());
    }

    /**
     * Get skill by ID.
     */
    public Optional<Skill> getSkill(UUID skillId) {
        return Optional.ofNullable(skills.get(skillId));
    }

    /**
     * Get skill by name.
     */
    public Optional<Skill> getSkillByName(String name) {
        return skills.values().stream()
            .filter(s -> s.getName().equalsIgnoreCase(name))
            .findFirst();
    }

    /**
     * Get all skills in a category.
     */
    public List<Skill> getSkillsByCategory(Skill.SkillCategory category) {
        return new ArrayList<>(skillsByCategory.get(category));
    }

    /**
     * Get all skills.
     */
    public List<Skill> getAllSkills() {
        return new ArrayList<>(skills.values());
    }

    /**
     * Get most reliable skills (success rate >= 70%, used >= 3 times).
     */
    public List<Skill> getReliableSkills() {
        return skills.values().stream()
            .filter(Skill::isReliable)
            .sorted(Comparator.comparingDouble(Skill::getSuccessRate).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Get skills by complexity range.
     */
    public List<Skill> getSkillsByComplexity(int minComplexity, int maxComplexity) {
        return skills.values().stream()
            .filter(s -> s.getComplexity() >= minComplexity && s.getComplexity() <= maxComplexity)
            .collect(Collectors.toList());
    }

    /**
     * Get top skills by quality score.
     */
    public List<Skill> getTopSkills(int limit) {
        return skills.values().stream()
            .sorted(Comparator.comparingInt(Skill::getQualityScore).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Get recently used skills.
     */
    public List<Skill> getRecentlyUsedSkills() {
        return skills.values().stream()
            .filter(Skill::isRecentlyUsed)
            .sorted(Comparator.comparingLong(Skill::getLastUsedTimestamp).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Find relevant skills for a task description.
     *
     * Simple keyword matching for now.
     * Phase 5 will use embeddings for semantic search.
     */
    public List<Skill> findRelevantSkills(String taskDescription, int limit) {
        String lowerTask = taskDescription.toLowerCase();

        return skills.values().stream()
            .filter(skill -> {
                String skillText = (skill.getName() + " " + skill.getDescription()).toLowerCase();
                // Check if any words overlap
                String[] taskWords = lowerTask.split("\\s+");
                for (String word : taskWords) {
                    if (word.length() > 3 && skillText.contains(word)) {
                        return true;
                    }
                }
                return false;
            })
            .sorted(Comparator.comparingInt(Skill::getQualityScore).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Remove a skill.
     */
    public void removeSkill(UUID skillId) {
        Skill skill = skills.remove(skillId);
        if (skill != null) {
            skillsByCategory.get(skill.getCategory()).remove(skill);
            LOGGER.info("Removed skill: {}", skill.getName());
        }
    }

    /**
     * Get number of skills in library.
     */
    public int getSkillCount() {
        return skills.size();
    }

    /**
     * Clear all skills.
     */
    public void clear() {
        skills.clear();
        for (List<Skill> list : skillsByCategory.values()) {
            list.clear();
        }
        LOGGER.info("Cleared skill library");
    }

    /**
     * Get library statistics.
     */
    public LibraryStats getStats() {
        int totalUses = skills.values().stream()
            .mapToInt(Skill::getTimesUsed)
            .sum();

        int totalSuccesses = skills.values().stream()
            .mapToInt(Skill::getTimesSucceeded)
            .sum();

        double avgSuccessRate = skills.values().stream()
            .filter(s -> s.getTimesUsed() > 0)
            .mapToDouble(Skill::getSuccessRate)
            .average()
            .orElse(0.0);

        return new LibraryStats(
            skills.size(),
            totalUses,
            totalSuccesses,
            avgSuccessRate
        );
    }

    /**
     * Format skills for LLM context.
     */
    public String formatSkillsForContext(int limit) {
        List<Skill> topSkills = getTopSkills(limit);
        if (topSkills.isEmpty()) {
            return "No skills learned yet.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## Available Skills\n\n");

        for (Skill skill : topSkills) {
            sb.append(skill.toContextString()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Initialize basic skills.
     */
    private void initializeBasicSkills() {
        // Mining skills
        Skill mineLogs = new Skill(
            "Mine Logs",
            "Find and mine the nearest tree to collect logs",
            Skill.SkillCategory.MINING,
            2
        );
        mineLogs.addPrerequisite("None");
        mineLogs.addStep("Scan for nearby logs (wood blocks)");
        mineLogs.addStep("Pathfind to nearest log");
        mineLogs.addStep("Mine the log block");
        mineLogs.addStep("Collect dropped items");
        addSkill(mineLogs);

        // Survival skills
        Skill findFood = new Skill(
            "Find Food",
            "Search for and collect food sources (animals, crops)",
            Skill.SkillCategory.SURVIVAL,
            3
        );
        findFood.addPrerequisite("None");
        findFood.addStep("Scan for animals or crops");
        findFood.addStep("Move towards food source");
        findFood.addStep("Kill animal or harvest crop");
        findFood.addStep("Collect food items");
        addSkill(findFood);

        // Exploration skills
        Skill exploreSafely = new Skill(
            "Explore Safely",
            "Move to unexplored areas while avoiding dangers",
            Skill.SkillCategory.EXPLORATION,
            4
        );
        exploreSafely.addPrerequisite("None");
        exploreSafely.addStep("Pick random direction");
        exploreSafely.addStep("Check for hostile mobs");
        exploreSafely.addStep("Move while monitoring health");
        exploreSafely.addStep("Return if health is low");
        addSkill(exploreSafely);

        // Combat skills
        Skill fightMob = new Skill(
            "Fight Hostile Mob",
            "Engage and defeat a hostile mob",
            Skill.SkillCategory.COMBAT,
            6
        );
        fightMob.addPrerequisite("Weapon (sword preferred)");
        fightMob.addStep("Equip best weapon");
        fightMob.addStep("Approach mob");
        fightMob.addStep("Attack while dodging");
        fightMob.addStep("Retreat if health drops below 6");
        addSkill(fightMob);

        // Building skills
        Skill buildShelter = new Skill(
            "Build Simple Shelter",
            "Construct a basic 3x3 shelter for protection",
            Skill.SkillCategory.BUILDING,
            5
        );
        buildShelter.addPrerequisite("Building blocks (dirt, cobblestone, etc.)");
        buildShelter.addStep("Find flat area");
        buildShelter.addStep("Place floor blocks (3x3)");
        buildShelter.addStep("Build walls (4 blocks high)");
        buildShelter.addStep("Add roof");
        addSkill(buildShelter);

        LOGGER.info("Initialized {} basic skills", skills.size());
    }

    /**
     * Library statistics.
     */
    public static class LibraryStats {
        private final int totalSkills;
        private final int totalUses;
        private final int totalSuccesses;
        private final double averageSuccessRate;

        public LibraryStats(int totalSkills, int totalUses, int totalSuccesses, double averageSuccessRate) {
            this.totalSkills = totalSkills;
            this.totalUses = totalUses;
            this.totalSuccesses = totalSuccesses;
            this.averageSuccessRate = averageSuccessRate;
        }

        public int getTotalSkills() {
            return totalSkills;
        }

        public int getTotalUses() {
            return totalUses;
        }

        public int getTotalSuccesses() {
            return totalSuccesses;
        }

        public double getAverageSuccessRate() {
            return averageSuccessRate;
        }

        @Override
        public String toString() {
            return String.format("LibraryStats{skills=%d, uses=%d, successes=%d, avgSuccessRate=%.1f%%}",
                totalSkills, totalUses, totalSuccesses, averageSuccessRate * 100);
        }
    }
}
