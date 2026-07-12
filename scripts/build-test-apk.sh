#!/usr/bin/env bash
# Build a sideloadable Polli test APK (FOSS debug, arm64).
#
# Usage:
#   ./scripts/build-test-apk.sh              # build only
#   ./scripts/build-test-apk.sh --install    # build + install on connected device
#
set -euo pipefail
cd "$(dirname "$0")/.."

INSTALL=false
if [[ "${1:-}" == "--install" ]]; then
  INSTALL=true
fi

echo "Building Polli test APK (fossDebug, arm64-v8a)…"
./gradlew --no-daemon clean assembleFossDebug -PABI_FILTER=arm64-v8a -x lint

APK="$(ls -1 build/outputs/apk/foss/debug/polli-foss-debug-*.apk 2>/dev/null | head -1)"
if [[ -z "$APK" || ! -f "$APK" ]]; then
  echo "error: APK not found under build/outputs/apk/foss/debug/" >&2
  exit 1
fi

"$(dirname "$0")/verify-apk-signature.sh" "$APK"

echo ""
echo "✓ Test APK ready:"
echo "  $APK"
echo "  $(du -h "$APK" | cut -f1)"

if $INSTALL; then
  echo ""
  echo "Installing on connected device…"
  adb install -r -d "$APK"
fi
