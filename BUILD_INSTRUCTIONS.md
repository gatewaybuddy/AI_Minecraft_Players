# Build Instructions - AI Minecraft Players

**For Developers**: Complete guide to building the mod from source

---

## Prerequisites

### Required Software

- **Java Development Kit (JDK) 17 or higher**
  - Download: https://adoptium.net/
  - Verify: `java -version` and `javac -version`

- **Git** (for cloning repository)
  - Download: https://git-scm.com/
  - Verify: `git --version`

- **8 GB RAM minimum** (for Gradle build)

---

## Quick Build

\`\`\`bash
# Clone repository
git clone https://github.com/gatewaybuddy/AI_Minecraft_Players.git
cd AI_Minecraft_Players

# Build mod
./gradlew build        # Unix/Linux/Mac
gradlew.bat build      # Windows

# Find output
ls -l build/libs/
# Output: ai-minecraft-player-0.1.0-SNAPSHOT.jar
\`\`\`

---

## Development Setup

### IntelliJ IDEA (Recommended)

1. Install Minecraft Development plugin
2. Import project as Gradle project
3. Run: `./gradlew genSources`
4. Use Gradle tasks: runClient, runServer

### Building

\`\`\`bash
# Clean build
./gradlew clean build

# Run Minecraft client with mod
./gradlew runClient

# Run tests
./gradlew test
\`\`\`

---

## Output

Built JAR: `build/libs/ai-minecraft-player-0.1.0-SNAPSHOT.jar`

Install by copying to `.minecraft/mods/` folder.

**Happy Building!** 🚀
