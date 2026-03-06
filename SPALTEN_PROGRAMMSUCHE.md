# Spalten bei der Programmsuche

Diese Liste zeigt alle Excel-Spalten, die bei der Programmsuche in `TagesSuche.java` ausgewertet werden.

## Spalten-Übersicht

| Excel-Spalte | Index | Konstante | Beschreibung | Datentyp | Format/Beispiele |
|--------------|-------|-----------|--------------|----------|------------------|
| **A** | 0 | `SPALTE_A_STARTZEIT` | Startzeit | **String** | `"HH:MM:SS"` (z.B. `"12:00:00"`) oder `"SA"` (Sonnenaufgang) oder `"SU"` (Sonnenuntergang) |
| **B** | 1 | `SPALTE_B_FUNKTION` | Funktion | **String** | `"Melodie"` oder `"Heizung"` oder andere Funktionsnamen |
| **C** | 2 | `SPALTE_C_MELODIE_NAME` | Melodienname | **String** | Dateiname ohne `.xls` (z.B. `"Glocke1"`) |
| **D** | 3 | `SPALTE_D_DAUER_MINUTEN_HEIZUNG` | Dauer/Minuten Heizung | **String** | `"hh:mm:ss"` (z.B. `"01:30:00"` = 1 Stunde 30 Minuten) oder Minuten als Zahl |
| **E** | 4 | `SPALTE_E_WOCHENTAG_MONTAG` | Montag | **String** | `"x"` = aktiv, leer = nicht aktiv |
| **F** | 5 | - | Dienstag | **String** | `"x"` = aktiv, leer = nicht aktiv |
| **G** | 6 | - | Mittwoch | **String** | `"x"` = aktiv, leer = nicht aktiv |
| **H** | 7 | - | Donnerstag | **String** | `"x"` = aktiv, leer = nicht aktiv |
| **I** | 8 | - | Freitag | **String** | `"x"` = aktiv, leer = nicht aktiv |
| **J** | 9 | - | Samstag | **String** | `"x"` = aktiv, leer = nicht aktiv |
| **K** | 10 | - | Sonntag | **String** | `"x"` = aktiv, leer = nicht aktiv |
| **L** | 11 | `SPALTE_L_IMMER` | Immer | **String** | `"1"` = immer aktiv, `"0"` oder leer = nur wenn verknüpfte Taste aktiv |
| **M** | 12 | `SPALTE_M_PERIODISCH` | Periodisch (Sommer/Winter) | **String** | `"1"` = nur Sommerzeit, `"2"` = nur Winterzeit, leer = immer |
| **N** | 13 | `SPALTE_N_START` | Startdatum | **String** | `"TT.MM"` (z.B. `"01.03"`), `"TT.MM.JJJJ.A"` (z.B. `"23.03.2016.7.A"` = alle 7 Tage ab 23.03.2016), oder `"TT.T"` (z.B. `"8.T"` = ab 8. Tag) |
| **O** | 14 | `SPALTE_O_ENDE` | Enddatum | **String** | `"TT.MM"` (z.B. `"31.10"`), `"TT.MM.JJJJ.A"` (Wiederholung), `"TT.T"` (z.B. `"14.T"` = bis 14. Tag), `"1.W"`–`"4.W"` (1.–4. Woche des Monats: 1–7, 8–14, 15–21, 22–31), `"14.A"`/`"21.A"`/`"28.A"` (alle 2/3/4 Wochen ab Startdatum) |
| **P** | 15 | `SPALTE_P_VERKNUEPFTE_TASTE` | Verknüpfte Taste | **String** | Name der Taste (z.B. `"Schlagwerk"`), leer = keine verknüpfte Taste |
| **Q** | 16 | `SPALTE_Q_PRIORITAET` | Priorität | **String** (wird zu **Integer** geparst) | `"1"` bis `"9"` (höhere Zahl = höhere Priorität), leer = 0 |

## Prüfreihenfolge bei der Programmsuche

