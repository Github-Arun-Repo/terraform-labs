#!/usr/bin/env bash
set -euo pipefail

# wire-platform-values.sh
# -----------------------------------------------------------------------------
# Populates the GitOps Helm values placeholders with the real values produced by
# `terraform apply`. The CI/GitOps loop only rewrites image tags, so account-id,
# IRSA ARNs, ECR repositories, region, datastore endpoints, the SQS queue URL and
# the Karpenter cluster identity are wired here from Terraform outputs.
#
# Workflow:
#   1. cd terraform && terraform apply
#   2. ./k8s/scripts/wire-platform-values.sh
#   3. git diff   # review the substitutions
#   4. git commit && git push   # ArgoCD reconciles the real values
#
# Requirements: terraform, jq. Override the Terraform dir with TF_DIR if needed.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TF_DIR="${TF_DIR:-${ROOT_DIR}/terraform}"
EKS_DIR="${ROOT_DIR}/k8s/eks"
KARPENTER_VALUES="${ROOT_DIR}/k8s/karpenter/karpenter/values.yaml"

command -v terraform >/dev/null 2>&1 || { echo "terraform is required on PATH" >&2; exit 1; }
command -v jq >/dev/null 2>&1 || { echo "jq is required on PATH" >&2; exit 1; }

tf() { terraform -chdir="${TF_DIR}" output -raw "$1"; }
tfjson() { terraform -chdir="${TF_DIR}" output -json "$1"; }

# Replace literal SEARCH with literal REPLACE (sed-special characters escaped) in FILE.
replace() {
  local file="$1" search="$2" value="$3"
  local esc_search esc_value
  esc_search="$(printf '%s' "$search" | sed -e 's/[\/&|]/\\&/g')"
  esc_value="$(printf '%s' "$value" | sed -e 's/[\/&|]/\\&/g')"
  sed -i "s|${esc_search}|${esc_value}|g" "$file"
}

echo "Reading Terraform outputs from ${TF_DIR} ..."
AWS_REGION="$(tf aws_region)"
DB_HOST="$(tf db_host)"
DB_PORT="$(tf db_port)"
DB_NAME="$(tf db_name)"
RDS_SECRET_ARN="$(tf rds_master_user_secret_arn)"
QUEUE_URL="$(tf document_ingestion_queue_url)"
PROCESSOR_REPO_URL="$(tf document_processor_ecr_repository_url)"

declare -A ROLE_ARN
ROLE_ARN[document-api-service]="$(tf document_api_service_role_arn)"
ROLE_ARN[document-processing-service]="$(tf document_processing_service_role_arn)"
ROLE_ARN[document-review-service]="$(tf document_review_service_role_arn)"
ROLE_ARN[user-management-service]="$(tf user_management_service_role_arn)"

repo_url_for() { tfjson service_ecr_repository_urls | jq -r --arg s "$1" '.[$s]'; }

# --- Spring Boot application services ----------------------------------------
for svc in document-api-service document-processing-service document-review-service user-management-service; do
  file="${EKS_DIR}/${svc}/values.yaml"
  [[ -f "$file" ]] || { echo "skip (missing): $file"; continue; }
  echo "Wiring ${svc} ..."
  replace "$file" "your-account-id.dkr.ecr.your-region.amazonaws.com/${svc}" "$(repo_url_for "$svc")"
  replace "$file" "arn:aws:iam::123456789012:role/${svc}-irsa" "${ROLE_ARN[$svc]}"
  replace "$file" "eu-central-1" "${AWS_REGION}"
done

# Service-specific endpoints.
replace "${EKS_DIR}/document-processing-service/values.yaml" "REPLACE_ME" "${QUEUE_URL}"
replace "${EKS_DIR}/user-management-service/values.yaml" "REPLACE_ME" \
  "jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
replace "${EKS_DIR}/user-management-service/values.yaml" \
  "REPLACE_WITH_TERRAFORM_RDS_SECRET_ARN_OR_NAME" "${RDS_SECRET_ARN}"

# --- Legacy document-processor (image repo only; no IRSA role provisioned) ---
proc_file="${EKS_DIR}/document-processor/values.yaml"
if [[ -f "$proc_file" ]]; then
  echo "Wiring document-processor (image repository) ..."
  replace "$proc_file" "your-account-id.dkr.ecr.your-region.amazonaws.com/document-processor" "${PROCESSOR_REPO_URL}"
  replace "$proc_file" "eu-central-1" "${AWS_REGION}"
fi

# --- Karpenter (only when enabled) -------------------------------------------
if [[ "$(tf karpenter_enabled)" == "true" && -f "$KARPENTER_VALUES" ]]; then
  echo "Wiring Karpenter identity ..."
  CLUSTER_NAME="$(tf eks_cluster_name)"
  CLUSTER_ENDPOINT="$(tf eks_cluster_endpoint)"
  KARPENTER_CONTROLLER_ROLE="$(tf karpenter_controller_role_arn)"
  KARPENTER_NODE_ROLE="$(tf karpenter_node_role_arn)"
  KARPENTER_QUEUE="$(tf karpenter_interruption_queue_name)"

  replace "$KARPENTER_VALUES" "https://REPLACE_ME" "${CLUSTER_ENDPOINT}"
  replace "$KARPENTER_VALUES" "arn:aws:iam::123456789012:role/karpenter-controller-placeholder" "${KARPENTER_CONTROLLER_ROLE}"
  replace "$KARPENTER_VALUES" "karpenter-node-placeholder" "${KARPENTER_NODE_ROLE}"
  replace "$KARPENTER_VALUES" "my-app-eks-karpenter-interruption" "${KARPENTER_QUEUE}"
  replace "$KARPENTER_VALUES" "my-app-eks" "${CLUSTER_NAME}"
else
  echo "Karpenter disabled (karpenter_enabled != true); skipping Karpenter wiring."
fi

echo ""
echo "Done. Review with 'git diff' and commit so ArgoCD reconciles the real values."
