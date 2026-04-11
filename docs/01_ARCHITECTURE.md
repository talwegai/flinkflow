---
title: System Architecture Diagram
slug: /ARCHITECTURE
---
# Flinkflow — System Architecture Diagram

## Full System Overview

```mermaid
graph TB
    subgraph USER["👤 User / Developer"]
        YAML["Pipeline YAML / CR<br/>(pipeline.yaml or kind: Pipeline)"]
    end

    subgraph DEPLOY["🚀 Deployment Options"]
        D1["Local<br/>scripts/run-local.sh"]
        D2["Docker<br/>docker run"]
        D3["Manual K8s<br/>kubectl apply"]
        D4["Flink K8s Operator<br/>FlinkDeployment CR"]
        D5["Native K8s<br/>flink run-application"]
    end

    subgraph CORE["⚙️ Flinkflow Application (JVM / Flink Job)"]
        MAIN["FlinkflowApp.java<br/>(Main Entrypoint)"]

        subgraph FLOWLET_RES["Flowlet Resolver"]
            FR_BUILT["Built-in Catalog<br/>(JAR Resources)"]
            FR_K8S["Kubernetes Discovery<br/>(--enable-k8s-flowlets)"]
        end

        subgraph PIPELINE["Pipeline Engine"]
            direction LR
            SRC["Source Layer"]
            OPS["Operations Layer"]
            SNK["Sink Layer"]
            SRC --> OPS --> SNK
        end

        subgraph POLYGLOT["Polyglot Engine"]
            JANINO["Janino<br/>(Java Compiler)"]
            GRAALVM["GraalVM Python<br/>(Restricted Sandbox)"]
            CAMEL["Apache Camel Engine<br/>(YAML DSL & Expressions)"]
            AGENT_NODE["Agentic Bridge<br/>(OpenAI / Gemini / Vertex AI)"]
        end
        
        XSLT["Saxon<br/>XSLT 3.0 DataMapper"]
    end

    subgraph SOURCES["📥 Sources"]
        S_KAFKA["kafka-source<br/>(Apache Kafka)"]
        S_CONFLUENT["confluent-kafka-source<br/>(Confluent Cloud / SASL_SSL)"]
        S_FILE["file-source / s3-source<br/>(Local FS / S3)"]
        S_STATIC["static-source<br/>(Bounded Test Data)"]
        S_DATAGEN["datagen<br/>(DataGen Connector)"]
    end

    subgraph OPS_DETAIL["🔄 Operations"]
        OP_PROCESS["process / transform<br/>(Polyglot snippet)"]
        OP_FILTER["filter<br/>(Polyglot Predicate)"]
        OP_FLATMAP["flatmap<br/>(Polyglot 1-to-N)"]
        OP_KEYBY["keyby / groupby<br/>(Stream Partitioning)"]
        OP_REDUCE["reduce / aggregate<br/>(Rolling Aggregation)"]
        OP_WINDOW["window<br/>(Tumbling / Sliding / Session)"]
        OP_SIDE["sideoutput<br/>(Branch / DLQ)"]
        OP_JOIN["join<br/>(Interval Join — 2 Streams)"]
        OP_DM["datamapper<br/>(XSLT 3.0 Structural Transform)"]
        OP_HTTP["http-lookup<br/>(Async REST Enrichment)"]
    end

    subgraph SINKS["📤 Sinks"]
        SK_CONSOLE["console-sink<br/>(stdout)"]
        SK_KAFKA["kafka-sink<br/>(Apache Kafka)"]
        SK_CONFLUENT["confluent-kafka-sink<br/>(Confluent Cloud / SASL_SSL)"]
        SK_FILE["file-sink / s3-sink<br/>(Local FS / S3, Partitioned)"]
        SK_HTTP["http-sink / webhook-sink<br/>(HTTP POST/PUT)"]
        SK_JDBC["jdbc-sink<br/>(PostgreSQL / MySQL / Oracle)"]
    end

    subgraph K8S["☸️ Kubernetes Cluster"]
        subgraph FLINK_NS["flink namespace"]
            JM["JobManager<br/>(Application Mode)"]
            TM["TaskManagers<br/>(Worker Pool)"]
        end

        subgraph CRD_NS["Custom Resources"]
            CR_PIPELINE["Pipeline CR<br/>(kind: Pipeline)"]
            CR_FLOWLET["Flowlet CRs<br/>(kind: Flowlet)"]
            CR_FLINK_OP["FlinkDeployment CR<br/>(Flink Operator)"]
        end

        subgraph MONITOR_NS["flinkmonitor namespace"]
            DASH["Monitoring Dashboard<br/>(nicegui / Python)"]
            DASH --> |"Flink REST API<br/>(port-forward / NodePort)"| JM
            DASH --> |"kubectl (in-cluster)"| K8S_API
        end

        K8S_API["Kubernetes API Server"]
    end

    subgraph FLOWLET_CATALOG["📦 Built-in Flowlet Catalog"]
        FC1["kafka-source"]
        FC2["kafka-sink"]
        FC3["confluent-kafka-source"]
        FC4["confluent-kafka-sink"]
        FC5["log-transform"]
        FC6["http-enrich"]
    end

    subgraph TOOLS["🛠️ External / Control Plane"]
        EXT_KAFKA["Apache Kafka<br/>(Self-hosted)"]
        EXT_CC["Confluent Cloud"]
        EXT_S3["Amazon S3"]
        EXT_HTTP["REST API / Webhook"]
        EXT_DB["RDBMS<br/>(PostgreSQL / MySQL / Oracle)"]
        EXT_REGISTRY["Apicurio / Confluent<br/>Schema Registry"]
    end


    %% Connections — User to Deploy
    YAML -->|"submitted via"| D1
    YAML -->|"submitted via"| D2
    YAML -->|"kubectl apply"| D3
    YAML -->|"Pipeline CR"| CR_PIPELINE
    YAML -->|"FlinkDeployment CR"| CR_FLINK_OP

    %% Deploy paths to Core
    D1 --> MAIN
    D2 --> MAIN
    D3 --> JM
    D4 --> CR_FLINK_OP
    D5 --> JM
    CR_FLINK_OP --> JM
    CR_PIPELINE --> |"read at startup"| MAIN

    %% Core internals
    MAIN --> FLOWLET_RES
    MAIN --> PIPELINE
    FR_BUILT --> PIPELINE
    FR_K8S --> |"query"| K8S_API
    K8S_API --> CR_FLOWLET
    CR_FLOWLET --> |"expand flowlet steps"| PIPELINE
    POLYGLOT --> |"executes snippets"| OPS
    XSLT --> |"runs XSLT transforms"| OPS

    %% Pipeline layers
    SRC --> |"reads from"| SOURCES
    OPS --> OPS_DETAIL
    SNK --> |"writes to"| SINKS

    %% Sources → External
    S_KAFKA --> EXT_KAFKA
    S_CONFLUENT --> EXT_CC
    S_FILE --> EXT_S3
    S_HTTP_EXT["http-lookup"] --> EXT_HTTP

    %% Sinks → External
    SK_KAFKA --> EXT_KAFKA
    SK_CONFLUENT --> EXT_CC
    SK_FILE --> EXT_S3
    SK_HTTP --> EXT_HTTP
    SK_JDBC --> EXT_DB


    %% Flink internals
    JM --> |"distributes tasks"| TM

    %% Flowlet catalog
    FR_BUILT --> FLOWLET_CATALOG
```

