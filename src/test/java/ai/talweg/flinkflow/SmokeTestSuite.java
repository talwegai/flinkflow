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

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import java.util.stream.Stream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Smoke Test Suite for Flinkflow.
 * This suite automatically finds and executes all YAML examples in the 
 * standalone directory using the --dry-run flag.
 */
public class SmokeTestSuite {

    private static final String EXAMPLES_DIR = "examples/standalone";

    @TestFactory
    public Stream<DynamicTest> smokeTestStandaloneExamples() throws Exception {
        Path examplesPath = Paths.get(EXAMPLES_DIR);
        if (!Files.exists(examplesPath)) {
            System.err.println("Warning: Examples directory not found at " + examplesPath.toAbsolutePath());
            return Stream.empty();
        }

        return Files.walk(examplesPath)
            .filter(path -> path.toString().endsWith(".yaml"))
            .map(path -> {
                String testName = "Smoke Test: " + path.getFileName().toString();
                return DynamicTest.dynamicTest(testName, () -> {
                    System.out.println("Running smoke test for: " + path);
                    
                    // We call execute with --dry-run and point to the flowlet catalog
                    String[] args = new String[]{path.toString(), "--dry-run", "--flowlet-dir", "k8s/flowlets"};
                    
                    assertDoesNotThrow(() -> {
                        int status = FlinkflowApp.execute(args);
                        assertEquals(0, status, "Dry-run failed for " + path);
                    }, "Exception thrown during dry-run of " + path);
                });
            });
    }
}
