package ai.talweg.flinkflow.core;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GeminiDirectChatModelTest {

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

    @Test
    public void testSuccessfulResponse() {
        String jsonResponse = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"content\": {\n" +
                "        \"parts\": [\n" +
                "          {\n" +
                "            \"text\": \"Test AI response\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        mockServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .setResponseCode(200));

        GeminiDirectChatModel model = new GeminiDirectChatModel("fake-key", "gemini-2.5-flash", baseUrl);
        Response<AiMessage> response = model.generate(Collections.singletonList(UserMessage.from("Hello")));

        assertNotNull(response);
        assertEquals("Test AI response", response.content().text());
    }

    @Test
    public void testExceptionOnNon200() throws Exception {
        mockServer.enqueue(new MockResponse()
                .setBody("Bad Request")
                .setResponseCode(400));

        GeminiDirectChatModel model = new GeminiDirectChatModel("fake", "gemini-2.5", baseUrl);
        assertThrows(RuntimeException.class,
                () -> model.generate(List.of(new dev.langchain4j.data.message.UserMessage("Hi"))));
    }

    @Test
    public void testMissingCandidates() throws Exception {
        mockServer.enqueue(new MockResponse()
                .setBody("{\"promptFeedback\": {\"blockReason\": \"SAFETY\"}}")
                .setResponseCode(200));

        GeminiDirectChatModel model = new GeminiDirectChatModel("fake", "gemini-2.5", baseUrl);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> model.generate(List.of(new dev.langchain4j.data.message.UserMessage("Hi"))));
        assertTrue(ex.getMessage().contains("Block reason: SAFETY"));
    }

    @Test
    public void testMissingApiKeyThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new GeminiDirectChatModel("", "model"));
        assertThrows(IllegalArgumentException.class, () -> new GeminiDirectChatModel(null, "model"));
    }

    @Test
    public void testError503ServiceUnavailable() {
        String errorJson = "{\n" +
                "  \"error\": {\n" +
                "    \"code\": 503,\n" +
                "    \"message\": \"Service Unavailable\"\n" +
                "  }\n" +
                "}";

        mockServer.enqueue(new MockResponse()
                .setBody(errorJson)
                .setResponseCode(503));

        GeminiDirectChatModel model = new GeminiDirectChatModel("fake-key", "gemini-2.5-flash", baseUrl);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            model.generate(Collections.singletonList(UserMessage.from("Hello")));
        });

        assertTrue(exception.getMessage().contains("503"));
        assertTrue(exception.getMessage().contains("Service Unavailable"));
    }

    @Test
    public void testEmptyCandidates() {
        String emptyJson = "{\n" +
                "  \"candidates\": [],\n" +
                "  \"promptFeedback\": { \"blockReason\": \"SAFETY\" }\n" +
                "}";

        mockServer.enqueue(new MockResponse()
                .setBody(emptyJson)
                .setResponseCode(200));

        GeminiDirectChatModel model = new GeminiDirectChatModel("fake-key", "gemini-2.5-flash", baseUrl);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            model.generate(Collections.singletonList(UserMessage.from("Hello")));
        });

        assertTrue(exception.getMessage().contains("no candidates"));
        assertTrue(exception.getMessage().contains("SAFETY"));
    }
}
