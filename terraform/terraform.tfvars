# Author: Arunasalam Govindasamy

aws_region = "eu-west-1"

vpc_name = "my-vpc"
vpc_cidr = "10.0.0.0/16"

# Three AZs - adjust to match your region
availability_zones = ["eu-west-1a", "eu-west-1b", "eu-west-1c"]

# One public subnet per AZ
public_subnet_cidrs = [
  "10.0.0.0/24",
  "10.0.1.0/24",
  "10.0.2.0/24",
]

# One private app subnet per AZ
private_app_subnet_cidrs = [
  "10.0.10.0/24",
  "10.0.11.0/24",
  "10.0.12.0/24",
]

# One private DB subnet per AZ
private_db_subnet_cidrs = [
  "10.0.20.0/24",
  "10.0.21.0/24",
  "10.0.22.0/24",
]

# NAT - one per AZ for full HA; set single_nat_gateway = true to save cost in non-prod
enable_nat_gateway   = true
single_nat_gateway   = false
db_subnet_enable_nat = false

create_db_subnet_group = true

default_tags = {
  Project     = "terraform-labs"
  Environment = "dev"
  ManagedBy   = "terraform"
}

# -- RDS (free tier) -----------------------------------------------------------

db_identifier             = "my-app-db"
db_engine                 = "mysql"
db_engine_version         = "8.0"
db_parameter_group_family = "mysql8.0"
db_port                   = 3306
db_name                   = "appdb"
db_username               = "dbadmin"
# db_password - supply via environment variable to avoid committing secrets:
#   export TF_VAR_db_password="YourStrongPassword123!"

# -- EKS -----------------------------------------------------------------------

eks_cluster_name    = "my-app-eks"
eks_cluster_version = "1.30"

# Three self-managed node groups - all t3.micro (free-tier eligible), 1 node each for labs.
# Labels are passed straight through to kubelet --node-labels on every node in the group.
eks_node_groups = {
  api = {
    instance_type = "t3.micro"
    desired_size  = 1
    min_size      = 1
    max_size      = 2
    labels = {
      role        = "api"
      tier        = "app"
      environment = "dev"
    }
  }
  worker = {
    instance_type = "t3.micro"
    desired_size  = 1
    min_size      = 1
    max_size      = 2
    labels = {
      role        = "worker"
      tier        = "app"
      environment = "dev"
    }
  }
  batch = {
    instance_type = "t3.micro"
    desired_size  = 1
    min_size      = 0
    max_size      = 2
    labels = {
      role        = "batch"
      tier        = "app"
      environment = "dev"
    }
  }
}

