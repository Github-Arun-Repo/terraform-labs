#!/usr/bin/env sh
set -e

awslocal s3 mb s3://documents-inventory-s3 || true
awslocal sqs create-queue --queue-name document-ingestion-dlq || true
awslocal sqs create-queue --queue-name document-ingestion-queue || true

awslocal dynamodb create-table \
	--table-name DocumentInventory \
	--attribute-definitions \
		AttributeName=PK,AttributeType=S \
		AttributeName=SK,AttributeType=S \
		AttributeName=GSI1PK,AttributeType=S \
		AttributeName=GSI1SK,AttributeType=S \
		AttributeName=GSI2PK,AttributeType=S \
		AttributeName=GSI2SK,AttributeType=S \
	--key-schema \
		AttributeName=PK,KeyType=HASH \
		AttributeName=SK,KeyType=RANGE \
	--global-secondary-indexes \
		'[{"IndexName":"GSI1","KeySchema":[{"AttributeName":"GSI1PK","KeyType":"HASH"},{"AttributeName":"GSI1SK","KeyType":"RANGE"}],"Projection":{"ProjectionType":"ALL"},"ProvisionedThroughput":{"ReadCapacityUnits":5,"WriteCapacityUnits":5}},{"IndexName":"GSI2","KeySchema":[{"AttributeName":"GSI2PK","KeyType":"HASH"},{"AttributeName":"GSI2SK","KeyType":"RANGE"}],"Projection":{"ProjectionType":"ALL"},"ProvisionedThroughput":{"ReadCapacityUnits":5,"WriteCapacityUnits":5}}]' \
	--provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 || true
