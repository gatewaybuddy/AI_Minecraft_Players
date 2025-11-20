# Scripts Directory

Automation scripts for building, testing, and releasing the AI Minecraft Players mod.

---

## 📜 Available Scripts

### option-c-quickstart.sh

**Purpose**: Automate the Option C (Hybrid Approach) build and test process

**What it does**:
1. Checks prerequisites (Java, Gradle)
2. Attempts to build the project
3. Runs all unit tests
4. Analyzes results
5. Generates summary report

**Usage**:
```bash
./scripts/option-c-quickstart.sh
```

**Output**:
- Build logs in `build-logs/[timestamp]/`
- Test results in `build-logs/[timestamp]/test-results.log`
- Summary report in `build-logs/[timestamp]/SUMMARY.md`

**Time**: ~5-10 minutes (if build succeeds on first try)

---

### analyze-test-failures.sh

**Purpose**: Analyze test failures and provide recommendations

**What it does**:
1. Parses test results
2. Groups failures by test suite
3. Prioritizes failures (Critical/Important/Minor)
4. Provides decision guidance (ship or fix?)
5. Identifies common error patterns

**Usage**:
```bash
# After running tests
./gradlew test --no-daemon
./scripts/analyze-test-failures.sh
```

**Output**:
- Test summary with pass rate
- Failed tests grouped by priority
- Recommendations for next steps
- Decision guidance (ship beta or fix more?)

**Time**: < 1 minute

---

## 🚀 Typical Workflow

### First Time Build

```bash
# 1. Run quick start script
./scripts/option-c-quickstart.sh

# 2. If build fails, fix errors manually and retry
# 3. If tests fail, run analysis
./scripts/analyze-test-failures.sh

# 4. Fix critical test failures
# 5. Re-run tests
./gradlew test --no-daemon

# 6. Analyze again
./scripts/analyze-test-failures.sh

# 7. When 80%+ tests pass, proceed to Minecraft testing
# (See OPTION_C_IMPLEMENTATION.md Phase 3)
```

### Iterative Development

```bash
# Make code changes
# ...

# Quick test
./gradlew test --tests "MemorySystemTest" --no-daemon

# Full test suite
./gradlew test --no-daemon

# Analyze results
./scripts/analyze-test-failures.sh
```

### Pre-Release

```bash
# Clean build and full test
./scripts/option-c-quickstart.sh

# Verify 80%+ pass rate
# Check summary in build-logs/latest/SUMMARY.md

# Follow BETA_RELEASE_CHECKLIST.md
```

---

## 📋 Prerequisites

All scripts require:
- **Bash shell** (Linux, Mac, Git Bash on Windows)
- **Java 17+** installed and in PATH
- **Gradle** (wrapper included in project)
- **Internet access** (for dependency downloads)

---

## 🔧 Troubleshooting

### Script won't run

```bash
# Make script executable
chmod +x scripts/option-c-quickstart.sh
chmod +x scripts/analyze-test-failures.sh
```

### "Java not found"

```bash
# Check Java installation
java -version

# If not installed, download from:
# https://adoptium.net/
```

### "Gradle wrapper not found"

```bash
# Ensure you're in project root
cd /path/to/AI_Minecraft_Players

# Check for gradlew
ls -la gradlew
```

### Build fails immediately

```bash
# Check internet connection
# Gradle needs to download dependencies on first run

# Try with more verbose output
./gradlew clean build --no-daemon --stacktrace
```

---

## 📝 Customization

### Modify Time Budgets

Edit `option-c-quickstart.sh`:
```bash
# Around line 100-150
# Change time expectations for your environment
```

### Change Test Pass Threshold

Edit `analyze-test-failures.sh`:
```bash
# Around line 50
# Default is 80%, change if needed
if [ $PASS_PERCENT -ge 80 ]; then
```

### Add Custom Checks

Both scripts are designed to be extended. Add custom validation logic as needed.

---

## 🤝 Contributing

Improvements to these scripts are welcome!

Common additions:
- Windows batch file equivalents
- PowerShell versions
- Additional error detection
- Performance profiling
- Memory leak detection

See [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

---

## 📖 Related Documentation

- [OPTION_C_IMPLEMENTATION.md](../OPTION_C_IMPLEMENTATION.md) - Full implementation guide
- [VERIFICATION_PLAN.md](../VERIFICATION_PLAN.md) - Detailed testing plan
- [BETA_RELEASE_CHECKLIST.md](../BETA_RELEASE_CHECKLIST.md) - Release process

---

**Happy Building!** 🚀
