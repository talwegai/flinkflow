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

import ai.talweg.flinkflow.core.DynamicAsyncHttpFunction;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.groups.OperatorMetricGroup;
import org.apache.flink.util.Collector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetSocketAddress;
import java.sql.PreparedStatement;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CoreCoverageTest {

    @Mock
    private PreparedStatement mockStmt;

    @Mock
    private RuntimeContext mockRuntimeContext;

    @Mock
    private OperatorMetricGroup mockMetricGroup;

    @Test
    public void testDynamicJdbcStatementBuilder() throws Exception {
        String code = "stmt.setString(1, input.toUpperCase());";
        DynamicJdbcStatementBuilder builder = new DynamicJdbcStatementBuilder(code);
        
        builder.accept(mockStmt, "hello");
        verify(mockStmt).setString(1, "HELLO");
    }

    @Test
    public void testDynamicJoinFunction() throws Exception {
        String code = "return \"Joined: \" + left + \" & \" + right;";
        DynamicJoinFunction joinFunction = new DynamicJoinFunction(code);
        
        when(mockRuntimeContext.getMetricGroup()).thenReturn(mockMetricGroup);
        
        joinFunction.setRuntimeContext(mockRuntimeContext);
        joinFunction.open(new Configuration());
        
        List<String> results = new ArrayList<>();
        Collector<String> stubCollector = new Collector<String>() {
            @Override
            public void collect(String record) {
                results.add(record);
            }
            @Override
            public void close() {}
        };
        
        joinFunction.processElement("A", "B", null, stubCollector);
        
        assertEquals(1, results.size());
        assertEquals("Joined: A & B", results.get(0));
    }

    @Test
    public void testDynamicHttpSinkFunction() throws Exception {
        // Start a local HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        int port = server.getAddress().getPort();
        final String[] receivedBody = {null};
        
        server.createContext("/test", exchange -> {
            receivedBody[0] = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] response = "OK".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();

        try {
            String url = String.format("http://localhost:%d/test", port);
            String urlCode = "return \"" + url + "\";";
            String authCode = "return \"Bearer token123\";";
            
            DynamicHttpSinkFunction sink = new DynamicHttpSinkFunction(urlCode, "POST", authCode);
            
            sink.open(new Configuration());
            sink.invoke("payload", null);
            
            assertEquals("payload", receivedBody[0]);
        } finally {
            server.stop(0);
        }
    }
    @Test
    public void testDynamicCodeFunction() throws Exception {
        String code = "return input.toUpperCase();";
        DynamicCodeFunction mapFunction = new DynamicCodeFunction(code);
        
        when(mockRuntimeContext.getMetricGroup()).thenReturn(mockMetricGroup);
        mapFunction.setRuntimeContext(mockRuntimeContext);
        mapFunction.open(new Configuration());
        
        assertEquals("HELLO", mapFunction.map("hello"));
    }

    @Test
    public void testDynamicFilterFunction() throws Exception {
        String code = "return input.length() > 3;";
        DynamicFilterFunction filterFunction = new DynamicFilterFunction(code);
        
        when(mockRuntimeContext.getMetricGroup()).thenReturn(mockMetricGroup);
        filterFunction.setRuntimeContext(mockRuntimeContext);
        filterFunction.open(new Configuration());
        
        assertTrue(filterFunction.filter("hello"));
        assertFalse(filterFunction.filter("hi"));
    }

    @Test
    public void testDynamicFlatMapFunction() throws Exception {
        String code = "out.collect(input.toUpperCase()); out.collect(input.toLowerCase());";
        DynamicFlatMapFunction flatMapFunction = new DynamicFlatMapFunction(code);
        
        when(mockRuntimeContext.getMetricGroup()).thenReturn(mockMetricGroup);
        flatMapFunction.setRuntimeContext(mockRuntimeContext);
        flatMapFunction.open(new Configuration());
        
        List<String> results = new ArrayList<>();
        Collector<String> stubCollector = new Collector<String>() {
            @Override
            public void collect(String record) {
                results.add(record);
            }
            @Override
            public void close() {}
        };
        
        flatMapFunction.flatMap("Hello", stubCollector);
        
        assertEquals(2, results.size());
        assertEquals("HELLO", results.get(0));
        assertEquals("hello", results.get(1));
    }

    @Test
    public void testDynamicKeySelector() throws Exception {
        String code = "return input.substring(0, 1);";
        DynamicKeySelector keySelector = new DynamicKeySelector(code);
        
        assertEquals("h", keySelector.getKey("hello"));
    }

    @Test
    public void testDynamicReduceFunction() throws Exception {
        String code = "return value1 + \"-\" + value2;";
        DynamicReduceFunction reduceFunction = new DynamicReduceFunction(code);
        
        when(mockRuntimeContext.getMetricGroup()).thenReturn(mockMetricGroup);
        reduceFunction.setRuntimeContext(mockRuntimeContext);
        reduceFunction.open(new Configuration());
        
        assertEquals("a-b", reduceFunction.reduce("a", "b"));
    }
    @Test
    public void testJsonToGenericRecordMapper() throws Exception {
        String schemaStr = "{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"id\",\"type\":\"int\"}]}";
        JsonToGenericRecordMapper mapper = new JsonToGenericRecordMapper(schemaStr);
        org.apache.avro.generic.GenericRecord record = mapper.map("{\"id\": 123}");
        assertNotNull(record);
        assertEquals(123, record.get("id"));
    }

    @Test
    public void testProcessorFactory() {
        assertNotNull(ProcessorFactory.createMapper("return null;"));
        assertNotNull(ProcessorFactory.createFilter("return true;"));
        assertNotNull(ProcessorFactory.createFlatMap("return null;"));
        assertNotNull(ProcessorFactory.createReducer("return null;"));
        assertNotNull(ProcessorFactory.createWindowReducer("return null;"));
        assertNotNull(ProcessorFactory.createSideOutput("return null;", "side"));
        assertNotNull(ProcessorFactory.createJoiner("return null;"));
        assertNotNull(ProcessorFactory.createKeySelector("return null;"));
        assertNotNull(ProcessorFactory.createDataMapper("dummy.xslt"));
        assertNotNull(ProcessorFactory.createAsyncHttpLookup("return null;", "return null;", "return null;"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDynamicAsyncHttpFunction() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        int port = server.getAddress().getPort();
        server.createContext("/async", exchange -> {
            byte[] response = "SUCCESS".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();

        try {
            String url = String.format("http://localhost:%d/async", port);
            String urlCode = "return \"" + url + "\";";
            String responseCode = "return response;";
            String authCode = "return \"Token\";";

            ai.talweg.flinkflow.core.DynamicAsyncHttpFunction func = new ai.talweg.flinkflow.core.DynamicAsyncHttpFunction(urlCode, responseCode, authCode, "java");
            func.open(new Configuration());

            java.util.concurrent.atomic.AtomicReference<String> result = new java.util.concurrent.atomic.AtomicReference<>();
            org.apache.flink.streaming.api.functions.async.ResultFuture<String> mockFuture = mock(org.apache.flink.streaming.api.functions.async.ResultFuture.class);
            doAnswer(invocation -> {
                java.util.Collection<String> col = invocation.getArgument(0);
                result.set(col.iterator().next());
                return null;
            }).when(mockFuture).complete(any());

            func.asyncInvoke("input1", mockFuture);
            
            // Wait for async call to complete
            for (int i = 0; i < 50 && result.get() == null; i++) {
                Thread.sleep(100);
            }
            assertEquals("SUCCESS", result.get());
            
            // test timeout
            func.timeout("hello", mockFuture);
            verify(mockFuture, atLeastOnce()).completeExceptionally(any(RuntimeException.class));
            
        } finally {
            server.stop(0);
        }
    }
    @Test
    public void testJsonToGenericRecordMapperError() throws Exception {
        String schemaStr = "{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"id\",\"type\":\"int\"}]}";
        JsonToGenericRecordMapper mapper = new JsonToGenericRecordMapper(schemaStr);
        assertThrows(RuntimeException.class, () -> mapper.map("invalid json"));
    }

    @Test
    public void testDynamicHttpSinkFunctionPutAndError() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        int port = server.getAddress().getPort();
        server.createContext("/put", exchange -> {
            exchange.sendResponseHeaders(400, -1); // Simulating an error response
        });
        server.start();

        try {
            String url = String.format("http://localhost:%d/put", port);
            String urlCode = "return \"" + url + "\";";
            
            DynamicHttpSinkFunction sinkPut = new DynamicHttpSinkFunction(urlCode, "PUT", null);
            sinkPut.open(new Configuration());
            
            assertThrows(RuntimeException.class, () -> sinkPut.invoke("payload", null));
            
            DynamicHttpSinkFunction sinkInvalid = new DynamicHttpSinkFunction(urlCode, "INVALID_METHOD", null);
            sinkInvalid.open(new Configuration());
            assertThrows(IllegalArgumentException.class, () -> sinkInvalid.invoke("payload", null));
        } finally {
            server.stop(0);
        }
    }
}
