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

import ai.talweg.flinkflow.config.StepConfig;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class GraphValidatorTest {

    @Test
    public void testValidGraph() {
        StepConfig source = new StepConfig(); source.setType("source"); source.setName("source");
        StepConfig process = new StepConfig(); process.setType("process"); process.setName("process");
        StepConfig sink = new StepConfig(); sink.setType("sink"); sink.setName("sink");
        
        assertDoesNotThrow(() -> GraphValidator.validate(Arrays.asList(source, process, sink)));
    }

    @Test
    public void testEmptyStepsThrows() {
        assertThrows(IllegalArgumentException.class, () -> GraphValidator.validate(Collections.emptyList()));
    }

    @Test
    public void testDisconnectedOldStreamThrows() {
        StepConfig source1 = new StepConfig(); source1.setType("source"); source1.setName("source1");
        StepConfig process = new StepConfig(); process.setType("process"); process.setName("process");
        StepConfig source2 = new StepConfig(); source2.setType("source"); source2.setName("source2");
        StepConfig sink = new StepConfig(); sink.setType("sink"); sink.setName("sink");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> GraphValidator.validate(Arrays.asList(source1, process, source2, sink)));
        assertTrue(ex.getMessage().contains("Disconnected DAG detected"));
    }

    @Test
    public void testMissingSinkThrows() {
        StepConfig source = new StepConfig(); source.setType("source"); source.setName("source");
        StepConfig process = new StepConfig(); process.setType("process"); process.setName("process");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> GraphValidator.validate(Arrays.asList(source, process)));
        assertTrue(ex.getMessage().contains("never sent to a sink"));
    }

    @Test
    public void testSinkBeforeSourceThrows() {
        StepConfig sink = new StepConfig(); sink.setType("sink"); sink.setName("sink");
        StepConfig source = new StepConfig(); source.setType("source"); source.setName("source");
        StepConfig process = new StepConfig(); process.setType("process"); process.setName("process");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> GraphValidator.validate(Arrays.asList(sink, source, process)));
        assertTrue(ex.getMessage().contains("Sink defined before any source step"));
    }

    @Test
    public void testProcessorBeforeSourceThrows() {
        StepConfig process = new StepConfig(); process.setType("process"); process.setName("process");
        StepConfig source = new StepConfig(); source.setType("source"); source.setName("source");
        StepConfig sink = new StepConfig(); sink.setType("sink"); sink.setName("sink");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> GraphValidator.validate(Arrays.asList(process, source, sink)));
        assertTrue(ex.getMessage().contains("Processor step defined before any source step"));
    }
}
