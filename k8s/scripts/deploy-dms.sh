#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-dms}"
RELEASE_NAME="${RELEASE_NAME:-dms-app}"
CHART_PATH="${CHART_PATH:-k8s/eks/document-management-service}"
VALUES_FILE="${VALUES_FILE:-}"
IMAGE_REPOSITORY="${IMAGE_REPOSITORY:-}"
IMAGE_TAG="${IMAGE_TAG:-}"

if [[ ! -d "${CHART_PATH}" ]]; then
  echo "Chart path not found: ${CHART_PATH}" >&2
  exit 1
fi

kubectl get namespace "${NAMESPACE}" >/dev/null 2>&1 || kubectl create namespace "${NAMESPACE}"

if [[ -n "${VALUES_FILE}" ]]; then
  HELM_ARGS=(upgrade --install "${RELEASE_NAME}" "${CHART_PATH}" -n "${NAMESPACE}" -f "${VALUES_FILE}")
else
  HELM_ARGS=(upgrade --install "${RELEASE_NAME}" "${CHART_PATH}" -n "${NAMESPACE}")
fi

if [[ -n "${IMAGE_REPOSITORY}" ]]; then
  HELM_ARGS+=(--set "image.repository=${IMAGE_REPOSITORY}")
fi

if [[ -n "${IMAGE_TAG}" ]]; then
  HELM_ARGS+=(--set "image.tag=${IMAGE_TAG}")
fi

helm "${HELM_ARGS[@]}"

echo "Document Management Service deployed in namespace ${NAMESPACE}."