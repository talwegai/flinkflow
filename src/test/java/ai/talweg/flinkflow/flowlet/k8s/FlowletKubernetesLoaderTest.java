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


package ai.talweg.flinkflow.flowlet.k8s;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import ai.talweg.flinkflow.config.StepConfig;
import ai.talweg.flinkflow.flowlet.FlowletParameter;
import ai.talweg.flinkflow.flowlet.FlowletSpec;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnableKubernetesMockClient
public class FlowletKubernetesLoaderTest {

    KubernetesClient client;
    KubernetesMockServer server;

    @Test
    public void testSuccessfulFlowletDiscovery() {
        String mockResponseJson = "{"
            + "\"apiVersion\":\"flinkflow.io/v1alpha1\","
            + "\"kind\":\"FlowletList\","
            + "\"metadata\":{},"
            + "\"items\":[{"
            +   "\"apiVersion\":\"flinkflow.io/v1alpha1\","
            +   "\"kind\":\"Flowlet\","
            +   "\"metadata\":{\"name\":\"mock-confluent-source\",\"namespace\":\"test-namespace\"},"
            +   "\"spec\":{"
            +     "\"title\":\"Mock Confluent Source\","
            +     "\"description\":\"Mock flowlet reading from confluent\","
            +     "\"type\":\"source\","
            +     "\"version\":\"1.0.0\","
            +     "\"parameters\":{\"myParam\":{\"required\":true,\"defaultValue\":\"default-val\"}},"
            +     "\"template\":[{\"type\":\"source\",\"name\":\"kafka-source-internal\"}]"
            +   "}"
            + "}]"
            + "}";

        server.expect().get().withPath("/apis/flinkflow.io/v1alpha1/namespaces/test-namespace/flowlets")
                .andReturn(200, mockResponseJson).always();

        FlowletKubernetesLoader loader = new FlowletKubernetesLoader(() -> server.createClient());
        List<FlowletSpec> flowlets = loader.load("test-namespace");

        assertEquals(1, flowlets.size());
        FlowletSpec loadedSpec = flowlets.get(0);
        
        assertEquals("mock-confluent-source", loadedSpec.getName());
        assertEquals("Mock Confluent Source", loadedSpec.getTitle());
        assertEquals("Mock flowlet reading from confluent", loadedSpec.getDescription());
        assertEquals("source", loadedSpec.getType());
        assertEquals("1.0.0", loadedSpec.getVersion());
        
        assertTrue(loadedSpec.getParameters().containsKey("myParam"));
        assertEquals("default-val", loadedSpec.getParameters().get("myParam").getDefaultValue());

        assertEquals(1, loadedSpec.getTemplate().size());
        assertEquals("kafka-source-internal", loadedSpec.getTemplate().get(0).getName());
    }

    @Test
    public void testEmptyNamespaceDiscovery() {
        server.expect().get().withPath("/apis/flinkflow.io/v1alpha1/namespaces/empty-namespace/flowlets")
                .andReturn(404, "Not Found").always();

        FlowletKubernetesLoader loader = new FlowletKubernetesLoader();
        List<FlowletSpec> flowlets = loader.load("empty-namespace");
        assertTrue(flowlets.isEmpty());
    }
}
