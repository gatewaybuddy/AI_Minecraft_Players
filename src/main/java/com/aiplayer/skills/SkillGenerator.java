package com.aiplayer.skills;

import com.aiplayer.llm.LLMOptions;
import com.aiplayer.llm.LLMProvider;
import com.aiplayer.perception.WorldState;
import com.aiplayer.planning.Goal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates new skills using LLM (Phase 5).
 *
 * Inspired by Voyager's self-improving skill system:
 * - AI writes its own code/procedures
 * - Skills are tested and refined iteratively
 * - Successful skills are stored in library
 *
 * Example: "Learn how to build a simple shelter"
 * → LLM generates step-by-step procedure
 * → AI executes and learns from results
 * → Procedure is refined based on outcomes
 *
 * Phase 5: Advanced AI - Skill Library Enhancement
 */
public class SkillGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillGenerator.class);

    private final LLMProvider llmProvider;
    private final SkillValidator validator;

    /**
     * Create a skill generator.
     *
     * @param llmProvider The LLM provider for generation
     */
    public SkillGenerator(LLMProvider llmProvider) {
        this.llmProvider = llmProvider;
        this.validator = new SkillValidator();

        LOGGER.debug("SkillGenerator initialized with provider: {}",
            llmProvider != null ? llmProvider.getProviderName() : "NONE");
    }

    /**
     * Generate a new skill for a given goal.
     *
     * @param goal The goal to achieve
     * @param worldState Current world state
     * @param previousAttempts Number of previous failed attempts
     * @return Future containing the generated skill
     */
    public CompletableFuture<Skill> generateSkill(Goal goal, WorldState worldState, int previousAttempts) {
        if (llmProvider == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("No LLM provider available for skill generation")
            );
        }

        LOGGER.info("Generating skill for goal: {}", goal.getDescription());

        String prompt = buildSkillGenerationPrompt(goal, worldState, previousAttempts);

        return llmProvider.complete(prompt, LLMOptions.planning())
            .thenApply(response -> parseSkillFromResponse(response, goal))
            .thenApply(skill -> {
                // Validate the generated skill
                SkillValidator.ValidationResult validation = validator.validate(skill);
                if (!validation.isValid()) {
                    LOGGER.warn("Generated skill failed validation: {}",
                        validation.getIssues());
                    // Return skill anyway but log warning
                }
                return skill;
            })
            .exceptionally(e -> {
                LOGGER.error("Error generating skill for goal: {}", goal.getDescription(), e);
                // Return a basic fallback skill
                return createFallbackSkill(goal);
            });
    }

    /**
     * Refine an existing skill based on failure.
     *
     * @param skill The skill that failed
     * @param failureReason Why it failed
     * @param worldState Current world state
     * @return Future containing the refined skill
     */
    public CompletableFuture<Skill> refineSkill(Skill skill, String failureReason, WorldState worldState) {
        if (llmProvider == null) {
            return CompletableFuture.completedFuture(skill);
        }

        LOGGER.info("Refining skill '{}' due to: {}", skill.getName(), failureReason);

        String prompt = buildSkillRefinementPrompt(skill, failureReason, worldState);

        return llmProvider.complete(prompt, LLMOptions.planning())
            .thenApply(response -> {
                // Parse improvements and create refined skill
                Skill refinedSkill = parseSkillFromResponse(response, null);
                // Preserve usage statistics
                for (int i = 0; i < skill.getTimesUsed(); i++) {
                    refinedSkill.recordUse(i < skill.getTimesSucceeded());
                }
                return refinedSkill;
            })
            .exceptionally(e -> {
                LOGGER.error("Error refining skill", e);
                return skill; // Return original on error
            });
    }

    /**
     * Build prompt for skill generation.
     */
    private String buildSkillGenerationPrompt(Goal goal, WorldState worldState, int previousAttempts) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an AI playing Minecraft. Create a step-by-step skill procedure to achieve this goal:\n\n");
        prompt.append("GOAL: ").append(goal.getDescription()).append("\n");
        prompt.append("TYPE: ").append(goal.getType()).append("\n\n");

        // Add world context
        prompt.append("CURRENT SITUATION:\n");
        prompt.append(String.format("- Position: %.1f, %.1f, %.1f\n",
            worldState.getPlayerPosition().x,
            worldState.getPlayerPosition().y,
            worldState.getPlayerPosition().z));
        prompt.append(String.format("- Health: %.1f/20\n", worldState.getHealth()));
        prompt.append(String.format("- Hunger: %.1f/20\n", worldState.getHunger()));

        if (!worldState.getNearbyEntities().isEmpty()) {
            prompt.append(String.format("- Nearby entities: %d\n", worldState.getNearbyEntities().size()));
        }

        if (previousAttempts > 0) {
            prompt.append(String.format("\nPREVIOUS ATTEMPTS: %d (all failed)\n", previousAttempts));
            prompt.append("Please provide a DIFFERENT approach than before.\n");
        }

        prompt.append("\nGenerate a skill in this format:\n\n");
        prompt.append("SKILL_NAME: <concise name>\n");
        prompt.append("DESCRIPTION: <what the skill does>\n");
        prompt.append("CATEGORY: <MINING|BUILDING|CRAFTING|COMBAT|SURVIVAL|EXPLORATION|SOCIAL|FARMING|UTILITY>\n");
        prompt.append("COMPLEXITY: <1-10>\n");
        prompt.append("PREREQUISITES:\n");
        prompt.append("- <prerequisite 1>\n");
        prompt.append("- <prerequisite 2>\n");
        prompt.append("STEPS:\n");
        prompt.append("1. <step 1>\n");
        prompt.append("2. <step 2>\n");
        prompt.append("3. <step 3>\n\n");

        prompt.append("Make steps specific and executable. Consider:\n");
        prompt.append("- Available resources and tools\n");
        prompt.append("- Safety (avoid hazards)\n");
        prompt.append("- Efficiency (minimize time/resources)\n");
        prompt.append("- Realistic actions for Minecraft\n");

        return prompt.toString();
    }

    /**
     * Build prompt for skill refinement.
     */
    private String buildSkillRefinementPrompt(Skill skill, String failureReason, WorldState worldState) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are refining a Minecraft skill that failed. Here's the current skill:\n\n");
        prompt.append("SKILL: ").append(skill.getName()).append("\n");
        prompt.append("DESCRIPTION: ").append(skill.getDescription()).append("\n");
        prompt.append("SUCCESS RATE: ").append(String.format("%.0f%%", skill.getSuccessRate() * 100)).append("\n\n");

        prompt.append("CURRENT STEPS:\n");
        for (int i = 0; i < skill.getSteps().size(); i++) {
            prompt.append((i + 1)).append(". ").append(skill.getSteps().get(i)).append("\n");
        }

        prompt.append("\nFAILURE REASON: ").append(failureReason).append("\n\n");

        prompt.append("CURRENT SITUATION:\n");
        prompt.append(String.format("- Health: %.1f/20\n", worldState.getHealth()));
        prompt.append(String.format("- Hunger: %.1f/20\n", worldState.getHunger()));

        prompt.append("\nProvide IMPROVED steps that address the failure:\n\n");
        prompt.append("SKILL_NAME: ").append(skill.getName()).append("\n");
        prompt.append("DESCRIPTION: <improved description if needed>\n");
        prompt.append("CATEGORY: ").append(skill.getCategory()).append("\n");
        prompt.append("COMPLEXITY: ").append(skill.getComplexity()).append("\n");
        prompt.append("IMPROVED_STEPS:\n");
        prompt.append("1. <improved step 1>\n");
        prompt.append("2. <improved step 2>\n");
        prompt.append("...\n\n");

        prompt.append("Focus on fixing the specific failure while keeping successful parts.\n");

        return prompt.toString();
    }

    /**
     * Parse skill from LLM response.
     */
    private Skill parseSkillFromResponse(String response, Goal goal) {
        // Extract skill components using regex
        String name = extractPattern(response, "SKILL_NAME:\\s*(.+)");
        String description = extractPattern(response, "DESCRIPTION:\\s*(.+)");
        String categoryStr = extractPattern(response, "CATEGORY:\\s*(\\w+)");
        String complexityStr = extractPattern(response, "COMPLEXITY:\\s*(\\d+)");

        // Set defaults if parsing fails
        if (name == null || name.isEmpty()) {
            name = goal != null ? goal.getDescription() : "Generated Skill";
        }
        if (description == null || description.isEmpty()) {
            description = "A procedural skill generated by the AI";
        }

        // Parse category
        Skill.SkillCategory category = Skill.SkillCategory.UTILITY;
        if (categoryStr != null) {
            try {
                category = Skill.SkillCategory.valueOf(categoryStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid category: {}, using UTILITY", categoryStr);
            }
        } else if (goal != null) {
            // Infer from goal type
            category = inferCategoryFromGoal(goal);
        }

        // Parse complexity
        int complexity = 5;
        if (complexityStr != null) {
            try {
                complexity = Integer.parseInt(complexityStr);
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid complexity: {}, using 5", complexityStr);
            }
        }

        // Create skill
        Skill skill = new Skill(name, description, category, complexity);

        // Extract prerequisites
        Pattern prereqPattern = Pattern.compile("PREREQUISITES:\\s*(.+?)(?:STEPS:|IMPROVED_STEPS:|$)", Pattern.DOTALL);
        Matcher prereqMatcher = prereqPattern.matcher(response);
        if (prereqMatcher.find()) {
            String prereqSection = prereqMatcher.group(1);
            Pattern itemPattern = Pattern.compile("-\\s*(.+)");
            Matcher itemMatcher = itemPattern.matcher(prereqSection);
            while (itemMatcher.find()) {
                String prereq = itemMatcher.group(1).trim();
                if (!prereq.isEmpty() && !prereq.startsWith("<")) {
                    skill.addPrerequisite(prereq);
                }
            }
        }

        // Extract steps
        Pattern stepsPattern = Pattern.compile("(?:STEPS:|IMPROVED_STEPS:)\\s*(.+?)(?:Make steps|Focus on|$)", Pattern.DOTALL);
        Matcher stepsMatcher = stepsPattern.matcher(response);
        if (stepsMatcher.find()) {
            String stepsSection = stepsMatcher.group(1);
            Pattern stepPattern = Pattern.compile("\\d+\\.\\s*(.+)");
            Matcher stepMatcher = stepPattern.matcher(stepsSection);
            while (stepMatcher.find()) {
                String step = stepMatcher.group(1).trim();
                if (!step.isEmpty() && !step.startsWith("<")) {
                    skill.addStep(step);
                }
            }
        }

        LOGGER.debug("Parsed skill: {} with {} steps", name, skill.getSteps().size());
        return skill;
    }

    /**
     * Extract pattern from text.
     */
    private String extractPattern(String text, String pattern) {
        Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    /**
     * Infer skill category from goal type.
     */
    private Skill.SkillCategory inferCategoryFromGoal(Goal goal) {
        switch (goal.getType()) {
            case RESOURCE_GATHERING:
                return Skill.SkillCategory.MINING;
            case BUILD:
                return Skill.SkillCategory.BUILDING;
            case CRAFTING:
                return Skill.SkillCategory.CRAFTING;
            case COMBAT:
                return Skill.SkillCategory.COMBAT;
            case SURVIVAL:
                return Skill.SkillCategory.SURVIVAL;
            case EXPLORATION:
                return Skill.SkillCategory.EXPLORATION;
            case SOCIAL:
                return Skill.SkillCategory.SOCIAL;
            default:
                return Skill.SkillCategory.UTILITY;
        }
    }

    /**
     * Create a basic fallback skill when generation fails.
     */
    private Skill createFallbackSkill(Goal goal) {
        Skill skill = new Skill(
            goal.getDescription(),
            "Basic approach to: " + goal.getDescription(),
            inferCategoryFromGoal(goal),
            5
        );

        // Add generic steps based on goal type
        switch (goal.getType()) {
            case RESOURCE_GATHERING:
                skill.addStep("Scan surroundings for resources");
                skill.addStep("Navigate to nearest resource");
                skill.addStep("Collect the resource");
                break;
            case EXPLORATION:
                skill.addStep("Choose a random direction");
                skill.addStep("Walk 50-100 blocks");
                skill.addStep("Scan for interesting features");
                break;
            case SURVIVAL:
                skill.addStep("Find nearest safe location");
                skill.addStep("Address immediate needs (food/health)");
                skill.addStep("Return to safe behavior");
                break;
            default:
                skill.addStep("Attempt to achieve goal");
                skill.addStep("Monitor progress");
                skill.addStep("Adjust strategy if needed");
                break;
        }

        return skill;
    }
}
