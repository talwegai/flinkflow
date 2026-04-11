package ai.talweg.flinkflow.core;

import ai.talweg.flinkflow.core.GeminiDirectChatModel;
import ai.talweg.flinkflow.core.AgentToolBridge;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.flink.util.Collector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DynamicAgentFunctionTest {

    private MockWebServer mockServer;
    private String baseUrl;

    @BeforeEach
    void startServer() throws Exception {
        mockServer = new MockWebServer();
        mockServer.start();
        baseUrl = mockServer.url("/").toString();
    }

    @AfterEach
    void stopServer() throws Exception {
        mockServer.shutdown();
    }

    private static class MockCollector implements Collector<String> {
        List<String> results = new ArrayList<>();
        @Override
        public void collect(String record) {
            results.add(record);
        }
        @Override
        public void close() {}
    }

    @Test
    public void testAgentProcessSuccess() throws Exception {
        String jsonResponse = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"content\": {\n" +
                "        \"parts\": [\n" +
                "          {\n" +
                "            \"text\": \"Agent Response\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        mockServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .setResponseCode(200));

        Map<String, String> properties = new HashMap<>();
        properties.put("apiKey", "fake-key");
        properties.put("baseUrl", baseUrl);

        org.apache.flink.streaming.api.functions.ProcessFunction<String, String> agentFn = ProcessorFactory.createAgent(
                "test-agent",
                "gemini-1.5-flash",
                "You are an assistant",
                false,
                properties,
                null
        );

        agentFn.open(new org.apache.flink.configuration.Configuration());

        MockCollector collector = new MockCollector();
        agentFn.processElement("Hello", null, collector);

        assertEquals(1, collector.results.size());
        String out = collector.results.get(0);
        assertTrue(out.contains("\"agent\":\"test-agent\""));
        assertTrue(out.contains("\"output\":\"Agent Response\""));
    }

    @Test
    public void testAgentProcessFailure() throws Exception {
        mockServer.enqueue(new MockResponse()
                .setBody("Error")
                .setResponseCode(500));

        Map<String, String> properties = new HashMap<>();
        properties.put("apiKey", "fake-key");
        properties.put("baseUrl", baseUrl);

        org.apache.flink.streaming.api.functions.ProcessFunction<String, String> agentFn = ProcessorFactory.createAgent(
                "fail-agent",
                "gemini-1.5-flash",
                "You are an assistant",
                false,
                properties,
                null
        );

        agentFn.open(new org.apache.flink.configuration.Configuration());

        MockCollector collector = new MockCollector();
        agentFn.processElement("Hello", null, collector);

        assertEquals(1, collector.results.size());
        String out = collector.results.get(0);
        assertTrue(out.contains("\"error\":\"Agent failure\""));
        assertTrue(out.contains("500"));
    }

    @Test
    public void testProviderResolutions() {
        // Test OpenAI branch
        Map<String, String> oaiProps = new HashMap<>();
        oaiProps.put("apiKey", "dummy-key");
        org.apache.flink.streaming.api.functions.ProcessFunction<String, String> oaiFn = ProcessorFactory.createAgent(
                "oai", "gpt-4o", "sys", false, oaiProps, null);
        assertDoesNotThrow(() -> oaiFn.open(new org.apache.flink.configuration.Configuration()));

        // Test Unsupported Anthropic
        org.apache.flink.streaming.api.functions.ProcessFunction<String, String> anthropicFn = ProcessorFactory.createAgent(
                "claude", "claude-3-opus", "sys", false, new HashMap<>(), null);
        assertThrows(IllegalArgumentException.class, () -> anthropicFn.open(new org.apache.flink.configuration.Configuration()));

        // Test explicit provider
        Map<String, String> explicitProps = new HashMap<>();
        explicitProps.put("provider", "openai");
        explicitProps.put("apiKey", "dummy-key");
        org.apache.flink.streaming.api.functions.ProcessFunction<String, String> explicitFn = ProcessorFactory.createAgent(
                "exp", "unknown-model-format", "sys", false, explicitProps, null);
        assertDoesNotThrow(() -> explicitFn.open(new org.apache.flink.configuration.Configuration()));

        // Test Ollama resolution (prefix-based and family-based)
        assertDoesNotThrow(() -> ProcessorFactory.createAgent(
                "ollama-phi", "phi", "sys", false, new HashMap<>(), null).open(new org.apache.flink.configuration.Configuration()));
        assertDoesNotThrow(() -> ProcessorFactory.createAgent(
                "ollama-mistral", "ollama:mistral", "sys", false, new HashMap<>(), null).open(new org.apache.flink.configuration.Configuration()));
        
        // Test Ollama resolution (explicit provider for non-prefixed models like qwen)
        Map<String, String> qwenProps = new HashMap<>();
        qwenProps.put("provider", "ollama");
        assertDoesNotThrow(() -> ProcessorFactory.createAgent(
                "ollama-qwen", "qwen2", "sys", false, qwenProps, null).open(new org.apache.flink.configuration.Configuration()));
    }

    @Test
    public void testFlowletToolMapping() {
        Map<String, String> props = new HashMap<>();
        props.put("apiKey", "fake");
        props.put("provider", "openai");
        props.put("tools", "my-tool, unknown");

        Map<String, ai.talweg.flinkflow.flowlet.FlowletSpec> catalog = new HashMap<>();
        ai.talweg.flinkflow.flowlet.FlowletSpec mockTool = new ai.talweg.flinkflow.flowlet.FlowletSpec();
        mockTool.setName("my-tool");
        mockTool.setDescription("Does a thing");
        mockTool.setType("process");
        catalog.put("my-tool", mockTool);

        org.apache.flink.streaming.api.functions.ProcessFunction<String, String> agentFn = ProcessorFactory.createAgent(
                "tools-agent", "gpt-4", "sys", false, props, catalog);
        
        assertDoesNotThrow(() -> agentFn.open(new org.apache.flink.configuration.Configuration()));
    }
}
