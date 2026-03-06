# Logcat-Übersicht – Turmtechnik-App

Kurzüberblick, was im Android Logcat (Tag `tom.turmtechnik`) so erscheinen kann, gruppiert nach Thema.

---

## 1. Carambola / WLAN-Platine (Tag: `carambola`)

| Stufe | Meldung | Bedeutung |
|-------|---------|-----------|
| **E** | `connect O.K. Platine=0` | TCP-Verbindung zur Platine (0 = erste Platine) aufgebaut |
| **E** | `connect Error nach 3 Versuchen Platine=0` | Verbindung nach 3 Versuchen fehlgeschlagen (ESP aus? falsche IP? WLAN weg?) |
| **E** | `Antwort O.K. (impulsok) Platine=0 nach X ms` | ESP hat „impulsok“ zurückgeschickt, Antwortzeit X ms |
| **E** | `Antwort O.K. Platine=0 nach X ms` | ESP hat gültige Antwort (≥8 Bytes) geliefert (ohne „impulsok“-Konfiguration) |
| **E** | `Keine Antwort Platine=0 (Abbruch nach 26 Versuchen)` | Nach 26 Prüfdurchläufen keine gültige Antwort → Status wird auf „Fehler“ gesetzt |

**Hinweis:** Bei Bluetooth-Modus laufen andere Threads; dann erscheinen ggf. `carambola`-Meldungen nicht.

---

## 2. Serial I/O – Bluetooth (Tag: `carambola` im Serial_IoThread)

Diese Meldungen kommen nur, wenn **Bluetooth-Modus** aktiv ist:

| Stufe | Meldung | Bedeutung |
|-------|---------|-----------|
| **E** | `connect O.K.` | Bluetooth-Verbindung steht |
| **E** | `connect Error` | Bluetooth-Verbindung fehlgeschlagen |
| **E** | `antwort versuche=X` | Warten auf Antwort vom BT-Modul (X = Zähler) |
| **E** | `Antwort O.K.` / `keine Antwort` | Antwort gelesen oder nicht (alte Logik) |
| **E** | `RelaisState` + Binärstring | Aktueller Relais-Zustand (zu Debug-Zwecken) |

---

## 3. Web-Server / Platinen-Config (Tag: `ConfigWebServer`)

| Stufe | Meldung | Bedeutung |
|-------|---------|-----------|
| **I** | `Web-Server gestartet auf Port 8080` | Interner Web-Server läuft |
| **I** | `Web-UI erreichbar unter: http://...` | Aufruf-URL der Web-UI |
| **D** | `Request: GET/PUT ...` | Jede HTTP-Anfrage (bei Debug-Ausgaben) |
| **D** | `Modus gespeichert: wifi/bluetooth ...` | Modus in der Platinen-Config gespeichert |
| **E** | `Fehler beim ...` | Fehler bei einer API-Anfrage (z. B. Platinen, Programme, Import) |

---

## 4. TurmtechnikActivity / Start & Modus (Tag: `TurmtechnikActivity` u. a.)

| Stufe | Tag | Meldung | Bedeutung |
|-------|-----|---------|-----------|
| **D** | TurmtechnikActivity | `loadPlatinenIpListFromDb: WiFi, ipList.size=X` | Beim Start oder nach Speichern: X WLAN-Platinen aus DB geladen |
| **D** | TurmtechnikActivity | `Modus aus DB: wifi -> bluetoothMode=false` | Modus beim Start aus Datenbank gelesen |
| **D** | TurmtechnikActivity | `scanRelaisMS aus DB: 200 ms (Excel überschrieben)` | Scan-Relais-Zeit aus DB statt aus Excel |
| **D** | TurmtechnikActivity | `onDestroy` | Activity wird beendet |

---

## 5. Datenbank (Tag: `PlatinenDatabaseHelper`)

| Stufe | Meldung | Bedeutung |
|-------|---------|-----------|
| **D** | `Modus gesetzt: wifi/bluetooth` | Modus in io_config gespeichert |
| **D** | `scan_relais_ms gesetzt: 200` | Scan-Relais-Wert in DB gespeichert |
| **D** | `Erstelle Datenbank-Tabellen ...` | DB wird angelegt/aktualisiert |
| **W** / **E** | diverse Migrations-/Fehlermeldungen | DB-Upgrade oder Fehler |

---

## 6. Weitere Tags (Auswahl)

- **checkMelodien** – Programme/Melodien aus DB oder Excel
- **nebenuhrSave** / **nebenuhrLoad** – Nebenuhr-Zeiten und Relais-Status
- **VorschwingenLoad** / **ensureVorschwingen** – Vorschwingen-Konfiguration
- **ConfigWebServer** – sehr viele Einträge bei Fehlern (Import, Export, API, etc.)
- **F I L E**, **START**, **beenden**, **serialGPS** – Start, Dateien, Beenden, GPS-Serial

---

## Nur relevante Carambola/Platinen-Meldungen im Logcat anzeigen

**Android Studio / Logcat:** Filter z. B.:

- `tag:carambola` – nur Carambola/WLAN-Platine
- `tag:ConfigWebServer` – nur Web-Server
- `tag:TurmtechnikActivity` – nur Activity (Modus, DB-Laden)
- `package:tom.turmtechnik` – alles von der App

**adb:**

```bash
adb logcat -s carambola:E ConfigWebServer:I TurmtechnikActivity:D
```

Damit siehst du vor allem Verbindung, Antwortzeiten und Modus/DB-Meldungen.
