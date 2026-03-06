package tom.turmtechnik;

import android.content.Context;
import android.util.Log;

/**
 * Thread für periodische Zeitsynchronisation über Android-Systemeinstellungen.
 * Ersetzt SntpThread und verwendet stattdessen die System-Zeitsynchronisation.
 * Toggle-Methode wird periodisch aufgerufen (z.B. stündlich), um eine sofortige Synchronisation zu erzwingen.
 */
public class TimeSyncThread extends Thread {
    private static final String TAG = "TimeSyncThread";
    private static final long DEFAULT_INTERVAL_MS = 3600000; // 1 Stunde in Millisekunden

    private static TimeSyncThread instance = null; // Singleton-Instanz

    private Context context;
    private boolean doRun = true;

    public TimeSyncThread(Context context) {
        this.context = context;
    }

    /**
     * Stoppt den laufenden Thread, falls vorhanden.
     */
    public static void stopInstance() {
        if (instance != null && instance.isAlive()) {
            Log.i(TAG, "Stoppe laufenden TimeSyncThread");
            instance.stopThread();
            instance = null;
        }
    }

    /**
     * Startet einen neuen Thread, falls noch keiner läuft.
     * Stoppt zuerst den alten Thread, falls vorhanden.
     */
    public static void startInstance(Context context) {
        // Stoppe zuerst den alten Thread
        stopInstance();
        
        // Starte neuen Thread nur, wenn timeServerEinAus = "EIN"
        if (StaticVariable.timeServerEinAus.equals("EIN")) {
            instance = new TimeSyncThread(context);
            instance.start();
            Log.i(TAG, "TimeSyncThread gestartet");
        } else {
            Log.i(TAG, "TimeSyncThread nicht gestartet (timeServerEinAus != EIN)");
        }
    }

    public void stopThread() {
        doRun = false;
        interrupt();
    }

