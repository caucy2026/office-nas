#!/usr/bin/env bash
# Builds only the upstream Collabora Android smoke candidate. It deliberately
# does not copy upstream sources into DeskLink or claim to integrate Office.
set -euo pipefail

required=(ABI ANDROID_SDK_ROOT COLLABORA_ROOT BUILD_MODE GITHUB_WORKSPACE)
for name in "${required[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required environment variable: $name" >&2
    exit 2
  fi
done

if [[ "$ABI" != "arm64-v8a" ]]; then
  echo "This smoke path intentionally builds only arm64-v8a, got: $ABI" >&2
  exit 2
fi

case "$BUILD_MODE" in
  preflight|assemble) ;;
  *)
    echo "Unsupported BUILD_MODE: $BUILD_MODE" >&2
    exit 2
    ;;
esac

android_ndk="$ANDROID_SDK_ROOT/ndk/${NDK_VERSION:-23.0.7599858}"
engine_dir="$COLLABORA_ROOT/engine"
jobs="${BUILD_JOBS:-$(nproc)}"

test -x "$engine_dir/autogen.sh"
test -x "$COLLABORA_ROOT/autogen.sh"
test -x "$COLLABORA_ROOT/android/gradlew"
test -d "$android_ndk"

mkdir -p "$GITHUB_WORKSPACE/out"

cat > "$engine_dir/autogen.input" <<EOF
--build=x86_64-unknown-linux-gnu
--with-android-ndk=$android_ndk
--with-android-sdk=$ANDROID_SDK_ROOT
--with-distro=CPAndroidAarch64
--with-parallelism=$jobs
EOF

pushd "$engine_dir"
./autogen.sh 2>&1 | tee "$GITHUB_WORKSPACE/out/engine-autogen.log"
if [[ "$BUILD_MODE" == "assemble" ]]; then
  make -j"$jobs"
fi
popd

if [[ "$BUILD_MODE" == "preflight" ]]; then
  printf 'Collabora Android preflight passed. Re-run with mode=assemble and immutable source SHA: %s\n' \
    "$(git -C "$COLLABORA_ROOT" rev-parse HEAD)" | tee "$GITHUB_WORKSPACE/out/README.txt"
  exit 0
fi

pushd "$COLLABORA_ROOT"
./autogen.sh
./configure \
  --enable-androidapp \
  --with-lo-builddir="$engine_dir" \
  --enable-debug \
  --with-android-abi="$ABI"
make -j"$jobs"
popd

pushd "$COLLABORA_ROOT/android"
./gradlew build --stacktrace
popd

mapfile -t apks < <(find "$COLLABORA_ROOT/android" -type f -path '*/build/outputs/apk/*/*.apk' -print)
if (( ${#apks[@]} == 0 )); then
  echo "No APK was found under the Collabora Android build outputs." >&2
  exit 1
fi

for apk in "${apks[@]}"; do
  cp "$apk" "$GITHUB_WORKSPACE/out/"
done

sha256sum "$GITHUB_WORKSPACE"/out/*.apk | tee "$GITHUB_WORKSPACE/out/SHA256SUMS.txt"
