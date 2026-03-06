#!/bin/sh
# Turmtechnik-Setup für Beryl AX (GL-MT3000) / OpenWrt
# Dieses Script auf USB-Stick legen und per Launcher oder SSH starten.
# Verwendung: sh /mnt/sda1/turmtechnik-setup.sh

set -e
LOG="/tmp/turmtechnik-setup.log"
exec > "$LOG" 2>&1

echo "=== Turmtechnik-Setup gestartet: $(date) ==="

# 1) Paketquellen aktualisieren
echo "[1/5] opkg update..."
opkg update || true

# 2) Basis-Pakete (USB-Mount, Dateisystem)
echo "[2/5] Installiere Pakete..."
for pkg in block-mount kmod-fs-ext4 e2fsprogs; do
    if opkg list_installed | grep -q "^$pkg "; then
        echo "  $pkg bereits installiert."
    else
        opkg install "$pkg" --force-overwrite || echo "  Warnung: $pkg konnte nicht installiert werden."
    fi
done

# SQLite: Paketname je nach OpenWrt (sqlite3, sqlite3-cli oder nicht verfügbar)
if command -v sqlite3 >/dev/null 2>&1; then
    echo "  sqlite3 bereits vorhanden."
else
    for pkg in sqlite3 sqlite3-cli; do
        if opkg install "$pkg" 2>/dev/null; then
            echo "  $pkg installiert."
            break
        fi
    done
    command -v sqlite3 >/dev/null 2>&1 || echo "  Hinweis: SQLite nicht installiert (optional, für spätere DB)."
fi

# 3) USB-Mount ermitteln (typisch sda1 oder vom Launcher übergeben)
USB_MOUNT=""
for d in /mnt/sda1 /mnt/sdb1 /mnt/usb /overlay; do
    if [ -d "$d" ] && [ -w "$d" ]; then
        USB_MOUNT="$d"
        break
    fi
done
if [ -z "$USB_MOUNT" ]; then
    USB_MOUNT="/overlay"
    echo "  Kein USB-Mount gefunden, nutze $USB_MOUNT"
fi
echo "[3/5] Speicher: $USB_MOUNT"

# 4) Turmtechnik-Verzeichnis anlegen
TURMDIR="$USB_MOUNT/turmtechnik"
mkdir -p "$TURMDIR"
mkdir -p "$TURMDIR/db"
mkdir -p "$TURMDIR/config"
mkdir -p "$TURMDIR/logs"
echo "[4/5] Verzeichnisse: $TURMDIR"

# 5) Hinweis für spätere Nutzung (Datenbank, Web-UI)
echo "[5/5] Grundeinstellung abgeschlossen."
echo ""
echo "Nächste Schritte (manuell oder in späterer Version):"
echo "  - Datenbank anlegen in: $TURMDIR/db/"
echo "  - Web-Server/API konfigurieren"
echo "  - Optional: WireGuard/OpenVPN in der GL.iNet-UI aktivieren"
echo ""
echo "=== Turmtechnik-Setup beendet: $(date) ==="