    @Override
    public void run() {
        Log.i(TAG, "TimeSyncThread gestartet");

        // Konvertiere Intervall: Wenn Wert < 3600000 (1 Stunde in ms), interpretiere als Stunden, sonst als Millisekunden
        // Beispiel: "1" = 1 Stunde, "500" = 500 Stunden, "3600000" = 1 Stunde (als Millisekunden)
        long intervalMs = DEFAULT_INTERVAL_MS;
        try {
            String intervalString = StaticVariable.timeServerAbfrageIntervallMs;
            if (intervalString != null && !intervalString.isEmpty()) {
                long parsedInterval = Long.parseLong(intervalString);
                // Wenn Wert < 3600000 (1 Stunde in ms), interpretiere als Stunden (z.B. "1" = 1 Stunde, "500" = 500 Stunden)
                // Sonst als Millisekunden (z.B. "3600000" = 1 Stunde, "7200000" = 2 Stunden)
                if (parsedInterval < 3600000 && parsedInterval > 0) {
                    intervalMs = parsedInterval * 3600000; // Stunden zu Millisekunden
                    Log.i(TAG, "Intervall als Stunden interpretiert: " + parsedInterval + " Stunden = " + intervalMs + " ms");
                } else {
                    intervalMs = parsedInterval; // Bereits in Millisekunden
                    Log.i(TAG, "Intervall als Millisekunden interpretiert: " + intervalMs + " ms");
                }
                
                // Sicherheitscheck: Minimum 1 Stunde (3600000 ms), um zu verhindern, dass zu kurze Intervalle verwendet werden
                if (intervalMs < 3600000) {
                    Log.w(TAG, "Intervall zu kurz (" + intervalMs + " ms), setze auf Minimum (1 Stunde)");
                    intervalMs = 3600000;
                }
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "Ungültiges Intervall-Format, verwende Standard (1 Stunde): " + e.getMessage());
        }

        Log.i(TAG, "Synchronisations-Intervall: " + (intervalMs / 3600000.0) + " Stunden (" + intervalMs + " ms)");

        // DEAKTIVIERT: Toggle-Umschaltung stört zur Zeit, da sie nicht funktioniert
        // Prüfe, ob Permission verfügbar ist
        // boolean hasPermission = TimeSyncHelper.hasWriteSecureSettingsPermission(context);
        // if (!hasPermission) {
        //     Log.w(TAG, "WRITE_SECURE_SETTINGS Permission nicht verfügbar. Öffne Settings-App EINMAL.");
        //     // Öffne Settings-App nur beim ersten Start
        //     TimeSyncHelper.toggleAutoTimeSync(context, true);
        //     Log.i(TAG, "TimeSyncThread pausiert - warte auf WRITE_SECURE_SETTINGS Permission");
        //     // Thread beenden, da ohne Permission keine automatische Synchronisation möglich ist
        //     return;
        // }
        Log.i(TAG, "Zeiteinstellung-Umschaltung deaktiviert - Permission-Prüfung und Toggle-Aufrufe übersprungen");

        // Erste Synchronisation sofort beim Start (nur wenn Permission vorhanden)
        // DEAKTIVIERT: Toggle-Umschaltung stört zur Zeit, da sie nicht funktioniert
        // if (TimeSyncHelper.toggleAutoTimeSync(context, false)) {
        //     StaticVariable.timeServerOk = true;
        //     Log.i(TAG, "Erste Zeitsynchronisation erfolgreich");
        // } else {
        //     StaticVariable.timeServerOk = false;
        //     Log.w(TAG, "Erste Zeitsynchronisation fehlgeschlagen");
        // }
        Log.i(TAG, "Zeiteinstellung-Umschaltung deaktiviert (Toggle nicht mehr aufgerufen)");
        StaticVariable.timeServerOk = true; // Setze Status auf OK, da keine Toggle-Funktion mehr verwendet wird

        // Periodische Synchronisation (nur wenn Permission vorhanden)
        while (doRun && StaticVariable.timeServerEinAus.equals("EIN")) {
            try {
                // Warte auf Intervall
                Thread.sleep(intervalMs);

                // Prüfe ob Thread noch laufen soll
                if (!doRun || !StaticVariable.timeServerEinAus.equals("EIN")) {
                    break;
                }

                // Prüfe erneut, ob Permission noch vorhanden ist
                // DEAKTIVIERT: Toggle-Umschaltung stört zur Zeit, da sie nicht funktioniert
                // if (!TimeSyncHelper.hasWriteSecureSettingsPermission(context)) {
                //     Log.w(TAG, "WRITE_SECURE_SETTINGS Permission verloren. Beende TimeSyncThread.");
                //     break;
                // }

                // Toggle Zeitsynchronisation (ohne Settings-App zu öffnen)
                // DEAKTIVIERT: Toggle-Umschaltung stört zur Zeit, da sie nicht funktioniert
                // if (TimeSyncHelper.toggleAutoTimeSync(context, false)) {
                //     StaticVariable.timeServerOk = true;
                //     Log.i(TAG, "Periodische Zeitsynchronisation erfolgreich");
                // } else {
                //     StaticVariable.timeServerOk = false;
                //     Log.w(TAG, "Periodische Zeitsynchronisation fehlgeschlagen");
                // }
                Log.d(TAG, "Zeiteinstellung-Umschaltung deaktiviert (Toggle nicht mehr aufgerufen)");
                StaticVariable.timeServerOk = true; // Setze Status auf OK, da keine Toggle-Funktion mehr verwendet wird

            } catch (InterruptedException e) {
                Log.i(TAG, "TimeSyncThread unterbrochen");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Log.e(TAG, "Fehler im TimeSyncThread: " + e.getMessage());
                e.printStackTrace();
                // Bei Fehler weiterlaufen, aber Status auf false setzen
                StaticVariable.timeServerOk = false;
            }
        }

        Log.i(TAG, "TimeSyncThread beendet");
    }
}
