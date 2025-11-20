package com.aiplayer.communication;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks conversation history and context for a single player.
 *
 * Maintains a sliding window of recent messages for LLM context.
 *
 * Phase 4: Natural Language Communication
 */
public class ConversationContext {
    /**
     * A single message in the conversation.
     */
    public static class Message {
        private final String speaker;
        private final String content;
        private final Instant timestamp;
        private final MessageType type;

        public enum MessageType {
            PLAYER_MESSAGE,  // Message from human player
            AI_RESPONSE,     // Response from AI
            SYSTEM_EVENT     // System notification (e.g., "task completed")
        }

        public Message(String speaker, String content, MessageType type) {
            this.speaker = speaker;
            this.content = content;
            this.timestamp = Instant.now();
            this.type = type;
        }

        public String getSpeaker() {
            return speaker;
        }

        public String getContent() {
            return content;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public MessageType getType() {
            return type;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s", type, speaker, content);
        }
    }

    private final String playerName;
    private final List<Message> messages;
    private final int maxMessages;
    private Instant lastInteractionTime;
    private String currentTopic; // Optional: track conversation topic

    // Timeout for conversation (minutes)
    private static final long CONVERSATION_TIMEOUT_MINUTES = 5;

    /**
     * Create a new conversation context.
     *
     * @param playerName The player this conversation is with
     * @param maxMessages Maximum messages to keep in history
     */
    public ConversationContext(String playerName, int maxMessages) {
        this.playerName = playerName;
        this.maxMessages = maxMessages;
        this.messages = new ArrayList<>();
        this.lastInteractionTime = Instant.now();
        this.currentTopic = null;
    }

    /**
     * Create a conversation context with default max messages (20).
     *
     * @param playerName The player this conversation is with
     */
    public ConversationContext(String playerName) {
        this(playerName, 20);
    }

    /**
     * Add a player message to the conversation.
     *
     * @param content The message content
     */
    public void addPlayerMessage(String content) {
        addMessage(new Message(playerName, content, Message.MessageType.PLAYER_MESSAGE));
    }

    /**
     * Add an AI response to the conversation.
     *
     * @param aiName The AI's name
     * @param content The response content
     */
    public void addAIResponse(String aiName, String content) {
        addMessage(new Message(aiName, content, Message.MessageType.AI_RESPONSE));
    }

    /**
     * Add a system event to the conversation.
     *
     * @param content The event description
     */
    public void addSystemEvent(String content) {
        addMessage(new Message("System", content, Message.MessageType.SYSTEM_EVENT));
    }

    /**
     * Add a message to the history.
     *
     * @param message The message to add
     */
    private void addMessage(Message message) {
        messages.add(message);

        // Trim to max size (sliding window)
        while (messages.size() > maxMessages) {
            messages.remove(0);
        }

        // Update last interaction time
        this.lastInteractionTime = Instant.now();
    }

    /**
     * Get all messages in the conversation.
     *
     * @return List of messages
     */
    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    /**
     * Get the N most recent messages.
     *
     * @param count Number of messages to retrieve
     * @return List of recent messages
     */
    public List<Message> getRecentMessages(int count) {
        int size = messages.size();
        int startIndex = Math.max(0, size - count);
        return new ArrayList<>(messages.subList(startIndex, size));
    }

    /**
     * Get the last player message.
     *
     * @return The last message from the player, or null if none
     */
    public Message getLastPlayerMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg.getType() == Message.MessageType.PLAYER_MESSAGE) {
                return msg;
            }
        }
        return null;
    }

    /**
     * Format conversation history for LLM context.
     *
     * @param recentCount Number of recent messages to include
     * @return Formatted conversation string
     */
    public String formatForLLM(int recentCount) {
        List<Message> recent = getRecentMessages(recentCount);
        if (recent.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## Conversation History\n");
        for (Message msg : recent) {
            String speaker = msg.getSpeaker();
            String content = msg.getContent();

            switch (msg.getType()) {
                case PLAYER_MESSAGE:
                    sb.append(String.format("%s: %s\n", speaker, content));
                    break;
                case AI_RESPONSE:
                    sb.append(String.format("You: %s\n", content));
                    break;
                case SYSTEM_EVENT:
                    sb.append(String.format("[%s]\n", content));
                    break;
            }
        }

        return sb.toString();
    }

    /**
     * Check if the conversation is still active.
     * A conversation is active if it hasn't timed out.
     *
     * @return true if conversation is active
     */
    public boolean isActive() {
        Instant now = Instant.now();
        long minutesSinceLastInteraction = (now.toEpochMilli() - lastInteractionTime.toEpochMilli()) / 1000 / 60;
        return minutesSinceLastInteraction < CONVERSATION_TIMEOUT_MINUTES;
    }

    /**
     * Get the player name for this conversation.
     *
     * @return The player name
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Get the current conversation topic (if any).
     *
     * @return The topic, or null
     */
    public String getCurrentTopic() {
        return currentTopic;
    }

    /**
     * Set the current conversation topic.
     *
     * @param topic The topic
     */
    public void setCurrentTopic(String topic) {
        this.currentTopic = topic;
    }

    /**
     * Get the time of last interaction.
     *
     * @return The timestamp
     */
    public Instant getLastInteractionTime() {
        return lastInteractionTime;
    }

    /**
     * Get the number of messages in this conversation.
     *
     * @return Message count
     */
    public int getMessageCount() {
        return messages.size();
    }

    /**
     * Clear the conversation history.
     */
    public void clear() {
        messages.clear();
        currentTopic = null;
        lastInteractionTime = Instant.now();
    }

    @Override
    public String toString() {
        return String.format("ConversationContext{player=%s, messages=%d, active=%s, topic=%s}",
            playerName, messages.size(), isActive(), currentTopic);
    }
}
