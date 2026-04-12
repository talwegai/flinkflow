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

import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import org.apache.flink.api.common.functions.OpenContext;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PythonParityTest {

    @Test
    public void testPythonKeyBy() throws Exception {
        String code = "import json\nobj = json.loads(input)\nreturn obj['category']";
        KeySelector<String, String> selector = new DynamicPythonKeySelector(code);
        
        String input = "{\"category\": \"tech\", \"val\": 10}";
        assertEquals("tech", selector.getKey(input));
    }

    @Test
    public void testPythonReduce() throws Exception {
        String code = "import json\no1 = json.loads(value1)\no2 = json.loads(value2)\nreturn json.dumps({'total': o1['val'] + o2['val']})";
        ReduceFunction<String> reducer = new DynamicPythonReduceFunction(code);
        ((DynamicPythonReduceFunction)reducer).open((OpenContext) null);
        
        String v1 = "{\"val\": 10}";
        String v2 = "{\"val\": 25}";
        String result = reducer.reduce(v1, v2);
        assertEquals("{\"total\": 35}", result);
    }

    @Test
    public void testPythonSideOutput() throws Exception {
        String code = "if 'error' in input:\n    side_emit(input)\n    return None\nelse:\n    return input.upper()";
        DynamicPythonSideOutputFunction function = new DynamicPythonSideOutputFunction(code, "errors");
        function.open((OpenContext) null);
        
        List<String> mainOut = new ArrayList<>();
        List<String> sideOut = new ArrayList<>();
        
        // Mock Collector
        org.apache.flink.util.Collector<String> collector = new org.apache.flink.util.Collector<String>() {
            @Override public void collect(String record) { mainOut.add(record); }
            @Override public void close() {}
        };
        
        // Mock Context
        org.apache.flink.streaming.api.functions.ProcessFunction<String, String>.Context ctx = 
            new org.apache.flink.streaming.api.functions.ProcessFunction<String, String>() {
                @Override public void processElement(String v, Context c, org.apache.flink.util.Collector<String> o) {}
            }.new Context() {
            @Override public Long timestamp() { return null; }
            @Override public <X> void output(org.apache.flink.util.OutputTag<X> tag, X value) {
                if (tag.getId().equals("errors")) sideOut.add((String)value);
            }
            @Override public org.apache.flink.streaming.api.TimerService timerService() { return null; }
        };
        
        function.processElement("hello", ctx, collector);
        function.processElement("error-msg", ctx, collector);
        
        assertEquals(1, mainOut.size());
        assertEquals("HELLO", mainOut.get(0));
        assertEquals(1, sideOut.size());
        assertEquals("error-msg", sideOut.get(0));
    }

    @Test
    public void testPythonFilter() throws Exception {
        String code = "return 'skip' not in input";
        DynamicPythonFilterFunction function = new DynamicPythonFilterFunction(code);
        function.open((OpenContext) null);
        
        assertEquals(true, function.filter("hello"));
        assertEquals(false, function.filter("skip-this"));
    }

    @Test
    public void testPythonFlatMap() throws Exception {
        String code = "if '-' in input:\n    return input.split('-')\nelse:\n    return input.upper()";
        DynamicPythonFlatMapFunction function = new DynamicPythonFlatMapFunction(code);
        function.open((OpenContext) null);
        
        List<String> results = new ArrayList<>();
        org.apache.flink.util.Collector<String> collector = new org.apache.flink.util.Collector<String>() {
            @Override public void collect(String record) { results.add(record); }
            @Override public void close() {}
        };
        
        // Path 1: Array Return
        function.flatMap("a-b", collector);
        assertEquals(2, results.size());
        assertEquals("a", results.get(0));
        assertEquals("b", results.get(1));
        
        // Path 2: Single Element Fallback
        results.clear();
        function.flatMap("hello", collector);
        assertEquals(1, results.size());
        assertEquals("HELLO", results.get(0));
    }

    @Test
    public void testPythonFlatMapUninitialised() {
        DynamicPythonFlatMapFunction function = new DynamicPythonFlatMapFunction("return input");
        // Skipping function.open(null) intentionally
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            function.flatMap("test", null);
        }, "Python flatmap function not initialised");
    }

    @Test
    public void testPythonProcess() throws Exception {
        String code = "return input.upper()";
        DynamicPythonMapFunction function = new DynamicPythonMapFunction(code);
        function.open((OpenContext) null);
        
        assertEquals("HELLO", function.map("hello"));
    }

    @Test
    public void testPythonJoin() throws Exception {
        String code = "return f'{left}-{right}'";
        DynamicPythonJoinFunction function = new DynamicPythonJoinFunction(code);
        function.open((OpenContext) null);
        
        List<String> results = new ArrayList<>();
        org.apache.flink.util.Collector<String> collector = new org.apache.flink.util.Collector<String>() {
            @Override public void collect(String record) { results.add(record); }
            @Override public void close() {}
        };
        
        function.processElement("A", "1", null, collector);
        assertEquals("A-1", results.get(0));
    }

    @Test
    public void testPythonWindowReduce() throws Exception {
        String code = "return f'{value1}:{value2}'";
        DynamicPythonWindowReduceFunction function = new DynamicPythonWindowReduceFunction(code);
        
        assertEquals("A:B", function.reduce("A", "B"));
        assertEquals("A:B:C", function.reduce("A:B", "C"));
    }

    @Test
    public void testPythonSandboxSecurity() throws Exception {
        // 1. Attempt to access Java classes - should fail because allowHostClassLookup is false
        String javaCode = "import java\nreturn java.lang.System.getProperty('java.version')";
        DynamicPythonMapFunction javaFunction = new DynamicPythonMapFunction(javaCode);
        javaFunction.open((OpenContext) null);
        
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            javaFunction.map("test");
        }, "Should block access to Java classes");

        // 2. Attempt to read from file system - should fail because allowIO is false
        String ioCode = "with open('/etc/hosts', 'r') as f: return f.read()";
        DynamicPythonMapFunction ioFunction = new DynamicPythonMapFunction(ioCode);
        ioFunction.open((OpenContext) null);

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            ioFunction.map("test");
        }, "Should block access to file system");
    }

    @Test
    public void testPythonFunctionsLifecycleAndUninitialized() throws Exception {
        // Test MapFunction uninitialized
        DynamicPythonMapFunction mapFunction = new DynamicPythonMapFunction("return input");
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> mapFunction.map("test"));
        mapFunction.close(); // closing uninitialized

        mapFunction.open((OpenContext) null);
        mapFunction.close(); // closing initialized

        // Test FilterFunction uninitialized
        DynamicPythonFilterFunction filterFunction = new DynamicPythonFilterFunction("return True");
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> filterFunction.filter("test"));
        filterFunction.close();
        filterFunction.open((OpenContext) null);
        filterFunction.close();

        // Test FlatMapFunction uninitialized & close
        DynamicPythonFlatMapFunction flatMapFunction = new DynamicPythonFlatMapFunction("return input");
        flatMapFunction.close(); // close uninitialized
        flatMapFunction.open((OpenContext) null);
        flatMapFunction.close(); // close initialized

        // Test SideOutputFunction uninitialized
        DynamicPythonSideOutputFunction sideOutput = new DynamicPythonSideOutputFunction("return input", "tag");
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> sideOutput.processElement("test", null, null));
        sideOutput.close();
        sideOutput.open((OpenContext) null);
        sideOutput.close();

        // Test ReduceFunction uninitialized
        DynamicPythonReduceFunction reduceConfig = new DynamicPythonReduceFunction("return value1");
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> reduceConfig.reduce("v1", "v2"));
        reduceConfig.close();
        reduceConfig.open((OpenContext) null);
        reduceConfig.close();

        // Test JoinFunction uninitialized
        DynamicPythonJoinFunction joinFunction = new DynamicPythonJoinFunction("return left");
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> joinFunction.processElement("A", "B", null, null));
        joinFunction.close();
        joinFunction.open((OpenContext) null);
        joinFunction.close();

        // Test WindowReduce uninitialized
        DynamicPythonWindowReduceFunction windowReduce = new DynamicPythonWindowReduceFunction("return value1");
        org.junit.jupiter.api.Assertions.assertEquals("v1", windowReduce.reduce("v1", "v2")); 
        // KeySelector initializes itself if missing
        DynamicPythonKeySelector keySelector = new DynamicPythonKeySelector("return input");
        org.junit.jupiter.api.Assertions.assertEquals("test", keySelector.getKey("test")); 
    }
}
