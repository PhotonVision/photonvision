#!/bin/bash

# Find the directory of this script
SCRIPT_DIR="$(dirname -- "$0")"

# Path to the calibration tool
TOOL_PATH="$SCRIPT_DIR/../photon-calibration-tool/build/install/photon-calibration-tool/bin/photon-calibration-tool"

if [[ ! -f "$TOOL_PATH" ]]; then
  # Build the calibration tool with Gradle if it doesn't exist
  "$SCRIPT_DIR/../gradlew" installDist --console=basic
fi

# Execute the calibration tool
"$TOOL_PATH" "$@"
