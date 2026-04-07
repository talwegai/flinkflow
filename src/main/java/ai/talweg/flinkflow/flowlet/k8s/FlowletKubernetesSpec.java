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


package ai.talweg.flinkflow.flowlet.k8s;

import ai.talweg.flinkflow.config.StepConfig;
import ai.talweg.flinkflow.flowlet.FlowletParameter;

import java.util.List;
import java.util.Map;

/**
 * Represents the {@code spec:} section of a {@code Flowlet} Kubernetes Custom
 * Resource.
 *
 * <p>
 * Unlike the flat {@link ai.talweg.flinkflow.flowlet.FlowletSpec}
 * (used for classpath/file-based Flowlets), here the {@code name} lives in
 * {@code metadata.name} on the Kubernetes side — not inside the spec. This
 * class therefore only carries the payload fields.
 * </p>
 *
 * <p>
 * The {@link FlowletKubernetesLoader} merges {@code metadata.name} with this
 * spec to produce a unified
 * {@link ai.talweg.flinkflow.flowlet.FlowletSpec}
 * for the registry.
 * </p>
 */
public class FlowletKubernetesSpec {

    private String title;
    private String description;

    /** "source", "sink", or "action". */
    private String type;

    private String version;

    /** Optional free-form grouping labels, e.g. {@code group: kafka}. */
    private Map<String, String> labels;

    /**
     * Declared parameters keyed by parameter name.
     * Each value is a {@link FlowletParameter} describing title, type,
     * default, and whether the parameter is required.
     */
    private Map<String, FlowletParameter> parameters;

    /**
     * The template steps expanded at pipeline-build time.
     * Property values may contain {@code {{paramName}}} placeholders.
     */
    private List<StepConfig> template;

    // ------------------------------------------------------------------ //
    // Getters & Setters
    // ------------------------------------------------------------------ //

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
