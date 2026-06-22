# Author: Arunasalam Govindasamy

output "vpc_id" {
  description = "ID of the VPC."
  value       = aws_vpc.this.id
}

output "vpc_cidr" {
  description = "CIDR block of the VPC."
  value       = aws_vpc.this.cidr_block
}

output "internet_gateway_id" {
  description = "ID of the Internet Gateway."
  value       = aws_internet_gateway.this.id
}

# Public
output "public_subnet_ids" {
  description = "IDs of the public subnets (one per AZ)."
  value       = aws_subnet.public[*].id
}

output "public_route_table_id" {
  description = "ID of the shared public route table."
  value       = aws_route_table.public.id
}

# Private â€” App tier
output "private_app_subnet_ids" {
  description = "IDs of the private application-tier subnets (one per AZ)."
  value       = aws_subnet.private_app[*].id
}

output "private_app_route_table_ids" {
  description = "IDs of the private app-tier route tables."
  value       = aws_route_table.private_app[*].id
}

# Private â€” DB tier
output "private_db_subnet_ids" {
  description = "IDs of the private database-tier subnets (one per AZ)."
  value       = aws_subnet.private_db[*].id
}

output "private_db_route_table_ids" {
  description = "IDs of the private DB-tier route tables."
  value       = aws_route_table.private_db[*].id
}

output "db_subnet_group_name" {
  description = "Name of the RDS DB subnet group (empty string if not created)."
  value       = var.create_db_subnet_group ? aws_db_subnet_group.this[0].name : ""
}

# NAT
output "nat_gateway_ids" {
  description = "IDs of the NAT Gateways."
  value       = aws_nat_gateway.this[*].id
}

output "nat_gateway_public_ips" {
  description = "Public Elastic IPs attached to NAT Gateways."
  value       = aws_eip.nat[*].public_ip
}

