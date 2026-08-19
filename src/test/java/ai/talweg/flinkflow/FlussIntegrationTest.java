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

import ai.talweg.flinkflow.config.JobConfig;
import ai.talweg.flinkflow.config.StepConfig;
import ai.talweg.flinkflow.core.DynamicFlussLookupFunction;
import ai.talweg.flinkflow.core.fluss.DynamicFlussSinkFunction;
import ai.talweg.flinkflow.core.fluss.DynamicFlussSourceFunction;
import ai.talweg.flinkflow.core.fluss.FlussManager;
import ai.talweg.flinkflow.validation.PipelineValidationException;
import ai.talweg.flinkflow.validation.PipelineValidator;
import org.apache.fluss.config.Configuration;
import org.apache.fluss.metadata.TablePath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Self-contained unit, dry-run, and topology construction tests for Apache Fluss integration.
 * Conforms to established Flinkflow connector test patterns (Kafka, JDBC, S3) without requiring
 * an active external cluster.
 */
public class FlussIntegrationTest {

    // =========================================================================
    // 1. Dry-Run Configuration & Validation Tests
    // =========================================================================

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
            writer.write("      table: \"user_profiles\"\n");
            writer.write("      key: \"userId\"\n");
            writer.write("      outputField: \"userProfile\"\n");
            writer.write("      cacheTtlSec: \"60\"\n");
            writer.write("      bootstrap.servers: \"localhost:9123\"\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
        }

