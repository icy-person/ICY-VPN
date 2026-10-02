#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LIB_DIR="$ROOT/app/libs";AAR="$LIB_DIR/libXray.aar"
VERSION="v26.9.30";URL="https://github.com/XTLS/libXray/releases/download/$VERSION/libxray-android.zip";SHA256="394b59d147b37ab46003bd3f1d687ed3b2f637cabfc0cb863f179c5a87bddde3"
mkdir -p "$LIB_DIR"
if [[ -s "$AAR" ]];then exit 0;fi
command -v curl >/dev/null||exit 1
command -v unzip >/dev/null||exit 1
tmp="$(mktemp -d)";trap 'rm -rf "$tmp"' EXIT
curl -L --fail --retry 3 "$URL" -o "$tmp/libxray.zip"
echo "$SHA256  $tmp/libxray.zip"|sha256sum -c -
unzip -q "$tmp/libxray.zip" -d "$tmp/unpacked"
found="$(find "$tmp/unpacked" -type f -name 'libXray.aar' -print -quit)"
[[ -n "$found" ]]||{ echo "libXray.aar not found" >&2;exit 1; }
cp "$found" "$AAR"