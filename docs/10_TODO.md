# Flinkflow Project Backlog (TODO)

This file is the **Single Source of Truth** for the Flinkflow project roadmap and task list. It tracks concrete implementations organized by Milestone.

---

## ✅ Foundation (Achieved)
*Core capabilities already implemented and validated.*

### ⚙️ Core Engine
- [x] **YAML Multi-Runtime**: Parsing, Flowlet resolution, and Flink DAG generation.
- [x] **Polyglot Runtime**: In-memory compilation (Janino/Java) and execution (GraalVM/Python/Camel) of dynamic snippets.
- [x] **Processor Matrix**: Support for `process`, `filter`, `flatMap`, `keyBy`, `reduce`, `window`, `sideOutput`, and `join`.
- [x] **Flowlet System**: Parameterized reusable components with K8s CRD discovery.
- [x] **Basic Connectors**: Kafka (Apache/Confluent), File/S3, DataGen, JDBC, HTTP Sinks.
- [x] **Deployment Options**: Local script, Docker, and K8s (Manual, Operator, Native, and Pipeline CR).
- [x] **Flink 2.2 Migration**: Full support for Flink 2.2.0 and the new `OpenContext` lifecycle.
- [x] **Flink State V2**: Asynchronous, non-blocking managed state for AI Agents.

### 🐫 Low-Code & Polyglot
- [x] **Python Snippets**: Support for inline Python syntax across core steps (GraalVM).
- [x] **Camel Expressions**: Integration of Camel Simple and JsonPath for declarative logic.
- [x] **Camel YAML DSL**: Support for complex route fragments and EIPs (Choice, Splitter, etc.) within processors.
- [x] **Groovy Integration**: High-performance JVM scripting for complex object math.
- [x] **XSLT 3.0 DataMapper**: Saxon-HE integration for structural transformations.

### 🛡️ Validation & Observability
- [x] **YAML Schema Enforcement**: OpenAPI v3 validation for Pipeline and Flowlet CRDs.
- [x] **Graph Validation**: Detect disconnected DAGs or missing sinks at submission time.
- [x] **Advanced Metrics**: Native Prometheus/Grafana export with pre-built templates.
- [x] **Test Coverage**: Comprehensive unit/lifecycle tests for all polyglot runtimes (Java, Python, Camel). Line coverage ≥ 80%.

---

## 🚀 Milestone 1 — v1.0: Stable Release
*Refine the developer and operator experience for standard deployments.*

### 🔐 Safety & Secrets
- [x] **Kubernetes Secrets**: Resolve `secret:name/key` in Flowlet/Pipeline properties.
- [ ] **Validation Feedback**: Improved error messages when YAML/Parameter validation fails.

> [!NOTE]
> **Secret Masking & Encryption**: To maintain the simplicity of the Apache 2.0 core, advanced field-level encryption and log masking are prioritized for the **Flinkflow Enterprise Edition** distribution. 

> [!NOTE]
> **Helm Chart** — deferred to v2.0. The current `kubectl`-apply workflow (CRDs + RBAC + ConfigMap) is sufficient for `v0.9-x` and `v1.0` early adopters. A Helm chart adds significant maintenance overhead (values schema, templating, upgrade hooks, chart tests) that is not justified until the installation surface stabilises. See Milestone 3 for the planned chart scope.

---

## 🌐 Milestone 2 — v1.5: Ecosystem & Lifecycle
*Broaden connectivity and manage the full pipeline lifecycle.*

### 🔌 New Connectors (Flowlet Catalog)
- [ ] **Sources**: Debezium (CDC), MongoDB Change Streams, MQTT, Pulsar, HTTP Polling, AWS Kinesis.
- [ ] **Sinks**: ClickHouse, Pinecone (Vector), ElasticSearch, OpenSearch.
- [ ] **Universal Wrap**: Re-implement all core connectors as standard templates in the Flowlet catalog.

### 📦 Management & State
- [ ] **Flowlet Versioning**: Support for semver pinning in pipeline definitions (e.g., `version: "1.2.0"`).

### 🐫 Advanced Camel Features
- [ ] **Side Output Support**: Ability to emit to multiple streams directly from Camel logic.
- [ ] **Join Function Support**: Bridging Flink's `ProcessJoinFunction` to the Camel YAML DSL (v2).
- [ ] **Flink State Access**: Exposing Flink's managed state (ValueState, ListState) as Camel headers.

