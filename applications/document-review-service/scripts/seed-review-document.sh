#!/usr/bin/env bash
set -euo pipefail

DOCUMENT_ID="${1:-doc-1001}"
NOW="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

awslocal dynamodb put-item \
  --table-name DocumentInventory \
  --item "{
    \"PK\": {\"S\": \"DOCUMENT#${DOCUMENT_ID}\"},
    \"SK\": {\"S\": \"METADATA\"},
    \"entityType\": {\"S\": \"DOCUMENT\"},
    \"documentId\": {\"S\": \"${DOCUMENT_ID}\"},
    \"customerId\": {\"S\": \"customer-1\"},
    \"documentType\": {\"S\": \"INVOICE\"},
    \"fileName\": {\"S\": \"invoice-1001.pdf\"},
    \"contentType\": {\"S\": \"application/pdf\"},
    \"fileSize\": {\"N\": \"102400\"},
    \"bucketName\": {\"S\": \"documents-inventory-s3\"},
    \"s3Key\": {\"S\": \"invoice/raw/invoice-1001.pdf\"},
    \"status\": {\"S\": \"PENDING_APPROVAL\"},
    \"uploadedBy\": {\"S\": \"uploader@company.com\"},
    \"processingAttempts\": {\"N\": \"1\"},
    \"documentRevision\": {\"N\": \"1\"},
    \"createdAt\": {\"S\": \"${NOW}\"},
    \"updatedAt\": {\"S\": \"${NOW}\"},
    \"GSI1PK\": {\"S\": \"CUSTOMER#customer-1\"},
    \"GSI1SK\": {\"S\": \"DOCUMENT#${DOCUMENT_ID}\"},
    \"GSI2PK\": {\"S\": \"REVIEW#PENDING_APPROVAL\"},
    \"GSI2SK\": {\"S\": \"${NOW}#${DOCUMENT_ID}\"}
  }"

awslocal dynamodb put-item \
  --table-name DocumentInventory \
  --item "{
    \"PK\": {\"S\": \"DOCUMENT#${DOCUMENT_ID}\"},
    \"SK\": {\"S\": \"EXTRACTION#LATEST\"},
    \"entityType\": {\"S\": \"EXTRACTION\"},
    \"documentId\": {\"S\": \"${DOCUMENT_ID}\"},
    \"invoiceNumber\": {\"S\": \"INV-1001\"},
    \"supplierName\": {\"S\": \"Acme GmbH\"},
    \"invoiceDate\": {\"S\": \"2026-01-01\"},
    \"currency\": {\"S\": \"EUR\"},
    \"totalAmount\": {\"N\": \"1200.50\"},
    \"confidenceScore\": {\"N\": \"0.94\"},
    \"createdAt\": {\"S\": \"${NOW}\"},
    \"updatedAt\": {\"S\": \"${NOW}\"}
  }"

echo "Seeded ${DOCUMENT_ID}"
