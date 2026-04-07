# Flinkflow — Product Roadmap & Design Document

> **Version**: 1.0 (March 2026)  
> **Status**: Living Document — updated as the product evolves  
> **Maintainer**: Talweg Authors

---

## 1. Vision & Mission

**Flinkflow** is a low-code, YAML-driven pipeline framework built on Apache Flink. Its mission is to make stateful, distributed stream processing **accessible to any developer** — not just those with deep Flink expertise — by abstracting the Flink API surface behind a composable, declarative DSL.

> _"Define once in YAML. Run everywhere — local, Docker, Kubernetes."_

Flinkflow draws direct inspiration from **Apache Camel / Camel K** (declarative routing primitives) and **Kamelets** (reusable, parameterised connectors), reimagined natively for the Flink stream processing engine and Kubernetes-first deployment.

---

## 2. Design Principles

| Principle | Description |
|---|---|
| **YAML First** | Every pipeline, connector, and transformation is expressible in YAML with zero Java boilerplate required for the common case |
| **Progressively Escapable** | Inline Java (Janino) and Python (GraalVM) snippets provide an escape hatch for custom logic without leaving the YAML paradigm |
| **Kubernetes Native** | The Flowlet catalog, pipeline definitions, and operational concerns live as Kubernetes CRDs — managed via `kubectl`, GitOps, and standard K8s tooling |
| **Pluggable by Design** | Sources, sinks, and operations are discrete, independently composable units with no hidden coupling |
| **Operator Composable** | Flinkflow pipelines and Flowlets are first-class K8s resources, composable with the Flink Kubernetes Operator, cert-manager, Helm, and ArgoCD |
| **Zero Lock-in** | Flinkflow generates and submits standard Flink `DataStream` DAGs — there is no proprietary runtime, only configuration |

---

## 3. Why Flinkflow? (vs. Native Java Flink)

While the Flink DataStream API is powerful, implementing pipelines in pure Java introduces overhead in the development lifecycle. Flinkflow provides several distinct advantages:

### 3.1. Code Change & Deployment Velocity
- **No Recompilation**: In native Java, even a small change to a filter condition or a Kafka topic requires a full Maven/Gradle build, JAR packaging, and deployment. In Flinkflow, you simply update a YAML file or a Kubernetes Custom Resource.
- **Static Payload, Dynamic Logic**: The project JAR is deployed once. Business logic changes are "Lightweight" YAML updates. This enables CI/CD pipelines to run in seconds rather than minutes.
- **Hot-Reloading Ready**: Because of the polyglot dynamic executor (Janino/GraalVM), Flinkflow is architecturally ready for "Live Update" scenarios where transformation logic can be swapped without stopping the Flink job (roadmap item).

### 3.2. Radical Testability
- **Boilerplate-Free Unit Tests**: Testing native Flink logic usually requires setting up a `StreamExecutionEnvironment` and `TestHarness`. Flinkflow's `static-source` allows developers to define test data in YAML and verify output via `console-sink` without writing test infrastructure code.
- **Declarative Validation**: Since the pipeline is data (YAML), it can be validated using standard tools like `kube-linter` or custom JSON Schema checks before a container even starts.
- **Developer Sandboxing**: The `run-local.sh` script provides a near-instant feedback loop for developers to iterate on logic locally.

---

## 4. Current State (Foundation — Achieved)

The following capabilities constitute the **v0.x foundation** that has been built and validated:

### 4.1 Core Engine

```
FlinkflowApp.java (Main Entrypoint)
 ├── Pipeline YAML Parser
 ├── Flowlet Resolver
 │    ├── Built-in Catalog (JAR resources)
 │    └── Kubernetes CRD Discovery (--enable-k8s-flowlets)
 ├── Pipeline Engine (Source → Operations → Sink)
 ├── Janino Dynamic Java Executor
 ├── GraalVM Polyglot Python Executor
 └── Saxon XSLT 3.0 DataMapper
```

### 4.2 Connector Matrix

