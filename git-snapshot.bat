@echo off
REM Git-Snapshot: add + commit + push. Doppelklick oder "git-snapshot.bat [Nachricht]"
REM Zeitplan: Aufgabenplanung -> Aktion "Starten": Pfad zu git-snapshot.bat, Argument: auto
chcp 65001 >nul
cd /d "%~dp0"

set SILENT=0
if /i "%~1"=="auto" set SILENT=1
if /i "%~1"=="silent" set SILENT=1
if %SILENT%==1 (set MSG=Auto-Snapshot %date% %time%) else (set MSG=Snapshot %date% %time%)
if not "%~1"=="" if %SILENT%==0 set MSG=%~1

echo [Git] Add ...
git add .
if errorlevel 1 ( echo Fehler bei git add. & if %SILENT%==0 pause & exit /b 1 )

echo [Git] Commit: %MSG%
git commit -m "%MSG%" 2>nul
if errorlevel 1 (
    echo Keine Änderungen zum Committen oder Fehler.
) else (
    echo [Git] Push ...
    git push origin master 2>nul
    if errorlevel 1 echo Push fehlgeschlagen (z.B. keine Anmeldung). Commit ist lokal gespeichert.
)

echo Fertig.
if %SILENT%==0 pause
