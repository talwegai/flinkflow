# ADR-005: The Agentic Bridge (Flink Agents Integration)

## Status
Accepted (April 2026) - Implemented in v0.9.0-BETA

## Context
Standard Flinkflow pipelines are deterministic: Data flows from Source through a series of predefined Steps to a Sink. While powerful, this doesn't support "reasoning" — the ability for a pipeline to decide which operations to perform based on complex, unstructured context or to interact with LLMs in a loop.

The `apache/flink-agents` project provides a runtime for autonomous agents. Flinkflow aims to be the first **Declarative Agentic Platform**, allowing users to define these agents using YAML and bind existing Flowlets to them as "Tools."

## Decision
We will introduce a first-class `agent` step type into the Flinkflow DSL. This step will encapsulate an Apache Flink Agent, providing a bridge between the declarative YAML configuration and the agentic runtime.

### Key Components

1.  **`AgentProcessor`**: A specialized Flink `ProcessFunction` that manages the lifecycle of an AI agent, including its context, memory (state), and tool invocation.
2.  **Tool Mapping**: A mechanism to transform any Flinkflow Flowlet into a callable tool for the LLM. 
3.  **Context Injection**: The ability to inject streaming event data, system prompts, and external metadata into the agent's prompt context.
4.  **Stateful Memory**: Leveraging Flink's managed state (ValueState) to persist agent "history" (messages, reasoning steps, tool results) keyed by event ID or user ID.

### Proposed YAML Schema
```yaml
- type: agent
  name: support-assistant
  properties:
    model: "gpt-4o"
    systemPrompt: "You are a customer support agent. Resolve the user request."
    memory: true
    tools:
      - name: "get_order_info"
        flowlet: "db-lookup-flowlet" # References a registered Flowlet
```

## Rationale
*   **Accessibility**: Users can deploy Agentic AI without writing boilerplate Java/Python for LLM interaction.
*   **Leverage**: By "toolizing" Flowlets, we allow agents to use any existing Flinkflow logic (Kafka, JDBC, HTTP) as an action.
*   **Scalability**: Agents run natively on Flink's distributed runtime, benefiting from exactly-once state and high throughput.

## Consequences
*   **Complexity**: Integration with external LLM APIs introduces latency and cost management concerns into the pipeline.
*   **Dependency**: Requires adding `flink-agents` and model-specific libraries to the shaded JAR.
*   **Reliability**: Non-deterministic agent behavior requires new validation and monitoring patterns (SLA tracking for LLM tokens/latency).
