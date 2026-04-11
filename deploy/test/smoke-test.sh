#!/bin/bash
# Copyright 2026 Talweg Authors
# Flinkflow Smoke Test Script
# This script builds the project and runs a --dry-run for each standalone example YAML.

set -e

# Configuration
EXAMPLES_DIR="examples/standalone"
RUN_SCRIPT="./scripts/run-local.sh"

echo "========================================================="
echo "   Flinkflow Smoke Test Suite: CLI Validation            "
echo "========================================================="

# Build project once to ensure jar is up-to-date
echo "Step 1: Building Flinkflow project..."
mvn clean package -DskipTests -P local-run > /dev/null
echo "[OK] Build complete."

rm -f /tmp/flinkflow-smoke-test-errors.log

SUCCESS_COUNT=0
FAILURE_COUNT=0
FAILED_EXAMPLES=()

echo "Step 2: Iterating through examples in $EXAMPLES_DIR..."

# Change to the project root directory
cd "$(dirname "$0")/../.."

# Use find to get all yaml files recursively in subdirectories
for f in $(find $EXAMPLES_DIR -name "*.yaml"); do
    echo -n "Checking $(basename $f)... "
    
    # Run via mvn exec:java which handles classpath correctly including logging
    TMP_LOG=$(mktemp)
    if mvn exec:java -P local-run -Dexec.mainClass="ai.talweg.flinkflow.FlinkflowApp" \
        -Dexec.args="$f --dry-run --flowlet-dir deploy/k8s/flowlets" > "$TMP_LOG" 2>&1; then
        echo "[PASS]"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        echo "[FAIL]"
        FAILURE_COUNT=$((FAILURE_COUNT + 1))
        FAILED_EXAMPLES+=("$f")
        cat "$TMP_LOG" >> /tmp/flinkflow-smoke-test-errors.log
    fi
    rm "$TMP_LOG"
done

echo "========================================================="
echo "   Smoke Test Execution Summary                          "
echo "========================================================="
echo "Total Examples Tested: $((SUCCESS_COUNT + FAILURE_COUNT))"
echo "Passed:                $SUCCESS_COUNT"
echo "Failed:                $FAILURE_COUNT"

if [ $FAILURE_COUNT -gt 0 ]; then
    echo "The following examples failed dry-run validation:"
    for failed in "${FAILED_EXAMPLES[@]}"; do
        echo " - $failed"
    done
    echo "========================================================="
    echo "Detailed error logs can be found in: /tmp/flinkflow-smoke-test-errors.log"
    echo "========================================================="
    echo "[RESULT] Smoke tests FAILED."
    exit 1
else
    echo "========================================================="
    echo "[RESULT] All smoke tests PASSED."
    exit 0
fi
