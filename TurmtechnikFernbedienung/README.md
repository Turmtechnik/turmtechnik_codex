# Turmtechnik Fernbedienung

Kleine Android-App zum Fernsteuern der Turmtechnik-Web-UI („App Seite 1“) über Tailscale.

## Nutzung

1. **Erstes Mal:** Adresse eingeben (Tailscale-IP z. B. `100.101.102.103` oder MagicDNS-Name z. B. `turmtechnik-tablet`), auf „Speichern und öffnen“ tippen.
2. **Danach:** Beim Start der App wird automatisch `http://<gespeicherte-Adresse>:8080/app-seite1.html` im WebView geöffnet.
3. **Adresse ändern:** Button „Adresse ändern“ oben rechts tippen, neue Adresse eintragen und speichern.

## Bauen

- Projekt **TurmtechnikFernbedienung** in Android Studio öffnen (Ordner `TurmtechnikFernbedienung`).
- Gradle Sync ausführen, ggf. Gradle Wrapper erstellen lassen.
- Build → Build Bundle(s) / APK(s) → Build APK(s).

Oder aus dem Hauptprojekt-Verzeichnis, falls die Fernbedienung als Modul eingebunden wird:

```bash
cd TurmtechnikFernbedienung
./gradlew assembleDebug
```

Die APK liegt danach unter `app/build/outputs/apk/debug/`.

## Anforderungen

- Android minSdk 21
- Turmtechnik-App auf dem anderen Gerät läuft und ist per Tailscale (oder WLAN) erreichbar
- Port 8080 (Web-Server der Turmtechnik-App)
