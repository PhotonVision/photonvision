#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Extract package info from control/control file
if [ ! -f "control/control" ]; then
    echo "Error: control/control not found!"
    echo "Create a control/control file with package metadata"
    exit 1
fi

# Parse package name from control file
PACKAGE_NAME=$(grep "^Package:" control/control | cut -d' ' -f2- | tr -d ' ')

if [ -z "$PACKAGE_NAME" ]; then
    echo "Err: Package must be set in control/control"
    echo "Package: my-package"
    exit 1
fi

GIT_DESCRIBE="$(git -C "$SCRIPT_DIR" describe --tags --always)"
if [ -n "$(git -C "$SCRIPT_DIR" status --porcelain --untracked-files=all -- .)" ]; then
    PACKAGE_VERSION="${GIT_DESCRIBE}-$(date +%y%m%d%H%M%S)"
else
    PACKAGE_VERSION="$GIT_DESCRIBE"
fi

PACKAGE_DIR="${PACKAGE_NAME}_${PACKAGE_VERSION}"
BUILD_DIR="$(mktemp -d)"
OUTPUT_DIR="$SCRIPT_DIR/../build/sc-app"
OUTPUT_IPK="$OUTPUT_DIR/${PACKAGE_NAME}_${PACKAGE_VERSION}.ipk"
trap 'rm -rf "$BUILD_DIR"' EXIT

echo "Building IPK package from overlay structure..."
echo "Package: ${PACKAGE_NAME}_${PACKAGE_VERSION}.ipk"

if [ ! -d "overlay" ]; then
    echo "overlay/ directory not found"
    exit 1
fi

if [ ! -d "control" ]; then
    echo "Error: control/ directory not found!"
    echo "Create control/ with control, postinst, prerm, postrm files"
    exit 1
fi

echo "Preparing temporary build..."
mkdir -p "$BUILD_DIR/$PACKAGE_DIR"

echo "Copying overlay structure..."
cp -r overlay/* "$BUILD_DIR/$PACKAGE_DIR/"
find "$BUILD_DIR/$PACKAGE_DIR" -type d -name __pycache__ -prune -exec rm -rf {} +
rm -f "$BUILD_DIR/$PACKAGE_DIR/usr/local/bin/photon-sc-app/www/discovery_status.json"

echo "Writing VERSION file..."
echo "$PACKAGE_VERSION" > "$BUILD_DIR/$PACKAGE_DIR/usr/local/bin/photon-sc-app/VERSION"

echo "Copying CONTROL files..."
mkdir -p "$BUILD_DIR/$PACKAGE_DIR/CONTROL"
cp control/* "$BUILD_DIR/$PACKAGE_DIR/CONTROL/"

awk -v version="$PACKAGE_VERSION" '
    /^Version:/ { print "Version: " version; version_written = 1; next }
    { print }
    END { if (!version_written) print "Version: " version }
' "$BUILD_DIR/$PACKAGE_DIR/CONTROL/control" > "$BUILD_DIR/$PACKAGE_DIR/CONTROL/control.tmp"
mv "$BUILD_DIR/$PACKAGE_DIR/CONTROL/control.tmp" "$BUILD_DIR/$PACKAGE_DIR/CONTROL/control"

echo "Normalizing text file line endings..."
while IFS= read -r -d '' file; do
    if grep -Iq . "$file"; then
        awk '{ sub(/\r$/, ""); print }' "$file" > "$file.normalized"
        mv "$file.normalized" "$file"
    fi
done < <(find "$BUILD_DIR/$PACKAGE_DIR" -type f -print0)

echo "Setting file permissions..."

# Make scripts executable
find "$BUILD_DIR/$PACKAGE_DIR" -name "*.py" -exec chmod +x {} \;

if [ -d "$BUILD_DIR/$PACKAGE_DIR/CONTROL" ]; then
    chmod +x "$BUILD_DIR/$PACKAGE_DIR/CONTROL"/* 2>/dev/null || true
fi

find "$BUILD_DIR/$PACKAGE_DIR" -name "*.sh" -exec chmod +x {} \;

echo "Building IPK dir structure"
cd "$BUILD_DIR"

echo "Creating data.tar.gz"
(
    cd "$PACKAGE_DIR"
    find . -mindepth 1 ! -path './CONTROL' ! -path './CONTROL/*' -print0 |
        tar --null --hard-dereference --files-from=- -czf "$BUILD_DIR/data.tar.gz"
)

echo "Creating control.tar.gz"
(
    cd "$PACKAGE_DIR/CONTROL"
    find . -mindepth 1 -print0 |
        tar --null --files-from=- -czf "$BUILD_DIR/control.tar.gz"
)

echo "Creating IPK..."
mkdir -p "$OUTPUT_DIR"
rm -f "$OUTPUT_IPK"
ar r "$OUTPUT_IPK" control.tar.gz data.tar.gz

cd "$SCRIPT_DIR"

echo ""
echo "IPK package created."
echo "Package: $OUTPUT_IPK"
echo ""
echo "Package structure:"
echo "  CONTROL files:"
find control -type f | sort | sed 's/^/    /'
echo "  Overlay files (will be installed):"
find overlay -type f ! -path '*/__pycache__/*' ! -path '*/www/discovery_status.json' | sort | sed 's/^overlay/    /' | head -15

if [ $(find overlay -type f ! -path '*/__pycache__/*' ! -path '*/www/discovery_status.json' | wc -l) -gt 15 ]; then
    echo "    ... and $(($(find overlay -type f ! -path '*/__pycache__/*' ! -path '*/www/discovery_status.json' | wc -l) - 15)) more files"
fi

echo ""
echo "Build complete"