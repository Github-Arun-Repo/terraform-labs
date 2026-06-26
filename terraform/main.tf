# Author: Arunasalam Govindasamy

module "vpc" {
  source = "./modules/vpc"

  vpc_name = var.vpc_name
  vpc_cidr = var.vpc_cidr

  availability_zones       = var.availability_zones
  public_subnet_cidrs      = var.public_subnet_cidrs
  private_app_subnet_cidrs = var.private_app_subnet_cidrs
  private_db_subnet_cidrs  = var.private_db_subnet_cidrs

  enable_nat_gateway     = var.enable_nat_gateway
  single_nat_gateway     = var.single_nat_gateway
  db_subnet_enable_nat   = var.db_subnet_enable_nat
  create_db_subnet_group = var.create_db_subnet_group

  tags = var.default_tags
}

# -- App-tier Security Group ---------------------------------------------------
# Attach this SG to EC2 instances (or ECS tasks) in the private app subnets.
# The RDS module references it to scope its ingress rule.

resource "aws_security_group" "app" {
  name_prefix = "${var.vpc_name}-app-"
  description = "Security group for the application tier"
  vpc_id      = module.vpc.vpc_id

  tags = merge(var.default_tags, {
    Name = "${var.vpc_name}-app-sg"
  })

  lifecycle {
    create_before_destroy = true
  }
}

# Allow app tier to reach the internet via NAT (HTTP/HTTPS)
resource "aws_vpc_security_group_egress_rule" "app_egress" {
  security_group_id = aws_security_group.app.id
  description       = "Allow all outbound from app tier"
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"

  tags = merge(var.default_tags, {
    Name = "${var.vpc_name}-app-egress"
  })
}

# -- ALB Security Group (public subnets) --------------------------------------
# The ALB lives in public subnets and is the only path to reach EKS nodes.

resource "aws_security_group" "alb" {
  name_prefix = "${var.vpc_name}-alb-"
  description = "Security group for the public-facing Application Load Balancer"
  vpc_id      = module.vpc.vpc_id

  tags = merge(var.default_tags, {
    Name = "${var.vpc_name}-alb-sg"
  })

  lifecycle { create_before_destroy = true }
}

resource "aws_vpc_security_group_ingress_rule" "alb_http" {
  security_group_id = aws_security_group.alb.id
  description       = "Allow inbound HTTP from internet"
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
  cidr_ipv4         = "0.0.0.0/0"

  tags = merge(var.default_tags, { Name = "${var.vpc_name}-alb-http" })
}

resource "aws_vpc_security_group_ingress_rule" "alb_https" {
  security_group_id = aws_security_group.alb.id
  description       = "Allow inbound HTTPS from internet"
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
  cidr_ipv4         = "0.0.0.0/0"

  tags = merge(var.default_tags, { Name = "${var.vpc_name}-alb-https" })
}

resource "aws_vpc_security_group_egress_rule" "alb_egress" {
  security_group_id = aws_security_group.alb.id
  description       = "Allow ALB to forward to node NodePorts"
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"

  tags = merge(var.default_tags, { Name = "${var.vpc_name}-alb-egress" })
}

# -- EKS Module ----------------------------------------------------------------

locals {
  effective_eks_node_groups = var.karpenter_enabled ? merge({
    "karpenter-bootstrap" = var.karpenter_bootstrap_node_group
  }, var.eks_node_groups) : var.eks_node_groups
}

module "eks" {
  source = "./modules/eks"

  cluster_name    = var.eks_cluster_name
  cluster_version = var.eks_cluster_version
  authentication_mode = var.eks_authentication_mode
  vpc_id          = module.vpc.vpc_id

  private_app_subnet_ids = module.vpc.private_app_subnet_ids
  alb_security_group_id  = aws_security_group.alb.id

  node_groups = local.effective_eks_node_groups

  tags = var.default_tags
}

module "document_processor_ecr" {
  source = "./modules/ecr"

  repository_name      = var.document_processor_ecr_repository_name
  image_tag_mutability = var.document_processor_ecr_image_tag_mutability
  image_scan_on_push   = var.document_processor_ecr_image_scan_on_push
  force_delete         = var.document_processor_ecr_force_delete
  max_image_count      = var.document_processor_ecr_max_image_count

  tags = var.default_tags
}

# -- SQS (Document Ingestion) -------------------------------------------------

resource "aws_sqs_queue" "document_ingestion_dlq" {
  name                      = var.document_ingestion_dlq_name
  message_retention_seconds = var.document_ingestion_dlq_retention_seconds
  sqs_managed_sse_enabled   = true

  tags = merge(var.default_tags, {
    Name = var.document_ingestion_dlq_name
  })
}

