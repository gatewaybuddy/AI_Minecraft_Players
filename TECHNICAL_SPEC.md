# Technical Specification - AI Minecraft Player

## 1. Project Structure

```
AI_Minecraft_Players/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── aiplayer/
│       │           ├── core/
│       │           │   ├── AIPlayerEntity.java
│       │           │   ├── AIPlayerManager.java
│       │           │   └── PlayerAuthenticator.java
│       │           ├── perception/
│       │           │   ├── WorldPerceptionEngine.java
│       │           │   ├── EntityDetector.java
│       │           │   ├── BlockScanner.java
│       │           │   └── InventoryMonitor.java
│       │           ├── action/
│       │           │   ├── ActionController.java
│       │           │   ├── MovementController.java
│       │           │   ├── MiningController.java
│       │           │   ├── BuildingController.java
│       │           │   ├── CombatController.java
│       │           │   ├── PathfindingEngine.java
│       │           │   ├── InventoryManager.java
│       │           │   └── CraftingController.java
│       │           ├── memory/
│       │           │   ├── MemorySystem.java
│       │           │   ├── WorkingMemory.java
│       │           │   ├── EpisodicMemory.java
│       │           │   └── SemanticMemory.java
│       │           ├── planning/
│       │           │   ├── PlanningEngine.java
│       │           │   ├── Goal.java
│       │           │   ├── Task.java
│       │           │   ├── Plan.java
│       │           │   └── SkillLibrary.java
│       │           ├── communication/
│       │           │   ├── ChatListener.java
│       │           │   ├── NLUEngine.java
│       │           │   ├── DialogueManager.java
│       │           │   └── ResponseGenerator.java
│       │           ├── llm/
│       │           │   ├── LLMProvider.java
│       │           │   ├── OpenAIProvider.java
│       │           │   ├── ClaudeProvider.java
│       │           │   ├── LocalLLMProvider.java
│       │           │   └── LLMCache.java
│       │           ├── config/
│       │           │   └── AIPlayerConfig.java
│       │           └── AIPlayerMod.java
│       └── resources/
│           ├── fabric.mod.json
│           ├── assets/
│           │   └── aiplayer/
│           │       └── icon.png
│           └── data/
│               └── aiplayer/
│                   ├── config/
│                   │   └── default.json
│                   └── skills/
│                       └── basic_skills.json
├── gradle/
├── build.gradle
├── gradle.properties
├── settings.gradle
├── README.md
├── PROJECT_PLAN.md
├── TECHNICAL_SPEC.md
└── LICENSE
```

## 2. Core Data Structures

### 2.1 World State
```java
public class WorldState {
    private final Vec3d playerPosition;
    private final Vec3d lookDirection;
    private final float health;
    private final float hunger;
    private final int experience;

    private final List<EntityInfo> nearbyEntities;
    private final Map<BlockPos, BlockState> visibleBlocks;
    private final Inventory inventory;
    private final Set<PlayerInfo> nearbyPlayers;

    private final long timestamp;
    private final String dimension;

    // Methods
    public Optional<EntityInfo> findNearestEntity(Predicate<EntityInfo> filter);
    public List<BlockPos> findBlocksOfType(Block blockType, int radius);
    public boolean canSeePosition(BlockPos pos);
    public double distanceTo(BlockPos pos);
}
```

### 2.2 Actions
```java
public interface Action {
    String getName();
    boolean canExecute(WorldState state, AIPlayerEntity player);
    CompletableFuture<ActionResult> execute(AIPlayerEntity player);
    double getEstimatedDuration();
    int getPriority();
    List<String> getPrerequisites();
}

public class ActionResult {
    private final boolean success;
    private final String message;
    private final WorldState resultingState;
    private final Optional<Throwable> error;

    public static ActionResult success(WorldState newState) { ... }
    public static ActionResult failure(String reason, Throwable error) { ... }
}

// Example Action Implementation
public class MineBlockAction implements Action {
    private final BlockPos targetBlock;
    private final boolean requireCorrectTool;

    @Override
    public boolean canExecute(WorldState state, AIPlayerEntity player) {
        // Check if block exists, is reachable, has correct tool
        return state.findBlock(targetBlock).isPresent() &&
               state.distanceTo(targetBlock) <= 5.0 &&
               (requireCorrectTool ? hasCorrectTool(state, targetBlock) : true);
    }

    @Override
    public CompletableFuture<ActionResult> execute(AIPlayerEntity player) {
        return CompletableFuture.supplyAsync(() -> {
            // Look at block
            player.lookAt(targetBlock);

            // Select best tool
            selectBestTool(player, targetBlock);

            // Start mining
            return player.breakBlock(targetBlock);
        });
    }
}
```

