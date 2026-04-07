# ☸️ Kubernetes Manifests & Tools

This directory contains all the resources necessary to deploy and manage Flinkflow in a Kubernetes environment.

## 📂 Resource Map

| File | Type | Description |
| :--- | :--- | :--- |
| **`crds/crd-pipeline.yaml`** | CRD | CustomResourceDefinition for `Pipeline` resources. |
| **`crds/crd-flowlet.yaml`** | CRD | CustomResourceDefinition for `Flowlet` resources. |
| **`rbac.yaml`** | RBAC | Defines the ServiceAccount and Permissions for Flink pods. |
| **`deployment.yaml`** | Manual | Standard Deployment for a manual Flink cluster setup. |
| **`flink-operator-deployment.yaml`** | Operator | Example `FlinkDeployment` for the **Flink Kubernetes Operator**. |
| **`native-pipeline-deployment.yaml`** | Native | Deployment for a Flink application that resolves its config from a `Pipeline` CR. |
| **`monitor-deployment.yaml`** | Dashboard | Deployment for the **NiceGUI** monitoring dashboard. |
| **`kafka-topics.yaml`** | Demo | Strimzi `KafkaTopic` resources for the built-in demos. |
| **`flowlets/`** | Catalog | [Built-in catalog](flowlets/README.md) of reusable Flowlets. |

## 🛠️ Helper Scripts

- **`install-flowlets.sh`**: Installs the `Flowlet` and `Pipeline` CRDs and populates the cluster with the default Flowlet catalog.
- **`submit-native.sh`**: Simplifies the `flink run-application` command for submitting jobs to a Kubernetes cluster locally.

---

## 🚀 Deployment Strategies

Flinkflow supports four main ways to run on Kubernetes:

### 1. Flink Kubernetes Operator (Recommended)
Use `flink-operator-deployment.yaml` to manage Flink jobs as native Kubernetes objects. This approach handles job lifecycle, savepoints, and horizontal scaling automatically.
> See **[README-flinkflow-k8s.md](../../docs/README-flinkflow-k8s.md)** for a full guide.

### 2. Kubernetes-Native Pipeline (GitOps)
Apply `crds/crd-pipeline.yaml`, then define your pipeline as a `kind: Pipeline` resource. Use `native-pipeline-deployment.yaml` to start a Flink worker that dynamically reads and executes it.

### 3. Manual Cluster Deployment
Apply `deployment.yaml` and `rbac.yaml` to create a long-running Flink cluster, then submit JARs manually or via CI/CD.

### 4. Native Application Mode
Use `submit-native.sh` to submit a local YAML pipeline to a Kubernetes cluster. Flink will dynamically provision the JobManager and TaskManagers as needed.

---

## 🔐 Security (RBAC)
The `rbac.yaml` file is critical. It allows Flinkflow pods to:
- Discover other pods (for Flink networking).
- Read `Pipeline` and `Flowlet` resources from the Kubernetes API.
- Create and manage ConfigMaps (for Flink High Availability).
