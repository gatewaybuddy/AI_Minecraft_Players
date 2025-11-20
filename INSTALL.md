# Installation Guide

Complete guide for installing and deploying AI Minecraft Players on your server.

## Prerequisites

### Server Requirements

- **Minecraft Version**: 1.20.4
- **Mod Loader**: Fabric Loader 0.15.3+
- **Fabric API**: 0.95.4+1.20.4
- **Java**: 17 or higher
- **RAM**: Minimum 2GB (4GB+ recommended for multiple AI players)
- **Disk Space**: 500MB for mod + config + data

### LLM Requirements (Optional)

Choose one:

1. **OpenAI** (Cloud)
   - API key from https://platform.openai.com/
   - Cost: ~$0.36-0.90/hour per AI player

2. **Anthropic Claude** (Cloud)
   - API key from https://console.anthropic.com/
   - Cost: ~$0.45-0.90/hour per AI player

3. **Ollama** (Local - FREE)
   - Server with 8GB+ RAM (for Mistral)
   - Install from https://ollama.ai/
   - No API key needed

4. **Simple Mode** (FREE)
   - No LLM required
   - Basic random walk behavior

## Installation Methods

### Method 1: Quick Install (Recommended)

This method provides everything pre-configured.

```bash
# 1. Download installation package
wget https://github.com/your-username/AI_Minecraft_Players/releases/latest/download/install.zip
unzip install.zip
cd install/

# 2. Copy to your Minecraft server
cp ai-minecraft-player-*.jar /path/to/minecraft/server/mods/
cp config/aiplayer-config.json /path/to/minecraft/server/config/

# 3. Edit configuration
nano /path/to/minecraft/server/config/aiplayer-config.json
# Add your API key or set up local model

# 4. Restart server
cd /path/to/minecraft/server
./start.sh  # or however you start your server
```

### Method 2: Manual Installation

If you built from source or want more control.

```bash
# 1. Ensure Fabric is installed on your server
# Download Fabric installer: https://fabricmc.net/use/installer/

# 2. Install Fabric API mod
cd server/mods/
wget https://cdn.modrinth.com/data/P7dR8mSH/versions/.../fabric-api-0.95.4+1.20.4.jar

# 3. Copy AI Player mod
cp /path/to/ai-minecraft-player-0.1.0-SNAPSHOT.jar server/mods/

# 4. Start server once to generate config
cd server/
./start.sh

# 5. Stop server and edit config
nano config/aiplayer-config.json

# 6. Restart server
./start.sh
```

### Method 3: Docker Deployment

Use the provided Docker setup for easy deployment.

```bash
# 1. Clone repository
git clone https://github.com/your-username/AI_Minecraft_Players.git
cd AI_Minecraft_Players/

# 2. Build Docker image
docker build -t minecraft-ai-players .

# 3. Run container
docker run -d \
  -p 25565:25565 \
  -v $(pwd)/server-data:/server \
  -v $(pwd)/config:/server/config \
  --name minecraft-ai \
  minecraft-ai-players

# 4. Edit config
nano config/aiplayer-config.json

# 5. Restart container
docker restart minecraft-ai
```

## Configuration

### Basic Configuration

Edit `config/aiplayer-config.json`:

#### For OpenAI (GPT-4)

```json
{
  "username": "AISteve",
  "llm": {
    "provider": "openai",
    "model": "gpt-4-turbo",
    "apiKey": "sk-proj-YOUR_KEY_HERE",
    "maxTokens": 1500,
    "temperature": 0.7
  },
  "personality": {
    "description": "helpful explorer",
    "traits": {
      "aggression": 0.3,
      "curiosity": 0.8,
      "caution": 0.6,
      "sociability": 0.7,
      "independence": 0.5
    }
  }
}
```

#### For Claude

```json
{
  "llm": {
    "provider": "claude",
    "model": "claude-3-5-sonnet-20241022",
    "apiKey": "sk-ant-YOUR_KEY_HERE"
  }
}
```

