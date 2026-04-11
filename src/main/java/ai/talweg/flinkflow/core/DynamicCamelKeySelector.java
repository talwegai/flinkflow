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

import org.apache.flink.api.java.functions.KeySelector;

/**
 * A Flink KeySelector that evaluates a Camel expression (Simple, JsonPath, etc.)
 * to extract a partition key.
 */
public class DynamicCamelKeySelector implements KeySelector<String, String> {
    private final CamelEvaluator evaluator;
    private boolean opened = false;

    public DynamicCamelKeySelector(String expression, String language) {
        this.evaluator = new CamelEvaluator(expression, language);
    }

    @Override
    public String getKey(String value) {
        if (!opened) {
            evaluator.open();
            opened = true;
        }
        Object result = evaluator.evaluate(value);
        return result != null ? result.toString() : "";
    }

    public void close() {
        if (evaluator != null) {
            evaluator.close();
        }
    }
}
