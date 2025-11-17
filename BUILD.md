# Build Instructions

This document provides instructions for building and running the AI Minecraft Player mod.

## Prerequisites

### Required
- **Java Development Kit (JDK) 17 or higher**
  - Download from: https://adoptium.net/ or https://www.oracle.com/java/technologies/downloads/
  - Verify installation: `java -version` (should show 17 or higher)

- **Minecraft Java Edition 1.20.4**
  - Purchase and download from: https://www.minecraft.net/
  - Launch at least once to download game files

### Optional
- **IntelliJ IDEA** (recommended for development)
  - Community Edition: https://www.jetbrains.com/idea/download/
- **Ollama** (for local LLM testing)
  - Download from: https://ollama.ai/

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/gatewaybuddy/AI_Minecraft_Players.git
cd AI_Minecraft_Players
```

### 2. Build the Mod

```bash
# On Linux/Mac:
./gradlew build

# On Windows:
gradlew.bat build
```

The first build will download dependencies and may take a few minutes.

**Expected Output:**
```
BUILD SUCCESSFUL in 1m 23s
```

The compiled mod JAR will be in: `build/libs/ai-minecraft-player-0.1.0-alpha.jar`

### 3. Run in Development Mode

To run Minecraft with the mod loaded for testing:

```bash
# Run Minecraft client
./gradlew runClient

# Or run a dedicated server
./gradlew runServer
```

## Development Setup

### IntelliJ IDEA

1. **Import Project**
   - Open IntelliJ IDEA
   - File → Open
   - Select the project directory
   - IntelliJ will auto-detect it as a Gradle project

2. **Wait for Gradle Sync**
   - IntelliJ will automatically download dependencies
   - Wait for "Gradle sync finished" notification

3. **Generate Minecraft Sources** (Important!)
   ```bash
   ./gradlew genSources
   ```
   This decompiles Minecraft sources for development.

4. **Run Configurations**
   - Gradle will create run configurations automatically
   - Look for "Minecraft Client" and "Minecraft Server" in the run configurations dropdown

### VS Code

1. **Install Extensions**
   - Extension Pack for Java
   - Gradle for Java

2. **Open Project**
   - File → Open Folder → Select project directory

3. **Run Tasks**
   - Terminal → Run Task → Select Gradle tasks

## Build Commands

### Common Tasks

```bash
# Clean build directory
./gradlew clean

# Build mod JAR
./gradlew build

# Run Minecraft client
./gradlew runClient

# Run dedicated server
./gradlew runServer

# Generate Minecraft sources (for IDE)
./gradlew genSources

# Run tests
./gradlew test

# Build without tests
./gradlew build -x test
```

### Gradle Wrapper

The project includes Gradle Wrapper, so you don't need to install Gradle separately.

- Linux/Mac: `./gradlew <task>`
- Windows: `gradlew.bat <task>`

## Configuration

### First Run

On first run, the mod will create a default configuration file:
```
config/aiplayer.json
```

### Configure LLM API

Edit `config/aiplayer.json` and add your API key:

```json
{
  "llm": {
    "provider": "openai",
    "model": "gpt-4",
    "apiKey": "sk-your-api-key-here",
    "baseUrl": "https://api.openai.com/v1"
  }
}
```

### Using Local LLM (Ollama)

1. Install Ollama: https://ollama.ai/
2. Pull a model:
   ```bash
   ollama pull mistral
   ```
3. Configure the mod:
   ```json
   {
     "llm": {
       "provider": "local",
       "model": "mistral",
       "apiKey": "",
       "baseUrl": "http://localhost:11434"
     }
   }
   ```

## Testing the Mod

### 1. Start the Game

```bash
./gradlew runClient
```

### 2. Check Mod Loaded

- In Minecraft main menu, click "Mods"
- Look for "AI Minecraft Player v0.1.0-alpha"
- Should appear in the mod list

### 3. Test Commands

Create or join a world, then test commands:

```
/aiplayer help
/aiplayer status
/aiplayer list
```

**Expected Output:**
```
[AI Player] Commands:
  /aiplayer spawn - Spawn an AI player
  /aiplayer list - List active AI players
  /aiplayer status - Show mod status
  /aiplayer reload - Reload configuration
  /aiplayer help - Show this help