### 2.3 Memory Structures
```java
public class Memory {
    private final UUID id;
    private final long timestamp;
    private final MemoryType type;
    private final String content;
    private final Map<String, Object> metadata;
    private final double importance; // 0.0 to 1.0

    // For semantic search
    private float[] embedding;
}

public enum MemoryType {
    OBSERVATION,      // Saw a player, found diamonds, etc.
    ACTION,          // What the AI did
    CONVERSATION,    // Chat messages
    GOAL_COMPLETION, // Finished a goal
    GOAL_FAILURE,    // Failed at a goal
    LEARNING,        // Learned strategy or fact
    RELATIONSHIP     // Interaction with player
}

public class EpisodicMemory {
    private final Deque<Memory> recentMemories; // Last N memories
    private final Map<String, List<Memory>> categorizedMemories;

    public void store(Memory memory);
    public List<Memory> recall(String query, int limit);
    public List<Memory> recallByTimeRange(long start, long end);
    public List<Memory> recallByType(MemoryType type, int limit);
}

public class SemanticMemory {
    private final Map<String, String> facts; // "spawn_location" -> "(100, 64, 200)"
    private final Map<String, Integer> playerRelationships; // Trust scores
    private final Map<String, Double> strategyRatings; // Strategy -> success rate

    public void learn(String key, String value);
    public Optional<String> retrieve(String key);
    public void updateRelationship(String playerName, int delta);
}
```

### 2.4 Planning Structures
```java
public class Goal {
    private final UUID id;
    private final String description;
    private final GoalType type;
    private final int priority;
    private final long createdAt;
    private final Optional<String> requestedBy; // Player who requested

    private GoalStatus status;
    private List<Subgoal> subgoals;

    public enum GoalType {
        SURVIVAL,      // Stay alive, get food
        EXPLORATION,   // Explore the world
        RESOURCE,      // Gather specific resources
        BUILDING,      // Build structures
        SOCIAL,        // Interact with players
        COMBAT,        // Fight mobs/players
        PLAYER_REQUEST // Fulfill player request
    }

    public enum GoalStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED, ABANDONED
    }
}

public class Task {
    private final String description;
    private final List<Action> actions;
    private final Predicate<WorldState> successCondition;

    private int currentActionIndex;
    private TaskStatus status;

    public boolean isComplete(WorldState state) {
        return successCondition.test(state);
    }
}

public class Plan {
    private final Goal goal;
    private final List<Task> tasks;
    private final String generatedBy; // "llm" or "cached" or "heuristic"

    private int currentTaskIndex;

    public Task getCurrentTask();
    public void markTaskComplete();
    public boolean isComplete();
}
```

### 2.5 Skill Library
```java
public class Skill {
    private final String name;
    private final String description;
    private final String code; // Executable Java code or JSON config
    private final List<String> prerequisites;
    private final double successRate;
    private final int timesUsed;

    public ActionResult execute(AIPlayerEntity player, Map<String, Object> params);
}

public class SkillLibrary {
    private final Map<String, Skill> skills;
    private final Path storageDirectory;

    public void learnSkill(Skill skill);
    public Optional<Skill> getSkill(String name);
    public List<Skill> getApplicableSkills(Goal goal);
    public void updateSkillRating(String name, boolean success);

    // Voyager-inspired: LLM generates and refines skills
    public CompletableFuture<Skill> generateSkill(String description, WorldState state);
}
```

## 3. LLM Integration

### 3.1 LLM Provider Interface
```java
public interface LLMProvider {
    CompletableFuture<String> complete(String prompt, LLMOptions options);
    CompletableFuture<List<String>> completeBatch(List<String> prompts, LLMOptions options);
    boolean isAvailable();
    String getModelName();
}

public class LLMOptions {
    private double temperature = 0.7;
    private int maxTokens = 1000;
    private double topP = 1.0;
    private List<String> stopSequences = new ArrayList<>();
    private String systemPrompt;
}
```

