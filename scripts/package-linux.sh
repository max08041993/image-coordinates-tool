#!/usr/bin/env bash
set -euo pipefail

TYPE="${1:-deb}"

if [[ "$TYPE" != "deb" && "$TYPE" != "rpm" && "$TYPE" != "app-image" ]]; then
  echo "Usage: $0 [deb|rpm|app-image]" >&2
  exit 1
fi

if [[ "$(uname -s)" != "Linux" ]]; then
  echo "Linux packages can only be built on Linux because jpackage does not cross-package." >&2
  exit 1
fi

command -v mvn >/dev/null || { echo "mvn is required" >&2; exit 1; }
command -v jpackage >/dev/null || { echo "jpackage is required. Install a full JDK, not only a JRE." >&2; exit 1; }

if [[ "$TYPE" == "deb" ]]; then
  command -v dpkg-deb >/dev/null || { echo "dpkg-deb is required to build .deb packages." >&2; exit 1; }
fi

if [[ "$TYPE" == "rpm" ]]; then
  command -v rpmbuild >/dev/null || { echo "rpmbuild is required to build .rpm packages." >&2; exit 1; }
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TARGET_DIR="$PROJECT_ROOT/target"
INPUT_DIR="$TARGET_DIR/jpackage-linux-input"
OUTPUT_DIR="$TARGET_DIR/linux-installer"
ASSOCIATION_DIR="$TARGET_DIR/linux-associations"
ICON_PATH="$PROJECT_ROOT/src/main/resources/icons/app-icon.png"

cd "$PROJECT_ROOT"

mvn -q package

rm -rf "$INPUT_DIR" "$OUTPUT_DIR" "$ASSOCIATION_DIR"
mkdir -p "$INPUT_DIR" "$OUTPUT_DIR" "$ASSOCIATION_DIR"

cp "$TARGET_DIR/image-coordinates-tool-1.0.0.jar" "$INPUT_DIR/"
cp -R "$TARGET_DIR/lib" "$INPUT_DIR/lib"

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
  --name image-coordinates-tool \
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

echo "Created Linux $TYPE package in: $OUTPUT_DIR"
