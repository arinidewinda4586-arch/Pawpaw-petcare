@echo off
cd /d "%~dp0"
call Build-Windows.bat
if errorlevel 1 exit /b %ERRORLEVEL%
java -cp "bin;lib/*" pawpaw.Main
exit /b %ERRORLEVEL%
