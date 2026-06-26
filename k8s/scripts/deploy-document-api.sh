#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-document-api}"
RELEASE_NAME="${RELEASE_NAME:-document-api-service}"
CHART_PATH="${CHART_PATH:-k8s/eks/document-api-service}"

kubectl get namespace "${NAMESPACE}" >/dev/null 2>&1 || kubectl create namespace "${NAMESPACE}"
helm upgrade --install "${RELEASE_NAME}" "${CHART_PATH}" -n "${NAMESPACE}"

echo "document-api-service deployed in ${NAMESPACE}" 
