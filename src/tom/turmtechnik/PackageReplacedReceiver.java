package tom.turmtechnik;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Reagiert auf APK-Updates der eigenen App und startet den Foreground-Service sowie die Haupt-Activity neu.
 */
public class PackageReplacedReceiver extends BroadcastReceiver {

    private static final String sourceFileName = "PackageReplacedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            return;
        }

        Context appContext = context.getApplicationContext();

        try {
            if (TurmtechnikActivity.getExitForSettingsUntilMillis(appContext) > System.currentTimeMillis()) {
                Log.i(sourceFileName, "Update erkannt, aber Exit für Einstellungen aktiv – kein Auto-Start");
                return;
            }

            long backgroundUntil = TurmtechnikActivity.getBackgroundAllowedUntilMillis(appContext);
            if (backgroundUntil > 0 && System.currentTimeMillis() < backgroundUntil) {
                Log.i(sourceFileName, "Update erkannt, aber Hintergrundphase 5644 aktiv – kein Auto-Start");
                return;
            }

            Intent serviceIntent = new Intent(appContext, StartTurmtechnikService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(serviceIntent);
            } else {
                appContext.startService(serviceIntent);
            }

            StartTurmtechnikService.touchHeartbeat();
            StartTurmtechnikService.setTimeTurmtechnik(90);
            long triggerAt = System.currentTimeMillis() + 1500L;
            StartTurmtechnikService.scheduleDirectActivityLaunch(appContext, triggerAt, "MY_PACKAGE_REPLACED");
            Log.i(sourceFileName, "Update erkannt – Direktstart per Alarm für " + triggerAt + " geplant");
        } catch (Exception e) {
            Log.w(sourceFileName, "Neustart nach Installation fehlgeschlagen: " + (e != null ? e.getMessage() : ""), e);
            try {
                StartTurmtechnikService.launchTurmtechnikActivityFromBackground(appContext);
            } catch (Exception ignored) {
            }
        }
    }
}
