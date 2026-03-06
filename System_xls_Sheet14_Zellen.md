# System.xls Sheet 14 - Gelesene Zellen

## Übersicht aller abgefragten Zellen

### TurmtechnikActivity.java

| Spalte | Zeile | Wert | Verwendung | Status |
|--------|-------|------|------------|--------|
| 0 | 1 | Bluetooth Modus | `StaticVariable.bluetoothMode` | ✅ Bereits in DB (als "modus") |
| 1-4 | 1 | IP-Adressen | `StaticVariable.ipList` (Platinen 1-4) | ✅ Bereits in DB (platinen_config) |
| 5-8 | 1 | Ports | `StaticVariable.portList` (Platinen 1-4) | ✅ Bereits in DB (platinen_config) |
| 1 | 2 | Scan-Relais (ms) | `StaticVariable.scanRelaisMS` | ❌ Noch nicht in DB |
| 1 | 3 | Power off Akku % | `StaticVariable.batt_shut_down_level2` | ✅ Bereits in DB |
| 1 | 4 | Logfile On/Off | `StaticVariable.logOnOff` | ❌ Noch nicht in DB |
| 1 | 5 | Reboot OK Flag | `StaticVariable.flagRebootOk` | ❌ Noch nicht in DB |
| 1 | 6 | Autostart der App | `StaticVariable.autostartDerApp` | ❌ Noch nicht in DB |
| 1 | 7 | Internet On Zeit | `internetOnString` | ❌ Noch nicht in DB |
| 2 | 7 | Internet Off Zeit | `internetOffString` | ❌ Noch nicht in DB |
| 2 | 9 | Password Exit Normal | `passwordExitNormal` | ❌ Noch nicht in DB |
| 2 | 10 | Password Exit + Delete | `passwordExitAndDeleteBeschriftungTasten` | ❌ Noch nicht in DB |

### AlarmReceiver.java, BatteryReceiver.java, BootUpReceiver.java, StartTurmtechnikService.java

| Spalte | Zeile | Wert | Verwendung | Status |
|--------|-------|------|------------|--------|
| 1 | 6 | Autostart der App | `StaticVariable.autostartDerApp` | ❌ Noch nicht in DB |

## Zusammenfassung

### ✅ Bereits in Datenbank:
- **Modus** (0, 1) → `io_config` Key: "modus"
- **Platinen IPs/Ports** (1-4, 1) und (5-8, 1) → `platinen_config` Tabelle
- **Power off Akku %** (1, 3) → `io_config` Key: "power_off_akku_prozent"

### ❌ Noch NICHT in Datenbank:
- **Scan-Relais MS** (1, 2) → `StaticVariable.scanRelaisMS`
- **Logfile On/Off** (1, 4) → `StaticVariable.logOnOff`
- **Reboot OK Flag** (1, 5) → `StaticVariable.flagRebootOk`
- **Autostart der App** (1, 6) → `StaticVariable.autostartDerApp` (wird in mehreren Receivern gelesen)
- **Internet On Zeit** (1, 7) → `internetOnString`
- **Internet Off Zeit** (2, 7) → `internetOffString`
- **Password Exit Normal** (2, 9) → `passwordExitNormal`
- **Password Exit + Delete** (2, 10) → `passwordExitAndDeleteBeschriftungTasten`

## Nächste Schritte für Migration

1. **Priorität 1** (aktuell):
   - `scanRelaisMS` (1, 2) - wird aktiv verwendet

2. **Priorität 2** (wichtig):
   - `autostartDerApp` (1, 6) - wird in mehreren Receivern gelesen
   - `flagRebootOk` (1, 5) - System-Flag

3. **Priorität 3** (später):
   - `logOnOff` (1, 4)
   - `internetOnString` / `internetOffString` (1, 7 / 2, 7)
   - `passwordExitNormal` / `passwordExitAndDeleteBeschriftungTasten` (2, 9 / 2, 10)
