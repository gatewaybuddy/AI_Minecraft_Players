package com.aiplayer.memory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Semantic Memory - Stores learned facts, strategies, and relationships.
 *
 * This is the AI's "knowledge base" - things it has learned:
 * - Facts: "spawn_location = (100, 64, 200)"
 * - Relationships: "Steve" → trust_score = 85
 * - Strategies: "mine_with_pickaxe" → success_rate = 0.92
 *
 * Unlike episodic memory (what happened), semantic memory is about
 * general knowledge and patterns.
 */
public class SemanticMemory {

    private final Map<String, String> facts;
    private final Map<String, Integer> playerRelationships;
    private final Map<String, StrategyRating> strategyRatings;

    public SemanticMemory() {
        this.facts = new HashMap<>();
        this.playerRelationships = new HashMap<>();
        this.strategyRatings = new HashMap<>();
    }

    /**
     * Learn a fact (key-value pair).
     */
    public void learn(String key, String value) {
        facts.put(key, value);
    }

    /**
     * Retrieve a learned fact.
     */
    public Optional<String> retrieve(String key) {
        return Optional.ofNullable(facts.get(key));
    }

    /**
     * Check if a fact is known.
     */
    public boolean knows(String key) {
        return facts.containsKey(key);
    }

    /**
     * Update relationship with a player.
     * Positive delta = more trust, negative = less trust.
     */
    public void updateRelationship(String playerName, int delta) {
        int current = playerRelationships.getOrDefault(playerName, 50); // Default: neutral (50)
        int updated = Math.max(0, Math.min(100, current + delta)); // Clamp to 0-100
        playerRelationships.put(playerName, updated);
    }

    /**
     * Get relationship score with a player (0-100).
     */
    public int getRelationship(String playerName) {
        return playerRelationships.getOrDefault(playerName, 50); // Default: neutral
    }

    /**
     * Check if player is trusted (score >= 70).
     */
    public boolean isTrusted(String playerName) {
        return getRelationship(playerName) >= 70;
    }

    /**
     * Update strategy rating based on success/failure.
     */
    public void updateStrategyRating(String strategy, boolean success) {
        StrategyRating rating = strategyRatings.computeIfAbsent(strategy, k -> new StrategyRating());
        rating.record(success);
    }

    /**
     * Get strategy success rate (0.0 to 1.0).
     */
    public double getStrategyRating(String strategy) {
        StrategyRating rating = strategyRatings.get(strategy);
        return rating != null ? rating.getSuccessRate() : 0.5; // Default: 50% unknown
    }

    /**
     * Get number of times strategy was used.
     */
    public int getStrategyUsageCount(String strategy) {
        StrategyRating rating = strategyRatings.get(strategy);
        return rating != null ? rating.getTotalUses() : 0;
    }

    /**
     * Get all known facts.
     */
    public Map<String, String> getAllFacts() {
        return new HashMap<>(facts);
    }

    /**
     * Get all known players.
     */
    public Map<String, Integer> getAllRelationships() {
        return new HashMap<>(playerRelationships);
    }

    /**
     * Get number of facts stored.
     */
    public int getFactCount() {
        return facts.size() + playerRelationships.size() + strategyRatings.size();
    }

    /**
     * Clear all semantic memory.
     */
    public void clear() {
        facts.clear();
        playerRelationships.clear();
        strategyRatings.clear();
    }

    /**
     * Strategy rating tracker.
     */
    private static class StrategyRating {
        private int successes;
        private int failures;

        public void record(boolean success) {
            if (success) {
                successes++;
            } else {
                failures++;
            }
        }

        public double getSuccessRate() {
            int total = successes + failures;
            return total > 0 ? (double) successes / total : 0.5;
        }

        public int getTotalUses() {
            return successes + failures;
        }
    }
}
