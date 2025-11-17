# Production Completion Plan - AI Minecraft Players

**Created**: 2025-11-17
**Goal**: Complete remaining work to deliver drop-in mod with cooperating AI players
**Current Status**: Phase 3 Complete → Need Phase 4 + Build/Distribution
**Target**: MVP Release in 4-6 weeks

---

## I. Executive Summary

### What We Have ✅
- **7,300 lines** of production-quality Java code
- **Complete AI player implementation** with autonomous behavior
- **Full action system**: mining, building, combat, crafting, inventory
- **LLM-powered planning** with ReAct framework
- **Hierarchical memory** system (working, episodic, semantic)
- **Multi-provider LLM** support (OpenAI, Claude, Ollama)
- **Comprehensive documentation** (16 files)

### What We Need ❌
1. **Natural language chat system** (Phase 4) - CRITICAL
2. **Build system verification** and distribution package
3. **Basic testing** and stability verification
4. **User installation guide** for non-developers

### Timeline
- **Aggressive**: 4 weeks (MVP only)
- **Conservative**: 6 weeks (MVP + polish)
- **Full**: 8-12 weeks (MVP + Phase 5 features)

---

## II. Phase 4: Natural Language Communication

**Priority**: P0 (CRITICAL)
**Estimated Effort**: 2-3 weeks
**Why Critical**: Without this, AI cannot cooperate with human players

### 4.1 Chat Event Listener

**File**: `src/main/java/com/aiplayer/communication/ChatListener.java`
**Lines**: ~150
**Time**: 1 day

```java
package com.aiplayer.communication;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Listens to server chat events and routes messages to AI players.
 */
public class ChatListener {

    private final AIPlayerManager playerManager;
    private final MessageProcessor processor;

    public ChatListener(AIPlayerManager playerManager) {
        this.playerManager = playerManager;
        this.processor = new MessageProcessor();
    }

    /**
     * Register chat event handlers.
     */
    public void register() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            handleChatMessage(sender, message.getContent().getString());
        });
    }

    /**
     * Process incoming chat message.
     */
    private void handleChatMessage(ServerPlayerEntity sender, String message) {
        // Don't respond to AI's own messages
        if (playerManager.isAIPlayer(sender.getUuid())) {
            return;
        }

        // Check if any AI player is addressed
        for (AIPlayerEntity aiPlayer : playerManager.getAllPlayers()) {
            if (isAddressed(message, aiPlayer.getName().getString())) {
                processor.processMessage(message, sender, aiPlayer);
            }
        }
    }

    /**
     * Check if message addresses this AI player.
     */
    private boolean isAddressed(String message, String aiName) {
        String lower = message.toLowerCase();

        // Direct mention: @AIName or "hey AIName"
        if (lower.contains("@" + aiName.toLowerCase())) return true;
        if (lower.contains("hey " + aiName.toLowerCase())) return true;
        if (lower.startsWith(aiName.toLowerCase())) return true;

        // Generic AI address
        if (lower.contains("@ai") || lower.contains("hey ai")) return true;

        return false;
    }
}
```

**Testing**:
- [ ] Receives all chat messages
- [ ] Correctly identifies when addressed
- [ ] Filters own messages
- [ ] Handles multiple AIs

---

### 4.2 Message Processor & Intent Classification

**File**: `src/main/java/com/aiplayer/communication/MessageProcessor.java`
**Lines**: ~200
**Time**: 1.5 days

