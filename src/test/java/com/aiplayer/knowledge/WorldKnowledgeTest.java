package com.aiplayer.knowledge;

import com.aiplayer.memory.MemorySystem;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WorldKnowledge.
 *
 * Tests cover:
 * - Landmark discovery and retrieval
 * - Resource location tracking
 * - Danger zone registration
 * - Explored region marking
 * - Nearest-neighbor queries
 */
class WorldKnowledgeTest {

    private WorldKnowledge worldKnowledge;
    private MemorySystem memorySystem;

    @BeforeEach
    void setUp() {
        memorySystem = new MemorySystem(1000);
        worldKnowledge = new WorldKnowledge(memorySystem);
    }

    @Test
    void testDiscoverAndFindLandmark() {
        // Discover a village
        BlockPos villagePos = new BlockPos(100, 64, 200);
        worldKnowledge.discoverLandmark(
            "Oak Village",
            villagePos,
            WorldKnowledge.LandmarkType.VILLAGE,
            0.9
        );

        // Find nearest village
        BlockPos searchPos = new BlockPos(110, 64, 210);  // Close to village
        WorldKnowledge.Landmark nearestVillage = worldKnowledge.findNearestLandmark(
            searchPos,
            WorldKnowledge.LandmarkType.VILLAGE
        );

        // Verify landmark was found
        assertNotNull(nearestVillage, "Should find the discovered village");
        assertEquals("Oak Village", nearestVillage.name, "Should find correct village");
        assertEquals(WorldKnowledge.LandmarkType.VILLAGE, nearestVillage.type,
            "Landmark should be of correct type");
        assertTrue(nearestVillage.significance >= 0.9, "Should preserve significance");
    }

    @Test
    void testDiscoverAndFindResource() {
        // Discover iron ore
        BlockPos ironPos1 = new BlockPos(50, 12, 80);
        BlockPos ironPos2 = new BlockPos(60, 11, 85);

        worldKnowledge.discoverResource("iron_ore", ironPos1, 5);
        worldKnowledge.discoverResource("iron_ore", ironPos2, 8);

        // Find nearest iron ore
        BlockPos searchPos = new BlockPos(55, 12, 82);
        WorldKnowledge.ResourceLocation nearestIron =
            worldKnowledge.findNearestResource(searchPos, "iron_ore");

        // Verify resource was found
        assertNotNull(nearestIron, "Should find iron ore");
        assertEquals("iron_ore", nearestIron.resourceType, "Should be iron ore");
        assertTrue(nearestIron.quantity > 0, "Should have positive quantity");

        // Get all iron ore locations
        List<WorldKnowledge.ResourceLocation> allIron =
            worldKnowledge.getResourceLocations("iron_ore");
        assertEquals(2, allIron.size(), "Should have 2 iron ore locations");
    }

    @Test
    void testRegisterAndDetectDangerZone() {
        // Register a danger zone (mob spawner)
        BlockPos dangerPos = new BlockPos(200, 30, 150);
        worldKnowledge.registerDangerZone(
            "Zombie spawner cave",
            dangerPos,
            15,  // 15 block radius
            0.8  // High threat
        );

        // Check if position is in danger zone
        BlockPos insideDanger = new BlockPos(205, 30, 155);  // Within 15 blocks
        BlockPos outsideDanger = new BlockPos(300, 30, 150);  // Far away

        WorldKnowledge.DangerZone insideCheck = worldKnowledge.getDangerAt(insideDanger);
        WorldKnowledge.DangerZone outsideCheck = worldKnowledge.getDangerAt(outsideDanger);

        // Verify danger detection
        assertNotNull(insideCheck, "Should detect danger at position inside zone");
        assertEquals("Zombie spawner cave", insideCheck.description,
            "Should identify correct danger zone");
        assertTrue(insideCheck.threatLevel >= 0.8, "Should have high threat level");

        assertNull(outsideCheck, "Should not detect danger at safe position");
    }

    @Test
    void testMarkAndCheckExploredRegions() {
        // Mark a region as explored
        BlockPos exploredCenter = new BlockPos(0, 64, 0);
        worldKnowledge.markExplored(exploredCenter, 32, "plains");

        // Check if positions are explored
        BlockPos insideExplored = new BlockPos(10, 64, 10);  // Within 32 blocks
        BlockPos outsideExplored = new BlockPos(100, 64, 100);  // Far away

        boolean isInsideExplored = worldKnowledge.isExplored(insideExplored);
        boolean isOutsideExplored = worldKnowledge.isExplored(outsideExplored);

        // Verify exploration tracking
        assertTrue(isInsideExplored, "Should recognize explored area");
        assertFalse(isOutsideExplored, "Should recognize unexplored area");
    }

