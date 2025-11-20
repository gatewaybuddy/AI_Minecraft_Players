# Known Issues - v0.9.0 Beta

**Last Updated**: [UPDATE DATE]
**Version**: 0.9.0-beta
**Status**: Beta Release

---

## 🎯 Beta Release Philosophy

This is a **beta release**. We're shipping with known limitations to get real-world feedback quickly. We value transparency over perfection.

**What "Beta" means**:
- ✅ Core features work (AI spawning, movement, basic intelligence)
- ⚠️ Some edge cases may fail
- ⚠️ Performance not fully optimized
- ⚠️ Some features may be incomplete

**We appreciate your patience and feedback!**

---

## 🐛 Known Issues

### Critical Issues

*Issues that affect core functionality but have workarounds*

#### 1. [TEMPLATE - DELETE IF NONE]

**Issue**: [Description]

**Impact**: [Who this affects and how]

**Workaround**: [How to avoid or work around]

**Status**: [Planned fix in v0.9.1 / v1.0 / etc]

**Example**:
```
#### AI Sometimes Gets Stuck on Terrain

**Issue**: AI players occasionally get stuck when navigating complex terrain (caves, ravines)

**Impact**: Affects all users. AI may stop moving and require respawn.

**Workaround**:
- Use `/aiplayer despawn <name>` and respawn the AI
- Keep AIs in relatively flat areas for now

**Status**: Fix planned for v0.9.1 (improved pathfinding)
```

---

### Important Issues

*Issues with significant features but don't block basic usage*

#### 1. [TEMPLATE - DELETE IF NONE]

**Issue**: [Description]

**Impact**: [Which features affected]

**Workaround**: [How to work around]

**Status**: [Fix timeline]

**Example**:
```
#### Skill Learning Sometimes Fails

**Issue**: Self-improvement system (Phase 5) may not always generate skills correctly

**Impact**: Affects users using intelligent modes (OpenAI, Claude, Ollama) with skill learning enabled

**Workaround**:
- Simple mode not affected
- Skills may work intermittently
- Check logs for LLM errors

**Status**: Under investigation, fix planned for v1.0
```

---

### Minor Issues

*Issues that don't significantly impact functionality*

#### 1. [TEMPLATE - DELETE IF NONE]

**Issue**: [Description]

**Impact**: [Minor impact]

**Status**: [Fix timeline or "deferred"]

**Example**:
```
#### Log Messages Sometimes Verbose

**Issue**: Console logs can be very verbose in intelligent modes

**Impact**: Minor annoyance, makes logs harder to read

**Status**: Will add log level configuration in v0.9.1
```

---

## ✅ What's Working Well

*Celebrate the wins!*

- ✅ **AI Spawning**: Reliable across all modes
- ✅ **Basic Movement**: AI navigation works smoothly
- ✅ **Commands**: All /aiplayer commands function correctly
- ✅ **Configuration**: JSON config system works well
- ✅ **Simple Mode**: Works perfectly for basic AI behavior
- ✅ [Add more based on testing]

---

## 🧪 Test Results

### Unit Tests

- **Total Tests**: 42
- **Passed**: [FILL IN ACTUAL NUMBER]
- **Failed**: [FILL IN ACTUAL NUMBER]
- **Pass Rate**: [FILL IN PERCENTAGE]%

**Failed Tests** (if any):
```
[List specific test names]
- MemorySystemTest.testConsolidation (FAILED)
- [etc]
```

**Notes**: [Why these tests failed, are they critical?]

---

## 🎮 Minecraft Compatibility

### Tested Configurations

✅ **Working**:
- Minecraft 1.20.4 + Fabric Loader 0.15.3+
- Java 17, 21
- [Add other tested configs]

⚠️ **Untested** (may work, feedback wanted):
- Minecraft 1.20.1-1.20.3 (should work)
- Java 18-20 (should work)
- [Others]

❌ **Not Compatible**:
- Minecraft 1.19.x and earlier (not supported)
- Forge (Fabric only)
- Quilt (untested)

---

## 🔌 LLM Provider Status

### OpenAI
- **Status**: ✅ Working / ⚠️ Partial / ❌ Not Tested
- **Tested Models**: [gpt-3.5-turbo, gpt-4, etc]
- **Known Issues**: [List if any]

### Claude (Anthropic)
- **Status**: ✅ Working / ⚠️ Partial / ❌ Not Tested
- **Tested Models**: [claude-3-opus, etc]
- **Known Issues**: [List if any]

