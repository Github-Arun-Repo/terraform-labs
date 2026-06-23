#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-dms}"
RELEASE_NAME="${RELEASE_NAME:-dms-alb}"
CHART_PATH="${CHART_PATH:-k8s/eks/document-management-alb}"
VALUES_FILE="${VALUES_FILE:-}"

if [[ ! -d "${CHART_PATH}" ]]; then
  echo "Chart path not found: ${CHART_PATH}" >&2
  exit 1
fi

if [[ -n "${VALUES_FILE}" ]]; then
  helm upgrade --install "${RELEASE_NAME}" "${CHART_PATH}" -n "${NAMESPACE}" -f "${VALUES_FILE}"
else
  helm upgrade --install "${RELEASE_NAME}" "${CHART_PATH}" -n "${NAMESPACE}"
fi

echo "ALB ingress chart deployed in namespace ${NAMESPACE}."