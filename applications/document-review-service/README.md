# document-review-service

Status: Implemented

## Purpose
`document-review-service` is the finance decision layer for processed documents. It serves review queues, allows manual correction, supports approve/reject actions, and exposes audit history.

## Responsibilities
1. Read review queue by status from DynamoDB.
2. Assemble review detail with extraction data and view URL context.
3. Apply manual extraction field corrections.
4. Record and expose approval/rejection outcomes.
5. Serve audit and latest decision history.

## API surface
1. `GET /api/review/queue`
2. `GET /api/review/{documentId}`
3. `PATCH /api/review/{documentId}/fields`
4. `POST /api/review/{documentId}/approve`
5. `POST /api/review/{documentId}/reject`
6. `GET /api/audit/{documentId}`
7. `GET /api/audit/{documentId}/decision`

Actuator:
1. `GET /actuator/health`
2. `GET /actuator/info`
3. `GET /actuator/prometheus` (ADMIN)

## Security model
JWT claims expected:
1. `sub`
2. `roles`

Role access configured:
1. `/api/review/**` -> `FINANCE_REVIEWER`, `FINANCE_APPROVER`, `ADMIN`
2. `/api/audit/**` -> `FINANCE_REVIEWER`, `FINANCE_APPROVER`, `ADMIN`

Signing mode in this module:
1. HS256 secret (`JWT_SECRET`) with issuer validation (`JWT_ISSUER`)

## Data model usage
Primary table: `DocumentInventory`

Reads and writes include:
1. document metadata
2. extraction latest item
3. audit event items
4. review queue index reads on `GSI2`

## Local run
1. `docker compose up --build`
2. service URL: `http://localhost:8084`

Optional helper scripts:
1. `./scripts/seed-review-document.sh doc-1001`
2. `./scripts/generate-jwt.sh`
3. `./scripts/demo-requests.sh`

## Build and tests
1. `mvn clean verify`

## Environment variables (selected)
1. `SERVER_PORT`
2. `AWS_REGION`
3. `AWS_ENDPOINT_OVERRIDE`
4. `DYNAMODB_DOCUMENT_TABLE_NAME`
5. `DYNAMODB_REVIEW_QUEUE_INDEX_NAME`
6. `S3_BUCKET_NAME`
7. `S3_VIEW_URL_EXPIRY_MINUTES`
8. `JWT_SECRET`
9. `JWT_ISSUER`
10. `AUDIT_READ_EVENTS_ENABLED`

## Notes
1. This service is the decision layer, not the extractor.
2. Queue pagination remains an MVP implementation and can be expanded.
