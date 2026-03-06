# Turmtechnik-Setup für Beryl AX (GL-MT3000)

**Einfachste Variante:** Eine Datei hochladen, ein Befehl – fertig.

---

## Alles auf einmal aktuell halten: Deploy-Script

Damit auf dem Router **immer die gleichen, aktuellen Dateien** liegen wie im Projektordner:

1. **PowerShell** im Ordner `router-setup` öffnen (Rechtsklick im Explorer → „PowerShell hier öffnen“ oder `cd` in den Ordner).
2. Ausführen:
   ```powershell
   .\deploy-to-router.ps1 192.168.1.200
   ```
   (Router-IP weglassen = es wird 192.168.1.200 genutzt.)
3. Passwort für `root@192.168.1.200` eingeben (mehrmals, pro SCP/SSH-Aufruf).

Das Script kopiert auf den Router:

- **turmtechnik-api** und **turmtechnik-launcher** → `/www/cgi-bin/`
- **web-ui-index.html** → `/www/turmtechnik.html`
- **dist/public** → `/www/hauptuhr/` (Hauptuhr2025)
- **turmtechnik-setup.sh** → `/tmp/`
- bereinigt Zeilenenden (CRLF → LF) bei den CGI-Skripten auf dem Router.

**Hinweis:** Wenn PowerShell das Script blockiert: einmal `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass` ausführen oder `powershell -ExecutionPolicy Bypass -File deploy-to-router.ps1 192.168.1.200`. Wenn `scp` mit Fehlern abbricht (z. B. SFTP nicht verfügbar), die Dateien weiter per **WinSCP** manuell kopieren; das Deploy-Script nutzt `scp -O` (älteres Protokoll), das auf vielen Routern funktioniert.

---

## So geht’s (3 Schritte)

### 1. Script auf den Router kopieren

- **WinSCP** starten, verbinden: Host `192.168.1.200`, User `root`, Passwort eingeben.
- Links deinen Ordner öffnen:  
  `C:\Users\ThinkStation P920\Documents\Turmtechnik-studio3\router-setup`
- Rechts auf dem Router nach **`/tmp`** wechseln.
- Datei **`turmtechnik-setup.sh`** per Drag & Drop nach `/tmp` ziehen.

### 2. Per SSH einloggen und Script starten

- **SSH** öffnen (z. B. PuTTY oder `ssh root@192.168.1.200`).
- Falls beim ersten Versuch Fehler wie „not found“ oder „illegal option“ kommen (Windows-Zeilenenden): einmal Zeilenenden bereinigen:

```bash
sed -i 's/\r$//' /tmp/turmtechnik-setup.sh
```

- Dann ausführen:

```bash
sh /tmp/turmtechnik-setup.sh
```

- Warten, bis die Ausgabe durch ist (Pakete werden installiert, Ordner angelegt).

### 3. Fertig

Das war’s. Die Turmtechnik-Verzeichnisse liegen danach z. B. unter `/overlay/turmtechnik` (oder auf dem USB, wenn einer eingesteckt ist). Log: `/tmp/turmtechnik-setup.log`.

---

## Optional: Mit USB-Stick und „Klick im Browser“

Wenn du später **ohne SSH** nur per Browser klicken willst, kannst du den **Launcher** einrichten (eine Zusatzseite im Router). Das ist nicht nötig für die Grundinstallation – siehe unten „Launcher (optional)“.

---

## Was macht das Setup-Script?

- Aktualisiert die Paketquellen (`opkg update`)
- Installiert: block-mount, kmod-fs-ext4, e2fsprogs, ggf. sqlite3
- Legt an: `turmtechnik/`, `turmtechnik/db/`, `turmtechnik/config/`, `turmtechnik/logs/`

---

## Wenn etwas schiefgeht

- **Router braucht Internet** (WAN), damit `opkg update` und die Paket-Installation funktionieren.
- **Script mit LF-Zeilenenden** speichern (in Cursor unten rechts „LF“ wählen), sonst Fehler wie „illegal option“. Bereits hochgeladen? Auf dem Router: `sed -i 's/\r$//' /tmp/turmtechnik-setup.sh` ausführen, dann Script erneut starten.
- Log ansehen: `cat /tmp/turmtechnik-setup.log` (per SSH).

---

## Launcher (optional)

Nur wenn du die Installation später **per Klick im Browser** starten willst (z. B. mit USB-Stick):

1. **turmtechnik-launcher** per WinSCP nach `/www/cgi-bin/turmtechnik-launcher` kopieren.
2. Per SSH: `chmod 755 /www/cgi-bin/turmtechnik-launcher`
3. Datei mit **LF-Zeilenenden** (nicht CRLF), sonst „No such file or directory“.
4. Im Browser: `http://192.168.1.200/cgi-bin/turmtechnik-launcher` – dort „Installation starten“ (benötigt USB-Stick mit `turmtechnik-setup.sh`).

