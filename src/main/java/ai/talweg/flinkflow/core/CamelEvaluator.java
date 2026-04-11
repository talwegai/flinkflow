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

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import java.io.Serializable;

/**
 * A helper class that abstracts Apache Camel context management and
 * expression evaluation for use in Flink dynamic functions.
 */
public class CamelEvaluator implements Serializable {
    private final String language;
    private final String expressionString;
    private transient CamelContext context;
    private transient Expression expression;
    private transient org.apache.camel.Predicate predicate;

    public CamelEvaluator(String expressionString, String language) {
        this.expressionString = expressionString;
        this.language = language != null ? language : "simple";
    }

    public void open() {
        try {
            if (context == null) {
                context = new DefaultCamelContext();
                context.start();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to start Camel Context", e);
        }
    }

    public Object evaluate(Object input) {
        return evaluate(input, null);
    }

    public Object evaluate(Object input, java.util.Map<String, Object> headers) {
        if (expression == null) {
            expression = context.resolveLanguage(language).createExpression(expressionString);
        }
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setBody(input);
        if (headers != null) {
            for (java.util.Map.Entry<String, Object> entry : headers.entrySet()) {
                exchange.getIn().setHeader(entry.getKey(), entry.getValue());
            }
        }
        return expression.evaluate(exchange, Object.class);
    }

    public boolean evaluateCondition(Object input) {
        if (predicate == null) {
            predicate = context.resolveLanguage(language).createPredicate(expressionString);
        }
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setBody(input);
        return predicate.matches(exchange);
    }

    public void close() {
        if (context != null) {
            try {
                context.stop();
            } catch (Exception ignored) {}
        }
    }
}
