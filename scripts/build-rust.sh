#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)";OUT="$ROOT/app/src/main/jniLibs";mkdir -p "$OUT"
command -v cargo >/dev/null||{ echo "Rust/cargo is required" >&2;exit 1; }
command -v cargo-ndk >/dev/null 2>&1||cargo install cargo-ndk --locked
NDK="$(printenv ANDROID_NDK_HOME 2>/dev/null || true)"
[[ -n "$NDK" ]]||{ echo "ANDROID_NDK_HOME is required" >&2;exit 1; }
rm -rf "$OUT";mkdir -p "$OUT"
cd "$ROOT/native"
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -t x86 -o "$OUT" build --release 