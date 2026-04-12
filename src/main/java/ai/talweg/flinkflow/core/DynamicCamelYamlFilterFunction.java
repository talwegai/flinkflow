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
 * A Flink FilterFunction that evaluates a Camel YAML DSL route fragment.
 * The route must return a boolean or a value that can be interpreted as one.
 */
public class DynamicCamelYamlFilterFunction extends RichFilterFunction<String> {
    private final CamelYamlEvaluator evaluator;

    public DynamicCamelYamlFilterFunction(String yamlRoutes) {
        this.evaluator = new CamelYamlEvaluator(yamlRoutes);
    }

    @Override
    public void open(OpenContext parameters) {
        evaluator.open();
    }

    @Override
    public boolean filter(String value) {
        Object result = evaluator.evaluate(value);
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        return result != null && Boolean.parseBoolean(result.toString());
    }

    @Override
    public void close() {
        evaluator.close();
    }
}