---

## Pipeline Step Flow

```mermaid
flowchart LR
    subgraph INPUT["Input Layer"]
        A["Pipeline YAML / CR"]
    end

    subgraph RESOLVE["Resolution"]
        B{"Step type?"}
        B -->|"source"| C["Source Connector"]
        B -->|"process/filter/etc"| PL{"Runtime?"}
        PL -->|"java<br/>(Janino)"| D1["Direct JVM<br/>Execution"]
        PL -->|"python<br/>(GraalVM)"| D2["Sandboxed<br/>Execution"]
        PL -->|"camel-yaml<br/>(YAML DSL)"| D3["Full Route<br/>Execution"]
        PL -->|"camel-simple<br/>(Expressions)"| D3
        B -->|"agent"| D4["Agentic Bridge<br/>(OpenAI/Gemini/Vertex)"]
        B -->|"datamapper"| E["XSLT 3.0<br/>(Saxon)"]
        B -->|"http-lookup"| F["Async HTTP<br/>Client"]
        B -->|"flowlet"| G["Flowlet Resolver"]
        B -->|"sink"| H["Sink Connector"]
        G -->|"expand into steps"| B
    end

    subgraph EXEC["Flink DAG Execution"]
        C --> D1 --> H
        C --> D2 --> H
        C --> E --> H
        C --> F --> H
    end

    A --> B
```

---

## Kubernetes Deployment Architecture

