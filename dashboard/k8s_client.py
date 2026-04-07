# Copyright 2026 Talweg Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


from nicegui import ui
import os
import random
from datetime import datetime
import threading
import queue

# ─── Kubernetes Client (graceful fallback if no cluster) ─────────────────────
K8S_AVAILABLE = False
k8s_core_v1 = None
k8s_custom_objects = None
K8S_CONNECTION_ERROR = ""

try:
    from kubernetes import client, config

    try:
        config.load_incluster_config()
    except config.ConfigException:
        config.load_kube_config()

    k8s_core_v1 = client.CoreV1Api()
    k8s_custom_objects = client.CustomObjectsApi()
    K8S_AVAILABLE = True
except Exception as e:
    K8S_CONNECTION_ERROR = str(e)


# ─── Constants ────────────────────────────────────────────────────────────────

FLOWLET_GROUP = "flinkflow.io"
FLOWLET_VERSION = "v1alpha1"
FLOWLET_PLURAL = "flowlets"

# ─── Pure Helpers (no UI) ─────────────────────────────────────────────────────

def _pod_ready(pod) -> bool:
    if pod.status and pod.status.conditions:
        for cond in pod.status.conditions:
            if cond.type == "Ready":
                return cond.status == "True"
    return False


def _age_str(ts) -> str:
    if not ts:
        return "—"
    delta = datetime.now(ts.tzinfo) - ts
    if delta.days > 0:
        return f"{delta.days}d"
    hours = delta.seconds // 3600
    if hours > 0:
        return f"{hours}h"
    return f"{(delta.seconds % 3600) // 60}m"

# ─── Demo / Fallback Data ─────────────────────────────────────────────────────

def _demo_flink_pods() -> list:
    phases = ["Running", "Running", "Running", "Pending"]
    return [
        {"name": "flink-jobmanager-5f6d8-xk7j2",  "namespace": "flink-system",
         "role": "JOBMANAGER",  "phase": "Running",               "ready": True,  "age": "3d"},
        {"name": "flink-taskmanager-7c9b4-pq1lz",  "namespace": "flink-system",
         "role": "TASKMANAGER", "phase": "Running",               "ready": True,  "age": "3d"},
        {"name": "flink-taskmanager-7c9b4-mn8rw",  "namespace": "flink-system",
         "role": "TASKMANAGER", "phase": "Running",               "ready": True,  "age": "3d"},
        {"name": "flink-operator-6d4b9-zt5vq",     "namespace": "flink-system",
         "role": "OPERATOR",    "phase": random.choice(phases),   "ready": False, "age": "5d"},
    ]


def _demo_flowlets() -> list:
    return [
        {"name": "kafka-source",          "namespace": "flink-system", "type": "source", "version": "1.0", "title": "Kafka Source", "description": "Reads data from a Kafka topic.", "parameters": {"topic": {"description": "Name of the Kafka topic to consume from.", "type": "string", "required": True}}},
        {"name": "confluent-kafka-source","namespace": "flink-system", "type": "source", "version": "1.0", "title": "Confluent Kafka Source", "description": "Reads records from a Confluent Cloud, Confluent Platform, or CFK-managed Kafka topic.", "parameters": {
            "bootstrapServers": {"title": "Bootstrap Servers", "description": "Confluent cluster bootstrap endpoint.", "type": "string", "required": True},
            "topic": {"title": "Topic", "description": "Name of the Kafka topic to consume from.", "type": "string", "required": True}
        }},
        {"name": "http-enrichment",       "namespace": "flink-system", "type": "action", "version": "1.0", "title": "HTTP Enrichment", "description": "Enriches stream via HTTP GET.", "parameters": {}},
        {"name": "kafka-sink",            "namespace": "flink-system", "type": "sink",   "version": "1.0", "title": "Kafka Sink", "description": "Writes data to a Kafka topic.", "parameters": {}},
        {"name": "logging-sink",          "namespace": "flink-system", "type": "sink",   "version": "1.0", "title": "Logging Sink", "description": "Logs records to standard out.", "parameters": {}},
    ]

# ─── K8s Discovery ────────────────────────────────────────────────────────────

