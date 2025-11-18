package com.aiplayer.coordination;

import com.aiplayer.core.AIPlayerEntity;
import com.aiplayer.planning.Goal;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Coordination System - Enables multiple AI players to work together.
 *
 * Phase 5: Advanced AI
 *
 * Manages collaborative activities between AI players:
 * - Shared goal distribution
 * - Task allocation and role assignment
 * - Resource sharing and trading
 * - Team formation for complex tasks
 * - Conflict resolution (e.g., two AIs wanting same resource)
 * - Communication between AI players
 *
 * Allows AI players to act as a coordinated team rather than independent agents.
 */
public class CoordinationSystem {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoordinationSystem.class);

    // Shared state across all AI players
    private final Map<UUID, AIPlayerEntity> registeredPlayers;
    private final Map<String, SharedGoal> sharedGoals;
    private final Map<UUID, Set<String>> playerGoalAssignments;
    private final Map<String, Team> teams;

    // Coordination parameters
    private static final int MAX_TEAM_SIZE = 5;
    private static final long GOAL_TIMEOUT_MS = 600000; // 10 minutes
    private static final double COLLABORATION_DISTANCE = 100.0; // blocks

    public CoordinationSystem() {
        this.registeredPlayers = new ConcurrentHashMap<>();
        this.sharedGoals = new ConcurrentHashMap<>();
        this.playerGoalAssignments = new ConcurrentHashMap<>();
        this.teams = new ConcurrentHashMap<>();
    }

    /**
     * Register an AI player with the coordination system.
     *
     * @param player AI player to register
     */
    public void registerPlayer(AIPlayerEntity player) {
        UUID playerId = player.getUuid();
        registeredPlayers.put(playerId, player);
        playerGoalAssignments.put(playerId, Collections.newSetFromMap(new ConcurrentHashMap<>()));

        LOGGER.info("Registered AI player: {} (total: {})",
            player.getName().getString(), registeredPlayers.size());
    }

    /**
     * Unregister an AI player.
     *
     * @param playerId Player UUID
     */
    public void unregisterPlayer(UUID playerId) {
        AIPlayerEntity removed = registeredPlayers.remove(playerId);
        if (removed != null) {
            // Clean up player's goal assignments
            Set<String> playerGoals = playerGoalAssignments.remove(playerId);
            if (playerGoals != null) {
                for (String goalId : playerGoals) {
                    SharedGoal goal = sharedGoals.get(goalId);
                    if (goal != null) {
                        goal.removeParticipant(playerId);
                    }
                }
            }

            LOGGER.info("Unregistered AI player: {}", removed.getName().getString());
        }
    }

    /**
     * Create a shared goal that multiple AI players can work on together.
     *
     * @param description Goal description
     * @param priority Priority (0.0 to 1.0)
     * @param requiredParticipants Minimum number of participants needed
     * @return Shared goal ID
     */
    public String createSharedGoal(String description, double priority, int requiredParticipants) {
        String goalId = UUID.randomUUID().toString();
        SharedGoal goal = new SharedGoal(goalId, description, priority, requiredParticipants);
        sharedGoals.put(goalId, goal);

        LOGGER.info("Created shared goal: {} (requires {} participants)", description, requiredParticipants);

        return goalId;
    }

    /**
     * Assign a player to a shared goal.
     *
     * @param playerId Player UUID
     * @param goalId Shared goal ID
     * @return True if assignment succeeded
     */
    public boolean assignToGoal(UUID playerId, String goalId) {
        SharedGoal goal = sharedGoals.get(goalId);
        if (goal == null) {
            LOGGER.warn("Cannot assign to non-existent goal: {}", goalId);
            return false;
        }

        if (!registeredPlayers.containsKey(playerId)) {
            LOGGER.warn("Cannot assign unregistered player to goal");
            return false;
        }

        // Add player to goal
        boolean added = goal.addParticipant(playerId);
        if (added) {
            playerGoalAssignments.get(playerId).add(goalId);
            LOGGER.debug("Assigned player {} to goal: {}", playerId, goal.description);

            // Check if goal is now ready (has enough participants)
            if (goal.isReady()) {
                LOGGER.info("Shared goal ready: {} ({} participants)",
                    goal.description, goal.participants.size());
            }
        }

        return added;
    }

    /**
     * Remove a player from a shared goal.
     *
     * @param playerId Player UUID
     * @param goalId Shared goal ID
     */
    public void unassignFromGoal(UUID playerId, String goalId) {
        SharedGoal goal = sharedGoals.get(goalId);
        if (goal != null) {
            goal.removeParticipant(playerId);
        }

        Set<String> playerGoals = playerGoalAssignments.get(playerId);
        if (playerGoals != null) {
            playerGoals.remove(goalId);
        }
    }

    /**
     * Complete a shared goal.
     *
     * @param goalId Goal ID
     * @param success Whether goal was completed successfully
     */
    public void completeGoal(String goalId, boolean success) {
        SharedGoal goal = sharedGoals.remove(goalId);
        if (goal == null) {
            return;
        }

        // Clean up participant assignments
        for (UUID participantId : goal.participants) {
            Set<String> playerGoals = playerGoalAssignments.get(participantId);
            if (playerGoals != null) {
                playerGoals.remove(goalId);
            }
        }

        LOGGER.info("Completed shared goal: {} (success: {}, participants: {})",
            goal.description, success, goal.participants.size());
    }

    /**
     * Find suitable AI players for a collaborative task.
     *
     * @param nearPosition Position to search near
     * @param maxDistance Maximum distance in blocks
     * @param maxPlayers Maximum number of players to find
     * @return List of suitable AI players
     */
    public List<AIPlayerEntity> findCollaborators(BlockPos nearPosition, double maxDistance, int maxPlayers) {
        return registeredPlayers.values().stream()
            .filter(player -> {
                BlockPos playerPos = player.getBlockPos();
                return playerPos.isWithinDistance(nearPosition, maxDistance);
            })
            .filter(player -> {
                // Check if player is available (not too busy with other goals)
                UUID playerId = player.getUuid();
                Set<String> goals = playerGoalAssignments.get(playerId);
                return goals == null || goals.size() < 3; // Max 3 concurrent shared goals
            })
            .limit(maxPlayers)
            .collect(Collectors.toList());
    }

    /**
     * Create a team for complex collaborative tasks.
     *
     * @param teamName Team name
     * @param leader Team leader
     * @param purpose Team purpose
     * @return Team ID
     */
    public String createTeam(String teamName, UUID leader, String purpose) {
        String teamId = UUID.randomUUID().toString();
        Team team = new Team(teamId, teamName, leader, purpose);
        teams.put(teamId, team);

        // Add leader to team
        team.addMember(leader);

        LOGGER.info("Created team: {} (leader: {})", teamName, leader);

        return teamId;
    }

    /**
     * Add a member to a team.
     *
     * @param teamId Team ID
     * @param playerId Player UUID
     * @return True if added successfully
     */
    public boolean addToTeam(String teamId, UUID playerId) {
        Team team = teams.get(teamId);
        if (team == null) {
            return false;
        }

        if (team.members.size() >= MAX_TEAM_SIZE) {
            LOGGER.warn("Team {} is full", team.name);
            return false;
        }

        boolean added = team.addMember(playerId);
        if (added) {
            LOGGER.debug("Added player {} to team {}", playerId, team.name);
        }

        return added;
    }

    /**
     * Distribute a task among team members.
     *
     * @param teamId Team ID
     * @param task Task to distribute
     * @return Map of player ID to assigned sub-task
     */
    public Map<UUID, String> distributeTask(String teamId, String task) {
        Team team = teams.get(teamId);
        if (team == null) {
            return Collections.emptyMap();
        }

        Map<UUID, String> assignments = new HashMap<>();

        // Simple round-robin distribution
        // In a more advanced version, this would consider player skills and current load
        List<UUID> members = new ArrayList<>(team.members);
        for (int i = 0; i < members.size(); i++) {
            UUID memberId = members.get(i);
            String subTask = String.format("%s (part %d/%d)", task, i + 1, members.size());
            assignments.put(memberId, subTask);
        }

        LOGGER.info("Distributed task '{}' to {} team members", task, members.size());

        return assignments;
    }

    /**
     * Request help from nearby AI players.
     *
     * @param requestingPlayer Player requesting help
     * @param helpType Type of help needed
     * @param position Position where help is needed
     * @return List of AI players willing to help
     */
    public List<AIPlayerEntity> requestHelp(AIPlayerEntity requestingPlayer, String helpType, BlockPos position) {
        UUID requesterId = requestingPlayer.getUuid();

        List<AIPlayerEntity> helpers = registeredPlayers.values().stream()
            .filter(player -> !player.getUuid().equals(requesterId)) // Not self
            .filter(player -> player.getBlockPos().isWithinDistance(position, COLLABORATION_DISTANCE))
            .filter(player -> {
                // Check if player is available
                Set<String> goals = playerGoalAssignments.get(player.getUuid());
                return goals == null || goals.size() < 2;
            })
            .limit(3) // Max 3 helpers
            .collect(Collectors.toList());

        LOGGER.info("Found {} AI players to help with: {}", helpers.size(), helpType);

        return helpers;
    }

    /**
     * Check if two AI players should coordinate on a task.
     *
     * @param player1 First player
     * @param player2 Second player
     * @return True if they should coordinate
     */
    public boolean shouldCoordinate(AIPlayerEntity player1, AIPlayerEntity player2) {
        // Check if they're working on same or related goals
        Set<String> goals1 = playerGoalAssignments.get(player1.getUuid());
        Set<String> goals2 = playerGoalAssignments.get(player2.getUuid());

        if (goals1 == null || goals2 == null) {
            return false;
        }

        // Check for shared goals
        Set<String> sharedGoalIds = new HashSet<>(goals1);
        sharedGoalIds.retainAll(goals2);

        if (!sharedGoalIds.isEmpty()) {
            return true;
        }

        // Check if they're on the same team
        for (Team team : teams.values()) {
            if (team.members.contains(player1.getUuid()) &&
                team.members.contains(player2.getUuid())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get all shared goals.
     */
    public List<SharedGoal> getAllSharedGoals() {
        return new ArrayList<>(sharedGoals.values());
    }

    /**
     * Get shared goals that need more participants.
     */
    public List<SharedGoal> getGoalsNeedingHelp() {
        return sharedGoals.values().stream()
            .filter(goal -> !goal.isReady())
            .sorted(Comparator.comparingDouble(g -> -g.priority)) // Highest priority first
            .collect(Collectors.toList());
    }

    /**
     * Get all teams.
     */
    public List<Team> getAllTeams() {
        return new ArrayList<>(teams.values());
    }

    /**
     * Clean up expired goals.
     */
    public void cleanupExpiredGoals() {
        long currentTime = System.currentTimeMillis();
        List<String> expired = new ArrayList<>();

        for (SharedGoal goal : sharedGoals.values()) {
            if (currentTime - goal.createdAt > GOAL_TIMEOUT_MS) {
                expired.add(goal.id);
            }
        }

        for (String goalId : expired) {
            completeGoal(goalId, false);
            LOGGER.debug("Removed expired shared goal: {}", goalId);
        }
    }

    /**
     * Get coordination statistics.
     */
    public CoordinationStats getStats() {
        int activeGoals = sharedGoals.size();
        int readyGoals = (int) sharedGoals.values().stream().filter(SharedGoal::isReady).count();
        int activeTeams = teams.size();
        int totalPlayers = registeredPlayers.size();

        return new CoordinationStats(activeGoals, readyGoals, activeTeams, totalPlayers);
    }

    /**
     * Represents a goal shared by multiple AI players.
     */
    public static class SharedGoal {
        public final String id;
        public final String description;
        public final double priority;
        public final int requiredParticipants;
        public final Set<UUID> participants;
        public final long createdAt;

        public SharedGoal(String id, String description, double priority, int requiredParticipants) {
            this.id = id;
            this.description = description;
            this.priority = Math.max(0.0, Math.min(1.0, priority));
            this.requiredParticipants = Math.max(1, requiredParticipants);
            this.participants = Collections.newSetFromMap(new ConcurrentHashMap<>());
            this.createdAt = System.currentTimeMillis();
        }

        public boolean addParticipant(UUID playerId) {
            return participants.add(playerId);
        }

        public void removeParticipant(UUID playerId) {
            participants.remove(playerId);
        }

        public boolean isReady() {
            return participants.size() >= requiredParticipants;
        }

        @Override
        public String toString() {
            return String.format("SharedGoal{%s, participants=%d/%d, priority=%.2f}",
                description, participants.size(), requiredParticipants, priority);
        }
    }

    /**
     * Represents a team of AI players.
     */
    public static class Team {
        public final String id;
        public final String name;
        public final UUID leader;
        public final String purpose;
        public final Set<UUID> members;
        public final long createdAt;

        public Team(String id, String name, UUID leader, String purpose) {
            this.id = id;
            this.name = name;
            this.leader = leader;
            this.purpose = purpose;
            this.members = Collections.newSetFromMap(new ConcurrentHashMap<>());
            this.createdAt = System.currentTimeMillis();
        }

        public boolean addMember(UUID playerId) {
            return members.add(playerId);
        }

        public void removeMember(UUID playerId) {
            members.remove(playerId);
        }

        @Override
        public String toString() {
            return String.format("Team{%s, members=%d, purpose=%s}", name, members.size(), purpose);
        }
    }

    /**
     * Coordination statistics.
     */
    public static class CoordinationStats {
        public final int activeSharedGoals;
        public final int readyGoals;
        public final int activeTeams;
        public final int registeredPlayers;

        public CoordinationStats(int activeSharedGoals, int readyGoals,
                               int activeTeams, int registeredPlayers) {
            this.activeSharedGoals = activeSharedGoals;
            this.readyGoals = readyGoals;
            this.activeTeams = activeTeams;
            this.registeredPlayers = registeredPlayers;
        }

        @Override
        public String toString() {
            return String.format("Coordination{goals=%d (%d ready), teams=%d, players=%d}",
                activeSharedGoals, readyGoals, activeTeams, registeredPlayers);
        }
    }
}
