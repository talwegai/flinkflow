# ☸️ Kubernetes-Native Pipeline Examples

This directory contains example **`Pipeline`** Kubernetes Custom Resources (CRDs). These are designed to be managed directly via `kubectl` and are executed by Flinkflow in a Kubernetes-native environment.

## 🚀 Overview

Unlike the standalone YAML examples, these files are formatted as Kubernetes objects with `apiVersion: flinkflow.io/v1alpha1` and `kind: Pipeline`.

### Prerequisites
1.  **Install CRDs**:
    ```bash
    kubectl apply -f deploy/k8s/crds/crd-pipeline.yaml
    kubectl apply -f deploy/k8s/crds/crd-flowlet.yaml
    ```
2.  **Setup Permissions**:
    ```bash
    kubectl apply -f deploy/k8s/rbac.yaml
    ```

---

## 📦 Available Examples

### 🤖 Agentic & AI (Gemini)
| File | Description | Features |
| :--- | :--- | :--- |
| **[`camel/agent-support-triage.yaml`](camel/agent-support-triage.yaml)** | **AI Triage (Camel)** | **[NEW]** Gemini ticket categorization via Camel CRD with K8s Secrets. |
| **[`python/agent-support-triage.yaml`](python/agent-support-triage.yaml)** | **AI Triage (Python)**| **[NEW]** Gemini ticket categorization via Python CRD. |

### 🐫 Low-Code & Declarative (Apache Camel)
*(Refs: [Simple Language](https://camel.apache.org/components/latest/languages/simple-language.html) | [JsonPath](https://camel.apache.org/components/latest/languages/jsonpath-language.html) | [YAML DSL](https://camel.apache.org/manual/camel-yaml-dsl.html))*
| File | Description | Features |
| :--- | :--- | :--- |
| **[`camel/iot-fleet-analytics-camel.yaml`](camel/iot-fleet-analytics-camel.yaml)** | IoT (Camel) | High-scale sensor telemetry via Camel & Groovy CRD. |
| **[`camel/camel-yaml-dsl-example.yaml`](camel/camel-yaml-dsl-example.yaml)** | YAML DSL | Complex routing (Choice/When) using Camel YAML DSL. |
| **[`camel/fraud-detection-camel.yaml`](camel/fraud-detection-camel.yaml)** | Fraud (YAML) | Risk scoring and alerting via YAML DSL routes. |
| **[`camel/camel-flatmap-example.yaml`](camel/camel-flatmap-example.yaml)** | FlatMap (Camel) | Explode JSON arrays into individual records via JsonPath. |
| **[`camel/camel-filter-example.yaml`](camel/camel-filter-example.yaml)** | Filter (Camel) | Filter records using Camel Simple logical expressions. |
| **[`camel/camel-simple-transform.yaml`](camel/camel-simple-transform.yaml)** | Transform (Camel) | Declarative string manipulation and formatting. |

### 🐍 Procedural (Python)
| File | Description | Features |
| :--- | :--- | :--- |
| **[`python/iot-fleet-analytics-python.yaml`](python/iot-fleet-analytics-python.yaml)** | IoT (Python) | High-scale sensor telemetry using Python CRD. |
| **[`python/fraud-detection-flowlets-python.yaml`](python/fraud-detection-flowlets-python.yaml)** | Fraud (Python) | Transaction monitoring using Python Flowlets. |
| **[`python/python-complex-example.yaml`](python/python-complex-example.yaml)** | Advanced Python | JSON parsing and array 'exploding' logic. |
| **[`python/python-stateful-example.yaml`](python/python-stateful-example.yaml)** | Stateful Python | Python-based `keyBy`, `window`, and `reduce`. |
| **[`python/http-lookup-python-example.yaml`](python/http-lookup-python-example.yaml)** | Async (Python) | Async REST lookups with Python handlers. |

### ☕ Procedural (Java)
| File | Description | Features |
| :--- | :--- | :--- |
| **[`java/iot-fleet-analytics.yaml`](java/iot-fleet-analytics.yaml)** | IoT (Java) | Original high-scale sensor telemetry demo. |
| **[`java/fraud-detection-flowlets.yaml`](java/fraud-detection-flowlets.yaml)** | Fraud (Java) | Transaction monitoring via Java Flowlets. |
| **[`java/complex-enrichment-example.yaml`](java/complex-enrichment-example.yaml)** | Advanced | Multi-step pipeline with joins and lookups. |
| **[`java/kafka-streaming-demo.yaml`](java/kafka-streaming-demo.yaml)** | Full Demo | Complete Kafka-to-Kafka streaming pipeline. |
| **[`java/window-example.yaml`](java/window-example.yaml)** | Time Windows | Tumbling time-window aggregation. |
| **[`java/datamapper-example.yaml`](java/datamapper-example.yaml)** | Data Mapping | XSLT 3.0 structural transformations. |

---

## 💡 How to Run

1.  **Apply an Example**:
    ```bash
    kubectl apply -f examples/k8s/camel/agent-support-triage.yaml
    ```

2.  **Deploy the Flinkflow Engine**:
    Use the native-pipeline deployment manifest to start a Flink worker that watches for `Pipeline` CRs:
    ```bash
    kubectl apply -f deploy/k8s/native-pipeline-deployment.yaml
    ```
    *(Note: Ensure the JobManager arguments in `native-pipeline-deployment.yaml` match the `metadata.name` of your Pipeline CR.)*

3.  **Check Status**:
    ```bash
    kubectl get pipes
    # or
    kubectl describe pipeline agent-support-triage-camel
    ```

---

## 🛠️ Design Philosophy

- **Declarative GitOps**: Treat your data pipelines as first-class Kubernetes citizens.
- **Unified Management**: Manage connectors and transformations using standard Kubernetes workflows (`ArgoCD`, `Helm`, etc.).
- **Infrastructure Abstraction**: No need to manage local YAML files or ConfigMaps; the Flinkflow engine resolves the configuration directly from the Kubernetes API.