#### For Local Ollama (FREE)

```bash
# First, install and start Ollama
curl -fsSL https://ollama.com/install.sh | sh
ollama pull mistral
ollama serve &
```

```json
{
  "llm": {
    "provider": "local",
    "model": "mistral",
    "localModelUrl": "http://localhost:11434"
  }
}
```

#### For Simple Mode (No LLM)

```json
{
  "llm": {
    "apiKey": ""
  }
}
```

### Advanced Configuration

#### Multiple AI Players

Create multiple config files:

```bash
# Copy default config
cp config/aiplayer-config.json config/ai-miner.json
cp config/aiplayer-config.json config/ai-builder.json

# Edit each with different personalities
nano config/ai-miner.json  # Set role to "miner"
nano config/ai-builder.json  # Set role to "builder"
```

#### Personality Presets

Use pre-made personality configurations:

```bash
# Located in install/config/roles/
ls install/config/roles/
# adventurer.json
# builder.json
# hunter_aggressive.json
# miner.json
# support_defensive.json

# Copy a role preset
cp install/config/roles/miner.json config/aiplayer-config.json
# Then add your API key
```

### Security Configuration

#### Protect Your API Keys

```bash
# Set restrictive permissions
chmod 600 config/aiplayer-config.json

# Or use environment variables instead
export OPENAI_API_KEY="sk-proj-YOUR_KEY"
export CLAUDE_API_KEY="sk-ant-YOUR_KEY"
```

Update config to use environment variables:

```json
{
  "llm": {
    "apiKey": "${OPENAI_API_KEY}"
  }
}
```

#### Limit AI Permissions

Configure what AI players can do:

```json
{
  "goals": {
    "acceptPlayerRequests": true,     // Allow commands from players
    "maxActiveGoals": 3,                // Limit concurrent goals
    "autonomousGoalGeneration": true   // Allow AI to set own goals
  },
  "behavior": {
    "chatEnabled": true,         // Allow AI to chat
    "autoRespawn": true,         // Respawn when killed
    "actionCacheSize": 100       // Limit memory usage
  }
}
```

## First Run

### 1. Start Server

```bash
cd /path/to/minecraft/server
java -Xmx4G -Xms4G -jar fabric-server-launch.jar nogui
```

Watch logs for:
```
[INFO] AI Minecraft Players mod loaded
[INFO] LLM provider configured: openai (gpt-4-turbo)
[INFO] AI brain initialized in INTELLIGENT mode
```

Or for Simple mode:
```
[WARN] No LLM API key configured - AI players will run in SIMPLE mode
```

### 2. Spawn AI Player

In Minecraft console or chat:

```
/aiplayer spawn AISteve
```

Expected output:
```
[Server] Spawned AI player: AISteve at (100, 64, 200)
[INFO] Loaded 5 skills for AI player AISteve
[INFO] Loaded world knowledge: 0 landmarks, 0 resource locations
```

### 3. Verify Functionality

Test AI player behavior:

```
# Check status
/aiplayer status AISteve

# Output shows:
# - Current health and hunger
# - Active goal
# - Skills learned
# - World knowledge acquired

# Talk to AI (in chat)
@AISteve hello!
AISteve: Hello! I'm ready to help. What would you like me to do?

# Give command
@AISteve gather some wood
AISteve: I'll start gathering wood for you.
```

### 4. Monitor Performance

```
# Check AI player list
/aiplayer list

# View performance stats (if enabled)
/aiplayer perf AISteve

# Output:
# brain_update: count=600, avg=12.3ms, max=45ms
# llm_planning: count=20, avg=145ms, max=340ms
# Memory: 245MB / 2048MB
```

## Data Persistence

AI players automatically save:

### Skills

Location: `config/aiplayer/skills/{player-uuid}/skills.json`

