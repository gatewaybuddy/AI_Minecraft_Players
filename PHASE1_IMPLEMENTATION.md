# Phase 1 Implementation - Foundation

This document describes the Phase 1 implementation of the AI Minecraft Player mod.

## Overview

Phase 1 (Tasks 1.1-1.8) establishes the foundation for the mod:
- ✅ Project structure and build system
- ✅ Configuration system
- ✅ AI player entity (FakePlayer)
- ✅ Simple AI brain with random walk behavior
- ✅ World perception engine
- ✅ Movement system
- ✅ Commands for spawning/managing AI players

## What's Implemented

### Core Components

#### 1. AIPlayerEntity (`src/main/java/com/aiplayer/core/AIPlayerEntity.java`)

A fully functional fake player entity that:
- Extends `ServerPlayerEntity` to integrate with Minecraft's player system
- Appears in the Tab player list
- Has its own AI brain for decision making
- Can move autonomously
- Perceives the world through `WorldPerceptionEngine`

**Key Features:**
- Ticks every 0.5 seconds to make AI decisions (optimized for performance)
- Smooth movement updates every tick
- Auto-respawn capability (configurable)
- Status reporting

#### 2. AIPlayerBrain (`src/main/java/com/aiplayer/core/AIPlayerBrain.java`)

The "brain" that controls AI behavior:
- **Phase 1**: Implements simple random walk with obstacle avoidance
- Makes decisions every second (20 ticks)
- Picks random movement targets 5-15 blocks away
- Moves towards targets smoothly

**Future Phases:**
- Phase 3: Goal-based planning with LLM integration
- Phase 4: Natural language understanding
- Phase 5: Skill library and learning

#### 3. WorldPerceptionEngine (`src/main/java/com/aiplayer/perception/WorldPerceptionEngine.java`)

Gathers information about the environment:
- Scans for nearby entities (32 block radius)
- Detects other players
- Scans blocks in a 16x8x16 area
- Creates immutable `WorldState` snapshots

**Optimization:**
- Only scans loaded chunks
- Skips air blocks
- Periodic updates (0.5 seconds)

#### 4. WorldState (`src/main/java/com/aiplayer/perception/WorldState.java`)

Immutable snapshot of the AI's understanding of the world:
- Player position, health, hunger, XP
- Nearby entities with type, position, hostility
- Nearby players with name and position
- Visible blocks (non-air only)

**Query Methods:**
- `findNearestEntity(predicate)` - Find closest matching entity
- `findBlocksOfType(id)` - Find specific block types
- `distanceTo(pos)` - Calculate distances
- `getBlockAt(pos)` - Get block state if visible

#### 5. AIPlayerManager (`src/main/java/com/aiplayer/core/AIPlayerManager.java`)

Manages all AI player instances:
- Spawn AI players with custom names and positions
- Despawn by name or UUID
- Track all active AI players
- Cleanup on server shutdown

**Thread-safe tracking:**
- Map by player name for commands
- Map by UUID for internal lookups

#### 6. Commands (`src/main/java/com/aiplayer/command/AIPlayerCommand.java`)

Full command system for managing AI players:

```
/aiplayer spawn <name>     - Spawn a new AI player
/aiplayer despawn <name>   - Despawn an AI player
/aiplayer list             - List all active AI players
/aiplayer status <name>    - Get detailed status
/aiplayer reload           - Reload configuration
```

**Permissions:**
- All commands require OP level 2

### Supporting Components

#### 7. Configuration System (`src/main/java/com/aiplayer/config/AIPlayerConfig.java`)

Complete configuration with:
- LLM provider settings (OpenAI, Claude, local)
- Behavior settings (reaction time, humanization, etc.)
- Goal settings (default goal, auto-generation, etc.)
- Memory settings (storage type, limits, etc.)

**Features:**
- JSON serialization with Gson
- Validation on load
- Default config generation
- Hot reload via `/aiplayer reload`

#### 8. FakeClientConnection (`src/main/java/com/aiplayer/core/FakeClientConnection.java`)

