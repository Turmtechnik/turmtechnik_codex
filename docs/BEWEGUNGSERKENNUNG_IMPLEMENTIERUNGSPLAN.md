# Implementierungsplan: Kamera als Bewegungserkennung zum Aufwecken des Bildschirms

## Ziel

Bei erkannten Bewegungen vor der Kamera soll der Bildschirm wieder eingeschaltet bzw. hell geschaltet werden (z. B. wenn jemand zum Tablet geht). Konfigurierbar über die bestehende Anlagen-Config (DB) und optional über die Web-UI.

---

## 1. Manifest (AndroidManifest.xml)

### 1.1 Berechtigungen

- **`android.permission.CAMERA`**  
  Für Zugriff auf die Kamera (Laufzeit-Permission ab API 23, bei Bedarf in Activity/Service anfordern).

- **`android.permission.FOREGROUND_SERVICE_CAMERA`** (API 34 / Android 14)  
  Erforderlich, wenn ein Foreground Service die Kamera nutzt.  
  Mit `android:maxSdkVersion="33"` für ältere APIs weglassen und nur für API 34+ deklarieren, oder einheitlich angeben (auf neueren Geräten obligatorisch).

- **`android.permission.POST_NOTIFICATIONS`** (API 33+)  
  Falls noch nicht vorhanden: für die Foreground-Service-Benachrichtigung (je nach Ziel-API).

### 1.2 Service eintragen

- Neuer **Foreground Service** für die Bewegungserkennung:
  - `android:name=".MotionDetectionService"`
  - `android:exported="false"`
  - Ab API 34: `android:foregroundServiceType="camera"` (mit entsprechender Permission).

Beispiel:

```xml
<uses-permission android:name="android.permission.CAMERA"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" android:minSdkVersion="34"/>

<service
    android:name=".MotionDetectionService"
    android:exported="false"
    android:foregroundServiceType="camera"
    tools:targetApi="34"/>
```

---

## 2. Datenbank / Config-Felder (PlatinenDatabaseHelper, config_value-Tabelle)

Alle Werte wie bisher über **`getConfigValue` / `setConfigValue`** mit String-Keys. Keine Schema-Migration nötig, wenn die Tabelle bereits beliebige Keys erlaubt.

| Config-Key | Typ / Werte | Bedeutung | Standard |
|------------|-------------|-----------|----------|
| `anlage_bewegungserkennung_ein` | `"ein"` / `"aus"` | Bewegungserkennung ein oder aus | `"aus"` |
| `anlage_bewegungserkennung_empfindlichkeit` | Zahl 1–100 (String) | Schwellwert für Bilddifferenz (höher = weniger empfindlich) | `"50"` |
| `anlage_bewegungserkennung_intervall_sekunden` | Zahl 1–10 (String) | Abstand zwischen zwei Kameraprüfungen in Sekunden | `"2"` |
| `anlage_bewegungserkennung_nur_bei_bildschirm_aus` | `"ein"` / `"aus"` | Nur aktiv, wenn Bildschirm aus/gedimmt (Akku sparen) | `"ein"` |

- **Defaults:** Beim ersten Abruf, wenn Key fehlt: `ein=aus`, `empfindlichkeit=50`, `intervall_sekunden=2`, `nur_bei_bildschirm_aus=ein`.
- **Web-UI / Anlagen-Config:** Diese Keys in der bestehenden Config-API lesen/schreiben (siehe Abschnitt 5).

---

## 3. Klassen

### 3.1 MotionDetectionService (neu)

- **Typ:** `Service`, als **Foreground Service** mit permanenter Notification (wie `StartTurmtechnikService`).
- **Aufgabe:**
  - Beim Start: Config aus DB lesen; wenn `anlage_bewegungserkennung_ein` != `"ein"`, Service beenden.
  - Notification mit kurzem Text (z. B. „Bewegungserkennung aktiv“) anzeigen, `startForeground` aufrufen.
  - Kamera öffnen (siehe 3.2), in festem Intervall Frames auswerten.
  - Wenn `anlage_bewegungserkennung_nur_bei_bildschirm_aus` = `"ein"`: nur prüfen, wenn `StaticVariable.screenIsOff == true` (oder PowerManager „Display off“).
  - Bei erkannter Bewegung: Bildschirm aufwecken (WakeLock + ggf. Activity in den Vordergrund holen), siehe 3.3.
- **Lifecycle:**
  - Start: von `TurmtechnikActivity` (nach Config-Check) oder von `BootUpReceiver` (wenn Autostart und Bewegungserkennung ein).
  - Stop: wenn Config auf „aus“ gesetzt wird; ggf. beim Beenden der App optional stoppen (oder weiterlaufen lassen, bis Nutzer in Config „aus“ wählt).
