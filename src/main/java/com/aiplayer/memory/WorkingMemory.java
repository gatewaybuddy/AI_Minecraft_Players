package com.aiplayer.memory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Working Memory - Short-term memory for current context.
 *
 * Keeps the most recent and important memories that are actively
 * being used for decision making.
 *
 * Size: ~10-20 memories (cognitive psychology suggests 7±2 items)
 * Duration: Last 5-10 minutes
 */
public class WorkingMemory {

    private static final int MAX_SIZE = 20;
    private static final long MAX_AGE_MS = 600000; // 10 minutes

    private final List<Memory> memories;

    public WorkingMemory() {
        this.memories = new ArrayList<>();
    }

    /**
     * Add memory to working memory.
     */
    public void add(Memory memory) {
        memories.add(0, memory); // Add to front (most recent)

        // Cleanup if over capacity
        if (memories.size() > MAX_SIZE) {
            memories.remove(memories.size() - 1);
        }
    }

    /**
     * Get recent memories.
     */
    public List<Memory> getRecent(int limit) {
        return memories.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Get all memories in working memory.
     */
    public List<Memory> getAll() {
        return new ArrayList<>(memories);
    }

    /**
     * Remove old memories (called periodically).
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        Iterator<Memory> iter = memories.iterator();

        while (iter.hasNext()) {
            Memory memory = iter.next();
            if (now - memory.getTimestamp() > MAX_AGE_MS) {
                iter.remove();
            }
        }
    }

    /**
     * Get size of working memory.
     */
    public int size() {
        return memories.size();
    }

    /**
     * Clear working memory.
     */
    public void clear() {
        memories.clear();
    }
}
