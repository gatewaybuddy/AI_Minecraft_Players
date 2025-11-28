# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AI Minecraft Players is a Fabric mod that creates autonomous AI players for Minecraft 1.20.4. These AI players use Large Language Models (GPT-4, Claude, or local models via Ollama) to make intelligent decisions, remember experiences, and interact naturally with human players.

**Current Status**: Phase 3 complete (Memory, Planning, Skills). Phase 4 (Natural Language Communication) in progress.

## Build System

### Building the Mod

```bash
# Build the mod (creates JAR in build/libs/)
./gradlew build

# Clean build artifacts
./gradlew clean

# Run development client (for testing)
./gradlew runClient

# Run development server
./gradlew runServer
```

**Build output**: `build/libs/ai-minecraft-player-0.1.0-SNAPSHOT.jar`

### Dependencies

Key dependencies (defined in `build.gradle`):
- Fabric Loader 0.15.3 (mod framework)
- Fabric API 0.95.4+1.20.4 (Minecraft API)
- OkHttp 4.12.0 (HTTP client for LLM APIs)
- Gson 2.10.1 (JSON parsing)
- Caffeine 3.1.8 (LLM response caching)

### Testing

```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests "ClassName.methodName"
```

## Architecture Overview

The mod follows a **cognitive architecture** inspired by human decision-making:

### Core Data Flow

```
WorldState (Perception)
    ↓
AIPlayerBrain (Cognition)
    ↓
├─ MemorySystem (3-tier: Working + Episodic + Semantic)
├─ PlanningEngine (LLM-powered ReAct framework)
└─ SkillLibrary (Learned behaviors)
    ↓
ActionController (Execution)
    ↓
Movement/Mining/Building/Combat Controllers
```

### Package Structure

- `com.aiplayer.core/`: Core entity and brain systems
  - `AIPlayerEntity`: The fake player entity
  - `AIPlayerBrain`: Main decision-making loop (INTELLIGENT vs SIMPLE mode)
  - `AIPlayerManager`: Spawning and lifecycle management

- `com.aiplayer.llm/`: LLM integration layer
  - `LLMProvider`: Abstract interface for LLM backends
  - `OpenAIProvider`, `ClaudeProvider`, `LocalLLMProvider`: Implementations
  - `LLMCache`: Response caching to reduce API costs (Caffeine-based, 1 hour TTL)
  - `LLMFactory`: Provider creation from config

- `com.aiplayer.memory/`: Three-tier memory system
  - `MemorySystem`: Coordinator for all memory tiers
  - `WorkingMemory`: Short-term context (20 items, 10 min TTL)
  - `EpisodicMemory`: Event history (chronological, importance-ranked)
  - `SemanticMemory`: Facts, relationships, strategy ratings

- `com.aiplayer.planning/`: Goal and task management
  - `PlanningEngine`: LLM-powered planning (ReAct framework)
  - `Goal`: Hierarchical goals with types (SURVIVAL, EXPLORATION, COMBAT, etc.)
  - `Task`: Executable work units with action sequences

- `com.aiplayer.skills/`: Skill learning system (Voyager-inspired)
  - `SkillLibrary`: Storage and retrieval of learned behaviors
  - `Skill`: Individual skills with success tracking and quality scores

- `com.aiplayer.perception/`: World state perception
  - `WorldState`: Snapshot of environment (entities, blocks, inventory, stats)
  - `WorldPerceptionEngine`: Processes Minecraft world data

- `com.aiplayer.action/`: Action execution layer
  - `ActionController`: High-level action coordinator
  - `MovementController`, `MiningController`, `BuildingController`, `CombatController`
  - `PathfindingEngine`: A* pathfinding implementation
  - `InventoryManager`: Item management and tool selection

- `com.aiplayer.config/`: Configuration management
  - `AIPlayerConfig`: JSON-based configuration with LLM settings

### Operating Modes

The AI brain operates in two modes based on LLM availability:

1. **INTELLIGENT Mode** (with LLM):
   - Uses LLM for goal planning and decision-making
   - Stores memories and learns from experiences
   - Forms goals based on current situation (hunger → seek food, mobs → combat)
   - Executes goal-directed behavior using skills

