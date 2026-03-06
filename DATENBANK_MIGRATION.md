# Datenbank-Migration: Excel → SQLite

## Übersicht der aktuellen Excel-Dateien

### Programm-Dateien
1. **Normalprogramm.xls** - Tagesprogramme
2. **Variable Festtage.xls** - Variable Feiertage
3. **Feste Festtage.xls** - Feste Feiertage
4. **Benutzermelodien.xls** - Benutzerdefinierte Programme

### Konfigurations-Dateien
5. **System.xls** (Multi-Sheet) - Systemkonfiguration
   - Sheet 0-13: Verschiedene Konfigurationen
   - Sheet 14: Bluetooth-Konfiguration
6. **Nebenuhr.xls** - Nebenuhr-Konfiguration
7. **Schlagwerkzeiten.xls** - Schlagwerk-Konfiguration
8. **Beschriftung Tasten.xls** - Button-Konfiguration

### Melodie-Dateien
9. **Melodien/*.xls** - Einzelne Melodie-Dateien (jede Melodie eine Datei)

## Datenbank-Design-Vorschlag

### 1. Programm-Tabellen

#### Tabelle: `programme`
```sql
CREATE TABLE programme (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    startzeit TEXT NOT NULL,              -- "HH:MM:SS" oder "SA"/"SU"
    funktion TEXT NOT NULL,                -- "Melodie" oder "Heizung"
    melodie_name TEXT,                     -- NULL wenn Heizung
    dauer_heizung TEXT,                    -- "hh:mm:ss" Format, NULL wenn Melodie
    montag BOOLEAN DEFAULT 0,              -- Statt "x" → true/false
    dienstag BOOLEAN DEFAULT 0,
    mittwoch BOOLEAN DEFAULT 0,
    donnerstag BOOLEAN DEFAULT 0,
    freitag BOOLEAN DEFAULT 0,
    samstag BOOLEAN DEFAULT 0,
    sonntag BOOLEAN DEFAULT 0,
    immer BOOLEAN DEFAULT 0,               -- "1" → true
    periodisch INTEGER DEFAULT 0,          -- 0=immer, 1=Sommer, 2=Winter
    start_datum TEXT,                      -- "TT.MM" oder "TT.MM.JJJJ.A" oder "TT.T"
    ende_datum TEXT,                       -- "TT.MM" oder "TT.MM.JJJJ.A" oder "TT.T"
    verknuepfte_taste TEXT,                 -- NULL wenn keine
    prioritaet INTEGER DEFAULT 0,          -- 1-9
    programm_typ TEXT NOT NULL,            -- "normal", "festtag_variabel", "festtag_fest", "benutzer"
    festtag_datum TEXT,                    -- Für Feiertage: "TT.MM" oder "TT.MM.JJJJ"
    festtag_jahr INTEGER,                  -- Für variable Feiertage
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_programme_startzeit ON programme(startzeit);
CREATE INDEX idx_programme_prioritaet ON programme(prioritaet DESC);
CREATE INDEX idx_programme_typ ON programme(programm_typ);
```

### 2. Melodie-Tabellen

#### Tabelle: `melodien`
```sql
CREATE TABLE melodien (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,             -- Dateiname ohne .xls
    vorlauf_minuten INTEGER DEFAULT 0,     -- Aus Spalte B, Zeile 1
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Tabelle: `melodie_zeilen`
```sql
CREATE TABLE melodie_zeilen (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    melodie_id INTEGER NOT NULL,
    zeile_index INTEGER NOT NULL,          -- Excel-Zeile (0-basiert)
    zeit_offset TEXT,                      -- "HH:MM:SS" Format (Spalte 0)
    wartezeit_hundertstel INTEGER,         -- Spalte 3
    kloeppel_a TEXT,                       -- Spalte 4
    kloeppel_b TEXT,                       -- Spalte 5
    kloeppel_c TEXT,                       -- Spalte 6
    kloeppel_d TEXT,                       -- Spalte 7
    kloeppel_e TEXT,                       -- Spalte 8
    kloeppel_f TEXT,                       -- Spalte 9
    kloeppel_g TEXT,                       -- Spalte 10
    kloeppel_h TEXT,                       -- Spalte 11
    sound_mp3 TEXT,                        -- Spalte 21
    melodie_xls TEXT,                      -- Spalte 20
    FOREIGN KEY (melodie_id) REFERENCES melodien(id) ON DELETE CASCADE,
    UNIQUE(melodie_id, zeile_index)
);

CREATE INDEX idx_melodie_zeilen_melodie ON melodie_zeilen(melodie_id);
```

### 3. Konfigurations-Tabellen

#### Tabelle: `system_config`
```sql
CREATE TABLE system_config (
    key TEXT PRIMARY KEY,
    value TEXT,
    sheet_number INTEGER,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Tabelle: `nebenuhr_config`
```sql
CREATE TABLE nebenuhr_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    uhr_name TEXT NOT NULL,                -- "A", "B", "C"
    zeile_index INTEGER NOT NULL,
    spalte_index INTEGER NOT NULL,
    wert TEXT,
    UNIQUE(uhr_name, zeile_index, spalte_index)
);
```

#### Tabelle: `schlagwerk_config`
```sql
CREATE TABLE schlagwerk_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    zeile_index INTEGER NOT NULL,
    stunde INTEGER NOT NULL,
    minute INTEGER NOT NULL,
    aktiv BOOLEAN DEFAULT 1,
    UNIQUE(zeile_index, stunde, minute)
);
```

#### Tabelle: `beschriftung_tasten`
```sql
CREATE TABLE beschriftung_tasten (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    zeile_index INTEGER NOT NULL,
    name TEXT,                             -- Spalte 2
    relais_nummer INTEGER,                 -- Spalte 3
    platine_nummer INTEGER DEFAULT 1,      -- Spalte 5
    UNIQUE(zeile_index)
);
```

## Was muss beachtet werden?

### 1. Code-Änderungen

#### A. Ersetzung von ExcelRead/ExcelWrite
**Betroffene Dateien (38 Dateien):**
- `TagesSuche.java` - Hauptlogik für Programmsuche
- `UhrThread.java` - Programmausführung
- `MelodieThreadNew.java` - Melodie-Ausführung
- `EditorActivity.java` - Programm-Editor
- `ProgrammKontrolleActivity.java` - Programm-Kontrolle
- `ExcelRead.java` - Muss durch DAO ersetzt werden
- `ExcelReadWrite.java` - Muss durch DAO ersetzt werden
- `ExcelWrite.java` - Muss durch DAO ersetzt werden
- Weitere 30+ Dateien

**Neue Klassen benötigt:**
```java
// Data Access Objects (DAO)
ProgrammDao.java
MelodieDao.java
ConfigDao.java
NebenuhrDao.java
SchlagwerkDao.java
BeschriftungTastenDao.java

// Database Helper
TurmtechnikDatabase.java (Room Database)
```

#### B. Abstraktionsschicht
**Empfehlung:** Repository-Pattern einführen
```java
public interface ProgrammRepository {
    List<Programm> findProgrammeByTime(int stunde, int minute);
    Programm findProgrammById(int id);
    void saveProgramm(Programm programm);
    void deleteProgramm(int id);
}
```

### 2. Daten-Migration

#### A. Excel → SQLite Import
**Benötigt:**
- Einmaliger Import-Script/Activity
- Validierung der importierten Daten
- Backup-Mechanismus

**Herausforderungen:**
- Komplexe Datumsformate (`"TT.MM.JJJJ.A"`, `"TT.T"`)
- Sonderwerte (`"SA"`, `"SU"` für Sonnenaufgang/Untergang)
- Fehlende Spalte D (dynamische Spaltenposition)

#### B. Datenvalidierung
- Alle Programme müssen validiert werden
- Melodien müssen existieren
- Verknüpfte Tasten müssen existieren

### 3. Performance-Überlegungen

#### Vorteile:
- ✅ **Schnellere Suche**: SQL-Indexe statt sequenzielles Durchsuchen
- ✅ **Weniger Speicher**: Keine Excel-Dateien im RAM
- ✅ **Parallele Zugriffe**: SQLite unterstützt mehrere Reader
- ✅ **Transaktionen**: Atomare Updates

#### Herausforderungen:
- ⚠️ **Initiale Migration**: Einmaliger Aufwand
- ⚠️ **Code-Refactoring**: Viele Dateien müssen angepasst werden
- ⚠️ **Testing**: Umfangreiches Testen erforderlich

### 4. Remote Control Integration

#### Vorteile:
- ✅ **Einfache Synchronisation**: SQLite kann einfach synchronisiert werden
- ✅ **Inkrementelle Updates**: Nur geänderte Einträge übertragen
- ✅ **Konfliktauflösung**: Timestamps für Last-Write-Wins
- ✅ **Offline-First**: Lokale DB, dann Sync

#### Implementierung:
```java
// Sync-Strategie
public class DatabaseSync {
    public void syncFromRemote(List<Programm> remoteProgramme) {
        // Merge-Strategie: Remote hat Priorität
        // Oder: Konfliktauflösung mit Timestamps
    }
    
    public List<Programm> getChangesSince(Timestamp lastSync) {
        // Nur geänderte Programme zurückgeben
    }
}
```

### 5. Backward Compatibility

#### Option A: Hybrid-Ansatz (Empfohlen)
- Datenbank als primäre Quelle
- Excel-Export für Backup/Manuelle Bearbeitung
- Excel-Import für Migration bestehender Dateien

#### Option B: Vollständige Migration
- Kompletter Umstieg auf Datenbank
- Excel-Support entfernen
- Einmaliger Migrationsprozess

### 6. Spezielle Herausforderungen

#### A. Komplexe Datumsformate
**Problem:** `"23.03.2016.7.A"` (alle 7 Tage ab 23.03.2016)

**Lösung:**
```sql
-- Separate Felder für Wiederholungen
ALTER TABLE programme ADD COLUMN wiederholung_start_datum TEXT;
ALTER TABLE programme ADD COLUMN wiederholung_alle_tage INTEGER;
ALTER TABLE programme ADD COLUMN wiederholung_typ TEXT; -- "A" = alle X Tage
```

#### B. Sonnenaufgang/Untergang
**Problem:** `"SA"` und `"SU"` als Startzeit

**Lösung:**
```sql
-- Enum oder separate Spalte
ALTER TABLE programme ADD COLUMN startzeit_typ TEXT; -- "normal", "sonnenaufgang", "sonnenuntergang"
-- startzeit bleibt für normale Zeiten
```

#### C. Dynamische Spalten (fehlende Spalte D)
**Problem:** Spalte D kann fehlen, Wochentage verschieben sich

**Lösung:**
- In Datenbank immer vorhanden (NULL = nicht vorhanden)
- Keine Verschiebung mehr nötig

### 7. Migrations-Strategie

#### Phase 1: Vorbereitung (1-2 Wochen)
1. Datenbank-Schema erstellen
2. DAO-Klassen implementieren
3. Repository-Pattern einführen
4. Unit-Tests schreiben

#### Phase 2: Parallel-Betrieb (2-3 Wochen)
1. Datenbank neben Excel betreiben
2. Beide Systeme synchron halten
3. Validierung: Beide liefern gleiche Ergebnisse

#### Phase 3: Migration (1 Woche)
1. Excel → SQLite Import-Tool
2. Daten validieren
3. Backup erstellen

#### Phase 4: Umstellung (2-3 Wochen)
1. Code schrittweise umstellen
2. Excel-Code entfernen
3. Testing

#### Phase 5: Cleanup (1 Woche)
1. Excel-Dateien optional behalten (Backup)
2. Code aufräumen
3. Dokumentation aktualisieren

### 8. Empfohlene Technologie

#### Room Persistence Library (Android)
**Vorteile:**
- ✅ Offizielle Android-Bibliothek
- ✅ Type-safe SQL-Queries
- ✅ LiveData/Flow Integration
- ✅ Einfache Migrationen
- ✅ Compile-time Validierung

**Dependencies:**
```gradle
dependencies {
    def room_version = "2.6.1"
    implementation "androidx.room:room-runtime:$room_version"
    annotationProcessor "androidx.room:room-compiler:$room_version"
    // Optional: Kotlin Extensions
    implementation "androidx.room:room-ktx:$room_version"
}
```

### 9. Code-Beispiel: Migration

#### Vorher (Excel):
```java
// TagesSuche.java
excelread2.openXls(excelTableFileName);
String startzeit = excelread2.getCellString(SPALTE_A_STARTZEIT, zeile);
String melodie = excelread2.getCellString(SPALTE_C_MELODIE_NAME, zeile);
```

#### Nachher (Datenbank):
```java
// ProgrammRepository.java
@Dao
public interface ProgrammDao {
    @Query("SELECT * FROM programme WHERE startzeit = :startzeit AND programm_typ = :typ")
    List<Programm> findProgrammeByTime(String startzeit, String typ);
    
    @Query("SELECT * FROM programme WHERE id = :id")
    Programm findById(int id);
}

// TagesSuche.java (angepasst)
ProgrammRepository repo = new ProgrammRepositoryImpl(context);
List<Programm> programme = repo.findProgrammeByTime("12:00:00", "normal");
for (Programm p : programme) {
    if (wochenTagOk(p) && verknuepfteTasteOk(p) && ...) {
        return p.getMelodieName();
    }
}
```

### 10. Risiken und Mitigation

| Risiko | Wahrscheinlichkeit | Impact | Mitigation |
|--------|-------------------|--------|------------|
| Datenverlust bei Migration | Niedrig | Hoch | Backup + Validierung + Rollback |
| Performance-Probleme | Niedrig | Mittel | Indexe + Query-Optimierung |
| Code-Fehler | Mittel | Hoch | Umfangreiches Testing + Staging |
| Kompatibilität | Niedrig | Mittel | Hybrid-Ansatz (Excel-Export) |
| Remote-Sync-Konflikte | Mittel | Mittel | Timestamp-basierte Konfliktauflösung |

### 11. Vorteile der Migration

✅ **Performance**: SQL-Indexe statt sequenzielle Suche
✅ **Wartbarkeit**: Strukturierte Daten statt Excel-Dateien
✅ **Skalierbarkeit**: Einfach erweiterbar
✅ **Remote Control**: Einfache Synchronisation
✅ **Datenintegrität**: Foreign Keys, Constraints
✅ **Backup**: Einfache SQLite-Backups
✅ **Offline-First**: Funktioniert ohne Internet

### 12. Nachteile / Herausforderungen

⚠️ **Aufwand**: Große Code-Änderungen (38+ Dateien)
⚠️ **Migration**: Einmaliger Import-Prozess
⚠️ **Testing**: Umfangreiches Testen erforderlich
⚠️ **Lernkurve**: Team muss Room/SQLite lernen
⚠️ **Excel-Kompatibilität**: Verloren (außer Export)

## Empfehlung

**Schrittweise Migration mit Hybrid-Ansatz:**

1. **Phase 1**: Datenbank parallel zu Excel betreiben
2. **Phase 2**: Neue Features nur noch in Datenbank
3. **Phase 3**: Bestehende Features schrittweise migrieren
4. **Phase 4**: Excel als Export-Option behalten
5. **Phase 5**: Excel-Import für Migration bestehender Dateien

**Technologie:** Room Persistence Library (Android)

**Zeitaufwand:** 6-8 Wochen für vollständige Migration

**Priorität:** Mittel (wenn Remote Control wichtig ist, dann Hoch)
