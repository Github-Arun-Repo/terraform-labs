#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-jenkins}"
RELEASE_NAME="${RELEASE_NAME:-dynamic-jenkins}"
CHART_PATH="${CHART_PATH:-k8s/jenkins/dynamic-jenkins}"
VALUES_FILE="${VALUES_FILE:-k8s/jenkins/dynamic-jenkins/values.yaml}"

if [[ ! -d "${CHART_PATH}" ]]; then
  echo "Chart path not found: ${CHART_PATH}" >&2
  exit 1
fi

kubectl get namespace "${NAMESPACE}" >/dev/null 2>&1 || kubectl create namespace "${NAMESPACE}"

helm repo add jenkins https://charts.jenkins.io >/dev/null 2>&1 || true
helm repo update
helm dependency update "${CHART_PATH}"

helm upgrade --install "${RELEASE_NAME}" "${CHART_PATH}" -n "${NAMESPACE}" -f "${VALUES_FILE}"

echo "Jenkins deployed in namespace ${NAMESPACE}."
echo "Jenkins is configured for dynamic Kubernetes agents through JCasC and Kubernetes cloud templates."