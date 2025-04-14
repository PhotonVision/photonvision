@echo off
setlocal

python -m pip config set global.find-links %~dp0\dist