```java
package com.aiplayer.communication;

import com.aiplayer.core.AIPlayerEntity;
import com.aiplayer.llm.LLMProvider;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Processes chat messages and determines appropriate responses.
 */
public class MessageProcessor {

    private final IntentClassifier intentClassifier;
    private final ResponseGenerator responseGenerator;

    public MessageProcessor() {
        this.intentClassifier = new IntentClassifier();
        this.responseGenerator = new ResponseGenerator();
    }

    /**
     * Process incoming message and generate response.
     */
    public void processMessage(
        String message,
        ServerPlayerEntity sender,
        AIPlayerEntity aiPlayer
    ) {
        // Classify intent
        Intent intent = intentClassifier.classify(message);

        // Route based on intent
        switch (intent.getType()) {
            case TASK_REQUEST:
                handleTaskRequest(message, intent, sender, aiPlayer);
                break;
            case QUESTION:
                handleQuestion(message, intent, sender, aiPlayer);
                break;
            case STATUS_QUERY:
                handleStatusQuery(sender, aiPlayer);
                break;
            case CASUAL_CHAT:
                handleChat(message, sender, aiPlayer);
                break;
        }
    }

    private void handleTaskRequest(
        String message,
        Intent intent,
        ServerPlayerEntity sender,
        AIPlayerEntity aiPlayer
    ) {
        // Parse task from message
        TaskRequest task = intentClassifier.extractTaskRequest(message, intent);

        // Create goal from task
        Goal goal = task.toGoal();

        // Check if feasible
        if (aiPlayer.getBrain().canAcceptGoal(goal)) {
            aiPlayer.getBrain().addGoal(goal);
            aiPlayer.sendChatMessage(
                "Okay, I'll " + task.getDescription() + "!"
            );

            // Store in memory
            aiPlayer.getBrain().getMemorySystem().store(
                new Memory(
                    Memory.MemoryType.SOCIAL,
                    "Accepted task from " + sender.getName() + ": " + task.getDescription(),
                    0.8
                )
            );
        } else {
            aiPlayer.sendChatMessage(
                "Sorry, I can't do that right now. " + task.getRejectionReason()
            );
        }
    }

    private void handleStatusQuery(
        ServerPlayerEntity sender,
        AIPlayerEntity aiPlayer
    ) {
        Goal currentGoal = aiPlayer.getBrain().getCurrentGoal();

        if (currentGoal != null) {
            aiPlayer.sendChatMessage(
                "I'm currently " + currentGoal.getDescription() +
                " (" + currentGoal.getProgress() + "% complete)"
            );
        } else {
            aiPlayer.sendChatMessage("I'm just exploring right now.");
        }
    }

    private void handleQuestion(
        String message,
        Intent intent,
        ServerPlayerEntity sender,
        AIPlayerEntity aiPlayer
    ) {
        // Use LLM to generate contextual answer
        responseGenerator.generateQuestionResponse(
            message,
            aiPlayer.getBrain().getWorldState(),
            aiPlayer.getBrain().getMemorySystem()
        ).thenAccept(response -> {
            aiPlayer.sendChatMessage(response);
        });
    }

    private void handleChat(
        String message,
        ServerPlayerEntity sender,
        AIPlayerEntity aiPlayer
    ) {
        // Generate casual chat response
        responseGenerator.generateChatResponse(
            message,
            sender.getName().getString(),
            aiPlayer.getConfig().getPersonality()
        ).thenAccept(response -> {
            aiPlayer.sendChatMessage(response);
        });
    }
}
```

**Testing**:
- [ ] Classifies intents correctly (>85% accuracy)
- [ ] Handles task requests
- [ ] Responds to questions
- [ ] Casual chat works

---

### 4.3 Intent Classifier

**File**: `src/main/java/com/aiplayer/communication/IntentClassifier.java`
**Lines**: ~180
**Time**: 1 day

