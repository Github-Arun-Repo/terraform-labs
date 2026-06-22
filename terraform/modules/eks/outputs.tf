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

output "node_iam_instance_profile_arn" {
  description = "ARN of the EC2 instance profile for worker nodes."
  value       = aws_iam_instance_profile.node.arn
}

output "autoscaling_group_names" {
  description = "Map of node group name -> ASG name."
  value       = { for k, v in aws_autoscaling_group.node_group : k => v.name }
}

