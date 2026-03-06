@echo off
REM Setzt JAVA_HOME auf Temurin 17 fuer diesen Build
set "JAVA_HOME=%USERPROFILE%\.jdks\temurin-17.0.17"
set "PATH=%JAVA_HOME%\bin;%PATH%"
call gradlew.bat %*
