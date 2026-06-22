# Author: Arunasalam Govindasamy

# Remote state backend - S3 + DynamoDB locking
#
# PREREQUISITES: run the bootstrap module first:
#   cd bootstrap && terraform init && terraform apply
#
# Then update the values below to match the bootstrap outputs and run:
#   terraform init (from the repo root)

terraform {
  backend "s3" {
    bucket         = "my-terraform-state-123456" # must match bootstrap state_bucket_name
    key            = "terraform-labs/terraform.tfstate"
    region         = "eu-west-1"                 # must match bootstrap aws_region
    encrypt        = true
    dynamodb_table = "terraform-state-locks"     # must match bootstrap dynamodb_table_name
  }
}

