#!/usr/bin/env bash
set -euo pipefail

# Simple helper to build the debug APK (uses ./gradlew if present, otherwise system gradle)
# and install it to a connected Android device using adb.
# Usage: ./scripts/install_and_run.sh [--apk PATH]

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK_OVERRIDE=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    -a|--apk)
      APK_OVERRIDE="$2"
      shift 2
      ;;
    -h|--help)
      echo "Usage: $0 [--apk PATH]"
      echo "If --apk is not provided the script will try to build app-debug.apk using ./gradlew or gradle."
      exit 0
      ;;
    *)
      echo "Unknown arg: $1" >&2
      exit 2
      ;;
  esac
done

cd "$ROOT_DIR"

if [[ -n "$APK_OVERRIDE" ]]; then
  APK_PATH="$APK_OVERRIDE"
else
  echo "Looking for Gradle wrapper..."
  if [[ -x "./gradlew" ]]; then
    echo "Using ./gradlew to build debug APK"
    BUILD_LOG=$(mktemp)
    # Run with verbose output and capture to a log so we can show a focused tail on failure
    if ./gradlew --no-daemon --console=plain --stacktrace --info assembleDebug >"$BUILD_LOG" 2>&1; then
      echo "Gradle build succeeded"
    else
      echo "Gradle build failed. Showing last 300 lines of the build log:" >&2
      tail -n 300 "$BUILD_LOG" >&2 || true
      echo "" >&2
      echo "You can try to run with refreshed dependencies: ./gradlew --refresh-dependencies assembleDebug" >&2
      echo "If the error mentions missing artifacts (e.g. androidx.compose.compiler:compiler), ensure your project settings.gradle.kts and Gradle repositories include 'google()' and 'mavenCentral()' and that the Compose compiler version matches your Kotlin version." >&2
      echo "Configured kotlinCompilerExtensionVersion (app/build.gradle.kts):" >&2
      awk '/kotlinCompilerExtensionVersion/ { print; exit }' app/build.gradle.kts 2>/dev/null || true
      echo "Search available Compose compiler releases:" >&2
      echo "  https://search.maven.org/search?q=g:androidx.compose.compiler%20AND%20a:compiler" >&2
      rm -f "$BUILD_LOG"
      exit 3
    fi
    rm -f "$BUILD_LOG"
  elif command -v gradle >/dev/null 2>&1; then
    echo "Using system gradle to build debug APK"
    BUILD_LOG=$(mktemp)
    if gradle --console=plain --stacktrace --info assembleDebug >"$BUILD_LOG" 2>&1; then
      echo "Gradle build succeeded"
    else
      echo "Gradle build failed. Showing last 300 lines of the build log:" >&2
      tail -n 300 "$BUILD_LOG" >&2 || true
      echo "" >&2
      echo "Try: gradle --refresh-dependencies assembleDebug" >&2
      echo "Configured kotlinCompilerExtensionVersion (app/build.gradle.kts):" >&2
      awk '/kotlinCompilerExtensionVersion/ { print; exit }' app/build.gradle.kts 2>/dev/null || true
      echo "Search available Compose compiler releases:" >&2
      echo "  https://search.maven.org/search?q=g:androidx.compose.compiler%20AND%20a:compiler" >&2
      rm -f "$BUILD_LOG"
      exit 3
    fi
    rm -f "$BUILD_LOG"
  else
    echo "No gradle wrapper or system gradle found. Please build the APK with Android Studio or install Gradle." >&2
    exit 3
  fi

  APK_PATH="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
fi

if [[ ! -f "$APK_PATH" ]]; then
  echo "APK not found at: $APK_PATH" >&2
  exit 4
fi

echo "Found APK: $APK_PATH"

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found on PATH. Please install platform-tools and ensure adb is on PATH." >&2
  exit 5
fi

echo "Restarting adb server..."
adb kill-server || true
adb start-server

# collect device list where state == device
mapfile -t DEVICES < <(adb devices | tail -n +2 | awk '$2 == "device" { print $1 }')

if [[ ${#DEVICES[@]} -eq 0 ]]; then
  echo "No connected devices found (state 'device'). Run 'adb devices' to debug." >&2
  exit 6
fi

# prefer a physical device over emulator if possible
TARGET_DEVICE="${DEVICES[0]}"
for d in "${DEVICES[@]}"; do
  if [[ "$d" != emulator-* ]]; then
    TARGET_DEVICE="$d"
    break
  fi
done

echo "Installing APK to device: $TARGET_DEVICE"
adb -s "$TARGET_DEVICE" install -r "$APK_PATH"

echo "Install finished. You can now run the app on the device or use 'adb -s $TARGET_DEVICE logcat' to view logs."

exit 0
