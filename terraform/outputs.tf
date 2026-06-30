# Author: Arunasalam Govindasamy

output "aws_region" {
  description = "AWS region the platform is deployed into."
  value       = var.aws_region
}

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

output "document_processor_ecr_repository_name" {
  description = "Name of the ECR repository used for document-processor images."
  value       = module.document_processor_ecr.repository_name
}

output "document_processor_ecr_repository_arn" {
  description = "ARN of the ECR repository used for document-processor images."
  value       = module.document_processor_ecr.repository_arn
}

output "document_processor_ecr_repository_url" {
  description = "Repository URL used by Jenkins and Helm for document-processor image pushes and pulls."
  value       = module.document_processor_ecr.repository_url
}

output "service_ecr_repository_urls" {
  description = "Map of application service name -> ECR repository URL (used by Helm image.repository and CI)."
  value       = { for name, repo in module.service_ecr : name => repo.repository_url }
}

output "service_ecr_repository_arns" {
  description = "Map of application service name -> ECR repository ARN."
  value       = { for name, repo in module.service_ecr : name => repo.repository_arn }
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

output "document_ingestion_queue_url" {
  description = "SQS queue URL consumed by document-processing-service"
  value       = aws_sqs_queue.document_ingestion_queue.id
}

output "document_ingestion_queue_arn" {
  description = "SQS queue ARN for document ingestion events"
  value       = aws_sqs_queue.document_ingestion_queue.arn
}

output "document_ingestion_dlq_url" {
  description = "SQS DLQ URL for failed document ingestion messages"
  value       = aws_sqs_queue.document_ingestion_dlq.id
}

output "document_ingestion_dlq_arn" {
  description = "SQS DLQ ARN for failed document ingestion messages"
  value       = aws_sqs_queue.document_ingestion_dlq.arn
}

output "document_inventory_table_name" {
  description = "DynamoDB table name for document metadata and processing state"
  value       = aws_dynamodb_table.document_inventory.name
}

output "document_inventory_table_arn" {
  description = "DynamoDB table ARN for document metadata and processing state"
  value       = aws_dynamodb_table.document_inventory.arn
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

output "rds_master_user_secret_arn" {
  description = "Secrets Manager ARN containing RDS master credentials."
  value       = module.rds.master_user_secret_arn
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

output "eks_node_groups" {
  description = "Map of node group name -> backing ASG name (managed by EKS)."
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

output "document_processing_service_role_arn" {
  description = "IRSA role ARN to annotate on the document-processing-service Kubernetes ServiceAccount."
  value       = aws_iam_role.document_processing_service.arn
}

output "document_api_service_role_arn" {
  description = "IRSA role ARN to annotate on the document-api-service Kubernetes ServiceAccount."
  value       = aws_iam_role.document_api_service.arn
}

output "document_review_service_role_arn" {
  description = "IRSA role ARN to annotate on the document-review-service Kubernetes ServiceAccount."
  value       = aws_iam_role.document_review_service.arn
}

output "user_management_service_role_arn" {
  description = "IRSA role ARN to annotate on the user-management-service Kubernetes ServiceAccount."
  value       = aws_iam_role.user_management_service.arn
}

# -- GitHub Actions OIDC Outputs ----------------------------------------------

output "github_actions_ci_role_arn" {
  description = "IAM role ARN assumed by GitHub Actions CI jobs for ECR push/pull operations."
  value       = aws_iam_role.github_actions_ci.arn
}

output "github_actions_plan_role_arn" {
  description = "IAM role ARN assumed by GitHub Actions Terraform plan jobs with read-only access."
  value       = aws_iam_role.github_actions_plan.arn
}

output "github_actions_deploy_role_arn" {
  description = "IAM role ARN assumed by GitHub Actions deploy jobs for EKS access-entry based cluster operations."
  value       = aws_iam_role.github_actions_deploy.arn
}

output "eks_authentication_mode" {
  description = "Current EKS authentication mode for access entry cutover planning."
  value       = var.eks_authentication_mode
}

output "eks_addon_versions" {
  description = "EKS managed addon versions selected by Terraform for the cluster version."
  value       = module.eks.eks_addon_versions
}

output "ebs_csi_role_arn" {
  description = "IAM role ARN used by the managed aws-ebs-csi-driver addon."
  value       = module.eks.ebs_csi_role_arn
}

output "pod_identity_enabled" {
  description = "Whether the optional Pod Identity migration path is enabled."
  value       = var.use_pod_identity
}

output "document_api_service_pod_identity_role_arn" {
  description = "Pod Identity role ARN for document-api-service when use_pod_identity=true."
  value       = try(aws_iam_role.document_api_service_pod_identity[0].arn, null)
}

output "document_processing_service_pod_identity_role_arn" {
  description = "Pod Identity role ARN for document-processing-service when use_pod_identity=true."
  value       = try(aws_iam_role.document_processing_service_pod_identity[0].arn, null)
}

output "document_review_service_pod_identity_role_arn" {
  description = "Pod Identity role ARN for document-review-service when use_pod_identity=true."
  value       = try(aws_iam_role.document_review_service_pod_identity[0].arn, null)
}

output "karpenter_enabled" {
  description = "Whether Karpenter infrastructure resources are enabled."
  value       = var.karpenter_enabled
}

output "karpenter_controller_role_arn" {
  description = "IAM role ARN for the Karpenter controller when karpenter_enabled=true."
  value       = try(aws_iam_role.karpenter_controller[0].arn, null)
}

output "karpenter_node_role_arn" {
  description = "IAM role ARN for Karpenter-provisioned worker nodes when karpenter_enabled=true."
  value       = try(aws_iam_role.karpenter_node[0].arn, null)
}

output "karpenter_interruption_queue_name" {
  description = "SQS queue name used by Karpenter interruption handling when karpenter_enabled=true."
  value       = try(aws_sqs_queue.karpenter_interruption[0].name, null)
}

