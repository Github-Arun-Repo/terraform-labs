# Author: Arunasalam Govindasamy

variable "vpc_name" {
  description = "Name of the VPC."
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC (e.g. 10.0.0.0/16)."
  type        = string
}

variable "availability_zones" {
  description = "List of Availability Zones. Length drives the number of subnets created."
  type        = list(string)

  validation {
    condition     = length(var.availability_zones) > 0
    error_message = "At least one Availability Zone must be specified."
  }
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets - one entry per AZ."
  type        = list(string)

  validation {
    condition     = length(var.public_subnet_cidrs) == length(var.availability_zones)
    error_message = "public_subnet_cidrs must have the same number of entries as availability_zones."
  }
}

variable "private_app_subnet_cidrs" {
  description = "CIDR blocks for private application-tier subnets - one entry per AZ."
  type        = list(string)

  validation {
    condition     = length(var.private_app_subnet_cidrs) == length(var.availability_zones)
    error_message = "private_app_subnet_cidrs must have the same number of entries as availability_zones."
  }
}

variable "private_db_subnet_cidrs" {
  description = "CIDR blocks for private database-tier subnets - one entry per AZ."
  type        = list(string)

  validation {
    condition     = length(var.private_db_subnet_cidrs) == length(var.availability_zones)
    error_message = "private_db_subnet_cidrs must have the same number of entries as availability_zones."
  }
}

variable "enable_nat_gateway" {
  description = "Create NAT Gateways so private subnets can reach the internet."
  type        = bool
  default     = true
}

variable "single_nat_gateway" {
  description = "Use one shared NAT Gateway instead of one per AZ. Reduces cost; not recommended for production."
  type        = bool
  default     = false
}

variable "db_subnet_enable_nat" {
  description = "Allow DB subnets outbound internet access via NAT (e.g. for OS patching). Disabled by default for tighter isolation."
  type        = bool
  default     = false
}

variable "create_db_subnet_group" {
  description = "Create an RDS DB subnet group from the private DB subnets."
  type        = bool
  default     = true
}

variable "tags" {
  description = "Tags applied to every resource in this module."
  type        = map(string)
  default     = {}
}

