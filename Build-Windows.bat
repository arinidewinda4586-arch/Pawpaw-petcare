@echo off
cd /d "%~dp0"
if not exist bin mkdir bin
javac -encoding UTF-8 -d bin src\pawpaw\*.java
if errorlevel 1 (
  echo Kompilasi gagal.
  pause
  exit /b 1
)
xcopy /E /I /Y resources\images bin\images >nul
