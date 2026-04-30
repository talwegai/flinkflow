# Flinkflow Developer Guide

Welcome to the Flinkflow core! This guide is intended for developers who want to understand the internal mechanics of the engine or extend its core functionality.

---

## 🏗️ Architecture Overview

Flinkflow acts as a **Declarative Translation Layer** between YAML-based definitions and the Apache Flink DataStream API.

For a detailed visual guide and component breakdown, see **[Architecture.md](01_ARCHITECTURE.md)**.

### Core Stack
*   **Language**: Java 17
*   **Engine**: Apache Flink 1.18+
*   **Polyglot Logic**: 
    *   **Janino**: For in-memory Java compilation of code snippets.
    *   **GraalVM (Polyglot API)**: For the secure zero-trust Python sandbox.
    *   **Apache Camel**: Declarative expression engine for Simple and JsonPath.
*   **Data Mapping**: Saxon-HE (XSLT 3.0) for transform logic.
*   **Configuration**: Jackson YAML for pipeline deserialization.

---

## 📂 Project Structure

*   **/src/main/java**:
    *   `core/`: Pipeline building logic and execution engine.
    *   `config/`: YAML models and deserialization logic.
    *   `flowlet/`: Registry and discovery for reusable components.
    *   `operators/`: Flink custom operators (Transformations, Sinks).
*   **/dashboard**: Python-based (NiceGUI) monitoring dashboard for real-time observability.
*   **/website**: Hugo (Doks theme) documentation site source.
*   **/k8s**: Kubernetes CRDs, manifests, and deployment scripts.

---

## ⚙️ Extending the Engine

### Adding a New Step Type
1.  **Define the Model**: Add a new configuration class in `ai.talweg.flinkflow.config`.
2.  **Implement the Logic**: Add a corresponding operator or generator in `ai.talweg.flinkflow.core.PipelineGraphBuilder`.
3.  **Register the Type**: Update the YAML parser to recognize your new step.

### Camel Integration Logic
The `CamelEvaluator` provides a unified engine for evaluating Simple and JsonPath expressions.
- **Header Mapping**: For key-selectors and reducers, Flinkflow automatically maps input variables to Camel headers (e.g., `value1`, `value2`).
- **Lazy Initialization**: Camel contexts are heavy. Our `DynamicCamel...` functions utilize lazy initialization in the `open()` method to avoid overhead on the JobManager.

### Adding a New Flowlet
Flowlets are discoverable, parameterized components.
1.  **Create a YAML definition**: Set the `kind: Flowlet`.
2.  **Define parameters**: Use `properties` to expose customization.
3.  **Place it** in the `deploy/k8s/flowlets/` directory for discovery.

---

## 🛠️ Development Tools

### Automated Testing
*   **Maven Unit Tests**: `mvn test` (covers logic, validation, and parsing).
*   **Smoke Test Suite**: `./deploy/test/smoke-test.sh` (validates all YAML examples against the engine in dry-run mode).

### Debugging Snapshots
When the engine builds a graph, it logs a textual representation of the DAG. Use `-Dlog.level=DEBUG` to see the detailed step-by-step resolution.

---

## 📦 Building and Distribution

### JAR Distribution
The project uses the `maven-shade-plugin` to create a "fat JAR" containing all dependencies (Janino, GraalVM dependencies, etc.).

```bash
mvn clean package
```
Output: `target/flinkflow-{version}.jar`

