# Author: Arunasalam Govindasamy

output "cluster_name" {
  description = "Name of the EKS cluster."
  value       = aws_eks_cluster.this.name
}

output "cluster_endpoint" {
  description = "API server endpoint of the EKS cluster."
  value       = aws_eks_cluster.this.endpoint
}

output "cluster_certificate_authority_data" {
  description = "Base64-encoded certificate authority data for the cluster."
  value       = aws_eks_cluster.this.certificate_authority[0].data
}

output "cluster_security_group_id" {
  description = "ID of the EKS cluster control-plane Security Group."
  value       = aws_security_group.cluster.id
}

output "node_security_group_id" {
  description = "ID of the EKS worker node Security Group."
  value       = aws_security_group.node.id
}

output "node_iam_role_arn" {
  description = "ARN of the IAM role attached to worker nodes."
  value       = aws_iam_role.node.arn
}

output "autoscaling_group_names" {
  description = "Map of node group name -> backing ASG name managed by EKS."
  value       = { for k, v in aws_eks_node_group.node_group : k => v.resources[0].autoscaling_groups[0].name }
}

output "oidc_provider_arn" {
  description = "ARN of the IAM OIDC provider for this EKS cluster. Used to create IRSA trust policies."
  value       = aws_iam_openid_connect_provider.eks.arn
}

output "oidc_issuer_url" {
  description = "HTTPS OIDC issuer URL for this EKS cluster (without trailing slash)."
  value       = aws_eks_cluster.this.identity[0].oidc[0].issuer
}

output "ebs_csi_role_arn" {
  description = "IAM role ARN used by the aws-ebs-csi-driver managed addon."
  value       = aws_iam_role.ebs_csi_controller.arn
}

output "eks_addon_versions" {
  description = "Managed EKS addon versions selected for the current cluster version."
  value = {
    vpc_cni            = aws_eks_addon.vpc_cni.addon_version
    coredns            = aws_eks_addon.coredns.addon_version
    kube_proxy         = aws_eks_addon.kube_proxy.addon_version
    aws_ebs_csi_driver = aws_eks_addon.aws_ebs_csi_driver.addon_version
    pod_identity_agent = aws_eks_addon.eks_pod_identity_agent.addon_version
  }
}

