#!/usr/bin/env bash
# Generate Android launcher icons from assets/branding/
#
#   icon-full.png       1024x1024 composite (purple bg + white logo)
#   icon-foreground.png optional transparent PNG; auto-extracted if missing/invalid
#
# Usage:
#   ./scripts/generate-launcher-icons.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VENV="${ROOT}/.venv-icons"
PYTHON="${VENV}/bin/python"

if [[ ! -d "${VENV}" ]]; then
  echo "Creating icon tooling venv..."
  python3 -m venv "${VENV}"
  "${VENV}/bin/pip" install -q pillow
fi

exec "${PYTHON}" "${ROOT}/scripts/generate_launcher_icons.py"