Dummy network connection for AI players:
- Allows AI players to integrate with `PlayerManager`
- Silently ignores packets (AI players don't need network I/O)
- Always reports as "connected"

### Mod Integration

#### Main Mod Class (`src/main/java/com/aiplayer/AIPlayerMod.java`)

- Initializes all components
- Registers commands via Fabric Command API v2
- Registers lifecycle events (server shutdown)
- Provides static access to config and manager

## Architecture Highlights

### Performance Optimization

1. **Tick Throttling:**
   - AI decision making: Every 1 second (20 ticks)
   - World perception: Every 0.5 seconds (10 ticks)
   - Movement updates: Every tick (for smooth movement)

2. **Efficient Scanning:**
   - Block scan: Only non-air blocks in loaded chunks
   - Entity scan: Bounded by 32 block radius
   - No redundant calculations

3. **Immutable State:**
   - WorldState is immutable (thread-safe)
   - No locks needed for concurrent access

### Design Patterns

1. **Manager Pattern:**
   - `AIPlayerManager` as central coordinator
   - Singleton per server

2. **Brain-Body Separation:**
   - `AIPlayerEntity` = body (Minecraft integration)
   - `AIPlayerBrain` = brain (decision making)
   - Clean separation of concerns

3. **Perception-Action Loop:**
   - Perceive → Decide → Act
   - Classic robotics/AI architecture

## Usage

### Spawning Your First AI Player

1. **Start Minecraft with the mod loaded:**
   ```bash
   ./gradlew runClient
   ```

2. **Open a world or join a server (with OP permissions)**

3. **Spawn an AI player:**
   ```
   /aiplayer spawn AISteve
   ```

4. **Watch it walk around randomly!**

### Managing AI Players

```bash
# List all AI players
/aiplayer list

# Check status
/aiplayer status AISteve

# Despawn
/aiplayer despawn AISteve

# Reload config (if you change settings)
/aiplayer reload
```

### Configuration

Edit `config/aiplayer.json`:

```json
{
  "username": "AISteve",
  "personality": "helpful and curious",
  "llm": {
    "provider": "openai",
    "apiKey": "YOUR_KEY_HERE"
  },
  "behavior": {
    "reactionTimeMs": 200,
    "movementHumanization": true,
    "autoRespawn": true
  }
}
```

## Testing Phase 1

### Basic Tests

1. **Spawn Test:**
   - `/aiplayer spawn TestAI`
   - AI should appear in player list
   - AI should start walking randomly

2. **Movement Test:**
   - AI should move smoothly
   - AI should avoid falling off cliffs (future phase)
   - AI should not get stuck in walls (future phase)

3. **Multiple AIs:**
   - Spawn 3-5 AI players
   - All should move independently
   - No crashes or lag spikes

4. **Despawn Test:**
   - `/aiplayer despawn TestAI`
   - AI should disappear from world
   - AI should disappear from player list

5. **Server Shutdown:**
   - Stop server
   - Should see "Cleaning up AI players..." in logs
   - No errors on shutdown

### Performance Tests

- **1 AI:** < 1ms tick time impact
- **5 AIs:** < 5ms tick time impact
- **10 AIs:** < 10ms tick time impact

Check with `/debug` command in game.

## Known Limitations (Phase 1)

These will be addressed in later phases:

1. **No pathfinding yet** - AIs walk in straight lines
   - **Phase 2**: A* pathfinding

2. **No obstacle avoidance** - AIs may walk into walls
   - **Phase 2**: Pathfinding with collision detection

3. **No mining/building** - AIs only move
   - **Phase 2**: Mining, building, inventory, crafting

4. **No goals** - AIs just wander randomly
   - **Phase 3**: Goal-based planning with LLM

5. **No chat** - AIs don't communicate
   - **Phase 4**: Natural language chat

6. **No learning** - AIs don't improve
   - **Phase 5**: Skill library and experience learning

## Next Steps - Phase 2 (Weeks 4-6)

Implement all player action capabilities:

- **Task 2.1**: Mining controller with tool selection
- **Task 2.2**: Building/placement controller
- **Task 2.3**: Inventory management system
- **Task 2.4**: Crafting controller
- **Task 2.5**: Combat controller (PvE and PvP)
- **Task 2.6**: Pathfinding engine (A* implementation)
- **Task 2.7**: Swimming, climbing, vehicle usage

**Milestone M2 Goal**: AI can mine, build, craft, and fight

## Code Statistics

- **Total Lines:** ~1,500 lines of Java
- **Classes:** 8 core classes
- **Packages:** 4 (core, perception, config, command)
- **Test Coverage:** TBD (Phase 6)

## File Structure

```
src/main/java/com/aiplayer/
├── AIPlayerMod.java                     # Main mod entry point
├── command/
│   └── AIPlayerCommand.java             # /aiplayer commands
├── config/
│   └── AIPlayerConfig.java              # Configuration system
├── core/
│   ├── AIPlayerEntity.java              # Fake player entity
│   ├── AIPlayerBrain.java               # Decision making
│   ├── AIPlayerManager.java             # Player management
│   └── FakeClientConnection.java        # Network stub
└── perception/
    ├── WorldPerceptionEngine.java       # Environment scanning
    └── WorldState.java                  # World state snapshot

src/main/resources/
├── fabric.mod.json                      # Mod metadata
├── aiplayer.mixins.json                 # Mixin config
└── data/aiplayer/config/
    └── default.json                     # Default configuration
```

## Dependencies

- Fabric Loader 0.15.3
- Fabric API 0.95.4
- Minecraft 1.20.4
- Java 17+
- Gson 2.10.1 (JSON)
- OkHttp 4.12.0 (HTTP, for future LLM integration)
- Caffeine 3.1.8 (Caching, for future LLM caching)

## Contributing

When contributing to Phase 1:

1. Follow the architecture in `TECHNICAL_SPEC.md`
2. Keep methods focused and documented
3. Use proper logging (LOGGER.info/debug/error)
4. Test with multiple AI players
5. Ensure no memory leaks (test long-running sessions)

## Troubleshooting

### "AI player spawns but doesn't move"

- Check logs for errors in `AIPlayerBrain.update()`
- Verify world is loaded where AI spawned
- Try `/aiplayer status <name>` to see what it's doing

### "Command not found: /aiplayer"

- Ensure you have OP permissions (level 2+)
- Check mod loaded: `/mods` command
- Check logs for command registration errors

### "Out of memory with many AIs"

- Reduce block scan radius in `WorldPerceptionEngine`
- Reduce AI update frequency
- Limit number of concurrent AIs

## Acknowledgments

Phase 1 implementation based on:
- Fabric mod development best practices
- FakePlayer pattern from various Minecraft mods
- Perception-action architecture from robotics/AI research

---

**Phase 1 Status:** ✅ **COMPLETE** (Ready for Phase 2)

Last updated: 2025-11-17
