#!/usr/bin/env bash
# Run chat feed performance benchmarks and write build/reports/chat-perf/latest.json
#
# Usage:
#   ./scripts/run-chat-perf.sh                 # JVM thresholds (CI gate)
#   ./scripts/run-chat-perf.sh --device        # + Android benchmark (fossBenchmark APK)
#   ./scripts/run-chat-perf.sh --no-haze       # A/B without feed haze
#   ./scripts/run-chat-perf.sh --compare baseline.json
#
set -euo pipefail
cd "$(dirname "$0")/.."

REPORT_DIR="build/reports/chat-perf"
REPORT_JSON="$REPORT_DIR/latest.json"
BASELINE=""
RUN_DEVICE=false
HAZE_PROP=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device) RUN_DEVICE=true ;;
    --no-haze) HAZE_PROP="-PCHAT_HAZE_ENABLED=false" ;;
    --compare)
      shift
      BASELINE="${1:-}"
      ;;
  esac
  shift || true
done

mkdir -p "$REPORT_DIR"
STAMP="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
COMMIT="$(git rev-parse --short HEAD 2>/dev/null || echo unknown)"

echo "== JVM feed perf tests =="
JVM_START=$(python3 - <<'PY'
import time; print(int(time.time()*1000))
PY
)
./gradlew :polli-ui:jvmTest --tests com.polli.ui.chat.ChatFeedPerfTest --quiet
JVM_END=$(python3 - <<'PY'
import time; print(int(time.time()*1000))
PY
)
JVM_MS=$((JVM_END - JVM_START))

OPEN_MS="null"
JANK_PCT="null"
BIND_CALLS="null"
DEVICE_OK="false"

if $RUN_DEVICE; then
  if adb get-state >/dev/null 2>&1; then
    echo "== Android benchmark (fossBenchmark) =="
    ./gradlew assembleFossBenchmark $HAZE_PROP --quiet
    APK="build/outputs/apk/foss/benchmark/polli-foss-benchmark-"*.apk
    APK_FILE=$(ls -1 $APK 2>/dev/null | head -1)
    if [[ -n "$APK_FILE" ]]; then
      adb install -r "$APK_FILE" >/dev/null
      LOG_FILE="$REPORT_DIR/instrumentation.log"
      ./gradlew connectedFossBenchmarkAndroidTest \
        -Pandroid.testInstrumentationRunnerArguments.class=com.polli.android.benchmark.ChatFeedBenchmark \
        $HAZE_PROP 2>&1 | tee "$LOG_FILE" || true
      OPEN_MS=$(rg -o 'open_ms=\d+' "$LOG_FILE" | tail -1 | cut -d= -f2 || echo null)
      JANK_PCT=$(rg -o 'jank_pct=[0-9.]+' "$LOG_FILE" | tail -1 | cut -d= -f2 || echo null)
      BIND_CALLS=$(rg -o 'bind_calls=\d+' "$LOG_FILE" | tail -1 | cut -d= -f2 || echo null)
      if rg -q 'BUILD SUCCESSFUL' "$LOG_FILE" 2>/dev/null || rg -q 'OK \(' "$LOG_FILE"; then
        DEVICE_OK="true"
      fi
    fi
  else
    echo "No adb device — skipping Android benchmarks"
  fi
fi

cat >"$REPORT_JSON" <<EOF
{
  "timestamp": "$STAMP",
  "commit": "$COMMIT",
  "jvm_ms": $JVM_MS,
  "open_ms": ${OPEN_MS:-null},
  "jank_pct": ${JANK_PCT:-null},
  "bind_calls_on_scroll": ${BIND_CALLS:-null},
  "device_ran": $DEVICE_OK,
  "haze_enabled": $([ "$HAZE_PROP" = "" ] && echo true || echo false)
}
EOF

echo "Wrote $REPORT_JSON"

if [[ -n "$BASELINE" && -f "$BASELINE" ]]; then
  python3 - <<PY
import json, sys
cur = json.load(open("$REPORT_JSON"))
base = json.load(open("$BASELINE"))
for key in ("jvm_ms", "open_ms", "jank_pct"):
    if cur.get(key) is not None and base.get(key) is not None:
        delta = cur[key] - base[key]
        print(f"{key}: {base[key]} -> {cur[key]} ({delta:+})")
PY
fi

echo "✓ Chat perf run complete"
