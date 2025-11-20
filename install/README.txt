================================================================================
           AI MINECRAFT PLAYERS - INSTALLATION PACKAGE
================================================================================

Thank you for downloading AI Minecraft Players!

This package contains everything you need to get started quickly.

================================================================================
QUICK START
================================================================================

1. PLACE MOD FILE
   Copy the JAR file to your Minecraft server's mods folder:

   cp ai-minecraft-player-*.jar /path/to/minecraft/server/mods/

2. START SERVER ONCE
   Start your Minecraft server to generate the config file:

   cd /path/to/minecraft/server
   ./start.sh

   (Wait for server to fully start, then stop it)

3. CONFIGURE
   Copy and edit the configuration file:

   cp config/aiplayer-config.json /path/to/minecraft/server/config/
   nano /path/to/minecraft/server/config/aiplayer-config.json

   Add your OpenAI/Claude API key, OR configure local Ollama model.

   For Simple Mode (no LLM): Leave apiKey empty.

4. RESTART SERVER
   Start the server again:

   ./start.sh

5. SPAWN AI PLAYER
   In-game or console:

   /aiplayer spawn AISteve

================================================================================
CONFIGURATION OPTIONS
================================================================================

INTELLIGENT MODE (with LLM):
-----------------------------

Option A: OpenAI (Cloud - $$$)
  - Get API key: https://platform.openai.com/
  - Set in config:
    "provider": "openai"
    "model": "gpt-4-turbo"
    "apiKey": "sk-proj-YOUR_KEY_HERE"
  - Cost: ~$0.36-0.90/hour per AI player

Option B: Claude (Cloud - $$$)
  - Get API key: https://console.anthropic.com/
  - Set in config:
    "provider": "claude"
    "model": "claude-3-5-sonnet-20241022"
    "apiKey": "sk-ant-YOUR_KEY_HERE"
  - Cost: ~$0.45-0.90/hour per AI player

Option C: Ollama (Local - FREE!)
  - Install Ollama: curl -fsSL https://ollama.com/install.sh | sh
  - Pull model: ollama pull mistral
  - Start server: ollama serve &
  - Set in config:
    "provider": "local"
    "model": "mistral"
    "localModelUrl": "http://localhost:11434"
  - Cost: $0 (requires 8GB+ RAM)

SIMPLE MODE (no LLM):
---------------------
  - Leave apiKey empty in config
  - AI will use basic random walk behavior
  - Cost: $0

================================================================================
PERSONALITY PRESETS
================================================================================

Use pre-made personality configurations in config/roles/:

  - miner.json           - Loves mining and resource gathering
  - adventurer.json      - Curious explorer
  - hunter_aggressive.json - Aggressive mob fighter
  - builder.json         - Creative constructor
  - support_defensive.json - Protective team player

To use a preset:
  cp config/roles/miner.json /path/to/server/config/aiplayer-config.json
  nano /path/to/server/config/aiplayer-config.json
  (Add your API key if using intelligent mode)

================================================================================
DIRECTORY STRUCTURE
================================================================================

install/
├── README.txt                          (This file)
├── ai-minecraft-player-*.jar           (The mod - place in mods/)
├── config/
│   ├── aiplayer-config.json           (Main config - copy to server/config/)
│   └── roles/                          (Personality presets)
│       ├── miner.json
│       ├── adventurer.json
│       ├── hunter_aggressive.json
│       ├── builder.json
│       └── support_defensive.json
└── docs/
    ├── README.md                       (Full documentation)
    ├── INSTALL.md                      (Detailed installation guide)
    ├── BUILD.md                        (Build from source guide)
    └── LLM_SETUP.md                    (LLM provider setup details)

================================================================================
FEATURES
================================================================================

