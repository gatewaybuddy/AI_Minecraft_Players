package com.aiplayer.communication;

import java.util.regex.Pattern;

/**
 * Represents a classified user intent from a chat message.
 *
 * Intents are categorized into types:
 * - TASK_REQUEST: Player wants AI to perform a task
 * - STATUS_QUERY: Player asking about AI's current activity
 * - QUESTION: Player asking a question about world/inventory/etc
 * - CASUAL_CHAT: General conversation
 */
public class Intent {

    /**
     * Types of user intent.
     */
    public enum Type {
        TASK_REQUEST,   // "gather 64 oak logs", "build a house"
        STATUS_QUERY,   // "what are you doing?", "status?"
        QUESTION,       // "do you have diamonds?", "where can I find iron?"
        CASUAL_CHAT     // "hello", "how are you?"
    }

    private final Type type;
    private final Pattern matchedPattern;
    private final double confidence;

    /**
     * Create intent with matched pattern.
     */
    public Intent(Type type, Pattern matchedPattern) {
        this(type, matchedPattern, 1.0);
    }

    /**
     * Create intent with confidence score.
     */
    public Intent(Type type, Pattern matchedPattern, double confidence) {
        this.type = type;
        this.matchedPattern = matchedPattern;
        this.confidence = confidence;
    }

    public Type getType() {
        return type;
    }

    public Pattern getPattern() {
        return matchedPattern;
    }

    public double getConfidence() {
        return confidence;
    }

    @Override
    public String toString() {
        return "Intent{type=" + type + ", confidence=" + confidence + "}";
    }
}
