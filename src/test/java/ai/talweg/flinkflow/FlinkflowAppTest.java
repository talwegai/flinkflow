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

public class FlinkflowAppTest {

    @Test
    public void testBasicPipelineExecution(@TempDir Path tempDir) throws Exception {
        // Create a basic YAML configuration for testing
        File configFile = tempDir.resolve("test-pipeline.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"Test Pipeline\"\n");
            writer.write("parallelism: 1\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: static-source\n");
            writer.write("    properties:\n");
            writer.write("      content: \"Hello|World|From|Flinkflow\"\n");
            writer.write("  - type: process\n");
            writer.write("    name: uppercase-transform\n");
            writer.write("    code: |\n");
            writer.write("      return input.toUpperCase();\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
            writer.write("    properties:\n");
            writer.write("      type: console\n");
        }

        // Run the execute method with the test config. This avoids System.exit.
        assertDoesNotThrow(() -> {
            int status = FlinkflowApp.execute(new String[]{configFile.getAbsolutePath()});
            assertEquals(0, status, "Expected success (0) for valid config");
        });
    }

    @Test
    public void testMissingFileOrArgs(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("nonexistent.yaml").toFile();
        // We test the execute method instead of main() to avoid JVM termination.
        int status = FlinkflowApp.execute(new String[]{configFile.getAbsolutePath()});
        // status should be non-zero for error
        org.junit.jupiter.api.Assertions.assertNotEquals(0, status, "Expected non-zero status for missing file");
    }
}
