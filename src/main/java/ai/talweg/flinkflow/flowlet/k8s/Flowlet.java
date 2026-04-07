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

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

/**
 * Fabric8 model for a {@code Flowlet} Kubernetes Custom Resource.
 *
 * <p>
 * Example CR on cluster:
 * </p>
 * 
 * <pre>
 * apiVersion: flinkflow.io/v1alpha1
 * kind: Flowlet
 * metadata:
 *   name: confluent-kafka-source
 *   namespace: flink-jobs
 * spec:
 *   title: "Confluent Kafka Source"
 *   type: source
 *   parameters:
 *     bootstrapServers:
 *       required: true
 *   template:
 *     - type: source
 *       name: kafka-source
 *       properties:
 *         bootstrap.servers: "{{bootstrapServers}}"
 * </pre>
 *
 * <p>
 * Instances of this class are created by the Fabric8 client when listing
 * {@code Flowlet} resources from the cluster API, and are then converted to
 * {@link ai.talweg.flinkflow.flowlet.FlowletSpec} by the
 * {@link FlowletKubernetesLoader}.
 * </p>
 */
@Group("flinkflow.io")
@Version("v1alpha1")
@ShortNames("fl")
public class Flowlet
        extends CustomResource<FlowletKubernetesSpec, Void>
        implements Namespaced {
    // Fabric8 generates boilerplate. The spec is typed via the generic parameter.
}
