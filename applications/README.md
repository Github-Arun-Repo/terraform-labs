# Application Design — Document Management Platform

This folder contains the business application layer for Terraform Labs.

Current services:

- [applications/document-management-service](applications/document-management-service)
- [applications/document-processor](applications/document-processor)
- [applications/invoice-upload-service](applications/invoice-upload-service)

---

## 1. Executive View

The application is a stateless Spring Boot API that does four high-value tasks:

1. Provisions user credentials.
2. Issues JWT access tokens.
3. Accepts document uploads with size/type governance.
4. Serves user-owned document downloads.

This service is designed to run behind ALB on EKS while persisting data in MySQL RDS.

---

## 2. Architecture Overview

```mermaid
flowchart TB
	Client[API Consumer] --> API[Spring Boot API]

	subgraph Runtime[Service Runtime]
		API --> SEC[Security Filter Chain]
		SEC --> CTRL[Controllers]
		CTRL --> SRV[Services]
		SRV --> REPO[Repositories]
	end

	REPO --> DB[(MySQL RDS)]
	SRV --> JWT[JWT Service]
```

---

## 3. Layered Design

```mermaid
flowchart LR
	C1[AuthController]
	C2[UserController]
	C3[DocumentController]

	S1[AuthService]
	S2[UserService]
	S3[DocumentService]
	S4[CredentialGenerator]

	R1[AppUserRepository]
	R2[DocumentRepository]

	C1 --> S1
	C2 --> S2
	C3 --> S3

	S1 --> S2
	S1 --> JWT[JwtService]
	S2 --> S4
	S2 --> R1
	S3 --> S2
	S3 --> R2

	R1 --> DB[(app_users)]
	R2 --> DB2[(documents)]
```

---

## 4. Security Model

The API uses JWT-based stateless authentication.

- Public endpoints:
	- `POST /api/users`
	- `POST /api/auth/token`
	- `GET /actuator/health`
- All other endpoints require Bearer token.

Security path:

1. `JwtAuthenticationFilter` checks `Authorization` header.
2. Token is validated using `JwtService`.
3. Username from token becomes authenticated principal.
4. Service layer uses principal identity for ownership checks.

---

## 5. Domain Model

```mermaid
erDiagram
	APP_USERS ||--o{ DOCUMENTS : owns

	APP_USERS {
		bigint id PK
		varchar username UK
		varchar password_hash
		timestamp created_at
	}

	DOCUMENTS {
		bigint id PK
		bigint user_id FK
		varchar original_filename
		varchar doc_type
		varchar content_type
		bigint file_size
		blob content
		timestamp created_at
	}
```

Design notes:

- Usernames are generated uniquely.
- Passwords are stored as hashes, never plaintext.
- Document content is stored as BLOB in MySQL.
- Access control is owner-scoped during document download.

---

## 6. API Journey

```mermaid
sequenceDiagram
	participant U as User
	participant API as Service API
	participant DB as MySQL

	U->>API: POST /api/users
	API->>DB: Insert app_users
	API-->>U: username + generated password

	U->>API: POST /api/auth/token
	API->>DB: Verify user hash
	API-->>U: JWT token

	U->>API: POST /api/documents (Bearer)
	API->>DB: Insert documents row + blob
	API-->>U: document metadata

	U->>API: GET /api/documents/{id} (Bearer)
	API->>DB: Verify owner + fetch blob
	API-->>U: file download
```

---

## 7. Runtime Configuration

Primary runtime settings are in:

- `applications/document-management-service/src/main/resources/application.yml`

Important controls:

- DB endpoint/user/password via env vars.
- JWT secret and expiration.
- Upload limits (size + allowed types).

---

## 8. Quality and Extensibility

What is already in place:

- Clear layer boundaries (controller/service/repository).
- Consistent exception mapping (`GlobalExceptionHandler`).
- Unit-testable service classes.

How this can grow cleanly:

1. Add domain modules (for example `audit`, `retention`, `search`).
2. Externalize file storage from BLOB to S3 while preserving metadata in RDS.
3. Add role-based authorization for admin/reporting endpoints.
4. Introduce async workflows for heavy document processing.

---

## 9. Where to Go Next

- Infra and networking: [terraform/README.md](../terraform/README.md)
- Kubernetes runtime and Helm model: [k8s/README.md](../k8s/README.md)
- CI/CD and dynamic Jenkins agents: [jenkins/README.md](../jenkins/README.md)
