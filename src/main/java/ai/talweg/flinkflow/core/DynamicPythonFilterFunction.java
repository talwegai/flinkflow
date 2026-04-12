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


package ai.talweg.flinkflow.core;

import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.functions.RichFilterFunction;

/**
 * A Flink RichFilterFunction that executes inline Python code via GraalVM Polyglot.
 *
 * <p>The user-supplied {@code code} block is treated as the body of a Python
 * function with the signature {@code def process(input) -> bool}.
 * The snippet must return a Python boolean (True/False).
 *
 * <p>Example YAML usage:
 * <pre>
 *   - type: filter
 *     name: valid-only
 *     language: python
 *     code: |
 *       return "valid" in input
 * </pre>
 */
public class DynamicPythonFilterFunction extends RichFilterFunction<String> {

    /** The Python code body supplied by the user. */
    private final String codeBody;

    /** Manages the GraalVM context lifecycle. */
    private transient PythonEvaluator evaluator;

    /**
     * Constructs a DynamicPythonFilterFunction with the specified Python code body.
     *
     * @param codeBody the body of the Python {@code process(input)} function
     */
    public DynamicPythonFilterFunction(String codeBody) {
        this.codeBody = codeBody;
    }

    /**
     * Initialises the GraalVM Python context and compiles the user-supplied script.
     *
     * @param parameters Flink configuration
     * @throws Exception if the Python context cannot be created
     */
    @Override
    public void open(OpenContext parameters) throws Exception {
        evaluator = new PythonEvaluator(codeBody, "def process(input)");
        evaluator.open();
    }

    /**
     * Executes the Python {@code process(input)} function and interprets the
     * result as a boolean keep/discard decision.
     *
     * @param value the incoming stream record
     * @return {@code true} if the element should be forwarded downstream
     * @throws Exception if execution fails
     */
    @Override
    public boolean filter(String value) throws Exception {
        if (evaluator == null) {
            throw new RuntimeException("Python filter function not initialised");
        }
        return evaluator.execute(value).asBoolean();
    }

    /** Releases the GraalVM context on task teardown. */
    @Override
    public void close() {
        if (evaluator != null) {
            evaluator.close();
        }
    }
}
