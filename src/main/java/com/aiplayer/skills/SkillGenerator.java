package com.aiplayer.skills;

import com.aiplayer.llm.LLMProvider;
import com.aiplayer.llm.LLMRequest;
import com.aiplayer.llm.LLMResponse;
import com.aiplayer.memory.Memory;
import com.aiplayer.memory.MemorySystem;
import com.aiplayer.perception.WorldState;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Skill Generator - Creates new skills using LLM.
 *
 * Phase 5: Advanced AI
 *
 * Generates new skills based on:
 * - Successful action sequences
 * - Failed attempts (learn what NOT to do)
 * - Observed player behaviors
 * - Environmental patterns
 * - Goal achievement strategies
 *
 * Voyager-Inspired Approach:
 * - Generate skills from experience
 * - Iteratively refine based on execution results
 * - Compose simple skills into complex ones
 * - Validate skills before adding to library
 */
public class SkillGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkillGenerator.class);

    private final LLMProvider llmProvider;
    private final MemorySystem memorySystem;
    private final SkillLibrary skillLibrary;

    // Skill generation parameters
    private static final int MIN_SKILL_STEPS = 2;
    private static final int MAX_SKILL_STEPS = 10;
    private static final double MIN_CONFIDENCE_THRESHOLD = 0.6;

    public SkillGenerator(LLMProvider llmProvider, MemorySystem memorySystem, SkillLibrary skillLibrary) {
        this.llmProvider = llmProvider;
        this.memorySystem = memorySystem;
        this.skillLibrary = skillLibrary;
    }

    /**
     * Generate a new skill from a successful action sequence.
     *
     * @param actionSequence List of actions that succeeded
     * @param goalAchieved What was accomplished
     * @param worldState Current world state
     * @return Future containing generated skill (or null if generation failed)
     */
    public CompletableFuture<Skill> generateFromSuccess(
            List<String> actionSequence,
            String goalAchieved,
            WorldState worldState) {

        LOGGER.info("Generating skill from successful sequence: {}", goalAchieved);

        String prompt = buildSuccessPrompt(actionSequence, goalAchieved, worldState);

        return llmProvider.generateResponse(new LLMRequest(prompt, 0.7, 800))
                .thenApply(response -> parseSkillFromResponse(response))
                .exceptionally(ex -> {
                    LOGGER.error("Failed to generate skill from success", ex);
                    return null;
                });
    }

    /**
     * Generate a refined skill based on execution failure.
     *
     * @param originalSkill The skill that failed
     * @param failureReason Why it failed
     * @param worldState Current world state
     * @return Future containing refined skill
     */
    public CompletableFuture<Skill> refineSkillFromFailure(
            Skill originalSkill,
            String failureReason,
            WorldState worldState) {

        LOGGER.info("Refining skill '{}' after failure: {}", originalSkill.getName(), failureReason);

        String prompt = buildRefinementPrompt(originalSkill, failureReason, worldState);

        return llmProvider.generateResponse(new LLMRequest(prompt, 0.7, 800))
                .thenApply(response -> parseSkillFromResponse(response))
                .exceptionally(ex -> {
                    LOGGER.error("Failed to refine skill", ex);
                    return originalSkill; // Return original if refinement fails
                });
    }

    /**
     * Generate a new skill by observing player actions.
     *
     * @param observedActions Actions observed from a player
     * @param playerName Name of observed player
     * @param outcome What the player achieved
     * @return Future containing generated skill
     */
    public CompletableFuture<Skill> generateFromObservation(
            List<String> observedActions,
            String playerName,
            String outcome) {

        LOGGER.info("Generating skill from observing player: {}", playerName);

        String prompt = buildObservationPrompt(observedActions, playerName, outcome);

        return llmProvider.generateResponse(new LLMRequest(prompt, 0.7, 800))
                .thenApply(response -> parseSkillFromResponse(response))
                .exceptionally(ex -> {
                    LOGGER.error("Failed to generate skill from observation", ex);
                    return null;
                });
    }

    /**
     * Compose a complex skill from multiple simple skills.
     *
     * @param simpleSkills List of simple skills to compose
     * @param complexGoal The complex goal to achieve
     * @return Future containing composed skill
     */
    public CompletableFuture<Skill> composeComplexSkill(
            List<Skill> simpleSkills,
            String complexGoal) {

        LOGGER.info("Composing complex skill for goal: {}", complexGoal);

        String prompt = buildCompositionPrompt(simpleSkills, complexGoal);

        return llmProvider.generateResponse(new LLMRequest(prompt, 0.7, 1000))
                .thenApply(response -> parseSkillFromResponse(response))
                .exceptionally(ex -> {
                    LOGGER.error("Failed to compose complex skill", ex);
                    return null;
                });
    }

    /**
     * Build LLM prompt for generating skill from successful action sequence.
     */
    private String buildSuccessPrompt(List<String> actions, String goal, WorldState worldState) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are helping an AI player in Minecraft learn new skills.\n\n");
        prompt.append("The AI just successfully accomplished: ").append(goal).append("\n\n");
        prompt.append("Action sequence that worked:\n");
        for (int i = 0; i < actions.size(); i++) {
            prompt.append((i + 1)).append(". ").append(actions.get(i)).append("\n");
        }
        prompt.append("\n");

        if (worldState != null) {
            prompt.append("Context:\n");
            prompt.append("- Position: ").append(worldState.getPlayerPosition()).append("\n");
            prompt.append("- Biome: ").append(worldState.getBiome()).append("\n");
            prompt.append("- Time: ").append(worldState.getTimeOfDay()).append("\n");
        }

        prompt.append("\nGenerate a reusable skill that captures this successful pattern.\n\n");
        prompt.append("Return a JSON object with this exact structure:\n");
        prompt.append("{\n");
        prompt.append("  \"name\": \"Skill name (concise, descriptive)\",\n");
        prompt.append("  \"description\": \"What this skill does\",\n");
        prompt.append("  \"category\": \"One of: MINING, BUILDING, CRAFTING, COMBAT, SURVIVAL, EXPLORATION, SOCIAL, FARMING, UTILITY\",\n");
        prompt.append("  \"complexity\": 1-10 (how difficult),\n");
        prompt.append("  \"prerequisites\": [\"required items or conditions\"],\n");
        prompt.append("  \"steps\": [\"step 1\", \"step 2\", ...],\n");
        prompt.append("  \"expectedOutcome\": \"What should be achieved\"\n");
        prompt.append("}\n\n");
        prompt.append("Make steps clear, actionable, and generalizable to similar situations.");

        return prompt.toString();
    }

    /**
     * Build LLM prompt for refining a failed skill.
     */
    private String buildRefinementPrompt(Skill skill, String failureReason, WorldState worldState) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are helping an AI player in Minecraft improve a skill that failed.\n\n");
        prompt.append("Skill that failed: ").append(skill.getName()).append("\n");
        prompt.append("Description: ").append(skill.getDescription()).append("\n\n");

        prompt.append("Current steps:\n");
        List<String> steps = skill.getSteps();
        for (int i = 0; i < steps.size(); i++) {
            prompt.append((i + 1)).append(". ").append(steps.get(i)).append("\n");
        }
        prompt.append("\n");

        prompt.append("Failure reason: ").append(failureReason).append("\n\n");

        if (worldState != null) {
            prompt.append("Context when it failed:\n");
            prompt.append("- Position: ").append(worldState.getPlayerPosition()).append("\n");
            prompt.append("- Biome: ").append(worldState.getBiome()).append("\n");
            prompt.append("- Time: ").append(worldState.getTimeOfDay()).append("\n");
        }

        prompt.append("\nAnalyze the failure and generate an improved version of this skill.\n\n");
        prompt.append("Return a JSON object with the same structure as before:\n");
        prompt.append("{\n");
        prompt.append("  \"name\": \"Improved skill name\",\n");
        prompt.append("  \"description\": \"Updated description\",\n");
        prompt.append("  \"category\": \"").append(skill.getCategory()).append("\",\n");
        prompt.append("  \"complexity\": 1-10,\n");
        prompt.append("  \"prerequisites\": [\"requirements\"],\n");
        prompt.append("  \"steps\": [\"improved step 1\", \"improved step 2\", ...],\n");
        prompt.append("  \"expectedOutcome\": \"What should be achieved\"\n");
        prompt.append("}\n\n");
        prompt.append("Focus on fixing the specific failure point while keeping what worked.");

        return prompt.toString();
    }

    /**
     * Build LLM prompt for generating skill from observation.
     */
    private String buildObservationPrompt(List<String> actions, String playerName, String outcome) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are helping an AI player in Minecraft learn by observing other players.\n\n");
        prompt.append("Observed player: ").append(playerName).append("\n");
        prompt.append("What they achieved: ").append(outcome).append("\n\n");

        prompt.append("Observed actions:\n");
        for (int i = 0; i < actions.size(); i++) {
            prompt.append((i + 1)).append(". ").append(actions.get(i)).append("\n");
        }
        prompt.append("\n");

        prompt.append("Generate a skill that the AI can learn from this observation.\n\n");
        prompt.append("Return a JSON object:\n");
        prompt.append("{\n");
        prompt.append("  \"name\": \"Skill name\",\n");
        prompt.append("  \"description\": \"What this skill does\",\n");
        prompt.append("  \"category\": \"Appropriate category\",\n");
        prompt.append("  \"complexity\": 1-10,\n");
        prompt.append("  \"prerequisites\": [\"requirements\"],\n");
        prompt.append("  \"steps\": [\"step 1\", \"step 2\", ...],\n");
        prompt.append("  \"expectedOutcome\": \"What should be achieved\"\n");
        prompt.append("}\n");

        return prompt.toString();
    }

    /**
     * Build LLM prompt for composing complex skill from simple skills.
     */
    private String buildCompositionPrompt(List<Skill> simpleSkills, String complexGoal) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are helping an AI player in Minecraft create a complex skill by combining simpler skills.\n\n");
        prompt.append("Complex goal to achieve: ").append(complexGoal).append("\n\n");

        prompt.append("Available simple skills:\n");
        for (int i = 0; i < simpleSkills.size(); i++) {
            Skill skill = simpleSkills.get(i);
            prompt.append((i + 1)).append(". ").append(skill.getName())
                    .append(" - ").append(skill.getDescription()).append("\n");
            prompt.append("   Steps: ").append(String.join(", ", skill.getSteps())).append("\n");
        }
        prompt.append("\n");

        prompt.append("Create a new skill that composes these simpler skills to achieve the complex goal.\n\n");
        prompt.append("Return a JSON object:\n");
        prompt.append("{\n");
        prompt.append("  \"name\": \"Complex skill name\",\n");
        prompt.append("  \"description\": \"What this skill does\",\n");
        prompt.append("  \"category\": \"Appropriate category\",\n");
        prompt.append("  \"complexity\": 1-10 (higher than component skills),\n");
        prompt.append("  \"prerequisites\": [\"combined requirements\"],\n");
        prompt.append("  \"steps\": [\"orchestrated steps that use the simple skills\"],\n");
        prompt.append("  \"expectedOutcome\": \"Complex goal achievement\"\n");
        prompt.append("}\n\n");
        prompt.append("The steps should reference or incorporate the simple skills appropriately.");

        return prompt.toString();
    }

    /**
     * Parse LLM response into a Skill object.
     *
     * Handles both JSON and natural language responses.
     */
    private Skill parseSkillFromResponse(LLMResponse response) {
        if (response == null || response.getText() == null) {
            LOGGER.warn("Null response from LLM");
            return null;
        }

        String text = response.getText().trim();

        try {
            // Try to extract JSON from response (may be wrapped in markdown code blocks)
            String jsonText = extractJson(text);

            JsonObject json = JsonParser.parseString(jsonText).getAsJsonObject();

            // Extract required fields
            String name = json.get("name").getAsString();
            String description = json.get("description").getAsString();
            String categoryStr = json.get("category").getAsString();
            int complexity = json.get("complexity").getAsInt();
            String expectedOutcome = json.get("expectedOutcome").getAsString();

            // Parse category
            Skill.SkillCategory category;
            try {
                category = Skill.SkillCategory.valueOf(categoryStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid category '{}', defaulting to UTILITY", categoryStr);
                category = Skill.SkillCategory.UTILITY;
            }

            // Parse prerequisites
            List<String> prerequisites = new ArrayList<>();
            JsonArray prereqArray = json.getAsJsonArray("prerequisites");
            for (int i = 0; i < prereqArray.size(); i++) {
                prerequisites.add(prereqArray.get(i).getAsString());
            }

            // Parse steps
            List<String> steps = new ArrayList<>();
            JsonArray stepsArray = json.getAsJsonArray("steps");
            for (int i = 0; i < stepsArray.size(); i++) {
                steps.add(stepsArray.get(i).getAsString());
            }

            // Validate skill
            if (!validateSkill(name, steps, complexity)) {
                LOGGER.warn("Generated skill failed validation");
                return null;
            }

            // Create skill
            Skill skill = new Skill(name, description, category, prerequisites, steps, complexity);

            LOGGER.info("Successfully generated skill: {}", name);

            // Store in memory
            memorySystem.store(new Memory(
                Memory.MemoryType.LEARNING,
                String.format("Generated new skill: %s (complexity %d, %d steps)",
                    name, complexity, steps.size()),
                0.8
            ));

            return skill;

        } catch (Exception e) {
            LOGGER.error("Failed to parse skill from LLM response: " + text, e);
            return null;
        }
    }

    /**
     * Extract JSON from text that may contain markdown code blocks.
     */
    private String extractJson(String text) {
        // Remove markdown code blocks if present
        Pattern codeBlockPattern = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");
        Matcher matcher = codeBlockPattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // Try to find JSON object directly
        int startIdx = text.indexOf('{');
        int endIdx = text.lastIndexOf('}');

        if (startIdx >= 0 && endIdx > startIdx) {
            return text.substring(startIdx, endIdx + 1);
        }

        return text;
    }

    /**
     * Validate a generated skill before adding to library.
     */
    private boolean validateSkill(String name, List<String> steps, int complexity) {
        // Check name
        if (name == null || name.trim().isEmpty()) {
            LOGGER.warn("Skill has empty name");
            return false;
        }

        // Check steps count
        if (steps.size() < MIN_SKILL_STEPS) {
            LOGGER.warn("Skill has too few steps: {}", steps.size());
            return false;
        }

        if (steps.size() > MAX_SKILL_STEPS) {
            LOGGER.warn("Skill has too many steps: {}", steps.size());
            return false;
        }

        // Check complexity range
        if (complexity < 1 || complexity > 10) {
            LOGGER.warn("Skill complexity out of range: {}", complexity);
            return false;
        }

        // All validations passed
        return true;
    }

    /**
     * Generate and add a skill to the library from successful actions.
     *
     * Convenience method that generates and immediately adds to library.
     */
    public CompletableFuture<Boolean> learnFromSuccess(
            List<String> actions,
            String goal,
            WorldState worldState) {

        return generateFromSuccess(actions, goal, worldState)
                .thenApply(skill -> {
                    if (skill != null) {
                        skillLibrary.addSkill(skill);
                        LOGGER.info("Learned new skill: {}", skill.getName());
                        return true;
                    }
                    return false;
                });
    }

    /**
     * Refine and replace a skill in the library.
     */
    public CompletableFuture<Boolean> improveSkill(
            Skill originalSkill,
            String failureReason,
            WorldState worldState) {

        return refineSkillFromFailure(originalSkill, failureReason, worldState)
                .thenApply(refinedSkill -> {
                    if (refinedSkill != null && refinedSkill != originalSkill) {
                        // Remove old version and add refined version
                        skillLibrary.addSkill(refinedSkill);
                        LOGGER.info("Improved skill: {} -> {}",
                            originalSkill.getName(), refinedSkill.getName());
                        return true;
                    }
                    return false;
                });
    }
}
