# document-processor

Status: In progress / legacy path

## Purpose
`document-processor` is a standalone invoice upload service that accepts multipart uploads and stores files directly in S3.

## Current implementation scope
Implemented endpoints:
1. `POST /api/invoices/upload`
2. `GET /api/invoices/constraints`

Implemented behavior:
1. Validates customer ID format.
2. Validates file extension, content type, and size.
3. Sanitizes filename.
4. Uploads binary directly to S3.
5. Returns upload metadata response.

## What is not implemented here
1. No SQS event orchestration logic.
2. No DynamoDB document lifecycle integration.
3. No finance review workflow integration.
4. No module-level docker-compose file in this folder.

## Local run
1. Run with Maven from this module:
   - `mvn clean spring-boot:run`
2. Default port from config: `8080`

## Build and tests
1. `mvn clean verify`

## Environment variables (selected)
1. `DOCUMENTS_S3_BUCKET`
2. `MAX_UPLOAD_FILE_SIZE_BYTES`
3. `ALLOWED_EXTENSIONS`
4. `ALLOWED_CONTENT_TYPES`
5. `MULTIPART_MAX_FILE_SIZE`
6. `MULTIPART_MAX_REQUEST_SIZE`

## Notes
1. This module overlaps with intake concerns already covered by `document-api-service`.
2. Treat this module as an in-progress or transitional path unless integrated intentionally into the main architecture.
