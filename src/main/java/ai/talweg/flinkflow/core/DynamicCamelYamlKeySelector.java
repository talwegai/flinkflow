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
 * A Flink KeySelector that evaluates a Camel YAML DSL route fragment.
 * The route must return the key to be used for partitioning.
 */
public class DynamicCamelYamlKeySelector implements KeySelector<String, String> {
    private final CamelYamlEvaluator evaluator;

    public DynamicCamelYamlKeySelector(String yamlRoutes) {
        this.evaluator = new CamelYamlEvaluator(yamlRoutes);
    }

    @Override
    public String getKey(String value) {
        Object result = evaluator.evaluate(value);
        return result != null ? result.toString() : null;
    }

    public void close() {
        if (evaluator != null) {
            evaluator.close();
        }
    }
}
