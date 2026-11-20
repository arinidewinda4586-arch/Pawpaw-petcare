@echo off
setlocal
cd /d "%~dp0"

call Build-Windows.bat
if errorlevel 1 exit /b %ERRORLEVEL%

if not exist bin-test mkdir bin-test

dir /s /b src\test\java\*.java > test-sources.txt 2>nul
if errorlevel 1 (
  del /q test-sources.txt 2>nul
  echo Tidak ada berkas test ditemukan di src\test\java.
  exit /b 1
)

javac -encoding UTF-8 -cp "bin;lib/*" -d bin-test @test-sources.txt
set "COMPILE_ERR=%ERRORLEVEL%"
del /q test-sources.txt 2>nul
if not "%COMPILE_ERR%"=="0" (
  echo Kompilasi test gagal.
  exit /b %COMPILE_ERR%
)

if not "%~1"=="" goto :run_single

for %%T in (
  pawpaw.BuildLayoutTest
  pawpaw.CoreUtilAndConfigTest
  pawpaw.AuthAndProfileServiceTest
  pawpaw.AdoptionServiceTest
  pawpaw.ShopServiceTest
  pawpaw.HealthAndDashboardServiceTest
  pawpaw.ShellAndAuthUiSmokeTest
  pawpaw.UserCorePanelsSmokeTest
  pawpaw.UserShopHealthPanelsSmokeTest
  pawpaw.AdminMasterPanelsSmokeTest
  pawpaw.AdminTransactionPanelsSmokeTest
) do (
  java -cp "bin-test;bin;lib/*" %%T
  if errorlevel 1 (
    echo [GAGAL] Test suite %%T gagal.
    exit /b 1
  )
)
echo [SUKSES] Seluruh 11 test suite lulus.
exit /b 0

:run_single
java -cp "bin-test;bin;lib/*" %~1
exit /b %ERRORLEVEL%
