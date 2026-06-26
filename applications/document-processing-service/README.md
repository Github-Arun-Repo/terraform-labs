# document-processing-service

Status: Implemented

## Role in the platform

`document-processing-service` is the asynchronous processing worker between upload and finance review. It consumes document ingestion events, validates S3 objects, moves documents through processing statuses, extracts invoice data in `MOCK` or `TEXTRACT` mode, writes extraction and audit items, and leaves review-ready records in DynamoDB. In the wider workflow it starts after S3 upload and finishes before review; see [../README.md](../README.md) for the cross-service view.

## Internal architecture

Package: `com.documentplatform.documentprocessing`.

```mermaid
%%{init: {'theme':'default','flowchart':{'useMaxWidth':true,'htmlLabels':true}}}%%
flowchart TB
  SQS["SQS\ndocument-ingestion-queue"]:::data --> Poller["SqsPollingService\n@EnableScheduling"]:::runtime
  Internal["Internal caller"]:::edge --> Controller["ProcessingController\n/api/internal/processing"]:::runtime
  Poller --> Parser["S3EventParser"]:::runtime
  Parser --> Service["DocumentProcessingService"]:::runtime
  Controller --> Service
  Service --> Validator["S3ObjectValidator"]:::runtime
  Service --> Extractor["InvoiceExtractor\nMock or Textract"]:::runtime
  Service --> Documents["DynamoDocumentRepository"]:::runtime
  Service --> Extractions["DynamoExtractionRepository"]:::runtime
  Service --> Audit["DynamoAuditRepository"]:::runtime
  Validator --> S3["S3\ndocuments-inventory-s3"]:::data
  Extractor --> S3
  Extractor -. optional .-> Textract["AWS Textract"]:::data
  Documents --> DynamoDB["DynamoDB\nDocumentInventory"]:::data
  Extractions --> DynamoDB
  Audit --> DynamoDB
  Config["AwsProperties, AppProperties"]:::control -. configures .-> Service

  classDef edge fill:#EAF4FF,stroke:#1D4ED8,color:#0F172A,stroke-width:1.5px;
  classDef runtime fill:#EFFFF7,stroke:#059669,color:#052E2B,stroke-width:1.5px;
  classDef data fill:#FFF7ED,stroke:#EA580C,color:#431407,stroke-width:1.5px;
  classDef control fill:#F5ECFF,stroke:#7C3AED,color:#2E1065,stroke-width:1.5px;
  classDef legacy fill:#F8FAFC,stroke:#94A3B8,color:#475569,stroke-width:1px,stroke-dasharray: 5 5;
```

*The worker has two entry points: scheduled SQS polling for normal flow and an internal HTTP trigger for targeted processing.*

Core implementation classes include `SqsPollingService`, `S3EventParser`, `DocumentProcessingService`, `S3ObjectValidator`, `MockInvoiceExtractor`, `TextractInvoiceExtractor`, `DynamoDocumentRepository`, `DynamoExtractionRepository`, and `DynamoAuditRepository`.

## API contract

Base path: `/api/internal/processing`.

| Method | Path | Auth / role required | Request -> response |
|---|---|---|---|
| `POST` | `/api/internal/processing/process` | Internal; no Spring Security filter in this service | JSON map with `bucket` and `key` -> `{ "success": true/false }`. |
| `GET` | `/api/internal/processing/health` | Internal; no Spring Security filter in this service | none -> `{ "status": "UP" }`. |

## Data model

| Model | Storage | Notes |
|---|---|---|
| `DocumentItem` | DynamoDB table `DocumentInventory` | Metadata item keyed as `PK=DOCUMENT#{documentId}`, `SK=METADATA`; includes S3 location, status, attempts, processing timestamps, revision, and index keys. |
| `ExtractionItem` | Same DynamoDB table | Extraction output for the document, including structured invoice fields. |
| `AuditEventItem` | Same DynamoDB table | Status transition and processing audit trail. |
| `DocumentStatus` | Enum | Includes processing, review, approval, rejection, duplicate, extraction failure, and failed states. |
| `ExtractorMode` | Enum | `MOCK` or `TEXTRACT`. |

