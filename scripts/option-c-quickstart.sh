#!/bin/bash
# Option C Quick Start Script
# Automates the build, test, and verification process

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Log functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Header
echo "================================================"
echo "  AI Minecraft Players - Option C Quick Start"
echo "  Beta Release Preparation"
echo "================================================"
echo ""

# Check prerequisites
log_info "Checking prerequisites..."

# Check Java
if ! command -v java &> /dev/null; then
    log_error "Java not found! Please install Java 17+"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    log_error "Java 17+ required. Found Java $JAVA_VERSION"
    exit 1
fi
log_info "Java version: $(java -version 2>&1 | head -1)"

# Check Gradle
if [ ! -f "./gradlew" ]; then
    log_error "Gradle wrapper not found! Are you in the project root?"
    exit 1
fi
log_info "Gradle wrapper found"

# Create output directory for logs
mkdir -p build-logs
LOG_DIR="build-logs/$(date +%Y%m%d_%H%M%S)"
mkdir -p "$LOG_DIR"
log_info "Logs will be saved to: $LOG_DIR"

# Phase 1: Build
echo ""
echo "================================================"
echo "  PHASE 1: Build & Compilation (3 hours budget)"
echo "================================================"
echo ""

START_TIME=$(date +%s)

log_info "Step 1.1: Initial build attempt..."
./gradlew clean build --no-daemon 2>&1 | tee "$LOG_DIR/build-initial.log"
BUILD_EXIT_CODE=${PIPESTATUS[0]}

if [ $BUILD_EXIT_CODE -eq 0 ]; then
    log_info "✅ Build succeeded on first try! (Amazing!)"
else
    log_warn "⚠️  Build failed (expected). Check $LOG_DIR/build-initial.log"

    # Count errors
    ERROR_COUNT=$(grep -i "error" "$LOG_DIR/build-initial.log" | wc -l)
    log_info "Found approximately $ERROR_COUNT error messages"

    # Extract unique errors
    grep -i "error" "$LOG_DIR/build-initial.log" | sort | uniq > "$LOG_DIR/errors-unique.txt"
    log_info "Unique errors saved to: $LOG_DIR/errors-unique.txt"

    echo ""
    log_warn "MANUAL ACTION REQUIRED:"
    echo "1. Review errors in $LOG_DIR/errors-unique.txt"
    echo "2. Fix compilation errors one by one"
    echo "3. Run './gradlew clean build --no-daemon' after each fix"
    echo "4. Once build succeeds, re-run this script"
    echo ""

    read -p "Have you fixed the errors and want to retry build? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        log_info "Retrying build..."
        ./gradlew clean build --no-daemon 2>&1 | tee "$LOG_DIR/build-retry.log"
        BUILD_EXIT_CODE=${PIPESTATUS[0]}

        if [ $BUILD_EXIT_CODE -ne 0 ]; then
            log_error "Build still failing. Please fix errors manually."
            exit 1
        fi
    else
        log_info "Exiting. Re-run this script after fixing errors."
        exit 1
    fi
fi

BUILD_TIME=$(($(date +%s) - START_TIME))
log_info "✅ Build successful! (took ${BUILD_TIME}s)"

# Check JAR was created
if [ ! -f build/libs/ai-minecraft-player-*.jar ]; then
    log_error "JAR file not found in build/libs/"
    exit 1
fi

JAR_FILE=$(ls build/libs/ai-minecraft-player-*.jar | head -1)
JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
log_info "JAR created: $JAR_FILE ($JAR_SIZE)"

# Phase 2: Tests
echo ""
echo "================================================"
echo "  PHASE 2: Test Validation (2 hours budget)"
echo "================================================"
echo ""

TEST_START=$(date +%s)

log_info "Step 2.1: Running all tests..."
./gradlew test --no-daemon 2>&1 | tee "$LOG_DIR/test-results.log"
TEST_EXIT_CODE=${PIPESTATUS[0]}

# Parse test results
TESTS_RUN=$(grep "tests completed" "$LOG_DIR/test-results.log" | tail -1 || echo "")
log_info "Test summary: $TESTS_RUN"

# Extract pass/fail counts
if grep -q "42 tests completed" "$LOG_DIR/test-results.log"; then
    TOTAL_TESTS=42
else
    TOTAL_TESTS=$(grep -oP '\d+(?= tests completed)' "$LOG_DIR/test-results.log" | tail -1 || echo "0")
fi

FAILED_TESTS=$(grep -oP '\d+(?= failed)' "$LOG_DIR/test-results.log" | tail -1 || echo "0")
PASSED_TESTS=$((TOTAL_TESTS - FAILED_TESTS))

log_info "Tests passed: $PASSED_TESTS / $TOTAL_TESTS"

# Calculate pass percentage
if [ $TOTAL_TESTS -gt 0 ]; then
    PASS_PERCENT=$((PASSED_TESTS * 100 / TOTAL_TESTS))
