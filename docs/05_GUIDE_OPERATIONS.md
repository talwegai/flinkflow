# 🛠️ Flinkflow Operations & Monitoring (alpha)

This guide covers the technical operations of Flinkflow, including visual monitoring, throughput tracking, and high-performance JVM optimization.

---

## 📊 Monitoring & Observability

Flinkflow includes a built-in **NiceGUI-based dashboard** (Python) for real-time visibility into your streaming jobs.

### Key Capabilities
- **Job Health**: Monitor stateful checkpointing, backpressure, and resource utilization.
- **Live Metrics**: Track per-step record counts and processing time directly in the UI.
- **In-Cluster Insights**: View Kubernetes pod status and Flink JobManager/TaskManager logs.

### Deployment in Kubernetes
The dashboard can be deployed as a side-channel monitoring service using the `k8s/monitor-deployment.yaml` manifest.

```bash
kubectl apply -f k8s/monitor-deployment.yaml
```

For detailed configuration of the Python client and UI components, refer to the **[Dashboard Directory](https://github.com/talwegai/flinkflow/blob/main/dashboard/README.md)**.

### 📈 Enterprise Monitoring: Prometheus & Grafana
For high-scale production workloads, Flinkflow exposes native metrics for ingestion via Prometheus.

1.  **Metric Exposure**: The Flink cluster is configured to export metrics on port `9249` via the `PrometheusReporter`.
2.  **Scraping**: If you have a Prometheus operator installed, the `deployment.yaml` includes annotations to automatically trigger scraping.
3.  **Visualizing**: A pre-built Grafana dashboard is available at **`deploy/monitor/grafana-dashboard.json`**. This dashboard provides real-time visibility into Job-level throughput, JVM memory health, and task-level record rates.

**Key Metrics to Watch:**
*   `flink_jobmanager_job_numRecordsInPerSecond`: Input throughput per job.
*   `flink_jobmanager_job_numRecordsOutPerSecond`: Output throughput per job.
*   `flink_taskmanager_Status_JVM_Memory_Heap_Used`: Memory footprint of processing workers.

---

## ⚡ Performance: Polyglot "Compile Once" Architecture

Flinkflow achieves native-level performance through its **Janino-powered** (Java) and **GraalVM-powered** (Python) code injection system.

1.  **Compile Once, Execute Many**: Code snippets are compiled/optimized into JVM bytecode or GraalVM machine code **exactly once** during the Flink task initialization (`open()` method). This eliminates compilation overhead during the actual processing.
2.  **JIT Optimizations**: Once loaded, the bytecode is fully optimized by the JVM's **Just-In-Time (JIT)** compiler. For hot code paths (high-throughput streams), the performance is identical to pre-compiled Java classes.
3.  **Zero-Overhead Metrics**: Dynamic snippets interact directly with the `MetricGroup` object, ensuring low-latency metric collection without expensive context switching.
4.  **Memory Efficiency**: All compilation happens in-memory. No `.class` files are written to disk, which is critical for read-only container environments like Kubernetes.

### Throughput Benchmarks
Flinkflow has been tested to handle over **100,000 records/sec per task slot** for simple filtering and transformation logic on standard heap configurations.

### 🧠 State V2: Asynchronous Memory Management (New in Flink 2.2)
Starting with Flinkflow v1.0, stateful operations (like Agentic Memory) utilize **Flink State V2**.

1.  **Non-blocking I/O**: Previously, state access (`ValueState.value()`) was a blocking operation that halted the task thread. With State V2, Flinkflow uses `asyncValue().thenAccept(...)`, allowing the thread to continue processing other records while the state is being fetched from the backend (especially beneficial for RocksDB or remote S3 state backends).
2.  **Higher Throughput for Agents**: Agents with large history can now process records more efficiently as the LLM inference and state persistence happen concurrently.
3.  **Disaggregated State Compatibility**: Flink 2.2's State V2 is designed for disaggregated storage, allowing your Agentic Bridge to scale to millions of concurrent "conversations" without filling TaskManager local disks.

---

## 🔍 Troubleshooting & Verification

### Dry-Run Mode
Use the `--dry-run` flag to validate your YAML structure and view the fully expanded and resolved graph without submitting the job to Flink.

```bash
./scripts/run-local.sh examples/standalone/simple-transform-example.yaml --dry-run
```

### Log Inspection
All Flinkflow logs prefix processing errors with **`[FLINKFLOW-ERROR]`**, making it easy to filter for pipeline transformation issues in centralized logging systems (e.g., Elasticsearch/Loki).
