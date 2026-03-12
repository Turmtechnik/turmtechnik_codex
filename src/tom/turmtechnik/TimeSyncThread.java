package tom.turmtechnik;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import com.awr_technology.sntp_client.SntpClient;

/**
 * Thread fuer periodische Zeitsynchronisation ueber Android-Systemeinstellungen.
 * Fragt einen NTP-Server ab und toggelt bei ausreichender Abweichung kurz AUTO_TIME,
 * damit Android die Systemzeit neu synchronisiert.
 *
 * Zusaetzlich wird AUTO_TIME einmal pro Tag kurz AUS/EIN geschaltet, damit Android
 * die Zeit auch ohne erkannte grosse Abweichung neu einliest.
 */
public class TimeSyncThread extends Thread {
    private static final String TAG = "TimeSyncThread";
    private static final long DEFAULT_INTERVAL_MS = 3600000L;
    private static final long DAILY_AUTO_TIME_TOGGLE_INTERVAL_MS = 24L * 60L * 60L * 1000L;
    private static final int NTP_TIMEOUT_MS = 10000;
    private static final long AUTO_TIME_TOGGLE_THRESHOLD_MS = 1000L;
    private static final String PREFS_NAME = "time_sync_prefs";
    private static final String KEY_LAST_DAILY_AUTO_TIME_TOGGLE_MS = "last_daily_auto_time_toggle_ms";

    private static TimeSyncThread instance = null;

    private final Context context;
    private boolean doRun = true;

    public TimeSyncThread(Context context) {
        this.context = context.getApplicationContext();
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
     * Startet einen neuen Thread, falls noch keiner laeuft.
     * Stoppt zuerst den alten Thread, falls vorhanden.
     */
    public static void startInstance(Context context) {
        stopInstance();

        if ("EIN".equals(StaticVariable.timeServerEinAus)) {
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
            Log.w(TAG, "WRITE_SECURE_SETTINGS fehlt - automatische Zeitsynchronisation nicht moeglich");
            return;
        }

        performSync(serverHost);

        while (doRun && "EIN".equals(StaticVariable.timeServerEinAus)) {
            try {
                Thread.sleep(intervalMs);
                if (!doRun || !"EIN".equals(StaticVariable.timeServerEinAus)) {
                    break;
                }
                performSync(serverHost);
            } catch (InterruptedException e) {
                Log.i(TAG, "TimeSyncThread unterbrochen");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Log.e(TAG, "Fehler im TimeSyncThread: " + e.getMessage(), e);
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
            Log.w(TAG, "Ungueltiges Intervall-Format, verwende Standard (1 Stunde): " + e.getMessage());
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
            Log.w(TAG, "WRITE_SECURE_SETTINGS nicht mehr verfuegbar - Synchronisation uebersprungen");
            return;
        }

        long systemTimeMs = System.currentTimeMillis();
        boolean dailyToggleDue = shouldPerformDailyToggle(systemTimeMs);
        if (dailyToggleDue) {
            boolean dailyToggled = TimeSyncHelper.toggleAutoTimeSync(context, false);
            if (dailyToggled) {
                markDailyToggleSuccessful(systemTimeMs);
                StaticVariable.timeServerOk = true;
                Log.i(TAG, "AUTO_TIME erfolgreich per Tages-Toggle getoggelt");
            } else {
                StaticVariable.timeServerOk = false;
                Log.w(TAG, "AUTO_TIME-Tages-Toggle fehlgeschlagen");
            }
        }

        SntpClient sntpClient = new SntpClient();
        if (!sntpClient.requestTime(serverHost, NTP_TIMEOUT_MS)) {
            if (!dailyToggleDue) {
                StaticVariable.timeServerOk = false;
            }
            Log.w(TAG, "NTP-Abfrage fehlgeschlagen: " + serverHost);
            return;
        }

        long ntpTimeMs = sntpClient.getNtpTime()
                + (SystemClock.elapsedRealtime() - sntpClient.getNtpTimeReference());
        systemTimeMs = System.currentTimeMillis();
        long diffMs = Math.abs(ntpTimeMs - systemTimeMs);

        StaticVariable.readSntpTimeMs = ntpTimeMs;
        StaticVariable.timeServerOk = true;

        Log.i(TAG, "NTP-Zeit=" + ntpTimeMs + ", Systemzeit=" + systemTimeMs + ", Abweichung=" + diffMs + " ms");

        if (diffMs < AUTO_TIME_TOGGLE_THRESHOLD_MS) {
            if (dailyToggleDue) {
                Log.i(TAG, "Zeitabweichung unter 1 Sekunde - Tages-Toggle wurde bereits ausgefuehrt");
            } else {
                Log.i(TAG, "Zeitabweichung unter 1 Sekunde - kein AUTO_TIME-Toggle noetig");
            }
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

    private boolean shouldPerformDailyToggle(long nowMs) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastToggleMs = prefs.getLong(KEY_LAST_DAILY_AUTO_TIME_TOGGLE_MS, 0L);
        return lastToggleMs <= 0L || (nowMs - lastToggleMs) >= DAILY_AUTO_TIME_TOGGLE_INTERVAL_MS;
    }

    private void markDailyToggleSuccessful(long nowMs) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_DAILY_AUTO_TIME_TOGGLE_MS, nowMs).apply();
    }
}
