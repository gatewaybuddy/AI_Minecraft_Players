package com.aiplayer.communication;

import com.aiplayer.core.AIPlayerEntity;
import com.aiplayer.llm.LLMProvider;
import com.aiplayer.planning.Goal;
import com.aiplayer.planning.PlanningEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Handles task requests from players.
 *
 * Parses player requests and converts them into Goals for the AI to execute.
 *
 * Phase 4: Natural Language Communication
 */
public class TaskRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskRequestHandler.class);

    private final AIPlayerEntity aiPlayer;
    private final LLMProvider llmProvider;
    private final PromptTemplates promptTemplates;

    /**
     * Result of processing a task request.
     */
    public static class TaskRequestResult {
        private final boolean accepted;
        private final String taskDescription;
        private final String confirmationMessage;
        private final String rejectionReason;
        private final Goal goal;

        private TaskRequestResult(boolean accepted, String taskDescription,
                                 String confirmationMessage, String rejectionReason,
                                 Goal goal) {
            this.accepted = accepted;
            this.taskDescription = taskDescription;
            this.confirmationMessage = confirmationMessage;
            this.rejectionReason = rejectionReason;
            this.goal = goal;
        }

        public static TaskRequestResult accepted(String taskDescription, String confirmation, Goal goal) {
            return new TaskRequestResult(true, taskDescription, confirmation, null, goal);
        }

        public static TaskRequestResult rejected(String taskDescription, String reason) {
            return new TaskRequestResult(false, taskDescription, null, reason, null);
        }

        public boolean isAccepted() {
            return accepted;
        }

        public String getTaskDescription() {
            return taskDescription;
        }

        public String getConfirmationMessage() {
            return confirmationMessage;
        }

        public String getRejectionReason() {
            return rejectionReason;
        }

        public Goal getGoal() {
            return goal;
        }
    }

    /**
     * Create a task request handler.
     *
     * @param aiPlayer The AI player
     * @param llmProvider The LLM provider (can be null)
     */
    public TaskRequestHandler(AIPlayerEntity aiPlayer, LLMProvider llmProvider) {
        this.aiPlayer = aiPlayer;
        this.llmProvider = llmProvider;
        this.promptTemplates = new PromptTemplates();

        LOGGER.debug("TaskRequestHandler initialized for AI player: {}",
            aiPlayer.getGameProfile().getName());
    }

    /**
     * Handle a task request from a player.
     *
     * @param message The request message
     * @param intent The classified intent
     * @param context The conversation context
     * @return The result of processing the request
     */
    public CompletableFuture<TaskRequestResult> handleRequest(String message,
                                                              NLUEngine.Intent intent,
                                                              ConversationContext context) {
        LOGGER.debug("Handling task request: {}", message);

        // Parse the request into a goal
        Goal goal = parseRequestToGoal(message, intent);

        // Check if the task can be accepted
        boolean canAccept = canAcceptTask(goal);

        if (canAccept) {
            // Add goal to planning engine
            if (aiPlayer.getBrain() != null && aiPlayer.getBrain().getPlanningEngine() != null) {
                PlanningEngine planner = aiPlayer.getBrain().getPlanningEngine();
                planner.addGoal(goal);

                LOGGER.info("AI {} accepted task: {}",
                    aiPlayer.getGameProfile().getName(), goal.getDescription());
            }

            // Generate confirmation message
            String confirmation = generateConfirmation(goal.getDescription(), true);
            return CompletableFuture.completedFuture(
                TaskRequestResult.accepted(goal.getDescription(), confirmation, goal)
            );
        } else {
            // Generate rejection message
            String reason = "I'm not able to do that right now.";
            String rejection = generateConfirmation(goal.getDescription(), false);
            return CompletableFuture.completedFuture(
                TaskRequestResult.rejected(goal.getDescription(), rejection)
            );
        }
    }

    /**
     * Parse a request message into a Goal.
     *
     * @param message The request message
     * @param intent The classified intent
     * @return A Goal representing the request
     */
    private Goal parseRequestToGoal(String message, NLUEngine.Intent intent) {
        String action = (String) intent.getEntity("action");
        Object quantityObj = intent.getEntity("quantity");
        String item = (String) intent.getEntity("item");

        // Build goal description
        StringBuilder description = new StringBuilder();

        if (action != null) {
            switch (action) {
                case "gather":
                case "mine":
                    description.append("Gather ");
                    break;
                case "build":
                    description.append("Build ");
                    break;
                case "craft":
                    description.append("Craft ");
                    break;
                case "find":
                    description.append("Find ");
                    break;
                case "combat":
                    description.append("Hunt ");
                    break;
                default:
                    description.append("Complete task: ");
                    break;
            }
        }

        if (quantityObj != null) {
            description.append(quantityObj).append(" ");
        }

        if (item != null) {
            description.append(item);
        }

        // If description is still empty, use the original message
        if (description.length() == 0) {
            description.append(message);
        }

        // Determine goal type
        Goal.GoalType goalType = determineGoalType(action, message);

        // Create goal with high priority (player-requested tasks are important)
        Goal goal = new Goal(description.toString().trim(), goalType, 9);

        // Store request details in goal metadata (if available in Goal class)
        LOGGER.debug("Parsed goal: {} (type: {})", goal.getDescription(), goalType);

        return goal;
    }

    /**
     * Determine the goal type from action and message.
     */
    private Goal.GoalType determineGoalType(String action, String message) {
        if (action != null) {
            switch (action) {
                case "gather":
                case "mine":
                    return Goal.GoalType.RESOURCE_GATHERING;
                case "build":
                    return Goal.GoalType.BUILD;
                case "craft":
                    return Goal.GoalType.CRAFTING;
                case "find":
                    return Goal.GoalType.EXPLORATION;
                case "combat":
                    return Goal.GoalType.COMBAT;
            }
        }

        // Keyword-based fallback
        String lower = message.toLowerCase();
        if (lower.contains("build") || lower.contains("construct")) {
            return Goal.GoalType.BUILD;
        } else if (lower.contains("gather") || lower.contains("collect") || lower.contains("mine")) {
            return Goal.GoalType.RESOURCE_GATHERING;
        } else if (lower.contains("craft") || lower.contains("make")) {
            return Goal.GoalType.CRAFTING;
        } else if (lower.contains("find") || lower.contains("explore") || lower.contains("locate")) {
            return Goal.GoalType.EXPLORATION;
        } else if (lower.contains("fight") || lower.contains("kill") || lower.contains("attack")) {
            return Goal.GoalType.COMBAT;
        }

        // Default to exploration
        return Goal.GoalType.EXPLORATION;
    }

    /**
     * Check if a task can be accepted.
     *
     * @param goal The goal to check
     * @return true if the task can be accepted
     */
    private boolean canAcceptTask(Goal goal) {
        // Check if AI is in intelligent mode
        if (aiPlayer.getBrain() == null || !aiPlayer.getBrain().isIntelligentMode()) {
            LOGGER.debug("Cannot accept task - AI in SIMPLE mode");
            return false;
        }

        // Check if planning engine is available
        if (aiPlayer.getBrain().getPlanningEngine() == null) {
            LOGGER.debug("Cannot accept task - Planning engine unavailable");
            return false;
        }

        // Check health (don't accept tasks if critically low health)
        if (aiPlayer.getHealth() < aiPlayer.getMaxHealth() * 0.2) {
            LOGGER.debug("Cannot accept task - Health too low");
            return false;
        }

        // Add more validation as needed:
        // - Check if task is possible given current situation
        // - Check if AI has necessary resources/tools
        // - Check if task conflicts with current goals

        return true;
    }

    /**
     * Generate a confirmation or rejection message.
     */
    private String generateConfirmation(String taskDescription, boolean accepted) {
        if (llmProvider != null) {
            // Use LLM for natural responses
            String prompt = promptTemplates.buildTaskAcceptancePrompt(taskDescription, accepted);
            try {
                return llmProvider.complete(prompt, com.aiplayer.llm.LLMOptions.chat())
                    .thenApply(response -> cleanResponse(response))
                    .get(); // Block for simplicity
            } catch (Exception e) {
                LOGGER.error("Error generating task confirmation", e);
                // Fall through to fallback
            }
        }

        // Fallback messages
        if (accepted) {
            return String.format("Sure, I'll %s!", taskDescription.toLowerCase());
        } else {
            return String.format("Sorry, I can't %s right now.", taskDescription.toLowerCase());
        }
    }

    /**
     * Clean up LLM response.
     */
    private String cleanResponse(String response) {
        if (response == null || response.isEmpty()) {
            return "";
        }

        String cleaned = response.trim();

        // Remove quotes
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }

        // Ensure punctuation
        if (!cleaned.matches(".*[.!?]$")) {
            cleaned += "!";
        }

        return cleaned;
    }
}
