::: Uninstall if it already was installed
uv pip uninstall photonlibpy

::: Build wheel
if exist dist rmdir /s /q dist
uv build --wheel

::: Install whatever wheel was made
for %%f in (dist/*.whl) do (
    echo installing dist/%%f
    uv pip install --no-cache-dir dist/%%f
)

::: Run the test suite
pytest
