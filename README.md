# AI Minecraft Players

An autonomous AI player mod for Minecraft that creates fully functional AI players capable of logging into servers, performing all player actions, communicating naturally with human players, and collaborating to accomplish complex tasks.

## Overview

This project implements an AI-powered Minecraft player that:
- Logs into any Minecraft server as a regular player
- Performs all actions humans can: mining, building, crafting, combat, exploration
- Communicates naturally via chat using Large Language Models (LLMs)
- Remembers past experiences and learns from them
- Accepts and completes tasks requested by human players
- Collaborates with other players on shared goals

## Project Status

🔄 **Currently in Planning Phase**

We have completed comprehensive planning and architecture design. The project is ready to begin implementation.

## Documentation

- **[PROJECT_PLAN.md](PROJECT_PLAN.md)** - Comprehensive project architecture, phases, and timeline
- **[TECHNICAL_SPEC.md](TECHNICAL_SPEC.md)** - Detailed implementation specifications and code examples
- **[QUICK_START.md](QUICK_START.md)** - Development environment setup and getting started guide

## Key Features

### Autonomous Behavior
- Self-directed exploration and resource gathering
- Dynamic goal generation based on environment
- Multi-step task planning and execution
- Adaptive replanning when encountering obstacles

### Natural Communication
- LLM-powered natural language understanding
- Context-aware conversation
- Accepts tasks via chat ("Hey AI, gather 64 oak logs")
- Provides status updates and asks for help when needed

### Advanced Memory System
- **Short-term memory**: Current goals and recent observations
- **Episodic memory**: What happened, when, and where
- **Semantic memory**: Learned facts, strategies, and relationships
- Learns from successes and failures

### Full Player Capabilities
- Movement: Walking, running, jumping, swimming, climbing
- Mining: Intelligent tool selection and block breaking
- Building: Precise block placement with orientation
- Combat: Fighting mobs and PvP (configurable)
- Crafting: Recipe execution and inventory management
- Pathfinding: A* navigation with obstacle avoidance

## Technology Stack

- **Mod Framework**: Fabric (with planned NeoForge support)
- **Language**: Java 17+
- **LLM Integration**: OpenAI GPT-4, Anthropic Claude, or local models (Ollama)
- **Pathfinding**: Custom A* implementation inspired by Baritone
- **Memory**: Hierarchical system with JSON/SQLite storage

## Inspiration

This project draws inspiration from:
- **Voyager** (MineDojo) - LLM-powered embodied agent with skill library
- **Project Sid** (Altera) - Multi-agent collaboration and emergent behavior
- **Baritone** - Efficient pathfinding and goal-based architecture
- **Mineflayer** - Bot control and action primitives

## Development Timeline

**16-week development plan across 6 phases:**

1. **Phase 1** (Weeks 1-3): Foundation - Basic mod structure and player entity
2. **Phase 2** (Weeks 4-6): Action System - All player capabilities
3. **Phase 3** (Weeks 7-9): Memory & Planning - Goal tracking and LLM integration
4. **Phase 4** (Weeks 10-11): Communication - Natural language chat
5. **Phase 5** (Weeks 12-14): Advanced AI - Skill library and learning
6. **Phase 6** (Weeks 15-16): Optimization & Polish

## Getting Started

See [QUICK_START.md](QUICK_START.md) for detailed setup instructions.

**Quick setup:**

```bash
# 1. Clone the repository
git clone https://github.com/yourusername/AI_Minecraft_Players.git
cd AI_Minecraft_Players

# 2. Set up development environment (requires Java 17+)
./gradlew build

# 3. Configure your LLM API key
cp config/aiplayer.example.json config/aiplayer.json
# Edit config/aiplayer.json with your API key

# 4. Run development server
./gradlew runClient
```

## Configuration

Example configuration:

```json
{
  "username": "AISteve",
  "personality": "helpful and curious",
  "llm": {
    "provider": "openai",
    "model": "gpt-4",
    "apiKey": "sk-your-key-here"
  },
  "goals": {
    "defaultGoal": "explore and gather resources",
    "acceptPlayerRequests": true
  }
}
```

## Usage Examples

```
# In Minecraft chat:
/aiplayer spawn                          # Spawn AI player
/aiplayer goal AISteve "build a house"   # Set a goal
/aiplayer status AISteve                 # Check status

# Natural language interaction:
You: "Hey AISteve, can you mine some diamonds for me?"
AISteve: "Sure! I'll start looking for diamonds underground."
```

## Contributing

Contributions are welcome! Please read the project plan and technical specifications before contributing.

1. Fork the repository
2. Create a feature branch
3. Follow the architecture in TECHNICAL_SPEC.md
4. Submit a pull request

## License

MIT License - see [LICENSE](LICENSE) file for details

## Research & References

- [Voyager Paper](https://arxiv.org/abs/2305.16291) - Open-Ended Embodied Agent with LLMs
- [MineDojo](https://minedojo.org/) - Building Open-Ended Embodied Agents
- [Baritone](https://github.com/cabaletta/baritone) - Minecraft Pathfinding
- [Fabric Wiki](https://fabricmc.net/wiki/) - Modding Documentation

## Contact

For questions, issues, or suggestions, please open an issue on GitHub.

---

**Note**: This is an educational/research project. Use responsibly on servers where AI bots are permitted.
