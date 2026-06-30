# Author: Arunasalam Govindasamy

aws_region = "eu-west-1"

vpc_name = "my-vpc"
vpc_cidr = "10.0.0.0/16"

# Three AZs - adjust to match your region
availability_zones = ["eu-west-1a", "eu-west-1b", "eu-west-1c"]

# One public subnet per AZ
public_subnet_cidrs = [
  "10.0.0.0/24",
  "10.0.1.0/24",
  "10.0.2.0/24",
]

# One private app subnet per AZ
private_app_subnet_cidrs = [
  "10.0.10.0/24",
  "10.0.11.0/24",
  "10.0.12.0/24",
]

# One private DB subnet per AZ
private_db_subnet_cidrs = [
  "10.0.20.0/24",
  "10.0.21.0/24",
  "10.0.22.0/24",
]

# NAT - one per AZ for full HA; set single_nat_gateway = true to save cost in non-prod
enable_nat_gateway   = true
single_nat_gateway   = false
db_subnet_enable_nat = false

create_db_subnet_group = true

default_tags = {
  Project     = "terraform-labs"
  Environment = "dev"
  ManagedBy   = "terraform"
}

# -- GitHub Actions OIDC ------------------------------------------------------

github_actions_repository = "Github-Arun-Repo/terraform-labs"

# Restrict CI role to workflows running from main branch in this repository.
github_actions_ci_subs = [
  "repo:Github-Arun-Repo/terraform-labs:ref:refs/heads/main",
]

# Restrict plan role to main and pull_request contexts in this repository.
github_actions_plan_subs = [
  "repo:Github-Arun-Repo/terraform-labs:ref:refs/heads/main",
  "repo:Github-Arun-Repo/terraform-labs:pull_request",
]

# -- ECR -----------------------------------------------------------------------

document_processor_ecr_repository_name       = "document-processor"
document_processor_ecr_image_tag_mutability  = "MUTABLE"
document_processor_ecr_image_scan_on_push    = true
document_processor_ecr_force_delete          = false
document_processor_ecr_max_image_count       = 30

# -- S3 (Document Inventory) ---------------------------------------------------

documents_inventory_bucket_name              = "documents-inventory-s3"
documents_inventory_bucket_force_delete      = false
documents_inventory_bucket_enable_versioning = true
documents_inventory_kms_alias_name           = "alias/documents-inventory-s3"
documents_inventory_kms_key_description      = "KMS key for documents-inventory-s3 bucket encryption"

document_ingestion_queue_name                  = "document-ingestion-queue"
document_ingestion_dlq_name                    = "document-ingestion-dlq"
document_ingestion_visibility_timeout_seconds  = 180
document_ingestion_message_retention_seconds   = 345600
document_ingestion_max_receive_count           = 3
document_ingestion_dlq_retention_seconds       = 1209600

sqs_notification_prefixes = [
  "invoice/raw/",
  "receipt/raw/",
]

document_inventory_table_name = "DocumentInventory"

document_api_sa_namespace        = "document-api-service"
document_api_sa_name             = "document-api-service"
document_processing_sa_namespace = "document-processing-service"
document_processing_sa_name      = "document-processing-service"
document_review_sa_namespace     = "document-review-service"
document_review_sa_name          = "document-review-service"
user_management_sa_namespace     = "user-management-service"
user_management_sa_name          = "user-management-service"

# -- RDS (free tier) -----------------------------------------------------------
# Engine is PostgreSQL to match user-management-service (Spring Data JPA + Flyway
# postgres migrations, jdbc:postgresql driver).

db_identifier             = "my-app-db"
db_engine                 = "postgres"
db_engine_version         = "16.4"
db_parameter_group_family = "postgres16"
db_port                   = 5432
db_name                   = "document_identity"
db_username               = "doc_user"
db_manage_master_user_password = true

# -- EKS -----------------------------------------------------------------------

eks_cluster_name    = "my-app-eks"
eks_cluster_version = "1.36"
eks_authentication_mode = "API_AND_CONFIG_MAP"

use_pod_identity = false
karpenter_enabled = false

# Three managed node groups - all t3.micro (free-tier eligible), 1 node each for labs.
# Labels are applied through managed node group configuration.
eks_node_groups = {
  api = {
    instance_type = "t3.micro"
    desired_size  = 1
    min_size      = 1
    max_size      = 2
    labels = {
      role        = "api"
      tier        = "app"
      environment = "dev"
    }
  }
  worker = {
    instance_type = "t3.micro"
    desired_size  = 1
    min_size      = 1
    max_size      = 2
    labels = {
      role        = "worker"
      tier        = "app"
      environment = "dev"
    }
  }
  batch = {
    instance_type = "t3.micro"
    desired_size  = 1
    min_size      = 0
    max_size      = 2
    labels = {
      role        = "batch"
      tier        = "app"
      environment = "dev"
    }
  }
}

eks_access_admin_principal_arns = []
eks_access_ci_principal_arns    = []

