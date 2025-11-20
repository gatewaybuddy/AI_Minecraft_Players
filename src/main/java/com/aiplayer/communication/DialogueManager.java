package com.aiplayer.communication;

import com.aiplayer.core.AIPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Manages conversations between AI player and human players.
 *
 * Tracks multiple concurrent conversations and routes messages
 * to the appropriate handlers.
 *
 * Phase 4: Natural Language Communication
 */
public class DialogueManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DialogueManager.class);

    private final AIPlayerEntity aiPlayer;
    private final ResponseGenerator responseGenerator;
    private final NLUEngine nluEngine;
    private final TaskRequestHandler taskRequestHandler;

    // Active conversations by player name
    private final Map<String, ConversationContext> conversations;

    // Cleanup interval (minutes)
    private static final long CLEANUP_INTERVAL_MINUTES = 10;
    private Instant lastCleanup;

    /**
     * Create a dialogue manager for an AI player.
     *
     * @param aiPlayer The AI player
     * @param responseGenerator The response generator
     * @param nluEngine The NLU engine
     * @param taskRequestHandler The task request handler
     */
    public DialogueManager(AIPlayerEntity aiPlayer,
                          ResponseGenerator responseGenerator,
                          NLUEngine nluEngine,
                          TaskRequestHandler taskRequestHandler) {
        this.aiPlayer = aiPlayer;
        this.responseGenerator = responseGenerator;
        this.nluEngine = nluEngine;
        this.taskRequestHandler = taskRequestHandler;
        this.conversations = new ConcurrentHashMap<>();
        this.lastCleanup = Instant.now();

        LOGGER.debug("DialogueManager initialized for AI player: {}",
            aiPlayer.getGameProfile().getName());
    }

    /**
     * Handle a message from a player.
     *
     * @param playerName The player who sent the message
     * @param message The message content
     */
    public void handleMessage(String playerName, String message) {
        // Get or create conversation context
        ConversationContext context = getOrCreateConversation(playerName);

        // Add player message to history
        context.addPlayerMessage(message);

        LOGGER.debug("Handling message from {}: {}", playerName, message);

        // Understand intent
        NLUEngine.Intent intent = nluEngine.classifyIntent(message);

        // Route based on intent
        CompletableFuture<String> responseFuture;

        switch (intent.getType()) {
            case TASK_REQUEST:
                responseFuture = handleTaskRequest(context, message, intent);
                break;

            case QUESTION:
                responseFuture = handleQuestion(context, message, intent);
                break;

            case GREETING:
                responseFuture = handleGreeting(context, message);
                break;

            case CASUAL_CHAT:
                responseFuture = handleCasualChat(context, message);
                break;

            case COMMAND:
                responseFuture = handleCommand(context, message, intent);
                break;

            default:
                responseFuture = handleGeneric(context, message);
                break;
        }

        // Send response when ready
        responseFuture.thenAccept(response -> {
            if (response != null && !response.isEmpty()) {
                sendResponse(playerName, response, context);
            }
        }).exceptionally(e -> {
            LOGGER.error("Error generating response for {}", playerName, e);
            sendResponse(playerName, "Sorry, I had trouble understanding that.", context);
            return null;
        });

        // Periodic cleanup
        cleanupInactiveConversations();
    }

    /**
     * Handle a task request (e.g., "gather 64 oak logs").
     */
    private CompletableFuture<String> handleTaskRequest(ConversationContext context,
                                                        String message,
                                                        NLUEngine.Intent intent) {
        return taskRequestHandler.handleRequest(message, intent, context)
            .thenApply(result -> {
                if (result.isAccepted()) {
                    context.setCurrentTopic("task: " + result.getTaskDescription());
                    return result.getConfirmationMessage();
                } else {
                    return result.getRejectionReason();
                }
            });
    }

    /**
     * Handle a question (e.g., "what are you doing?").
     */
    private CompletableFuture<String> handleQuestion(ConversationContext context,
                                                     String message,
                                                     NLUEngine.Intent intent) {
        return responseGenerator.answerQuestion(message, intent, context, aiPlayer);
    }

    /**
     * Handle a greeting (e.g., "hello").
     */
    private CompletableFuture<String> handleGreeting(ConversationContext context, String message) {
        return responseGenerator.generateGreeting(context.getPlayerName(), aiPlayer);
    }

    /**
     * Handle casual chat (e.g., "nice weather today").
     */
    private CompletableFuture<String> handleCasualChat(ConversationContext context, String message) {
        return responseGenerator.generateChatResponse(message, context, aiPlayer);
    }

    /**
     * Handle a command (e.g., "stop", "follow me").
     */
    private CompletableFuture<String> handleCommand(ConversationContext context,
                                                   String message,
                                                   NLUEngine.Intent intent) {
        // Commands are handled as simplified task requests
        return handleTaskRequest(context, message, intent);
    }

    /**
     * Handle generic/unknown messages.
     */
    private CompletableFuture<String> handleGeneric(ConversationContext context, String message) {
        return responseGenerator.generateGenericResponse(message, context, aiPlayer);
    }

    /**
     * Send a response to the player.
     */
    private void sendResponse(String playerName, String response, ConversationContext context) {
        // Add AI response to conversation history
        context.addAIResponse(aiPlayer.getGameProfile().getName(), response);

        // Send via chat system
        if (aiPlayer.getServer() != null) {
            aiPlayer.sendMessage(net.minecraft.text.Text.literal(response), false);
        }

        LOGGER.debug("AI {} responded to {}: {}",
            aiPlayer.getGameProfile().getName(), playerName, response);
    }

    /**
     * Get or create a conversation context for a player.
     */
    private ConversationContext getOrCreateConversation(String playerName) {
        return conversations.computeIfAbsent(playerName, name -> {
            LOGGER.debug("Starting new conversation with player: {}", name);
            return new ConversationContext(name);
        });
    }

    /**
     * Check if there's an active conversation with a player.
     *
     * @param playerName The player name
     * @return true if active conversation exists
     */
    public boolean hasActiveConversation(String playerName) {
        ConversationContext context = conversations.get(playerName);
        return context != null && context.isActive();
    }

    /**
     * Get a conversation context for a player.
     *
     * @param playerName The player name
     * @return The conversation context, or null if none exists
     */
    public ConversationContext getConversation(String playerName) {
        return conversations.get(playerName);
    }

    /**
     * End a conversation with a player.
     *
     * @param playerName The player name
     */
    public void endConversation(String playerName) {
        ConversationContext context = conversations.remove(playerName);
        if (context != null) {
            LOGGER.debug("Ended conversation with player: {}", playerName);
        }
    }

    /**
     * Clean up inactive conversations periodically.
     */
    private void cleanupInactiveConversations() {
        Instant now = Instant.now();
        long minutesSinceCleanup = (now.toEpochMilli() - lastCleanup.toEpochMilli()) / 1000 / 60;

        if (minutesSinceCleanup >= CLEANUP_INTERVAL_MINUTES) {
            int removed = 0;
            for (Map.Entry<String, ConversationContext> entry : conversations.entrySet()) {
                if (!entry.getValue().isActive()) {
                    conversations.remove(entry.getKey());
                    removed++;
                }
            }

            if (removed > 0) {
                LOGGER.debug("Cleaned up {} inactive conversations", removed);
            }

            lastCleanup = now;
        }
    }

    /**
     * Get the number of active conversations.
     *
     * @return Active conversation count
     */
    public int getActiveConversationCount() {
        return (int) conversations.values().stream()
            .filter(ConversationContext::isActive)
            .count();
    }

    /**
     * Notify that a task was completed.
     * This can be used to inform players in active conversations.
     *
     * @param taskDescription The task that was completed
     */
    public void notifyTaskCompleted(String taskDescription) {
        // Notify all active conversations
        for (ConversationContext context : conversations.values()) {
            if (context.isActive()) {
                context.addSystemEvent("Task completed: " + taskDescription);
                String notification = String.format("I've finished: %s", taskDescription);
                sendResponse(context.getPlayerName(), notification, context);
            }
        }
    }

    /**
     * Notify that a task failed.
     *
     * @param taskDescription The task that failed
     * @param reason The reason for failure
     */
    public void notifyTaskFailed(String taskDescription, String reason) {
        // Notify all active conversations
        for (ConversationContext context : conversations.values()) {
            if (context.isActive()) {
                context.addSystemEvent("Task failed: " + taskDescription + " - " + reason);
                String notification = String.format("Sorry, I couldn't complete: %s. Reason: %s",
                    taskDescription, reason);
                sendResponse(context.getPlayerName(), notification, context);
            }
        }
    }
}