def discover_flink_pods(namespace: str = None) -> list:
    if not K8S_AVAILABLE:
        return _demo_flink_pods()
    try:
        raw = (
            k8s_core_v1.list_namespaced_pod(namespace=namespace).items
            if namespace
            else k8s_core_v1.list_pod_for_all_namespaces().items
        )
        results = []
        for pod in raw:
            labels = pod.metadata.labels or {}
            if not (
                "flink" in pod.metadata.name.lower()
                or labels.get("app") == "flink"
                or labels.get("app.kubernetes.io/name") == "flink"
                or labels.get("component") in ("jobmanager", "taskmanager")
            ):
                continue
            results.append({
                "name":      pod.metadata.name,
                "namespace": pod.metadata.namespace,
                "role":      labels.get("component", labels.get("app", "unknown")).upper(),
                "phase":     pod.status.phase or "Unknown",
                "ready":     _pod_ready(pod),
                "age":       _age_str(pod.metadata.creation_timestamp),
            })
        return results or _demo_flink_pods()
    except Exception:
        return _demo_flink_pods()


def discover_flowlets(namespace: str = None) -> list:
    if not K8S_AVAILABLE:
        return _demo_flowlets()
    try:
        items = (
            k8s_custom_objects.list_namespaced_custom_object(
                group=FLOWLET_GROUP, version=FLOWLET_VERSION,
                namespace=namespace, plural=FLOWLET_PLURAL,
            ).get("items", [])
            if namespace
            else k8s_custom_objects.list_cluster_custom_object(
                group=FLOWLET_GROUP, version=FLOWLET_VERSION, plural=FLOWLET_PLURAL,
            ).get("items", [])
        )
        results = []
        for item in items:
            meta, spec = item.get("metadata", {}), item.get("spec", {})
            results.append({
                "name":      meta.get("name", "unknown"),
                "namespace": meta.get("namespace", "cluster-wide"),
                "type":      spec.get("type", "unknown"),
                "version":   spec.get("version", "—"),
                "title":     spec.get("title", meta.get("name", "—")),
                "description": spec.get("description", "No description provided."),
                "parameters": spec.get("parameters", {}),
            })
        return results or _demo_flowlets()
    except Exception:
        return _demo_flowlets()

# ─── Log Streaming ────────────────────────────────────────────────────────────
import asyncio

log_queue = queue.Queue()

def stream_pod_logs(pod_name: str, namespace: str, is_streaming_ref: dict):
    if not K8S_AVAILABLE:
        log_queue.put(f"[DEMO] Tailing {pod_name}")
        return

    try:
        w = k8s_core_v1.read_namespaced_pod_log(
            name=pod_name,
            namespace=namespace,
            follow=True,
            _preload_content=False,
            tail_lines=100
        )
        for line in w:
            if not is_streaming_ref.get('active', False):
                break
            text_line = line.decode('utf-8').strip()
            # Push safely to a standard Python queue
            log_queue.put(text_line)
    except Exception as e:
        log_queue.put(f"ERROR: Failed to stream logs: {e}")

# We use a mutable dict to signal the thread to stop
current_stream_ref = {'active': False}

def start_log_stream(pod_name: str, namespace: str, log_ui: ui.log):
    global current_stream_ref
    
    # 1. Stop old stream
    current_stream_ref['active'] = False
    log_ui.clear()
    
    # 2. Empty out the queue 
    while not log_queue.empty():
        try:
            log_queue.get_nowait()
        except queue.Empty:
            break
            
    # 3. Start new stream
    current_stream_ref = {'active': True}
    threading.Thread(target=stream_pod_logs, args=(pod_name, namespace, current_stream_ref), daemon=True).start()

def consume_log_queue(log_ui: ui.log):
    """Timer callback to safely pull from the thread queue into the UI."""
    messages = []
    # Pull up to 100 messages at a time so we don't lag the UI
    for _ in range(100):
        try:
            messages.append(log_queue.get_nowait())
        except queue.Empty:
            break
            
    # NiceGUI allows this because it's called from a ui.timer (which is on the main loop)
    for msg in messages:
        # Instead of `log_ui.push` creating Label elements (which lags/breaks),
        # use a more generic javascript approach or raw push.
        # However log_ui.push is reliable if on the main thread and not overloaded:
        log_ui.push(msg)

