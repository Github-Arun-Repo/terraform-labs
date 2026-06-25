# Observability

This folder anchors the observability stack for the Terraform-Labs platform.
The actual Kubernetes/Helm deployment manifests live under `../k8s/prometheus`
and `../k8s/grafana`, following the same wrapper-chart pattern used by Jenkins
and ArgoCD.

## Components

| Component | Purpose | Helm chart |
|-----------|---------|-----------|
| **Prometheus** (via kube-prometheus-stack) | Metrics collection, alerting, recording rules | `k8s/prometheus/kube-prometheus-stack` |
| **Node Exporter** | Node-level CPU, memory, disk, network metrics (bundled in kube-prometheus-stack) | same as above |
| **kube-state-metrics** | Pod/Deployment/Node health metrics (bundled in kube-prometheus-stack) | same as above |
| **Alertmanager** | Alert routing and de-duplication (bundled in kube-prometheus-stack) | same as above |
| **Grafana** | Dashboards for all of the above | `k8s/grafana/grafana` |

## Key Metrics Collected

### Node / CPU
| Metric | Description |
|--------|-------------|
| `node_cpu_seconds_total` | CPU time by mode (idle, user, system, iowait, …) |
| `node_load1 / node_load5 / node_load15` | 1 / 5 / 15-minute load averages |
| `node_memory_MemAvailable_bytes` | Available node memory |
| `node_memory_MemTotal_bytes` | Total node memory |
| `node_filesystem_avail_bytes` | Available disk space |
| `node_network_receive_bytes_total` | Network bytes received |
| `node_network_transmit_bytes_total` | Network bytes transmitted |

### Pod / Container Health
| Metric | Description |
|--------|-------------|
| `kube_pod_status_phase` | Pod phase (Running, Pending, Failed, …) |
| `kube_pod_container_status_ready` | Container readiness (0 = not ready) |
| `kube_pod_container_status_restarts_total` | Container restart count |
| `kube_deployment_status_replicas_available` | Available replicas per Deployment |
| `kube_deployment_status_replicas_unavailable` | Unavailable replicas per Deployment |
| `kube_node_status_condition` | Node conditions (Ready, MemoryPressure, DiskPressure, …) |

## Architecture

```
EKS Nodes
  └── Node Exporter (DaemonSet) ──────────────────┐
                                                   │  scrape
kube-apiserver / kubelets                          ▼
  └── kube-state-metrics (Deployment) ──► Prometheus (StatefulSet)
                                                   │
                             ServiceMonitor/        │  query
                             PodMonitor CRDs        ▼
                                              Grafana (Deployment)
                                              (dashboards: Node, CPU, Pod health)
```

## Deploying

```bash
# Deploy Prometheus stack + Grafana individually
./k8s/scripts/deploy-prometheus.sh
./k8s/scripts/deploy-grafana.sh

# Or as part of the full cluster bootstrap
./k8s/scripts/deploy-all.sh
```

Both services are also managed by ArgoCD:
```bash
kubectl apply -f cicd/argocd/prometheus-application.yaml
kubectl apply -f cicd/argocd/grafana-application.yaml
```

## Accessing Dashboards

```bash
# Grafana (default admin password is in the grafana-admin-credentials secret)
kubectl port-forward svc/grafana -n monitoring 3000:80

# Prometheus UI
kubectl port-forward svc/prometheus-kube-prometheus-stack-prometheus -n monitoring 9090:9090
```

## Pre-loaded Grafana Dashboards

| Dashboard | Grafana ID | Covers |
|-----------|-----------|--------|
| Node Exporter Full | 1860 | CPU, memory, disk, network per node |
| Kubernetes Cluster Overview | 7249 | Cluster-wide pod & resource usage |
| Kubernetes Pod & Container Overview | 6417 | Individual pod / container health |
| Kubernetes Deployment StatefulSet | 8588 | Deployment rollout health |
