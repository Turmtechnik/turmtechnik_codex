@echo off
cd /d "%~dp0"
REM Java fuer Gradle (wie build-mit-java.bat)
set "JAVA_HOME=%USERPROFILE%\.jdks\temurin-17.0.17"
set "PATH=%JAVA_HOME%\bin;%PATH%"
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo JAVA_HOME nicht gefunden: %JAVA_HOME%
    echo Bitte Java 17 installieren oder Pfad in compile.bat anpassen.
    pause
    exit /b 1
)
echo Kompiliere Turmtechnik-studio3 (assembleDebug)...
echo.
call gradlew.bat assembleDebug
echo.
echo Exit-Code: %ERRORLEVEL%
pause
