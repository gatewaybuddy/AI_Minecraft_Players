# Build Instructions

## Prerequisites

- Java JDK 17 or higher
- Internet connection (for first build to download dependencies)
- Minecraft Java Edition 1.20.4 (for testing)

## Building the Mod

### First Time Setup

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/AI_Minecraft_Players.git
   cd AI_Minecraft_Players
   ```

2. **Build the mod** (requires internet for dependency download):
   ```bash
   ./gradlew build
   ```

   On Windows:
   ```cmd
   gradlew.bat build
   ```

3. **The built JAR will be in**: `build/libs/ai-minecraft-player-0.1.0-SNAPSHOT.jar`

### Testing in Development

Run the Minecraft client with the mod loaded:

```bash
./gradlew runClient
```

Run a dedicated server with the mod:

```bash
./gradlew runServer
```

## Configuration

Before running, configure your LLM API key:

1. Copy the default configuration:
   ```bash
   cp src/main/resources/data/aiplayer/config/default.json config/aiplayer.json
   ```

2. Edit `config/aiplayer.json` and add your API key:
   ```json
   {
     "llm": {
       "provider": "openai",
       "apiKey": "sk-your-actual-api-key-here"
     }
   }
   ```

3. **IMPORTANT**: Never commit `config/aiplayer.json` with your actual API key!

## Build Issues

### Issue: "Plugin 'fabric-loom' was not found"

**Solution**: This happens when Gradle can't download the Fabric Loom plugin. Ensure you have internet access on first build.

### Issue: "Cannot resolve dependencies"

**Solution**: Run with `--refresh-dependencies`:
```bash
./gradlew build --refresh-dependencies
```

### Issue: Build fails with out of memory error

**Solution**: Increase Gradle memory in `gradle.properties`:
```
org.gradle.jvmargs=-Xmx4G
```

## Development Workflow

1. **Make code changes** in `src/main/java/`
2. **Build**: `./gradlew build`
3. **Test**: `./gradlew runClient`
4. **Debug**: Use IntelliJ IDEA's built-in Gradle integration

## Installing the Mod

### For Testing

The mod is automatically loaded when running `./gradlew runClient`.

### For Use in Regular Minecraft

1. Install Fabric Loader for Minecraft 1.20.4
2. Download Fabric API from https://www.curseforge.com/minecraft/mc-mods/fabric-api
3. Copy `build/libs/ai-minecraft-player-0.1.0-SNAPSHOT.jar` to `.minecraft/mods/`
4. Copy Fabric API JAR to `.minecraft/mods/`
5. Launch Minecraft with the Fabric profile

## Current Implementation Status

As of the current commit, the following is implemented:

### ✅ Completed (Phase 1 - Foundation, Partial)
- [x] Project structure and Gradle build setup
- [x] Configuration system (`AIPlayerConfig.java`)
- [x] Main mod entry point (`AIPlayerMod.java`)
- [x] AI Player Manager stub (`AIPlayerManager.java`)
- [x] Package structure for all modules

### 🚧 In Progress
- [ ] FakePlayer entity creation (Task 1.5)
- [ ] Movement controller (Task 1.6)
- [ ] Perception system (Task 1.7)
- [ ] Commands (`/aiplayer spawn`, etc.) (Task 1.8)

### ⏳ Not Started
- [ ] Remaining Phase 1 tasks
- [ ] Phases 2-6

See `ROADMAP.md` for the complete task list.

## Getting Help

- Check `QUICK_START.md` for detailed setup instructions
- Review `TECHNICAL_SPEC.md` for implementation details
- See `TROUBLESHOOTING.md` for common issues (to be created)
- Open an issue on GitHub if you encounter problems

## Contributing

Contributions are welcome! Please:

1. Follow the architecture defined in `TECHNICAL_SPEC.md`
2. Check `ROADMAP.md` for available tasks
3. Write tests for new features
4. Submit a pull request

## License

MIT License - see LICENSE file for details
