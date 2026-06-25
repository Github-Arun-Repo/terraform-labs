#!/usr/bin/env bash
set -euo pipefail

# Deploy Grafana into the EKS cluster with Prometheus datasource and
# pre-provisioned Kubernetes dashboards (node/CPU and pod health).
#
# Prerequisites:
#   1. Prometheus stack is already running (run deploy-prometheus.sh first).
#   2. The grafana-admin-credentials Secret exists in the target namespace:
#        kubectl create secret generic grafana-admin-credentials \
#          --from-literal=admin-user=admin \
#          --from-literal=admin-password=<your-password> \
#          -n monitoring
#
# Environment overrides:
#   NAMESPACE    – target namespace          (default: monitoring)
#   RELEASE_NAME – Helm release name         (default: grafana)
#   CHART_PATH   – path to wrapper chart     (default: k8s/grafana/grafana)
#   VALUES_FILE  – path to values override   (default: <CHART_PATH>/values.yaml)

NAMESPACE="${NAMESPACE:-monitoring}"
RELEASE_NAME="${RELEASE_NAME:-grafana}"
CHART_PATH="${CHART_PATH:-k8s/grafana/grafana}"
VALUES_FILE="${VALUES_FILE:-${CHART_PATH}/values.yaml}"

if [[ ! -d "${CHART_PATH}" ]]; then
  echo "Chart path not found: ${CHART_PATH}" >&2
  exit 1
fi

kubectl get namespace "${NAMESPACE}" >/dev/null 2>&1 || kubectl create namespace "${NAMESPACE}"

# Verify the admin credentials secret exists before deploying
if ! kubectl get secret grafana-admin-credentials -n "${NAMESPACE}" >/dev/null 2>&1; then
  echo "ERROR: Secret 'grafana-admin-credentials' not found in namespace ${NAMESPACE}." >&2
  echo "Create it first:" >&2
  echo "  kubectl create secret generic grafana-admin-credentials \\" >&2
  echo "    --from-literal=admin-user=admin \\" >&2
  echo "    --from-literal=admin-password=<your-password> \\" >&2
  echo "    -n ${NAMESPACE}" >&2
  exit 1
fi

helm repo add grafana https://grafana.github.io/helm-charts >/dev/null 2>&1 || true
helm repo update
helm dependency update "${CHART_PATH}"

helm upgrade --install "${RELEASE_NAME}" "${CHART_PATH}" \
  --namespace "${NAMESPACE}" \
  --values "${VALUES_FILE}" \
  --timeout 5m \
  --wait

echo "Grafana deployed in namespace ${NAMESPACE}."
echo "Access Grafana:  kubectl port-forward svc/${RELEASE_NAME} -n ${NAMESPACE} 3000:80"
echo "Admin password:  kubectl get secret grafana-admin-credentials -n ${NAMESPACE} -o jsonpath='{.data.admin-password}' | base64 -d"
