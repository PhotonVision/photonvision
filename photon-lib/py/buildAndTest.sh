#!/usr/bin/env bash

set -euo pipefail
cd -- "$(dirname -- "$0")"

# Uninstall if it already was installed
uv pip uninstall photonlibpy || true

# Build wheel
rm -rf dist
uv build --wheel

# Install whatever wheel was made
uv pip install --no-cache-dir dist/*.whl

# Run the test suite
pytest -rP
