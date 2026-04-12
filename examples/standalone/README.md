# 🚀 Standalone Pipeline Examples

This directory contains standalone **Flinkflow Pipeline** definitions intended for local development, testing, and Docker-based deployments.

## 🏃 How to Run

Unlike the Kubernetes examples, these are plain YAML files that should be run using the `scripts/run-local.sh` helper script:

```bash
./scripts/run-local.sh examples/standalone/[java|python|camel]/[EXAMPLE_NAME].yaml
```

---

## 📦 Examples Catalog

### 🐫 Low-Code & Declarative (Apache Camel)
*Best for: Rapid field extraction, JSON routing, and template-based formatting.*
*(See: [Simple Language](https://camel.apache.org/components/latest/languages/simple-language.html) | [JsonPath](https://camel.apache.org/components/latest/languages/jsonpath-language.html) | [YAML DSL](https://camel.apache.org/manual/camel-yaml-dsl.html))*
- **[`camel/iot-fleet-analytics-camel.yaml`](camel/iot-fleet-analytics-camel.yaml)**: High-scale sensor telemetry using 100% declarative Camel & Groovy.
- **[`camel/camel-yaml-dsl-test.yaml`](camel/camel-yaml-dsl-test.yaml)**: Complex routing logic using the **Camel YAML DSL** (Choice/When/Otherwise).
- **[`camel/fraud-detection-camel.yaml`](camel/fraud-detection-camel.yaml)**: Real-time fraud detection using Camel YAML DSL for risk scoring and alerting.
- **[`camel/camel-flatmap-test.yaml`](camel/camel-flatmap-test.yaml)**: Demonstrates "exploding" JSON arrays into individual records via JsonPath.
- **[`camel/agent-support-triage.yaml`](camel/agent-support-triage.yaml)**: **[NEW]** Gemini-powered support ticket triage using Camel YAML DSL for robust formatting and error handling.

### 🤖 Agentic & AI (Gemini)
*Best for: Intelligent categorization, sentiment analysis, and structured extraction.*
- **[`python/agent-support-triage.yaml`](python/agent-support-triage.yaml)**: **[NEW]** Customer support triage dashboard using Gemini for analysis and Python for high-performance terminal rendering.
- **[`camel/agent-support-triage.yaml`](camel/agent-support-triage.yaml)**: **[NEW]** The same triage pipeline implemented using Camel YAML DSL logic blocks for declarative logic.
- **[`java/agent-test.yaml`](java/agent-test.yaml)**: Tool-calling smoke test using GPT-4o/Gemini and stateful memory.

### 🐍 Procedural (Python)
*Best for: Data science, existing Python logic, and complex JSON manipulation.*
- **[`python/iot-fleet-analytics-python.yaml`](python/iot-fleet-analytics-python.yaml)**: IoT analytics using Polyglot (Python) for flexible logic.
- **[`python/fraud-detection-flowlets-python.yaml`](python/fraud-detection-flowlets-python.yaml)**: Transaction monitoring using Python-based logic within Flowlets.
- **[`python/http-lookup-python-example.yaml`](python/http-lookup-python-example.yaml)**: Async REST API enrichment using Python handlers for URL generation.
- **[`python/python-complex-example.yaml`](python/python-complex-example.yaml)**: Advanced logic with JSON parsing, `datetime`, and array 'exploding'.
- **[`python/python-stateful-example.yaml`](python/python-stateful-example.yaml)**: Demonstrates `keyBy`, `window`, and `reduce` in Python.
- **[`python/python-snippets-example.yaml`](python/python-snippets-example.yaml)**: Basic snippets for `filter`, `process`, and `flatmap`.

### ☕ Procedural (Java)
*Best for: Maximum performance and native Flink DataStream API features.*
- **[`java/hello-world.yaml`](java/hello-world.yaml)**: The simplest possible pipeline - static source to console sink.
- **[`java/simple-transform-example.yaml`](java/simple-transform-example.yaml)**: Basic 1-to-1 processing (Map function).
- **[`java/iot-fleet-analytics.yaml`](java/iot-fleet-analytics.yaml)**: Original IoT analytics demo using Java snippets.
- **[`java/fraud-detection-flowlets.yaml`](java/fraud-detection-flowlets.yaml)**: Real-time fraud monitoring using modular Java Flowlets.
- **[`java/complex-enrichment-example.yaml`](java/complex-enrichment-example.yaml)**: multi-step pipeline with joins, lookups, and nested transforms.
- **[`java/kafka-avro-schema-registry-example.yaml`](java/kafka-avro-schema-registry-example.yaml)**: Avro serialization with automatic schema resolution.
- **[`java/jdbc-sink-example.yaml`](java/jdbc-sink-example.yaml)**: High-throughput batch inserts to PostgreSQL/MySQL.
- **[`java/window-example.yaml`](java/window-example.yaml)**: Time-based windowing (Tumbling, Sliding, Session).

---

## 🛠️ Design Philosophy

- **Local-First Development**: All examples here can be run without a Kubernetes cluster.
- **Instant Hot-Reload**: Modify the Java/Python/Camel snippets and re-run instantly — no JAR recompilation required.
- **Polyglot Safety**: Procedural logic stays in targeted snippets while the pipeline structure stays declarative in YAML.