### Ollama (Local)
- **Status**: ✅ Working / ⚠️ Partial / ❌ Not Tested
- **Tested Models**: [llama2, mistral, etc]
- **Known Issues**: [List if any]

### Simple Mode
- **Status**: ✅ Fully Working
- **Known Issues**: None (deterministic behavior)

---

## 📊 Performance Notes

### Current Performance

**Single AI**:
- FPS Impact: [Measure actual impact]
- Memory Usage: [Measure actual usage]
- CPU Usage: [Measure actual usage]

**Multiple AIs** (5 AIs):
- FPS Impact: [Measure]
- Memory Usage: [Measure]
- CPU Usage: [Measure]

**LLM Call Frequency**:
- Average: [X] calls per minute per AI
- Cost estimate: [Y] USD per hour (if using paid API)

### Performance Issues

[List any performance problems discovered]

---

## 🚀 Features Status

### Phase 1: Foundation
- ✅ AI entity spawning
- ✅ Basic commands
- ✅ Configuration system

### Phase 2: Actions
- ✅ Movement and pathfinding
- ⚠️ Mining (works but may be slow)
- ⚠️ Building (basic functionality)
- ⚠️ Combat (implemented but untested thoroughly)

### Phase 3: Intelligence
- ✅ Memory system
- ✅ Goal planning
- ✅ LLM integration

### Phase 4: Communication
- [✅ / ⚠️ / ❌] Natural language chat
- [✅ / ⚠️ / ❌] Intent classification
- [✅ / ⚠️ / ❌] Task parsing

### Phase 5: Advanced AI
- [✅ / ⚠️ / ❌] Self-improvement
- [✅ / ⚠️ / ❌] Skill learning
- [✅ / ⚠️ / ❌] World knowledge
- [✅ / ⚠️ / ❌] Multi-AI coordination

---

## 📝 Testing Checklist

*Use this to track what's been tested*

### Basic Functionality
- [ ] AI spawns successfully
- [ ] AI moves autonomously
- [ ] All commands work (/spawn, /list, /status, /despawn)
- [ ] Config file generated correctly
- [ ] Mod doesn't crash Minecraft

### Simple Mode
- [ ] AI performs random actions
- [ ] Pathfinding works
- [ ] No errors in logs

### Intelligent Mode (OpenAI)
- [ ] AI responds to chat
- [ ] API calls work
- [ ] Response caching works
- [ ] Tasks are executed

### Intelligent Mode (Claude)
- [ ] [Same as OpenAI]

### Intelligent Mode (Ollama)
- [ ] [Same as OpenAI]

### Advanced Features
- [ ] Skill learning works
- [ ] AIs remember landmarks
- [ ] Multi-AI coordination works
- [ ] Memory system functions

### Performance
- [ ] Playable FPS with 1 AI
- [ ] Playable FPS with 5 AIs
- [ ] No memory leaks over 30+ minutes
- [ ] No lag spikes

---

## 🔧 Fixes Planned

### v0.9.1 (Hotfix Release - 1 week)
- [ ] Fix critical bugs reported by beta testers
- [ ] [Specific fix 1]
- [ ] [Specific fix 2]

### v0.9.2 (Minor Release - 2 weeks)
- [ ] Fix important bugs
- [ ] Improve performance
- [ ] [Specific improvement 1]

### v1.0 (Production Release - 4-6 weeks)
- [ ] All tests passing (100%)
- [ ] All features working smoothly
- [ ] Performance optimized
- [ ] Comprehensive testing across platforms

---

## 🤝 How to Report Issues

**Found a bug?** Please report it!

1. **Check if it's already listed above**
2. **Gather information**:
   - Minecraft version
   - Fabric version
   - Java version
   - Mod version
   - Config (aiplayers.json)
   - Log file (logs/latest.log)
   - Crash report (if crashed)

3. **Create GitHub issue**:
   - Go to: https://github.com/gatewaybuddy/AI_Minecraft_Players/issues
   - Title: Brief description
   - Include: All information from step 2
   - Steps to reproduce
   - Expected vs actual behavior

**Thank you for helping make this mod better!** 🙏

---

## 📜 Version History

### v0.9.0-beta (Current)
- Initial beta release
- [Date of release]
- Known issues: [Count] issues documented above

---

**Last Updated**: [Date]
**Maintainer**: [Your name/handle]
**Feedback**: GitHub Issues or [contact method]
