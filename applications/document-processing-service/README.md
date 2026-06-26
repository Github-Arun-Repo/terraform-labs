# document-processing-service

Status: Implemented (MVP)

## Purpose
`document-processing-service` is the asynchronous orchestration component. It reacts to uploaded document events, validates source files, performs extraction, writes processing artifacts, and advances lifecycle status in DynamoDB.

## Processing flow
1. Poll SQS queue for upload events.
2. Parse S3 notification payload.
3. Resolve document metadata by `S3KEY#{objectKey}` on `GSI1`.
4. Attempt conditional transition to `PROCESSING` with attempt counters.
5. Validate S3 object metadata/content constraints.
6. Extract invoice payload using configured extractor mode.
7. Write outputs to S3:
	- `.../processed/.../textract-raw-output.json`
	- `.../processed/.../normalized-output.json`
8. Persist extraction item (`EXTRACTION#LATEST`).
9. Write audit events (`AUDIT#{timestamp}#{uuid}`).
10. Transition to terminal business state.

## Extractor modes
1. `MOCK`
2. `AWS_TEXTRACT`

Current note:
1. `AWS_TEXTRACT` path in code is deterministic placeholder logic in this repository snapshot.

## Lifecycle outcomes handled
1. `PENDING_APPROVAL`
2. `MANUAL_REVIEW_REQUIRED`
3. `DUPLICATE_DETECTED`
4. `EXTRACTION_FAILED`
5. `FAILED`

## Idempotency and duplicate handling
1. Conditional start transition prevents unsafe concurrent processing.
2. Stale-processing timeout and max attempts are enforced.
3. Duplicate detection writes a conditional `DUPLICATE#{customerId}#{supplier}#{invoice}` index record.
4. If duplicate key exists, status transitions to `DUPLICATE_DETECTED`.

## API and runtime contracts
HTTP endpoint:
1. `POST /api/internal/processing/process`

Background contract:
1. SQS queue poll loop for S3 events (`document-ingestion-queue` in local defaults)

Actuator:
1. `GET /actuator/health`
2. `GET /actuator/info`
3. `GET /actuator/prometheus`

## Local run
1. `docker compose up --build`
2. service URL: `http://localhost:8083`

## Build and tests
1. `mvn clean verify`

## Environment variables (selected)
1. `SERVER_PORT`
2. `AWS_REGION`
3. `AWS_ENDPOINT_OVERRIDE`
4. `S3_BUCKET_NAME`
5. `DYNAMODB_DOCUMENT_TABLE_NAME`
6. `DYNAMODB_S3_KEY_INDEX_NAME`
7. `DOCUMENT_INGESTION_QUEUE_URL`
8. `MAX_PROCESSING_ATTEMPTS`
9. `PROCESSING_STALE_TIMEOUT_MINUTES`
10. `EXTRACTOR_MODE`

## Observability
Implemented counters include:
1. `document_processing_success_total`
2. `document_processing_failed_total`
3. `document_processing_duplicate_detected_total`

## Notes
1. This service owns processing transitions and extraction persistence, not final finance decisions.
2. Duplicate and retry behavior is implemented with DynamoDB conditional writes to reduce race conditions.