2. **SIMPLE Mode** (fallback):
   - Random walk with obstacle avoidance
   - No LLM calls (free, always works)
   - Used when API key missing or LLM unavailable

## Key Implementation Details

### Memory System Design

The memory system uses a **three-tier architecture** based on human cognition:

- **Working Memory**: FIFO with 20-item capacity, stores recent context for LLM prompts
- **Episodic Memory**: Chronological deque with type-based indices, importance-based consolidation
- **Semantic Memory**: Key-value facts, player relationships (trust 0-100), strategy success rates

**Important**: Memories flow from episodic → working based on recency and importance. High-importance events (health < 6, nearby hostiles) are automatically stored.

### LLM Integration Pattern

All LLM providers return `CompletableFuture<String>` for async execution:

```java
LLMProvider llm = LLMFactory.create("openai", apiKey, "gpt-4-turbo", null, true);
CompletableFuture<String> future = llm.complete(prompt, LLMOptions.planning());
String response = future.get(); // Or use .thenApply() for non-blocking
```

**Caching**: All providers are automatically wrapped in `LLMCache` when created via `LLMFactory` with caching enabled. Cache keys include prompt + system prompt + temperature + model to ensure correctness.

### Planning Loop

The `PlanningEngine` runs every 5 seconds (configurable):

1. Build context: world state + recent memories + current goals
2. Send to LLM with ReAct-style prompt
3. Parse response for goals using regex: `GOAL:`, `PRIORITY:`, `TASKS:`
4. Add parsed goals to priority queue
5. Execute highest-priority goal via `AIPlayerBrain.executeGoal()`

**Important**: Goals are NOT fully decomposed into tasks yet (Phase 3 limitation). Phase 4 will add proper task decomposition and skill-based execution.

### Skill System

Skills track their own success rates and quality scores:

- **Success Rate**: `successes / totalUses`
- **Quality Score**: Weighted combination of success rate, experience (uses), and recency
- **Categories**: MINING, BUILDING, CRAFTING, COMBAT, SURVIVAL, EXPLORATION, SOCIAL, FARMING, UTILITY

**Phase 5 Enhancement**: Skills will be LLM-generated (AI writes its own code), composed, and refined based on failures.

### Configuration System

Config loads from `config/aiplayer.json` (or `config/aiplayer-config.json`). Falls back to defaults if missing.

**Critical**: The mod checks `llm.apiKey` to determine mode:
- Empty/null API key → SIMPLE mode (no LLM calls)
- Valid API key → INTELLIGENT mode (LLM-powered)

