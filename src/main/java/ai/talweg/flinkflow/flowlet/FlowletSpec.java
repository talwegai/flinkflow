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

import java.util.List;
import java.util.Map;

/**
 * Represents the complete definition (spec) of a Flowlet — a reusable,
 * parameterised pipeline component, analogous to an Apache Camel Kamelet.
 *
 * <p>
 * A Flowlet YAML file has the following shape:
 * </p>
 * 
 * <pre>
 * apiVersion: flinkflow.io/v1
 * kind: Flowlet
 * metadata:
 *   name: kafka-confluent-source
 *   title: "Confluent Kafka Source"
 *   description: "Reads records from a Confluent Cloud Kafka topic."
 *   type: source            # source | sink | action
 *   version: "1.0"
 *   labels:
 *     group: kafka
 * spec:
 *   parameters:
 *     - name: bootstrapServers
 *       title: "Bootstrap Servers"
 *       required: true
 *     - name: topic
 *       required: true
 *     - name: groupId
 *       defaultValue: "flinkflow-consumer"
 *   template:
 *     steps:
 *       - type: source
 *         name: kafka-source
 *         properties:
 *           bootstrap.servers: "{{bootstrapServers}}"
 *           topic: "{{topic}}"
 *           group.id: "{{groupId}}"
 * </pre>
 */
public class FlowletSpec {

    // ------------------------------------------------------------------ //
    // metadata fields (promoted from the nested `metadata` object)
    // ------------------------------------------------------------------ //

    /** Unique name / id of the Flowlet (used to reference it from pipelines). */
    private String name;

    /** Human-readable display title. */
    private String title;

    /** Markdown-capable description of what this Flowlet does. */
    private String description;

    /** Flowlet kind: "source", "sink", or "action". */
    private String type;

    /** Optional version string, e.g. "1.0". */
    private String version;

    /** Optional free-form labels (e.g. group, provider). */
    private Map<String, String> labels;

    // ------------------------------------------------------------------ //
    // spec fields
    // ------------------------------------------------------------------ //

    /**
     * Declared parameters that callers must (or may) provide.
     * Key = parameter name; Value = parameter metadata.
     */
    private Map<String, FlowletParameter> parameters;

    /**
     * The internal pipeline template. This is a list of {@link StepConfig}
     * instances that are exactly what you would write in a pipeline.yaml,
     * except that property values may include {@code {{paramName}}}
     * placeholders which are substituted at runtime.
     */
    private List<StepConfig> template;

    // ------------------------------------------------------------------ //
    // Getters & Setters
    // ------------------------------------------------------------------ //

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public Map<String, FlowletParameter> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, FlowletParameter> parameters) {
        this.parameters = parameters;
    }

    public List<StepConfig> getTemplate() {
        return template;
    }

    public void setTemplate(List<StepConfig> template) {
        this.template = template;
    }
}
