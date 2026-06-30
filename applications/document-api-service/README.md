# document-api-service

Status: Implemented

## Role in the platform

`document-api-service` is the authenticated intake and document metadata API. It creates the durable DynamoDB metadata record before upload, returns short-lived S3 presigned URLs, lists document records, and exposes view URLs after upload. In the platform workflow it sits after identity and before asynchronous processing; see [../README.md](../README.md) for the cross-service view.

## Internal architecture

Package: `com.documentplatform.documentapi`.

```mermaid
%%{init: {'theme':'default','flowchart':{'useMaxWidth':true,'htmlLabels':true}}}%%
flowchart TB
  Client["Authenticated client"]:::edge --> JwtFilter["JwtAuthenticationFilter"]:::control
  JwtFilter --> Controller["DocumentController\n/api/v1/documents"]:::runtime
  Controller --> Service["DocumentService\nvalidation, pagination, metadata"]:::runtime
  Service --> IdGen["DocumentIdGenerator"]:::runtime
  Service --> KeyBuilder["S3KeyBuilder"]:::runtime
  Service --> Repository["DynamoDbDocumentRepository"]:::runtime
  Service --> Presign["S3PresignedUrlService"]:::runtime
  Service --> Metrics["DocumentMetricsService"]:::runtime
  Repository --> DynamoDB["DynamoDB\nDocumentInventory"]:::data
  Presign --> S3["S3\ndocuments-inventory-s3"]:::data
  Config["AwsProperties, DocumentProperties, JwtProperties"]:::control -. configures .-> Service
  Correlation["CorrelationIdFilter"]:::control -. adds X-Correlation-ID .-> Controller

  classDef edge fill:#EAF4FF,stroke:#1D4ED8,color:#0F172A,stroke-width:1.5px;
  classDef runtime fill:#EFFFF7,stroke:#059669,color:#052E2B,stroke-width:1.5px;
  classDef data fill:#FFF7ED,stroke:#EA580C,color:#431407,stroke-width:1.5px;
  classDef control fill:#F5ECFF,stroke:#7C3AED,color:#2E1065,stroke-width:1.5px;
  classDef legacy fill:#F8FAFC,stroke:#94A3B8,color:#475569,stroke-width:1px,stroke-dasharray: 5 5;
```

*The API layer validates JWTs and request constraints before it writes DynamoDB metadata and delegates object transfer to S3 presigned URLs.*

Core implementation classes include `DocumentController`, `DocumentService`, `DynamoDbDocumentRepository`, `S3PresignedUrlService`, `S3KeyBuilder`, `FileNameSanitizer`, `JwtAuthenticationFilter`, and `SecurityConfig`.

## API contract

Base path: `/api/v1/documents`.

| Method | Path | Auth / role required | Request -> response |
|---|---|---|---|
| `POST` | `/api/v1/documents/upload-request` | JWT with `SUPPLIER` or `ADMIN` | `CreateUploadRequest` -> `CreateUploadResponse` with document ID, S3 key, presigned upload URL, status, and expiry. |
| `GET` | `/api/v1/documents` | JWT with `ADMIN`, `FINANCE_REVIEWER`, `FINANCE_APPROVER`, `SUPPLIER`, or `AUDITOR` | Query `customerId`, `status`, `documentType`, `page`, `size` -> `PagedDocumentResponse`. |
| `GET` | `/api/v1/documents/{documentId}` | Same roles as list | Path `documentId` -> `DocumentResponse`. |
| `GET` | `/api/v1/documents/{documentId}/view-url` | Same roles as list | Path `documentId` -> `ViewUrlResponse` with short-lived S3 URL. |

## Data model

| Model | Storage | Notes |
|---|---|---|
| `DocumentItem` | DynamoDB table `DocumentInventory` | Primary item `PK=DOCUMENT#{documentId}`, `SK=METADATA`; stores customer, document type, file metadata, bucket/key, status, uploader, processing attempts, revision, and timestamps. |
| `DocumentStatus` | Enum | `UPLOAD_REQUESTED`, `UPLOADED`, `PROCESSING`, `EXTRACTION_COMPLETED`, `PENDING_APPROVAL`, `MANUAL_REVIEW_REQUIRED`, `DUPLICATE_DETECTED`, `APPROVED`, `REJECTED`, `EXTRACTION_FAILED`, `FAILED`. |
| `DocumentType` | Enum | `INVOICE`, `RECEIPT`. |
| `GSI1` | DynamoDB customer index | Configured as `DYNAMODB_CUSTOMER_INDEX_NAME`. |
| `GSI2` | DynamoDB review index | Configured as `DYNAMODB_REVIEW_INDEX_NAME`. |

