package com.aiplayer.action;

import com.aiplayer.perception.WorldState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Result of executing an action.
 *
 * Contains:
 * - Success/failure status
 * - Result message
 * - Updated world state (if available)
 * - Error information (if failed)
 */
public class ActionResult {

    private final boolean success;
    private final String message;
    private final WorldState resultingState;
    private final Throwable error;

    private ActionResult(boolean success, String message, WorldState resultingState, Throwable error) {
        this.success = success;
        this.message = message;
        this.resultingState = resultingState;
        this.error = error;
    }

    /**
     * Create a successful result.
     */
    public static ActionResult success(String message, @Nullable WorldState newState) {
        return new ActionResult(true, message, newState, null);
    }

    /**
     * Create a successful result without updated state.
     */
    public static ActionResult success(String message) {
        return new ActionResult(true, message, null, null);
    }

    /**
     * Create a failure result.
     */
    public static ActionResult failure(String reason, @Nullable Throwable error) {
        return new ActionResult(false, reason, null, error);
    }

    /**
     * Create a failure result without error.
     */
    public static ActionResult failure(String reason) {
        return new ActionResult(false, reason, null, null);
    }

    // Getters

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public String getMessage() {
        return message;
    }

    public Optional<WorldState> getResultingState() {
        return Optional.ofNullable(resultingState);
    }

    public Optional<Throwable> getError() {
        return Optional.ofNullable(error);
    }

    @Override
    public String toString() {
        if (success) {
            return "ActionResult{SUCCESS: " + message + "}";
        } else {
            return "ActionResult{FAILURE: " + message +
                   (error != null ? " (" + error.getMessage() + ")" : "") + "}";
        }
    }
}
