package com.aiplayer.learning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * World knowledge acquisition system (Phase 5).
 *
 * Stores and manages knowledge about the Minecraft world:
 * - Landmarks (villages, structures, interesting locations)
 * - Resource locations (ore veins, tree groves, water sources)
 * - Danger zones (mob spawners, cliffs, lava)
 * - Explored areas
 * - Environmental patterns
 *
 * Phase 5: Advanced AI - World Knowledge System
 */
public class WorldKnowledge {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldKnowledge.class);

    // Location storage
    private final Map<String, Landmark> landmarks;
    private final Map<String, ResourceLocation> resourceLocations;
    private final Set<ChunkCoordinate> exploredChunks;

    // World facts
    private final Map<String, String> worldFacts; // Key-value facts about the world

    /**
     * Represents a landmark (notable location).
     */
    public static class Landmark {
        public enum LandmarkType {
            VILLAGE,
            STRUCTURE,
            SPAWN_POINT,
            HOME_BASE,
            WAYPOINT,
            DANGER_ZONE,
            RESOURCE_AREA,
            LANDMARK
        }

        private final String name;
        private final Position position;
        private final LandmarkType type;
        private final String description;
        private final long discoveredTimestamp;
        private int visits;

        public Landmark(String name, Position position, LandmarkType type, String description) {
            this.name = name;
            this.position = position;
            this.type = type;
            this.description = description;
            this.discoveredTimestamp = System.currentTimeMillis();
            this.visits = 0;
        }

        public String getName() { return name; }
        public Position getPosition() { return position; }
        public LandmarkType getType() { return type; }
        public String getDescription() { return description; }
        public long getDiscoveredTimestamp() { return discoveredTimestamp; }
        public int getVisits() { return visits; }

        public void recordVisit() {
            visits++;
        }

        @Override
        public String toString() {
            return String.format("%s (%s) at %s - %s", name, type, position, description);
        }
    }

    /**
     * Represents a resource location.
     */
    public static class ResourceLocation {
        public enum ResourceType {
            WOOD,
            STONE,
            COAL,
            IRON,
            GOLD,
            DIAMOND,
            WATER,
            FOOD,
            OTHER
        }

        private final Position position;
        private final ResourceType resourceType;
        private final int estimatedQuantity;
        private final long discoveredTimestamp;
        private boolean depleted;

        public ResourceLocation(Position position, ResourceType resourceType, int estimatedQuantity) {
            this.position = position;
            this.resourceType = resourceType;
            this.estimatedQuantity = estimatedQuantity;
            this.discoveredTimestamp = System.currentTimeMillis();
            this.depleted = false;
        }

        public Position getPosition() { return position; }
        public ResourceType getResourceType() { return resourceType; }
        public int getEstimatedQuantity() { return estimatedQuantity; }
        public boolean isDepleted() { return depleted; }

        public void markDepleted() {
            depleted = true;
        }

        @Override
        public String toString() {
            return String.format("%s at %s (%d units%s)",
                resourceType, position, estimatedQuantity,
                depleted ? ", depleted" : "");
        }
    }

    /**
     * Represents a 3D position.
     */
    public static class Position {
        public final double x;
        public final double y;
        public final double z;

        public Position(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public double distanceTo(Position other) {
            double dx = x - other.x;
            double dy = y - other.y;
            double dz = z - other.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        public ChunkCoordinate toChunkCoordinate() {
            return new ChunkCoordinate((int) x >> 4, (int) z >> 4);
        }

        @Override
        public String toString() {
            return String.format("(%.1f, %.1f, %.1f)", x, y, z);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Position)) return false;
            Position position = (Position) o;
            return Double.compare(position.x, x) == 0 &&
                   Double.compare(position.y, y) == 0 &&
                   Double.compare(position.z, z) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }

    /**
     * Represents a chunk coordinate.
     */
    public static class ChunkCoordinate {
        public final int x;
        public final int z;

        public ChunkCoordinate(int x, int z) {
            this.x = x;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ChunkCoordinate)) return false;
            ChunkCoordinate that = (ChunkCoordinate) o;
            return x == that.x && z == that.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, z);
        }

        @Override
        public String toString() {
            return String.format("Chunk(%d, %d)", x, z);
        }
    }

    /**
     * Create a world knowledge system.
     */
    public WorldKnowledge() {
        this.landmarks = new ConcurrentHashMap<>();
        this.resourceLocations = new ConcurrentHashMap<>();
        this.exploredChunks = ConcurrentHashMap.newKeySet();
        this.worldFacts = new ConcurrentHashMap<>();

        LOGGER.debug("WorldKnowledge initialized");
    }

    /**
     * Add a landmark.
     *
     * @param landmark The landmark to add
     */
    public void addLandmark(Landmark landmark) {
        landmarks.put(landmark.getName(), landmark);
        LOGGER.info("Discovered landmark: {}", landmark);
    }

    /**
     * Add a landmark by parameters.
     */
    public void addLandmark(String name, Position position, Landmark.LandmarkType type, String description) {
        addLandmark(new Landmark(name, position, type, description));
    }

    /**
     * Get a landmark by name.
     *
     * @param name The landmark name
     * @return The landmark, or null if not found
     */
    public Landmark getLandmark(String name) {
        return landmarks.get(name);
    }

    /**
     * Get all landmarks of a type.
     *
     * @param type The landmark type
     * @return List of matching landmarks
     */
    public List<Landmark> getLandmarksByType(Landmark.LandmarkType type) {
        List<Landmark> result = new ArrayList<>();
        for (Landmark landmark : landmarks.values()) {
            if (landmark.getType() == type) {
                result.add(landmark);
            }
        }
        return result;
    }

    /**
     * Find nearest landmark to a position.
     *
     * @param position The position
     * @param type Optional type filter (null for any type)
     * @return Nearest landmark, or null if none
     */
    public Landmark findNearestLandmark(Position position, Landmark.LandmarkType type) {
        Landmark nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Landmark landmark : landmarks.values()) {
            if (type != null && landmark.getType() != type) {
                continue;
            }

            double distance = position.distanceTo(landmark.getPosition());
            if (distance < minDistance) {
                minDistance = distance;
                nearest = landmark;
            }
        }

        return nearest;
    }

    /**
     * Add a resource location.
     *
     * @param resourceLocation The resource location
     */
    public void addResourceLocation(ResourceLocation resourceLocation) {
        String key = resourceLocation.getResourceType() + "_" +
                    resourceLocation.getPosition().toChunkCoordinate();
        resourceLocations.put(key, resourceLocation);
        LOGGER.debug("Discovered resource: {}", resourceLocation);
    }

    /**
     * Find nearest resource of a type.
     *
     * @param position Current position
     * @param resourceType The resource type
     * @return Nearest resource location, or null
     */
    public ResourceLocation findNearestResource(Position position, ResourceLocation.ResourceType resourceType) {
        ResourceLocation nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (ResourceLocation resource : resourceLocations.values()) {
            if (resource.getResourceType() == resourceType && !resource.isDepleted()) {
                double distance = position.distanceTo(resource.getPosition());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = resource;
                }
            }
        }

        return nearest;
    }

    /**
     * Mark a chunk as explored.
     *
     * @param chunk The chunk coordinate
     */
    public void markChunkExplored(ChunkCoordinate chunk) {
        if (exploredChunks.add(chunk)) {
            LOGGER.debug("Explored new chunk: {}", chunk);
        }
    }

    /**
     * Mark position as explored.
     */
    public void markPositionExplored(Position position) {
        markChunkExplored(position.toChunkCoordinate());
    }

    /**
     * Check if a chunk has been explored.
     *
     * @param chunk The chunk coordinate
     * @return true if explored
     */
    public boolean isChunkExplored(ChunkCoordinate chunk) {
        return exploredChunks.contains(chunk);
    }

    /**
     * Get exploration progress (0.0 to 1.0).
     *
     * Based on ratio of explored chunks to a target.
     *
     * @param targetChunks Target number of chunks to explore
     * @return Exploration progress
     */
    public double getExplorationProgress(int targetChunks) {
        return Math.min(1.0, (double) exploredChunks.size() / targetChunks);
    }

    /**
     * Add a world fact.
     *
     * @param key The fact key
     * @param value The fact value
     */
    public void addFact(String key, String value) {
        worldFacts.put(key, value);
        LOGGER.debug("Learned fact: {} = {}", key, value);
    }

    /**
     * Get a world fact.
     *
     * @param key The fact key
     * @return The fact value, or null
     */
    public String getFact(String key) {
        return worldFacts.get(key);
    }

    /**
     * Format knowledge for LLM context.
     *
     * @param currentPosition Current position
     * @param maxEntries Maximum entries to include
     * @return Formatted knowledge string
     */
    public String formatForLLM(Position currentPosition, int maxEntries) {
        StringBuilder sb = new StringBuilder();

        // Nearby landmarks
        List<Landmark> nearbyLandmarks = new ArrayList<>();
        for (Landmark landmark : landmarks.values()) {
            double distance = currentPosition.distanceTo(landmark.getPosition());
            if (distance < 500) { // Within 500 blocks
                nearbyLandmarks.add(landmark);
            }
        }

        if (!nearbyLandmarks.isEmpty()) {
            sb.append("## Nearby Landmarks\n");
            nearbyLandmarks.sort((a, b) ->
                Double.compare(
                    currentPosition.distanceTo(a.getPosition()),
                    currentPosition.distanceTo(b.getPosition())
                )
            );

            int count = 0;
            for (Landmark landmark : nearbyLandmarks) {
                if (count >= maxEntries) break;
                double distance = currentPosition.distanceTo(landmark.getPosition());
                sb.append(String.format("- %s (%.0f blocks away): %s\n",
                    landmark.getName(), distance, landmark.getDescription()));
                count++;
            }
        }

        // Exploration stats
        sb.append(String.format("\nExplored %d chunks\n", exploredChunks.size()));

        return sb.toString();
    }

    /**
     * Get statistics.
     *
     * @return Statistics string
     */
    public String getStatistics() {
        return String.format("Landmarks: %d, Resources: %d, Explored Chunks: %d, Facts: %d",
            landmarks.size(),
            resourceLocations.size(),
            exploredChunks.size(),
            worldFacts.size());
    }

    /**
     * Get all landmarks.
     *
     * @return Collection of landmarks
     */
    public Collection<Landmark> getAllLandmarks() {
        return landmarks.values();
    }

    /**
     * Get explored chunk count.
     *
     * @return Number of explored chunks
     */
    public int getExploredChunkCount() {
        return exploredChunks.size();
    }
}
