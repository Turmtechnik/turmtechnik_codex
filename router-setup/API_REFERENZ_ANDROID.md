# API-Referenz (Android-Projekt)

Die Turmtechnik-Android-App stellt unter `ConfigWebServer.java` eine REST-API bereit. Diese Referenz listet die Endpunkte, damit sie schrittweise auf dem Router (Beryl AX) nachgebaut werden können.

**Basis-URL Android:** `http://<Gerät-IP>:8080`  
**Basis-URL Router:** `http://<Router-IP>/cgi-bin/turmtechnik-api` (bzw. zukünftige API-Routen)

---

## Programme

| Methode | Pfad | Beschreibung |
|--------|------|--------------|
| GET | `/api/programme/count` | Anzahl Programme |
| GET | `/api/programme?tagtyp=...` | Programme eines Tagtyps |
| GET | `/api/programme/:id?tagtyp=...` | Einzelnes Programm |
| POST | `/api/programme` | Programm anlegen |
| PUT | `/api/programme/:id` | Programm aktualisieren |
| DELETE | `/api/programme/:id` | Programm löschen |
| POST | `/api/programme/delete-all?typ=normal` | Alle (Normal-)Programme löschen |
| POST | `/api/programme/reload?tagtyp=...` | Reload |
| POST | `/api/programme/import-all` | Import aller Programmtage |
| POST | `/api/programme/copy?tagtypName=...&sourceTagtyp=...` | Kopieren |

## Feiertage

| Methode | Pfad | Beschreibung |
|--------|------|--------------|
| GET/PUT | `/api/osterfeiertage` | Osterfeiertage (variable) |
| POST | `/api/osterfeiertage/migrate` | Migration aus Excel |
| GET/PUT | `/api/feste-feiertage` | Feste Feiertage |
| POST | `/api/feste-feiertage/migrate` | Migration aus Excel |
| GET/PUT | `/api/sonderfeiertage` | Sonderfeiertage (verschoben) |
| GET/PUT/DELETE | `/api/sonderfeiertage/:id` | Einzelner Sonderfeiertag |
| POST | `/api/sonderfeiertage/berechnen` | Sonderfeiertage berechnen |
| POST | `/api/sonderfeiertage/neuberechnen` | Neuberechnung aller |
| POST | `/api/sonderfeiertage/alle-loeschen` | Alle löschen |

## Tagtypen & Tasten

| Methode | Pfad | Beschreibung |
|--------|------|--------------|
| GET | `/api/tagtypen` | Alle Tagtypen |
| GET | `/api/verknuepfte-tasten` | Verknüpfte Tasten |
| GET | `/api/verknuepfte-tasten-pruefung` | Prüfung Tasten ↔ Programme |
| GET | `/api/schwingen-tasten` | Tasten mit Funktion Schwingen |
| GET | `/api/melodie-tasten` | Tasten mit Funktion Melodie |
| GET | `/api/ausgang-tasten` | Tasten Typ Ausgang |
| GET/PUT | `/api/beschriftung-tasten` | Beschriftung Tasten (GET oder ersetzen) |
| POST | `/api/beschriftung-tasten/import-from-excel` | Excel-Upload |
| POST | `/api/beschriftung-tasten/import-from-device` | Excel vom Gerät |
| GET/PUT | `/api/soforttasten` | Soforttasten |
| GET/PUT | `/api/soforttasten/:index` | Einzelne Soforttaste |

## Platinen & Hardware

| Methode | Pfad | Beschreibung |
|--------|------|--------------|
| GET/PUT | `/api/platinen` | Platinen-Konfiguration |
| GET/PUT/DELETE | `/api/platinen/:nummer` | Einzelne Platine |
| GET/PUT | `/api/platinen/config` | Modus (wifi/bluetooth) |
| GET | `/api/platinen/scan-relais` | Scan-Relais-Wert |

## Nebenuhren

| Methode | Pfad | Beschreibung |
|--------|------|--------------|
| GET | `/api/nebenuhren` | Nebenuhr-Konfiguration |
| PUT | `/api/nebenuhren` | Nebenuhr-Konfiguration speichern |

## Vorschwingen

| Methode | Pfad | Beschreibung |
|--------|------|--------------|
| GET/PUT | `/api/vorschwingen` | Vorschwingen-Konfiguration |
| DELETE | `/api/vorschwingen/:id` | Einzelner Eintrag löschen |
| POST | `/api/vorschwingen/migrate` | Migration aus Excel |

## Schlagwerk

| Methode | Pfad | Beschreibung |
|--------|------|--------------|
| GET/PUT | `/api/schlagwerk` | Schlagwerk (Typ 1 & 2) |

## MIDI & Melodien

| Methode | Pfad | Beschreibung |
|--------|------|--------------|
| GET/PUT | `/api/midi-config` | MIDI-Datei-Konfiguration |
| GET/DELETE | `/api/midi-config/:id` | Einzelne MIDI-Config |
| POST | `/api/midi-config/upload` | MIDI-Datei-Upload |
| GET/PUT | `/api/midi-noten-relais` | Noten-zu-Relais |
| POST | `/api/midi-noten-relais/test` | Test-Impuls Note |
| GET/POST | `/api/melodien` | Melodien |
| GET/PUT/DELETE | `/api/melodien/:id` | Einzelne Melodie |
| GET/POST | `/api/melodien/:id/zeilen` | Zeilen einer Melodie |
| GET/PUT/DELETE | `/api/melodien/zeilen/:zeileId` | Einzelne Melodie-Zeile |
| POST | `/api/melodien/import` | Excel-Import |
| POST | `/api/melodien/import-all` | Alle aus Ordner |
| POST | `/api/melodien/import-json` | JSON-Import |
| GET | `/api/melodien/:id/export` | Melodie als JSON exportieren |

## Anlage & System

| Methode | Pfad | Beschreibung |
|--------|------|--------------|
| GET/PUT | `/api/anlagendaten` | Baustelle, Installationsdatum |
| GET/PUT | `/api/fernsteuern-config` | Fernsteuern, Zeitserver, Serial-GPS |
| GET/PUT | `/api/system-auto-time` | Zeit aus Internet (AUTO_TIME) |
| GET/PUT | `/api/anlagenstandort` | Standort, Sonnenauf-/untergang |
| GET/PUT | `/api/power-off-akku` | Power-off Akku Prozent |
| GET | `/api/backup` | Datenbank herunterladen |
| POST | `/api/restore` | Datenbank wiederherstellen |
| POST | `/api/reset-to-factory` | Werkszustand |
| GET | `/api/sound-files` | Sound-Dateien (Turmtechnik/Sound/) |

---

## Nutzung für den Router

- Zuerst **Schema** anlegen: `schema-android.sql` (wenn SQLite auf dem Router verfügbar oder auf PC).
- **Priorität für Router-API:** z. B. `/api/status`, `/api/platinen`, `/api/nebenuhren`, `/api/tagtypen`, `/api/programme` (GET), `/api/vorschwingen`, `/api/schlagwerk` – dann gleiche Web-UI wie Android nutzbar, wenn die Startseite (Index) auf dem Router ausgeliefert wird.