| Layer | Connector | Status |
|---|---|---|
| **Source** | kafka-source | ✅ |
| **Source** | confluent-kafka-source (SASL_SSL) | ✅ |
| **Source** | file-source / s3-source | ✅ |
| **Source** | static-source (bounded test data) | ✅ |
| **Source** | datagen (DataGen connector) | ✅ |
| **Sink** | console-sink | ✅ |
| **Sink** | kafka-sink | ✅ |
| **Sink** | confluent-kafka-sink (SASL_SSL) | ✅ |
| **Sink** | file-sink / s3-sink (partitioned) | ✅ |
| **Sink** | http-sink / webhook-sink | ✅ |
| **Sink** | jdbc-sink (PostgreSQL / MySQL / Oracle) | ✅ |

### 4.3 Operations Matrix

| Operation | Description | Status |
|---|---|---|
| `process` / `transform` | 1-to-1 Java/Python snippet | ✅ |
| `filter` | Boolean predicate | ✅ |
| `flatmap` | 1-to-N with Collector | ✅ |
| `keyby` / `groupby` | Stream partitioning | ✅ |
| `reduce` / `aggregate` | Rolling stateful aggregation | ✅ |
| `window` | Tumbling / Sliding / Session | ✅ |
| `sideoutput` | DLQ / branch splitting | ✅ |
| `join` | Interval Join on two streams | ✅ |
| `datamapper` | XSLT 3.0 structural transform | ✅ |
| `http-lookup` | Async REST enrichment | ✅ |
| `python-snippets` | Polyglot Python logic (GraalVM) | ✅ |

### 4.4 Deployment Targets

| Target | Mechanism | Status |
|---|---|---|
| Local | `run-local.sh` / Maven exec | ✅ |
| Docker | `docker run` | ✅ |
| Manual K8s | `kubectl apply` (cluster mode) | ✅ |
| Flink K8s Operator | `FlinkDeployment` CR | ✅ |
| Native K8s | `flink run-application` | ✅ |
| Pipeline CR | K8s-native CR execution | ✅ |

### 4.5 Platform

| Component | Status |
|---|---|
| Flowlet system (reusable parameterised components) | ✅ |
| Flowlet CRD + K8s Discovery | ✅ |
| Built-in Flowlet Catalog | ✅ |
| Monitoring Dashboard (nicegui) | ✅ |
| Flink REST API integration in dashboard | ✅ |
| DAG Visualization (Mermaid.js) | ✅ |
| K8s pod / CR discovery in dashboard | ✅ |
| Metric exposure in pipeline DSL | ✅ |
| Java 17 runtime + Flink 1.18.x | ✅ |

---

## 5. Roadmap

The roadmap is structured as three progressive milestones: **v1.0 (Production Ready)**, **v1.5 (Ecosystem)**, and **v2.0 (Platform)**. Features within each milestone are sequenced by dependency and impact.

---

### Milestone 1 — v1.0: Production Ready

> **Goal**: Harden Flinkflow for real production workloads with enterprise-grade security, observability, and operational safety.

#### 1.1 Secret Management

*Priority: Critical*

Credentials (Kafka API keys, JDBC passwords, JWT tokens) are currently embedded in plain YAML. Production deployment requires native secret resolution.

- [ ] **Kubernetes Secrets Integration**: Resolve property values at runtime from K8s Secrets using the syntax `secret:my-secret/key-field`
- [ ] **HashiCorp Vault Integration**: Vault KV v2 lookups via `vault:secret/path/key`
- [ ] **Environment Variable Substitution**: `${ENV_VAR}` resolved from container environment at startup (basic tier already partially supported via shell)
- [ ] **Secret Auditing**: Log which secrets were resolved (not their values) at job startup for audit trails

**Design note**: Resolution should occur in the Flowlet substitution phase so secrets work transparently inside Flowlet templates without any changes to individual connectors.

---

#### 1.2 Schema Validation & Pre-flight Checks

*Priority: High*

Currently, malformed YAMLs produce cryptic Flink runtime errors. A pre-flight validation layer should catch configuration issues at submission time.

