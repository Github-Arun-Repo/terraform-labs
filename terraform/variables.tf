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

variable "db_password" {
  description = "Master password for the database. Provide via TF_VAR_db_password env var - do not commit to source control."
  type        = string
  sensitive   = true

# -- EKS -----------------------------------------------------------------------

variable "eks_cluster_name" {
  description = "Name of the EKS cluster."
  type        = string
}

variable "eks_cluster_version" {
  description = "Kubernetes version for the EKS cluster (e.g. '1.30')."
  type        = string
  default     = "1.30"
}

variable "eks_node_groups" {
  description = <<-EOT
    Map of self-managed node group configurations.
    Labels are injected as kubelet --node-labels at bootstrap time.
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
}

