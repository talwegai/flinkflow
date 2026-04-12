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

JARPrefix="target/flinkflow-"
# Ensure the fat JAR exists (pick the shaded one if multiple exist, fallback to main if needed, but it should be fixed context)
# We know version is 0.9.0-BETA based on context, but let's just use wildcard if safe, or exact name
JARFile="target/flinkflow-*-shaded.jar"
if ! ls $JARFile 1> /dev/null 2>&1; then
   JARFile="target/flinkflow-0.9.0-BETA.jar"
fi

java -cp target/flinkflow-0.9.0-BETA.jar -XX:+IgnoreUnrecognizedVMOptions \
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
