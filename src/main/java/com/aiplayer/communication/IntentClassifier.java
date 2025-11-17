package com.aiplayer.communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classifies user intent from chat messages.
 *
 * Phase 4 MVP: Rule-based classification using regex patterns
 * Future: Could be upgraded to LLM-based classification for better accuracy
 *
 * Supports common Minecraft task patterns:
 * - "gather/collect/mine X items"
 * - "build/place/construct X"
 * - "kill/fight/attack X mobs"
 * - "craft/make X items"
 * - Status queries: "what are you doing?"
 * - Questions: "do you have X?", "where is X?"
 */
public class IntentClassifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntentClassifier.class);

    // Task request patterns (priority order matters)
    private static final Pattern[] TASK_PATTERNS = {
        // Gathering: "gather/collect/get X items"
        Pattern.compile("(?:can you |could you |please )?(?:gather|collect|get|find)(?: me)? (\\d+)? ?(\\w+(?:\\s+\\w+)*)"),

        // Mining: "mine/dig/break X blocks"
        Pattern.compile("(?:mine|dig|break)(?: me)? (\\d+)? ?(\\w+(?:\\s+\\w+)*)"),

        // Building: "build/place/construct X"
        Pattern.compile("(?:build|place|construct)(?: a| an)? (\\d+)? ?(\\w+(?:\\s+\\w+)*)"),

        // Combat: "kill/fight/attack X"
        Pattern.compile("(?:kill|fight|attack|slay)(?: a| an| the)? (\\d+)? ?(\\w+)"),

        // Crafting: "craft/make X items"
        Pattern.compile("(?:craft|make)(?: me)? (\\d+)? ?(\\w+(?:\\s+\\w+)*)"),

        // Following: "follow me"
        Pattern.compile("(?:follow|come with)(?: me)?"),

        // Guarding: "guard this area"
        Pattern.compile("(?:guard|protect|defend)(?: this| the)? ?(\\w+)?")
    };

    // Status query patterns
    private static final Pattern[] STATUS_PATTERNS = {
        Pattern.compile("(?:what are you doing|what's up|status|progress)\\??"),
        Pattern.compile("(?:how's it going|how are you|how is it going)\\??"),
        Pattern.compile("(?:what's your|what is your) (?:task|goal|current task)\\??"),
        Pattern.compile("(?:are you busy|you busy)\\??")
    };

    // Question patterns
    private static final Pattern[] QUESTION_PATTERNS = {
        Pattern.compile("(?:where is|where are|where can i find|where's) (\\w+(?:\\s+\\w+)*)\\??"),
        Pattern.compile("(?:do you have|got any|have you got|you have) (\\w+(?:\\s+\\w+)*)\\??"),
        Pattern.compile("(?:how many|how much) (\\w+(?:\\s+\\w+)*) (?:do you have|you have)\\??"),
        Pattern.compile("(?:can you find|know where) (\\w+(?:\\s+\\w+)*) (?:is|are)\\??")
    };

    // Item name normalization map
    private static final Map<String, String> ITEM_ALIASES = new HashMap<>();

    static {
        // Common aliases
        ITEM_ALIASES.put("logs", "oak_log");
        ITEM_ALIASES.put("wood", "oak_planks");
        ITEM_ALIASES.put("planks", "oak_planks");
        ITEM_ALIASES.put("sticks", "stick");
        ITEM_ALIASES.put("stones", "stone");
        ITEM_ALIASES.put("cobble", "cobblestone");
        ITEM_ALIASES.put("cobblestones", "cobblestone");
        ITEM_ALIASES.put("dirt", "dirt");
        ITEM_ALIASES.put("iron", "iron_ingot");
        ITEM_ALIASES.put("gold", "gold_ingot");
        ITEM_ALIASES.put("diamond", "diamond");
        ITEM_ALIASES.put("diamonds", "diamond");
        ITEM_ALIASES.put("coal", "coal");
        ITEM_ALIASES.put("food", "bread");

        // Mob aliases
        ITEM_ALIASES.put("zombie", "zombie");
        ITEM_ALIASES.put("zombies", "zombie");
        ITEM_ALIASES.put("skeleton", "skeleton");
        ITEM_ALIASES.put("skeletons", "skeleton");
        ITEM_ALIASES.put("creeper", "creeper");
        ITEM_ALIASES.put("creepers", "creeper");
        ITEM_ALIASES.put("spider", "spider");
        ITEM_ALIASES.put("spiders", "spider");
    }

    /**
     * Classify the intent of a chat message.
     */
    public Intent classify(String message) {
        if (message == null || message.trim().isEmpty()) {
            return new Intent(Intent.Type.CASUAL_CHAT, null);
        }

        String lower = message.toLowerCase().trim();

        // Remove AI name mentions for cleaner parsing
        lower = lower.replaceAll("@\\w+\\s*,?\\s*", "");
        lower = lower.replaceAll("hey \\w+\\s*,?\\s*", "");

        // Check task patterns first (highest priority)
        for (Pattern pattern : TASK_PATTERNS) {
            Matcher matcher = pattern.matcher(lower);
            if (matcher.find()) {
                LOGGER.debug("Classified as TASK_REQUEST: {}", message);
                return new Intent(Intent.Type.TASK_REQUEST, pattern);
            }
        }

        // Check status queries
        for (Pattern pattern : STATUS_PATTERNS) {
            Matcher matcher = pattern.matcher(lower);
            if (matcher.find()) {
                LOGGER.debug("Classified as STATUS_QUERY: {}", message);
                return new Intent(Intent.Type.STATUS_QUERY, pattern);
            }
        }

        // Check questions
        for (Pattern pattern : QUESTION_PATTERNS) {
            Matcher matcher = pattern.matcher(lower);
            if (matcher.find()) {
                LOGGER.debug("Classified as QUESTION: {}", message);
                return new Intent(Intent.Type.QUESTION, pattern);
            }
        }

        // Default to casual chat
        LOGGER.debug("Classified as CASUAL_CHAT: {}", message);
        return new Intent(Intent.Type.CASUAL_CHAT, null);
    }

    /**
     * Extract task request details from message.
     */
    public TaskRequest extractTaskRequest(String message, Intent intent) {
        if (intent.getType() != Intent.Type.TASK_REQUEST) {
            return null;
        }

        String lower = message.toLowerCase().trim();

        // Remove AI name mentions
        lower = lower.replaceAll("@\\w+\\s*,?\\s*", "");
        lower = lower.replaceAll("hey \\w+\\s*,?\\s*", "");

        Matcher matcher = intent.getPattern().matcher(lower);
        if (!matcher.find()) {
            LOGGER.warn("Failed to match pattern for task extraction: {}", message);
            return null;
        }

        // Determine action type from the matched text
        TaskRequest.ActionType actionType = determineActionType(matcher.group(0));

        // Extract quantity
        int quantity = extractQuantity(matcher);

        // Extract target item/entity
        String target = extractTarget(matcher);
        target = normalizeItemName(target);

        LOGGER.info("Extracted task: action={}, quantity={}, target={}", actionType, quantity, target);

        return new TaskRequest(actionType, quantity, target);
    }

    /**
     * Determine action type from matched text.
     */
    private TaskRequest.ActionType determineActionType(String matchedText) {
        if (matchedText.contains("gather") || matchedText.contains("collect") || matchedText.contains("get")) {
            return TaskRequest.ActionType.GATHER;
        } else if (matchedText.contains("mine") || matchedText.contains("dig") || matchedText.contains("break")) {
            return TaskRequest.ActionType.MINE;
        } else if (matchedText.contains("build") || matchedText.contains("place") || matchedText.contains("construct")) {
            return TaskRequest.ActionType.BUILD;
        } else if (matchedText.contains("kill") || matchedText.contains("fight") || matchedText.contains("attack") || matchedText.contains("slay")) {
            return TaskRequest.ActionType.COMBAT;
        } else if (matchedText.contains("craft") || matchedText.contains("make")) {
            return TaskRequest.ActionType.CRAFT;
        } else if (matchedText.contains("follow") || matchedText.contains("come with")) {
            return TaskRequest.ActionType.FOLLOW;
        } else if (matchedText.contains("guard") || matchedText.contains("protect") || matchedText.contains("defend")) {
            return TaskRequest.ActionType.GUARD;
        }

        return TaskRequest.ActionType.UNKNOWN;
    }

    /**
     * Extract quantity from matcher groups.
     */
    private int extractQuantity(Matcher matcher) {
        try {
            // Try to find a number in the groups
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String group = matcher.group(i);
                if (group != null) {
                    try {
                        return Integer.parseInt(group.trim());
                    } catch (NumberFormatException e) {
                        // Not a number, continue
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to extract quantity: {}", e.getMessage());
        }

        return 1; // Default quantity
    }

    /**
     * Extract target item/entity from matcher groups.
     */
    private String extractTarget(Matcher matcher) {
        try {
            // Find the last non-empty, non-numeric group (usually the item name)
            String target = null;
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String group = matcher.group(i);
                if (group != null && !group.trim().isEmpty()) {
                    // Skip if it's just a number
                    try {
                        Integer.parseInt(group.trim());
                    } catch (NumberFormatException e) {
                        // Not a number, this is likely our target
                        target = group.trim();
                    }
                }
            }
            return target;
        } catch (Exception e) {
            LOGGER.debug("Failed to extract target: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Normalize item names to Minecraft IDs.
     */
    private String normalizeItemName(String rawName) {
        if (rawName == null) {
            return null;
        }

        String lower = rawName.toLowerCase().trim();

        // Check aliases first
        if (ITEM_ALIASES.containsKey(lower)) {
            return ITEM_ALIASES.get(lower);
        }

        // Handle multi-word items
        if (lower.contains(" ")) {
            // "oak logs" → "oak_log"
            String normalized = lower.replace(" ", "_");

            // Remove plural 's' if present
            if (normalized.endsWith("s") && !normalized.endsWith("ss")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }

            return normalized;
        }

        // Remove plural 's'
        if (lower.endsWith("s") && !lower.endsWith("ss")) {
            lower = lower.substring(0, lower.length() - 1);
        }

        // Convert spaces to underscores
        return lower.replace(" ", "_");
    }

    /**
     * Extract question subject (what the question is about).
     */
    public String extractQuestionSubject(String message) {
        String lower = message.toLowerCase().trim();

        for (Pattern pattern : QUESTION_PATTERNS) {
            Matcher matcher = pattern.matcher(lower);
            if (matcher.find() && matcher.groupCount() >= 1) {
                return matcher.group(1);
            }
        }

        return null;
    }
}
