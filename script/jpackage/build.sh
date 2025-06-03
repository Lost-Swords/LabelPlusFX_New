#!/bin/bash

# Set version number: use first argument or prompt user
if [ -z "$1" ]; then
    read -p "Enter version number: " VERSION
else
    VERSION=$1
fi

echo "VERSION: $VERSION"

# Define project and script directories
DIR=$(cd "$(dirname "$0")/../../" && pwd)
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)

# Remove old build directory if exists
rm -rf "./LabelPlusFX"
mkdir -p "./LabelPlusFX"

# Set module path and application icon
MODULES="$DIR/target/build"
ICON="$DIR/images/icons/cat.icns"

# Build Java application image using jpackage
jpackage --verbose \
    --type app-image \
    --app-version "$VERSION" \
    --copyright "Meodinger Tech (C) 2025" \
    --name LabelPlusFX \
    --icon "$ICON" \
    --dest "$SCRIPT_DIR" \
    --module-path "$MODULES" \
    --add-modules jdk.crypto.cryptoki \
    --module lpfx/ink.meodinger.lpfx.LauncherKt

# Copy additional resource files into the output directory
TARGET_DIR="$SCRIPT_DIR/LabelPlusFX"
mkdir -p "$TARGET_DIR"

FILE_LIST="IMEInterface.dylib IMEWrapper.dylib LabelPlusFXDict.alias"

for file in $FILE_LIST; do
    if [ -f "$SCRIPT_DIR/$file" ]; then
        cp -f "$SCRIPT_DIR/$file" "$TARGET_DIR/"
        echo "[Success] Copied: $file"
    else
        echo "[Warning] Missing: $file (skipped)"
    fi
done

# Create output directory for final ZIP package
OUTPUT_DIR="$DIR/Output"
ZIP_NAME="LabelPlusFX-$VERSION-Mac.zip"

mkdir -p "$OUTPUT_DIR"

# Package the application folder into a ZIP file
echo "Packing into ZIP: $ZIP_NAME"
cd "$SCRIPT_DIR" || exit
zip -r "$OUTPUT_DIR/$ZIP_NAME" "LabelPlusFX"

echo
echo "All completed"