---

## 🎨 Milestone 3 — v2.0: Platform & Enterprise
*Self-service platform with agentic developer tools and governance.*

### 🛡️ Governance & Compliance
- [ ] **Secret Auditing**: Implementation of job-level audit logs for secret resolution.
- [ ] **Quota Management**: Per-namespace resource limits (parallelism, max jobs).

### 🛠️ Developer Tooling & Extensibility
- [ ] **Helm Chart**: Official Helm chart packaging all Flinkflow infrastructure components (Operator CRDs, RBAC, Dashboard, Flowlet Catalog). Enables single-command install via `helm install flinkflow ./chart` and GitOps-compatible upgrades via ArgoCD/Flux. *Deferred from v1.0 — kubectl-based manifests are sufficient until the install surface stabilises.*
- [ ] **Plugin SDK**: Java/SPI interface for third-party Sources, Sinks, and operations.

### 🤖 Intelligent Features & AI (Flink Agents)
- [x] **Agentic Bridge**: Declarative integration with LLMs via LangChain4j (`type: agent` step).
- [x] **Agent Step Type**: Native `type: agent` with system prompt, memory toggle, and tool discovery.
- [x] **Flowlets-as-Tools**: Automatic registration of Flinkflow Flowlets as agent-invokable tools.
- [x] **Multi-Provider Routing**: Auto-detect provider from model name — OpenAI (`gpt-*`), Google AI Studio (`gemini-*`), Vertex AI (`provider: vertex`), Anthropic (`claude-*`), and Ollama (`ollama:*` or `llama/mistral/phi`).
- [x] **Stateful Agent Memory**: Multi-turn conversation history stored as Flink `ValueState` per key.
- [x] **GeminiDirectChatModel**: Custom REST client targeting `v1beta` Gemini endpoint, bypassing LangChain4j `v1beta` limitations.
- [x] **GeminiDirectChatModel**: Custom REST client targeting `v1beta` Gemini endpoint, bypassing LangChain4j `v1beta` limitations.

---

## 🏢 Flinkflow Enterprise Edition & Platform
*Proprietary features and managed services for organizational scale.*

### 🛡️ Governance & Security
- [ ] **Enterprise RBAC**: OpenID Connect (OIDC) and SSO integration for the Flinkflow Dashboard.
- [ ] **Namespace Isolation**: Multi-tenant security boundaries and resource quotas.
- [ ] **Secret Encryption**: End-to-end masking and field-level encryption for PII compliance.
- [ ] **HashiCorp Vault**: Native `vault:path/key` engine for enterprise secret management.

### 🔌 Proprietary High-Perf Connectors
- [ ] **Cloud Data Warehouses**: Optimized, high-throughput sinks for **Snowflake**, **BigQuery**, and **Redshift**.
- [ ] **Table Formats**: Commercial support for **Delta Lake**, **Iceberg**, and **Hudi** with managed compaction.
- [ ] **Legacy Systems**: Specialized connectors for **SAP**, **Salesforce (CDC)**, and **Mainframe (CDC)**.

### 📊 Advanced Observability & Developer Tools
- [ ] **Agentic Development Tool**: Agent-based development tool for pipeline authoring and logic injection, integrated directly into any modern IDE (VS Code, Cursor, IntelliJ).
- [ ] **Flowlet Marketplace**: UI for browsing and installing community/commercial Flowlets.
- [ ] **Live Log Streaming**: WebSocket-based container log tailing in the dashboard.
- [ ] **Log Filtering**: Dashboard controls for log levels and keyword highlighting.
- [ ] **Job Control UI**: Buttons to Cancel or Stop jobs from the monitoring interface.
- [ ] **SLA Monitoring**: Service Level Agreement (SLA/SLO) tracking and latency forecasting.

### 🤖 Agentic AI & Machine Learning
- [ ] **Validation Assist**: AI-powered explanations and auto-fixes for pipeline configuration errors.
- [ ] **In-stream ML**: Dedicated `model-inference` step for high-performance real-time AI (CPU/GPU-accelerated).
- [ ] **Hot-Reloading**: Live update of transformation logic and agent prompts without stopping the Flink job.
- [ ] **Agentic Audit**: Full prompt/response debugging and token cost attribution for AI Agents.
- [ ] **Air-Gapped Gallery**: Hardened container images with pre-loaded LLM models for offline AI.

---
*Last updated: April 28, 2026*
