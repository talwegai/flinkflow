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
import org.apache.flink.api.common.functions.RichReduceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Flink RichReduceFunction that executes Python code via GraalVM.
 */
public class DynamicPythonReduceFunction extends RichReduceFunction<String> {
    private static final Logger LOG = LoggerFactory.getLogger(DynamicPythonReduceFunction.class);
    private final String codeBody;
    private transient PythonEvaluator evaluator;

    public DynamicPythonReduceFunction(String codeBody) {
        this.codeBody = codeBody;
    }

    @Override
    public void open(OpenContext parameters) throws Exception {
        evaluator = new PythonEvaluator(codeBody, "def process(value1, value2)");
        evaluator.open();
        LOG.info("Python ReduceFunction initialized");
    }

    @Override
    public String reduce(String value1, String value2) throws Exception {
        return evaluator.execute(value1, value2).asString();
    }

    @Override
    public void close() {
        if (evaluator != null) {
            evaluator.close();
        }
    }
}
