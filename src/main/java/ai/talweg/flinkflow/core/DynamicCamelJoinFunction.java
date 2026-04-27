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
import org.apache.flink.streaming.api.functions.co.ProcessJoinFunction;
import org.apache.flink.util.Collector;

import java.util.HashMap;
import java.util.Map;

/**
 * A Flink ProcessJoinFunction that evaluates a Camel expression during join.
 * The left record is exposed as body, and both records are available in headers:
 * - header.left
 * - header.right
 */
public class DynamicCamelJoinFunction extends ProcessJoinFunction<String, String, String> {
    private final CamelEvaluator evaluator;

    public DynamicCamelJoinFunction(String expression, String language) {
        this.evaluator = new CamelEvaluator(expression, language);
    }

    @Override
    public void open(OpenContext parameters) {
        evaluator.open();
    }

    @Override
    public void processElement(String left, String right, Context context, Collector<String> out) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("left", left);
        headers.put("right", right);

        Object result = evaluator.evaluate(left, headers);
        if (result != null) {
            out.collect(result.toString());
        }
    }

    @Override
    public void close() {
        evaluator.close();
    }
}
