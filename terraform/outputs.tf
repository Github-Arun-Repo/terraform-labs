# Author: Arunasalam Govindasamy

output "vpc_id" {
  description = "ID of the VPC."
  value       = module.vpc.vpc_id
}

output "vpc_cidr" {
  description = "CIDR block of the VPC."
  value       = module.vpc.vpc_cidr
}

output "public_subnet_ids" {
  description = "IDs of the public subnets."
  value       = module.vpc.public_subnet_ids
}

output "private_app_subnet_ids" {
  description = "IDs of the private app-tier subnets."
  value       = module.vpc.private_app_subnet_ids
}

output "private_db_subnet_ids" {
  description = "IDs of the private DB-tier subnets."
  value       = module.vpc.private_db_subnet_ids
}

output "db_subnet_group_name" {
  description = "Name of the RDS DB subnet group."
  value       = module.vpc.db_subnet_group_name
}

output "nat_gateway_public_ips" {
  description = "Public IPs of the NAT Gateways."
  value       = module.vpc.nat_gateway_public_ips
}

output "app_security_group_id" {
  description = "ID of the app-tier Security Group. Attach this to your EC2/ECS workloads."
  value       = aws_security_group.app.id
}

# -- ECR Outputs ---------------------------------------------------------------

output "ecr_repository_name" {
  description = "Name of the ECR repository used for application images."
  value       = module.ecr.repository_name
}

output "ecr_repository_arn" {
  description = "ARN of the ECR repository used for application images."
  value       = module.ecr.repository_arn
}

output "ecr_repository_url" {
  description = "Repository URL used by Jenkins and Helm for image pushes and pulls."
  value       = module.ecr.repository_url
}

# -- S3 Outputs ----------------------------------------------------------------

output "documents_inventory_bucket_name" {
  description = "Name of the S3 bucket used by the document processing application."
  value       = module.documents_inventory_s3.bucket_name
}

output "documents_inventory_bucket_arn" {
  description = "ARN of the S3 bucket used by the document processing application."
  value       = module.documents_inventory_s3.bucket_arn
}

output "documents_inventory_kms_key_arn" {
  description = "ARN of the KMS key used to encrypt the document inventory S3 bucket."
  value       = module.documents_inventory_s3.kms_key_arn
}

output "documents_inventory_kms_key_alias" {
  description = "Alias of the KMS key used to encrypt the document inventory S3 bucket."
  value       = module.documents_inventory_s3.kms_key_alias
}

# -- RDS Outputs ---------------------------------------------------------------

output "db_endpoint" {
  description = "RDS connection endpoint (host:port)."
  value       = module.rds.db_endpoint
}

output "db_host" {
  description = "RDS hostname."
  value       = module.rds.db_host
}

output "db_port" {
  description = "RDS port."
  value       = module.rds.db_port
}

output "db_name" {
  description = "Initial database name."
  value       = module.rds.db_name
}

output "rds_security_group_id" {
  description = "ID of the RDS Security Group."
  value       = module.rds.rds_security_group_id
}

# -- EKS Outputs ---------------------------------------------------------------

output "eks_cluster_name" {
  description = "EKS cluster name."
  value       = module.eks.cluster_name
}

output "eks_cluster_endpoint" {
  description = "EKS API server endpoint (private)."
  value       = module.eks.cluster_endpoint
}

output "eks_node_security_group_id" {
  description = "Security Group ID of the EKS worker nodes."
  value       = module.eks.node_security_group_id
}

output "alb_security_group_id" {
  description = "Security Group ID of the public ALB."
  value       = aws_security_group.alb.id
}

output "eks_node_groups" {
  description = "Map of node group name -> ASG name."
  value       = module.eks.autoscaling_group_names
}

# -- IRSA Outputs --------------------------------------------------------------

output "eks_oidc_provider_arn" {
  description = "ARN of the EKS OIDC provider. Referenced by all IRSA trust policies."
  value       = module.eks.oidc_provider_arn
}

output "jenkins_build_agent_role_arn" {
  description = "IRSA role ARN to annotate on the jenkins-build-agent Kubernetes ServiceAccount."
  value       = aws_iam_role.jenkins_build_agent.arn
}

