# ☸️ Kubernetes-Native Pipeline Examples

This directory contains example **`Pipeline`** Kubernetes Custom Resources (CRDs). These are designed to be managed directly via `kubectl` and are executed by Flinkflow in a Kubernetes-native environment.

## 🚀 Overview

Unlike the standalone YAML examples, these files are formatted as Kubernetes objects with `apiVersion: flinkflow.io/v1alpha1` and `kind: Pipeline`.

### Prerequisites
1.  **Install CRDs**:
    ```bash
    kubectl apply -f k8s/crd-pipeline.yaml
    kubectl apply -f k8s/crd-flowlet.yaml
    ```
2.  **Setup Permissions**:
    ```bash
    kubectl apply -f k8s/rbac.yaml
    ```

---

## 📦 Available Examples

| File | Description | Features |
| :--- | :--- | :--- |
| **`simple-transform-example.yaml`** | Minimal Example | Basic static source → uppercase → console sink. |
| **`filter-example.yaml`** | Filter Logic | Filter records by length using Java/Python logic. |
| **`flatmap-example.yaml`** | N-to-N Mapping | Split sentences into words (Java/Python). |
| **`keyby-example.yaml`** | Partitioning | Partition stream by extracted key for parallelism. |
| **`reduce-example.yaml`** | Aggregations | Rolling word count aggregation. |
| **`window-example.yaml`** | Time Windows | Tumbling time-window aggregation. |
| **`sideoutput-example.yaml`** | Flow Control | Route error records to a side stream. |
| **`join-example.yaml`** | Stream Joins | Interval join between two Kafka topics. |
| **`kafka-example.yaml`** | Standard Kafka | Production-ready Kafka source → sink. |
| **`kafka-auth-example.yaml`** | Secure Kafka | Kafka with SASL_SSL authentication. |
| **`kafka-avro-schema-registry-example.yaml`** | Avro & Registry | Avro serialization with automatic schema fetching. |
| **`kafka-streaming-demo.yaml`** | Full Demo | Complete Kafka-to-Kafka streaming pipeline. |
| **`datagen-streaming-demo.yaml`** | Data Generation | Continuous unbounded data generation for testing. |
| **`datamapper-example.yaml`** | Data Mapping | XSLT 3.0 structural transformations via Saxon-HE. |
| **`python-snippets-example.yaml`** | Basic Python | Inline Python code via GraalVM for simple logic. |
| **`python-json-datetime-example.yaml`** | JSON & Dates | Standard Python modules for JSON/Datetime logic. |
| **`python-complex-example.yaml`** | Advanced Python | JSON parsing and array 'exploding' logic. |

| **`python-stateful-example.yaml`** | Stateful Python | Python-based `keyBy`, `window`, and `reduce`. |
| **`http-lookup-example.yaml`** | Async Enrichment | Asynchronous REST API lookups (Async I/O). |
| **`http-sink-example.yaml`** | Webhook Sink | Push results to external HTTP endpoints. |
| **`jdbc-sink-example.yaml`** | DB Batch Sink | High-throughput batch inserts into PostgreSQL/MySQL. |
| **`file-sink-example.yaml`** | File/S3 Sink | Write partitioned, rolling results to storage. |
| **`file-monitoring-example.yaml`** | File Source | Continuous directory monitoring for new files. |
| **`flowlet-example.yaml`** | Flowlets | Reusable, parameterized components in K8s. |
| **`k8s-native-pipeline-resource.yaml`** | Minimal Template | Simple k8s-native pipeline structure. |
| **`monitor-demo.yaml`** | Dashboard Demo | Optimized for live metrics in the NiceGUI dashboard. |
| **`complex-enrichment-example.yaml`** | Multi-step Pipeline | Advanced enrichment involving joins and lookups. |

---

## 💡 How to Run

1.  **Apply an Example**:
    ```bash
    kubectl apply -f examples/k8s/kafka-streaming-demo.yaml
    ```

2.  **Deploy the Flinkflow Engine**:
    Use the native-pipeline deployment manifest to start a Flink worker that watches for `Pipeline` CRs:
    ```bash
    kubectl apply -f k8s/native-pipeline-deployment.yaml
    ```
    *(Note: Ensure the JobManager arguments in `native-pipeline-deployment.yaml` match the `metadata.name` of your Pipeline CR.)*

3.  **Check Status**:
    ```bash
    kubectl get pipes
    # or
    kubectl describe pipeline kafka-streaming-demo
    ```

---

## 🛠️ Design Philosophy

- **Declarative GitOps**: Treat your data pipelines as first-class Kubernetes citizens.
- **Unified Management**: Manage connectors and transformations using standard Kubernetes workflows (`ArgoCD`, `Helm`, etc.).
- **Infrastructure Abstraction**: No need to manage local YAML files or ConfigMaps; the Flinkflow engine resolves the configuration directly from the Kubernetes API.
