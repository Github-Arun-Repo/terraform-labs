# Author: Arunasalam Govindasamy

variable "cluster_name" {
  description = "Name of the EKS cluster."
  type        = string
}

variable "cluster_version" {
  description = "Kubernetes version for the EKS cluster (e.g. '1.30')."
  type        = string
  default     = "1.30"
}

variable "vpc_id" {
  description = "ID of the VPC."
  type        = string
}

variable "private_app_subnet_ids" {
  description = "IDs of the private app-tier subnets where nodes and the control plane ENIs are placed."
  type        = list(string)
}

variable "alb_security_group_id" {
  description = "ID of the ALB Security Group. Nodes will accept NodePort traffic from this SG only."
  type        = string
}

variable "node_groups" {
  description = <<-EOT
    Map of self-managed node group definitions.
    Each key becomes the group name. Labels are applied as kubelet node labels.
    Example:
      {
        api = {
          instance_type = "t3.micro"
          desired_size  = 1
          min_size      = 1
          max_size      = 2
          labels        = { role = "api", env = "dev" }
        }
      }
  EOT
  type = map(object({
    instance_type = string
    desired_size  = number
    min_size      = number
    max_size      = number
    labels        = map(string)
  }))
}

variable "tags" {
  description = "Tags applied to all resources in this module."
  type        = map(string)
  default     = {}
}

