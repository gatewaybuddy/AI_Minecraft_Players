package com.aiplayer.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MemorySystem.
 *
 * Tests cover:
 * - Memory storage and retrieval
 * - Working memory capacity limits
 * - Memory recall by type and query
 * - Semantic memory learning
 * - Memory consolidation
 */
class MemorySystemTest {

    private MemorySystem memorySystem;
    private static final int MAX_EPISODIC_MEMORIES = 1000;

    @BeforeEach
    void setUp() {
        memorySystem = new MemorySystem(MAX_EPISODIC_MEMORIES);
    }

    @Test
    void testStoreAndRecallMemory() {
        // Create and store a memory
        Memory memory = new Memory(
            Memory.MemoryType.OBSERVATION,
            "Spotted a zombie at coordinates (100, 64, 200)",
            0.8
        );

        memorySystem.store(memory);

        // Recall by query
        List<Memory> recalled = memorySystem.recall("zombie", 5);

        // Verify memory was stored and can be recalled
        assertFalse(recalled.isEmpty(), "Should recall at least one memory");
        assertTrue(recalled.stream().anyMatch(m -> m.getContent().contains("zombie")),
            "Recalled memories should contain the zombie observation");
    }

    @Test
    void testWorkingMemoryContainsImportantMemories() {
        // Store a high-importance memory
        memorySystem.storeObservation("Critical: Low health warning", 0.9);

        // Store a low-importance memory
        memorySystem.storeObservation("Saw a chicken", 0.2);

        // Recall recent memories from working memory
        List<Memory> recent = memorySystem.recallRecent(10);

        // High importance memory should be in working memory
        assertTrue(recent.stream().anyMatch(m -> m.getContent().contains("Low health")),
            "High importance memories should be in working memory");
    }

    @Test
    void testRecallByType() {
        // Store memories of different types
        memorySystem.storeObservation("Found diamonds", 0.9);
        memorySystem.storeAction("Mined 3 diamond ore blocks", 0.8);
        memorySystem.storeLearning("Diamonds are found at Y-level 11", 0.85);

        // Recall only observations
        List<Memory> observations = memorySystem.recallByType(Memory.MemoryType.OBSERVATION, 10);

        // Verify correct type filtering
        assertFalse(observations.isEmpty(), "Should have observations");
        assertTrue(observations.stream().allMatch(m -> m.getType() == Memory.MemoryType.OBSERVATION),
            "All recalled memories should be observations");
        assertTrue(observations.stream().anyMatch(m -> m.getContent().contains("diamonds")),
            "Should recall the diamond observation");
    }

    @Test
    void testSemanticMemoryLearning() {
        // Learn facts
        memorySystem.learn("diamond_location", "Y-level 11");
        memorySystem.learn("best_pickaxe", "iron or better");

        // Retrieve facts
        Optional<String> location = memorySystem.retrieve("diamond_location");
        Optional<String> pickaxe = memorySystem.retrieve("best_pickaxe");

        // Verify facts were learned
        assertTrue(location.isPresent(), "Diamond location should be learned");
        assertEquals("Y-level 11", location.get(), "Should retrieve correct diamond location");
        assertTrue(pickaxe.isPresent(), "Pickaxe fact should be learned");
        assertEquals("iron or better", pickaxe.get(), "Should retrieve correct pickaxe fact");

        // Test non-existent fact
        Optional<String> nonExistent = memorySystem.retrieve("nonexistent_key");
        assertFalse(nonExistent.isPresent(), "Non-existent fact should not be found");
    }

    @Test
    void testPlayerRelationships() {
        // Test relationship updates
        memorySystem.updateRelationship("Steve", 10);  // Positive interaction
        memorySystem.updateRelationship("Steve", 5);   // Another positive
        memorySystem.updateRelationship("Alex", -5);    // Negative interaction

        // Retrieve relationships
        int steveRelationship = memorySystem.getRelationship("Steve");
        int alexRelationship = memorySystem.getRelationship("Alex");
        int unknownRelationship = memorySystem.getRelationship("Unknown");

        // Verify relationships
        assertEquals(15, steveRelationship, "Steve relationship should be 15 (10 + 5)");
        assertEquals(-5, alexRelationship, "Alex relationship should be -5");
        assertEquals(0, unknownRelationship, "Unknown player should have neutral relationship");
    }

    @Test
    void testStrategyRatings() {
        // Test strategy rating updates
        memorySystem.updateStrategy("flee_from_creeper", true);  // Success
        memorySystem.updateStrategy("flee_from_creeper", true);  // Success
        memorySystem.updateStrategy("flee_from_creeper", false); // Failure

        memorySystem.updateStrategy("attack_zombie", true);      // Success

        // Retrieve strategy ratings
        double fleeRating = memorySystem.getStrategyRating("flee_from_creeper");
        double attackRating = memorySystem.getStrategyRating("attack_zombie");
        double unknownRating = memorySystem.getStrategyRating("unknown_strategy");

        // Verify ratings
        assertTrue(fleeRating > 0.5, "Flee strategy should have positive rating (2 success, 1 fail)");
        assertTrue(fleeRating < 1.0, "Flee strategy should not be perfect");
        assertEquals(1.0, attackRating, 0.01, "Attack strategy should be perfect (1 success, 0 fail)");
        assertEquals(0.0, unknownRating, 0.01, "Unknown strategy should have neutral rating");
    }

    @Test
    void testMemoryStats() {
        // Initially empty
        String initialStats = memorySystem.getStats();
        assertNotNull(initialStats, "Stats should not be null");
        assertTrue(initialStats.contains("0 episodic"), "Should show 0 episodic memories initially");

        // Add some memories
        memorySystem.storeObservation("Test observation 1", 0.7);
        memorySystem.storeAction("Test action 1", 0.6);
        memorySystem.learn("test_fact", "test_value");

        // Check updated stats
        String updatedStats = memorySystem.getStats();
        assertFalse(updatedStats.contains("0 episodic"), "Should show non-zero episodic memories");
        assertFalse(updatedStats.contains("0 semantic"), "Should show non-zero semantic memories");
    }

    @Test
    void testMemoryConsolidation() {
        // Add memories
        for (int i = 0; i < 10; i++) {
            memorySystem.storeObservation("Observation " + i, 0.5);
        }

        int beforeConsolidation = memorySystem.getTotalMemories();

        // Run consolidation
        memorySystem.consolidate();

        // Verify consolidation ran (memories may or may not be pruned depending on implementation)
        int afterConsolidation = memorySystem.getTotalMemories();
        assertTrue(afterConsolidation <= beforeConsolidation,
            "Consolidation should not increase memory count");
    }

    @Test
    void testFormatRecentForContext() {
        // Add some memories
        memorySystem.storeObservation("Saw a creeper", 0.8);
        memorySystem.storeAction("Ran away from creeper", 0.7);

        // Format for LLM context
        String context = memorySystem.formatRecentForContext(5);

        // Verify formatting
        assertNotNull(context, "Context should not be null");
        assertTrue(context.contains("creeper"), "Context should mention creeper");
    }
}
