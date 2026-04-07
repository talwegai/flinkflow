# 🚀 Standalone Pipeline Examples

This directory contains standalone **Flinkflow Pipeline** definitions intended for local development, testing, and Docker-based deployments.

## 🏃 How to Run

Unlike the Kubernetes examples, these are plain YAML files that should be run using the `run-local.sh` helper script:

```bash
./run-local.sh examples/standalone/[EXAMPLE_NAME].yaml
```

---

## 📦 Examples Catalog

### 🔄 Core Transformations
- **`simple-transform-example.yaml`**: Basic 1-to-1 processing (Map function) with standard input and console output.
- **`filter-example.yaml`**: Retain or discard records based on boolean logic (Filter function).
- **`flatmap-example.yaml`**: Emit multiple records from a single input, such as splitting sentences into words.

### 📊 Aggregations & Windows
- **`keyby-example.yaml`**: Partition your stream by a dynamic key (e.g., user ID) for parallel processing.
- **`reduce-example.yaml`**: Rolling aggregations that combine consecutive records.
- **`window-example.yaml`**: Time-based windowing (Tumbling, Sliding, Session) for bounded time-period analytics.

### 🛡️ Error Handling & Flow Control
- **`sideoutput-example.yaml`**: Branch your pipeline to handle errors or specific conditions separately (e.g., Dead Letter Queues).

### 🔌 Connectivity
- **Kafka**: 
    - `kafka-example.yaml`: Standard producer/consumer.
    - `kafka-auth-example.yaml`: SASL_SSL/Scram-SHA-256 secure authenticated setup.
    - `kafka-avro-schema-registry-example.yaml`: Enterprise-grade Avro serialization with automatic schema resolution.
- **Files & S3**: 
    - `file-monitoring-example.yaml`: Watch a directory for new incoming files (Unbounded source).
    - `file-sink-example.yaml`: Write results to local or S3 storage with rolling policies.
- **Databases**: 
    - `jdbc-sink-example.yaml`: High-throughput batch inserts to PostgreSQL/MySQL via JDBC.
- **HTTP**: 
    - `http-lookup-example.yaml`: Asynchronous REST API enrichment (Async I/O).
    - `http-sink-example.yaml`: Push results directly to webhooks or external APIs.

### 💎 Advanced Framework Features
- **`flowlet-example.yaml`**: Showcase how to structure complex pipelines using reusable, shared Flowlets.
- **`datamapper-example.yaml`**: Complex structural transformations between JSON/XML schemas using the **XSLT 3.0** DataMapper.
- `python-snippets-example.yaml`: Basic **Python snippets** (GraalVM) for `filter`, `process`, and `flatmap`.
- `python-json-datetime-example.yaml`: **JSON & Datetime** logic using standard Python modules.
- `python-complex-example.yaml`: Advanced **Python processing** with JSON parsing, `datetime`, and array 'exploding' logic.

- `complex-enrichment-example.yaml`: A multi-step pipeline demonstrating joins, lookups, and nested transformations.

---

## 🛠️ Design Philosophy

- **Local-First Development**: All examples here can be run without a Kubernetes cluster.
- **Instant Hot-Reload**: Modify the Java/Python snippets in these YAMLs and re-run instantly — no JAR recompilation required.
- **Constrained Logic**: High-level structure stays in YAML, while business logic stays in targeted Java or Python snippets.
