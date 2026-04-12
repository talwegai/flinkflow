/*
 * Copyright 2026 Talweg Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.talweg.flinkflow.core;

import ai.talweg.flinkflow.flowlet.FlowletSpec;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import org.apache.flink.api.common.state.v2.ValueState;
import org.apache.flink.api.common.state.v2.ValueStateDescriptor;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A Flink ProcessFunction that implements the Agentic Bridge (ADR-005).
 *
 * <p>Supports multiple LLM providers via provider auto-detection from the model name:
 * <ul>
 *   <li><b>OpenAI</b>: {@code model: "gpt-4o"} — requires {@code OPENAI_API_KEY}</li>
 *   <li><b>Google Gemini (AI Studio)</b>: {@code model: "gemini-2.5-flash"} — requires {@code GOOGLE_API_KEY}</li>
 *   <li><b>Google Vertex AI</b>: any gemini model + {@code provider: "vertex"} — requires {@code GCP_PROJECT} + ADC</li>
 * </ul>
 */
public class DynamicAgentFunction extends ProcessFunction<String, String> {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicAgentFunction.class);

    private final String agentName;
    private final String model;
    private final String systemPrompt;
    private final boolean useMemory;
    private final Map<String, String> properties;
    private final Map<String, ?> flowletCatalog;

    interface FlinkAgent {
        @dev.langchain4j.service.SystemMessage("{{system}}")
        String chat(@dev.langchain4j.service.V("system") String system, @dev.langchain4j.service.UserMessage String user);
    }

    private transient FlinkAgent agent;
    private transient ValueState<String> chatHistoryState;

    public DynamicAgentFunction(String agentName, String model, String systemPrompt,
                               boolean useMemory, Map<String, String> properties,
                               Map<String, ?> flowletCatalog) {
        this.agentName = agentName;
        this.model = model;
        this.systemPrompt = systemPrompt;
        this.useMemory = useMemory;
        this.properties = properties;
        this.flowletCatalog = flowletCatalog;
    }

    @Override
    public void open(OpenContext parameters) throws Exception {
        String provider = detectProvider();
        LOG.info("Initializing Agentic Bridge [{}] — model: {}, provider: {}", agentName, model, provider);

        ChatLanguageModel chatModel = buildChatModel(provider);

        // Phase 3: Native Tool Resolution from the Flowlet catalog
        List<Object> tools = new ArrayList<>();
        String toolsCsv = properties.getOrDefault("tools", "");
        if (!toolsCsv.isEmpty()) {
            for (String toolName : toolsCsv.split(",")) {
                toolName = toolName.trim().toLowerCase();
                if (flowletCatalog != null && flowletCatalog.containsKey(toolName)) {
                    FlowletSpec spec = (FlowletSpec) flowletCatalog.get(toolName);
                    LOG.info("Registering Flowlet as Tool: {} — {}", spec.getName(), spec.getDescription());
                    tools.add(new AgentToolBridge.DynamicTool(
                            spec.getName(), spec.getDescription(), spec.getType()));
                } else {
                    LOG.warn("Agent [{}] requested unknown tool '{}' — skipping.", agentName, toolName);
                }
            }
        }

        // Use var — AiServices.builder() returns an internal builder type (Java 17)
        var builder = AiServices.builder(FlinkAgent.class)
                .chatLanguageModel(chatModel);
        if (!tools.isEmpty()) {
            builder.tools(tools);
        }
        this.agent = builder.build();

        if (useMemory) {
            ValueStateDescriptor<String> descriptor = new ValueStateDescriptor<>(
                    "agent-history-" + agentName, TypeInformation.of(String.class));
            this.chatHistoryState = getRuntimeContext().getState(descriptor);
        }
    }

    @Override
    public void processElement(String input, Context ctx, Collector<String> out) throws Exception {
        if (!useMemory || chatHistoryState == null) {
            executeAgent(input, null, out);
            return;
        }

        // State V2: Access history asynchronously
        chatHistoryState.asyncValue().thenAccept(history -> {
            try {
                executeAgent(input, history, out);
            } catch (Exception e) {
                LOG.error("Agent [{}] async execution failed: {}", agentName, e.getMessage());
            }
        });
    }

    private void executeAgent(String input, String history, Collector<String> out) throws Exception {
        try {
            String systemContext = (history != null) ? (systemPrompt + "\n--- History ---\n" + history) : systemPrompt;
            String response = agent.chat(systemContext, input);

            if (useMemory && chatHistoryState != null) {
                String newHistory = systemContext + "\nUser: " + input + "\nAssistant: " + response;
                chatHistoryState.asyncUpdate(newHistory);
            }

            out.collect(String.format("{\"agent\":\"%s\",\"model\":\"%s\",\"output\":%s}",
                    agentName, model, toJsonString(response)));

        } catch (Exception e) {
            LOG.error("Agent [{}] failed: {}", agentName, e.getMessage());
            out.collect(String.format("{\"error\":\"Agent failure\",\"message\":\"%s\"}", escapeJson(e.getMessage())));
        }
    }

    // -----------------------------------------------------------------------
    // Provider Resolution
    // -----------------------------------------------------------------------

    private String detectProvider() {
        String explicitProvider = properties.getOrDefault("provider", "").toLowerCase();
        if (!explicitProvider.isEmpty()) {
            return explicitProvider;
        }
        if (model.startsWith("gemini-")) return "google";
        if (model.startsWith("gpt-") || model.startsWith("o1-") || model.startsWith("o3-")) return "openai";
        if (model.startsWith("claude-")) return "anthropic";
        if (model.startsWith("ollama:") || model.contains("llama") || model.contains("mistral") || model.contains("phi")) return "ollama";
        throw new IllegalArgumentException(
            "Cannot detect provider for model '" + model + "'. Set 'provider' property explicitly.");
    }

    private ChatLanguageModel buildChatModel(String provider) {
        switch (provider) {
            case "openai": {
                String apiKey = properties.getOrDefault("apiKey", System.getenv("OPENAI_API_KEY"));
                return OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(model)
                        .build();
            }
            case "google": {
                // Google AI Studio — direct REST endpoint
                // Get a key at: https://aistudio.google.com/apikey
                String apiKey = properties.getOrDefault("apiKey", System.getenv("GOOGLE_API_KEY"));
                String baseUrl = properties.get("baseUrl");
                if (baseUrl != null) {
                    return new GeminiDirectChatModel(apiKey, model, baseUrl);
                }
                return new GeminiDirectChatModel(apiKey, model);
            }
            case "vertex": {
                // Vertex AI — uses Application Default Credentials (ADC)
                // Vertex AI Gemini class lives in langchain4j-vertex-ai-gemini module.
                // Loaded reflectively to make the dependency optional at compile time.
                try {
                    String project = properties.getOrDefault("gcpProject", System.getenv("GCP_PROJECT"));
                    String location = properties.getOrDefault("gcpLocation", System.getenv("GCP_LOCATION"));
                    if (location == null) location = "us-central1";
                    Class<?> builderClass = Class.forName(
                            "dev.langchain4j.model.vertexai.gemini.VertexAiGeminiChatModel");
                    Object b = builderClass.getMethod("builder").invoke(null);
                    b = b.getClass().getMethod("project", String.class).invoke(b, project);
                    b = b.getClass().getMethod("location", String.class).invoke(b, location);
                    b = b.getClass().getMethod("modelName", String.class).invoke(b, model);
                    return (ChatLanguageModel) b.getClass().getMethod("build").invoke(b);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to initialize Vertex AI Gemini model: " + e.getMessage(), e);
                }
            }
            case "ollama": {
                String baseUrl = properties.getOrDefault("baseUrl", "http://localhost:11434");
                // Remove 'ollama:' prefix if present for the actual model name
                String modelName = model.startsWith("ollama:") ? model.substring(7) : model;
                return OllamaChatModel.builder()
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .timeout(Duration.ofSeconds(60))
                        .build();
            }
            default:
                throw new IllegalArgumentException(
                    "Unsupported LLM provider: '" + provider + "'. Supported: openai, google, vertex, ollama.");
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String toJsonString(String value) {
        if (value == null) return "null";
        return "\"" + escapeJson(value) + "\"";
    }

    private String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