Ohne Launcher reicht: Script nach `/tmp` kopieren und `sh /tmp/turmtechnik-setup.sh` ausführen.

---

## Web-Server / API (optional)

Nach dem Setup kannst du eine **kleine Turmtechnik-Webseite und API** auf dem Router aktivieren:

1. **turmtechnik-api** per WinSCP nach **`/www/cgi-bin/turmtechnik-api`** kopieren (gleicher Ordner wie der Launcher).
2. Per SSH: `chmod 755 /www/cgi-bin/turmtechnik-api`
3. Datei mit **LF-Zeilenenden** speichern (sonst „No such file or directory“). Bereits hochgeladen? Auf dem Router: `sed -i 's/\r$//' /www/cgi-bin/turmtechnik-api` ausführen.
4. Im Browser öffnen: **http://192.168.1.200/cgi-bin/turmtechnik-api**

Dort siehst du ein kleines **Dashboard** mit Links zu:
- **API: Status** – JSON mit `status`, `service`, `version`, `time`
- **API: Config** – liest optional `/overlay/turmtechnik/config/config.json` (kann leer sein)
- **Setup / Installation starten** – Link zum Launcher

Die API ist minimal (ohne Datenbank) und kann später um weitere Endpunkte ergänzt werden (Programme, Platinen, …).

---

## Hauptuhr2025 aus dem Ordner `dist` einbinden

Im Ordner **`router-setup/dist`** liegt eine gebaute Web-App (**Hauptuhr2025**). So nimmst du sie auf dem Router in Betrieb:

1. **Inhalt von `dist/public` auf den Router kopieren**
   - Mit **WinSCP** verbinden, rechts in **`/www`** wechseln.
   - Auf dem PC den Ordner **`router-setup/dist/public`** öffnen.
   - Einen Ordner **`hauptuhr`** unter `/www` anlegen (rechts: Rechtsklick → Neu → Verzeichnis → `hauptuhr`).
   - **Inhalt** von `dist/public` in `/www/hauptuhr/` kopieren:
     - `index.html` → `/www/hauptuhr/index.html`
     - Ordner **`assets`** (mit `index-*.js` und `index-*.css`) → `/www/hauptuhr/assets/`
     - Falls vorhanden: `favicon.png` → `/www/hauptuhr/favicon.png`

2. **Im Browser aufrufen**
   - **http://192.168.1.200/hauptuhr/**  
   - oder **http://192.168.1.200/hauptuhr/index.html**

Die App nutzt den Basis-Pfad `/hauptuhr/` (steht so in der gebauten `index.html`). Wenn die App später eine API anspricht, muss die Router-API unter dem gleichen Host laufen oder die App so gebaut sein, dass sie die Turmtechnik-API unter `/cgi-bin/turmtechnik-api` verwendet.

---

## Übernahme aus dem Android-Projekt

Aus dem Turmtechnik-Android-Projekt übernommen bzw. abgeleitet:

- **schema-android.sql** – SQLite-Schema (alle Tabellen) aus `PlatinenDatabaseHelper.java`. Nutzbar auf dem PC oder auf dem Router, sobald SQLite verfügbar ist: `sqlite3 turmtechnik_config.db < schema-android.sql`
- **API_REFERENZ_ANDROID.md** – Liste aller API-Endpunkte der Android-App. Dient als Vorlage, um die Router-API schrittweise zu erweitern (z. B. `/api/platinen`, `/api/nebenuhren`, `/api/programme`).
- **web-ui-index.html** – Startseite der Web-UI (wie auf Android), mit zusätzlichem Block „Router / API“ (Links zu Turmtechnik-API und Setup). Auf dem Router unter `/www/` ablegen (z. B. als `turmtechnik.html`), dann im Browser `http://<Router-IP>/turmtechnik.html` aufrufen. Die Links zu `.html`-Unterseiten funktionieren erst, wenn diese auf dem Router ergänzt werden.

---

## Weitere Dateien

| Datei | Zweck |
|-------|--------|
| `turmtechnik-setup.sh` | **Das brauchst du** – einmal nach /tmp kopieren und ausführen. |
| `turmtechnik-api` | Optional: kleine Webseite + API (Status, Config) auf dem Router. |
| `turmtechnik-launcher` | Optional, für Browser-Button (siehe oben). |
| `schema-android.sql` | SQLite-Schema aus dem Android-Projekt (für Router/PC). |
| `API_REFERENZ_ANDROID.md` | API-Endpunkte der Android-App (Referenz für Router-API). |
| `web-ui-index.html` | Startseite Web-UI (Android-Look), für Router unter /www/. |
| `deploy-to-router.ps1` | **Deploy:** Alle aktuellen Dateien mit einem Befehl auf den Router kopieren. |
| `BERYL_AX_PHASE1_2.md` | Detaillierte Schritte: Router einrichten, SSH aktivieren. |
