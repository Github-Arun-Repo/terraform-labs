# document-review-service

Finance review workflow service for invoice/receipt documents, backed by DynamoDB single-table patterns and secured with JWT role claims.

## Features

- Review queue listing by status (`PENDING_APPROVAL`, `MANUAL_REVIEW_REQUIRED`, `DUPLICATE_DETECTED`)
- Detailed review payload: document metadata + extraction + presigned S3 view URL
- Manual extraction field corrections with optimistic revision checks
- Approve/reject decisions with audit trail and latest decision endpoint
- Role-based access control for finance analysts/managers/admins
- OpenAPI docs and Prometheus metrics

## API Endpoints

- `GET /api/review/queue`
- `GET /api/review/{documentId}`
- `PATCH /api/review/{documentId}/fields`
- `POST /api/review/{documentId}/approve`
- `POST /api/review/{documentId}/reject`
- `GET /api/audit/{documentId}`
- `GET /api/audit/{documentId}/decision`

## Security

Expected JWT claims:

- `sub`: username/email
- `roles`: array with values like `FINANCE_ANALYST`, `FINANCE_MANAGER`, `ADMIN`
- token signed with HS256 using `JWT_SECRET`

## Run Locally

1. Start dependencies and service:

```bash
docker compose up --build
```

2. Seed sample review data:

```bash
./scripts/seed-review-document.sh doc-1001
```

3. Generate JWT token:

```bash
ROLE=FINANCE_ANALYST SECRET=this-is-a-long-enough-secret-for-hs256-signing SUBJECT=analyst@company.com ISSUER=document-platform ./scripts/generate-jwt.sh
```

4. Run demo requests:

```bash
TOKEN=<paste-token> ./scripts/demo-requests.sh
```

## Environment Variables

- `AWS_REGION` (default `eu-central-1`)
- `AWS_ENDPOINT_OVERRIDE` (default empty)
- `DYNAMODB_DOCUMENT_TABLE_NAME` (default `DocumentInventory`)
- `DYNAMODB_REVIEW_QUEUE_INDEX_NAME` (default `GSI2`)
- `S3_BUCKET_NAME` (default `documents-inventory-s3`)
- `S3_VIEW_URL_EXPIRY_MINUTES` (default `5`)
- `JWT_SECRET` (required in non-dev)
- `JWT_ISSUER` (default `document-platform`)
- `AUDIT_READ_EVENTS_ENABLED` (default `false`)

## Notes

- The service uses a pragmatic subset of the full design: queue and detail reads, correction and decision writes, audit/decision reads.
- Pagination token handling is intentionally simplified in this MVP and can be upgraded to carry full DynamoDB exclusive start keys.
