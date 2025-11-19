package com.aiplayer.skills;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SkillLibrary.
 *
 * Tests cover:
 * - Adding skills to library
 * - Finding reliable skills
 * - Finding relevant skills by task description
 * - Skill success tracking
 * - Category-based organization
 */
class SkillLibraryTest {

    private SkillLibrary library;

    @BeforeEach
    void setUp() {
        library = new SkillLibrary();
    }

    @Test
    void testAddAndRetrieveSkill() {
        // Create a test skill
        Skill miningSkill = new Skill(
            "Mine Oak Logs",
            "Find and mine oak trees for wood",
            Skill.SkillCategory.MINING,
            Arrays.asList("wooden_axe"),  // Prerequisites
            Arrays.asList(
                "Scan surroundings for oak trees",
                "Pathfind to nearest oak tree",
                "Mine all log blocks",
                "Collect dropped items"
            ),
            3  // Complexity
        );

        // Add to library
        library.addSkill(miningSkill);

        // Verify skill was added
        List<Skill> allSkills = library.getAllSkills();
        assertTrue(allSkills.contains(miningSkill), "Library should contain added skill");
        assertEquals(6, allSkills.size(), "Library should have 6 skills (5 basic + 1 added)");
    }

    @Test
    void testGetReliableSkills() {
        // Create a skill and mark it as reliable through successful uses
        Skill reliableSkill = new Skill(
            "Reliable Skill",
            "A skill that always works",
            Skill.SkillCategory.SURVIVAL,
            Arrays.asList(),
            Arrays.asList("Step 1", "Step 2"),
            2
        );

        // Simulate successful uses to make it reliable
        for (int i = 0; i < 5; i++) {
            reliableSkill.recordSuccess();
        }

        library.addSkill(reliableSkill);

        // Create an unreliable skill (not used enough)
        Skill unreliableSkill = new Skill(
            "Unreliable Skill",
            "A skill that hasn't been tested",
            Skill.SkillCategory.UTILITY,
            Arrays.asList(),
            Arrays.asList("Step 1"),
            1
        );

        library.addSkill(unreliableSkill);

        // Get reliable skills
        List<Skill> reliable = library.getReliableSkills();

        // Verify reliable skill is included and unreliable is excluded
        assertTrue(reliable.stream().anyMatch(s -> s.getName().equals("Reliable Skill")),
            "Reliable skill should be in reliable skills list");
        assertFalse(reliable.stream().anyMatch(s -> s.getName().equals("Unreliable Skill")),
            "Unreliable skill should not be in reliable skills list");
    }

    @Test
    void testFindRelevantSkills() {
        // Create skills with specific keywords
        Skill miningSkill = new Skill(
            "Mine Diamonds",
            "Find and mine diamond ore deep underground",
            Skill.SkillCategory.MINING,
            Arrays.asList("iron_pickaxe"),
            Arrays.asList("Go to Y-level 11", "Mine diamond ore"),
            7
        );

        Skill buildingSkill = new Skill(
            "Build Shelter",
            "Construct a simple shelter for protection",
            Skill.SkillCategory.BUILDING,
            Arrays.asList("wood_planks"),
            Arrays.asList("Place walls", "Add roof", "Add door"),
            4
        );

        library.addSkill(miningSkill);
        library.addSkill(buildingSkill);

        // Search for mining-related skills
        List<Skill> miningResults = library.findRelevantSkills("find diamonds", 5);

        // Verify correct skill is found
        assertFalse(miningResults.isEmpty(), "Should find mining-related skills");
        assertTrue(miningResults.stream().anyMatch(s -> s.getName().contains("Diamond")),
            "Should find diamond mining skill");

        // Search for building-related skills
        List<Skill> buildingResults = library.findRelevantSkills("build a house", 5);

        assertFalse(buildingResults.isEmpty(), "Should find building-related skills");
        assertTrue(buildingResults.stream().anyMatch(s -> s.getName().contains("Shelter")),
            "Should find shelter building skill");
    }

