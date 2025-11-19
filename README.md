# AI Minecraft Players

> **Intelligent, autonomous AI players for Minecraft servers powered by LLMs**

Create fully autonomous AI players capable of intelligent decision-making, goal-directed behavior, and natural language understanding using Large Language Models (GPT-4, Claude, or local models).

---

## ✨ Features

- ✅ **Autonomous Behavior** - Explore, mine, build, farm, and fight independently
- ✅ **LLM-Powered Planning** - Form intelligent goals using GPT-4, Claude, or local models
- ✅ **Memory System** - Remember past events and learn from experiences
- ✅ **Personality System** - Customize behavior with roles (Miner, Explorer, Hunter, Builder, Support)
- ✅ **Natural Language Chat** - Communicate with AI players in plain English, give commands, ask questions
- ✅ **Self-Improvement** - AIs learn new skills from successful experiences and failures
- ✅ **World Memory** - AIs remember landmarks, resource locations, and danger zones
- ✅ **Multi-AI Coordination** - Multiple AIs collaborate on shared goals and work in teams
- ✅ **Multi-Mode** - Intelligent mode (with LLM) or Simple mode (random walk)

---

## 📊 Project Status

| Phase | Status | Description |
|-------|--------|-------------|
| Phase 1 | ✅ Complete | Foundation, config, commands |
| Phase 2 | ✅ Complete | Pathfinding, mining, building, combat |
| Phase 3 | ✅ Complete | Memory, LLM planning, skills |
| Phase 4 | ✅ Complete | Natural language chat & communication |
| Phase 5 | ✅ Complete | Self-improvement, world knowledge, multi-AI coordination |
| **Overall** | **✅ 85% Production Ready** | **Ready for beta testing** |

---

## 🚀 Quick Start

### Installation

1. **Download the mod** (requires Minecraft 1.20.4 + Fabric)
2. **Copy to mods folder**: `cp aiplayer-1.0.0.jar server/mods/`
3. **Start server** (generates config)
4. **Configure** (see below)
5. **Restart and spawn**: `/aiplayer spawn`

### Configuration Modes

| Mode | Setup | Capabilities | Cost |
|------|-------|--------------|------|
| **INTELLIGENT** | Add API key | Goal planning, memory, learning | $0.36-0.90/hr (or FREE with Ollama) |
| **SIMPLE** | Leave apiKey empty | Random walk, basic movement | FREE |

---

## ⚙️ Configuration

### Quick Setup: OpenAI (Intelligent Mode)

Edit `config/aiplayer-config.json`:

```json
{
  "username": "AISteve",
  "llm": {
    "provider": "openai",
    "model": "gpt-4-turbo",
    "apiKey": "sk-proj-YOUR_KEY_HERE"
  }
}
```

### Quick Setup: Local Ollama (Intelligent Mode - FREE!)

```bash
# Install Ollama
curl https://ollama.ai/install.sh | sh
ollama pull mistral
ollama serve &
```

Edit `config/aiplayer-config.json`:

```json
{
  "llm": {
    "provider": "local",
    "model": "mistral"
  }
}
```

### Quick Setup: Simple Mode (FREE!)

```json
{
  "llm": {
    "apiKey": ""
  }
}
```

**No API key needed** - AI uses simple random walk behavior.

---

## 🎭 Personality Roles

Customize AI behavior with personality traits and activity preferences!

### Pre-made Roles

| Role | Description | Best For |
|------|-------------|----------|
| **⛏️ Miner** | Loves digging and gathering resources | Resource farms, cave exploration |
| **🗺️ Adventurer** | Curious explorer seeking discoveries | Mapping terrain, finding structures |
| **⚔️ Aggressive Hunter** | Fearless mob hunter | Mob clearing, aggressive defense |
| **🛡️ Defensive Support** | Protective team player | Base protection, bodyguard |
| **🏗️ Builder** | Creative constructor | Building projects, farms |

### Using Presets

```bash
# Copy a role preset
cp config/roles/miner.json config/aiplayer-config.json

# Add your API key
nano config/aiplayer-config.json
```

### Custom Personalities

Customize traits (0.0 - 1.0):