### Docker Images
The **[Dockerfile](https://github.com/talwegai/flinkflow/blob/main/Dockerfile)** is based on the official Flink image and includes all necessary environment variables for the Python evaluator.

---
## Getting Started

### Prerequisites for development and local run

- Java 17+ (Java 21 recommended for Flink 2.2)
- Maven 3.8+
- Apache Flink 2.2.0 (for deployment)
- Docker (optional, for containerization)
- Kubernetes (optional, for deployment)

### Prerequisites for flinkflow runtime

- Docker (optional)
- Kubernetes (recommended, for production deployment)

### Build the Project

```bash
mvn clean package
```

This will produce a shaded JAR in `target/flinkflow-{version}.jar`.

### 🧪 Smoke Testing

To ensure all examples and core components are functional, you can run the smoke test suite. This suite performs a `--dry-run` validation on all standalone YAML examples.

**Via Shell Script (Full validation):**
```bash
./deploy/test/smoke-test.sh
```

**Via Maven (JUnit-integrated):**
```bash
mvn test -Dtest=SmokeTestSuite
```

### Run Locally

You can run a pipeline locally using the provided helper script:

1. Create a `pipeline.yaml` (see `examples/standalone/java/simple-transform-example.yaml`):

```yaml
name: "My Flink Job"
parallelism: 1
steps:
  - type: source
    name: static-source
    properties:
      content: "Hello,World"

  - type: process
    name: simple-transform
    code: |
      return input.toUpperCase() + " processed!";

  - type: sink
    name: console-sink
    properties:
      type: console
```

2. Run using the helper script:

```bash
./scripts/run-local.sh examples/standalone/java/simple-transform-example.yaml
```

Or manually using Maven to spin up a managed local cluster execution:

```bash
mvn exec:exec -P local-run -Dapp.args="examples/standalone/java/simple-transform-example.yaml"
```
*(This automatically safely handles Flink 2.2 classloading and Java 17+ memory flags via a natively forked JVM.)*

### 🛠️ Advanced CLI Arguments

Flinkflow supports several arguments to aid local development and validation:

| Flag | Description |
| :--- | :--- |
| `--dry-run` | Validates the YAML structure and expands all Flowlets into a final pipeline, printing the result without executing the Flink job. |
| `--flowlet-dir <path>` | Specifies a local directory to search for Flowlet definitions. This allows you to test Flowlets locally without a Kubernetes cluster. |

**Example (Dry-run with local flowlets):**
```bash
mvn exec:exec -P local-run -Dapp.args="examples/standalone/java/complex-enrichment-example.yaml --dry-run --flowlet-dir deploy/k8s/flowlets"
```
#### Building Locally

1. Build the Docker image:
   (Ensure you have run `mvn clean package` first to generate the JAR)

```bash
docker build -t flinkflow:latest -f deploy/docker/Dockerfile .
```

2. **Quick Test**: Run a sample pipeline in Local Mode (MiniCluster) directly from the container:

```bash
# Run a bounded example (Hello World)
docker run --rm flinkflow:latest \
  java -cp "/opt/flink/usrlib/flinkflow.jar:/opt/flink/lib/*" \
  ai.talweg.flinkflow.FlinkflowApp \
  /opt/flink/examples/standalone/java/hello-world.yaml
```

3. **Solutions Demo**: Run a continuous, multi-step IoT analytics pipeline:

```bash
# Run the Python-based solutions demo
docker run --rm flinkflow:latest \
  java -cp "/opt/flink/usrlib/flinkflow.jar:/opt/flink/lib/*" \
  ai.talweg.flinkflow.FlinkflowApp \
  /opt/flink/examples/standalone/python/iot-fleet-analytics-python.yaml

# Run the Camel-based (Low-Code/Declarative) solutions demo
docker run --rm flinkflow:latest \
  java -cp "/opt/flink/usrlib/flinkflow.jar:/opt/flink/lib/*" \
  ai.talweg.flinkflow.FlinkflowApp \
  /opt/flink/examples/standalone/camel/iot-fleet-analytics-camel.yaml
```

---
### Quick Syntax Example
```yaml
name: "Secure Kafka Filter"
steps:
  - type: source
    name: kafka-source
    properties:
      topic: "raw-orders"
      sasl.jaas.config: "secret:kafka-auth/jaas-config" # Resolved from K8s
  - type: filter
    code: "return input.contains(\"valid\");"
  - type: sink
    name: console-sink
```

### Python Processing Example
```yaml
name: "Python Order Processor"
steps:
  - type: source
    name: static-source
    properties: { content: '{"id": "ORD-1", "amount": 100}|{"id": "ORD-2", "amount": 200}' }
  - type: process
    language: python
    code: |
      import json
      from datetime import datetime
      order = json.loads(input)
      order["processed_at"] = datetime.now().isoformat()
      return json.dumps(order)
  - type: sink
    name: console-sink
```

### 🤖 Agentic AI Example (Autonomous Reasoning)
```yaml
name: "Autonomous Support Agent"
steps:
  - type: source
    name: customer-query-stream
  - type: agent
    name: help-genius
    properties:
      model: "gpt-4o"
      systemPrompt: "Analyze the user query. Use the order-lookup tool if needed."
      tools: "order-lookup, refund-processor"
  - type: sink
    name: action-log

### 🤖 Local AI with Ollama
```yaml
name: "Local Offline Agent"
steps:
  - type: source
    name: logs
  - type: agent
    name: local-analyst
    properties:
      model: "ollama:mistral" # Auto-detected via 'ollama:' prefix
      baseUrl: "http://localhost:11434"
      systemPrompt: "Summarize this log entry for any anomalies."
  - type: sink
    name: console
```

### 🌍 Advanced End-to-End Examples
For full end-to-end pipelines that stream data, utilize Flowlets, tumbling windows, and condition engines, check out our catalog!

We provide both Kubernetes CRD and Standalone definitions, implemented in both Java and Python:

**Real-Time Fraud Evaluation Pipeline:**
- **Kubernetes (Java)**: `examples/k8s/java/fraud-detection-flowlets.yaml`
- **Kubernetes (Python)**: `examples/k8s/python/fraud-detection-flowlets-python.yaml`

**IoT Sensor Fleet Analytics:**
- **Kubernetes (Java)**: `examples/k8s/java/iot-fleet-analytics.yaml`
- **Kubernetes (Python)**: `examples/k8s/python/iot-fleet-analytics-python.yaml`
- **Standalone (Camel/Low-Code)**: `examples/standalone/camel/iot-fleet-analytics-camel.yaml`

---


## 📜 Coding Guidelines

1.  **Zero-Trust by Default**: Any logic that handles user-supplied code (Java/Python) MUST be executed through the isolated executors.
2.  **No Direct Flink Hacks**: Avoid using internal Flink APIs that might break between minor versions.
3.  **Documentation First**: Every new feature or connector MUST be accompanied by an example in `/examples`.