```java
package com.aiplayer.communication;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classifies user intent from chat messages.
 *
 * Phase 4 MVP: Rule-based classification
 * Phase 5: Upgrade to LLM-based classification for better accuracy
 */
public class IntentClassifier {

    // Task request patterns
    private static final Pattern[] TASK_PATTERNS = {
        Pattern.compile("(?:can you |could you |please )?(?:gather|collect|get|find) (\\d+)? ?(\\w+)"),
        Pattern.compile("(?:mine|dig|break) (\\d+)? ?(\\w+)"),
        Pattern.compile("(?:build|place|construct) (?:a |an )?(\\w+)"),
        Pattern.compile("(?:kill|fight|attack) (?:a |an |the )?(\\w+)"),
        Pattern.compile("(?:craft|make) (\\d+)? ?(\\w+)")
    };

    // Status query patterns
    private static final Pattern[] STATUS_PATTERNS = {
        Pattern.compile("(?:what are you doing|what's up|status|progress)"),
        Pattern.compile("(?:how's it going|how are you)"),
        Pattern.compile("(?:what's your current|what is your) (?:task|goal)")
    };

    // Question patterns
    private static final Pattern[] QUESTION_PATTERNS = {
        Pattern.compile("(?:where is|where are|where can i find) (\\w+)"),
        Pattern.compile("(?:do you have|got any) (\\w+)"),
        Pattern.compile("(?:how many|how much) (\\w+)"),
        Pattern.compile("(?:what|why|when|how) .*\\?")
    };

    /**
     * Classify message intent.
     */
    public Intent classify(String message) {
        String lower = message.toLowerCase();

        // Check task patterns first (highest priority)
        for (Pattern pattern : TASK_PATTERNS) {
            if (pattern.matcher(lower).find()) {
                return new Intent(Intent.Type.TASK_REQUEST, pattern);
            }
        }

        // Check status queries
        for (Pattern pattern : STATUS_PATTERNS) {
            if (pattern.matcher(lower).find()) {
                return new Intent(Intent.Type.STATUS_QUERY, pattern);
            }
        }

        // Check questions
        for (Pattern pattern : QUESTION_PATTERNS) {
            if (pattern.matcher(lower).find()) {
                return new Intent(Intent.Type.QUESTION, pattern);
            }
        }

        // Default to casual chat
        return new Intent(Intent.Type.CASUAL_CHAT, null);
    }

    /**
     * Extract task request details from message.
     */
    public TaskRequest extractTaskRequest(String message, Intent intent) {
        if (intent.getType() != Intent.Type.TASK_REQUEST) {
            return null;
        }

        Matcher matcher = intent.getPattern().matcher(message.toLowerCase());
        if (!matcher.find()) {
            return null;
        }

        // Extract components
        String action = determineAction(matcher.group(0));
        int quantity = extractQuantity(matcher);
        String item = extractItem(matcher);

        return new TaskRequest(action, quantity, item);
    }

    private String determineAction(String text) {
        if (text.contains("gather") || text.contains("collect") || text.contains("get")) {
            return "GATHER";
        } else if (text.contains("mine") || text.contains("dig")) {
            return "MINE";
        } else if (text.contains("build") || text.contains("place")) {
            return "BUILD";
        } else if (text.contains("kill") || text.contains("fight")) {
            return "COMBAT";
        } else if (text.contains("craft") || text.contains("make")) {
            return "CRAFT";
        }
        return "UNKNOWN";
    }

    private int extractQuantity(Matcher matcher) {
        try {
            if (matcher.groupCount() >= 1 && matcher.group(1) != null) {
                return Integer.parseInt(matcher.group(1));
            }
        } catch (NumberFormatException e) {
            // Ignore
        }
        return 1; // Default quantity
    }

    private String extractItem(Matcher matcher) {
        if (matcher.groupCount() >= 2 && matcher.group(2) != null) {
            return normalizeItemName(matcher.group(2));
        }
        return null;
    }

    private String normalizeItemName(String raw) {
        // Convert "logs" → "oak_log", "stone" → "stone", etc.
        // This would need a comprehensive mapping
        if (raw.contains("log")) return "oak_log";
        if (raw.contains("wood")) return "oak_planks";
        // ... etc

        return raw.toLowerCase().replace(" ", "_");
    }
}

/**
 * Represents classified intent.
 */
class Intent {
    public enum Type {
        TASK_REQUEST,
        STATUS_QUERY,
        QUESTION,
        CASUAL_CHAT
    }

    private final Type type;
    private final Pattern pattern;

    public Intent(Type type, Pattern pattern) {
        this.type = type;
        this.pattern = pattern;
    }

    public Type getType() { return type; }
    public Pattern getPattern() { return pattern; }
}

/**
 * Represents a parsed task request.
 */
class TaskRequest {
    private final String action;
    private final int quantity;
    private final String item;

    public TaskRequest(String action, int quantity, String item) {
        this.action = action;
        this.quantity = quantity;
        this.item = item;
    }

    public String getAction() { return action; }
    public int getQuantity() { return quantity; }
    public String getItem() { return item; }

    public String getDescription() {
        return action.toLowerCase() + " " + quantity + " " + item;
    }

    public Goal toGoal() {
        // Convert to Goal object
        Goal.GoalType goalType = mapActionToGoalType(action);
        Goal goal = new Goal(goalType, getDescription());

        // Add task details as parameters
        goal.setParameter("action", action);
        goal.setParameter("quantity", quantity);
        goal.setParameter("item", item);

        return goal;
    }

    private Goal.GoalType mapActionToGoalType(String action) {
        switch (action) {
            case "GATHER":
            case "MINE":
                return Goal.GoalType.RESOURCE_GATHERING;
            case "BUILD":
                return Goal.GoalType.BUILD;
            case "COMBAT":
                return Goal.GoalType.COMBAT;
            case "CRAFT":
                return Goal.GoalType.CRAFTING;
            default:
                return Goal.GoalType.EXPLORATION;
        }
    }

    public String getRejectionReason() {
        // Determine why task can't be accepted
        return "I don't have the necessary resources or skills";
    }
}
```

**Testing**:
- [ ] "gather 64 oak logs" → TASK_REQUEST (GATHER, 64, oak_log)
- [ ] "what are you doing?" → STATUS_QUERY
- [ ] "where can I find diamonds?" → QUESTION
- [ ] "hello!" → CASUAL_CHAT

---

### 4.4 Response Generator