✅ Autonomous Behavior   - Explore, mine, build, farm, fight independently
✅ LLM-Powered Planning  - Intelligent goals using GPT-4, Claude, or local models
✅ Memory System         - Remember past events and learn from experiences
✅ Natural Language      - Chat and accept commands in plain English
✅ Skill Generation      - LLM creates custom skills for any goal
✅ Experience Learning   - Learn patterns from successes and failures
✅ World Knowledge       - Remember landmarks, resources, explored areas
✅ Data Persistence      - Skills and knowledge saved across server restarts
✅ Performance Monitoring - Track and optimize AI performance
✅ Auto-Save             - Automatic periodic saves to prevent data loss

================================================================================
REQUIREMENTS
================================================================================

SERVER:
  - Minecraft 1.20.4
  - Fabric Loader 0.15.3+
  - Fabric API 0.95.4+1.20.4
  - Java 17+
  - RAM: 2GB minimum (4GB+ recommended)

FOR INTELLIGENT MODE:
  - OpenAI API key, Claude API key, OR Ollama (local)
  - Internet connection (for cloud APIs)

================================================================================
BASIC COMMANDS
================================================================================

/aiplayer spawn [name]       - Spawn an AI player
/aiplayer despawn <name>     - Remove an AI player
/aiplayer list               - List all active AI players
/aiplayer status <name>      - Show current status and goal
/aiplayer reload             - Reload configuration
/aiplayer perf [name]        - Show performance statistics (if enabled)
/aiplayer save <name>        - Manually save AI data

CHAT WITH AI:
  @AISteve hello!                   - Greet the AI
  @AISteve gather some wood         - Give a command
  @AISteve what are you doing?      - Ask what it's doing

================================================================================
DATA PERSISTENCE
================================================================================

AI players automatically save their progress:

SKILLS:
  Location: config/aiplayer/skills/{player-uuid}/skills.json
  Contains: All learned skills with success/failure statistics

WORLD KNOWLEDGE:
  Location: config/aiplayer/world/{player-uuid}/world_knowledge.json
  Contains: Discovered landmarks, resource locations, explored areas

Auto-save runs every 10 minutes (configurable).
Manual save: /aiplayer save <name>

Backup important! Copy these directories before updates.

================================================================================
TROUBLESHOOTING
================================================================================

PROBLEM: AI in SIMPLE mode (unexpected)
SOLUTION: Check API key is correctly set in config file
          Verify internet connection (for cloud APIs)
          Test API key works with: curl test (see docs)

PROBLEM: AI not moving
SOLUTION: Give it a command: @AISteve explore the area
          Check status: /aiplayer status AISteve
          Check logs for errors: logs/latest.log

PROBLEM: High LLM costs
SOLUTION: Switch to local Ollama model (FREE!)
          Or use cheaper model: "gpt-3.5-turbo"
          Or use Simple mode (no LLM)

PROBLEM: Performance issues
SOLUTION: Check performance: /aiplayer perf
          Reduce number of AI players (start with 1-2)
          Increase server RAM
          Use local model instead of API

See docs/INSTALL.md for complete troubleshooting guide.

================================================================================
NEXT STEPS
================================================================================

1. Complete installation (follow steps above)
2. Read docs/README.md for full feature list
3. Check docs/INSTALL.md for detailed configuration
4. Try different personality presets
5. Monitor performance with /aiplayer perf

================================================================================
DOCUMENTATION
================================================================================

Full documentation available in docs/:

  - README.md      : Complete feature overview and quick start
  - INSTALL.md     : Detailed installation and deployment guide
  - BUILD.md       : Build from source instructions
  - LLM_SETUP.md   : LLM provider setup details

Online documentation:
  https://github.com/your-username/AI_Minecraft_Players

================================================================================
SUPPORT
================================================================================

- GitHub Issues: https://github.com/your-username/AI_Minecraft_Players/issues
- Documentation: See docs/ folder
- Community: Discord/Forum (coming soon)

================================================================================
LICENSE
================================================================================

MIT License - See LICENSE file for details

Copyright (c) 2024 AI Minecraft Players Contributors

================================================================================

Happy AI Minecrafting! 🤖⛏️

For detailed instructions, see docs/INSTALL.md

================================================================================
