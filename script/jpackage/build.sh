#!/bin/bash

# 设置版本号
if [ -z "$1" ]; then
    read -p "Enter version number: " VERSION
else
    VERSION="$1"
fi

echo "VERSION: $VERSION"

# 定义目录
DIR=$(cd "$(dirname "$0")/../../" && pwd)
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
MODULES="$DIR/target/build"
ICON="$DIR/images/icons/cat.icns"
TARGET_DIR="$SCRIPT_DIR/LabelPlusFX.app"
OUTPUT_DIR="$SCRIPT_DIR/Output"
ZIP_NAME="LabelPlusFX-$VERSION-Mac.zip"

# 清理旧目录
rm -rf "$TARGET_DIR"

# 构建 Java 应用镜像
echo "[INFO] Building Java application image..."
jpackage --verbose \
    --type app-image \
    --app-version "$VERSION" \
    --copyright "Meodinger Tech (C) 2025" \
    --name LabelPlusFX \
    --icon "$ICON" \
    --dest "$SCRIPT_DIR" \
    --module-path %MODULES% \
    --add-modules lpfx,jdk.crypto.cryptoki \
    --module lpfx/ink.meodinger.lpfx.LauncherKt



# 验证是否真的生成了内容
if [ "$(ls -A "$TARGET_DIR")" ]; then
    echo "[INFO] Application folder is NOT empty."
else
    echo "[ERROR] Application folder is empty!" >&2
    exit 1
fi

# 拷贝额外资源文件
FILE_LIST="LabelPlusFXDict.alias"

for file in $FILE_LIST; do
    if [ -f "$SCRIPT_DIR/$file" ]; then
        cp -f "$SCRIPT_DIR/$file" "$TARGET_DIR/Contents/Resources/"
        echo "[Success] Copied: $file"
    else
        echo "[Warning] Missing: $file (skipped)"
    fi
done

# 创建输出目录
mkdir -p "$OUTPUT_DIR"

# 删除旧 ZIP
if [ -f "$OUTPUT_DIR/$ZIP_NAME" ]; then
    rm -f "$OUTPUT_DIR/$ZIP_NAME"
fi

# 打包成 ZIP
echo "Packing into ZIP: $ZIP_NAME"
cd "$SCRIPT_DIR" || exit
zip -r "$OUTPUT_DIR/$ZIP_NAME" "LabelPlusFX.app"

# 验证 ZIP 是否存在
if [ -f "$OUTPUT_DIR/$ZIP_NAME" ]; then
    echo "[INFO] ZIP file generated successfully at: $OUTPUT_DIR/$ZIP_NAME"
else
    echo "[ERROR] Failed to generate ZIP file!"
    exit 1
fi

echo
echo "All completed"
