# Anleitung: WRITE_SECURE_SETTINGS Permission vergeben

## Problem
Die App benötigt die `WRITE_SECURE_SETTINGS` Permission, um die automatische Zeitsynchronisation zu steuern. Diese Permission kann **nicht** über die normalen App-Einstellungen vergeben werden.

## Lösung: Permission über ADB vergeben

### Schritt 1: USB-Debugging aktivieren
1. Auf dem Android-Gerät: **Einstellungen** → **Über das Telefon** → **Build-Nummer** 7x antippen (bis "Sie sind jetzt Entwickler" erscheint)
2. **Einstellungen** → **Entwickleroptionen** → **USB-Debugging** aktivieren

### Schritt 2: Gerät per USB verbinden
- Android-Gerät per USB-Kabel mit dem Computer verbinden
- Auf dem Gerät: "USB-Debugging zulassen" bestätigen

### Schritt 3: ADB-Befehl ausführen
Öffnen Sie ein Terminal/Command Prompt und führen Sie aus:

```bash
adb shell pm grant tom.turmtechnik android.permission.WRITE_SECURE_SETTINGS
```

**Wichtig:** Ersetzen Sie `tom.turmtechnik` durch das tatsächliche Package, falls es anders ist!

### Schritt 4: Permission prüfen
Prüfen Sie, ob die Permission vergeben wurde:

```bash
adb shell dumpsys package tom.turmtechnik | grep WRITE_SECURE_SETTINGS
```

Oder alle Permissions der App anzeigen:

```bash
adb shell dumpsys package tom.turmtechnik | grep permission
```

### Schritt 5: App neu starten
Nach dem Vergeben der Permission:
1. App vollständig beenden (aus dem Hintergrund entfernen)
2. App neu starten
3. Im Logcat sollte jetzt erscheinen: `WRITE_SECURE_SETTINGS Permission verfügbar`

## Alternative: Permission über App-Einstellungen (funktioniert NICHT)
Die `WRITE_SECURE_SETTINGS` Permission kann **nicht** über die normalen App-Einstellungen vergeben werden. Sie ist eine System-Permission, die nur über ADB oder als System-App vergeben werden kann.

## Troubleshooting

### Permission wird nicht erkannt
1. **App neu starten** (vollständig beenden und neu öffnen)
2. **Package-Name prüfen**: Stellen Sie sicher, dass der Package-Name korrekt ist (`tom.turmtechnik`)
3. **ADB-Verbindung prüfen**: `adb devices` sollte das Gerät anzeigen

### Permission geht nach Neustart verloren
- Die Permission bleibt normalerweise erhalten, auch nach Neustart
- Falls sie verloren geht, muss sie erneut vergeben werden

### Alternative: Als System-App installieren
Wenn die Permission dauerhaft benötigt wird (z. B. für AUTO_TIME / WRITE_SETTINGS ohne manuellen ADB-Grant), kann die App als System-App installiert werden. **Voraussetzung:** Root-Zugriff auf das Gerät.

**Vorgehen (typisch):**

1. **APK bauen** (Release oder Debug, je nach Gerät).
2. **Root-Zugriff** auf dem Gerät sicherstellen (z. B. `adb root` oder Custom Recovery).
3. **System-Partition beschreibbar machen** (falls nötig):
   ```bash
   adb root
   adb remount
   ```
4. **APK als System-App ablegen** (eines der Verzeichnisse):
   - `/system/priv-app/` (für privilegierte System-Apps, empfohlen wenn AUTO_TIME/Settings.Global genutzt wird)
   - `/system/app/`
   Beispiel:
   ```bash
   adb push app-release.apk /system/priv-app/Turmtechnik/Turmtechnik.apk
   ```
   Oder per Recovery/File-Manager mit Root: APK nach `/system/priv-app/Turmtechnik/` kopieren, Ordner anlegen falls nötig.
5. **Berechtigungen setzen** (falls nötig):
   ```bash
   adb shell chmod 644 /system/priv-app/Turmtechnik/Turmtechnik.apk
   ```
6. **Gerät neu starten.** Nach dem Neustart wird die App als System-App geführt und hat dauerhaft Zugriff auf System-Permissions wie WRITE_SECURE_SETTINGS / WRITE_SETTINGS (AUTO_TIME).

**Hinweis:** Bei OTA-Updates kann die System-Partition zurückgesetzt werden; dann die System-App-Installation ggf. wiederholen. Die ADB-Methode (`pm grant ...`) bleibt die einfachste Option, wenn kein Root gewünscht ist.

## Verifizierung im Logcat
Nach erfolgreicher Vergabe der Permission sollte im Logcat erscheinen:
```
TimeSyncHelper: WRITE_SECURE_SETTINGS Permission verfügbar
TimeSyncHelper: Zeitsynchronisation getoggelt - sofortige Synchronisation erzwungen
```

## Weitere Informationen
- Die Permission wird benötigt für: `Settings.Global.putInt()` (Zeitsynchronisation)
- Ohne diese Permission kann die App die Settings-App öffnen, aber nicht direkt die Einstellung ändern
- Die Permission ist eine "signature|privileged" Permission, die nur System-Apps standardmäßig haben
