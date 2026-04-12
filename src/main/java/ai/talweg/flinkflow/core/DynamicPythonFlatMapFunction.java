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
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.util.Collector;
import org.graalvm.polyglot.Value;

/**
 * A Flink RichFlatMapFunction that executes inline Python code via GraalVM Polyglot.
 *
 * <p>The user-supplied {@code code} block is treated as the body of a Python
 * function with the signature {@code def process(input) -> list}.
 * Every element in the returned Python list is emitted as a separate downstream record.
 *
 * <p>Example YAML usage:
 * <pre>
 *   - type: flatmap
 *     name: split-csv
 *     language: python
 *     code: |
 *       return input.split(",")
 * </pre>
 */
public class DynamicPythonFlatMapFunction extends RichFlatMapFunction<String, String> {

    /** The Python code body supplied by the user. */
    private final String codeBody;

    /** Manages the GraalVM context lifecycle. */
    private transient PythonEvaluator evaluator;

    /**
     * Constructs a DynamicPythonFlatMapFunction with the specified Python code body.
     *
     * @param codeBody the body of the Python {@code process(input)} function
     */
    public DynamicPythonFlatMapFunction(String codeBody) {
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
     * Executes the Python {@code process(input)} function and emits each element
     * of the returned list as a separate downstream record.
     *
     * @param value the incoming stream record
     * @param out   the Flink collector for downstream emission
     * @throws Exception if execution fails
     */
    @Override
    public void flatMap(String value, Collector<String> out) throws Exception {
        if (evaluator == null) {
            throw new RuntimeException("Python flatmap function not initialised");
        }
        Value result = evaluator.execute(value);
        // Support Python list return: iterate over each element and collect
        if (result.hasArrayElements()) {
            for (long i = 0; i < result.getArraySize(); i++) {
                out.collect(result.getArrayElement(i).asString());
            }
        } else {
            // Fallback: treat return value as a single string record
            out.collect(result.asString());
        }
    }

    /** Releases the GraalVM context on task teardown. */
    @Override
    public void close() {
        if (evaluator != null) {
            evaluator.close();
        }
    }
}
