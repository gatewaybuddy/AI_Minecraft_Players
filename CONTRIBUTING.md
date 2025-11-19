# Contributing to AI Minecraft Players

Thank you for considering contributing to AI Minecraft Players! This document provides guidelines and instructions for contributing to the project.

---

## Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [Getting Started](#getting-started)
3. [Development Setup](#development-setup)
4. [How to Contribute](#how-to-contribute)
5. [Coding Standards](#coding-standards)
6. [Testing](#testing)
7. [Pull Request Process](#pull-request-process)
8. [Project Structure](#project-structure)
9. [Questions](#questions)

---

## Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inspiring community for all. Please be respectful and constructive in all interactions.

### Expected Behavior

- Be respectful of differing viewpoints and experiences
- Give and accept constructive feedback gracefully
- Focus on what is best for the community
- Show empathy towards other community members

### Unacceptable Behavior

- Harassment, discrimination, or derogatory comments
- Trolling, insulting/derogatory comments, and personal attacks
- Publishing others' private information
- Other conduct which could reasonably be considered inappropriate

---

## Getting Started

### Prerequisites

Before contributing, ensure you have:

- **Java 17 or higher** installed
- **Git** for version control
- **An IDE** (IntelliJ IDEA recommended)
- **Minecraft 1.20.4** with Fabric for testing
- Basic understanding of **Minecraft modding** and **Fabric framework**

### Familiarize Yourself

1. Read the [README.md](README.md) for project overview
2. Review [PROJECT_PLAN.md](PROJECT_PLAN.md) for architecture
3. Check [TECHNICAL_SPEC.md](TECHNICAL_SPEC.md) for implementation details
4. Browse existing issues on GitHub

---

## Development Setup

### 1. Fork and Clone

```bash
# Fork the repository on GitHub
# Then clone your fork
git clone https://github.com/YOUR_USERNAME/AI_Minecraft_Players.git
cd AI_Minecraft_Players
```

### 2. Build the Project

```bash
# Build with Gradle
./gradlew build

# Or use system Gradle if wrapper fails
gradle build
```

**Note**: Build requires internet access to download Fabric Loom plugin. See [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md) for troubleshooting.

### 3. Run Tests

```bash
./gradlew test
```

### 4. Set Up IDE

**IntelliJ IDEA** (Recommended):
1. Open project folder
2. IDEA will auto-detect Gradle project
3. Wait for Gradle sync to complete
4. Set Project SDK to Java 17+
5. Enable annotation processing (for @Override, etc.)

**Eclipse**:
1. Import as Gradle project
2. Set Java compiler to 17+
3. Run Gradle refresh

---

## How to Contribute

### Types of Contributions

We welcome:
- **Bug fixes** - Fix issues in existing code
- **New features** - Add functionality (discuss first in issues)
- **Documentation** - Improve or add documentation
- **Tests** - Add unit tests or integration tests
- **Performance** - Optimize existing code
- **Examples** - Add usage examples or tutorials

### Before You Start

1. **Check existing issues** - Avoid duplicate work
2. **Create an issue** - Discuss major changes before implementing
3. **Assign yourself** - Let others know you're working on it

### Making Changes

1. **Create a branch** from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/bug-description
   ```

2. **Make your changes** following coding standards

3. **Write tests** for new functionality

4. **Update documentation** as needed

5. **Commit your changes**:
   ```bash
   git add .
   git commit -m "Add feature: your feature description"
   ```

---

## Coding Standards

### Java Style

Follow standard Java conventions:

**Naming**:
- **Classes**: `PascalCase` (e.g., `MemorySystem`, `AIPlayerBrain`)
- **Methods**: `camelCase` (e.g., `storeMemory`, `generateSkill`)
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `MAX_MEMORIES`, `DEFAULT_PRIORITY`)
- **Variables**: `camelCase` (e.g., `skillLibrary`, `worldState`)

**Formatting**:
- **Indentation**: 4 spaces (no tabs)
- **Line length**: 120 characters maximum
- **Braces**: Opening brace on same line
- **Whitespace**: Space after keywords (`if (condition)` not `if(condition)`)

**Example**:
```java
public class ExampleClass {

    private static final int MAX_ITEMS = 100;
    private final List<Item> items;

    public ExampleClass() {
        this.items = new ArrayList<>();
    }

    public void addItem(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }

        if (items.size() >= MAX_ITEMS) {
            removeOldestItem();
        }

        items.add(item);
    }
}
```

### Documentation

**Javadoc for Public APIs**:
```java
/**
 * Generates a new skill from successful action sequence.
 *
 * @param actionSequence List of actions that succeeded
 * @param goalAchieved What was accomplished
 * @param worldState Current world state
 * @return Future containing generated skill (or null if generation failed)
 */
public CompletableFuture<Skill> generateFromSuccess(
        List<String> actionSequence,
        String goalAchieved,
        WorldState worldState) {
    // Implementation...
}
```

**Inline Comments**:
- Explain **why**, not **what**
- Use comments for complex logic only
- Keep comments up to date with code changes

### Code Organization

**Package Structure**:
```
com.aiplayer
├── core (Entity, Manager, Brain)
├── action (Controllers for actions)
├── perception (World state, scanning)
├── planning (Goals, tasks, planning engine)
├── memory (Memory system)
├── llm (LLM providers)
├── skills (Skill library, executor, generator)
├── communication (Chat, NLU, responses)
├── knowledge (World knowledge)
├── coordination (Multi-AI collaboration)
├── config (Configuration)
└── command (Commands)
```

**File Naming**:
- One public class per file
- Filename matches class name
- Group related classes in same package

---

## Testing

### Writing Tests

**Test Structure**:
```java
@Test
void testFeatureDescription() {
    // Arrange - Set up test data
    MemorySystem memorySystem = new MemorySystem(1000);
    Memory testMemory = new Memory(Memory.MemoryType.OBSERVATION, "test", 0.8);

    // Act - Execute the functionality
    memorySystem.store(testMemory);

    // Assert - Verify the results
    List<Memory> recalled = memorySystem.recall("test", 5);
    assertFalse(recalled.isEmpty(), "Should recall stored memory");
}
```

**Test Coverage**:
- Aim for 30-50% code coverage minimum
- Focus on critical paths and public APIs
- Test edge cases and error conditions
- Use descriptive test method names

**Running Tests**:
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests MemorySystemTest

# Run with coverage report
./gradlew test jacocoTestReport
```

### Test Guidelines

- **Unit tests** - Test individual components in isolation
- **Integration tests** - Test component interactions
- **Use mocks** sparingly (prefer real objects when possible)
- **Independent tests** - Each test should run independently
- **Fast tests** - Tests should run quickly (< 1s each)

---

## Pull Request Process

### Before Submitting

1. ✅ **Code compiles** without errors
2. ✅ **All tests pass** (`./gradlew test`)
3. ✅ **No new warnings** introduced
4. ✅ **Code follows** coding standards
5. ✅ **Documentation** updated (if applicable)
6. ✅ **CHANGELOG.md** updated (for notable changes)

### Submitting PR

1. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```

2. **Create Pull Request** on GitHub:
   - Clear title describing the change
   - Reference related issues (#123)
   - Describe what changed and why
   - List any breaking changes

3. **PR Description Template**:
   ```markdown
   ## Description
   Brief description of what this PR does.

   ## Related Issue
   Fixes #123

   ## Changes Made
   - Added feature X
   - Fixed bug Y
   - Updated documentation Z

   ## Testing
   - Added unit tests for new functionality
   - All existing tests pass
   - Tested manually in Minecraft 1.20.4

   ## Checklist
   - [ ] Code compiles without errors
   - [ ] Tests pass
   - [ ] Documentation updated
   - [ ] CHANGELOG.md updated
   ```

### Review Process

1. **Automated checks** will run (if configured)
2. **Maintainers** will review your code
3. **Address feedback** by pushing new commits
4. **PR will be merged** once approved

### After Merge

- Your contribution will be included in the next release
- You'll be credited in release notes
- Thank you for contributing! 🎉

---

## Project Structure

### Key Files

```
AI_Minecraft_Players/
├── src/
│   ├── main/java/com/aiplayer/  # Source code
│   ├── test/java/com/aiplayer/  # Unit tests
│   └── main/resources/          # Mod resources
├── docs/                        # Additional documentation
├── build.gradle                 # Build configuration
├── gradle.properties            # Gradle properties
├── README.md                    # Project overview
├── CHANGELOG.md                 # Version history
├── CONTRIBUTING.md              # This file
├── LICENSE                      # MIT License
└── *.md                         # Various documentation files
```

### Important Modules

- **core** - Core AI player logic
- **action** - Player action controllers
- **planning** - Goal planning and task decomposition
- **memory** - Multi-tier memory system
- **llm** - LLM provider integrations
- **skills** - Skill library and learning
- **communication** - Natural language chat
- **knowledge** - World knowledge tracking
- **coordination** - Multi-AI collaboration

---

## Common Tasks

### Adding a New Feature

1. Create issue to discuss feature
2. Create branch: `feature/feature-name`
3. Implement feature with tests
4. Update documentation
5. Update CHANGELOG.md
6. Submit PR

### Fixing a Bug

1. Create issue (if not exists) describing bug
2. Create branch: `fix/bug-description`
3. Fix bug with regression test
4. Update CHANGELOG.md
5. Submit PR

### Adding Documentation

1. Create branch: `docs/what-youre-documenting`
2. Add or update documentation
3. Ensure links work
4. Submit PR

### Improving Performance

1. Profile to identify bottleneck
2. Create issue with profiling results
3. Implement optimization
4. Add benchmark tests
5. Document performance improvements
6. Submit PR

---

## Questions?

### Get Help

- **Issues**: Create an issue on GitHub for questions
- **Discussions**: Use GitHub Discussions for general chat
- **Documentation**: Check existing docs first

### Reporting Bugs

When reporting bugs, include:
- **Minecraft version** (1.20.4)
- **Mod version** (from build.gradle or JAR name)
- **Fabric Loader version**
- **Steps to reproduce** the bug
- **Expected behavior** vs actual behavior
- **Logs** if applicable (from `.minecraft/logs/`)
- **Configuration** if relevant (sanitize API keys!)

### Suggesting Features

When suggesting features:
- **Clear description** of what you want
- **Use case** - why is this useful?
- **Examples** of how it would work
- **Alternatives** you've considered

---

## License

By contributing to AI Minecraft Players, you agree that your contributions will be licensed under the MIT License.

---

## Recognition

Contributors will be recognized in:
- Release notes (CHANGELOG.md)
- GitHub contributors page
- Special thanks section (for major contributions)

---

## Thank You!

Thank you for contributing to AI Minecraft Players! Your help makes this project better for everyone. 🎉

**Happy coding!** 🚀

---

**Maintained by**: AI Minecraft Players Team
**Last Updated**: November 2025
