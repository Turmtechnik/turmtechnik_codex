#!/bin/sh
# RustDesk-Server (hbbs + hbbr) per Docker starten.
# Auf dem Zielrechner ausführen (z. B. chmod +x rustdesk-server-docker.sh && ./rustdesk-server-docker.sh).

set -e

mkdir -p /overlay/rustdesk/data

docker rm -f hbbs hbbr 2>/dev/null || true

docker run -d --name hbbs \
  --net=host --restart unless-stopped \
  -v /overlay/rustdesk/data:/root \
  rustdesk/rustdesk-server hbbs

docker run -d --name hbbr \
  --net=host --restart unless-stopped \
  -v /overlay/rustdesk/data:/root \
  rustdesk/rustdesk-server hbbr

echo "RustDesk-Server gestartet (hbbs, hbbr)."
echo "Keys/ID in /overlay/rustdesk/data – für Clients als eigenes RustDesk-Netzwerk eintragen."
