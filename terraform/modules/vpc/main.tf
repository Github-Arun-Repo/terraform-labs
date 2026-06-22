# Author: Arunasalam Govindasamy

locals {
  az_count     = length(var.availability_zones)
  nat_gw_count = var.enable_nat_gateway ? (var.single_nat_gateway ? 1 : local.az_count) : 0
}

# -- VPC -----------------------------------------------------------------------

resource "aws_vpc" "this" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = merge(var.tags, {
    Name = var.vpc_name
  })
}

# -- Internet Gateway ----------------------------------------------------------

resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id

  tags = merge(var.tags, {
    Name = "${var.vpc_name}-igw"
  })
}

# -- Public Subnets (one per AZ) -----------------------------------------------

resource "aws_subnet" "public" {
  count = local.az_count

  vpc_id                  = aws_vpc.this.id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = true

  tags = merge(var.tags, {
    Name = "${var.vpc_name}-public-${var.availability_zones[count.index]}"
    Tier = "public"
  })
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }

  tags = merge(var.tags, {
    Name = "${var.vpc_name}-public-rt"
  })
}

resource "aws_route_table_association" "public" {
  count = local.az_count

  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# -- NAT Gateways --------------------------------------------------------------

resource "aws_eip" "nat" {
  count  = local.nat_gw_count
  domain = "vpc"

  tags = merge(var.tags, {
    Name = "${var.vpc_name}-nat-eip-${var.availability_zones[count.index]}"
  })

  depends_on = [aws_internet_gateway.this]
}

resource "aws_nat_gateway" "this" {
  count = local.nat_gw_count

  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index].id

  tags = merge(var.tags, {
    Name = "${var.vpc_name}-nat-${var.availability_zones[count.index]}"
  })

  depends_on = [aws_internet_gateway.this]
}

# -- Private App Subnets (one per AZ) -----------------------------------------

resource "aws_subnet" "private_app" {
  count = local.az_count

  vpc_id            = aws_vpc.this.id
  cidr_block        = var.private_app_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]

  tags = merge(var.tags, {
    Name = "${var.vpc_name}-private-app-${var.availability_zones[count.index]}"
    Tier = "private-app"
  })
}

resource "aws_route_table" "private_app" {
  count  = local.az_count
  vpc_id = aws_vpc.this.id

  dynamic "route" {
    for_each = var.enable_nat_gateway ? [1] : []
    content {
      cidr_block     = "0.0.0.0/0"
      nat_gateway_id = var.single_nat_gateway ? aws_nat_gateway.this[0].id : aws_nat_gateway.this[count.index].id
    }
  }

  tags = merge(var.tags, {
    Name = "${var.vpc_name}-private-app-rt-${var.availability_zones[count.index]}"
  })
}

resource "aws_route_table_association" "private_app" {
  count = local.az_count

  subnet_id      = aws_subnet.private_app[count.index].id
  route_table_id = aws_route_table.private_app[count.index].id
}

# -- Private DB Subnets (one per AZ) ------------------------------------------

resource "aws_subnet" "private_db" {
  count = local.az_count

  vpc_id            = aws_vpc.this.id
  cidr_block        = var.private_db_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]

  tags = merge(var.tags, {
    Name = "${var.vpc_name}-private-db-${var.availability_zones[count.index]}"
    Tier = "private-db"
  })
}

resource "aws_route_table" "private_db" {
  count  = local.az_count
  vpc_id = aws_vpc.this.id

  # DB subnets have no outbound internet route by default (isolated tier).
  # Set var.db_subnet_enable_nat = true to allow outbound NAT for patch/update traffic.
  dynamic "route" {
    for_each = var.db_subnet_enable_nat && var.enable_nat_gateway ? [1] : []
    content {
      cidr_block     = "0.0.0.0/0"
      nat_gateway_id = var.single_nat_gateway ? aws_nat_gateway.this[0].id : aws_nat_gateway.this[count.index].id
    }
  }

  tags = merge(var.tags, {
    Name = "${var.vpc_name}-private-db-rt-${var.availability_zones[count.index]}"
  })
}

resource "aws_route_table_association" "private_db" {
  count = local.az_count

  subnet_id      = aws_subnet.private_db[count.index].id
  route_table_id = aws_route_table.private_db[count.index].id
}

# -- DB Subnet Group (for RDS/Aurora) -----------------------------------------

resource "aws_db_subnet_group" "this" {
  count = var.create_db_subnet_group ? 1 : 0

  name        = "${var.vpc_name}-db-subnet-group"
  description = "DB subnet group for ${var.vpc_name}"
  subnet_ids  = aws_subnet.private_db[*].id

  tags = merge(var.tags, {
    Name = "${var.vpc_name}-db-subnet-group"
  })
}

