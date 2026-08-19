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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates the full job config, resolved pipeline steps, and individual step parameters.
 */
public class PipelineValidator {

    public static void validate(JobConfig jobConfig, List<StepConfig> resolvedSteps) {
        List<String> errors = new ArrayList<>();

        if (jobConfig == null) {
            errors.add("Pipeline configuration is null.");
            throw new PipelineValidationException(errors);
        }

        // Job level validation
        if (jobConfig.getName() == null || jobConfig.getName().trim().isEmpty()) {
            errors.add("Pipeline configuration 'name' must be defined and not empty.");
        }
        if (jobConfig.getParallelism() <= 0) {
            errors.add("Pipeline parallelism must be a positive integer. Value provided: " + jobConfig.getParallelism());
        }

        // Steps validation
        if (resolvedSteps == null || resolvedSteps.isEmpty()) {
            errors.add("Pipeline contains no steps.");
        } else {
            for (int i = 0; i < resolvedSteps.size(); i++) {
                StepConfig step = resolvedSteps.get(i);
                validateStep(step, i, errors);
            }
        }

        // Graph validation - only run if there are no step-level errors to avoid noisy errors
        if (errors.isEmpty() && resolvedSteps != null && !resolvedSteps.isEmpty()) {
            try {
                GraphValidator.validate(resolvedSteps);
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new PipelineValidationException(errors);
        }
    }

    private static void validateStep(StepConfig step, int index, List<String> errors) {
        if (step.getName() == null || step.getName().trim().isEmpty()) {
            errors.add(String.format("Step at index %d is missing a 'name'.", index));
        }

        String stepType = step.getType();
        if (stepType == null || stepType.trim().isEmpty()) {
            errors.add(String.format("Step '%s' at index %d is missing a 'type'.", 
                    step.getName() != null ? step.getName() : "unknown", index));
            return;
        }

        String normalizedType = stepType.toLowerCase().trim();
        switch (normalizedType) {
            case "source":
            case "datagen":
            case "datagen-source":
                validateSourceStep(step, errors);
                break;
            case "process":
            case "transform":
            case "filter":
            case "flatmap":
            case "keyby":
            case "groupby":
            case "reduce":
            case "aggregate":
                validateCodeStep(step, errors);
                break;
            case "window":
                validateWindowStep(step, errors);
                break;
            case "sideoutput":
                validateSideOutputStep(step, errors);
                break;
            case "datamapper":
                validateDataMapperStep(step, errors);
                break;
            case "join":
                validateJoinStep(step, errors);
                break;
            case "http-lookup":
                validateHttpLookupStep(step, errors);
                break;
            case "fluss-lookup":
                validateFlussLookupStep(step, errors);
                break;
            case "agent":
                break;
            case "ml":
                validateMLStep(step, errors);
                break;
            case "sql":
                validateSQLStep(step, errors);
                break;
            case "sink":
                validateSinkStep(step, errors);
                break;
            default:
                errors.add(String.format("Step '%s' at index %d has unsupported step type: '%s'.", 
                        step.getName(), index, stepType));
        }
    }

    private static void validateSourceStep(StepConfig step, List<String> errors) {
        String connector = step.getConnector() != null ? step.getConnector() : step.getName();
        if (connector == null) {
            errors.add(String.format("Source step '%s' must define a connector type (via 'connector' or 'name').", step.getName()));
            return;
        }

        String normConnector = connector.toLowerCase().trim();
        Map<String, String> props = step.getProperties();

        if (normConnector.startsWith("kafka-source") || normConnector.startsWith("kafka-avro-source")) {
            validateRequiredProperty(step, "bootstrap.servers", props, errors);
            validateRequiredProperty(step, "topic", props, errors);
            
            boolean isAvro = normConnector.startsWith("kafka-avro-source") || 
                    (props != null && "avro".equalsIgnoreCase(props.get("format")));
            if (isAvro) {
                validateRequiredProperty(step, "schema.registry.url", props, errors);
            }
        } else if (normConnector.startsWith("fluss-source") || normConnector.equals("fluss")) {
            boolean hasTable = props != null && ((props.containsKey("table") && props.get("table") != null && !props.get("table").trim().isEmpty())
                    || (props.containsKey("table.path") && props.get("table.path") != null && !props.get("table.path").trim().isEmpty()));
            if (!hasTable) {
                errors.add(String.format("Source step '%s' (fluss) requires 'table' or 'table.path' property.", step.getName()));
            }
        } else if (normConnector.startsWith("file-source") || normConnector.startsWith("s3-source")) {
            validateRequiredProperty(step, "path", props, errors);
        } else if (normConnector.startsWith("static-source")) {
            // content is optional
        } else if (normConnector.startsWith("datagen") || normConnector.startsWith("datagen-source")) {
            // parameters have defaults
        } else {
            boolean matched = false;
            String[] validSources = {"kafka-source", "kafka-avro-source", "fluss", "fluss-source", "file-source", "s3-source", "static-source", "datagen", "datagen-source"};
            for (String v : validSources) {
                if (v.equalsIgnoreCase(normConnector)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                errors.add(String.format("Source step '%s' specifies an unsupported connector: '%s'. Supported: %s", 
                        step.getName(), connector, String.join(", ", validSources)));
            }
        }
    }

    private static void validateSinkStep(StepConfig step, List<String> errors) {
        String connector = step.getConnector() != null ? step.getConnector() : step.getName();
        if (connector == null) {
            errors.add(String.format("Sink step '%s' must define a connector type (via 'connector' or 'name').", step.getName()));
            return;
        }

        String normConnector = connector.toLowerCase().trim();
        Map<String, String> props = step.getProperties();

        if (normConnector.startsWith("kafka-sink") || normConnector.startsWith("kafka-avro-sink")) {
            validateRequiredProperty(step, "bootstrap.servers", props, errors);
            validateRequiredProperty(step, "topic", props, errors);
            
            boolean isAvro = normConnector.startsWith("kafka-avro-sink") ||
                    (props != null && "avro".equalsIgnoreCase(props.get("format")));
            if (isAvro) {
                validateRequiredProperty(step, "schema.registry.url", props, errors);
            }
        } else if (normConnector.startsWith("fluss-sink") || normConnector.equals("fluss")) {
            boolean hasTable = props != null && ((props.containsKey("table") && props.get("table") != null && !props.get("table").trim().isEmpty())
                    || (props.containsKey("table.path") && props.get("table.path") != null && !props.get("table.path").trim().isEmpty()));
            if (!hasTable) {
                errors.add(String.format("Sink step '%s' (fluss) requires 'table' or 'table.path' property.", step.getName()));
            }
        } else if (normConnector.startsWith("file-sink") || normConnector.startsWith("s3-sink")) {
            validateRequiredProperty(step, "path", props, errors);
        } else if (normConnector.startsWith("http-sink") || normConnector.startsWith("webhook-sink")) {
            boolean hasUrl = props != null && props.containsKey("url") && props.get("url") != null && !props.get("url").trim().isEmpty();
            boolean hasUrlCode = props != null && props.containsKey("urlCode") && props.get("urlCode") != null && !props.get("urlCode").trim().isEmpty();
            if (!hasUrl && !hasUrlCode) {
                errors.add(String.format("Sink step '%s' (http-sink) requires either 'url' or 'urlCode' property.", step.getName()));
            }
        } else if (normConnector.startsWith("jdbc-sink")) {
            validateRequiredProperty(step, "url", props, errors);
            validateRequiredProperty(step, "sql", props, errors);
            if (step.getCode() == null || step.getCode().trim().isEmpty()) {
                errors.add(String.format("jdbc-sink step '%s' must define 'code' containing the statement builder logic.", step.getName()));
            }
        } else if (normConnector.startsWith("console") || normConnector.startsWith("console-sink")) {
            // no required properties
        } else {
            boolean matched = false;
            String[] validSinks = {"console", "console-sink", "fluss", "fluss-sink", "kafka-sink", "kafka-avro-sink", "file-sink", "s3-sink", "http-sink", "webhook-sink", "jdbc-sink"};
            for (String v : validSinks) {
                if (v.equalsIgnoreCase(normConnector)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                errors.add(String.format("Sink step '%s' specifies an unsupported connector: '%s'. Supported: %s", 
                        step.getName(), connector, String.join(", ", validSinks)));
            }
        }
    }

    private static void validateFlussLookupStep(StepConfig step, List<String> errors) {
        Map<String, String> props = step.getProperties();
        boolean hasTable = props != null && ((props.containsKey("table") && props.get("table") != null && !props.get("table").trim().isEmpty())
                || (props.containsKey("table.path") && props.get("table.path") != null && !props.get("table.path").trim().isEmpty()));
        if (!hasTable) {
            errors.add(String.format("Fluss lookup step '%s' requires 'table' or 'table.path' property.", step.getName()));
        }
        boolean hasKey = props != null && ((props.containsKey("key") && props.get("key") != null && !props.get("key").trim().isEmpty())
                || (props.containsKey("lookupKey") && props.get("lookupKey") != null && !props.get("lookupKey").trim().isEmpty()));
        if (!hasKey) {
            errors.add(String.format("Fluss lookup step '%s' requires 'key' or 'lookupKey' property.", step.getName()));
        }
    }

    private static void validateCodeStep(StepConfig step, List<String> errors) {
        if (step.getCode() == null || step.getCode().trim().isEmpty()) {
            errors.add(String.format("Step '%s' of type '%s' requires a 'code' snippet.", step.getName(), step.getType()));
        }
    }

    private static void validateWindowStep(StepConfig step, List<String> errors) {
        validateCodeStep(step, errors);
        Map<String, String> props = step.getProperties();
        if (props == null) {
            errors.add(String.format("Window step '%s' requires 'properties' containing window configuration.", step.getName()));
            return;
        }

        String windowType = props.getOrDefault("windowType", "tumbling").toLowerCase().trim();
        if ("tumbling".equals(windowType)) {
            validateNumericProperty(step, "size", props, errors);
        } else if ("sliding".equals(windowType)) {
            validateNumericProperty(step, "size", props, errors);
            validateNumericProperty(step, "slide", props, errors);
        } else if ("session".equals(windowType)) {
            validateNumericProperty(step, "gap", props, errors);
        } else {
            errors.add(String.format("Window step '%s' has unknown windowType: '%s'. Supported: tumbling, sliding, session.", 
                    step.getName(), props.get("windowType")));
        }
    }

    private static void validateSideOutputStep(StepConfig step, List<String> errors) {
        validateCodeStep(step, errors);
        validateRequiredProperty(step, "outputName", step.getProperties(), errors);
    }

    private static void validateDataMapperStep(StepConfig step, List<String> errors) {
        validateRequiredProperty(step, "xsltPath", step.getProperties(), errors);
    }

    private static void validateJoinStep(StepConfig step, List<String> errors) {
        validateCodeStep(step, errors);
        Map<String, String> props = step.getProperties();
        validateRequiredProperty(step, "leftKey", props, errors);
        validateRequiredProperty(step, "rightKey", props, errors);
        
        validateSourceStep(step, errors);
    }

    private static void validateHttpLookupStep(StepConfig step, List<String> errors) {
        validateCodeStep(step, errors);
    }

    private static void validateRequiredProperty(StepConfig step, String propName, Map<String, String> props, List<String> errors) {
        if (props == null || !props.containsKey(propName) || props.get(propName) == null || props.get(propName).trim().isEmpty()) {
            errors.add(String.format("Step '%s' of type '%s' is missing required property '%s'.", 
                    step.getName(), step.getType(), propName));
        }
    }

    private static void validateNumericProperty(StepConfig step, String propName, Map<String, String> props, List<String> errors) {
        if (props == null || !props.containsKey(propName) || props.get(propName) == null || props.get(propName).trim().isEmpty()) {
            errors.add(String.format("Step '%s' of type '%s' is missing required property '%s'.", 
                    step.getName(), step.getType(), propName));
            return;
        }
        try {
            Long.parseLong(props.get(propName).trim());
        } catch (NumberFormatException e) {
            errors.add(String.format("Step '%s' property '%s' must be a valid number, but was: '%s'.", 
                    step.getName(), propName, props.get(propName)));
        }
    }

    private static void validateMLStep(StepConfig step, List<String> errors) {
        Map<String, String> props = step.getProperties();
        if (props == null || !props.containsKey("algorithm") || props.get("algorithm") == null || props.get("algorithm").trim().isEmpty()) {
            errors.add(String.format("ML step '%s' is missing required property 'algorithm'.", step.getName()));
        }

        boolean hasSchema = false;
        if (props != null) {
            for (Map.Entry<String, String> entry : props.entrySet()) {
                if (entry.getKey().startsWith("schema.")) {
                    hasSchema = true;
                    try {
                        ai.talweg.flinkflow.core.SchemaHelper.resolveType(entry.getValue());
                    } catch (IllegalArgumentException e) {
                        errors.add(String.format("ML step '%s' has invalid schema type for key '%s': %s",
                                step.getName(), entry.getKey(), e.getMessage()));
                    }
                }
            }
        }
        if (!hasSchema) {
            errors.add(String.format("ML step '%s' requires at least one schema property starting with 'schema.' to define the input schema.", step.getName()));
        }
    }

    private static void validateSQLStep(StepConfig step, List<String> errors) {
        Map<String, String> props = step.getProperties();
        String query = props != null ? props.get("query") : null;
        if (query == null || query.trim().isEmpty()) {
            if (step.getCode() == null || step.getCode().trim().isEmpty()) {
                errors.add(String.format("SQL step '%s' must define a SQL query either in 'properties.query' or as a step 'code' body.", step.getName()));
            }
        }

        if (props != null && props.containsKey("outputMode")) {
            String outputMode = props.get("outputMode").toLowerCase().trim();
            if (!"append".equals(outputMode) && !"changelog".equals(outputMode) && !"auto".equals(outputMode)) {
                errors.add(String.format("SQL step '%s' has invalid outputMode '%s'. Supported: append, changelog, auto.",
                        step.getName(), props.get("outputMode")));
            }
        }

        List<String> inputs = step.getInputs();
        boolean hasSchema = false;
        if (inputs != null && !inputs.isEmpty()) {
            // Multi-table mode: check for schema.<inputName>.*
            for (String inputName : inputs) {
                boolean inputHasSchema = false;
                if (props != null) {
                    String prefix = "schema." + inputName + ".";
                    for (Map.Entry<String, String> entry : props.entrySet()) {
                        if (entry.getKey().startsWith(prefix)) {
                            inputHasSchema = true;
                            try {
                                ai.talweg.flinkflow.core.SchemaHelper.resolveType(entry.getValue());
                            } catch (IllegalArgumentException e) {
                                errors.add(String.format("SQL step '%s' has invalid schema type for key '%s': %s",
                                        step.getName(), entry.getKey(), e.getMessage()));
                            }
                        }
                    }
                }
                if (!inputHasSchema) {
                    errors.add(String.format("SQL step '%s' is missing schema definitions starting with 'schema.%s.' for input '%s'.",
                            step.getName(), inputName, inputName));
                } else {
                    hasSchema = true;
                }
            }
        } else {
            // Single-table mode (existing behavior)
            if (props != null) {
                for (Map.Entry<String, String> entry : props.entrySet()) {
                    if (entry.getKey().startsWith("schema.")) {
                        hasSchema = true;
                        try {
                            ai.talweg.flinkflow.core.SchemaHelper.resolveType(entry.getValue());
                        } catch (IllegalArgumentException e) {
                            errors.add(String.format("SQL step '%s' has invalid schema type for key '%s': %s",
                                    step.getName(), entry.getKey(), e.getMessage()));
                        }
                    }
                }
            }
        }

        if (!hasSchema) {
            errors.add(String.format("SQL step '%s' requires at least one schema property starting with 'schema.' to define the input schema.", step.getName()));
        }

        // Validate watermarks
        if (props != null) {
            if (inputs != null && !inputs.isEmpty()) {
                for (String inputName : inputs) {
                    String wmColumnKey = "watermark." + inputName + ".column";
                    String wmDelayKey = "watermark." + inputName + ".delay";
                    if (props.containsKey(wmColumnKey)) {
                        String wmColumn = props.get(wmColumnKey);
                        String schemaKey = "schema." + inputName + "." + wmColumn;
                        if (!props.containsKey(schemaKey)) {
                            errors.add(String.format("SQL step '%s' specifies watermark column '%s' for input '%s' which is not defined in the schema.",
                                    step.getName(), wmColumn, inputName));
                        } else {
                            String type = props.get(schemaKey);
                            if (!"timestamp".equalsIgnoreCase(type) && !"long".equalsIgnoreCase(type)) {
                                errors.add(String.format("SQL step '%s' watermark column '%s' for input '%s' must be of type 'timestamp' or 'long', but was '%s'.",
                                        step.getName(), wmColumn, inputName, type));
                            }
                        }
                        if (props.containsKey(wmDelayKey)) {
                            try {
                                long delay = Long.parseLong(props.get(wmDelayKey).trim());
                                if (delay < 0) {
                                    errors.add(String.format("SQL step '%s' watermark delay for input '%s' must be non-negative: %d",
                                            step.getName(), inputName, delay));
                                }
                            } catch (NumberFormatException e) {
                                errors.add(String.format("SQL step '%s' watermark delay for input '%s' must be a valid number, but was '%s'.",
                                        step.getName(), inputName, props.get(wmDelayKey)));
                            }
                        }
                    }
                }
            } else {
                String wmColumnKey = "watermark.column";
                String wmDelayKey = "watermark.delay";
                if (props.containsKey(wmColumnKey)) {
                    String wmColumn = props.get(wmColumnKey);
                    String schemaKey = "schema." + wmColumn;
                    if (!props.containsKey(schemaKey)) {
                        errors.add(String.format("SQL step '%s' specifies watermark column '%s' which is not defined in the schema.",
                                step.getName(), wmColumn));
                    } else {
                        String type = props.get(schemaKey);
                        if (!"timestamp".equalsIgnoreCase(type) && !"long".equalsIgnoreCase(type)) {
                            errors.add(String.format("SQL step '%s' watermark column '%s' must be of type 'timestamp' or 'long', but was '%s'.",
                                    step.getName(), wmColumn, type));
                        }
                    }
                    if (props.containsKey(wmDelayKey)) {
                        try {
                            long delay = Long.parseLong(props.get(wmDelayKey).trim());
                            if (delay < 0) {
                                errors.add(String.format("SQL step '%s' watermark delay must be non-negative: %d",
                                        step.getName(), delay));
                            }
                        } catch (NumberFormatException e) {
                            errors.add(String.format("SQL step '%s' watermark delay must be a valid number, but was '%s'.",
                                    step.getName(), props.get(wmDelayKey)));
                        }
                    }
                }
            }
        }
    }
}
