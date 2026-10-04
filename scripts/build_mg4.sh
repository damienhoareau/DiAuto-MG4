#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
BT=${ANDROID_BUILD_TOOLS:-${ANDROID_HOME:-}/build-tools/35.0.0}
KEYS=${MG4_PLATFORM_KEYS_DIR:?Set MG4_PLATFORM_KEYS_DIR to the directory containing platform.pk8 and platform.x509.pem}
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
