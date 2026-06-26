# user-management-service

Status: Implemented

## Business Responsibility
This service provides centralized identity and token management for the platform.

It solves:
1. Secure login for all personas.
2. Role-based authorization claims used by other services.
3. Token lifecycle management (access + refresh).
4. Administrative user onboarding and role updates.

## API Specification

Base paths:
1. `/api/v1/auth`
2. `/api/v1/users`

### Authentication APIs

| Endpoint | Auth required | Request body | Response |
|---|---|---|---|
| `POST /api/v1/auth/register` | No | `email`, `fullName`, `password` | `UserResponse` |
| `POST /api/v1/auth/login` | No | `email`, `password` | `AuthResponse` |
| `POST /api/v1/auth/refresh` | No | `refreshToken` | `RefreshAccessTokenResponse` |
| `POST /api/v1/auth/logout` | Optional auth, optional `X-Refresh-Token` | none | empty body |
| `POST /api/v1/auth/validate` | No | `token` | `TokenValidationResponse` |
| `GET /api/v1/auth/public-key` | No | none | `PublicKeyResponse` |

Request/response highlights from implementation:
1. `register` enforces email format and password length 8-128.
2. `login` returns `accessToken`, `refreshToken`, `tokenType`, `expiresIn`.
3. `validate` returns `valid`, `userId`, `email`, `roles`, `expiresAtEpochSeconds`.
4. `public-key` indicates RSA or HMAC mode via `rsaModeEnabled` and `algorithm`.

### User Management APIs

| Endpoint | Auth required | Request body | Response |
|---|---|---|---|
| `POST /api/v1/users` | ADMIN | `email`, `fullName`, `password`, `roles` | `UserResponse` |
| `PUT /api/v1/users/{userId}/roles` | ADMIN | `roles` | `UserResponse` |
| `GET /api/v1/users/me` | Authenticated | none | `UserResponse` |

## Security And Token Behavior

Roles emitted in JWT claims:
1. `ADMIN`
2. `FINANCE_REVIEWER`
3. `FINANCE_APPROVER`
4. `SUPPLIER`
5. `AUDITOR`

Security behavior implemented:
1. Failed login attempts increment lockout counters.
2. Accounts are temporarily locked after max failed attempts.
3. Refresh tokens are stored hashed (SHA-256), not plaintext.
4. JWT signing uses RSA when key files are available, otherwise HS256 fallback.

## Database Model (PostgreSQL)

Flyway migrations define these core tables:
1. `users`
2. `user_roles`
3. `refresh_tokens`

```mermaid
erDiagram
   users ||--o{ user_roles : has
   users ||--o{ refresh_tokens : owns

   users {
      BIGSERIAL id PK
      VARCHAR email UNIQUE
      VARCHAR full_name
      VARCHAR password_hash
      VARCHAR status
      INT failed_login_attempts
      TIMESTAMPTZ locked_until
      TIMESTAMPTZ created_at
      TIMESTAMPTZ updated_at
   }

   user_roles {
      BIGINT user_id FK
      VARCHAR role
   }

   refresh_tokens {
      BIGSERIAL id PK
      BIGINT user_id FK
      VARCHAR token_hash UNIQUE
      TIMESTAMPTZ expires_at
      BOOLEAN revoked
      TIMESTAMPTZ created_at
      TIMESTAMPTZ revoked_at
   }
```

Indexing from migrations:
1. `idx_refresh_tokens_user_id`
2. `idx_refresh_tokens_expires_at`

Bootstrap:
1. `V2__seed_bootstrap_admin_user.sql` creates initial ADMIN user via Flyway placeholders.

## Local Run

1. `docker compose up --build`
2. service URL: `http://localhost:8081`

## Build And Test

1. `mvn clean verify`

## Environment Variables (Important)

1. `SPRING_DATASOURCE_URL`
2. `SPRING_DATASOURCE_USERNAME`
3. `SPRING_DATASOURCE_PASSWORD`
4. `JWT_ISSUER`
5. `JWT_SECRET`
6. `JWT_PRIVATE_KEY_PATH`
7. `JWT_PUBLIC_KEY_PATH`
8. `JWT_ACCESS_TOKEN_EXPIRY_MINUTES`
9. `JWT_REFRESH_TOKEN_EXPIRY_DAYS`
10. `MAX_LOGIN_ATTEMPTS`
11. `ACCOUNT_LOCK_MINUTES`
