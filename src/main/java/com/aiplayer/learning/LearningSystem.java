package com.aiplayer.learning;

import com.aiplayer.memory.Memory;
import com.aiplayer.memory.MemorySystem;
import com.aiplayer.planning.Goal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Experience-based learning system (Phase 5).
 *
 * Tracks experiences and learns patterns:
 * - Goal success/failure rates by context
 * - Environmental patterns (time of day, biome, weather)
 * - Action sequences that lead to success
 * - Mistakes to avoid
 *
 * Inspired by reinforcement learning and case-based reasoning.
 *
 * Phase 5: Advanced AI - Learning System
 */
public class LearningSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(LearningSystem.class);

    private final MemorySystem memorySystem;

    // Experience tracking
    private final Map<String, ExperienceRecord> experiences;
    private final List<LearningInsight> insights;

    // Pattern recognition
    private final Map<String, Integer> actionPatterns; // Action sequence → success count
    private final Map<String, Double> contextualSuccess; // Context → success rate

    /**
     * Represents a single experience record.
     */
    public static class ExperienceRecord {
        private final String context;
        private final String action;
        private final boolean success;
        private final long timestamp;
        private final String outcome;

        public ExperienceRecord(String context, String action, boolean success, String outcome) {
            this.context = context;
            this.action = action;
            this.success = success;
            this.outcome = outcome;
            this.timestamp = System.currentTimeMillis();
        }

        public String getContext() { return context; }
        public String getAction() { return action; }
        public boolean isSuccess() { return success; }
        public String getOutcome() { return outcome; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Represents a learned insight.
     */
    public static class LearningInsight {
        public enum InsightType {
            SUCCESS_PATTERN,    // "Action X works well in context Y"
            FAILURE_PATTERN,    // "Action X fails in context Y"
            PREREQUISITE,       // "X requires Y to succeed"
            OPTIMIZATION        // "X is faster/better than Y"
        }

        private final InsightType type;
        private final String description;
        private final double confidence;
        private final int supportingExperiences;

        public LearningInsight(InsightType type, String description, double confidence, int supportingExperiences) {
            this.type = type;
            this.description = description;
            this.confidence = confidence;
            this.supportingExperiences = supportingExperiences;
        }

        public InsightType getType() { return type; }
        public String getDescription() { return description; }
        public double getConfidence() { return confidence; }
        public int getSupportingExperiences() { return supportingExperiences; }

        @Override
        public String toString() {
            return String.format("[%s] %s (confidence: %.0f%%, samples: %d)",
                type, description, confidence * 100, supportingExperiences);
        }
    }

    /**
     * Create a learning system.
     *
     * @param memorySystem The memory system for storing experiences
     */
    public LearningSystem(MemorySystem memorySystem) {
        this.memorySystem = memorySystem;
        this.experiences = new ConcurrentHashMap<>();
        this.insights = new ArrayList<>();
        this.actionPatterns = new ConcurrentHashMap<>();
        this.contextualSuccess = new ConcurrentHashMap<>();

        LOGGER.debug("LearningSystem initialized");
    }

    /**
     * Record an experience.
     *
     * @param context The context (e.g., "night, forest, low health")
     * @param action The action taken
     * @param success Whether it succeeded
     * @param outcome Description of outcome
     */
    public void recordExperience(String context, String action, boolean success, String outcome) {
        String key = context + "::" + action;
        ExperienceRecord record = new ExperienceRecord(context, action, success, outcome);
        experiences.put(key + "::" + System.currentTimeMillis(), record);

        // Update pattern tracking
        updatePatterns(context, action, success);

        // Store in memory
        memorySystem.store(new Memory(
            Memory.MemoryType.EXPERIENCE,
            String.format("%s → %s (%s)",
                action, success ? "SUCCESS" : "FAILURE", outcome),
            success ? 0.6 : 0.8 // Failures are slightly more important to remember
        ));

        LOGGER.debug("Recorded experience: {} in context '{}' → {}",
            action, context, success ? "success" : "failure");

        // Periodically analyze patterns
        if (experiences.size() % 10 == 0) {
            analyzePatternsAndGenerateInsights();
        }
    }

    /**
     * Record a goal completion.
     *
     * @param goal The completed goal
     * @param success Whether it succeeded
     * @param context Current context
     */
    public void recordGoalCompletion(Goal goal, boolean success, String context) {
        String goalType = goal.getType().toString();
        recordExperience(context, "Complete goal: " + goalType, success,
            success ? "Goal achieved" : "Goal failed");

        // Update semantic memory with goal success rates
        String key = "goal_success_" + goalType;
        Double currentRate = contextualSuccess.get(key);
        if (currentRate == null) {
            contextualSuccess.put(key, success ? 1.0 : 0.0);
        } else {
            // Running average
            contextualSuccess.put(key, currentRate * 0.9 + (success ? 0.1 : 0.0));
        }
    }

    /**
     * Update pattern tracking.
     */
    private void updatePatterns(String context, String action, boolean success) {
        if (success) {
            String pattern = context + " → " + action;
            actionPatterns.merge(pattern, 1, Integer::sum);
        }

        // Update contextual success rates
        String contextKey = context + "_success";
        Double currentRate = contextualSuccess.get(contextKey);
        if (currentRate == null) {
            contextualSuccess.put(contextKey, success ? 1.0 : 0.0);
        } else {
            // Exponential moving average
            contextualSuccess.put(contextKey, currentRate * 0.95 + (success ? 0.05 : 0.0));
        }
    }

    /**
     * Analyze patterns and generate insights.
     */
    private void analyzePatternsAndGenerateInsights() {
        insights.clear();

        // Find successful patterns (high success rate, multiple uses)
        for (Map.Entry<String, Integer> entry : actionPatterns.entrySet()) {
            if (entry.getValue() >= 3) { // At least 3 successes
                insights.add(new LearningInsight(
                    LearningInsight.InsightType.SUCCESS_PATTERN,
                    entry.getKey() + " works well",
                    Math.min(0.95, entry.getValue() / 10.0),
                    entry.getValue()
                ));
            }
        }

        // Find contexts with high/low success rates
        for (Map.Entry<String, Double> entry : contextualSuccess.entrySet()) {
            if (entry.getValue() > 0.8) {
                insights.add(new LearningInsight(
                    LearningInsight.InsightType.SUCCESS_PATTERN,
                    "High success in: " + entry.getKey().replace("_success", ""),
                    entry.getValue(),
                    10 // Estimate
                ));
            } else if (entry.getValue() < 0.3) {
                insights.add(new LearningInsight(
                    LearningInsight.InsightType.FAILURE_PATTERN,
                    "Low success in: " + entry.getKey().replace("_success", ""),
                    1.0 - entry.getValue(),
                    10 // Estimate
                ));
            }
        }

        LOGGER.debug("Generated {} learning insights", insights.size());
    }

    /**
     * Get recommendations for a given context.
     *
     * @param context The current context
     * @return List of recommended actions based on past experience
     */
    public List<String> getRecommendations(String context) {
        List<String> recommendations = new ArrayList<>();

        // Find patterns that match the context
        for (Map.Entry<String, Integer> entry : actionPatterns.entrySet()) {
            String pattern = entry.getKey();
            if (pattern.startsWith(context)) {
                // Extract action from pattern
                String action = pattern.substring(pattern.indexOf("→") + 2);
                recommendations.add(action);
            }
        }

        // Sort by success count
        recommendations.sort((a, b) -> {
            int countA = actionPatterns.getOrDefault(context + " → " + a, 0);
            int countB = actionPatterns.getOrDefault(context + " → " + b, 0);
            return Integer.compare(countB, countA);
        });

        return recommendations;
    }

    /**
     * Get all learned insights.
     *
     * @return List of insights
     */
    public List<LearningInsight> getInsights() {
        return new ArrayList<>(insights);
    }

    /**
     * Get insights for LLM context.
     *
     * @param maxInsights Maximum number of insights to include
     * @return Formatted insights string
     */
    public String formatInsightsForLLM(int maxInsights) {
        if (insights.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## Learned Insights\n");

        // Sort by confidence
        List<LearningInsight> sortedInsights = new ArrayList<>(insights);
        sortedInsights.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));

        int count = 0;
        for (LearningInsight insight : sortedInsights) {
            if (count >= maxInsights) break;

            if (insight.getConfidence() >= 0.5) { // Only include confident insights
                sb.append(String.format("- %s\n", insight.getDescription()));
                count++;
            }
        }

        return sb.toString();
    }

    /**
     * Get total number of experiences.
     *
     * @return Experience count
     */
    public int getExperienceCount() {
        return experiences.size();
    }

    /**
     * Get number of insights.
     *
     * @return Insight count
     */
    public int getInsightCount() {
        return insights.size();
    }

    /**
     * Get statistics.
     *
     * @return Statistics string
     */
    public String getStatistics() {
        int totalExperiences = experiences.size();
        int successCount = (int) experiences.values().stream()
            .filter(ExperienceRecord::isSuccess)
            .count();
        double overallSuccessRate = totalExperiences > 0
            ? (double) successCount / totalExperiences
            : 0.0;

        return String.format("Experiences: %d, Success Rate: %.0f%%, Insights: %d, Patterns: %d",
            totalExperiences,
            overallSuccessRate * 100,
            insights.size(),
            actionPatterns.size());
    }

    /**
     * Clear old experiences (keep only recent ones).
     *
     * @param maxAge Maximum age in milliseconds
     */
    public void cleanupOldExperiences(long maxAge) {
        long cutoff = System.currentTimeMillis() - maxAge;
        experiences.entrySet().removeIf(entry ->
            entry.getValue().getTimestamp() < cutoff
        );

        LOGGER.debug("Cleaned up old experiences, {} remaining", experiences.size());
    }
}