```mermaid
graph TB
    subgraph CLUSTER["Kubernetes Cluster"]
        subgraph FLINK["flink namespace"]
            JM["JobManager Pod<br/>(Application Mode)"]
            TM1["TaskManager Pod 1"]
            TM2["TaskManager Pod 2"]
            TM3["TaskManager Pod N"]
            SVC["Flink REST Service<br/>(NodePort)"]
            CM["ConfigMap<br/>(pipeline.yaml)"]
            RBAC["ServiceAccount +<br/>ClusterRole<br/>(rbac.yaml)"]

            JM --> TM1
            JM --> TM2
            JM --> TM3
            SVC --> JM
            JM -->|"reads"| CM
            JM -->|"uses"| RBAC
        end

        subgraph CRDs["Custom Resource Definitions"]
            CRD_PL["Pipeline CRD<br/>(crd-pipeline.yaml)"]
            CRD_FL["Flowlet CRD<br/>(crd-flowlet.yaml)"]
            PL_CR["Pipeline CR<br/>(my-stream-job)"]
            FL_CRS["Flowlet CRs<br/>(kafka-source,<br/>confluent-kafka-source,<br/>http-enrich, etc.)"]

            CRD_PL --> PL_CR
            CRD_FL --> FL_CRS
        end

        subgraph MONITOR["flinkmonitor namespace"]
            DASH["Monitoring Dashboard<br/>Pod (nicegui)"]
            DASH_SVC["Dashboard Service<br/>(NodePort)"]
            DASH_SVC --> DASH
        end

        FLINK_OP["Flink Kubernetes<br/>Operator<br/>(cert-manager +<br/>Helm chart)"]
        FLINK_OP -->|"manages"| JM

        K8S_API["Kubernetes<br/>API Server"]
        JM -->|"read Pipeline/Flowlet CRs"| K8S_API
        K8S_API --> PL_CR
        K8S_API --> FL_CRS
        DASH -->|"in-cluster kubectl"| K8S_API
        DASH -->|"Flink REST"| SVC

    end

    DEV["👤 Developer / Analyst<br/>kubectl apply / SQL UI"]
    DEV --> CRD_PL
    DEV --> CRD_FL
    DEV --> PL_CR
    DEV --> FL_CRS
    BROWSER["🌐 Browser"] --> DASH_SVC
```

---

## Flowlet Resolution Flow

```mermaid
sequenceDiagram
    participant PY as Pipeline YAML
    participant APP as FlinkflowApp
    participant BUILT as Built-in Catalog (JAR)
    participant K8S as Kubernetes API
    participant FLINK as Flink DAG

    APP->>PY: Parse pipeline steps
    loop For each step
        alt type == flowlet
            APP->>BUILT: Lookup flowlet by name
            APP->>K8S: Query Flowlet CR (if --enable-k8s-flowlets)
            K8S-->>APP: Return Flowlet CR (overrides built-in if same name)
            APP->>APP: Substitute {{params}} with `with:` values
            APP->>APP: Expand flowlet into concrete steps
        else type == source/process/sink/etc
            APP->>APP: Use step directly
        end
    end
    APP->>FLINK: Build and execute Flink DataStream DAG
```

---

## Polyglot & Security Architecture

Flinkflow separates pipeline structure (YAML) from custom business logic (Java/Python/Camel/Agent).

### Execution Engines
- **Janino Engine**: High-performance Java compilation at runtime. Snippets are compiled directly into Flink `RichMapFunction`, `RichFilterFunction`, etc., and execute at native JVM speeds.
- **GraalVM Python Engine**: Advanced polyglot execution. Python snippets are executed within a **Restricted Sandbox** provided by the GraalVM Context API.
- **Apache Camel Engine**: Declarative expression and route evaluation.
    - **Expressions**: Supports **Simple**, **JsonPath**, and **Groovy** for data-centric transformations.
    - **YAML DSL**: Supports full **Camel YAML DSL** fragments, enabling complex EIPs (Choice, Throttler, Unmarshal) within a single Flink operator.
- **Agentic Bridge** (`language: agent`): Runs autonomous LLM-backed AI agents directly inside a Flink `ProcessFunction`. Supports multi-turn stateful memory (Flink `ValueState`), Flowlet-as-a-Tool execution, and multiple providers:
    - **OpenAI**: `gpt-4o`, `gpt-4`, `o1-*`, `o3-*` — requires `OPENAI_API_KEY`
    - **Google Gemini (AI Studio)**: `gemini-*` — requires `GOOGLE_API_KEY`
    - **Google Vertex AI**: Any Gemini model with `provider: vertex` — uses Application Default Credentials (ADC)

### Security Sandboxing (Zero-Trust)

Flinkflow implements a strict, **deny-by-default** security model for guest code execution to protect the Flink JobManager and TaskManagers from potential exploits within user-supplied YAML logic.

#### Python Sandbox Protections (GraalVM)

The Python execution environment ([PythonEvaluator.java](../src/main/java/ai/talweg/flinkflow/core/PythonEvaluator.java)) is configured with a zero-trust policy:

- **Blocked File System Access**: `IOAccess.NONE` is enforced. Guest scripts cannot read from or write to the host disk (it cannot access secrets, `/etc/hosts`, or log files).
- **Blocked Java Class Lookup**: Scripts are prohibited from using `import java` or looking up arbitrary Java classes. This prevents scripts from calling `java.lang.System.exit()` or accessing internal JVM state.
- **Blocked Native Access**: Loading native libraries or executing external binaries is strictly prohibited.
- **Blocked Multi-Threading**: Scripts cannot spawn new host threads or background processes.
- **Blocked Polyglot Interop**: Python logic cannot access or execute other guest languages.
- **Scoped Host Access**: Guest code can only interact with host objects that are explicitly passed as arguments by the Flinkflow engine.

#### Java Logic Validation (Janino)

Java code snippets are dynamically compiled into isolated functional blocks. By default, they do not share broad access to the Flinkflow application's internal classes, ensuring that custom business logic remains bounded by the pipeline context.

