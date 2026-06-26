# Author: Arunasalam Govindasamy

variable "aws_region" {
  description = "AWS region to deploy into (e.g. eu-west-1, us-east-1)."
  type        = string
}

variable "vpc_name" {
  description = "Name prefix used for all VPC resources."
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC."
  type        = string
}

variable "availability_zones" {
  description = "List of Availability Zones to deploy subnets into."
  type        = list(string)
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets - one per AZ."
  type        = list(string)
}

variable "private_app_subnet_cidrs" {
  description = "CIDR blocks for private app-tier subnets - one per AZ."
  type        = list(string)
}

variable "private_db_subnet_cidrs" {
  description = "CIDR blocks for private DB-tier subnets - one per AZ."
  type        = list(string)
}

variable "enable_nat_gateway" {
  description = "Create NAT Gateways for private subnet outbound internet access."
  type        = bool
  default     = true
}

variable "single_nat_gateway" {
  description = "Share a single NAT Gateway across all AZs (cost saving, not HA)."
  type        = bool
  default     = false
}

variable "db_subnet_enable_nat" {
  description = "Allow DB subnets outbound internet access via NAT."
  type        = bool
  default     = false
}

variable "create_db_subnet_group" {
  description = "Create an RDS DB subnet group from the private DB subnets."
  type        = bool
  default     = true
}

variable "default_tags" {
  description = "Tags applied to every AWS resource via the provider default_tags block."
  type        = map(string)
  default     = {}
}

# -- GitHub Actions OIDC -------------------------------------------------------

variable "github_actions_repository" {
  description = "GitHub repository in owner/name format allowed to assume GitHub Actions OIDC roles."
  type        = string
  default     = "Github-Arun-Repo/terraform-labs"
}

variable "github_actions_ci_subs" {
  description = "Exact GitHub OIDC sub claims allowed to assume the CI role (for example main branch or protected environments)."
  type        = list(string)
  default     = ["repo:Github-Arun-Repo/terraform-labs:ref:refs/heads/main"]
}

variable "github_actions_plan_subs" {
  description = "Exact GitHub OIDC sub claims allowed to assume the Terraform plan read-only role."
  type        = list(string)
  default = [
    "repo:Github-Arun-Repo/terraform-labs:ref:refs/heads/main",
    "repo:Github-Arun-Repo/terraform-labs:pull_request",
  ]
}

variable "github_actions_deploy_subs" {
  description = "Exact GitHub OIDC sub claims allowed to assume the GitHub Actions deploy role used for cluster access."
  type        = list(string)
  default = [
    "repo:Github-Arun-Repo/terraform-labs:ref:refs/heads/main",
  ]
}

# -- ECR -----------------------------------------------------------------------

variable "document_processor_ecr_repository_name" {
  description = "Name of the ECR repository that stores document-processor images."
  type        = string
  default     = "document-processor"
}

variable "document_processor_ecr_image_tag_mutability" {
  description = "Whether document-processor ECR tags are mutable or immutable."
  type        = string
  default     = "MUTABLE"
}

variable "document_processor_ecr_image_scan_on_push" {
  description = "Enable vulnerability scanning for document-processor images on push."
  type        = bool
  default     = true
}

variable "document_processor_ecr_force_delete" {
  description = "Allow document-processor repository deletion when images still exist. Use true only for disposable lab environments."
  type        = bool
  default     = false
}

variable "document_processor_ecr_max_image_count" {
  description = "Maximum number of images to keep in the document-processor repository lifecycle policy."
  type        = number
  default     = 30
}

# -- S3 (Document Inventory) ---------------------------------------------------

variable "documents_inventory_bucket_name" {
  description = "Name of the S3 bucket used by the document processing application."
  type        = string
  default     = "documents-inventory-s3"
}

variable "documents_inventory_bucket_force_delete" {
  description = "Allow bucket deletion when objects exist. Keep false for non-disposable environments."
  type        = bool
  default     = false
}

variable "documents_inventory_bucket_enable_versioning" {
  description = "Enable versioning on the document inventory S3 bucket."
  type        = bool
  default     = true
}

variable "documents_inventory_kms_alias_name" {
  description = "Alias for the KMS key used by the document inventory S3 bucket."
  type        = string
  default     = "alias/documents-inventory-s3"
}

variable "documents_inventory_kms_key_description" {
  description = "Description for the KMS key used to encrypt the document inventory S3 bucket."
  type        = string
  default     = "KMS key for documents-inventory-s3 bucket encryption"
}

variable "enable_documents_inventory_sqs_notifications" {
  description = "Whether to enable S3 ObjectCreated notifications from document inventory bucket to SQS."
  type        = bool
  default     = true
}

variable "sqs_notification_prefixes" {
  description = "S3 prefixes under documents inventory bucket that should trigger ingestion SQS notifications."
  type        = list(string)
  default = [
    "invoice/raw/",
    "receipt/raw/",
  ]
}

variable "sqs_notification_events" {
  description = "S3 events that should trigger ingestion SQS notifications."
  type        = list(string)
  default     = ["s3:ObjectCreated:*"]
}

variable "document_ingestion_queue_name" {
  description = "SQS queue name for S3 document upload events"
  type        = string
  default     = "document-ingestion-queue"
}

variable "document_ingestion_visibility_timeout_seconds" {
  description = "SQS visibility timeout for document ingestion queue"
  type        = number
  default     = 180
}

variable "document_ingestion_message_retention_seconds" {
  description = "SQS message retention in seconds"
  type        = number
  default     = 345600
}

variable "document_ingestion_max_receive_count" {
  description = "Maximum receive count before message moves to DLQ"
  type        = number
  default     = 3
}

