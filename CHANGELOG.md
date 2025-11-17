# Changelog

All notable changes to the AI Minecraft Player mod will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Phase 1 - Foundation (In Progress)

#### [0.1.0-alpha] - 2025-11-17

##### Added
- Initial project structure with Fabric mod framework
- Build configuration (Gradle, dependencies)
- Main mod class (`AIPlayerMod`) with initialization
- Configuration system (`AIPlayerConfig`) with JSON loading/saving
  - LLM configuration (provider, model, API key)
  - Behavior configuration (reaction time, humanization, chat)
  - Goals configuration (default goals, autonomy settings)
  - Memory configuration (storage type, limits)
- Logging system (`AILogger`) with categorized logging
  - Goal, Action, Perception, Memory, LLM, Chat logging
  - Debug mode support
- AI Player Manager (`AIPlayerManager`) stub for player lifecycle
- AI Player Entity (`AIPlayerEntity`) stub (to be implemented)
- Default configuration file template
- Command system with basic commands:
  - `/aiplayer spawn` - Spawn AI player (stub)
  - `/aiplayer list` - List active AI players
  - `/aiplayer status` - Show mod status
  - `/aiplayer reload` - Reload configuration
  - `/aiplayer help` - Show available commands

##### Technical Details
- Java 17+ requirement
- Minecraft 1.20.4 support
- Fabric API integration
- Gson for JSON configuration
- OkHttp for future LLM API calls
- Caffeine for caching (future use)

##### Project Documentation
- Complete architecture plan (PROJECT_PLAN.md)
- Technical specifications (TECHNICAL_SPEC.md)
- Detailed roadmap with 100+ tasks (ROADMAP.md)
- 6 major milestones defined (MILESTONES.md)
- Sprint planning templates (SPRINT_PLANNING.md)
- Getting started guide (GETTING_STARTED.md)
- Quick start guide (QUICK_START.md)

##### Status
- ✅ Task 1.1: Development environment setup (documented)
- ✅ Task 1.2: Fabric mod project structure (complete)
- ✅ Task 1.3: Configuration system (complete)
- ✅ Task 1.4: Logging system (complete)
- ⏳ Task 1.5: FakePlayer entity creation (next)
- ⏳ Task 1.6: Basic movement controller (planned)
- ⏳ Task 1.7: Basic perception system (planned)

##### Next Steps
- Implement FakePlayer entity that can spawn in-world
- Add basic movement capabilities
- Create perception system for world awareness
- Implement simple autonomous behavior

---

## Version History

### Alpha Versions (Phase 1-2)
- Focus on core functionality and action system
- Not recommended for production use
- Breaking changes expected

### Beta Versions (Phase 3-4)
- Add intelligence and communication
- Feature-complete for basic use
- API may still change

### Release Candidates (Phase 5-6)
- Advanced AI and optimization
- Production-ready candidates
- Stable API

### Stable Releases (v1.0.0+)
- Full feature set
- Stable, optimized, well-tested
- Public release on CurseForge/Modrinth
