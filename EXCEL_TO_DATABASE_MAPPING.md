# Excel zu Datenbank Mapping

Diese Datei dokumentiert, welche Excel-Zellen aus `System.xls` in welche Datenbankfelder migriert werden.

## System.xls Sheet 14 - Platinen & IO-Konfiguration

| Excel-Zelle | Beschreibung | Datenbank-Tabelle | Datenbank-Feld | Status | Migration-Methode |
|-------------|--------------|-------------------|----------------|--------|-------------------|
| (0, 1) | Bluetooth Modus ("AUS" = bluetooth, sonst wifi) | `io_config` | `key="modus"`, `value="bluetooth"` oder `"wifi"` | ✅ Migriert | `PlatinenDatabaseHelper.migrateFromExcel()` |
| (1, 1) | IP-Adresse Platine 1 | `platinen_config` | `platine_nummer=1`, `ip_adresse` | ✅ Migriert | `PlatinenDatabaseHelper.migrateFromExcel()` |
| (2, 1) | IP-Adresse Platine 2 | `platinen_config` | `platine_nummer=2`, `ip_adresse` | ✅ Migriert | `PlatinenDatabaseHelper.migrateFromExcel()` |
| (3, 1) | IP-Adresse Platine 3 | `platinen_config` | `platine_nummer=3`, `ip_adresse` | ✅ Migriert | `PlatinenDatabaseHelper.migrateFromExcel()` |
| (4, 1) | IP-Adresse Platine 4 | `platinen_config` | `platine_nummer=4`, `ip_adresse` | ✅ Migriert | `PlatinenDatabaseHelper.migrateFromExcel()` |
| (5, 1) | Port Platine 1 | `platinen_config` | `platine_nummer=1`, `port` | ✅ Migriert | `PlatinenDatabaseHelper.migrateFromExcel()` |
| (6, 1) | Port Platine 2 | `platinen_config` | `platine_nummer=2`, `port` | ✅ Migriert | `PlatinenDatabaseHelper.migrateFromExcel()` |
| (7, 1) | Port Platine 3 | `platinen_config` | `platine_nummer=3`, `port` | ✅ Migriert | `PlatinenDatabaseHelper.migrateFromExcel()` |
| (8, 1) | Port Platine 4 | `platinen_config` | `platine_nummer=4`, `port` | ✅ Migriert | `PlatinenDatabaseHelper.migrateFromExcel()` |
| (1, 2) | Scan-Relais MS | `io_config` | `key="scan_relais_ms"`, `value` | ❌ Noch nicht migriert | Geplant |
| (1, 3) | Power off Akku % | `io_config` | `key="power_off_akku_prozent"`, `value` | ✅ Migriert | `PlatinenDatabaseHelper.migrateFromExcel()` |
| (1, 4) | Logfile On/Off | `io_config` | `key="logfile_on_off"`, `value` | ❌ Noch nicht migriert | Geplant |
| (1, 5) | Reboot OK Flag | `io_config` | `key="reboot_ok_flag"`, `value` | ❌ Noch nicht migriert | Geplant |
| (1, 6) | Autostart der App | `io_config` | `key="autostart_der_app"`, `value` | ❌ Noch nicht migriert | Geplant |
| (2, 9) | Password Exit Normal | `io_config` | `key="password_exit_normal"`, `value` | ❌ Noch nicht migriert | Geplant |

### Nicht mehr verwendete Zellen (werden deaktiviert/entfernt):
- (1, 7) - Internet On Zeit → **Wird entfernt**
- (2, 7) - Internet Off Zeit → **Wird entfernt**
- (2, 10) - Password Exit + Delete → **Wird entfernt**

## System.xls Sheet 15 - Nebenuhr-Konfiguration

