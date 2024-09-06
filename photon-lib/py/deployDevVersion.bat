@echo off
setlocal

:: Check if the first argument is provided
if "%~1"=="" (
    echo Error: No example-to-deploy provided.
    exit /b 1
)

:: To run any example, we want to use photonlib out of the source code in this repo.
:: Build the wheel first
pushd %~dp0..\photon-lib\py
if exist build rmdir /S /Q build
python setup.py bdist_wheel
popd

:: Add the output directory to PYTHONPATH to make sure it gets picked up ahead of any other installs
set PHOTONLIBPY_ROOT=%~dp0..\photon-lib\py
set PYTHONPATH=%PHOTONLIBPY_ROOT%

:: make sure pip knows what we just built so it can sync/deploy it
python -m pip config --site set global.find-links %~dp0..\photon-lib\py\dist

:: Move to to the right example folder
pushd %~1

:: Deploy the example
python -m robotpy sync
python -m robotpy deploy

:: clean up
popd
python -m pip config --site unset global.find-links
