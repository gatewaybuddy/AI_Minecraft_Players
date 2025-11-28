package com.aiplayer.action;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A* Pathfinding Engine for AI players.
 *
 * Features:
 * - A* algorithm for optimal paths
 * - Handles walking, jumping, falling
 * - Avoids obstacles and dangerous blocks
 * - Configurable iteration limit for performance
 *
 * Based on Baritone's pathfinding architecture.
 */
public class PathfindingEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathfindingEngine.class);

    // Configuration
    private final int maxIterations;
    private final double jumpCost;
    private final double diagonalCost;
    private final double fallCost;

    public PathfindingEngine() {
        this(10000, 1.5, 1.414, 0.5);
    }

    public PathfindingEngine(int maxIterations, double jumpCost, double diagonalCost, double fallCost) {
        this.maxIterations = maxIterations;
        this.jumpCost = jumpCost;
        this.diagonalCost = diagonalCost;
        this.fallCost = fallCost;
    }

    /**
     * Find a path from start to goal.
     *
     * @param start Starting position
     * @param goal Goal position
     * @param world World to path in
     * @return Optional path if found, empty if no path exists
     */
    public Optional<Path> findPath(BlockPos start, BlockPos goal, World world) {
        // Quick check: same position
        if (start.equals(goal)) {
            return Optional.of(new Path(Collections.singletonList(start)));
        }

        // Initialize A* data structures
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Set<BlockPos> closedSet = new HashSet<>();
        Map<BlockPos, Node> nodes = new HashMap<>();

        // Create start node
        Node startNode = new Node(start, 0, heuristic(start, goal));
        openSet.add(startNode);
        nodes.put(start, startNode);

        int iterations = 0;

        // A* main loop
        while (!openSet.isEmpty() && iterations++ < maxIterations) {
            Node current = openSet.poll();

            // Goal reached!
            if (current.pos.equals(goal)) {
                return Optional.of(reconstructPath(current));
            }

            closedSet.add(current.pos);

            // Explore neighbors
            for (BlockPos neighbor : getNeighbors(current.pos, world)) {
                if (closedSet.contains(neighbor)) {
                    continue;
                }

                double moveCost = calculateMoveCost(current.pos, neighbor, world);
                double tentativeG = current.gScore + moveCost;

                Node neighborNode = nodes.computeIfAbsent(neighbor,
                    pos -> new Node(pos, Double.POSITIVE_INFINITY, heuristic(pos, goal)));

                // Found a better path to this neighbor
                if (tentativeG < neighborNode.gScore) {
                    neighborNode.parent = current;
                    neighborNode.gScore = tentativeG;
                    neighborNode.fScore = tentativeG + neighborNode.hScore;

                    if (!openSet.contains(neighborNode)) {
                        openSet.add(neighborNode);
                    }
                }
            }
        }

        // No path found
        if (iterations >= maxIterations) {
            LOGGER.debug("Pathfinding reached max iterations ({}) from {} to {}", maxIterations, start, goal);
        }

        return Optional.empty();
    }

    /**
     * Get walkable neighbors of a position.
     */
    private List<BlockPos> getNeighbors(BlockPos pos, World world) {
        List<BlockPos> neighbors = new ArrayList<>();

        // 8 horizontal directions
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                // Same level
                BlockPos neighbor = pos.add(dx, 0, dz);
                if (isWalkable(neighbor, world)) {
                    neighbors.add(neighbor);
                }

                // Jump up one block
                BlockPos jumpUp = pos.add(dx, 1, dz);
                if (isWalkable(jumpUp, world) && canJumpTo(pos, jumpUp, world)) {
                    neighbors.add(jumpUp);
                }
            }
        }

        // Fall down
        for (int dy = -1; dy >= -3; dy--) {
            BlockPos below = pos.down(-dy);
            if (isWalkable(below, world)) {
                neighbors.add(below);
                break; // Only consider first walkable position below
            }
            // Stop if we hit a solid block
            if (!world.getBlockState(below).isAir()) {
                break;
            }
        }

        return neighbors;
    }

    /**
     * Check if a position is walkable (solid block with air above).
     */
    private boolean isWalkable(BlockPos pos, World world) {
        if (!world.isChunkLoaded(pos)) {
            return false;
        }

        BlockState floor = world.getBlockState(pos);
        BlockState above1 = world.getBlockState(pos.up());
        BlockState above2 = world.getBlockState(pos.up(2));

        // Need solid floor and 2 air blocks above
        return !floor.isAir() &&
               floor.getBlock().getDefaultState().isSolidBlock(world, pos) &&
               above1.isAir() &&
               above2.isAir() &&
               !isDangerous(floor);
    }

    /**
     * Check if can jump from one position to another.
     */
    private boolean canJumpTo(BlockPos from, BlockPos to, World world) {
        // Can only jump up 1 block
        if (to.getY() - from.getY() != 1) {
            return false;
        }

        // Need headroom
        return world.getBlockState(from.up(2)).isAir() &&
               world.getBlockState(to.up()).isAir();
    }

    /**
     * Check if a block is dangerous (lava, fire, etc.).
     */
    private boolean isDangerous(BlockState state) {
        String blockId = state.getBlock().getTranslationKey();
        return blockId.contains("lava") ||
               blockId.contains("fire") ||
               blockId.contains("magma") ||
               blockId.contains("cactus");
    }

    /**
     * Calculate movement cost between adjacent positions.
     */
    private double calculateMoveCost(BlockPos from, BlockPos to, World world) {
        int dx = Math.abs(to.getX() - from.getX());
        int dy = to.getY() - from.getY();
        int dz = Math.abs(to.getZ() - from.getZ());

        // Diagonal movement
        if (dx > 0 && dz > 0) {
            if (dy > 0) return diagonalCost + jumpCost; // Diagonal jump
            if (dy < 0) return diagonalCost * fallCost; // Diagonal fall
            return diagonalCost;
        }

        // Straight movement
        if (dy > 0) return 1.0 + jumpCost; // Jump up
        if (dy < 0) return 1.0 * fallCost; // Fall down
        return 1.0; // Straight walk
    }

    /**
     * Heuristic function for A* (Euclidean distance with vertical penalty).
     */
    private double heuristic(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();

        // Vertical movement costs more
        return Math.sqrt(dx * dx + dy * dy * 4 + dz * dz);
    }

    /**
     * Reconstruct path by following parent pointers.
     */
    private Path reconstructPath(Node goalNode) {
        List<BlockPos> positions = new ArrayList<>();
        Node current = goalNode;

        while (current != null) {
            positions.add(current.pos);
            current = current.parent;
        }

        Collections.reverse(positions);
        return new Path(positions);
    }

    /**
     * A* node.
     */
    private static class Node {
        BlockPos pos;
        double gScore; // Cost from start
        double hScore; // Heuristic to goal
        double fScore; // Total score
        Node parent;

        Node(BlockPos pos, double gScore, double hScore) {
            this.pos = pos;
            this.gScore = gScore;
            this.hScore = hScore;
            this.fScore = gScore + hScore;
        }
    }

    /**
     * Path result.
     */
    public static class Path {
        private final List<BlockPos> positions;

        public Path(List<BlockPos> positions) {
            this.positions = Collections.unmodifiableList(positions);
        }

        public List<BlockPos> getPositions() {
            return positions;
        }

        public BlockPos getEnd() {
            return positions.get(positions.size() - 1);
        }

        public int getLength() {
            return positions.size();
        }

        public boolean isEmpty() {
            return positions.isEmpty();
        }

        @Override
        public String toString() {
            return "Path{length=" + positions.size() + ", end=" + getEnd() + "}";
        }
    }
}
