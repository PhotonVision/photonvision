@echo off
rem Run the Photon SC App in local development mode
set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%"
python "%SCRIPT_DIR%overlay\usr\local\bin\photon-sc-app\photon_sc_app.py" --local %*
