# Flinkflow Project Backlog (TODO)

This file tracks the concrete tasks and feature implementations for Flinkflow, organized against the [Product Roadmap](/docs/ROADMAP).

---

## ✅ Foundation (Achieved)
*Core capabilities already implemented and validated.*

- [x] **Core Engine**: YAML parsing, Flowlet resolution, and Flink DAG generation.
- [x] **Polyglot Engine**: In-memory compilation (Janino/Java) and execution (GraalVM/Python) of dynamic logic snippets.
- [x] **Python Snippets**: Support for inline Python syntax (`language: python`) across `process`, `filter`, and `flatmap` steps.
- [x] **Python Logic Parity**: Native Python support for stateful operations including `keyby`, `reduce`, `window`, `sideoutput`, and `join`.
- [x] **Python Sandbox Security**: Hardened GraalVM context with zero-trust I/O and class-loader isolation.
- [x] **Processor Matrix**: Support for `process`, `filter`, `flatMap`, `keyBy`, `reduce`, `window`, `sideOutput`, and `join`.
- [x] **XSLT 3.0 DataMapper**: Saxon-HE integration for complex structural transformations.
- [x] **Flowlet System**: Parameterized reusable components with K8s CRD discovery.
- [x] **Basic Connectors**: Kafka (Apache/Confluent), File/S3, DataGen, JDBC, HTTP Sinks.
- [x] **Monitoring Dashboard**: Initial NiceGUI dashboard with Flink REST API and K8s pod discovery.
- [x] **Deployment Options**: Local script, Docker, and K8s (Manual, Operator, Native, and Pipeline CR).
- [ ] ** Checkpoint 
- [ ] ** Savepoint
---

## 🚀 Milestone 1 — v1.0: Production Ready
*Harden the platform for mission-critical enterprise workloads.*

### 🔐 Secret Management
- [x] **Kubernetes Secrets**: Resolve `secret:name/key` in Flowlet/Pipeline properties.
- [ ] **HashiCorp Vault**: Support `vault:path/key` for KV v2 secrets.
- [ ] **Secret Auditing**: Implementation of job-level audit logs for secret resolution.

### 🛡️ Validation & Safety
- [ ] **YAML Schema Enforcement**: Add JSON Schema validation for CRDs.
- [x] **Parameter Validation**: Type-checking for `with:` values in Flowlets (`int`, `boolean`, `string`).
- [x] **Graph Validation**: Detect disconnected DAGs or missing sinks at submission time.
- [x] **Dry Run Mode**: `--dry-run` flag to print expanded config without execution.

### 📊 Observability
- [ ] **Live Log Streaming**: WebSocket-based container log tailing in the dashboard.
- [ ] **Log Filtering**: Dashboard controls for log levels and keyword highlighting.
- [x] **Schema Registry (Confluent Avro)**: Support for Avro serialization/deserialization with automatic schema fetching.
- [ ] **Schema Registry (Advanced)**: Support for AWS Glue and Protobuf serialization.

### 📦 Lifecycle
- [ ] **Flowlet Versioning**: Support for semver pinning in pipeline definitions.
- [ ] **Upgrade Tooling**: CLI helper to manage Flowlet migrations in the cluster.

---

## 🌐 Milestone 2 — v1.5: Ecosystem Expansion
*Broaden connectivity and developer experience.*

### 🔌 New Connectors (Flowlet Catalog)
- [ ] **Sources**: Debezium (CDC), MongoDB Change Streams, MQTT, Pulsar, HTTP Polling.
- [ ] **Sinks**: Snowflake, ClickHouse, Iceberg, Delta Lake, Pinecone, ElasticSearch.
- [ ] **Universal Wrap**: Re-implement all core connectors as standard templates in the Flowlet catalog.

### 🛠️ Developer Tooling
- [ ] **`flinkflow` CLI**: Native binary (GraalVM) for pipeline/flowlet management.
- [ ] **Diff Tool**: Sub-command to compare running pipelines vs local YAML proposed changes.
- [ ] **Helm Chart**: Official chart for Flinkflow infrastructure (Operator + RBAC + Dashboard).

### ⚙️ Stateful & Advanced Execution
- [ ] **Checkpointing DSL**: Expose interval and mode configuration in YAML.
- [ ] **Savepoint Management**: Native CLI support for triggered savepoints and restores.
- [ ] **SQL Transform**: Allow `type: sql` blocks for Flink SQL expressions.
- [ ] **Unified Batch**: Support `execution.runtime-mode: BATCH` in the spec.

---

## 🎨 Milestone 3 — v2.0: Platform
*Self-service platform with visual design and AI assistance.*

### 🖥️ Flinkflow Web IDE
- [ ] **Visual Pipeline Designer**: Drag-and-drop DAG builder with bidirectional YAML sync.
- [ ] **Data Analyst SQL Workbench**: Table API-driven UI for Kafka topic exploration and Apicurio Registry integration.
    - [ ] **Auto-Catalog Discovery**: Zero-config mapping of topics to Flink tables via Schema Registry sync.
    - [ ] **AI SQL Functions**: Implementing `ML_PREDICT` and `AI_GENERATE` directly within the SQL dialect.
    - [ ] **Live Results Preview**: Interactive SQL execution with sampling/head for immediate feedback.
    - [ ] **Federated SQL Support**: Joins across Kafka, JDBC sources, and S3 datasets in a single query.
    - [ ] **Visual Explain & Lineage**: Rendering SQL DAGs and data flow transparency in the UI.
    - [ ] **SQL-Embedded UDF Editor**: Authoring small code snippets (Java/Python) directly in the SQL tab.
- [ ] **Materialized SQL Queries**: User interface to define, test, and materialize streams using simple SQL commands.
- [ ] **AI-Driven Synthesis**: Generate visual DAGs and Janino (Java) or GraalVM (Python) snippets from natural language prompts.
- [ ] **Flowlet Marketplace**: UI for browsing and installing community Flowlets.

### 👥 Multi-tenancy & Plugins
- [ ] **Namespace Isolation**: RBAC-governed resource scoping for shared clusters.
- [ ] **Plugin SDK**: Java SPI for custom Sources/Sinks without forking the core engine.
- [ ] **Prometheus/Grafana**: Native metrics export with pre-built dashboard templates.

### 🤖 Intelligent Features
- [ ] **Validation Assist**: AI explanations for pipeline configuration errors.
- [ ] **In-stream ML**: Dedicated `model-inference` step for real-time AI processing.


---
*Generated based on Roadmap v1.0 — March 2026*
