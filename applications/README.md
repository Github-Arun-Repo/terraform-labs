# Application Design — Document Platform Services

This folder contains the platform services used in the current stack:

- `applications/document-api-service`
- `applications/document-processing-service`
- `applications/document-processor`
- `applications/document-review-service`
- `applications/user-management-service`

## Service responsibilities

- `document-api-service`: intake APIs and metadata persistence.
- `document-processing-service`: event-driven processing orchestration.
- `document-processor`: extraction/processing worker runtime.
- `document-review-service`: review and status transitions.
- `user-management-service`: identity, authentication, and token flows.

## Local build commands

Run inside each service folder:

```bash
mvn clean verify
```

## Related guides

- Infra: `terraform/README.md`
- Runtime and deploy scripts: `k8s/README.md`
- CI/CD and GitOps: `cicd/README.md`
