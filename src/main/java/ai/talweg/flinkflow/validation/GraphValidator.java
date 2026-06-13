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

        java.util.Set<String> stepNames = new java.util.HashSet<>();
        boolean hasSource = false;
        boolean hasSink = false;

        // Pass 1: populate step names and basic validation
        for (int i = 0; i < steps.size(); i++) {
            StepConfig step = steps.get(i);
            String name = step.getName();
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Pipeline graph validation failed: Step at index " + i + " is missing a name.");
            }
            if (!stepNames.add(name)) {
                throw new IllegalArgumentException("Pipeline graph validation failed: Duplicate step name '" + name + "' at index " + i + ".");
            }
            String type = step.getType() != null ? step.getType().toLowerCase() : "";
            if (isSource(type)) {
                hasSource = true;
            } else if (isSink(type)) {
                hasSink = true;
            }
        }

        if (!hasSource) {
            throw new IllegalArgumentException("Pipeline graph validation failed: Missing source step.");
        }
        if (!hasSink) {
            throw new IllegalArgumentException("Pipeline graph validation failed: Missing sink step. The final stream was never sent to a sink.");
        }

        // Pass 2: track connections and check that every non-source has input(s)
        java.util.Set<String> consumedOutputs = new java.util.HashSet<>();
        String currentOutput = null;
        boolean seenSource = false;

        for (int i = 0; i < steps.size(); i++) {
            StepConfig step = steps.get(i);
            String type = step.getType() != null ? step.getType().toLowerCase() : "";

            if (isSource(type)) {
                currentOutput = step.getName();
                seenSource = true;
            } else if (isSink(type)) {
                if (!seenSource) {
                    throw new IllegalArgumentException("Pipeline graph validation failed: Sink defined before any source step at index " + i + ".");
                }
                // Sinks consume their input
                java.util.List<String> inputs = step.getInputs();
                if (inputs != null && !inputs.isEmpty()) {
                    for (String inputName : inputs) {
                        if (!stepNames.contains(inputName)) {
                            throw new IllegalArgumentException("Pipeline graph validation failed: Sink '" + step.getName() + "' references unknown input '" + inputName + "'.");
                        }
                        // Make sure the referenced input is declared before this step
                        boolean found = false;
                        for (int j = 0; j < i; j++) {
                            if (steps.get(j).getName().equals(inputName)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            throw new IllegalArgumentException("Pipeline graph validation failed: Sink '" + step.getName() + "' references input '" + inputName + "' which is declared after it.");
                        }
                        consumedOutputs.add(inputName);
                    }
                } else {
                    if (currentOutput == null) {
                        throw new IllegalArgumentException("Pipeline graph validation failed: Sink '" + step.getName() + "' has no preceding stream to consume.");
                    }
                    consumedOutputs.add(currentOutput);
                    currentOutput = null;
                }
            } else {
                if (!seenSource) {
                    throw new IllegalArgumentException("Pipeline graph validation failed: Processor step defined before any source step at index " + i + ".");
                }
                // Processor/SQL step
                java.util.List<String> inputs = step.getInputs();
                if (inputs != null && !inputs.isEmpty()) {
                    for (String inputName : inputs) {
                        if (!stepNames.contains(inputName)) {
                            throw new IllegalArgumentException("Pipeline graph validation failed: Step '" + step.getName() + "' references unknown input '" + inputName + "'.");
                        }
                        // Make sure the referenced input is declared before this step
                        boolean found = false;
                        for (int j = 0; j < i; j++) {
                            if (steps.get(j).getName().equals(inputName)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            throw new IllegalArgumentException("Pipeline graph validation failed: Step '" + step.getName() + "' references input '" + inputName + "' which is declared after it.");
                        }
                        consumedOutputs.add(inputName);
                    }
                } else {
                    if (currentOutput == null) {
                        throw new IllegalArgumentException("Pipeline graph validation failed: Step '" + step.getName() + "' has no preceding stream to consume.");
                    }
                    consumedOutputs.add(currentOutput);
                }
                currentOutput = step.getName();
            }
        }

        // Validate that all outputs from non-sinks are consumed
        for (StepConfig step : steps) {
            String type = step.getType() != null ? step.getType().toLowerCase() : "";
            if (!isSink(type)) {
                if (!consumedOutputs.contains(step.getName())) {
                    throw new IllegalArgumentException("Disconnected DAG detected: Step '" + step.getName() + "' produces an output that is never consumed.");
                }
            }
        }
    }

    private static boolean isSource(String type) {
        return "source".equals(type) || "datagen".equals(type) || "datagen-source".equals(type);
    }

    private static boolean isSink(String type) {
        return "sink".equals(type);
    }
}
