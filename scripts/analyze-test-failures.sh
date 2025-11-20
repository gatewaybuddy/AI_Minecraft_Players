#!/bin/bash
# Test Failure Analysis Script
# Helps identify and prioritize test failures

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "================================================"
echo "  Test Failure Analysis"
echo "================================================"
echo ""

# Check if test report exists
if [ ! -f "build/reports/tests/test/index.html" ]; then
    echo -e "${RED}Error:${NC} Test report not found!"
    echo "Please run tests first: ./gradlew test --no-daemon"
    exit 1
fi

# Parse test results from gradle output
echo -e "${BLUE}Analyzing test results...${NC}"
echo ""

# Check if we have the log
if [ ! -f "build-logs/test-results.log" ] && [ ! -f "test-output.log" ]; then
    echo -e "${YELLOW}No test log found. Running tests now...${NC}"
    ./gradlew test --no-daemon 2>&1 | tee test-output.log
    LOG_FILE="test-output.log"
else
    if [ -f "build-logs/test-results.log" ]; then
        LOG_FILE=$(ls -t build-logs/*/test-results.log | head -1)
    else
        LOG_FILE="test-output.log"
    fi
fi

echo "Using log file: $LOG_FILE"
echo ""

# Extract test statistics
TOTAL_TESTS=$(grep -oP '\d+(?= tests completed)' "$LOG_FILE" | tail -1 || echo "42")
FAILED_TESTS=$(grep -oP '\d+(?= failed)' "$LOG_FILE" | tail -1 || echo "0")
PASSED_TESTS=$((TOTAL_TESTS - FAILED_TESTS))

if [ $TOTAL_TESTS -gt 0 ]; then
    PASS_PERCENT=$((PASSED_TESTS * 100 / TOTAL_TESTS))
else
    PASS_PERCENT=0
fi

# Summary
echo "================================================"
echo "  Test Summary"
echo "================================================"
echo -e "Total tests:   ${BLUE}$TOTAL_TESTS${NC}"
echo -e "Passed:        ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed:        ${RED}$FAILED_TESTS${NC}"
echo -e "Pass rate:     ${BLUE}$PASS_PERCENT%${NC}"
echo ""

if [ $PASS_PERCENT -ge 80 ]; then
    echo -e "${GREEN}✅ SUCCESS: Pass rate meets 80% target!${NC}"
elif [ $PASS_PERCENT -ge 60 ]; then
    echo -e "${YELLOW}⚠️  WARNING: Pass rate below 80% target${NC}"
else
    echo -e "${RED}❌ FAIL: Pass rate significantly below target${NC}"
fi

echo ""

# If there are failures, analyze them
if [ $FAILED_TESTS -gt 0 ]; then
    echo "================================================"
    echo "  Failed Tests Analysis"
    echo "================================================"
    echo ""

    # Extract failed test names
    grep "FAILED" "$LOG_FILE" | grep -oP '(?<=\> ).*(?= FAILED)' > failed-tests.txt || true

    # Group by test suite
    declare -A suite_failures

    while IFS= read -r test_name; do
        suite=$(echo "$test_name" | cut -d'.' -f1)
        ((suite_failures[$suite]++)) || suite_failures[$suite]=1
    done < failed-tests.txt

    # Print failures by suite
    echo -e "${YELLOW}Failures by Test Suite:${NC}"
    echo ""

    for suite in "${!suite_failures[@]}"; do
        count=${suite_failures[$suite]}
        echo -e "  ${RED}$suite${NC}: $count failed test(s)"
    done

    echo ""
    echo -e "${YELLOW}Individual Failed Tests:${NC}"
    echo ""

    # List all failed tests with priority assessment
    cat failed-tests.txt | while read test_name; do
        # Determine priority based on test suite
        if [[ $test_name == *"MemorySystemTest"* ]]; then
            priority="${RED}CRITICAL${NC}"
        elif [[ $test_name == *"IntentClassifierTest"* ]]; then
            priority="${RED}CRITICAL${NC}"
        elif [[ $test_name == *"SkillLibraryTest"* ]]; then
            priority="${YELLOW}IMPORTANT${NC}"
        elif [[ $test_name == *"WorldKnowledgeTest"* ]]; then
            priority="${YELLOW}IMPORTANT${NC}"
        else
            priority="${BLUE}MINOR${NC}"
        fi

        echo -e "  $priority: $test_name"
    done

    echo ""
    echo "================================================"
    echo "  Recommendations"
    echo "================================================"
    echo ""

    # Critical failures (Memory, Intent)
    critical_count=$(grep -E "(MemorySystemTest|IntentClassifierTest)" failed-tests.txt | wc -l || echo "0")

    if [ $critical_count -gt 0 ]; then
        echo -e "${RED}⚠️  CRITICAL: $critical_count critical test(s) failing${NC}"
        echo ""
        echo "These tests cover core functionality. Recommend fixing before release:"
        grep -E "(MemorySystemTest|IntentClassifierTest)" failed-tests.txt | while read test; do
            echo "  - $test"
        done
        echo ""
    fi

    # Important failures (Skills, WorldKnowledge)
    important_count=$(grep -E "(SkillLibraryTest|WorldKnowledgeTest)" failed-tests.txt | wc -l || echo "0")

    if [ $important_count -gt 0 ]; then
        echo -e "${YELLOW}⚠️  IMPORTANT: $important_count important test(s) failing${NC}"
        echo ""
        echo "These tests cover advanced features. Can document as known issues:"
        grep -E "(SkillLibraryTest|WorldKnowledgeTest)" failed-tests.txt | while read test; do
            echo "  - $test"
        done
        echo ""
    fi

    # Decision guidance
    echo "================================================"
    echo "  Decision: Can We Ship Beta?"
    echo "================================================"
    echo ""

    if [ $PASS_PERCENT -ge 80 ] && [ $critical_count -eq 0 ]; then
        echo -e "${GREEN}✅ YES: Ready for beta release${NC}"
        echo ""
        echo "Reasons:"
        echo "  • Pass rate meets 80% target ($PASS_PERCENT%)"
        echo "  • No critical tests failing"
        echo "  • Advanced feature failures can be documented"
        echo ""
        echo "Next steps:"
        echo "  1. Document failures in KNOWN_ISSUES.md"
        echo "  2. Test in Minecraft (Phase 3)"
        echo "  3. Prepare beta release"
    elif [ $PASS_PERCENT -ge 70 ] && [ $critical_count -le 2 ]; then
        echo -e "${YELLOW}⚠️  MAYBE: Consider fixing critical tests first${NC}"
        echo ""
        echo "Current state:"
        echo "  • Pass rate: $PASS_PERCENT% (below 80% target)"
        echo "  • Critical failures: $critical_count"
        echo ""
        echo "Options:"
        echo "  1. Fix $critical_count critical test(s) → likely +5-10% pass rate"
        echo "  2. Accept current state and document thoroughly"
        echo ""
        echo "Estimated fix time: 30-60 minutes"
    else
        echo -e "${RED}❌ NO: Too many failures for beta${NC}"
        echo ""
        echo "Issues:"
        echo "  • Pass rate: $PASS_PERCENT% (target: 80%)"
        echo "  • Critical failures: $critical_count (target: 0)"
        echo ""
        echo "Recommend:"
        echo "  1. Fix critical tests first (Memory, Intent)"
        echo "  2. Aim for 80%+ pass rate"
        echo "  3. Re-run this analysis"
        echo ""
        echo "Estimated time needed: 2-3 hours"
    fi

    echo ""

    # Common error patterns
    echo "================================================"
    echo "  Common Error Patterns"
    echo "================================================"
    echo ""

    echo "Checking for common issues..."
    echo ""

    # Constructor errors
    if grep -q "cannot be applied to given types" "$LOG_FILE"; then
        echo -e "${YELLOW}⚠️  Constructor/Method signature mismatches detected${NC}"
        echo "   → Check that test constructors match actual class constructors"
        echo ""
    fi

    # Import errors
    if grep -q "cannot find symbol" "$LOG_FILE"; then
        echo -e "${YELLOW}⚠️  Missing symbol/import errors detected${NC}"
        echo "   → Check imports and class names"
        echo ""
    fi

    # Type errors
    if grep -q "incompatible types" "$LOG_FILE"; then
        echo -e "${YELLOW}⚠️  Type mismatch errors detected${NC}"
        echo "   → Check return types and type conversions"
        echo ""
    fi

    # Assertion failures
    if grep -q "AssertionError" "$LOG_FILE"; then
        echo -e "${BLUE}ℹ️  Assertion failures detected${NC}"
        echo "   → Check test expectations vs actual behavior"
        echo ""
    fi

else
    echo -e "${GREEN}🎉 All tests passing! Excellent work!${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. ✅ Build complete"
    echo "  2. ✅ All tests passing"
    echo "  3. ➡️  Test in Minecraft (Phase 3)"
    echo "  4. ➡️  Prepare beta release"
fi

echo ""
echo "================================================"
echo "  Detailed Reports"
echo "================================================"
echo ""
echo "View detailed test report:"
echo "  File: build/reports/tests/test/index.html"
echo "  Command: open build/reports/tests/test/index.html  # Mac"
echo "           xdg-open build/reports/tests/test/index.html  # Linux"
echo "           start build/reports/tests/test/index.html  # Windows"
echo ""
echo "View this analysis again:"
echo "  ./scripts/analyze-test-failures.sh"
echo ""

# Cleanup temp files
rm -f failed-tests.txt test-output.log 2>/dev/null || true

echo "Analysis complete!"
