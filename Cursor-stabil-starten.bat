@echo off
REM Cursor mit deaktivierter GPU starten - reduziert Abstuerze bei Speicher/GPU-Problemen
REM Siehe CURSOR_SPEICHER_ANLEITUNG.md

set NODE_OPTIONS=--max-old-space-size=16384

REM Typische Cursor-Installation unter Windows:
set CURSOR_EXE=%LOCALAPPDATA%\Programs\cursor\Cursor.exe
if exist "%CURSOR_EXE%" (
    start "" "%CURSOR_EXE%" --disable-gpu "%~dp0"
) else (
    REM Fallback: cursor aus PATH (wenn im PATH)
    where cursor >nul 2>&1
    if %errorlevel% equ 0 (
        start "" cursor --disable-gpu "%~dp0"
    ) else (
        echo Cursor.exe nicht gefunden. Bitte Pfad in dieser .bat anpassen.
        echo Ueblicher Pfad: %LOCALAPPDATA%\Programs\cursor\Cursor.exe
        pause
    )
)
