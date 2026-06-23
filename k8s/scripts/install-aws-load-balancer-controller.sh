#!/usr/bin/env bash
set -euo pipefail

# Required environment variables:
# CLUSTER_NAME, AWS_REGION, VPC_ID, AWS_ACCOUNT_ID, IAM_ROLE_ARN
: "${CLUSTER_NAME:?CLUSTER_NAME is required}"
: "${AWS_REGION:?AWS_REGION is required}"
: "${VPC_ID:?VPC_ID is required}"
: "${AWS_ACCOUNT_ID:?AWS_ACCOUNT_ID is required}"
: "${IAM_ROLE_ARN:?IAM_ROLE_ARN is required}"

helm repo add eks https://aws.github.io/eks-charts >/dev/null 2>&1 || true
helm repo update

kubectl get namespace kube-system >/dev/null

# Service account with IRSA annotation.
kubectl apply -f - <<EOF
apiVersion: v1
kind: ServiceAccount
metadata:
  name: aws-load-balancer-controller
  namespace: kube-system
  annotations:
    eks.amazonaws.com/role-arn: ${IAM_ROLE_ARN}
EOF

helm upgrade --install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName="${CLUSTER_NAME}" \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller \
  --set region="${AWS_REGION}" \
  --set vpcId="${VPC_ID}" \
  --set image.repository="602401143452.dkr.ecr.${AWS_REGION}.amazonaws.com/amazon/aws-load-balancer-controller"

echo "AWS Load Balancer Controller installed for cluster ${CLUSTER_NAME}."