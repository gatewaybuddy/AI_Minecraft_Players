# Configuration Guide

Complete reference for configuring AI Minecraft Players.

---

## Configuration File

**Location**: `config/aiplayer-config.json`

Generated automatically on first run. Edit this file to customize your AI player.

---

## Quick Setup Examples

### Example 1: OpenAI GPT-4 (Intelligent Mode)

```json
{
  "username": "AISteve",
  "personality": {
    "description": "helpful explorer",
    "role": "balanced"
  },
  "llm": {
    "provider": "openai",
    "model": "gpt-4-turbo",
    "apiKey": "sk-proj-YOUR_API_KEY_HERE"
  }
}
```

### Example 2: Local Ollama (Intelligent Mode - FREE)

```bash
# First, install and start Ollama
curl https://ollama.ai/install.sh | sh
ollama pull mistral
ollama serve &
```

```json
{
  "username": "AISteve",
  "llm": {
    "provider": "local",
    "model": "mistral",
    "localModelUrl": "http://localhost:11434"
  }
}
```

### Example 3: Simple Mode (No LLM - FREE)

```json
{
  "username": "AISteve",
  "llm": {
    "apiKey": ""
  }
}
```

Leave `apiKey` empty = Simple mode (random walk)

---

## Personality System

### Complete Personality Configuration

```json
{
  "personality": {
    "description": "helpful and curious explorer",
    "role": "balanced",
    "traits": {
      "aggression": 0.3,
      "curiosity": 0.8,
      "caution": 0.6,
      "sociability": 0.7,
      "independence": 0.5
    },
    "preferences": {
      "mining": 0.5,
      "building": 0.5,
      "exploration": 0.8,
      "combat": 0.3,
      "farming": 0.4,
      "trading": 0.6
    }
  }
}
```

### Personality Traits

All traits range from **0.0 to 1.0**:

#### `aggression` - Combat Tendency

| Value | Behavior |
|-------|----------|
| 0.0-0.3 | **Passive** - Flees from combat, only fights when cornered |
| 0.4-0.6 | **Defensive** - Fights when attacked or protecting territory |
| 0.7-1.0 | **Aggressive** - Actively seeks out and engages hostile mobs |

**Examples**:
- Miner: `0.2` (flees from danger)
- Guardian: `0.4` (defends base)
- Hunter: `0.9` (seeks out mobs)

#### `curiosity` - Exploration Drive

| Value | Behavior |
|-------|----------|
| 0.0-0.3 | **Homebody** - Stays near base, rarely explores |
| 0.4-0.6 | **Balanced** - Explores nearby, returns regularly |
| 0.7-1.0 | **Wanderer** - Constantly exploring, travels far |

**Examples**:
- Builder: `0.3` (stays near projects)
- Balanced: `0.5` (moderate exploration)
- Adventurer: `1.0` (maximum exploration)

#### `caution` - Risk Aversion

| Value | Behavior |
|-------|----------|
| 0.0-0.3 | **Reckless** - Takes risks, ignores dangers |
| 0.4-0.6 | **Careful** - Balances risk vs reward |
| 0.7-1.0 | **Very Cautious** - Avoids all unnecessary risks |

**Examples**:
- Aggressive Hunter: `0.3` (fearless)
- Balanced: `0.5` (reasonable caution)
- Miner/Support: `0.7-0.9` (very careful)

#### `sociability` - Player Interaction

| Value | Behavior |
|-------|----------|
| 0.0-0.3 | **Loner** - Works independently, minimal interaction |
| 0.4-0.6 | **Friendly** - Occasionally helps, responds to requests |
| 0.7-1.0 | **Social** - Actively collaborates, seeks out players |

**Examples**:
- Independent Miner: `0.3` (works alone)
- Balanced: `0.5-0.7` (helpful)
- Support: `0.9` (team-oriented)

#### `independence` - Autonomy Level

