# Author: Arunasalam Govindasamy

variable "cluster_name" {
  description = "Name of the EKS cluster."
  type        = string
}

variable "cluster_version" {
  description = "Kubernetes version for the EKS cluster (e.g. '1.36')."
  type        = string
  default     = "1.36"
}

variable "authentication_mode" {
  description = "EKS authentication mode: CONFIG_MAP, API_AND_CONFIG_MAP, or API."
  type        = string
  default     = "API_AND_CONFIG_MAP"
}

variable "vpc_id" {
  description = "ID of the VPC."
  type        = string
}

variable "private_app_subnet_ids" {
  description = "IDs of the private app-tier subnets where nodes and the control plane ENIs are placed."
  type        = list(string)
}

variable "node_groups" {
  description = <<-EOT
    Map of EKS managed node group definitions.
    Each key becomes the node group name. Labels are applied at the node group level.
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

