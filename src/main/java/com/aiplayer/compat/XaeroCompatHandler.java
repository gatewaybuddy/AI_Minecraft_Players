package com.aiplayer.compat;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compatibility handler for Xaero's Party and Claims mod.
 *
 * Xaero's PAC mod has a bug where it crashes when encountering AI players
 * due to null task handling in party synchronization. This handler wraps
 * server tick events to catch and suppress these crashes.
 */
public class XaeroCompatHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("AIPlayer-XaeroCompat");
    private static boolean initialized = false;
    private static int suppressedCrashes = 0;

    /**
     * Initialize Xaero compatibility handling.
     * Should be called during mod initialization.
     */
    public static void initialize() {
        if (initialized) {
            return;
        }

        LOGGER.info("Initializing Xaero's PAC compatibility handler...");

        // Wrap server tick events to catch Xaero crashes
        ServerTickEvents.START_SERVER_TICK.register(XaeroCompatHandler::onServerTickStart);

        initialized = true;
        LOGGER.info("Xaero's PAC compatibility handler initialized");
    }

    /**
     * Server tick event handler that catches Xaero crashes.
     */
    private static void onServerTickStart(MinecraftServer server) {
        // Set up a custom exception handler for this tick
        Thread currentThread = Thread.currentThread();
        Thread.UncaughtExceptionHandler oldHandler = currentThread.getUncaughtExceptionHandler();

        currentThread.setUncaughtExceptionHandler((thread, throwable) -> {
            if (isXaeroCrash(throwable)) {
                suppressCrash(throwable);
            } else if (oldHandler != null) {
                oldHandler.uncaughtException(thread, throwable);
            } else {
                LOGGER.error("Uncaught exception", throwable);
            }
        });

        // Logging
        if (suppressedCrashes > 0 && server.getTicks() % 6000 == 0) {
            LOGGER.info("Suppressed {} Xaero PAC crashes in the last 5 minutes", suppressedCrashes);
            suppressedCrashes = 0;
        }
    }

    /**
     * Check if an exception is a known Xaero PAC crash.
     */
    public static boolean isXaeroCrash(Throwable e) {
        if (e == null) {
            return false;
        }

        // Check if this is the Xaero null task crash
        String message = e.getMessage();
        if (message != null && message.contains("xaero.pac") && message.contains("task") && message.contains("null")) {
            return true;
        }

        // Check the cause chain
        Throwable cause = e.getCause();
        while (cause != null) {
            message = cause.getMessage();
            if (message != null && message.contains("task") && message.contains("null")) {
                String stackTrace = getStackTraceString(cause);
                if (stackTrace.contains("xaero.pac")) {
                    return true;
                }
            }
            cause = cause.getCause();
        }

        return false;
    }

    /**
     * Suppress a Xaero crash and log it.
     */
    public static void suppressCrash(Throwable e) {
        suppressedCrashes++;
        if (suppressedCrashes <= 3) {
            LOGGER.warn("Suppressed Xaero PAC crash (likely from AI player): {}", e.getMessage());
            if (suppressedCrashes == 3) {
                LOGGER.warn("Further Xaero PAC crashes will be suppressed silently");
            }
        }
    }

    /**
     * Get stack trace as string.
     */
    private static String getStackTraceString(Throwable e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
