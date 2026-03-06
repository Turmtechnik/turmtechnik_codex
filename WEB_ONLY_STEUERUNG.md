# Nur über den Web-Server steuern – Was muss umgebaut werden?

Ziel: Die Anlage soll **ausschließlich über den Web-Server** (Browser) steuerbar sein – ohne die native Android-Oberfläche (Tasten, Activities) vor Ort zu brauchen.

---

## 1. Was der Web-Server heute schon kann (Konfiguration)

- **Programme:** Programm-Editor, Osterfeiertage, Fixe Feiertage, Benutzerprogramme (`/api/programme`, Feiertage, Tagtypen)
- **Hardware:** Platinen (IP, Port, Modus WLAN/Bluetooth), Beschriftung Tasten, Soforttasten **konfigurieren**
- **Uhren & Zeiten:** Nebenuhr Config/Layout, Vorschwingen, Schlagwerk
- **Läuten & Melodien:** MIDI-Config, Melodien-Editor, Noten-zu-Relais
- **Daten:** Anlagendaten (Baustelle, Autostart, Log, Reboot), Fernsteuern, Anlagenstandort, Daten sichern/Import, PDF-Export
- **Platinen-Test:** Ping/Verbindungstest (`/api/platinen/ping`, Test-Platine-Seite)
- **MIDI-Test:** Einzelnen Ton testen (`POST /api/midi-noten-relais/test`)

Das reicht, um die **gesamte Konfiguration** im Browser zu pflegen.

---

## 2. Was heute nur in der App (native) passiert – Lücken für „nur Web“

### 2.1 Laufzeit-Steuerung („Jetzt ausführen“)

| Aktion | Wo heute | Was für Web fehlt |
|--------|----------|-------------------|
| **Soforttaste auslösen** | Nutzer tippt auf Taste → `StartMelodieSofort` startet Melodie | API z. B. `POST /api/play/sofort/:index` – ruft dieselbe Logik wie Tastendruck auf (Melodie sofort starten) |
| **Einzelnes Relais an/aus** | Tasten (Glocken, Hammer, …) schalten Relais | API z. B. `POST /api/relais/trigger` mit Relais-Nr. + Dauer oder Ein/Aus – sendet Befehl an Platine(n) |
| **Alle Glocken aus** / **Stop** | Systemtasten in der App | API z. B. `POST /api/control/stop` oder `POST /api/control/all-off` |
| **Automatik ein/aus** | Schalter in der App | Optional: `GET/PUT /api/control/automatik` (wenn gewünscht) |

Ohne diese APIs kann man von außen **keine** Läute-/Relais-Aktionen auslösen, nur konfigurieren.

### 2.2 Status & Anzeige („Was passiert gerade?“)

| Info | Wo heute | Was für Web fehlt |
|------|----------|-------------------|
| **Aktueller Tagtyp** | Wird in der App angezeigt | `GET /api/status` mit z. B. `tagtyp`, `datum`, `uhrzeit` |
| **Verbindungsstatus** | `StaticVariable.bt_io_ok`, Carambola/WLAN | Im selben `GET /api/status`: `platinenOnline`, `modus` (bluetooth/wifi) |
| **Batterie** | App zeigt Akku | `GET /api/status`: `batterieProzent`, `netzteilAngeschlossen` |
| **Läuft gerade** | Welche Melodie / welches Programm | Optional: `aktuellesProgramm`, `melodieLaeuft` |
| **Nächste Termine** | TagesSuche / Programmzeiten | Optional: `naechsteProgramme[]` für Dashboard |

Damit kann die Web-UI eine **Dashboard-Seite** anzeigen (wie ein kleines Kontrollpanel), ohne das Gerät zu sehen.

### 2.3 Modus-Umschaltung (Bluetooth ↔ WLAN)

- **Bereits da:** `GET/PUT /api/platinen/config` mit `modus: "bluetooth"` oder `"wifi"`.
- Die App setzt danach `StaticVariable.bluetoothMode` und die Platinen-Kommunikation wird umgestellt.  
→ **Kein zusätzlicher Umbau nötig**, nur in der Web-UI sichtbar machen (z. B. auf Platinen-Seite oder Anlagendaten).

### 2.4 App-Start / Gerät ohne Bildschirm

- **Autostart** ist konfigurierbar (Anlagendaten, „Autostart der App“).
- Wenn die Anlage **ohne dauerhafte Bildschirm-Anzeige** laufen soll:
  - **Option A:** Tablet zeigt nur eine **Fullscreen-WebView** (z. B. `WebUiActivity`) mit Startseite oder Dashboard – dann ist die „Steuerung“ faktisch nur Web.
  - **Option B:** App als **Service** im Hintergrund, **ohne** Haupt-Activity („Headless“): Nur `StartTurmtechnikService` + ConfigWebServer; dann muss der Server auch ohne TurmtechnikActivity starten (z. B. aus Service heraus).

### 2.5 Verbleibende Excel-Abhängigkeiten

- Einige Werte werden noch aus **Excel** gelesen (z. B. System.xls, Sofortstart Programme.xls), wenn DB-Einträge fehlen.
- Für „nur Web“ sinnvoll: **Alle** relevanten Konfigurationen aus der **Datenbank** lesen (und nur noch optional Excel als Import-Quelle). Dann reicht es, alles über die Web-API zu schreiben; kein Excel auf dem Gerät nötig.

---

## 3. Konkrete Umbau-Schritte (Priorität)

