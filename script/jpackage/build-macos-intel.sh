#!/bin/bash
set -euo pipefail

# =========================
# Config
# =========================
APP_NAME="LabelPlusFX"
MAIN_MODULE="lpfx"
MAIN_CLASS="ink.meodinger.lpfx.LauncherKt"
ARCH="x86_64"

# =========================
# Version
# =========================
if [[ $# -ge 1 ]]; then
  VERSION="$1"
else
  read -p "Enter version number: " VERSION
fi

echo "[INFO] Version      : $VERSION"
echo "[INFO] Target Arch  : $ARCH"

# =========================
# Paths
# =========================
ROOT_DIR="$(cd "$(dirname "$0")/../../" && pwd)"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

MODULE_PATH="$ROOT_DIR/target/build"
ICON_PATH="$ROOT_DIR/images/icons/cat-macos.icns"

APP_DIR="$SCRIPT_DIR/${APP_NAME}.app"
OUTPUT_DIR="$SCRIPT_DIR/Output"
ZIP_NAME="${APP_NAME}-${VERSION}-Mac-Intel.zip"

# =========================
# Clean
# =========================
echo "[INFO] Cleaning old build..."
rm -rf "$APP_DIR"
mkdir -p "$OUTPUT_DIR"

# =========================
# Build app-image
# =========================
echo "[INFO] Running jpackage..."

jpackage \
  --type app-image \
  --name "$APP_NAME" \
  --app-version "$VERSION" \
  --icon "$ICON_PATH" \
  --dest "$SCRIPT_DIR" \
  --module-path "$MODULE_PATH" \
  --add-modules jdk.crypto.cryptoki \
  --java-options "--add-opens nsmenufx/de.jangassen.platform.mac=lpfx" \
  --module "${MAIN_MODULE}/${MAIN_CLASS}"

# =========================
# Verify
# =========================
if [[ ! -d "$APP_DIR" ]]; then
  echo "[ERROR] App bundle was not created!"
  exit 1
fi

echo "[INFO] App bundle created: $APP_DIR"

# =========================
# Extra resources
# =========================
EXTRA_FILES=(
  "LabelPlusFXDict.alias"
)

for file in "${EXTRA_FILES[@]}"; do
  SRC="$SCRIPT_DIR/$file"
  DST="$APP_DIR/Contents/Resources/$file"

  if [[ -f "$SRC" ]]; then
    cp -f "$SRC" "$DST"
    echo "[INFO] Copied extra file: $file"
  else
    echo "[WARN] Extra file not found, skipped: $file"
  fi
done

# =========================
# Package ZIP
# =========================
echo "[INFO] Creating ZIP package..."

rm -f "$OUTPUT_DIR/$ZIP_NAME"
cd "$SCRIPT_DIR"
zip -qry "$OUTPUT_DIR/$ZIP_NAME" "${APP_NAME}.app"

# =========================
# Done
# =========================
echo
echo "[SUCCESS] macOS Intel package created:"
echo "👉 $OUTPUT_DIR/$ZIP_NAME"