```mermaid
%%{init: {'theme':'default','flowchart':{'useMaxWidth':true,'htmlLabels':true}}}%%
sequenceDiagram
  autonumber
  participant SQS as SQS queue
  participant Poller as SqsPollingService
  participant Parser as S3EventParser
  participant Worker as DocumentProcessingService
  participant S3 as S3
  participant DDB as DocumentInventory

  Poller->>SQS: Long poll queue URL
  SQS-->>Poller: S3 event message
  Poller->>Parser: Parse direct or SQS-wrapped S3 notification
  Parser-->>Worker: bucket + key
  Worker->>DDB: Find document by S3 key index GSI1
  Worker->>DDB: Conditional transition to PROCESSING
  Worker->>S3: Validate source object and write outputs
  Worker->>DDB: Write EXTRACTION and AUDIT items
  Worker->>DDB: Set PENDING_APPROVAL, MANUAL_REVIEW_REQUIRED, DUPLICATE_DETECTED, or failure status
```

*The signature flow is queue-driven and guarded by conditional state transitions so duplicate or concurrent processors do not corrupt document state.*

```mermaid
%%{init: {'theme':'default','flowchart':{'useMaxWidth':true,'htmlLabels':true}}}%%
stateDiagram-v2
  [*] --> UPLOADED
  UPLOADED --> PROCESSING
  PROCESSING --> PENDING_APPROVAL
  PROCESSING --> MANUAL_REVIEW_REQUIRED
  PROCESSING --> DUPLICATE_DETECTED
  PROCESSING --> EXTRACTION_FAILED
  PROCESSING --> FAILED
```

*Processing owns the transition from uploaded object to review-ready or failure states.*

## Security

This service has no explicit Spring Security chain. Its HTTP API is internal by route convention, and the normal production path is SQS polling rather than public HTTP traffic. AWS access should be granted through the chart ServiceAccount IRSA annotation for DynamoDB, S3, SQS, and optional Textract calls.

## Configuration

| Property / env var | Default or source | Purpose |
|---|---|---|
| `SERVER_PORT` | `8083` | HTTP port. |
| `AWS_REGION` | `eu-central-1` | AWS SDK region. |
| `AWS_ENDPOINT_OVERRIDE` | empty | LocalStack or alternate AWS endpoint. |
| `S3_BUCKET_NAME` | `documents-inventory-s3` | Source and output bucket. |
| `DOCUMENT_INGESTION_QUEUE_URL` | `http://localhost:4566/000000000000/document-ingestion-queue` | SQS queue URL. |
| `DOCUMENT_INGESTION_SQS_WAIT_TIME_SECONDS` | `10` | Long-poll wait time. |
| `DOCUMENT_INGESTION_SQS_MAX_MESSAGES` | `5` | Max SQS messages per poll. |
| `DYNAMODB_DOCUMENT_TABLE_NAME` | `DocumentInventory` | Shared document table. |
| `DYNAMODB_S3_KEY_INDEX_NAME` | `GSI1` | S3-key lookup index. |
| `MAX_PROCESSING_ATTEMPTS` | `3` | Retry guard. |
| `PROCESSING_STALE_TIMEOUT_MINUTES` | `10` | Stale processing timeout. |
| `EXTRACTOR_MODE` | `MOCK` | Extractor implementation switch. |
| `MAX_FILE_SIZE_BYTES` | `20971520` | 20 MiB processing limit. |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://otel-collector.observability.svc.cluster.local:4318` | OTLP traces and metrics endpoint. |

## Testing

| Test class | Count | Coverage |
|---|---:|---|
| `DocumentProcessingServiceTest` | 8 | Processing outcomes, duplicate handling, failure paths, and repository interactions. |
| `S3ObjectValidatorTest` | 2 | S3 object validation rules. |
| `S3EventParserTest` | 1 | S3 event parsing. |

Total `@Test` methods: `11`.

## Run locally

| Command | Purpose |
|---|---|
| `mvn test` | Run the test suite. |
| `mvn clean package -DskipTests` | Build the service jar. |
| `mvn spring-boot:run` | Run directly from the module; requires LocalStack or AWS endpoint configuration. |
| `docker-compose up` | Start LocalStack on `4566` and the service on `8083`. |

Service URL: `http://localhost:8083`.