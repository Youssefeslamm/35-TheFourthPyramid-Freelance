# M3 Freelance Marketplace — Kubernetes (Section 10)

All workloads run in the `freelance` namespace. Only `api-gateway` is exposed externally (NodePort `30080`).

## Deployment order

1. Namespace  
   `kubectl apply -f k8s/namespaces/`
2. Secrets  
   `kubectl apply -f k8s/secrets/`
3. ConfigMaps  
   `kubectl apply -f k8s/configmaps/`
4. PVCs  
   `kubectl apply -f k8s/pvcs/`
5. StatefulSets (data stores)  
   `kubectl apply -f k8s/statefulsets/`
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
kubectl apply -f k8s/configmaps/
kubectl apply -f k8s/pvcs/
kubectl apply -f k8s/statefulsets/
kubectl apply -f k8s/services/
kubectl apply -f k8s/deployments/
kubectl apply -f k8s/api-gateway/
```

## Verification

```bash
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

Deployments use local tags (`user-service:latest`, etc.). Build and load images into your cluster (or push to a registry and update `image` fields) before expecting pods to run.

## Optional monitoring (outside Section 10)

Observability stacks live under `k8s/monitoring/` (namespace `monitoring`): Loki, Prometheus, Grafana dashboards. Apply after the `freelance` namespace stack if needed:

```bash
kubectl apply -f k8s/monitoring/namespaces/
kubectl apply -f k8s/monitoring/loki/
kubectl apply -f k8s/monitoring/prometheus/
kubectl apply -f k8s/monitoring/grafana/
```

Section 10 manifests are canonical under root `k8s/` only. Do not maintain duplicates under `infra/k8s/`.
