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
}
