# Applications: Document Platform Service Layer

## Business Problem
Finance and operations teams receive high volumes of supplier documents (invoices, receipts, and related artifacts) and need a controlled path from upload to approval with traceability.

This application layer addresses five practical requirements:
1. Secure intake with role-aware access.
2. Durable storage of document metadata and workflow state.
3. Asynchronous processing for extraction and validation.
4. Human-in-the-loop review for exceptions and approvals.
5. Auditable status transitions across the lifecycle.

High-level flow:

```text
Supplier/Admin -> user-management-service -> JWT
Supplier/Admin -> document-api-service -> upload request + metadata (DynamoDB)
Client -> S3 (presigned upload URL)
S3 ObjectCreated -> SQS -> document-processing-service
document-processing-service -> extraction output (S3) + status updates (DynamoDB) + audit events
Finance roles -> document-review-service -> corrections/approve/reject + audit reads
```

## Microservices

| Service | Status | What it does now | Key APIs or contracts |
|---|---|---|---|
| user-management-service | Implemented | Issues and validates JWTs, manages users and roles, persists identity data in PostgreSQL with Flyway migrations. | `/api/v1/auth/*`, `/api/v1/users/*` |
| document-api-service | Implemented | Creates upload requests, stores document metadata in `DocumentInventory`, and returns S3 presigned URLs for upload/view. | `POST /api/v1/documents/upload-request`, `GET /api/v1/documents/*` |
| document-processing-service | Implemented (MVP) | Polls SQS for S3 events, validates source object, writes extraction artifacts to S3, updates DynamoDB lifecycle state, and writes audit events. | Internal processing endpoint + SQS poll loop |
| document-review-service | Implemented | Provides review queue, document detail, field correction, approve/reject decisions, and audit/decision endpoints with JWT role checks. | `/api/review/*`, `/api/audit/*` |
| document-processor | In progress / legacy path | Standalone multipart upload service to S3 with validation constraints; not integrated with the current SQS-driven processing flow in this repo. | `/api/invoices/upload`, `/api/invoices/constraints` |

### Service Documentation Index

| Service | Documentation |
|---|---|
| user-management-service | [user-management-service/README.md](user-management-service/README.md) |
| document-api-service | [document-api-service/README.md](document-api-service/README.md) |
| document-processing-service | [document-processing-service/README.md](document-processing-service/README.md) |
| document-review-service | [document-review-service/README.md](document-review-service/README.md) |
| document-processor | [document-processor/README.md](document-processor/README.md) |

## Database Strategy

| Data domain | Storage | Why |
|---|---|---|
| Identity, credentials, refresh token lifecycle | PostgreSQL (user-management-service) | Strong relational consistency and transactional operations are a good fit for auth data. |
| Document metadata, processing state, extraction, review, and audit timeline | DynamoDB `DocumentInventory` | Single-table access patterns support document-centric reads/writes at scale. |
| Document binaries and extraction artifacts | S3 | Cost-effective object storage for raw and processed files. |

Design rationale:
1. Keep auth data isolated from document workflow concerns.
2. Keep document workflow data close to event-driven processing and query patterns.
3. Keep binary payloads out of primary databases.

## DynamoDB Application Model

Current services use a single-table model centered on `DocumentInventory`.

Primary entities in use:
1. Document metadata item (`PK=DOCUMENT#{documentId}`, `SK=METADATA`)
2. Extraction latest item (`PK=DOCUMENT#{documentId}`, `SK=EXTRACTION#LATEST`)
3. Audit event items (`PK=DOCUMENT#{documentId}`, `SK=AUDIT#{timestamp}#{uuid}`)
4. Duplicate detection index item (`PK=DUPLICATE#{customerId}#{supplier}#{invoice}`, `SK=INDEX`)

Indexes used by the application code:
1. `GSI1` for S3 key lookup (`S3KEY#{objectKey}` in processing flow)
2. `GSI2` for review queue reads by customer/status (review and status-oriented queries)

Notes:
1. Duplicate detection uses conditional writes to prevent re-processing of same logical invoice key.
2. Processing start uses conditional status transitions and attempt counters for idempotency and stale-processing protection.

## Document Lifecycle

Observed lifecycle states from implemented services:
1. `UPLOAD_REQUESTED`
2. `UPLOADED`
3. `PROCESSING`
4. `PENDING_APPROVAL`
5. `MANUAL_REVIEW_REQUIRED`
6. `DUPLICATE_DETECTED`
7. `APPROVED`
8. `REJECTED`
9. `EXTRACTION_FAILED`
10. `FAILED`

