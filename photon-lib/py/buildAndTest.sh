#!/usr/bin/env bash

set -euo pipefail
cd -- "$(dirname -- "$0")"

# Create and activate virtual environment
if [ ! -d ".venv" ]; then
    echo "Creating virtual environment in .venv"
    python3 -m venv .venv
fi

# Activate the virtual environment
source .venv/bin/activate

# Uninstall if it already was installed
python3 -m pip uninstall -y photonlibpy

# Build wheel
python3 -m pip install --upgrade pip
python3 -m pip install wheel setuptools pytest mypy
python3 setup.py bdist_wheel

# Install whatever wheel was made
for f in dist/*.whl; do
    echo "installing $f"
    python3 -m pip install --no-cache-dir "$f"
done

# Run the test suite
pytest -rP

cd ../../
mypy --show-column-numbers --config-file photon-lib/py/pyproject.toml photon-lib
