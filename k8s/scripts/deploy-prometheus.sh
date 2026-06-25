#!/usr/bin/env bash
set -euo pipefail

# Deploy the kube-prometheus-stack (Prometheus Operator, Prometheus, Alertmanager,
# Node Exporter, kube-state-metrics) into the EKS cluster.
#
# Environment overrides:
#   NAMESPACE    – target namespace          (default: monitoring)
#   RELEASE_NAME – Helm release name         (default: prometheus)
#   CHART_PATH   – path to wrapper chart     (default: k8s/prometheus/kube-prometheus-stack)
#   VALUES_FILE  – path to values override   (default: <CHART_PATH>/values.yaml)

NAMESPACE="${NAMESPACE:-monitoring}"
RELEASE_NAME="${RELEASE_NAME:-prometheus}"
CHART_PATH="${CHART_PATH:-k8s/prometheus/kube-prometheus-stack}"
VALUES_FILE="${VALUES_FILE:-${CHART_PATH}/values.yaml}"

if [[ ! -d "${CHART_PATH}" ]]; then
  echo "Chart path not found: ${CHART_PATH}" >&2
  exit 1
fi

kubectl get namespace "${NAMESPACE}" >/dev/null 2>&1 || kubectl create namespace "${NAMESPACE}"

helm repo add prometheus-community https://prometheus-community.github.io/helm-charts >/dev/null 2>&1 || true
helm repo update
helm dependency update "${CHART_PATH}"

helm upgrade --install "${RELEASE_NAME}" "${CHART_PATH}" \
  --namespace "${NAMESPACE}" \
  --values "${VALUES_FILE}" \
  --timeout 10m \
  --wait

echo "Prometheus stack deployed in namespace ${NAMESPACE}."
echo "Prometheus UI:    kubectl port-forward svc/${RELEASE_NAME}-kube-prometheus-stack-prometheus -n ${NAMESPACE} 9090:9090"
echo "Alertmanager UI:  kubectl port-forward svc/${RELEASE_NAME}-kube-prometheus-stack-alertmanager -n ${NAMESPACE} 9093:9093"
