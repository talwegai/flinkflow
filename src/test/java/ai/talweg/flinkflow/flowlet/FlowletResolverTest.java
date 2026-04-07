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


package ai.talweg.flinkflow.flowlet;

import ai.talweg.flinkflow.config.StepConfig;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FlowletResolverTest {

    @Test
    public void testBasicResolutionAndSubstitution() {
        // 1. Create a dummy Flowlet Registry
        FlowletRegistry registry = new FlowletRegistry(null, false);
        
        // 2. Mock a FlowletSpec
        FlowletSpec spec = new FlowletSpec();
        spec.setName("test-source");
        
        Map<String, FlowletParameter> params = new HashMap<>();
        FlowletParameter topicParam = new FlowletParameter();
        topicParam.setRequired(true);
        params.put("topic", topicParam);
        
        FlowletParameter groupParam = new FlowletParameter();
        groupParam.setDefaultValue("default-group");
        params.put("group", groupParam);
        spec.setParameters(params);

        StepConfig internalStep = new StepConfig();
        internalStep.setType("source");
        internalStep.setName("kafka-{{topic}}");
        Map<String, String> props = new HashMap<>();
        props.put("topic", "{{topic}}");
        props.put("group.id", "{{group}}");
        internalStep.setProperties(props);
        spec.setTemplate(List.of(internalStep));

        // Inject into the private catalog map of Registry using reflection or add a package-private setter 
        // For test purposes, we will mock the registry behavior indirectly by extending it or using reflection.
        try {
            java.lang.reflect.Field catalogField = FlowletRegistry.class.getDeclaredField("catalog");
            catalogField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, FlowletSpec> catalog = (Map<String, FlowletSpec>) catalogField.get(registry);
            catalog.put("test-source", spec);
        } catch (Exception e) {
            fail("Failed to inject mock spec into registry");
        }

        // 3. Create the Resolver
        FlowletResolver resolver = new FlowletResolver(registry);

        // 4. Create the caller step
        StepConfig callerStep = new StepConfig();
        callerStep.setType("flowlet");
        callerStep.setName("test-source");
        Map<String, String> callerWith = new HashMap<>();
        callerWith.put("topic", "my-test-topic");
        callerStep.setWith(callerWith);

        // 5. Resolve!
        List<StepConfig> resolvedSteps = resolver.resolve(callerStep);

        // 6. Assertions
        assertEquals(1, resolvedSteps.size());
        StepConfig resolved = resolvedSteps.get(0);
        
        assertEquals("source", resolved.getType());
        assertEquals("kafka-my-test-topic", resolved.getName(), "Name placeholder not substituted");
        
        Map<String, String> resolvedProps = resolved.getProperties();
        assertEquals("my-test-topic", resolvedProps.get("topic"), "Required parameter not substituted");
        assertEquals("default-group", resolvedProps.get("group.id"), "Default parameter not substituted");
    }

    @Test
    public void testMissingRequiredParameterThrowsException() {
        FlowletRegistry registry = new FlowletRegistry(null, false);
        
        FlowletSpec spec = new FlowletSpec();
        spec.setName("strict-flowlet");
        
        Map<String, FlowletParameter> params = new HashMap<>();
        FlowletParameter requiredParam = new FlowletParameter();
        requiredParam.setRequired(true);
        params.put("mandatoryKey", requiredParam);
        spec.setParameters(params);

        try {
            java.lang.reflect.Field catalogField = FlowletRegistry.class.getDeclaredField("catalog");
            catalogField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, FlowletSpec> catalog = (Map<String, FlowletSpec>) catalogField.get(registry);
            catalog.put("strict-flowlet", spec);
        } catch (Exception e) {
            fail("Failed to inject mock spec into registry");
        }

        FlowletResolver resolver = new FlowletResolver(registry);
        
        StepConfig callerStep = new StepConfig();
        callerStep.setType("flowlet");
        callerStep.setName("strict-flowlet");
        // We omit 'with' entirely here
        
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            resolver.resolve(callerStep);
        });
        
        assertTrue(thrown.getMessage().contains("missing required parameter(s): [mandatoryKey]"));
    }

    @Test
    public void testParameterTypeValidationThrowsException() {
        FlowletRegistry registry = new FlowletRegistry(null, false);
        
        FlowletSpec spec = new FlowletSpec();
        spec.setName("type-flowlet");
        
        Map<String, FlowletParameter> params = new HashMap<>();
        FlowletParameter intParam = new FlowletParameter();
        intParam.setType("integer");
        params.put("port", intParam);

        FlowletParameter boolParam = new FlowletParameter();
        boolParam.setType("boolean");
        params.put("debug", boolParam);

        spec.setParameters(params);

        try {
            java.lang.reflect.Field catalogField = FlowletRegistry.class.getDeclaredField("catalog");
            catalogField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, FlowletSpec> catalog = (Map<String, FlowletSpec>) catalogField.get(registry);
            catalog.put("type-flowlet", spec);
        } catch (Exception e) {
            fail("Failed to inject mock spec into registry");
        }

        FlowletResolver resolver = new FlowletResolver(registry);
        
        // 1. Test invalid integer
        StepConfig callerStep1 = new StepConfig();
        callerStep1.setType("flowlet");
        callerStep1.setName("type-flowlet");
        Map<String, String> callerWith1 = new HashMap<>();
        callerWith1.put("port", "not-an-int");
        callerStep1.setWith(callerWith1);
        
        IllegalArgumentException thrown1 = assertThrows(IllegalArgumentException.class, () -> {
            resolver.resolve(callerStep1);
        });
        assertTrue(thrown1.getMessage().contains("must be of type 'integer', but received: 'not-an-int'"));

        // 2. Test invalid boolean
        StepConfig callerStep2 = new StepConfig();
        callerStep2.setType("flowlet");
        callerStep2.setName("type-flowlet");
        Map<String, String> callerWith2 = new HashMap<>();
        callerWith2.put("port", "8080");
        callerWith2.put("debug", "yes"); // invalid bool
        callerStep2.setWith(callerWith2);
        
        IllegalArgumentException thrown2 = assertThrows(IllegalArgumentException.class, () -> {
            resolver.resolve(callerStep2);
        });
        assertTrue(thrown2.getMessage().contains("must be of type 'boolean', but received: 'yes'"));
        
        // 3. Test valid execution
        Map<String, String> callerWith3 = new HashMap<>();
        callerWith3.put("port", "8080");
        callerWith3.put("debug", "true"); 
        callerStep2.setWith(callerWith3);
        
        // Should not throw
        assertDoesNotThrow(() -> resolver.resolve(callerStep2));
    }
}
