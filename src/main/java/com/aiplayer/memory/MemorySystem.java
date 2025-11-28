package com.aiplayer.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Memory System - Manages all memories for an AI player.
 *
 * Three types of memory:
 * 1. Working Memory - Current context (last few minutes)
 * 2. Episodic Memory - What happened, when, where
 * 3. Semantic Memory - Learned facts, strategies, relationships
 *
 * Based on cognitive psychology models and Voyager's memory system.
 */
public class MemorySystem {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemorySystem.class);

    private final EpisodicMemory episodicMemory;
    private final SemanticMemory semanticMemory;
    private final WorkingMemory workingMemory;

    private final int maxEpisodicMemories;

    public MemorySystem(int maxEpisodicMemories) {
        this.maxEpisodicMemories = maxEpisodicMemories;
        this.episodicMemory = new EpisodicMemory(maxEpisodicMemories);
        this.semanticMemory = new SemanticMemory();
        this.workingMemory = new WorkingMemory();
    }

    /**
     * Default constructor with standard memory limits.
     */
    public MemorySystem() {
        this(1000); // Default to 1000 episodic memories
    }

    /**
     * Store a new memory.
     */
    public void store(Memory memory) {
        // Add to episodic memory
        episodicMemory.store(memory);

        // Add to working memory if important enough
        if (memory.getImportance() >= 0.5 || memory.isRecent()) {
            workingMemory.add(memory);
        }

        // Extract learnings for semantic memory
        if (memory.getType() == Memory.MemoryType.LEARNING ||
            memory.getType() == Memory.MemoryType.GOAL_COMPLETION) {
            // Semantic memory is updated separately via learn() method
        }

        LOGGER.debug("Stored memory: {}", memory);
    }

    /**
     * Store an observation memory.
     */
    public void storeObservation(String content, double importance) {
        store(new Memory(Memory.MemoryType.OBSERVATION, content, importance));
    }

    /**
     * Store an action memory.
     */
    public void storeAction(String content, double importance) {
        store(new Memory(Memory.MemoryType.ACTION, content, importance));
    }

    /**
     * Store a learning.
     */
    public void storeLearning(String content, double importance) {
        store(new Memory(Memory.MemoryType.LEARNING, content, importance));
    }

    /**
     * Recall memories by query.
     */
    public List<Memory> recall(String query, int limit) {
        return episodicMemory.recall(query, limit);
    }

    /**
     * Recall recent memories (working memory).
     */
    public List<Memory> recallRecent(int limit) {
        return workingMemory.getRecent(limit);
    }

    /**
     * Recall memories by type.
     */
    public List<Memory> recallByType(Memory.MemoryType type, int limit) {
        return episodicMemory.recallByType(type, limit);
    }

    /**
     * Get all memories in time range.
     */
    public List<Memory> recallByTimeRange(long startTime, long endTime) {
        return episodicMemory.recallByTimeRange(startTime, endTime);
    }

    /**
     * Learn a fact (semantic memory).
     */
    public void learn(String key, String value) {
        semanticMemory.learn(key, value);
    }

    /**
     * Retrieve a learned fact.
     */
    public Optional<String> retrieve(String key) {
        return semanticMemory.retrieve(key);
    }

    /**
     * Update player relationship.
     */
    public void updateRelationship(String playerName, int delta) {
        semanticMemory.updateRelationship(playerName, delta);
    }

    /**
     * Get relationship score with a player.
     */
    public int getRelationship(String playerName) {
        return semanticMemory.getRelationship(playerName);
    }

    /**
     * Update strategy rating.
     */
    public void updateStrategy(String strategy, boolean success) {
        semanticMemory.updateStrategyRating(strategy, success);
    }

    /**
     * Get strategy success rate.
     */
    public double getStrategyRating(String strategy) {
        return semanticMemory.getStrategyRating(strategy);
    }

    /**
     * Get total number of memories.
     */
    public int getTotalMemories() {
        return episodicMemory.size();
    }

    /**
     * Get number of semantic facts.
     */
    public int getSemanticFactCount() {
        return semanticMemory.getFactCount();
    }

    /**
     * Get working memory.
     */
    public WorkingMemory getWorkingMemory() {
        return workingMemory;
    }

    /**
     * Get episodic memory.
     */
    public EpisodicMemory getEpisodicMemory() {
        return episodicMemory;
    }

    /**
     * Get semantic memory.
     */
    public SemanticMemory getSemanticMemory() {
        return semanticMemory;
    }

    /**
     * Get recent memories (alias for recallRecent).
     */
    public List<Memory> getRecentMemories(int limit) {
        return recallRecent(limit);
    }

    /**
     * Clear old memories (called periodically).
     */
    public void consolidate() {
        episodicMemory.consolidate();
        workingMemory.cleanup();
        LOGGER.debug("Memory consolidation complete");
    }

    /**
     * Alias for consolidate() - cleanup old memories.
     */
    public void cleanup() {
        consolidate();
    }

    /**
     * Get memory statistics.
     */
    public String getStats() {
        return String.format("Memories: %d episodic, %d semantic, %d working",
            episodicMemory.size(),
            semanticMemory.getFactCount(),
            workingMemory.size());
    }

    /**
     * Format recent memories for LLM context.
     */
    public String formatRecentForContext(int limit) {
        List<Memory> recent = recallRecent(limit);
        if (recent.isEmpty()) {
            return "No recent memories.";
        }

        StringBuilder sb = new StringBuilder("Recent memories:\n");
        for (Memory memory : recent) {
            sb.append("- ").append(memory.format()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Format memories by type for LLM context.
     */
    public String formatByTypeForContext(Memory.MemoryType type, int limit) {
        List<Memory> memories = recallByType(type, limit);
        if (memories.isEmpty()) {
            return "No " + type + " memories.";
        }

        StringBuilder sb = new StringBuilder(type + " memories:\n");
        for (Memory memory : memories) {
            sb.append("- ").append(memory.format()).append("\n");
        }
        return sb.toString();
    }
}
