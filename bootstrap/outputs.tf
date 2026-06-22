# Author: Arunasalam Govindasamy

output "state_bucket_name" {
  description = "S3 bucket name â€” use this in the root backend.tf."
  value       = aws_s3_bucket.terraform_state.bucket
}

output "dynamodb_table_name" {
  description = "DynamoDB table name â€” use this in the root backend.tf."
  value       = aws_dynamodb_table.terraform_locks.name
}

output "state_bucket_arn" {
  description = "ARN of the Terraform state S3 bucket."
  value       = aws_s3_bucket.terraform_state.arn
}

