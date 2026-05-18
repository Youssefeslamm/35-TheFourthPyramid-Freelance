# M3 Freelance Marketplace — Kubernetes (Section 10)

All workloads run in the `freelance` namespace. Only `api-gateway` is exposed externally (NodePort `30080`).

## Primary path: MiniKube deployment

Prerequisites:

- Docker Desktop running
- MiniKube installed
- kubectl installed

Start MiniKube and select its Kubernetes context:

```powershell
minikube start --driver=docker
kubectl config use-context minikube
```

Build the Spring Boot service images with the same local tags used by the Kubernetes deployments:

```powershell
docker compose build
```

Load the local images into MiniKube so the cluster can run them:

```powershell
minikube image load user-service:latest
minikube image load job-service:latest
minikube image load proposal-service:latest
minikube image load contract-service:latest
minikube image load wallet-service:latest
minikube image load api-gateway:latest
```

Apply Kubernetes manifests in this order:

```powershell
kubectl apply -f k8s/namespaces/
kubectl apply -f k8s/secrets/
kubectl apply -f k8s/pvcs/
kubectl apply -f k8s/statefulsets/
kubectl apply -f k8s/configmaps/
kubectl apply -f k8s/services/
kubectl apply -f k8s/deployments/
kubectl apply -f k8s/api-gateway/
```

Verify the application pods and services:

```powershell
kubectl get pods -n freelance
kubectl get svc -n freelance
```

MiniKube PowerShell health check:

```powershell
$MINIKUBE_IP = minikube ip
curl.exe http://$MINIKUBE_IP`:30080/actuator/health
```

Expected result:

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

## Optional: Docker Desktop Kubernetes

Docker Desktop Kubernetes may use the host Docker image cache directly after `docker compose build`. In that setup, the NodePort health check may work through localhost:

```powershell
curl.exe http://localhost:30080/actuator/health
```

## Local Kubernetes image build and deploy

Build the Spring Boot service images with the same local tags used by the Kubernetes deployments:

```bash
docker compose build
```

Then apply Kubernetes manifests in this order:

```bash
kubectl apply -f k8s/namespaces/
kubectl apply -f k8s/secrets/
kubectl apply -f k8s/pvcs/
kubectl apply -f k8s/statefulsets/
kubectl apply -f k8s/configmaps/
kubectl apply -f k8s/services/
kubectl apply -f k8s/deployments/
kubectl apply -f k8s/api-gateway/
```

Verify the application pods and services:

```bash
kubectl get pods -n freelance
kubectl get svc -n freelance
```

All application pods should become `Running`, and `api-gateway` should expose NodePort `30080`.

## Deployment order

1. Namespace  
   `kubectl apply -f k8s/namespaces/`
2. Secrets  
   `kubectl apply -f k8s/secrets/`
3. PVCs  
   `kubectl apply -f k8s/pvcs/`
4. StatefulSets (data stores)  
   `kubectl apply -f k8s/statefulsets/`
5. ConfigMaps  
   `kubectl apply -f k8s/configmaps/`
6. Services (data stores)  
   `kubectl apply -f k8s/services/`  
   *(Spring Boot app services can be applied with deployments; postgres headless services must exist before apps connect.)*
7. Deployments (microservices)  
   `kubectl apply -f k8s/deployments/`
8. API Gateway  
   `kubectl apply -f k8s/api-gateway/`

Or apply everything in order:

```bash
kubectl apply -f k8s/namespaces/
kubectl apply -f k8s/secrets/
kubectl apply -f k8s/pvcs/
kubectl apply -f k8s/statefulsets/
kubectl apply -f k8s/configmaps/
kubectl apply -f k8s/services/
kubectl apply -f k8s/deployments/
kubectl apply -f k8s/api-gateway/
```

## Verification

```bash
kubectl get pods -n freelance
kubectl get svc -n freelance
kubectl get ns freelance
kubectl get all -n freelance
kubectl get pvc -n freelance
kubectl get svc api-gateway -n freelance
```

Health (after pods are running):

```bash
kubectl port-forward -n freelance svc/api-gateway 8080:8080
curl http://localhost:8080/actuator/health
```

External access (NodePort):

```bash
curl http://<node-ip>:30080/actuator/health
```

Client dry-run validation:

```bash
kubectl apply --dry-run=client -f k8s/namespaces/
kubectl apply --dry-run=client -f k8s/secrets/
kubectl apply --dry-run=client -f k8s/configmaps/
kubectl apply --dry-run=client -f k8s/pvcs/
kubectl apply --dry-run=client -f k8s/statefulsets/
kubectl apply --dry-run=client -f k8s/deployments/
kubectl apply --dry-run=client -f k8s/services/
kubectl apply --dry-run=client -f k8s/api-gateway/
```

## Image names

Deployments use local tags (`user-service:latest`, etc.) built by `docker compose build`.

## Fresh local databases

The Kubernetes service ConfigMaps set `SPRING_JPA_HIBERNATE_DDL_AUTO=update` for the database-backed Spring Boot services so fresh local PostgreSQL databases can initialize tables automatically for TA grading.

## Optional monitoring (outside Section 10)

Observability stacks live under `k8s/monitoring/` (namespace `monitoring`): Loki, Prometheus, Grafana dashboards. Apply after the `freelance` namespace stack if needed:

```bash
kubectl apply -f k8s/monitoring/namespaces/
kubectl apply -f k8s/monitoring/loki/
kubectl apply -f k8s/monitoring/prometheus/
kubectl apply -f k8s/monitoring/grafana/
```

Section 10 manifests are canonical under root `k8s/` only. Do not maintain duplicates under `infra/k8s/`.
