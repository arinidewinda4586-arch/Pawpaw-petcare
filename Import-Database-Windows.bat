@echo off
setlocal
cd /d "%~dp0"

set "NO_PAUSE=0"
if /i "%~1"=="--no-pause" set "NO_PAUSE=1"

set "MYSQL_BIN="
if exist "D:\XAMPP\mysql\bin\mysql.exe" (
  set "MYSQL_BIN=D:\XAMPP\mysql\bin\mysql.exe"
) else if exist "C:\xampp\mysql\bin\mysql.exe" (
  set "MYSQL_BIN=C:\xampp\mysql\bin\mysql.exe"
) else (
  for /f "delims=" %%I in ('where mysql.exe 2^>nul') do (
    if not defined MYSQL_BIN set "MYSQL_BIN=%%I"
  )
)

if not defined MYSQL_BIN (
  echo [GAGAL] mysql.exe tidak ditemukan di D:\XAMPP\mysql\bin, C:\xampp\mysql\bin, maupun PATH.
  if "%NO_PAUSE%"=="0" pause
  exit /b 1
)

if not exist "database\pet_care.sql" (
  echo [GAGAL] Berkas database\pet_care.sql tidak ditemukan.
  if "%NO_PAUSE%"=="0" pause
  exit /b 1
)

echo Mengimpor database\pet_care.sql menggunakan "%MYSQL_BIN%"...
"%MYSQL_BIN%" --default-character-set=utf8mb4 -u root < "database\pet_care.sql"
set "IMPORT_ERR=%ERRORLEVEL%"

if not "%IMPORT_ERR%"=="0" (
  echo [GAGAL] Import database gagal dengan kode %IMPORT_ERR%. Pastikan MySQL berjalan di 127.0.0.1:3306.
  if "%NO_PAUSE%"=="0" pause
  exit /b %IMPORT_ERR%
)

echo [SUKSES] Database pet_care berhasil diimpor.
if "%NO_PAUSE%"=="0" pause
exit /b 0
