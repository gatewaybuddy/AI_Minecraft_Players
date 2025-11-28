package com.aiplayer.action;

import com.aiplayer.core.AIPlayerEntity;
import com.aiplayer.perception.WorldState;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Action interface - represents an executable action that an AI player can perform.
 *
 * Actions are the building blocks of AI behavior:
 * - MoveToAction - Navigate to a position
 * - MineBlockAction - Break a block
 * - PlaceBlockAction - Place a block
 * - CraftItemAction - Craft an item
 * - AttackEntityAction - Attack an entity
 * - etc.
 *
 * Actions can be chained together to form complex behaviors.
 */
public interface Action {

    /**
     * Get the name/description of this action.
     */
    String getName();

    /**
     * Check if this action can currently be executed.
     *
     * @param state Current world state
     * @param player AI player that would execute this action
     * @return true if the action is possible right now
     */
    boolean canExecute(WorldState state, AIPlayerEntity player);

    /**
     * Execute this action asynchronously.
     *
     * @param player AI player executing the action
     * @return Future that completes with the result
     */
    CompletableFuture<ActionResult> execute(AIPlayerEntity player);

    /**
     * Get estimated duration in ticks (20 ticks = 1 second).
     */
    double getEstimatedDuration();

    /**
     * Get action priority (higher = more important).
     * Used for action selection when multiple actions are possible.
     */
    int getPriority();

    /**
     * Get prerequisites for this action.
     * E.g., "iron_pickaxe" for mining obsidian.
     */
    List<String> getPrerequisites();

    /**
     * Cancel this action if it's currently executing.
     */
    default void cancel() {
        // Default: no-op, override if cancellation is supported
    }

    /**
     * Check if this action is currently executing.
     */
    default boolean isExecuting() {
        return false;
    }
}