**File**: `src/main/java/com/aiplayer/communication/ResponseGenerator.java`
**Lines**: ~200
**Time**: 1.5 days

```java
package com.aiplayer.communication;

import com.aiplayer.config.PersonalityConfig;
import com.aiplayer.llm.LLMProvider;
import com.aiplayer.llm.LLMOptions;
import com.aiplayer.memory.MemorySystem;
import com.aiplayer.perception.WorldState;

import java.util.concurrent.CompletableFuture;

/**
 * Generates natural language responses using LLM.
 */
public class ResponseGenerator {

    private final LLMProvider llmProvider;

    public ResponseGenerator(LLMProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    /**
     * Generate response to a question about the world/inventory/etc.
     */
    public CompletableFuture<String> generateQuestionResponse(
        String question,
        WorldState worldState,
        MemorySystem memorySystem
    ) {
        // Build context prompt
        String prompt = buildQuestionPrompt(question, worldState, memorySystem);

        // Call LLM
        return llmProvider.complete(prompt, LLMOptions.chat())
            .thenApply(this::cleanResponse);
    }

    /**
     * Generate casual chat response.
     */
    public CompletableFuture<String> generateChatResponse(
        String message,
        String playerName,
        PersonalityConfig personality
    ) {
        String prompt = buildChatPrompt(message, playerName, personality);

        return llmProvider.complete(prompt, LLMOptions.chat())
            .thenApply(this::cleanResponse);
    }

    /**
     * Generate task acceptance/rejection response.
     */
    public String generateTaskResponse(
        TaskRequest task,
        boolean accepted,
        String reason
    ) {
        if (accepted) {
            return pickRandom(
                "Sure, I'll " + task.getDescription() + "!",
                "Okay, working on it!",
                "Got it, I'll take care of that.",
                "On it!"
            );
        } else {
            return "Sorry, I can't do that right now. " + reason;
        }
    }

    /**
     * Build prompt for question answering.
     */
    private String buildQuestionPrompt(
        String question,
        WorldState worldState,
        MemorySystem memorySystem
    ) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an AI player in Minecraft. Answer the player's question concisely.\n\n");

        // Add context
        prompt.append("Current Status:\n");
        prompt.append("- Position: ").append(worldState.getPosition()).append("\n");
        prompt.append("- Health: ").append(worldState.getHealth()).append("/20\n");
        prompt.append("- Hunger: ").append(worldState.getHunger()).append("/20\n");

        // Add inventory
        prompt.append("\nInventory:\n");
        worldState.getInventory().forEach((item, count) -> {
            prompt.append("- ").append(item).append(": ").append(count).append("\n");
        });

        // Add recent memories
        prompt.append("\nRecent Events:\n");
        memorySystem.getWorkingMemory().getRecent(5).forEach(memory -> {
            prompt.append("- ").append(memory.getContent()).append("\n");
        });

        prompt.append("\nQuestion: ").append(question).append("\n");
        prompt.append("Answer (1-2 sentences):");

        return prompt.toString();
    }

    /**
     * Build prompt for casual chat.
     */
    private String buildChatPrompt(
        String message,
        String playerName,
        PersonalityConfig personality
    ) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an AI player in Minecraft with the following personality:\n");
        prompt.append("Description: ").append(personality.getDescription()).append("\n");
        prompt.append("Traits: ").append(personality.getTraits()).append("\n\n");

        prompt.append("A player named ").append(playerName).append(" says: \"").append(message).append("\"\n\n");
        prompt.append("Respond naturally in 1-2 sentences (stay in character):\n");

        return prompt.toString();
    }

    /**
     * Clean LLM response (remove quotes, extra whitespace).
     */
    private String cleanResponse(String response) {
        return response
            .trim()
            .replaceAll("^[\"']|[\"']$", "") // Remove quotes
            .replaceAll("\\s+", " "); // Normalize whitespace
    }

    /**
     * Pick random response from options.
     */
    private String pickRandom(String... options) {
        return options[(int) (Math.random() * options.length)];
    }
}
```

**Testing**:
- [ ] Question responses include relevant context
- [ ] Chat responses match personality
- [ ] Response length is reasonable (1-3 sentences)
- [ ] Response time <3 seconds

---

### 4.5 Integration with AIPlayerEntity

**File**: `src/main/java/com/aiplayer/core/AIPlayerEntity.java` (modifications)
**Time**: 0.5 days

