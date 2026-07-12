#!/usr/bin/env bash
# Build locally and publish a test APK to GitHub Releases in ~1 minute.
# Skips CI entirely — uses your already-compiled native libs.
#
# Usage:
#   ./scripts/publish-test-apk-gh.sh                    # auto tag test/VERSION-DATE
#   ./scripts/publish-test-apk-gh.sh test/2.52.0       # explicit tag
#
set -euo pipefail
cd "$(dirname "$0")/.."

REPO="${POLLI_GH_REPO:-NielLiesmons/polli-chat}"
VERSION="$(grep 'versionName' build.gradle | head -1 | sed 's/.*"\([^"]*\)".*/\1/')"
TAG="${1:-test/${VERSION}-$(git rev-parse --short HEAD)}"
VARIANT="fossDebug"

echo "Building test APK locally…"
./scripts/build-test-apk.sh

APK="$(ls -1 build/outputs/apk/foss/debug/polli-foss-debug-*.apk | head -1)"
OUT="polli-test-${VERSION}-${VARIANT}-arm64.apk"
cp "$APK" "$OUT"

echo ""
echo "Publishing GitHub pre-release: $TAG"
gh release create "$TAG" "$OUT" \
  --repo "$REPO" \
  --prerelease \
  --title "Polli Test ${VERSION} (${VARIANT})" \
  --notes "## Polli test build (local)

| | |
|---|---|
| **Version** | \`${VERSION}\` |
| **Variant** | \`${VARIANT}\` |
| **ABI** | \`arm64-v8a\` |
| **Package** | \`com.polli.chat.beta\` |

Signed with the same debug certificate as local \`./scripts/build-test-apk.sh\` builds on this machine.

Built locally and uploaded — no CI wait.

### Install
1. Download the APK below.
2. Enable *Install unknown apps* for your browser/files app.
3. Open and install."

URL="$(gh release view "$TAG" --repo "$REPO" --json url -q .url)"
echo ""
echo "Pruning older test releases…"
while IFS= read -r old_tag; do
  if [[ -n "$old_tag" && "$old_tag" != "$TAG" ]]; then
    echo "  deleting $old_tag"
    gh release delete "$old_tag" --repo "$REPO" --yes --cleanup-tag 2>/dev/null || true
  fi
done < <(gh release list --repo "$REPO" --limit 100 --json tagName -q '.[].tagName')

echo ""
echo "✓ Release live: $URL"
