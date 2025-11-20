package com.aiplayer.communication;

import com.aiplayer.core.AIPlayerEntity;
import com.aiplayer.llm.LLMOptions;
import com.aiplayer.llm.LLMProvider;
import com.aiplayer.memory.Memory;
import com.aiplayer.memory.MemorySystem;
import com.aiplayer.perception.WorldState;
import com.aiplayer.planning.Goal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Generates natural language responses using LLM.
 *
 * Handles different response types:
 * - Greetings
 * - Question answering
 * - Casual conversation
 * - Status updates
 *
 * Phase 4: Natural Language Communication
 */
public class ResponseGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseGenerator.class);

    private final LLMProvider llmProvider;
    private final PromptTemplates promptTemplates;

    // Maximum response length (characters)
    private static final int MAX_RESPONSE_LENGTH = 200;

    /**
     * Create a response generator.
     *
     * @param llmProvider The LLM provider for generating responses
     */
    public ResponseGenerator(LLMProvider llmProvider) {
        this.llmProvider = llmProvider;
        this.promptTemplates = new PromptTemplates();

        LOGGER.debug("ResponseGenerator initialized with provider: {}",
            llmProvider != null ? llmProvider.getProviderName() : "NONE");
    }

    /**
     * Generate a greeting response.
     *
     * @param playerName The player to greet
     * @param aiPlayer The AI player
     * @return Greeting message
     */
    public CompletableFuture<String> generateGreeting(String playerName, AIPlayerEntity aiPlayer) {
        if (llmProvider == null) {
            return CompletableFuture.completedFuture(getFallbackGreeting(playerName));
        }

        String context = buildBasicContext(aiPlayer);
        String prompt = promptTemplates.buildGreetingPrompt(playerName, context);

        return llmProvider.complete(prompt, LLMOptions.chat())
            .thenApply(this::cleanResponse)
            .exceptionally(e -> {
                LOGGER.error("Error generating greeting", e);
                return getFallbackGreeting(playerName);
            });
    }

    /**
     * Answer a question from a player.
     *
     * @param question The question
     * @param intent The classified intent
     * @param conversationContext The conversation context
     * @param aiPlayer The AI player
     * @return Answer to the question
     */
    public CompletableFuture<String> answerQuestion(String question,
                                                    NLUEngine.Intent intent,
                                                    ConversationContext conversationContext,
                                                    AIPlayerEntity aiPlayer) {
        if (llmProvider == null) {
            return CompletableFuture.completedFuture(getFallbackAnswer(intent, aiPlayer));
        }

        String questionType = (String) intent.getEntity("questionType");
        if (questionType != null) {
            // Use specialized handlers for known question types
            switch (questionType) {
                case "currentActivity":
                    return answerActivityQuestion(aiPlayer);
                case "location":
                    return answerLocationQuestion(aiPlayer);
                case "status":
                    return answerStatusQuestion(aiPlayer);
                case "inventory":
                    return answerInventoryQuestion(aiPlayer);
            }
        }

        // General question answering
        String context = buildDetailedContext(aiPlayer, conversationContext);
        String prompt = promptTemplates.buildQuestionPrompt(question, context);

        return llmProvider.complete(prompt, LLMOptions.chat())
            .thenApply(this::cleanResponse)
            .exceptionally(e -> {
                LOGGER.error("Error answering question", e);
                return "I'm not sure how to answer that right now.";
            });
    }

    /**
     * Generate a casual chat response.
     *
     * @param message The player's message
     * @param conversationContext The conversation context
     * @param aiPlayer The AI player
     * @return Chat response
     */
    public CompletableFuture<String> generateChatResponse(String message,
                                                          ConversationContext conversationContext,
                                                          AIPlayerEntity aiPlayer) {
        if (llmProvider == null) {
            return CompletableFuture.completedFuture(getFallbackChatResponse());
        }

        String context = buildDetailedContext(aiPlayer, conversationContext);
        String prompt = promptTemplates.buildChatPrompt(message, context, conversationContext);

        return llmProvider.complete(prompt, LLMOptions.chat())
            .thenApply(this::cleanResponse)
            .exceptionally(e -> {
                LOGGER.error("Error generating chat response", e);
                return getFallbackChatResponse();
            });
    }

    /**
     * Generate a generic response for unknown messages.
     *
     * @param message The message
     * @param conversationContext The conversation context
     * @param aiPlayer The AI player
     * @return Generic response
     */
    public CompletableFuture<String> generateGenericResponse(String message,
                                                             ConversationContext conversationContext,
                                                             AIPlayerEntity aiPlayer) {
        if (llmProvider == null) {
            return CompletableFuture.completedFuture("I'm not sure what you mean.");
        }

        String context = buildDetailedContext(aiPlayer, conversationContext);
        String prompt = promptTemplates.buildGenericPrompt(message, context);

        return llmProvider.complete(prompt, LLMOptions.chat())
            .thenApply(this::cleanResponse)
            .exceptionally(e -> {
                LOGGER.error("Error generating generic response", e);
                return "I'm not sure what you mean.";
            });
    }

    /**
     * Answer "what are you doing?" question.
     */
    private CompletableFuture<String> answerActivityQuestion(AIPlayerEntity aiPlayer) {
        if (aiPlayer.getBrain() != null && aiPlayer.getBrain().getPlanningEngine() != null) {
            var currentGoal = aiPlayer.getBrain().getPlanningEngine().getCurrentGoal();
            if (currentGoal.isPresent()) {
                Goal goal = currentGoal.get();
                String response = String.format("I'm currently working on: %s",
                    goal.getDescription());
                return CompletableFuture.completedFuture(response);
            }
        }

        return CompletableFuture.completedFuture("I'm just exploring and looking for things to do.");
    }

    /**
     * Answer "where are you?" question.
     */
    private CompletableFuture<String> answerLocationQuestion(AIPlayerEntity aiPlayer) {
        var pos = aiPlayer.getPos();
        String response = String.format("I'm at coordinates X: %.1f, Y: %.1f, Z: %.1f",
            pos.x, pos.y, pos.z);
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Answer status/health question.
     */
    private CompletableFuture<String> answerStatusQuestion(AIPlayerEntity aiPlayer) {
        float health = aiPlayer.getHealth();
        float maxHealth = aiPlayer.getMaxHealth();
        int hunger = aiPlayer.getHungerManager().getFoodLevel();

        String healthStatus = health > maxHealth * 0.8 ? "great" :
                             health > maxHealth * 0.5 ? "okay" : "not so good";

        String response = String.format("I'm feeling %s. Health: %.1f/%.1f, Hunger: %d/20",
            healthStatus, health, maxHealth, hunger);
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Answer inventory question.
     */
    private CompletableFuture<String> answerInventoryQuestion(AIPlayerEntity aiPlayer) {
        // Count non-empty inventory slots
        int itemCount = 0;
        for (int i = 0; i < aiPlayer.getInventory().size(); i++) {
            if (!aiPlayer.getInventory().getStack(i).isEmpty()) {
                itemCount++;
            }
        }

        String response = String.format("I have %d different items in my inventory.", itemCount);
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Build basic context string for LLM.
     */
    private String buildBasicContext(AIPlayerEntity aiPlayer) {
        StringBuilder context = new StringBuilder();

        // Position
        var pos = aiPlayer.getPos();
        context.append(String.format("Position: %.1f, %.1f, %.1f\n", pos.x, pos.y, pos.z));

        // Health/hunger
        context.append(String.format("Health: %.1f/%.1f\n",
            aiPlayer.getHealth(), aiPlayer.getMaxHealth()));
        context.append(String.format("Hunger: %d/20\n",
            aiPlayer.getHungerManager().getFoodLevel()));

        return context.toString();
    }

    /**
     * Build detailed context including memories and goals.
     */
    private String buildDetailedContext(AIPlayerEntity aiPlayer, ConversationContext conversationContext) {
        StringBuilder context = new StringBuilder();

        // Basic status
        context.append(buildBasicContext(aiPlayer));

        // Current goal
        if (aiPlayer.getBrain() != null && aiPlayer.getBrain().getPlanningEngine() != null) {
            var currentGoal = aiPlayer.getBrain().getPlanningEngine().getCurrentGoal();
            if (currentGoal.isPresent()) {
                context.append(String.format("Current Goal: %s\n",
                    currentGoal.get().getDescription()));
            }
        }

        // Recent memories (if available)
        if (aiPlayer.getBrain() != null && aiPlayer.getBrain().getMemorySystem() != null) {
            MemorySystem memory = aiPlayer.getBrain().getMemorySystem();
            List<Memory> recentMemories = memory.getRecentMemories(5);
            if (!recentMemories.isEmpty()) {
                context.append("\nRecent Events:\n");
                for (Memory mem : recentMemories) {
                    context.append(String.format("- %s\n", mem.getContent()));
                }
            }
        }

        return context.toString();
    }

    /**
     * Clean up LLM response (remove extra whitespace, limit length).
     */
    private String cleanResponse(String response) {
        if (response == null || response.isEmpty()) {
            return getFallbackChatResponse();
        }

        // Trim whitespace
        String cleaned = response.trim();

        // Remove any quotes the LLM might have added
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }

        // Limit length
        if (cleaned.length() > MAX_RESPONSE_LENGTH) {
            cleaned = cleaned.substring(0, MAX_RESPONSE_LENGTH - 3) + "...";
        }

        // Ensure it ends with punctuation
        if (!cleaned.matches(".*[.!?]$")) {
            cleaned += ".";
        }

        return cleaned;
    }

    /**
     * Get fallback greeting when LLM is unavailable.
     */
    private String getFallbackGreeting(String playerName) {
        String[] greetings = {
            "Hello, " + playerName + "!",
            "Hi there, " + playerName + "!",
            "Hey " + playerName + "!",
            "Greetings, " + playerName + "!"
        };
        return greetings[(int) (Math.random() * greetings.length)];
    }

    /**
     * Get fallback answer when LLM is unavailable.
     */
    private String getFallbackAnswer(NLUEngine.Intent intent, AIPlayerEntity aiPlayer) {
        String questionType = (String) intent.getEntity("questionType");
        if (questionType != null) {
            switch (questionType) {
                case "currentActivity":
                    return answerActivityQuestion(aiPlayer).join();
                case "location":
                    return answerLocationQuestion(aiPlayer).join();
                case "status":
                    return answerStatusQuestion(aiPlayer).join();
                case "inventory":
                    return answerInventoryQuestion(aiPlayer).join();
            }
        }
        return "I'm not sure how to answer that.";
    }

    /**
     * Get fallback chat response when LLM is unavailable.
     */
    private String getFallbackChatResponse() {
        String[] responses = {
            "That's interesting!",
            "I see.",
            "Okay!",
            "Alright.",
            "Got it!"
        };
        return responses[(int) (Math.random() * responses.length)];
    }
}
