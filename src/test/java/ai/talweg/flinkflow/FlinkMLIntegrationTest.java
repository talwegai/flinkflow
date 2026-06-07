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

public class FlinkMLIntegrationTest {

    @Test
    public void testVectorAssemblerPipeline(@TempDir Path tempDir) throws Exception {
        File configFile = tempDir.resolve("ml-pipeline.yaml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("name: \"ML Test Pipeline\"\n");
            writer.write("parallelism: 1\n");
            writer.write("steps:\n");
            writer.write("  - type: source\n");
            writer.write("    name: static-source\n");
            writer.write("    properties:\n");
            writer.write("      content: '{\"x\": 1.0, \"y\": 2.0}|{\"x\": 3.0, \"y\": 4.0}'\n");
            writer.write("  - type: ml\n");
            writer.write("    name: assembler\n");
            writer.write("    properties:\n");
            writer.write("      algorithm: \"VectorAssembler\"\n");
            writer.write("      inputCols: \"x,y\"\n");
            writer.write("      inputSizes: \"1,1\"\n");
            writer.write("      outputCol: \"features\"\n");
            writer.write("      schema.x: \"double\"\n");
            writer.write("      schema.y: \"double\"\n");
            writer.write("  - type: sink\n");
            writer.write("    name: console-sink\n");
        }

        assertDoesNotThrow(() -> {
            int status = FlinkflowApp.execute(new String[]{configFile.getAbsolutePath()});
            assertEquals(0, status, "Expected success (0) for valid ML pipeline config");
        });
    }
}
