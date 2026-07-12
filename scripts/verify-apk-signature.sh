#!/usr/bin/env bash
# Fail if an APK is not signed with the expected local debug certificate.
set -euo pipefail

APK="${1:?usage: verify-apk-signature.sh path/to.apk}"
EXPECTED="${POLLI_DEBUG_CERT_SHA256:-C8:7E:32:82:56:BC:63:4A:C2:C9:74:4B:6C:0E:46:A2:59:B9:65:6D:35:2D:90:FF:2A:E2:2C:96:8B:71:44:16}"

ACTUAL="$(keytool -printcert -jarfile "$APK" 2>/dev/null | awk -F': ' '/SHA256:/ {print $2; exit}')"
if [[ -z "$ACTUAL" ]]; then
  echo "error: could not read APK certificate from $APK" >&2
  exit 1
fi

if [[ "$ACTUAL" != "$EXPECTED" ]]; then
  echo "error: APK signature mismatch" >&2
  echo "  expected: $EXPECTED" >&2
  echo "  actual:   $ACTUAL" >&2
  echo "  This APK will fail to install over existing Polli sideload builds (App not installed)." >&2
  exit 1
fi

echo "✓ APK signed with expected debug certificate"
