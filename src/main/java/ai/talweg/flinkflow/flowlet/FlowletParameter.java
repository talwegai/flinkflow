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

/**
 * Represents a single configurable parameter declared in a Flowlet definition.
 * This is part of the Flowlet spec's parameter schema, inspired by Apache
 * Camel Kamelet parameter definitions.
 */
public class FlowletParameter {

    /** Short human-readable title for this parameter. */
    private String title;

    /** Detailed description of what this parameter controls. */
    private String description;

    /** JSON Schema type: "string", "integer", "boolean", etc. */
    private String type = "string";

    /** Optional default value, used when the caller omits this parameter. */
    private String defaultValue;

    /** Whether this parameter must be supplied by the caller. */
    private boolean required;

    /** Optional example value shown in documentation / catalog UIs. */
    private String example;

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

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }
}
