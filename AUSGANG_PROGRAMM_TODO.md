# Programme mit Funktion „Ausgang“ – was noch zu tun ist

Damit man im Programm-Editor Programme mit **Funktion = Ausgang** anlegen kann (z. B. „um 08:00 Ausgang X für 5 Minuten einschalten“), sind folgende Schritte nötig.

---

## 1. Web-UI: Programm-Editor (ConfigWebServer.java)

- **Funktion-Dropdown:** Option `<option value="Ausgang">Ausgang</option>` ergänzen (neben Melodie und Heizung).
- **Ausgang wählen:** Wenn Funktion = „Ausgang“, ein zweites Dropdown anzeigen: „Ausgang (welcher?)“, gefüllt mit den **Beschriftung-Tasten vom Typ „Ausgang“** (Name = c2, z. B. „Licht Turm“). Der gewählte Name wird in **melodie_name** gespeichert (wie bei Heizung die Dauer in **dauer_heizung**).
- **Dauer:** Feld „Dauer Heizung (hh:mm:ss)“ auch bei Ausgang nutzen (Relais X für Dauer Y ein). Beim Speichern: **dauer_heizung** setzen.
- **toggleMelodieDauer:** Erweiterung für „Ausgang“: Melodienname-Dropdown ausblenden, stattdessen „Ausgang (welcher?)“-Dropdown und Dauer anzeigen; beim Wechsel zwischen Melodie/Heizung/Ausgang die richtigen Felder ein-/ausblenden.
- **Laden/Bearbeiten:** Beim Öffnen eines Programms mit `funktion === 'Ausgang'` das Ausgang-Dropdown mit `melodie_name` füllen und Dauer aus `dauer_heizung` anzeigen.

---

## 2. API: Ausgang-Tasten für Dropdown (ConfigWebServer.java)

- **Route:** z. B. `GET /api/ausgang-tasten` hinzufügen.
- **Implementierung:** Entweder bestehende Hilfsmethode nutzen: `handleGetTastenByFunktion("Ausgang")` (wie bei Melodie-Tasten/Schwingen-Tasten). Die Antwort enthält die Liste der Tasten mit Typ „Ausgang“ (inkl. name/c2, relais, platine) – das Frontend füllt damit das Dropdown „Ausgang (welcher?)“.

---

## 3. TagesSuche: getMelodieName für „Ausgang“ (TagesSuche.java)

- **DB-Modus, Programm mit funktion = "Ausgang":**
  - `melodieNameTemp[1]` = `programm.getMelodieName()` (der gewählte Ausgang-Name, z. B. „Licht Turm“) – wird später für `getRelaisNumber(melodieName[1])` genutzt.
  - `melodieNameTemp[0]` = Dauer in Minuten (aus `programm.getDauerHeizung()` im Format hh:mm:ss umrechnen, gleiche Logik wie bei Heizung).
- So ruft der UhrThread weiterhin `setHeizungStartSekunden(melodieName[1], melodieName[0], ...)` auf; für Ausgang ist dann melodieName[1] = Ausgang-Label und melodieName[0] = Dauer – die bestehende Heizung-Logik (ein Relais für eine Dauer ein) passt.

---

## 4. TagesSuche: getInfoText2 für „Ausgang“ (TagesSuche.java)

- Wenn `funktionNameTemp.equals("Ausgang")`: wie bei Heizung behandeln (z. B. `melodieTemp` = Dauer oder Ausgang-Name, `beginnTemp` = Startzeit), damit die Anzeige „nächstes Programm“/Info-Text für Ausgang-Programme sinnvoll ist.

---

## 5. Keine Änderung nötig

- **UhrThread:** Der Block `if (!"Melodie".equals(melodieName[1]) && (heizungGefunden == false))` mit `setHeizungStartSekunden(melodieName[1], melodieName[0], ...)` deckt bereits „Ausgang“ ab, sobald getMelodieName für Ausgang [0]=Dauer und [1]=Ausgang-Label liefert.
- **TagesSuche.getRelaisNumber(String):** Findet das Relais anhand der Beschriftung (c2); Ausgang-Tasten haben c1=„Ausgang“ und c2=Name – der in melodie_name gespeicherte Name reicht.
- **AusgangHeizungThreadNew2 / SaveAndLoadHeizung:** Eine „Heizung“ oder ein „Ausgang“ zur Zeit – gleicher Mechanismus (ein Relais, Start-/Endzeit). Kein zweiter Thread nötig.
- **Datenbank:** Spalte `funktion` kann „Melodie“, „Heizung“, „Ausgang“ enthalten; `melodie_name` speichert bei Ausgang den gewählten Tastennamen; `dauer_heizung` die Dauer – kein Schema-Update nötig.

---

## Reihenfolge der Umsetzung

1. API `/api/ausgang-tasten` (oder Nutzung von handleGetTastenByFunktion("Ausgang")).
2. Programm-Editor: Option Ausgang + Ausgang-Dropdown + Dauer + toggleMelodieDauer anpassen.
3. TagesSuche.getMelodieName: Zweig für funktion „Ausgang“ (Dauer + melodie_name als Label).
4. TagesSuche.getInfoText2: Ausgang wie Heizung anzeigen.

Damit können Programme mit Funktion **Ausgang** eingegeben und zur Laufzeit wie Heizung (Relais für Dauer ein) ausgeführt werden.
