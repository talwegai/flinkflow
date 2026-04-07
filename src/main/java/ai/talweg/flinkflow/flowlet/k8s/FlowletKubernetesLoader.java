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

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import ai.talweg.flinkflow.flowlet.FlowletSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads {@link FlowletSpec} instances from Kubernetes by listing installed
 * {@code Flowlet} Custom Resources via the Fabric8 client.
 *
 * <h3>Discovery logic</h3>
 * <ol>
 * <li>If an explicit namespace is given, only that namespace is searched.</li>
 * <li>If no namespace is given and the job is running in-cluster, the
 * pod's own namespace (from the mounted service-account token) is used.</li>
 * <li>If the Kubernetes API is unreachable the loader returns an empty list
 * and logs a warning — the registry then falls back to its classpath
 * and file-system sources.</li>
 * </ol>
 *
 * <h3>Priority</h3>
 * Flowlets discovered from Kubernetes <em>override</em> same-named classpath
 * Flowlets, giving cluster administrators full control over the catalog.
 */
public class FlowletKubernetesLoader {

    private static final Logger LOG = Logger.getLogger(FlowletKubernetesLoader.class.getName());

    private final java.util.function.Supplier<KubernetesClient> clientSupplier;

    public FlowletKubernetesLoader() {
        this.clientSupplier = () -> new KubernetesClientBuilder().build();
    }

    public FlowletKubernetesLoader(java.util.function.Supplier<KubernetesClient> clientSupplier) {
        this.clientSupplier = clientSupplier;
    }

    /**
     * Loads all {@code Flowlet} CRs from the Kubernetes cluster.
     *
     * @param namespace explicit namespace to search, or {@code null} to use
     *                  the in-cluster service-account namespace
     * @return list of resolved {@link FlowletSpec} instances (never {@code null})
     */
    public List<FlowletSpec> load(String namespace) {
        List<FlowletSpec> specs = new ArrayList<>();

        try (KubernetesClient client = clientSupplier.get()) {

            // Determine the target namespace
            String targetNamespace = (namespace != null && !namespace.isBlank())
                    ? namespace
                    : client.getNamespace(); // reads from in-cluster token

            LOG.info("Discovering Flowlet CRs from namespace: " + targetNamespace);

            var flowletClient = client
                    .resources(Flowlet.class, FlowletList.class)
                    .inNamespace(targetNamespace);

            List<Flowlet> items = flowletClient.list().getItems();
            LOG.info("Found " + items.size() + " Flowlet CR(s) in namespace '" + targetNamespace + "'.");

            for (Flowlet resource : items) {
                FlowletSpec spec = toFlowletSpec(resource);
                if (spec != null) {
                    specs.add(spec);
                }
            }

        } catch (KubernetesClientException e) {
            LOG.log(Level.WARNING,
                    "Could not reach Kubernetes API to load Flowlet CRs: "
                            + e.getMessage());
        } catch (Exception e) {
            LOG.log(Level.WARNING,
                    "Unexpected error loading Flowlet CRs from Kubernetes: " + e.getMessage(), e);
        }

        return specs;
    }

    // ------------------------------------------------------------------ //
    // Conversion helper
    // ------------------------------------------------------------------ //

    /**
     * Converts a {@link FlowletResource} (Kubernetes CR) into a unified
     * {@link FlowletSpec} by combining {@code metadata.name} with the CR's
     * {@code spec} payload.
     */
    private FlowletSpec toFlowletSpec(Flowlet resource) {
        if (resource == null || resource.getMetadata() == null)
            return null;
        FlowletKubernetesSpec k8sSpec = resource.getSpec();
        if (k8sSpec == null) {
            LOG.warning("Flowlet CR '" + resource.getMetadata().getName()
                    + "' has no spec — skipping.");
            return null;
        }

        FlowletSpec spec = new FlowletSpec();
        // The canonical name comes from metadata.name on the K8s resource
        spec.setName(resource.getMetadata().getName());
        spec.setTitle(k8sSpec.getTitle());
        spec.setDescription(k8sSpec.getDescription());
        spec.setType(k8sSpec.getType());
        spec.setVersion(k8sSpec.getVersion());
        spec.setLabels(k8sSpec.getLabels());
        spec.setParameters(k8sSpec.getParameters());
        spec.setTemplate(k8sSpec.getTemplate());
        return spec;
    }
}
