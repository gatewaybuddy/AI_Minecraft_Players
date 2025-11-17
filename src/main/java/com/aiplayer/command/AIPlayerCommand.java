package com.aiplayer.command;

import com.aiplayer.AIPlayerMod;
import com.aiplayer.core.AIPlayerEntity;
import com.aiplayer.core.AIPlayerManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/**
 * Command handler for /aiplayer commands.
 *
 * Provides commands to:
 * - /aiplayer spawn <name> - Spawn a new AI player
 * - /aiplayer despawn <name> - Despawn an AI player
 * - /aiplayer list - List all active AI players
 * - /aiplayer status <name> - Get status of an AI player
 * - /aiplayer reload - Reload configuration
 */
public class AIPlayerCommand {

    /**
     * Register all /aiplayer commands.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(
            CommandManager.literal("aiplayer")
                .requires(source -> source.hasPermissionLevel(2)) // Require OP

                // /aiplayer spawn <name>
                .then(CommandManager.literal("spawn")
                    .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(AIPlayerCommand::spawnCommand)))

                // /aiplayer despawn <name>
                .then(CommandManager.literal("despawn")
                    .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(AIPlayerCommand::despawnCommand)))

                // /aiplayer list
                .then(CommandManager.literal("list")
                    .executes(AIPlayerCommand::listCommand))

                // /aiplayer status <name>
                .then(CommandManager.literal("status")
                    .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(AIPlayerCommand::statusCommand)))

                // /aiplayer reload
                .then(CommandManager.literal("reload")
                    .executes(AIPlayerCommand::reloadCommand))
        );
    }

    /**
     * Handle /aiplayer spawn <name>
     */
    private static int spawnCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        ServerCommandSource source = context.getSource();
        AIPlayerManager manager = AIPlayerMod.getPlayerManager();

        // Check if already exists
        if (manager.hasPlayer(name)) {
            source.sendError(Text.literal("AI player '" + name + "' already exists"));
            return 0;
        }

        // Spawn the AI player
        AIPlayerEntity aiPlayer = manager.spawnAIPlayer(source.getServer(), name);

        if (aiPlayer != null) {
            source.sendFeedback(
                () -> Text.literal("✓ Spawned AI player: " + name),
                true // Broadcast to ops
            );
            return 1;
        } else {
            source.sendError(Text.literal("Failed to spawn AI player: " + name));
            return 0;
        }
    }

    /**
     * Handle /aiplayer despawn <name>
     */
    private static int despawnCommand(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        ServerCommandSource source = context.getSource();
        AIPlayerManager manager = AIPlayerMod.getPlayerManager();

        // Check if exists
        if (!manager.hasPlayer(name)) {
            source.sendError(Text.literal("AI player '" + name + "' not found"));
            return 0;
        }

        // Despawn the AI player
        boolean success = manager.despawnAIPlayer(name);

        if (success) {
            source.sendFeedback(
                () -> Text.literal("✓ Despawned AI player: " + name),
                true // Broadcast to ops
            );
            return 1;
        } else {
            source.sendError(Text.literal("Failed to despawn AI player: " + name));
            return 0;
        }
    }

    /**
     * Handle /aiplayer list
     */
    private static int listCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        AIPlayerManager manager = AIPlayerMod.getPlayerManager();

        int count = manager.getActivePlayerCount();

        if (count == 0) {
            source.sendFeedback(
                () -> Text.literal("No AI players active"),
                false
            );
            return 1;
        }

        source.sendFeedback(
            () -> Text.literal("Active AI players (" + count + "):"),
            false
        );

        for (String playerName : manager.getAllPlayerNames()) {
            source.sendFeedback(
                () -> Text.literal("  - " + playerName),
                false
            );
        }

        return 1;
    }

    /**
     * Handle /aiplayer status <name>
     */
    private static int statusCommand(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        ServerCommandSource source = context.getSource();
        AIPlayerManager manager = AIPlayerMod.getPlayerManager();

        AIPlayerEntity player = manager.getPlayer(name);

        if (player == null) {
            source.sendError(Text.literal("AI player '" + name + "' not found"));
            return 0;
        }

        String status = player.getStatusInfo();
        source.sendFeedback(
            () -> Text.literal(status),
            false
        );

        // Also show perception stats
        String perceptionStats = player.getBrain().getCurrentGoalDescription();
        source.sendFeedback(
            () -> Text.literal("Current Goal: " + perceptionStats),
            false
        );

        return 1;
    }

    /**
     * Handle /aiplayer reload
     */
    private static int reloadCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        try {
            AIPlayerMod.reloadConfig();
            source.sendFeedback(
                () -> Text.literal("✓ Configuration reloaded"),
                true
            );
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to reload configuration: " + e.getMessage()));
            return 0;
        }
    }
}
