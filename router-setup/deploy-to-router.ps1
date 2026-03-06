# Deploy Turmtechnik-Dateien auf den Beryl AX Router
# Nutzung: .\deploy-to-router.ps1 [Router-IP]
# Beispiel: .\deploy-to-router.ps1 192.168.1.200
# Voraussetzung: SSH/SCP (OpenSSH) und Zugriff auf den Router (root).

param(
    [string]$RouterIP = "192.168.1.200"
)

$ErrorActionPreference = "Stop"
$scriptDir = $PSScriptRoot
Set-Location $scriptDir

Write-Host "Deploy auf Router: root@$RouterIP" -ForegroundColor Cyan
Write-Host ""

# 1) CGI-Skripte nach /www/cgi-bin/
Write-Host "[1/4] CGI-Skripte (turmtechnik-api, turmtechnik-launcher)..." -ForegroundColor Yellow
scp -O "$scriptDir\turmtechnik-api" "root@${RouterIP}:/www/cgi-bin/"
scp -O "$scriptDir\turmtechnik-launcher" "root@${RouterIP}:/www/cgi-bin/"
ssh "root@$RouterIP" "chmod 755 /www/cgi-bin/turmtechnik-api /www/cgi-bin/turmtechnik-launcher; sed -i 's/\r$//' /www/cgi-bin/turmtechnik-api /www/cgi-bin/turmtechnik-launcher 2>/dev/null || true"
Write-Host "  OK" -ForegroundColor Green

# 2) Startseite Turmtechnik nach /www/
Write-Host "[2/4] Startseite (web-ui-index.html -> turmtechnik.html)..." -ForegroundColor Yellow
scp -O "$scriptDir\web-ui-index.html" "root@${RouterIP}:/www/turmtechnik.html"
Write-Host "  OK" -ForegroundColor Green

# 3) Hauptuhr2025 (dist/public) nach /www/hauptuhr/
Write-Host "[3/4] Hauptuhr2025 (dist/public)..." -ForegroundColor Yellow
if (Test-Path "dist\public") {
    scp -r -O "dist\public" "root@${RouterIP}:/tmp/public"
    ssh "root@$RouterIP" "rm -rf /www/hauptuhr; mv /tmp/public /www/hauptuhr"
    Write-Host "  OK" -ForegroundColor Green
} else {
    Write-Host "  dist/public nicht gefunden, uebersprungen." -ForegroundColor Gray
}

# 4) Setup-Script nach /tmp (optional, fuer manuellen Lauf)
Write-Host "[4/4] Setup-Script (turmtechnik-setup.sh -> /tmp)..." -ForegroundColor Yellow
scp -O "$scriptDir\turmtechnik-setup.sh" "root@${RouterIP}:/tmp/"
ssh "root@$RouterIP" "sed -i 's/\r$//' /tmp/turmtechnik-setup.sh 2>/dev/null || true"
Write-Host "  OK" -ForegroundColor Green

Write-Host ""
Write-Host "Fertig. Aufrufe im Browser:" -ForegroundColor Cyan
Write-Host "  Startseite:     http://${RouterIP}/turmtechnik.html"
Write-Host "  Hauptuhr2025:   http://${RouterIP}/hauptuhr/"
Write-Host "  API / Status:   http://${RouterIP}/cgi-bin/turmtechnik-api"
Write-Host "  Setup-Launcher: http://${RouterIP}/cgi-bin/turmtechnik-launcher"