- [ ] **YAML Schema Validation**: JSON Schema for Pipeline and Flowlet CRDs, enforced via `kubectl` admission webhook or the CLI
- [ ] **Parameter Type Checking**: Validate that `with:` values passed to Flowlets match declared parameter types (`string`, `int`, `boolean`)
- [ ] **Required Parameter Enforcement**: Fail fast with a clear error if a required Flowlet parameter is missing
- [ ] **Step Graph Validation**: Detect impossible DAG configurations (e.g., two sources, no sink, orphaned operations)
- [ ] **Dry Run Mode**: `--dry-run` flag that parses, validates, and prints the resolved expanded pipeline without submitting to Flink

---

#### 1.3 Live Log Streaming in Dashboard

*Priority: High*

The monitoring dashboard can discover pods and show job status, but live log tailing from K8s containers is not yet implemented.

- [ ] **Container Log Streaming**: Stream `kubectl logs -f` output via WebSocket to the dashboard UI
- [ ] **Log Filtering**: Filter by log level (INFO, WARN, ERROR) and keyword search within the stream
- [ ] **Multi-container Support**: Allow toggling between JobManager and TaskManager pods within the same view

---

#### 1.4 Schema Registry Support

*Priority: High*

Real production Kafka pipelines rely on Avro or Protobuf schemas managed via a registry.

- [ ] **Confluent Schema Registry**: Deserialize/serialize Avro records using the Confluent Wire Format (magic byte + schema ID)
- [ ] **AWS Glue Schema Registry**: Support AWS Glue for Avro on Kinesis/MSK workloads
- [ ] **DSL Integration**: Schema registry configuration as first-class source/sink properties (`schema.registry.url`, `schema.id`, `schema.subject`)
- [ ] **Schema Evolution**: Support `BACKWARD` and `FORWARD` compatibility strategies

---

#### 1.5 Flowlet Versioning

*Priority: Medium*

As Flowlets evolve, pipelines need the ability to pin to a specific version and migrate gracefully.

- [ ] **Version Field in Flowlet CR Spec**: `spec.version: "1.2.0"` as a semver string
- [ ] **Version-pinned References**: `type: flowlet / name: kafka-source / version: "1.1.0"` in pipeline YAML
- [ ] **Latest Resolution**: Resolve `version: latest` to the highest version installed in the cluster
- [ ] **Upgrade Path**: CLI/script tooling to compare installed vs. available versions and apply upgrades

---

### Milestone 2 — v1.5: Ecosystem Expansion

> **Goal**: Dramatically broaden the connectivity surface and introduce a first-class CLI and GitOps experience.

#### 2.1 Connector Expansion

##### New Sources

| Connector | Use Case | Priority |
|---|---|---|
| **Debezium CDC Source** | Change Data Capture from PostgreSQL, MySQL, MongoDB | High |
| **MongoDB Source** | Change Stream ingestion for document data | High |
| **Pulsar Source** | Apache Pulsar cloud-native messaging | Medium |
| **MQTT Source** | IoT sensor/device event ingestion | Medium |
| **HTTP Polling Source** | REST API polling as a bounded/unbounded source | Medium |
| **Kinesis Source** | AWS Kinesis Data Streams for AWS-native workloads | Medium |
| **RabbitMQ Source** | Traditional AMQP message queuing | Medium |
| **Pub/Sub Source** | GCP Pub/Sub for GCP-native workloads | Low |
| **Event Hubs Source** | Azure Event Hubs for Azure-native workloads | Low |
| **OpenTelemetry Source** | Receiving metrics/traces directly into pipelines | Low |

##### New Sinks

