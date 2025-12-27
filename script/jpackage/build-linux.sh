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
ICON="$DIR/images/icons/cat.png"
TARGET_DIR="$SCRIPT_DIR/LabelPlusFX"
OUTPUT_DIR="$SCRIPT_DIR/Output"
TARBALL_NAME="LabelPlusFX-$VERSION-Linux.tar.gz"

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
    --module-path $MODULES \
    --add-modules jdk.crypto.cryptoki \
    --module lpfx/ink.meodinger.lpfx.LauncherKt



# 验证是否真的生成了内容
if [ "$(ls -A "$TARGET_DIR")" ]; then
    echo "[INFO] Application folder is NOT empty."
else
    echo "[ERROR] Application folder is empty!" >&2
    exit 1
fi

# 创建输出目录
mkdir -p "$OUTPUT_DIR"

# 删除旧 TAR
rm -f "$OUTPUT_DIR/$TARBALL_NAME"

# 打包成 TAR
echo "Packing into tarball: $TARBALL_NAME"
cd "$SCRIPT_DIR"
tar -czvf "$OUTPUT_DIR/$TARBALL_NAME" "LabelPlusFX"

# 验证 TAR 是否存在
if [ -f "$OUTPUT_DIR/$TARBALL_NAME" ]; then
    echo "[INFO] Tarball generated successfully at: $OUTPUT_DIR/$TARBALL_NAME"
else
    echo "[ERROR] Failed to generate tarball!"
    exit 1
fi

echo
echo "All completed"
