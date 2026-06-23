# Document Management Service (Spring Boot)

This service provides a simple document management API for users, backed by the AWS RDS MySQL instance from your Terraform stack.

## Features

- Create users using an API (`POST /api/users`)
- Auto-generate username and password and return them in the response
- Store users in RDS (`app_users` table)
- Issue JWT token using generated credentials (`POST /api/auth/token`)
- Upload user-owned documents (`POST /api/documents`)
- Download user-owned documents (`GET /api/documents/{id}`)
- Store documents in RDS (`documents` table)
- Enforce upload size and document type limits

## Tech Stack

- Java 21
- Spring Boot 3
- Spring Web, Spring Security, Spring Data JPA
- Flyway (schema migration)
- MySQL Connector/J
- JWT (`io.jsonwebtoken`)

## Project Location

- `applications/document-management-service`

## Database Schema (created if not present)

Flyway migration file:
- `src/main/resources/db/migration/V1__create_users_and_documents.sql`

### `app_users`

- `id` BIGINT PK AUTO_INCREMENT
- `username` VARCHAR(64) UNIQUE NOT NULL
- `password_hash` VARCHAR(255) NOT NULL
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP

### `documents`

- `id` BIGINT PK AUTO_INCREMENT
- `user_id` BIGINT NOT NULL (FK to `app_users.id`)
- `original_filename` VARCHAR(255) NOT NULL
- `doc_type` VARCHAR(20) NOT NULL
- `content_type` VARCHAR(100) NOT NULL
- `file_size` BIGINT NOT NULL
- `content` LONGBLOB NOT NULL
- `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP

Indexes:
- `idx_documents_user_id`
- `idx_documents_doc_type`

## Limits and Doc Types

Configured in `src/main/resources/application.yml`:

- Max upload size: `10MB`
- Max request size: `10MB`
- Max file bytes: `10485760`
- Allowed doc types: `PDF, PNG, JPEG, JPG, DOCX, TXT`

## Configuration

Set these environment variables:

- `DB_URL` (example: `jdbc:mysql://<rds-endpoint>:3306/document_mgmt`)
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET` (at least 32 bytes; base64 is supported)

Default local values are provided in `application.yml` for development only.

## Run Locally

```bash
cd applications/document-management-service
mvn spring-boot:run
```

## API Endpoints

### 1) Create User

`POST /api/users`

Request body: none

Response example:

```json
{
  "userId": 1,
  "username": "user_ab12cd34",
  "password": "T!5mA8x2..."
}
```

### 2) Get Token

`POST /api/auth/token`

Request:

```json
{
  "username": "user_ab12cd34",
  "password": "T!5mA8x2..."
}
```

Response:

```json
{
  "token": "<jwt>",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600
}
```

### 3) Upload Document

`POST /api/documents` (requires `Authorization: Bearer <jwt>`)

Multipart fields:
- `file`: binary file
- `docType`: one of `PDF|PNG|JPEG|JPG|DOCX|TXT`

Response example:

```json
{
  "documentId": 10,
  "originalFilename": "invoice.pdf",
  "docType": "PDF",
  "size": 93412,
  "createdAt": "2026-06-22T10:20:30.000Z"
}
```

### 4) Download Document

`GET /api/documents/{documentId}` (requires token)

Only the owning user can fetch their document.

## Notes

- Passwords are never stored in plain text (BCrypt hash in DB).
- Generated password is shown once in create-user response; store it safely.
- Documents are currently stored in RDS as BLOB as requested.
