# Author: Arunasalam Govindasamy

# Remote state backend - S3 + DynamoDB locking (partial configuration)
#
# PREREQUISITES: run the bootstrap module first:
#   cd bootstrap && terraform init && terraform apply
#
# Environment-specific values (bucket, region, lock table) are supplied at init
# time so no account-specific identifiers are hardcoded here:
#   terraform init -backend-config=backend.hcl
#
# Copy backend.hcl.example to backend.hcl and fill in the bootstrap outputs.

terraform {
  backend "s3" {
    key     = "terraform-labs/terraform.tfstate"
    encrypt = true
  }
}

