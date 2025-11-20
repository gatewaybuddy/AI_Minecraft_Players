package com.aiplayer.skills;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates generated skills for correctness and safety (Phase 5).
 *
 * Ensures skills are:
 * - Structurally valid (have name, steps, etc.)
 * - Logically sound (steps make sense)
 * - Safe (don't cause self-harm)
 * - Executable (actions are possible in Minecraft)
 *
 * Phase 5: Advanced AI - Skill Library Enhancement
 */
public class SkillValidator {

    /**
     * Result of skill validation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> issues;

        private ValidationResult(boolean valid, List<String> issues) {
            this.valid = valid;
            this.issues = issues;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, new ArrayList<>());
        }

        public static ValidationResult invalid(List<String> issues) {
            return new ValidationResult(false, issues);
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getIssues() {
            return issues;
        }

        @Override
        public String toString() {
            if (valid) {
                return "Valid";
            }
            return "Invalid: " + String.join(", ", issues);
        }
    }

    /**
     * Validate a skill.
     *
     * @param skill The skill to validate
     * @return Validation result
     */
    public ValidationResult validate(Skill skill) {
        List<String> issues = new ArrayList<>();

        // Check basic structure
        if (skill.getName() == null || skill.getName().trim().isEmpty()) {
            issues.add("Skill must have a name");
        }

        if (skill.getDescription() == null || skill.getDescription().trim().isEmpty()) {
            issues.add("Skill must have a description");
        }

        if (skill.getSteps().isEmpty()) {
            issues.add("Skill must have at least one step");
        }

        // Check complexity range
        if (skill.getComplexity() < 1 || skill.getComplexity() > 10) {
            issues.add("Complexity must be between 1 and 10");
        }

        // Check for safety issues
        if (hasSafetyIssues(skill)) {
            issues.add("Skill contains unsafe actions");
        }

        // Check for logical issues
        if (hasLogicalIssues(skill)) {
            issues.add("Skill has illogical step sequence");
        }

        // Check if steps are too vague
        if (hasVagueSteps(skill)) {
            issues.add("Skill has overly vague steps");
        }

        if (issues.isEmpty()) {
            return ValidationResult.valid();
        } else {
            return ValidationResult.invalid(issues);
        }
    }

    /**
     * Check if skill has safety issues.
     */
    private boolean hasSafetyIssues(Skill skill) {
        List<String> steps = skill.getSteps();

        // List of unsafe patterns
        String[] unsafePatterns = {
            "jump.*lava",
            "walk.*cliff",
            "attack.*player",
            "destroy.*own",
            "fall.*void",
            "eat.*poison",
            "drink.*harm"
        };

        for (String step : steps) {
            String lowerStep = step.toLowerCase();
            for (String pattern : unsafePatterns) {
                if (lowerStep.matches(".*" + pattern + ".*")) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if skill has logical issues.
     */
    private boolean hasLogicalIssues(Skill skill) {
        List<String> steps = skill.getSteps();

        if (steps.size() < 2) {
            return false; // Can't check logic with 1 step
        }

        // Check for impossible sequences
        // Example: "Craft iron tools" before "Mine iron ore"
        for (int i = 1; i < steps.size(); i++) {
            String prevStep = steps.get(i - 1).toLowerCase();
            String currStep = steps.get(i).toLowerCase();

            // Check if current step requires result of later step
            if (currStep.contains("craft") && prevStep.contains("use")) {
                // Using something before crafting it
                return true;
            }

            if (currStep.contains("mine") && prevStep.contains("need") && prevStep.contains("pickaxe")) {
                // Mining without pickaxe
                return true;
            }
        }

        return false;
    }

    /**
     * Check if skill has vague steps.
     */
    private boolean hasVagueSteps(Skill skill) {
        List<String> steps = skill.getSteps();

        // List of vague phrases that make steps non-executable
        String[] vaguePatterns = {
            "somehow",
            "figure out",
            "try to",
            "maybe",
            "if possible",
            "attempt",
            "consider",
            "think about"
        };

        int vagueCount = 0;
        for (String step : steps) {
            String lowerStep = step.toLowerCase();
            for (String pattern : vaguePatterns) {
                if (lowerStep.contains(pattern)) {
                    vagueCount++;
                    break;
                }
            }
        }

        // Allow some vagueness, but not if >50% of steps are vague
        return vagueCount > steps.size() / 2;
    }

    /**
     * Get a score for skill quality (0-100).
     *
     * Based on validation criteria.
     *
     * @param skill The skill to score
     * @return Quality score
     */
    public int getQualityScore(Skill skill) {
        int score = 100;

        // Deduct for structural issues
        if (skill.getName() == null || skill.getName().trim().isEmpty()) {
            score -= 30;
        }

        if (skill.getDescription() == null || skill.getDescription().trim().isEmpty()) {
            score -= 20;
        }

        if (skill.getSteps().isEmpty()) {
            score -= 50;
        } else if (skill.getSteps().size() < 3) {
            score -= 10; // Very simple skills are less useful
        }

        // Deduct for safety/logical issues
        if (hasSafetyIssues(skill)) {
            score -= 40;
        }

        if (hasLogicalIssues(skill)) {
            score -= 30;
        }

        if (hasVagueSteps(skill)) {
            score -= 20;
        }

        // Bonus for good structure
        if (!skill.getPrerequisites().isEmpty()) {
            score += 10; // Well-defined prerequisites
        }

        if (skill.getSteps().size() >= 5 && skill.getSteps().size() <= 10) {
            score += 10; // Good level of detail
        }

        return Math.max(0, Math.min(100, score));
    }
}
