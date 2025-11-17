package com.aiplayer.planning;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Goal - Represents a high-level objective for an AI player.
 *
 * Goals are hierarchical and can be decomposed into subgoals and tasks.
 * Examples:
 * - "Build a house"
 * - "Gather 64 oak logs"
 * - "Reach diamond level"
 * - "Survive the night"
 *
 * Goals drive the AI's behavior and are used by the planning engine
 * to generate action sequences.
 */
public class Goal {

    private final UUID id;
    private final String description;
    private final GoalType type;
    private final int priority;
    private final long createdAt;
    private final String requestedBy; // Player name who requested (null if autonomous)

    private GoalStatus status;
    private List<Goal> subgoals;
    private String failureReason;
    private long completedAt;

    public Goal(String description, GoalType type, int priority, @Nullable String requestedBy) {
        this.id = UUID.randomUUID();
        this.description = description;
        this.type = type;
        this.priority = priority;
        this.createdAt = System.currentTimeMillis();
        this.requestedBy = requestedBy;
        this.status = GoalStatus.PENDING;
        this.subgoals = new ArrayList<>();
    }

    /**
     * Goal types categorize the nature of goals.
     */
    public enum GoalType {
        SURVIVAL,       // Stay alive, get food, avoid danger
        EXPLORATION,    // Explore the world, discover new areas
        RESOURCE,       // Gather specific resources (wood, stone, etc.)
        BUILDING,       // Build structures
        SOCIAL,         // Interact with players
        COMBAT,         // Fight mobs/players
        CRAFTING,       // Craft items
        PLAYER_REQUEST, // Fulfill player request
        AUTONOMOUS      // Self-generated goal
    }

    /**
     * Goal status lifecycle.
     */
    public enum GoalStatus {
        PENDING,       // Not started yet
        IN_PROGRESS,   // Currently working on it
        COMPLETED,     // Successfully finished
        FAILED,        // Could not complete
        ABANDONED      // Gave up or superseded
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public GoalType getType() {
        return type;
    }

    public int getPriority() {
        return priority;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Optional<String> getRequestedBy() {
        return Optional.ofNullable(requestedBy);
    }

    public GoalStatus getStatus() {
        return status;
    }

    public List<Goal> getSubgoals() {
        return new ArrayList<>(subgoals);
    }

    public Optional<String> getFailureReason() {
        return Optional.ofNullable(failureReason);
    }

    public long getCompletedAt() {
        return completedAt;
    }

    // Setters

    public void setStatus(GoalStatus status) {
        this.status = status;
        if (status == GoalStatus.COMPLETED || status == GoalStatus.FAILED || status == GoalStatus.ABANDONED) {
            this.completedAt = System.currentTimeMillis();
        }
    }

    public void setFailureReason(String reason) {
        this.failureReason = reason;
    }

    public void addSubgoal(Goal subgoal) {
        this.subgoals.add(subgoal);
    }

    // Helper methods

    /**
     * Check if this goal is active (pending or in progress).
     */
    public boolean isActive() {
        return status == GoalStatus.PENDING || status == GoalStatus.IN_PROGRESS;
    }

    /**
     * Check if this goal is complete.
     */
    public boolean isComplete() {
        return status == GoalStatus.COMPLETED;
    }

    /**
     * Check if this goal has failed.
     */
    public boolean hasFailed() {
        return status == GoalStatus.FAILED;
    }

    /**
     * Check if this is a player-requested goal.
     */
    public boolean isPlayerRequested() {
        return requestedBy != null;
    }

    /**
     * Get age of goal in milliseconds.
     */
    public long getAge() {
        return System.currentTimeMillis() - createdAt;
    }

    /**
     * Get duration if completed (milliseconds).
     */
    public long getDuration() {
        if (completedAt > 0) {
            return completedAt - createdAt;
        }
        return 0;
    }

    /**
     * Check if all subgoals are complete.
     */
    public boolean areAllSubgoalsComplete() {
        if (subgoals.isEmpty()) {
            return true;
        }
        return subgoals.stream().allMatch(Goal::isComplete);
    }

    /**
     * Get next pending subgoal.
     */
    public Optional<Goal> getNextSubgoal() {
        return subgoals.stream()
            .filter(g -> g.getStatus() == GoalStatus.PENDING)
            .findFirst();
    }

    @Override
    public String toString() {
        return String.format("Goal{%s, type=%s, status=%s, priority=%d}",
            description, type, status, priority);
    }

    /**
     * Get detailed status string.
     */
    public String getDetailedStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Goal: %s [%s]\n", description, status));
        sb.append(String.format("  Type: %s, Priority: %d\n", type, priority));
        sb.append(String.format("  Age: %.1fs\n", getAge() / 1000.0));

        if (requestedBy != null) {
            sb.append(String.format("  Requested by: %s\n", requestedBy));
        }

        if (!subgoals.isEmpty()) {
            sb.append(String.format("  Subgoals: %d total, %d pending, %d complete\n",
                subgoals.size(),
                subgoals.stream().filter(g -> g.getStatus() == GoalStatus.PENDING).count(),
                subgoals.stream().filter(Goal::isComplete).count()));
        }

        if (failureReason != null) {
            sb.append(String.format("  Failure: %s\n", failureReason));
        }

        return sb.toString();
    }
}
