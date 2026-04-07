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


package ai.talweg.flinkflow.config.k8s;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import ai.talweg.flinkflow.config.JobConfig;
import ai.talweg.flinkflow.config.StepConfig;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnableKubernetesMockClient
public class PipelineKubernetesLoaderTest {

    KubernetesClient client;
    KubernetesMockServer server;

    @Test
    public void testSuccessfulPipelineLoad() {
        String mockResponseJson = "{"
            + "\"apiVersion\":\"flinkflow.io/v1alpha1\","
            + "\"kind\":\"Pipeline\","
            + "\"metadata\":{\"name\":\"test-pipeline\",\"namespace\":\"test-namespace\"},"
            + "\"spec\":{"
            +   "\"name\":\"Mock Job\","
            +   "\"parallelism\":3,"
            +   "\"steps\":[{\"type\":\"source\",\"name\":\"mock-source\"}]"
            + "}"
            + "}";

        server.expect().get().withPath("/apis/flinkflow.io/v1alpha1/namespaces/test-namespace/pipelines/test-pipeline")
                .andReturn(200, mockResponseJson).always();

        PipelineKubernetesLoader loader = new PipelineKubernetesLoader(() -> server.createClient());
        JobConfig jobConfig = loader.load("test-pipeline", "test-namespace");

        assertNotNull(jobConfig);
        assertEquals("Mock Job", jobConfig.getName());
        assertEquals(3, jobConfig.getParallelism());
        assertEquals(1, jobConfig.getSteps().size());
        assertEquals("mock-source", jobConfig.getSteps().get(0).getName());
    }

    @Test
    public void testPipelineNotFound() {
        server.expect().get().withPath("/apis/flinkflow.io/v1alpha1/namespaces/test-namespace/pipelines/unknown-pipeline")
                .andReturn(404, "Not Found").always();

        PipelineKubernetesLoader loader = new PipelineKubernetesLoader(() -> server.createClient());
        
        try {
            loader.load("unknown-pipeline", "test-namespace");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Failed to load Pipeline from Kubernetes"));
        }
    }
}
