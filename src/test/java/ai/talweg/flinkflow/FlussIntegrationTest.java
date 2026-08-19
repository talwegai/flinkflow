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
import ai.talweg.flinkflow.core.fluss.FlussManager;
import ai.talweg.flinkflow.validation.PipelineValidationException;
import ai.talweg.flinkflow.validation.PipelineValidator;
import org.apache.fluss.config.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Self-contained unit and dry-run tests for Apache Fluss integration.
 * Mimics external connector test patterns (Kafka, JDBC, S3) without requiring a live cluster.
 */
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
            writer.write("      table: \"user_profiles\"\n");
            writer.write("      key: \"userId\"\n");
            writer.write("      outputField: \"userProfile\"\n");
            writer.write("      cacheTtlSec: \"60\"\n");
            writer.write("      bootstrap.servers: \"localhost:9123\"\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
        }

        assertDoesNotThrow(() -> {
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath().replace("\\", "/"), "--dry-run"});
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
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath().replace("\\", "/"), "--dry-run"});
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
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath().replace("\\", "/"), "--dry-run"});
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
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath().replace("\\", "/"), "--dry-run"});
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
    }
}
