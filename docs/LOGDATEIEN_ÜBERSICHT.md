# Logdateien der Turmtechnik-App

Kurzüberblick, welche Logdateien auf dem Gerät (z. B. unter `/sdcard/Turmtechnik/`) erzeugt werden und was hineingeschrieben wird.

---

## 1. LogTurmtechnik-&lt;dd-MM-yy&gt;.txt (z. B. LogTurmtechnik-03-03-26.txt)

**Steuerung:** Anlagen-Einstellung **„Logfile On/Off“** (DB: `logfile_on_off` = ein/aus). Wenn **aus**, wird diese Datei **nicht** beschrieben.

**Inhalt pro Zeile:**
- Zeit (HH:mm:ss)
- Kurztext der Aktion (z. B. „Manual Relais 3“, „Automatic“, „save Nebenuhr“, „load Nebenuhr“, „onStart NEU GESTARTET!!!!“, „onStop“, „Strom weg und Akku unter soll%“, „Stop gedrueckt“, „Hammer manual“, „verknuepfte Taste manual“)
- Verbindung (O.K. / ERROR)
- Uhr A soll= / Uhr A ist= (Nebenuhr A in Minuten 12h)
- Relais gesendet (4× Binär 0/1)
- Versionsstring

**Wann geschrieben:**
- App-Lifecycle: onStart, onStop
- Nebenuhr: save/load Nebenuhr
- Akku: „Strom weg und Akku unter soll%“
- Manuelle Aktionen: Manual Relais, Hammer manual, verknüpfte Taste, Stop gedrückt
- Automatisch: bei jedem Relais-Senden (Carambola/Serial), wenn logOnOff ein

**Brauchen wir das noch?**  
Ja, wenn ihr den allgemeinen Ablauf (Relais, manuell/automatisch, Verbindung, Nebenuhr-Stand) nachvollziehen wollt. Ohne diese Datei habt ihr nur LogNebenuhrRelais (Nebenuhr detailliert) und LogError (Fehler). Wer nur Fehler und Nebenuhr-Details braucht, kann „Logfile On/Off“ auf **aus** stellen.

---

## 2. LogError.txt (Fehler / Crashes)

**Steuerung:** Anlagen-Einstellung **„Logfile Fehler/Crashes“** (DB: `anlage_log_fehler_crashes` = ein/aus). Wenn **aus**, wird **LogError.txt** nicht beschrieben.

**Inhalt pro Zeile:**
- Datum/Uhrzeit
- Dateiname (z. B. Excel/Config-Datei)
- sheet / spalte / zeile (bei Excel-Fehlern)
- source (Quelldatei, z. B. TurmtechnikActivity, UhrThread, TagesSuche)
- line (Zeilennummer im Code)

**Wann geschrieben:**  
Überall dort, wo `new LogExcelError(...)` aufgerufen wird:

- **Excel/Konfiguration:** Fehler beim Lesen von Programmen, Beschriftung, Schlagwerk, Bluetooth-Config, Feiertagen, Melodien, Vorschwingen, etc. (fehlende Datei, ungültige Zelle, Parse-Fehler)
- **Android-Fehler:** Unbehandelte Java-Exceptions in der App (in TurmtechnikActivity als „Android Error:“ + Fehlertext) – hilft bei der Analyse von Abstürzen, da oft ein Java-Fehler vor einem nativen Crash steht

**Zusammenfassung:**  
LogError.txt = detaillierte **Excel/Config-Fehler** (sheet, Spalte, Zeile, Quelle) + Java-Exceptions. Viele Einträge sind Excel-bezogen.

---

## 3. LogCrash-&lt;dd-MM-yy&gt;.txt (Vorfälle / alles was nicht normal ist)

**Steuerung:** Derselbe Schalter wie für LogError.txt: Anlagen-Einstellung **„Logfile Fehler/Crashes“** (DB: `anlage_log_fehler_crashes` = ein/aus). Button **„Error Log ein/aus“** in der Anlage steuert also **beide**: LogError.txt **und** LogCrash.

**Inhalt pro Zeile:**  
`HH:mm:ss.SSS  <Nachricht>`

**Wann geschrieben:**
- **Neustart** – einmal pro App-Start (Prozessstart)
- **Beenden** – wenn die App gezielt beendet wird (z. B. Back/Exit)
- **Fehler (Exception): …** – unbehandelte Java-Exceptions (Crash-Vorbote)
- **Fehler (Excel/Config): …** – Kurzzeile zu jedem Eintrag in LogError.txt (Datei, sheet, Quelle:Zeile)

**Zusammenfassung:**  
LogCrash = **eine Datei für alles Unnormale**: Neustarts, Beenden, Fehler und Exceptions. Ideal für schnelle Übersicht ohne LogError.txt zu durchsuchen. Ein/Aus wie LogError über „Logfile Fehler/Crashes“.

---

## Weitere Logdateien (ohne „brauchen wir noch?“)

- **LogNebenuhrRelais-&lt;dd-MM-yy&gt;.txt** – Nebenuhr detailliert: Ist/Soll, Relais EIN/AUS, Zeit gestellt, Neustart, Impuls-Fehler, **Keine Verbindung** (wenn Relais-Verbindung fehlt, max. alle 60 s pro Uhr). Bei Timeout der Relais-Verbindung wird nun korrekt **Impuls Fehler: Ja** geloggt (nicht mehr fälschlich „Nein“).
- **LogAbgelaufeneMelodien-&lt;dd-MM-yy&gt;.txt** – Start/Ende von abgespielten Melodien (wenn „Log abgelaufene Melodien“ ein).

Diese werden in der Codebasis und in LOGCAT_ÜBERSICHT.md erwähnt.
