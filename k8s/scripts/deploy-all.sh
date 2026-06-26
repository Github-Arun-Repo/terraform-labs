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

# Observability stack: Prometheus (Node Exporter, kube-state-metrics) + Grafana
./k8s/scripts/deploy-prometheus.sh
./k8s/scripts/deploy-grafana.sh

echo "Deployment complete: ALB controller, Jenkins, ArgoCD, and observability stack are installed."
echo ""
echo "Apply the ArgoCD root app-of-apps to hand over GitOps control:"
echo "  kubectl apply -f cicd/argocd/root-app-of-apps.yaml"