# Building AI Minecraft Players

This document describes how to build the AI Minecraft Players mod from source.

## Prerequisites

### Required Software

1. **Java 17 or higher**
   ```bash
   java -version  # Should show 17 or higher
   ```

2. **Gradle 8.0+** (or use the Gradle wrapper)
   ```bash
   gradle --version
   ```

3. **Git** (for cloning the repository)
   ```bash
   git --version
   ```

### Minecraft Development Environment

The project requires:
- Minecraft 1.20.4
- Fabric Loader 0.15.3+
- Fabric API 0.95.4+
- Fabric Loom 1.5+

These are automatically downloaded by Gradle during the build process.

## Quick Build

### Using Gradle Wrapper (Recommended)

```bash
# Clone the repository
git clone https://github.com/your-username/AI_Minecraft_Players.git
cd AI_Minecraft_Players

# Build the mod (first build may take 5-10 minutes)
./gradlew build

# The compiled JAR will be in: build/libs/
ls -lh build/libs/
```

### Using System Gradle

```bash
# If you have Gradle installed system-wide
gradle build

# Output JAR location
ls -lh build/libs/
```

## Build Output

After a successful build, you'll find two JAR files in `build/libs/`:

```
ai-minecraft-player-0.1.0-SNAPSHOT.jar        # Main mod file (use this one)
ai-minecraft-player-0.1.0-SNAPSHOT-sources.jar # Source code (for reference)
```

## Development Build Tasks

### Common Gradle Tasks

```bash
# Clean previous build artifacts
./gradlew clean

# Build without running tests
./gradlew build -x test

# Run tests only
./gradlew test

# Generate sources JAR
./gradlew sourcesJar

# Run the mod in a development environment
./gradlew runServer  # Starts a test server
./gradlew runClient  # Starts a test client
```

### IDE Setup

#### IntelliJ IDEA

```bash
# Generate IntelliJ project files
./gradlew idea

# Then open the project in IntelliJ IDEA
# File -> Open -> select AI_Minecraft_Players directory
```

The Fabric Loom plugin will automatically configure:
- Minecraft sources
- Fabric API
- Run configurations

#### Eclipse

```bash
# Generate Eclipse project files
./gradlew eclipse

# Then import the project in Eclipse
# File -> Import -> Existing Projects into Workspace
```

#### Visual Studio Code

```bash
# Install Java Extension Pack
# Open the project folder
# VS Code will automatically detect Gradle and configure
```

## Build Configuration

### Modifying Versions

Edit `gradle.properties` to change versions:

```properties
# Minecraft and Fabric versions
minecraft_version=1.20.4
yarn_mappings=1.20.4+build.3
loader_version=0.15.3
fabric_version=0.95.4+1.20.4

# Mod version
mod_version=0.1.0-SNAPSHOT
```

### Adjusting JVM Memory

For large builds, increase Gradle's memory:

```properties
# In gradle.properties
org.gradle.jvmargs=-Xmx4G
```

Or use environment variable:
```bash
export GRADLE_OPTS="-Xmx4G"
./gradlew build
```

## Troubleshooting Build Issues

### Issue: "Could not resolve dependencies"

**Solution**: Check your internet connection and try:
```bash
./gradlew build --refresh-dependencies
```

### Issue: "Unsupported class file major version"

**Problem**: Java version mismatch

**Solution**: Ensure Java 17+ is installed and used:
```bash
java -version
export JAVA_HOME=/path/to/java17
./gradlew build
```

### Issue: "Fabric Loom plugin not found"

**Solution**: The snapshot repository needs to be configured. This is already done in `settings.gradle`:

```groovy
pluginManagement {
    repositories {
        maven {
            name = 'Fabric'
            url = 'https://maven.fabricmc.net/'
        }
        maven {
            name = 'Fabric Snapshots'
            url = 'https://maven.fabricmc.net/snapshots'
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
```

If still failing, try:
```bash
./gradlew clean build --refresh-dependencies
```

### Issue: "Out of memory during compilation"

**Solution**: Increase JVM memory:
```bash
export GRADLE_OPTS="-Xmx4G -XX:MaxMetaspaceSize=512m"
./gradlew build
```

### Issue: "Tests failing"

**Solution**: Skip tests during build:
```bash
./gradlew build -x test
```

Then investigate test failures separately:
```bash
./gradlew test --info
```

## Building for Production

### Create Release Build

```bash
# Clean previous builds
./gradlew clean

# Build with all checks
./gradlew build

# Output JAR
cp build/libs/ai-minecraft-player-*.jar release/
```

### Version Numbering

Update version in `gradle.properties` before release:

```properties
# Development
mod_version=0.1.0-SNAPSHOT

# Release
mod_version=0.1.0

# Next release
mod_version=0.2.0-SNAPSHOT
```

## Build Performance

### Faster Builds

Enable parallel builds and caching in `gradle.properties`:

```properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
```

### Gradle Daemon

The daemon speeds up subsequent builds:

```bash
# Check daemon status
./gradlew --status

# Stop daemon (if issues occur)
./gradlew --stop
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Build

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Build with Gradle
        run: ./gradlew build

      - name: Upload artifacts
        uses: actions/upload-artifact@v3
        with:
          name: mod-jar
          path: build/libs/*.jar
```

## Dependencies

The mod automatically includes these dependencies:

- **OkHttp 4.12.0**: HTTP client for LLM APIs
- **Gson 2.10.1**: JSON parsing
- **Caffeine 3.1.8**: Caching library
- **SLF4J 2.0.9**: Logging facade

These are bundled in the final JAR automatically by Gradle.

## Next Steps

After building:

1. Test the mod in a development environment:
   ```bash
   ./gradlew runServer
   ```

2. Install on a real server (see `INSTALL.md`)

3. Configure LLM provider (see `README.md`)

4. Monitor logs for any issues

## Getting Help

- **Build issues**: Check Fabric Discord or open an issue
- **Gradle help**: See [Gradle documentation](https://docs.gradle.org/)
- **Fabric help**: See [Fabric Wiki](https://fabricmc.net/wiki/)

## License

This project is licensed under the MIT License - see the LICENSE file for details.
