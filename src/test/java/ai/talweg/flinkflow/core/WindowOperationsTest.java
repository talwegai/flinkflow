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

import ai.talweg.flinkflow.FlinkflowApp;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows;
import java.time.Duration;
import org.apache.flink.streaming.api.windowing.triggers.CountTrigger;
import org.apache.flink.util.CloseableIterator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for windowed pipeline operations.
 *
 * Processing-time windows only fire when wall-clock time passes the boundary,
 * which is non-deterministic in a bounded test.  We therefore test in two ways:
 *
 * 1. DAG-construction tests: apply the real window assigner but override the
 *    trigger to CountTrigger so the window fires deterministically after N
 *    elements rather than waiting for time to advance.  This verifies the
 *    reduce logic (Janino compilation + correct merge semantics) without
 *    relying on wall-clock timing.
 *
 * 2. YAML integration tests: call {@link FlinkflowApp#main(String[])} with a
 *    pipeline YAML that contains a window step, asserting the job runs without
 *    exception (DAG and configuration parsing are valid).
 */
public class WindowOperationsTest {

    private List<String> collect(DataStream<String> stream, String name) throws Exception {
        List<String> results = new ArrayList<>();
        try (CloseableIterator<String> it = stream.executeAndCollect(name)) {
            it.forEachRemaining(results::add);
        }
        return results;
    }

    // ------------------------------------------------------------------ //
    // Test 1 — Tumbling window: reduce fires after N elements              //
    // ------------------------------------------------------------------ //

    @Test
    public void testTumblingWindowReduceFiresAfterCount() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        // Elements: key,value pairs — we group by key and concatenate values
        DataStream<String> source = env.fromElements(
                "fruit,apple",
                "fruit,banana",   // <- window of 2 fires here for "fruit"
                "car,tesla"
        );

        KeyedStream<String, String> keyed = source
                .keyBy(ProcessorFactory.createKeySelector("return input.split(\",\")[0];"));

        // Use a CountTrigger(2): fires as soon as 2 elements accumulate in the window
        String reduceCode =
                "String key = value1.split(\",\")[0];\n" +
                "String v1  = value1.split(\",\")[1];\n" +
                "String v2  = value2.split(\",\")[1];\n" +
                "return key + \",\" + v1 + \"+\" + v2;";

        DataStream<String> windowed = keyed
                .window(TumblingProcessingTimeWindows.of(Duration.ofSeconds(60)))
                .trigger(CountTrigger.of(2))  // fire every 2 elements — deterministic
                .reduce(ProcessorFactory.createWindowReducer(reduceCode));

        List<String> results = collect(windowed, "Tumbling Window Test");

        // Only the "fruit" bucket should have fired (2 elements)
        assertEquals(1, results.size());
        assertEquals("fruit,apple+banana", results.get(0));
    }

    // ------------------------------------------------------------------ //
    // Test 2 — Sliding window: reduce fires on every slide                //
    // ------------------------------------------------------------------ //

    @Test
    public void testSlidingWindowReduceFiresAfterCount() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        DataStream<String> source = env.fromElements(
                "grp,1", "grp,2", "grp,3"
        );

        KeyedStream<String, String> keyed = source
                .keyBy(ProcessorFactory.createKeySelector("return input.split(\",\")[0];"));

        String reduceCode =
                "String key = value1.split(\",\")[0];\n" +
                "int sum = Integer.parseInt(value1.split(\",\")[1]) "
                        + "      + Integer.parseInt(value2.split(\",\")[1]);\n" +
                "return key + \",\" + sum;";

        // Sliding window, forced to fire after every 2 elements via CountTrigger
        DataStream<String> windowed = keyed
                .window(SlidingProcessingTimeWindows.of(Duration.ofSeconds(60), Duration.ofSeconds(30)))
                .trigger(CountTrigger.of(2))
                .reduce(ProcessorFactory.createWindowReducer(reduceCode));

        List<String> results = collect(windowed, "Sliding Window Test");

        // At least one window fires: grp,1 + grp,2 = grp,3
        assertFalse(results.isEmpty(), "At least one sliding window should have fired");
        assertTrue(results.stream().allMatch(r -> r.startsWith("grp,")),
                "All results should be in the 'grp' key group");
    }

    // ------------------------------------------------------------------ //
    // Test 3 — Window step preceded by non-keyed stream throws             //
    // ------------------------------------------------------------------ //

    @Test
    public void testWindowWithoutKeyByThrowsViaYaml(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("bad-window-pipeline.yaml").toFile();
        try (FileWriter w = new FileWriter(configFile)) {
            w.write("name: 'Bad Window Pipeline'\n");
            w.write("parallelism: 1\n");
            w.write("steps:\n");
            w.write("  - type: source\n");
            w.write("    name: static-source\n");
            w.write("    properties:\n");
            w.write("      content: 'a|b|c'\n");
            // Jump straight to window without a keyby — should throw
            w.write("  - type: window\n");
            w.write("    name: bad-window\n");
            w.write("    properties:\n");
            w.write("      windowType: tumbling\n");
            w.write("      size: '5'\n");
            w.write("    code: 'return value1 + value2;'\n");
            w.write("  - type: sink\n");
            w.write("    name: console-sink\n");
            w.write("    properties:\n");
            w.write("      type: console\n");
        }

        // Should fail because stream is not keyed before window step
        assertThrows(Exception.class, () -> {
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath()});
        });
    }

    // ------------------------------------------------------------------ //
    // Test 4 — Full YAML pipeline with keyby + tumbling window is valid   //
    // ------------------------------------------------------------------ //

    @Test
    public void testFullYamlWindowPipelineParsesWithoutError(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("window-pipeline.yaml").toFile();
        try (FileWriter w = new FileWriter(configFile)) {
            w.write("name: 'Window Pipeline'\n");
            w.write("parallelism: 1\n");
            w.write("steps:\n");
            // Source: static stream of key,value lines
            w.write("  - type: source\n");
            w.write("    name: static-source\n");
            w.write("    properties:\n");
            w.write("      content: 'k,1|k,2|k,3'\n");
            // KeyBy on the key part
            w.write("  - type: keyby\n");
            w.write("    name: group-by-key\n");
            w.write("    code: 'return input.split(\",\")[0];'\n");
            // Tumbling window - uses processing time; in local mini-cluster
            // the window may not fire (no elements emitted is valid)
            w.write("  - type: window\n");
            w.write("    name: tumbling-5s\n");
            w.write("    properties:\n");
            w.write("      windowType: tumbling\n");
            w.write("      size: '5'\n");
            w.write("    code: |\n");
            w.write("      return value1 + \"+\" + value2;\n");
            w.write("  - type: sink\n");
            w.write("    name: console-sink\n");
            w.write("    properties:\n");
            w.write("      type: console\n");
        }

        // The job should build and execute without any exception
        // (window may yield 0 results in bounded mode — that's expected)
        assertDoesNotThrow(() -> {
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath()});
        });
    }

    // ------------------------------------------------------------------ //
    // Test 5 — Unknown window type throws via YAML                        //
    // ------------------------------------------------------------------ //

    @Test
    public void testUnknownWindowTypeThrowsViaYaml(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("unknown-window.yaml").toFile();
        try (FileWriter w = new FileWriter(configFile)) {
            w.write("name: 'Unknown Window'\n");
            w.write("parallelism: 1\n");
            w.write("steps:\n");
            w.write("  - type: source\n");
            w.write("    name: static-source\n");
            w.write("    properties:\n");
            w.write("      content: 'k,1|k,2'\n");
            w.write("  - type: keyby\n");
            w.write("    name: group-by-key\n");
            w.write("    code: 'return input.split(\",\")[0];'\n");
            w.write("  - type: window\n");
            w.write("    name: unknown-window\n");
            w.write("    properties:\n");
            w.write("      windowType: foobar\n");   // unknown type
            w.write("      size: '5'\n");
            w.write("    code: 'return value1;'\n");
            w.write("  - type: sink\n");
            w.write("    name: console-sink\n");
            w.write("    properties:\n");
            w.write("      type: console\n");
        }

        assertThrows(Exception.class, () -> {
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath()});
        });
    }
}