```mermaid
%%{init: {'theme':'default','flowchart':{'useMaxWidth':true,'htmlLabels':true}}}%%
sequenceDiagram
  autonumber
  participant Client as Supplier/Admin client
  participant API as DocumentController
  participant Service as DocumentService
  participant DDB as DocumentInventory
  participant S3 as S3 presigner

  Client->>API: POST /api/v1/documents/upload-request
  API->>Service: validate request and authenticated user
  Service->>Service: generate documentId and sanitized S3 key
  Service->>DDB: Put METADATA item with status UPLOAD_REQUESTED
  Service->>S3: Create presigned PUT URL
  S3-->>Service: uploadUrl with 10-minute expiry
  Service-->>API: CreateUploadResponse
  API-->>Client: documentId, bucketName, s3Key, uploadUrl
  Client->>S3: PUT file bytes directly
```

*The signature flow separates metadata ownership from byte transfer: the API owns the record, while S3 receives the file directly from the client.*

## Security

`SecurityConfig` requires authentication for `/api/v1/documents/**`, permits selected actuator/OpenAPI endpoints, uses a custom `JwtAuthenticationFilter`, and enables method-level role checks on controller methods. JWT configuration uses issuer `document-platform` by default and an HMAC secret from `JWT_SECRET`.

The Helm chart binds the pod to a ServiceAccount with an `eks.amazonaws.com/role-arn` annotation so AWS access should come from IRSA in EKS.

## Configuration

| Property / env var | Default or source | Purpose |
|---|---|---|
| `SERVER_PORT` | `8082` | HTTP port. |
| `AWS_REGION` | `eu-central-1` | AWS SDK region. |
| `AWS_ENDPOINT_OVERRIDE` | empty | LocalStack or alternate AWS endpoint. |
| `DYNAMODB_DOCUMENT_TABLE_NAME` | `DocumentInventory` | Document metadata table. |
| `DYNAMODB_CUSTOMER_INDEX_NAME` | `GSI1` | Customer query index. |
| `DYNAMODB_REVIEW_INDEX_NAME` | `GSI2` | Review/status query index. |
| `S3_BUCKET_NAME` | `documents-inventory-s3` | Document object bucket. |
| `S3_UPLOAD_URL_EXPIRY_MINUTES` | `10` | Presigned upload URL lifetime. |
| `S3_VIEW_URL_EXPIRY_MINUTES` | `5` | Presigned view URL lifetime. |
| `JWT_ISSUER` / `JWT_SECRET` | `document-platform` / `very-strong-secret-key-please-change` | JWT validation settings. |
| `document.max-file-size-bytes` | `20971520` | 20 MiB upload request limit. |
| `document.allowed-content-types` | PDF, PNG, JPEG, TIFF | MIME allowlist. |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://otel-collector.observability.svc.cluster.local:4318` | OTLP traces and metrics endpoint. |

## Testing

| Test class | Count | Coverage |
|---|---:|---|
| `DocumentApiIntegrationTest` | 6 | Upload request, listing, lookup, view URL, validation, and integration behavior with test containers. |
| `DocumentServiceValidationTest` | 1 | Service validation behavior. |
| `S3KeyBuilderTest` | 2 | S3 key construction and sanitization expectations. |
| `JwtTokenValidatorTest` | 2 | JWT validation behavior. |
| `FileNameSanitizerTest` | 2 | Filename cleanup and unsafe input handling. |

Total `@Test` methods: `13`.

## Run locally

| Command | Purpose |
|---|---|
| `mvn test` | Run unit and integration tests. |
| `mvn clean package -DskipTests` | Build the service jar. |
| `mvn spring-boot:run` | Run directly from the module. |
| `docker-compose up` | Start LocalStack on `4566` and the service on `8082`. |

Service URL: `http://localhost:8082`.