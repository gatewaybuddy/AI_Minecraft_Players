package com.aiplayer.communication;

import com.aiplayer.core.AIPlayerBrain;
import com.aiplayer.core.AIPlayerEntity;
import com.aiplayer.memory.Memory;
import com.aiplayer.planning.Goal;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes chat messages and determines appropriate responses.
 *
 * This is the main message routing and handling system that:
 * 1. Classifies user intent
 * 2. Routes to appropriate handler (task, question, status, chat)
 * 3. Generates and sends responses
 * 4. Updates AI memory with interactions
 */
public class MessageProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageProcessor.class);

    private final IntentClassifier intentClassifier;
    private final ResponseGenerator responseGenerator;

    public MessageProcessor(ResponseGenerator responseGenerator) {
        this.intentClassifier = new IntentClassifier();
        this.responseGenerator = responseGenerator;
    }

    /**
     * Process incoming message and generate response.
     */
    public void processMessage(
        String message,
        ServerPlayerEntity sender,
        AIPlayerEntity aiPlayer
    ) {
        try {
            LOGGER.info("Processing message from {}: {}", sender.getName().getString(), message);

            // Classify intent
            Intent intent = intentClassifier.classify(message);
            LOGGER.debug("Classified intent: {}", intent);

            // Store interaction in memory
            storeInteractionMemory(aiPlayer, sender, message, intent);

            // Route based on intent
            switch (intent.getType()) {
                case TASK_REQUEST:
                    handleTaskRequest(message, intent, sender, aiPlayer);
                    break;
                case STATUS_QUERY:
                    handleStatusQuery(sender, aiPlayer);
                    break;
                case QUESTION:
                    handleQuestion(message, intent, sender, aiPlayer);
                    break;
                case CASUAL_CHAT:
                    handleCasualChat(message, sender, aiPlayer);
                    break;
            }

        } catch (Exception e) {
            LOGGER.error("Error processing message from " + sender.getName().getString(), e);
            // Send error response
            aiPlayer.sendChatMessage("Sorry, I didn't quite catch that!");
        }
    }

    /**
     * Handle task request from player.
     */
    private void handleTaskRequest(
        String message,
        Intent intent,
        ServerPlayerEntity sender,
        AIPlayerEntity aiPlayer
    ) {
        // Parse task from message
        TaskRequest taskRequest = intentClassifier.extractTaskRequest(message, intent);

        if (taskRequest == null) {
            LOGGER.warn("Failed to extract task from message: {}", message);
            aiPlayer.sendChatMessage("I'm not sure what you want me to do.");
            return;
        }

        LOGGER.info("Task request from {}: {}", sender.getName().getString(), taskRequest);

        // Check basic feasibility
        if (!taskRequest.isFeasible()) {
            String response = responseGenerator.generateTaskRejectionResponse(
                taskRequest,
                taskRequest.getRejectionReason()
            );
            aiPlayer.sendChatMessage(response);
            return;
        }

        // Convert to goal
        Goal goal = taskRequest.toGoal();

        // Add requester information to goal
        goal.setParameter("requester", sender.getName().getString());
        goal.setParameter("requester_uuid", sender.getUuidAsString());

        // Try to add goal to AI's brain
        AIPlayerBrain brain = aiPlayer.getBrain();
        if (brain != null && brain.canAcceptGoal(goal)) {
            brain.addGoal(goal);

            // Generate acceptance response
            String response = responseGenerator.generateTaskAcceptanceResponse(taskRequest);
            aiPlayer.sendChatMessage(response);

            // Store in memory
            brain.getMemorySystem().store(new Memory(
                Memory.MemoryType.SOCIAL,
                "Accepted task from " + sender.getName().getString() + ": " + taskRequest.getDescription(),
                0.8
            ));

            LOGGER.info("AI {} accepted task: {}", aiPlayer.getName().getString(), taskRequest);

        } else {
            // Cannot accept goal
            String reason = determineRejectionReason(goal, brain);
            String response = responseGenerator.generateTaskRejectionResponse(taskRequest, reason);
            aiPlayer.sendChatMessage(response);

            LOGGER.info("AI {} rejected task: {} (reason: {})",
                aiPlayer.getName().getString(), taskRequest, reason);
        }
    }

    /**
     * Handle status query.
     */
    private void handleStatusQuery(ServerPlayerEntity sender, AIPlayerEntity aiPlayer) {
        AIPlayerBrain brain = aiPlayer.getBrain();
        if (brain == null) {
            aiPlayer.sendChatMessage("I'm just exploring right now.");
            return;
        }

        Goal currentGoal = brain.getCurrentGoal();

        if (currentGoal != null) {
            int progress = currentGoal.getProgress();
            String response = responseGenerator.generateStatusResponse(
                currentGoal.getDescription(),
                progress
            );
            aiPlayer.sendChatMessage(response);
        } else {
            String response = responseGenerator.generateStatusResponse(null, -1);
            aiPlayer.sendChatMessage(response);
        }

        LOGGER.debug("Sent status response to {}", sender.getName().getString());
    }

    /**
     * Handle question about world/inventory/etc.
     */
    private void handleQuestion(
        String message,
        Intent intent,
        ServerPlayerEntity sender,
        AIPlayerEntity aiPlayer
    ) {
        AIPlayerBrain brain = aiPlayer.getBrain();

        // Generate response using LLM (async)
        responseGenerator.generateQuestionResponse(
            message,
            brain != null ? brain.getWorldState() : null,
            brain != null ? brain.getMemorySystem() : null
        ).thenAccept(response -> {
            aiPlayer.sendChatMessage(response);
            LOGGER.debug("Sent question response to {}: {}", sender.getName().getString(), response);
        }).exceptionally(e -> {
            LOGGER.error("Failed to generate question response", e);
            aiPlayer.sendChatMessage("Hmm, I'm not sure about that.");
            return null;
        });
    }

    /**
     * Handle casual chat.
     */
    private void handleCasualChat(
        String message,
        ServerPlayerEntity sender,
        AIPlayerEntity aiPlayer
    ) {
        // Generate chat response using LLM (async)
        responseGenerator.generateChatResponse(
            message,
            sender.getName().getString(),
            aiPlayer.getConfig() != null ? aiPlayer.getConfig().getPersonality() : null
        ).thenAccept(response -> {
            aiPlayer.sendChatMessage(response);
            LOGGER.debug("Sent chat response to {}: {}", sender.getName().getString(), response);
        }).exceptionally(e -> {
            LOGGER.error("Failed to generate chat response", e);
            aiPlayer.sendChatMessage("Hey there!");
            return null;
        });
    }

    /**
     * Store interaction in AI's memory.
     */
    private void storeInteractionMemory(
        AIPlayerEntity aiPlayer,
        ServerPlayerEntity sender,
        String message,
        Intent intent
    ) {
        AIPlayerBrain brain = aiPlayer.getBrain();
        if (brain == null || brain.getMemorySystem() == null) {
            return;
        }

        String memoryContent = String.format(
            "%s said: \"%s\" (intent: %s)",
            sender.getName().getString(),
            message,
            intent.getType()
        );

        // Importance based on intent type
        double importance = switch (intent.getType()) {
            case TASK_REQUEST -> 0.9;  // High importance
            case QUESTION -> 0.6;
            case STATUS_QUERY -> 0.4;
            case CASUAL_CHAT -> 0.3;
        };

        brain.getMemorySystem().store(new Memory(
            Memory.MemoryType.SOCIAL,
            memoryContent,
            importance
        ));
    }

    /**
     * Determine why a goal cannot be accepted.
     */
    private String determineRejectionReason(Goal goal, AIPlayerBrain brain) {
        if (brain == null) {
            return "I'm not fully initialized yet.";
        }

        // Check if at max goals
        if (brain.getActiveGoals().size() >= 5) {
            return "I'm too busy right now. I have " + brain.getActiveGoals().size() + " tasks already.";
        }

        // Check goal type
        if (goal.getType() == Goal.GoalType.BUILD) {
            return "I don't have the building materials yet.";
        }

        if (goal.getType() == Goal.GoalType.COMBAT) {
            return "I'm not equipped for combat right now.";
        }

        // Generic reason
        return "I don't think I can do that right now.";
    }
}
