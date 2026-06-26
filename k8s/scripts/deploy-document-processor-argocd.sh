#!/usr/bin/env bash
set -euo pipefail

MANIFEST_PATH="${MANIFEST_PATH:-cicd/argocd/root-app-of-apps.yaml}"

if [[ ! -f "${MANIFEST_PATH}" ]]; then
  echo "Manifest path not found: ${MANIFEST_PATH}" >&2
  exit 1
fi

kubectl apply -f "${MANIFEST_PATH}"

echo "ArgoCD root application applied from ${MANIFEST_PATH}."