```json
[
  {
    "name": "Mine Logs",
    "category": "MINING",
    "complexity": 2,
    "statistics": {
      "timesUsed": 15,
      "timesSucceeded": 12,
      "timesFailed": 3
    }
  }
]
```

### World Knowledge

Location: `config/aiplayer/world/{player-uuid}/world_knowledge.json`

```json
{
  "landmarks": [
    {
      "name": "Village Alpha",
      "type": "VILLAGE",
      "position": { "x": 245.5, "y": 64.0, "z": -128.3 },
      "visits": 5
    }
  ],
  "exploredChunkCount": 47
}
```

### Backup Strategy

```bash
# Create backup script
cat > backup-ai-data.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="backups/ai-data-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR"
cp -r config/aiplayer/ "$BACKUP_DIR/"
echo "Backup created: $BACKUP_DIR"
EOF

chmod +x backup-ai-data.sh

# Run daily via cron
crontab -e
# Add: 0 2 * * * /path/to/backup-ai-data.sh
```

## Updating

### Update Mod

```bash
# 1. Stop server
screen -r minecraft
# Press Ctrl+C

# 2. Backup current mod
mv mods/ai-minecraft-player-*.jar mods/ai-minecraft-player-old.jar

# 3. Copy new version
cp /path/to/new/ai-minecraft-player-*.jar mods/

# 4. Backup AI data (important!)
./backup-ai-data.sh

# 5. Start server
./start.sh

# 6. Check logs for migration messages
tail -f logs/latest.log
```

### Update Configuration

```bash
# 1. Backup current config
cp config/aiplayer-config.json config/aiplayer-config.json.backup

# 2. Compare with new default
diff config/aiplayer-config.json config/aiplayer-config.json.new

# 3. Merge changes manually or copy new and re-add API key
```

## Troubleshooting

### AI Players Not Spawning

**Problem**: `/aiplayer spawn` command fails

**Solutions**:
1. Check mod is installed: `/aiplayer list` should work
2. Check logs for errors: `grep ERROR logs/latest.log`
3. Verify Fabric API is installed
4. Ensure Java 17+ is used

### AI in SIMPLE Mode (Unexpected)

**Problem**: Logs show "SIMPLE mode" but you configured API key

**Solutions**:
1. Verify API key format:
   - OpenAI: starts with `sk-proj-` or `sk-`
   - Claude: starts with `sk-ant-`
2. Check config file location: `config/aiplayer-config.json`
3. Test API key manually:
   ```bash
   curl https://api.openai.com/v1/models \
     -H "Authorization: Bearer $YOUR_API_KEY"
   ```
4. Check firewall/proxy settings

### High LLM Costs

**Problem**: API bills too high

**Solutions**:
1. **Switch to local model** (FREE):
   ```bash
   ollama pull mistral
   # Update config to use "local" provider
   ```

2. **Use cheaper model**:
   ```json
   {"model": "gpt-3.5-turbo"}  // Instead of gpt-4
   ```

3. **Reduce frequency**:
   ```json
   {"behavior": {"reactionTimeMs": 1000}}  // Slower updates
   ```

4. **Verify caching**:
   ```bash
   grep "cache hit" logs/latest.log  // Should see hits
   ```

### AI Player Stuck/Not Moving

**Problem**: AI spawns but doesn't move

**Solutions**:
1. Check if goal is set: `/aiplayer status AISteve`
2. Manually set goal: `@AISteve explore the area`
3. Check for errors in logs
4. Respawn AI: `/aiplayer despawn AISteve && /aiplayer spawn AISteve`

### Performance Issues

**Problem**: Server lag with AI players

**Solutions**:
1. Check performance stats: `/aiplayer perf`
2. Reduce AI update frequency
3. Limit number of AI players (start with 1-2)
4. Increase server RAM: `-Xmx6G`
5. Use local model instead of API (faster)

### Data Not Persisting

**Problem**: Skills/knowledge lost after restart

