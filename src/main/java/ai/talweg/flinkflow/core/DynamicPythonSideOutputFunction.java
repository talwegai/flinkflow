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
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * A Flink ProcessFunction that executes Python code via GraalVM, supporting side outputs.
 */
public class DynamicPythonSideOutputFunction extends ProcessFunction<String, String> {
    private static final Logger LOG = LoggerFactory.getLogger(DynamicPythonSideOutputFunction.class);
    private final String codeBody;
    private final OutputTag<String> outputTag;
    private transient PythonEvaluator evaluator;

    public DynamicPythonSideOutputFunction(String codeBody, String sideOutputName) {
        this.codeBody = codeBody;
        this.outputTag = new OutputTag<String>(sideOutputName) {};
    }

    @Override
    public void open(OpenContext parameters) throws Exception {
        // Python signature: def process(input, side_emit)
        // side_emit is a function that can be called with a string
        evaluator = new PythonEvaluator(codeBody, "def process(input, side_emit)");
        evaluator.open();
        LOG.info("Python SideOutputFunction initialized");
    }

    @Override
    public void processElement(String value, Context ctx, Collector<String> out) throws Exception {
        // Create a bridge for side_emit
        Consumer<String> sideEmit = (record) -> ctx.output(outputTag, record);

        // Execute Python
        // It can return a value (for main stream) or call side_emit
        org.graalvm.polyglot.Value result = evaluator.execute(value, sideEmit);
        
        if (result != null && !result.isNull()) {
            out.collect(result.asString());
        }
    }

    @Override
    public void close() {
        if (evaluator != null) {
            evaluator.close();
        }
    }
}
