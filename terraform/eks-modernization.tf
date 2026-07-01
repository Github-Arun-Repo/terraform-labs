# Author: Arunasalam Govindasamy

# -- GitHub Actions Deploy Role (for EKS access entries) ----------------------

data "aws_iam_policy_document" "github_actions_deploy_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github_actions.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = var.github_actions_deploy_subs
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

resource "aws_iam_role" "github_actions_deploy" {
  name_prefix        = "github-actions-deploy-"
  assume_role_policy = data.aws_iam_policy_document.github_actions_deploy_assume_role.json

  tags = merge(var.default_tags, { Name = "github-actions-deploy" })
}

# -- EKS Access Entries --------------------------------------------------------

locals {
  eks_access_admin_principals = toset(concat(
    var.eks_access_admin_principal_arns,
    [aws_iam_role.github_actions_deploy.arn]
  ))

  eks_access_ci_principals = toset(concat(
    var.eks_access_ci_principal_arns,
    [aws_iam_role.github_actions_ci.arn, aws_iam_role.github_actions_plan.arn]
  ))
}

resource "aws_eks_access_entry" "admin" {
  for_each = local.eks_access_admin_principals

  cluster_name  = module.eks.cluster_name
  principal_arn = each.value
  type          = "STANDARD"
}

resource "aws_eks_access_policy_association" "admin" {
  for_each = local.eks_access_admin_principals

  cluster_name  = module.eks.cluster_name
  principal_arn = each.value
  policy_arn    = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"

  access_scope {
    type = "cluster"
  }
}

resource "aws_eks_access_entry" "ci" {
  for_each = local.eks_access_ci_principals

  cluster_name  = module.eks.cluster_name
  principal_arn = each.value
  type          = "STANDARD"
}

resource "aws_eks_access_policy_association" "ci" {
  for_each = local.eks_access_ci_principals

  cluster_name  = module.eks.cluster_name
  principal_arn = each.value
  policy_arn    = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSViewPolicy"

  access_scope {
    type = "cluster"
  }
}

# -- Optional EKS Pod Identity Migration Path ---------------------------------

locals {
  pod_identity_assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "pods.eks.amazonaws.com"
        }
        Action = [
          "sts:AssumeRole",
          "sts:TagSession"
        ]
      }
    ]
  })
}

resource "aws_iam_role" "document_api_service_pod_identity" {
  count = var.use_pod_identity ? 1 : 0

  name_prefix        = "document-api-service-pod-id-"
  assume_role_policy = local.pod_identity_assume_role_policy

  tags = merge(var.default_tags, { Name = "document-api-service-pod-identity" })
}

resource "aws_iam_role_policy_attachment" "document_api_service_pod_identity_access" {
  count = var.use_pod_identity ? 1 : 0

  role       = aws_iam_role.document_api_service_pod_identity[0].name
  policy_arn = aws_iam_policy.document_api_access.arn
}

resource "aws_eks_pod_identity_association" "document_api_service" {
  count = var.use_pod_identity ? 1 : 0

  cluster_name    = module.eks.cluster_name
  namespace       = var.document_api_sa_namespace
  service_account = var.document_api_sa_name
  role_arn        = aws_iam_role.document_api_service_pod_identity[0].arn
}

resource "aws_iam_role" "document_processing_service_pod_identity" {
  count = var.use_pod_identity ? 1 : 0

  name_prefix        = "document-processing-pod-id-"
  assume_role_policy = local.pod_identity_assume_role_policy

  tags = merge(var.default_tags, { Name = "document-processing-service-pod-identity" })
}

resource "aws_iam_role_policy_attachment" "document_processing_service_pod_identity_access" {
  count = var.use_pod_identity ? 1 : 0

  role       = aws_iam_role.document_processing_service_pod_identity[0].name
  policy_arn = aws_iam_policy.document_processing_access.arn
}

resource "aws_eks_pod_identity_association" "document_processing_service" {
  count = var.use_pod_identity ? 1 : 0

  cluster_name    = module.eks.cluster_name
  namespace       = var.document_processing_sa_namespace
  service_account = var.document_processing_sa_name
  role_arn        = aws_iam_role.document_processing_service_pod_identity[0].arn
}

resource "aws_iam_role" "document_review_service_pod_identity" {
  count = var.use_pod_identity ? 1 : 0

  name_prefix        = "document-review-service-pod-id-"
  assume_role_policy = local.pod_identity_assume_role_policy

  tags = merge(var.default_tags, { Name = "document-review-service-pod-identity" })
}

resource "aws_iam_role_policy_attachment" "document_review_service_pod_identity_access" {
  count = var.use_pod_identity ? 1 : 0

  role       = aws_iam_role.document_review_service_pod_identity[0].name
  policy_arn = aws_iam_policy.document_review_access.arn
}

resource "aws_eks_pod_identity_association" "document_review_service" {
  count = var.use_pod_identity ? 1 : 0

  cluster_name    = module.eks.cluster_name
  namespace       = var.document_review_sa_namespace
  service_account = var.document_review_sa_name
  role_arn        = aws_iam_role.document_review_service_pod_identity[0].arn
}

# -- Optional Karpenter Foundation --------------------------------------------

