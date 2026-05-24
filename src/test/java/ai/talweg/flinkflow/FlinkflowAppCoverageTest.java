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
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FlinkflowAppCoverageTest {

    @Test
    public void testComprehensivePipeline(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("comprehensive-pipeline.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"Comprehensive Pipeline\"\n");
            writer.write("parallelism: 1\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: static-source\n");
            writer.write("    properties:\n");
            writer.write("      content: \"a,1|b,2|a,3|c,4\"\n");
            writer.write("  - type: filter\n");
            writer.write("    name: filter-step\n");
            writer.write("    code: |\n");
            writer.write("      return input.contains(\",\");\n");
            writer.write("  - type: flatmap\n");
            writer.write("    name: flatmap-step\n");
            writer.write("    code: |\n");
            writer.write("      out.collect(input);\n");
            writer.write("  - type: keyby\n");
            writer.write("    name: keyby-step\n");
            writer.write("    code: |\n");
            writer.write("      return input.split(\",\")[0];\n");
            writer.write("  - type: reduce\n");
            writer.write("    name: reduce-step\n");
            writer.write("    code: |\n");
            writer.write("      String[] v1 = value1.split(\",\");\n");
            writer.write("      String[] v2 = value2.split(\",\");\n");
            writer.write("      int sum = Integer.parseInt(v1[1]) + Integer.parseInt(v2[1]);\n");
            writer.write("      return v1[0] + \",\" + sum;\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
        }

        assertDoesNotThrow(() -> {
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath()});
        });
    }

    @Test
    public void testWindowPipeline(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("window-pipeline.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"Window Pipeline\"\n");
            writer.write("parallelism: 1\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: static-source\n");
            writer.write("    properties:\n");
            writer.write("      content: \"key1,1|key1,2|key2,3\"\n");
            writer.write("  - type: keyby\n");
            writer.write("    name: keyby-step\n");
            writer.write("    code: \"return input.split(\\\",\\\")[0];\"\n");
            writer.write("  - type: window\n");
            writer.write("    name: window-step\n");
            writer.write("    properties:\n");
            writer.write("      windowType: tumbling\n");
            writer.write("      size: 1\n");
            writer.write("    code: \"return value1 + \\\"-\\\" + value2;\"\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
        }

        assertDoesNotThrow(() -> {
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath()});
        });
    }

    @Test
    public void testSlidingWindowPipeline(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("sliding-window-pipeline.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"Sliding Window Pipeline\"\n");
            writer.write("parallelism: 1\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: static-source\n");
            writer.write("    properties:\n");
            writer.write("      content: \"key1,1\"\n");
            writer.write("  - type: keyby\n");
            writer.write("    name: keyby-step\n");
            writer.write("    code: \"return input.split(\\\",\\\")[0];\"\n");
            writer.write("  - type: window\n");
            writer.write("    name: window-step\n");
            writer.write("    properties:\n");
            writer.write("      windowType: sliding\n");
            writer.write("      size: 2\n");
            writer.write("      slide: 1\n");
            writer.write("    code: \"return value1;\"\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
        }

        assertDoesNotThrow(() -> {
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath()});
        });
    }

    @Test
    public void testSessionWindowPipeline(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("session-window-pipeline.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"Session Window Pipeline\"\n");
            writer.write("parallelism: 1\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: static-source\n");
            writer.write("    properties:\n");
            writer.write("      content: \"key1,1\"\n");
            writer.write("  - type: keyby\n");
            writer.write("    name: keyby-step\n");
            writer.write("    code: \"return input.split(\\\",\\\")[0];\"\n");
            writer.write("  - type: window\n");
            writer.write("    name: window-step\n");
            writer.write("    properties:\n");
            writer.write("      windowType: session\n");
            writer.write("      gap: 1\n");
            writer.write("    code: \"return value1;\"\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
        }

        assertDoesNotThrow(() -> {
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath()});
        });
    }

    @Test
    public void testSideOutputPipeline(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("sideoutput-pipeline.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"SideOutput Pipeline\"\n");
            writer.write("parallelism: 1\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: static-source\n");
            writer.write("    properties:\n");
            writer.write("      content: \"data1|data2\"\n");
            writer.write("  - type: sideoutput\n");
            writer.write("    name: sideoutput-step\n");
            writer.write("    properties:\n");
            writer.write("      outputName: \"test-output\"\n");
            writer.write("    code: |\n");
            writer.write("      out.collect(input);\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
        }

        assertDoesNotThrow(() -> {
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath()});
        });
    }

    @Test
    public void testDataGenPipeline(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("datagen-pipeline.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"DataGen Pipeline\"\n");
            writer.write("parallelism: 1\n");
            writer.write("steps:\n");
            writer.write("  - type: datagen\n");
            writer.write("    name: datagen-source\n");
            writer.write("    properties:\n");
            writer.write("      rowsPerSecond: 10\n");
            writer.write("      totalRows: 5\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
        }

        assertDoesNotThrow(() -> {
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath()});
        });
    }

    @Test
    public void testFileSinkPipeline(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("filesink-pipeline.yaml").toFile();
        File outputDir = tempDir.resolve("output").toFile();
        outputDir.mkdirs();

        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"FileSink Pipeline\"\n");
            writer.write("parallelism: 1\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: static-source\n");
            writer.write("    properties:\n");
            writer.write("      content: \"file-data\"\n");
            writer.write("  - type: sink\n");
            writer.write("    name: file-sink\n");
            writer.write("    properties:\n");
            writer.write("      path: \"" + outputDir.getAbsolutePath() + "\"\n");
        }

        assertDoesNotThrow(() -> {
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath()});
        });
    }

    @Test
    public void testInvalidWindowType(@TempDir Path tempDir) {
        File configFile = tempDir.resolve("invalid-window.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"Invalid Window\"\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: static-source\n");
            writer.write("  - type: keyby\n");
            writer.write("    code: \"return input;\"\n");
            writer.write("  - type: window\n");
            writer.write("    properties:\n");
            writer.write("      windowType: unknown\n");
        } catch (Exception e) {}

        assertThrows(RuntimeException.class, () -> {
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath()});
        });
    }

    @Test
    public void testMissingSource(@TempDir Path tempDir) {
        File configFile = tempDir.resolve("missing-source.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"Missing Source\"\n");
            writer.write("steps:\n");
            writer.write("  - type: process\n");
            writer.write("    code: \"return input;\"\n");
        } catch (Exception e) {}

        assertThrows(RuntimeException.class, () -> {
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath()});
        });
    }

    @Test
    public void testDataMapperPipeline(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("datamapper-pipeline.yaml").toFile();
        File xslFile = new File("deploy/mappings/transform.xsl");

        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"DataMapper Pipeline\"\n");
            writer.write("parallelism: 1\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: static-source\n");
            writer.write("    properties:\n");
            writer.write("      content: '{\"id\": 1, \"name\": \"John Doe\", \"email\": \"john@example.com\"}'\n");
            writer.write("  - type: datamapper\n");
            writer.write("    name: mapper-step\n");
            writer.write("    properties:\n");
            writer.write("      xsltPath: \"" + xslFile.getAbsolutePath() + "\"\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
        }

        assertDoesNotThrow(() -> {
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath()});
        });
    }
    @Test
    public void testCLIArguments(@TempDir Path tempDir) throws Exception {
        File flowletDir = tempDir.resolve("flowlets").toFile();
        flowletDir.mkdirs();

        // 1. Missing arguments
        assertThrows(RuntimeException.class, () -> {
            FlinkflowApp.main(new String[]{});
        });

        // 2. K8s Pipeline arguments
        // This will attempt to load a pipeline from K8s, which will fail because there is no cluster,
        // but it will cover the argument parsing branches.
        assertThrows(RuntimeException.class, () -> {
            FlinkflowApp.main(new String[]{"--k8s-pipeline", "my-pipeline", "--k8s-namespace", "test-ns", "--enable-k8s-flowlets"});
        });

        // 3. Flowlet dir argument
        File configFile = tempDir.resolve("dummy.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"Dummy\"\nsteps: []\n");
        }
        
        assertThrows(RuntimeException.class, () -> {
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath(), "--flowlet-dir", flowletDir.getAbsolutePath()});
        });
    }

    @Test
    public void testYamlParsingErrors(@TempDir Path tempDir) throws Exception {
        // Unrecognized property
        File badPropFile = tempDir.resolve("bad-prop.yaml").toFile();
        try (FileWriter writer = new FileWriter(badPropFile)) {
            writer.write("name: \"Bad Prop\"\ninvalidField: true\n");
        }
        assertThrows(RuntimeException.class, () -> {
            FlinkflowApp.main(new String[]{badPropFile.getAbsolutePath()});
        });

        // Mismatched input (array instead of object)
        File mismatchFile = tempDir.resolve("mismatch.yaml").toFile();
        try (FileWriter writer = new FileWriter(mismatchFile)) {
            writer.write("name: [\"Array Name\"]\n");
        }
        assertThrows(RuntimeException.class, () -> {
            FlinkflowApp.main(new String[]{mismatchFile.getAbsolutePath()});
        });
    }

    @Test
    public void testJoinPipeline(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("join-pipeline.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"Join Pipeline\"\n");
            writer.write("parallelism: 1\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: static-source\n");
            writer.write("    properties:\n");
            writer.write("      content: \"id1,left1|id2,left2\"\n");
            writer.write("  - type: join\n");
            writer.write("    name: join-step\n");
            writer.write("    connector: static-source\n"); // Using static-source for the right side of the join
            writer.write("    properties:\n");
            writer.write("      content: \"id1,right1|id2,right2\"\n");
            writer.write("      leftKey: \"return input.split(\\\",\\\")[0];\"\n");
            writer.write("      rightKey: \"return input.split(\\\",\\\")[0];\"\n");
            writer.write("      lowerBound: \"-5\"\n");
            writer.write("      upperBound: \"5\"\n");
            writer.write("    code: \"out.collect(left + \\\"-\\\" + right);\"\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
        }
        assertDoesNotThrow(() -> {
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath(), "--dry-run"});
        });
    }

    @Test
    public void testHttpLookupPipeline(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("http-lookup.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"HTTP Lookup\"\n");
            writer.write("parallelism: 1\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: static-source\n");
            writer.write("    properties:\n");
            writer.write("      content: \"id1\"\n");
            writer.write("  - type: http-lookup\n");
            writer.write("    name: http-lookup-step\n");
            writer.write("    properties:\n");
            writer.write("      urlCode: \"return \\\"http://localhost:12345/api\\\";\"\n"); // Will fail connection but branch is covered
            writer.write("      timeout: \"10\"\n");
            writer.write("    code: \"return response;\"\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
        }
        assertDoesNotThrow(() -> {
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath(), "--dry-run"});
        });
    }

    @Test
    public void testKafkaSourcesAndSinks(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("kafka.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"Kafka Pipeline\"\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: kafka-source-step\n");
            writer.write("    connector: kafka-source\n");
            writer.write("    properties:\n");
            writer.write("      bootstrap.servers: \"localhost:9092\"\n");
            writer.write("      topic: \"in-topic\"\n");
            writer.write("      group.id: \"test-group\"\n");
            writer.write("      security.protocol: \"SASL_SSL\"\n");
            writer.write("  - type: sink\n");
            writer.write("    name: kafka-sink-step\n");
            writer.write("    connector: kafka-sink\n");
            writer.write("    properties:\n");
            writer.write("      bootstrap.servers: \"localhost:9092\"\n");
            writer.write("      topic: \"out-topic\"\n");
        }
        assertDoesNotThrow(() -> {
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath(), "--dry-run"});
        });
    }

    @Test
    public void testKafkaAvroSourcesAndSinks(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("kafka-avro.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"Kafka Avro Pipeline\"\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: kafka-avro-source-step\n");
            writer.write("    connector: kafka-avro-source\n");
            writer.write("    properties:\n");
            writer.write("      bootstrap.servers: \"localhost:9092\"\n");
            writer.write("      topic: \"in-topic\"\n");
            writer.write("      group.id: \"test-group\"\n");
            writer.write("      schema.registry.url: \"http://localhost:8081\"\n");
            writer.write("      schema.literal: '{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}'\n");
            writer.write("  - type: sink\n");
            writer.write("    name: kafka-avro-sink-step\n");
            writer.write("    connector: kafka-avro-sink\n");
            writer.write("    properties:\n");
            writer.write("      bootstrap.servers: \"localhost:9092\"\n");
            writer.write("      topic: \"out-topic\"\n");
            writer.write("      schema.registry.url: \"http://localhost:8081\"\n");
            writer.write("      schema.literal: '{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}'\n");
        }
        assertDoesNotThrow(() -> {
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath(), "--dry-run"});
        });
    }

    @Test
    public void testJdbcSinkPipeline(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("jdbc.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"JDBC Pipeline\"\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: jdbc-source-step\n");
            writer.write("    connector: static-source\n");
            writer.write("  - type: sink\n");
            writer.write("    name: jdbc-sink-step\n");
            writer.write("    connector: jdbc-sink\n");
            writer.write("    properties:\n");
            writer.write("      url: \"jdbc:postgresql://localhost:5432/mydb\"\n");
            writer.write("      username: \"user\"\n");
            writer.write("      password: \"pass\"\n");
            writer.write("      sql: \"INSERT INTO table VALUES (?)\"\n");
            writer.write("      batchSize: \"10\"\n");
            writer.write("      batchIntervalMs: \"1000\"\n");
            writer.write("      maxRetries: \"3\"\n");
            writer.write("    code: \"statement.setString(1, input);\"\n");
        }
        assertDoesNotThrow(() -> {
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath(), "--dry-run"});
        });
    }

    @Test
    public void testHttpSinkPipeline(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("http-sink.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"HTTP Sink Pipeline\"\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: http-sink-source-step\n");
            writer.write("    connector: static-source\n");
            writer.write("  - type: sink\n");
            writer.write("    name: http-sink-step\n");
            writer.write("    connector: http-sink\n");
            writer.write("    properties:\n");
            writer.write("      urlCode: \"return \\\"http://localhost\\\";\"\n");
            writer.write("      method: \"PUT\"\n");
        }
        assertDoesNotThrow(() -> {
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath(), "--dry-run"});
        });
    }

    @Test
    public void testAgentPipeline(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("agent.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"Agent Pipeline\"\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: agent-source-step\n");
            writer.write("    connector: static-source\n");
            writer.write("  - type: agent\n");
            writer.write("    name: agent-step\n");
            writer.write("    properties:\n");
            writer.write("      model: \"gpt-3.5-turbo\"\n");
            writer.write("      memory: \"false\"\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
        }
        assertDoesNotThrow(() -> {
            FlinkflowApp.main(new String[]{configFile.getAbsolutePath(), "--dry-run"});
        });
    }

    @Test
    public void testSemanticErrors(@TempDir Path tempDir) throws Exception {
        // Reduce without keyby
        File reduceErr = tempDir.resolve("reduce-err.yaml").toFile();
        try (FileWriter writer = new FileWriter(reduceErr)) {
            writer.write("name: \"err\"\nsteps:\n  - type: source\n    connector: static-source\n  - type: reduce\n    code: \"return \\\"\\\";\"\n");
        }
        assertThrows(RuntimeException.class, () -> {
            FlinkflowApp.main(new String[]{reduceErr.getAbsolutePath()});
        });

        // Window without keyby
        File windowErr = tempDir.resolve("window-err.yaml").toFile();
        try (FileWriter writer = new FileWriter(windowErr)) {
            writer.write("name: \"err\"\nsteps:\n  - type: source\n    connector: static-source\n  - type: window\n    code: \"return \\\"\\\";\"\n");
        }
        assertThrows(RuntimeException.class, () -> {
            FlinkflowApp.main(new String[]{windowErr.getAbsolutePath()});
        });
    }
}
