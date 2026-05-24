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

import java.util.List;

/**
 * Exception thrown when validation fails for a pipeline configuration.
 * It contains a list of all validation errors detected.
 */
public class PipelineValidationException extends RuntimeException {
    private final List<String> errors;

    public PipelineValidationException(List<String> errors) {
        super(buildMessage(errors));
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }

    private static String buildMessage(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return "Pipeline validation failed with no specific errors.";
        }
        StringBuilder sb = new StringBuilder("Pipeline validation failed with ")
                .append(errors.size())
                .append(" error(s):\n");
        for (String err : errors) {
            sb.append("  - ").append(err).append("\n");
        }
        return sb.toString().trim();
    }
}
