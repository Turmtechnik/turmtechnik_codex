package tom.turmtechnik;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.awr_technology.sntp_client.SntpClient;

/**
 * Thread für periodische Zeitsynchronisation über Android-Systemeinstellungen.
 * Fragt einen NTP-Server ab und toggelt bei ausreichender Abweichung kurz AUTO_TIME,
 * damit Android die Systemzeit neu synchronisiert.
 */
public class TimeSyncThread extends Thread {
    private static final String TAG = "TimeSyncThread";
    private static final long DEFAULT_INTERVAL_MS = 3600000L;
    private static final int NTP_TIMEOUT_MS = 10000;
    private static final long AUTO_TIME_TOGGLE_THRESHOLD_MS = 1000L;

    private static TimeSyncThread instance = null; // Singleton-Instanz

    private final Context context;
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

        long intervalMs = resolveIntervalMs();
        String serverHost = resolveServerHost();

        Log.i(TAG, "Synchronisations-Intervall: " + (intervalMs / 3600000.0) + " Stunden (" + intervalMs + " ms)");
        Log.i(TAG, "Zeitserver: " + serverHost + ", Toggle-Schwelle: " + AUTO_TIME_TOGGLE_THRESHOLD_MS + " ms");

        if (!TimeSyncHelper.hasWriteSecureSettingsPermission(context)) {
            StaticVariable.timeServerOk = false;
            Log.w(TAG, "WRITE_SECURE_SETTINGS fehlt - automatische Zeitsynchronisation nicht möglich");
            return;
        }

        performSync(serverHost);

        while (doRun && StaticVariable.timeServerEinAus.equals("EIN")) {
            try {
                Thread.sleep(intervalMs);
                if (!doRun || !StaticVariable.timeServerEinAus.equals("EIN")) {
                    break;
                }
                performSync(serverHost);
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

    private long resolveIntervalMs() {
        long intervalMs = DEFAULT_INTERVAL_MS;
        try {
            String intervalString = StaticVariable.timeServerAbfrageIntervallMs;
            if (intervalString == null || intervalString.trim().isEmpty()) {
                return intervalMs;
            }
            long parsedInterval = Long.parseLong(intervalString.trim());
            if (parsedInterval > 0) {
                intervalMs = parsedInterval;
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "Ungültiges Intervall-Format, verwende Standard (1 Stunde): " + e.getMessage());
        }
        if (intervalMs < 60000L) {
            Log.w(TAG, "Intervall zu kurz (" + intervalMs + " ms), setze auf Minimum 60000 ms");
            intervalMs = 60000L;
        }
        return intervalMs;
    }

    private String resolveServerHost() {
        String serverHost = StaticVariable.timeServerIp;
        if (serverHost == null || serverHost.trim().isEmpty()) {
            return "pool.ntp.org";
        }
        return serverHost.trim();
    }

    private void performSync(String serverHost) {
        if (!TimeSyncHelper.hasWriteSecureSettingsPermission(context)) {
            StaticVariable.timeServerOk = false;
            Log.w(TAG, "WRITE_SECURE_SETTINGS nicht mehr verfügbar - Synchronisation übersprungen");
            return;
        }

        SntpClient sntpClient = new SntpClient();
        if (!sntpClient.requestTime(serverHost, NTP_TIMEOUT_MS)) {
            StaticVariable.timeServerOk = false;
            Log.w(TAG, "NTP-Abfrage fehlgeschlagen: " + serverHost);
            return;
        }

        long ntpTimeMs = sntpClient.getNtpTime()
                + (SystemClock.elapsedRealtime() - sntpClient.getNtpTimeReference());
        long systemTimeMs = System.currentTimeMillis();
        long diffMs = Math.abs(ntpTimeMs - systemTimeMs);

        StaticVariable.readSntpTimeMs = ntpTimeMs;
        StaticVariable.timeServerOk = true;

        Log.i(TAG, "NTP-Zeit=" + ntpTimeMs + ", Systemzeit=" + systemTimeMs + ", Abweichung=" + diffMs + " ms");

        if (diffMs < AUTO_TIME_TOGGLE_THRESHOLD_MS) {
            Log.i(TAG, "Zeitabweichung unter 1 Sekunde - kein AUTO_TIME-Toggle nötig");
            return;
        }

        boolean toggled = TimeSyncHelper.toggleAutoTimeSync(context, false);
        if (toggled) {
            Log.i(TAG, "AUTO_TIME erfolgreich getoggelt - Android soll die Zeit neu synchronisieren");
        } else {
            StaticVariable.timeServerOk = false;
            Log.w(TAG, "AUTO_TIME-Toggle fehlgeschlagen");
        }
    }
}