### Phase 1: Laufzeit-Steuerung per API (wichtigste Lücke)

1. **`POST /api/play/sofort/:index`**  
   - Parameter: optional `startzeit` (HH:mm) oder „jetzt“.  
   - In der App: dieselbe Logik wie beim Drücken der Soforttaste (z. B. `StartMelodieSofort` oder zentrale „Play-Sofort“-Methode) aufrufen.  
   - Rückgabe: `{ "success": true }` oder Fehler.

2. **`POST /api/relais/trigger`** (oder `/api/control/relais`)  
   - Body: z. B. `{ "relaisBerechnet": 123, "aktion": "ein" | "aus" | "impuls", "impulsMs": 200 }`.  
   - Server ruft die bestehende Relais-Sendelogik auf (wie bei Tastendruck).  
   - Ggf. Sicherheit: nur bestimmte Relais oder nur aus lokalem Netz erlauben.

3. **`POST /api/control/stop`** (oder „alle Glocken aus“)  
   - Ruft die gleiche „Stop“-Logik wie die Systemtaste in der App auf.

4. **Web-Seite „Steuerung“ oder „Dashboard“**  
   - Soforttasten als Buttons („Melodie X jetzt starten“).  
   - Optional: Einzelrelais für Tests.  
   - Button „Alle aus / Stop“.

### Phase 2: Status-API für Dashboard

5. **`GET /api/status`**  
   - Liefert z. B.:  
     - `uhrzeit`, `datum`, `tagtyp`, `wochentag`  
     - `modus` (bluetooth/wifi)  
     - `platinenOnline` (oder pro Platine)  
     - `batterieProzent`, `netzteilAngeschlossen`  
     - optional: `melodieLaeuft`, `naechsteProgramme[]`  
   - Keine neue Logik nötig, nur Werte aus `StaticVariable` / TagesSuche / Batterie-Receiver auslesen und als JSON zurückgeben.

6. **Dashboard-Seite im Web**  
   - Ruft `/api/status` per Polling oder einmal beim Laden ab.  
   - Zeigt Uhr, Tagtyp, Verbindung, Akku, nächste Termine.  
   - Links zu Konfiguration und zur neuen „Steuerung“-Seite.

### Phase 3: Optional – Nur Web-UI am Gerät

7. **Startseite des Tablets = Web-UI**  
   - Beim App-Start direkt `WebUiActivity` mit Startseite oder Dashboard öffnen (oder Launcher-Activity zeigt nur WebView auf `http://localhost:8080/`).  
   - Native Tasten-Screens werden nur noch bei Bedarf (z. B. Tiefenlink) geöffnet oder ganz weggelassen.

8. **Web-Server ohne TurmtechnikActivity**  
   - Wenn „Headless“ gewünscht: ConfigWebServer beim Start des **Services** starten (z. B. in `StartTurmtechnikService`), nicht nur in der Activity.  
   - Sicherstellen, dass alle vom Server genutzten Komponenten (DB, StaticVariable, Relais-Logik, TagesSuche) auch ohne Activity verfügbar sind (Context aus Application/Service).

### Phase 4: Konfiguration nur noch aus DB

9. **Excel-Fallback entfernen oder minimieren**  
   - Alle Werte, die die Web-UI setzt, nur noch aus der DB lesen.  
   - Excel nur noch für **Import** (Migration), nicht für Laufzeit-Lesen.  
   - Dokumentation anpassen: „Konfiguration ausschließlich über Web-UI (und API).“

---

## 4. Kurz-Checkliste „Nur Web steuern“

| Bereich | Erledigt? | Hinweis |
|---------|-----------|--------|
| Konfiguration (Programme, Platinen, Melodien, …) | ✅ | Bereits über Web-API + Seiten |
| Modus Bluetooth/WLAN | ✅ | `/api/platinen/config` |
| Soforttaste auslösen | ❌ | `POST /api/play/sofort/:index` + Steuerungs-Seite |
| Relais direkt an/aus | ❌ | `POST /api/relais/trigger` (+ ggf. Stop) |
| Status (Uhr, Tagtyp, Verbindung, Akku) | ❌ | `GET /api/status` + Dashboard |
| Tablet zeigt nur Web-UI | Optional | WebView/WebUiActivity als Start |
| Server ohne Activity (Headless) | Optional | Server im Service starten |
| Konfiguration nur aus DB | Teilweise | Excel-Fallback schrittweise abbauen |

---

## 5. Technische Anmerkungen

- **Context:** ConfigWebServer bekommt bereits `Context` (Application). Für Relais/Play muss der Server Zugriff auf die **gleiche Logik** haben wie die Activity (z. B. zentrale Klasse oder statische Methoden in `TurmtechnikActivity` / Service). Keine Duplikation der Hardware-Befehle, nur Aufruf aus dem API-Handler heraus.
- **Threads:** Relais- und Play-Aufrufe ggf. auf den gleichen Thread/Handler wie die UI-Logik legen, falls die Hardware-Kommunikation nicht thread-sicher ist.
- **Sicherheit:** Web-Server nur im lokalen Netz oder mit Zugangskontrolle betreiben; für „Relais/Play“ optional Token oder Passwort, um versehentliche oder fremde Zugriffe zu vermeiden.

Wenn du mit einer der Phasen starten willst, reicht es, zuerst **Phase 1 (Play-Sofort + Relais + Stop)** und **Phase 2 (Status + Dashboard)** umzusetzen – danach ist die Anlage bereits vollständig über den Browser steuer- und beobachtbar.
