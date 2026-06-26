# Kubernetes Runtime — EKS Delivery Surface

This folder contains Kubernetes deployment assets for active services and platform tooling.

## Folder structure

- `k8s/eks/document-api-service`
- `k8s/eks/document-processing-service`
- `k8s/eks/document-processor`
- `k8s/eks/document-review-service`
- `k8s/eks/user-management-service`
- `k8s/jenkins/dynamic-jenkins`
- `k8s/argocd/argocd`
- `k8s/scripts`

## Deployment scripts

- `install-aws-load-balancer-controller.sh`
- `deploy-jenkins.sh`
- `deploy-argocd.sh`
- `deploy-document-api.sh`
- `deploy-document-processing.sh`
- `deploy-document-processor.sh`
- `deploy-document-review.sh`
- `deploy-user-management.sh`
- `deploy-prometheus.sh`
- `deploy-grafana.sh`
- `deploy-all.sh`

## Notes

1. ArgoCD manifests under `cicd/argocd` should be used for GitOps-managed deployments.
2. Keep Jenkins in a separate namespace and use IRSA for AWS access.
3. Do not commit generated chart dependencies under `charts/`.
