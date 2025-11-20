package com.aiplayer.communication;

import com.aiplayer.core.AIPlayerEntity;
import com.aiplayer.llm.LLMProvider;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinates all communication components for an AI player.
 *
 * This is the main interface for Phase 4 natural language communication.
 * It ties together:
 * - ChatListener: Detects and captures chat messages
 * - DialogueManager: Manages conversations
 * - NLUEngine: Understands player intent
 * - ResponseGenerator: Generates natural responses
 * - TaskRequestHandler: Handles player requests
 *
 * Phase 4: Natural Language Communication
 */
public class CommunicationSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommunicationSystem.class);

    private final AIPlayerEntity aiPlayer;
    private final ChatListener chatListener;
    private final DialogueManager dialogueManager;
    private final NLUEngine nluEngine;
    private final ResponseGenerator responseGenerator;
    private final TaskRequestHandler taskRequestHandler;

    private final boolean enabled;

    /**
     * Create a communication system for an AI player.
     *
     * @param aiPlayer The AI player
     * @param llmProvider The LLM provider (can be null)
     */
    public CommunicationSystem(AIPlayerEntity aiPlayer, LLMProvider llmProvider) {
        this.aiPlayer = aiPlayer;

        // Check if communication should be enabled
        // Requires intelligent mode (LLM available)
        this.enabled = (llmProvider != null && llmProvider.isAvailable());

        if (enabled) {
            // Initialize all components
            this.nluEngine = new NLUEngine();
            this.responseGenerator = new ResponseGenerator(llmProvider);
            this.taskRequestHandler = new TaskRequestHandler(aiPlayer, llmProvider);
            this.dialogueManager = new DialogueManager(
                aiPlayer,
                responseGenerator,
                nluEngine,
                taskRequestHandler
            );
            this.chatListener = new ChatListener(aiPlayer, dialogueManager);

            LOGGER.info("CommunicationSystem initialized for AI player: {} (ENABLED)",
                aiPlayer.getGameProfile().getName());
        } else {
            // Disabled - no LLM available
            this.nluEngine = null;
            this.responseGenerator = null;
            this.taskRequestHandler = null;
            this.dialogueManager = null;
            this.chatListener = null;

            LOGGER.debug("CommunicationSystem disabled for AI player: {} (no LLM)",
                aiPlayer.getGameProfile().getName());
        }
    }

    /**
     * Process a chat message from a player.
     *
     * This is the main entry point for chat communication.
     *
     * @param sender The player who sent the message
     * @param message The message content
     */
    public void onChatMessage(ServerPlayerEntity sender, String message) {
        if (!enabled || chatListener == null) {
            return;
        }

        try {
            chatListener.onChatMessage(sender, message);
        } catch (Exception e) {
            LOGGER.error("Error processing chat message from {} for AI {}",
                sender.getGameProfile().getName(),
                aiPlayer.getGameProfile().getName(),
                e);
        }
    }

    /**
     * Send a message from the AI player.
     *
     * @param message The message to send
     */
    public void sendMessage(String message) {
        if (!enabled || chatListener == null) {
            return;
        }

        chatListener.sendMessage(message);
    }

    /**
     * Notify that a task was completed.
     * This will inform any players in active conversations.
     *
     * @param taskDescription The completed task
     */
    public void notifyTaskCompleted(String taskDescription) {
        if (enabled && dialogueManager != null) {
            dialogueManager.notifyTaskCompleted(taskDescription);
        }
    }

    /**
     * Notify that a task failed.
     *
     * @param taskDescription The failed task
     * @param reason The reason for failure
     */
    public void notifyTaskFailed(String taskDescription, String reason) {
        if (enabled && dialogueManager != null) {
            dialogueManager.notifyTaskFailed(taskDescription, reason);
        }
    }

    /**
     * Get the chat listener.
     *
     * @return The chat listener, or null if disabled
     */
    public ChatListener getChatListener() {
        return chatListener;
    }

    /**
     * Get the dialogue manager.
     *
     * @return The dialogue manager, or null if disabled
     */
    public DialogueManager getDialogueManager() {
        return dialogueManager;
    }

    /**
     * Get the NLU engine.
     *
     * @return The NLU engine, or null if disabled
     */
    public NLUEngine getNluEngine() {
        return nluEngine;
    }

    /**
     * Get the response generator.
     *
     * @return The response generator, or null if disabled
     */
    public ResponseGenerator getResponseGenerator() {
        return responseGenerator;
    }

    /**
     * Get the task request handler.
     *
     * @return The task request handler, or null if disabled
     */
    public TaskRequestHandler getTaskRequestHandler() {
        return taskRequestHandler;
    }

    /**
     * Check if communication is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the number of active conversations.
     *
     * @return Active conversation count
     */
    public int getActiveConversationCount() {
        if (enabled && dialogueManager != null) {
            return dialogueManager.getActiveConversationCount();
        }
        return 0;
    }
}
