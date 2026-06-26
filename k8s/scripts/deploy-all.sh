#!/usr/bin/env bash
set -euo pipefail

# Example usage:
# export CLUSTER_NAME=my-eks
# export AWS_REGION=eu-west-1
# export VPC_ID=vpc-xxxx
# export AWS_ACCOUNT_ID=123456789012
# export IAM_ROLE_ARN=arn:aws:iam::123456789012:role/AWSLoadBalancerControllerRole
# ./k8s/scripts/deploy-all.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

./k8s/scripts/install-aws-load-balancer-controller.sh
./k8s/scripts/deploy-jenkins.sh
./k8s/scripts/deploy-argocd.sh
./k8s/scripts/deploy-alb.sh

# Observability stack: Prometheus (Node Exporter, kube-state-metrics) + Grafana
./k8s/scripts/deploy-prometheus.sh
./k8s/scripts/deploy-grafana.sh

# DMS is no longer deployed by this script.
# ArgoCD manages the DMS deployment via the Application manifest:
#   kubectl apply -f cicd/argocd/dms-application.yaml
# ArgoCD will sync the Helm chart at k8s/eks/document-management-service
# automatically whenever the image.tag in values.yaml changes.

echo "Deployment complete: ALB controller, Jenkins, ArgoCD, and observability stack are installed."
echo ""
echo "Apply ArgoCD Application manifests to hand over GitOps control:"
echo "  kubectl apply -f cicd/argocd/dms-application.yaml"
echo "  kubectl apply -f cicd/argocd/document-processor-application.yaml"
echo "  kubectl apply -f cicd/argocd/user-management-service-application.yaml"
echo "  kubectl apply -f cicd/argocd/document-api-service-application.yaml"
echo "  kubectl apply -f cicd/argocd/document-processing-service-application.yaml"
echo "  kubectl apply -f cicd/argocd/document-review-service-application.yaml"
echo "  kubectl apply -f cicd/argocd/prometheus-application.yaml"
echo "  kubectl apply -f cicd/argocd/grafana-application.yaml"