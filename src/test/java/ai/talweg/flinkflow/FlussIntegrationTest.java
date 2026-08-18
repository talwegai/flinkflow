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

import ai.talweg.flinkflow.core.fluss.FlussManager;
import org.apache.fluss.config.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FlussIntegrationTest {

    @Test
    public void testFlussLookupDryRunValidation(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("fluss-lookup-pipeline.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"Fluss Lookup Enrichment Test\"\n");
            writer.write("parallelism: 1\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: static-source\n");
            writer.write("    properties:\n");
            writer.write("      content: '{\"userId\": \"user1\", \"amount\": 100.0}|{\"userId\": \"user2\", \"amount\": 250.0}'\n");
            writer.write("  - type: fluss-lookup\n");
            writer.write("    name: enrich-user\n");
            writer.write("    properties:\n");
            writer.write("      table: user_profiles\n");
            writer.write("      key: userId\n");
            writer.write("      outputField: userProfile\n");
            writer.write("      cacheTtlSec: \"60\"\n");
            writer.write("      bootstrap.servers: \"localhost:9123\"\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
        }

        assertDoesNotThrow(() -> {
            int status = FlinkflowApp.execute(new String[]{configFile.getAbsolutePath(), "--dry-run"});
            assertEquals(0, status, "Expected dry-run success (0) for valid Fluss lookup pipeline");
        });
    }

    @Test
    public void testFlussSinkDryRunValidation(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("fluss-sink-pipeline.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"Fluss Sink Test\"\n");
            writer.write("parallelism: 1\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: static-source\n");
            writer.write("    properties:\n");
            writer.write("      content: '{\"order_id\": \"order1\", \"status\": \"PAID\"}'\n");
            writer.write("  - type: sink\n");
            writer.write("    connector: fluss-sink\n");
            writer.write("    name: fluss-orders-sink\n");
            writer.write("    properties:\n");
            writer.write("      table: orders\n");
            writer.write("      merge-engine: partial-update\n");
            writer.write("      bootstrap.servers: \"localhost:9123\"\n");
        }

        assertDoesNotThrow(() -> {
            int status = FlinkflowApp.execute(new String[]{configFile.getAbsolutePath(), "--dry-run"});
            assertEquals(0, status, "Expected dry-run success (0) for valid Fluss sink pipeline");
        });
    }

    @Test
    public void testFlussSourceDryRunValidation(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("fluss-source-pipeline.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"Fluss Source Test\"\n");
            writer.write("parallelism: 1\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    connector: fluss\n");
            writer.write("    name: fluss-in\n");
            writer.write("    properties:\n");
            writer.write("      table: orders\n");
            writer.write("      bootstrap.servers: \"localhost:9123\"\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
        }

        assertDoesNotThrow(() -> {
            int status = FlinkflowApp.execute(new String[]{configFile.getAbsolutePath(), "--dry-run"});
            assertEquals(0, status, "Expected dry-run success (0) for valid Fluss source pipeline");
        });
    }

    @Test
    public void testDynamicFlussConfigurationPassThrough() {
        Map<String, String> properties = new HashMap<>();
        properties.put("bootstrap.servers", "fluss-coordinator:9123");
        properties.put("client.writer.bucket.assignment", "hash");
        properties.put("lookup.cache.max-rows", "5000");
        properties.put("custom.future.fluss.feature", "enabled");

        Configuration conf = FlussManager.buildConfiguration(properties);
        assertNotNull(conf);
        Map<String, String> confMap = conf.toMap();
        assertEquals("fluss-coordinator:9123", confMap.get("bootstrap.servers"));
        assertEquals("5000", confMap.get("lookup.cache.max-rows"));
        assertEquals("enabled", confMap.get("custom.future.fluss.feature"));
    }

    @Test
    public void testFlussValidationErrorsOnMissingProperties(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("invalid-fluss.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"Invalid Fluss Pipeline\"\n");
            writer.write("parallelism: 1\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: static-source\n");
            writer.write("    properties:\n");
            writer.write("      content: '{\"id\": 1}'\n");
            writer.write("  - type: fluss-lookup\n");
            writer.write("    name: missing-table-and-key\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
        }

        assertThrows(RuntimeException.class, () -> {
            FlinkflowApp.execute(new String[]{configFile.getAbsolutePath(), "--dry-run"});
        });
    }
}
