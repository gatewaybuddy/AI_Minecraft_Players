package com.aiplayer.memory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Episodic Memory - Stores what happened, when, and where.
 *
 * This is the AI's "experience log" - a chronological record of events.
 * Memories are stored in a deque for efficient access to recent items,
 * and indexed by type for fast retrieval.
 */
public class EpisodicMemory {

    private final Deque<Memory> memories;
    private final Map<Memory.MemoryType, List<Memory>> memoriesByType;
    private final int maxSize;

    public EpisodicMemory(int maxSize) {
        this.maxSize = maxSize;
        this.memories = new LinkedList<>();
        this.memoriesByType = new EnumMap<>(Memory.MemoryType.class);

        // Initialize type indices
        for (Memory.MemoryType type : Memory.MemoryType.values()) {
            memoriesByType.put(type, new ArrayList<>());
        }
    }

    /**
     * Store a new memory.
     */
    public void store(Memory memory) {
        // Add to main storage
        memories.addFirst(memory);

        // Add to type index
        memoriesByType.get(memory.getType()).add(memory);

        // Trim if over capacity
        if (memories.size() > maxSize) {
            Memory removed = memories.removeLast();
            memoriesByType.get(removed.getType()).remove(removed);
        }
    }

    /**
     * Recall memories matching a query string (simple text search).
     */
    public List<Memory> recall(String query, int limit) {
        String lowerQuery = query.toLowerCase();

        return memories.stream()
            .filter(m -> m.getContent().toLowerCase().contains(lowerQuery))
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Recall memories by type.
     */
    public List<Memory> recallByType(Memory.MemoryType type, int limit) {
        List<Memory> typeMemories = memoriesByType.get(type);
        return typeMemories.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Recall memories in time range.
     */
    public List<Memory> recallByTimeRange(long startTime, long endTime) {
        return memories.stream()
            .filter(m -> m.getTimestamp() >= startTime && m.getTimestamp() <= endTime)
            .collect(Collectors.toList());
    }

    /**
     * Get most recent N memories.
     */
    public List<Memory> getRecent(int limit) {
        return memories.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Get most important memories (by importance score).
     */
    public List<Memory> getMostImportant(int limit) {
        return memories.stream()
            .sorted(Comparator.comparingDouble(Memory::getImportance).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Get most relevant memories (importance + recency).
     */
    public List<Memory> getMostRelevant(int limit) {
        return memories.stream()
            .sorted(Comparator.comparingDouble(Memory::getRelevance).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Consolidate memories (remove very old, low-importance ones).
     */
    public void consolidate() {
        // Remove memories older than 24 hours with low importance
        long cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000);

        memories.removeIf(m ->
            m.getTimestamp() < cutoffTime &&
            m.getImportance() < 0.3
        );

        // Rebuild type indices
        for (Memory.MemoryType type : Memory.MemoryType.values()) {
            List<Memory> typeList = memoriesByType.get(type);
            typeList.removeIf(m ->
                m.getTimestamp() < cutoffTime &&
                m.getImportance() < 0.3
            );
        }
    }

    /**
     * Get total number of memories.
     */
    public int size() {
        return memories.size();
    }

    /**
     * Clear all memories.
     */
    public void clear() {
        memories.clear();
        for (List<Memory> list : memoriesByType.values()) {
            list.clear();
        }
    }
}
