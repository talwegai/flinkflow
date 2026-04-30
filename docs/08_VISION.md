# Flinkflow — Product Vision & Design

> **Version**: 1.0 (April 2026)  
> **Status**: Living Document — defines the strategic direction of the platform.  
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
| **Progressively Escapable** | Inline Java (Janino), Python (GraalVM), and Camel fragments provide escape hatches for custom logic |
| **Kubernetes Native** | The Flowlet catalog, pipeline definitions, and operational concerns live as Kubernetes CRDs |
| **Pluggable by Design** | Sources, sinks, and operations are discrete, independently composable units |
| **Zero Lock-in** | Flinkflow generates and submits standard Flink `DataStream` DAGs — there is no proprietary runtime |
| **Agentic Reasoners** | Autonomous AI agents (gpt-4) can be dropped into any stream to perform non-deterministic reasoning |

---

## 3. Product Roadmap

The execution of the Flinkflow vision is tracked via our unified project backlog.

### Strategic Milestones
*   **Milestone 1 (v1.0) — Production Ready**: Focus on security (Vault/Secrets), operational safety (validation), and log observability.
*   **Milestone 2 (v1.5) — Ecosystem Expansion**: Expanding the Flowlet Catalog (Iceberg, Snowflake, CDC).
*   **Milestone 3 (v2.0) — The Platform**: Delivering the Visual IDE, **Declarative Agentic Bridge**, and enterprise governance features.

👉 **View the detailed task-level roadmap here: [Project Backlog (TODO)](/docs/10_TODO)**

---

## 4. Key Design Decisions (ADRs)

### ADR-001: String-typed Stream Data Model
The Flink `DataStream<String>` is the universal wire type. Any serialisation format (JSON, CSV, Avro text, XML) can be represented as a String and transformed via Java snippets, Camel logic, or XSLT.

### ADR-002: Polyglot Dynamic Code Execution
Inline Java (Janino) and Python (GraalVM) code snippets in YAML are compiled/interpreted and executed at runtime. This maintains Flinkflow's "Single JAR" deployment model.

### ADR-003: Flowlets as Kubernetes CRDs
Reusable components (Flowlets) are defined as `kind: Flowlet` Kubernetes Custom Resources. This makes the catalog natively versioned and GitOps-compatible.

### ADR-004: Flink 1.18 + Java 17
The target runtime is Apache Flink 1.18.1 on Java 17, leveraging modern LTS improvements and container official images.

### ADR-005: The Agentic Bridge
Autonomous AI agents are first-class YAML citizens. Flinkflow bridges `flink-agents` concepts to our DSL, allowing agents to use Flowlets as "Tools" and Flink State as "Memory".

---

## 5. Non-Functional Targets

| Concern | Target |
|---|---|
| **Throughput** | Pipeline overhead ≤ 5% vs. native Flink DAG for equivalent logic |
| **Startup Latency** | Pipeline submission within 10s of JVM start |
| **Container Size** | Docker image ≤ 1.5GB |

---
*Last updated: April 2026*