else
    PASS_PERCENT=0
fi

echo ""
if [ $PASS_PERCENT -ge 80 ]; then
    log_info "✅ Test pass rate: ${PASS_PERCENT}% (Target: 80%+)"
    log_info "✅ PHASE 2 COMPLETE: Sufficient tests passing!"
elif [ $PASS_PERCENT -ge 60 ]; then
    log_warn "⚠️  Test pass rate: ${PASS_PERCENT}% (Target: 80%+)"
    log_warn "Below target but may be acceptable. Review failures."
else
    log_error "❌ Test pass rate: ${PASS_PERCENT}% (Target: 80%+)"
    log_error "Too many failures. Manual fixes required."
fi

# List failed tests
if [ $FAILED_TESTS -gt 0 ]; then
    log_warn "Failed tests:"
    grep "FAILED" "$LOG_DIR/test-results.log" | tee "$LOG_DIR/failed-tests.txt"
    echo ""
    log_warn "Review test failures in: build/reports/tests/test/index.html"
fi

TEST_TIME=$(($(date +%s) - TEST_START))
log_info "Testing completed (took ${TEST_TIME}s)"

# Phase 3: Decision Point
echo ""
echo "================================================"
echo "  DECISION POINT"
echo "================================================"
echo ""

TOTAL_TIME=$((BUILD_TIME + TEST_TIME))
log_info "Time spent so far: ${TOTAL_TIME}s (~$((TOTAL_TIME / 60)) minutes)"

echo ""
echo "Current Status:"
echo "  • Build: ✅ SUCCESS"
echo "  • Tests: $PASSED_TESTS / $TOTAL_TESTS passed (${PASS_PERCENT}%)"
echo "  • JAR:   ✅ $JAR_FILE ($JAR_SIZE)"
echo ""

if [ $PASS_PERCENT -ge 80 ]; then
    log_info "✅ Ready to proceed to Phase 3 (Minecraft testing)"
    echo ""
    echo "Next steps:"
    echo "1. Install mod in Minecraft: cp $JAR_FILE ~/.minecraft/mods/"
    echo "2. Follow OPTION_C_IMPLEMENTATION.md Phase 3"
    echo "3. Test simple mode in Minecraft"
    echo "4. Prepare beta release"
else
    log_warn "⚠️  Consider fixing more tests before Minecraft testing"
    echo ""
    echo "Options:"
    echo "1. Fix failing tests (see $LOG_DIR/failed-tests.txt)"
    echo "2. Proceed anyway and document known issues"
    echo "3. Review test failures and decide which are critical"
fi

# Generate summary report
SUMMARY_FILE="$LOG_DIR/SUMMARY.md"
cat > "$SUMMARY_FILE" << EOF
# Build & Test Summary

**Date**: $(date)
**Total Time**: ${TOTAL_TIME}s (~$((TOTAL_TIME / 60)) minutes)

## Phase 1: Build

- **Status**: ✅ SUCCESS
- **Time**: ${BUILD_TIME}s
- **JAR**: $JAR_FILE ($JAR_SIZE)
- **Log**: $LOG_DIR/build-initial.log

## Phase 2: Tests

- **Status**: $([ $PASS_PERCENT -ge 80 ] && echo "✅ PASS" || echo "⚠️ PARTIAL")
- **Time**: ${TEST_TIME}s
- **Tests Run**: $TOTAL_TESTS
- **Passed**: $PASSED_TESTS ($PASS_PERCENT%)
- **Failed**: $FAILED_TESTS
- **Log**: $LOG_DIR/test-results.log
- **HTML Report**: build/reports/tests/test/index.html

### Failed Tests

$(grep "FAILED" "$LOG_DIR/test-results.log" 2>/dev/null || echo "None")

## Next Steps

$(if [ $PASS_PERCENT -ge 80 ]; then
    echo "✅ Proceed to Phase 3 (Minecraft testing)"
    echo "1. Install mod: cp $JAR_FILE ~/.minecraft/mods/"
    echo "2. Test in Minecraft"
    echo "3. Prepare beta release"
else
    echo "⚠️ Fix failing tests or proceed with known issues"
    echo "1. Review failed tests in build/reports/tests/test/index.html"
    echo "2. Fix critical tests"
    echo "3. Re-run: ./gradlew test"
fi)

## Files Generated

- Build log: $LOG_DIR/build-initial.log
- Test log: $LOG_DIR/test-results.log
- Failed tests: $LOG_DIR/failed-tests.txt
- Summary: $LOG_DIR/SUMMARY.md
EOF

log_info "Summary report saved: $SUMMARY_FILE"
echo ""
cat "$SUMMARY_FILE"

echo ""
echo "================================================"
echo "  Script Complete!"
echo "================================================"
log_info "Next: Follow OPTION_C_IMPLEMENTATION.md Phase 3"
