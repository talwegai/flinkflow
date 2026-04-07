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

import ai.talweg.flinkflow.flowlet.k8s.FlowletKubernetesLoader;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registry that discovers and loads {@link FlowletSpec} definitions from three
 * sources, applied in order of increasing priority:
 *
 * <ol>
 * <li><b>Classpath (lowest)</b> — built-in {@code flowlets/} directory
 * packaged inside the Flinkflow JAR.</li>
 * <li><b>Kubernetes CRs (medium)</b> — any {@code Flowlet} Custom Resources
 * installed in the current cluster namespace (requires the CRD from
 * {@code k8s/crd-flowlet.yaml} to be applied to the cluster). Loaded via
 * the Fabric8 client; silently skipped when not running in-cluster.</li>
 * <li><b>External directory (highest)</b> — any {@code *.yaml} files in a
 * local directory specified by the caller (e.g. mounted ConfigMap or a
 * development-time {@code ./flowlets} folder).</li>
 * </ol>
 *
 * <p>
 * Later sources override earlier ones with the same name, so cluster
 * administrators can customise or replace built-in Flowlets simply by
 * installing a different CR on the cluster.
 * </p>
 *
 * <h3>Usage</h3>
 * 
 * <pre>
 * // Auto-discovers K8s CRs + classpath built-ins
 * FlowletRegistry registry = new FlowletRegistry(null, null, true);
 *
 * // Local-only (IDE / local run): classpath + optional extra dir, no K8s
 * FlowletRegistry registry = new FlowletRegistry("./flowlets", null, false);
 *
 * // Full cluster mode: built-ins + K8s CRs from a specific namespace
 * FlowletRegistry registry = new FlowletRegistry(null, "flink-jobs", true);
 * </pre>
 */
public class FlowletRegistry {

    private static final Logger LOG = Logger.getLogger(FlowletRegistry.class.getName());

    private final Map<String, FlowletSpec> catalog = new LinkedHashMap<>();

    // ------------------------------------------------------------------ //
    // Constructors
    // ------------------------------------------------------------------ //

    /**
     * Creates a registry that discovers Flowlets exclusively from Kubernetes.
     *
     * @param k8sNamespace Kubernetes namespace to query for installed
     *                     {@code Flowlet} CRs; {@code null} to use the in-cluster
     *                     service-account namespace.
     * @param enableK8s    whether to attempt Kubernetes CR discovery.
     */
    public FlowletRegistry(String k8sNamespace, boolean enableK8s) {
        this(null, k8sNamespace, enableK8s);
    }

    /**
     * Creates a registry that discovers Flowlets from a local directory and/or Kubernetes.
     *
     * @param flowletDirPath Local directory to scan for Flowlet YAMLs.
     * @param k8sNamespace   Kubernetes namespace to query.
     * @param enableK8s      whether to attempt Kubernetes CR discovery.
     */
    public FlowletRegistry(String flowletDirPath, String k8sNamespace, boolean enableK8s) {
        if (flowletDirPath != null) {
            loadFromDirectory(flowletDirPath);
        }

        // Only load from Kubernetes if enabled
        if (enableK8s) {
            loadFromKubernetes(k8sNamespace);
        }

        LOG.info("FlowletRegistry initialised with " + catalog.size()
                + " Flowlet(s): " + catalog.keySet());
    }

    // ------------------------------------------------------------------ //
    // Public API
    // ------------------------------------------------------------------ //

    /**
     * Returns the {@link FlowletSpec} registered under the given name.
     *
     * @param name the Flowlet name (case-insensitive)
     * @return the matching FlowletSpec
     * @throws IllegalArgumentException if no Flowlet with that name is registered
     */
    public FlowletSpec get(String name) {
        FlowletSpec spec = catalog.get(name.toLowerCase(Locale.ROOT));
        if (spec == null) {
            throw new IllegalArgumentException(
                    "Unknown Flowlet: '" + name + "'. Available: " + catalog.keySet());
        }
        return spec;
    }

    /** Returns {@code true} if a Flowlet with the given name is registered. */
    public boolean contains(String name) {
        return catalog.containsKey(name.toLowerCase(Locale.ROOT));
    }

    /** Returns an unmodifiable view of the entire catalog. */
    public Map<String, FlowletSpec> getCatalog() {
        return Collections.unmodifiableMap(catalog);
    }

    /** Returns the number of registered Flowlets. */
    public int size() {
        return catalog.size();
    }

    // ------------------------------------------------------------------ //
    // Loading helpers
    // ------------------------------------------------------------------ //

    /**
     * Scans a local directory for Flowlet YAML files and registers them.
     */
    private void loadFromDirectory(String path) {
        java.io.File dir = new java.io.File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            LOG.warning("Flowlet directory not found: " + path);
            return;
        }

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
        // Configure mapper to ignore unknown fields common in K8s (apiVersion, kind, etc.)
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        java.io.File[] files = dir.listFiles((d, name) -> name.endsWith(".yaml") || name.endsWith(".yml"));
        if (files != null) {
            for (java.io.File file : files) {
                try {
                    // We parse the file as a Flowlet CR to reuse the K8s structure (metadata, spec)
                    ai.talweg.flinkflow.flowlet.k8s.Flowlet resource = mapper.readValue(file, ai.talweg.flinkflow.flowlet.k8s.Flowlet.class);
                    
                    if (resource != null && resource.getMetadata() != null && resource.getSpec() != null) {
                        FlowletSpec spec = new FlowletSpec();
                        spec.setName(resource.getMetadata().getName());
                        spec.setTitle(resource.getSpec().getTitle());
                        spec.setDescription(resource.getSpec().getDescription());
                        spec.setType(resource.getSpec().getType());
                        spec.setVersion(resource.getSpec().getVersion());
                        spec.setLabels(resource.getSpec().getLabels());
                        spec.setParameters(resource.getSpec().getParameters());
                        spec.setTemplate(resource.getSpec().getTemplate());
                        
                        register(spec, "Local directory (" + file.getName() + ")");
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to load flowlet from " + file.getAbsolutePath() + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Loads Flowlet CRs from the Kubernetes cluster using the Fabric8 client.
     * Silently degrades when not running in a cluster or if the CRD is not
     * installed.
     */
    private void loadFromKubernetes(String namespace) {
        try {
            FlowletKubernetesLoader k8sLoader = new FlowletKubernetesLoader();
            List<FlowletSpec> k8sSpecs = k8sLoader.load(namespace);
            for (FlowletSpec spec : k8sSpecs) {
                register(spec, "Kubernetes CR (namespace=" + namespace + ")");
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING,
                    "Kubernetes Flowlet discovery failed: "
                            + e.getMessage());
        }
    }

    private void register(FlowletSpec spec, String source) {
        if (spec == null || spec.getName() == null || spec.getName().isBlank()) {
            LOG.warning("Flowlet loaded from " + source + " has no name — skipping.");
            return;
        }
        String key = spec.getName().toLowerCase(Locale.ROOT);
        if (catalog.containsKey(key)) {
            LOG.info("Overriding Flowlet '" + key + "' with version from: " + source);
        } else {
            LOG.info("Registered Flowlet '" + key + "' from: " + source);
        }
        catalog.put(key, spec);
    }
}
