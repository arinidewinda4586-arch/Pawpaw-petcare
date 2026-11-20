@echo off
setlocal
cd /d "%~dp0"
if not exist bin mkdir bin

dir /s /b src\main\java\*.java > sources.txt 2>nul
if errorlevel 1 (
  del /q sources.txt 2>nul
  echo Tidak ada berkas sumber Java ditemukan di src\main\java.
  exit /b 1
)

javac -encoding UTF-8 -cp "lib/*" -d bin @sources.txt
set "COMPILE_ERR=%ERRORLEVEL%"
del /q sources.txt 2>nul
if not "%COMPILE_ERR%"=="0" (
  echo Kompilasi gagal.
  exit /b %COMPILE_ERR%
)

if exist src\main\resources (
  xcopy /E /I /Y src\main\resources bin >nul
  if errorlevel 1 (
    echo Gagal menyalin resources ke bin.
    exit /b 1
  )
)

exit /b 0
