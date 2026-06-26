# Author: Arunasalam Govindasamy

# -- RDS Security Group --------------------------------------------------------

resource "random_password" "master" {
  length           = 24
  special          = true
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

resource "aws_secretsmanager_secret" "master" {
  count = var.manage_master_user_password ? 0 : 1

  name_prefix = "${var.identifier}-master-"
  description = "Fallback master password secret for ${var.identifier} when RDS-managed master secret is disabled."

  tags = merge(var.tags, {
    Name = "${var.identifier}-master-secret"
  })
}

resource "aws_secretsmanager_secret_version" "master" {
  count = var.manage_master_user_password ? 0 : 1

  secret_id = aws_secretsmanager_secret.master[0].id
  secret_string = jsonencode({
    username = var.db_username
    password = random_password.master.result
    engine   = var.engine
    dbname   = var.db_name
    port     = var.db_port
  })
}

resource "aws_security_group" "rds" {
  name_prefix = "${var.identifier}-rds-"
  description = "Allow inbound DB traffic from the app tier only"
  vpc_id      = var.vpc_id

  tags = merge(var.tags, {
    Name = "${var.identifier}-rds-sg"
  })

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_vpc_security_group_ingress_rule" "rds_from_app" {
  security_group_id            = aws_security_group.rds.id
  description                  = "Allow ${var.engine} traffic from app-tier SG"
  from_port                    = var.db_port
  to_port                      = var.db_port
  ip_protocol                  = "tcp"
  referenced_security_group_id = var.app_security_group_id

  tags = merge(var.tags, {
    Name = "${var.identifier}-rds-ingress-from-app"
  })
}

resource "aws_vpc_security_group_egress_rule" "rds_egress" {
  security_group_id = aws_security_group.rds.id
  description       = "Allow all outbound (for patch/update traffic)"
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"

  tags = merge(var.tags, {
    Name = "${var.identifier}-rds-egress"
  })
}

# -- RDS Parameter Group -------------------------------------------------------

resource "aws_db_parameter_group" "this" {
  name_prefix = "${var.identifier}-"
  family      = var.db_parameter_group_family
  description = "Parameter group for ${var.identifier}"

  tags = merge(var.tags, {
    Name = "${var.identifier}-pg"
  })

  lifecycle {
    create_before_destroy = true
  }
}

# -- RDS Instance (free-tier) --------------------------------------------------

resource "aws_db_instance" "this" {
  identifier = var.identifier

  # Engine
  engine         = var.engine
  engine_version = var.engine_version

  # Free-tier sizing
  instance_class        = "db.t3.micro"
  allocated_storage     = 20    # GiB - free tier allowance
  max_allocated_storage = 0     # disable auto-scaling to stay within free tier
  storage_type          = "gp2"
  storage_encrypted     = true

  # Database
  db_name                     = var.db_name
  username                    = var.db_username
  manage_master_user_password = var.manage_master_user_password
  password                    = var.manage_master_user_password ? null : random_password.master.result
  port                        = var.db_port

  # Network
  db_subnet_group_name   = var.db_subnet_group_name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false

  # Free-tier: single-AZ, no Multi-AZ
  multi_az = false

  # Maintenance
  parameter_group_name       = aws_db_parameter_group.this.name
  auto_minor_version_upgrade = true
  maintenance_window         = "Mon:04:00-Mon:05:00"

  # Backups - free tier includes automated backup
  backup_retention_period = 7
  backup_window           = "03:00-04:00"

  # Lab-friendly settings
  skip_final_snapshot = true
  deletion_protection = false

  tags = merge(var.tags, {
    Name = var.identifier
  })
}