    @Test
    void testSkillsByCategory() {
        // Create skills in different categories
        Skill combatSkill = new Skill(
            "Fight Zombie",
            "Engage and defeat a zombie",
            Skill.SkillCategory.COMBAT,
            Arrays.asList("sword"),
            Arrays.asList("Approach zombie", "Attack until defeated"),
            5
        );

        Skill farmingSkill = new Skill(
            "Plant Wheat",
            "Plant and grow wheat crops",
            Skill.SkillCategory.FARMING,
            Arrays.asList("seeds", "hoe"),
            Arrays.asList("Till soil", "Plant seeds", "Wait for growth"),
            3
        );

        library.addSkill(combatSkill);
        library.addSkill(farmingSkill);

        // Get skills by category
        List<Skill> combatSkills = library.getSkillsByCategory(Skill.SkillCategory.COMBAT);
        List<Skill> farmingSkills = library.getSkillsByCategory(Skill.SkillCategory.FARMING);

        // Verify category filtering
        assertTrue(combatSkills.stream().anyMatch(s -> s.getName().contains("Zombie")),
            "Combat category should contain zombie fighting skill");
        assertTrue(farmingSkills.stream().anyMatch(s -> s.getName().contains("Wheat")),
            "Farming category should contain wheat planting skill");
        assertFalse(combatSkills.stream().anyMatch(s -> s.getCategory() != Skill.SkillCategory.COMBAT),
            "Combat skills should all be combat category");
    }

    @Test
    void testInitialSkillsLoaded() {
        // Verify library comes with pre-loaded basic skills
        List<Skill> allSkills = library.getAllSkills();

        assertFalse(allSkills.isEmpty(), "Library should have pre-loaded skills");
        assertEquals(5, allSkills.size(), "Library should have 5 basic skills initially");

        // Verify skill names (from SkillLibrary initialization)
        List<String> skillNames = allSkills.stream()
            .map(Skill::getName)
            .toList();

        assertTrue(skillNames.contains("Mine Logs"), "Should have 'Mine Logs' skill");
        assertTrue(skillNames.contains("Find Food"), "Should have 'Find Food' skill");
        assertTrue(skillNames.contains("Explore Safely"), "Should have 'Explore Safely' skill");
    }

    @Test
    void testGetStats() {
        // Get stats for initially loaded library
        SkillLibrary.SkillStats stats = library.getStats();

        assertNotNull(stats, "Stats should not be null");
        assertEquals(5, stats.getTotalSkills(), "Should have 5 initial skills");
        assertTrue(stats.getTotalSkills() > 0, "Should have some skills");

        // Add more skills and verify stats update
        library.addSkill(new Skill(
            "Test Skill",
            "A test skill",
            Skill.SkillCategory.UTILITY,
            Arrays.asList(),
            Arrays.asList("Step 1"),
            1
        ));

        SkillLibrary.SkillStats updatedStats = library.getStats();
        assertEquals(6, updatedStats.getTotalSkills(), "Stats should update after adding skill");
    }

    @Test
    void testSkillQualityScoring() {
        // Create a new skill
        Skill skill = new Skill(
            "Quality Test Skill",
            "Testing quality scoring",
            Skill.SkillCategory.UTILITY,
            Arrays.asList(),
            Arrays.asList("Step 1", "Step 2"),
            5
        );

        library.addSkill(skill);

        // Initially, quality should be low (no usage history)
        double initialQuality = skill.getQuality();
        assertTrue(initialQuality >= 0.0 && initialQuality <= 1.0,
            "Quality should be between 0 and 1");

        // Record successes
        for (int i = 0; i < 10; i++) {
            skill.recordSuccess();
        }

        // Quality should improve with successful uses
        double improvedQuality = skill.getQuality();
        assertTrue(improvedQuality > initialQuality,
            "Quality should improve with successful uses");

        // Record some failures
        for (int i = 0; i < 5; i++) {
            skill.recordFailure("Test failure");
        }

        // Quality should be impacted by failures
        double mixedQuality = skill.getQuality();
        assertTrue(mixedQuality < 1.0,
            "Quality should be less than perfect with failures");
        assertTrue(mixedQuality > 0.0,
            "Quality should still be positive with majority successes");
    }
}
