# Karpenter Setup

This directory contains Karpenter deployment assets:

- Wrapper Helm chart: `k8s/karpenter/karpenter`
- Karpenter NodeClass and NodePool manifests in chart templates
- ArgoCD application: `cicd/argocd/karpenter-application.yaml`

## Prerequisites

1. Terraform applied with `karpenter_enabled = true`.
2. Populate `k8s/karpenter/karpenter/values.yaml` placeholders:
   - `karpenter.settings.clusterEndpoint`
   - `karpenter.serviceAccount.annotations.eks.amazonaws.com/role-arn`
   - `ec2NodeClass.role`
3. Ensure VPC subnets and node security groups have `karpenter.sh/discovery=<cluster-name>` tags.

## Notes

- Keep at least one managed bootstrap node group for Karpenter controller availability.
- Karpenter provisioning can then scale additional AL2023 worker nodes via NodePool/EC2NodeClass.
