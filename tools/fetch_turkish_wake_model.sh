#!/usr/bin/env bash
set -euo pipefail

# Official Vosk small Turkish model (Apache-2.0): https://alphacephei.com/vosk/models
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ASSET_DIR="$ROOT/app/src/main/assets/model-tr"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

if [[ -s "$ASSET_DIR/am/final.mdl" ]]; then
  echo "Turkish Vosk model already present."
  exit 0
fi

mkdir -p "$ROOT/app/src/main/assets"
ARCHIVE="$TMP_DIR/vosk-model-small-tr-0.3.zip"
curl --fail --location --retry 3 --retry-delay 2 --connect-timeout 20 \
  "https://alphacephei.com/vosk/models/vosk-model-small-tr-0.3.zip" \
  --output "$ARCHIVE"
unzip -tq "$ARCHIVE"
unzip -q "$ARCHIVE" -d "$TMP_DIR/unpacked"
MODEL_ROOT="$(find "$TMP_DIR/unpacked" -mindepth 1 -maxdepth 1 -type d -print -quit)"
test -n "$MODEL_ROOT"
test -s "$MODEL_ROOT/am/final.mdl"
rm -rf "$ASSET_DIR"
mkdir -p "$ASSET_DIR"
cp -a "$MODEL_ROOT"/. "$ASSET_DIR"/
test -s "$ASSET_DIR/am/final.mdl"
echo "Turkish Vosk model installed into app assets."
