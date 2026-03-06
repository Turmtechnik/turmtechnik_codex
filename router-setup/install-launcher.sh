#!/bin/sh
# Einmal-Installation des Turmtechnik-Launchers auf dem Beryl AX (OpenWrt).
# Auf dem PC ausführen: ./install-launcher.sh [ROUTER_IP]
# Oder: Script auf den Router kopieren und dort ausführen (dann muss der Launcher
#       und ggf. turmtechnik-launcher bereits unter /tmp liegen).

ROUTER_IP="${1:-192.168.8.1}"
WWW_CGI="/www/cgi-bin"
LAUNCHER_NAME="turmtechnik-launcher"

echo "Installation des Turmtechnik-Launchers auf $ROUTER_IP"
echo "Stellen Sie sicher, dass Sie per SSH (ssh root@$ROUTER_IP) Zugriff haben."
echo ""

# Wenn wir auf dem PC laufen: Datei per SCP hochladen und per SSH ausführen
if [ -f "turmtechnik-launcher" ]; then
    echo "Lade turmtechnik-launcher hoch..."
    scp turmtechnik-launcher "root@${ROUTER_IP}:/tmp/"
    echo "Richte Launcher auf dem Router ein..."
    ssh "root@${ROUTER_IP}" "
        mkdir -p $WWW_CGI && \
        cp /tmp/turmtechnik-launcher $WWW_CGI/$LAUNCHER_NAME && \
        chmod 755 $WWW_CGI/$LAUNCHER_NAME && \
        echo 'Launcher installiert unter $WWW_CGI/$LAUNCHER_NAME'
    "
    echo ""
    echo "Fertig. Im Browser aufrufen: http://${ROUTER_IP}/cgi-bin/$LAUNCHER_NAME"
else
    echo "Hinweis: Dieses Script von einem PC aus ausführen (aus dem Ordner router-setup),"
    echo "damit turmtechnik-launcher per SCP hochgeladen werden kann."
    echo ""
    echo "Alternativ manuell auf dem Router (per SSH):"
    echo "  1. turmtechnik-launcher nach /tmp kopieren (z.B. per SCP)."
    echo "  2. mkdir -p $WWW_CGI"
    echo "  3. cp /tmp/turmtechnik-launcher $WWW_CGI/$LAUNCHER_NAME"
    echo "  4. chmod 755 $WWW_CGI/$LAUNCHER_NAME"
    echo "  5. Im Browser: http://<ROUTER-IP>/cgi-bin/$LAUNCHER_NAME"
fi
