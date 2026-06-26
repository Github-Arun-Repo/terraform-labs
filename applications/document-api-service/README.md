# document-api-service

Status: Implemented

## Purpose
`document-api-service` is the document intake API. It creates upload requests, writes document metadata into DynamoDB, and returns S3 presigned URLs for upload and view operations.

It does not perform binary upload proxying, extraction, or finance decisions.

## Responsibilities
1. Validate intake request fields.
2. Build canonical S3 object keys for raw uploads.
3. Persist document metadata in `DocumentInventory`.
4. Provide list/detail/view URL endpoints for clients.

S3 key pattern in implementation:
1. `{document_type}/raw/{customer_id}/{document_id}/{filename}`

## API surface
1. `POST /api/v1/documents/upload-request`
2. `GET /api/v1/documents`
3. `GET /api/v1/documents/{documentId}`
4. `GET /api/v1/documents/{documentId}/view-url`

Actuator:
1. `GET /actuator/health`
2. `GET /actuator/info`
3. `GET /actuator/prometheus`

Swagger UI:
1. `http://localhost:8082/swagger-ui/index.html`

## Security and authorization
JWT required for `/api/v1/documents/**`.

Role rules in controller:
1. Upload request: `SUPPLIER`, `ADMIN`
2. List/detail/view URL: `ADMIN`, `FINANCE_REVIEWER`, `FINANCE_APPROVER`, `SUPPLIER`, `AUDITOR`

## Storage model
1. DynamoDB table: `DocumentInventory`
2. Index names configured via env vars: customer/review indexes (`GSI1`, `GSI2` defaults)
3. Raw file bytes are stored in S3, not in DynamoDB

## Local run
1. `docker compose up --build`
2. service URL: `http://localhost:8082`

Without compose:
1. start LocalStack
2. run `./scripts/create-localstack-resources.sh`
3. run `mvn clean spring-boot:run`

## Build and tests
1. `mvn clean verify`

## Environment variables (selected)
1. `SERVER_PORT`
2. `AWS_REGION`
3. `AWS_ENDPOINT_OVERRIDE`
4. `DYNAMODB_DOCUMENT_TABLE_NAME`
5. `DYNAMODB_CUSTOMER_INDEX_NAME`
6. `DYNAMODB_REVIEW_INDEX_NAME`
7. `S3_BUCKET_NAME`
8. `S3_UPLOAD_URL_EXPIRY_MINUTES`
9. `S3_VIEW_URL_EXPIRY_MINUTES`
10. `JWT_ISSUER`
11. `JWT_SECRET`

## Observability
Implemented metrics include:
1. `documents_upload_requests_total`
2. `documents_upload_requests_failed_total`
3. `documents_view_url_generated_total`
4. `documents_upload_request_duration_seconds`

## Notes
1. This service is the source of truth for intake metadata but not for final finance decisions.
2. Presigned URL expiry and CORS behavior are environment-configurable.
