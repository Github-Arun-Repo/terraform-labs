# ⚠️ Legacy — Frozen Jenkins Pipelines (Not the Active CI/CD)

> These Jenkinsfiles are a **frozen historical reference only**. They are **not**
> maintained and are **not** part of the active delivery path.

The active CI/CD system is **GitHub Actions** (`.github/workflows/`), which builds,
tests, scans (Trivy), generates SBOMs, signs images with cosign, and writes image
tags back to Git for **ArgoCD** to reconcile. See [cicd/README.md](../../README.md).

These files are retained to document the previous Jenkins-based pipeline shape for
migration/comparison purposes. Do not extend or wire them into a running Jenkins
without first revisiting whether they should exist at all.

| File | Former purpose |
|------|----------------|
| `document-api-service-ci.Jenkinsfile` | CI build/test/push for document-api-service |
| `document-processing-service-ci.Jenkinsfile` | CI build/test/push for document-processing-service |
| `document-processor-ci.Jenkinsfile` | CI build/test/push for document-processor |
| `document-review-service-ci.Jenkinsfile` | CI build/test/push for document-review-service |
| `user-management-service-ci.Jenkinsfile` | CI build/test/push for user-management-service |