resource "aws_sqs_queue" "karpenter_interruption" {
  count = var.karpenter_enabled ? 1 : 0

  name                      = "${var.eks_cluster_name}-karpenter-interruption"
  message_retention_seconds = 300
  kms_master_key_id         = aws_kms_key.platform_data.arn
  kms_data_key_reuse_period_seconds = 300

  tags = merge(var.default_tags, { Name = "${var.eks_cluster_name}-karpenter-interruption" })
}

resource "aws_iam_role" "karpenter_node" {
  count = var.karpenter_enabled ? 1 : 0

  name_prefix = "karpenter-node-"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = merge(var.default_tags, { Name = "karpenter-node-role" })
}

resource "aws_iam_role_policy_attachment" "karpenter_node_worker" {
  count = var.karpenter_enabled ? 1 : 0

  role       = aws_iam_role.karpenter_node[0].name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"
}

resource "aws_iam_role_policy_attachment" "karpenter_node_cni" {
  count = var.karpenter_enabled ? 1 : 0

  role       = aws_iam_role.karpenter_node[0].name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
}

resource "aws_iam_role_policy_attachment" "karpenter_node_ecr" {
  count = var.karpenter_enabled ? 1 : 0

  role       = aws_iam_role.karpenter_node[0].name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

resource "aws_iam_role_policy_attachment" "karpenter_node_ssm" {
  count = var.karpenter_enabled ? 1 : 0

  role       = aws_iam_role.karpenter_node[0].name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "karpenter_node" {
  count = var.karpenter_enabled ? 1 : 0

  name_prefix = "karpenter-node-"
  role        = aws_iam_role.karpenter_node[0].name

  tags = merge(var.default_tags, { Name = "karpenter-node-profile" })
}

resource "aws_eks_access_entry" "karpenter_node" {
  count = var.karpenter_enabled ? 1 : 0

  cluster_name  = module.eks.cluster_name
  principal_arn = aws_iam_role.karpenter_node[0].arn
  type          = "EC2_LINUX"
}

locals {
  karpenter_oidc_host = trimprefix(module.eks.oidc_issuer_url, "https://")
}

data "aws_iam_policy_document" "karpenter_controller_assume_role" {
  count = var.karpenter_enabled ? 1 : 0

  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [module.eks.oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.karpenter_oidc_host}:sub"
      values   = ["system:serviceaccount:karpenter:karpenter"]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.karpenter_oidc_host}:aud"
      values   = ["sts.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "karpenter_controller" {
  count = var.karpenter_enabled ? 1 : 0

  name_prefix        = "karpenter-controller-"
  assume_role_policy = data.aws_iam_policy_document.karpenter_controller_assume_role[0].json

  tags = merge(var.default_tags, { Name = "karpenter-controller-role" })
}

data "aws_iam_policy_document" "karpenter_controller" {
  count = var.karpenter_enabled ? 1 : 0

  statement {
    effect = "Allow"
    actions = [
      "ec2:CreateFleet",
      "ec2:RunInstances",
      "ec2:CreateLaunchTemplate",
    ]
    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/kubernetes.io/cluster/${var.eks_cluster_name}"
      values   = ["owned"]
    }

    condition {
      test     = "StringLike"
      variable = "aws:RequestTag/karpenter.sh/nodepool"
      values   = ["*"]
    }
  }

  statement {
    effect = "Allow"
    actions = [
      "ec2:Describe*",
      "ec2:GetInstanceTypesFromInstanceRequirements",
      "pricing:GetProducts",
      "ssm:GetParameter",
      "eks:DescribeCluster",
    ]
    resources = ["*"]
  }

  statement {
    effect = "Allow"
    actions = [
      "ec2:DeleteLaunchTemplate",
    ]
    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "ec2:ResourceTag/kubernetes.io/cluster/${var.eks_cluster_name}"
      values   = ["owned"]
    }
  }

  statement {
    effect = "Allow"
    actions = [
      "ec2:TerminateInstances",
    ]
    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "ec2:ResourceTag/kubernetes.io/cluster/${var.eks_cluster_name}"
      values   = ["owned"]
    }

    condition {
      test     = "StringLike"
      variable = "ec2:ResourceTag/karpenter.sh/nodepool"
      values   = ["*"]
    }
  }

  statement {
    effect    = "Allow"
    actions   = ["iam:PassRole"]
    resources = [aws_iam_role.karpenter_node[0].arn]
  }

  statement {
    effect = "Allow"
    actions = [
      "sqs:GetQueueUrl",
      "sqs:GetQueueAttributes",
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
    ]
    resources = [aws_sqs_queue.karpenter_interruption[0].arn]
  }
}

resource "aws_iam_policy" "karpenter_controller" {
  count = var.karpenter_enabled ? 1 : 0

  name_prefix = "karpenter-controller-"
  description = "Permissions for Karpenter controller to provision and manage EC2 capacity."
  policy      = data.aws_iam_policy_document.karpenter_controller[0].json

  tags = merge(var.default_tags, { Name = "karpenter-controller-policy" })
}

resource "aws_iam_role_policy_attachment" "karpenter_controller" {
  count = var.karpenter_enabled ? 1 : 0

  role       = aws_iam_role.karpenter_controller[0].name
  policy_arn = aws_iam_policy.karpenter_controller[0].arn
}
