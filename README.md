# Flinkflow

**Flinkflow** is a declarative, low-code data streaming platform built on top of Apache Flink. Inspired by Apache Camel K, it democratizes stateful stream processing by abstracting the complexities of the Flink API into a simple, Kubernetes-native YAML DSL.

---
**[🌐 Documentation](https://talweg.ai)** | **[🚀 Get Started](https://talweg.ai/docs/)** | **[🏗️ Architecture](https://talweg.ai/docs/01_ARCHITECTURE)**

---

## 🚀 The Philosophy: Democratizing Data Engineering

Traditionally, building real-time data pipelines is a specialized engineering endeavor, requiring deep Java/Scala expertise and resulting in siloed data teams. Flinkflow breaks down this "Flink Complexity Gap" by shifting the focus from **infrastructure plumbing** to **data logic**.

Our mission is to be the **"Glue Layer"** for real-time event-driven architectures—empowering Data Analysts, DevOps, and Backend Developers to build, deploy, and scale enterprise-grade streaming workloads without ever touching a Maven assembly.

### Why Flinkflow?

| Feature | Native Java Flink | Flinkflow |
| :--- | :--- | :--- |
| **Authoring** | Heavy Java/Maven Boilerplate | Declarative YAML DSL |
| **Development Cycle** | Compile → Package → Deploy JAR | Instant Hot-Reload (YAML/Java/Python/Camel Snippets) |
| **Logic Changes** | ~10 minute CI/CD cycles | Seconds (Apply K8s CRD or YAML) |
| **Polyglot Runtimes** | Java Only | **Java** (Janino), **Python** (GraalVM), **Apache Camel** (Simple/JsonPath/YAML DSL) |
| **Target Persona** | Specialized Flink Engineers | Data Scientists, Analysts, DevOps, Integration Devs |
| **Component Model** | Custom Code / Classes | Reusable, Parameterized **Flowlets** |

---

## 👥 Designed for Every Data Persona

Flinkflow bridges the gap between high-performance data engineering and the broader developer ecosystem, empowering a diverse set of stakeholders:

- **🐍 Data Scientists & Analysts**: Port existing Python logic, complex JSON parsing, and feature-engineering snippets directly into production using the secure **GraalVM Python** runtime.
- **🐫 Low-Code & Integration Developers**: Build entire pipelines using **Apache Camel DSL**, **JsonPath**, and **Simple** expressions. Ideal for declarative transformations, filters, and field extractions without writing procedural code.
  - *Ref: [Apache Camel](https://camel.apache.org/), [Simple Language](https://camel.apache.org/components/latest/languages/simple-language.html), [YAML DSL](https://camel.apache.org/manual/camel-yaml-dsl.html)*
- **☸️ DevOps & Platform Engineers**: Manage high-throughput streaming as native Kubernetes **Pipeline CRDs**. No specialized JAR deployments or Maven assemblies—just pure GitOps via YAML.
- **💻 Backend & Fullstack Developers**: Rapidly build stateful filters, enrichments, and multi-stream joins using a declarative DSL instead of mastering the Flink DataStream API.
- **🏢 Enterprise Platforms**: Securely democratize streaming across teams. The **Zero-Trust Polyglot Sandbox** ensures that guest code (Java/Python) remains fully isolated and safe.
- **🤖 GenAI & Agentic AI**: Flinkflow is a **Declarative Agentic Platform**. Beyond simple "Chat-to-Pipeline" generation, it now supports native autonomous agents. These agents can reason over data streams, maintain stateful memory, and call your Flowlets as tools to take real-world actions.

---

## ✨ Features

- **Declarative YAML DSL**: Define entire pipeline structures—Sources, Sinks, and Operations—in clean YAML.
- **Polyglot Logic Snippets**: Inject custom logic directly into your YAML—support for **Java (Janino)**, **Python (GraalVM)**, and **Apache Camel (Simple/JsonPath/Groovy)** for transformations, filters, and flatmaps.
- **Kubernetes-Native (GitOps)**: Manage pipelines as `Pipeline` and `Flowlet` Custom Resources. Fully compatible with Helm, ArgoCD, and the Flink Kubernetes Operator. See [docs/07_DEPLOY_K8S.md](docs/07_DEPLOY_K8S.md).
- **Reusable Flowlet Catalog**: Drag-and-drop capability for complex connectors (Kafka, Confluent, S3, JDBC) using parameterized components.
- **Advanced Data Mapping**: Support for XSLT 3.0 via Saxon-HE for structural JSON/XML transformations (Kaoto integration). See [docs/06_GUIDE_DATAMAPPER.md](docs/06_GUIDE_DATAMAPPER.md).
- **Observability Built-in**: Real-time monitoring of job health and throughput via a dedicated dashboard.
- **Extensible Connectors**: Unified support for Kafka, S3, JDBC, HTTP Sinks, and more.
- **Agentic Bridge (New)**: Run autonomous AI agents directly in your stream. Support for **OpenAI** (GPT-4o), **Google Gemini**, and **Ollama** (Llama 3, Mistral, Phi-3). Includes stateful conversation history and Flowlet-as-a-Tool execution.
- **Apache Camel Integration**: Use Camel **Simple**, **JSONPath**, and **YAML DSL** for expressive, low-code transformations — no Java required.
- **Enterprise Security**: Native support for Kubernetes Secrets (`secret:name/key`) to secure credentials without hardcoding.
- **Schema Management**: First-class integration with Confluent/Apicurio Schema Registry for Avro-encoded streams with automatic schema fetching.


---

## 📖 Documentation Roadmap

To explore Flinkflow in detail, refer to the specialized documentation for each component:

*   **[Kubernetes Deployment Guide](docs/07_DEPLOY_K8S.md)**: authoritive guide for running Flinkflow via the Flink Kubernetes Operator.
*   **[Pipeline Configuration Reference](docs/04_GUIDE_CONFIGURATION.md)**: Comprehensive guide for the YAML DSL, connectors, and secret management.
*   **[Operations & Monitoring](docs/05_GUIDE_OPERATIONS.md)**: Details on performance, dashboard setup, and troubleshooting.
*   **[Infrastructure Catalog (deploy/k8s/)](deploy/k8s/README.md)**: Reference for manifests, RBAC, and system deployments.
*   **[Flowlet Registry (deploy/k8s/flowlets/)](deploy/k8s/flowlets/README.md)**: Library of reusable, parameterized pipeline components.
*   **[XSLT DataMapper Guide](docs/06_GUIDE_DATAMAPPER.md)**: Deep dive into using Saxon-HE for structural mapping.
*   **[System Architecture](docs/01_ARCHITECTURE.md)**: Detailed diagrams and component descriptions.
*   **[ADR-005: Agentic Bridge](adr/005_AGENTIC_BRIDGE.md)**: Conceptual design for autonomous AI agents on Flink.
*   **[Project Roadmap](docs/08_VISION.md)**: Future milestones and planned features.


---

## 🗺️ Visual Overview

Flinkflow bridges the gap between declarative configuration and high-performance execution.

```mermaid
graph LR
    subgraph "Control Plane (User)"
        YAML["Pipeline YAML / K8s CRD"]
        FLOW["Flowlet Catalog"]
    end

    subgraph "Flinkflow Engine"
        PARSER["YAML Parser & Flowlet Resolver"]
        JANINO["Polyglot Engine (Java/Python)"]
        CAMEL["Camel Engine (Simple/JSONPath/YAML DSL)"]
        AGENT["Agentic Bridge (OpenAI/Gemini/Ollama)"]
        DAG["Flink Stream Graph"]
    end

    subgraph "Infrastructure"
        K8S["Kubernetes (Job/Operator)"]
        FLINK["Apache Flink Cluster"]
    end

    subgraph "Data Sources / Sinks"
        SRC["Kafka / S3 / HTTP"]
        SNK["Kafka / JDBC / Webhooks"]
    end

    YAML --> PARSER
    FLOW --> PARSER
    PARSER --> JANINO
    PARSER --> CAMEL
    PARSER --> AGENT
    JANINO --> DAG
    CAMEL --> DAG
    AGENT --> DAG
    K8S --> FLINK
    DAG -.-> FLINK
    SRC --> FLINK
    FLINK --> SNK
```

---

## 🤖 Democratizing Data with GenAI

Flinkflow's YAML-first approach is specifically designed to be **LLM-optimized**. While traditional Flink Java code is verbose and prone to hallucination errors in logic flow, Flinkflow's declarative DSL provides a constrained, structured schema that GenAI models can generate with high precision.

*   **Chat-to-Pipeline**: Build complex real-time filters, enrichments, and aggregations using natural language.
*   **Predictable Output**: The YAML schema ensures that generated pipelines are syntactically valid and architecturally consistent.
*   **Encapsulated Logic**: Janino (Java), GraalVM (Python), and Apache Camel (Simple/JSONPath/YAML DSL) snippets allow for precise "injection" of custom business logic without breaking the high-level pipeline structure.

---

---

## ⚡ Performance: Polyglot-AOT Architecture
Flinkflow achieves native-level performance through its **Janino-powered** (Java), **GraalVM-powered** (Python), and **Camel-powered** (Simple/JSONPath/YAML DSL) code injection system. All logic snippets in your YAML are compiled/optimized **exactly once** during job startup, resulting in zero overhead during high-throughput record processing.
> See **[Operations & Performance (docs/05_GUIDE_OPERATIONS.md)](docs/05_GUIDE_OPERATIONS.md)** for details.

---

## 📂 Project Structure

To explore the Flinkflow codebase and directory layout, see the **[Developer Guide (docs/03_DEVELOPER_GUIDE.md)](docs/03_DEVELOPER_GUIDE.md)**.


## Getting Started

### Prerequisites

- Java 17+
- Maven 3+
- Docker (optional, for containerization)
- Kubernetes (optional, for deployment)

### Build the Project

```bash
mvn clean package
```

This will produce a shaded JAR in `target/flinkflow-0.9.0-BETA.jar`.

### 🧪 Smoke Testing

To ensure all examples and core components are functional, you can run the smoke test suite. This suite performs a `--dry-run` validation on all standalone YAML examples.

**Via Shell Script (Full CLI validation):**
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

Or manually using Maven with the `local-run` profile:

```bash
mvn exec:java -P local-run -Dexec.mainClass="ai.talweg.flinkflow.FlinkflowApp" \
    -Dexec.args="examples/standalone/java/simple-transform-example.yaml"
```

### 🛠️ Advanced CLI Arguments

Flinkflow supports several arguments to aid local development and validation:

| Flag | Description |
| :--- | :--- |
| `--dry-run` | Validates the YAML structure and expands all Flowlets into a final pipeline, printing the result without executing the Flink job. |
| `--flowlet-dir <path>` | Specifies a local directory to search for Flowlet definitions. This allows you to test Flowlets locally without a Kubernetes cluster. |

**Example (Dry-run with local flowlets):**
```bash
mvn exec:java -P local-run -Dexec.mainClass="ai.talweg.flinkflow.FlinkflowApp" \
    -Dexec.args="examples/standalone/java/complex-enrichment-example.yaml --dry-run --flowlet-dir deploy/k8s/flowlets"
```

### Docker Deployment

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

## ☸️ Kubernetes Deployment

For enterprise-grade deployments, Flinkflow is designed to be **Kubernetes-native**.



### 🏆 Recommended: Kubernetes-Native Pipeline (CRD Based)

For the best developer experience, Flinkflow allows you to define your entire job configuration as a **Pipeline** custom resource. This enables GitOps-driven streaming without managing local YAML files or ConfigMaps.

> [!IMPORTANT]
> **Prerequisites for CRD-Based Mode**:
> 1.  **Install the CRDs**: (`deploy/k8s/crds/crd-pipeline.yaml`, `deploy/k8s/crds/crd-flowlet.yaml`) 
> 2.  **Configure RBAC**: (`deploy/k8s/rbac.yaml`) This grants the Flink pods permission to query the Kubernetes API for your pipeline definitions at startup.
> 
> *Note: The Flink Kubernetes Operator is **not** a prerequisite for using the Pipeline CRD, though it is recommended for production lifecycle management.*

1.  **Install System Resources**:
    ```bash
    # Install CRDs and configure cluster permissions
    kubectl apply -f deploy/k8s/crds/crd-pipeline.yaml
    kubectl apply -f deploy/k8s/crds/crd-flowlet.yaml
    kubectl apply -f deploy/k8s/rbac.yaml
    ```

2.  **Define your Pipeline**:
    ```yaml
    apiVersion: flinkflow.io/v1alpha1
    kind: Pipeline
    metadata:
      name: my-stream-job
    spec:
      parallelism: 2
      steps:
        - type: source
          name: static-source
          properties:
            content: "cluster-data-1|cluster-data-2"
        - type: flowlet
          name: log-transform
          with:
            prefix: "[K8S-NATIVE]"
        - type: sink
          name: console-sink
    ```
    Apply it with `kubectl apply -f my-pipeline.yaml` (see **`examples/k8s/java/k8s-native-pipeline-resource.yaml`** for a full template).

3.  **Run the Native Deployment**:
    Use the provided manifest to start a Flink cluster that fetches this CR:
    ```bash
    kubectl apply -f deploy/k8s/native-pipeline-deployment.yaml
    ```
    (Note: Edit `deploy/k8s/native-pipeline-deployment.yaml` to point at your pipeline's name).

> **Pro Tip (Rancher Desktop / k3s)**: If you are testing locally and get `ImagePullBackOff`, build your image directly into the k8s namespace:
> `nerdctl --namespace k8s.io build -t flinkflow:latest .`

Flowlets used within a `Pipeline` CR are automatically discovered from the same cluster namespace.
   Alternatively, run the raw command:
   ```bash
   ./bin/flink run-application \
       --target kubernetes-application \
       -Dkubernetes.cluster-id=flinkflow-native-cluster \
       -Dkubernetes.container.image=flinkflow:latest \
       -Dkubernetes.service-account=flink-service-account \
       -Dkubernetes.rest-service.exposed.type=NodePort \
       -Djobmanager.memory.process.size=1600m \
       -Dtaskmanager.memory.process.size=1728m \
       -Dtaskmanager.numberOfTaskSlots=2 \
       local:///opt/flink/usrlib/flinkflow.jar \
       --job-args /opt/flink/conf/pipeline.yaml
   ```

3. **Delete the Cluster**:
   ```bash
   kubectl delete deployment flinkflow-native-cluster
   ```

### 📦 Alternative Kubernetes Methods

For traditional deployments or manual infrastructure control:
- **Flink Kubernetes Operator**: Standard `FlinkDeployment` manifests.
- **Manual Cluster Mode**: Direct JobManager/TaskManager Pod pool.
- **Native Submission**: Direct `flink run-application` via the K8s API.

> Detailed guides for these methods are available in the **[Kubernetes Deployment Guide (docs/07_DEPLOY_K8S.md)](docs/07_DEPLOY_K8S.md)**.


---

## 🛠️ Configuration & Secret Management

Flinkflow is configured via a high-level YAML DSL. You can define sources, sinks, and complex processing logic without writing a single line of Flink Java boilerplate.

> [!IMPORTANT]
> For the full specification of all connectors (Kafka, S3, JDBC, HTTP), operations (Windowing, Joins, Aggregations), and Secret Management (`secret:name/key`), refer to the **[Pipeline Configuration Reference (docs/04_GUIDE_CONFIGURATION.md)](docs/04_GUIDE_CONFIGURATION.md)**.

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

## 🏗️ Reusable Components: Flowlets

Flowlets are parameterized, shareable pipeline components. This allows you to define complex patterns (like "Confluent Kafka to S3") once and reuse them across dozens of pipelines by just changing parameters in the `with:` block.

> See **[Flowlet Catalog Index (deploy/k8s/flowlets/README.md)](deploy/k8s/flowlets/README.md)** and the **[Configuration Guide](docs/04_GUIDE_CONFIGURATION.md#flowlets)**.

---

## 📊 Monitoring

The **NiceGUI-based dashboard** provides real-time visibility into your Flink metrics and Kubernetes logs.

> See **[Operations & Monitoring (docs/05_GUIDE_OPERATIONS.md)](docs/05_GUIDE_OPERATIONS.md)**.

---

## 💡 Examples Catalog

*   **[Standalone Pipelines](examples/standalone/README.md)**: Explore joins, windowing, JDBC, and more.
*   **[Kubernetes CRDs](examples/k8s/README.md)**: Ready-to-apply `Pipeline` resources.

---

## 🔒 Security: Polyglot Sandboxing

Flinkflow implements a strict, **deny-by-default** security model for guest code execution. It uses a hardened **GraalVM Python sandbox** and restricted **Janino Java** runtime to protect the Flink cluster from potential exploits within user-supplied YAML logic.

> [!TIP]
> This security architecture makes Flinkflow uniquely suited for **LLM-generated pipelines** and **Multi-Tenant environments**. Detailed technical specs on the sandbox isolation can be found in our **[Security Policy (docs/09_SECURITY.md)](docs/09_SECURITY.md)**.

> [!IMPORTANT]
> This security architecture makes Flinkflow uniquely suited for **LLM-generated pipelines** and **Multi-Tenant environments**, where safety and isolation are paramount.

## 🏢 Talweg Enterprise & Services

Flinkflow is maintained by **[Talweg](https://talweg.ai)**. While the core engine is free and open-source, we offer professional services and an enterprise-grade distribution for organization-wide streaming platforms.

| Feature | Community Edition | Talweg Enterprise |
| :--- | :---: | :---: |
| **Core DSL & Polyglot Runtimes** | ✅ | ✅ |
| **Agentic Bridge (LLM Integration)** | ✅ | ✅ |
| **Kafka, S3, JDBC Connectors** | ✅ | ✅ |
| **Flinkflow Web IDE (Visual DAG Builder)** | ❌ | ✅ |
| **In-stream ML & AI Validation Assist** | ❌ | ✅ |
| **Live Hot-Reloading** | ❌ | ✅ |
| **Enterprise RBAC & SSO** | ❌ | ✅ |
| **Air-Gapped Container Gallery** | ❌ | ✅ |
| **Advanced Observability (SLA/Audit)** | ❌ | ✅ |
| **Proprietary High-Perf Connectors** | ❌ | ✅ |
| **24/7 Production Support** | ❌ | ✅ |

### 🛠️ Professional Services
Talweg provides expert consulting and managed services to accelerate your streaming journey:
- **Pipeline Architecture Reviews**: Expert validation of your DAGs, stateful logic, and secret management.
- **End-to-End Pipeline Development**: Full-lifecycle implementation support, from data source ingestion to final sink delivery.
- **Custom Flowlet Development**: Building proprietary, secure connectors for your internal systems (SAP, AS/400, etc.).
- **Agentic AI & ML Strategy**: Real-time inference optimization and agentic workflow design.
- **Managed Hosting**: Fully managed, auto-scaling container based Flinkflow clusters on AWS, Azure, GCP, or On-Premise.

**Need help or looking for enterprise features?** [Contact our team](mailto:contact@talweg.ai) or visit **[talweg.ai](https://talweg.ai)**.

---

## 📄 License

Flinkflow is licensed under the **Apache License, Version 2.0**. See the [LICENSE](LICENSE) file for the full license text.

---

## 🤝 Community & Contributing

Flinkflow is an open-source project and we welcome contributions of all kinds! Whether you are fixing a bug, improving the docs, or suggesting a new feature, your help is appreciated.

*   **[Contributing Guide](docs/community/CONTRIBUTING.md)**: For finding bugs and submitting features.
*   **[Developer Guide](docs/03_DEVELOPER_GUIDE.md)**: For deep-dive engine development and internals.
*   **[Code of Conduct](docs/community/CODE_OF_CONDUCT.md)**: Our standards for a welcoming community.
*   **[Security Policy](docs/09_SECURITY.md)**: How to report vulnerabilities and our support model.
*   **[Report an Issue](https://github.com/talwegai/flinkflow/issues)**: Help us make Flinkflow better by reporting bugs.

---

*Democratizing stateful stream processing for the modern data stack.*
