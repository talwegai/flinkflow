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
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.Resource;
import org.apache.camel.support.ResourceHelper;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

/**
 * A helper class that manages a Camel Context for executing YAML DSL routes.
 * It is used within Flink functions to allow complex transformations.
 */
public class CamelYamlEvaluator implements Serializable {
    private final String yamlRoutes;
    private transient CamelContext context;
    private transient ProducerTemplate producerTemplate;

    public CamelYamlEvaluator(String yamlRoutes) {
        this.yamlRoutes = yamlRoutes;
    }

    public void open() {
        try {
            if (context == null) {
                context = new DefaultCamelContext();
                
                // Load YAML routes from string
                Resource resource = ResourceHelper.fromString("dynamic-route.yaml", yamlRoutes);
                org.apache.camel.support.PluginHelper.getRoutesLoader(context).loadRoutes(resource);
                
                context.start();
                producerTemplate = context.createProducerTemplate();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to start Camel Context with YAML DSL", e);
        }
    }

    public Object evaluate(Object input) {
        if (context == null || producerTemplate == null) {
            open();
        }
        // We assume the YAML route starts with "direct:start"
        return producerTemplate.requestBody("direct:start", input);
    }

    public void close() {
        if (context != null) {
            try {
                if (producerTemplate != null) {
                    producerTemplate.stop();
                }
                context.stop();
            } catch (Exception ignored) {}
        }
    }
}
