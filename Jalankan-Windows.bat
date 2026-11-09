@echo off
cd /d "%~dp0"
call Build-Windows.bat
if errorlevel 1 exit /b 1
java -cp "bin;resources;lib/*" pawpaw.MainApp
