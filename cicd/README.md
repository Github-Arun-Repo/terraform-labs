# CI/CD Platform — GitHub Actions + ArgoCD GitOps

This folder contains CI pipelines and ArgoCD Application manifests for active services.

## Folder structure

```text
cicd/
├── jenkins/
│   ├── document-api-service-ci.Jenkinsfile
│   ├── document-processing-service-ci.Jenkinsfile
│   ├── document-processor-ci.Jenkinsfile
│   ├── document-review-service-ci.Jenkinsfile
│   └── user-management-service-ci.Jenkinsfile
└── argocd/
    ├── root-app-of-apps.yaml
    ├── eks-services-applicationset.yaml
    ├── grafana-application.yaml
    ├── karpenter-application.yaml
    ├── kyverno-application.yaml
    ├── kyverno-policies-application.yaml
    ├── prometheus-application.yaml
    └── ...
```

## Delivery model

- GitHub Actions: build/test/image push, signing, scanning, and Terraform PR automation.
- ArgoCD: watches manifests/charts in Git and reconciles cluster state.

## Security Scanning

- Trivy container image scanning runs on every build after the Docker image push stage.
- CRITICAL severity vulnerabilities fail the pipeline.
- HIGH severity vulnerabilities are logged as warnings, but the build is allowed to proceed.
- No local Trivy installation is required; scanning runs using the `aquasec/trivy` Docker image.

## Typical commands

```bash
# Bootstrap GitOps from a single root Application
kubectl apply -f cicd/argocd/root-app-of-apps.yaml
```

## Security notes

- Jenkins build agents use IRSA instead of static AWS keys.
- ArgoCD performs cluster-side deployment reconciliation.
- ArgoCD Image Updater is the preferred long-term write-back mechanism for image tags.
- Keep Git write credentials in Jenkins credential store only.

## Cosign keyless verification (GitHub OIDC)

Images pushed by GitHub Actions are signed keylessly with Sigstore cosign.

Use the command below to verify a published image digest:

```bash
cosign verify \
    --certificate-identity-regexp "^https://github.com/.*/terraform-labs/.github/workflows/ci-services.yml@refs/heads/main$" \
    --certificate-oidc-issuer "https://token.actions.githubusercontent.com" \
    <aws_account_id>.dkr.ecr.<region>.amazonaws.com/<repository>@sha256:<digest>
```

Notes:

- Replace `<aws_account_id>`, `<region>`, `<repository>`, and `<digest>` with real values.
- Verification must target an image digest (`@sha256:...`), not a tag.
