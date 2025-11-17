package com.aiplayer.planning;

import com.aiplayer.llm.LLMOptions;
import com.aiplayer.llm.LLMProvider;
import com.aiplayer.memory.Memory;
import com.aiplayer.memory.MemorySystem;
import com.aiplayer.perception.WorldState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Planning Engine - LLM-powered planning using ReAct framework.
 *
 * ReAct = Reasoning + Acting:
 * 1. Observe: Current world state + recent memories
 * 2. Think: LLM reasons about situation and goals
 * 3. Plan: Generate goals and task sequence
 * 4. Act: Execute tasks via action system
 * 5. Reflect: Update memories based on results
 *
 * The LLM receives:
 * - Current situation (health, hunger, position, nearby entities/blocks)
 * - Recent memories (episodic + working memory)
 * - Current goals and their status
 * - Available skills/actions
 *
 * The LLM produces:
 * - Thought: Reasoning about the situation
 * - Goal: High-level objective (e.g., "Find food")
 * - Plan: Sequence of tasks to achieve goal
 *
 * Based on the Voyager and AutoGPT architectures.
 */
public class PlanningEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanningEngine.class);

    private final LLMProvider llmProvider;
    private final MemorySystem memorySystem;
    private final Deque<Goal> activeGoals;
    private final Map<UUID, Goal> allGoals;

    // Planning parameters
    private final int maxGoals = 5;
    private final int planningInterval = 100; // ticks (~5 seconds)
    private int ticksSinceLastPlan = 0;

    public PlanningEngine(LLMProvider llmProvider, MemorySystem memorySystem) {
        this.llmProvider = llmProvider;
        this.memorySystem = memorySystem;
        this.activeGoals = new LinkedList<>();
        this.allGoals = new HashMap<>();
    }

    /**
     * Update planning (called every tick).
     */
    public void update(WorldState worldState) {
        ticksSinceLastPlan++;

        // Periodic planning
        if (ticksSinceLastPlan >= planningInterval) {
            replan(worldState);
            ticksSinceLastPlan = 0;
        }

        // Update active goals
        updateActiveGoals();
    }

    /**
     * Generate new plan based on current state.
     */
    public CompletableFuture<Goal> replan(WorldState worldState) {
        LOGGER.debug("Replanning...");

        // Build context for LLM
        String context = buildPlanningContext(worldState);

        // Generate plan using LLM
        LLMOptions options = LLMOptions.planning()
            .systemPrompt(getPlanningSystemPrompt());

        return llmProvider.complete(context, options)
            .thenApply(response -> {
                Goal goal = parsePlanFromResponse(response);
                if (goal != null) {
                    addGoal(goal);
                    LOGGER.info("Generated new goal: {}", goal.getDescription());

                    // Store planning event in memory
                    memorySystem.store(new Memory(
                        Memory.MemoryType.PLANNING,
                        "Planned: " + goal.getDescription(),
                        0.7
                    ));
                }
                return goal;
            })
            .exceptionally(e -> {
                LOGGER.error("Planning failed", e);
                return null;
            });
    }

    /**
     * Add a new goal.
     */
    public void addGoal(Goal goal) {
        if (activeGoals.size() >= maxGoals) {
            // Remove lowest priority goal
            Goal lowest = activeGoals.stream()
                .min(Comparator.comparingInt(Goal::getPriority))
                .orElse(null);

            if (lowest != null && lowest.getPriority() < goal.getPriority()) {
                activeGoals.remove(lowest);
                lowest.setStatus(Goal.GoalStatus.CANCELLED);
            } else {
                LOGGER.warn("Cannot add goal - at max capacity and no lower priority goals");
                return;
            }
        }

        activeGoals.addFirst(goal);
        allGoals.put(goal.getId(), goal);
    }

    /**
     * Get current top priority goal.
     */
    public Optional<Goal> getCurrentGoal() {
        return activeGoals.stream()
            .filter(g -> g.getStatus() == Goal.GoalStatus.IN_PROGRESS || g.getStatus() == Goal.GoalStatus.PENDING)
            .findFirst();
    }

    /**
     * Get all active goals.
     */
    public List<Goal> getActiveGoals() {
        return new ArrayList<>(activeGoals);
    }

    /**
     * Complete a goal.
     */
    public void completeGoal(UUID goalId) {
        Goal goal = allGoals.get(goalId);
        if (goal != null) {
            goal.setStatus(Goal.GoalStatus.COMPLETED);
            activeGoals.remove(goal);
            LOGGER.info("Completed goal: {}", goal.getDescription());

            // Store in memory
            memorySystem.store(new Memory(
                Memory.MemoryType.ACHIEVEMENT,
                "Completed: " + goal.getDescription(),
                0.9
            ));
        }
    }

    /**
     * Fail a goal.
     */
    public void failGoal(UUID goalId, String reason) {
        Goal goal = allGoals.get(goalId);
        if (goal != null) {
            goal.setStatus(Goal.GoalStatus.FAILED);
            activeGoals.remove(goal);
            LOGGER.warn("Failed goal: {} (reason: {})", goal.getDescription(), reason);

            // Store in memory
            memorySystem.store(new Memory(
                Memory.MemoryType.FAILURE,
                "Failed: " + goal.getDescription() + " - " + reason,
                0.6
            ));
        }
    }

    /**
     * Clear all goals.
     */
    public void clearGoals() {
        activeGoals.clear();
        allGoals.clear();
    }

    /**
     * Update active goals (check completion, progress subgoals).
     */
    private void updateActiveGoals() {
        Iterator<Goal> iter = activeGoals.iterator();
        while (iter.hasNext()) {
            Goal goal = iter.next();

            // Update status
            if (goal.getStatus() == Goal.GoalStatus.PENDING) {
                goal.setStatus(Goal.GoalStatus.IN_PROGRESS);
            }

            // Check if completed
            if (goal.isComplete()) {
                goal.setStatus(Goal.GoalStatus.COMPLETED);
                iter.remove();
                LOGGER.info("Goal completed: {}", goal.getDescription());
            }

            // Check if failed (timeout or impossible)
            if (goal.getStatus() == Goal.GoalStatus.FAILED) {
                iter.remove();
            }
        }
    }

    /**
     * Build context for LLM planning.
     */
    private String buildPlanningContext(WorldState worldState) {
        StringBuilder context = new StringBuilder();

        // Current status
        context.append("## Current Status\n");
        context.append(String.format("Position: %.1f, %.1f, %.1f\n",
            worldState.getPlayerPosition().x,
            worldState.getPlayerPosition().y,
            worldState.getPlayerPosition().z));
        context.append(String.format("Health: %.1f/20\n", worldState.getHealth()));
        context.append(String.format("Hunger: %.1f/20\n", worldState.getHunger()));
        context.append("\n");

        // Nearby entities
        if (!worldState.getNearbyEntities().isEmpty()) {
            context.append("## Nearby Entities\n");
            worldState.getNearbyEntities().stream()
                .limit(5)
                .forEach(e -> context.append(String.format("- %s (distance: %.1f)\n",
                    e.getName(), worldState.getPlayerPosition().distanceTo(e.getPosition()))));
            context.append("\n");
        }

        // Recent memories
        context.append("## Recent Memories\n");
        List<Memory> recentMemories = memorySystem.getWorkingMemory().getRecent(5);
        if (recentMemories.isEmpty()) {
            context.append("(none)\n");
        } else {
            for (Memory memory : recentMemories) {
                context.append(String.format("- [%s] %s\n",
                    memory.getType(), memory.getContent()));
            }
        }
        context.append("\n");

        // Current goals
        if (!activeGoals.isEmpty()) {
            context.append("## Current Goals\n");
            for (Goal goal : activeGoals) {
                context.append(String.format("- [%s] %s (priority: %d)\n",
                    goal.getStatus(), goal.getDescription(), goal.getPriority()));
            }
            context.append("\n");
        }

        // Request
        context.append("## Task\n");
        context.append("Based on the current situation, what should I do next?\n");
        context.append("Provide your response in this format:\n");
        context.append("THOUGHT: <your reasoning about the situation>\n");
        context.append("GOAL: <high-level goal description>\n");
        context.append("PRIORITY: <1-10>\n");
        context.append("TASKS: <list of specific tasks to complete the goal>\n");

        return context.toString();
    }

    /**
     * Get system prompt for planning.
     */
    private String getPlanningSystemPrompt() {
        return "You are an AI assistant helping a Minecraft player make decisions. " +
            "Your job is to analyze the current situation and suggest goals and tasks. " +
            "Be strategic and prioritize survival (health, food) over exploration. " +
            "Keep goals concrete and achievable. " +
            "Available actions: move, mine, build, fight, collect items, eat, craft.";
    }

    /**
     * Parse goal from LLM response.
     */
    private Goal parsePlanFromResponse(String response) {
        try {
            // Extract fields using regex
            String thought = extractField(response, "THOUGHT:");
            String goalDesc = extractField(response, "GOAL:");
            String priorityStr = extractField(response, "PRIORITY:");
            String tasksStr = extractField(response, "TASKS:");

            if (goalDesc == null || goalDesc.trim().isEmpty()) {
                LOGGER.warn("Failed to parse goal from response");
                return null;
            }

            // Parse priority
            int priority = 5; // default
            if (priorityStr != null) {
                try {
                    priority = Integer.parseInt(priorityStr.trim());
                    priority = Math.max(1, Math.min(10, priority)); // Clamp to 1-10
                } catch (NumberFormatException e) {
                    LOGGER.warn("Failed to parse priority: {}", priorityStr);
                }
            }

            // Determine goal type from description
            Goal.GoalType type = inferGoalType(goalDesc);

            // Create goal
            Goal goal = new Goal(goalDesc, type, priority);

            // Parse tasks (simplified - just store as description for now)
            // In Phase 4, we'll create actual Task objects
            if (tasksStr != null && !tasksStr.trim().isEmpty()) {
                LOGGER.debug("Planned tasks: {}", tasksStr);
            }

            if (thought != null && !thought.trim().isEmpty()) {
                LOGGER.debug("LLM reasoning: {}", thought);
            }

            return goal;

        } catch (Exception e) {
            LOGGER.error("Failed to parse plan from response", e);
            return null;
        }
    }

    /**
     * Extract field from response text.
     */
    private String extractField(String text, String fieldName) {
        Pattern pattern = Pattern.compile(fieldName + "\\s*(.+?)(?=\\n[A-Z]+:|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Infer goal type from description.
     */
    private Goal.GoalType inferGoalType(String description) {
        String lower = description.toLowerCase();

        if (lower.contains("food") || lower.contains("eat") || lower.contains("hunger")) {
            return Goal.GoalType.SURVIVAL;
        } else if (lower.contains("mine") || lower.contains("collect") || lower.contains("gather")) {
            return Goal.GoalType.RESOURCE_GATHERING;
        } else if (lower.contains("build") || lower.contains("craft") || lower.contains("construct")) {
            return Goal.GoalType.BUILD;
        } else if (lower.contains("fight") || lower.contains("kill") || lower.contains("attack")) {
            return Goal.GoalType.COMBAT;
        } else if (lower.contains("explore") || lower.contains("find") || lower.contains("search")) {
            return Goal.GoalType.EXPLORATION;
        } else if (lower.contains("interact") || lower.contains("talk") || lower.contains("player")) {
            return Goal.GoalType.SOCIAL;
        } else {
            return Goal.GoalType.EXPLORATION;
        }
    }
}
