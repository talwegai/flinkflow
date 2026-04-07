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


package ai.talweg.flinkflow.flowlet;

import ai.talweg.flinkflow.config.StepConfig;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves a {@code type: flowlet} pipeline step into one or more concrete
 * {@link StepConfig} instances by:
 * <ol>
 * <li>Looking up the named {@link FlowletSpec} in the
 * {@link FlowletRegistry}.</li>
 * <li>Merging the caller's parameter values with any declared defaults.</li>
 * <li>Validating that all required parameters are present.</li>
 * <li>Expanding {@code {{paramName}}} placeholders in every property value,
 * code snippet, and step name in the Flowlet's template.</li>
 * </ol>
 *
 * <p>
 * Example pipeline step that references a Flowlet:
 * </p>
 * 
 * <pre>
 * - type: flowlet
 *   name: kafka-confluent-source
 *   with:
 *     bootstrapServers: "pkc-xxx.us-east-1.aws.confluent.cloud:9092"
 *     topic: "my-topic"
 *     apiKey: "KEY"
 *     apiSecret: "SECRET"
 * </pre>
 */
public class FlowletResolver {

    /** Matches {@code {{paramName}}} placeholders anywhere in a string value. */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    private final FlowletRegistry registry;

    public FlowletResolver(FlowletRegistry registry) {
        this.registry = registry;
    }

    /**
     * Resolves a pipeline step of {@code type: flowlet} into the concrete steps
     * that the flowlet's template defines.
     *
     * @param step a step whose {@code type} is "flowlet", {@code name} is the
     *             Flowlet name, and {@code with} map holds the caller-supplied
     *             parameter values
     * @return the expanded list of concrete {@link StepConfig} instances
     * @throws IllegalArgumentException if the Flowlet is unknown or a required
     *                                  parameter is missing
     */
    public List<StepConfig> resolve(StepConfig step) {
        String flowletName = step.getName();
        FlowletSpec spec = registry.get(flowletName);

        // ---------- Build effective parameter map ----------
        Map<String, String> effective = buildEffectiveParams(spec, step.getWith());

        // ---------- Validate required parameters ----------
        validateRequiredParams(spec, effective, flowletName);

        // ---------- Validate parameter types ----------
        validateParameterTypes(spec, effective, flowletName);

        // ---------- Expand template ----------
        return expandTemplate(spec.getTemplate(), effective);
    }

    // ------------------------------------------------------------------ //
    // Internals
    // ------------------------------------------------------------------ //

    /**
     * Merges caller-supplied {@code with} values onto declared defaults for
     * every parameter, producing the final substitution map.
     */
    private Map<String, String> buildEffectiveParams(FlowletSpec spec, Map<String, String> callerWith) {
        Map<String, String> effective = new HashMap<>();

        // Start with all declared defaults
        if (spec.getParameters() != null) {
            for (Map.Entry<String, FlowletParameter> entry : spec.getParameters().entrySet()) {
                String pName = entry.getKey();
                FlowletParameter pDef = entry.getValue();
                if (pDef.getDefaultValue() != null) {
                    effective.put(pName, pDef.getDefaultValue());
                }
            }
        }

        // Overlay caller-supplied values
        if (callerWith != null) {
            effective.putAll(callerWith);
        }

        return effective;
    }

    /**
     * Checks that every parameter declared as {@code required: true} is present
     * in the effective parameter map.
     */
    private void validateRequiredParams(FlowletSpec spec,
            Map<String, String> effective,
            String flowletName) {
        if (spec.getParameters() == null)
            return;
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, FlowletParameter> entry : spec.getParameters().entrySet()) {
            if (entry.getValue().isRequired() && !effective.containsKey(entry.getKey())) {
                missing.add(entry.getKey());
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "Flowlet '" + flowletName + "' is missing required parameter(s): " + missing);
        }
    }

    /**
     * Checks that every supplied parameter matches its declared type.
     */
    private void validateParameterTypes(FlowletSpec spec, Map<String, String> effective, String flowletName) {
        if (spec.getParameters() == null) {
            return;
        }

        for (Map.Entry<String, FlowletParameter> entry : spec.getParameters().entrySet()) {
            String pName = entry.getKey();
            FlowletParameter pDef = entry.getValue();
            String value = effective.get(pName);

            if (value == null || pDef.getType() == null) {
                continue;
            }

            String type = pDef.getType();
            try {
                switch (type.toLowerCase()) {
                    case "int":
                    case "integer":
                        Integer.parseInt(value);
                        break;
                    case "long":
                        Long.parseLong(value);
                        break;
                    case "float":
                        Float.parseFloat(value);
                        break;
                    case "double":
                        Double.parseDouble(value);
                        break;
                    case "boolean":
                    case "bool":
                        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                            throw new IllegalArgumentException("Not a boolean");
                        }
                        break;
                    case "string":
                        // All strings are valid
                        break;
                    default:
                        // Unknown types gracefully pass for now
                        break;
                }
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        String.format("Flowlet '%s' parameter '%s' must be of type '%s', but received: '%s'",
                                flowletName, pName, type, value));
            }
        }
    }

    /**
     * Deep-clones the Flowlet's template steps and substitutes all
     * {@code {{paramName}}} placeholders with their resolved values.
     */
    private List<StepConfig> expandTemplate(List<StepConfig> template,
            Map<String, String> params) {
        if (template == null)
            return Collections.emptyList();
        List<StepConfig> expanded = new ArrayList<>();
        for (StepConfig tmplStep : template) {
            StepConfig concrete = new StepConfig();
            concrete.setType(tmplStep.getType());
            concrete.setName(tmplStep.getName() == null ? null
                    : substitute(tmplStep.getName(), params));
            concrete.setCode(tmplStep.getCode() == null ? null
                    : substitute(tmplStep.getCode(), params));
            concrete.setLanguage(tmplStep.getLanguage());
            concrete.setConnector(tmplStep.getConnector());

            // Expand all properties
            if (tmplStep.getProperties() != null) {
                Map<String, String> expandedProps = new LinkedHashMap<>();
                for (Map.Entry<String, String> e : tmplStep.getProperties().entrySet()) {
                    expandedProps.put(e.getKey(), substitute(e.getValue(), params));
                }
                concrete.setProperties(expandedProps);
            }

            // Recursively pass through `with` if present (rarely needed but safe)
            concrete.setWith(tmplStep.getWith());

            expanded.add(concrete);
        }
        return expanded;
    }

    /**
     * Replaces every {@code {{paramName}}} occurrence in {@code input} with the
     * corresponding value from {@code params}. Unresolved placeholders are left
     * as-is with a warning prefix so they are easy to spot.
     */
    private String substitute(String input, Map<String, String> params) {
        if (input == null)
            return null;
        Matcher m = PLACEHOLDER.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            String value = params.getOrDefault(key, "UNRESOLVED[" + key + "]");
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
