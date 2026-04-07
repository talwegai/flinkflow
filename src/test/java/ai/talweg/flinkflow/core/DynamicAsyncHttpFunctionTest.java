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

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.CloseableIterator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link DynamicAsyncHttpFunction}.
 *
 * An embedded {@link MockWebServer} (OkHttp) intercepts the Java HttpClient
 * calls so no real network traffic is generated.  Each test:
 *   1. Enqueues a scripted HTTP response on the mock server.
 *   2. Builds a dynamic URL snippet that points to the mock server's URL.
 *   3. Runs a mini Flink job via AsyncDataStream.unorderedWait().
 *   4. Asserts the results after collect().
 */
public class DynamicAsyncHttpFunctionTest {

    private MockWebServer mockServer;
    private String baseUrl;

    @BeforeEach
    void startServer() throws Exception {
        mockServer = new MockWebServer();
        mockServer.start();
        // e.g. "http://localhost:54321"
        baseUrl = mockServer.url("/").toString().replaceAll("/$", "");
    }

    @AfterEach
    void stopServer() throws Exception {
        mockServer.shutdown();
    }

    private List<String> collect(DataStream<String> stream, String jobName) throws Exception {
        List<String> results = new ArrayList<>();
        try (CloseableIterator<String> it = stream.executeAndCollect(jobName)) {
            it.forEachRemaining(results::add);
        }
        return results;
    }

    // ------------------------------------------------------------------ //
    // Test 1 — Basic GET: URL built from input, response body returned   //
    // ------------------------------------------------------------------ //

    @Test
    public void testBasicGetReturnsResponseBody() throws Exception {
        mockServer.enqueue(new MockResponse()
                .setBody("enriched:hello")
                .setResponseCode(200));

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        // URL code: point every request at our mock server
        String urlCode     = "return \"" + baseUrl + "/enrich?value=\" + input;";
        // Response code: just return the raw body
        String responseCode = "return response;";
        String authCode    = "return null;";

        DynamicAsyncHttpFunction fn = ProcessorFactory.createAsyncHttpLookup(urlCode, responseCode, authCode);

        DataStream<String> source = env.fromElements("hello");
        DataStream<String> result = AsyncDataStream.unorderedWait(source, fn, 10, TimeUnit.SECONDS, 10);

        List<String> results = collect(result, "Async HTTP Basic Test");

        assertEquals(1, results.size());
        assertEquals("enriched:hello", results.get(0));

        // Also verify the mock server received the right URL
        RecordedRequest request = mockServer.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);
        assertTrue(request.getPath().contains("/enrich?value=hello"));
    }

    // ------------------------------------------------------------------ //
    // Test 2 — Auth header is passed when authCode returns a token        //
    // ------------------------------------------------------------------ //

    @Test
    public void testAuthHeaderIsSentWhenProvided() throws Exception {
        mockServer.enqueue(new MockResponse()
                .setBody("secure-data")
                .setResponseCode(200));

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        String urlCode      = "return \"" + baseUrl + "/secure\";";
        String responseCode = "return response;";
        String authCode     = "return \"Bearer test-token-123\";";

        DynamicAsyncHttpFunction fn = ProcessorFactory.createAsyncHttpLookup(urlCode, responseCode, authCode);

        DataStream<String> source = env.fromElements("payload");
        DataStream<String> result = AsyncDataStream.unorderedWait(source, fn, 10, TimeUnit.SECONDS, 10);

        collect(result, "Async HTTP Auth Test");

        RecordedRequest request = mockServer.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("Bearer test-token-123", request.getHeader("Authorization"),
                "Authorization header should be forwarded");
    }

    // ------------------------------------------------------------------ //
    // Test 3 — Response body merging: enrich input with HTTP response     //
    // ------------------------------------------------------------------ //

    @Test
    public void testResponseCodeCanMergeInputAndBody() throws Exception {
        mockServer.enqueue(new MockResponse()
                .setBody("42")
                .setResponseCode(200));

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        String urlCode      = "return \"" + baseUrl + "/score\";";
        // Response code merges input + HTTP body
        String responseCode = "return input + \":score=\" + response;";
        String authCode     = "return null;";

        DynamicAsyncHttpFunction fn = ProcessorFactory.createAsyncHttpLookup(urlCode, responseCode, authCode);

        DataStream<String> source = env.fromElements("user-007");
        DataStream<String> result = AsyncDataStream.unorderedWait(source, fn, 10, TimeUnit.SECONDS, 10);

        List<String> results = collect(result, "Async HTTP Merge Test");

        assertEquals(1, results.size());
        assertEquals("user-007:score=42", results.get(0),
                "Response handler should merge input and HTTP body");
    }

    // ------------------------------------------------------------------ //
    // Test 4 — Multiple elements are processed independently              //
    // ------------------------------------------------------------------ //

    @Test
    public void testMultipleElementsEnriched() throws Exception {
        // Enqueue a response for each element
        mockServer.enqueue(new MockResponse().setBody("alpha-enriched").setResponseCode(200));
        mockServer.enqueue(new MockResponse().setBody("beta-enriched").setResponseCode(200));

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        String urlCode      = "return \"" + baseUrl + "/lookup?q=\" + input;";
        String responseCode = "return response;";
        String authCode     = "return null;";

        DynamicAsyncHttpFunction fn = ProcessorFactory.createAsyncHttpLookup(urlCode, responseCode, authCode);

        DataStream<String> source = env.fromElements("alpha", "beta");
        DataStream<String> result = AsyncDataStream.unorderedWait(source, fn, 10, TimeUnit.SECONDS, 10);

        List<String> results = collect(result, "Async HTTP Multi-Element Test");

        assertEquals(2, results.size());
        assertTrue(results.contains("alpha-enriched"), "alpha should be enriched");
        assertTrue(results.contains("beta-enriched"),  "beta should be enriched");
    }
}
