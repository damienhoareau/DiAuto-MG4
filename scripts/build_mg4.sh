#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK" ]; then
  if [ -d "$HOME/Library/Android/sdk" ]; then
    SDK="$HOME/Library/Android/sdk"
  elif [ -d "$HOME/Android/Sdk" ]; then
    SDK="$HOME/Android/Sdk"
  fi
fi

if [ -n "${ANDROID_BUILD_TOOLS:-}" ]; then
  BT="$ANDROID_BUILD_TOOLS"
elif [ -n "$SDK" ] && [ -d "$SDK/build-tools" ]; then
  BT=$(ls -d "$SDK"/build-tools/* 2>/dev/null | tail -n1)
else
  echo "Error: Android SDK or build-tools not found. Set ANDROID_HOME or ANDROID_BUILD_TOOLS." >&2
  exit 1
fi

KEYS="${MG4_PLATFORM_KEYS_DIR:-$ROOT/tools}"
if [ ! -f "$KEYS/platform.pk8" ]; then
  echo "Error: platform.pk8 not found in $KEYS. Set MG4_PLATFORM_KEYS_DIR to folder containing platform.pk8 & platform.x509.pem" >&2
  exit 1
fi
VERSION="0.3.11-mg4.1"
# Gradle output name varies; prefer the newest githubCar debug apk.
ALIGNED="$ROOT/app/build/outputs/apk/githubCar/debug/DiAuto-MG4-v$VERSION-aligned.apk"
SIGNED="$ROOT/app/build/outputs/apk/githubCar/debug/DiAuto-MG4-v$VERSION.apk"
DELIVERY_DIR=$(CDPATH= cd -- "$ROOT/.." && pwd)
DELIVERY_APK="$DELIVERY_DIR/DiAuto-MG4-v$VERSION.apk"

cd "$ROOT"
./gradlew :app:assembleGithubCarDebug

APK=$(ls -t "$ROOT"/app/build/outputs/apk/githubCar/debug/*.apk \
  "$ROOT"/app/build/outputs/apk/github/car/debug/*.apk 2>/dev/null | head -n1)
: "${APK:?No githubCar debug APK found}"

"$BT/zipalign" -f 4 "$APK" "$ALIGNED"
"$BT/apksigner" sign \
  --key "$KEYS/platform.pk8" \
  --cert "$KEYS/platform.x509.pem" \
  --out "$SIGNED" "$ALIGNED"
"$BT/apksigner" verify --verbose --print-certs "$SIGNED"
cp "$SIGNED" "$DELIVERY_APK"
echo "$DELIVERY_APK"