**LLM Providers**:
- `openai`: Requires `apiKey` starting with `sk-`
- `claude`: Requires `apiKey` starting with `sk-ant-`
- `local`: Uses Ollama at `localModelUrl` (default: http://localhost:11434), no API key needed

## Common Development Tasks

### Adding a New LLM Provider

1. Implement `LLMProvider` interface in `com.aiplayer.llm/`
2. Return `CompletableFuture<String>` for async execution
3. Add factory case in `LLMFactory.create()`
4. Update config schema in `AIPlayerConfig.LLMConfig`

### Adding a New Goal Type

1. Add enum value to `Goal.GoalType`
2. Add execution case in `AIPlayerBrain.executeGoal()`
3. Update `PlanningEngine` prompt to explain when to use new type
4. Add initial skill to `SkillLibrary` if needed

### Adding a New Memory Type

1. Add enum value to `Memory.MemoryType`
2. Update `AIPlayerBrain.storePerceptionMemories()` if auto-storing
3. Consider importance score (0.0-1.0) for prioritization

### Modifying LLM Prompts

LLM prompts are constructed in `PlanningEngine` methods:
- `buildPlanningContext()`: Context sent to LLM
- `buildSystemPrompt()`: System-level instructions
- Response parsing in `parseGoalFromResponse()` expects: `THOUGHT:`, `GOAL:`, `PRIORITY:`, `TASKS:`

**Important**: Keep prompts under ~500 tokens to fit within context limits. Use `memorySystem.formatRecentForContext(n)` for concise memory formatting.

## Important Constraints

### Minecraft Version Compatibility

This mod targets **Minecraft 1.20.4** with **Fabric Loader 0.15.3**. The codebase uses:
- Yarn mappings (`yarn_mappings` in gradle.properties)
- Fabric API 0.95.4+1.20.4
- Java 17+ (set in build.gradle)

When updating Minecraft versions, check Fabric compatibility and update all three versions together.

### Performance Considerations

- **Planning**: Runs every 5 seconds (not every tick) to reduce CPU/LLM costs
- **Memory**: O(1) storage, O(log n) search with indexed retrieval
- **LLM Cache**: 1-hour TTL, 1000-entry LRU cache (reduces costs by 50-80%)
- **Action Batching**: Group similar actions to reduce redundant LLM calls

**Per AI player overhead**: ~10-15 MB RAM, ~2-5% CPU with caching

### API Cost Management

With caching enabled (default), typical costs:
- **GPT-4 Turbo**: $0.36-0.90/hour (50-80% cache hit rate)
- **Claude 3.5 Sonnet**: $0.45-0.90/hour
- **Local (Ollama)**: $0 (requires local GPU, 2-5s response time)

**Cost Reduction Tips**:
1. Use `LLMOptions.planning()` preset (limits tokens to 1500)
2. Enable caching in `LLMFactory.create()` (5th parameter)
3. Increase planning interval in `AIPlayerBrain.DECISION_INTERVAL_TICKS`
4. Use cheaper models: `gpt-3.5-turbo`, `claude-3-haiku`, or local models

### Security & Safety

- **API Keys**: Never commit to Git. Use `.gitignore` for config files
- **Action Validation**: Future phases will validate actions to prevent griefing
- **Rate Limiting**: No rate limiting currently implemented (TODO for Phase 5)

## Debugging

### Logging

The mod uses SLF4J for logging:
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger LOGGER = LoggerFactory.getLogger(ClassName.class);
LOGGER.info("Message");
LOGGER.debug("Debug info");
LOGGER.warn("Warning");
LOGGER.error("Error", exception);
```

**Log locations**: Check server/client console output or `logs/latest.log`

### Checking AI Mode

```java
AIPlayerBrain brain = player.getBrain();
if (brain.isIntelligentMode()) {
    // LLM-powered mode
} else {
    // Simple random walk
}
```

### Inspecting Memory/Skills

```java
// Memory stats
MemorySystem memory = brain.getMemorySystem();
List<Memory> recent = memory.getRecentMemories(10);
Map<String, Integer> relationships = memory.getSemanticMemory().getAllRelationships();

// Skill stats
SkillLibrary skills = brain.getSkillLibrary();
LibraryStats stats = skills.getStats(); // Shows total skills, uses, success rate

// Cache stats (if using LLMCache)
if (llm instanceof LLMCache) {
    CacheStats cacheStats = ((LLMCache) llm).getStats();
    // Shows hits, misses, hit rate
}
```

### Commands

In-game commands for testing:
```
/aiplayer spawn [name]      - Spawn AI player
/aiplayer despawn <name>    - Remove AI player
/aiplayer list              - List all AI players
/aiplayer status <name>     - Show current goal/status
/aiplayer reload            - Reload configuration
```

## Code Style Notes

- **Async Patterns**: LLM calls use `CompletableFuture` to avoid blocking server tick
- **Null Safety**: Use `Optional<T>` for values that may be absent (goals, memories)
- **Immutability**: Core data structures (Memory, Goal) should be immutable where possible
- **Documentation**: Add Javadoc to public APIs, especially in core/ and llm/ packages

## References

Key research papers and projects that inform this implementation:

1. **ReAct Framework**: Reasoning and Acting in Language Models - Used in PlanningEngine
2. **Voyager**: Lifelong Learning Agent in Minecraft - Inspired SkillLibrary design
3. **Baritone**: Minecraft pathfinding - Reference for PathfindingEngine
4. **Atkinson-Shiffrin Model**: Human memory architecture - Basis for 3-tier memory system

See PROJECT_PLAN.md and TECHNICAL_SPEC.md for complete architectural details.
