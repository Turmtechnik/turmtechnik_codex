# Vorlage „leer“-Taste und Verarbeitung im Code

## Datenquelle (DB / Excel)

Die Beschriftungstasten kommen aus der Tabelle **beschriftung_tasten** (bzw. aus **Beschriftung-Tasten.xls**, Spalte 2 = c2).

| Spalte | DB-Feld | Bedeutung |
|--------|---------|-----------|
| 0 | c1 | Funktion (z. B. "Normal", "Leer", "Verknüpft") |
| 2 | c2 | **Beschriftung** – entscheidet über Tastentyp |
| 3 | c3 | Relais-Nummer |
| 4 | c4 | Hammerzeit |
| 5 | c5 | Platine |
| 13 | c13 | buttonId |

## Vorlage „leer“ (Platzhalter)

**Alle Felder einheitlich „leer“:** c1 = c2 = c3 = c4 = c5 = `"leer"`, c13 = `""`.

- **c2 = "leer"** (Groß-/Kleinschreibung egal)  
  → **Platzhalter**: Es wird **kein** Button gezeichnet, aber der Slot zählt mit.  
  → `leerTaste(index)` liefert **1**.
- Relais (c3), Hammerzeit (c4) und Platine (c5) werden beim Parsing als 0 / 0 / 1 behandelt, wenn der Wert „leer“ ist.

## Vorlage „NULL“ (leere Zeile)

**Alle Felder einheitlich „NULL“:** c1 = c2 = c3 = c4 = c5 = c13 = `"NULL"`.

- **c2 = "NULL"** oder **"null"**  
  → **Keine Taste / leere Zeile**: Kein Button, Slot zählt mit.  
  → `leerTaste(index)` liefert **2**.

**Default bei leerer Tabelle:** `ensureBeschriftungTastenFilledWithLeer()` legt 24 Zeilen mit Vorlage „leer“ (Zeilen 0–23) und 24 Zeilen mit Vorlage „NULL“ (Zeilen 24–47) an. Vorlage-Objekte: `PlatinenDatabaseHelper.createLeerTemplateRow(zeileIndex)` und `createNullTemplateRow(zeileIndex)`.

## Weitere Werte in c2

- **Leerer String `""`**  
  → Wie NULL; `leerTaste(index)` liefert **2**. Erste leere Zelle = Tabellenende (`break`).

- **Jeder andere Text** (z. B. "Melodie 1", "Schlagwerk")  
  → **Normale Taste**: Button wird erstellt, Text = c2.  
  → `leerTaste(index)` liefert **0**.

- **Erste leere Zelle (c2 = "")** in der Tabelle  
  → **Tabellenende**: Keine weiteren Zeilen werden geladen (`break`).

## Zwei getrennte Datenstrukturen

1. **beschriftungRowsFromDb** (Liste aller DB-Zeilen)  
   - Wird von **leerTaste(index)** genutzt.  
   - `index` = 1-basiert (Excel-Zeile 1, 2, … 24).  
   - `leerTaste(i)` liest `beschriftungRowsFromDb.get(i-1).c2` und gibt 0 / 1 / 2 zurück.

2. **beschriftung, relaisnumber, hammerzeit, buttonId** (Vektoren)  
   - Enthalten **nur Einträge für echte Buttons** (also nur Zeilen, bei denen c2 **nicht** "leer"/"NULL"/"null"/"" ist).  
   - **getButtonText(buttonIndex)** und **getRelaisNumber(buttonIndex)** verwenden den **Button-Index** (0-basiert, 0 = erster gezeichneter Button).  
   - Daher müssen diese vier Listen **gleich lang** sein und **nur** für „normale“ Tasten gefüllt werden.

## Ablauf im Code

### Button-Erstellung (makeXglockenButtons)

- Pro Slot (z. B. 8 Slots pro Reihe) wird **leerTaste(zeilenIndex)** aufgerufen (zeilenIndex 1-basiert).
- **Rückgabe 0**: Button wird erstellt (`initGlockenButton(…, ix)`), `ix` = Button-Index.
- **Rückgabe 1 oder 2**: Kein Button, nur `ix++` (Slot wird „übersprungen“).

→ Die **Anzahl Buttons** = Anzahl Zeilen mit c2 ≠ "leer" und c2 ≠ "NULL"/"null"/"".

### Beschriftung/Relais laden (loadButtonTextFromDatabase)

- Es dürfen **nur** Zeilen mit **normalem** c2 (nicht "leer", nicht "NULL", nicht "null", nicht "") in **beschriftung**, **relaisnumber**, **hammerzeit**, **buttonId** eingetragen werden.
- Bei c2 = "leer" / "NULL" / "null" → Zeile **nicht** in diese Vektoren aufnehmen (Slot wird nur über leerTaste/beschriftungRowsFromDb abgebildet).
- Bei c2 = "" → **break** (Ende der Tabelle).

So bleibt **beschriftung.size() = Anzahl der gezeichneten Buttons**, und **getButtonText(i)** / **getRelaisNumber(i)** passen zum Button-Index **i**.

### Methode leerTaste (Seite1Layout.java)

```text
leerTaste(index)  // index 1-basiert
  → liest beschriftungRowsFromDb.get(index-1).c2
  → "leer" (ignore case)  → return 1
  → "NULL" oder ""        → return 2
  → sonst                 → return 0
```

### Methode getButtonText / getRelaisNumber

- **getButtonText(index)** und **getRelaisNumber(index)** verwenden denselben **Button-Index** (0 = erster gezeichneter Button, 1 = zweiter, …).
- **beschriftung**, **relaisnumber**, **hammerzeit**, **buttonId** haben alle dieselbe Länge = Anzahl gezeichneter Tasten (nur „normale“ Zeilen, keine Einträge für „leer“/„NULL“).
- So stimmen Klick-Index, Relais-Logik und Anzeige wieder überein.
- **Button-Index und relaisNumber-Index sind dasselbe:** `TurmtechnikActivity.initRelais()` befüllt `relaisNumber[0..buttons.size()-1]` immer nach Button-Index (ohne „Packing“), sodass `relaisNumber[i]` stets zu Button `i` gehört.
