---
title: Flinkflow User's Guide
slug: /USER_GUIDE
---

# 📖 Flinkflow User's Guide

Welcome to the **Flinkflow User's Guide**! This document will walk you through the core concepts of the platform and help you build your first declarative stream processing pipeline.

---

## 🚀 1. What is Flinkflow?

Flinkflow is a **Declarative Stream Processing Engine** built on top of Apache Flink. It allows you to define enterprise-grade, stateful data pipelines using a simple **Kubernetes-native YAML DSL**.

### 🌟 Key Value Proposition
- **No Java Required**: You write pipeline logic in YAML, with optional Java (Janino) or Python (GraalVM) snippets for custom logic.
- **GitOps Ready**: Treat your data jobs as version-controlled code.
- **Portability**: Move pipelines between Local, Docker, and Kubernetes with zero code changes.

---

## 🏗️ 2. Core Concepts

To master Flinkflow, you need to understand its three primary building blocks:

### 🔄 Pipelines
A **Pipeline** is a directed acyclic graph (DAG) of processing steps. It defines where your data comes from, how it's transformed, and where it's stored.

### 🧪 Steps
Each step in a pipeline is a discrete unit of work. Steps are categorized into:
- **Source**: Ingests data (e.g., Kafka, S3, Static content).
- **Process**: Transforms data (e.g., Filtering, Mapping, Aggregating).
- **Sink**: Exports data (e.g., Kafka, JDBC, Console, Webhooks).

### 📦 Flowlets
**Flowlets** are reusable, parameterized pipeline components. Think of them as "templates" for steps. Instead of re-configuring a complex Kafka connector every time, you define it once as a Flowlet and reuse it across multiple pipelines with simple arguments.

---

## ⚡ 3. Writing Your First Pipeline

A Flinkflow pipeline is just a YAML file. Here is the canonical "Hello World" example (`hello-world.yaml`):

```yaml
name: "Hello World Pipeline"
parallelism: 1
steps:
  - type: source
    name: static-source
    properties:
      content: "Flinkflow,is,awesome"

  - type: process
    name: upper-case-transform
    code: |
      return input.toUpperCase();

  - type: sink
    name: console-sink
    properties:
      type: console
```

### 🏃 Running it locally:
If you have the Flinkflow JAR, you can run this instantly:
```bash
./run-local.sh hello-world.yaml
```

---

## 🐍 4. Using Polyglot Business Logic

One of Flinkflow's most powerful features is the ability to inject custom logic directly into the YAML.

### **Java Snippets (Janino)**
High-performance, JVM-native compilation for filters and maps.
```yaml
- type: filter
  code: "return input.length() > 10;"
```

### **Python Snippets (GraalVM)**
Secure, sandboxed Python for data scientists and analysts.
```yaml
- type: process
  language: python
  code: |
    import json
    data = json.loads(input)
    data['processed'] = True
    return json.dumps(data)
```

---

## ☸️ 5. Moving to Kubernetes

Flinkflow is designed for the modern cloud stack. Once your local YAML is ready, you can deploy it as a **Pipeline CRD**:

```yaml
apiVersion: flinkflow.io/v1alpha1
kind: Pipeline
metadata:
  name: my-cloud-job
spec:
  steps:
    - type: flowlet
      name: kafka-source
      with:
        topic: "raw-events"
    - type: sink
      name: console-sink
```

---

## 📚 6. Next Steps
- **[Configuration Reference](/docs/GUIDE_CONFIGURATION)**: Explore the full list of connectors and properties.
- **[System Architecture](/docs/ARCHITECTURE)**: Learn how the engine works under the hood.
- **[Roadmap](/docs/ROADMAP)**: See what's coming next (AI features and the v2.0 platform).

---

