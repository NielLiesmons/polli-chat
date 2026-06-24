#!/usr/bin/env bash
# Start Pixel 9 emulator (if needed), build fossDebug, install, and launch Polli.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"

AVD="${POLLI_AVD:-Pixel_9_API_35}"
APK="$ROOT/build/outputs/apk/foss/debug/deltachat-foss-debug-"*.apk

if ! pgrep -f qemu-system-aarch64 >/dev/null 2>&1; then
  echo "Starting emulator $AVD..."
  nohup emulator -avd "$AVD" -no-snapshot-load -no-boot-anim -gpu host >> /tmp/polli-emulator.log 2>&1 &
  echo "Waiting for boot..."
  adb wait-for-device
  for _ in $(seq 1 90); do
    boot="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
    [[ "$boot" == "1" ]] && break
    sleep 2
  done
else
  echo "Emulator already running."
fi

echo "Building fossDebug..."
cd "$ROOT"
./gradlew assembleFossDebug

APK_FILE=$(ls -t "$ROOT"/build/outputs/apk/foss/debug/deltachat-foss-debug-*.apk | head -1)
echo "Installing $APK_FILE..."
adb install -r "$APK_FILE"

echo "Launching app..."
adb shell monkey -p com.b44t.messenger.beta -c android.intent.category.LAUNCHER 1
echo "Done. Emulator window should show Delta Chat / Polli."
