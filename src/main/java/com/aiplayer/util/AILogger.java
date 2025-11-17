package com.aiplayer.util;

import com.aiplayer.AIPlayerMod;
import org.slf4j.Logger;

/**
 * Logging utility for AI Player mod
 *
 * Provides structured logging with different categories and levels.
 */
public class AILogger {
    private static final Logger LOGGER = AIPlayerMod.LOGGER;
    private static boolean debugMode = false;

    /**
     * Enable or disable debug mode
     */
    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
        if (enabled) {
            LOGGER.info("Debug mode enabled");
        }
    }

    /**
     * Check if debug mode is enabled
     */
    public static boolean isDebugMode() {
        return debugMode;
    }

    /**
     * Log a goal-related message
     */
    public static void logGoal(String playerName, String message) {
        LOGGER.info("[GOAL][{}] {}", playerName, message);
    }

    /**
     * Log an action-related message
     */
    public static void logAction(String playerName, String action, boolean success) {
        if (success) {
            LOGGER.debug("[ACTION][{}] Completed: {}", playerName, action);
        } else {
            LOGGER.warn("[ACTION][{}] Failed: {}", playerName, action);
        }
    }

    /**
     * Log an action with result message
     */
    public static void logAction(String playerName, String action, boolean success, String message) {
        if (success) {
            LOGGER.debug("[ACTION][{}] Completed: {} - {}", playerName, action, message);
        } else {
            LOGGER.warn("[ACTION][{}] Failed: {} - {}", playerName, action, message);
        }
    }

    /**
     * Log a perception-related message
     */
    public static void logPerception(String playerName, String message) {
        if (debugMode) {
            LOGGER.debug("[PERCEPTION][{}] {}", playerName, message);
        }
    }

    /**
     * Log a memory-related message
     */
    public static void logMemory(String playerName, String message) {
        if (debugMode) {
            LOGGER.debug("[MEMORY][{}] {}", playerName, message);
        }
    }

    /**
     * Log an LLM API call
     */
    public static void logLLM(String playerName, String callType, long durationMs) {
        LOGGER.debug("[LLM][{}] {} took {}ms", playerName, callType, durationMs);
    }

    /**
     * Log an LLM API call with token count
     */
    public static void logLLM(String playerName, String callType, long durationMs, int tokens) {
        LOGGER.debug("[LLM][{}] {} took {}ms, ~{} tokens", playerName, callType, durationMs, tokens);
    }

    /**
     * Log a communication-related message
     */
    public static void logChat(String playerName, String sender, String message) {
        LOGGER.info("[CHAT][{}] {} said: {}", playerName, sender, message);
    }

    /**
     * Log a planning-related message
     */
    public static void logPlanning(String playerName, String message) {
        LOGGER.debug("[PLANNING][{}] {}", playerName, message);
    }

    /**
     * Log an error
     */
    public static void logError(String playerName, String message, Throwable error) {
        LOGGER.error("[ERROR][{}] {}", playerName, message, error);
    }

    /**
     * Log an error without exception
     */
    public static void logError(String playerName, String message) {
        LOGGER.error("[ERROR][{}] {}", playerName, message);
    }

    /**
     * Log a debug message (only if debug mode is enabled)
     */
    public static void debug(String message) {
        if (debugMode) {
            LOGGER.debug(message);
        }
    }

    /**
     * Log a debug message with parameters
     */
    public static void debug(String format, Object... args) {
        if (debugMode) {
            LOGGER.debug(format, args);
        }
    }

    /**
     * Log an info message
     */
    public static void info(String message) {
        LOGGER.info(message);
    }

    /**
     * Log an info message with parameters
     */
    public static void info(String format, Object... args) {
        LOGGER.info(format, args);
    }

    /**
     * Log a warning message
     */
    public static void warn(String message) {
        LOGGER.warn(message);
    }

    /**
     * Log a warning message with parameters
     */
    public static void warn(String format, Object... args) {
        LOGGER.warn(format, args);
    }

    /**
     * Log an error message
     */
    public static void error(String message) {
        LOGGER.error(message);
    }

    /**
     * Log an error message with exception
     */
    public static void error(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
    }
}
