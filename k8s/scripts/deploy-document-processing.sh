#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-document-processing}"
RELEASE_NAME="${RELEASE_NAME:-document-processing-service}"
CHART_PATH="${CHART_PATH:-k8s/eks/document-processing-service}"

kubectl get namespace "${NAMESPACE}" >/dev/null 2>&1 || kubectl create namespace "${NAMESPACE}"
helm upgrade --install "${RELEASE_NAME}" "${CHART_PATH}" -n "${NAMESPACE}"

echo "document-processing-service deployed in ${NAMESPACE}" 
