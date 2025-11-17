package com.aiplayer.communication;

import com.aiplayer.planning.Goal;

/**
 * Represents a parsed task request from a player.
 *
 * Extracts actionable components from natural language:
 * "gather 64 oak logs" → TaskRequest(GATHER, 64, "oak_log")
 */
public class TaskRequest {

    /**
     * Types of actions that can be requested.
     */
    public enum ActionType {
        GATHER,     // Collect items (mining, picking up)
        MINE,       // Specifically breaking blocks
        BUILD,      // Place blocks, construct
        COMBAT,     // Fight mobs or entities
        CRAFT,      // Create items
        EXPLORE,    // Find locations or structures
        FOLLOW,     // Follow a player
        GUARD,      // Protect an area or player
        UNKNOWN     // Couldn't determine action
    }

    private final ActionType actionType;
    private final int quantity;
    private final String targetItem;
    private final String targetEntity;
    private final String location;

    public TaskRequest(ActionType actionType, int quantity, String targetItem) {
        this(actionType, quantity, targetItem, null, null);
    }

    public TaskRequest(ActionType actionType, int quantity, String targetItem, String targetEntity, String location) {
        this.actionType = actionType;
        this.quantity = quantity;
        this.targetItem = targetItem;
        this.targetEntity = targetEntity;
        this.location = location;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getTargetItem() {
        return targetItem;
    }

    public String getTargetEntity() {
        return targetEntity;
    }

    public String getLocation() {
        return location;
    }

    /**
     * Get human-readable description of task.
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(actionType.toString().toLowerCase());

        if (quantity > 1) {
            desc.append(" ").append(quantity);
        }

        if (targetItem != null) {
            desc.append(" ").append(targetItem.replace("_", " "));
        }

        if (targetEntity != null) {
            desc.append(" ").append(targetEntity);
        }

        if (location != null) {
            desc.append(" at ").append(location);
        }

        return desc.toString();
    }

    /**
     * Convert task request to a Goal object.
     */
    public Goal toGoal() {
        Goal.GoalType goalType = mapActionToGoalType(actionType);
        Goal goal = new Goal(goalType, getDescription());

        // Add parameters for goal execution
        goal.setParameter("action_type", actionType.toString());
        goal.setParameter("quantity", quantity);

        if (targetItem != null) {
            goal.setParameter("target_item", targetItem);
        }

        if (targetEntity != null) {
            goal.setParameter("target_entity", targetEntity);
        }

        if (location != null) {
            goal.setParameter("location", location);
        }

        return goal;
    }

    /**
     * Map action type to goal type.
     */
    private Goal.GoalType mapActionToGoalType(ActionType actionType) {
        switch (actionType) {
            case GATHER:
            case MINE:
                return Goal.GoalType.RESOURCE_GATHERING;
            case BUILD:
                return Goal.GoalType.BUILD;
            case COMBAT:
            case GUARD:
                return Goal.GoalType.COMBAT;
            case CRAFT:
                return Goal.GoalType.CRAFTING;
            case EXPLORE:
                return Goal.GoalType.EXPLORATION;
            case FOLLOW:
                return Goal.GoalType.SOCIAL;
            default:
                return Goal.GoalType.EXPLORATION;
        }
    }

    /**
     * Determine if this task can be accepted.
     * This is a simple check - more sophisticated validation happens in the brain.
     */
    public boolean isFeasible() {
        // Basic validation
        if (actionType == ActionType.UNKNOWN) {
            return false;
        }

        if (quantity < 0 || quantity > 1000) {
            return false;
        }

        return true;
    }

    /**
     * Get reason why task cannot be accepted.
     */
    public String getRejectionReason() {
        if (actionType == ActionType.UNKNOWN) {
            return "I don't understand that task.";
        }

        if (quantity < 0 || quantity > 1000) {
            return "That quantity seems unreasonable.";
        }

        return "I don't have the necessary resources or skills.";
    }

    @Override
    public String toString() {
        return "TaskRequest{" +
                "action=" + actionType +
                ", quantity=" + quantity +
                ", item=" + targetItem +
                ", entity=" + targetEntity +
                ", location=" + location +
                '}';
    }
}
