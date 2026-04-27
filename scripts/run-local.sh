#!/bin/bash
# Copyright 2026 Talweg Authors
set -e

# --- Argument Parsing ---
COMPILE=true
FULL_REBUILD=false
REMAINING_ARGS=()

while [[ $# -gt 0 ]]; do
  case $1 in
    --no-compile|-n)
      COMPILE=false
      shift
      ;;
    --full-rebuild|-r)
      FULL_REBUILD=true
      shift
      ;;
    *)
      REMAINING_ARGS+=("$1")
      shift
      ;;
  esac
done

if [ ${#REMAINING_ARGS[@]} -eq 0 ]; then
    echo "Usage: ./scripts/run-local.sh [--no-compile|-n] [--full-rebuild|-r] <pipeline-yaml> [other-args]"
    exit 1
fi

PIPELINE_FILE="${REMAINING_ARGS[0]}"
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

JAR_FILE="target/flinkflow-0.9.0-BETA.jar"

needs_compile() {
  if [ ! -f "$JAR_FILE" ]; then
    return 0
  fi

  if [ "$FULL_REBUILD" = true ]; then
    return 0
  fi

  if [ "pom.xml" -nt "$JAR_FILE" ]; then
    return 0
  fi

  if [ -d "src/main" ] && find src/main -type f -newer "$JAR_FILE" | head -n 1 | grep -q .; then
    return 0
  fi

  if [ -d "src/test" ] && find src/test -type f -newer "$JAR_FILE" | head -n 1 | grep -q .; then
    return 0
  fi

  return 1
}

if [ "$COMPILE" = true ]; then
    if needs_compile; then
        if [ "$FULL_REBUILD" = true ]; then
            echo "Full rebuild (clean + package)..."
            mvn -q clean package -P local-run -DskipTests
        else
            echo "Incremental package..."
            mvn -q package -P local-run -DskipTests
        fi
    else
        echo "No source changes detected. Skipping compilation."
    fi
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

if [ ! -f "$JAR_FILE" ]; then
  echo "Error: JAR not found at $JAR_FILE"
  echo "Run without --no-compile or use --full-rebuild to build it."
  exit 1
fi

java -cp "$JAR_FILE" -XX:+IgnoreUnrecognizedVMOptions \
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
