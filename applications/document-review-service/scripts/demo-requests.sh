#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8084}"
DOCUMENT_ID="${DOCUMENT_ID:-doc-1001}"
TOKEN="${TOKEN:-}"

if [[ -z "${TOKEN}" ]]; then
  echo "TOKEN env var is required"
  exit 1
fi

curl -sS -H "Authorization: Bearer ${TOKEN}" "${BASE_URL}/api/review/queue?status=PENDING_APPROVAL&limit=20" | jq .
curl -sS -H "Authorization: Bearer ${TOKEN}" "${BASE_URL}/api/review/${DOCUMENT_ID}" | jq .

curl -sS -X PATCH -H "Content-Type: application/json" -H "Authorization: Bearer ${TOKEN}" \
  "${BASE_URL}/api/review/${DOCUMENT_ID}/fields" \
  -d '{"expectedDocumentRevision":1,"corrections":{"supplierName":"Acme Corporation"},"comment":"Corrected supplier legal name"}' | jq .

curl -sS -X POST -H "Content-Type: application/json" -H "Authorization: Bearer ${TOKEN}" \
  "${BASE_URL}/api/review/${DOCUMENT_ID}/approve" \
  -d '{"expectedDocumentRevision":2,"comment":"Looks good","overrideDuplicate":false}' | jq .

curl -sS -H "Authorization: Bearer ${TOKEN}" "${BASE_URL}/api/audit/${DOCUMENT_ID}" | jq .
curl -sS -H "Authorization: Bearer ${TOKEN}" "${BASE_URL}/api/audit/${DOCUMENT_ID}/decision" | jq .
