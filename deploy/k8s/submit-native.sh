#!/bin/bash

# Flinkflow Native Kubernetes Submission Helper
# This script wraps the 'flink run-application' command for easier deployment.

set -e

# Default configuration
CLUSTER_ID=${CLUSTER_ID:-"flinkflow-native-cluster"}
IMAGE=${IMAGE:-"flinkflow:latest"}
SERVICE_ACCOUNT=${SERVICE_ACCOUNT:-"flink-service-account"}
PARALLELISM=${PARALLELISM:-"2"}
CONFIG_FILE=$1

# Basic usage check
if [ -z "$CONFIG_FILE" ]; then
    echo "Usage: $0 <path-to-pipeline.yaml> [image-name] [cluster-id]"
    echo "Example: $0 examples/standalone/simple-transform-example.yaml"
    exit 1
fi

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Configuration file '$CONFIG_FILE' not found."
    exit 1
fi

# Override image and cluster ID if provided as arguments
if [ ! -z "$2" ]; then
    IMAGE=$2
fi
if [ ! -z "$3" ]; then
    CLUSTER_ID=$3
fi

# Find flink binary
if [ -z "$FLINK_HOME" ]; then
    FLINK_BIN=$(which flink)
    if [ -z "$FLINK_BIN" ]; then
        echo "Error: 'flink' binary not found in PATH and FLINK_HOME is not set."
        echo "Please install Flink locally or set FLINK_HOME."
        exit 1
    fi
else
    FLINK_BIN="$FLINK_HOME/bin/flink"
fi

echo "🚀 Submitting Flinkflow Application to Kubernetes..."
echo "📍 Cluster ID: $CLUSTER_ID"
echo "🖼️  Image: $IMAGE"
echo "📄 Config: $CONFIG_FILE"

$FLINK_BIN run-application \
    --target kubernetes-application \
    -Dkubernetes.cluster-id="$CLUSTER_ID" \
    -Dkubernetes.container.image="$IMAGE" \
    -Dkubernetes.service-account="$SERVICE_ACCOUNT" \
    -Dkubernetes.rest-service.exposed.type=NodePort \
    -Djobmanager.memory.process.size=1600m \
    -Dtaskmanager.memory.process.size=1728m \
    -Dtaskmanager.numberOfTaskSlots="$PARALLELISM" \
    -Dparallelism.default="$PARALLELISM" \
    local:///opt/flink/usrlib/flinkflow.jar \
    --job-args "/opt/flink/conf/pipeline.yaml"

echo "✅ Submission complete. Use 'kubectl get pods' to monitor."
