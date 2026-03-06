# Melodie-Excel-Format (z. B. F4 Zusammenläuten 1-7   2 Min)

## Datei

- **Name:** `F4 Zusammenläuten 1-7   2 Min.xls`
- **Typischer Pfad auf dem Gerät:** `/storage/emulated/0/Turmtechnik/Melodien/`
- Die Datei liegt nicht im Projekt; sie wird auf dem Gerät oder aus der DB geladen.

## Spaltenaufbau (wie im Code gelesen)

| Spalte (Index) | Inhalt | Bedeutung |
|----------------|--------|-----------|
| 0 | Zeit (optional) | z. B. "0:5" für Anzeige |
| 1, 2 | Metadaten | z. B. Vorlauf |
| **3** | **Wartezeit** | **Hundertstel Sekunden.** 100 = 1 s, 500 = 5 s. Leer/"0"/"null" = Ende der Melodie. Erste Zeile mit gültiger Wartezeit = erste Datenzeile. |
| **4** | Kloeppel A | **"1" = Glocke ein, "0" = aus.** Ausgang 0 (Vorschwing-Relais 1, Kloeppel 1) |
| 5 | Kloeppel B | Ausgang 1 |
| 6 | Kloeppel C | Ausgang 2 |
| … | … | … |
| 10 | Kloeppel G | Ausgang 6 |
| 11–19 | Kloeppel H–P | Ausgänge 7–15 |
| 20 | Melodie-XLS | Verschachtelte Melodie (Dateiname) |
| 21 | Sound-MP3 | optional |

- **Zeilen:** Index 0/1 oft Header (z. B. "Zeit 100/sec"); erste Zeile mit lesbarer Wartezeit in Spalte 3 = Start der Daten (z. B. Index 1).
- **0 und 1** in Spalte 4–19 sind **absolute Schaltzeitpunkte** für die Melodie-Relais (Kloeppel). Die Vorschwing-Schaltzeitpunkte werden daraus vorausberechnet: **Motor ein = Zeitpunkt dieser Zeile − Vorschwingzeit.**

## Typischer Ablauf „Zusammenläuten 1–7“ (2 Min)

- **Zusammenläuten:** Mehrere Glocken (1–7) nacheinander oder gemeinsam.
- **2 Min:** Gesamtdauer der Melodie.
- Mögliche Varianten:
  - **Variante A:** Eine Zeile mit allen "1" in Spalte 4–10 und Wartezeit 500 (5 s) → alle sieben Glocken 5 s ein, dann nächste Zeile.
  - **Variante B:** Zeile 1: nur Spalte 4 = 1 (Glocke 1), 5 s. Zeile 2: Spalte 4+5 = 1 (1+2), 5 s. … Zeile 7: alle 4–10 = 1, 5 s. → Glocken kommen nacheinander dazu.

## Was der Code mit dieser Melodie macht

1. **Beim Start:** `preScheduleVorschwingMotors(zeile)` berechnet für **jedes "1"** in jeder Zeile den Zeitpunkt **T (ms ab Melodiestart)** und startet für jedes Vorschwing-Relais einen Thread mit Verzögerung **max(0, T − Vorschwingzeit)**.
2. **Pro Zeile in der Schleife:**
   - `schalteKloeppelAusFuerZeile(zeile)` schaltet alle Kanäle mit "0" sofort aus.
   - `schalteKloeppel(zeile, wait)` setzt Kloeppel (0/1) für alle Kanäle; bei "0" wird das Motor-Relais nur ausgeschaltet, wenn `motorAusschaltenOk` true ist (Pause bis zum nächsten "1" ≥ Vorschwingzeit).
   - `waitRealTime(wait)` wartet die Zeilendauer (Wartezeit aus Spalte 3, in ms).

## Zum Prüfen deiner Datei

- Spalte 3: In jeder Datenzeile eine Zahl (Hundertstel Sekunden), z. B. 500 für 5 s.
- Spalten 4–10 (Glocken 1–7): nur "0" oder "1". Leer = keine Aktion.
- Wenn du „alle 1 für 5 s“ willst: eine Zeile, in Spalte 4–10 überall "1", Spalte 3 = 500.
- Wenn Pausen zwischen zwei "1" kürzer als die Vorschwingzeit sind: Das Vorschwing-Relais bleibt an (wird nicht dazwischen ausgeschaltet).

Die Datei selbst liegt nicht im Repo; du kannst sie auf dem Gerät unter `Turmtechnik/Melodien/` oder im Melodien-Editor (Datenbank) prüfen.
