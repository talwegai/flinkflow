# 🛠️ Flinkflow Configuration Reference

This guide provides a detailed specification of the Flinkflow YAML DSL, including step types, connector properties, secret management, and complex workflow examples.

---

## 🏗️ Pipeline Specification

### Job Config
- `name`: Name of the Flink job (displayed in the Flink UI).
- `parallelism`: Default parallelism for the job.
- `steps`: A sequential list of pipeline steps.

### Step Config
- `type`: The type of operation (`source`, `process`, `datamapper`, `join`, `http-lookup`, `sink`, or `flowlet`).
- `name`: Unique identifier for the component (e.g., `kafka-source`).
- `code`: The logic snippet for transformation (used in `process`, `filter`, `flatmap`, etc.).
- `language`: (Optional) The runtime for the `code` snippet:
    - `java` (Default): High-performance Janino (Java) execution.
    - `python`: Inline Python via GraalVM Polyglot script engine.
- `properties`: Key-value configuration map. Values can reference Kubernetes Secrets using `secret:name/key`.
- `with`: (For `flowlet` steps) Mapping of parameters passed to a reusable Flowlet.

---

## 🔌 Supported Components

### Sources
| Connector | Type | Properties | Description |
| :--- | :--- | :--- | :--- |
| `kafka-source` | source | `bootstrap.servers`, `topic`, `group.id`, `format` | Reads from Apache Kafka or Confluent Cloud. |
| `file-source` | source | `path`, `monitorInterval` | Reads local or S3 files. Set `monitorInterval` for streaming. |
| `static-source`| source | `content` | Generates a bounded stream from a pipe-separated string. |

### Sinks
| Connector | Type | Properties | Description |
| :--- | :--- | :--- | :--- |
| `console-sink`| sink | (none) | Prints records to stdout. |
| `kafka-sink`  | sink | `bootstrap.servers`, `topic`, `format` | Writes results to a Kafka topic. |
| `file-sink`   | sink | `path`, `rolloverInterval`, `maxPartSize` | Writes partitioned results to local disk or S3. |
| `http-sink`   | sink | `url` or `urlCode`, `method`, `authCode` | Pushes records to an external API (Webhooks). |
| `jdbc-sink`   | sink | `url`, `sql`, `code`, `batchSize` | Batch inserts into PostgreSQL, MySQL, or Oracle. |

### Operations & Transformations
| `process` | 1-to-1 transformation. | `input` (str) | Java / Python |
| `filter` | Retains records returning `true`. | `input` (str) | Java / Python |
| `flatmap` | 1-to-N transformation. | `input` (str) | Java / Python |
| `keyby` | Partitions stream by an extracted key. | `input` (str) | Java-only |
| `reduce` | Rolling aggregation of two elements. | `value1`, `value2`| Java-only |
| `window` | Time-based windowing (Tumbling/Sliding). | `value1`, `value2`| Java-only |
| `sideoutput` | Branches stream using `ctx.output()`. | `input`, `ctx` | Java-only |
| `datamapper` | XSLT 3.0 structural transformation. | `xsltPath` (prop)| XML/JSON |
| `join` | Interval join between two streams. | `left`, `right`| Java-only |
| `http-lookup`| Async enrichment via REST API. | `input`, `resp` | Java-only |

> [!TIP]
> **Python Snippets (GraalVM)**: Use `language: python` to write your logic. The `code:` block is treated as the body of a `process(input)` function.
> ```yaml
> - type: process
>   language: python
>   code: |
>     import json
>     from datetime import datetime
>     
>     data = json.loads(input)
>     data["processed_at"] = datetime.now().isoformat()
>     return json.dumps(data)
> ```


> [!TIP]
> In all Java snippets, the **`metrics`** object (type `MetricGroup`) is available to emit custom live metrics: `metrics.counter("my_counter").inc();`.

---

## 🧩 Flowlets (Reusable Components)

Flowlets are parameterized, pre-built components (inspired by **Apache Camel Kamelets**) that can be shared across multiple pipelines.

1.  **Define**: Define as a Kubernetes CRD (`kind: Flowlet`) with parameters and a template.
2.  **Install**: Apply to the cluster using `./k8s/install-flowlets.sh`.
3.  **Reference**: Use `type: flowlet` in your pipeline and supply values in `with:`.

### Example Flowlet Usage
```yaml
steps:
  - type: flowlet
    name: confluent-kafka-source
    with:
      bootstrapServers: "pkc-xxx.confluent.cloud:9092"
      topic: "orders"
      apiKey: "secret:my-creds/key"
      apiSecret: "secret:my-creds/secret"
```

---

## 🔐 Secret Management

Flinkflow resolves secrets dynamically at job startup based on the environment:

| Strategy | Syntax | Source |
| :--- | :--- | :--- |
| **Kubernetes** | `secret:name/key` | Injected from K8s API (Best for prod). |
| **Env Var** | `${VARIABLE_NAME}` | Injected via `System.getenv()`. |
| **Plain Text**| `my-password` | Hardcoded in YAML (Prototyping only). |

---

## 📖 Deep Dive Examples

### Windowed Aggregation
```yaml
name: "Tumbling Window Count"
steps:
  - type: source
    name: kafka-source
    properties: { topic: "events" }
  - type: keyby
    code: "return input.split(',')[0];"
  - type: window
    properties: { windowType: tumbling, size: 60 }
    code: |
      int count1 = Integer.parseInt(value1.split(",")[1]);
      int count2 = Integer.parseInt(value2.split(",")[1]);
      return value1.split(",")[0] + "," + (count1 + count2);
  - type: sink
    name: console-sink
```

### Async HTTP Enrichment
```yaml
steps:
  - type: http-lookup
    properties:
      urlCode: 'return "https://api.my.com/user/" + input.split(",")[1];'
      timeout: "5000"
    code: 'return input + " | meta=" + response;'
```

### JDBC Batch Sink
```yaml
steps:
  - type: jdbc-sink
    properties:
      url: "jdbc:postgresql://localhost:5432/analytics"
      sql: "INSERT INTO events (id, payload) VALUES (?, ?)"
      code: |
        stmt.setLong(1, Long.parseLong(input.split(",")[0]));
        stmt.setString(2, input.split(",")[1]);
      batchSize: "500"
```

For more examples, check the **[standalone catalog](https://github.com/talwegai/flinkflow/tree/main/examples/standalone/README.md)** or **[Kubernetes library](https://github.com/talwegai/flinkflow/tree/main/deploy/k8s/README.md)**.
