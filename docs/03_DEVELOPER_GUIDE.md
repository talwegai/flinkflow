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
Output: `target/flinkflow-0.9.0-BETA.jar`

### Docker Images
The **[Dockerfile](https://github.com/talwegai/flinkflow/blob/main/Dockerfile)** is based on the official Flink image and includes all necessary environment variables for the Python evaluator.

---

## 📜 Coding Guidelines

1.  **Zero-Trust by Default**: Any logic that handles user-supplied code (Java/Python) MUST be executed through the isolated executors.
2.  **No Direct Flink Hacks**: Avoid using internal Flink APIs that might break between minor versions.
3.  **Documentation First**: Every new feature or connector MUST be accompanied by an example in `/examples`.
