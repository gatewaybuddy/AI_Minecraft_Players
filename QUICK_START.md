# Quick Start Guide - AI Minecraft Player

## Prerequisites

1. **Java Development Kit (JDK) 17 or higher**
   ```bash
   java -version  # Should show 17+
   ```

2. **Minecraft Java Edition** (1.20.x recommended)

3. **IDE** (IntelliJ IDEA recommended)
   - Download: https://www.jetbrains.com/idea/download/

4. **LLM API Key** (choose one)
   - OpenAI API key: https://platform.openai.com/api-keys
   - Anthropic API key: https://console.anthropic.com/
   - OR install Ollama for local models: https://ollama.ai/

## Setup Development Environment

### 1. Create Fabric Mod Project

```bash
# Clone template
git clone https://github.com/FabricMC/fabric-example-mod.git ai-minecraft-player
cd ai-minecraft-player

# Or use this project structure
mkdir -p src/main/java/com/aiplayer
mkdir -p src/main/resources
```

### 2. Configure build.gradle

```gradle
plugins {
    id 'fabric-loom' version '1.4-SNAPSHOT'
    id 'maven-publish'
}

version = project.mod_version
group = project.maven_group

repositories {
    mavenCentral()
}

dependencies {
    // Fabric
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"

    // HTTP client for LLM APIs
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'

    // JSON parsing
    implementation 'com.google.code.gson:gson:2.10.1'

    // Caching
    implementation 'com.github.ben-manes.caffeine:caffeine:3.1.8'

    // Logging
    implementation 'org.slf4j:slf4j-api:2.0.9'

    // Testing
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
}

test {
    useJUnitPlatform()
}

processResources {
    inputs.property "version", project.version
    filesMatching("fabric.mod.json") {
        expand "version": project.version
    }
}

tasks.withType(JavaCompile).configureEach {
    it.options.release = 17
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

jar {
    from("LICENSE") {
        rename { "${it}_${project.archivesBaseName}"}
    }
}
```

### 3. Configure gradle.properties

```properties
# Fabric Properties
minecraft_version=1.20.4
yarn_mappings=1.20.4+build.3
loader_version=0.15.3

# Mod Properties
mod_version=0.1.0
maven_group=com.aiplayer
archives_base_name=ai-minecraft-player

# Dependencies
fabric_version=0.91.2+1.20.4
```

### 4. Create fabric.mod.json

```json
{
  "schemaVersion": 1,
  "id": "aiplayer",
  "version": "${version}",
  "name": "AI Minecraft Player",
  "description": "Adds autonomous AI players to Minecraft",
  "authors": [
    "Your Name"
  ],
  "contact": {
    "homepage": "https://github.com/yourusername/ai-minecraft-player",
    "sources": "https://github.com/yourusername/ai-minecraft-player"
  },
  "license": "MIT",
  "icon": "assets/aiplayer/icon.png",
  "environment": "*",
  "entrypoints": {
    "main": [
      "com.aiplayer.AIPlayerMod"
    ]
  },
  "mixins": [
    "aiplayer.mixins.json"
  ],
  "depends": {
    "fabricloader": ">=0.15.0",
    "minecraft": "~1.20.4",
    "java": ">=17",
    "fabric-api": "*"
  }
}
```

## Phase 1: Minimal Working Example

### Step 1: Create Main Mod Class

**src/main/java/com/aiplayer/AIPlayerMod.java**

```java
package com.aiplayer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AIPlayerMod implements ModInitializer {
    public static final String MOD_ID = "aiplayer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("AI Minecraft Player mod initializing...");

        // Register commands
        registerCommands();

        LOGGER.info("AI Minecraft Player mod initialized!");
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("aiplayer")
                .then(CommandManager.literal("spawn")
                    .executes(context -> {
                        context.getSource().sendFeedback(
                            () -> Text.literal("Spawning AI player..."),
                            false
                        );
                        // TODO: Implement AI player spawning
                        return 1;
                    })
                )
                .then(CommandManager.literal("status")
                    .executes(context -> {
                        context.getSource().sendFeedback(
                            () -> Text.literal("AI Player status: Not implemented"),
                            false
                        );
                        return 1;
                    })
                )
            );
        });
    }
}
```

### Step 2: Create Config System

**src/main/java/com/aiplayer/config/AIPlayerConfig.java**

