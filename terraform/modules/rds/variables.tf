# Author: Arunasalam Govindasamy

variable "identifier" {
  description = "Unique identifier for the RDS instance and related resources."
  type        = string
}

variable "vpc_id" {
  description = "ID of the VPC where the RDS instance will be deployed."
  type        = string
}

variable "db_subnet_group_name" {
  description = "Name of the DB subnet group (must cover private DB subnets)."
  type        = string
}

variable "app_security_group_id" {
  description = "ID of the application-tier Security Group. Only this SG is allowed to reach the DB port."
  type        = string
}

variable "engine" {
  description = "Database engine (mysql | postgres | mariadb)."
  type        = string
  default     = "mysql"
}

variable "engine_version" {
  description = "Engine version. Must be a version available on db.t3.micro for free-tier eligibility."
  type        = string
  default     = "8.0"
}

variable "db_parameter_group_family" {
  description = "Parameter group family matching the engine and version (e.g. mysql8.0, postgres16)."
  type        = string
  default     = "mysql8.0"
}

variable "db_port" {
  description = "Port the database listens on."
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

variable "manage_master_user_password" {
  description = "Use RDS-managed master user password in Secrets Manager when supported."
  type        = bool
  default     = true
}

variable "tags" {
  description = "Tags applied to every resource in this module."
  type        = map(string)
  default     = {}
}

