# Uninstall if it already was installed
python -m pip uninstall -y photonlibpy

# Build wheel
python setup.py bdist_wheel

# Install whatever wheel was made
for f in dist/*.whl; do
    echo "installing $f"
    python -m pip install --no-cache-dir "$f"
done

# Run the test suite
pytest -rP