| Value | Behavior |
|-------|----------|
| 0.0-0.3 | **Follower** - Waits for commands, follows players |
| 0.4-0.6 | **Balanced** - Sometimes autonomous, sometimes follows |
| 0.7-1.0 | **Self-Directed** - Fully autonomous, generates own goals |

**Examples**:
- Support/Pet: `0.2-0.3` (follows orders)
- Balanced: `0.5` (semi-autonomous)
- Miner/Hunter: `0.8` (fully independent)

---

### Activity Preferences

All preferences range from **0.0 to 1.0**:

#### `mining` - Resource Gathering

How much the AI prioritizes:
- Finding and mining ores
- Gathering stone/cobblestone
- Cave exploration for resources
- Strip mining operations

**High (0.8-1.0)**: Actively seeks mining opportunities
**Low (0.0-0.2)**: Avoids mining, only mines when necessary

#### `building` - Construction

How much the AI prioritizes:
- Building structures
- Placing blocks
- Creating farms/redstone
- Organizing spaces

**High (0.8-1.0)**: Constantly builds and improves
**Low (0.0-0.2)**: Minimal building, functional only

#### `exploration` - Discovery

How much the AI prioritizes:
- Discovering new biomes
- Finding structures (villages, temples, etc.)
- Mapping terrain
- Traveling to new locations

**High (0.8-1.0)**: Always exploring new areas
**Low (0.0-0.2)**: Stays in familiar areas

#### `combat` - Fighting

How much the AI prioritizes:
- Engaging hostile mobs
- Hunting for drops
- Clearing areas of threats
- PvP (if enabled)

**High (0.8-1.0)**: Actively hunts mobs
**Low (0.0-0.2)**: Avoids all combat

#### `farming` - Agriculture

How much the AI prioritizes:
- Planting crops
- Breeding animals
- Harvesting food
- Managing farms

**High (0.8-1.0)**: Dedicates time to farming
**Low (0.0-0.2)**: Ignores farming

#### `trading` - Commerce

How much the AI prioritizes:
- Finding villagers
- Trading for items
- Managing emeralds
- Seeking trades

**High (0.8-1.0)**: Actively seeks trading opportunities
**Low (0.0-0.2)**: Ignores villagers

---

## Pre-made Personality Roles

Use these ready-made configurations:

### 1. Miner (`config/roles/miner.json`)

```json
{
  "username": "AIMiner",
  "personality": {
    "description": "dedicated miner who loves digging deep",
    "role": "miner",
    "traits": {
      "aggression": 0.2,
      "curiosity": 0.6,
      "caution": 0.7,
      "sociability": 0.4,
      "independence": 0.8
    },
    "preferences": {
      "mining": 1.0,
      "building": 0.3,
      "exploration": 0.6,
      "combat": 0.2,
      "farming": 0.1,
      "trading": 0.5
    }
  }
}
```

**Best for**: Resource gathering, cave exploration, strip mining

### 2. Adventurer (`config/roles/adventurer.json`)

```json
{
  "username": "AIExplorer",
  "personality": {
    "description": "adventurous explorer seeking discoveries",
    "role": "adventurer",
    "traits": {
      "aggression": 0.4,
      "curiosity": 1.0,
      "caution": 0.4,
      "sociability": 0.8,
      "independence": 0.7
    },
    "preferences": {
      "mining": 0.3,
      "building": 0.4,
      "exploration": 1.0,
      "combat": 0.5,
      "farming": 0.2,
      "trading": 0.7
    }
  }
}
```

**Best for**: Mapping terrain, finding structures, biome discovery

### 3. Aggressive Hunter (`config/roles/hunter_aggressive.json`)

```json
{
  "username": "AIHunter",
  "personality": {
    "description": "fearless monster hunter",
    "role": "hunter_aggressive",
    "traits": {
      "aggression": 0.9,
      "curiosity": 0.5,
      "caution": 0.3,
      "sociability": 0.5,
      "independence": 0.8
    },
    "preferences": {
      "mining": 0.2,
      "building": 0.2,
      "exploration": 0.6,
      "combat": 1.0,
      "farming": 0.1,
      "trading": 0.3
    }
  }
}
```

