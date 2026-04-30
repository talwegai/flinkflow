# Deploying Flinkflow to Kubernetes using Flink Operator

This guide explains how to build the Flinkflow application and deploy it to a Kubernetes cluster that has the [Apache Flink Kubernetes Operator](https://nightlies.apache.org/flink/flink-kubernetes-operator-docs-main/) installed.

> [!NOTE]
> Flinkflow supports **Polyglot Logic Snippets**. You can embed **Camel Expressions**, **Java (Janino)** and **Python (GraalVM)** code directly in your `Pipeline` Custom Resources.

## Prerequisites

- Kubernetes cluster with Flink Operator installed.
- `kubectl` configured to access your cluster.
- Docker or a similar container tool.
- Maven 3+ and Java 17+.

## 1. Build the Application

First, compile the project and create the shaded JAR:

```bash
mvn clean package
```

The resulting JAR will be at `target/flinkflow-{version}.jar`.

## 2. Docker Image Selection

The application needs to be containerized to run in Kubernetes. You can either use the official public image or build your own.

### Option A: Use Public Image (Recommended)

You can pull the official Flinkflow image from the GitHub Container Registry. This is the fastest way to get started.

```bash
docker pull ghcr.io/talwegai/flinkflow:0.9.3{version}
```

> [!NOTE]
> When using the public image, ensure you update the `image` field in your deployment manifests (e.g., `deploy/k8s/flink-operator-deployment.yaml`) to `ghcr.io/talwegai/flinkflow:0.9.3{version}`.

### Option B: Build Image Locally

If you have made custom changes to the Flinkflow core or need a specific base image:

1. **Build the image**:
   ```bash
   docker build -t flinkflow:latest .
   ```

2. **Push the image** (if using a remote cluster):
   If you are using a cloud provider (GKE, EKS, etc.), tag and push the image to your registry:
   ```bash
   docker tag flinkflow:latest your-registry/flinkflow:latest
   docker push your-registry/flinkflow:latest
   ```
   *Note: If pushing to a registry, update the `image` field in `deploy/k8s/flink-operator-deployment.yaml`.*

## 3. Prepare Kubernetes Resources

### RBAC Setup
Ensure the service account `flink-service-account` exists and has the necessary permissions. If not already applied:

```bash
kubectl apply -f deploy/k8s/rbac.yaml
```

### Pipeline Configuration (Optional)
The default `Dockerfile` bakes a sample pipeline into `/opt/flink/conf/pipeline.yaml`. If you want to use a custom pipeline without rebuilding the image, you can create a ConfigMap:

```bash
kubectl create configmap my-pipeline --from-file=pipeline.yaml=../examples/standalone/simple-transform-example.yaml
```

And then update the `FlinkDeployment` to mount this ConfigMap.

## 4. Deploy the Application

Apply the `FlinkDeployment` manifest to the cluster:

```bash
kubectl apply -f deploy/k8s/flink-operator-deployment.yaml
```

The Flink Operator will detect this resource and automatically:
1. Start the JobManager.
2. Start the TaskManagers.
3. Submit the Flink job defined in the `spec.job` section.

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

The operator usually creates a service for the JobManager. You can port-forward to access the dashboard:

```bash
kubectl port-forward svc/flinkflow-app-rest 8081:8081
```

Open `http://localhost:8081` in your browser.

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

1.  **Build and Tag Image**:
    ```bash
    docker build -t flinkflow:latest .
    ```

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
       -Dkubernetes.container.image=flinkflow:latest \
       -Dkubernetes.service-account=flink-service-account \
       -Dkubernetes.rest-service.exposed.type=NodePort \
       -Djobmanager.memory.process.size=1600m \
       -Dtaskmanager.memory.process.size=1728m \
       -Dtaskmanager.numberOfTaskSlots=2 \
       local:///opt/flink/usrlib/flinkflow.jar \
       --job-args /opt/flink/conf/pipeline.yaml
   ```