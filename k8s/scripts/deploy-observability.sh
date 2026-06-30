#!/usr/bin/env bash
set -euo pipefail

# Deploy the tracing tier (Grafana Tempo + OpenTelemetry Collector) into the
# `observability` namespace. The collector is the OTLP endpoint every Spring Boot
# service targets (OTEL_EXPORTER_OTLP_ENDPOINT); it forwards traces to Tempo.
#
# Environment overrides:
#   NAMESPACE          – target namespace                (default: observability)
#   TEMPO_RELEASE      – Tempo Helm release name         (default: tempo)
#   COLLECTOR_RELEASE  – Collector Helm release name     (default: otel-collector)
#   TEMPO_CHART        – path to Tempo wrapper chart      (default: k8s/observability/tempo)
#   COLLECTOR_CHART    – path to Collector wrapper chart  (default: k8s/observability/otel-collector)

NAMESPACE="${NAMESPACE:-observability}"
TEMPO_RELEASE="${TEMPO_RELEASE:-tempo}"
COLLECTOR_RELEASE="${COLLECTOR_RELEASE:-otel-collector}"
TEMPO_CHART="${TEMPO_CHART:-k8s/observability/tempo}"
COLLECTOR_CHART="${COLLECTOR_CHART:-k8s/observability/otel-collector}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

for chart in "${TEMPO_CHART}" "${COLLECTOR_CHART}"; do
  if [[ ! -d "${chart}" ]]; then
    echo "Chart path not found: ${chart}" >&2
    exit 1
  fi
done

kubectl get namespace "${NAMESPACE}" >/dev/null 2>&1 || kubectl create namespace "${NAMESPACE}"

# Tempo first — the collector forwards traces to it.
helm upgrade --install "${TEMPO_RELEASE}" "${TEMPO_CHART}" \
  --namespace "${NAMESPACE}" \
  --values "${TEMPO_CHART}/values.yaml" \
  --timeout 5m \
  --wait

helm upgrade --install "${COLLECTOR_RELEASE}" "${COLLECTOR_CHART}" \
  --namespace "${NAMESPACE}" \
  --values "${COLLECTOR_CHART}/values.yaml" \
  --timeout 5m \
  --wait

echo "Observability tracing tier deployed in namespace ${NAMESPACE}."
echo "Tempo query API:  kubectl port-forward svc/${TEMPO_RELEASE} -n ${NAMESPACE} 3200:3200"
echo "Collector OTLP:   otel-collector.${NAMESPACE}.svc.cluster.local:4318 (http) / :4317 (grpc)"
