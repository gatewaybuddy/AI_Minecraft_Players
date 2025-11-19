# Verification Checklist - AI Minecraft Players

**Quick reference for testing the mod in a real environment**

Print this and check off items as you verify them!

---

## ⚙️ Prerequisites Setup

- [ ] Workstation with **internet access**
- [ ] **Java 17+** installed: `java -version`
- [ ] **Git** installed: `git --version`
- [ ] **Minecraft 1.20.4** installed
- [ ] **Fabric Loader 0.15.3+** installed
- [ ] **Fabric API** in mods folder
- [ ] **8GB+ RAM** available

---

## 🔨 Build Verification (30-45 min)

### Build the Project

```bash
git clone https://github.com/gatewaybuddy/AI_Minecraft_Players.git
cd AI_Minecraft_Players
./gradlew clean build --no-daemon
```

**Checklist**:
- [ ] Build completes **without errors**
- [ ] JAR created: `build/libs/ai-minecraft-player-*.jar`
- [ ] JAR size is **2-10 MB** (reasonable)
- [ ] No compilation errors in output

**If build fails**: Document errors and check VERIFICATION_PLAN.md

---

### Run Unit Tests

```bash
./gradlew test --no-daemon
```

**Checklist**:
- [ ] Tests run without crashes
- [ ] **All 42 tests pass** (ideal)
- [ ] If some fail: **80%+ pass** (33+ tests) is acceptable for beta
- [ ] Test report: `build/reports/tests/test/index.html`

**Test Suites** (42 tests total):
- [ ] MemorySystemTest (10 tests)
- [ ] IntentClassifierTest (13 tests)
- [ ] SkillLibraryTest (8 tests)
- [ ] WorldKnowledgeTest (11 tests)

**If tests fail**: Note which tests and check error messages

---

## 📦 Installation Verification (15-20 min)

### Install the Mod

```bash
# Copy JAR to Minecraft mods folder
cp build/libs/ai-minecraft-player-*.jar ~/.minecraft/mods/
```

**Checklist**:
- [ ] JAR copied to mods folder
- [ ] Fabric API present in mods folder
- [ ] No conflicting mods

### First Launch

**Launch Minecraft 1.20.4 with Fabric**

**Checklist**:
- [ ] Minecraft launches **without crashing**
- [ ] Mod appears in **Mods list** (Mods button)
- [ ] Config generated: `~/.minecraft/config/aiplayers.json`
- [ ] No errors in `logs/latest.log`

**Look for in logs**:
```
[INFO] Loading mod: ai-minecraft-player
[INFO] AIPlayerManager initialized
```

**If crashes**: Check `crash-reports/` folder

---

## 🧪 Functional Testing (45-60 min)

### Test 1: Simple Mode (No API Key)

**Config** (`config/aiplayers.json`):
```json
{
  "llmProvider": "simple",
  ...
}
```

**Restart Minecraft** → Create/Load World

**Commands to test**:
```
/aiplayer spawn
/aiplayer spawn Bob
/aiplayer list
/aiplayer status Bob
/aiplayer despawn Bob
```

**Checklist**:
- [ ] `/aiplayer spawn` creates AI
- [ ] AI is **visible** in game
- [ ] AI **moves autonomously** within 5 seconds
- [ ] AI doesn't crash game
- [ ] `/aiplayer list` shows AI
- [ ] `/aiplayer status` shows AI state
- [ ] `/aiplayer despawn` removes AI

---

### Test 2: Intelligent Mode (OpenAI/Claude/Ollama)

**Choose ONE to test first**:

**Option A: OpenAI**
```json
{
  "llmProvider": "openai",
  "apiKeys": {
    "openai": "sk-..."
  }
}
```

**Option B: Claude**
```json
{
  "llmProvider": "claude",
  "apiKeys": {
    "claude": "sk-ant-..."
  }
}
```

**Option C: Ollama (FREE)**
```bash
# Install Ollama first: https://ollama.ai
ollama pull llama2
```
```json
{
  "llmProvider": "ollama",
  "ollamaConfig": {
    "model": "llama2"
  }
}
```

**Restart Minecraft** → Create/Load World

**Test Natural Language**:
```
/aiplayer spawn Alice explorer
<Player> Alice, gather some wood
<Player> Alice, what are you doing?
```

**Checklist**:
- [ ] AI spawns with intelligent brain
- [ ] AI **responds to chat** (if Phase 4 implemented)
- [ ] AI **performs tasks** (gather, mine, build)
- [ ] AI **remembers context**
- [ ] No API errors in logs
- [ ] Response caching works (check logs for cache hits)

---

### Test 3: Self-Improvement (Phase 5)