```java
// Add to AIPlayerEntity class:

/**
 * Send chat message to server.
 */
public void sendChatMessage(String message) {
    if (this.getServer() != null) {
        this.getServer().getPlayerManager().broadcast(
            Text.literal("<" + this.getName().getString() + "> " + message),
            false
        );
    }
}

/**
 * Send private message to specific player.
 */
public void sendPrivateMessage(ServerPlayerEntity recipient, String message) {
    recipient.sendMessage(
        Text.literal("[" + this.getName().getString() + " whispers] " + message),
        false
    );
}
```

---

### 4.6 Phase 4 Integration Test Plan

**Time**: 1 day

**Manual Test Scenarios**:

1. **Task Request Test**
   - Player: "@AISteve, gather 64 oak logs"
   - Expected: AI responds "Sure, I'll gather 64 oak logs!", starts gathering

2. **Status Query Test**
   - Player: "Hey AI, what are you doing?"
   - Expected: AI responds with current goal and progress

3. **Question Test**
   - Player: "@AISteve, do you have any diamonds?"
   - Expected: AI checks inventory, responds with count

4. **Chat Test**
   - Player: "Hey AISteve, how are you?"
   - Expected: AI responds naturally based on personality

5. **Multi-turn Conversation Test**
   - Player: "Hey AI, let's build a house"
   - AI: "Sure! What should I build?"
   - Player: "Make it out of oak planks"
   - Expected: Context maintained across messages

**Acceptance Criteria**:
- [ ] All 5 scenarios pass
- [ ] Response time <3 seconds
- [ ] No crashes or errors
- [ ] Responses feel natural (7+/10 rating)

---

## III. Build & Distribution System

**Priority**: P0 (CRITICAL)
**Estimated Effort**: 1 week
**Why Critical**: Can't distribute mod without working build

### 3.1 Fix Gradle Wrapper

**Time**: 1 hour

```bash
# In project root
gradle wrapper --gradle-version 8.5 --distribution-type all

# Verify
./gradlew --version

# Test build
./gradlew clean build
```

**Files to Commit**:
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradlew`
- `gradlew.bat`

---

### 3.2 Verify Build Configuration

**File**: `build.gradle` (review)
**Time**: 2 hours

**Checklist**:
- [ ] Version is set correctly
- [ ] All dependencies included
- [ ] Fabric API version correct
- [ ] Minecraft version matches (1.20.4)
- [ ] JAR manifest correct
- [ ] Resources included in JAR

**Test**:
```bash
./gradlew clean build
ls -lh build/libs/
# Should see: aiplayer-0.1.0.jar
```

---

### 3.3 Create Mod Icon

**File**: `src/main/resources/assets/aiplayer/icon.png`
**Time**: 1 hour

**Requirements**:
- 256x256 PNG
- Represents AI/Minecraft theme
- Clear and recognizable

**Tools**: DALL-E, Stable Diffusion, or commission artist

---

### 3.4 Test Mod Loading

**Time**: 4 hours

**Client Test**:
```bash
./gradlew runClient
# In game:
# 1. Create world
# 2. Run /aiplayer spawn AISteve
# 3. Verify AI appears
# 4. Test chat: "Hey AI, gather some wood"
# 5. Check logs for errors
```

**Server Test**:
```bash
./gradlew runServer
# In console:
# 1. /aiplayer spawn AISteve
# 2. Connect with client
# 3. Test chat interaction
# 4. Verify no crashes
```

**Acceptance Criteria**:
- [ ] Mod loads without errors
- [ ] AI spawns successfully
- [ ] Chat system works
- [ ] No crashes in 30-minute test

---

### 3.5 Create Distribution Package

**Time**: 2 hours

**Package Contents**:
```
aiplayer-1.0.0-release/
├── aiplayer-1.0.0.jar (the mod)
├── README.txt (installation instructions)
├── config/
│   ├── aiplayer-default.json (template)
│   └── roles/ (personality presets)
│       ├── miner.json
│       ├── builder.json
│       ├── adventurer.json
│       ├── hunter_aggressive.json
│       └── support_defensive.json
├── CHANGELOG.txt
└── LICENSE.txt
```

**Create Script**: `scripts/create-release.sh`
```bash
#!/bin/bash

VERSION="1.0.0"
RELEASE_DIR="release/aiplayer-${VERSION}"

# Build mod
./gradlew clean build

# Create release directory
mkdir -p "$RELEASE_DIR/config/roles"

