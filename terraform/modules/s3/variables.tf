# Author: Arunasalam Govindasamy

variable "bucket_name" {
  description = "Name of the S3 bucket to create."
  type        = string
}

variable "force_destroy" {
  description = "Allow bucket deletion even when objects exist. Keep false for non-disposable environments."
  type        = bool
  default     = false
}

variable "enable_versioning" {
  description = "Enable bucket versioning."
  type        = bool
  default     = true
}

variable "kms_alias_name" {
  description = "Alias for the KMS key used to encrypt S3 objects (must start with alias/)."
  type        = string
}

variable "kms_key_description" {
  description = "Description for the KMS key used by the bucket."
  type        = string
  default     = "KMS key for S3 bucket encryption"
}

variable "tags" {
  description = "Tags applied to every resource in this module."
  type        = map(string)
  default     = {}
}