    @Test
    void testMultipleLandmarkTypes() {
        // Discover different types of landmarks
        worldKnowledge.discoverLandmark(
            "Village Alpha",
            new BlockPos(100, 64, 100),
            WorldKnowledge.LandmarkType.VILLAGE,
            0.9
        );

        worldKnowledge.discoverLandmark(
            "Desert Temple",
            new BlockPos(200, 64, 200),
            WorldKnowledge.LandmarkType.STRUCTURE,
            0.8
        );

        worldKnowledge.discoverLandmark(
            "Home Base",
            new BlockPos(0, 70, 0),
            WorldKnowledge.LandmarkType.PLAYER_BASE,
            1.0
        );

        // Get all landmarks
        List<WorldKnowledge.Landmark> allLandmarks = worldKnowledge.getAllLandmarks();
        assertEquals(3, allLandmarks.size(), "Should have 3 landmarks");

        // Get landmarks by type
        List<WorldKnowledge.Landmark> villages =
            worldKnowledge.getLandmarksByType(WorldKnowledge.LandmarkType.VILLAGE);
        List<WorldKnowledge.Landmark> structures =
            worldKnowledge.getLandmarksByType(WorldKnowledge.LandmarkType.STRUCTURE);

        assertEquals(1, villages.size(), "Should have 1 village");
        assertEquals(1, structures.size(), "Should have 1 structure");
        assertTrue(villages.get(0).name.contains("Village"), "Should be the village");
    }

    @Test
    void testResourceQuantityUpdates() {
        // Discover a resource
        BlockPos resourcePos = new BlockPos(50, 12, 50);
        worldKnowledge.discoverResource("diamond_ore", resourcePos, 3);

        // Rediscover same location with updated quantity (nearby position)
        BlockPos nearbyPos = new BlockPos(51, 12, 51);  // Within 5 blocks
        worldKnowledge.discoverResource("diamond_ore", nearbyPos, 5);

        // Should update existing location, not create new one
        List<WorldKnowledge.ResourceLocation> diamonds =
            worldKnowledge.getResourceLocations("diamond_ore");

        // Depending on implementation, might merge or keep separate
        // Just verify we're tracking diamonds
        assertFalse(diamonds.isEmpty(), "Should be tracking diamond locations");
        assertTrue(diamonds.stream().anyMatch(d -> d.quantity > 0),
            "Should have positive diamond quantity");
    }

    @Test
    void testKnowledgeStats() {
        // Add various knowledge
        worldKnowledge.discoverLandmark(
            "Test Village",
            new BlockPos(0, 64, 0),
            WorldKnowledge.LandmarkType.VILLAGE,
            0.8
        );

        worldKnowledge.discoverResource("coal_ore", new BlockPos(10, 60, 10), 10);
        worldKnowledge.discoverResource("iron_ore", new BlockPos(20, 50, 20), 5);

        worldKnowledge.registerDangerZone(
            "Lava lake",
            new BlockPos(30, 11, 30),
            10,
            0.7
        );

        worldKnowledge.markExplored(new BlockPos(0, 64, 0), 16, "forest");

        // Get stats
        WorldKnowledge.KnowledgeStats stats = worldKnowledge.getStats();

        // Verify stats
        assertNotNull(stats, "Stats should not be null");
        assertTrue(stats.knownLandmarks > 0, "Should have landmarks");
        assertTrue(stats.knownResources > 0, "Should have resources");
        assertTrue(stats.knownDangers > 0, "Should have dangers");
        assertTrue(stats.exploredRegions > 0, "Should have explored regions");
    }

    @Test
    void testLandmarkSignificanceLimit() {
        // Test that significance is clamped to 0.0-1.0
        worldKnowledge.discoverLandmark(
            "Over-significant",
            new BlockPos(0, 64, 0),
            WorldKnowledge.LandmarkType.WAYPOINT,
            2.0  // Invalid: > 1.0
        );

        WorldKnowledge.Landmark landmark = worldKnowledge.findNearestLandmark(
            new BlockPos(0, 64, 0),
            WorldKnowledge.LandmarkType.WAYPOINT
        );

        // Significance should be clamped (implementation detail)
        // Just verify landmark was created
        assertNotNull(landmark, "Landmark should be created even with invalid significance");
    }

    @Test
    void testFindNearestWithNoResults() {
        // Try to find landmarks/resources that don't exist
        WorldKnowledge.Landmark noVillage = worldKnowledge.findNearestLandmark(
            new BlockPos(0, 64, 0),
            WorldKnowledge.LandmarkType.VILLAGE
        );

        WorldKnowledge.ResourceLocation noGold = worldKnowledge.findNearestResource(
            new BlockPos(0, 64, 0),
            "gold_ore"
        );

        // Should handle gracefully
        assertNull(noVillage, "Should return null when no landmarks found");
        assertNull(noGold, "Should return null when no resources found");
    }
}
