# Author: Arunasalam Govindasamy

variable "aws_region" {
  description = "AWS region where the state bucket and lock table will be created."
  type        = string
  default     = "eu-west-1"
}

variable "state_bucket_name" {
  description = "Globally unique S3 bucket name for Terraform remote state. Must be lowercase, no underscores."
  type        = string
}

variable "dynamodb_table_name" {
  description = "DynamoDB table name used for Terraform state locking."
  type        = string
  default     = "terraform-state-locks"
}

