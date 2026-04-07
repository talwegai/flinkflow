# 🛠️ Flinkflow Operations & Monitoring

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

---

## ⚡ Performance: Polyglot "Compile Once" Architecture

Flinkflow achieves native-level performance through its **Janino-powered** (Java) and **GraalVM-powered** (Python) code injection system.

1.  **Compile Once, Execute Many**: Code snippets are compiled/optimized into JVM bytecode or GraalVM machine code **exactly once** during the Flink task initialization (`open()` method). This eliminates compilation overhead during the actual processing.
2.  **JIT Optimizations**: Once loaded, the bytecode is fully optimized by the JVM's **Just-In-Time (JIT)** compiler. For hot code paths (high-throughput streams), the performance is identical to pre-compiled Java classes.
3.  **Zero-Overhead Metrics**: Dynamic snippets interact directly with the `MetricGroup` object, ensuring low-latency metric collection without expensive context switching.
4.  **Memory Efficiency**: All compilation happens in-memory. No `.class` files are written to disk, which is critical for read-only container environments like Kubernetes.

### Throughput Benchmarks
Flinkflow has been tested to handle over **100,000 records/sec per task slot** for simple filtering and transformation logic on standard heap configurations.

---

## 🔍 Troubleshooting & Verification

### Dry-Run Mode
Use the `--dry-run` flag to validate your YAML structure and view the fully expanded and resolved graph without submitting the job to Flink.

```bash
./run-local.sh ../examples/standalone/simple-transform-example.yaml --dry-run
```

### Log Inspection
All Flinkflow logs prefix processing errors with **`[FLINKFLOW-ERROR]`**, making it easy to filter for pipeline transformation issues in centralized logging systems (e.g., Elasticsearch/Loki).