| Excel-Zelle | Beschreibung | Datenbank-Tabelle | Datenbank-Feld | Status | Migration-Methode |
|-------------|--------------|-------------------|----------------|--------|-------------------|
| (0, 3) | Impuls-Zeit Nebenuhr A (Sekunden) | **Noch nicht in DB** | - | ❌ Noch nicht migriert | Geplant: `nebenuhr_config` |
| (1, 3) | Impuls-Pause Nebenuhr A (Sekunden) | **Noch nicht in DB** | - | ❌ Noch nicht migriert | Geplant: `nebenuhr_config` |
| (3, 3) | Relais-Nummer (gerade) Nebenuhr A | **Noch nicht in DB** | - | ❌ Noch nicht migriert | Geplant: `nebenuhr_config` |
| (4, 3) | Relais-Nummer (ungerade) Nebenuhr A | **Noch nicht in DB** | - | ❌ Noch nicht migriert | Geplant: `nebenuhr_config` |
| (5, 3) | Uhr-Name Nebenuhr A | **Noch nicht in DB** | - | ❌ Noch nicht migriert | Geplant: `nebenuhr_config` |
| (0, 4) | Impuls-Zeit Nebenuhr B (Sekunden) | **Noch nicht in DB** | - | ❌ Noch nicht migriert | Geplant: `nebenuhr_config` |
| (1, 4) | Impuls-Pause Nebenuhr B (Sekunden) | **Noch nicht in DB** | - | ❌ Noch nicht migriert | Geplant: `nebenuhr_config` |
| (3, 4) | Relais-Nummer (gerade) Nebenuhr B | **Noch nicht in DB** | - | ❌ Noch nicht migriert | Geplant: `nebenuhr_config` |
| (4, 4) | Relais-Nummer (ungerade) Nebenuhr B | **Noch nicht in DB** | - | ❌ Noch nicht migriert | Geplant: `nebenuhr_config` |
| (5, 4) | Uhr-Name Nebenuhr B | **Noch nicht in DB** | - | ❌ Noch nicht migriert | Geplant: `nebenuhr_config` |
| (0, 5) | Impuls-Zeit Nebenuhr C (Sekunden) | **Noch nicht in DB** | - | ❌ Noch nicht migriert | Geplant: `nebenuhr_config` |
| (1, 5) | Impuls-Pause Nebenuhr C (Sekunden) | **Noch nicht in DB** | - | ❌ Noch nicht migriert | Geplant: `nebenuhr_config` |
| (3, 5) | Relais-Nummer (gerade) Nebenuhr C | **Noch nicht in DB** | - | ❌ Noch nicht migriert | Geplant: `nebenuhr_config` |
| (4, 5) | Relais-Nummer (ungerade) Nebenuhr C | **Noch nicht in DB** | - | ❌ Noch nicht migriert | Geplant: `nebenuhr_config` |
| (5, 5) | Uhr-Name Nebenuhr C | **Noch nicht in DB** | - | ❌ Noch nicht migriert | Geplant: `nebenuhr_config` |

**Hinweis**: Spalte 2 (Uhr-Modus) wird nicht mehr verwendet - immer 12-Stunden-Modus.

## Datenbank-Schema

### Tabelle: `io_config`
Key-Value-Paare für globale Konfiguration:
- `key` (TEXT PRIMARY KEY)
- `value` (TEXT)
- `updated_at` (TIMESTAMP)

### Tabelle: `platinen_config`
- `id` (INTEGER PRIMARY KEY)
- `platine_nummer` (INTEGER UNIQUE)
- `ip_adresse` (TEXT)
- `port` (INTEGER)
- `aktiv` (BOOLEAN)
- `mac_adresse` (TEXT)
- `online` (BOOLEAN)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)

### Geplante Tabelle: `nebenuhr_config`
```sql
CREATE TABLE nebenuhr_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    uhr_name TEXT NOT NULL,                -- "A", "B", "C"
    zeile_index INTEGER NOT NULL,          -- Excel-Zeile (3, 4, 5)
    impuls_zeit INTEGER,                   -- Sekunden (Spalte 0)
    impuls_pause INTEGER,                  -- Sekunden (Spalte 1)
    relais_gerade INTEGER,                 -- Relais-Nummer (Spalte 3)
    relais_ungerade INTEGER,               -- Relais-Nummer (Spalte 4)
    uhr_name_display TEXT,                 -- Anzeige-Name (Spalte 5)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(uhr_name, zeile_index)
);
```

## Migration-Strategie

1. **Beim ersten Start der App** (`onCreate()`):
   - Prüfe ob `System.xls` existiert
   - Wenn ja: Migriere alle konfigurierten Felder
   - Wenn nein: Verwende Default-Werte

2. **Bei Datenbank-Upgrade** (`onUpgrade()`):
   - Prüfe ob neue Felder hinzugefügt wurden
   - Migriere fehlende Werte aus Excel (falls vorhanden)

3. **Nach Migration**:
   - App liest nur noch aus Datenbank
   - Excel wird nicht mehr geschrieben (außer für Kompatibilität)

## Code-Referenzen

- **Platinen-Migration**: `PlatinenDatabaseHelper.migrateFromExcel()`
- **Nebenuhr-Migration**: Noch nicht implementiert
- **IO-Config-Migration**: Teil von `PlatinenDatabaseHelper.migrateFromExcel()`
