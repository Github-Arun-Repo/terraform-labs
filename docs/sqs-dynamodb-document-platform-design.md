# Design Document: SQS Event Processing with DynamoDB for Document Platform

## Scope
This design covers ingestion and extraction state up to ready-for-review status. It intentionally stops before finance review and approval APIs.

## Service boundaries
- user-management-service: PostgreSQL for users, login, JWT, refresh tokens, roles.
- document-api-service: DynamoDB for document metadata and S3 upload/view URL orchestration.
- document-processing-service: DynamoDB for processing state, extraction results, audit, duplicate guard.

## Event flow
Client uploads document via pre-signed URL:

1. document-api-service creates metadata item in DynamoDB with status UPLOAD_REQUESTED.
2. Client uploads object to S3 raw prefix.
3. S3 ObjectCreated event is sent to document-ingestion-queue.
4. document-processing-service consumes SQS message.
5. Service resolves document via DynamoDB GSI1 (S3 key lookup), validates idempotency, extracts fields.
6. Service stores raw and normalized extraction JSON in S3 processed path.
7. Service writes extraction and audit items to DynamoDB.
8. Service sets final state: PENDING_APPROVAL, MANUAL_REVIEW_REQUIRED, DUPLICATE_DETECTED, EXTRACTION_FAILED, or FAILED.

## DynamoDB table design
Table: DocumentInventory

Keys:
- PK (string)
- SK (string)

GSI1:
- GSI1PK
- GSI1SK
- Use for lookup by S3 key

Optional GSI2:
- GSI2PK
- GSI2SK
- Use for global status dashboards

Entity patterns:
- DOCUMENT: PK=CUSTOMER#{customerId}, SK=DOCUMENT#{documentId}
- EXTRACTION: PK=DOCUMENT#{documentId}, SK=EXTRACTION#LATEST
- AUDIT: PK=DOCUMENT#{documentId}, SK=AUDIT#{timestamp}#{eventId}
- DUPLICATE_INDEX: PK=DUPLICATE#{customerId}#{supplierNorm}#{invoiceNorm}, SK=DOCUMENT#{documentId}

## S3 and SQS routing rules
S3 bucket: documents-inventory-s3

Trigger only:
- invoice/raw/
- receipt/raw/

Do not trigger:
- invoice/processed/
- invoice/failed/
- receipt/processed/
- receipt/failed/

Queues:
- document-ingestion-queue
- document-ingestion-dlq

## Idempotency strategy
- Skip if document status already in terminal states.
- Conditional status update to PROCESSING only when prior state is UPLOAD_REQUESTED or UPLOADED.
- Increment processingAttempts.
- Respect MAX_PROCESSING_ATTEMPTS, then set FAILED.
- Delete SQS message only after successful handling or safe skip.

## Extractor modes
- MOCK (default): deterministic sample invoice payload for local dev.
- AWS_TEXTRACT: AnalyzeExpense for real extraction.

## Current repository implementation delta
The Terraform stack now includes:
- SQS ingestion queue and DLQ
- SQS queue policy scoped to existing S3 bucket and account
- S3 module notification support with prefix filters
- DynamoDB DocumentInventory table with GSI1 and optional GSI2
- IRSA policy for document-api-service (DynamoDB + S3 raw paths)
- IRSA policy for document-processing-service (SQS + S3 + DynamoDB + optional Textract)

Application migration notes:
- document-api-service currently uses relational metadata and should be migrated to DynamoDB single-table writes with PK/SK + GSI1 attributes.
- document-processing-service is not yet present in this repository and should be added using SQS polling and DynamoDB state transitions per this design.

## Interview explanation
I intentionally use different databases based on access patterns. User management is relational and transactional, so PostgreSQL is a better fit for users, roles, refresh tokens, and strict constraints. Document ingestion and processing are event-driven, high-write, and state-transition heavy, so DynamoDB is a better fit for low-latency key-based access, idempotent conditional updates, and scalable stream-oriented workflows.