# Copy files
cp build/libs/aiplayer-*.jar "$RELEASE_DIR/aiplayer-${VERSION}.jar"
cp README_INSTALL.txt "$RELEASE_DIR/README.txt"
cp LICENSE "$RELEASE_DIR/LICENSE.txt"
cp CHANGELOG.md "$RELEASE_DIR/CHANGELOG.txt"
cp src/main/resources/data/aiplayer/config/default.json "$RELEASE_DIR/config/aiplayer-default.json"
cp src/main/resources/data/aiplayer/config/roles/*.json "$RELEASE_DIR/config/roles/"

# Create ZIP
cd release
zip -r "aiplayer-${VERSION}.zip" "aiplayer-${VERSION}"
cd ..

echo "Release package created: release/aiplayer-${VERSION}.zip"
```

---

### 3.6 User Installation Guide

**File**: `README_INSTALL.md`
**Time**: 2 hours

```markdown
# AI Minecraft Players - Installation Guide

**Version**: 1.0.0
**Minecraft**: 1.20.4
**Mod Loader**: Fabric

---

## Prerequisites

- Minecraft Java Edition 1.20.4
- Java 17 or higher

---

## Step 1: Install Fabric Loader

1. Download Fabric installer:
   - Visit: https://fabricmc.net/use/installer/
   - Download the installer for your OS

2. Run the installer:
   - Select "Client" or "Server" (or both)
   - Choose Minecraft version: 1.20.4
   - Click "Install"
   - Wait for installation to complete

3. Verify installation:
   - Open Minecraft Launcher
   - You should see "Fabric Loader 1.20.4" profile

---

## Step 2: Install Fabric API

1. Download Fabric API:
   - Visit: https://modrinth.com/mod/fabric-api
   - Download version for Minecraft 1.20.4
   - File: `fabric-api-0.95.4+1.20.4.jar`

2. Place in mods folder:
   - **Windows**: `%APPDATA%\.minecraft\mods\`
   - **Mac**: `~/Library/Application Support/minecraft/mods/`
   - **Linux**: `~/.minecraft/mods/`

---

## Step 3: Install AI Minecraft Players Mod

1. Extract the release package:
   - Unzip `aiplayer-1.0.0.zip`

2. Copy mod to mods folder:
   - Copy `aiplayer-1.0.0.jar` to `.minecraft/mods/`

3. First launch (generates config):
   - Start Minecraft with Fabric profile
   - Load into a world (or create new)
   - Close Minecraft

---

## Step 4: Configure AI Players

### Option A: Intelligent Mode with OpenAI (RECOMMENDED)

1. Get OpenAI API key:
   - Visit: https://platform.openai.com/api-keys
   - Create account (if needed)
   - Generate new API key
   - Copy the key (starts with `sk-proj-...`)

2. Edit config:
   - Open: `config/aiplayer.json`
   - Find: `"llm"` section
   - Change:
     ```json
     {
       "llm": {
         "provider": "openai",
         "model": "gpt-4-turbo",
         "apiKey": "YOUR_API_KEY_HERE"
       }
     }
     ```
   - Save file

3. Cost: ~$0.36-0.90 per AI per hour (with caching)

### Option B: Intelligent Mode with Local Ollama (FREE!)

1. Install Ollama:
   - Visit: https://ollama.ai/
   - Download for your OS
   - Install and start Ollama

2. Download model:
   ```bash
   ollama pull mistral
   ```

3. Edit config:
   - Open: `config/aiplayer.json`
   - Change:
     ```json
     {
       "llm": {
         "provider": "local",
         "model": "mistral",
         "apiKey": ""
       }
     }
     ```

4. Cost: FREE (runs on your computer)

### Option C: Simple Mode (NO LLM)

1. Edit config:
   - Open: `config/aiplayer.json`
   - Change:
     ```json
     {
       "llm": {
         "apiKey": ""
       }
     }
     ```

2. AI will use basic random walk behavior (no planning)

---

## Step 5: Customize Personality (Optional)

Choose a role preset or create custom personality:

### Use Preset:
```bash
# Copy desired role
cp config/roles/miner.json config/aiplayer.json

# Edit to add API key
nano config/aiplayer.json
```

### Available Roles:
- **miner.json**: Loves mining and resource gathering
- **builder.json**: Creative constructor
- **adventurer.json**: Curious explorer
- **hunter_aggressive.json**: Fearless combat specialist
- **support_defensive.json**: Protective team player

---

## Step 6: Start Playing!

1. Launch Minecraft with Fabric profile

2. Create or load world

3. Spawn AI player:
   ```
   /aiplayer spawn AISteve
   ```

4. Interact with AI:
   ```
   @AISteve, gather 64 oak logs
   Hey AI, what are you doing?
   @AISteve, help me build a house
   ```

5. Manage AI players:
   ```
   /aiplayer list          # Show all AI players
   /aiplayer status AISteve # Check AI status
   /aiplayer despawn AISteve # Remove AI
   /aiplayer reload        # Reload config
   ```

---

## Troubleshooting

### "AI players will run in SIMPLE mode"

**Cause**: No LLM configured or API key invalid

**Solution**:
- Check `config/aiplayer.json`
- Verify API key is correct
- For OpenAI: key starts with `sk-proj-` or `sk-`
- For local: check Ollama is running: `curl http://localhost:11434`

### "Mod failed to load"

**Cause**: Missing Fabric API or wrong Minecraft version

**Solution**:
- Verify Fabric API is installed in `mods/` folder
- Check Minecraft version is 1.20.4
- Check logs in `.minecraft/logs/latest.log`

### "Chat commands don't work"

**Cause**: Phase 4 feature, needs LLM

**Solution**:
- Configure OpenAI or local Ollama
- Check logs for LLM connection errors
- Restart Minecraft after config change

### High LLM costs

**Solution**:
1. Switch to local Ollama (FREE)
2. Use cheaper model: `gpt-3.5-turbo` ($0.05/hr)
3. Check caching is working (logs show hit rate)

---

## Configuration Reference

### Full Config Example:

```json
{
  "username": "AISteve",
  "personality": {
    "description": "helpful and curious",
    "role": "balanced",
    "traits": {
      "aggression": 0.3,
      "curiosity": 0.8,
      "caution": 0.6,
      "sociability": 0.7,
      "independence": 0.5
    },
    "preferences": {
      "mining": 0.5,
      "building": 0.5,
      "exploration": 0.8,
      "combat": 0.3,
      "farming": 0.4,
      "trading": 0.6
    }
  },
  "llm": {
    "provider": "openai",
    "model": "gpt-4-turbo",
    "apiKey": "sk-proj-...",
    "localModelUrl": "http://localhost:11434",
    "maxTokens": 1500,
    "temperature": 0.7
  },
  "behavior": {
    "reactionTimeMs": 200,
    "movementHumanization": true,
    "chatEnabled": true,
    "autoRespawn": true,
    "actionCacheSize": 100
  },
  "goals": {
    "defaultGoal": "explore and gather resources",
    "acceptPlayerRequests": true,
    "maxActiveGoals": 5,
    "autonomousGoalGeneration": true
  },
  "memory": {
    "storageType": "json",
    "maxEpisodicMemories": 1000,
    "enableSemanticSearch": false,
    "vectorDbUrl": ""
  }
}
```

---

## Need Help?

- GitHub Issues: https://github.com/yourusername/AI_Minecraft_Players/issues
- Discord: [coming soon]
- Documentation: https://github.com/yourusername/AI_Minecraft_Players/wiki

---

## Credits

Built with:
- Fabric mod framework
- OpenAI GPT-4 / Anthropic Claude / Ollama
- Inspired by Voyager, Baritone, and MineDojo projects

---

**Enjoy your AI companions!** 🤖⛏️
```

---

## IV. Testing & Stability

**Priority**: P1 (HIGH)
**Estimated Effort**: 1 week
**Why Important**: Prevent crashes and bugs in production

### 4.1 Manual Test Suite

**File**: `TESTING.md`
**Time**: 1 day to create, 2 days to execute

**Test Scenarios**:

1. **Installation Test**
   - Fresh Minecraft install
   - Follow installation guide
   - Verify mod loads
   - Spawn AI
   - Test basic interaction

2. **Core Functionality Tests**
   - Spawn/despawn AI
   - AI autonomous behavior
   - Mining, building, crafting
   - Combat with mobs
   - Inventory management

3. **Chat Integration Tests**
   - Task requests
   - Status queries
   - Questions
   - Casual chat
   - Multi-turn conversations

4. **Stress Tests**
   - Multiple AI players (5+)
   - Long session (24 hours)
   - High task frequency
   - Complex world

5. **Edge Cases**
   - No LLM configured
   - Invalid API key
   - Network disconnection
   - World reload
   - Server restart

6. **Performance Tests**
   - CPU usage measurement
   - Memory usage tracking
   - Response time monitoring
   - LLM call frequency

---

### 4.2 Automated Tests (Basic)

**Time**: 2 days

**Priority Tests**:

```java
// Test memory system
@Test
public void testEpisodicMemoryStorage() {
    MemorySystem memory = new MemorySystem();
    Memory m = new Memory(Memory.MemoryType.OBSERVATION, "Test", 0.8);
    memory.store(m);

    assertEquals(1, memory.getEpisodicMemory().size());
}

// Test intent classification
@Test
public void testTaskRequestClassification() {
    IntentClassifier classifier = new IntentClassifier();
    Intent intent = classifier.classify("gather 64 oak logs");

    assertEquals(Intent.Type.TASK_REQUEST, intent.getType());
}

// Test goal creation
@Test
public void testGoalCreation() {
    Goal goal = new Goal(Goal.GoalType.RESOURCE_GATHERING, "Gather logs");
    assertEquals(Goal.GoalStatus.PENDING, goal.getStatus());
}
```

---

## V. Polish & Documentation

**Priority**: P2 (MEDIUM)
**Estimated Effort**: 3-5 days

### 5.1 Error Handling Review

- [ ] All LLM calls have timeout handling
- [ ] Network errors don't crash mod
- [ ] Invalid commands show helpful messages
- [ ] File I/O errors are caught
- [ ] Null checks on all external data

### 5.2 Performance Optimization

- [ ] Path caching implemented
- [ ] LLM prompt token reduction
- [ ] Memory consolidation tuning
- [ ] Action queue optimization

### 5.3 Documentation Updates

- [ ] README.md refresh
- [ ] API documentation (Javadoc)
- [ ] Configuration guide update
- [ ] Troubleshooting expansion
- [ ] Video tutorial (optional)

---

## VI. Release Timeline

### Week 1: Phase 4 Core (Chat Foundation)
**Days 1-2**: ChatListener + MessageProcessor
**Days 3-4**: IntentClassifier + TaskRequest parsing
**Day 5**: Integration + initial testing

### Week 2: Phase 4 Advanced (LLM Responses)
**Days 1-2**: ResponseGenerator implementation
**Day 3**: Conversation context manager
**Days 4-5**: Integration testing + refinement

### Week 3: Build & Distribution
**Day 1**: Fix Gradle wrapper + verify build
**Day 2**: Create mod icon + distribution package
**Days 3-4**: Installation guide + testing
**Day 5**: User acceptance testing

### Week 4: Testing & Polish
**Days 1-2**: Comprehensive testing (all scenarios)
**Days 3-4**: Bug fixes + performance tuning
**Day 5**: Final review + release preparation

### Week 5-6: Buffer & Advanced Features (Optional)
- Additional testing
- Performance optimization
- Documentation polish
- Community feedback incorporation

---

## VII. Success Metrics

### Minimum Viable Product (MVP) Requirements

**Functional**:
- [x] AI player spawns and moves autonomously
- [x] AI can mine, build, craft, fight
- [x] AI has memory and planning
- [ ] AI responds to chat commands ← **IN PROGRESS**
- [ ] AI accepts and completes tasks ← **IN PROGRESS**
- [ ] Mod builds and distributes cleanly ← **TODO**

**Quality**:
- [ ] No crashes in 1-hour test
- [ ] Chat response time <3 seconds
- [ ] Task completion rate >70%
- [ ] Installation success rate >90% (with guide)

**Performance**:
- [ ] CPU usage <10% per AI
- [ ] Memory usage <256MB per AI
- [ ] LLM cost <$0.20/hour with caching

---

## VIII. Post-MVP Roadmap

### Phase 5: Advanced AI (Weeks 7-10)
- LLM-generated skills
- Multi-AI coordination
- Advanced learning
- Emergent social behaviors

### Phase 6: Production Hardening (Weeks 11-12)
- Comprehensive test suite (60%+ coverage)
- Performance profiling + optimization
- Multi-version support
- GUI configuration (optional)

### Future Enhancements
- Voice chat integration
- Vision-based perception
- Creative building templates
- Multi-player campaigns

---

## IX. Risk Mitigation

### High-Risk Items

**Risk**: Chat integration more complex than expected
**Mitigation**: Start with simple rule-based NLU, upgrade to LLM later

**Risk**: LLM costs too high for users
**Mitigation**: Emphasize free Ollama option, optimize prompts

**Risk**: Build issues delay release
**Mitigation**: Fix build system first (Week 3, Day 1)

### Contingency Plans

**If Phase 4 takes longer**: Release MVP without chat, add in v1.1

**If build issues persist**: Get community help, document workarounds

**If performance issues**: Profile early, optimize critical paths first

---

## X. Conclusion

**Current Status**: 60% complete (excellent foundation)
**Path to MVP**: Clear and achievable
**Timeline**: 4-6 weeks to production release
**Confidence**: High ✅

**Next Actions**:
1. Start Phase 4: ChatListener (Week 1, Day 1)
2. Fix build system (Week 3, Day 1)
3. Begin testing early and often

**Recommendation**: Proceed with 4-week aggressive timeline for MVP, allocate 2 additional weeks for polish if needed.

---

**Plan Prepared By**: Production Planning Agent
**Date**: 2025-11-17
**Status**: Ready for Implementation
