package com.aiplayer.communication;

/**
 * Prompt templates for LLM-based chat responses.
 *
 * Provides structured prompts for different conversation types.
 *
 * Phase 4: Natural Language Communication
 */
public class PromptTemplates {

    /**
     * Build a greeting prompt.
     *
     * @param playerName The player to greet
     * @param context Current AI context
     * @return The prompt string
     */
    public String buildGreetingPrompt(String playerName, String context) {
        return String.format(
            "You are an AI player in Minecraft. A player named '%s' just greeted you.\n\n" +
            "Your Current Status:\n%s\n" +
            "Generate a friendly, brief greeting response (1-2 sentences max). " +
            "Be natural and conversational. Don't mention that you're an AI unless asked.\n\n" +
            "Response:",
            playerName, context
        );
    }

    /**
     * Build a question-answering prompt.
     *
     * @param question The question to answer
     * @param context Current AI context
     * @return The prompt string
     */
    public String buildQuestionPrompt(String question, String context) {
        return String.format(
            "You are an AI player in Minecraft. A player asked you: \"%s\"\n\n" +
            "Your Current Status:\n%s\n" +
            "Answer the question naturally and concisely (1-2 sentences max). " +
            "Be helpful and informative. If you don't know something, say so.\n\n" +
            "Answer:",
            question, context
        );
    }

    /**
     * Build a casual chat prompt.
     *
     * @param message The player's message
     * @param context Current AI context
     * @param conversationContext The conversation history
     * @return The prompt string
     */
    public String buildChatPrompt(String message, String context, ConversationContext conversationContext) {
        String conversationHistory = conversationContext != null
            ? conversationContext.formatForLLM(5)
            : "";

        return String.format(
            "You are an AI player in Minecraft having a casual conversation.\n\n" +
            "Your Current Status:\n%s\n" +
            "%s" +
            "Player said: \"%s\"\n\n" +
            "Respond naturally and conversationally (1-2 sentences max). " +
            "Be friendly and engaging. Match the tone of the conversation.\n\n" +
            "Response:",
            context, conversationHistory, message
        );
    }

    /**
     * Build a generic prompt for unknown message types.
     *
     * @param message The message
     * @param context Current AI context
     * @return The prompt string
     */
    public String buildGenericPrompt(String message, String context) {
        return String.format(
            "You are an AI player in Minecraft. A player said: \"%s\"\n\n" +
            "Your Current Status:\n%s\n" +
            "Respond appropriately to this message (1-2 sentences max). " +
            "Be natural and helpful.\n\n" +
            "Response:",
            message, context
        );
    }

    /**
     * Build a task acceptance prompt.
     *
     * @param taskDescription Description of the task
     * @param canAccept Whether the task can be accepted
     * @return The prompt string
     */
    public String buildTaskAcceptancePrompt(String taskDescription, boolean canAccept) {
        if (canAccept) {
            return String.format(
                "You are an AI player in Minecraft. A player asked you to: \"%s\"\n\n" +
                "Generate a brief confirmation message that you'll do this task (1 sentence). " +
                "Be enthusiastic and helpful.\n\n" +
                "Confirmation:",
                taskDescription
            );
        } else {
            return String.format(
                "You are an AI player in Minecraft. A player asked you to: \"%s\"\n\n" +
                "You cannot complete this task right now. " +
                "Generate a brief, polite rejection message explaining why (1-2 sentences). " +
                "Be apologetic but don't make excuses.\n\n" +
                "Rejection:",
                taskDescription
            );
        }
    }

    /**
     * Build a task completion notification prompt.
     *
     * @param taskDescription The completed task
     * @return The prompt string
     */
    public String buildTaskCompletionPrompt(String taskDescription) {
        return String.format(
            "You are an AI player in Minecraft. You just completed the task: \"%s\"\n\n" +
            "Generate a brief message announcing the completion (1 sentence). " +
            "Be proud but not boastful.\n\n" +
            "Announcement:",
            taskDescription
        );
    }

    /**
     * Build a task failure notification prompt.
     *
     * @param taskDescription The failed task
     * @param reason The reason for failure
     * @return The prompt string
     */
    public String buildTaskFailurePrompt(String taskDescription, String reason) {
        return String.format(
            "You are an AI player in Minecraft. You failed to complete: \"%s\"\n" +
            "Reason: %s\n\n" +
            "Generate a brief apology message (1-2 sentences). " +
            "Be sincere and acknowledge the failure without making excuses.\n\n" +
            "Apology:",
            taskDescription, reason
        );
    }

    /**
     * Build a status report prompt.
     *
     * @param context Current AI context including goals and memories
     * @return The prompt string
     */
    public String buildStatusReportPrompt(String context) {
        return String.format(
            "You are an AI player in Minecraft. A player asked about your status.\n\n" +
            "Your Current Status:\n%s\n" +
            "Generate a brief status report (2-3 sentences). " +
            "Include what you're doing, your health/hunger, and any important activities.\n\n" +
            "Status Report:",
            context
        );
    }

    /**
     * Build a help request prompt.
     *
     * @param helpNeeded What help is needed
     * @param context Current AI context
     * @return The prompt string
     */
    public String buildHelpRequestPrompt(String helpNeeded, String context) {
        return String.format(
            "You are an AI player in Minecraft. You need help with: \"%s\"\n\n" +
            "Your Current Status:\n%s\n" +
            "Generate a brief message asking a nearby player for help (1-2 sentences). " +
            "Be polite and specific about what you need.\n\n" +
            "Request:",
            helpNeeded, context
        );
    }
}
