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

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.CloseableIterator;
import org.apache.flink.util.OutputTag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProcessorFactoryTest {

    private List<String> collectStream(DataStream<String> stream, String jobName) throws Exception {
        List<String> results = new ArrayList<>();
        try (CloseableIterator<String> it = stream.executeAndCollect(jobName)) {
            it.forEachRemaining(results::add);
        }
        return results;
    }

    @Test
    public void testDynamicMapper() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        DataStream<String> stream = env.fromElements("hello", "world")
                .map(ProcessorFactory.createMapper(
                        "metrics.counter(\"testCounter\").inc();\n" +
                        "return input.toUpperCase();"
                ));

        List<String> results = collectStream(stream, "Mapper Test");
        assertEquals(2, results.size());
        assertEquals("HELLO", results.get(0));
        assertEquals("WORLD", results.get(1));
    }

    @Test
    public void testDynamicFilter() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        DataStream<String> stream = env.fromElements("apple", "banana", "cat")
                .filter(ProcessorFactory.createFilter("return input.length() > 3;"));

        List<String> results = collectStream(stream, "Filter Test");
        assertEquals(2, results.size());
        assertEquals("apple", results.get(0));
        assertEquals("banana", results.get(1));
    }

    @Test
    public void testDynamicFlatMap() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        DataStream<String> stream = env.fromElements("a b", "c d")
                .flatMap(ProcessorFactory.createFlatMap(
                        "for (String part : input.split(\" \")) { out.collect(part); }"
                ));

        List<String> results = collectStream(stream, "FlatMap Test");
        assertEquals(4, results.size());
        assertEquals("a", results.get(0));
        assertEquals("b", results.get(1));
        assertEquals("c", results.get(2));
        assertEquals("d", results.get(3));
    }

    @Test
    public void testDynamicKeySelectorAndReducer() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        DataStream<String> stream = env.fromElements("fruit,apple", "fruit,banana", "car,tesla")
                .keyBy(ProcessorFactory.createKeySelector("return input.split(\",\")[0];"))
                .reduce(ProcessorFactory.createReducer(
                        "String key = value1.split(\",\")[0];\n" +
                        "String v1 = value1.split(\",\")[1];\n" +
                        "String v2 = value2.split(\",\")[1];\n" +
                        "return key + \",\" + v1 + \"+\" + v2;"
                ));

        List<String> results = collectStream(stream, "KeyBy Reduce Test");
        assertEquals(3, results.size());
        assertTrue(results.contains("fruit,apple"));
        assertTrue(results.contains("fruit,apple+banana"));
        assertTrue(results.contains("car,tesla"));
    }

    @Test
    public void testDynamicSideOutput() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        SingleOutputStreamOperator<String> stream = env.fromElements("INFO: login", "ERROR: failed")
                .process(ProcessorFactory.createSideOutput(
                        "if (input.startsWith(\"ERROR\")) {\n" +
                        "    ctx.output(input);\n" +
                        "} else {\n" +
                        "    out.collect(input);\n" +
                        "}",
                        "dlq"
                ));

        OutputTag<String> dlqTag = new OutputTag<String>("dlq"){};
        DataStream<String> sideOutput = stream.getSideOutput(dlqTag);

        List<String> sideResults = collectStream(sideOutput, "Side Output Test");
        assertEquals(1, sideResults.size());
        assertEquals("ERROR: failed", sideResults.get(0));
    }

    @Test
    public void testFactoryLanguageBranches() {
        // Just verify that using different languages correctly triggers the switch statements
        // without throwing unsupported language exceptions and constructs the appropriate wrapper.

        // Python
        assertTrue(ProcessorFactory.createMapper("pass", "python") != null);
        assertTrue(ProcessorFactory.createFilter("pass", "python") != null);
        assertTrue(ProcessorFactory.createFlatMap("pass", "python") != null);
        assertTrue(ProcessorFactory.createKeySelector("pass", "python") != null);
        assertTrue(ProcessorFactory.createReducer("pass", "python") != null);
        assertTrue(ProcessorFactory.createWindowReducer("pass", "python") != null);
        assertTrue(ProcessorFactory.createSideOutput("pass", "dlq", "python") != null);
        assertTrue(ProcessorFactory.createJoiner("pass", "python") != null);

        // Camel sub-types
        assertTrue(ProcessorFactory.createMapper("pass", "camel-simple") != null);
        assertTrue(ProcessorFactory.createFilter("pass", "camel-jsonpath") != null);
        assertTrue(ProcessorFactory.createFlatMap("pass", "camel-groovy") != null);
        assertTrue(ProcessorFactory.createKeySelector("pass", "jsonpath") != null);
        assertTrue(ProcessorFactory.createReducer("pass", "groovy") != null);
        assertTrue(ProcessorFactory.createWindowReducer("pass", "camel-simple") != null);

        // Camel-YAML
        assertTrue(ProcessorFactory.createMapper("- set-body: constant: hello", "camel-yaml") != null);
        assertTrue(ProcessorFactory.createFilter("- simple: '${body} == 1'", "camel-yaml") != null);
        assertTrue(ProcessorFactory.createFlatMap("- split: { simple: '${body}' }", "camel-yaml") != null);
        assertTrue(ProcessorFactory.createKeySelector("- set-body: constant: key", "camel-yaml") != null);
    }

    @Test
    public void testCreateAdditionalProcessors() {
        // DataMapper
        assertTrue(ProcessorFactory.createDataMapper("dummy.xslt") != null);
        
        // Async HTTP
        assertTrue(ProcessorFactory.createAsyncHttpLookup("url", "res", "auth", "java") != null);
        assertTrue(ProcessorFactory.createAsyncHttpLookup("url", "res", "auth") != null);
        
        // Agent
        assertTrue(ProcessorFactory.createAgent("test-agent", "gpt-4", "test system prompt", true, new java.util.HashMap<>(), new java.util.HashMap<>()) != null);
    }
}
