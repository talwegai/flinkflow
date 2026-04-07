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

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.ProcessJoinFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Flink ProcessJoinFunction that executes Python code via GraalVM during a join.
 */
public class DynamicPythonJoinFunction extends ProcessJoinFunction<String, String, String> {
    private static final Logger LOG = LoggerFactory.getLogger(DynamicPythonJoinFunction.class);
    private final String codeBody;
    private transient PythonEvaluator evaluator;

    public DynamicPythonJoinFunction(String codeBody) {
        this.codeBody = codeBody;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        evaluator = new PythonEvaluator(codeBody, "def process(left, right)");
        evaluator.open();
        LOG.info("Python JoinFunction initialized");
    }

    @Override
    public void processElement(String left, String right, Context context, Collector<String> out) throws Exception {
        org.graalvm.polyglot.Value result = evaluator.execute(left, right);
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
