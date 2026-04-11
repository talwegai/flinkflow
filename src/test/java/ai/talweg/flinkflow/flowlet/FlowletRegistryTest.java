package ai.talweg.flinkflow.flowlet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FlowletRegistryTest {

    @Test
    public void testLoadFromDirectory(@TempDir Path tempDir) throws Exception {
        // Create a dummy flowlet yaml
        File flowletFile = tempDir.resolve("my-flowlet.yaml").toFile();
        try (FileWriter writer = new FileWriter(flowletFile)) {
            writer.write("apiVersion: flinkflow.io/v1alpha1\n");
            writer.write("kind: Flowlet\n");
            writer.write("metadata:\n");
            writer.write("  name: my-flowlet\n");
            writer.write("spec:\n");
            writer.write("  title: My Flowlet\n");
            writer.write("  description: Test flowlet\n");
            writer.write("  type: process\n");
            writer.write("  template:\n");
            writer.write("    - type: transform\n");
            writer.write("      name: step1\n");
            writer.write("      code: return;\n");
        }

        // Initialize registry with directory
        FlowletRegistry registry = new FlowletRegistry(tempDir.toFile().getAbsolutePath(), null, false);

        assertTrue(registry.contains("my-flowlet"));
        assertEquals(1, registry.size());

        FlowletSpec spec = registry.get("my-flowlet");
        assertEquals("my-flowlet", spec.getName());
        assertEquals("My Flowlet", spec.getTitle());
        assertEquals("Test flowlet", spec.getDescription());
        assertEquals("process", spec.getType());
        assertEquals(1, spec.getTemplate().size());
        assertEquals("transform", spec.getTemplate().get(0).getType());
    }

    @Test
    public void testMissingFlowlet() {
        FlowletRegistry registry = new FlowletRegistry(null, false);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            registry.get("non-existent");
        });
        assertTrue(thrown.getMessage().contains("Unknown Flowlet"));
    }

    @Test
    public void testLoadInvalidDirectory() {
        FlowletRegistry registry = new FlowletRegistry("/path/to/nowhere/that/does/not/exist", null, false);
        assertEquals(0, registry.size());
    }
}
