# AI Minecraft Players - User Installation Guide

**Version**: 1.0.0
**Minecraft**: 1.20.4
**Mod Loader**: Fabric

---

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Quick Start](#quick-start)
3. [Step-by-Step Installation](#step-by-step-installation)
4. [Configuration](#configuration)
5. [First Launch](#first-launch)
6. [Usage & Commands](#usage--commands)
7. [Troubleshooting](#troubleshooting)
8. [FAQ](#faq)

---

## Prerequisites

### Required

- **Minecraft Java Edition** - Version 1.20.4
- **Java 17 or higher** - [Download here](https://adoptium.net/)
- **10 GB free disk space** (for Minecraft + mod + dependencies)

### Recommended

- **4 GB RAM minimum** (8 GB recommended)
- **Stable internet connection** (for LLM API calls, or use local Ollama)

---

## Quick Start

**For experienced Minecraft modders:**

1. Install Fabric Loader 0.15.3+ for Minecraft 1.20.4
2. Install Fabric API 0.95.4+
3. Copy `aiplayer-1.0.0.jar` to `.minecraft/mods/`
4. Configure API key in `config/aiplayer.json`
5. Launch Minecraft and run `/aiplayer spawn AISteve`

---

## Step-by-Step Installation

### Step 1: Install Java 17+

**Check if you have Java**:
```bash
java -version
```

If version is 17 or higher, you're good! Otherwise:

**Windows**:
1. Download from https://adoptium.net/
2. Run installer
3. Restart computer

**Mac**:
```bash
brew install openjdk@17
```

**Linux**:
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

---

### Step 2: Install Fabric Loader

**Option A: Using Fabric Installer (Recommended)**

1. **Download Fabric Installer**:
   - Visit: https://fabricmc.net/use/installer/
   - Click "Download Installer" (Universal/.jar)

2. **Run the Installer**:
   - **Windows**: Double-click the `.jar` file
     - If it doesn't open: Right-click → "Open with" → "Java(TM) Platform SE binary"
   - **Mac/Linux**: Double-click or run `java -jar fabric-installer-*.jar`

3. **Installation Options**:
   - **Client** tab (for playing) or **Server** tab (for hosting)
   - Select **Minecraft Version**: `1.20.4`
   - Select **Loader Version**: `0.15.3` or higher
   - Installation Directory: Default is fine
   - Click **Install**

4. **Verify Installation**:
   - Open Minecraft Launcher
   - You should see "Fabric Loader 1.20.4" in the version list

**Option B: Manual Installation**
- Advanced users can manually download from https://fabricmc.net/use/

---

### Step 3: Install Fabric API

1. **Download Fabric API**:
   - Visit: https://modrinth.com/mod/fabric-api
   - OR: https://www.curseforge.com/minecraft/mc-mods/fabric-api
   - Find version for **Minecraft 1.20.4**
   - Download: `fabric-api-0.95.4+1.20.4.jar` (or newer)

2. **Place in Mods Folder**:
   - **Windows**: `%APPDATA%\.minecraft\mods\`
   - **Mac**: `~/Library/Application Support/minecraft/mods/`
   - **Linux**: `~/.minecraft/mods/`

   If `mods/` folder doesn't exist, create it.

---

### Step 4: Install AI Minecraft Players Mod

1. **Download the Mod**:
   - Get `aiplayer-1.0.0.jar` from releases
   - OR build from source (see BUILD_INSTRUCTIONS.md)

2. **Place in Mods Folder**:
   - Copy `aiplayer-1.0.0.jar` to `.minecraft/mods/`
   - Same location as Fabric API

3. **Verify Mod Folder**:
   ```
   .minecraft/mods/
   ├── fabric-api-0.95.4+1.20.4.jar
   └── aiplayer-1.0.0.jar
   ```

---

### Step 5: First Launch (Generate Config)

1. **Launch Minecraft**:
   - Select "Fabric Loader 1.20.4" profile
   - Click "Play"

2. **Create/Load a World**:
   - Single Player → Create New World (or load existing)
   - Game will generate default configuration

3. **Close Minecraft**:
   - Exit back to desktop
   - This ensures config file is created

---

## Configuration

### Configuration File Location

- **Windows**: `%APPDATA%\.minecraft\config\aiplayer.json`
- **Mac**: `~/Library/Application Support/minecraft/config/aiplayer.json`
- **Linux**: `~/.minecraft/config/aiplayer.json`

---

### Configuration Options

Choose ONE of these setup modes:

---

#### Option A: Intelligent Mode with OpenAI (RECOMMENDED)

**Best for**: Full AI capabilities with planning, memory, and natural chat

**Cost**: ~$0.36-0.90 per AI per hour (with caching)

**Setup**:

1. **Get OpenAI API Key**:
   - Visit: https://platform.openai.com/api-keys
   - Create account (if needed)
   - Click "Create new secret key"
   - Copy key (starts with `sk-proj-...` or `sk-...`)
   - **Important**: Save this key securely!

2. **Edit Config**:
   Open `config/aiplayer.json` and set:
   ```json
   {
     "username": "AISteve",
     "llm": {
       "provider": "openai",
       "model": "gpt-4-turbo",
       "apiKey": "sk-proj-YOUR_KEY_HERE",
       "maxTokens": 1500,
       "temperature": 0.7
     }
   }
   ```

3. **Save and close**

**Models Available**:
- `gpt-4-turbo` - Most intelligent (recommended)
- `gpt-4` - Very intelligent but slower
- `gpt-3.5-turbo` - Fast and cheap (~$0.05/hour)

---

#### Option B: Intelligent Mode with Claude

**Best for**: Alternative to OpenAI with excellent reasoning

**Cost**: ~$0.45-0.90 per AI per hour

**Setup**:

1. **Get Claude API Key**:
   - Visit: https://console.anthropic.com/
   - Create account
   - Generate API key (starts with `sk-ant-...`)

2. **Edit Config**:
   ```json
   {
     "llm": {
       "provider": "claude",
       "model": "claude-3-5-sonnet-20241022",
       "apiKey": "sk-ant-YOUR_KEY_HERE"
     }
   }
   ```

**Models Available**:
- `claude-3-5-sonnet-20241022` - Excellent (recommended)
- `claude-3-haiku-20240307` - Fast and cheap (~$0.10/hour)
- `claude-3-opus-20240229` - Most powerful

---

#### Option C: Intelligent Mode with Local Ollama (FREE!)

**Best for**: No API costs, runs on your computer

**Requirements**:
- **8 GB RAM** (16 GB recommended)
- **10 GB disk space**
- **GPU recommended** (works on CPU but slower)

**Setup**:

1. **Install Ollama**:
   - **Windows**: Download from https://ollama.ai/download
   - **Mac**: `brew install ollama`
   - **Linux**: `curl https://ollama.ai/install.sh | sh`

2. **Download Model**:
   ```bash
   ollama pull mistral
   ```
   Other options: `llama2`, `phi`, `codellama`

3. **Start Ollama**:
   ```bash
   ollama serve
   ```
   Leave this running in the background

4. **Edit Config**:
   ```json
   {
     "llm": {
       "provider": "local",
       "model": "mistral",
       "apiKey": "",
       "localModelUrl": "http://localhost:11434"
     }
   }
   ```

5. **Verify Ollama is Running**:
   ```bash
   curl http://localhost:11434
   ```
   Should return: `Ollama is running`

---

#### Option D: Simple Mode (NO LLM - FREE)

**Best for**: Testing, no API costs, basic behavior

**Limitations**:
- No intelligent planning
- No natural language chat
- Random walk behavior only

**Setup**:

```json
{
  "llm": {
    "apiKey": ""
  }
}
```

That's it! Leave `apiKey` empty.

---

### Personality Configuration (Optional)

Customize your AI's behavior and preferences:

```json
{
  "personality": {
    "description": "helpful explorer",
    "role": "adventurer",
    "traits": {
      "aggression": 0.3,      // Combat tendency (0-1)
      "curiosity": 0.9,       // Exploration drive (0-1)
      "caution": 0.5,         // Risk aversion (0-1)
      "sociability": 0.8,     // Player interaction (0-1)
      "independence": 0.6     // Autonomy level (0-1)
    },
    "preferences": {
      "mining": 0.6,
      "building": 0.4,
      "exploration": 0.9,
      "combat": 0.3,
      "farming": 0.5,
      "trading": 0.7
    }
  }
}
```

**Pre-made Roles** (in `config/roles/`):
- `miner.json` - Loves mining and resource gathering
- `builder.json` - Creative constructor
- `adventurer.json` - Curious explorer
- `hunter_aggressive.json` - Fearless combat specialist
- `support_defensive.json` - Protective team player

**To use a preset**:
```bash
cp config/roles/adventurer.json config/aiplayer.json
# Then add your API key to the file
```

---

## First Launch

1. **Start Minecraft** with Fabric Loader 1.20.4

2. **Check Logs** (optional but recommended):
   - Press `F3` in-game to open debug
   - Check latest.log for:
     - ✅ "AI Minecraft Player mod initialized successfully"
     - ✅ "LLM provider initialized: openai (gpt-4-turbo)" (if using LLM)
     - ⚠️ "AI players will run in SIMPLE mode" (if no LLM)

3. **Create or Load World**

4. **Spawn an AI Player**:
   ```
   /aiplayer spawn AISteve
   ```

5. **Verify AI Spawned**:
   - You should see "AISteve" in the player list (Tab key)
   - AI will start exploring autonomously

6. **Interact with AI** (if using Intelligent mode):
   ```
   @AISteve, gather 64 oak logs
   Hey AI, what are you doing?
   @AISteve, do you have any diamonds?
   ```

---

## Usage & Commands

### Commands

All commands require **OP permissions** (level 2+).

```
/aiplayer spawn [name]       - Spawn a new AI player
/aiplayer despawn <name>     - Remove an AI player
/aiplayer list               - Show all active AI players
/aiplayer status <name>      - Show AI's current status and goal
/aiplayer reload             - Reload configuration
```

### Examples

```bash
# Spawn AI
/aiplayer spawn AISteve

# Spawn multiple AIs
/aiplayer spawn Miner1
/aiplayer spawn Builder1
/aiplayer spawn Explorer1

# Check status
/aiplayer status AISteve

# List all AIs
/aiplayer list

# Remove AI
/aiplayer despawn AISteve

# Reload config (after editing aiplayer.json)
/aiplayer reload
```

---

### Interacting with AI (Intelligent Mode)

**Task Requests**:
```
@AISteve, gather 64 oak logs
@AISteve, mine stone
@AISteve, build a house
@AISteve, kill zombies
@AISteve, craft stone tools
```

**Questions**:
```
@AISteve, do you have any diamonds?
Hey AI, where are you?
@AISteve, how many logs do you have?
```

**Status Queries**:
```
@AISteve, what are you doing?
Hey AI, status
@AISteve, how's it going?
```

**Casual Chat**:
```
Hello AISteve!
@AISteve, how are you?
Hey AI, thank you!
```

**Addressing Multiple AIs**:
```
@AI, gather wood          ← All AIs respond
Hey AI, what are you all doing?
```

---

## Troubleshooting

### AI in SIMPLE Mode (No Intelligence)

**Symptoms**:
- Logs show: "AI players will run in SIMPLE mode"
- AI just wanders randomly
- Doesn't respond to chat

**Solutions**:

1. **Check API Key**:
   - Open `config/aiplayer.json`
   - Verify `apiKey` is filled in
   - OpenAI keys start with `sk-proj-` or `sk-`
   - Claude keys start with `sk-ant-`

2. **Verify Key is Valid**:
   - Test at https://platform.openai.com/ (OpenAI)
   - OR https://console.anthropic.com/ (Claude)

3. **For Local Ollama**:
   ```bash
   # Check Ollama is running
   curl http://localhost:11434

   # Should return: "Ollama is running"

   # If not running:
   ollama serve
   ```

4. **Restart Minecraft** after fixing config

---

### AI Doesn't Respond to Chat

**Solutions**:

1. **Address AI Correctly**:
   - Use `@AIName` or `Hey AIName`
   - Example: `@AISteve, gather wood`

2. **Check AI is in Intelligent Mode**:
   - Look for "Phase 4 chat system initialized" in logs
   - Requires valid API key

3. **Check Logs for Errors**:
   - `.minecraft/logs/latest.log`
   - Search for "chat" or "LLM"

---

### High LLM Costs

**Solutions**:

1. **Switch to Local Ollama** (FREE):
   ```json
   {
     "llm": {
       "provider": "local",
       "model": "mistral"
     }
   }
   ```

2. **Use Cheaper Model**:
   ```json
   {
     "llm": {
       "model": "gpt-3.5-turbo"  // ~$0.05/hour
     }
   }
   ```

3. **Verify Caching is Working**:
   - Logs should show cache hit rates
   - Should see 50-80% cache hits

4. **Monitor Usage**:
   - OpenAI: https://platform.openai.com/usage
   - Claude: https://console.anthropic.com/usage

---

### Mod Failed to Load

**Symptoms**:
- Minecraft crashes on startup
- Mod doesn't appear in mods list

**Solutions**:

1. **Check Fabric Loader Installed**:
   - Launcher should show "Fabric Loader" profile

2. **Check Fabric API Installed**:
   - `fabric-api-*.jar` must be in `mods/` folder

3. **Check Minecraft Version**:
   - Must be 1.20.4 exactly
   - Check launcher profile

4. **Check Java Version**:
   ```bash
   java -version
   ```
   - Must be 17 or higher

5. **Check Logs**:
   - `.minecraft/logs/latest.log`
   - Look for error messages

---

### AI Gets Stuck

**Solutions**:

1. **Teleport AI**:
   ```
   /tp AISteve @p
   ```

2. **Despawn and Respawn**:
   ```
   /aiplayer despawn AISteve
   /aiplayer spawn AISteve
   ```

3. **Check Current Goal**:
   ```
   /aiplayer status AISteve
   ```

---

### Performance Issues

**Symptoms**:
- Lag when AI is active
- Low FPS

**Solutions**:

1. **Reduce Number of AIs**:
   - Each AI uses ~10-15% CPU
   - Limit to 2-3 AIs on lower-end systems

2. **Use Simpler Model**:
   - Switch to `gpt-3.5-turbo` or `claude-haiku`

3. **Allocate More RAM**:
   - Minecraft launcher settings
   - Increase to 4-8 GB

4. **For Ollama**:
   - Ensure you have a GPU
   - 8+ GB system RAM

---

## FAQ

### Is this mod compatible with other mods?

Generally yes! This mod uses Fabric API and follows best practices. However:
- ✅ Works with: Most Fabric mods, optimization mods (Sodium, Lithium)
- ⚠️ May conflict with: Other AI/bot mods, anti-cheat mods
- Test in a separate world first!

### Can I use this on a server?

Yes! Works on both single-player and multiplayer servers.
- Server must have Fabric Loader + Fabric API
- Each player needs the mod installed
- Configure API key on server

### How many AI players can I run?

Depends on your hardware:
- **4 GB RAM**: 1-2 AI players
- **8 GB RAM**: 3-5 AI players
- **16 GB RAM**: 5-10 AI players
- Each AI uses ~10-15% CPU, 10-15 MB RAM

### What happens if I run out of API credits?

- AI will log warnings
- Fall back to SIMPLE mode (random walk)
- No crash or errors
- Refill credits and `/aiplayer reload`

### Can AI players die?

Yes! They're real players and can:
- Take damage
- Lose hunger
- Die from mobs, falls, lava, etc.
- Configure `autoRespawn: true` for automatic respawn

### Do AI players need food?

Yes! In survival mode they:
- Lose hunger over time
- Need to eat food
- Can starve if hunger reaches 0
- Intelligent mode AIs will seek food automatically

### Can AI players level up and gain XP?

Yes! They collect XP from:
- Mining ores
- Killing mobs
- Smelting items
- Can use XP for enchanting

### Is this mod safe/allowed on servers?

- ✅ Safe: No malicious code, open source
- ⚠️ Server Permission: Check with server admins first
- ⚠️ Anti-Cheat: May be flagged as bot, configure accordingly

### How do I update the mod?

1. Download new version JAR
2. Remove old `aiplayer-*.jar` from `mods/` folder
3. Add new JAR to `mods/` folder
4. Restart Minecraft
5. Config file will auto-migrate

### Where can I get help?

- 📖 Documentation: See README.md and other docs
- 🐛 Report Bugs: GitHub Issues
- 💬 Community: Discord (link in README)
- 📧 Email: Check README for contact

---

## Next Steps

✅ **You're all set!**

Try these:
1. Spawn an AI: `/aiplayer spawn AISteve`
2. Give it a task: `@AISteve, gather 64 oak logs`
3. Check status: `/aiplayer status AISteve`
4. Experiment with different personalities
5. Try multiple AIs working together!

---

## Additional Resources

- **README.md** - Project overview
- **PHASE4_IMPLEMENTATION.md** - Technical details
- **BUILD_INSTRUCTIONS.md** - Building from source
- **CONFIGURATION.md** - Advanced configuration guide
- **LLM_SETUP.md** - Detailed LLM provider setup

---

**Made with ❤️ for the Minecraft modding community**

**Version**: 1.0.0
**License**: MIT
**Repository**: https://github.com/gatewaybuddy/AI_Minecraft_Players

---

*Note: This is an educational/research project. Use responsibly and check server rules before using AI players on multiplayer servers.*
