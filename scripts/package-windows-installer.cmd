@echo off
powershell -ExecutionPolicy Bypass -File "%~dp0package-windows-installer.ps1" %*
