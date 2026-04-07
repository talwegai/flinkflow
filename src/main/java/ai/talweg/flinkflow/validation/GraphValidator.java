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
import java.util.List;

/**
 * Validates the parsed sequence of Flinkflow steps to ensure
 * a connected Directed Acyclic Graph (DAG) with proper sources and sinks.
 */
public class GraphValidator {

    public static void validate(List<StepConfig> steps) {
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("Pipeline graph validation failed: Pipeline contains no steps.");
        }

        boolean hasSource = false;
        boolean hasSink = false;
        int streamCount = 0;
        int sinkCountForCurrentStream = 0;

        for (int i = 0; i < steps.size(); i++) {
            StepConfig step = steps.get(i);
            String type = step.getType() != null ? step.getType().toLowerCase() : "";

            if (isSource(type)) {
                if (streamCount > 0 && sinkCountForCurrentStream == 0) {
                    throw new IllegalArgumentException(
                        "Disconnected DAG detected: Step " + i + " ('" + step.getName() + "') starts a new source stream, but the previous stream was never sent to a sink."
                    );
                }
                hasSource = true;
                streamCount++;
                sinkCountForCurrentStream = 0;
            } else if (isSink(type)) {
                if (!hasSource) {
                    throw new IllegalArgumentException("Pipeline graph validation failed: Sink defined before any source step at index " + i + ".");
                }
                hasSink = true;
                sinkCountForCurrentStream++;
            } else {
                if (!hasSource) {
                    throw new IllegalArgumentException("Pipeline graph validation failed: Processor step defined before any source step at index " + i + ".");
                }
            }
        }
        
        if (streamCount > 0 && sinkCountForCurrentStream == 0) {
            throw new IllegalArgumentException("Disconnected DAG detected: The final stream was never sent to a sink.");
        }

        if (!hasSink) {
            throw new IllegalArgumentException("Pipeline graph validation failed: Missing sink step.");
        }
    }

    private static boolean isSource(String type) {
        return "source".equals(type) || "datagen".equals(type) || "datagen-source".equals(type);
    }

    private static boolean isSink(String type) {
        return "sink".equals(type);
    }
}
