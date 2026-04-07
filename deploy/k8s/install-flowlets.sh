#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# install-flowlets.sh
#
# Installs the Flinkflow Flowlet CRD and the built-in Flowlet catalog onto a
# Kubernetes cluster.  This is analogous to installing the Apache Camel
# Kamelet catalog.
#
# Usage:
#   ./k8s/install-flowlets.sh [NAMESPACE]
#
# Arguments:
#   NAMESPACE   Kubernetes namespace to install the Flowlet CRs into.
#               Defaults to the current kubectl namespace.
#
# Prerequisites:
#   - kubectl configured against the target cluster
#   - cluster-admin privileges (for CRD installation)
#
# Examples:
#   ./k8s/install-flowlets.sh              # installs into current namespace
#   ./k8s/install-flowlets.sh flink-jobs   # installs into 'flink-jobs' namespace
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NAMESPACE="${1:-}"

# ── 1. Install / update the CRD (cluster-scoped) ────────────────────────────
echo "▶ Installing Flowlet CRD (flinkflow.io/v1alpha1)..."
kubectl apply -f "${SCRIPT_DIR}/crds/crd-flowlet.yaml"

echo "  Waiting for CRD to become established..."
kubectl wait --for=condition=established \
  --timeout=60s \
  crd/flowlets.flinkflow.io

# ── 2. Determine the target namespace ───────────────────────────────────────
if [ -z "${NAMESPACE}" ]; then
  NAMESPACE="$(kubectl config view --minify -o jsonpath='{..namespace}')"
  NAMESPACE="${NAMESPACE:-default}"
fi
echo "▶ Installing built-in Flowlet catalog into namespace: ${NAMESPACE}"

# ── 3. Install each built-in Flowlet CR ─────────────────────────────────────
FLOWLETS_DIR="${SCRIPT_DIR}/flowlets"
if [ ! -d "${FLOWLETS_DIR}" ]; then
  echo "ERROR: Flowlets directory not found at ${FLOWLETS_DIR}"
  exit 1
fi

for FLOWLET_FILE in "${FLOWLETS_DIR}"/*.yaml; do
  NAME="$(basename "${FLOWLET_FILE}" .yaml)"
  echo "  Installing Flowlet: ${NAME}"
  kubectl apply -n "${NAMESPACE}" -f "${FLOWLET_FILE}"
done

# ── 4. Summary ───────────────────────────────────────────────────────────────
echo ""
echo "✅ Flowlet catalog installed successfully."
echo ""
echo "Installed Flowlets in namespace '${NAMESPACE}':"
kubectl get flowlets -n "${NAMESPACE}" \
  -o custom-columns="NAME:.metadata.name,TYPE:.spec.type,VERSION:.spec.version,TITLE:.spec.title"

echo ""
echo "To use a Flowlet in your pipeline.yaml, add:"
echo ""
echo "  - type: flowlet"
echo "    name: <flowlet-name>"
echo "    with:"
echo "      <paramName>: <value>"
echo ""
echo "And start Flinkflow with Kubernetes Flowlet discovery enabled:"
echo ""
echo "  args: [\"/opt/flink/conf/pipeline.yaml\", \"--enable-k8s-flowlets\", \"--k8s-namespace\", \"${NAMESPACE}\"]"
