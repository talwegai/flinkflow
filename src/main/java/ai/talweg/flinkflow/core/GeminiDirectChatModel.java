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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * A lightweight {@link ChatLanguageModel} that calls the Google Gemini REST API
 * directly using the {@code v1} endpoint (not {@code v1beta}).
 *
 * <p>This bypasses LangChain4j's built-in Gemini module (which hardcodes {@code v1beta})
 * and provides stable access to all current production Gemini models:
 * {@code gemini-2.5-flash}, {@code gemini-2.0-flash}, {@code gemini-1.5-pro}, etc.
 *
 * <p>Requires a Google AI Studio API key: https://aistudio.google.com/apikey
 */
public class GeminiDirectChatModel implements ChatLanguageModel {

    private static final Logger LOG = LoggerFactory.getLogger(GeminiDirectChatModel.class);
    // Google AI Studio developer keys use v1beta; v1 is for certain production access patterns
    private static final String BASE_URL_V1BETA = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String BASE_URL_V1 = "https://generativelanguage.googleapis.com/v1/models/";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String apiKey;
    private final String modelName;
    private final String baseUrl;
    private final HttpClient httpClient;

    /** Defaults to v1beta — the primary endpoint for Google AI Studio API keys. */
    public GeminiDirectChatModel(String apiKey, String modelName) {
        this(apiKey, modelName, BASE_URL_V1BETA);
    }

    public GeminiDirectChatModel(String apiKey, String modelName, String baseUrl) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Gemini API key is missing. Please set the GOOGLE_API_KEY environment variable.");
        }
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        try {
            String url = baseUrl + modelName + ":generateContent?key=" + apiKey;

            RequestBody requestBody = buildRequestBody(messages);
            String jsonRequest = MAPPER.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Gemini API error (" + response.statusCode() + "): " + response.body());
            }

            JsonNode root = MAPPER.readTree(response.body());
            JsonNode candidates = root.path("candidates");
            
            if (candidates.isMissingNode() || candidates.isEmpty()) {
                String reason = root.path("promptFeedback").path("blockReason").asText("UNKNOWN");
                throw new RuntimeException("Gemini returned no candidates. Block reason: " + reason);
            }

            JsonNode firstCandidate = candidates.get(0);
            JsonNode parts = firstCandidate.path("content").path("parts");
            
            if (parts.isMissingNode() || parts.isEmpty()) {
                throw new RuntimeException("Gemini response is empty. Finish reason: " 
                        + firstCandidate.path("finishReason").asText("UNKNOWN"));
            }

            String responseText = parts.get(0).path("text").asText();
            return Response.from(AiMessage.from(responseText));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Gemini call interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException("Gemini call failed: " + e.getMessage(), e);
        }
    }

    private RequestBody buildRequestBody(List<ChatMessage> messages) {
        List<Content> contents = new java.util.ArrayList<>();
        Content systemInstruction = null;

        for (ChatMessage msg : messages) {
            if (msg instanceof dev.langchain4j.data.message.SystemMessage) {
                systemInstruction = new Content(msg.text());
            } else if (msg instanceof dev.langchain4j.data.message.UserMessage) {
                contents.add(new Content("user", msg.text()));
            } else if (msg instanceof dev.langchain4j.data.message.AiMessage) {
                contents.add(new Content("model", msg.text()));
            }
        }
        
        return new RequestBody(contents, systemInstruction);
    }

    // -----------------------------------------------------------------------
    // Internal request/response models
    // -----------------------------------------------------------------------

    static class RequestBody {
        public final List<Content> contents;
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        public final Content system_instruction;
        RequestBody(List<Content> contents, Content systemInstruction) {
            this.contents = contents;
            this.system_instruction = systemInstruction;
        }
    }

    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    static class Content {
        public final String role;
        public final List<Part> parts;
        Content(String text) { this(null, text); }
        Content(String role, String text) {
            this.role = role;
            this.parts = List.of(new Part(text));
        }
    }

    static class Part {
        public final String text;
        Part(String text) { this.text = text; }
    }
}
