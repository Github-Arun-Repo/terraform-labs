# Author: Arunasalam Govindasamy

variable "repository_name" {
  description = "Name of the ECR repository to create."
  type        = string
}

variable "image_tag_mutability" {
  description = "Whether image tags can be overwritten (MUTABLE or IMMUTABLE)."
  type        = string
  default     = "MUTABLE"
}

variable "image_scan_on_push" {
  description = "Enable vulnerability scanning when new images are pushed."
  type        = bool
  default     = true
}

variable "force_delete" {
  description = "Delete the repository even if it still contains images. Useful for non-production labs."
  type        = bool
  default     = false
}

variable "max_image_count" {
  description = "Maximum number of images to retain before lifecycle cleanup expires older entries."
  type        = number
  default     = 30
}

variable "tags" {
  description = "Tags applied to every resource in this module."
  type        = map(string)
  default     = {}
}