### 3.2 Prompt Templates
```java
public class PromptTemplates {

    public static String buildPlanningPrompt(Goal goal, WorldState state, List<Memory> relevantMemories) {
        return String.format("""
            You are an AI player in Minecraft. Your current goal is: %s

            Current State:
            - Position: %s
            - Inventory: %s
            - Health: %.1f/20
            - Nearby blocks: %s
            - Nearby entities: %s

            Relevant past experiences:
            %s

            Generate a detailed step-by-step plan to achieve this goal. Format:
            1. [Action] - [Reason]
            2. [Action] - [Reason]
            ...

            Plan:
            """,
            goal.getDescription(),
            state.getPlayerPosition(),
            state.getInventory().summarize(),
            state.getHealth(),
            state.getNearbyBlocks().summarize(),
            state.getNearbyEntities().summarize(),
            formatMemories(relevantMemories)
        );
    }

    public static String buildChatResponsePrompt(String message, String sender, ConversationContext ctx) {
        return String.format("""
            You are an AI player named %s in Minecraft. You are %s.

            Current activity: %s
            Current location: %s

            Recent conversation:
            %s

            %s said: "%s"

            Respond naturally and helpfully. Keep responses concise (1-2 sentences).
            If they're asking you to do something, confirm you'll do it.

            Response:
            """,
            ctx.getAIName(),
            ctx.getPersonality(),
            ctx.getCurrentGoal().map(Goal::getDescription).orElse("exploring"),
            ctx.getCurrentPosition(),
            ctx.getRecentMessages(),
            sender,
            message
        );
    }

    public static String buildSkillGenerationPrompt(String skillDescription, WorldState state) {
        return String.format("""
            Generate JavaScript code for a Minecraft bot skill: %s

            Available APIs:
            - bot.dig(block) - Mine a block
            - bot.placeBlock(block, position) - Place a block
            - bot.pathfinder.goto(position) - Navigate to position
            - bot.equip(item) - Equip an item
            - bot.craft(recipe, count) - Craft items
            - bot.attack(entity) - Attack an entity

            Current state:
            - Position: %s
            - Inventory: %s

            Write efficient, robust code with error handling.

            Code:
            ```javascript
            async function %s(bot, params) {
            """,
            skillDescription,
            state.getPlayerPosition(),
            state.getInventory().summarize(),
            toFunctionName(skillDescription)
        );
    }
}
```

### 3.3 LLM Caching Strategy
```java
public class LLMCache {
    private final Cache<String, String> cache;
    private final int maxSize = 1000;
    private final Duration ttl = Duration.ofHours(24);

    public LLMCache() {
        this.cache = Caffeine.newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(ttl)
            .build();
    }

    public Optional<String> get(String prompt) {
        return Optional.ofNullable(cache.getIfPresent(hashPrompt(prompt)));
    }

    public void put(String prompt, String response) {
        cache.put(hashPrompt(prompt), response);
    }

    private String hashPrompt(String prompt) {
        // Fuzzy matching: similar prompts return same hash
        String normalized = prompt.toLowerCase()
            .replaceAll("\\d+", "N")  // Replace numbers
            .replaceAll("\\s+", " ");  // Normalize whitespace
        return DigestUtils.sha256Hex(normalized);
    }
}
```

## 4. Pathfinding Implementation

### 4.1 A* Pathfinding
```java
public class PathfindingEngine {
    private final int maxIterations = 10000;
    private final double jumpCost = 1.5;
    private final double diagonalCost = 1.414;

    public Optional<Path> findPath(BlockPos start, BlockPos goal, World world) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Set<BlockPos> closedSet = new HashSet<>();
        Map<BlockPos, Node> nodes = new HashMap<>();

        Node startNode = new Node(start, 0, heuristic(start, goal));
        openSet.add(startNode);
        nodes.put(start, startNode);

        int iterations = 0;
        while (!openSet.isEmpty() && iterations++ < maxIterations) {
            Node current = openSet.poll();

            if (current.pos.equals(goal)) {
                return Optional.of(reconstructPath(current));
            }

            closedSet.add(current.pos);

            for (BlockPos neighbor : getNeighbors(current.pos, world)) {
                if (closedSet.contains(neighbor)) continue;

                double moveCost = calculateMoveCost(current.pos, neighbor, world);
                double tentativeG = current.gScore + moveCost;

                Node neighborNode = nodes.computeIfAbsent(neighbor,
                    pos -> new Node(pos, Double.POSITIVE_INFINITY, heuristic(pos, goal)));

                if (tentativeG < neighborNode.gScore) {
                    neighborNode.parent = current;
                    neighborNode.gScore = tentativeG;
                    neighborNode.fScore = tentativeG + neighborNode.hScore;

                    if (!openSet.contains(neighborNode)) {
                        openSet.add(neighborNode);
                    }
                }
            }
        }

        return Optional.empty(); // No path found
    }

    private double heuristic(BlockPos a, BlockPos b) {
        // Euclidean distance with vertical penalty
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx*dx + dy*dy*4 + dz*dz); // Vertical movement costs more
    }

    private List<BlockPos> getNeighbors(BlockPos pos, World world) {
        List<BlockPos> neighbors = new ArrayList<>();

        // 8 horizontal directions + up/down
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                BlockPos neighbor = pos.add(dx, 0, dz);
                if (isWalkable(neighbor, world)) {
                    neighbors.add(neighbor);
                }

                // Can jump up one block
                BlockPos jumpUp = pos.add(dx, 1, dz);
                if (isWalkable(jumpUp, world)) {
                    neighbors.add(jumpUp);
                }
            }
        }

        // Can fall down
        BlockPos below = pos.down();
        if (isWalkable(below, world)) {
            neighbors.add(below);
        }

        return neighbors;
    }

    private boolean isWalkable(BlockPos pos, World world) {
        BlockState state = world.getBlockState(pos);
        BlockState above = world.getBlockState(pos.up());
        BlockState twoAbove = world.getBlockState(pos.up(2));

        // Can stand on solid blocks with 2 air blocks above
        return !state.isAir() &&
               state.getCollisionShape(world, pos).isEmpty() == false &&
               above.isAir() &&
               twoAbove.isAir();
    }
}

class Node {
    BlockPos pos;
    double gScore; // Cost from start
    double hScore; // Heuristic to goal
    double fScore; // Total score
    Node parent;

    Node(BlockPos pos, double gScore, double hScore) {
        this.pos = pos;
        this.gScore = gScore;
        this.hScore = hScore;
        this.fScore = gScore + hScore;
    }
}
```

## 5. Minecraft-Specific Implementations

### 5.1 FakePlayer Creation
```java
public class AIPlayerEntity extends ServerPlayerEntity {
    private final AIPlayerBrain brain;
    private final UUID aiPlayerId;

    public AIPlayerEntity(MinecraftServer server, ServerWorld world, GameProfile profile) {
        super(server, world, profile);
        this.aiPlayerId = UUID.randomUUID();
        this.brain = new AIPlayerBrain(this);
    }

    @Override
    public void tick() {
        super.tick();

        // AI decision making each tick
        if (server.getTicks() % 10 == 0) { // Every 0.5 seconds
            brain.update(new WorldState(this));
        }
    }

    // Expose control methods
    public void moveToward(Vec3d target) {
        Vec3d direction = target.subtract(getPos()).normalize();
        this.setVelocity(direction.multiply(0.2));

        // Update control states
        this.input.movementForward = (float) direction.z;
        this.input.movementSideways = (float) direction.x;
    }

    public CompletableFuture<Boolean> mineBlock(BlockPos pos) {
        return CompletableFuture.supplyAsync(() -> {
            // Look at block
            lookAt(Vec3d.ofCenter(pos));

            // Start mining
            this.interactionManager.tryBreakBlock(pos);

            // Wait for block to break
            while (!world.getBlockState(pos).isAir()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    return false;
                }
            }

            return true;
        });
    }
}
```

### 5.2 Chat Integration
```java
public class ChatListener {
    private final AIPlayerEntity player;
    private final ResponseGenerator responseGenerator;
    private final DialogueManager dialogueManager;

    public void onChatMessage(ServerPlayNetworkHandler handler, String message) {
        String sender = handler.player.getName().getString();

        // Check if message is directed at AI
        if (shouldRespond(message)) {
            CompletableFuture.runAsync(() -> {
                ConversationContext ctx = dialogueManager.getContext(sender);
                String response = responseGenerator.generateResponse(message, sender, ctx);

                // Send response after realistic delay
                try {
                    Thread.sleep(1000 + new Random().nextInt(1000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                player.sendMessage(Text.of(response), false);
            });
        }

        // Process as potential command/request
        processCommand(sender, message);
    }

    private boolean shouldRespond(String message) {
        String playerName = player.getName().getString().toLowerCase();
        message = message.toLowerCase();

        // Respond if mentioned by name, or in direct conversation
        return message.contains(playerName) ||
               message.startsWith("@" + playerName) ||
               dialogueManager.isInConversation(player.getUuid());
    }

    private void processCommand(String sender, String message) {
        // Parse natural language commands
        if (message.matches(".*\\b(get|gather|collect|mine)\\b.*")) {
            extractResourceRequest(sender, message);
        } else if (message.matches(".*\\b(build|create|make)\\b.*")) {
            extractBuildRequest(sender, message);
        } else if (message.matches(".*\\b(follow|come here)\\b.*")) {
            createFollowGoal(sender);
        }
    }
}
```

## 6. Configuration System

### 6.1 Config File Format
```java
public class AIPlayerConfig {
    private String username = "AISteve";
    private String personality = "helpful and friendly";

    private LLMConfig llm = new LLMConfig();
    private BehaviorConfig behavior = new BehaviorConfig();
    private GoalsConfig goals = new GoalsConfig();
    private MemoryConfig memory = new MemoryConfig();

    public static class LLMConfig {
        private String provider = "openai"; // openai, claude, local
        private String model = "gpt-4";
        private String apiKey = "";
        private String localModelUrl = "http://localhost:11434";
        private int maxTokens = 1000;
        private double temperature = 0.7;
    }

    public static class BehaviorConfig {
        private int reactionTimeMs = 200;
        private boolean movementHumanization = true;
        private boolean chatEnabled = true;
        private boolean autoRespawn = true;
        private int actionCacheSize = 100;
    }

    public static class GoalsConfig {
        private String defaultGoal = "explore and gather resources";
        private boolean acceptPlayerRequests = true;
        private int maxActiveGoals = 3;
        private boolean autonomousGoalGeneration = true;
    }

    public static class MemoryConfig {
        private String storageType = "json"; // json, sqlite, vector
        private int maxEpisodicMemories = 1000;
        private boolean enableSemanticSearch = false;
        private String vectorDbUrl = "";
    }

    public static AIPlayerConfig load(Path configPath) {
        // Load from JSON file
    }

    public void save(Path configPath) {
        // Save to JSON file
    }
}
```

## 7. Performance Optimization Strategies

### 7.1 Action Batching
```java
public class ActionBatcher {
    private final Queue<Action> pendingActions = new ConcurrentLinkedQueue<>();
    private final int batchSize = 5;

    public void queueAction(Action action) {
        pendingActions.offer(action);
    }

    public CompletableFuture<List<ActionResult>> executeBatch() {
        List<Action> batch = new ArrayList<>();
        for (int i = 0; i < batchSize && !pendingActions.isEmpty(); i++) {
            batch.add(pendingActions.poll());
        }

        List<CompletableFuture<ActionResult>> futures = batch.stream()
            .map(action -> action.execute(player))
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }
}
```

### 7.2 Async LLM Calls
```java
public class AsyncPlanningEngine {
    private final ExecutorService llmExecutor = Executors.newFixedThreadPool(2);
    private final LLMProvider llm;
    private final LLMCache cache;

    public CompletableFuture<Plan> generatePlanAsync(Goal goal, WorldState state) {
        String prompt = PromptTemplates.buildPlanningPrompt(goal, state, getRelevantMemories(goal));

        // Check cache first
        return cache.get(prompt)
            .map(CompletableFuture::completedFuture)
            .orElseGet(() ->
                CompletableFuture.supplyAsync(() -> {
                    String response = llm.complete(prompt, new LLMOptions()).join();
                    cache.put(prompt, response);
                    return parsePlanFromResponse(response);
                }, llmExecutor)
            );
    }
}
```

## 8. Testing Strategy

### 8.1 Unit Tests
```java
@Test
public void testPathfindingFindsDirectPath() {
    World testWorld = createFlatWorld();
    PathfindingEngine engine = new PathfindingEngine();

    BlockPos start = new BlockPos(0, 64, 0);
    BlockPos goal = new BlockPos(10, 64, 10);

    Optional<Path> path = engine.findPath(start, goal, testWorld);

    assertTrue(path.isPresent());
    assertEquals(goal, path.get().getEnd());
    assertTrue(path.get().getLength() <= 15); // Should be roughly straight
}

@Test
public void testMiningActionRequiresCorrectTool() {
    WorldState state = createTestState();
    MineBlockAction action = new MineBlockAction(new BlockPos(0, 64, 0), true);

    // No pickaxe in inventory
    assertFalse(action.canExecute(state, testPlayer));

    // Add pickaxe
    state.getInventory().addItem(Items.IRON_PICKAXE);
    assertTrue(action.canExecute(state, testPlayer));
}
```

### 8.2 Integration Tests
```java
@Test
public void testAICanCompleteSimpleGoal() {
    TestServer server = new TestServer();
    AIPlayerEntity ai = server.spawnAIPlayer("TestAI");

    Goal goal = new Goal("Mine 10 oak logs", GoalType.RESOURCE);
    ai.getBrain().setGoal(goal);

    // Run for 5 minutes (in-game)
    server.tick(6000);

    int oakLogs = ai.getInventory().count(Items.OAK_LOG);
    assertTrue(oakLogs >= 10, "AI should have mined at least 10 oak logs");
    assertEquals(GoalStatus.COMPLETED, goal.getStatus());
}
```

## 9. Security & Safety

### 9.1 Action Validation
```java
public class ActionValidator {
    public boolean isSafe(Action action, WorldState state) {
        // Prevent griefing
        if (action instanceof PlaceBlockAction) {
            BlockPos pos = ((PlaceBlockAction) action).getPosition();
            if (isNearPlayerStructure(pos, state)) {
                return false; // Don't place blocks near other players' builds
            }
        }

        // Prevent dangerous actions
        if (action instanceof AttackAction) {
            Entity target = ((AttackAction) action).getTarget();
            if (target instanceof PlayerEntity && !pvpEnabled) {
                return false; // Don't attack players if PvP disabled
            }
        }

        return true;
    }

    private boolean isNearPlayerStructure(BlockPos pos, WorldState state) {
        // Check if position is near blocks placed by other players
        // This would require tracking block ownership
        return false; // Simplified
    }
}
```

### 9.2 Rate Limiting
```java
public class RateLimiter {
    private final Map<String, Long> lastApiCall = new ConcurrentHashMap<>();
    private final long minIntervalMs = 1000; // 1 second between LLM calls

    public boolean canMakeCall(String callType) {
        long now = System.currentTimeMillis();
        Long last = lastApiCall.get(callType);

        if (last == null || now - last >= minIntervalMs) {
            lastApiCall.put(callType, now);
            return true;
        }

        return false;
    }
}
```

## 10. Monitoring & Debugging

### 10.1 Logging
```java
public class AILogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("AIPlayer");

    public static void logGoal(Goal goal) {
        LOGGER.info("[GOAL] {}: {}", goal.getType(), goal.getDescription());
    }

    public static void logAction(Action action, ActionResult result) {
        if (result.isSuccess()) {
            LOGGER.debug("[ACTION] Completed: {}", action.getName());
        } else {
            LOGGER.warn("[ACTION] Failed: {} - {}", action.getName(), result.getMessage());
        }
    }

    public static void logLLMCall(String prompt, String response, long durationMs) {
        LOGGER.debug("[LLM] Call took {}ms, tokens: ~{}",
            durationMs, estimateTokens(prompt + response));
    }
}
```

### 10.2 Debug UI
```java
public class DebugOverlay {
    public void render(AIPlayerEntity player, MatrixStack matrices) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        int y = 10;
        drawText(matrices, textRenderer, "AI Player: " + player.getName().getString(), 10, y);
        y += 12;

        Goal currentGoal = player.getBrain().getCurrentGoal();
        if (currentGoal != null) {
            drawText(matrices, textRenderer, "Goal: " + currentGoal.getDescription(), 10, y);
            y += 12;
        }

        Task currentTask = player.getBrain().getCurrentTask();
        if (currentTask != null) {
            drawText(matrices, textRenderer, "Task: " + currentTask.getDescription(), 10, y);
            y += 12;
        }

        // Show memory stats
        MemorySystem memory = player.getBrain().getMemory();
        drawText(matrices, textRenderer,
            String.format("Memories: %d episodic, %d semantic",
                memory.getEpisodicCount(), memory.getSemanticCount()),
            10, y);
    }
}
```

---

This technical specification provides the implementation details needed to build the AI Minecraft Player mod. It should be read in conjunction with the PROJECT_PLAN.md document.