| Connector | Use Case | Priority |
|---|---|---|
| **Snowflake Sink** | Enterprise cloud data warehousing | High |
| **ClickHouse Sink** | High-performance real-time OLAP and analytics | High |
| **Elasticsearch Sink** | Full-text search, log analytics, observability | High |
| **Apache Iceberg Sink** | Modern Lakehouse / open table format for data lakes | High |
| **Pinecone / Milvus Sink** | Vector database ingestion for RAG/AI workloads | High |
| **Delta Lake Sink** | Lakehouse support for Databricks/Spark ecosystems | Medium |
| **Redis Sink** | High-speed cache writes, operational data serving | Medium |
| **Apache Hudi Sink** | Upsert-capable Lakehouse table format | Medium |
| **MongoDB Sink** | Document storage and operational data serving | Medium |
| **Slack / PagerDuty Sink** | Operational alerting and instant notifications | Medium |
| **BigQuery Sink** | Google Cloud analytics data warehouse | Low |
| **Kinesis Sink** | AWS Kinesis Firehose / Data Streams output | Low |
| **Apache Pinot / Druid Sink** | Real-time user-facing analytics | Low |

> [!NOTE]
> **Community-Driven Connectivity**: This list is non-exhaustive. New source and sink connectors are continuously evaluated and added to the Flowlet Catalog based on community demand, industry shifts, and specific user requests.

---

#### 2.2 Catalog Expansion — Standardize All Connectors as Flowlets

Currently, connectors are first-class source/sink types while some are also wrapped as Flowlets. The goal is to wrap **all** connectors in the Flowlet system for a unified UX.

- [ ] Wrap `jdbc-sink` as a parameterised Flowlet (parameters: `url`, `sql`, `username`, `password`)
- [ ] Wrap `http-sink` / `webhook-sink` as Flowlets
- [ ] Wrap `file-sink` / `s3-sink` as Flowlets with S3 configuration parameters
- [ ] Create composite Flowlets (e.g., `kafka-to-postgres`, `kafka-to-iceberg`) bundling source + transform + sink
- [ ] **Flowlet Registry API**: A RESTful catalog service that indexes available Flowlets and their schemas (backing the UI)

---

#### 2.3 `flinkflow` CLI

A dedicated command-line tool for developers to manage the full pipeline lifecycle without raw `kubectl` commands.

```
flinkflow pipeline apply   <yaml>     # Validate, expand, and submit a pipeline
flinkflow pipeline status  <name>     # Show current job status and metrics
flinkflow pipeline logs    <name>     # Tail live logs from job containers
flinkflow pipeline delete  <name>     # Cleanly stop and remove job resources

flinkflow flowlet list                # List all installed Flowlets and versions
flinkflow flowlet inspect <name>      # Show Flowlet spec, parameters, and template
flinkflow flowlet install <yaml>      # Install or upgrade a Flowlet CR
flinkflow flowlet validate <yaml>     # Validate a Flowlet definition without installing

flinkflow catalog search  <keyword>   # Search the built-in catalog
flinkflow dry-run         <yaml>      # Parse + validate + expand without submitting
```

- [ ] Implement CLI using a Java or Python CLI framework
- [ ] Distribute as a single binary (native image via GraalVM) or pip-installable package
- [ ] Shell completion for `bash` and `zsh`

---

#### 2.4 GitOps & CI/CD Integration

- [ ] **Helm Chart**: Package the Flinkflow CRDs, RBAC, and monitoring dashboard as a Helm chart for one-command cluster setup
- [ ] **ArgoCD Application Template**: Sample ArgoCD `Application` manifest for declarative pipeline deployment
- [ ] **GitHub Actions Workflow**: Reference CI pipeline that builds the JAR, Docker image, and applies updated Pipeline CRs on merge
- [ ] **Pipeline Diff Tool**: `flinkflow diff <yaml>` shows the logical difference between the running pipeline and the proposed change

---

#### 2.5 Stateful Processing Enhancements

- [ ] **Checkpointing DSL**: Configure Flink checkpointing behaviour (interval, mode, storage) directly in the pipeline YAML
- [ ] **Savepoint Support**: `flinkflow savepoint <name>` triggers a savepoint; `flinkflow restore <name> --from <path>` resumes from one
- [ ] **State Backend Configuration**: Choose between `hashmap`, `rocksdb` backends in the pipeline spec
- [ ] **Event Time Processing**: Native support for watermark strategies (`bounded_out_of_orderness`, `monotonously_increasing`) in the DSL

