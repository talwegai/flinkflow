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

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import ai.talweg.flinkflow.config.JobConfig;

import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Loads a {@link JobConfig} from a Kubernetes {@code Pipeline} Custom Resource.
 */
public class PipelineKubernetesLoader {

    private static final Logger LOG = Logger.getLogger(PipelineKubernetesLoader.class.getName());

    private final Supplier<KubernetesClient> clientSupplier;

    public PipelineKubernetesLoader() {
        this.clientSupplier = () -> new KubernetesClientBuilder().build();
    }

    public PipelineKubernetesLoader(Supplier<KubernetesClient> clientSupplier) {
        this.clientSupplier = clientSupplier;
    }

    public JobConfig load(String name, String namespace) {
        try (KubernetesClient client = clientSupplier.get()) {
            String targetNamespace = (namespace != null && !namespace.isBlank())
                    ? namespace
                    : client.getNamespace();

            LOG.info("Fetching Pipeline CR '" + name + "' from namespace '" + targetNamespace + "'");

            Pipeline pipeline = client.resources(Pipeline.class)
                    .inNamespace(targetNamespace)
                    .withName(name)
                    .get();

            if (pipeline == null) {
                throw new RuntimeException(
                        "Pipeline CR '" + name + "' not found in namespace '" + targetNamespace + "'");
            }

            PipelineSpec spec = pipeline.getSpec();
            JobConfig jobConfig = new JobConfig();
            jobConfig.setName(spec.getName() != null ? spec.getName() : pipeline.getMetadata().getName());
            jobConfig.setParallelism(spec.getParallelism());
            jobConfig.setSteps(spec.getSteps());

            return jobConfig;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Pipeline from Kubernetes: " + e.getMessage(), e);
        }
    }
}
