package com.aiplayer.communication;

import com.aiplayer.core.AIPlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listens to chat messages and routes them to AI players when mentioned.
 *
 * Detects mentions in formats:
 * - @AIName: direct mention
 * - "Hey AIName": casual mention
 * - Messages sent while near AI player
 *
 * Phase 4: Natural Language Communication
 */
public class ChatListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatListener.class);

    private final AIPlayerEntity aiPlayer;
    private final DialogueManager dialogueManager;

    // Pattern for detecting mentions: @AIName or "hey AIName" or "AIName,"
    private final Pattern mentionPattern;

    // Proximity distance for auto-listen (blocks)
    private static final double AUTO_LISTEN_DISTANCE = 10.0;

    // Track last message time per player (prevent spam)
    private final Map<String, Long> lastMessageTime = new ConcurrentHashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 1000; // 1 second between messages

    /**
     * Create chat listener for an AI player.
     *
     * @param aiPlayer The AI player entity
     * @param dialogueManager The dialogue manager for conversation tracking
     */
    public ChatListener(AIPlayerEntity aiPlayer, DialogueManager dialogueManager) {
        this.aiPlayer = aiPlayer;
        this.dialogueManager = dialogueManager;

        // Build mention pattern for this AI's name
        String aiName = aiPlayer.getGameProfile().getName();
        String patternStr = String.format(
            "(?i)(?:@%s|hey\\s+%s|%s[,:])",
            Pattern.quote(aiName),
            Pattern.quote(aiName),
            Pattern.quote(aiName)
        );
        this.mentionPattern = Pattern.compile(patternStr);

        LOGGER.debug("ChatListener initialized for AI player: {}", aiName);
    }

    /**
     * Process a chat message from a player.
     *
     * @param sender The player who sent the message
     * @param message The message content
     */
    public void onChatMessage(ServerPlayerEntity sender, String message) {
        // Ignore messages from the AI itself
        if (sender.getUuid().equals(aiPlayer.getUuid())) {
            return;
        }

        String senderName = sender.getGameProfile().getName();

        // Check cooldown
        if (isOnCooldown(senderName)) {
            return;
        }

        // Determine if AI should respond
        boolean shouldRespond = shouldRespondToMessage(sender, message);

        if (shouldRespond) {
            LOGGER.debug("AI {} responding to message from {}: {}",
                aiPlayer.getGameProfile().getName(), senderName, message);

            // Clean the message (remove mention prefix)
            String cleanedMessage = cleanMessage(message);

            // Update last message time
            lastMessageTime.put(senderName, System.currentTimeMillis());

            // Process through dialogue manager
            dialogueManager.handleMessage(senderName, cleanedMessage);
        }
    }

    /**
     * Check if AI should respond to this message.
     *
     * @param sender The player who sent the message
     * @param message The message content
     * @return true if AI should respond
     */
    private boolean shouldRespondToMessage(ServerPlayerEntity sender, String message) {
        // Check for direct mention
        if (isMentioned(message)) {
            return true;
        }

        // Check proximity (if player is near AI, listen to their messages)
        if (isPlayerNearby(sender)) {
            return true;
        }

        // Check if in active conversation
        String senderName = sender.getGameProfile().getName();
        if (dialogueManager.hasActiveConversation(senderName)) {
            return true;
        }

        return false;
    }

    /**
     * Check if the message mentions this AI player.
     *
     * @param message The message to check
     * @return true if message contains a mention
     */
    private boolean isMentioned(String message) {
        Matcher matcher = mentionPattern.matcher(message);
        return matcher.find();
    }

    /**
     * Check if a player is nearby the AI.
     *
     * @param player The player to check
     * @return true if player is within AUTO_LISTEN_DISTANCE blocks
     */
    private boolean isPlayerNearby(ServerPlayerEntity player) {
        double distance = player.getPos().distanceTo(aiPlayer.getPos());
        return distance <= AUTO_LISTEN_DISTANCE;
    }

    /**
     * Check if sender is on message cooldown.
     *
     * @param senderName The sender's name
     * @return true if on cooldown
     */
    private boolean isOnCooldown(String senderName) {
        Long lastTime = lastMessageTime.get(senderName);
        if (lastTime == null) {
            return false;
        }

        long timeSince = System.currentTimeMillis() - lastTime;
        return timeSince < MESSAGE_COOLDOWN_MS;
    }

    /**
     * Clean message by removing mention prefixes.
     *
     * @param message The raw message
     * @return Cleaned message
     */
    private String cleanMessage(String message) {
        // Remove mention patterns
        String cleaned = mentionPattern.matcher(message).replaceAll("").trim();

        // Remove leading punctuation
        cleaned = cleaned.replaceFirst("^[,.:;!?\\s]+", "");

        return cleaned;
    }

    /**
     * Send a message from the AI player to chat.
     *
     * @param message The message to send
     */
    public void sendMessage(String message) {
        if (aiPlayer.getServer() != null) {
            Text textComponent = Text.literal(message);
            aiPlayer.sendMessage(textComponent, false);

            LOGGER.debug("AI {} sent message: {}",
                aiPlayer.getGameProfile().getName(), message);
        }
    }

    /**
     * Send a private message to a specific player.
     *
     * @param targetPlayer The player to message
     * @param message The message content
     */
    public void sendPrivateMessage(ServerPlayerEntity targetPlayer, String message) {
        Text textComponent = Text.literal(
            String.format("[%s → %s] %s",
                aiPlayer.getGameProfile().getName(),
                targetPlayer.getGameProfile().getName(),
                message)
        );
        targetPlayer.sendMessage(textComponent, false);

        LOGGER.debug("AI {} sent private message to {}: {}",
            aiPlayer.getGameProfile().getName(),
            targetPlayer.getGameProfile().getName(),
            message);
    }

    /**
     * Get the AI player this listener is for.
     *
     * @return The AI player entity
     */
    public AIPlayerEntity getAIPlayer() {
        return aiPlayer;
    }
}