1. **Zeitprüfung**: Spalte A (Startzeit) wird mit der aktuellen Zeit verglichen
2. **Wochentagprüfung**: Spalte E-K (Wochentage) - es wird geprüft, ob für den aktuellen Wochentag ein "x" steht
3. **Prioritätsprüfung**: Spalte Q (Priorität) - Programme mit höherer Priorität werden zuerst geprüft
4. **Verknüpfte Taste**: Spalte P - wenn eine Taste angegeben ist, muss diese aktiv sein
5. **Immer-Flag**: Spalte L - wenn "1", ist das Programm immer aktiv (unabhängig von verknüpfter Taste)
6. **Periodisch (Sommer/Winter)**: Spalte M - prüft, ob aktuell Sommer- oder Winterzeit ist
7. **Start/Ende-Datum**: Spalte N und O - prüft, ob das aktuelle Datum im angegebenen Zeitraum liegt

## Datentypen-Details

### String-Spalten (werden als Text gelesen)
- **Spalte A**: Zeitformat `"HH:MM:SS"` oder Sonderwerte `"SA"`/`"SU"`
- **Spalte B**: Funktionsname als Text
- **Spalte C**: Melodienname als Text (ohne Dateiendung)
- **Spalte D**: Zeitformat `"hh:mm:ss"` für Heizungsdauer
- **Spalte E-K**: Wochentage als `"x"` oder leer
- **Spalte L**: `"1"` oder `"0"` oder leer
- **Spalte M**: `"1"` (Sommer), `"2"` (Winter) oder leer
- **Spalte N-O**: Datumsformate (siehe Tabelle)
- **Spalte P**: Tastename als Text

### Numerische Spalten (werden geparst)
- **Spalte Q**: Wird von String zu Integer geparst (`Integer.parseInt()`)

### Besondere Formate

**Spalte A (Startzeit):**
- Normale Zeit: `"12:00:00"` (Stunde:Minute:Sekunde)
- Sonnenaufgang: `"SA"`
- Sonnenuntergang: `"SU"`

**Spalte D (Dauer Heizung):**
- Zeitformat: `"01:30:00"` (Stunden:Minuten:Sekunden)
- Wird intern in Minuten umgerechnet: `(Stunden * 60) + Minuten`

**Spalte N-O (Start/Ende-Datum):**
- Standard: `"01.03"` (Tag.Monat)
- Wiederholung: `"23.03.2016.7.A"` (Startdatum + alle 7 Tage); Ende `"14.A"`/`"21.A"`/`"28.A"` = alle 2/3/4 Wochen ab Startdatum (Start mit JJJJ angeben)
- Tag-Bereich: `"8.T"` bis `"14.T"` (vom 8. bis 14. Tag des Monats)
- Woche des Monats (Ende): `"1.W"` (Tag 1–7), `"2.W"` (8–14), `"3.W"` (15–21), `"4.W"` (22–31)

**Spalte Q (Priorität):**
- Wird als String gelesen, dann zu Integer konvertiert
- Gültige Werte: `"1"` bis `"9"`
- Bei Fehler oder leer: `0` (niedrigste Priorität)

## Wichtige Hinweise

- **Spalte D** kann fehlen. Wenn sie fehlt, müssen die Wochentage bei Spalte D (Index 3) beginnen statt bei Spalte E (Index 4)
- **Wochentage** beginnen immer bei **Spalte E (Index 4)**, wenn Spalte D vorhanden ist
- Die **Priorität** wird von 9 bis 1 durchsucht (höchste zuerst)
- Wenn mehrere Programme zur gleichen Zeit passen, wird das mit der höchsten Priorität ausgewählt
- Alle Spalten werden als **String** aus Excel gelesen und bei Bedarf konvertiert

## Methoden, die diese Spalten verwenden

- `checkProgrammliste()` - Hauptmethode für die Programmsuche
- `wochenTagOk()` - Prüft, ob der Wochentag passt (Spalte E-K)
- `getPrioritaet()` - Liest die Priorität (Spalte Q)
- `verknuepfteTasteOn()` - Prüft verknüpfte Taste (Spalte P)
- `periodischSommerWinterImmer()` - Prüft Sommer/Winter (Spalte M)
- `startEndeDatum()` - Prüft Start/Ende-Datum (Spalte N, O)
- `getMelodieString()` - Liest Melodienname oder Heizungsdauer (Spalte C oder D)
