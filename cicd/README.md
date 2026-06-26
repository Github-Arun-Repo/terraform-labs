# CI/CD Platform — Jenkins + ArgoCD GitOps

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
    ├── document-api-service-application.yaml
    ├── document-processing-service-application.yaml
    ├── document-processor-application.yaml
    ├── document-review-service-application.yaml
    ├── grafana-application.yaml
    ├── prometheus-application.yaml
    └── user-management-service-application.yaml
```

## Delivery model

- Jenkins: build/test/image push and optional GitOps write-back.
- ArgoCD: watches manifests/charts in Git and reconciles cluster state.

## Typical commands

```bash
# Apply active ArgoCD Applications
kubectl apply -f cicd/argocd/document-processor-application.yaml
kubectl apply -f cicd/argocd/user-management-service-application.yaml
kubectl apply -f cicd/argocd/document-api-service-application.yaml
kubectl apply -f cicd/argocd/document-processing-service-application.yaml
kubectl apply -f cicd/argocd/document-review-service-application.yaml
```

## Security notes

- Jenkins build agents use IRSA instead of static AWS keys.
- ArgoCD performs cluster-side deployment reconciliation.
- Keep Git write credentials in Jenkins credential store only.