```json
{
  "personality": {
    "description": "helpful explorer",
    "traits": {
      "aggression": 0.3,    // Combat tendency
      "curiosity": 0.8,     // Exploration drive
      "caution": 0.6,       // Risk aversion
      "sociability": 0.7,   // Player interaction
      "independence": 0.5   // Autonomy level
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

**See [CONFIGURATION.md](docs/CONFIGURATION.md) for complete personality system guide.**

---

## 🎮 Commands

| Command | Description |
|---------|-------------|
| `/aiplayer spawn [name]` | Spawn AI player |
| `/aiplayer despawn <name>` | Remove AI player |
| `/aiplayer list` | List all active AI players |
| `/aiplayer status <name>` | Show current status and goal |
| `/aiplayer reload` | Reload configuration |

---

## 💰 Cost Estimates

| Provider | Model | Cost/Hour | Cost/Day |
|----------|-------|-----------|----------|
| OpenAI | gpt-4-turbo | $0.36-0.90 | $8-21 |
| OpenAI | gpt-3.5-turbo | $0.05-0.15 | $1-4 |
| Claude | claude-3-5-sonnet | $0.45-0.90 | $11-21 |
| Claude | claude-3-haiku | $0.10-0.25 | $2-6 |
| **Local** | **mistral/llama2** | **$0** | **$0** |

*Estimates with intelligent response caching (50-80% hit rate) and 5s planning interval*

**💡 Cost Saving Features**:
- ✅ Automatic response caching reduces costs by 50-80%
- ✅ Configurable planning intervals (balance cost vs intelligence)
- ✅ Use local models with Ollama for completely FREE operation!
- ✅ Simple mode available (no LLM required)

---

## 📚 Documentation

### Setup & Configuration
- **[USER_INSTALLATION_GUIDE.md](USER_INSTALLATION_GUIDE.md)** - Complete installation guide for end users
- **[LLM_SETUP.md](LLM_SETUP.md)** - Detailed LLM provider setup guide
- **[CONFIGURATION.md](docs/CONFIGURATION.md)** - Complete configuration reference with personality system
- **[BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md)** - Building from source

### Technical Documentation
- **[PHASE3_IMPLEMENTATION.md](PHASE3_IMPLEMENTATION.md)** - Memory, planning, and skills (Phase 3)
- **[PHASE4_IMPLEMENTATION.md](PHASE4_IMPLEMENTATION.md)** - Natural language communication (Phase 4)
- **[PHASE5_IMPLEMENTATION.md](PHASE5_IMPLEMENTATION.md)** - Self-improvement and coordination (Phase 5)
- **[PROJECT_PLAN.md](PROJECT_PLAN.md)** - Full project architecture
- **[TECHNICAL_SPEC.md](TECHNICAL_SPEC.md)** - Technical specifications
- **[FINAL_CODE_REVIEW.md](FINAL_CODE_REVIEW.md)** - Comprehensive code review and analysis

---

## 🔧 Troubleshooting

### AI in SIMPLE mode

**Problem**: Logs show "AI players will run in SIMPLE mode"

**Solution**: Check your API key:
- OpenAI: Starts with `sk-proj-` or `sk-`
- Claude: Starts with `sk-ant-`
- Local: Check Ollama is running: `curl http://localhost:11434`

### High costs

**Problem**: LLM costs too high

**Solutions**:
1. **Use local models** (FREE): `"provider": "local", "model": "mistral"`
2. Use cheaper models: `"model": "gpt-3.5-turbo"`
3. Verify caching is working (check logs)

### Ollama model not found

```bash
ollama pull mistral
ollama list
```

---

## 🏗️ Technology Stack

- **Mod Framework**: Fabric
- **Language**: Java 17+
- **LLM Integration**: OpenAI, Anthropic Claude, Ollama
- **Memory**: Hierarchical system (working + episodic + semantic)
- **Planning**: ReAct framework (Reasoning + Acting)
- **Pathfinding**: Custom A* implementation

---

## 💡 Inspiration

Based on cutting-edge AI research:

- **[Voyager](https://arxiv.org/abs/2305.16291)** - LLM-powered Minecraft agent with skill library
- **[MineDojo](https://minedojo.org/)** - Open-ended embodied agents
- **[Baritone](https://github.com/cabaletta/baritone)** - Minecraft pathfinding
- **ReAct** - Reasoning + Acting framework

---

## ⚠️ Important Notes

### API Key Security

- ⚠️ **NEVER commit API keys to Git**
- Use `.gitignore` for config files
- Rotate keys regularly
- Monitor usage on provider dashboards

### Server Permissions

- AI players can perform all player actions
- Consider using separate permissions/roles
- Configure `goals.acceptPlayerRequests` based on trust level

### Performance

- Each AI player: ~10-15MB RAM
- LLM calls are async (non-blocking)
- For many AIs, use local models (no rate limits)

---

## 📜 License

MIT License - See [LICENSE](LICENSE) for details

---

## 🤝 Contributing

Contributions welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Follow the architecture in TECHNICAL_SPEC.md
4. Submit a pull request

---

## 🎯 What's New in Phase 5

- ✅ **Self-Improving Skills** - AIs generate new skills from successful action sequences
- ✅ **Learn from Failure** - Failed skills are automatically refined using LLM
- ✅ **Observation Learning** - AIs learn by watching human players
- ✅ **World Knowledge** - Spatial memory of landmarks, resources, and dangers
- ✅ **Multi-AI Coordination** - Shared goals, teams, and task distribution

See [PHASE5_IMPLEMENTATION.md](PHASE5_IMPLEMENTATION.md) for technical details.

---

## 🔜 Coming Next

- 🧪 Unit test suite
- 💾 Persistent world knowledge (save/load)
- 🎨 Multi-version support (1.19, 1.21)
- 📊 Performance optimization
- 🌐 Web dashboard for AI monitoring

---

**Made with ❤️ for the Minecraft modding community**

---

*Note: This is an educational/research project. Use responsibly on servers where AI bots are permitted.*