**Best for**: Mob clearing, XP farms, aggressive area defense

### 4. Defensive Support (`config/roles/support_defensive.json`)

```json
{
  "username": "AIGuardian",
  "personality": {
    "description": "protective guardian and team player",
    "role": "support_defensive",
    "traits": {
      "aggression": 0.4,
      "curiosity": 0.4,
      "caution": 0.9,
      "sociability": 0.9,
      "independence": 0.3
    },
    "preferences": {
      "mining": 0.3,
      "building": 0.7,
      "exploration": 0.3,
      "combat": 0.6,
      "farming": 0.5,
      "trading": 0.6
    }
  }
}
```

**Best for**: Base protection, bodyguard, defensive combat

### 5. Builder (`config/roles/builder.json`)

```json
{
  "username": "AIBuilder",
  "personality": {
    "description": "creative builder and organizer",
    "role": "builder",
    "traits": {
      "aggression": 0.1,
      "curiosity": 0.6,
      "caution": 0.7,
      "sociability": 0.6,
      "independence": 0.6
    },
    "preferences": {
      "mining": 0.4,
      "building": 1.0,
      "exploration": 0.4,
      "combat": 0.1,
      "farming": 0.6,
      "trading": 0.5
    }
  }
}
```

**Best for**: Construction projects, farm building, organization

---

## Complete Configuration Reference

### Full Template

```json
{
  "username": "AISteve",

  "personality": {
    "description": "helpful explorer",
    "role": "balanced",
    "traits": {
      "aggression": 0.3,
      "curiosity": 0.8,
      "caution": 0.6,
      "sociability": 0.7,
      "independence": 0.5
    },
    "preferences": {
      "mining": 0.5,
      "building": 0.5,
      "exploration": 0.8,
      "combat": 0.3,
      "farming": 0.4,
      "trading": 0.6
    }
  },

  "llm": {
    "provider": "openai",
    "model": "gpt-4-turbo",
    "apiKey": "",
    "localModelUrl": "http://localhost:11434",
    "maxTokens": 1500,
    "temperature": 0.7
  },

  "behavior": {
    "reactionTimeMs": 200,
    "movementHumanization": true,
    "chatEnabled": true,
    "autoRespawn": true,
    "actionCacheSize": 100
  },

  "goals": {
    "defaultGoal": "explore and gather resources",
    "acceptPlayerRequests": true,
    "maxActiveGoals": 3,
    "autonomousGoalGeneration": true
  },

  "memory": {
    "storageType": "json",
    "maxEpisodicMemories": 1000,
    "enableSemanticSearch": false,
    "vectorDbUrl": ""
  }
}
```

