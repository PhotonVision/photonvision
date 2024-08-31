# Uninstall if it already was installed
python3 -m pip uninstall -y photonlibpy

# Build wheel
python3 setup.py bdist_wheel

# Install whatever wheel was made
for f in dist/*.whl; do
    echo "installing $f"
    python3 -m pip install --no-cache-dir "$f"
done

# Run the test suite
pytest -rP --full-trace