```java
package com.aiplayer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AIPlayerConfig {
    public String username = "AISteve";
    public String personality = "helpful and friendly";
    public LLMConfig llm = new LLMConfig();

    public static class LLMConfig {
        public String provider = "openai";
        public String model = "gpt-4";
        public String apiKey = "";
        public String baseUrl = "https://api.openai.com/v1";
    }

    public static AIPlayerConfig load(Path configPath) throws IOException {
        if (!Files.exists(configPath)) {
            AIPlayerConfig defaultConfig = new AIPlayerConfig();
            defaultConfig.save(configPath);
            return defaultConfig;
        }

        String json = Files.readString(configPath);
        return new Gson().fromJson(json, AIPlayerConfig.class);
    }

    public void save(Path configPath) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(this);
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath, json);
    }
}
```

### Step 3: Build and Test

```bash
# Build the mod
./gradlew build

# The built mod will be in build/libs/
# Copy it to your Minecraft mods folder
cp build/libs/ai-minecraft-player-0.1.0.jar ~/.minecraft/mods/
```

### Step 4: Run in Development

```bash
# Run Minecraft client with your mod
./gradlew runClient

# Or run a server
./gradlew runServer
```

## Development Workflow

### 1. Daily Development Cycle

```bash
# Make changes to code
# ...

# Build and test
./gradlew build

# Run in development
./gradlew runClient

# Check logs
tail -f run/logs/latest.log
```

### 2. Testing AI Features

```java
// Create a test class
@Test
public void testAIPlayerSpawn() {
    // Your test code
}

// Run tests
./gradlew test
```

### 3. Debugging

In IntelliJ IDEA:
1. Set up Gradle run configuration
2. Add breakpoints in your code
3. Run in Debug mode
4. Step through AI decision-making

## Configuration

### Example config.json

Create in `.minecraft/config/aiplayer.json`:

```json
{
  "username": "AIHelper",
  "personality": "friendly assistant who loves mining",
  "llm": {
    "provider": "openai",
    "model": "gpt-4-turbo-preview",
    "apiKey": "sk-your-api-key-here",
    "baseUrl": "https://api.openai.com/v1"
  },
  "behavior": {
    "reactionTimeMs": 200,
    "chatEnabled": true,
    "autoRespawn": true
  },
  "goals": {
    "defaultGoal": "explore and gather resources",
    "acceptPlayerRequests": true
  }
}
```

### Using Local LLM (Ollama)

```bash
# Install Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# Pull a model
ollama pull mistral

# Update config
{
  "llm": {
    "provider": "local",
    "model": "mistral",
    "baseUrl": "http://localhost:11434"
  }
}
```

## Common Commands

```bash
# In Minecraft chat:
/aiplayer spawn              # Spawn an AI player
/aiplayer goal AISteve "mine 64 oak logs"  # Set a goal
/aiplayer status AISteve     # Check status
/aiplayer stop AISteve       # Stop AI player
```

## Troubleshooting

### Mod doesn't load
- Check Fabric API is installed
- Verify Java 17+
- Check logs in `run/logs/latest.log`

### AI player doesn't spawn
- Check config file is valid JSON
- Verify LLM API key is set
- Check server logs for errors

### LLM calls failing
- Verify API key is correct
- Check internet connection
- Try local LLM with Ollama

### Performance issues
- Enable action caching
- Use faster model (GPT-3.5, Claude Haiku)
- Reduce update frequency

## Next Steps

After getting the basic mod working:

1. **Implement FakePlayer entity** (see TECHNICAL_SPEC.md §5.1)
2. **Add basic movement** (see TECHNICAL_SPEC.md §2.2)
3. **Integrate LLM** (see TECHNICAL_SPEC.md §3.1)
4. **Add chat listener** (see TECHNICAL_SPEC.md §5.2)
5. **Implement pathfinding** (see TECHNICAL_SPEC.md §4.1)

## Resources

- **Fabric Wiki**: https://fabricmc.net/wiki/
- **Minecraft Forge Docs**: https://docs.minecraftforge.net/
- **Baritone (pathfinding)**: https://github.com/cabaletta/baritone
- **Voyager (LLM agent)**: https://github.com/MineDojo/Voyager
- **MineDojo**: https://github.com/MineDojo/MineDojo

## Getting Help

- Check PROJECT_PLAN.md for architecture overview
- Check TECHNICAL_SPEC.md for implementation details
- Review Fabric documentation
- Look at example mods on GitHub

## Development Roadmap

Follow the phases in PROJECT_PLAN.md:
- ✅ **Phase 0**: Planning & Research (You are here!)
- 🔄 **Phase 1**: Foundation (Weeks 1-3)
- ⏳ **Phase 2**: Action System (Weeks 4-6)
- ⏳ **Phase 3**: Memory & Planning (Weeks 7-9)
- ⏳ **Phase 4**: Communication (Weeks 10-11)
- ⏳ **Phase 5**: Advanced AI (Weeks 12-14)
- ⏳ **Phase 6**: Polish (Weeks 15-16)

Good luck building your AI Minecraft player! 🎮🤖
