@echo off
setlocal

:: To run any example, we want to use photonlib out of the source code in this repo.
:: Build the wheel first
pushd %~dp0..\photon-lib\py
if exist build rmdir /S /Q build
python setup.py bdist_wheel
popd

:: Add the output directory to PYTHONPATH to make sure it gets picked up ahead of any other installs
set PHOTONLIBPY_ROOT=%~dp0..\photon-lib\py
set PYTHONPATH=%PHOTONLIBPY_ROOT%

:: If an example to run is not provided, run all examples
if "%~1"=="" (
    echo No example provided, running all examples
    for /D %%d in (*) do (
        if (not "%%d"=="." if not "%%d"=="..") (
            echo Running example in %%d
            call "%~dp0run.bat" "%%d"
        )
    )
    exit /b 0
)

:: Move to to the right example folder
cd %~1

:: Run the example
robotpy sim
