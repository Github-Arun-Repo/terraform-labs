#!/usr/bin/env bash
set -euo pipefail

ROLE="${1:-FINANCE_ANALYST}"
SECRET="${JWT_SECRET:-this-is-a-long-enough-secret-for-hs256-signing}"
SUBJECT="${JWT_SUBJECT:-analyst@company.com}"
ISSUER="${JWT_ISSUER:-document-platform}"

python3 - <<'PY'
import base64
import hashlib
import hmac
import json
import os
import time


def b64url(v: bytes) -> str:
    return base64.urlsafe_b64encode(v).decode().rstrip('=')

secret = os.environ['SECRET'].encode()
sub = os.environ['SUBJECT']
role = os.environ['ROLE']
issuer = os.environ['ISSUER']
now = int(time.time())

header = {'alg': 'HS256', 'typ': 'JWT'}
payload = {
    'iss': issuer,
    'sub': sub,
    'roles': [role],
    'iat': now,
    'exp': now + 3600,
}

h = b64url(json.dumps(header, separators=(',', ':')).encode())
p = b64url(json.dumps(payload, separators=(',', ':')).encode())
sig = b64url(hmac.new(secret, f"{h}.{p}".encode(), hashlib.sha256).digest())
print(f"{h}.{p}.{sig}")
PY
