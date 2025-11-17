package com.aiplayer.planning;

import com.aiplayer.action.Action;
import com.aiplayer.perception.WorldState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Task - A concrete sequence of actions to accomplish part of a goal.
 *
 * Tasks are the executable units in the goal hierarchy:
 * Goal → Subgoals → Tasks → Actions
 *
 * Example task: "Mine 10 oak logs"
 * Actions:
 * 1. Find oak tree
 * 2. Navigate to tree
 * 3. Mine log (repeat 10x)
 */
public class Task {

    private final UUID id;
    private final String description;
    private final List<Action> actions;
    private final Predicate<WorldState> successCondition;

    private TaskStatus status;
    private int currentActionIndex;
    private String failureReason;

    public Task(String description, List<Action> actions, Predicate<WorldState> successCondition) {
        this.id = UUID.randomUUID();
        this.description = description;
        this.actions = new ArrayList<>(actions);
        this.successCondition = successCondition;
        this.status = TaskStatus.PENDING;
        this.currentActionIndex = 0;
    }

    /**
     * Task status lifecycle.
     */
    public enum TaskStatus {
        PENDING,       // Not started
        IN_PROGRESS,   // Currently executing
        COMPLETED,     // Successfully finished
        FAILED         // Could not complete
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public List<Action> getActions() {
        return new ArrayList<>(actions);
    }

    public TaskStatus getStatus() {
        return status;
    }

    public int getCurrentActionIndex() {
        return currentActionIndex;
    }

    public String getFailureReason() {
        return failureReason;
    }

    // Status management

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public void setFailureReason(String reason) {
        this.failureReason = reason;
    }

    /**
     * Get current action being executed.
     */
    public Action getCurrentAction() {
        if (currentActionIndex >= 0 && currentActionIndex < actions.size()) {
            return actions.get(currentActionIndex);
        }
        return null;
    }

    /**
     * Move to next action in sequence.
     */
    public void advanceToNextAction() {
        currentActionIndex++;
    }

    /**
     * Check if task is complete based on world state.
     */
    public boolean isComplete(WorldState state) {
        // If we have a success condition, check it
        if (successCondition != null) {
            return successCondition.test(state);
        }

        // Otherwise, complete when all actions are done
        return currentActionIndex >= actions.size();
    }

    /**
     * Check if there are more actions to execute.
     */
    public boolean hasMoreActions() {
        return currentActionIndex < actions.size();
    }

    /**
     * Get progress percentage (0-100).
     */
    public int getProgress() {
        if (actions.isEmpty()) {
            return 100;
        }
        return (currentActionIndex * 100) / actions.size();
    }

    /**
     * Check if task is active.
     */
    public boolean isActive() {
        return status == TaskStatus.PENDING || status == TaskStatus.IN_PROGRESS;
    }

    @Override
    public String toString() {
        return String.format("Task{%s, status=%s, progress=%d%%}",
            description, status, getProgress());
    }
}
