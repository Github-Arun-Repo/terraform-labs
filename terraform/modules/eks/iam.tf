# Author: Arunasalam Govindasamy

# -- EKS Cluster IAM Role ------------------------------------------------------

data "aws_iam_policy_document" "cluster_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["eks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "cluster" {
  name_prefix        = "${var.cluster_name}-cluster-"
  assume_role_policy = data.aws_iam_policy_document.cluster_assume_role.json

  tags = merge(var.tags, { Name = "${var.cluster_name}-cluster-role" })
}

resource "aws_iam_role_policy_attachment" "cluster_AmazonEKSClusterPolicy" {
  role       = aws_iam_role.cluster.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
}

resource "aws_iam_role_policy_attachment" "cluster_AmazonEKSVPCResourceController" {
  role       = aws_iam_role.cluster.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSVPCResourceController"
}

data "aws_iam_policy_document" "cluster_kms_access" {
  count = var.cluster_secrets_kms_key_arn == null ? 0 : 1

  statement {
    effect = "Allow"
    actions = [
      "kms:DescribeKey",
      "kms:Encrypt",
      "kms:Decrypt",
      "kms:GenerateDataKey*",
    ]
    resources = [var.cluster_secrets_kms_key_arn]
  }
}

resource "aws_iam_policy" "cluster_kms_access" {
  count = var.cluster_secrets_kms_key_arn == null ? 0 : 1

  name_prefix = "${var.cluster_name}-cluster-kms-"
  description = "Allow EKS control plane role to use KMS key for secret envelope encryption."
  policy      = data.aws_iam_policy_document.cluster_kms_access[0].json

  tags = merge(var.tags, { Name = "${var.cluster_name}-cluster-kms-policy" })
}

resource "aws_iam_role_policy_attachment" "cluster_kms_access" {
  count = var.cluster_secrets_kms_key_arn == null ? 0 : 1

  role       = aws_iam_role.cluster.name
  policy_arn = aws_iam_policy.cluster_kms_access[0].arn
}

# -- Worker Node IAM Role (shared across all node groups) ----------------------

data "aws_iam_policy_document" "node_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "node" {
  name_prefix        = "${var.cluster_name}-node-"
  assume_role_policy = data.aws_iam_policy_document.node_assume_role.json

  tags = merge(var.tags, { Name = "${var.cluster_name}-node-role" })
}

resource "aws_iam_role_policy_attachment" "node_AmazonEKSWorkerNodePolicy" {
  role       = aws_iam_role.node.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"
}

resource "aws_iam_role_policy_attachment" "node_AmazonEKS_CNI_Policy" {
  role       = aws_iam_role.node.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
}

resource "aws_iam_role_policy_attachment" "node_AmazonEC2ContainerRegistryReadOnly" {
  role       = aws_iam_role.node.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

# -- OIDC Provider (required for IRSA) ----------------------------------------
# The TLS thumbprint is fetched from the OIDC issuer endpoint and locked in
# so the provider trust relationship is not sensitive to CA rotations.

data "tls_certificate" "eks_oidc" {
  url = aws_eks_cluster.this.identity[0].oidc[0].issuer
}

resource "aws_iam_openid_connect_provider" "eks" {
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.eks_oidc.certificates[0].sha1_fingerprint]
  url             = aws_eks_cluster.this.identity[0].oidc[0].issuer

  tags = merge(var.tags, { Name = "${var.cluster_name}-oidc-provider" })
}

# -- EBS CSI Addon IRSA Role --------------------------------------------------

data "aws_iam_policy_document" "ebs_csi_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.eks.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "${trimprefix(aws_eks_cluster.this.identity[0].oidc[0].issuer, "https://")}:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "${trimprefix(aws_eks_cluster.this.identity[0].oidc[0].issuer, "https://")}:sub"
      values   = ["system:serviceaccount:kube-system:ebs-csi-controller-sa"]
    }
  }
}

resource "aws_iam_role" "ebs_csi_controller" {
  name_prefix        = "${var.cluster_name}-ebs-csi-"
  assume_role_policy = data.aws_iam_policy_document.ebs_csi_assume_role.json

  tags = merge(var.tags, { Name = "${var.cluster_name}-ebs-csi-role" })
}

resource "aws_iam_role_policy_attachment" "ebs_csi_AmazonEBSCSIDriverPolicy" {
  role       = aws_iam_role.ebs_csi_controller.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicy"
}

