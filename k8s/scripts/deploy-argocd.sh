#!/usr/bin/env bash
# deploy-argocd.sh
# Installs or upgrades ArgoCD in the cluster using the wrapper Helm chart at
# k8s/argocd/argocd/.
#
# Usage:
#   ./k8s/scripts/deploy-argocd.sh
#
# After installation apply service Application manifests so ArgoCD starts
# managing the deployments.
#
# Retrieve the initial admin password:
#   kubectl get secret -n argocd argocd-initial-admin-secret \
#     -o jsonpath='{.data.password}' | base64 -d && echo

set -euo pipefail

NAMESPACE="argocd"
RELEASE_NAME="argocd"
CHART_DIR="k8s/argocd/argocd"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

echo "==> Creating namespace '${NAMESPACE}' (idempotent)"
kubectl get namespace "${NAMESPACE}" >/dev/null 2>&1 \
  || kubectl create namespace "${NAMESPACE}"

echo "==> Adding / updating the Argo Helm repository"
helm repo add argo https://argoproj.github.io/argo-helm 2>/dev/null || true
helm repo update argo

echo "==> Fetching chart dependencies"
helm dependency update "${CHART_DIR}"

echo "==> Installing / upgrading ArgoCD"
helm upgrade --install "${RELEASE_NAME}" "${CHART_DIR}" \
  --namespace "${NAMESPACE}" \
  --values "${CHART_DIR}/values.yaml" \
  --atomic \
  --timeout 10m \
  --wait

echo ""
echo "ArgoCD deployed in namespace '${NAMESPACE}'."
echo ""
echo "Access the UI:"
echo "  kubectl port-forward svc/argocd-server -n ${NAMESPACE} 8080:80"
echo "  then open http://localhost:8080"
echo ""
echo "Initial admin password:"
echo "  kubectl get secret -n ${NAMESPACE} argocd-initial-admin-secret \\"
echo "    -o jsonpath='{.data.password}' | base64 -d && echo"
echo ""
echo "Apply service Applications (example):"
echo "  kubectl apply -f cicd/argocd/document-api-service-application.yaml"
echo "  kubectl apply -f cicd/argocd/document-processor-application.yaml"
