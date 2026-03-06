package tom.turmtechnik;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

/**
 * Reagiert auf Stromanschluss (ACTION_POWER_CONNECTED): Startet die App, wenn das Tab
 * im Standby/Bildschirm aus war – inkl. Aufwecken per WakeLock, damit die Activity zuverlässig erscheint.
 * Wenn das Gerät komplett aus war (ausgeschaltet), bootet es nur bei „Einschalten bei Ladeanschluss“ (herstellerabhängig).
 */
public class BatteryReceiver extends BroadcastReceiver {

    private static final String sourceFileName = "BatteryReciver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
            return;
        }
        Context appContext = context.getApplicationContext();
        try {
            Boolean autostartFromDb = null;
            try {
                autostartFromDb = PlatinenDatabaseHelper.getInstance(appContext).getAutostartDerApp();
            } catch (Exception e) {
                Log.w(sourceFileName, "Autostart aus DB fehlgeschlagen (z. B. nach Stromausfall), starte App", e);
                StaticVariable.autostartDerApp = true;
            }
            if (autostartFromDb != null) {
                StaticVariable.autostartDerApp = autostartFromDb;
            } else {
                StaticVariable.autostartDerApp = true;
            }

            if (!StaticVariable.autostartDerApp) {
                Log.d(sourceFileName, "Strom angesteckt, aber Autostart in Config aus – starte App nicht");
                return;
            }
            if (TurmtechnikActivity.getExitForSettingsUntilMillis(appContext) > System.currentTimeMillis()) {
                Log.d(sourceFileName, "Strom angesteckt, aber Exit für Einstellungen aktiv – starte App nicht");
                return;
            }
            long backgroundUntil = TurmtechnikActivity.getBackgroundAllowedUntilMillis(appContext);
            if (backgroundUntil > 0 && System.currentTimeMillis() < backgroundUntil) {
                Log.d(sourceFileName, "Strom angesteckt, aber noch 15-Min-Hintergrund (5644) – starte App nicht");
                return;
            }

            appContext.startService(new Intent(appContext, StartTurmtechnikService.class));
            StartTurmtechnikService.touchHeartbeat();
            StartTurmtechnikService.setTimeTurmtechnik(90);

            PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isInteractive()) {
                try {
                    PowerManager.WakeLock wl = pm.newWakeLock(
                            PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                            "BatteryReceiver:PowerConnected");
                    wl.acquire(5000); // Auto-Release nach 5 s – Activity hat dann FLAG_KEEP_SCREEN_ON
                } catch (Exception e) {
                    Log.w(sourceFileName, "WakeLock bei Stromanschluss: " + (e != null ? e.getMessage() : ""));
                }
            }

            Intent startIntent = new Intent(appContext, TurmtechnikActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            appContext.startActivity(startIntent);
            Log.i(sourceFileName, "Strom angesteckt – App gestartet (PM)");
        } catch (Exception e) {
            Log.e(sourceFileName, "BatteryReceiver nach Stromanschluss fehlgeschlagen – starte App trotzdem", e);
            appContext.startService(new Intent(appContext, StartTurmtechnikService.class));
            StaticVariable.autostartDerApp = true;
            StartTurmtechnikService.touchHeartbeat();
            StartTurmtechnikService.setTimeTurmtechnik(90);
            Intent startIntent = new Intent(appContext, TurmtechnikActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            appContext.startActivity(startIntent);
        }
    }
}
