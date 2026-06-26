# document-api-service

## Purpose
`document-api-service` is the document intake microservice in the platform. It validates JWT access tokens, stores document metadata in DynamoDB `DocumentInventory`, and returns Amazon S3 pre-signed URLs for upload and view/download.

It does **not** upload file bytes through backend APIs, does not run Textract, and does not implement approval flows.

## Architecture

Client / Frontend
      |
      | JWT token
      v
document-api-service
      |
      | metadata
      v
DynamoDB (DocumentInventory)
      |
      | pre-signed upload URL
      v
Amazon S3 / LocalStack S3

## Core API list
- `POST /api/v1/documents/upload-request`
- `GET /api/v1/documents`
- `GET /api/v1/documents/{documentId}`
- `GET /api/v1/documents/{documentId}/view-url`
- `GET /actuator/health`
- `GET /actuator/prometheus`

Swagger UI:
- `http://localhost:8082/swagger-ui/index.html`

## Security model
- JWT Bearer token required for `/api/v1/documents/**`.
- JWT validation checks signature, issuer, and expiry.
- Role restrictions:
  - Upload request: `SUPPLIER`, `ADMIN`
  - List/get/view-url: `ADMIN`, `FINANCE_REVIEWER`, `FINANCE_APPROVER`, `SUPPLIER`, `AUDITOR`

Important decisions:
- File bytes are never posted to backend APIs.
- Client uploads directly to S3 using pre-signed PUT URL.
- Upload URL expiry: 10 minutes.
- View URL expiry: 5 minutes.
- Metadata only stored in DynamoDB.
- File only stored in S3.
- Filename is sanitized and path traversal is rejected.
- JWT and pre-signed URLs are never logged.

## Environment variables
See `.env.example` for the full list.

Main variables:
- `SERVER_PORT=8082`
- `S3_BUCKET_NAME=documents-inventory-s3`
- `S3_UPLOAD_URL_EXPIRY_MINUTES=10`
- `S3_VIEW_URL_EXPIRY_MINUTES=5`
- `JWT_ISSUER=document-platform`
- `JWT_SECRET=change-this-secret`
- `AWS_ENDPOINT_OVERRIDE=http://localhost:4566` (for local only)

## Run locally (without Docker Compose)
1. Start LocalStack manually.
2. Create S3/SQS/DynamoDB resources in LocalStack:
   - `./scripts/create-localstack-resources.sh`
3. Build and run:
   - `mvn clean spring-boot:run`

## Run with Docker Compose
1. `docker compose up --build`
2. Service becomes available at `http://localhost:8082`.
3. Swagger UI at `http://localhost:8082/swagger-ui/index.html`.

## Testing
- Unit + integration tests:
  - `mvn test`

Integration tests use service-level mocks and LocalStack-compatible patterns.

## curl examples

### Upload Request
```bash
curl -X POST http://localhost:8082/api/v1/documents/upload-request \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-1001",
    "documentType": "INVOICE",
    "fileName": "invoice-1001.pdf",
    "contentType": "application/pdf",
    "fileSize": 1048576
  }'
```

### List Documents
```bash
curl -X GET "http://localhost:8082/api/v1/documents?customerId=customer-1001&page=0&size=20" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

### Get Document
```bash
curl -X GET http://localhost:8082/api/v1/documents/<DOCUMENT_ID> \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

### Generate View URL
```bash
curl -X GET http://localhost:8082/api/v1/documents/<DOCUMENT_ID>/view-url \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

## Health and metrics
- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/prometheus`

Custom metrics:
- `documents_upload_requests_total`
- `documents_upload_requests_failed_total`
- `documents_view_url_generated_total`
- `documents_upload_request_duration_seconds`

## Future improvements
- RSA/JWKS token verification from user-management-service.
- Signed URL audit trail and object upload completion callback.
- Stronger role-to-resource checks (e.g., customer-level authorization).
- Add LocalStack bucket policy and CORS bootstrap automation.
