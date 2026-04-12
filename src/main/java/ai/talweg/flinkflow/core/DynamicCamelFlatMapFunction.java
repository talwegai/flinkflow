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
import java.util.Collection;

/**
 * A Flink FlatMapFunction that evaluates a Camel expression.
 * If the expression returns a Collection or Array, each element is emitted individually.
 */
public class DynamicCamelFlatMapFunction extends RichFlatMapFunction<String, String> {
    private final CamelEvaluator evaluator;

    public DynamicCamelFlatMapFunction(String expression, String language) {
        this.evaluator = new CamelEvaluator(expression, language);
    }

    @Override
    public void open(OpenContext parameters) {
        evaluator.open();
    }

    @Override
    public void flatMap(String value, Collector<String> out) {
        Object result = evaluator.evaluate(value);
        if (result == null) return;

        if (result instanceof Collection) {
            for (Object item : (Collection<?>) result) {
                if (item != null) out.collect(item.toString());
            }
        } else if (result.getClass().isArray()) {
            Object[] array = (Object[]) result;
            for (Object item : array) {
                if (item != null) out.collect(item.toString());
            }
        } else {
            out.collect(result.toString());
        }
    }

    @Override
    public void close() {
        evaluator.close();
    }
}