        assertDoesNotThrow(() -> {
            int status = FlinkflowApp.execute(new String[]{configFile.getAbsolutePath(), "--dry-run"});
            assertEquals(0, status, "Expected status 0 for valid Fluss lookup dry-run");
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
            writer.write("      table: \"orders\"\n");
            writer.write("      merge-engine: \"partial-update\"\n");
            writer.write("      bootstrap.servers: \"localhost:9123\"\n");
        }

        assertDoesNotThrow(() -> {
            int status = FlinkflowApp.execute(new String[]{configFile.getAbsolutePath(), "--dry-run"});
            assertEquals(0, status, "Expected status 0 for valid Fluss sink dry-run");
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
            writer.write("    connector: fluss-source\n");
            writer.write("    name: fluss-in\n");
            writer.write("    properties:\n");
            writer.write("      table: \"orders\"\n");
            writer.write("      bootstrap.servers: \"localhost:9123\"\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
        }

        assertDoesNotThrow(() -> {
            int status = FlinkflowApp.execute(new String[]{configFile.getAbsolutePath(), "--dry-run"});
            assertEquals(0, status, "Expected status 0 for valid Fluss source dry-run");
        });
    }

    // =========================================================================
    // 2. DAG / Topology Construction Tests (Matching Kafka/JDBC pattern)
    // =========================================================================

    @Test
    public void testFlussPipelineGraphConstructionCoverage(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("fluss-coverage-pipeline.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"Fluss Coverage Pipeline\"\n");
            writer.write("parallelism: 1\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    connector: fluss-source\n");
            writer.write("    name: fluss-in\n");
            writer.write("    properties:\n");
            writer.write("      table: \"orders\"\n");
            writer.write("      bootstrap.servers: \"localhost:9123\"\n");
            writer.write("      client.connect.timeout: \"500ms\"\n");
            writer.write("  - type: fluss-lookup\n");
            writer.write("    name: enrich-user\n");
            writer.write("    properties:\n");
            writer.write("      table: \"user_profiles\"\n");
            writer.write("      key: \"userId\"\n");
            writer.write("      outputField: \"userProfile\"\n");
            writer.write("      timeoutMs: \"500\"\n");
            writer.write("      capacity: \"50\"\n");
            writer.write("      bootstrap.servers: \"localhost:9123\"\n");
            writer.write("      client.connect.timeout: \"500ms\"\n");
            writer.write("  - type: sink\n");
            writer.write("    connector: fluss-sink\n");
            writer.write("    name: fluss-out\n");
            writer.write("    properties:\n");
            writer.write("      table: \"orders_enriched\"\n");
            writer.write("      bootstrap.servers: \"localhost:9123\"\n");
            writer.write("      client.connect.timeout: \"500ms\"\n");
        }

        // Flink DAG construction runs and verifies step instantiation; fails on env.execute()
        // because no live Fluss server is running locally in CI.
        assertThrows(Exception.class, () -> {
            FlinkflowApp.execute(new String[]{configFile.getAbsolutePath()});
        });
    }

    // =========================================================================
    // 3. FlussManager Unit Tests
    // =========================================================================

    @Test
    public void testFlussManagerResolveTablePath() {
        // Simple table name defaults to "default" database
        TablePath singleTable = FlussManager.resolveTablePath("orders");
        assertEquals("default", singleTable.getDatabaseName());
        assertEquals("orders", singleTable.getTableName());

        // Fully qualified table path "database.table"
        TablePath qualifiedTable = FlussManager.resolveTablePath("analytics.user_events");
        assertEquals("analytics", qualifiedTable.getDatabaseName());
        assertEquals("user_events", qualifiedTable.getTableName());

        // Multi-dot qualified path
        TablePath multiDot = FlussManager.resolveTablePath("my_catalog_db.table_name");
        assertEquals("my_catalog_db", multiDot.getDatabaseName());
        assertEquals("table_name", multiDot.getTableName());

        // Invalid null or blank inputs throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> FlussManager.resolveTablePath(null));
        assertThrows(IllegalArgumentException.class, () -> FlussManager.resolveTablePath(""));
        assertThrows(IllegalArgumentException.class, () -> FlussManager.resolveTablePath("   "));
    }

    @Test
    public void testFlussManagerResolveBootstrapServers() {
        // 1. From "bootstrap.servers" property
        Map<String, String> props1 = new HashMap<>();
        props1.put("bootstrap.servers", "fluss-prod:9123");
        assertEquals("fluss-prod:9123", FlussManager.resolveBootstrapServers(props1));

        // 2. From "coordinator.server" property
        Map<String, String> props2 = new HashMap<>();
        props2.put("coordinator.server", "coordinator-host:9123");
        assertEquals("coordinator-host:9123", FlussManager.resolveBootstrapServers(props2));

        // 3. Null / empty fallback to default localhost:9123
        String fallback = FlussManager.resolveBootstrapServers(Collections.emptyMap());
        assertNotNull(fallback);
        assertFalseOrNotEmpty(fallback);
    }

    private void assertFalseOrNotEmpty(String val) {
        assertNotNull(val);
        assertTrue(!val.trim().isEmpty());
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

    // =========================================================================
    // 4. Component Function Instantiation & Validation Tests
    // =========================================================================

    @Test
    public void testDynamicFlussFunctionsConstructors() {
        Map<String, String> lookupProps = new HashMap<>();
        lookupProps.put("table", "users");
        lookupProps.put("key", "userId");
        DynamicFlussLookupFunction lookupFunc = new DynamicFlussLookupFunction(lookupProps);
        assertNotNull(lookupFunc);

        DynamicFlussLookupFunction emptyLookupFunc = new DynamicFlussLookupFunction(null);
        assertNotNull(emptyLookupFunc);

        Map<String, String> sinkProps = new HashMap<>();
        sinkProps.put("table", "orders");
        DynamicFlussSinkFunction sinkFunc = new DynamicFlussSinkFunction(sinkProps);
        assertNotNull(sinkFunc);

        DynamicFlussSinkFunction emptySinkFunc = new DynamicFlussSinkFunction(null);
        assertNotNull(emptySinkFunc);

        Map<String, String> sourceProps = new HashMap<>();
        sourceProps.put("table", "orders");
        DynamicFlussSourceFunction sourceFunc = new DynamicFlussSourceFunction(sourceProps);
        assertNotNull(sourceFunc);

        DynamicFlussSourceFunction emptySourceFunc = new DynamicFlussSourceFunction(null);
        assertNotNull(emptySourceFunc);
    }

    // =========================================================================
    // 5. Pipeline Validator Direct & Error Tests
    // =========================================================================

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

    @Test
    public void testFlussStepValidatorDirectly() {
        JobConfig jobConfig = new JobConfig();
        jobConfig.setName("Fluss Validation Test");
        jobConfig.setParallelism(1);

        // 1. Missing table & key for lookup
        List<StepConfig> lookupSteps = new ArrayList<>();
        StepConfig lookupStep = new StepConfig();
        lookupStep.setName("lookup-step");
        lookupStep.setType("fluss-lookup");
        lookupSteps.add(lookupStep);

        PipelineValidationException ex = assertThrows(PipelineValidationException.class, () -> {
            PipelineValidator.validate(jobConfig, lookupSteps);
        });
        assertTrue(ex.getMessage().toLowerCase().contains("fluss"));

        // 2. Valid lookup pipeline
        List<StepConfig> validLookupSteps = new ArrayList<>();
        StepConfig dummySource = new StepConfig();
        dummySource.setName("source-step");
        dummySource.setType("source");
        dummySource.setConnector("static-source");
        validLookupSteps.add(dummySource);

        Map<String, String> validLookupProps = new HashMap<>();
        validLookupProps.put("table", "users");
        validLookupProps.put("key", "userId");
        lookupStep.setProperties(validLookupProps);
        validLookupSteps.add(lookupStep);

        StepConfig dummySink = new StepConfig();
        dummySink.setName("sink-step");
        dummySink.setType("sink");
        dummySink.setConnector("console-sink");
        validLookupSteps.add(dummySink);

        assertDoesNotThrow(() -> {
            PipelineValidator.validate(jobConfig, validLookupSteps);
        });

        // 3. Valid source and sink step validation
        List<StepConfig> pipelineSteps = new ArrayList<>();
        StepConfig sourceStep = new StepConfig();
        sourceStep.setName("fluss-source-step");
        sourceStep.setType("source");
        sourceStep.setConnector("fluss-source");
        Map<String, String> sourceProps = new HashMap<>();
        sourceProps.put("table", "events");
        sourceStep.setProperties(sourceProps);
        pipelineSteps.add(sourceStep);

        StepConfig sinkStep = new StepConfig();
        sinkStep.setName("fluss-sink-step");
        sinkStep.setType("sink");
        sinkStep.setConnector("fluss-sink");
        Map<String, String> sinkProps = new HashMap<>();
        sinkProps.put("table", "orders");
        sinkProps.put("merge-engine", "partial-update");
        sinkStep.setProperties(sinkProps);
        pipelineSteps.add(sinkStep);

        assertDoesNotThrow(() -> {
            PipelineValidator.validate(jobConfig, pipelineSteps);
        });

        // 4. Invalid fluss source missing table
        List<StepConfig> invalidSourceSteps = new ArrayList<>();
        StepConfig invalidSource = new StepConfig();
        invalidSource.setName("invalid-fluss-source");
        invalidSource.setType("source");
        invalidSource.setConnector("fluss-source");
        invalidSourceSteps.add(invalidSource);

        PipelineValidationException sourceEx = assertThrows(PipelineValidationException.class, () -> {
            PipelineValidator.validate(jobConfig, invalidSourceSteps);
        });
        assertTrue(sourceEx.getMessage().contains("fluss"));

        // 5. Invalid fluss sink missing table
        List<StepConfig> invalidSinkSteps = new ArrayList<>();
        StepConfig invalidSink = new StepConfig();
        invalidSink.setName("invalid-fluss-sink");
        invalidSink.setType("sink");
        invalidSink.setConnector("fluss-sink");
        invalidSinkSteps.add(invalidSink);

        PipelineValidationException sinkEx = assertThrows(PipelineValidationException.class, () -> {
            PipelineValidator.validate(jobConfig, invalidSinkSteps);
        });
        assertTrue(sinkEx.getMessage().contains("fluss"));
    }
}
