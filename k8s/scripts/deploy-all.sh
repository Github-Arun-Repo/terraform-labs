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

# DMS is no longer deployed by this script.
# ArgoCD manages the DMS deployment via the Application manifest:
#   kubectl apply -f cicd/argocd/dms-application.yaml
# ArgoCD will sync the Helm chart at k8s/eks/document-management-service
# automatically whenever the image.tag in values.yaml changes.

echo "Deployment complete: ALB controller, Jenkins, and ArgoCD are installed."
echo "Apply the DMS ArgoCD Application to start GitOps deployments:"
echo "  kubectl apply -f cicd/argocd/dms-application.yaml"