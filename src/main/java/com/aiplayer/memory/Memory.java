package com.aiplayer.memory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Memory - A single memory entry.
 *
 * Memories capture important events, observations, actions, and learnings.
 * They form the basis for the AI's experience and decision making.
 */
public class Memory {

    private final UUID id;
    private final long timestamp;
    private final MemoryType type;
    private final String content;
    private final Map<String, Object> metadata;
    private final double importance; // 0.0 to 1.0

    // Optional: embedding vector for semantic search (Phase 5)
    private float[] embedding;

    public Memory(MemoryType type, String content, double importance, Map<String, Object> metadata) {
        this.id = UUID.randomUUID();
        this.timestamp = System.currentTimeMillis();
        this.type = type;
        this.content = content;
        this.importance = Math.max(0.0, Math.min(1.0, importance));
        this.metadata = new HashMap<>(metadata);
    }

    /**
     * Simplified constructor without metadata.
     */
    public Memory(MemoryType type, String content, double importance) {
        this(type, content, importance, new HashMap<>());
    }

    /**
     * Memory types categorize different kinds of memories.
     */
    public enum MemoryType {
        OBSERVATION,     // Saw something (player, mob, block, etc.)
        ACTION,          // What the AI did
        CONVERSATION,    // Chat messages
        GOAL_COMPLETION, // Successfully completed a goal
        GOAL_FAILURE,    // Failed at a goal
        LEARNING,        // Learned something (strategy, fact, etc.)
        RELATIONSHIP,    // Interaction with another player
        DISCOVERY,       // Found something new
        EVENT,           // General event
        PLANNING,        // Planning and decision making
        ACHIEVEMENT,     // Completed achievement (same as GOAL_COMPLETION)
        FAILURE          // Failed at something (same as GOAL_FAILURE)
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public MemoryType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public double getImportance() {
        return importance;
    }

    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    /**
     * Get age of memory in milliseconds.
     */
    public long getAge() {
        return System.currentTimeMillis() - timestamp;
    }

    /**
     * Check if memory is recent (less than 5 minutes old).
     */
    public boolean isRecent() {
        return getAge() < 300000; // 5 minutes
    }

    /**
     * Calculate memory relevance (importance decays over time).
     */
    public double getRelevance() {
        // Importance decays logarithmically with time
        long ageMinutes = getAge() / 60000;
        double decay = 1.0 / (1.0 + Math.log(1.0 + ageMinutes));
        return importance * decay;
    }

    @Override
    public String toString() {
        return String.format("Memory{type=%s, content='%s', importance=%.2f, age=%dmin}",
            type, content, importance, getAge() / 60000);
    }

    /**
     * Format memory for display or LLM context.
     */
    public String format() {
        long minutes = getAge() / 60000;
        String timeStr;
        if (minutes < 1) {
            timeStr = "just now";
        } else if (minutes < 60) {
            timeStr = minutes + " minutes ago";
        } else {
            timeStr = (minutes / 60) + " hours ago";
        }

        return String.format("[%s %s] %s", type, timeStr, content);
    }
}
