# Action Execution Debug Plan

## Problem Statement
AI player (TestBot) generates goals using LLM but doesn't execute actions:
- ✅ Goals are generated ("Maintain safe distance...", "Gather resources...")
- ❌ No actual movement, mining, building, or combat happening
- Player just stands still

## Current Execution Flow

```
AIPlayerEntity.tick()
  → AIPlayerEntity.updateAI()
    → WorldPerceptionEngine.perceiveWorld() → WorldState
    → ActionController.update()
    → AIPlayerBrain.update(worldState)
      → storePerceptionMemories()
      → makeIntelligentDecision(worldState)
        → PlanningEngine.update(worldState)
          → replan() [async LLM call]
          → updateActiveGoals()
          → getCurrentGoal()
        → executeGoal(goal, worldState)
          → executeSurvivalGoal() / executeExplorationGoal() / etc.
            → makeSimpleDecision(worldState) [fallback]
              → moveTowardsTarget(worldState)
                → player.setAIMovement(dx, dz)
```

## Suspected Issues

### 1. **Goal Execution is Incomplete**
- Line `AIPlayerBrain.java:155`: `switch (goal.getType())` only has basic stubs
- Most goal executors fall back to `makeSimpleDecision()` or `executeExplorationGoal()`
- `executeExplorationGoal()` just calls `makeSimpleDecision()` which does random walk

### 2. **ActionController Not Used**
- `ActionController` exists and is initialized
- Has `MovementController`, `MiningController`, etc.
- **BUT** `AIPlayerBrain` doesn't call any ActionController methods!
- Goals should use: `actionController.movement().walkTo(pos)`
- Instead using: `player.setAIMovement(dx, dz)` directly

### 3. **Movement May Not Apply**
- `setAIMovement()` sets internal fields but might not apply velocity
- Need to verify `AIPlayerEntity.applyAIMovement()` is working

### 4. **Async Goal Generation**
- `replan()` returns `CompletableFuture<Goal>`
- Goals are added asynchronously
- Might be race condition or goals not being retrieved properly

## Logging Strategy

### Level 1: Entry/Exit Points (INFO)
```java
LOGGER.info("[BRAIN] Starting update - Tick: {}", tickCount);
LOGGER.info("[BRAIN] Decision made - Mode: {}, Goal: {}", mode, goalDesc);
LOGGER.info("[ACTION] Movement started - Target: {}", target);
LOGGER.info("[ACTION] Action completed - Type: {}, Result: {}", type, result);
```

### Level 2: Execution Flow (DEBUG)
```java
LOGGER.debug("[BRAIN] Perception stored - Memories: {}", memCount);
LOGGER.debug("[BRAIN] Planning engine updated - Goals: {}", goalCount);
LOGGER.debug("[BRAIN] Executing goal - Type: {}, Status: {}", type, status);
LOGGER.debug("[PLAN] Goal retrieved - Priority: {}, Desc: {}", priority, desc);
LOGGER.debug("[ACTION] Controller update - Moving: {}, Mining: {}", moving, mining);
```

### Level 3: Data Details (TRACE - if needed)
```java
LOGGER.trace("[BRAIN] WorldState - Pos: {}, Health: {}, Entities: {}", pos, health, entities);
LOGGER.trace("[PLAN] LLM Context: {}", context);
LOGGER.trace("[ACTION] Movement vector - dx: {}, dz: {}", dx, dz);
```

### Error Handling (ERROR/WARN)
```java
LOGGER.error("[BRAIN] Failed to execute goal: {}", goal, exception);
LOGGER.warn("[BRAIN] No active goals - falling back to simple behavior");
LOGGER.warn("[ACTION] Path not found - Target: {}", target);
```

## Implementation Plan

### Phase 1: Add Comprehensive Logging
1. **AIPlayerBrain.java**
   - Entry/exit of `update()`
   - Goal execution start/end
   - Decision branch taken (intelligent vs simple)
   - ActionController method calls (ADD THESE!)

2. **PlanningEngine.java**
   - Goal generation (LLM call + response)
   - Goal priority queue state
   - Current goal retrieval

3. **ActionController.java**
   - Update cycle
   - Each action initiation
   - Action completion/failure

4. **MovementController.java**
   - Path calculation
   - Movement application
   - Arrival at destination

5. **AIPlayerEntity.java**
   - Tick cycle (every 10 ticks)
   - Movement application
   - Velocity changes

### Phase 2: Fix Action Execution
Based on logs, likely fixes:

1. **Connect Goals → ActionController**
   ```java
   // In executeExplorationGoal():
   Vec3d target = findExplorationTarget(worldState);
   actionController.movement().walkTo(target);
   LOGGER.info("[BRAIN] Started exploration movement to {}", target);
   ```

2. **Fix Goal Execution Logic**
   - Implement actual logic in `executeSurvivalGoal()`, etc.
   - Use ActionController methods instead of direct movement
   - Check ActionController.isBusy() before starting new actions

3. **Verify Movement Application**
   - Ensure `applyAIMovement()` actually sets velocity
   - Check if entity physics applies the movement
   - Verify no other system is overriding movement

### Phase 3: Test & Iterate
1. Spawn TestBot
2. Watch logs to trace execution
3. Identify exact point where execution stops
4. Fix and repeat

## Expected Log Output (Working)

```
[INFO] [BRAIN] Starting update - Health: 20.0, Hunger: 20
[DEBUG] [BRAIN] Perception stored - 3 new memories
[DEBUG] [PLAN] Planning engine updated - Active goals: 1
[DEBUG] [PLAN] Current goal: EXPLORATION - "Explore safely"
[INFO] [BRAIN] Executing goal: EXPLORATION
[INFO] [ACTION] Movement started - Target: (100, 64, 200)
[DEBUG] [ACTION] Path calculated - 15 waypoints
[DEBUG] [ACTION] Applying movement - Velocity: (0.2, 0, 0.15)
[INFO] [ACTION] Movement in progress - Distance: 45.2 blocks
```

## Success Criteria

- [x] Can trace complete execution from Brain → Action (logging added)
- [x] Can see which goal type is being executed (logging added)
- [ ] Can see ActionController methods being called (need to fix goal execution first)
- [x] Can see actual movement velocity being applied (logging added)
- [ ] TestBot visibly moves in the world (need testing)
- [x] Logs show clear execution path (logging complete)

## Phase 1 Complete ✓

Comprehensive logging has been added to all components:
- ✅ AIPlayerBrain.java - Goal execution, decision modes, memory storage
- ✅ PlanningEngine.java - LLM calls, goal parsing, queue management, context building
- ✅ ActionController.java - Update cycles, action states, controller status
- ✅ MovementController.java - Pathfinding, movement direction, waypoints, velocity
- ✅ AIPlayerEntity.java - Tick cycles, movement application, velocity changes

All logs use consistent prefixes: `[BRAIN]`, `[PLAN]`, `[ACTION]`, `[MOVEMENT]`, `[ENTITY]`

## Next Steps After Debugging

Once we identify the issue:
1. Fix goal → action execution
2. Implement proper ActionController usage
3. Test each action type (movement, mining, combat)
4. Then move to chat system implementation
