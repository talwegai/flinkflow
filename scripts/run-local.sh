#!/bin/bash
# Copyright 2026 Talweg Authors
set -e

# --- Argument Parsing ---
COMPILE=true
REMAINING_ARGS=()

while [[ $# -gt 0 ]]; do
  case $1 in
    --no-compile|-n)
      COMPILE=false
      shift
      ;;
    *)
      REMAINING_ARGS+=("$1")
      shift
      ;;
  esac
done

if [ ${#REMAINING_ARGS[@]} -eq 0 ]; then
    echo "Usage: ./scripts/run-local.sh [--no-compile|-n] <pipeline-yaml> [other-args]"
    exit 1
fi

PIPELINE_FILE="${REMAINING_ARGS[0]}"

if [ "$COMPILE" = true ]; then
    echo "Compiling..."
    mvn clean package -P local-run -DskipTests
else
    echo "Skipping compilation..."
fi

echo "Running Pipeline: $PIPELINE_FILE..."
export MAVEN_OPTS="-XX:+IgnoreUnrecognizedVMOptions \
                   --sun-misc-unsafe-memory-access=allow \
                   --add-opens java.base/java.lang=ALL-UNNAMED \
                   --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
                   --add-opens java.base/java.util=ALL-UNNAMED \
                   --add-opens java.base/java.time=ALL-UNNAMED \
                   --add-opens java.base/java.nio=ALL-UNNAMED \
                   --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
                   --add-opens java.base/sun.net.util=ALL-UNNAMED \
                   --add-exports java.base/sun.net.util=ALL-UNNAMED \
                   -Dsun.misc.Unsafe.allowTerminallyDeprecated=true"

# Locate the project JAR (avoiding 'original-' prefixed ones from the shade plugin)
JARFile=$(ls target/flinkflow-*.jar 2>/dev/null | grep -v "original-" | head -n 1)

if [ -z "$JARFile" ]; then
    echo "Error: Could not find Flinkflow JAR in target directory. Please run 'mvn clean package' first."
    exit 1
fi

java -cp "$JARFile" -XX:+IgnoreUnrecognizedVMOptions \
  --sun-misc-unsafe-memory-access=allow \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
  --add-opens java.base/java.util=ALL-UNNAMED \
  --add-opens java.base/java.time=ALL-UNNAMED \
  --add-opens java.base/java.nio=ALL-UNNAMED \
  --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
  --add-opens java.base/sun.net.util=ALL-UNNAMED \
  --add-exports java.base/sun.net.util=ALL-UNNAMED \
  -Dsun.misc.Unsafe.allowTerminallyDeprecated=true \
  ai.talweg.flinkflow.FlinkflowApp "${REMAINING_ARGS[@]}"
