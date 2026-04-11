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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

public class CamelEvaluatorTest {

    private CamelEvaluator evaluator;

    @Test
    public void testSimpleExpression() {
        evaluator = new CamelEvaluator("Hello ${body}", "simple");
        evaluator.open();
        
        Object result = evaluator.evaluate("World");
        assertEquals("Hello World", result);
        
        evaluator.close();
    }

    @Test
    public void testSimpleWithHeaders() {
        evaluator = new CamelEvaluator("User ${header.user} says ${body}", "simple");
        evaluator.open();
        
        Map<String, Object> headers = new HashMap<>();
        headers.put("user", "Alice");
        
        Object result = evaluator.evaluate("Hello", headers);
        assertEquals("User Alice says Hello", result);
        
        evaluator.close();
    }

    @Test
    public void testJsonPathExpression() {
        evaluator = new CamelEvaluator("$.user.name", "jsonpath");
        evaluator.open();
        
        String json = "{\"user\": {\"name\": \"Bob\", \"age\": 30}}";
        Object result = evaluator.evaluate(json);
        assertEquals("Bob", result);
        
        evaluator.close();
    }

    @Test
    public void testConditionEvaluation() {
        // Test simple predicate
        evaluator = new CamelEvaluator("${body} > 100", "simple");
        evaluator.open();
        
        assertTrue(evaluator.evaluateCondition(150));
        assertFalse(evaluator.evaluateCondition(50));
        
        evaluator.close();
    }

    @Test
    public void testGroovyEvaluation() {
        evaluator = new CamelEvaluator("body.toUpperCase()", "groovy");
        evaluator.open();
        
        Object result = evaluator.evaluate("input");
        assertEquals("INPUT", result);
        
        evaluator.close();
    }
}
