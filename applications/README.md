# Applications: Business Document Workflow Platform

## What Business Problem This Solves
Organizations with high invoice and receipt volumes need to move from manual email/spreadsheet handling to an auditable, secure, and scalable workflow.

This application layer enables:
1. Secure supplier/admin document intake.
2. Automatic extraction and validation pipeline.
3. Human finance review and decisioning.
4. Full traceability of status transitions and decisions.
5. Separation of identity, intake, processing, and review responsibilities.

## Business Capability Map

```mermaid
flowchart LR
   A[Identity and Access] --> B[Document Intake]
   B --> C[Asynchronous Processing]
   C --> D[Finance Review and Decision]
   D --> E[Audit and Compliance]

   A1[user-management-service]:::svc
   B1[document-api-service]:::svc
   C1[document-processing-service]:::svc
   D1[document-review-service]:::svc
   C2[document-processor legacy path]:::legacy

   A -.implemented by.-> A1
   B -.implemented by.-> B1
   C -.implemented by.-> C1
   C -.partially overlaps.-> C2
   D -.implemented by.-> D1

   classDef svc fill:#e8f5e9,stroke:#2e7d32,color:#1b5e20;
   classDef legacy fill:#fff3e0,stroke:#ef6c00,color:#bf360c;
```

## End-to-End Runtime Flow

```mermaid
sequenceDiagram
   participant Supplier as Supplier/Admin
   participant UMS as user-management-service
   participant API as document-api-service
   participant S3 as Amazon S3
   participant SQS as SQS
   participant DPS as document-processing-service
   participant DRS as document-review-service

   Supplier->>UMS: Login / token
   UMS-->>Supplier: JWT access + refresh
   Supplier->>API: POST upload-request
   API-->>Supplier: documentId + presigned upload URL
   Supplier->>S3: Upload file bytes
   S3->>SQS: ObjectCreated event
   SQS->>DPS: Poll and process
   DPS->>S3: Write processed artifacts
   DPS->>DRS: Persist reviewable state in DynamoDB
   Supplier->>DRS: Finance users review/approve/reject
```

## Microservices And Detailed Docs

| Service | Status | Primary role | Detailed documentation |
|---|---|---|---|
| user-management-service | Implemented | Authentication, authorization roles, token lifecycle | [user-management-service/README.md](user-management-service/README.md) |
| document-api-service | Implemented | Upload request APIs, metadata persistence, presigned URLs | [document-api-service/README.md](document-api-service/README.md) |
| document-processing-service | Implemented (MVP) | SQS-driven orchestration, extraction persistence, status transitions | [document-processing-service/README.md](document-processing-service/README.md) |
| document-review-service | Implemented | Review queue, corrections, approve/reject, audit APIs | [document-review-service/README.md](document-review-service/README.md) |
| document-processor | In progress / legacy | Standalone multipart upload service to S3 | [document-processor/README.md](document-processor/README.md) |

## Data Architecture

```mermaid
flowchart LR
   UMS[user-management-service] --> PG[(PostgreSQL)]
   API[document-api-service] --> DDB[(DynamoDB DocumentInventory)]
   DPS[document-processing-service] --> DDB
   DRS[document-review-service] --> DDB
   API --> S3[(S3 documents)]
   DPS --> S3
   DP[document-processor legacy] --> S3
```

Storage strategy:
1. PostgreSQL stores identity and refresh token records.
2. DynamoDB single-table design stores document workflow entities.
3. S3 stores binary uploads and processed artifact files.

## DocumentInventory Single-Table View

```mermaid
flowchart TD
   A[PK DOCUMENT#docId SK METADATA] --> B[PK DOCUMENT#docId SK EXTRACTION#LATEST]
   A --> C[PK DOCUMENT#docId SK AUDIT#timestamp#uuid]
   A --> D[PK DOCUMENT#docId SK DECISION#LATEST]
   E[PK DUPLICATE#customer#supplier#invoice SK INDEX]:::dup

   classDef dup fill:#ffebee,stroke:#c62828,color:#b71c1c;
```

Access patterns used by implemented code:
1. GSI1 lookup by `S3KEY#{objectKey}` for processing correlation.
2. GSI2 lookup by `CUSTOMER#{customerId}#STATUS#{status}` for review queues.

## Lifecycle States Used In Implementation

```mermaid
stateDiagram-v2
   [*] --> UPLOAD_REQUESTED
   UPLOAD_REQUESTED --> UPLOADED
   UPLOADED --> PROCESSING
   PROCESSING --> PENDING_APPROVAL
   PROCESSING --> MANUAL_REVIEW_REQUIRED
   PROCESSING --> DUPLICATE_DETECTED
   PROCESSING --> EXTRACTION_FAILED
   PROCESSING --> FAILED
   PENDING_APPROVAL --> APPROVED
   PENDING_APPROVAL --> REJECTED
   PENDING_APPROVAL --> MANUAL_REVIEW_REQUIRED
   MANUAL_REVIEW_REQUIRED --> APPROVED
   MANUAL_REVIEW_REQUIRED --> REJECTED
   DUPLICATE_DETECTED --> APPROVED
   DUPLICATE_DETECTED --> REJECTED
```

## API Ownership Summary

| Domain | Owner service | API base path |
|---|---|---|
| Identity/Auth | user-management-service | `/api/v1/auth`, `/api/v1/users` |
| Intake | document-api-service | `/api/v1/documents` |
| Internal processing trigger | document-processing-service | `/api/internal/processing` |
| Review and decisions | document-review-service | `/api/review`, `/api/audit` |
| Legacy upload | document-processor | `/api/invoices` |

## Local Development

Run in each module directory:
1. `mvn clean verify`
2. `docker compose up --build` where compose file exists

Default service ports:
1. 8081 user-management-service
2. 8082 document-api-service
3. 8083 document-processing-service
4. 8084 document-review-service
5. 8080 document-processor

## Read Order For Engineering Or Interview Review

1. Read [user-management-service/README.md](user-management-service/README.md) for auth and role model.
2. Read [document-api-service/README.md](document-api-service/README.md) for intake API contracts and metadata schema.
3. Read [document-processing-service/README.md](document-processing-service/README.md) for orchestration/idempotency and DynamoDB write patterns.
4. Read [document-review-service/README.md](document-review-service/README.md) for correction and decision workflows.
5. Read [document-processor/README.md](document-processor/README.md) for the legacy standalone upload path.
