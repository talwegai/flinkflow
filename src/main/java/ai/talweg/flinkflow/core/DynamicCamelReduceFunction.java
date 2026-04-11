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
import java.util.HashMap;
import java.util.Map;

/**
 * A Flink ReduceFunction that evaluates a Camel expression (Simple, JsonPath, Groovy, etc.)
 * to combine two elements in a stream or window.
 *
 * NOTE: This implements a plain ReduceFunction (not Rich) to satisfy Flink's
 * WindowedStream.reduce() requirements.
 */
public class DynamicCamelReduceFunction implements ReduceFunction<String> {
    private final CamelEvaluator evaluator;
    private boolean opened = false;

    public DynamicCamelReduceFunction(String expression, String language) {
        this.evaluator = new CamelEvaluator(expression, language);
    }

    @Override
    public String reduce(String value1, String value2) {
        // Lazy initialization because we are not in a RichFunction
        if (!opened) {
            evaluator.open();
            opened = true;
        }

        Map<String, Object> headers = new HashMap<>();
        headers.put("value1", value1);
        headers.put("value2", value2);
        
        // Use the first value as the initial body, and provide both values in headers
        Object result = evaluator.evaluate(value1, headers);
        return result != null ? result.toString() : null;
    }

    public void close() {
        if (evaluator != null) {
            evaluator.close();
        }
    }
}