**Solutions**:
1. Check directory exists: `ls config/aiplayer/skills/`
2. Check permissions: `ls -la config/aiplayer/`
3. Check logs for save errors: `grep "auto-save" logs/latest.log`
4. Manually trigger save: `/aiplayer save AISteve`

## Monitoring

### Log Files

Important log locations:

```bash
# Main server log
tail -f logs/latest.log

# Filter AI-specific logs
tail -f logs/latest.log | grep "aiplayer"

# Check for errors
grep ERROR logs/latest.log | grep aiplayer

# Monitor LLM calls
grep "LLM" logs/latest.log
```

### Performance Monitoring

```bash
# In-game command
/aiplayer perf

# Expected output:
=== Performance Statistics ===
brain_update: count=1000, avg=12.45ms, max=89ms
llm_planning: count=45, avg=156.23ms, max=890ms
skill_generation: count=8, avg=234.56ms, max=450ms
auto_save: count=5, avg=45.23ms, max=78ms
Memory: 245MB / 2048MB
```

### Health Checks

Create a monitoring script:

```bash
#!/bin/bash
# check-ai-health.sh

AI_NAME="AISteve"
LOG_FILE="/path/to/minecraft/logs/latest.log"

# Check if AI is spawned
if screen -S minecraft -X stuff "aiplayer status $AI_NAME\n" | grep -q "Active"; then
    echo "✓ AI player $AI_NAME is active"
else
    echo "✗ AI player $AI_NAME is NOT active"
    # Auto-respawn
    screen -S minecraft -X stuff "aiplayer spawn $AI_NAME\n"
fi

# Check for errors
ERROR_COUNT=$(grep -c "ERROR.*aiplayer" "$LOG_FILE")
if [ "$ERROR_COUNT" -gt 10 ]; then
    echo "⚠ High error count: $ERROR_COUNT errors in log"
fi
```

## Best Practices

### Production Deployment

1. **Use screen/tmux** for server management:
   ```bash
   screen -S minecraft -dm java -Xmx4G -jar fabric-server-launch.jar
   screen -r minecraft  # Attach to view
   # Ctrl+A, D to detach
   ```

2. **Set up auto-restart** on crash:
   ```bash
   #!/bin/bash
   while true; do
       java -Xmx4G -jar fabric-server-launch.jar
       echo "Server crashed! Restarting in 10 seconds..."
       sleep 10
   done
   ```

3. **Monitor costs** (for cloud LLMs):
   ```bash
   # Track API usage
   grep "LLM call" logs/latest.log | wc -l
   # Estimate cost: calls * $0.01 (approximate)
   ```

4. **Regular backups**:
   ```bash
   # Automated backup via cron
   0 */6 * * * /path/to/backup-ai-data.sh
   ```

5. **Rate limiting** for API calls:
   ```json
   {
     "llm": {
       "maxTokens": 1000,  // Reduce for lower cost
       "temperature": 0.5   // More deterministic = fewer retries
     }
   }
   ```

## Uninstallation

### Remove Mod

```bash
# 1. Stop server
# 2. Remove mod file
rm mods/ai-minecraft-player-*.jar

# 3. Optionally remove config and data
rm -rf config/aiplayer-config.json
rm -rf config/aiplayer/

# 4. Start server
```

### Preserve AI Data

```bash
# Keep AI skills and knowledge for future use
tar -czf ai-player-data-backup.tar.gz config/aiplayer/
# Restore later with:
# tar -xzf ai-player-data-backup.tar.gz -C /path/to/server/
```

## Support

- **Issues**: https://github.com/your-username/AI_Minecraft_Players/issues
- **Documentation**: See README.md and PHASE*_IMPLEMENTATION.md files
- **Community**: Join our Discord/Forum (coming soon)

## Next Steps

After installation:

1. **Read the full README.md** for features and configuration
2. **Check CONFIGURATION.md** for advanced personality settings
3. **Review PHASE documentation** for technical details
4. **Join the community** to share your AI players!

---

**Happy AI Minecrafting!** 🤖⛏️