---

#### 2.6 Advanced Processing & Execution Models

- [ ] **Unified Batch Processing**: Native support for `execution.runtime-mode: BATCH` in the YAML configuration to process bounded historical datasets with the exact same pipeline code.
- [ ] **SQL Transform Operations**: Introduce a `sql-transform` block as an alternative to Java/Python snippets, allowing developers to manipulate streams using Flink SQL expressions.

---

### Milestone 3 — v2.0: Platform

> **Goal**: Evolve Flinkflow from a framework into a full self-service data streaming platform with a rich UI, multi-tenancy, and an extensible plugin ecosystem.

#### 3.1 Flinkflow Web IDE

A browser-based pipeline authoring and management console — the "Control Plane" for Flinkflow.

**Core Modules:**

| Module | Description |
|---|---|
| **Pipeline Designer** | Drag-and-drop DAG builder that generates YAML (visual-to-YAML and YAML-to-visual bidirectional) |
| **Data Analyst SQL Workbench** | SQL-native interface using Flink Table API for connecting to Kafka, exploring Apicurio/Confluent registries, and materializing continuous queries. Includes **AI SQL Functions** (`ML_PREDICT`), **Auto-Catalog Discovery**, **Live Sampling Previews**, and **Federated Queries** (Kafka-to-JDBC/S3). |
| **Flowlet Store** | Marketplace-style UI for browsing, installing, and publishing Flowlets |
| **Live Monitor** | Real-time job status board — metrics, DAG visualization, log streaming, backpressure heatmap |
| **Pipeline History** | Version history, diff view, rollback capability |
| **Secret Vault UI** | Manage K8s/Vault secrets referenced by pipelines |

**GUI-Based Drag & Drop Experience:**
- **Visual Canvas**: A drag-and-drop designer for building DAGs visually.
- **Bidirectional YAML Sync**: The GUI and YAML are two views of the same state; editing one updates the other instantly.
- **AI-Driven DAG Generation**: Leverage natural language prompts to automatically generate and arrange nodes on the visual canvas. Users describe the desired pipeline, and the IDE renders the resulting DAG visually for immediate refinement.
- **AI-Ready Abstraction**: Because the configuration is structured YAML (not opaque code), it is perfectly suited for AI-driven design sessions and automated reasoning.

**Design Stack:**
- React + TypeScript frontend
- REST + WebSocket backend (Spring Boot or FastAPI)
- Deployed as a K8s `Deployment` in the `flinkflow-system` namespace

---

#### 3.2 Multi-tenancy & RBAC

Support multiple teams sharing a single Flinkflow installation on a shared cluster.

- [ ] **Namespace Isolation**: Pipelines and Flowlets scoped to team namespaces
- [ ] **Role Definitions**: `pipeline-developer`, `pipeline-operator`, `flowlet-author`, `platform-admin` roles
- [ ] **Quota Management**: Per-namespace resource limits (parallelism ceiling, max jobs)
- [ ] **Audit Log**: Immutable log of all pipeline and Flowlet mutations, keyed by user/service account

---

#### 3.3 Plugin SDK

Enable third-party teams to build and publish their own Sources, Sinks, Operations, and Flowlets without forking the core project.

- [ ] **Java SPI Interface**: `FlinkflowSource`, `FlinkflowSink`, `FlinkflowOperation` interfaces published to Maven Central
- [ ] **Plugin Packaging**: A plugin is a JAR dropped into `/opt/flink/plugins/flinkflow/` at Docker build time
- [ ] **Plugin Registry**: Published index of community plugins (akin to the Kafka Connect Hub)
- [ ] **Flowlet Authoring Kit**: Tooling and templates to scaffold, test, and publish a new Flowlet

---

#### 3.4 Observability Platform Integration

