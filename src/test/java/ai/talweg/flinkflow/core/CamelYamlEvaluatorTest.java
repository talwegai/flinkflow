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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CamelYamlEvaluatorTest {

    @Test
    public void testSimpleYamlRoute() {
        String yaml = 
            "- from:\n" +
            "    uri: \"direct:start\"\n" +
            "    steps:\n" +
            "      - setBody:\n" +
            "          simple: \"Hello ${body}\"";
        
        CamelYamlEvaluator evaluator = new CamelYamlEvaluator(yaml);
        evaluator.open();
        
        Object result = evaluator.evaluate("World");
        assertEquals("Hello World", result);
        
        evaluator.close();
    }

    @Test
    public void testYamlRouteWithChoice() {
        String yaml = 
            "- from:\n" +
            "    uri: \"direct:start\"\n" +
            "    steps:\n" +
            "      - choice:\n" +
            "          when:\n" +
            "            - simple: \"${body} > 10\"\n" +
            "              steps:\n" +
            "                - setBody:\n" +
            "                    constant: \"HIGH\"\n" +
            "          otherwise:\n" +
            "            steps:\n" +
            "              - setBody:\n" +
            "                  constant: \"LOW\"";
        
        CamelYamlEvaluator evaluator = new CamelYamlEvaluator(yaml);
        evaluator.open();
        
        assertEquals("HIGH", evaluator.evaluate(20));
        assertEquals("LOW", evaluator.evaluate(5));
        
        evaluator.close();
    }
}
