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

package ai.talweg.flinkflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FlinkSQLIntegrationTest {

    @Test
    public void testSQLQueryProperty(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("sql-pipeline-prop.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"SQL Test Pipeline (Prop)\"\n");
            writer.write("parallelism: 1\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: static-source\n");
            writer.write("    properties:\n");
            writer.write("      content: '{\"userId\": \"user1\", \"age\": 30}|{\"userId\": \"user2\", \"age\": 25}'\n");
            writer.write("  - type: sql\n");
            writer.write("    name: select-users\n");
            writer.write("    properties:\n");
            writer.write("      schema.userId: \"string\"\n");
            writer.write("      schema.age: \"int\"\n");
            writer.write("      query: \"SELECT userId, age + 1 AS age_plus_one FROM input WHERE age > 26\"\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
        }

        assertDoesNotThrow(() -> {
            int status = FlinkflowApp.execute(new String[]{configFile.getAbsolutePath()});
            assertEquals(0, status, "Expected success (0) for valid SQL pipeline config");
        });
    }

    @Test
    public void testSQLQueryCodeBody(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("sql-pipeline-code.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"SQL Test Pipeline (Code)\"\n");
            writer.write("parallelism: 1\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: static-source\n");
            writer.write("    properties:\n");
            writer.write("      content: '{\"userId\": \"user1\", \"age\": 30}|{\"userId\": \"user2\", \"age\": 25}'\n");
            writer.write("  - type: sql\n");
            writer.write("    name: select-users\n");
            writer.write("    properties:\n");
            writer.write("      schema.userId: \"string\"\n");
            writer.write("      schema.age: \"int\"\n");
            writer.write("    code: \"SELECT userId FROM input\"\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
        }

        assertDoesNotThrow(() -> {
            int status = FlinkflowApp.execute(new String[]{configFile.getAbsolutePath()});
            assertEquals(0, status, "Expected success (0) for valid SQL pipeline config");
        });
    }

    @Test
    public void testSQLQueryExpandedTypes(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("sql-pipeline-expanded.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"SQL Test Pipeline (Expanded Types)\"\n");
            writer.write("parallelism: 1\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: static-source\n");
            writer.write("    properties:\n");
            writer.write("      content: '{\"userId\": \"user1\", \"eventTime\": \"2026-06-13T12:00:00\", \"price\": \"19.99\"}'\n");
            writer.write("  - type: sql\n");
            writer.write("    name: select-users\n");
            writer.write("    properties:\n");
            writer.write("      schema.userId: \"string\"\n");
            writer.write("      schema.eventTime: \"timestamp\"\n");
            writer.write("      schema.price: \"decimal\"\n");
            writer.write("      query: \"SELECT userId, CAST(price AS DOUBLE) * 2 AS double_price, eventTime FROM input\"\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
        }

        assertDoesNotThrow(() -> {
            int status = FlinkflowApp.execute(new String[]{configFile.getAbsolutePath()});
            assertEquals(0, status, "Expected success (0) for valid SQL pipeline config with expanded types");
        });
    }

    @Test
    public void testSQLWindowTumble(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("sql-pipeline-window.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"SQL Window Tumble Test\"\n");
            writer.write("parallelism: 1\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: static-source\n");
            writer.write("    properties:\n");
            writer.write("      content: '{\"userId\": \"user1\", \"eventTime\": \"2026-06-13T12:00:00\", \"price\": 10.0}|{\"userId\": \"user1\", \"eventTime\": \"2026-06-13T12:00:05\", \"price\": 15.0}|{\"userId\": \"user1\", \"eventTime\": \"2026-06-13T12:00:15\", \"price\": 20.0}'\n");
            writer.write("  - type: sql\n");
            writer.write("    name: window-aggregate\n");
            writer.write("    properties:\n");
            writer.write("      schema.userId: \"string\"\n");
            writer.write("      schema.eventTime: \"timestamp\"\n");
            writer.write("      schema.price: \"double\"\n");
            writer.write("      watermark.column: \"eventTime\"\n");
            writer.write("      watermark.delay: \"1\"\n");
            writer.write("      query: |\n");
            writer.write("        SELECT window_start, window_end, SUM(price) AS total_price\n");
            writer.write("        FROM TABLE(TUMBLE(TABLE input, DESCRIPTOR(eventTime), INTERVAL '10' SECOND))\n");
            writer.write("        GROUP BY window_start, window_end\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
        }

        assertDoesNotThrow(() -> {
            int status = FlinkflowApp.execute(new String[]{configFile.getAbsolutePath()});
            assertEquals(0, status, "Expected success (0) for valid SQL window tumble config");
        });
    }

    @Test
    public void testSQLMultiTableJoin(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("sql-pipeline-join.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"SQL Multi-Table Join Test\"\n");
            writer.write("parallelism: 1\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: orders\n");
            writer.write("    connector: static-source\n");
            writer.write("    properties:\n");
            writer.write("      content: '{\"orderId\": \"o1\", \"customerId\": \"c1\", \"amount\": 100.50}'\n");
            writer.write("  - type: source\n");
            writer.write("    name: customers\n");
            writer.write("    connector: static-source\n");
            writer.write("    properties:\n");
            writer.write("      content: '{\"customerId\": \"c1\", \"name\": \"Alice\"}'\n");
            writer.write("  - type: sql\n");
            writer.write("    name: enriched-orders\n");
            writer.write("    inputs: [orders, customers]\n");
            writer.write("    properties:\n");
            writer.write("      schema.orders.orderId: \"string\"\n");
            writer.write("      schema.orders.customerId: \"string\"\n");
            writer.write("      schema.orders.amount: \"double\"\n");
            writer.write("      schema.customers.customerId: \"string\"\n");
            writer.write("      schema.customers.name: \"string\"\n");
            writer.write("      query: |\n");
            writer.write("        SELECT o.orderId, c.name, o.amount\n");
            writer.write("        FROM orders o\n");
            writer.write("        JOIN customers c ON o.customerId = c.customerId\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
        }

        assertDoesNotThrow(() -> {
            int status = FlinkflowApp.execute(new String[]{configFile.getAbsolutePath()});
            assertEquals(0, status, "Expected success (0) for valid SQL pipeline join config");
        });
    }

    @Test
    public void testSQLChangelogGroupBy(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("sql-pipeline-changelog.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"SQL Changelog GroupBy Test\"\n");
            writer.write("parallelism: 1\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: static-source\n");
            writer.write("    properties:\n");
            writer.write("      content: '{\"userId\": \"user1\", \"price\": 10.0}|{\"userId\": \"user1\", \"price\": 15.0}'\n");
            writer.write("  - type: sql\n");
            writer.write("    name: select-users\n");
            writer.write("    properties:\n");
            writer.write("      schema.userId: \"string\"\n");
            writer.write("      schema.price: \"double\"\n");
            writer.write("      outputMode: \"changelog\"\n");
            writer.write("      query: \"SELECT userId, SUM(price) AS total_price FROM input GROUP BY userId\"\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
        }

        assertDoesNotThrow(() -> {
            int status = FlinkflowApp.execute(new String[]{configFile.getAbsolutePath()});
            assertEquals(0, status, "Expected success (0) for valid SQL changelog config");
        });
    }
}