*Need help? Connect with the community on [Slack](https://slack.com).*

---

## 🔌 7. Sources & Sinks Reference

### Sources

| Connector | Key Properties | Description |
| :--- | :--- | :--- |
| `kafka-source` | `bootstrap.servers`, `topic`, `group.id`, `format` | Reads from Apache Kafka or Confluent Cloud. |
| `confluent-kafka-source` | `bootstrapServers`, `topic`, `apiKey`, `apiSecret` | Pre-built Flowlet for Confluent SASL_SSL consumers. |
| `file-source` | `path`, `monitorInterval` | Reads local or S3 files; set `monitorInterval` for continuous streaming. |
| `s3-source` | `bucket`, `prefix`, `region` | Direct Amazon S3 read (requires AWS credentials). |
| `static-source` | `content` | Pipe-separated bounded test data. Great for local development. |
| `datagen` | (auto configured) | Random data generator using the Flink Datagen connector. |

### Sinks

| Connector | Key Properties | Description |
| :--- | :--- | :--- |
| `console-sink` | (none) | Prints records to stdout. |
| `kafka-sink` | `bootstrap.servers`, `topic`, `format` | Writes to a Kafka topic. |
| `confluent-kafka-sink` | `bootstrapServers`, `topic`, `apiKey`, `apiSecret` | Confluent Cloud producer via Flowlet. |
| `file-sink` / `s3-sink` | `path`, `rolloverInterval`, `maxPartSize` | Partitioned writes to disk or S3. |
| `http-sink` | `urlCode`, `method`, `authCode` | Push to webhooks or external REST APIs. |
| `jdbc-sink` | `url`, `sql`, `batchSize` | High-throughput inserts into PostgreSQL, MySQL, or Oracle. |

---

## 🧩 8. Working with Flowlets

**Flowlets** are the reusable "building blocks" of Flinkflow — similar to how Apache Camel uses Kamelets. Once installed into your cluster, they can be referenced by name in any pipeline, with parameters supplied via the `with:` block.

> [!TIP]
> Flowlets dramatically reduce repetition across pipelines. You define a complex Confluent Kafka connector **once**, and all teams reference it with just `type: flowlet` and their specific `topic`.

### Installing the Default Catalog

```bash
# Install Flowlet and Pipeline CRDs, then populate the cluster with built-in Flowlets
./deploy/k8s/install-flowlets.sh [NAMESPACE]
```

### Using a Flowlet in Your Pipeline

```yaml
name: "Production Kafka Pipeline"
steps:
  - type: flowlet
    name: confluent-kafka-source
    with:
      bootstrapServers: "pkc-xxx.confluent.cloud:9092"
      topic: "raw-orders"
      apiKey: "secret:confluent-creds/key"
      apiSecret: "secret:confluent-creds/secret"

  - type: filter
    code: "return input.contains(\"COMPLETED\");"

  - type: flowlet
    name: kafka-sink
    with:
      bootstrapServers: "my-kafka:9092"
      topic: "cleaned-orders"
```

### Built-in Flowlet Catalog

| Flowlet Name | Type | Purpose |
| :--- | :--- | :--- |
| `kafka-source` | Source | Apache Kafka consumer |
| `kafka-sink` | Sink | Apache Kafka producer |
| `confluent-kafka-source` | Source | Confluent Cloud consumer (SASL_SSL) |
| `confluent-kafka-sink` | Sink | Confluent Cloud producer (SASL_SSL) |
| `http-enrich` | Operation | Async REST API enrichment |
| `log-transform` | Operation | Log records to stdout with a configurable prefix |

---

## 🔐 9. Secret Management

Never hardcode credentials. Flinkflow integrates directly with Kubernetes Secrets and environment variables.

### Kubernetes Secrets (Recommended for Production)

```yaml
# 1. Create the Kubernetes Secret
kubectl create secret generic kafka-creds \
  --from-literal=api-key=mykey \
  --from-literal=api-secret=mysecret

# 2. Reference it in your pipeline using secret:<name>/<key>
steps:
  - type: flowlet
    name: confluent-kafka-source
    with:
      apiKey: "secret:kafka-creds/api-key"
      apiSecret: "secret:kafka-creds/api-secret"
```

### Environment Variables

```yaml
properties:
  bootstrap.servers: "${KAFKA_BOOTSTRAP_SERVERS}"
```

| Strategy | Syntax | Use Case |
| :--- | :--- | :--- |
| **Kubernetes Secret** | `secret:name/key` | Production (recommended) |
| **Env Variable** | `${VAR_NAME}` | CI/CD pipelines, staging |
| **Plain text** | `literal-value` | Local development only |

---

## 📊 10. Monitoring Your Pipelines

The **NiceGUI-based monitoring dashboard** provides real-time visibility into your Flink jobs running in Kubernetes.

### What You Can Monitor
- ✅ Job health status (Running, Failed, Finished)
- ✅ Record throughput per step
- ✅ Backpressure and checkpoint health
- ✅ TaskManager and JobManager resource usage

### Deploy the Dashboard

```bash
kubectl apply -f deploy/k8s/monitor-deployment.yaml
```

Access via NodePort, or use port-forwarding:

```bash
kubectl port-forward svc/flinkflow-dashboard 8081:8081 -n flinkmonitor
```

### Inline Custom Metrics

You can emit custom metrics directly from your code snippets using the built-in `metrics` object:

```yaml
- type: process
  code: |
    metrics.counter("records_processed").inc();
    return input.toUpperCase();
```

---

## 🔍 11. Troubleshooting

### Validate Before Running: Dry-Run Mode

The fastest way to check your YAML is correct — without starting Flink — is the `--dry-run` flag:

```bash
./run-local.sh my-pipeline.yaml --dry-run
```

This will:
1. Parse and validate the YAML structure
2. Resolve all Flowlet references into concrete steps
3. Print the fully expanded pipeline graph — without executing it

### Common Issues

| Symptom | Likely Cause | Fix |
| :--- | :--- | :--- |
| `ClassNotFoundException` in code snippet | Missing import in Janino | Add explicit Java `import` statements at top of `code:` block |
| `SecretResolutionException` | Secret not found in K8s | Run `kubectl get secret <name>` to confirm it exists |
| `FlowletNotFound` error | Flowlet CRDs not installed yet | Run `./deploy/k8s/install-flowlets.sh` |
| Pipeline exits immediately | Bounded source finished | Replace `static-source` with `kafka-source` for unbounded streaming |
| High latency on Python steps | GraalVM Context initialization | Expected on startup; use `--dry-run` to pre-warm in development |

### Log Filtering

All Flinkflow errors are prefixed with `[FLINKFLOW-ERROR]`, making them easy to filter in any logging stack:

```bash
# Tail logs from a running Flink JobManager
kubectl logs -f flinkflow-app-jm-0 | grep FLINKFLOW-ERROR
```

---

## 📚 12. Next Steps & Further Reading

| Guide | Description |
| :--- | :--- |
| **[Configuration Reference](/docs/GUIDE_CONFIGURATION)** | Full DSL spec for all connectors and operations |
| **[Operations & Monitoring](/docs/GUIDE_OPERATIONS)** | Performance tuning and advanced dashboard setup |
| **[XSLT DataMapper Guide](/docs/GUIDE_DATAMAPPER)** | Complex JSON/XML transformations using Saxon-HE |
| **[Kubernetes Deployment Guide](/docs/DEPLOY_K8S)** | Step-by-step K8s operator deployment |
| **[System Architecture](/docs/ARCHITECTURE)** | How the engine works under the hood |
| **[Project Roadmap](/docs/ROADMAP)** | Planned features for upcoming releases |

---

*Questions? Feature requests? Connect with the community on [Slack](https://slack.com) or [open an issue](https://github.com/talwegai/flinkflow/issues).*
