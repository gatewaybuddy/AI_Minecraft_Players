package com.aiplayer.communication;

import com.aiplayer.core.AIPlayerEntity;
import com.aiplayer.core.AIPlayerManager;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens to server chat events and routes messages to AI players.
 *
 * This class:
 * 1. Registers with Fabric's ServerMessageEvents
 * 2. Filters incoming chat messages
 * 3. Determines which AI players are addressed
 * 4. Routes messages to MessageProcessor for handling
 *
 * Addressing patterns supported:
 * - "@AIName, do something"
 * - "Hey AIName, do something"
 * - "AIName: do something"
 * - "@AI" (addresses all AI players)
 * - "hey AI" (addresses all AI players)
 */
public class ChatListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatListener.class);

    private final AIPlayerManager playerManager;
    private final MessageProcessor messageProcessor;

    public ChatListener(AIPlayerManager playerManager, MessageProcessor messageProcessor) {
        this.playerManager = playerManager;
        this.messageProcessor = messageProcessor;
        LOGGER.info("ChatListener created");
    }

    /**
     * Register chat event handlers with Fabric.
     */
    public void register() {
        ServerMessageEvents.CHAT_MESSAGE.register(this::onChatMessage);
        LOGGER.info("ChatListener registered for chat events");
    }

    /**
     * Handle incoming chat message.
     */
    private void onChatMessage(
        SignedMessage message,
        ServerPlayerEntity sender,
        net.minecraft.network.message.MessageType.Parameters params
    ) {
        try {
            // Get message content
            String content = message.getContent().getString();

            // Don't respond to empty messages
            if (content == null || content.trim().isEmpty()) {
                return;
            }

            // Don't respond to AI's own messages
            if (playerManager.isAIPlayer(sender.getUuid())) {
                LOGGER.debug("Ignoring message from AI player: {}", sender.getName().getString());
                return;
            }

            LOGGER.debug("Received chat message from {}: {}", sender.getName().getString(), content);

            // Check which AI players are addressed
            for (AIPlayerEntity aiPlayer : playerManager.getAllPlayers()) {
                if (isAddressed(content, aiPlayer.getName().getString())) {
                    LOGGER.info("AI player {} addressed by {}: {}",
                        aiPlayer.getName().getString(),
                        sender.getName().getString(),
                        content);

                    // Process message
                    messageProcessor.processMessage(content, sender, aiPlayer);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error handling chat message from " + sender.getName().getString(), e);
        }
    }

    /**
     * Check if message addresses a specific AI player.
     *
     * Supports multiple addressing patterns:
     * - @AIName
     * - hey AIName
     * - AIName: message
     * - @AI (generic)
     * - hey AI (generic)
     */
    private boolean isAddressed(String message, String aiName) {
        String lower = message.toLowerCase().trim();
        String aiNameLower = aiName.toLowerCase();

        // Direct @ mention
        if (lower.contains("@" + aiNameLower)) {
            return true;
        }

        // Hey AIName pattern
        if (lower.startsWith("hey " + aiNameLower) ||
            lower.startsWith("hello " + aiNameLower) ||
            lower.startsWith("hi " + aiNameLower)) {
            return true;
        }

        // AIName: pattern
        if (lower.startsWith(aiNameLower + ":") ||
            lower.startsWith(aiNameLower + ",")) {
            return true;
        }

        // Generic AI mentions (addresses all AIs)
        if (lower.contains("@ai") && !lower.contains("@ais")) {  // Avoid "iais" etc
            return true;
        }

        if (lower.startsWith("hey ai") ||
            lower.startsWith("hello ai") ||
            lower.startsWith("hi ai")) {
            return true;
        }

        return false;
    }

    /**
     * Unregister event handlers (cleanup).
     */
    public void unregister() {
        // Fabric doesn't provide explicit unregistration, but we can log for tracking
        LOGGER.info("ChatListener unregistering");
    }
}
