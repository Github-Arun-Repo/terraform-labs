#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-user-management}"
RELEASE_NAME="${RELEASE_NAME:-user-management-service}"
CHART_PATH="${CHART_PATH:-k8s/eks/user-management-service}"

kubectl get namespace "${NAMESPACE}" >/dev/null 2>&1 || kubectl create namespace "${NAMESPACE}"
helm upgrade --install "${RELEASE_NAME}" "${CHART_PATH}" -n "${NAMESPACE}"

echo "user-management-service deployed in ${NAMESPACE}" 
