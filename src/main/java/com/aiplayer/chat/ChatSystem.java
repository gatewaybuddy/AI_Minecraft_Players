package com.aiplayer.chat;

import com.aiplayer.core.AIPlayerEntity;
import com.aiplayer.llm.LLMOptions;
import com.aiplayer.llm.LLMProvider;
import com.aiplayer.memory.Memory;
import com.aiplayer.perception.WorldState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Chat System - Handles AI player communication.
 *
 * Features:
 * - Detects messages directed at AI
 * - Generates contextual responses using LLM
 * - Maintains conversation memory
 * - Natural language understanding
 */
public class ChatSystem {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatSystem.class);

    private final AIPlayerEntity aiPlayer;
    private final LLMProvider llmProvider;

    public ChatSystem(AIPlayerEntity aiPlayer, LLMProvider llmProvider) {
        this.aiPlayer = aiPlayer;
        this.llmProvider = llmProvider;
    }

    /**
     * Process incoming chat message.
     *
     * @param sender The player who sent the message
     * @param message The message content
     */
    public void onChatMessage(ServerPlayerEntity sender, String message) {
        // Ignore messages from self
        if (sender.getUuid().equals(aiPlayer.getUuid())) {
            return;
        }

        String aiName = aiPlayer.getName().getString();
        String senderName = sender.getName().getString();

        LOGGER.info("[CHAT] {} received message from {}: '{}'", aiName, senderName, message);

        // Check if message is directed at this AI
        if (!isMessageForMe(message, aiName)) {
            LOGGER.debug("[CHAT] Message not directed at {}", aiName);
            return;
        }

        LOGGER.info("[CHAT] Message IS directed at {} - analyzing with LLM", aiName);

        // Store conversation in memory
        aiPlayer.getAIBrain().getMemorySystem().store(new Memory(
            Memory.MemoryType.CONVERSATION,
            String.format("%s said: %s", senderName, message),
            0.9 // High importance for direct communication
        ));

        // Generate response, extract goals, AND determine actions asynchronously
        generateResponseAndGoals(senderName, message)
            .thenAccept(result -> {
                // Execute immediate action if LLM decided one is needed
                if (result.action != null && !result.action.equals("CONTINUE")) {
                    executeAction(result.action, senderName);
                }

                // Send response
                if (result.response != null && !result.response.trim().isEmpty()) {
                    LOGGER.info("[CHAT] {} responding: '{}'", aiName, result.response);
                    aiPlayer.sendChatMessage(result.response);

                    // Store own response
                    aiPlayer.getAIBrain().getMemorySystem().store(new Memory(
                        Memory.MemoryType.CONVERSATION,
                        String.format("I said to %s: %s", senderName, result.response),
                        0.7
                    ));
                }

                // Update goals if any were extracted
                if (result.goal != null) {
                    LOGGER.info("[CHAT] Creating new goal from conversation: {} (Priority: {})",
                        result.goal.getDescription(), result.goal.getPriority());
                    aiPlayer.getAIBrain().getPlanningEngine().addGoal(result.goal);

                    // Store in memory
                    aiPlayer.getAIBrain().getMemorySystem().store(new Memory(
                        Memory.MemoryType.OBSERVATION,
                        String.format("%s asked me to: %s", senderName, result.goal.getDescription()),
                        0.95 // Very high importance
                    ));
                }
            })
            .exceptionally(e -> {
                LOGGER.error("[CHAT] Error processing message", e);
                return null;
            });
    }

    /**
     * Check if message is directed at this AI.
     * More liberal approach - let LLM decide if it should respond.
     */
    private boolean isMessageForMe(String message, String aiName) {
        String lower = message.toLowerCase().trim();
        String nameLower = aiName.toLowerCase();

        // If only 2 players total (sender + this AI), assume all messages are for us
        int totalPlayers = aiPlayer.getServer().getCurrentPlayerCount();
        if (totalPlayers <= 2) {
            LOGGER.debug("[CHAT] Only {} players online - assuming message is for me", totalPlayers);
            return true;
        }

        // Direct address: "TestBot, ..." or "TestBot: ..."
        if (lower.startsWith(nameLower + ",") || lower.startsWith(nameLower + ":")) {
            return true;
        }

        // Contains name anywhere: "Hey TestBot!" or "TestBot?"
        if (lower.contains(nameLower)) {
            return true;
        }

        // Any question - be helpful!
        if (lower.contains("?")) {
            return true;
        }

        // Short messages (likely commands/conversation)
        // "stop", "go", "hello", "thanks", etc.
        if (message.split("\\s+").length <= 3) {
            return true;
        }

        return false;
    }

    /**
     * Result of processing a chat message.
     */
    private static class ChatResult {
        String response;
        String action;
        com.aiplayer.planning.Goal goal;

        ChatResult(String response, String action, com.aiplayer.planning.Goal goal) {
            this.response = response;
            this.action = action;
            this.goal = goal;
        }
    }

    /**
     * Generate response AND extract any goals from the message.
     */
    private CompletableFuture<ChatResult> generateResponseAndGoals(String senderName, String message) {
        // Capture sender name for use in lambda (immutable for thread safety)
        final String requesterName = senderName;

        // Build context
        StringBuilder context = new StringBuilder();

        context.append("## Conversation\n");
        context.append(String.format("%s: %s\n\n", senderName, message));

        // Add current situation
        context.append("## Your Current Status\n");
        context.append(String.format("Name: %s\n", aiPlayer.getName().getString()));
        context.append(String.format("Health: %.1f/20\n", aiPlayer.getHealth()));
        context.append(String.format("Position: %.1f, %.1f, %.1f\n",
            aiPlayer.getX(), aiPlayer.getY(), aiPlayer.getZ()));

        // Add current goal if any
        String goalDesc = aiPlayer.getAIBrain().getCurrentGoalDescription();
        if (goalDesc != null && !goalDesc.equals("Idle")) {
            context.append(String.format("Current Activity: %s\n", goalDesc));
        }
        context.append("\n");

        // Add recent memories
        context.append("## Recent Memories\n");
        var recentMemories = aiPlayer.getAIBrain().getMemorySystem().getWorkingMemory().getRecent(3);
        if (recentMemories.isEmpty()) {
            context.append("(none)\n");
        } else {
            for (var memory : recentMemories) {
                context.append(String.format("- %s\n", memory.getContent()));
            }
        }
        context.append("\n");

        context.append("## Task\n");
        context.append("Analyze the message and provide:\n");
        context.append("1. A natural response (1-2 sentences)\n");
        context.append("2. An immediate ACTION to take (if needed)\n");
        context.append("3. Extract any long-term GOAL if the player is requesting something\n\n");
        context.append("Format your response as:\n");
        context.append("RESPONSE: <your natural reply>\n");
        context.append("ACTION: <STOP_MOVEMENT, CLEAR_GOALS, FOLLOW_PLAYER, CHECK_INVENTORY, or CONTINUE>\n");
        context.append("GOAL: <goal description if player requested something, or NONE>\n");
        context.append("GOAL_TYPE: <SURVIVAL, RESOURCE_GATHERING, BUILD, COMBAT, EXPLORATION, or SOCIAL>\n");
        context.append("PRIORITY: <1-10, use 8-10 for player requests>\n");

        LOGGER.debug("[CHAT] LLM context: {}", context);

        // Generate response with action and goal extraction
        LLMOptions options = LLMOptions.chat()
            .maxTokens(800)
            .systemPrompt(String.format(
                "You are %s, an AI player in Minecraft. " +
                "You are helpful, friendly, and eager to assist. " +
                "You can take immediate ACTIONS and create long-term GOALS.\n\n" +
                "Actions available:\n" +
                "- STOP_MOVEMENT: Stop moving immediately\n" +
                "- CLEAR_GOALS: Cancel all current goals\n" +
                "- FOLLOW_PLAYER: Start following the player\n" +
                "- CHECK_INVENTORY: Report what's in your inventory\n" +
                "- CONTINUE: Keep doing what you're doing\n\n" +
                "Examples:\n" +
                "- 'Can you get some wood?' → RESPONSE: Sure, I'll gather wood! | ACTION: CONTINUE | GOAL: Gather wood | TYPE: RESOURCE_GATHERING | PRIORITY: 9\n" +
                "- 'Mine some coal' → RESPONSE: I'll mine some coal for you! | ACTION: CONTINUE | GOAL: Mine coal | TYPE: RESOURCE_GATHERING | PRIORITY: 9\n" +
                "- 'Chop down that tree' → RESPONSE: On it! Chopping wood! | ACTION: CONTINUE | GOAL: Chop wood | TYPE: RESOURCE_GATHERING | PRIORITY: 9\n" +
                "- 'What do you have?' → RESPONSE: Let me check my inventory! | ACTION: CHECK_INVENTORY | GOAL: NONE\n" +
                "- 'Check your inventory' → RESPONSE: Here's what I have: | ACTION: CHECK_INVENTORY | GOAL: NONE\n" +
                "- 'Stop walking' → RESPONSE: Okay, I've stopped! | ACTION: STOP_MOVEMENT | GOAL: NONE\n" +
                "- 'Stop that' → RESPONSE: Stopping! | ACTION: CLEAR_GOALS | GOAL: NONE\n" +
                "- 'Come here' → RESPONSE: On my way! | ACTION: FOLLOW_PLAYER | GOAL: NONE\n" +
                "- 'Hello!' → RESPONSE: Hi there! | ACTION: CONTINUE | GOAL: NONE\n" +
                "- 'What are you doing?' → RESPONSE: [describe activity] | ACTION: CONTINUE | GOAL: NONE",
                aiPlayer.getName().getString()
            ));

        return llmProvider.complete(context.toString(), options)
            .thenApply(llmResponse -> {
                LOGGER.debug("[CHAT] LLM full response: {}", llmResponse);

                // Parse response, action, and goal
                String response = extractField(llmResponse, "RESPONSE:");
                String action = extractField(llmResponse, "ACTION:");
                String goalDescription = extractField(llmResponse, "GOAL:");
                String goalTypeStr = extractField(llmResponse, "GOAL_TYPE:");
                String priorityStr = extractField(llmResponse, "PRIORITY:");

                // Clean up response
                if (response != null) {
                    response = response.trim().replaceAll("^[\"']|[\"']$", "");
                }

                // Clean up action
                if (action != null) {
                    action = action.trim().toUpperCase();
                } else {
                    action = "CONTINUE"; // Default action
                }

                // Create goal if requested
                com.aiplayer.planning.Goal goal = null;
                if (goalDescription != null && !goalDescription.equalsIgnoreCase("NONE") && !goalDescription.trim().isEmpty()) {
                    int priorityValue = 9; // High priority for player requests
                    if (priorityStr != null) {
                        try {
                            priorityValue = Integer.parseInt(priorityStr.trim());
                            priorityValue = Math.max(1, Math.min(10, priorityValue));
                        } catch (NumberFormatException e) {
                            LOGGER.warn("[CHAT] Failed to parse priority: {}", priorityStr);
                        }
                    }

                    // Determine goal type
                    com.aiplayer.planning.Goal.GoalType type = parseGoalType(goalTypeStr);

                    // Create goal with player as requester
                    goal = new com.aiplayer.planning.Goal(
                        goalDescription.trim(),
                        type,
                        priorityValue,
                        requesterName // Track who requested it
                    );

                    LOGGER.info("[CHAT] Extracted goal from conversation: '{}' (Type: {}, Priority: {})",
                        goalDescription, type, priorityValue);
                }

                LOGGER.info("[CHAT] LLM decision - Action: {}, Goal: {}", action, goal != null ? goal.getDescription() : "NONE");
                return new ChatResult(response, action, goal);
            });
    }

    /**
     * Parse goal type from string.
     */
    private com.aiplayer.planning.Goal.GoalType parseGoalType(String typeStr) {
        if (typeStr == null) {
            return com.aiplayer.planning.Goal.GoalType.EXPLORATION;
        }

        try {
            return com.aiplayer.planning.Goal.GoalType.valueOf(typeStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("[CHAT] Unknown goal type: {}, defaulting to EXPLORATION", typeStr);
            return com.aiplayer.planning.Goal.GoalType.EXPLORATION;
        }
    }

    /**
     * Execute an action decided by the LLM.
     */
    private void executeAction(String action, String requester) {
        LOGGER.info("[CHAT] Executing LLM-decided action: {}", action);

        switch (action.toUpperCase()) {
            case "STOP_MOVEMENT":
                LOGGER.info("[CHAT] ACTION: Stopping movement");
                aiPlayer.stopMovement();
                aiPlayer.getAIBrain().getMemorySystem().store(new Memory(
                    Memory.MemoryType.OBSERVATION,
                    String.format("%s told me to stop moving", requester),
                    0.9
                ));
                break;

            case "CLEAR_GOALS":
                LOGGER.info("[CHAT] ACTION: Clearing all goals");
                aiPlayer.stopMovement();
                aiPlayer.getAIBrain().getPlanningEngine().clearGoals();
                aiPlayer.getAIBrain().getMemorySystem().store(new Memory(
                    Memory.MemoryType.OBSERVATION,
                    String.format("%s told me to stop what I'm doing", requester),
                    0.95
                ));
                break;

            case "FOLLOW_PLAYER":
                LOGGER.info("[CHAT] ACTION: Following player {}", requester);
                // Create a high-priority SOCIAL goal to follow the player
                com.aiplayer.planning.Goal followGoal = new com.aiplayer.planning.Goal(
                    String.format("Follow %s", requester),
                    com.aiplayer.planning.Goal.GoalType.SOCIAL,
                    10, // Highest priority
                    requester
                );
                aiPlayer.getAIBrain().getPlanningEngine().addGoal(followGoal);
                aiPlayer.getAIBrain().getMemorySystem().store(new Memory(
                    Memory.MemoryType.OBSERVATION,
                    String.format("%s asked me to follow them", requester),
                    0.9
                ));
                break;

            case "CHECK_INVENTORY":
                LOGGER.info("[CHAT] ACTION: Checking inventory");
                // Get inventory summary and send to chat
                java.util.List<net.minecraft.item.ItemStack> items = aiPlayer.getActionController().inventory().getAllItems();
                StringBuilder inventoryReport = new StringBuilder("My inventory: ");

                if (items.isEmpty()) {
                    inventoryReport.append("Empty - I have nothing!");
                } else {
                    // Group items by type and count
                    java.util.Map<String, Integer> itemCounts = new java.util.HashMap<>();
                    for (net.minecraft.item.ItemStack stack : items) {
                        String itemName = stack.getItem().getName().getString();
                        int count = stack.getCount();
                        itemCounts.put(itemName, itemCounts.getOrDefault(itemName, 0) + count);
                    }

                    // Build summary (limit to 5 most common items to avoid spam)
                    java.util.List<java.util.Map.Entry<String, Integer>> sortedItems = new java.util.ArrayList<>(itemCounts.entrySet());
                    sortedItems.sort((a, b) -> b.getValue().compareTo(a.getValue()));

                    int count = 0;
                    for (java.util.Map.Entry<String, Integer> entry : sortedItems) {
                        if (count > 0) inventoryReport.append(", ");
                        inventoryReport.append(entry.getValue()).append("x ").append(entry.getKey());
                        count++;
                        if (count >= 5) {
                            if (sortedItems.size() > 5) {
                                inventoryReport.append(", and ").append(sortedItems.size() - 5).append(" more types");
                            }
                            break;
                        }
                    }
                }

                aiPlayer.sendChatMessage(inventoryReport.toString());
                LOGGER.info("[CHAT] Inventory report: {}", inventoryReport);
                break;

            case "CONTINUE":
            default:
                LOGGER.debug("[CHAT] ACTION: Continue current activity");
                // No action needed - continue what we're doing
                break;
        }
    }

    /**
     * Extract field from LLM response.
     */
    private String extractField(String text, String fieldName) {
        if (text == null) return null;

        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.trim().toUpperCase().startsWith(fieldName.toUpperCase())) {
                return line.substring(fieldName.length()).trim();
            }
        }
        return null;
    }

    /**
     * Check if LLM is available for chat.
     */
    public boolean isAvailable() {
        return llmProvider != null && llmProvider.isAvailable();
    }
}