resource "aws_sqs_queue" "document_ingestion_queue" {
  name                       = var.document_ingestion_queue_name
  visibility_timeout_seconds = var.document_ingestion_visibility_timeout_seconds
  message_retention_seconds  = var.document_ingestion_message_retention_seconds
  sqs_managed_sse_enabled    = true

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.document_ingestion_dlq.arn
    maxReceiveCount     = var.document_ingestion_max_receive_count
  })

  tags = merge(var.default_tags, {
    Name = var.document_ingestion_queue_name
  })
}

data "aws_caller_identity" "current" {}
data "aws_partition" "current" {}

# -- GitHub Actions OIDC Federation -------------------------------------------

data "tls_certificate" "github_actions_oidc" {
  url = "https://token.actions.githubusercontent.com"
}

resource "aws_iam_openid_connect_provider" "github_actions" {
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.github_actions_oidc.certificates[0].sha1_fingerprint]

  tags = merge(var.default_tags, { Name = "github-actions-oidc-provider" })
}

data "aws_iam_policy_document" "github_actions_ci_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github_actions.arn]
    }

    # Restrict assume-role to this repository and approved CI subjects.
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = var.github_actions_ci_subs
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:repository"
      values   = [var.github_actions_repository]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "github_actions_ci" {
  name_prefix        = "github-actions-ci-"
  assume_role_policy = data.aws_iam_policy_document.github_actions_ci_assume_role.json

  tags = merge(var.default_tags, { Name = "github-actions-ci" })
}

data "aws_iam_policy_document" "github_actions_ci_ecr" {
  statement {
    effect    = "Allow"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    effect = "Allow"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:CompleteLayerUpload",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart",
      "ecr:BatchGetImage",
      "ecr:GetDownloadUrlForLayer",
      "ecr:DescribeImages",
      "ecr:DescribeRepositories",
      "ecr:ListImages",
    ]
    resources = [module.document_processor_ecr.repository_arn]
  }
}

resource "aws_iam_policy" "github_actions_ci_ecr" {
  name_prefix = "github-actions-ci-ecr-"
  description = "Allows GitHub Actions CI role to push and pull images for the document-processor ECR repository."
  policy      = data.aws_iam_policy_document.github_actions_ci_ecr.json

  tags = merge(var.default_tags, { Name = "github-actions-ci-ecr-policy" })
}

resource "aws_iam_role_policy_attachment" "github_actions_ci_ecr" {
  role       = aws_iam_role.github_actions_ci.name
  policy_arn = aws_iam_policy.github_actions_ci_ecr.arn
}

data "aws_iam_policy_document" "github_actions_plan_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github_actions.arn]
    }

    # Restrict assume-role to this repository and approved Terraform plan subjects.
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = var.github_actions_plan_subs
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:repository"
      values   = [var.github_actions_repository]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "github_actions_plan" {
  name_prefix        = "github-actions-plan-"
  assume_role_policy = data.aws_iam_policy_document.github_actions_plan_assume_role.json

  tags = merge(var.default_tags, { Name = "github-actions-plan" })
}

resource "aws_iam_role_policy_attachment" "github_actions_plan_readonly" {
  role       = aws_iam_role.github_actions_plan.name
  policy_arn = "arn:aws:iam::aws:policy/ReadOnlyAccess"
}

data "aws_iam_policy_document" "allow_s3_to_send_document_events" {
  statement {
    sid    = "AllowS3ToSendDocumentUploadEvents"
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["s3.amazonaws.com"]
    }

    actions = ["sqs:SendMessage"]

    resources = [aws_sqs_queue.document_ingestion_queue.arn]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = ["arn:${data.aws_partition.current.partition}:s3:::${var.documents_inventory_bucket_name}"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:SourceAccount"
      values   = [data.aws_caller_identity.current.account_id]
    }
  }
}

resource "aws_sqs_queue_policy" "allow_s3_to_send_document_events" {
  queue_url = aws_sqs_queue.document_ingestion_queue.id
  policy    = data.aws_iam_policy_document.allow_s3_to_send_document_events.json
}

# -- DynamoDB (Document Inventory) ---------------------------------------------

