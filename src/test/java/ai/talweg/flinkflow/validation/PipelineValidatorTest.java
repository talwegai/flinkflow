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

package ai.talweg.flinkflow.validation;

import ai.talweg.flinkflow.config.JobConfig;
import ai.talweg.flinkflow.config.StepConfig;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PipelineValidatorTest {

    @Test
    public void testValidPipeline() {
        JobConfig job = new JobConfig();
        job.setName("Valid Job");
        job.setParallelism(2);

        StepConfig source = new StepConfig();
        source.setName("static-source");
        source.setType("source");
        Map<String, String> srcProps = new HashMap<>();
        srcProps.put("content", "hello");
        source.setProperties(srcProps);

        StepConfig process = new StepConfig();
        process.setName("my-process");
        process.setType("process");
        process.setCode("return input.toUpperCase();");

        StepConfig sink = new StepConfig();
        sink.setName("console-sink");
        sink.setType("sink");

        List<StepConfig> steps = Arrays.asList(source, process, sink);
        job.setSteps(steps);

        assertDoesNotThrow(() -> PipelineValidator.validate(job, steps));
    }

    @Test
    public void testNullOrEmptyJob() {
        PipelineValidationException ex = assertThrows(PipelineValidationException.class, () -> 
            PipelineValidator.validate(null, Collections.emptyList())
        );
        assertTrue(ex.getMessage().contains("Pipeline configuration is null"));

        JobConfig emptyJob = new JobConfig();
        PipelineValidationException ex2 = assertThrows(PipelineValidationException.class, () -> 
            PipelineValidator.validate(emptyJob, Collections.emptyList())
        );
        assertTrue(ex2.getMessage().contains("Pipeline configuration 'name' must be defined"));
        assertTrue(ex2.getMessage().contains("Pipeline contains no steps"));
    }

    @Test
    public void testInvalidParallelism() {
        JobConfig job = new JobConfig();
        job.setName("Invalid Parallelism");
        job.setParallelism(0);

        StepConfig source = new StepConfig();
        source.setName("static-source");
        source.setType("source");

        List<StepConfig> steps = Collections.singletonList(source);
        job.setSteps(steps);

        PipelineValidationException ex = assertThrows(PipelineValidationException.class, () -> 
            PipelineValidator.validate(job, steps)
        );
        assertTrue(ex.getMessage().contains("Pipeline parallelism must be a positive integer"));
    }

    @Test
    public void testStepMissingNameAndType() {
        JobConfig job = new JobConfig();
        job.setName("Test Job");
        
        StepConfig step = new StepConfig();
        List<StepConfig> steps = Collections.singletonList(step);
        job.setSteps(steps);

        PipelineValidationException ex = assertThrows(PipelineValidationException.class, () -> 
            PipelineValidator.validate(job, steps)
        );
        assertTrue(ex.getMessage().contains("Step at index 0 is missing a 'name'"));
        assertTrue(ex.getMessage().contains("Step 'unknown' at index 0 is missing a 'type'"));
    }

    @Test
    public void testKafkaSourceValidation() {
        JobConfig job = new JobConfig();
        job.setName("Kafka Validation");

        StepConfig step = new StepConfig();
        step.setName("kafka-source");
        step.setType("source");
        // missing properties entirely
        
        List<StepConfig> steps = Collections.singletonList(step);
        job.setSteps(steps);

        PipelineValidationException ex = assertThrows(PipelineValidationException.class, () -> 
            PipelineValidator.validate(job, steps)
        );
        assertTrue(ex.getMessage().contains("missing required property 'bootstrap.servers'"));
        assertTrue(ex.getMessage().contains("missing required property 'topic'"));
    }

    @Test
    public void testKafkaAvroSourceValidation() {
        JobConfig job = new JobConfig();
        job.setName("Kafka Avro Validation");

        StepConfig step = new StepConfig();
        step.setName("kafka-avro-source");
        step.setType("source");
        
        Map<String, String> props = new HashMap<>();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("topic", "test");
        step.setProperties(props);

        List<StepConfig> steps = Collections.singletonList(step);
        job.setSteps(steps);

        PipelineValidationException ex = assertThrows(PipelineValidationException.class, () -> 
            PipelineValidator.validate(job, steps)
        );
        assertTrue(ex.getMessage().contains("missing required property 'schema.registry.url'"));
    }

    @Test
    public void testFileSourceValidation() {
        JobConfig job = new JobConfig();
        job.setName("File Source Validation");

        StepConfig step = new StepConfig();
        step.setName("file-source");
        step.setType("source");

        List<StepConfig> steps = Collections.singletonList(step);
        job.setSteps(steps);

        PipelineValidationException ex = assertThrows(PipelineValidationException.class, () -> 
            PipelineValidator.validate(job, steps)
        );
        assertTrue(ex.getMessage().contains("missing required property 'path'"));
    }

    @Test
    public void testCodeStepsValidation() {
        JobConfig job = new JobConfig();
        job.setName("Code Validation");

        StepConfig step = new StepConfig();
        step.setName("my-filter");
        step.setType("filter");
        // missing code

        List<StepConfig> steps = Collections.singletonList(step);
        job.setSteps(steps);

        PipelineValidationException ex = assertThrows(PipelineValidationException.class, () -> 
            PipelineValidator.validate(job, steps)
        );
        assertTrue(ex.getMessage().contains("requires a 'code' snippet"));
    }

    @Test
    public void testWindowStepsValidation() {
        JobConfig job = new JobConfig();
        job.setName("Window Validation");

        StepConfig step = new StepConfig();
        step.setName("my-window");
        step.setType("window");
        step.setCode("return value1;");
        Map<String, String> props = new HashMap<>();
        props.put("windowType", "tumbling");
        props.put("size", "abc"); // non-numeric
        step.setProperties(props);

        List<StepConfig> steps = Collections.singletonList(step);
        job.setSteps(steps);

        PipelineValidationException ex = assertThrows(PipelineValidationException.class, () -> 
            PipelineValidator.validate(job, steps)
        );
        assertTrue(ex.getMessage().contains("property 'size' must be a valid number"));
    }

    @Test
    public void testSinkValidation() {
        JobConfig job = new JobConfig();
        job.setName("Sink Validation");

        StepConfig step = new StepConfig();
        step.setName("jdbc-sink");
        step.setType("sink");
        
        List<StepConfig> steps = Collections.singletonList(step);
        job.setSteps(steps);

        PipelineValidationException ex = assertThrows(PipelineValidationException.class, () -> 
            PipelineValidator.validate(job, steps)
        );
        assertTrue(ex.getMessage().contains("missing required property 'url'"));
        assertTrue(ex.getMessage().contains("missing required property 'sql'"));
        assertTrue(ex.getMessage().contains("must define 'code' containing the statement builder logic"));
    }

    @Test
    public void testHttpSinkValidation() {
        JobConfig job = new JobConfig();
        job.setName("HTTP Sink Validation");

        StepConfig step = new StepConfig();
        step.setName("http-sink");
        step.setType("sink");

        List<StepConfig> steps = Collections.singletonList(step);
        job.setSteps(steps);

        PipelineValidationException ex = assertThrows(PipelineValidationException.class, () -> 
            PipelineValidator.validate(job, steps)
        );
        assertTrue(ex.getMessage().contains("requires either 'url' or 'urlCode' property"));
    }

    @Test
    public void testGraphValidationIntegration() {
        JobConfig job = new JobConfig();
        job.setName("Graph Validation Integration");

        // Use 'static-source' connector name so step validation passes cleanly
        StepConfig source = new StepConfig();
        source.setName("static-source");
        source.setType("source");

        StepConfig process = new StepConfig();
        process.setName("my-process");
        process.setType("process");
        process.setCode("return input;");

        // No sink — graph validator should fire after step validation passes
        List<StepConfig> steps = Arrays.asList(source, process);
        job.setSteps(steps);

        PipelineValidationException ex = assertThrows(PipelineValidationException.class, () -> 
            PipelineValidator.validate(job, steps)
        );
        // GraphValidator message: "Pipeline graph validation failed: Missing sink step."
        assertTrue(ex.getMessage().contains("sink"),
            "Expected error about missing sink, but got: " + ex.getMessage());
    }
}
