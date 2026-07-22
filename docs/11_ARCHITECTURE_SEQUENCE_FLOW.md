---
title: Architecture Sequence and End-to-End Flow
slug: /ARCHITECTURE_SEQUENCE_FLOW
---

# Flinkflow End-to-End Architecture Sequence and Flow

## End-to-End Sequence

```mermaid
sequenceDiagram
    autonumber
    actor Dev as Developer
    participant Repo as Pipeline YAML or Pipeline CR
    participant App as FlinkflowApp
    participant K8s as Kubernetes API
    participant FR as FlowletRegistry and FlowletResolver
    participant GV as GraphValidator
    participant Flink as Flink Runtime (JobManager and TaskManagers)
    participant Ext as External Systems (Kafka, S3, HTTP, JDBC)

    Dev->>Repo: Define pipeline and optional flowlet references
    Dev->>App: Start job (CLI or deployment)

    alt Pipeline loaded from file
        App->>Repo: Read pipeline YAML
    else Pipeline loaded from Kubernetes
        App->>K8s: Fetch Pipeline CR by name and namespace
        K8s-->>App: Return pipeline spec
    end

    App->>FR: Initialize flowlet registry

    alt Kubernetes flowlets enabled
        FR->>K8s: Discover Flowlet CRs
        K8s-->>FR: Return flowlet definitions
    end

    FR-->>App: Return resolved and expanded steps
    App->>GV: Validate expanded graph and step wiring
    GV-->>App: Validation result

    alt Validation failed
        App-->>Dev: Stop with actionable error
    else Validation passed
        App->>Flink: Build and submit executable stream graph
        loop For each incoming event
            Flink->>Flink: Execute source, operations, and sink chain
            Flink->>Ext: Read and write records
            Ext-->>Flink: Return data or acknowledgments
        end
        Flink-->>Dev: Expose job status and metrics
    end
```

## End-to-End Runtime Flow

```mermaid
flowchart TD
    A[Pipeline Definition<br/>YAML or Pipeline CR] --> B{Load Mode}

    B -->|File| C[Parse YAML into JobConfig]
    B -->|Kubernetes| D[Fetch Pipeline CR and map to JobConfig]

    C --> E[Initialize Flowlet Registry]
    D --> E

    E --> F{Kubernetes Flowlets Enabled}
    F -->|Yes| G[Load Flowlet CRs from Kubernetes API]
    F -->|No| H[Use built-in and local flowlets only]
    G --> I[Resolve flowlet references into concrete steps]
    H --> I

    I --> J[Validate graph structure and configuration]
    J --> K{Graph valid}

    K -->|No| L[Fail fast with validation report]
    K -->|Yes| M[Build Flink stream graph]

    M --> N[Execute runtime chain]

    subgraph Runtime Chain
      N1[Source Layer]
      N2[Operation Layer<br/>process, filter, flatmap, join, window, datamapper, http-lookup]
      N3[Sink Layer]
      N1 --> N2 --> N3
    end

    N --> N1
    N3 --> O[External Targets<br/>Kafka, S3, HTTP, JDBC]
    O --> P[Operational Visibility<br/>logs, metrics, monitor dashboard]
```

## Reading Guide

- The sequence diagram explains call order and decision points from submission to live processing.
- The flow diagram explains transformation stages and branching logic in the runtime lifecycle.
- Together they document both control-plane behavior (loading and validation) and data-plane behavior (stream execution).
