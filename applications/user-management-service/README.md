# user-management-service

Status: Implemented

## Purpose
`user-management-service` is the identity provider for this application layer. It manages users and roles, authenticates credentials, and issues/validates JWT tokens used by other services.

## Responsibilities
1. User registration and admin-driven user creation.
2. Login/logout and refresh token lifecycle.
3. Role management for platform authorization.
4. JWT validation and public-key exposure endpoint.

## Data model and storage
Primary store: PostgreSQL.

Schema (Flyway):
1. `users`
2. `user_roles`
3. `refresh_tokens`

Key migrations:
1. `V1__create_user_management_tables.sql`
2. `V2__seed_bootstrap_admin_user.sql`

## API surface
1. `POST /api/v1/auth/register`
2. `POST /api/v1/auth/login`
3. `POST /api/v1/auth/refresh`
4. `POST /api/v1/auth/logout`
5. `POST /api/v1/auth/validate`
6. `GET /api/v1/auth/public-key`
7. `POST /api/v1/users` (ADMIN)
8. `PUT /api/v1/users/{userId}/roles` (ADMIN)
9. `GET /api/v1/users/me`

## Roles
Implemented role enum values:
1. `ADMIN`
2. `FINANCE_REVIEWER`
3. `FINANCE_APPROVER`
4. `SUPPLIER`
5. `AUDITOR`

## Local run
1. Start with Docker Compose:
   - `docker compose up --build`
2. Service endpoint:
   - `http://localhost:8081`

## Build and test
1. `mvn clean verify`

## Environment variables (selected)
1. `SERVER_PORT`
2. `SPRING_DATASOURCE_URL`
3. `SPRING_DATASOURCE_USERNAME`
4. `SPRING_DATASOURCE_PASSWORD`
5. `JWT_ISSUER`
6. `JWT_SECRET`
7. `JWT_ACCESS_TOKEN_EXPIRY_MINUTES`
8. `JWT_REFRESH_TOKEN_EXPIRY_DAYS`

## Notes
1. Current configuration supports HS256 by default; RSA key paths are present in config for stronger setups.
2. Bootstrap admin seeding is controlled via Flyway placeholders.
