#!/bin/bash
# Copyright 2026 Talweg Authors
set -e

if [ $# -lt 1 ]; then
    echo "Usage: ./run-local.sh <pipeline-yaml>"
    exit 1
fi

PIPELINE_FILE=$1

echo "Compiling..."
mvn clean package -P local-run -DskipTests

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

mvn exec:exec -P local-run \
 -Dexec.executable="java" \
 -Dexec.args="-classpath %classpath -XX:+IgnoreUnrecognizedVMOptions --sun-misc-unsafe-memory-access=allow --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.time=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/sun.net.util=ALL-UNNAMED --add-exports java.base/sun.net.util=ALL-UNNAMED -Dsun.misc.Unsafe.allowTerminallyDeprecated=true ai.talweg.flinkflow.FlinkflowApp $@"
