package com.aiplayer.knowledge;

import com.aiplayer.memory.Memory;
import com.aiplayer.memory.MemorySystem;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * World Knowledge - Tracks discovered locations and landmarks.
 *
 * Phase 5: Advanced AI
 *
 * Maintains spatial knowledge about the Minecraft world:
 * - Important landmarks (villages, structures, bases)
 * - Resource locations (ore veins, trees, water)
 * - Danger zones (mob spawners, lava, cliffs)
 * - Player bases and meeting points
 * - Environmental patterns (biome locations, terrain features)
 *
 * This allows AI to remember "where things are" and navigate intelligently.
 */
public class WorldKnowledge {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldKnowledge.class);

    private final MemorySystem memorySystem;

    // Knowledge storage
    private final Map<String, Landmark> landmarks;
    private final Map<String, List<ResourceLocation>> resources;
    private final Map<String, DangerZone> dangerZones;
    private final List<ExploredRegion> exploredRegions;

    // Discovery tracking
    private int landmarksDiscovered;
    private int resourcesDiscovered;
    private long lastUpdateTime;

    // Search parameters
    private static final int MAX_LANDMARKS = 100;
    private static final int MAX_RESOURCES_PER_TYPE = 50;
    private static final double LANDMARK_SIGNIFICANCE_THRESHOLD = 0.3;

    public WorldKnowledge(MemorySystem memorySystem) {
        this.memorySystem = memorySystem;
        this.landmarks = new ConcurrentHashMap<>();
        this.resources = new ConcurrentHashMap<>();
        this.dangerZones = new ConcurrentHashMap<>();
        this.exploredRegions = Collections.synchronizedList(new ArrayList<>());
        this.landmarksDiscovered = 0;
        this.resourcesDiscovered = 0;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Register a discovered landmark.
     *
     * @param name Landmark name/description
     * @param position Position in world
     * @param type Type of landmark
     * @param significance How important this landmark is (0.0 to 1.0)
     */
    public void discoverLandmark(String name, BlockPos position, LandmarkType type, double significance) {
        if (landmarks.size() >= MAX_LANDMARKS) {
            // Remove least significant landmark
            removeLeastSignificantLandmark();
        }

        Landmark landmark = new Landmark(name, position, type, significance);
        landmarks.put(name, landmark);
        landmarksDiscovered++;

        LOGGER.info("Discovered landmark: {} at {} ({})", name, position, type);

        // Store in memory
        memorySystem.store(new Memory(
            Memory.MemoryType.SEMANTIC,
            String.format("Landmark discovered: %s at (%d, %d, %d) - %s",
                name, position.getX(), position.getY(), position.getZ(), type),
            Math.max(0.5, significance)
        ));

        lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Register a resource location.
     *
     * @param resourceType Type of resource (e.g., "iron_ore", "oak_tree")
     * @param position Position where found
     * @param quantity Estimated quantity available
     */
    public void discoverResource(String resourceType, BlockPos position, int quantity) {
        List<ResourceLocation> locations = resources.computeIfAbsent(
            resourceType,
            k -> Collections.synchronizedList(new ArrayList<>())
        );

        // Check if we already know about this location
        for (ResourceLocation existing : locations) {
            if (existing.position.isWithinDistance(position, 5.0)) {
                // Update existing location
                existing.updateQuantity(quantity);
                existing.lastSeen = System.currentTimeMillis();
                return;
            }
        }

        // Add new resource location
        if (locations.size() >= MAX_RESOURCES_PER_TYPE) {
            // Remove oldest location
            locations.sort(Comparator.comparingLong(r -> r.lastSeen));
            locations.remove(0);
        }

        ResourceLocation resource = new ResourceLocation(resourceType, position, quantity);
        locations.add(resource);
        resourcesDiscovered++;

        LOGGER.debug("Discovered resource: {} at {} (qty: {})", resourceType, position, quantity);

        // Store significant resource finds in memory
        if (quantity > 10) {
            memorySystem.store(new Memory(
                Memory.MemoryType.SEMANTIC,
                String.format("Found %d %s at (%d, %d, %d)",
                    quantity, resourceType, position.getX(), position.getY(), position.getZ()),
                0.6
            ));
        }

        lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Register a danger zone.
     *
     * @param name Description of danger
     * @param position Center of danger zone
     * @param radius Radius of danger area
     * @param threat Threat level (0.0 to 1.0)
     */
    public void registerDangerZone(String name, BlockPos position, int radius, double threat) {
        DangerZone zone = new DangerZone(name, position, radius, threat);
        dangerZones.put(generateDangerKey(position), zone);

        LOGGER.warn("Danger zone registered: {} at {} (radius: {}, threat: {})",
            name, position, radius, threat);

        memorySystem.store(new Memory(
            Memory.MemoryType.EPISODIC,
            String.format("Danger: %s at (%d, %d, %d) - avoid this area!",
                name, position.getX(), position.getY(), position.getZ()),
            Math.max(0.7, threat)
        ));

        lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Mark a region as explored.
     *
     * @param center Center of explored region
     * @param radius Radius of exploration
     * @param biome Biome type
     */
    public void markExplored(BlockPos center, int radius, String biome) {
        ExploredRegion region = new ExploredRegion(center, radius, biome);
        exploredRegions.add(region);

        LOGGER.debug("Marked region as explored: {} (radius: {}, biome: {})",
            center, radius, biome);
    }

    /**
     * Find nearest landmark of a specific type.
     *
     * @param fromPosition Current position
     * @param type Type of landmark to find
     * @return Nearest landmark or null
     */
    public Landmark findNearestLandmark(BlockPos fromPosition, LandmarkType type) {
        return landmarks.values().stream()
            .filter(l -> l.type == type)
            .min(Comparator.comparingDouble(l -> l.position.getSquaredDistance(fromPosition)))
            .orElse(null);
    }

    /**
     * Find nearest resource location.
     *
     * @param fromPosition Current position
     * @param resourceType Type of resource
     * @return Nearest resource location or null
     */
    public ResourceLocation findNearestResource(BlockPos fromPosition, String resourceType) {
        List<ResourceLocation> locations = resources.get(resourceType);
        if (locations == null || locations.isEmpty()) {
            return null;
        }

        return locations.stream()
            .filter(r -> r.quantity > 0) // Only consider locations with resources
            .min(Comparator.comparingDouble(r -> r.position.getSquaredDistance(fromPosition)))
            .orElse(null);
    }

    /**
     * Check if a position is in a known danger zone.
     *
     * @param position Position to check
     * @return Danger zone if in one, null otherwise
     */
    public DangerZone getDangerAt(BlockPos position) {
        for (DangerZone zone : dangerZones.values()) {
            if (zone.contains(position)) {
                return zone;
            }
        }
        return null;
    }

    /**
     * Check if a region has been explored.
     *
     * @param position Position to check
     * @return True if this area has been explored
     */
    public boolean isExplored(BlockPos position) {
        for (ExploredRegion region : exploredRegions) {
            if (region.contains(position)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get all known landmarks.
     */
    public List<Landmark> getAllLandmarks() {
        return new ArrayList<>(landmarks.values());
    }

    /**
     * Get all known landmarks of a specific type.
     */
    public List<Landmark> getLandmarksByType(LandmarkType type) {
        return landmarks.values().stream()
            .filter(l -> l.type == type)
            .collect(Collectors.toList());
    }

    /**
     * Get all known resource locations for a type.
     */
    public List<ResourceLocation> getResourceLocations(String resourceType) {
        List<ResourceLocation> locations = resources.get(resourceType);
        return locations != null ? new ArrayList<>(locations) : new ArrayList<>();
    }

    /**
     * Get knowledge statistics.
     */
    public KnowledgeStats getStats() {
        int totalResources = resources.values().stream()
            .mapToInt(List::size)
            .sum();

        return new KnowledgeStats(
            landmarks.size(),
            totalResources,
            dangerZones.size(),
            exploredRegions.size(),
            landmarksDiscovered,
            resourcesDiscovered
        );
    }

    /**
     * Remove least significant landmark to make room.
     */
    private void removeLeastSignificantLandmark() {
        landmarks.values().stream()
            .min(Comparator.comparingDouble(l -> l.significance))
            .ifPresent(l -> {
                landmarks.remove(l.name);
                LOGGER.debug("Removed least significant landmark: {}", l.name);
            });
    }

    /**
     * Generate unique key for danger zone.
     */
    private String generateDangerKey(BlockPos position) {
        return String.format("%d_%d_%d", position.getX(), position.getY(), position.getZ());
    }

    /**
     * Represents a discovered landmark.
     */
    public static class Landmark {
        public final String name;
        public final BlockPos position;
        public final LandmarkType type;
        public final double significance;
        public final long discoveredAt;
        public long lastVisited;

        public Landmark(String name, BlockPos position, LandmarkType type, double significance) {
            this.name = name;
            this.position = position;
            this.type = type;
            this.significance = Math.max(0.0, Math.min(1.0, significance));
            this.discoveredAt = System.currentTimeMillis();
            this.lastVisited = this.discoveredAt;
        }

        public void visit() {
            this.lastVisited = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return String.format("Landmark{%s at (%d, %d, %d), type=%s}",
                name, position.getX(), position.getY(), position.getZ(), type);
        }
    }

    /**
     * Types of landmarks.
     */
    public enum LandmarkType {
        VILLAGE,          // NPC village
        STRUCTURE,        // Temple, mansion, etc.
        PLAYER_BASE,      // Player-built base
        MEETING_POINT,    // Designated meeting location
        SPAWN_POINT,      // World spawn or bed spawn
        PORTAL,           // Nether portal
        FARM,             // Agricultural area
        MINE,             // Mining operation
        WAYPOINT,         // General navigation point
        NATURAL_FEATURE   // Mountain, river, etc.
    }

    /**
     * Represents a known resource location.
     */
    public static class ResourceLocation {
        public final String resourceType;
        public final BlockPos position;
        public int quantity;
        public long lastSeen;
        public final long discoveredAt;

        public ResourceLocation(String resourceType, BlockPos position, int quantity) {
            this.resourceType = resourceType;
            this.position = position;
            this.quantity = quantity;
            this.discoveredAt = System.currentTimeMillis();
            this.lastSeen = this.discoveredAt;
        }

        public void updateQuantity(int newQuantity) {
            this.quantity = newQuantity;
            this.lastSeen = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return String.format("Resource{%s x%d at (%d, %d, %d)}",
                resourceType, quantity, position.getX(), position.getY(), position.getZ());
        }
    }

    /**
     * Represents a danger zone to avoid.
     */
    public static class DangerZone {
        public final String description;
        public final BlockPos center;
        public final int radius;
        public final double threatLevel;
        public final long identifiedAt;

        public DangerZone(String description, BlockPos center, int radius, double threatLevel) {
            this.description = description;
            this.center = center;
            this.radius = radius;
            this.threatLevel = Math.max(0.0, Math.min(1.0, threatLevel));
            this.identifiedAt = System.currentTimeMillis();
        }

        public boolean contains(BlockPos position) {
            return center.isWithinDistance(position, radius);
        }

        @Override
        public String toString() {
            return String.format("DangerZone{%s at (%d, %d, %d), radius=%d, threat=%.2f}",
                description, center.getX(), center.getY(), center.getZ(), radius, threatLevel);
        }
    }

    /**
     * Represents an explored region.
     */
    public static class ExploredRegion {
        public final BlockPos center;
        public final int radius;
        public final String biome;
        public final long exploredAt;

        public ExploredRegion(BlockPos center, int radius, String biome) {
            this.center = center;
            this.radius = radius;
            this.biome = biome;
            this.exploredAt = System.currentTimeMillis();
        }

        public boolean contains(BlockPos position) {
            return center.isWithinDistance(position, radius);
        }

        @Override
        public String toString() {
            return String.format("ExploredRegion{(%d, %d, %d), radius=%d, biome=%s}",
                center.getX(), center.getY(), center.getZ(), radius, biome);
        }
    }

    /**
     * Knowledge statistics.
     */
    public static class KnowledgeStats {
        public final int knownLandmarks;
        public final int knownResources;
        public final int knownDangers;
        public final int exploredRegions;
        public final int totalLandmarksDiscovered;
        public final int totalResourcesDiscovered;

        public KnowledgeStats(int knownLandmarks, int knownResources, int knownDangers,
                            int exploredRegions, int totalLandmarksDiscovered,
                            int totalResourcesDiscovered) {
            this.knownLandmarks = knownLandmarks;
            this.knownResources = knownResources;
            this.knownDangers = knownDangers;
            this.exploredRegions = exploredRegions;
            this.totalLandmarksDiscovered = totalLandmarksDiscovered;
            this.totalResourcesDiscovered = totalResourcesDiscovered;
        }

        @Override
        public String toString() {
            return String.format("Knowledge{landmarks=%d, resources=%d, dangers=%d, explored=%d}",
                knownLandmarks, knownResources, knownDangers, exploredRegions);
        }
    }
}
