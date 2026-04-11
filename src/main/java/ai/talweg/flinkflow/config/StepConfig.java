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


package ai.talweg.flinkflow.config;

import java.util.Map;

/**
 * Configuration class representing a single processing step in a Flink job.
 * Each step has a type (e.g., source, map, sink), a name, and specific
 * properties.
 */
public class StepConfig implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * The type of the step (e.g., "source", "map", "filter", "sink", "datamapper",
     * "join").
     */
    private String type;

    /**
     * The unique name of the step.
     */
    private String name;

    /**
     * Optional dynamic code or transformation logic associated with the step.
     */
    private String code;

    /**
     * Optional language for the code snippet (e.g., "java" or "python").
     */
    private String language;

    /**
     * Optional connector name for source or sink steps (e.g., "kafka-source", "static-source").
     */
    private String connector;

    /**
     * A map of properties specific to the step type (e.g., connection details,
     * configuration parameters).
     */
    private Map<String, String> properties;

    /**
     * Caller-supplied parameter values for {@code type: flowlet} steps.
     * These are substituted into the Flowlet template's {@code {{paramName}}}
     * placeholders by
     * {@link ai.talweg.flinkflow.flowlet.FlowletResolver}.
     */
    private Map<String, String> with;

    /**
     * Gets the type of this step.
     * 
     * @return the step type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of this step.
     * 
     * @param type the step type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the name of this step.
     * 
     * @return the step name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this step.
     * 
     * @param name the step name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the code or logic associated with this step.
     * 
     * @return the code string
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the code or logic for this step.
     * 
     * @param code the code string to set
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Gets the properties map for this step.
     * 
     * @return the properties map
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Sets the properties map for this step.
     * 
     * @param properties the properties map to set
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * Gets the caller-supplied parameter values for a {@code type: flowlet} step.
     *
     * @return the with-parameters map, or {@code null} if not a flowlet step
     */
    public Map<String, String> getWith() {
        return with;
    }

    /**
     * Sets the caller-supplied parameter values for a {@code type: flowlet} step.
     *
     * @param with the parameter key-value map
     */
    public void setWith(Map<String, String> with) {
        this.with = with;
    }

    /**
     * Gets the programming language of the code snippet.
     * 
     * @return the language (e.g., "java", "python"), defaults to "java" if not set
     */
    public String getLanguage() {
        return language != null ? language : "java";
    }

    /**
     * Sets the programming language of the code snippet.
     * 
     * @param language the language to set
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Gets the connector name for source or sink steps.
     * 
     * @return the connector name
     */
    public String getConnector() {
        return connector;
    }

    /**
     * Sets the connector name for source or sink steps.
     * 
     * @param connector the connector name to set
     */
    public void setConnector(String connector) {
        this.connector = connector;
    }
}

