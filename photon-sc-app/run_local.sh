#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$SCRIPT_DIR"

echo "Starting Photon SC App in local development mode..."
python3 "${SCRIPT_DIR}/overlay/usr/local/bin/photon-sc-app/photon_sc_app.py" --local "$@"
