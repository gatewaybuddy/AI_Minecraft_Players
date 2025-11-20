package com.aiplayer.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Natural Language Understanding Engine.
 *
 * Classifies player messages into intent categories and extracts entities.
 * Uses pattern matching and keyword detection for fast, deterministic classification.
 *
 * Phase 4: Natural Language Communication
 */
public class NLUEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(NLUEngine.class);

    /**
     * Intent types for player messages.
     */
    public enum IntentType {
        TASK_REQUEST,    // "gather 64 oak logs", "build a house"
        QUESTION,        // "what are you doing?", "where are you?"
        GREETING,        // "hello", "hi there"
        CASUAL_CHAT,     // "nice weather", "how are you"
        COMMAND,         // "stop", "follow me", "come here"
        UNKNOWN          // Unclassified
    }

    /**
     * Represents the classified intent and extracted entities.
     */
    public static class Intent {
        private final IntentType type;
        private final Map<String, Object> entities;
        private final double confidence;

        public Intent(IntentType type, double confidence) {
            this.type = type;
            this.confidence = confidence;
            this.entities = new HashMap<>();
        }

        public IntentType getType() {
            return type;
        }

        public double getConfidence() {
            return confidence;
        }

        public Map<String, Object> getEntities() {
            return entities;
        }

        public void addEntity(String key, Object value) {
            entities.put(key, value);
        }

        public Object getEntity(String key) {
            return entities.get(key);
        }

        @Override
        public String toString() {
            return String.format("Intent{type=%s, confidence=%.2f, entities=%s}",
                type, confidence, entities);
        }
    }

    // Pattern for detecting greetings
    private static final Pattern GREETING_PATTERN = Pattern.compile(
        "(?i)^(hi|hello|hey|greetings|howdy|sup|yo)\\b"
    );

    // Pattern for detecting questions
    private static final Pattern QUESTION_PATTERN = Pattern.compile(
        "(?i)(what|where|when|why|how|who|which|are you|do you|can you|will you|have you).*\\?"
    );

    // Pattern for detecting commands
    private static final Pattern COMMAND_PATTERN = Pattern.compile(
        "(?i)^(stop|wait|follow|come|stay|go|attack|defend|help)"
    );

    // Pattern for detecting task requests
    private static final Pattern TASK_PATTERN = Pattern.compile(
        "(?i)(gather|collect|get|fetch|mine|chop|build|craft|make|create|find|kill|hunt)"
    );

    // Pattern for extracting quantities
    private static final Pattern QUANTITY_PATTERN = Pattern.compile(
        "\\b(\\d+)\\s+(\\w+)"
    );

    /**
     * Classify the intent of a message.
     *
     * @param message The message to classify
     * @return The classified intent
     */
    public Intent classifyIntent(String message) {
        String cleaned = message.trim();

        // Try each classification in order of specificity
        Intent intent;

        // 1. Check for greetings
        if (GREETING_PATTERN.matcher(cleaned).find()) {
            intent = new Intent(IntentType.GREETING, 0.95);
            LOGGER.debug("Classified as GREETING: {}", message);
            return intent;
        }

        // 2. Check for questions
        if (QUESTION_PATTERN.matcher(cleaned).find()) {
            intent = new Intent(IntentType.QUESTION, 0.90);
            extractQuestionEntities(cleaned, intent);
            LOGGER.debug("Classified as QUESTION: {}", message);
            return intent;
        }

        // 3. Check for commands
        if (COMMAND_PATTERN.matcher(cleaned).find()) {
            intent = new Intent(IntentType.COMMAND, 0.85);
            extractCommandEntities(cleaned, intent);
            LOGGER.debug("Classified as COMMAND: {}", message);
            return intent;
        }

        // 4. Check for task requests
        if (TASK_PATTERN.matcher(cleaned).find()) {
            intent = new Intent(IntentType.TASK_REQUEST, 0.80);
            extractTaskEntities(cleaned, intent);
            LOGGER.debug("Classified as TASK_REQUEST: {}", message);
            return intent;
        }

        // 5. Check for casual chat indicators
        if (isCasualChat(cleaned)) {
            intent = new Intent(IntentType.CASUAL_CHAT, 0.70);
            LOGGER.debug("Classified as CASUAL_CHAT: {}", message);
            return intent;
        }

        // Default: unknown
        intent = new Intent(IntentType.UNKNOWN, 0.50);
        LOGGER.debug("Classified as UNKNOWN: {}", message);
        return intent;
    }

    /**
     * Extract entities from a question.
     */
    private void extractQuestionEntities(String message, Intent intent) {
        String lower = message.toLowerCase();

        // Detect question type
        if (lower.contains("what") && lower.contains("doing")) {
            intent.addEntity("questionType", "currentActivity");
        } else if (lower.contains("where")) {
            intent.addEntity("questionType", "location");
        } else if (lower.contains("how") && (lower.contains("health") || lower.contains("feeling"))) {
            intent.addEntity("questionType", "status");
        } else if (lower.contains("what") && lower.contains("have")) {
            intent.addEntity("questionType", "inventory");
        } else {
            intent.addEntity("questionType", "general");
        }
    }

    /**
     * Extract entities from a command.
     */
    private void extractCommandEntities(String message, Intent intent) {
        String lower = message.toLowerCase();

        // Detect command type
        if (lower.contains("stop") || lower.contains("wait")) {
            intent.addEntity("commandType", "stop");
        } else if (lower.contains("follow")) {
            intent.addEntity("commandType", "follow");
        } else if (lower.contains("come") || lower.contains("here")) {
            intent.addEntity("commandType", "comeHere");
        } else if (lower.contains("go")) {
            intent.addEntity("commandType", "go");
        } else if (lower.contains("attack")) {
            intent.addEntity("commandType", "attack");
        } else {
            intent.addEntity("commandType", "generic");
        }
    }

    /**
     * Extract entities from a task request.
     */
    private void extractTaskEntities(String message, Intent intent) {
        String lower = message.toLowerCase();

        // Detect action verb
        if (lower.contains("gather") || lower.contains("collect") || lower.contains("get") || lower.contains("fetch")) {
            intent.addEntity("action", "gather");
        } else if (lower.contains("mine") || lower.contains("chop")) {
            intent.addEntity("action", "mine");
        } else if (lower.contains("build") || lower.contains("create") || lower.contains("construct")) {
            intent.addEntity("action", "build");
        } else if (lower.contains("craft") || lower.contains("make")) {
            intent.addEntity("action", "craft");
        } else if (lower.contains("find") || lower.contains("locate") || lower.contains("search")) {
            intent.addEntity("action", "find");
        } else if (lower.contains("kill") || lower.contains("hunt") || lower.contains("attack")) {
            intent.addEntity("action", "combat");
        }

        // Extract quantities and items
        Matcher quantityMatcher = QUANTITY_PATTERN.matcher(message);
        if (quantityMatcher.find()) {
            try {
                int quantity = Integer.parseInt(quantityMatcher.group(1));
                String item = quantityMatcher.group(2);
                intent.addEntity("quantity", quantity);
                intent.addEntity("item", item);
            } catch (NumberFormatException e) {
                // Ignore invalid numbers
            }
        }

        // Extract common Minecraft items/blocks
        extractMinecraftItems(lower, intent);
    }

    /**
     * Extract Minecraft item names from message.
     */
    private void extractMinecraftItems(String message, Intent intent) {
        // Common materials
        String[] materials = {
            "wood", "logs", "oak", "birch", "spruce", "jungle", "acacia", "dark_oak",
            "stone", "cobblestone", "iron", "gold", "diamond", "emerald",
            "coal", "copper", "redstone", "lapis",
            "dirt", "grass", "sand", "gravel",
            "wheat", "carrot", "potato", "beef", "pork", "chicken",
            "sword", "pickaxe", "axe", "shovel", "hoe"
        };

        List<String> foundItems = new ArrayList<>();
        for (String material : materials) {
            if (message.contains(material)) {
                foundItems.add(material);
            }
        }

        if (!foundItems.isEmpty()) {
            // If no item was set by quantity pattern, use first found item
            if (!intent.getEntities().containsKey("item")) {
                intent.addEntity("item", foundItems.get(0));
            }
            intent.addEntity("materials", foundItems);
        }
    }

    /**
     * Check if message is casual chat.
     */
    private boolean isCasualChat(String message) {
        String lower = message.toLowerCase();

        // Casual chat indicators
        String[] casualPhrases = {
            "how are you", "nice weather", "good job", "well done",
            "thanks", "thank you", "cool", "awesome", "great",
            "lol", "haha", "hehe", "nice", "interesting"
        };

        for (String phrase : casualPhrases) {
            if (lower.contains(phrase)) {
                return true;
            }
        }

        // Short messages without clear intent are usually casual
        return message.split("\\s+").length <= 3 && !message.endsWith("?");
    }

    /**
     * Extract all entities from a message (for debugging/testing).
     *
     * @param message The message
     * @return Map of extracted entities
     */
    public Map<String, Object> extractAllEntities(String message) {
        Map<String, Object> entities = new HashMap<>();

        // Extract quantities
        Matcher quantityMatcher = QUANTITY_PATTERN.matcher(message);
        while (quantityMatcher.find()) {
            try {
                int quantity = Integer.parseInt(quantityMatcher.group(1));
                String item = quantityMatcher.group(2);
                entities.put("quantity_" + item, quantity);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        // Extract Minecraft items
        Intent tempIntent = new Intent(IntentType.UNKNOWN, 0.0);
        extractMinecraftItems(message.toLowerCase(), tempIntent);
        entities.putAll(tempIntent.getEntities());

        return entities;
    }
}
