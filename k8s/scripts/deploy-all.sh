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
./k8s/scripts/deploy-dms.sh
./k8s/scripts/deploy-alb.sh
./k8s/scripts/deploy-jenkins.sh

echo "Deployment complete: app, ALB ingress, and Jenkins are installed."