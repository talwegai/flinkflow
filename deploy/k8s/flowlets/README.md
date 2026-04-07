# 🧩 Flinkflow Flowlet Catalog

This directory contains the standard **Flowlet** catalog for Flinkflow. Flowlets are reusable, parameterized pipeline components that can be installed as Kubernetes Custom Resources (`kind: Flowlet`).

## 🚀 Overview

Flowlets provide a high-level abstraction over complex Flink connectors and processors, inspired by **Apache Camel Kamelets**. Instead of repeating verbose configuration in every pipeline, you define a Flowlet once and reference it by name.

### Installation
To install the full catalog into your cluster:
```bash
./k8s/install-flowlets.sh [NAMESPACE]
```

---

## 📦 Available Flowlets

| Name | Type | Description | Key Parameters |
| :--- | :--- | :--- | :--- |
| **`kafka-source`** | `source` | Reads from an Apache Kafka topic. | `bootstrapServers`, `topic`, `groupId` |
| **`kafka-sink`** | `sink` | Writes to an Apache Kafka topic. | `bootstrapServers`, `topic` |
| **`confluent-kafka-source`** | `source` | Confluent Cloud consumer with SASL_SSL. | `bootstrapServers`, `topic`, `apiKey`, `apiSecret` |
| **`confluent-kafka-sink`** | `sink` | Confluent Cloud producer with SASL_SSL. | `bootstrapServers`, `topic`, `apiKey`, `apiSecret` |
| **`http-enrich`** | `action` | Async HTTP enrichment (REST API). | `urlCode`, `mergeCode`, `timeout` |
| **`log-transform`** | `action` | Logs every record to stdout for debugging. | `prefix` |

---

## 📖 Usage in Pipelines

Once installed, you can use these Flowlets in your `Pipeline` CRDs by setting `type: flowlet` and providing arguments via the `with:` block.

### Example: Consuming from Kafka
```yaml
apiVersion: flinkflow.io/v1alpha1
kind: Pipeline
metadata:
  name: my-pipeline
spec:
  steps:
    - type: flowlet
      name: kafka-source
      with:
        bootstrapServers: "my-kafka:9092"
        topic: "input-data"
        groupId: "my-consumer-group"
```

### Example: Enrichment via HTTP
```yaml
    - type: flowlet
      name: http-enrich
      with:
        urlCode: 'return "https://api.myapp.com/lookup/" + input;'
        mergeCode: 'return input + " | meta=" + response;'
```

---

## 🛠️ Design Philosophy

1. **Parameterization**: Internal properties are exposed as typed parameters (string, integer, etc.).
2. **Dynamic Resolution**: Flinkflow expands Flowlets into concrete pipeline steps at job startup time.
3. **Secret Support**: Parameter values in `with:` can reference Kubernetes Secrets using the `secret:name/key` syntax.
4. **Labels & Groups**: Flowlets are grouped by functionality (e.g., `group: kafka`, `group: http`) for easier discovery.