- [ ] **Prometheus Metrics Export**: Native Flink metrics → Prometheus endpoint, with pre-built Grafana dashboards for Flinkflow jobs
- [ ] **OpenTelemetry Tracing**: Trace individual records through the pipeline for latency debugging
- [ ] **Alerting Rules**: Preconfigured Prometheus alerting rules for backpressure, checkpoint failures, operator lag
- [ ] **SLA Tracking**: Per-pipeline throughput and latency SLA dashboards with breach alerting

---

#### 3.5 AI-Assisted Pipeline Authoring

Accelerate pipeline development with AI assistance embedded in the authoring experience.

**AI-Native Implementation strategy:**
- **NL-to-Pipeline**: Generate complete Flinkflow YAMLs from natural language prompts. Structured YAML is the perfect intermediate representation for LLMs, avoiding the "hallucination" risks of generating complex Java class structures.
- **Visual DAG Synthesis**: The AI doesn't just generate YAML; it "paints" the pipeline. It proposes a visual arrangement of Flowlets on the Web IDE canvas based on the user's prompt.
- **Smart Polyglot Snippets**: Use AI to generate optimized Java (Janino) or Python code for `process` and `filter` steps. Small functional blocks are easier for AI to optimize and for humans to verify.
- **Validation Assist**: AI-driven "Pre-flight" checks that explain *why* a pipeline might fail (e.g. type mismatch in a join) before it's deployed.
- **Schema Mapping Assist**: Given source and target JSON schemas, suggest XSLT or code-based mapping logic.

---

#### 3.6 Runtime AI & Machine Learning Inference

Bring AI into the data path for real-time analytics and intelligent workflows.

- [ ] **AI Model Inference**: A dedicated `model-inference` operation type to seamlessly evaluate ML models directly within the stream processing pipeline.
- [ ] **Built-in ML Functions**: Native streaming operations for real-time forecasting, anomaly detection, and vector embeddings generation.

## 6. Key Design Decisions & ADRs

### ADR-001: String-typed Stream Data Model

**Decision**: The Flink `DataStream<String>` is the universal wire type throughout the pipeline. All connectors deserialize to String at source and serialize from String at sink.

**Rationale**: Simplicity and universality. Any serialisation format (JSON, CSV, Avro text, XML) can be represented as a String and transformed via Java snippets or XSLT. This avoids the complexity of a generic type system while keeping the DSL simple.

**Trade-off**: Typed schemas, schema evolution, and columnar efficiency require explicit `datamapper` steps or future Schema Registry integration (see v1.5 roadmap).

---

### ADR-002: Polyglot Dynamic Code Execution

**Decision**: Inline Java (Janino) and Python (GraalVM) code snippets in YAML are compiled/interpreted and executed at runtime.

**Rationale**: Provides a full polyglot escape hatch without requiring a compile step or external process. Keeps Flinkflow as a single JAR deployable anywhere Flink runs.

**Trade-off**: Janino does not support all Java features (lambdas have limitations). GraalVM Python requires additional JAR size. Snippets are compiled per-job-start, not cached across restarts.

---

### ADR-003: Flowlets as Kubernetes CRDs

**Decision**: Reusable pipeline components (Flowlets) are defined as `kind: Flowlet` Kubernetes Custom Resources, not as files on a shared filesystem or in a central database.

**Rationale**: K8s CRDs are natively versioned, RBAC-governed, GitOps-compatible, and discoverable via the API server — all properties desirable for a distributed catalog. Built-in Flowlets ship in the JAR as a fallback.

**Trade-off**: Flowlets cannot be used without a Kubernetes cluster in scope. Local development requires minikube/k3s or Rancher Desktop.

---

### ADR-004: Flink 1.18 + Java 17

**Decision**: The target runtime is Apache Flink 1.18.1 on Java 17.

**Rationale**: Java 17 is the current LTS with significant performance improvements (Pattern Matching, ZGC improvements, virtual threads in preview). Flink 1.18 is the first release to ship official Java 17 Docker images. Flink 1.17's external connector versioning (`flink-connector-kafka:3.x.y`) aligns with 1.18.

