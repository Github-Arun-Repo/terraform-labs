# document-processing-service

Consumes S3 upload events from SQS, validates and extracts invoice metadata, writes processed JSON outputs to S3, and updates document state in DynamoDB.

## Main Flow

1. Poll `document-ingestion-queue`
2. Parse S3 event (`Records` AWS format or simplified local format)
3. Lookup document in DynamoDB by GSI1 `S3KEY#{key}`
4. Validate S3 object via HeadObject
5. Extract invoice fields in `MOCK` or `AWS_TEXTRACT` mode
6. Write `processed/.../textract-raw-output.json` and `processed/.../normalized-output.json`
7. Write extraction item and audit event in DynamoDB
8. Update status to `PENDING_APPROVAL` or `MANUAL_REVIEW_REQUIRED`

## Run Local

```bash
docker compose up --build
```

Endpoints:
- `GET /actuator/health`
- `GET /actuator/prometheus`
- `POST /api/internal/processing/process`
- `GET /swagger-ui/index.html`
