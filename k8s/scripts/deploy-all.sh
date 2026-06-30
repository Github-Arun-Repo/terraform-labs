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

# NOTE: Prometheus, Grafana, and the tracing tier (Tempo + OpenTelemetry Collector)
# are owned by ArgoCD via the root app-of-apps (cicd/argocd/*-application.yaml), so
# they are intentionally NOT installed imperatively here. This avoids dual ownership
# and drift between Helm-installed and GitOps-reconciled releases. The standalone
# deploy-prometheus.sh / deploy-grafana.sh / deploy-observability.sh scripts remain
# available for local clusters or one-off manual installs.

echo "Bootstrap complete: ALB controller, Jenkins, and ArgoCD are installed."
echo ""
echo "Apply the ArgoCD root app-of-apps to hand over GitOps control"
echo "(this deploys Prometheus, Grafana, Tempo, the OTel Collector, Kyverno, and the services):"
echo "  kubectl apply -f cicd/argocd/root-app-of-apps.yaml"