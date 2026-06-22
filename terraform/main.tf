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

module "eks" {
  source = "./modules/eks"

  cluster_name    = var.eks_cluster_name
  cluster_version = var.eks_cluster_version
  vpc_id          = module.vpc.vpc_id

  private_app_subnet_ids = module.vpc.private_app_subnet_ids
  alb_security_group_id  = aws_security_group.alb.id

  node_groups = var.eks_node_groups

  tags = var.default_tags
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
  db_password = var.db_password

  tags = var.default_tags
}