resource "aws_dynamodb_table" "document_inventory" {
  name         = var.document_inventory_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "PK"
  range_key    = "SK"

  attribute {
    name = "PK"
    type = "S"
  }

  attribute {
    name = "SK"
    type = "S"
  }

  attribute {
    name = "GSI1PK"
    type = "S"
  }

  attribute {
    name = "GSI1SK"
    type = "S"
  }

  attribute {
    name = "GSI2PK"
    type = "S"
  }

  attribute {
    name = "GSI2SK"
    type = "S"
  }

  global_secondary_index {
    name            = "GSI1"
    hash_key        = "GSI1PK"
    range_key       = "GSI1SK"
    projection_type = "ALL"
  }

  dynamic "global_secondary_index" {
    for_each = var.enable_document_inventory_status_gsi ? [1] : []
    content {
      name            = "GSI2"
      hash_key        = "GSI2PK"
      range_key       = "GSI2SK"
      projection_type = "ALL"
    }
  }

  point_in_time_recovery {
    enabled = true
  }

  server_side_encryption {
    enabled = true
  }

  tags = merge(var.default_tags, {
    Name = var.document_inventory_table_name
  })
}

# -- S3 Module (Document Inventory) -------------------------------------------

module "documents_inventory_s3" {
  source = "./modules/s3"

  bucket_name         = var.documents_inventory_bucket_name
  force_destroy       = var.documents_inventory_bucket_force_delete
  enable_versioning   = var.documents_inventory_bucket_enable_versioning
  kms_alias_name      = var.documents_inventory_kms_alias_name
  kms_key_description = var.documents_inventory_kms_key_description

  enable_sqs_notifications = var.enable_documents_inventory_sqs_notifications
  sqs_notification_queue_arn = aws_sqs_queue.document_ingestion_queue.arn
  sqs_notification_prefixes = var.sqs_notification_prefixes
  sqs_notification_events   = var.sqs_notification_events

  tags = var.default_tags

  depends_on = [aws_sqs_queue_policy.allow_s3_to_send_document_events]
}

# Allow EKS nodes to reach RDS - node SG gets the DB port opened
resource "aws_vpc_security_group_ingress_rule" "rds_from_eks_nodes" {
  security_group_id            = module.rds.rds_security_group_id
  description                  = "Allow EKS nodes to reach RDS"
  from_port                    = var.db_port
  to_port                      = var.db_port
  ip_protocol                  = "tcp"
  referenced_security_group_id = module.eks.node_security_group_id

  tags = merge(var.default_tags, { Name = "${var.vpc_name}-rds-ingress-eks-nodes" })
}

# -- RDS Module ----------------------------------------------------------------

module "rds" {
  source = "./modules/rds"

  identifier            = var.db_identifier
  vpc_id                = module.vpc.vpc_id
  db_subnet_group_name  = module.vpc.db_subnet_group_name
  app_security_group_id = aws_security_group.app.id

  engine                    = var.db_engine
  engine_version            = var.db_engine_version
  db_parameter_group_family = var.db_parameter_group_family
  db_port                   = var.db_port

  db_name     = var.db_name
  db_username = var.db_username
  manage_master_user_password = var.db_manage_master_user_password

  tags = var.default_tags
}

# -- IRSA: Jenkins Build Agent → ECR Push -------------------------------------
# The Jenkins build agent pods assume this role via the Kubernetes service
# account annotation instead of using static AWS credentials.

locals {
  jenkins_sa_namespace = "jenkins"
  jenkins_sa_name      = "jenkins-build-agent"
  document_api_sa_namespace        = var.document_api_sa_namespace
  document_api_sa_name             = var.document_api_sa_name
  document_processing_sa_namespace = var.document_processing_sa_namespace
  document_processing_sa_name      = var.document_processing_sa_name
  document_review_sa_namespace     = var.document_review_sa_namespace
  document_review_sa_name          = var.document_review_sa_name
  user_management_sa_namespace     = var.user_management_sa_namespace
  user_management_sa_name          = var.user_management_sa_name
  # Strip the https:// scheme – AWS condition keys use the bare host/path form
  oidc_issuer_host = trimprefix(module.eks.oidc_issuer_url, "https://")
}

data "aws_iam_policy_document" "jenkins_agent_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [module.eks.oidc_provider_arn]
    }

    # Restrict to the exact Kubernetes service account
    condition {
      test     = "StringEquals"
      variable = "${local.oidc_issuer_host}:sub"
      values   = ["system:serviceaccount:${local.jenkins_sa_namespace}:${local.jenkins_sa_name}"]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.oidc_issuer_host}:aud"
      values   = ["sts.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "jenkins_build_agent" {
  name_prefix        = "jenkins-build-agent-"
  assume_role_policy = data.aws_iam_policy_document.jenkins_agent_assume_role.json

  tags = merge(var.default_tags, { Name = "jenkins-build-agent-irsa" })
}