- **Threading:** Kamera und Bildverarbeitung in eigenem Hintergrund-Thread oder Executor; nur WakeLock und Start der Activity auf dem Main-Thread.

### 3.2 Kamera und Bewegungserkennung (neu, z. B. in Service oder eigene Helper-Klasse)

- **Empfehlung:** **CameraX** (Lifecycle-bindung vereinfacht; im Service über `ProcessCameraProvider` und LifecycleService).
- **Wichtig:** Es muss die **Frontkamera** sein (`CameraSelector.LENS_FACING_FRONT`), damit sich jemand vor dem Gerät erkannt wird.
- **Alternative:** Camera2 API (mehr Kontrolle, mehr Code).
- **Ablauf:**
  - Frontkamera mit ImageAnalysis (niedrige Auflösung reicht für Bewegungserkennung).
  - Alle X Sekunden (Config: `anlage_bewegungserkennung_intervall_sekunden`) ein Bild holen (ImageAnalysis UseCase).
  - **Bewegungserkennung:** Vergleich mit vorherigem Frame (z. B. mittlere Helligkeit pro Region oder Pixel-Differenz); wenn Änderung > Schwellwert (aus `anlage_bewegungserkennung_empfindlichkeit` abgeleitet) → „Bewegung“.
  - Keine Speicherung von Bildern; nur Auswertung im RAM.

Optional: eigene kleine Klasse **`MotionDetector`** mit Methode `boolean detectMotion(Image current, Image previous, int sensitivity)` – dann ist der Service schlank und die Logik testbar.

### 3.3 Bildschirm aufwecken (in MotionDetectionService oder Helper)

- **PowerManager:**  
  `PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);`  
  `WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "turmtechnik:motion");`  
  `wl.acquire(5000);` (kurz halten, z. B. 5 Sekunden)  
  `wl.release();` nach kurzer Zeit, damit das System wieder normal weiterläuft.
- **Activity in den Vordergrund:**  
  `Intent intent = new Intent(this, TurmtechnikActivity.class);`  
  `intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);`  
  `startActivity(intent);`  
  So wird die Turmtechnik-Oberfläche sichtbar; bei Bedarf in `TurmtechnikActivity` beim Aufwecken den Bildschirmschoner-Overlay ausblenden und `FLAG_KEEP_SCREEN_ON` setzen (analog zu bestehender Logik).
- **Debounce:** Nach einer ausgelösten Aktion z. B. 30–60 Sekunden keine erneute „Bewegung“ auslösen, um Flackern zu vermeiden.

---

## 4. Integration in bestehende App

### 4.1 TurmtechnikActivity

- **Beim Start (onCreate/onResume):** Config lesen (`anlage_bewegungserkennung_ein`).  
  Wenn `"ein"`: `startService(new Intent(this, MotionDetectionService.class));`  
  Wenn `"aus"`: `stopService(new Intent(this, MotionDetectionService.class));`
- **Optional:** Wenn Bewegungserkennung die Activity in den Vordergrund holt, in `onNewIntent` prüfen (z. B. Extra `EXTRA_WOKE_BY_MOTION`) und dann Bildschirmschoner-Overlay ausblenden und `FLAG_KEEP_SCREEN_ON` setzen.

### 4.2 BootUpReceiver

- Nach dem Start von `StartTurmtechnikService` und ggf. Activity: Config lesen (z. B. über `PlatinenDatabaseHelper.getInstance(context).getConfigValue("anlage_bewegungserkennung_ein")`).  
  Wenn `"ein"`: `context.startService(new Intent(context, MotionDetectionService.class));`  
  So ist die Bewegungserkennung nach Neustart aktiv, wenn sie eingeschaltet war.

### 4.3 WebUiActivity / Bildschirmschoner

- Wenn der Nutzer im Bildschirmschoner (WebView) ist und Bewegung erkannt wird: gleiche Logik wie oben – Activity in den Vordergrund holen, Overlay ausblenden, Bildschirm an.  
  Dafür kann `MotionDetectionService` immer die gleiche Activity starten (`TurmtechnikActivity`); die aktuelle Anzeige (Hauptbildschirm oder WebUi-Schoner) wird durch die bestehende Activity-Struktur bestimmt.

### 4.4 ScreenReceiver / StaticVariable.screenIsOff