**Migration note**: `flink-connector-kafka` is pinned to `3.2.0-1.18` (independent release cadence from core Flink).

---

### ADR-005: Monitoring Dashboard as a Separate K8s Namespace

**Decision**: The nicegui monitoring dashboard runs in the `flinkmonitor` namespace, separate from the `flink` namespace where jobs execute.

**Rationale**: Operational separation of concerns. The dashboard is a read-only observability tool; isolating it prevents blast radius cross-contamination. In-cluster RBAC grants it read-only access to the `flink` namespace and the Flink REST API.

---

## 7. Non-Functional Requirements

| Concern | Target |
|---|---|
| **Throughput** | Pipeline overhead ≤ 5% vs. native Flink DAG for equivalent logic |
| **Startup Latency** | Pipeline YAML parsed, Flowlets resolved, and Flink job submitted within 10s of JVM start |
| **Checkpoint Recovery** | Jobs recover from the latest checkpoint within 30s of TaskManager failure |
| **Dashboard Refresh** | Job status and metrics refresh within 5s of change in Flink REST API |
| **Container Size** | Docker image ≤ 1.5GB (base Flink image + application JAR) |
| **Java Compatibility** | Java 17 minimum; Java 21 (next LTS) target for v2.0 |

---

## 8. Release Schedule (Indicative)

```
2026 Q1   Foundation complete (current) ──────────────────── v0.9
2026 Q2   v1.0: Secret mgmt, validation, log streaming ───── v1.0
2026 Q3   v1.5: CDC, Iceberg, Elasticsearch, CLI ─────────── v1.5
2026 Q4   v1.5.x: GitOps, Schema Registry, Helm Chart ─────  v1.5.x
2027 Q1   v2.0: Web IDE, Plugin SDK, Multi-tenancy ─────────  v2.0
2027 Q2   v2.0.x: AI assist, Observability platform ────────  v2.0.x
```

---

## 9. Open Questions

| # | Question | Owner | Status |
|---|---|---|---|
| 1 | Should the `process` code sandbox be restricted (ClassLoader isolation) for multi-tenant deployments? | Platform | Open |
| 2 | What is the right abstraction for typed schemas — stay with `DataStream<String>` or introduce a `DataStream<Map<String,Object>>` tier? | Core | Open |
| 3 | Should the Flowlet SDK be a separate Maven module / repo, or stay embedded in the main flinkflow artifact? | Platform | Open |
| 4 | Is python based dashboard the right long-term choice for the monitoring dashboard, or should we migrate to a React-based UI for v2.0? | Dashboard | Open |
| 5 | Which secret backend should be the v1.0 primary (K8s Secrets vs. Vault)? | Security | Open |

---

### ADR-006: GraalVM Polyglot for Python Snippets

**Decision**: Support for Python snippets is implemented using the GraalVM Polyglot API, specifically the `python-language` and `truffle` runtimes.

**Rationale**: Unlike Jython (which is deprecated and stuck on Python 2.7), GraalVM provides a modern, high-performance Python 3.10+ compatible runtime that executes directly on the JVM. This maintains Flinkflow's "Single JAR" deployment model while providing a first-class experience for Python-centric data scientists.

**Trade-off**: Requires inclusion of GraalVM dependencies in the shaded JAR, increasing artifact size (~80MB increase). Optimal performance requires running the Flinkflow JAR on a GraalVM JDK, although it functions correctly on standard OpenJDK via the polyglot interpreter.

---

## 10. Related Documents

| Document | Path |
|---|---|
| Architecture Diagrams | [System Architecture](/docs/01_ARCHITECTURE) |
| Feature Backlog (granular) | [Project Backlog](/docs/10_TODO) |
| Kubernetes Deployment Guide | [Deploying to Kubernetes](/docs/07_DEPLOY_K8S) |
| Full User Guide | [README.md](../../README.md) |
| Security Policy | [Security Policy](/docs/09_SECURITY) |

---

*Last updated: March 2026*
