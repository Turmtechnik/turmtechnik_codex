package tom.turmtechnik;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

/**
 * Helper-Klasse für die Steuerung der automatischen Zeitsynchronisation über Android-Systemeinstellungen.
 * 
 * HINWEIS: Settings.Global.AUTO_TIME benötigt WRITE_SECURE_SETTINGS Permission, die nur System-Apps haben können.
 * Normale Apps können diese Einstellung nicht direkt ändern. Stattdessen wird die Settings-App geöffnet,
 * damit der Benutzer die Einstellung manuell ändern kann.
 * 
 * Alternative: Die automatische Zeitsynchronisation wird über die Systemeinstellungen gesteuert.
 * Wenn der Benutzer "Automatisch Datum & Uhrzeit" aktiviert hat, synchronisiert Android automatisch mit dem Google-Server.
 */
public class TimeSyncHelper {
    private static final String TAG = "TimeSyncHelper";

    /**
     * Versucht, die automatische Zeitsynchronisation zu aktivieren.
     * Da WRITE_SECURE_SETTINGS nur für System-Apps verfügbar ist, wird stattdessen die Settings-App geöffnet.
     * @param context Android Context
     * @return true wenn erfolgreich, false bei Fehler
     */
    public static boolean enableAutoTimeSync(Context context) {
        try {
            // Versuche Settings.Global (benötigt WRITE_SECURE_SETTINGS - nur System-Apps)
            boolean result = Settings.Global.putInt(context.getContentResolver(), Settings.Global.AUTO_TIME, 1);
            if (result) {
                Log.i(TAG, "Automatische Zeitsynchronisation aktiviert");
                return true;
            } else {
                Log.w(TAG, "Konnte automatische Zeitsynchronisation nicht aktivieren");
                return false;
            }
        } catch (SecurityException e) {
            // WRITE_SECURE_SETTINGS nicht verfügbar - öffne Settings-App
            Log.w(TAG, "Keine WRITE_SECURE_SETTINGS Permission. Öffne Settings-App für manuelle Aktivierung.");
            openTimeSettings(context);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Aktivieren der Zeitsynchronisation: " + e.getMessage());
            return false;
        }
    }

    /**
     * Versucht, die automatische Zeitsynchronisation zu deaktivieren.
     * Da WRITE_SECURE_SETTINGS nur für System-Apps verfügbar ist, wird stattdessen die Settings-App geöffnet.
     * @param context Android Context
     * @return true wenn erfolgreich, false bei Fehler
     */
    public static boolean disableAutoTimeSync(Context context) {
        try {
            // Versuche Settings.Global (benötigt WRITE_SECURE_SETTINGS - nur System-Apps)
            boolean result = Settings.Global.putInt(context.getContentResolver(), Settings.Global.AUTO_TIME, 0);
            if (result) {
                Log.i(TAG, "Automatische Zeitsynchronisation deaktiviert");
                return true;
            } else {
                Log.w(TAG, "Konnte automatische Zeitsynchronisation nicht deaktivieren");
                return false;
            }
        } catch (SecurityException e) {
            // WRITE_SECURE_SETTINGS nicht verfügbar - öffne Settings-App
            Log.w(TAG, "Keine WRITE_SECURE_SETTINGS Permission. Öffne Settings-App für manuelle Deaktivierung.");
            openTimeSettings(context);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Deaktivieren der Zeitsynchronisation: " + e.getMessage());
            return false;
        }
    }

