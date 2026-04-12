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

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.CloseableIterator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.apache.flink.api.common.functions.OpenContext;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link DataMapperFunction}: verifies that XSLT 3.0
 * stylesheets are correctly loaded, compiled, and applied to streaming String
 * elements via the Saxon-HE processor.
 *
 * The test XSLT fixtures live under src/test/resources/ and are resolved from
 * the classpath so the tests are portable (no hard-coded absolute paths).
 */
public class DataMapperFunctionTest {

    /** Resolved absolute filesystem path to the test XSLT resources directory. */
    private static String xsltDir;

    @BeforeAll
    static void resolveXsltDir() {
        // Locate one of the test XSLTs on the classpath so we can derive the
        // directory, since DataMapperFunction expects a file:// path.
        URL resource = DataMapperFunctionTest.class.getClassLoader()
                .getResource("test-uppercase.xslt");
        assertNotNull(resource, "test-uppercase.xslt not found on classpath");
        // Strip the filename — we just need the parent directory.
        xsltDir = resource.getFile().replace("test-uppercase.xslt", "");
    }

    private List<String> collectStream(DataStream<String> stream, String jobName) throws Exception {
        List<String> results = new ArrayList<>();
        try (CloseableIterator<String> it = stream.executeAndCollect(jobName)) {
            it.forEachRemaining(results::add);
        }
        return results;
    }

    // ------------------------------------------------------------------ //
    // Test 1 — Simple uppercase transform                                 //
    // ------------------------------------------------------------------ //

    @Test
    public void testUppercaseXsltTransform() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        String xsltPath = xsltDir + "test-uppercase.xslt";
        MapFunction<String, String> mapper = ProcessorFactory.createDataMapper(xsltPath);

        DataStream<String> stream = env.fromElements("hello", "world")
                .map(mapper);

        List<String> results = collectStream(stream, "XSLT Uppercase Test");

        assertEquals(2, results.size());
        assertEquals("HELLO", results.get(0), "XSLT should uppercase the string");
        assertEquals("WORLD", results.get(1), "XSLT should uppercase the string");
    }

    // ------------------------------------------------------------------ //
    // Test 2 — Conditional XSLT: tag strings that contain "error"        //
    // ------------------------------------------------------------------ //

    @Test
    public void testConditionalXsltTransform() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        String xsltPath = xsltDir + "test-filter-errors.xslt";
        MapFunction<String, String> mapper = ProcessorFactory.createDataMapper(xsltPath);

        DataStream<String> stream = env.fromElements(
                "INFO: user logged in",
                "ERROR: connection refused",
                "INFO: all good"
        ).map(mapper);

        List<String> results = collectStream(stream, "XSLT Filter Errors Test");

        assertEquals(3, results.size());
        assertEquals("INFO: user logged in",           results.get(0), "Non-error should pass through");
        assertEquals("FILTERED:ERROR: connection refused", results.get(1), "Error line should be prefixed");
        assertEquals("INFO: all good",                  results.get(2), "Non-error should pass through");
    }

    // ------------------------------------------------------------------ //
    // Test 3 — Invalid XSLT path should throw during open()              //
    // ------------------------------------------------------------------ //

    @Test
    public void testMissingXsltFileThrows() {
        DataMapperFunction fn = new DataMapperFunction("/tmp/nonexistent.xslt");
        assertThrows(Exception.class, () -> {
            fn.open((OpenContext) null);
        }, "Opening a non-existent XSLT file should throw");
    }
}