### Option Reference

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| **username** | string | "AISteve" | Player name shown in-game |
| **personality.description** | string | "helpful explorer" | Natural language description (used in LLM prompts) |
| **personality.role** | string | "balanced" | Role preset identifier |
| **personality.traits.aggression** | number (0-1) | 0.3 | Combat tendency |
| **personality.traits.curiosity** | number (0-1) | 0.8 | Exploration drive |
| **personality.traits.caution** | number (0-1) | 0.6 | Risk aversion |
| **personality.traits.sociability** | number (0-1) | 0.7 | Player interaction level |
| **personality.traits.independence** | number (0-1) | 0.5 | Autonomy level |
| **personality.preferences.mining** | number (0-1) | 0.5 | Mining priority |
| **personality.preferences.building** | number (0-1) | 0.5 | Building priority |
| **personality.preferences.exploration** | number (0-1) | 0.8 | Exploration priority |
| **personality.preferences.combat** | number (0-1) | 0.3 | Combat priority |
| **personality.preferences.farming** | number (0-1) | 0.4 | Farming priority |
| **personality.preferences.trading** | number (0-1) | 0.6 | Trading priority |
| **llm.provider** | "openai"/"claude"/"local" | "openai" | LLM service provider |
| **llm.model** | string | "gpt-4-turbo" | Specific model to use |
| **llm.apiKey** | string | "" | API key (empty = Simple mode) |
| **llm.localModelUrl** | string | "http://localhost:11434" | Ollama server URL |
| **llm.maxTokens** | number | 1500 | Max LLM response length |
| **llm.temperature** | number (0-2) | 0.7 | LLM creativity (0=deterministic, 1=creative) |
| **behavior.reactionTimeMs** | number | 200 | Artificial delay before actions (ms) |
| **behavior.movementHumanization** | boolean | true | Add movement imperfections |
| **behavior.chatEnabled** | boolean | true | Allow chat messages (Phase 4) |
| **behavior.autoRespawn** | boolean | true | Auto-respawn on death |
| **behavior.actionCacheSize** | number | 100 | Action history size |
| **goals.defaultGoal** | string | "explore and gather resources" | Initial goal |
| **goals.acceptPlayerRequests** | boolean | true | Accept player commands (Phase 4) |
| **goals.maxActiveGoals** | number | 3 | Max simultaneous goals |
| **goals.autonomousGoalGeneration** | boolean | true | AI generates own goals |
| **memory.storageType** | "json" | "json" | Memory storage format |
| **memory.maxEpisodicMemories** | number | 1000 | Max past events remembered |
| **memory.enableSemanticSearch** | boolean | false | Advanced memory search (Phase 5) |
| **memory.vectorDbUrl** | string | "" | Vector database URL (Phase 5) |

---

## How to Apply Role Presets

### Option 1: Copy Preset File

```bash
# Copy a role preset to main config
cp config/roles/miner.json config/aiplayer-config.json

# Then add your API key
nano config/aiplayer-config.json
```

### Option 2: Merge with Existing Config

Keep your LLM settings, just update personality:

1. Open your config: `nano config/aiplayer-config.json`
2. Open role preset: `cat config/roles/miner.json`
3. Copy the `personality` section from preset to your config
4. Save and reload: `/aiplayer reload`

---

## Tips & Best Practices

### Creating Custom Personalities

1. **Start with a preset** that's close to what you want
2. **Adjust traits** one at a time
3. **Test in-game** and observe behavior
4. **Fine-tune** based on results

### Trait Combinations

**Aggressive Hunter**:
- High aggression (0.8-1.0)
- Low caution (0.2-0.4)
- High combat preference (0.8-1.0)

**Peaceful Farmer**:
- Low aggression (0.1-0.2)
- High farming preference (0.8-1.0)
- High independence (0.7-0.9)

**Team Support**:
- High sociability (0.8-1.0)
- Low independence (0.2-0.4)
- High caution (0.7-0.9)

**Solo Explorer**:
- High curiosity (0.8-1.0)
- High independence (0.7-1.0)
- Low sociability (0.2-0.4)

---

## Troubleshooting

### Personality Not Working

**Q**: Changed personality but AI behavior unchanged?

**A**: Reload the config:
```
/aiplayer reload
/aiplayer despawn <name>
/aiplayer spawn <name>
```

### Understanding Behavior

**Q**: How do I know if personality is working?

**A**: Check the logs:
- LLM prompts include personality description
- Goals reflect preferences (e.g., miners seek mining goals)
- Behavior changes based on traits (aggressive → seeks combat)

### Balancing Traits

**Q**: What's a good balanced setup?

**A**: Use the default `balanced` role or:
- All traits: 0.4-0.6 (moderate)
- Preferences: 0.4-0.6 except one at 0.8+ (specialization)

---

## See Also

- [LLM_SETUP.md](../LLM_SETUP.md) - LLM provider configuration
- [README.md](../README.md) - Main documentation
- [PHASE3_IMPLEMENTATION.md](../PHASE3_IMPLEMENTATION.md) - Technical details
