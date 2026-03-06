# Nebenuhr-Felder aus System.xls Sheet 15

## Übersicht aller benötigten Felder

### Für jede Nebenuhr (A, B, C)

Jede Nebenuhr hat eine eigene Zeile in Excel:
- **Uhr A**: Zeile 3
- **Uhr B**: Zeile 4  
- **Uhr C**: Zeile 5

#### Pro Nebenuhr (Zeile 3, 4, 5):

| Spalte | Zeile | Feld | Verwendung | Typ |
|--------|-------|------|------------|-----|
| 0 | 3/4/5 | Impuls-Zeit (Sekunden) | Dauer des Impulses | Integer |
| 1 | 3/4/5 | Impuls-Pause (Sekunden) | Pause zwischen Impulsen | Integer |
| ~~2~~ | ~~3/4/5~~ | ~~Uhr-Modus~~ | ~~"12" oder "24" Stunden~~ | ~~String~~ **NICHT MEHR VERWENDET - Immer 12-Stunden-Modus** |
| 3 | 3/4/5 | Relais-Nummer (gerade Minuten) | Relais für gerade Minuten | Integer |
| 4 | 3/4/5 | Relais-Nummer (ungerade Minuten) | Relais für ungerade Minuten | Integer |
| 5 | 3/4/5 | Uhr-Name | Name der Nebenuhr (z.B. "A", "B", "C") | String |

### Dynamische Konfiguration (SetNebenuhrLayout)

| Spalte | Zeile | Feld | Verwendung | Typ |
|--------|-------|------|------------|-----|
| ~~2~~ | ~~nebenUhrCount + 3~~ | ~~Uhr-Modus~~ | ~~"12" oder "24" für jede Nebenuhr~~ | ~~String~~ **NICHT MEHR VERWENDET - Immer 12-Stunden-Modus** |
| 5 | nebenUhrCount + 3 | Uhr-Name | Name der Nebenuhr | String |

**Hinweis**: `nebenUhrCount` beginnt bei 0 und wird für jede gefundene Nebenuhr erhöht (Zeile 3, 4, 5, ...)

## Zusammenfassung pro Nebenuhr

### Nebenuhr A (Zeile 3):
- **Impuls-Zeit**: (0, 3) - Sekunden
- **Impuls-Pause**: (1, 3) - Sekunden
- ~~**Uhr-Modus**: (2, 3) - "12" oder "24"~~ **NICHT MEHR VERWENDET - Immer 12-Stunden-Modus**
- **Relais gerade**: (3, 3) - Relais-Nummer
- **Relais ungerade**: (4, 3) - Relais-Nummer
- **Uhr-Name**: (5, 3) - Name (z.B. "A")

### Nebenuhr B (Zeile 4):
- **Impuls-Zeit**: (0, 4) - Sekunden
- **Impuls-Pause**: (1, 4) - Sekunden
- ~~**Uhr-Modus**: (2, 4) - "12" oder "24"~~ **NICHT MEHR VERWENDET - Immer 12-Stunden-Modus**
- **Relais gerade**: (3, 4) - Relais-Nummer
- **Relais ungerade**: (4, 4) - Relais-Nummer
- **Uhr-Name**: (5, 4) - Name (z.B. "B")

### Nebenuhr C (Zeile 5):
- **Impuls-Zeit**: (0, 5) - Sekunden
- **Impuls-Pause**: (1, 5) - Sekunden
- ~~**Uhr-Modus**: (2, 5) - "12" oder "24"~~ **NICHT MEHR VERWENDET - Immer 12-Stunden-Modus**
- **Relais gerade**: (3, 5) - Relais-Nummer
- **Relais ungerade**: (4, 5) - Relais-Nummer
- **Uhr-Name**: (5, 5) - Name (z.B. "C")

## Verwendung im Code

### NebenUhrA_thread.java:
- Liest (0, 3), (1, 3), (3, 3), (4, 3)
- **NICHT MEHR**: (2, 3) - Uhr-Modus (immer 12-Stunden-Modus)

### NebenUhrB_thread.java:
- Liest (0, 4), (1, 4), (3, 4), (4, 4)
- **NICHT MEHR**: (2, 4) - Uhr-Modus (immer 12-Stunden-Modus)

### NebenUhrC_thread.java:
- Liest (0, 5), (1, 5), (3, 5), (4, 5)
- **NICHT MEHR**: (2, 5) - Uhr-Modus (immer 12-Stunden-Modus)

### NebenUhrThread.java:
- **NICHT MEHR**: (2, 3), (2, 4), (2, 5) - Uhr-Modus (immer 12-Stunden-Modus)
- Liest (0, 3), (1, 3), (0, 4), (1, 4), (0, 5), (1, 5) für Impuls-Dauer

### SetNebenuhrLayout.java:
- **NICHT MEHR**: (2, nebenUhrCount + 3) - Uhr-Modus (immer 12-Stunden-Modus)
- Liest (5, nebenUhrCount + 3) für Uhr-Name

### SetNebenuhrActivity.java:
- Liest (0, 3), (1, 3) für Impuls-Dauer A
- Liest (0, 4), (1, 4) für Impuls-Dauer B
- Liest (0, 5), (1, 5) für Impuls-Dauer C

## Datenstruktur für Web-UI

Für die Web-UI sollten wir folgende Struktur haben:

```javascript
{
  "nebenuhren": [
    {
      "uhrName": "A",
      "zeile": 3,
      "impulsZeit": 1,        // (0, 3)
      "impulsPause": 1,       // (1, 3)
      "uhrModus": "12",       // (2, 3) - "12" oder "24"
      "relaisGerade": 1,      // (3, 3)
      "relaisUngerade": 2     // (4, 3)
    },
    {
      "uhrName": "B",
      "zeile": 4,
      "impulsZeit": 1,        // (0, 4)
      "impulsPause": 1,       // (1, 4)
      // "uhrModus" NICHT MEHR VERWENDET - Immer 12-Stunden-Modus
      "relaisGerade": 3,      // (3, 4)
      "relaisUngerade": 4     // (4, 4)
    },
    {
      "uhrName": "C",
      "zeile": 5,
      "impulsZeit": 1,        // (0, 5)
      "impulsPause": 1,       // (1, 5)
      // "uhrModus" NICHT MEHR VERWENDET - Immer 12-Stunden-Modus
      "relaisGerade": 5,      // (3, 5)
      "relaisUngerade": 6     // (4, 5)
    }
  ]
}
```