variable "document_ingestion_dlq_name" {
  description = "DLQ name for failed document ingestion messages"
  type        = string
  default     = "document-ingestion-dlq"
}

variable "document_ingestion_dlq_retention_seconds" {
  description = "DLQ message retention in seconds"
  type        = number
  default     = 1209600
}

variable "document_processing_sa_namespace" {
  description = "Kubernetes namespace for document-processing-service service account used with IRSA."
  type        = string
  default     = "document-processing"
}

variable "document_processing_sa_name" {
  description = "Kubernetes service account name for document-processing-service IRSA."
  type        = string
  default     = "document-processing-service"
}

variable "enable_document_processing_textract_permissions" {
  description = "Whether to include textract:AnalyzeExpense in document-processing-service IAM policy."
  type        = bool
  default     = false
}

variable "document_inventory_table_name" {
  description = "DynamoDB table name for document metadata and processing state."
  type        = string
  default     = "DocumentInventory"
}

variable "enable_document_inventory_status_gsi" {
  description = "Whether to create optional GSI2 for status-based listing in DocumentInventory table."
  type        = bool
  default     = true
}

variable "document_api_sa_namespace" {
  description = "Kubernetes namespace for document-api-service service account used with IRSA."
  type        = string
  default     = "document-api"
}

variable "document_api_sa_name" {
  description = "Kubernetes service account name for document-api-service IRSA."
  type        = string
  default     = "document-api-service"
}

variable "document_review_sa_namespace" {
  description = "Kubernetes namespace for document-review-service service account used with IRSA."
  type        = string
  default     = "document-review"
}

variable "document_review_sa_name" {
  description = "Kubernetes service account name for document-review-service IRSA."
  type        = string
  default     = "document-review-service"
}

variable "user_management_sa_namespace" {
  description = "Kubernetes namespace for user-management-service service account used with IRSA."
  type        = string
  default     = "user-management"
}

variable "user_management_sa_name" {
  description = "Kubernetes service account name for user-management-service IRSA."
  type        = string
  default     = "user-management-service"
}

# -- RDS -----------------------------------------------------------------------

variable "db_identifier" {
  description = "Unique identifier for the RDS instance."
  type        = string
}

variable "db_engine" {
  description = "Database engine (mysql | postgres | mariadb)."
  type        = string
  default     = "mysql"
}

variable "db_engine_version" {
  description = "Engine version (must be free-tier eligible on db.t3.micro)."
  type        = string
  default     = "8.0"
}

variable "db_parameter_group_family" {
  description = "Parameter group family matching the engine + version (e.g. mysql8.0, postgres16)."
  type        = string
  default     = "mysql8.0"
}

variable "db_port" {
  description = "Port the database listens on (3306 for MySQL, 5432 for PostgreSQL)."
  type        = number
  default     = 3306
}

variable "db_name" {
  description = "Name of the initial database to create."
  type        = string
}

variable "db_username" {
  description = "Master username for the database."
  type        = string
}

variable "db_manage_master_user_password" {
  description = "Use RDS-managed master user password in Secrets Manager when supported by the selected engine/version."
  type        = bool
  default     = true
}

# -- EKS -----------------------------------------------------------------------

variable "eks_cluster_name" {
  description = "Name of the EKS cluster."
  type        = string
}

variable "eks_cluster_version" {
  description = "Kubernetes version for the EKS cluster (e.g. '1.36')."
  type        = string
  default     = "1.36"
}

variable "eks_authentication_mode" {
  description = "EKS cluster authentication mode: CONFIG_MAP, API_AND_CONFIG_MAP, or API."
  type        = string
  default     = "API_AND_CONFIG_MAP"
}

variable "eks_node_groups" {
  description = <<-EOT
    Map of EKS managed node group configurations.
    Labels are applied to nodes via managed node group settings.
    Three groups are expected: api, worker, and batch (or any custom names).
    Use t3.micro (free-tier eligible) and keep sizes small for labs.
  EOT
  type = map(object({
    instance_type = string
    desired_size  = number
    min_size      = number
    max_size      = number
    labels        = map(string)
  }))
  default = {
    api = {
      instance_type = "t3.micro"
      desired_size  = 1
      min_size      = 1
      max_size      = 2
      labels        = { role = "api", tier = "app" }
    }
    worker = {
      instance_type = "t3.micro"
      desired_size  = 1
      min_size      = 1
      max_size      = 2
      labels        = { role = "worker", tier = "app" }
    }
    batch = {
      instance_type = "t3.micro"
      desired_size  = 1
      min_size      = 0
      max_size      = 2
      labels        = { role = "batch", tier = "app" }
    }
  }
}

variable "use_pod_identity" {
  description = "Enable EKS Pod Identity associations for supported workloads while retaining IRSA resources for migration safety."
  type        = bool
  default     = false
}

variable "eks_access_admin_principal_arns" {
  description = "Additional IAM principal ARNs that should receive EKS cluster-admin access via access entries."
  type        = list(string)
  default     = []
}

variable "eks_access_ci_principal_arns" {
  description = "Additional IAM principal ARNs that should receive EKS view access via access entries."
  type        = list(string)
  default     = []
}

variable "karpenter_enabled" {
  description = "Enable Karpenter IAM/SQS resources and add a bootstrap managed node group for the Karpenter controller."
  type        = bool
  default     = false
}

variable "karpenter_bootstrap_node_group" {
  description = "Managed node group kept for core system workloads (including Karpenter controller) when Karpenter is enabled."
  type = object({
    instance_type = string
    desired_size  = number
    min_size      = number
    max_size      = number
    labels        = map(string)
  })
  default = {
    instance_type = "t3.small"
    desired_size  = 1
    min_size      = 1
    max_size      = 2
    labels        = { role = "system", tier = "platform" }
  }
}