data "aws_iam_policy_document" "ecr_push" {
  # GetAuthorizationToken has no resource ARN – it must be on "*"
  statement {
    effect    = "Allow"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  # All other ECR push/pull actions are scoped to the document-processor repository
  statement {
    effect = "Allow"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:CompleteLayerUpload",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart",
      "ecr:BatchGetImage",
      "ecr:GetDownloadUrlForLayer",
      "ecr:DescribeImages",
      "ecr:DescribeRepositories",
      "ecr:ListImages",
    ]
    resources = [module.document_processor_ecr.repository_arn]
  }
}

resource "aws_iam_policy" "ecr_push" {
  name_prefix = "jenkins-ecr-push-"
  description = "Allows the Jenkins build-agent IRSA role to push images to the document-processor ECR repository."
  policy      = data.aws_iam_policy_document.ecr_push.json

  tags = merge(var.default_tags, { Name = "jenkins-ecr-push-policy" })
}

resource "aws_iam_role_policy_attachment" "jenkins_agent_ecr_push" {
  role       = aws_iam_role.jenkins_build_agent.name
  policy_arn = aws_iam_policy.ecr_push.arn
}

# -- IRSA: document-api-service → DynamoDB + S3 (presigned URL paths) ---------

data "aws_iam_policy_document" "document_api_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [module.eks.oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.oidc_issuer_host}:sub"
      values   = ["system:serviceaccount:${local.document_api_sa_namespace}:${local.document_api_sa_name}"]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.oidc_issuer_host}:aud"
      values   = ["sts.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "document_api_service" {
  name_prefix        = "document-api-service-"
  assume_role_policy = data.aws_iam_policy_document.document_api_assume_role.json

  tags = merge(var.default_tags, { Name = "document-api-service-irsa" })
}

data "aws_iam_policy_document" "document_api_access" {
  statement {
    effect = "Allow"
    actions = [
      "dynamodb:PutItem",
      "dynamodb:GetItem",
      "dynamodb:Query",
      "dynamodb:UpdateItem",
    ]
    resources = [
      aws_dynamodb_table.document_inventory.arn,
      "${aws_dynamodb_table.document_inventory.arn}/index/*",
    ]
  }

  statement {
    effect = "Allow"
    actions = [
      "s3:PutObject",
      "s3:GetObject",
    ]
    resources = [
      "${module.documents_inventory_s3.bucket_arn}/invoice/raw/*",
      "${module.documents_inventory_s3.bucket_arn}/receipt/raw/*",
    ]
  }
}

resource "aws_iam_policy" "document_api_access" {
  name_prefix = "document-api-access-"
  description = "Allows document-api-service IRSA role to write/query DocumentInventory and create presigned URL target object permissions."
  policy      = data.aws_iam_policy_document.document_api_access.json

  tags = merge(var.default_tags, { Name = "document-api-access-policy" })
}

resource "aws_iam_role_policy_attachment" "document_api_access" {
  role       = aws_iam_role.document_api_service.name
  policy_arn = aws_iam_policy.document_api_access.arn
}

# -- IRSA: document-processing-service → SQS + S3 (+optional Textract) --------

data "aws_iam_policy_document" "document_processing_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [module.eks.oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.oidc_issuer_host}:sub"
      values   = ["system:serviceaccount:${local.document_processing_sa_namespace}:${local.document_processing_sa_name}"]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.oidc_issuer_host}:aud"
      values   = ["sts.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "document_processing_service" {
  name_prefix        = "document-processing-service-"
  assume_role_policy = data.aws_iam_policy_document.document_processing_assume_role.json

  tags = merge(var.default_tags, { Name = "document-processing-service-irsa" })
}

data "aws_iam_policy_document" "document_processing_access" {
  statement {
    effect = "Allow"
    actions = [
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:GetQueueAttributes",
      "sqs:GetQueueUrl",
      "sqs:ChangeMessageVisibility",
    ]
    resources = [aws_sqs_queue.document_ingestion_queue.arn]
  }

  statement {
    effect = "Allow"
    actions = [
      "s3:GetObject",
      "s3:GetObjectVersion",
    ]
    resources = [
      "${module.documents_inventory_s3.bucket_arn}/invoice/raw/*",
      "${module.documents_inventory_s3.bucket_arn}/receipt/raw/*",
    ]
  }

  statement {
    effect = "Allow"
    actions = [
      "s3:PutObject",
    ]
    resources = [
      "${module.documents_inventory_s3.bucket_arn}/invoice/processed/*",
      "${module.documents_inventory_s3.bucket_arn}/receipt/processed/*",
      "${module.documents_inventory_s3.bucket_arn}/invoice/failed/*",
      "${module.documents_inventory_s3.bucket_arn}/receipt/failed/*",
    ]
  }

  statement {
    effect = "Allow"
    actions = [
      "dynamodb:GetItem",
      "dynamodb:PutItem",
      "dynamodb:UpdateItem",
      "dynamodb:Query",
      "dynamodb:ConditionCheckItem",
    ]
    resources = [
      aws_dynamodb_table.document_inventory.arn,
      "${aws_dynamodb_table.document_inventory.arn}/index/*",
    ]
  }

  dynamic "statement" {
    for_each = var.enable_document_processing_textract_permissions ? [1] : []
    content {
      effect    = "Allow"
      actions   = ["textract:AnalyzeExpense"]
      resources = ["*"]
    }
  }
}