**Test Skill Learning**:
```
<Player> Bob, gather 64 oak logs
[Wait for completion]
<Player> Bob, gather 64 oak logs again
[Should be faster - skill learned!]
```

**Checklist**:
- [ ] AI completes task first time
- [ ] AI **learns from success**
- [ ] Second attempt is **faster/better**
- [ ] Skill visible in `/aiplayer status Bob`

---

### Test 4: World Knowledge (Phase 5)

**Test Memory**:
```
<Player> Alice, remember this village
<Player> Alice, explore around
<Player> Alice, go back to the village
```

**Checklist**:
- [ ] AI **discovers landmarks**
- [ ] AI **remembers locations**
- [ ] AI can **navigate back** to known places

---

### Test 5: Multi-AI Coordination (Phase 5)

**Test Collaboration**:
```
/aiplayer spawn Alice builder
/aiplayer spawn Bob gatherer
<Player> Alice and Bob, build a house together
```

**Checklist**:
- [ ] Multiple AIs spawn
- [ ] AIs **coordinate** on shared goal
- [ ] AIs **don't interfere** with each other
- [ ] Task **divided** between AIs

---

### Test 6: Performance & Stability

**Spawn Multiple AIs**:
```
/aiplayer spawn AI1
/aiplayer spawn AI2
/aiplayer spawn AI3
/aiplayer spawn AI4
/aiplayer spawn AI5
```

**Play for 30+ minutes**

**Checklist**:
- [ ] Game remains **playable** (>30 FPS)
- [ ] No **memory leaks** (F3 → memory stays stable)
- [ ] No **lag spikes**
- [ ] No errors after extended play

---

## 📊 Results Summary

### Overall Status

**Build**: ⬜ Pass / ⬜ Fail
**Tests**: ⬜ All Pass / ⬜ Most Pass / ⬜ Many Fail
**Installation**: ⬜ Success / ⬜ Crashes
**Simple Mode**: ⬜ Works / ⬜ Partial / ⬜ Broken
**Intelligent Mode**: ⬜ Works / ⬜ Partial / ⬜ Broken
**Advanced Features**: ⬜ Works / ⬜ Partial / ⬜ Broken

### Feature Matrix

| Feature | Status | Notes |
|---------|--------|-------|
| AI Spawning | ⬜ ✅ / ⬜ ⚠️ / ⬜ ❌ | |
| AI Movement | ⬜ ✅ / ⬜ ⚠️ / ⬜ ❌ | |
| Chat (Phase 4) | ⬜ ✅ / ⬜ ⚠️ / ⬜ ❌ | |
| Task Execution | ⬜ ✅ / ⬜ ⚠️ / ⬜ ❌ | |
| Skill Learning (Phase 5) | ⬜ ✅ / ⬜ ⚠️ / ⬜ ❌ | |
| World Knowledge (Phase 5) | ⬜ ✅ / ⬜ ⚠️ / ⬜ ❌ | |
| Multi-AI Coordination (Phase 5) | ⬜ ✅ / ⬜ ⚠️ / ⬜ ❌ | |
| Performance | ⬜ ✅ / ⬜ ⚠️ / ⬜ ❌ | |

---

## 🐛 Issues Found

**List all bugs/issues encountered**:

1. _______________________________________________
   - Severity: ⬜ Critical / ⬜ High / ⬜ Medium / ⬜ Low
   - Details: ___________________________________

2. _______________________________________________
   - Severity: ⬜ Critical / ⬜ High / ⬜ Medium / ⬜ Low
   - Details: ___________________________________

3. _______________________________________________
   - Severity: ⬜ Critical / ⬜ High / ⬜ Medium / ⬜ Low
   - Details: ___________________________________

---

## ✅ Beta Release Decision

**Is this ready for beta release?**

### Minimum Criteria:
- [ ] Builds successfully
- [ ] 80%+ tests pass
- [ ] Loads in Minecraft
- [ ] Simple mode works
- [ ] At least ONE intelligent mode works

**Decision**: ⬜ **Ready for Beta** / ⬜ **Needs More Work**

**If needs work**: Priority fixes:
1. _______________________________________________
2. _______________________________________________
3. _______________________________________________

---

## 📝 Notes

**Additional observations**:

__________________________________________________

__________________________________________________

__________________________________________________

__________________________________________________

---

**Testing Date**: _______________
**Tested By**: _______________
**Environment**: ⬜ Windows / ⬜ Mac / ⬜ Linux
**Java Version**: _______________
**Minecraft Version**: _______________
**Fabric Version**: _______________

---

**Next Steps**:
- [ ] Create GitHub issues for bugs found
- [ ] Fix critical issues
- [ ] Re-test
- [ ] Prepare beta release
- [ ] Announce to community

---

**End of Checklist** ✓
