package ai.talweg.flinkflow.core;

import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.junit.jupiter.api.Test;
import org.apache.flink.api.common.functions.OpenContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CamelFunctionParityTest {

    @Test
    public void testCamelSimpleMap() throws Exception {
        DynamicCamelMapFunction function = new DynamicCamelMapFunction("${body.toUpperCase()}", "simple");
        function.open((OpenContext) null);
        String result = function.map("hello");
        assertEquals("HELLO", result);
        function.close();
    }

    @Test
    public void testCamelJsonPathMap() throws Exception {
        DynamicCamelMapFunction function = new DynamicCamelMapFunction("$.user.name", "jsonpath");
        function.open((OpenContext) null);
        String result = function.map("{\"user\": {\"name\": \"Alice\"}}");
        assertEquals("Alice", result);
        function.close();
    }

    @Test
    public void testCamelSimpleFilter() throws Exception {
        DynamicCamelFilterFunction function = new DynamicCamelFilterFunction("${body} contains 'Apple'", "simple");
        function.open((OpenContext) null);
        assertTrue(function.filter("Apple Pie"));
        function.close();
    }

    @Test
    public void testCamelSimpleKeySelector() throws Exception {
        DynamicCamelKeySelector selector = new DynamicCamelKeySelector("${body}", "simple");
        String result = selector.getKey("key-val");
        assertEquals("key-val", result);
        selector.close();
    }

    @Test
    public void testCamelSimpleFlatMap() throws Exception {
        DynamicCamelFlatMapFunction function = new DynamicCamelFlatMapFunction("${body}", "simple");
        function.open((OpenContext) null);
        List<String> results = new ArrayList<>();
        org.apache.flink.util.Collector<String> collector = new org.apache.flink.util.Collector<String>() {
            @Override public void collect(String record) { results.add(record); }
            @Override public void close() {}
        };
        function.flatMap("A", collector);
        assertEquals(1, results.size());
        assertEquals("A", results.get(0));
        function.close();
    }

    @Test
    public void testCamelSimpleReduce() throws Exception {
        DynamicCamelReduceFunction function = new DynamicCamelReduceFunction("${header.value1} + ${header.value2}", "simple");
        String result = function.reduce("10", "20");
        assertEquals("10 + 20", result); // simple language concatenation behavior
        function.close();
    }

    @Test
    public void testCamelYamlMap() throws Exception {
        String yaml = "- route:\n    from:\n      uri: \"direct:start\"\n      steps:\n        - setBody:\n            simple: \"${body.toUpperCase()}\"";
        DynamicCamelYamlMapFunction function = new DynamicCamelYamlMapFunction(yaml);
        function.open((OpenContext) null);
        String result = function.map("yaml");
        assertEquals("YAML", result);
        function.close();
    }

    @Test
    public void testCamelYamlFilter() throws Exception {
        String yaml = "- route:\n    from:\n      uri: \"direct:start\"\n      steps:\n        - filter:\n            simple: \"${body} contains 'OK'\"\n            steps:\n              - setBody:\n                  constant: \"true\"";
        DynamicCamelYamlFilterFunction function = new DynamicCamelYamlFilterFunction(yaml);
        function.open((OpenContext) null);
        assertTrue(function.filter("Everything is OK"));
        function.close();
    }

    @Test
    public void testCamelYamlKeySelector() throws Exception {
        String yaml = "- route:\n    from:\n      uri: \"direct:start\"\n      steps:\n        - setBody:\n            simple: \"${body}ID\"";
        DynamicCamelYamlKeySelector selector = new DynamicCamelYamlKeySelector(yaml);
        String result = selector.getKey("test");
        assertEquals("testID", result);
        selector.close();
    }

    @Test
    public void testCamelYamlFlatMap() throws Exception {
        String yaml = "- route:\n    from:\n      uri: \"direct:start\"\n      steps:\n        - setBody:\n            simple: \"${body.toUpperCase()}\"";
        DynamicCamelYamlFlatMapFunction function = new DynamicCamelYamlFlatMapFunction(yaml);
        function.open((OpenContext) null);
        List<String> results = new ArrayList<>();
        org.apache.flink.util.Collector<String> collector = new org.apache.flink.util.Collector<String>() {
            @Override public void collect(String record) { results.add(record); }
            @Override public void close() {}
        };
        function.flatMap("b", collector);
        assertEquals(1, results.size());
        assertEquals("B", results.get(0));
        function.close();
    }
}
