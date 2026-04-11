# 🛠️ Flinkflow Configuration Reference

This guide provides a detailed specification of the Flinkflow YAML DSL, including step types, connector properties, secret management, and complex workflow examples.

---

## 🏗️ Pipeline Specification

### Job Config
- `name`: Name of the Flink job (displayed in the Flink UI).
- `parallelism`: Default parallelism for the job.
- `steps`: A sequential list of pipeline steps.

### Step Config
- `type`: The type of operation (`source`, `process`, `datamapper`, `join`, `http-lookup`, `agent`, `sink`, or `flowlet`).
- `name`: Unique identifier for the component (e.g., `kafka-source`).
- `code`: The logic snippet for transformation (used in `process`, `filter`, `flatmap`, etc.).
- `language`: (Optional) The runtime for the `code` snippet:
    - `java` (Default): High-performance Janino (Java) execution.
    - `python`: Inline Python via GraalVM Polyglot script engine.
    - `camel-simple` (or `camel`): Declarative Camel Simple expression. [[Docs]](https://camel.apache.org/components/latest/languages/simple-language.html)
    - `camel-jsonpath` (or `jsonpath`): JSON extractions and filters. [[Docs]](https://camel.apache.org/components/latest/languages/jsonpath-language.html)
    - `camel-groovy` (or `groovy`): High-performance JVM-native scripting. [[Docs]](https://camel.apache.org/components/latest/languages/groovy-language.html)
    - `camel-yaml`: Complex route fragments using Camel YAML DSL. [[Docs]](https://camel.apache.org/manual/camel-yaml-dsl.html)
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
| `process` | 1-to-1 transformation. | `input`/`body` | Java / Python / Camel |
| `filter` | Retains records returning `true`. | `input`/`body` | Java / Python / Camel |
| `flatmap` | 1-to-N transformation. | `input`/`body` | Java / Python / Camel |
| `keyby` | Partitions stream by an extracted key. | `input`/`body` | Java / Camel |
| `reduce` | Rolling aggregation of two elements. | `value1`, `value2`| Java / Camel |
| `window` | Time-based windowing (Tumbling/Sliding). | `value1`, `value2`| Java / Camel |
| `sideoutput` | Branches stream using `ctx.output()`. | `input`, `ctx` | Java-only |
| `datamapper` | XSLT 3.0 structural transformation. | `xsltPath` (prop)| XML/JSON |
| `join` | Interval join between two streams. | `left`, `right`| Java-only |
| `agent` | Autonomous LLM agent over each record. | `input` | OpenAI / Gemini / Vertex |
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
>
> [!TIP]
> **Declarative Logic (Apache Camel)**: Use `language: camel` or `language: jsonpath` for low-code operations. This removes the need for manual JSON parsing and boilerplate.
>
> **Field Extraction (JsonPath):**
> ```yaml
> - type: keyby
>   language: jsonpath
>   code: "$.user.id"
> ```
>
> **Template Formatting (Camel Simple):**
> ```yaml
> - type: process
>   language: camel
>   code: "User ${jsonpath($.user.name)} logged in from ${headers.ip}"
> ```
>
> **Complex Stateful Math (Camel Groovy):**
> For windowed reductions, use `headers.get("value1")` and `headers.get("value2")`.
> ```yaml
> - type: window
>   language: camel-groovy
>   code: |
>     def o1 = new groovy.json.JsonSlurper().parseText(headers.get("value1"))
>     def o2 = new groovy.json.JsonSlurper().parseText(headers.get("value2"))
>     return groovy.json.JsonOutput.toJson([sum: o1.val + o2.val])
> ```
>
> **Enterprise Patterns (Camel YAML DSL):**
> For complex routing or multi-step logic within a single operator, use `language: camel-yaml`. The route must start with `from: uri: direct:start`.
> ```yaml
> - type: process
>   language: camel-yaml
>   code: |
>     - from:
>         uri: "direct:start"
>         steps:
>           - choice:
>               when:
>                 - simple: "${body} > 100"
>                   steps:
>                     - setBody:
>                         constant: "OVER_LIMIT"
>               otherwise:
>                 steps:
>                   - setBody:
>                       simple: "Accepted: ${body}"
> ```


> [!TIP]
> In all Java snippets, the **`metrics`** object (type `MetricGroup`) is available to emit custom live metrics: `metrics.counter("my_counter").inc();`.

> [!TIP]
> **Agentic Bridge**: Use `type: agent` to run an autonomous LLM over each stream record. Supports stateful multi-turn memory, Flowlet-as-a-Tool execution, and multiple providers.
> ```yaml
> - type: agent
>   name: support-triage
>   properties:
>     model: "gemini-2.5-flash"       # or gpt-4o, claude-3-opus
>     systemPrompt: "Classify the following customer message as: BILLING, TECHNICAL, or GENERAL."
>     memory: "false"                  # set true for multi-turn stateful conversation
>     apiKey: "secret:llm-creds/google-api-key"
>     tools: "log-transform,http-enrich"  # optional: Flowlets exposed as agent tools
> ```
> **Supported Providers** (auto-detected from model name):
> | Provider | Models | Auth |
> | :--- | :--- | :--- |
> | OpenAI | `gpt-4o`, `gpt-4`, `o1-*` | `OPENAI_API_KEY` or `apiKey` property |
> | Google Gemini | `gemini-*` | `GOOGLE_API_KEY` or `apiKey` property |
> | Google Vertex | Any Gemini + `provider: vertex` | Application Default Credentials (ADC) |
> | Ollama | `ollama:*`, `phi*`, `llama*`, `mistral*` | `baseUrl: "http://localhost:11434"` |
>
> **Example: Local Ollama Agent**
> ```yaml
> - type: agent
>   name: local-classifier
>   properties:
>     # Uses 'ollama:' prefix to target local Ollama server
>     model: "ollama:llama3"
>     baseUrl: "http://localhost:11434"
>     systemPrompt: "Classify this record."
> ```

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

For more examples, check the **[standalone catalog](https://github.com/talwegai/flinkflow/blob/main/examples/standalone/README.md)** or **[Kubernetes library](https://github.com/talwegai/flinkflow/blob/main/deploy/k8s/README.md)**.
