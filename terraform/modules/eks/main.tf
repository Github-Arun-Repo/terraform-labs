# Author: Arunasalam Govindasamy

data "aws_ami" "eks_node" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["amazon-eks-node-${var.cluster_version}-v*"]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
  }
}

# -- Cluster Security Group ----------------------------------------------------

resource "aws_security_group" "cluster" {
  name_prefix = "${var.cluster_name}-cluster-"
  description = "EKS cluster control-plane SG"
  vpc_id      = var.vpc_id

  tags = merge(var.tags, { Name = "${var.cluster_name}-cluster-sg" })

  lifecycle { create_before_destroy = true }
}

# Allow control-plane to receive webhooks from nodes
resource "aws_vpc_security_group_ingress_rule" "cluster_from_nodes" {
  security_group_id            = aws_security_group.cluster.id
  description                  = "Allow nodes to reach control-plane"
  from_port                    = 443
  to_port                      = 443
  ip_protocol                  = "tcp"
  referenced_security_group_id = aws_security_group.node.id

  tags = merge(var.tags, { Name = "${var.cluster_name}-cluster-ingress-nodes" })
}

resource "aws_vpc_security_group_egress_rule" "cluster_egress" {
  security_group_id = aws_security_group.cluster.id
  description       = "Allow all outbound from control plane"
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"

  tags = merge(var.tags, { Name = "${var.cluster_name}-cluster-egress" })
}

# -- Node Security Group -------------------------------------------------------

resource "aws_security_group" "node" {
  name_prefix = "${var.cluster_name}-node-"
  description = "EKS worker node SG"
  vpc_id      = var.vpc_id

  tags = merge(var.tags, {
    Name                                        = "${var.cluster_name}-node-sg"
    "kubernetes.io/cluster/${var.cluster_name}" = "owned"
  })

  lifecycle { create_before_destroy = true }
}

# Nodes talk to each other (required for pod networking)
resource "aws_vpc_security_group_ingress_rule" "node_self" {
  security_group_id            = aws_security_group.node.id
  description                  = "Allow inter-node communication"
  ip_protocol                  = "-1"
  referenced_security_group_id = aws_security_group.node.id

  tags = merge(var.tags, { Name = "${var.cluster_name}-node-self" })
}

# Nodes receive kubelet/kube-proxy traffic from control plane
resource "aws_vpc_security_group_ingress_rule" "node_from_cluster" {
  security_group_id            = aws_security_group.node.id
  description                  = "Allow control-plane to reach nodes"
  from_port                    = 1025
  to_port                      = 65535
  ip_protocol                  = "tcp"
  referenced_security_group_id = aws_security_group.cluster.id

  tags = merge(var.tags, { Name = "${var.cluster_name}-node-ingress-cluster" })
}

# Load balancer SG can reach NodePort services
resource "aws_vpc_security_group_ingress_rule" "node_from_alb" {
  security_group_id            = aws_security_group.node.id
  description                  = "Allow ALB to reach NodePort services"
  from_port                    = 30000
  to_port                      = 32767
  ip_protocol                  = "tcp"
  referenced_security_group_id = var.alb_security_group_id

  tags = merge(var.tags, { Name = "${var.cluster_name}-node-ingress-alb" })
}

resource "aws_vpc_security_group_egress_rule" "node_egress" {
  security_group_id = aws_security_group.node.id
  description       = "Allow all outbound from nodes (NAT for internet)"
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"

  tags = merge(var.tags, { Name = "${var.cluster_name}-node-egress" })
}

# -- EKS Cluster ---------------------------------------------------------------

resource "aws_eks_cluster" "this" {
  name     = var.cluster_name
  version  = var.cluster_version
  role_arn = aws_iam_role.cluster.arn

  vpc_config {
    subnet_ids              = var.private_app_subnet_ids
    security_group_ids      = [aws_security_group.cluster.id]
    endpoint_private_access = true
    endpoint_public_access  = false # control plane accessible only from within VPC
  }

  enabled_cluster_log_types = ["api", "audit", "authenticator"]

  tags = merge(var.tags, { Name = var.cluster_name })

  depends_on = [
    aws_iam_role_policy_attachment.cluster_AmazonEKSClusterPolicy,
    aws_iam_role_policy_attachment.cluster_AmazonEKSVPCResourceController,
  ]
}

# -- Bootstrap userdata for self-managed nodes ---------------------------------

locals {
  node_userdata_tpl = <<-EOT
    #!/bin/bash
    set -ex
    /etc/eks/bootstrap.sh ${aws_eks_cluster.this.name} \
      --b64-cluster-ca '${aws_eks_cluster.this.certificate_authority[0].data}' \
      --apiserver-endpoint '${aws_eks_cluster.this.endpoint}' \
      --kubelet-extra-args '--node-labels=%s'
  EOT
}

# -- Self-Managed Node Groups (Launch Template per group) ---------------------

resource "aws_launch_template" "node_group" {
  for_each = var.node_groups

  name_prefix = "${var.cluster_name}-${each.key}-"
  image_id    = data.aws_ami.eks_node.id
  # t3.micro is the smallest EKS-capable, free-tier eligible instance
  instance_type = each.value.instance_type

  iam_instance_profile {
    arn = aws_iam_instance_profile.node.arn
  }

  vpc_security_group_ids = [aws_security_group.node.id]

  user_data = base64encode(format(local.node_userdata_tpl,
    join(",", [
      for k, v in each.value.labels : "${k}=${v}"
    ])
  ))

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required" # IMDSv2 enforced
    http_put_response_hop_limit = 1
  }

  block_device_mappings {
    device_name = "/dev/xvda"
    ebs {
      volume_size           = 20
      volume_type           = "gp3"
      delete_on_termination = true
      encrypted             = true
    }
  }

  tag_specifications {
    resource_type = "instance"
    tags = merge(var.tags, {
      Name                                        = "${var.cluster_name}-${each.key}-node"
      "kubernetes.io/cluster/${var.cluster_name}" = "owned"
    })
  }

  lifecycle { create_before_destroy = true }
}

resource "aws_autoscaling_group" "node_group" {
  for_each = var.node_groups

  name_prefix = "${var.cluster_name}-${each.key}-"

  vpc_zone_identifier = var.private_app_subnet_ids

  desired_capacity = each.value.desired_size
  min_size         = each.value.min_size
  max_size         = each.value.max_size

  launch_template {
    id      = aws_launch_template.node_group[each.key].id
    version = "$Latest"
  }

  dynamic "tag" {
    for_each = merge(var.tags, {
      Name                                        = "${var.cluster_name}-${each.key}-node"
      "kubernetes.io/cluster/${var.cluster_name}" = "owned"
      "k8s.io/cluster-autoscaler/enabled"         = "true"
      "k8s.io/cluster-autoscaler/${var.cluster_name}" = "owned"
    }, { for k, v in each.value.labels : "k8s-label/${k}" => v })

    content {
      key                 = tag.key
      value               = tag.value
      propagate_at_launch = true
    }
  }

  lifecycle {
    ignore_changes = [desired_capacity]
  }

  depends_on = [
    aws_iam_role_policy_attachment.node_AmazonEKSWorkerNodePolicy,
    aws_iam_role_policy_attachment.node_AmazonEKS_CNI_Policy,
    aws_iam_role_policy_attachment.node_AmazonEC2ContainerRegistryReadOnly,
  ]
}

