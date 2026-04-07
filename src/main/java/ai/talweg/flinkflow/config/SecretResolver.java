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

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.util.Base64;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Resolves properties that reference Kubernetes Secrets using the format {@code secret:name/key}.
 */
public class SecretResolver implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(SecretResolver.class.getName());
    private final KubernetesClient client;

    public SecretResolver() {
        this(() -> new KubernetesClientBuilder().build());
    }

    public SecretResolver(Supplier<KubernetesClient> clientSupplier) {
        this.client = clientSupplier.get();
    }

    /**
     * Resolves a single value if it is a secret reference.
     * 
     * @param value     The value to resolve (e.g., "secret:my-secret/my-key").
     * @param namespace The namespace to look in.
     * @return The resolved secret value, or the original value if it's not a secret reference.
     */
    public String resolve(String value, String namespace) {
        if (value == null || !value.startsWith("secret:")) {
            return value;
        }

        // Format: secret:name/key
        String parts = value.substring(7);
        int slashIndex = parts.indexOf('/');
        if (slashIndex == -1) {
            LOG.warning("Invalid secret format: " + value + ". Expected secret:name/key");
            return value;
        }

        String secretName = parts.substring(0, slashIndex);
        String key = parts.substring(slashIndex + 1);

        try {
            String targetNamespace = (namespace != null && !namespace.isBlank())
                    ? namespace
                    : client.getNamespace();

            LOG.info("Resolving secret '" + secretName + "' key '" + key + "' from namespace '" + targetNamespace + "'");

            var secret = client.secrets().inNamespace(targetNamespace)
                    .withName(secretName)
                    .get();

            if (secret == null) {
                throw new RuntimeException("Kubernetes secret '" + secretName + "' not found in namespace '" + targetNamespace + "'");
            }

            String base64Value = secret.getData().get(key);
            if (base64Value == null) {
                throw new RuntimeException("Key '" + key + "' not found in secret '" + secretName + "'");
            }

            return new String(Base64.getDecoder().decode(base64Value));
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve secret reference '" + value + "': " + e.getMessage(), e);
        }
    }

    /**
     * Recursively resolves secret references in all properties and parameters of a step.
     * 
     * @param step      The step to process.
     * @param namespace The namespace to look in.
     */
    public void resolveStepSecrets(StepConfig step, String namespace) {
        if (step.getProperties() != null) {
            for (Map.Entry<String, String> entry : step.getProperties().entrySet()) {
                if (entry.getValue() != null) {
                    entry.setValue(resolve(entry.getValue(), namespace));
                }
            }
        }
        if (step.getWith() != null) {
            for (Map.Entry<String, String> entry : step.getWith().entrySet()) {
                if (entry.getValue() != null) {
                    entry.setValue(resolve(entry.getValue(), namespace));
                }
            }
        }
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
    }
}