```

### 4. Check Logs

Logs are written to:
- Development: `run/logs/latest.log`
- Production: `.minecraft/logs/latest.log`

Look for:
```
[aiplayer] Initializing AI Minecraft Player v0.1.0-alpha
[aiplayer] Configuration loaded from: config/aiplayer.json
[aiplayer] AI Minecraft Player initialized successfully
```

## Installing the Mod

### For Testing (Singleplayer or Local Server)

1. Build the mod:
   ```bash
   ./gradlew build
   ```

2. Copy the JAR to your Minecraft mods folder:
   ```bash
   cp build/libs/ai-minecraft-player-0.1.0-alpha.jar ~/.minecraft/mods/
   ```

3. Ensure Fabric Loader is installed:
   - Download Fabric Installer: https://fabricmc.net/use/installer/
   - Run installer and select Minecraft 1.20.4

4. Launch Minecraft with Fabric profile

### For Servers

1. Install Fabric on the server:
   ```bash
   java -jar fabric-server-launch.jar
   ```

2. Copy mod JAR to server's `mods/` folder

3. Start the server

4. OP yourself to use commands:
   ```
   op YourUsername
   ```

## Troubleshooting

### Build Fails with "Java version" error

**Error:** `Unsupported class file major version`

**Solution:** Install JDK 17 or higher
```bash
java -version  # Should show 17.x.x or higher
```

### Build Fails with Dependency Errors

**Error:** `Could not resolve dependency`

**Solution:** Refresh dependencies
```bash
./gradlew clean build --refresh-dependencies
```

### Minecraft Won't Start

**Error:** Crashes on startup

**Solutions:**
1. Check logs in `run/logs/latest.log`
2. Ensure Fabric API is installed (included as dependency)
3. Verify Minecraft version is 1.20.4
4. Try clean build:
   ```bash
   ./gradlew clean
   ./gradlew runClient
   ```

### Mod Not Appearing in Mods List

**Solutions:**
1. Verify mod JAR is in `mods/` folder
2. Check JAR filename doesn't have spaces
3. Ensure Fabric Loader is installed
4. Check Fabric version compatibility

### Commands Not Working

**Error:** `/aiplayer` command not found

**Solutions:**
1. Verify you have OP permissions (singleplayer or server OP)
2. Check mod loaded in mods list
3. Try `/aiplayer help` explicitly
4. Check logs for initialization errors

### Configuration Not Loading

**Error:** Configuration errors in logs

**Solutions:**
1. Check `config/aiplayer.json` is valid JSON
2. Delete config and let mod recreate default
3. Check file permissions
4. Review logs for specific error messages

## Development Workflow

### Making Changes

1. **Edit Code**
   - Make changes in `src/main/java/`

2. **Hot Reload (Limited)**
   - Some changes can be reloaded without restart
   - Most changes require game restart

3. **Test Changes**
   ```bash
   ./gradlew runClient
   ```

4. **Run Tests**
   ```bash
   ./gradlew test
   ```

5. **Commit Changes**
   ```bash
   git add .
   git commit -m "Description of changes"
   git push
   ```

### Adding Dependencies

Edit `build.gradle`:

```gradle
dependencies {
    // Add new dependency
    implementation 'group:artifact:version'
}
```

Then refresh:
```bash
./gradlew build --refresh-dependencies
```

## Build Artifacts

After `./gradlew build`, you'll find:

```
build/
├── libs/
│   ├── ai-minecraft-player-0.1.0-alpha.jar       # Main mod JAR
│   └── ai-minecraft-player-0.1.0-alpha-sources.jar # Sources JAR
├── classes/                                       # Compiled classes
└── tmp/                                          # Temporary files
```

The main JAR is what you install in Minecraft.

## Performance

### Build Time
- **First build:** 2-5 minutes (downloads dependencies)
- **Subsequent builds:** 10-30 seconds
- **Clean build:** 20-60 seconds

### Runtime
- **Current implementation:** <1% CPU, <256MB RAM
- **Target:** <5% CPU, <512MB RAM per AI player

## Current Status

### ✅ Working
- Mod loads and initializes
- Configuration system functional
- Commands registered and working
- Logging system operational

### ⏳ In Progress
- AI player entity implementation (Phase 1, Task 1.5)

### 🔮 Not Yet Implemented
- AI player spawning
- Movement system
- Perception system
- LLM integration
- Planning and goals
- Chat communication

See [ROADMAP.md](ROADMAP.md) for complete development plan.

## Getting Help

### Documentation
- [PROJECT_PLAN.md](PROJECT_PLAN.md) - Architecture overview
- [TECHNICAL_SPEC.md](TECHNICAL_SPEC.md) - Implementation details
- [ROADMAP.md](ROADMAP.md) - Detailed task list
- [QUICK_START.md](QUICK_START.md) - Setup guide

### Community Resources
- Fabric Wiki: https://fabricmc.net/wiki/
- Fabric Discord: https://discord.gg/v6v4pMv
- Minecraft Modding subreddit: r/fabricmc

### Issues
- Report bugs: https://github.com/gatewaybuddy/AI_Minecraft_Players/issues
- Check existing issues before reporting

## Next Steps

Now that you have the mod building:

1. Read [GETTING_STARTED.md](GETTING_STARTED.md) for next development steps
2. Check [ROADMAP.md](ROADMAP.md) Task 1.5 for implementing the AI player entity
3. Join development and start coding!

## License

This project is licensed under the MIT License - see [LICENSE](LICENSE) file for details.
