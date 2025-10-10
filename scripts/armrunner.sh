#!/bin/bash
###
# Alternative ARM Runner installer to setup PhotonVision JAR
# for ARM based builds such as Raspberry Pi, Orange Pi, etc.
# This assumes that the image provided to arm-runner-action contains
# the servicefile needed to auto-launch PhotonVision.
###

set -e  # Exit on any error
set -x  # Print commands as they execute

# Find the PhotonVision JAR file
echo "=== Searching for PhotonVision JAR ==="
JAR_SEARCH_RESULT=$(find . -name "photonvision*-linuxarm64.jar" 2>/dev/null || true)

if [ -z "$JAR_SEARCH_RESULT" ]; then
    echo "ERROR: No PhotonVision JAR file found matching pattern: photonvision*-linuxarm64.jar"
    echo "Current working directory: $(pwd)"
    echo "Directory contents:"
    ls -la
    exit 1
fi

# Get the full path to the JAR
NEW_JAR=$(realpath "$JAR_SEARCH_RESULT")

if [ ! -f "$NEW_JAR" ]; then
    echo "ERROR: JAR file does not exist at: $NEW_JAR"
    exit 1
fi

echo "=== Found JAR file ==="
echo "JAR path: $NEW_JAR"
echo "JAR name: $(basename "$NEW_JAR")"
echo "JAR size: $(stat -c%s "$NEW_JAR" 2>/dev/null || stat -f%z "$NEW_JAR" 2>/dev/null) bytes"

# Debug information
echo "=== Current environment ==="
echo "Working directory: $(pwd)"
echo "User: $(whoami)"
echo "Directory contents:"
ls -la

# Setup destination
DEST_PV_LOCATION=/opt/photonvision
echo "=== Setting up destination directory ==="
echo "Destination: $DEST_PV_LOCATION"

if ! sudo mkdir -p "$DEST_PV_LOCATION"; then
    echo "ERROR: Failed to create destination directory: $DEST_PV_LOCATION"
    exit 1
fi

echo "=== Destination directory created ==="
ls -la /opt/

# Check if destination file already exists
echo "=== Checking destination ==="
if [ -f "${DEST_PV_LOCATION}/photonvision.jar" ]; then
    echo "WARNING: Existing JAR file found at destination, will be overwritten"
    echo "Existing file info:"
    ls -lh "${DEST_PV_LOCATION}/photonvision.jar"
    EXISTING_SIZE=$(stat -c%s "${DEST_PV_LOCATION}/photonvision.jar" 2>/dev/null || stat -f%z "${DEST_PV_LOCATION}/photonvision.jar" 2>/dev/null)
    echo "Existing file size: $EXISTING_SIZE bytes"

    # Backup the existing file
    BACKUP_NAME="${DEST_PV_LOCATION}/photonvision.jar.backup.$(date +%Y%m%d_%H%M%S)"
    echo "Creating backup: $BACKUP_NAME"
    if ! sudo cp "${DEST_PV_LOCATION}/photonvision.jar" "$BACKUP_NAME"; then
        echo "WARNING: Failed to create backup, proceeding anyway"
    else
        echo "✓ Backup created successfully"
    fi
else
    echo "No existing JAR file found at destination (this is expected for first-time installation)"
fi

# Copy the JAR file
echo "=== Copying JAR file ==="
if ! sudo cp "$NEW_JAR" "${DEST_PV_LOCATION}/photonvision.jar"; then
    echo "ERROR: Failed to copy JAR file from $NEW_JAR to ${DEST_PV_LOCATION}/photonvision.jar"
    echo "Source file check:"
    ls -la "$NEW_JAR"
    echo "Destination directory check:"
    ls -la "$DEST_PV_LOCATION" 2>/dev/null || echo "Destination directory does not exist or is not accessible"
    exit 1
fi

# Verify the copy was successful
echo "=== Verifying copy operation ==="
if [ ! -f "${DEST_PV_LOCATION}/photonvision.jar" ]; then
    echo "ERROR: JAR file not found at destination after copy: ${DEST_PV_LOCATION}/photonvision.jar"
    echo "Destination directory contents:"
    ls -la "$DEST_PV_LOCATION"
    exit 1
fi

# Check file size matches
DEST_SIZE=$(stat -c%s "${DEST_PV_LOCATION}/photonvision.jar" 2>/dev/null || stat -f%z "${DEST_PV_LOCATION}/photonvision.jar" 2>/dev/null)
SRC_SIZE=$(stat -c%s "$NEW_JAR" 2>/dev/null || stat -f%z "$NEW_JAR" 2>/dev/null)

if [ "$DEST_SIZE" != "$SRC_SIZE" ]; then
    echo "ERROR: Destination file size ($DEST_SIZE bytes) does not match source ($SRC_SIZE bytes)"
    exit 1
fi

# Set proper permissions
echo "=== Setting permissions ==="
if ! sudo chmod 644 "${DEST_PV_LOCATION}/photonvision.jar"; then
    echo "WARNING: Failed to set permissions on JAR file"
fi

# Final verification
echo "=== Installation complete ==="
echo "Destination file:"
ls -lh "${DEST_PV_LOCATION}/photonvision.jar"
echo "File size: $DEST_SIZE bytes"
echo "✓ PhotonVision JAR successfully installed"
