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

import org.apache.flink.api.common.functions.ReduceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A plain (non-rich) Flink ReduceFunction that executes Python code via GraalVM.
 * Used inside windowed reductions where RichFunction is not allowed.
 */
public class DynamicPythonWindowReduceFunction implements ReduceFunction<String> {
    private static final Logger LOG = LoggerFactory.getLogger(DynamicPythonWindowReduceFunction.class);
    private final String codeBody;
    private transient PythonEvaluator evaluator;

    public DynamicPythonWindowReduceFunction(String codeBody) {
        this.codeBody = codeBody;
    }

    private void ensureInit() {
        if (evaluator == null) {
            evaluator = new PythonEvaluator(codeBody, "def process(value1, value2)");
            evaluator.open();
            LOG.info("Python WindowReduceFunction (lightweight) initialized");
        }
    }

    @Override
    public String reduce(String value1, String value2) throws Exception {
        ensureInit();
        return evaluator.execute(value1, value2).asString();
    }
}