resource "aws_iam_policy" "document_processing_access" {
  name_prefix = "document-processing-access-"
  description = "Allows document-processing-service IRSA role to consume ingestion SQS and read/write required S3 prefixes."
  policy      = data.aws_iam_policy_document.document_processing_access.json

  tags = merge(var.default_tags, { Name = "document-processing-access-policy" })
}

resource "aws_iam_role_policy_attachment" "document_processing_access" {
  role       = aws_iam_role.document_processing_service.name
  policy_arn = aws_iam_policy.document_processing_access.arn
}

# -- IRSA: document-review-service -> DynamoDB + S3 --------------------------

data "aws_iam_policy_document" "document_review_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [module.eks.oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.oidc_issuer_host}:sub"
      values   = ["system:serviceaccount:${local.document_review_sa_namespace}:${local.document_review_sa_name}"]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.oidc_issuer_host}:aud"
      values   = ["sts.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "document_review_service" {
  name_prefix        = "document-review-service-"
  assume_role_policy = data.aws_iam_policy_document.document_review_assume_role.json

  tags = merge(var.default_tags, { Name = "document-review-service-irsa" })
}

data "aws_iam_policy_document" "document_review_access" {
  statement {
    effect = "Allow"
    actions = [
      "dynamodb:GetItem",
      "dynamodb:Query",
      "dynamodb:UpdateItem",
      "dynamodb:PutItem",
      "dynamodb:ConditionCheckItem",
    ]
    resources = [
      aws_dynamodb_table.document_inventory.arn,
      "${aws_dynamodb_table.document_inventory.arn}/index/*",
    ]
  }

  statement {
    effect = "Allow"
    actions = [
      "s3:GetObject",
    ]
    resources = [
      "${module.documents_inventory_s3.bucket_arn}/invoice/raw/*",
      "${module.documents_inventory_s3.bucket_arn}/receipt/raw/*",
      "${module.documents_inventory_s3.bucket_arn}/invoice/processed/*",
      "${module.documents_inventory_s3.bucket_arn}/receipt/processed/*",
    ]
  }
}

resource "aws_iam_policy" "document_review_access" {
  name_prefix = "document-review-access-"
  description = "Allows document-review-service IRSA role to read documents and manage review data in DocumentInventory."
  policy      = data.aws_iam_policy_document.document_review_access.json

  tags = merge(var.default_tags, { Name = "document-review-access-policy" })
}

resource "aws_iam_role_policy_attachment" "document_review_access" {
  role       = aws_iam_role.document_review_service.name
  policy_arn = aws_iam_policy.document_review_access.arn
}

# -- IRSA: user-management-service -> RDS master secret -----------------------

data "aws_iam_policy_document" "user_management_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [module.eks.oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.oidc_issuer_host}:sub"
      values   = ["system:serviceaccount:${local.user_management_sa_namespace}:${local.user_management_sa_name}"]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.oidc_issuer_host}:aud"
      values   = ["sts.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "user_management_service" {
  name_prefix        = "user-management-service-"
  assume_role_policy = data.aws_iam_policy_document.user_management_assume_role.json

  tags = merge(var.default_tags, { Name = "user-management-service-irsa" })
}

data "aws_iam_policy_document" "user_management_secrets_access" {
  statement {
    effect = "Allow"
    actions = [
      "secretsmanager:GetSecretValue",
      "secretsmanager:DescribeSecret",
    ]
    resources = [module.rds.master_user_secret_arn]
  }
}

resource "aws_iam_policy" "user_management_secrets_access" {
  name_prefix = "user-management-secrets-"
  description = "Allows user-management-service IRSA role to read RDS master credentials from Secrets Manager."
  policy      = data.aws_iam_policy_document.user_management_secrets_access.json

  tags = merge(var.default_tags, { Name = "user-management-secrets-policy" })
}

resource "aws_iam_role_policy_attachment" "user_management_secrets_access" {
  role       = aws_iam_role.user_management_service.name
  policy_arn = aws_iam_policy.user_management_secrets_access.arn
}