# Beryl AX (GL-MT3000): Phase 1 + 2 – Erste Schritte

Detaillierte Schritte, um den Router in Betrieb zu nehmen und den Zugang zu OpenWrt (SSH, Pakete) herzustellen. Danach können Sie den **Launcher** und das **Setup-Script** nutzen.

---

## Phase 1: Router in Betrieb nehmen

### 1.1 Auspacken und Strom

- Beryl AX auspacken, **USB-C-Netzteil** anschließen (Router hat keinen Akku).
- **Warten**, bis die LED(s) stabil sind (je nach Modell: Power/WLAN angezeigt).

### 1.2 Erste Verbindung

**Variante A – Kabel (einfach)**

- **LAN-Kabel** vom PC/Laptop in eine der LAN-Buchsen des Routers stecken (beim Beryl AX: 1× 2,5G-Port, 1× 1G WAN – für ersten Test z. B. 2,5G-Port nutzen).
- Der PC bekommt per DHCP eine IP (meist 192.168.8.x).

**Variante B – WLAN**

- Am Handy/PC nach dem **WLAN-Netz** des Routers suchen (SSID und Passwort stehen auf dem Aufkleber am Gerät oder in der Verpackung).
- Mit dem Netzwerk verbinden.

### 1.3 Admin-Oberfläche öffnen

1. **Browser** öffnen (Chrome, Firefox, Edge).
2. Adresse eingeben: **http://192.168.8.1** oder **http://192.168.1.200** (je nach Netzwerk – Ihre Router-IP steht oft in der GL.iNet-UI oder am Gerät).
   - Falls keine Seite lädt: prüfen, ob Sie mit dem Router-Netz verbunden sind; IP ggf. in der Anleitung oder im Router-Menü prüfen.
3. **Ersteinrichtungs-Assistent** (falls angezeigt):
   - **Admin-Passwort** setzen und gut notieren (für Web-UI und später SSH).
   - **Zeitzone** wählen (z. B. Europe/Berlin).
   - **WAN/Internet**: Wenn der Router später Internet braucht (für `opkg update`), hier WAN einrichten:
     - Kabel: oft automatisch (DHCP) am 2,5G- oder 1G-Port.
     - Oder „WLAN als WAN“: Router verbindet sich mit einem anderen WLAN und teilt es.

### 1.4 GL.iNet-UI vs. OpenWrt (LuCI)

- Die **Standard-Weboberfläche** ist die **GL.iNet-UI** (übersichtlich, wenig Optionen).
- **OpenWrt (LuCI)** ist die erweiterte Oberfläche. Bei GL.iNet oft erreichbar über:
  - Menü **„Erweiterte Einstellungen“** / **„Advanced“** oder
  - Direkt: **http://192.168.8.1/cgi-bin/luci** (wenn LuCI aktiviert ist).
- Für den **Launcher** und Paketinstallation wird **SSH** genutzt (Phase 2); LuCI ist optional, aber nützlich zum Durchstöbern.

---

## Phase 2: Zugang zu OpenWrt und Paketen

### 2.1 SSH aktivieren

1. In der **GL.iNet-Weboberfläche** einloggen (http://192.168.8.1).
2. Zu **Einstellungen** / **System** (oder **Settings** → **System**) gehen.
3. **SSH** suchen und **aktivieren**.
4. **Passwort** für SSH setzen (meist dasselbe wie das Admin-Passwort) oder **SSH-Key** hinterlegen, falls gewünscht.

### 2.2 Per SSH einloggen

**Unter Windows (PowerShell oder CMD):**

```bash
ssh root@192.168.1.200
```
(oder Ihre Router-IP, z. B. 192.168.8.1)

- Beim ersten Mal: Sicherheitsabfrage mit „yes“ bestätigen.
- **Passwort** eingeben (das in der GL.iNet-UI gesetzte).

**Unter Windows mit Git Bash / WSL:**

- Gleicher Befehl: `ssh root@192.168.8.1`

**Erfolg:** Sie sehen eine Eingabezeile wie `root@GL-MT3000:~#` – Sie sind auf dem Router.

### 2.3 Paketquellen prüfen

Im SSH-Fenster nacheinander ausführen:

```bash
opkg update
```

- Läuft ohne Fehler: Paketquellen sind erreichbar (Router hat dazu Internet/WAN benötigt).
- Fehler „Could not resolve …“: WAN/Internet prüfen (Phase 1.3).

**Optional – testweise ein Paket installieren:**

```bash
opkg install sqlite3-cli
```

- Wenn das klappt, funktioniert auch das **turmtechnik-setup.sh** (installiert u. a. SQLite und weitere Pakete).

### 2.4 Nützliche Befehle (zum Merken)

| Befehl | Bedeutung |
|--------|-----------|
| `opkg update` | Paketliste aktualisieren |
| `opkg list_installed` | Installierte Pakete anzeigen |
| `ls /mnt/` | Zeigt gemountete Laufwerke (z. B. USB-Stick als sda1) |
| `df -h` | Speicherplatz anzeigen |
| `logread` | System-Log anzeigen |

---

## Danach: Launcher und Setup-Script

- **Launcher** einmalig einrichten: siehe [README.md](README.md) → „Launcher einmalig auf dem Router einrichten“.
- **USB-Stick** mit `turmtechnik-setup.sh` vorbereiten und in der Launcher-Seite **„Installation starten“** klicken.

Wenn die Launcher-URL (http://192.168.8.1/cgi-bin/turmtechnik-launcher) bei Ihnen nicht erreichbar ist, können Sie das Setup-Script auch direkt per SSH starten, nachdem der USB-Stick eingesteckt ist:

```bash
sh /mnt/sda1/turmtechnik-setup.sh
```

(Der Mountpunkt kann `sda1`, `sdb1` oder `usb` heißen – mit `ls /mnt/` prüfen.)
