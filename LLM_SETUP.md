# LLM Setup Guide

This guide explains how to enable **intelligent mode** for AI players using LLM-powered planning.

---

## Quick Start

### Option 1: OpenAI (GPT-4, GPT-3.5)

**Requirements**: OpenAI API key

1. Get API key from https://platform.openai.com/api-keys
2. Edit `config/aiplayer-config.json`:
```json
{
  "llm": {
    "provider": "openai",
    "model": "gpt-4-turbo",
    "apiKey": "sk-proj-...",
    "maxTokens": 1500,
    "temperature": 0.7
  }
}
```

3. Restart server
4. AI players will use **INTELLIGENT mode**

**Cost**: ~$0.36-0.90/hour per AI player (with caching)

---

### Option 2: Anthropic Claude

**Requirements**: Anthropic API key

1. Get API key from https://console.anthropic.com/
2. Edit `config/aiplayer-config.json`:
```json
{
  "llm": {
    "provider": "claude",
    "model": "claude-3-5-sonnet-20240620",
    "apiKey": "sk-ant-...",
    "maxTokens": 1500,
    "temperature": 0.7
  }
}
```

3. Restart server
4. AI players will use **INTELLIGENT mode**

**Cost**: ~$0.45-0.90/hour per AI player (with caching)

---

### Option 3: Local (Ollama) - FREE!

**Requirements**: Ollama installed locally

1. Install Ollama:
```bash
curl https://ollama.ai/install.sh | sh
```

2. Pull a model:
```bash
ollama pull mistral
# or: ollama pull llama2, codellama, phi, etc.
```

3. Start Ollama server:
```bash
ollama serve
```

4. Edit `config/aiplayer-config.json`:
```json
{
  "llm": {
    "provider": "local",
    "model": "mistral",
    "apiKey": "",
    "localModelUrl": "http://localhost:11434",
    "maxTokens": 1500,
    "temperature": 0.7
  }
}
```

5. Restart server
6. AI players will use **INTELLIGENT mode**

**Cost**: FREE (requires local GPU, 8GB+ VRAM recommended)

---

## Simple Mode (No LLM)

If no API key is configured or LLM is unavailable, AI players automatically fall back to **SIMPLE mode** with basic random walk behavior.

No configuration needed - it just works!

---

## Model Recommendations

### For Production (Best Intelligence)

- **OpenAI**: `gpt-4-turbo` - Most capable, good cost/performance
- **Claude**: `claude-3-5-sonnet-20240620` - Excellent reasoning
- **Local**: `llama2:13b` - Best quality for local

### For Development (Fast & Cheap)

- **OpenAI**: `gpt-3.5-turbo` - Fast, very cheap
- **Claude**: `claude-3-haiku-20240307` - Fast, cheap
- **Local**: `mistral` - Fast, 7B parameters

### For Budget (Free)

- **Local**: `mistral`, `llama2`, `phi` - All free, run locally

---

## Configuration Options

### provider
- `"openai"` - OpenAI GPT models
- `"claude"` - Anthropic Claude models
- `"local"` - Ollama local models

### model
**OpenAI**:
- `"gpt-4-turbo"` - Latest GPT-4 (recommended)
- `"gpt-4"` - Standard GPT-4
- `"gpt-3.5-turbo"` - Cheaper, faster

**Claude**:
- `"claude-3-5-sonnet-20240620"` - Latest Claude (recommended)
- `"claude-3-opus-20240229"` - Most capable
- `"claude-3-haiku-20240307"` - Fastest, cheapest

**Local** (any Ollama model):
- `"mistral"` - General purpose (recommended)
- `"llama2"` - Meta's LLaMA 2
- `"codellama"` - Code-focused
- `"phi"` - Microsoft's small model

### apiKey
- Required for OpenAI and Claude
- Leave empty `""` for local models
- Never commit API keys to git!

### maxTokens
- Maximum response length
- `1500` recommended for planning
- Higher = more detailed plans, slower, more expensive

### temperature
- `0.0` - Deterministic (same input → same output)
- `0.7` - Balanced (recommended)
- `1.0` - Creative/random

