#!/bin/bash

set -e

# Extract package info from control/control file
if [ ! -f "control/control" ]; then
    echo "Error: control/control not found!"
    echo "Create a control/control file with package metadata"
    exit 1
fi

# Parse package name and version from control file
PACKAGE_NAME=$(grep "^Package:" control/control | cut -d' ' -f2- | tr -d ' ')
PACKAGE_VERSION=$(grep "^Version:" control/control | cut -d' ' -f2- | tr -d ' ')

# Validate required fields
if [ -z "$PACKAGE_NAME" ] || [ -z "$PACKAGE_VERSION" ]; then
    echo "Err: Package and Version must be set in control/control"
    echo "Package: my-package"
    echo "Version: 1.0.0"
    exit 1
fi

PACKAGE_DIR="${PACKAGE_NAME}_${PACKAGE_VERSION}"
BUILD_DIR="build"

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

echo "Cleaning previous build..."
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/$PACKAGE_DIR"

echo "Copying overlay structure..."
cp -r overlay/* "$BUILD_DIR/$PACKAGE_DIR/"

echo "Copying CONTROL files..."
mkdir -p "$BUILD_DIR/$PACKAGE_DIR/CONTROL"
cp control/* "$BUILD_DIR/$PACKAGE_DIR/CONTROL/"

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
tar --exclude='CONTROL' -czf data.tar.gz -C "$PACKAGE_DIR" .

echo "Creating control.tar.gz"
tar -czf control.tar.gz -C "$PACKAGE_DIR/CONTROL" .

echo "Creating IPK..."
ar r "../${PACKAGE_NAME}_${PACKAGE_VERSION}.ipk" control.tar.gz data.tar.gz

cd ..

echo ""
echo "IPK package created."
echo "Package: ${PACKAGE_NAME}_${PACKAGE_VERSION}.ipk"
echo ""
echo "Package structure:"
echo "  CONTROL files:"
find control -type f | sort | sed 's/^/    /'
echo "  Overlay files (will be installed):"
find overlay -type f | sort | sed 's/^overlay/    /' | head -15

if [ $(find overlay -type f | wc -l) -gt 15 ]; then
    echo "    ... and $(($(find overlay -type f | wc -l) - 15)) more files"
fi

rm -rf "$BUILD_DIR"

echo ""
echo "Build complete"