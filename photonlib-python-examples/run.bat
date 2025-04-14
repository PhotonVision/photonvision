@echo off
setlocal

:: Check if the first argument is provided
if "%~1"=="" (
    echo Error: No example-to-run provided.
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

:: Move to to the right example folder
cd %~1

:: Run the example
robotpy sim