    /**
     * Öffnet die Android Settings-App für Datum & Uhrzeit.
     * @param context Android Context
     */
    public static void openTimeSettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_DATE_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.i(TAG, "Settings-App für Datum & Uhrzeit geöffnet");
        } catch (Exception e) {
            Log.e(TAG, "Konnte Settings-App nicht öffnen: " + e.getMessage());
        }
    }

    /**
     * Prüft, ob die automatische Zeitsynchronisation aktiviert ist.
     * @param context Android Context
     * @return true wenn aktiviert, false wenn deaktiviert oder Fehler
     */
    public static boolean isAutoTimeSyncEnabled(Context context) {
        try {
            int autoTime = Settings.Global.getInt(context.getContentResolver(), Settings.Global.AUTO_TIME, 0);
            return autoTime == 1;
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Prüfen der Zeitsynchronisation: " + e.getMessage());
            return false;
        }
    }

    /**
     * Prüft, ob WRITE_SECURE_SETTINGS Permission verfügbar ist, OHNE die Settings-App zu öffnen.
     * @param context Android Context
     * @return true wenn Permission verfügbar, false wenn nicht
     */
    public static boolean hasWriteSecureSettingsPermission(Context context) {
        try {
            // Versuche eine Test-Leseoperation (benötigt keine Permission)
            int currentValue = Settings.Global.getInt(context.getContentResolver(), Settings.Global.AUTO_TIME, 0);
            
            // Versuche eine Test-Schreiboperation (benötigt WRITE_SECURE_SETTINGS)
            // Setze den Wert auf den aktuellen Wert zurück (keine Änderung, aber testet die Permission)
            boolean result = Settings.Global.putInt(context.getContentResolver(), Settings.Global.AUTO_TIME, currentValue);
            
            if (result) {
                Log.d(TAG, "WRITE_SECURE_SETTINGS Permission verfügbar");
            } else {
                Log.w(TAG, "WRITE_SECURE_SETTINGS Permission nicht verfügbar (putInt returned false)");
            }
            
            return result;
        } catch (SecurityException e) {
            Log.w(TAG, "WRITE_SECURE_SETTINGS Permission nicht verfügbar (SecurityException): " + e.getMessage());
            Log.i(TAG, "HINWEIS: Permission über ADB vergeben mit: adb shell pm grant tom.turmtechnik android.permission.WRITE_SECURE_SETTINGS");
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Prüfen der WRITE_SECURE_SETTINGS Permission: " + e.getMessage());
            return false;
        }
    }

    // Flag, um zu verhindern, dass die Settings-App mehrfach geöffnet wird
    private static boolean settingsAppOpened = false;

    /**
     * Toggle-Methode: Deaktiviert kurz die automatische Zeitsynchronisation und aktiviert sie wieder,
     * um eine sofortige Synchronisation mit dem Google-Server zu erzwingen.
     * 
     * Wenn WRITE_SECURE_SETTINGS Permission vorhanden ist, wird die Einstellung direkt geändert.
     * Wenn die Permission fehlt, wird die Settings-App NUR EINMAL geöffnet (beim ersten Versuch).
     * 
     * @param context Android Context
     * @param openSettingsIfNeeded Wenn true, wird die Settings-App geöffnet, wenn Permission fehlt. Wenn false, wird nur versucht, die Einstellung zu ändern.
     * @return true wenn erfolgreich, false wenn Permission fehlt oder Fehler
     */
    public static boolean toggleAutoTimeSync(Context context, boolean openSettingsIfNeeded) {
        try {
            // Versuche direkt zu togglen (wenn Permission vorhanden ist, funktioniert das)
            try {
                // Deaktivieren
                boolean disableResult = Settings.Global.putInt(context.getContentResolver(), Settings.Global.AUTO_TIME, 0);
                
                // Kurz warten (100ms), damit die Änderung wirksam wird
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Wieder aktivieren
                boolean enableResult = Settings.Global.putInt(context.getContentResolver(), Settings.Global.AUTO_TIME, 1);
                
                if (disableResult && enableResult) {
                    Log.i(TAG, "Zeitsynchronisation getoggelt - sofortige Synchronisation erzwungen");
                    settingsAppOpened = false; // Reset Flag, falls Permission später verfügbar wird
                    return true;
                } else {
                    Log.w(TAG, "Toggle teilweise fehlgeschlagen (disable=" + disableResult + ", enable=" + enableResult + ")");
                    return false;
                }
            } catch (SecurityException e) {
                // WRITE_SECURE_SETTINGS Permission fehlt
                Log.w(TAG, "SecurityException beim Toggle: " + e.getMessage());
                Log.i(TAG, "HINWEIS: WRITE_SECURE_SETTINGS Permission über ADB vergeben mit:");
                Log.i(TAG, "  adb shell pm grant tom.turmtechnik android.permission.WRITE_SECURE_SETTINGS");
                Log.i(TAG, "Oder die App muss als System-App installiert sein (im /system/app Verzeichnis)");
                
                if (openSettingsIfNeeded && !settingsAppOpened) {
                    // Öffne Settings-App NUR EINMAL
                    Log.i(TAG, "Keine WRITE_SECURE_SETTINGS Permission. Öffne Settings-App für manuelle Synchronisation.");
                    openTimeSettings(context);
                    settingsAppOpened = true;
                } else if (!openSettingsIfNeeded) {
                    Log.d(TAG, "Keine WRITE_SECURE_SETTINGS Permission. Synchronisation nicht möglich.");
                }
                return false; // Permission fehlt - keine Synchronisation möglich
            }
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Toggle der Zeitsynchronisation: " + e.getMessage());
            return false;
        }
    }

    /**
     * Toggle-Methode mit Standard-Verhalten (öffnet Settings-App, wenn Permission fehlt).
     * @param context Android Context
     * @return true wenn erfolgreich, false wenn Permission fehlt oder Fehler
     */
    public static boolean toggleAutoTimeSync(Context context) {
        return toggleAutoTimeSync(context, true);
    }
}
