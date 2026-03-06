# Refactoring-Plan: StaticVariable.java aufteilen

## Status: Config-Klassen erstellt, Migration vorbereitet

### ✅ Abgeschlossen

1. **8 neue Config-Klassen erstellt:**
   - `TimeConfig.java` - Zeitserver, GPS, Uhren (Uhr A/B/C), Sonnenaufgang/Untergang
   - `SoundConfig.java` - SoundPool, SoundGlocke, Sound-Listen
   - `UIState.java` - Layout, Buttons, Info-Text, UI-Zustand, Editor
   - `MelodyConfig.java` - Melodien, Benutzerprogramme, Vorschwingen, Manueller Start
   - `IOConfig.java` - Bluetooth, Serial, Carambola, Internet
   - `HeatingConfig.java` - Heizung-Status, Timer, Relais
   - `SchlagwerkConfig.java` - Schlagwerk-Zeiten, Status
   - `SystemConfig.java` - System-Status, Hammer, Excel, Thread-Status

### 📋 Nächste Schritte (Schrittweise Migration)

#### Phase 1: Statische Variablen durch direkte Referenzen ersetzen
**Ziel:** Variablen in `StaticVariable.java` durch Referenzen zu Config-Klassen ersetzen

**Beispiel:**
```java
// Vorher:
public static boolean timeServerOk = false;

// Nachher:
public static boolean timeServerOk = TimeConfig.timeServerOk;
```

**Vorteil:** Code funktioniert weiterhin, aber Variablen sind jetzt in Config-Klassen organisiert

**Nachteil:** Beide Variablen müssen synchron gehalten werden (z.B. `StaticVariable.timeServerOk = TimeConfig.timeServerOk;`)

#### Phase 2: Referenzen in anderen Dateien migrieren
**Ziel:** `StaticVariable.timeServerOk` → `TimeConfig.timeServerOk` ersetzen

**Dateien mit den meisten Referenzen:**
- `TurmtechnikActivity.java` - 337 Referenzen
- `Seite1Layout.java` - 19 Referenzen
- `Seite2Layout.java` - 6 Referenzen
- `NebenUhrThread.java` - 38 Referenzen
- `ManuelerStartLayout.java` - 19 Referenzen
- `TimeSyncThread.java` - 8 Referenzen
- Weitere 38 Dateien mit weniger Referenzen

**Gesamt:** ~1135 Referenzen in 44 Dateien

#### Phase 3: StaticVariable.java entfernen
**Ziel:** Nach vollständiger Migration kann `StaticVariable.java` gelöscht werden

### 📊 Kategorisierung der Variablen

#### TimeConfig (25 Variablen)
- Zeitserver: `timeServerOk`, `timeServerEinAus`, `timeServerIp`, etc.
- GPS: `serialGPS_OnOff`, `serialGPS_IP`, `serialGPS_port`, `nmeaGpsNoData`
- Uhren: `uhrA_calendarZeit`, `uhrA_angezeigteZeit`, `uhrA_doRun`, etc.
- Sonnenaufgang/Untergang: `sonnenAufgangString`, etc.

#### SoundConfig (5 Variablen)
- `soundPool2`, `soundGlocke`, `soundIndexList`, `soundIDsList`, `streamIDsList`

#### UIState (20 Variablen)
- Layout: `layoutReady`, `screenIsOff`, `seiteZweiIsRunning`, etc.
- Buttons: `tastenBreite`, `tastenHoehe`, `buttonsCount`, etc.
- Info-Text: `infoTextIndex`, `stringInfoTextField`, `infoStringHeizung`, etc.
- Editor: `pathAndFilenameEditor`, `pathAndFilenameEditorIndex`, etc.

#### MelodyConfig (25 Variablen)
- Melodie-Status: `melodieAktiv`, `indexMelodieStart`, `melodieThreadNew`
- Nächste Melodie: `nameNextMelodie`, `pathAndFileNameNextMelodie`, etc.
- Vorschwingen: `vorschwingenStartzeitenMotorRelais`, etc.
- Benutzerprogramme: `benutzerGeandert`, `userProgramIds`, etc.
- Manueller Start: `manuellerStartEinAus`, `startStundeManuell`, etc.

#### IOConfig (15 Variablen)
- Bluetooth: `bluetoothMode`, `myBluetoothAdapter`, `bluetoothStatus`, `bt_io_ok`
- Carambola: `carambolaStatus`, `carambola_io_ok`
- Serial IO: `serial_io_status4`, `serial_io_ThreadsRun`
- Internet: `internetOnOffFlag`, `internetOnStunden`, etc.

#### HeatingConfig (7 Variablen)
- `heizungOnTimer`, `heizungEingeschaltetTimer`, `nextHeizungFunktionsName`, etc.

#### SchlagwerkConfig (6 Variablen)
- `flagSchlagwerkOnOff`, `beginnSchlagwerk1`, `endeSchlagwerk1`, etc.

#### SystemConfig (15 Variablen)
- System: `serialNumber`, `versionString`, `autostartDerApp`, etc.
- Hammer: `hammerThreadLaeuft`, `hammerDelayTime2`, etc.
- Excel: `excelOpen`, `excelClose`
- Thread-Status: `ddpThreadRun`
- Sonstiges: `scanRelaisMS`, `minutenTakt`, `bigClockTimeout`, etc.

### 🔄 Migrations-Strategie

**Option A: Schrittweise Migration (Empfohlen)**
1. Eine Config-Klasse nach der anderen migrieren
2. Zuerst die am wenigsten verwendeten (z.B. `HeatingConfig`, `SchlagwerkConfig`)
3. Dann die mittleren (z.B. `SoundConfig`, `IOConfig`)
4. Zum Schluss die am meisten verwendeten (z.B. `TimeConfig`, `UIState`, `MelodyConfig`)

**Option B: Direkte Migration**
1. Alle Referenzen auf einmal ersetzen
2. Schneller, aber riskanter
3. Erfordert umfangreiches Testing

### 📝 Notizen

- Die Config-Klassen sind bereits erstellt und funktionsfähig
- `StaticVariable.java` muss wiederhergestellt werden (aktuell hat sie Properties-Syntax, die in Java nicht funktioniert)
- Migration kann schrittweise erfolgen, ohne die Funktionalität zu beeinträchtigen
- Nach vollständiger Migration kann `StaticVariable.java` entfernt werden
