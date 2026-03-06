# Turmtechnik Android App - Vollständige Projekt-Dokumentation

## Inhaltsverzeichnis

### Teil I: Einleitung und Übersicht
1. [Projektbeschreibung](#1-projektbeschreibung)
2. [Zielgruppe](#2-zielgruppe)
3. [Systemanforderungen](#3-systemanforderungen)
4. [Übersicht der Hauptfunktionen](#4-übersicht-der-hauptfunktionen)

### Teil II: Installation und Einrichtung (für Installateure)
5. [Installation der Android-App](#5-installation-der-android-app)
6. [Erste Konfiguration](#6-erste-konfiguration)
7. [Datenbank-Setup](#7-datenbank-setup)
8. [Excel-Dateien Import](#8-excel-dateien-import)
9. [Web-UI Zugriff einrichten](#9-web-ui-zugriff-einrichten)
10. [Berechtigungen](#10-berechtigungen)
11. [Netzwerk-Konfiguration](#11-netzwerk-konfiguration)

### Teil III: Funktionen und Konfiguration
12. [Programmsteuerung](#12-programmsteuerung)
13. [Feiertage-Verwaltung](#13-feiertage-verwaltung)
14. [Platinen-Konfiguration](#14-platinen-konfiguration)
15. [Nebenuhr-Konfiguration](#15-nebenuhr-konfiguration)
16. [Vorschwingen-Konfiguration](#16-vorschwingen-konfiguration)
17. [Relais-Verwaltung](#17-relais-verwaltung)
18. [Melodien-Verwaltung](#18-melodien-verwaltung)
19. [Zeitsynchronisation](#19-zeitsynchronisation)
20. [Schlagwerk](#20-schlagwerk)
21. [Heizung](#21-heizung)
22. [Web-UI Konfiguration](#22-web-ui-konfiguration)
23. [Datenbank-Migration](#23-datenbank-migration)
24. [System-Konfiguration](#24-system-konfiguration)

### Teil IV: Benutzerhandbuch (für Anwender)
25. [Grundfunktionen](#25-grundfunktionen)
26. [Programm-Abfrage](#26-programm-abfrage)
27. [Manuelle Steuerung](#27-manuelle-steuerung)
28. [Konfiguration über Web-UI](#28-konfiguration-über-web-ui)

### Teil V: Technische Details (für Installateure)
29. [Architektur](#29-architektur)
30. [Dateistruktur](#30-dateistruktur)
31. [API-Dokumentation](#31-api-dokumentation)

### Teil VI: Anhänge
32. [Anhang A: Excel-Datei-Format](#32-anhang-a-excel-datei-format)
33. [Anhang B: Datenbank-Schema](#33-anhang-b-datenbank-schema)
34. [Anhang C: API-Referenz](#34-anhang-c-api-referenz)
35. [Anhang D: Glossar](#35-anhang-d-glossar)
36. [Anhang E: Fehlerbehebung](#36-anhang-e-fehlerbehebung)

---

## 1. Projektbeschreibung

Die **Turmtechnik Android App** ist eine umfassende Steuerungssoftware für Kirchturmtechnik-Anlagen. Die App ermöglicht die vollautomatische Steuerung von Glocken, Schlagwerken, Nebenuhren, Heizungen und weiteren Komponenten einer Turmtechnik-Anlage.

### Hauptmerkmale:
- **Automatische Programmsteuerung** mit Zeitplanung, Wochentags-Filtern und Prioritäten
- **Feiertagsverwaltung** mit Osterfeiertagen, fixen Feiertagen und Sonderfeiertagen
- **Platinen-Konfiguration** für Bluetooth- und WiFi-Module (Carambola)
- **Nebenuhr-Steuerung** für bis zu 4 Nebenuhren (A, B, C, D) inkl. Monduhr
- **Vorschwingen-Konfiguration** für Glocken-Vorschwingen vor dem Läuten
- **Web-UI** für Remote-Konfiguration über Browser
- **Datenbank-Integration** mit SQLite für persistente Speicherung
- **Excel-Integration** für Import/Export von Konfigurationsdaten

### Technologie-Stack:
- **Plattform:** Android (Java)
- **Datenbank:** SQLite
- **Web-Server:** Embedded HTTP Server (SimpleHttpServer)
- **Datenformat:** Excel (.xls) für Import/Export, JSON für API

---

## 2. Zielgruppe

### Installateure
Diese Dokumentation enthält technische Details für:
- System-Installateure
- Wartungspersonal
- Entwickler
- Systemadministratoren

**Relevante Kapitel:** Teil II (Installation), Teil V (Technische Details), Teil VI (Anhänge)

### Anwender
Diese Dokumentation enthält Bedienungsanleitungen für:
- Endbenutzer
- Bediener der Anlage
- Personen, die Programme konfigurieren

**Relevante Kapitel:** Teil IV (Benutzerhandbuch), Teil III (Funktionen)

---

## 3. Systemanforderungen

### Hardware-Anforderungen:
- **Android-Gerät:** Tablet oder Smartphone
- **Android-Version:** Mindestens Android 5.0 (API Level 21)
- **Speicherplatz:** Mindestens 100 MB frei
- **RAM:** Mindestens 512 MB empfohlen
- **Netzwerk:** WiFi oder Mobilfunk für Web-UI Zugriff (optional)

### Software-Anforderungen:
- **Android Betriebssystem:** 5.0 oder höher
- **Java Runtime:** In Android integriert
- **Berechtigungen:** 
  - Internet-Zugriff
  - Bluetooth (optional)
  - WRITE_SECURE_SETTINGS (für Zeitsynchronisation, über ADB)

### Zusätzliche Hardware (optional):
- **Carambola-Module:** Für WiFi/Bluetooth-Kommunikation
- **GPS-Modul:** Für Zeitsynchronisation
- **NMEA-GPS-Interface:** Für präzise Zeitsynchronisation

---

## 4. Übersicht der Hauptfunktionen

### 4.1 Programmsteuerung
- Automatische Ausführung von Programmen zu festgelegten Zeiten
- Unterstützung für Normalprogramm, Feiertagsprogramme und Benutzerprogramme
- Sonnenaufgang/Sonnenuntergang-Integration (SA/SU)
- Wochentags-Filterung
- Prioritäts-System (1-9)
- Verknüpfte Tasten
- Periodisch (Sommer/Winter)

### 4.2 Feiertage-Verwaltung
- **Osterfeiertage:** Variable Feiertage, die sich nach dem Osterdatum richten
- **Fixe Feiertage:** Feste Feiertage mit festem Datum (z.B. Weihnachten)
- **Sonderfeiertage:** Verschobene Feiertage (auf Sonntag)
- **Vorfeiertag:** Automatische Erstellung des Tages vor einem Feiertag
- Dynamische Berechnung von Sonderfeiertagen

### 4.3 Platinen-Konfiguration
- Konfiguration von Carambola-Modulen
- Bluetooth- und WiFi-Modus
- IP-Adressen und Ports
- MAC-Adressen
- Relais-Anzahl pro Platine
- Online-Status-Anzeige
- Scan-Relais und Antwortzeit

### 4.4 Nebenuhr-Konfiguration
- 4 Nebenuhren (A, B, C, D)
- Monduhr (D) mit Mondphasen-Anzeige
- Wechselschaltung (Relais A/B)
- Impuls-Längen und Pause-Zeiten (in Sekunden)
- 12-Stunden-Modus
- Layout-Konfiguration

### 4.5 Vorschwingen-Konfiguration
- Vorschwingen-Relais und Läuten-Relais
- Vorschwingzeit in Sekunden
- Berechnete Relais-Nummern
- Dropdown-Auswahl für Schwingen/Melodie-Tasten

### 4.6 Web-UI
- Remote-Konfiguration über Browser
- REST API für alle Konfigurationsbereiche
- Responsive Design (Desktop, Tablet, Smartphone)
- Keine App-Updates für UI-Änderungen nötig

### 4.7 Datenbank-Integration
- SQLite-Datenbank für persistente Speicherung
- Migration von Excel-Dateien zur Datenbank
- Backup und Wiederherstellung
- Vollständige CRUD-Operationen

---

## 5. Installation der Android-App

### 5.1 APK-Installation

1. **APK-Datei übertragen:**
   - Übertragen Sie die `.apk` Datei auf das Android-Gerät
   - Per USB, E-Mail oder Cloud-Speicher

2. **Installation starten:**
   - Öffnen Sie die APK-Datei auf dem Gerät
   - Bestätigen Sie die Installation von "Unbekannten Quellen" (falls nötig)
   - Folgen Sie den Installationsanweisungen

3. **App starten:**
   - Öffnen Sie die App "Turmtechnik" aus dem App-Menü
   - Die App startet im Landscape-Modus (Querformat)

### 5.2 Erste Schritte nach Installation

1. **Berechtigungen erteilen:**
   - Die App fragt nach notwendigen Berechtigungen
   - Erteilen Sie alle angeforderten Berechtigungen

2. **WRITE_SECURE_SETTINGS Permission:**
   - Diese Permission muss über ADB vergeben werden (siehe Kapitel 10)

3. **Excel-Dateien kopieren:**
   - Kopieren Sie alle Excel-Dateien in das Verzeichnis `/sdcard/Turmtechnik/`
   - Erforderliche Dateien:
     - `System.xls`
     - `Normalprogramm.xls`
     - `Beschriftung-Tasten.xls`
     - Weitere Programm-Dateien (Feiertage, etc.)

4. **Datenbank initialisieren:**
   - Die Datenbank wird beim ersten Start automatisch erstellt
   - Verwenden Sie die Web-UI, um Daten aus Excel zu importieren

---

## 6. Erste Konfiguration

### 6.1 Grundkonfiguration

Nach der Installation müssen folgende Schritte durchgeführt werden:

1. **Platinen konfigurieren:**
   - Öffnen Sie die Web-UI (siehe Kapitel 9)
   - Navigieren Sie zu "Platinen-Konfiguration"
   - Fügen Sie alle Platinen hinzu (IP-Adressen, Ports, MAC-Adressen)

2. **Programme importieren:**
   - Navigieren Sie zu "Programm-Editor" in der Web-UI
   - Klicken Sie auf "Import Excel Programmtage"
   - Warten Sie, bis der Import abgeschlossen ist

3. **Feiertage konfigurieren:**
   - Osterfeiertage: Werden automatisch erstellt, können aber angepasst werden
   - Fixe Feiertage: Importieren Sie aus Excel oder fügen Sie manuell hinzu
   - Sonderfeiertage: Werden automatisch berechnet, wenn Verschiebung aktiviert ist

4. **Nebenuhren konfigurieren:**
   - Navigieren Sie zu "Nebenuhr Config"
   - Konfigurieren Sie Nebenuhr A, B, C und D (Monduhr)
   - Setzen Sie Relais-Nummern, Impuls-Längen und Pause-Zeiten

### 6.2 System-Einstellungen

1. **Autostart aktivieren:**
   - Die App startet automatisch beim Boot des Geräts
   - Kann in den System-Einstellungen deaktiviert werden

2. **Zeitsynchronisation:**
   - Konfigurieren Sie Zeitserver oder GPS
   - Aktivieren Sie die automatische Zeitsynchronisation

3. **Power off bei % Akku:**
   - Setzen Sie den Schwellwert für automatisches Abschalten
   - Standard: 75%

---

## 7. Datenbank-Setup

### 7.1 Datenbank-Erstellung

Die Datenbank wird automatisch beim ersten Start der App erstellt:

- **Datenbankname:** `turmtechnik_config.db`
- **Speicherort:** `/data/data/tom.turmtechnik/databases/turmtechnik_config.db`
- **Version:** 12 (wird automatisch aktualisiert)

### 7.2 Datenbank-Tabellen

Die Datenbank enthält folgende Tabellen:

1. **platinen_config** - Platinen-Konfiguration
2. **io_config** - I/O-Konfiguration (Key-Value-Paare)
3. **nebenuhr_config** - Nebenuhr-Konfiguration (A, B, C, D)
4. **tagtypen** - Tagtypen (Normalprogramm, Feiertage, etc.)
5. **programme** - Programme für alle Tagtypen
6. **osterfeiertage** - Osterfeiertage (variable Feiertage)
7. **feste_feiertage** - Fixe Feiertage
8. **sonderfeiertage** - Sonderfeiertage (verschobene Feiertage)
9. **vorschwingen_config** - Vorschwingen-Konfiguration

### 7.3 Datenbank-Migration

Bei App-Updates wird die Datenbank automatisch migriert:

- **onUpgrade()** Methode führt Migrationen durch
- Neue Spalten werden automatisch hinzugefügt
- Bestehende Daten bleiben erhalten

### 7.4 Datenbank-Backup

**Manuelles Backup:**
```bash
adb pull /data/data/tom.turmtechnik/databases/turmtechnik_config.db backup.db
```

**Wiederherstellung:**
```bash
adb push backup.db /data/data/tom.turmtechnik/databases/turmtechnik_config.db
```

---

## 8. Excel-Dateien Import

### 8.1 Erforderliche Excel-Dateien

Folgende Excel-Dateien müssen vorhanden sein:

1. **System.xls** - System-Konfiguration
   - Sheet 14: Platinen-Konfiguration
   - Sheet 15: Nebenuhr-Konfiguration
   - Sheet 9: Vorschwingen-Konfiguration
   - Sheet 12: Osterfeiertage
   - Sheet 13: Fixe Feiertage

2. **Normalprogramm.xls** - Normalprogramm

3. **Beschriftung-Tasten.xls** - Tasten-Beschriftungen und Relais-Zuordnung

4. **Weitere Programm-Dateien:**
   - Feiertagsprogramme (z.B. `Weihnachten.xls`)
   - Benutzerprogramme

### 8.2 Import über Web-UI

1. **Programme importieren:**
   - Öffnen Sie die Web-UI
   - Navigieren Sie zu "Programm-Editor"
   - Klicken Sie auf "Import Excel Programmtage"
   - Warten Sie, bis der Import abgeschlossen ist

2. **Feiertage importieren:**
   - **Osterfeiertage:** Klicken Sie auf "Aus Excel importieren" in der Osterfeiertage-Seite
   - **Fixe Feiertage:** Klicken Sie auf "Aus Excel importieren" in der Fixe Feiertage-Seite

3. **Vorschwingen importieren:**
   - Navigieren Sie zu "Vorschwingen Config"
   - Klicken Sie auf "Aus Excel importieren"

### 8.3 Import-Status

Nach dem Import:
- Programme werden in der Datenbank gespeichert
- Excel-Dateien bleiben als Backup erhalten
- Die App verwendet primär die Datenbank

---

## 9. Web-UI Zugriff einrichten

### 9.1 Web-Server starten

Der Web-Server startet automatisch beim Start der App:

- **Port:** 8080 (Standard)
- **Zugriff:** `http://<IP-Adresse>:8080`
- **Lokal:** `http://localhost:8080` (vom Android-Gerät aus)

### 9.2 IP-Adresse ermitteln

**Auf dem Android-Gerät:**
1. Öffnen Sie "Einstellungen" → "WLAN"
2. Tippen Sie auf das verbundene Netzwerk
3. Notieren Sie die IP-Adresse (z.B. `192.168.1.100`)

**Alternative:**
- Die IP-Adresse wird in der App angezeigt (falls implementiert)
- Oder über ADB: `adb shell ip addr show wlan0`

### 9.3 Zugriff vom PC/Tablet

1. **Stellen Sie sicher, dass Geräte im gleichen Netzwerk sind:**
   - Android-Gerät und PC/Tablet müssen im gleichen WiFi-Netzwerk sein

2. **Öffnen Sie Browser:**
   - Geben Sie ein: `http://<IP-Adresse>:8080`
   - Beispiel: `http://192.168.1.100:8080`

3. **Hauptmenü:**
   - Sie sehen das Hauptmenü mit allen verfügbaren Konfigurationsseiten

### 9.4 Browser-Kompatibilität

**Empfohlene Browser:**
- Chrome (empfohlen)
- Firefox
- Edge
- Safari (iOS/macOS)

**Nicht unterstützt:**
- Internet Explorer (veraltet)

---

## 10. Berechtigungen

### 10.1 Standard-Berechtigungen

Die App benötigt folgende Berechtigungen (werden automatisch angefordert):

- **INTERNET** - Für Web-UI und Zeitsynchronisation
- **ACCESS_NETWORK_STATE** - Netzwerk-Status prüfen
- **BLUETOOTH** - Bluetooth-Kommunikation (optional)
- **BLUETOOTH_ADMIN** - Bluetooth-Verwaltung (optional)
- **WAKE_LOCK** - Gerät wachhalten
- **RECEIVE_BOOT_COMPLETED** - Autostart
- **WRITE_EXTERNAL_STORAGE** - Excel-Dateien lesen/schreiben
- **READ_EXTERNAL_STORAGE** - Excel-Dateien lesen

### 10.2 WRITE_SECURE_SETTINGS Permission

Diese Permission ist **erforderlich** für die automatische Zeitsynchronisation, kann aber **nicht** über die normalen App-Einstellungen vergeben werden.

**Vergabe über ADB:**

1. **USB-Debugging aktivieren:**
   - Einstellungen → Über das Telefon → Build-Nummer 7x antippen
   - Einstellungen → Entwickleroptionen → USB-Debugging aktivieren

2. **Gerät per USB verbinden:**
   - Android-Gerät per USB-Kabel mit Computer verbinden
   - "USB-Debugging zulassen" bestätigen

3. **ADB-Befehl ausführen:**
   ```bash
   adb shell pm grant tom.turmtechnik android.permission.WRITE_SECURE_SETTINGS
   ```

4. **Permission prüfen:**
   ```bash
   adb shell dumpsys package tom.turmtechnik | grep WRITE_SECURE_SETTINGS
   ```

5. **App neu starten:**
   - App vollständig beenden
   - App neu starten

**Hinweis:** Die Permission bleibt auch nach Neustart erhalten.

**Detaillierte Anleitung:** Siehe `WRITE_SECURE_SETTINGS_ANLEITUNG.md`

---

## 11. Netzwerk-Konfiguration

### 11.1 WiFi-Konfiguration

**Für Web-UI Zugriff:**

1. **WiFi aktivieren:**
   - Einstellungen → WLAN aktivieren
   - Mit dem gewünschten Netzwerk verbinden

2. **Statische IP (optional):**
   - Für stabilen Zugriff kann eine statische IP konfiguriert werden
   - Einstellungen → WLAN → Netzwerk → Erweitert → Statische IP

### 11.2 Firewall-Einstellungen

**Falls Web-UI nicht erreichbar:**

1. **Android Firewall:**
   - Stellen Sie sicher, dass die App Internet-Zugriff hat
   - Einstellungen → Apps → Turmtechnik → Datenverbrauch → Unbegrenzt

2. **Router Firewall:**
   - Port 8080 muss für lokale Verbindungen offen sein
   - Normalerweise kein Problem im lokalen Netzwerk

### 11.3 Bluetooth-Konfiguration

**Für Bluetooth-Modus:**

1. **Bluetooth aktivieren:**
   - Einstellungen → Bluetooth aktivieren

2. **Platinen im Bluetooth-Modus:**
   - In der Platinen-Konfiguration: Modus auf "Bluetooth" setzen
   - MAC-Adressen der Platinen eintragen

### 11.4 Fernzugriff per RustDesk (Tailscale)

Wenn alle Geräte im **Tailscale**-Netz hängen, kann **RustDesk** ohne eigenen Relay-Server genutzt werden (Direct-IP / LAN-Modus):

1. **Tailscale** auf dem Android-Gerät und auf dem Admin-PC einrichten; die App zeigt die Tailscale-IP (100.x.x.x) im Web-UI-Header an.
2. In der **Web-UI** (App-Seite 1/2) werden im Header **RustDesk: &lt;IP&gt;:21118** und ein **Kopieren**-Button angezeigt (sobald WLAN- oder Tailscale-IP verfügbar ist). Diese Adresse in RustDesk auf dem Admin-PC eingeben, um eine direkte Verbindung zum Tablet aufzubauen.
3. **Ohne Server:** Die Verbindung läuft direkt über Tailscale; es ist kein RustDesk-Relay-Server nötig (analog zu VNC über Tailscale).
4. **Optional:** Ist die RustDesk-App auf dem Tablet installiert (z. B. `com.carriez.flutter_hbb`), kann über den Link „RustDesk“ in der Web-UI die RustDesk-App vom Gerät aus gestartet werden.

### 11.5 Datei-Transfer (Tablet ↔ PC)

Über die Web-UI können Dateien zwischen dem Turmtechnik-Tablet und einem PC im gleichen Netz (z. B. WLAN oder Tailscale) hin und her kopiert werden:

1. **Zugriff:** Im Browser `http://<Tablet-IP>:8080/` öffnen und „Datei-Transfer“ wählen (oder direkt `/file-transfer.html`).
2. **Ordner wählen:** In der Liste Ordner anklicken (z. B. Melodien, Config) – der aktuelle Pfad ist der gewählte Ordner. Basis ist immer der Turmtechnik-Ordner auf dem Gerät.
3. **Tablet → PC:** Bei einer Datei auf „Herunterladen“ klicken; die Datei wird im Browser gespeichert (Speicherort auf dem PC wählt der Nutzer).
4. **PC → Tablet:** Datei(en) auswählen und „Hochladen“ klicken; die Dateien werden in den aktuell angezeigten Ordner auf dem Tablet gespeichert.
5. **Max. Dateigröße:** 100 MB pro Datei (Download und Upload).

---

## 12. Programmsteuerung

### 12.1 Programmtypen

Die App unterstützt verschiedene Programmtypen:

1. **Normalprogramm** (`Normalprogramm.xls`)
   - Standard-Programm für normale Wochentage
   - Wird verwendet, wenn kein Feiertag oder Benutzerprogramm aktiv ist

2. **Feiertagsprogramme** (z.B. `Weihnachten.xls`)
   - Programme für spezielle Feiertage
   - Werden automatisch an Feiertagen verwendet

3. **Benutzerprogramme**
   - Individuelle Programme für bestimmte Tage
   - Können manuell erstellt werden

### 12.2 Programm-Parameter

Jedes Programm hat folgende Parameter:

#### Startzeit (Spalte A)
- **Format:** `HH:MM:SS` (z.B. `12:00:00`)
- **Sonderwerte:**
  - `SA` - Sonnenaufgang (wird automatisch berechnet)
  - `SU` - Sonnenuntergang (wird automatisch berechnet)

#### Funktion (Spalte B)
- **Melodie:** Läutet eine Melodie
- **Heizung:** Aktiviert Heizung für bestimmte Dauer

#### Melodienname (Spalte C)
- Name der Melodie-Datei (ohne `.xls`)
- Beispiel: `Glocke1`, `Glocke2`

#### Dauer Heizung (Spalte D)
- **Format:** `hh:mm:ss` (z.B. `01:30:00` = 1 Stunde 30 Minuten)
- Nur bei Funktion "Heizung" relevant

#### Wochentage (Spalte E-K)
- **Montag (E):** `x` = aktiv, leer = nicht aktiv
- **Dienstag (F):** `x` = aktiv, leer = nicht aktiv
- **Mittwoch (G):** `x` = aktiv, leer = nicht aktiv
- **Donnerstag (H):** `x` = aktiv, leer = nicht aktiv
- **Freitag (I):** `x` = aktiv, leer = nicht aktiv
- **Samstag (J):** `x` = aktiv, leer = nicht aktiv
- **Sonntag (K):** `x` = aktiv, leer = nicht aktiv

#### Immer (Spalte L)
- `1` = Programm ist immer aktiv (unabhängig von verknüpfter Taste)
- `0` oder leer = Programm ist nur aktiv, wenn verknüpfte Taste aktiv ist

#### Periodisch (Spalte M)
- `1` = Nur in Sommerzeit aktiv
- `2` = Nur in Winterzeit aktiv
- Leer = Immer aktiv (Sommer und Winter)

#### Start-Datum (Spalte N)
- **Format:** `TT.MM` (z.B. `01.03`)
- **Wiederholung:** `TT.MM.JJJJ.A` (z.B. `23.03.2016.7.A` = alle 7 Tage ab 23.03.2016)
- **Tag-Bereich:** `TT.T` (z.B. `8.T` = ab 8. Tag des Monats)

#### Ende-Datum (Spalte O)
- **Format:** `TT.MM` (z.B. `31.10`)
- **Wiederholung:** `TT.MM.JJJJ.A`
- **Tag-Bereich:** `TT.T` (z.B. `14.T` = bis 14. Tag des Monats)

#### Verknüpfte Taste (Spalte P)
- Name der Taste (z.B. `Schlagwerk`)
- Programm ist nur aktiv, wenn diese Taste aktiv ist
- Leer = keine verknüpfte Taste

#### Priorität (Spalte Q)
- **Werte:** `1` bis `9` (höhere Zahl = höhere Priorität)
- Bei mehreren passenden Programmen wird das mit höchster Priorität ausgewählt
- Leer = Priorität 0 (niedrigste)

### 12.3 Programm-Editor (Web-UI)

**Zugriff:**
- Web-UI → Programm-Editor

**Funktionen:**
- Programme anzeigen (nach Tagtyp gefiltert)
- Programm hinzufügen
- Programm bearbeiten
- Programm löschen
- Programme kopieren (von einem Tagtyp zu einem anderen)
- Import aus Excel
- Export (wird automatisch in Excel gespeichert)

**Bearbeitung:**
1. Wählen Sie einen Tagtyp aus (z.B. "Normalprogramm.xls")
2. Klicken Sie auf "Neues Programm" oder bearbeiten Sie ein bestehendes
3. Füllen Sie alle Felder aus
4. Klicken Sie auf "Speichern"

### 12.4 Programm-Editor (Android-UI)

**Zugriff:**
- Hauptbildschirm → Editor-Button

**Funktionen:**
- Programme anzeigen
- Programm bearbeiten
- Programm löschen
- Excel-Dateien direkt bearbeiten

**Hinweis:** Die Web-UI wird empfohlen, da sie benutzerfreundlicher ist.

### 12.5 Programm-Ausführung

**Prüfreihenfolge:**
1. **Zeitprüfung:** Startzeit muss mit aktueller Zeit übereinstimmen
2. **Wochentagprüfung:** Aktueller Wochentag muss aktiviert sein
3. **Prioritätsprüfung:** Programme mit höherer Priorität werden zuerst geprüft
4. **Verknüpfte Taste:** Taste muss aktiv sein (außer "Immer" = 1)
5. **Periodisch:** Sommer/Winter-Zeit muss passen
6. **Start/Ende-Datum:** Aktuelles Datum muss im Bereich liegen

**Sonnenaufgang/Sonnenuntergang:**
- Programme mit `SA` oder `SU` werden automatisch zur berechneten Zeit ausgeführt
- Berechnung basiert auf GPS-Koordinaten (aus System.xls)
- Anpassung möglich (Minuten vor/nach)

---

## 13. Feiertage-Verwaltung

### 13.1 Osterfeiertage (Variable Feiertage)

Osterfeiertage sind Feiertage, die sich nach dem Osterdatum richten (z.B. Karfreitag, Ostermontag).

**Zugriff:**
- Web-UI → Osterfeiertage

**Funktionen:**
- Osterfeiertage anzeigen
- Osterfeiertag hinzufügen
- Osterfeiertag bearbeiten
- Osterfeiertag löschen
- Import aus Excel

**Parameter:**
- **Name:** Feiertagsname (z.B. "Karfreitag")
- **Oster-Offset:** Tage +/- zum Osterdatum (z.B. -2 für Karfreitag)
- **Tagtyp:** Tagtyp-Dateiname (z.B. "Karfreitag.xls")

**Standard-Osterfeiertage:**
- Aschermittwoch (-46)
- Palmsonntag (-7)
- Gründonnerstag (-3)
- Karfreitag (-2)
- Karsamstag (-1)
- Ostersonntag (0)
- Ostermontag (+1)
- Christi Himmelfahrt (+39)
- Pfingstsonntag (+49)
- Pfingstmontag (+50)
- Fronleichnam (+60)

### 13.2 Fixe Feiertage

Fixe Feiertage haben ein festes Datum (z.B. Weihnachten am 25.12.).

**Zugriff:**
- Web-UI → Fixe Feiertage

**Funktionen:**
- Fixe Feiertage anzeigen
- Fixen Feiertag hinzufügen
- Fixen Feiertag bearbeiten
- Fixen Feiertag löschen
- Import aus Excel

**Parameter:**
- **Name:** Feiertagsname (z.B. "Weihnachten")
- **Datum:** Fixes Datum im Format `TT.MM` (z.B. `25.12`)
- **Tagtyp:** Tagtyp-Dateiname (z.B. "Weihnachten.xls")
- **Verschiebung:** 
  - Keine Verschiebung
  - Näher zum Sonntag
  - 1.-4. Sonntag im Monat

**Verschiebung auf Sonntag:**
- **Näher zum Sonntag:** Verschiebt auf den nächsten Sonntag (vorher oder nachher)
- **1.-4. Sonntag im Monat:** Verschiebt auf den 1., 2., 3. oder 4. Sonntag im Monat

### 13.3 Sonderfeiertage (Verschobene Feiertage)

Sonderfeiertage sind verschobene Feiertage, die automatisch berechnet werden.

**Zugriff:**
- Web-UI → Sonderfeiertage

**Funktionen:**
- Sonderfeiertage anzeigen
- Sonderfeiertag berechnen (für Test-Jahr)
- Sonderfeiertag bearbeiten
- Sonderfeiertag löschen
- Alle Sonderfeiertage löschen
- Alle neu berechnen

**Dynamische Berechnung:**
- Sonderfeiertage werden automatisch bei Bedarf berechnet (wie Osterfeiertage)
- Wenn ein Datum abgefragt wird, das noch nicht berechnet wurde, wird es automatisch berechnet und gespeichert
- Keine Vorberechnung für mehrere Jahre nötig

**Vorfeiertag:**
- Für jeden verschobenen Feiertag wird automatisch ein "Vorfeiertag" erstellt
- Vorfeiertag = Tag vor dem berechneten Feiertag
- Name: "Vorfeiertag" (einheitlich)

**Berechnung:**
- **Näher zum Sonntag:** Verschiebt auf den nächsten Sonntag
- **1.-4. Sonntag im Monat:** Berechnet den n-ten Sonntag im Monat

### 13.4 Tagtyp-Zuordnung

Jeder Feiertag kann einem Tagtyp zugeordnet werden:

- **Tagtyp:** Dateiname des Programms (z.B. "Weihnachten.xls")
- Wenn kein Tagtyp zugeordnet ist, wird "Normalprogramm" verwendet
- Tagtyp kann in der Web-UI geändert werden

---

## 14. Platinen-Konfiguration

### 14.1 Platinen-Übersicht

Platinen sind Carambola-Module, die über WiFi oder Bluetooth kommunizieren.

**Zugriff:**
- Web-UI → Platinen-Konfiguration

**Funktionen:**
- Platinen anzeigen
- Platine hinzufügen
- Platine bearbeiten
- Platine löschen
- Online-Status anzeigen

### 14.2 Platine-Parameter

**Platine-Nummer:**
- Eindeutige Nummer (1, 2, 3, ...)
- Wird für Relais-Berechnung verwendet

**IP-Adresse:**
- IPv4-Adresse (z.B. `192.168.1.100`)
- Nur im WiFi-Modus relevant

**Port:**
- Port-Nummer (Standard: 80)
- Nur im WiFi-Modus relevant

**MAC-Adresse:**
- MAC-Adresse der Platine (z.B. `AA:BB:CC:DD:EE:FF`)
- Nur im Bluetooth-Modus relevant

**Relais-Anzahl:**
- Anzahl der Relais pro Platine (Standard: 32)
- Wird für Relais-Berechnung verwendet

**Aktiv:**
- Platine aktivieren/deaktivieren
- Inaktive Platinen werden nicht verwendet

**Online-Status:**
- Wird automatisch aktualisiert
- Zeigt, ob die Platine erreichbar ist

### 14.3 Modus-Konfiguration

**WiFi-Modus:**
- Kommunikation über IP-Adresse und Port
- Mehrere Platinen möglich
- IP-Adressen müssen konfiguriert sein

**Bluetooth-Modus:**
- Kommunikation über MAC-Adressen
- Nur eine Platine möglich
- MAC-Adressen müssen konfiguriert sein

**Modus wechseln:**
- In der Platinen-Konfiguration: Modus auswählen
- Bei Bluetooth-Modus: Nur eine Platine anzeigen

### 14.4 Scan-Relais und Antwortzeit

**Scan-Relais:**
- Relais-Nummer für Scan-Operationen
- Wird für Kommunikationstests verwendet

**Antwortzeit:**
- Berechnete Antwortzeit basierend auf Scan-Relais
- Wird automatisch angezeigt

**Konfiguration:**
- Scan-Relais kann in der Web-UI gesetzt werden
- Antwortzeit wird automatisch berechnet

### 14.5 Power off bei % Akku

**Konfiguration:**
- Schwellwert für automatisches Abschalten (Standard: 75%)
- Kann in der Web-UI geändert werden

**Funktion:**
- Wenn Akku-Stand unter Schwellwert fällt, wird automatisch abgeschaltet
- Schützt vor Tiefentladung

---

## 15. Nebenuhr-Konfiguration

### 15.1 Nebenuhr-Übersicht

Die App unterstützt 4 Nebenuhren:
- **Nebenuhr A**
- **Nebenuhr B**
- **Nebenuhr C**
- **Monduhr D**

**Zugriff:**
- Web-UI → Nebenuhr Config
- Android-UI: Hauptbildschirm → Nebenuhr-Button

### 15.2 Nebenuhr-Parameter

**Uhr-Name:**
- "A", "B", "C" oder "D"
- Eindeutige Identifikation

**Relais A / Relais B:**
- Wechselschaltung: Zwei Relais wechseln sich ab
- Relais A: Für eine Hälfte der Zeit
- Relais B: Für die andere Hälfte der Zeit
- Verhindert gleichzeitiges Schalten

**Impuls-Dauer 1:**
- Dauer des Impulses in **Sekunden**
- Standard: 1 Sekunde

**Impuls-Dauer 2 (Pause):**
- Pause zwischen Impulsen in **Sekunden**
- Standard: 1 Sekunde

**Modus:**
- **12-Stunden-Modus:** Standard (immer aktiv)
- **24-Stunden-Modus:** Nicht mehr verwendet
- **MOND:** Nur für Monduhr D

**Angezeigte Zeit:**
- Aktuelle angezeigte Zeit (Minuten seit Mitternacht, 0-1439)
- Wird automatisch aktualisiert

**Aktiv:**
- Nebenuhr aktivieren/deaktivieren
- Inaktive Nebenuhren werden nicht gesteuert

### 15.3 Monduhr (D)

**Besonderheiten:**
- Zeigt Mondphasen an
- Mondphase kann manuell gesetzt werden (Slider)
- Mondphase wird auf Hauptbildschirm angezeigt

**Konfiguration:**
- Relais A/B für Wechselschaltung
- Impuls-Längen und Pause
- Mondphase (0-59)

**Anzeige:**
- Mondbild auf Hauptbildschirm
- Position: Zentral auf dem Logo
- Größe: Angepasst

### 15.4 Wechselschaltung

**Funktionsweise:**
- Zwei Relais wechseln sich ab
- Verhindert gleichzeitiges Schalten
- **last_relais_a:** Speichert, welches Relais zuletzt verwendet wurde (1=A, 0=B)

**Vorteile:**
- Gleichmäßige Belastung beider Relais
- Keine gleichzeitigen Schaltvorgänge
- Längere Lebensdauer

### 15.5 Layout-Konfiguration

**Android-UI:**
- Nebenuhr-Einstellungen: 2x2 Grid-Layout
- Buttons sind kleiner und übersichtlicher

**Hauptbildschirm:**
- Monduhr-Anzeige: Zentral auf Logo
- Größe: Angepasst

---

## 16. Vorschwingen-Konfiguration

### 16.1 Vorschwingen-Übersicht

Vorschwingen ist das Vorschwingen der Glocke vor dem eigentlichen Läuten.

**Zugriff:**
- Web-UI → Vorschwingen Config

**Funktionen:**
- Vorschwingen-Einträge anzeigen
- Vorschwingen-Eintrag hinzufügen
- Vorschwingen-Eintrag bearbeiten
- Vorschwingen-Eintrag löschen
- Import aus Excel

### 16.2 Vorschwingen-Parameter

**Vorschwingen Relais:**
- Relais-Nummer für Vorschwingen (1-32)
- Dropdown-Auswahl: Tasten mit Funktion "Schwingen"

**Läuten Relais:**
- Relais-Nummer für Läuten (1-32)
- Dropdown-Auswahl: Tasten mit Funktion "Melodie"

**Vorschwingzeit:**
- Zeit in **Sekunden**
- Wie lange vorgeschwungen wird, bevor geläutet wird

**Vorschwingen Platine:**
- Platine-Nummer für Vorschwingen-Relais (1-3)

**Läuten Platine:**
- Platine-Nummer für Läuten-Relais (1-3)

**Berechnete Relais-Nummern:**
- Werden automatisch berechnet
- Für Kompatibilität mit alter Berechnung

**Aktiv:**
- Vorschwingen-Eintrag aktivieren/deaktivieren
- Inaktive Einträge werden nicht verwendet

**Sortierung:**
- Sortierreihenfolge (für Anzeige)

### 16.3 Dropdown-Auswahl

**Schwingen-Tasten:**
- Zeigt alle Tasten mit Funktion "Schwingen" an
- Aus `Beschriftung-Tasten.xls`

**Melodie-Tasten:**
- Zeigt alle Tasten mit Funktion "Melodie" an
- Aus `Beschriftung-Tasten.xls`

---

## 17. Relais-Verwaltung

### 17.1 Relais-Status-Bar (Web-UI)

**Zugriff:**
- Wird oben auf allen Web-UI-Seiten angezeigt

**Funktionen:**
- Zeigt alle zugewiesenen Relais an
- **Grün:** Relais wird nur einmal verwendet
- **Rot:** Relais wird mehrfach verwendet (Duplikat)
- Tooltip: Zeigt Verwendung an (z.B. "Vorschwingen", "Nebenuhr A")

### 17.2 Relais-Zuordnung

Relais werden verschiedenen Komponenten zugeordnet:

1. **Platinen:** Jede Platine hat eine bestimmte Anzahl Relais
2. **Nebenuhren:** Relais A/B für jede Nebenuhr
3. **Vorschwingen:** Vorschwingen-Relais und Läuten-Relais
4. **Programme:** Über verknüpfte Tasten

### 17.3 Duplikat-Erkennung

**Automatische Erkennung:**
- System prüft, ob ein Relais mehrfach verwendet wird
- Duplikate werden rot angezeigt

**Tooltip:**
- Beim Hovern über ein Relais wird angezeigt, wo es verwendet wird
- Beispiel: "Vorschwingen, Nebenuhr A"

### 17.4 Aktive/Inaktive Relais

**Anzeige:**
- Nur aktive Relais werden angezeigt
- Inaktive Platinen/Nebenuhren werden nicht berücksichtigt

---

## 18. Melodien-Verwaltung

### 18.1 Melodien-Übersicht

Melodien sind Excel-Dateien, die die Läut-Sequenzen definieren.

**Speicherort:**
- `/sdcard/Turmtechnik/Melodien/`
- Format: `.xls` Dateien

**Verwendung:**
- Programme verweisen auf Melodien-Namen (ohne `.xls`)
- Beispiel: Programm mit Melodienname "Glocke1" verwendet `Glocke1.xls`

### 18.2 Melodie-Struktur

**Spalten:**
- **Spalte A:** Zeit (Sekunden seit Start)
- **Spalte B:** Relais-Nummer
- **Spalte C:** Ein/Aus (1/0)

**Zeilen:**
- Jede Zeile definiert einen Schaltvorgang
- Zeitpunkt, Relais und Zustand

### 18.3 Melodien bearbeiten

**Excel-Editor:**
- Melodien können direkt in Excel bearbeitet werden
- Dateien müssen im Melodien-Verzeichnis gespeichert werden

**Web-UI (geplant):**
- Melodien-Verwaltung über Web-UI
- Noch nicht implementiert

---

## 19. Zeitsynchronisation

### 19.1 Zeitserver-Konfiguration

**Zugriff:**
- System.xls → Zeitserver-Konfiguration
- Web-UI (geplant)

**Parameter:**
- **Zeitserver IP:** IP-Adresse des Zeitservers
- **Zeitserver Port:** Port (Standard: 123 für NTP)
- **Synchronisations-Intervall:** Wie oft synchronisiert wird

### 19.2 GPS-Zeitsynchronisation

**NMEA-GPS-Integration:**
- Unterstützung für NMEA-GPS-Module
- Präzise Zeitsynchronisation über GPS

**Konfiguration:**
- GPS IP/Port in System.xls
- Automatische Synchronisation

### 19.3 Manuelle Zeiteinstellung

**Android-Einstellungen:**
- Zeit kann manuell in Android-Einstellungen geändert werden
- App verwendet System-Zeit

**Zeitsynchronisation deaktivieren:**
- Kann in den Einstellungen deaktiviert werden
- App funktioniert weiterhin mit manueller Zeit

### 19.4 WRITE_SECURE_SETTINGS

**Erforderlich für:**
- Automatische Zeitsynchronisation
- Direkte Änderung der System-Zeit

**Vergabe:**
- Siehe Kapitel 10.2

---

## 20. Schlagwerk

### 20.1 Schlagwerk-Übersicht

Das Schlagwerk schlägt zu bestimmten Zeiten (z.B. jede Stunde).

**Zugriff:**
- System.xls → Schlagwerk-Konfiguration
- Web-UI (geplant)

**Funktionen:**
- Schlagwerk-Zeiten konfigurieren
- Schlagwerk ein/aus
- Beginn/Ende-Zeiten

### 20.2 Schlagwerk-Konfiguration

**Parameter:**
- **Schlagwerk-Zeiten:** Liste von Zeiten, zu denen geschlagen wird
- **Beginn-Zeit:** Ab wann Schlagwerk aktiv ist
- **Ende-Zeit:** Bis wann Schlagwerk aktiv ist
- **Ein/Aus:** Schlagwerk aktivieren/deaktivieren

---

## 21. Heizung

### 21.1 Heizungssteuerung

**Automatische Heizung:**
- Programme mit Funktion "Heizung"
- Dauer wird in Programm definiert (Spalte D)

**Manuelle Heizung:**
- Kann manuell über Android-UI gestartet werden
- Dauer kann eingestellt werden

### 21.2 Heizungs-Parameter

**Dauer:**
- Format: `hh:mm:ss` (z.B. `01:30:00`)
- Wird in Minuten umgerechnet

**Relais:**
- Heizungs-Relais wird über verknüpfte Taste definiert
- Oder direkt in Programm

---

## 22. Web-UI Konfiguration

### 22.1 Verfügbare Seiten

**Hauptmenü:**
- Programm-Editor
- Osterfeiertage
- Fixe Feiertage
- Sonderfeiertage
- Nebenuhr Config
- Vorschwingen Config
- Platinen-Konfiguration
- Zeitserver & GPS (geplant)
- Schlagwerk (geplant)

### 22.2 REST API Übersicht

**Programme:**
- `GET /api/programme?tagtyp=...` - Programme abrufen
- `POST /api/programme` - Programm erstellen
- `PUT /api/programme/:id?tagtyp=...` - Programm aktualisieren
- `DELETE /api/programme/:id?tagtyp=...` - Programm löschen
- `POST /api/programme/import-all` - Alle Programme importieren
- `POST /api/programme/reload?tagtyp=...` - Programme neu laden
- `POST /api/programme/copy?tagtypName=...&sourceTagtyp=...` - Programme kopieren

**Feiertage:**
- `GET /api/osterfeiertage` - Osterfeiertage abrufen
- `PUT /api/osterfeiertage` - Osterfeiertag aktualisieren
- `DELETE /api/osterfeiertage/:id` - Osterfeiertag löschen
- `POST /api/osterfeiertage/migrate` - Aus Excel importieren
- `GET /api/feste-feiertage` - Fixe Feiertage abrufen
- `PUT /api/feste-feiertage` - Fixen Feiertag aktualisieren
- `DELETE /api/feste-feiertage/:id` - Fixen Feiertag löschen
- `POST /api/feste-feiertage/migrate` - Aus Excel importieren
- `GET /api/sonderfeiertage` - Sonderfeiertage abrufen
- `POST /api/sonderfeiertage/berechnen` - Sonderfeiertag berechnen
- `POST /api/sonderfeiertage/neuberechnen` - Alle neu berechnen
- `DELETE /api/sonderfeiertage/alle-loeschen` - Alle löschen
- `PUT /api/sonderfeiertage/:id` - Sonderfeiertag aktualisieren
- `DELETE /api/sonderfeiertage/:id` - Sonderfeiertag löschen

**Platinen:**
- `GET /api/platinen` - Platinen abrufen
- `POST /api/platinen` - Platine erstellen
- `PUT /api/platinen/:nummer` - Platine aktualisieren
- `DELETE /api/platinen/:nummer` - Platine löschen
- `GET /api/platinen/config` - Modus-Konfiguration abrufen
- `PUT /api/platinen/config` - Modus-Konfiguration speichern
- `GET /api/platinen/scan-relais` - Scan-Relais abrufen
- `PUT /api/platinen/scan-relais` - Scan-Relais speichern

**Nebenuhren:**
- `GET /api/nebenuhren` - Nebenuhren abrufen
- `PUT /api/nebenuhren` - Nebenuhren speichern

**Vorschwingen:**
- `GET /api/vorschwingen` - Vorschwingen abrufen
- `POST /api/vorschwingen` - Vorschwingen erstellen
- `PUT /api/vorschwingen` - Vorschwingen aktualisieren
- `DELETE /api/vorschwingen/:id` - Vorschwingen löschen
- `POST /api/vorschwingen/migrate` - Aus Excel importieren

**Weitere:**
- `GET /api/device-ip` - WLAN-IP, Tailscale-IP und RustDesk-Direct-Adresse (`rustdesk_direct`: z. B. `100.x.x.x:21118` für Fernzugriff ohne Relay)
- `GET /api/file-transfer/list?path=...` - Verzeichnisinhalt (eine Ebene) im Turmtechnik-Ordner für Datei-Transfer
- `GET /api/file-transfer/download?path=...` - Einzeldatei herunterladen (max. 100 MB)
- `POST /api/file-transfer/upload` - Datei hochladen (multipart: `path`, `file`), max. 100 MB
- `GET /api/tagtypen` - Tagtypen abrufen
- `POST /api/tagtypen` - Tagtyp erstellen
- `DELETE /api/tagtypen/:id` - Tagtyp löschen
- `GET /api/verknuepfte-tasten` - Verknüpfte Tasten abrufen
- `GET /api/schwingen-tasten` - Schwingen-Tasten abrufen
- `GET /api/melodie-tasten` - Melodie-Tasten abrufen
- `GET /api/power-off-akku` - Power off Akku Prozent abrufen
- `PUT /api/power-off-akku` - Power off Akku Prozent speichern

### 22.3 Browser-Kompatibilität

**Empfohlen:**
- Chrome (Desktop/Android)
- Firefox (Desktop/Android)
- Edge (Desktop)
- Safari (iOS/macOS)

**Nicht unterstützt:**
- Internet Explorer (veraltet)

---

## 23. Datenbank-Migration

### 23.1 Excel zu Datenbank

**Automatische Migration:**
- Beim ersten Start werden Standard-Daten erstellt
- Programme müssen manuell importiert werden

**Manuelle Migration:**
- Web-UI → Programm-Editor → "Import Excel Programmtage"
- Web-UI → Osterfeiertage → "Aus Excel importieren"
- Web-UI → Fixe Feiertage → "Aus Excel importieren"
- Web-UI → Vorschwingen Config → "Aus Excel importieren"

### 23.2 Datenbank-Schema

Siehe Anhang B für vollständiges Datenbank-Schema.

### 23.3 Backup und Wiederherstellung

**Backup:**
```bash
adb pull /data/data/tom.turmtechnik/databases/turmtechnik_config.db backup.db
```

**Wiederherstellung:**
```bash
adb push backup.db /data/data/tom.turmtechnik/databases/turmtechnik_config.db
```

**App neu starten:**
- Nach Wiederherstellung App neu starten

---

## 24. System-Konfiguration

### 24.1 Autostart

**Aktivierung:**
- App startet automatisch beim Boot des Geräts
- Kann in Android-Einstellungen deaktiviert werden

**Konfiguration:**
- System.xls → Autostart-Flag
- Standard: Aktiviert

### 24.2 Serial Number

**Anzeige:**
- Serial Number wird in System.xls gespeichert
- Wird für Identifikation verwendet

### 24.3 Version

**Anzeige:**
- App-Version wird in System.xls gespeichert
- Wird für Updates verwendet

### 24.4 Thread-Status

**Überwachung:**
- Alle Threads werden überwacht
- Status wird in Logs angezeigt

### 24.5 Logging

**Log-Dateien:**
- Logs werden in `/sdcard/Turmtechnik/Logs/` gespeichert
- Format: Text-Dateien

**Debug-Modus:**
- Kann in StaticConstants.java aktiviert werden
- `DEBUG = true`

---

## 25. Grundfunktionen

### 25.1 Hauptbildschirm

**Layout:**
- Landscape-Modus (Querformat)
- Vollbild-Anzeige
- Tasten-Grid (8x3)

**Anzeigen:**
- Aktuelle Zeit
- Nächste Programm-Zeit
- Status-Informationen
- Monduhr (falls aktiv)

### 25.2 Navigation

**Tasten:**
- **Home:** Zurück zum Hauptbildschirm
- **Hilfe:** Hilfe-Seite öffnen
- **Editor:** Programm-Editor öffnen
- **Programm-Abfrage:** Programme für Datum anzeigen
- **Nebenuhr:** Nebenuhr-Einstellungen
- **Manueller Start:** Manuelle Steuerung

### 25.3 Tasten-Funktionen

**Tasten-Grid:**
- 8 Spalten × 3 Zeilen = 24 Tasten
- Tasten können konfiguriert werden
- Beschriftungen aus `Beschriftung-Tasten.xls`

**Funktionen:**
- **Melodie:** Startet Melodie
- **Heizung:** Aktiviert Heizung
- **Schwingen:** Vorschwingen
- **Verknüpft:** Verknüpfte Tasten

### 25.4 Status-Anzeigen

**Anzeigen:**
- Aktuelle Zeit
- Nächste Programm-Zeit
- Feiertag (falls vorhanden)
- Sonnenaufgang/Sonnenuntergang (falls Programme vorhanden)
- Online-Status der Platinen

---

## 26. Programm-Abfrage

### 26.1 Datum auswählen

**Zugriff:**
- Hauptbildschirm → Programm-Abfrage-Button
- Kalender-Popup öffnet sich

**Auswahl:**
- Jahr, Monat, Tag auswählen
- Klicken Sie auf "OK"

### 26.2 Programme anzeigen

**Anzeige:**
- Alle Programme für das ausgewählte Datum
- Sortiert nach Zeit
- Mit Melodienname oder Heizungsdauer

**Informationen:**
- Startzeit
- Funktion (Melodie/Heizung)
- Melodienname oder Heizungsdauer
- Beginn-Zeit (für verknüpfte Tasten)

### 26.3 Sonnenaufgang/Sonnenuntergang

**Anzeige:**
- Wird nur angezeigt, wenn Programme mit SA/SU vorhanden sind
- Format: `Sonnenaufgang HH:MM  Sonnenuntergang HH:MM`

**Berechnung:**
- Basiert auf GPS-Koordinaten (aus System.xls)
- Anpassung möglich (Minuten vor/nach)

### 26.4 Feiertags-Anzeige

**Anzeige:**
- Wird angezeigt, wenn das Datum ein Feiertag ist
- Format: `Feiertag: [Name] ([Datum])`
- Beispiel: `Feiertag: Weihnachten (25.12.2026)`

---

## 27. Manuelle Steuerung

### 27.1 Manueller Start

**Zugriff:**
- Hauptbildschirm → Manueller Start-Button

**Funktionen:**
- Melodie manuell starten
- Heizung manuell starten
- Dauer einstellen

### 27.2 Sofort-Start

**Zugriff:**
- Hauptbildschirm → Sofort-Start-Button (falls vorhanden)

**Funktion:**
- Startet sofort eine Melodie
- Ohne Zeitverzögerung

### 27.3 Stop-Funktion

**Zugriff:**
- Hauptbildschirm → Stop-Button

**Funktion:**
- Stoppt alle laufenden Programme
- Notfall-Stopp

---

## 28. Konfiguration über Web-UI

### 28.1 Zugriff auf Web-UI

**Schritte:**
1. Ermitteln Sie die IP-Adresse des Android-Geräts
2. Öffnen Sie Browser auf PC/Tablet
3. Geben Sie ein: `http://<IP-Adresse>:8080`
4. Hauptmenü wird angezeigt

### 28.2 Programme bearbeiten

**Schritte:**
1. Web-UI → Programm-Editor
2. Tagtyp auswählen (z.B. "Normalprogramm.xls")
3. Programm hinzufügen oder bearbeiten
4. Felder ausfüllen
5. Speichern

### 28.3 Feiertage verwalten

**Osterfeiertage:**
1. Web-UI → Osterfeiertage
2. Feiertag hinzufügen oder bearbeiten
3. Name, Offset, Tagtyp eingeben
4. Speichern

**Fixe Feiertage:**
1. Web-UI → Fixe Feiertage
2. Feiertag hinzufügen oder bearbeiten
3. Name, Datum, Tagtyp, Verschiebung eingeben
4. Speichern

**Sonderfeiertage:**
1. Web-UI → Sonderfeiertage
2. Sonderfeiertag berechnen (für Test-Jahr)
3. Oder bestehende Sonderfeiertage bearbeiten/löschen

### 28.4 Platinen konfigurieren

**Schritte:**
1. Web-UI → Platinen-Konfiguration
2. Platine hinzufügen oder bearbeiten
3. IP-Adresse, Port, MAC-Adresse, Relais-Anzahl eingeben
4. Aktivieren
5. Speichern

---

## 29. Architektur

### 29.1 Android-App Struktur

**Hauptkomponenten:**
- `TurmtechnikActivity.java` - Haupt-Activity
- `ConfigWebServer.java` - Web-Server und REST API
- `PlatinenDatabaseHelper.java` - Datenbank-Helper
- `TagesSuche.java` - Programm-Suche
- `UhrThread.java` - Haupt-Thread für Programm-Ausführung
- `MelodieThreadNew.java` - Melodien-Ausführung
- `NebenUhrA_thread.java`, `NebenUhrB_thread.java`, etc. - Nebenuhr-Threads

**Repository-Pattern:**
- `ProgrammRepository.java` - Interface
- `ExcelProgrammRepository.java` - Excel-Implementierung
- `MultiFileProgrammRepository.java` - Multi-File-Implementierung
- `RepositoryFactory.java` - Factory für Repository-Auswahl

### 29.2 Datenbank-Schema (SQLite)

**Tabellen:**
- `platinen_config` - Platinen
- `io_config` - I/O-Konfiguration
- `nebenuhr_config` - Nebenuhren
- `tagtypen` - Tagtypen
- `programme` - Programme
- `osterfeiertage` - Osterfeiertage
- `feste_feiertage` - Fixe Feiertage
- `sonderfeiertage` - Sonderfeiertage
- `vorschwingen_config` - Vorschwingen

**Version:**
- Aktuelle Version: 12
- Automatische Migration bei Updates

### 29.3 Excel-Integration

**Dateiformat:**
- `.xls` (Excel 97-2003)
- Bibliothek: `jxl.jar`

**Verwendung:**
- Import von Konfigurationsdaten
- Backup und Export
- Kompatibilität mit bestehenden Systemen

### 29.4 Web-Server (SimpleHttpServer)

**Implementierung:**
- Eigener HTTP-Server (keine externe Bibliothek)
- Port: 8080 (Standard)
- REST API für alle Konfigurationsbereiche
- HTML-Seiten für Web-UI

### 29.5 REST API

**Format:**
- JSON (Request/Response)
- HTTP-Methoden: GET, POST, PUT, DELETE
- Fehlerbehandlung: HTTP-Status-Codes

---

## 30. Dateistruktur

### 30.1 Excel-Dateien

**Speicherort:**
- `/sdcard/Turmtechnik/`

**Dateien:**
- `System.xls` - System-Konfiguration
- `Normalprogramm.xls` - Normalprogramm
- `Beschriftung-Tasten.xls` - Tasten-Beschriftungen
- `[Feiertag].xls` - Feiertagsprogramme
- `Melodien/[Melodie].xls` - Melodien

### 30.2 Datenbank-Dateien

**Speicherort:**
- `/data/data/tom.turmtechnik/databases/turmtechnik_config.db`

**Backup:**
- Manuelles Backup empfohlen
- Siehe Kapitel 7.4

### 30.3 Konfigurationsdateien

**Speicherort:**
- `/data/data/tom.turmtechnik/shared_prefs/`
- Android SharedPreferences

### 30.4 Log-Dateien

**Speicherort:**
- `/sdcard/Turmtechnik/Logs/`
- Format: Text-Dateien
- Rotation: Automatisch

---

## 31. API-Dokumentation

### 31.1 Alle REST API Endpunkte

Siehe Kapitel 22.2 für vollständige Liste.

### 31.2 Request/Response Formate

**Request (JSON):**
```json
{
  "feld1": "wert1",
  "feld2": 123
}
```

**Response (JSON):**
```json
{
  "success": true,
  "message": "Erfolgreich",
  "data": { ... }
}
```

**Fehler-Response:**
```json
{
  "error": "Fehlermeldung"
}
```

### 31.3 Fehlerbehandlung

**HTTP-Status-Codes:**
- `200` - Erfolg
- `400` - Ungültige Anfrage
- `404` - Nicht gefunden
- `500` - Server-Fehler

**Fehlermeldungen:**
- Werden im JSON-Response zurückgegeben
- Feld: `error`

---

## 32. Anhang A: Excel-Datei-Format

### 32.1 Programm-Dateien

**Spalten:**
- **A:** Startzeit (`HH:MM:SS` oder `SA`/`SU`)
- **B:** Funktion (`Melodie` oder `Heizung`)
- **C:** Melodienname (ohne `.xls`)
- **D:** Dauer Heizung (`hh:mm:ss`)
- **E-K:** Wochentage (`x` = aktiv)
- **L:** Immer (`1` = immer aktiv)
- **M:** Periodisch (`1` = Sommer, `2` = Winter)
- **N:** Start-Datum (`TT.MM` oder `TT.MM.JJJJ.A` oder `TT.T`)
- **O:** Ende-Datum (`TT.MM` oder `TT.MM.JJJJ.A` oder `TT.T`)
- **P:** Verknüpfte Taste (Name)
- **Q:** Priorität (`1`-`9`)

**Detaillierte Beschreibung:** Siehe `SPALTEN_PROGRAMMSUCHE.md`

### 32.2 System.xls

**Sheet 14 - Platinen:**
- Zeile 1: Überschriften
- Zeile 2+: Platinen-Daten

**Sheet 15 - Nebenuhren:**
- Zeile 3: Nebenuhr A
- Zeile 4: Nebenuhr B
- Zeile 5: Nebenuhr C
- Zeile 6: Nebenuhr D (Monduhr)

**Sheet 9 - Vorschwingen:**
- Zeile 1: Überschriften
- Zeile 2+: Vorschwingen-Daten

**Sheet 12 - Osterfeiertage (Header z.B. Feiertag | Tage +/- | Datum 2026 | Tagtyp | Aktionen):**
- Spalte A: Tagtyp (Programmtag, z.B. "kein Läuten.xls") – dem Feiertag in Spalte B zugeordnet
- Spalte B: Feiertag (Name, z.B. "Karfreitag")

**Sheet 13 - Fixe Feiertage:**
- Spalte A: Tagtyp
- Spalte B: Bezeichnung
- Spalte D: Datum

### 32.3 Beschriftung-Tasten.xls

**Spalten:**
- Tasten-Name
- Relais-Nummer
- Platine-Nummer
- Funktion (`verknuepft`, `Schwingen`, `Melodie`)

---

## 33. Anhang B: Datenbank-Schema

### 33.1 Tabellen-Übersicht

**platinen_config:**
- `id` (INTEGER PRIMARY KEY)
- `platine_nummer` (INTEGER UNIQUE)
- `ip_adresse` (TEXT)
- `port` (INTEGER)
- `aktiv` (BOOLEAN)
- `mac_adresse` (TEXT)
- `online` (BOOLEAN)
- `relais_anzahl` (INTEGER)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)

**io_config:**
- `id` (INTEGER PRIMARY KEY)
- `key` (TEXT UNIQUE)
- `value` (TEXT)
- `updated_at` (TIMESTAMP)

**nebenuhr_config:**
- `id` (INTEGER PRIMARY KEY)
- `uhr_name` (TEXT) - "A", "B", "C", "D"
- `zeile_index` (INTEGER)
- `relais_a` (INTEGER)
- `relais_b` (INTEGER)
- `impuls_dauer_1` (INTEGER) - Sekunden
- `impuls_dauer_2` (INTEGER) - Sekunden (Pause)
- `uhr_name_display` (TEXT)
- `modus` (TEXT) - "12", "24", "MOND"
- `angezeigte_zeit` (INTEGER) - Minuten seit Mitternacht
- `mondphase_ist` (INTEGER) - 0-59 (nur Monduhr)
- `last_relais_a` (INTEGER) - 1=A, 0=B
- `aktiv` (INTEGER)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)

**tagtypen:**
- `id` (INTEGER PRIMARY KEY)
- `name` (TEXT UNIQUE) - Dateiname
- `programm_typ` (TEXT) - "normal", "festtag_variabel", "festtag_fest", "benutzer"
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)

**programme:**
- `id` (INTEGER PRIMARY KEY)
- `tagtyp_id` (INTEGER) - Foreign Key zu tagtypen
- `zeile_index` (INTEGER)
- `startzeit` (TEXT) - "HH:MM:SS" oder "SA"/"SU"
- `funktion` (TEXT) - "Melodie" oder "Heizung"
- `melodie_name` (TEXT)
- `dauer_heizung` (TEXT) - "hh:mm:ss"
- `montag` (BOOLEAN)
- `dienstag` (BOOLEAN)
- `mittwoch` (BOOLEAN)
- `donnerstag` (BOOLEAN)
- `freitag` (BOOLEAN)
- `samstag` (BOOLEAN)
- `sonntag` (BOOLEAN)
- `immer` (BOOLEAN)
- `periodisch` (INTEGER) - 0=immer, 1=Sommer, 2=Winter
- `start_datum` (TEXT)
- `ende_datum` (TEXT)
- `verknuepfte_taste` (TEXT)
- `prioritaet` (INTEGER) - 1-9
- `festtag_datum` (TEXT)
- `festtag_jahr` (INTEGER)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)

**osterfeiertage:**
- `id` (INTEGER PRIMARY KEY)
- `name` (TEXT UNIQUE)
- `oster_offset` (INTEGER) - Tage +/- zum Osterdatum
- `tagtyp_name` (TEXT)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)

**feste_feiertage:**
- `id` (INTEGER PRIMARY KEY)
- `name` (TEXT)
- `datum` (TEXT) - "TT.MM"
- `tagtyp_name` (TEXT)
- `verschieben_auf_sonntag` (BOOLEAN)
- `sonntag_im_monat` (INTEGER) - 0=nächster, -1=näher zum Sonntag, 1-4=n-ter Sonntag
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)

**sonderfeiertage:**
- `id` (INTEGER PRIMARY KEY)
- `feiertag_id` (INTEGER) - Foreign Key zu feste_feiertage
- `jahr` (INTEGER)
- `berechnetes_datum` (TEXT) - "TT.MM"
- `tagtyp_name` (TEXT)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)

**vorschwingen_config:**
- `id` (INTEGER PRIMARY KEY)
- `vorschwingen_relais` (INTEGER) - 1-32
- `laeuten_relais` (INTEGER) - 1-32
- `zeit_sekunden` (INTEGER)
- `vorschwingen_platine` (INTEGER) - 1-3
- `laeuten_platine` (INTEGER) - 1-3
- `vorschwingen_relais_berechnet` (INTEGER)
- `laeuten_relais_berechnet` (INTEGER)
- `aktiv` (INTEGER)
- `sortierung` (INTEGER)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)

### 33.2 Indizes

- `idx_platinen_nummer` auf `platinen_config(platine_nummer)`
- `idx_nebenuhr_uhr_name` auf `nebenuhr_config(uhr_name)`
- `idx_tagtypen_name` auf `tagtypen(name)`
- `idx_programme_tagtyp` auf `programme(tagtyp_id)`
- `idx_programme_startzeit` auf `programme(startzeit)`
- `idx_osterfeiertage_name` auf `osterfeiertage(name)`
- `idx_feste_feiertage_datum` auf `feste_feiertage(datum)`
- `idx_sonderfeiertage_datum` auf `sonderfeiertage(berechnetes_datum, jahr)`
- `idx_vorschwingen_aktiv` auf `vorschwingen_config(aktiv)`

### 33.3 Beziehungen

- `programme.tagtyp_id` → `tagtypen.id` (ON DELETE CASCADE)
- `sonderfeiertage.feiertag_id` → `feste_feiertage.id` (ON DELETE CASCADE)

---

## 34. Anhang C: API-Referenz

### 34.1 Vollständige API-Liste

Siehe Kapitel 22.2 für vollständige Liste aller API-Endpunkte.

### 34.2 Code-Beispiele

**Programm abrufen:**
```javascript
fetch('/api/programme?tagtyp=Normalprogramm.xls')
  .then(response => response.json())
  .then(data => console.log(data));
```

**Programm erstellen:**
```javascript
fetch('/api/programme?tagtyp=Normalprogramm.xls', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    startzeit: '12:00:00',
    funktion: 'Melodie',
    melodie_name: 'Glocke1',
    montag: true,
    dienstag: true,
    // ... weitere Felder
  })
});
```

**Programm aktualisieren:**
```javascript
fetch('/api/programme/123?tagtyp=Normalprogramm.xls', {
  method: 'PUT',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    startzeit: '13:00:00',
    // ... geänderte Felder
  })
});
```

**Programm löschen:**
```javascript
fetch('/api/programme/123?tagtyp=Normalprogramm.xls', {
  method: 'DELETE'
});
```

### 34.3 Fehlercodes

**HTTP-Status-Codes:**
- `200` - Erfolg
- `400` - Ungültige Anfrage (z.B. fehlende Parameter)
- `404` - Nicht gefunden (z.B. Programm-ID existiert nicht)
- `500` - Server-Fehler (interner Fehler)

**JSON-Fehler-Response:**
```json
{
  "error": "Fehlermeldung"
}
```

---

## 35. Anhang D: Glossar

### 35.1 Fachbegriffe

**Tagtyp:**
- Programm-Typ (Normalprogramm, Feiertagsprogramm, etc.)
- Wird durch Dateiname identifiziert (z.B. "Normalprogramm.xls")

**Platine:**
- Carambola-Modul für WiFi/Bluetooth-Kommunikation
- Steuert Relais

**Relais:**
- Elektrischer Schalter
- Wird für Glocken, Heizung, etc. verwendet

**Nebenuhr:**
- Zusätzliche Uhr (neben Hauptuhr)
- Kann verschiedene Zeiten anzeigen

**Monduhr:**
- Spezielle Nebenuhr, die Mondphasen anzeigt
- Nebenuhr D

**Vorschwingen:**
- Vorschwingen der Glocke vor dem Läuten
- Reduziert Belastung

**Osterfeiertag:**
- Variable Feiertag, der sich nach Osterdatum richtet
- Beispiel: Karfreitag, Ostermontag

**Fester Feiertag:**
- Feiertag mit festem Datum
- Beispiel: Weihnachten (25.12.)

**Sonderfeiertag:**
- Verschobener Feiertag (auf Sonntag)
- Wird automatisch berechnet

**Vorfeiertag:**
- Tag vor einem Feiertag
- Wird automatisch erstellt

**Wechselschaltung:**
- Zwei Relais wechseln sich ab
- Verhindert gleichzeitiges Schalten

**Sonnenaufgang/Sonnenuntergang (SA/SU):**
- Sonderwerte für Startzeit in Programmen
- Werden automatisch berechnet

**Periodisch:**
- Sommer/Winter-Zeit
- Programme können nur in bestimmter Zeit aktiv sein

**Verknüpfte Taste:**
- Taste, die aktiv sein muss, damit Programm ausgeführt wird
- Beispiel: "Schlagwerk"

**Priorität:**
- 1-9 (höhere Zahl = höhere Priorität)
- Bei mehreren passenden Programmen wird das mit höchster Priorität ausgewählt

### 35.2 Abkürzungen

- **API:** Application Programming Interface
- **CRUD:** Create, Read, Update, Delete
- **GPS:** Global Positioning System
- **HTTP:** Hypertext Transfer Protocol
- **IP:** Internet Protocol
- **MAC:** Media Access Control
- **NMEA:** National Marine Electronics Association
- **NTP:** Network Time Protocol
- **REST:** Representational State Transfer
- **SQLite:** SQL Database Engine
- **UI:** User Interface
- **Web-UI:** Web User Interface
- **WiFi:** Wireless Fidelity

### 35.3 Erklärungen

**Dynamische Berechnung:**
- Sonderfeiertage werden bei Bedarf berechnet (nicht vorberechnet)
- Spart Datenbank-Speicher
- Funktioniert wie Osterfeiertage

**Repository-Pattern:**
- Abstraktionsschicht zwischen Datenquelle und Anwendung
- Ermöglicht Wechsel zwischen Excel und Datenbank

**REST API:**
- Architektur-Stil für Web-Services
- Verwendet HTTP-Methoden (GET, POST, PUT, DELETE)
- JSON als Datenformat

---

## 36. Anhang E: Fehlerbehebung

### 36.1 Häufige Probleme

**Problem: Web-UI nicht erreichbar**
- **Lösung:** 
  - Prüfen Sie, ob Geräte im gleichen Netzwerk sind
  - Prüfen Sie die IP-Adresse
  - Prüfen Sie, ob Port 8080 nicht blockiert ist
  - App neu starten

**Problem: Programme werden nicht ausgeführt**
- **Lösung:**
  - Prüfen Sie, ob Programme in Datenbank vorhanden sind
  - Import aus Excel durchführen
  - Prüfen Sie Zeit und Datum
  - Prüfen Sie Wochentags-Filter

**Problem: Feiertage werden nicht erkannt**
- **Lösung:**
  - Prüfen Sie, ob Feiertage in Datenbank vorhanden sind
  - Import aus Excel durchführen
  - Prüfen Sie Tagtyp-Zuordnung

**Problem: Platinen nicht erreichbar**
- **Lösung:**
  - Prüfen Sie IP-Adressen und Ports
  - Prüfen Sie Netzwerk-Verbindung
  - Prüfen Sie, ob Platinen aktiv sind

**Problem: WRITE_SECURE_SETTINGS Permission fehlt**
- **Lösung:**
  - Siehe Kapitel 10.2
  - Permission über ADB vergeben

### 36.2 Log-Dateien

**Speicherort:**
- `/sdcard/Turmtechnik/Logs/`

**Logcat (Android):**
```bash
adb logcat | grep Turmtechnik
```

**Wichtige Log-Tags:**
- `TurmtechnikActivity`
- `ConfigWebServer`
- `PlatinenDatabaseHelper`
- `TagesSuche`
- `UhrThread`

### 36.3 Debug-Modus

**Aktivierung:**
- `StaticConstants.java`: `DEBUG = true`
- App neu kompilieren und installieren

**Auswirkungen:**
- Ausführlichere Logs
- Mehr Debug-Informationen

### 36.4 Support-Informationen

**Bei Problemen bitte bereitstellen:**
- Android-Version
- App-Version
- Log-Dateien
- Beschreibung des Problems
- Schritte zur Reproduktion

**Kontakt:**
- Siehe Projekt-Dokumentation für Support-Kontakt

---

## Ende der Dokumentation

**Version:** 1.0  
**Datum:** 2026  
**Projekt:** Turmtechnik Android App

**Hinweis:** Diese Dokumentation wird kontinuierlich aktualisiert. Bitte prüfen Sie regelmäßig auf neue Versionen.