- Bereits vorhanden: `StaticVariable.screenIsOff` wird bei `SCREEN_OFF` / `SCREEN_ON` gesetzt.  
  `MotionDetectionService` liest diese Variable, wenn `anlage_bewegungserkennung_nur_bei_bildschirm_aus` = `"ein"`, und führt die Kameraprüfung nur bei `screenIsOff == true` aus.

---

## 5. Web-UI / ConfigWebServer (API)

- **GET** (z. B. in bestehender Anlagen-Config-Abfrage):  
  Die vier Keys `anlage_bewegungserkennung_ein`, `anlage_bewegungserkennung_empfindlichkeit`, `anlage_bewegungserkennung_intervall_sekunden`, `anlage_bewegungserkennung_nur_bei_bildschirm_aus` mit auslesen und im JSON mitliefern.
- **POST** (z. B. Anlagen-Config speichern):  
  Diese Keys entgegennehmen und mit `setConfigValue` speichern.  
  Nach dem Speichern: wenn „ein“ gesetzt wurde, `startService(MotionDetectionService)` aufrufen (von wo die Config gespeichert wird – z. B. Activity oder Service); wenn „aus“, `stopService(MotionDetectionService)`.
- **UI:** In der Web-UI ein kleines Anlagen-Subformular „Bewegungserkennung“ mit Checkbox Ein/Aus, Slider Empfindlichkeit, Intervall (Sekunden), Option „Nur bei ausgeschaltetem Bildschirm“.

---

## 6. Abhängigkeiten (build.gradle)

- **CameraX:**  
  `implementation "androidx.camera:camera-core:1.3.x"`  
  `implementation "androidx.camera:camera-camera2:1.3.x"`  
  `implementation "androidx.camera:camera-lifecycle:1.3.x"`  
  (aktuelle Version aus Android-Dokumentation verwenden.)  
  Dann im Service `ProcessCameraProvider` nutzen und mit `ImageAnalysis` Frames für die Bewegungserkennung bekommen.

---

## 7. Reihenfolge der Implementierung (Vorschlag)

1. **Manifest:** CAMERA, FOREGROUND_SERVICE_CAMERA, Service-Eintrag.
2. **Config-Keys** in DB/PlatinenDatabaseHelper: Defaults beim Lesen setzen; keine Migration nötig.
3. **MotionDetector** (optional): Hilfsklasse für Frame-Vergleich und Schwellwert.
4. **MotionDetectionService:** Foreground-Notification, Kamera starten (CameraX), Config lesen, Intervall-Loop, bei Bewegung WakeLock + Activity starten; Debounce einbauen.
5. **TurmtechnikActivity:** Beim Start/Boot je nach Config Service starten/stoppen; bei Intent mit `EXTRA_WOKE_BY_MOTION` Overlay ausblenden, `KEEP_SCREEN_ON`.
6. **BootUpReceiver:** Bei `anlage_bewegungserkennung_ein` = ein Service starten.
7. **ConfigWebServer:** GET/POST für die vier Keys; nach Speichern Service starten/stoppen (über Broadcast oder direkten Aufruf, je nach Architektur).
8. **Web-UI:** Formular für die vier Einstellungen anbinden.
9. **Laufzeit-Permission:** CAMERA in Activity oder beim ersten Start des MotionDetectionService anfragen; Service erst starten, wenn erteilt.

---

## 8. Kurzübersicht

| Bereich | Inhalt |
|--------|--------|
| **Manifest** | CAMERA, FOREGROUND_SERVICE_CAMERA (API 34), Service `MotionDetectionService` mit `foregroundServiceType="camera"`. |
| **DB/Config** | `anlage_bewegungserkennung_ein`, `anlage_bewegungserkennung_empfindlichkeit`, `anlage_bewegungserkennung_intervall_sekunden`, `anlage_bewegungserkennung_nur_bei_bildschirm_aus`. |
| **Neue Klassen** | `MotionDetectionService` (Foreground Service); optional `MotionDetector` (Bildvergleich). |
| **Bestehende Klassen** | `TurmtechnikActivity` (Service start/stop, Intent bei Aufwecken); `BootUpReceiver` (Service bei Boot starten); `ConfigWebServer` + Web-UI (Config lesen/schreiben, Service start/stop). |
| **Bildschirm aufwecken** | PowerManager WakeLock (SCREEN_BRIGHT_WAKE_LOCK, ACQUIRE_CAUSES_WAKEUP), startActivity TurmtechnikActivity mit FLAG_ACTIVITY_NEW_TASK etc. |

Damit ist der konkrete Implementierungsplan mit Klassen, Manifest und DB-Feldern festgelegt und kann schrittweise umgesetzt werden.
