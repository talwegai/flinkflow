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


package ai.talweg.flinkflow.config;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnableKubernetesMockClient(crud = true)
public class SecretResolverTest {

    KubernetesClient client;

    @Test
    public void testResolveSecret() {
        // Setup mock secret
        Map<String, String> data = new HashMap<>();
        data.put("password", Base64.getEncoder().encodeToString("secret-password".getBytes()));
        
        Secret secret = new SecretBuilder()
                .withNewMetadata().withName("my-secret").endMetadata()
                .withData(data)
                .build();
        
        client.secrets().inNamespace("default").resource(secret).create();

        try (SecretResolver resolver = new SecretResolver(() -> client)) {
            // Test resolution
            String resolvedValue = resolver.resolve("secret:my-secret/password", "default");
            assertEquals("secret-password", resolvedValue);

            // Test non-secret value
            assertEquals("normal-value", resolver.resolve("normal-value", "default"));
            
            // Test null value
            assertEquals(null, resolver.resolve(null, "default"));
        }
    }

    @Test
    public void testResolveStepSecrets() {
        Map<String, String> data = new HashMap<>();
        data.put("apiKey", Base64.getEncoder().encodeToString("test-api-key".getBytes()));
        
        Secret secret = new SecretBuilder()
                .withNewMetadata().withName("api-secret").endMetadata()
                .withData(data)
                .build();
        
        client.secrets().inNamespace("test-ns").resource(secret).create();

        try (SecretResolver resolver = new SecretResolver(() -> client)) {
            StepConfig step = new StepConfig();
            Map<String, String> props = new HashMap<>();
            props.put("url", "http://example.com");
            props.put("key", "secret:api-secret/apiKey");
            step.setProperties(props);

            resolver.resolveStepSecrets(step, "test-ns");

            assertEquals("http://example.com", step.getProperties().get("url"));
            assertEquals("test-api-key", step.getProperties().get("key"));
        }
    }

    @Test
    public void testSecretNotFound() {
        try (SecretResolver resolver = new SecretResolver(() -> client)) {
            assertThrows(RuntimeException.class, () -> {
                resolver.resolve("secret:nonexistent/key", "default");
            });
        }
    }

    @Test
    public void testKeyNotFound() {
        Secret secret = new SecretBuilder()
                .withNewMetadata().withName("my-secret").endMetadata()
                .withData(new HashMap<>())
                .build();
        client.secrets().inNamespace("default").resource(secret).create();

        try (SecretResolver resolver = new SecretResolver(() -> client)) {
            assertThrows(RuntimeException.class, () -> {
                resolver.resolve("secret:my-secret/missing-key", "default");
            });
        }
    }
}