Typical path:
1. API creates metadata and upload request.
2. File lands in S3.
3. Processing transitions to `PROCESSING`, writes extraction outputs.
4. Status moves to `PENDING_APPROVAL` or `MANUAL_REVIEW_REQUIRED`.
5. Review service applies corrections and decision (`APPROVED`/`REJECTED`) or escalates duplicate/failure outcomes.

## Local Development

Run each service from its folder.

1. user-management-service
   - `docker compose up --build`
   - service: `http://localhost:8081`

2. document-api-service
   - `docker compose up --build`
   - service: `http://localhost:8082`

3. document-processing-service
   - `docker compose up --build`
   - service: `http://localhost:8083`

4. document-review-service
   - `docker compose up --build`
   - service: `http://localhost:8084`

5. document-processor
   - no module-level docker-compose currently committed
   - run with Maven/Spring Boot from module folder

Build checks (module level):
1. `mvn clean verify`
2. generated artifacts under `target/` must remain untracked

## Common Environment Variables

These names appear across service configs.

| Variable | Purpose |
|---|---|
| `AWS_REGION` | AWS SDK region for DynamoDB/S3/SQS clients |
| `AWS_ENDPOINT_OVERRIDE` | LocalStack endpoint for local development |
| `S3_BUCKET_NAME` or `DOCUMENTS_S3_BUCKET` | Document storage bucket |
| `DYNAMODB_DOCUMENT_TABLE_NAME` | DynamoDB single-table name (`DocumentInventory`) |
| `JWT_ISSUER` | Expected JWT issuer |
| `JWT_SECRET` | HS256 shared secret used in current local/MVP setup |
| `SERVER_PORT` | Service HTTP port |
| `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` | Actuator endpoint exposure controls |

Service-specific examples remain in each module README and `application.yml`.

## Application Security

Current implementation highlights:
1. Stateless JWT-based auth at service boundaries.
2. Role-based access controls in API and review services.
3. User-management supports role assignment and JWT issuance/refresh flows.
4. File handling paths sanitize filenames and enforce content-type/size constraints.
5. Correlation ID and audit trails are present in review/processing paths.

Current maturity notes:
1. HS256 shared-secret mode is active in module configs by default.
2. RSA/JWKS style federation is not uniformly implemented across all services yet.

## Observability

Implemented observability surfaces:
1. Actuator health/info/prometheus endpoints are enabled across services.
2. Micrometer counters exist for processing and API events.
3. Review service logging includes correlation-id pattern support.

Examples of metrics in code:
1. `document_processing_success_total`
2. `document_processing_failed_total`
3. `document_processing_duplicate_detected_total`
4. `documents_upload_requests_total`

## API Overview

### user-management-service
1. `POST /api/v1/auth/register`
2. `POST /api/v1/auth/login`
3. `POST /api/v1/auth/refresh`
4. `POST /api/v1/auth/logout`
5. `POST /api/v1/auth/validate`
6. `GET /api/v1/auth/public-key`
7. `POST /api/v1/users` (ADMIN)
8. `PUT /api/v1/users/{userId}/roles` (ADMIN)
9. `GET /api/v1/users/me`

### document-api-service
1. `POST /api/v1/documents/upload-request`
2. `GET /api/v1/documents`
3. `GET /api/v1/documents/{documentId}`
4. `GET /api/v1/documents/{documentId}/view-url`

### document-processing-service
1. `POST /api/internal/processing/process`
2. SQS polling flow for S3 event messages

### document-review-service
1. `GET /api/review/queue`
2. `GET /api/review/{documentId}`
3. `PATCH /api/review/{documentId}/fields`
4. `POST /api/review/{documentId}/approve`
5. `POST /api/review/{documentId}/reject`
6. `GET /api/audit/{documentId}`
7. `GET /api/audit/{documentId}/decision`

### document-processor
1. `POST /api/invoices/upload`
2. `GET /api/invoices/constraints`

## How to Read This Folder

Recommended reading order:
1. Start with user-management-service for auth and role model.
2. Then document-api-service for intake and metadata creation.
3. Then document-processing-service for asynchronous orchestration and idempotency logic.
4. Then document-review-service for decision workflow and audit retrieval.
5. Finally read document-processor as legacy/in-progress module context.

If validating behavior end-to-end, trace one document by `documentId` through metadata, extraction item, audit events, and final decision.

## What This Application Layer Demonstrates

This folder demonstrates:
1. Real microservice boundary decisions (identity, intake, processing, review).
2. Event-driven document workflow with explicit state transitions.
3. Hybrid persistence strategy (PostgreSQL + DynamoDB + S3).
4. Security and authorization integrated into business APIs.
5. A pragmatic MVP where some paths are production-ready and some are explicitly still in progress.