---

## Verifying It Works

### 1. Check Server Logs

On server start, you should see:
```
[AIPlayerManager] LLM provider initialized: OpenAI (gpt-4-turbo)
```

Or if not configured:
```
[AIPlayerManager] LLM API key not configured - AI players will run in SIMPLE mode
```

### 2. Spawn AI Player

```
/aiplayer spawn TestBot
```

Check logs for:
```
[AIPlayerEntity] Created AI player: TestBot (UUID: ..., mode: INTELLIGENT)
```

### 3. Check Status

```
/aiplayer status TestBot
```

You should see goal-based behavior like:
```
AI Player: TestBot
Position: 100.5, 64.0, 200.3
Health: 20.0
Mode: INTELLIGENT
Goal: Find food and gather resources
```

---

## Troubleshooting

### "SIMPLE mode" instead of "INTELLIGENT"

**Problem**: LLM not working

**Solutions**:
- Check API key is correct (no typos, quotes)
- For OpenAI: Key should start with `sk-proj-` or `sk-`
- For Claude: Key should start with `sk-ant-`
- For local: Check Ollama is running (`ollama list`)
- Check server logs for error messages

### High API costs

**Problem**: Too many LLM calls

**Solutions**:
- Response caching is enabled by default (saves 50-80%)
- Use cheaper models: `gpt-3.5-turbo` or `claude-3-haiku`
- Use local models (free!)
- Increase planning interval (Phase 4)

### Slow responses

**Problem**: AI takes long to decide

**Solutions**:
- Use faster models: `gpt-3.5-turbo` or `claude-3-haiku`
- Lower `maxTokens` to 1000
- Use local models with smaller parameters (mistral, phi)

### Local model not found

**Problem**: `[Ollama] Model not found`

**Solution**:
```bash
ollama pull mistral
```

Then restart server.

---

## Environment Variables (Alternative)

Instead of putting API keys in config file, you can use environment variables:

### Linux/Mac
```bash
export OPENAI_API_KEY="sk-proj-..."
export ANTHROPIC_API_KEY="sk-ant-..."
```

### Windows (PowerShell)
```powershell
$env:OPENAI_API_KEY="sk-proj-..."
$env:ANTHROPIC_API_KEY="sk-ant-..."
```

Then set `apiKey: ""` in config and the mod will read from environment.

*(Note: This feature will be added in a future update)*

---

## Cost Estimates

Based on 1 AI player running continuously:

| Provider | Model | Cost/Hour | Cost/Day | Notes |
|----------|-------|-----------|----------|-------|
| OpenAI | gpt-4-turbo | $0.36-0.90 | $8-21 | With caching |
| OpenAI | gpt-3.5-turbo | $0.05-0.15 | $1-4 | Very cheap |
| Claude | claude-3-5-sonnet | $0.45-0.90 | $11-21 | With caching |
| Claude | claude-3-haiku | $0.10-0.25 | $2-6 | Fast & cheap |
| Local | mistral | $0 | $0 | FREE! |
| Local | llama2 | $0 | $0 | FREE! |

*Estimates assume:*
- Planning every 5 seconds
- 50-80% cache hit rate
- 500 input + 200 output tokens per request

---

## Next Steps

Once LLM is configured:

1. **Test basic behavior**: Spawn AI player, observe autonomous actions
2. **Monitor logs**: Check for planning decisions
3. **View memory**: AI players store memories of events
4. **Track skills**: AI learns which behaviors work best

**Phase 4** will add:
- Natural language chat ("Hey bot, follow me!")
- Player requests ("Mine diamonds for me")
- Multi-turn conversations

---

## Support

- Check server logs for detailed error messages
- See `PHASE3_IMPLEMENTATION.md` for technical details
- Join Discord: [coming soon]
- Report issues: [GitHub repo]

---

## Security Notes

⚠️ **IMPORTANT**: API keys are sensitive!

- Never commit API keys to git
- Use `.gitignore` for config files
- Consider environment variables for production
- Rotate keys regularly
- Monitor usage on provider dashboards

---

Happy experimenting! 🤖
