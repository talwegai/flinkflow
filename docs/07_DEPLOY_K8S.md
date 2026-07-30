# Deploying Flinkflow to Kubernetes using Flink Operator

This guide explains how to deploy the Flinkflow application to a Kubernetes cluster that has the [Apache Flink Kubernetes Operator](https://nightlies.apache.org/flink/flink-kubernetes-operator-docs-main/) installed.

> [!NOTE]
> Flinkflow supports **Polyglot Logic Snippets** and **Hybrid SQL**. You can embed **Camel Expressions**, **Java (Janino)**, **Python (GraalVM)** code, and **Apache Flink SQL** queries directly in your `Pipeline` Custom Resources.

## Prerequisites

- Kubernetes cluster configured with `kubectl`.
- [Helm](https://helm.sh/docs/intro/install/) installed.

## 1. Install Flink Kubernetes Operator

If you haven't already, install the Flink Operator using Helm. This operator will manage the lifecycle of your Flink deployments.

```bash
# Add the Helm repository
helm repo add flink-operator-repo https://downloads.apache.org/flink/flink-kubernetes-operator-1.15.0/

# Install the operator and its CRDs
helm install flink-kubernetes-operator flink-operator-repo/flink-kubernetes-operator \
  --set webhook.create=false # Optional: skip webhook if not using cert-manager
```

For detailed installation options, refer to the **[Official Flink Operator Documentation](https://nightlies.apache.org/flink/flink-kubernetes-operator-docs-main/docs/try-flink-kubernetes-operator/quick-start/)**.

---

## 2. Container Image

Flinkflow provides a pre-built Docker image on the GitHub Container Registry. This is the recommended way to deploy the application.

You can pull the official Flinkflow image from the GitHub Container Registry. This is the fastest way to get started.

```bash
docker pull ghcr.io/talwegai/flinkflow:0.9.6
```

> [!NOTE]
> When using the public image, ensure you update the `image` field in your deployment manifests (e.g., `deploy/k8s/flink-operator-deployment.yaml`) to `ghcr.io/talwegai/flinkflow:0.9.6`.



## 3. Prepare Kubernetes Resources

### RBAC Setup
Ensure the service account `flink-service-account` exists and has the necessary permissions. If not already applied:

```bash
kubectl apply -f deploy/k8s/rbac.yaml
```

### Pipeline Configuration (CRD)
To run pipelines natively from Kubernetes without building them into the image or managing ConfigMaps, you must install the Flinkflow Custom Resource Definitions (CRDs) and apply your pipeline directly to the cluster:

```bash
# Install Flinkflow CRDs
kubectl apply -f deploy/k8s/crds/

# Apply your Pipeline custom resource
kubectl apply -f examples/k8s/camel/fraud-detection-camel.yaml
```

## 4. Deploy the Application

### Using Flink Operator

The recommended way to run Flinkflow is using the **Flink Kubernetes Operator**. Now that your `Pipeline` CR is installed in Kubernetes, you can configure the `FlinkDeployment` to execute it natively.

1. **Configure your manifest** (`deploy/k8s/flink-operator-deployment.yaml`):

```yaml
apiVersion: flink.apache.org/v1beta1
kind: FlinkDeployment
metadata:
  name: flinkflow-app
spec:
  image: ghcr.io/talwegai/flinkflow:0.9.6
  flinkVersion: v2_2
  serviceAccount: flink-service-account
  jobManager:
    resource:
      memory: "2048m"
      cpu: 1
  taskManager:
    resource:
      memory: "2048m"
      cpu: 1
  job:
    jarURI: local:///opt/flink/usrlib/flinkflow.jar
    entryClass: ai.talweg.flinkflow.FlinkflowApp
    args: 
      - "--k8s-pipeline"
      - "fraud-detection-yaml-dsl"
      - "--enable-k8s-flowlets"
      - "--k8s-namespace"
      - "default"
    parallelism: 2
```

2. **Apply the manifest**:

```bash
kubectl apply -f deploy/k8s/flink-operator-deployment.yaml
```

The Flink Operator will detect this resource and automatically start the JobManager and TaskManagers to execute the pipeline discovered from the Kubernetes API.

## 5. Monitor the Deployment

Check the status of the FlinkDeployment:

```bash
kubectl get flinkdeployment flinkflow-app
```

View the logs of the JobManager or TaskManager:

```bash
# Get pod names
kubectl get pods

# View logs
kubectl logs -f flinkflow-app-jm-0
```

## 6. Accessing the Flink UI

The operator creates a REST service for the JobManager. You can port-forward this service to your local machine to access the official Flink Dashboard, where you can view your running jobs, resource usage, and pipeline graphs:

```bash
kubectl port-forward svc/flinkflow-app-rest 8081:8081
```

Open `http://localhost:8081` in your browser to view the running jobs!

## 7. Cleanup

To delete the application and all associated resources:
```bash
kubectl delete -f deploy/k8s/flink-operator-deployment.yaml
```

---

## Alternative Deployment Methods

If you prefer not to use the Flink Kubernetes Operator, you can use these alternative methods.

### 1. Manual Cluster Deployment

This model consists of a static JobManager and multiple TaskManagers defined in a single manifest.

1.  **Configure the Manifest**:
    Ensure `deploy/k8s/deployment.yaml` is configured with the correct image (`ghcr.io/talwegai/flinkflow:0.9.6`).

2.  **Apply the Cluster Resources**:
    This will deploy the JobManager (Application Mode) and a TaskManager pool.
    ```bash
    kubectl apply -f deploy/k8s/deployment.yaml
    ```

### 2. Native Kubernetes (run-application)

This mode allows Flink to manage cluster resources dynamically. It requires Flink binaries installed on your local machine.

1.  **Setup RBAC**:
    Allow Flink to create/manage pods:
    ```bash
    kubectl apply -f deploy/k8s/rbac.yaml
    ```

2.  **Submit the Application**:
    Use the helper script to submit the job directly to the Kubernetes API:
    ```bash
    ./deploy/k8s/submit-native.sh examples/standalone/simple-transform-example.yaml [image-name]
    ```

### 3. Note on Flowlets
Flowlets used within a `Pipeline` CR are automatically discovered from the same cluster namespace.
   Alternatively, run the raw command:
   ```bash
   ./bin/flink run-application \
       --target kubernetes-application \
       -Dkubernetes.cluster-id=flinkflow-native-cluster \
       -Dkubernetes.container.image=ghcr.io/talwegai/flinkflow:0.9.6 \
       -Dkubernetes.service-account=flink-service-account \
       -Dkubernetes.rest-service.exposed.type=NodePort \
       -Djobmanager.memory.process.size=1600m \
       -Dtaskmanager.memory.process.size=1728m \
       -Dtaskmanager.numberOfTaskSlots=2 \
       local:///opt/flink/usrlib/flinkflow.jar \
       --job-args /opt/flink/conf/pipeline.yaml
   ```