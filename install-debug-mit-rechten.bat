@echo off
setlocal

set "ADB=%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe"
set "APK=%~dp0build\outputs\apk\debug\Turmtechnik-studio3-debug.apk"
set "PACKAGE=tom.turmtechnik"

if not exist "%ADB%" (
  echo ADB nicht gefunden: %ADB%
  exit /b 1
)

if not exist "%APK%" (
  echo APK nicht gefunden: %APK%
  exit /b 1
)

echo Installiere Debug-APK...
"%ADB%" install -r "%APK%" || exit /b 1

echo Setze WRITE_SECURE_SETTINGS...
"%ADB%" shell pm grant %PACKAGE% android.permission.WRITE_SECURE_SETTINGS || exit /b 1

echo Starte App...
"%ADB%" shell am start -n %PACKAGE%/.WebUiActivity >nul

echo Fertig.
endlocal
