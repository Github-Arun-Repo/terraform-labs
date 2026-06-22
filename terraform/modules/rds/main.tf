# Author: Arunasalam Govindasamy

# -- RDS Security Group --------------------------------------------------------

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
  db_name  = var.db_name
  username = var.db_username
  password = var.db_password
  port     = var.db_port

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

