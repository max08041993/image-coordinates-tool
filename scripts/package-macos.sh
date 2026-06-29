#!/usr/bin/env bash
set -euo pipefail

TYPE="${1:-dmg}"

if [[ "$TYPE" != "dmg" && "$TYPE" != "pkg" ]]; then
  echo "Usage: $0 [dmg|pkg]" >&2
  exit 1
fi

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "macOS packages can only be built on macOS because jpackage does not cross-package." >&2
  exit 1
fi

command -v mvn >/dev/null || { echo "mvn is required" >&2; exit 1; }
command -v jpackage >/dev/null || { echo "jpackage is required. Install a full JDK, not only a JRE." >&2; exit 1; }
command -v sips >/dev/null || { echo "sips is required and is included with macOS." >&2; exit 1; }
command -v iconutil >/dev/null || { echo "iconutil is required and is included with macOS." >&2; exit 1; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TARGET_DIR="$PROJECT_ROOT/target"
INPUT_DIR="$TARGET_DIR/jpackage-macos-input"
OUTPUT_DIR="$TARGET_DIR/macos-installer"
ICONSET_DIR="$TARGET_DIR/app-icon.iconset"
ICON_PATH="$TARGET_DIR/app-icon.icns"
ASSOCIATION_DIR="$TARGET_DIR/macos-associations"
SOURCE_ICON="$PROJECT_ROOT/src/main/resources/icons/app-icon.png"

cd "$PROJECT_ROOT"

mvn -q package

rm -rf "$INPUT_DIR" "$OUTPUT_DIR" "$ICONSET_DIR" "$ICON_PATH" "$ASSOCIATION_DIR"
mkdir -p "$INPUT_DIR" "$OUTPUT_DIR" "$ICONSET_DIR" "$ASSOCIATION_DIR"

cp "$TARGET_DIR/image-coordinates-tool-1.0.0.jar" "$INPUT_DIR/"
cp -R "$TARGET_DIR/lib" "$INPUT_DIR/lib"

for size in 16 32 128 256 512; do
  sips -z "$size" "$size" "$SOURCE_ICON" --out "$ICONSET_DIR/icon_${size}x${size}.png" >/dev/null
  double_size=$((size * 2))
  sips -z "$double_size" "$double_size" "$SOURCE_ICON" --out "$ICONSET_DIR/icon_${size}x${size}@2x.png" >/dev/null
done

iconutil -c icns "$ICONSET_DIR" -o "$ICON_PATH"

cat > "$ASSOCIATION_DIR/png.properties" <<EOF
extension=png
mime-type=image/png
description=PNG image
icon=$ICON_PATH
EOF

cat > "$ASSOCIATION_DIR/jpg.properties" <<EOF
extension=jpg
mime-type=image/jpeg
description=JPEG image
icon=$ICON_PATH
EOF

cat > "$ASSOCIATION_DIR/jpeg.properties" <<EOF
extension=jpeg
mime-type=image/jpeg
description=JPEG image
icon=$ICON_PATH
EOF

jpackage \
  --type "$TYPE" \
  --name ImageCoordinatesTool \
  --app-version 1.0.0 \
  --vendor "QA Tools" \
  --dest "$OUTPUT_DIR" \
  --input "$INPUT_DIR" \
  --icon "$ICON_PATH" \
  --main-jar image-coordinates-tool-1.0.0.jar \
  --main-class ru.qa.tools.ImageCoordinatesToolLauncher \
  --java-options "-Dfile.encoding=UTF-8" \
  --file-associations "$ASSOCIATION_DIR/png.properties" \
  --file-associations "$ASSOCIATION_DIR/jpg.properties" \
  --file-associations "$ASSOCIATION_DIR/jpeg.properties"

echo "Created macOS $TYPE package in: $OUTPUT_DIR"
