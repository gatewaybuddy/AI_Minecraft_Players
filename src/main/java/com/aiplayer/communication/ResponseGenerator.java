package com.aiplayer.communication;

import com.aiplayer.config.PersonalityConfig;
import com.aiplayer.llm.LLMProvider;
import com.aiplayer.llm.LLMOptions;
import com.aiplayer.memory.MemorySystem;
import com.aiplayer.perception.WorldState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * Generates natural language responses using LLM.
 *
 * Handles different types of responses:
 * - Questions about world state/inventory
 * - Casual chat with personality
 * - Task acceptance/rejection
 * - Status updates
 */
public class ResponseGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseGenerator.class);
    private static final Random RANDOM = new Random();

    private final LLMProvider llmProvider;

    public ResponseGenerator(LLMProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    /**
     * Generate response to a question about the world/inventory/etc.
     */
    public CompletableFuture<String> generateQuestionResponse(
        String question,
        WorldState worldState,
        MemorySystem memorySystem
    ) {
        if (llmProvider == null || !llmProvider.isAvailable()) {
            return CompletableFuture.completedFuture(
                generateFallbackQuestionResponse(question, worldState)
            );
        }

        String prompt = buildQuestionPrompt(question, worldState, memorySystem);

        return llmProvider.complete(prompt, LLMOptions.chat())
            .thenApply(this::cleanResponse)
            .exceptionally(e -> {
                LOGGER.error("Failed to generate question response via LLM", e);
                return generateFallbackQuestionResponse(question, worldState);
            });
    }

    /**
     * Generate casual chat response with personality.
     */
    public CompletableFuture<String> generateChatResponse(
        String message,
        String playerName,
        PersonalityConfig personality
    ) {
        if (llmProvider == null || !llmProvider.isAvailable()) {
            return CompletableFuture.completedFuture(
                generateFallbackChatResponse(message)
            );
        }

        String prompt = buildChatPrompt(message, playerName, personality);

        return llmProvider.complete(prompt, LLMOptions.chat())
            .thenApply(this::cleanResponse)
            .exceptionally(e -> {
                LOGGER.error("Failed to generate chat response via LLM", e);
                return generateFallbackChatResponse(message);
            });
    }

    /**
     * Generate task acceptance response.
     */
    public String generateTaskAcceptanceResponse(TaskRequest task) {
        String[] templates = {
            "Sure, I'll " + task.getDescription() + "!",
            "Okay, I'll get started on that.",
            "Got it, I'll take care of that!",
            "On it!",
            "No problem, I'll " + task.getDescription() + ".",
            "Consider it done!"
        };

        return templates[RANDOM.nextInt(templates.length)];
    }

    /**
     * Generate task rejection response.
     */
    public String generateTaskRejectionResponse(TaskRequest task, String reason) {
        return "Sorry, I can't do that right now. " + reason;
    }

    /**
     * Generate status update response.
     */
    public String generateStatusResponse(String currentActivity, int progressPercent) {
        if (currentActivity == null || currentActivity.isEmpty()) {
            String[] idleResponses = {
                "I'm just exploring right now.",
                "Nothing much, just wandering around.",
                "I'm looking for something to do.",
                "Just exploring the area."
            };
            return idleResponses[RANDOM.nextInt(idleResponses.length)];
        }

        if (progressPercent >= 0) {
            return String.format("I'm currently %s (%d%% complete)", currentActivity, progressPercent);
        } else {
            return "I'm currently " + currentActivity + ".";
        }
    }

    /**
     * Build prompt for question answering.
     */
    private String buildQuestionPrompt(
        String question,
        WorldState worldState,
        MemorySystem memorySystem
    ) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an AI player in Minecraft. Answer the player's question concisely and naturally.\n\n");

        // Add context
        prompt.append("Current Status:\n");
        prompt.append("- Position: ").append(formatPosition(worldState)).append("\n");
        prompt.append("- Health: ").append(worldState.getHealth()).append("/20\n");
        prompt.append("- Hunger: ").append(worldState.getHunger()).append("/20\n");

        // Add inventory (top items)
        prompt.append("\nInventory:\n");
        Map<String, Integer> inventory = worldState.getInventorySummary();
        if (inventory.isEmpty()) {
            prompt.append("- (empty)\n");
        } else {
            int count = 0;
            for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
                if (count++ >= 10) break; // Limit to top 10 items
                prompt.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        // Add recent memories (if available)
        if (memorySystem != null) {
            prompt.append("\nRecent Events:\n");
            memorySystem.getWorkingMemory().getRecent(5).forEach(memory -> {
                prompt.append("- ").append(memory.getContent()).append("\n");
            });
        }

        prompt.append("\nQuestion: ").append(question).append("\n");
        prompt.append("Answer (1-2 sentences, natural and friendly):\n");

        return prompt.toString();
    }

    /**
     * Build prompt for casual chat.
     */
    private String buildChatPrompt(
        String message,
        String playerName,
        PersonalityConfig personality
    ) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an AI player in Minecraft with the following personality:\n");
        if (personality != null) {
            prompt.append("Description: ").append(personality.getDescription()).append("\n");
            prompt.append("Role: ").append(personality.getRole()).append("\n");
        } else {
            prompt.append("Description: helpful and friendly\n");
        }

        prompt.append("\nA player named ").append(playerName).append(" says: \"").append(message).append("\"\n\n");
        prompt.append("Respond naturally in 1-2 sentences (stay in character, be conversational):\n");

        return prompt.toString();
    }

    /**
     * Generate fallback response when LLM is unavailable (for questions).
     */
    private String generateFallbackQuestionResponse(String question, WorldState worldState) {
        String lower = question.toLowerCase();

        // Check for inventory questions
        if (lower.contains("have") || lower.contains("got")) {
            Map<String, Integer> inventory = worldState.getInventorySummary();
            if (inventory.isEmpty()) {
                return "I don't have anything in my inventory right now.";
            } else {
                return "I have: " + formatInventorySummary(inventory, 5);
            }
        }

        // Check for position questions
        if (lower.contains("where")) {
            return "I'm at " + formatPosition(worldState);
        }

        // Check for health/status
        if (lower.contains("health") || lower.contains("hp")) {
            return "My health is " + worldState.getHealth() + "/20";
        }

        // Default response
        return "I'm not sure about that. I'm still learning!";
    }

    /**
     * Generate fallback response for casual chat.
     */
    private String generateFallbackChatResponse(String message) {
        String lower = message.toLowerCase();

        if (lower.contains("hello") || lower.contains("hi")) {
            String[] greetings = {"Hello!", "Hi there!", "Hey!", "Greetings!"};
            return greetings[RANDOM.nextInt(greetings.length)];
        }

        if (lower.contains("how are you") || lower.contains("how's it going")) {
            String[] responses = {
                "I'm doing well, thanks!",
                "Pretty good! Just working on some tasks.",
                "All good here!",
                "Doing great!"
            };
            return responses[RANDOM.nextInt(responses.length)];
        }

        if (lower.contains("thank") || lower.contains("thx")) {
            String[] responses = {
                "You're welcome!",
                "No problem!",
                "Happy to help!",
                "Anytime!"
            };
            return responses[RANDOM.nextInt(responses.length)];
        }

        if (lower.contains("bye") || lower.contains("goodbye")) {
            String[] responses = {
                "See you later!",
                "Goodbye!",
                "Bye!",
                "Take care!"
            };
            return responses[RANDOM.nextInt(responses.length)];
        }

        // Generic response
        String[] genericResponses = {
            "Interesting!",
            "I see.",
            "Cool!",
            "Yeah!",
            "That's nice!"
        };
        return genericResponses[RANDOM.nextInt(genericResponses.length)];
    }

    /**
     * Clean LLM response (remove quotes, extra whitespace, etc).
     */
    private String cleanResponse(String response) {
        if (response == null) {
            return "...";
        }

        return response
            .trim()
            .replaceAll("^[\"']+|[\"']+$", "")  // Remove surrounding quotes
            .replaceAll("\\s+", " ")             // Normalize whitespace
            .trim();
    }

    /**
     * Format position as readable string.
     */
    private String formatPosition(WorldState worldState) {
        if (worldState.getPosition() == null) {
            return "unknown location";
        }

        return String.format("X:%.0f Y:%.0f Z:%.0f",
            worldState.getPosition().x,
            worldState.getPosition().y,
            worldState.getPosition().z
        );
    }

    /**
     * Format inventory summary as readable string.
     */
    private String formatInventorySummary(Map<String, Integer> inventory, int maxItems) {
        StringBuilder sb = new StringBuilder();
        int count = 0;

        for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
            if (count > 0) {
                sb.append(", ");
            }
            sb.append(entry.getValue()).append(" ").append(entry.getKey().replace("_", " "));

            if (++count >= maxItems) {
                if (inventory.size() > maxItems) {
                    sb.append(", and ").append(inventory.size() - maxItems).append(" more");
                }
                break;
            }
        }

        return sb.toString();
    }
}
