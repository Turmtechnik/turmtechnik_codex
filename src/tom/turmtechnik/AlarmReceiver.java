package tom.turmtechnik;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver
//public class BatteryReceiver extends BroadcastReceiver
{
    String bluetoothConfigFileString;

    private final static String sourceFileName = "AlarmReciver";

    private int batteryLevel;
    private Context workContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Service starten (aus Hintergrund nur mit startForegroundService erlaubt – Service ruft startForeground() in onCreate)
        try {
            Intent svc = new Intent(context, StartTurmtechnikService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(svc);
            } else {
                context.startService(svc);
            }
        } catch (Exception e) {
            android.util.Log.w(sourceFileName, "Service starten: " + (e != null ? e.getMessage() : ""));
        }

        Boolean autostartFromDb = null;
        try {
            autostartFromDb = PlatinenDatabaseHelper.getInstance(context).getAutostartDerApp();
        } catch (Exception e) {
            Log.w(sourceFileName, "Autostart aus DB fehlgeschlagen, Fallback Excel", e);
        }
        if (autostartFromDb != null) {
            StaticVariable.autostartDerApp = autostartFromDb;
        } else {
            // Nicht in DB: Standard = ein. Kein Excel mehr – nur Datenbank.
            StaticVariable.autostartDerApp = true;
        }

        Log.e("autostart", "StaticVariable.autostartDerApp=" + StaticVariable.autostartDerApp);

        // „App beenden für Einstellungen“: In dieser Zeit nicht starten
        if (TurmtechnikActivity.getExitForSettingsUntilMillis(context) > System.currentTimeMillis()) {
            android.util.Log.d(sourceFileName, "Exit für Einstellungen aktiv – Alarm startet Activity nicht");
            StartTurmtechnikService.setTimeTurmtechnik(60);
            return;
        }

        // App nur starten wenn Akku/Netz OK (wie beim Boot)
        int schwellwert = 75;
        try {
            schwellwert = PlatinenDatabaseHelper.getInstance(context).getPowerOffAkkuProzent();
        } catch (Exception e) { }
        boolean powerOk = isPowerConnected(context);
        int batt = getBatteryLevel(context);
        if (!powerOk && batt <= schwellwert) {
            android.util.Log.d(sourceFileName, "Alarm: Akku " + batt + "% und kein Netz – starte Activity nicht");
            StartTurmtechnikService.setTimeTurmtechnik(60);
            return;
        }

        // Passwort 5644: 15 Min Hintergrund – in dieser Zeit Activity nicht starten
        long backgroundUntil = TurmtechnikActivity.getBackgroundAllowedUntilMillis(context);
        if (backgroundUntil > 0 && System.currentTimeMillis() < backgroundUntil) {
            android.util.Log.d(sourceFileName, "Noch in Hintergrundphase (5644), Alarm startet Activity nicht");
            StartTurmtechnikService.setTimeTurmtechnik(60);
            return;
        }

        if (StaticVariable.autostartDerApp) {
            // Option "Activity wecken" aus: nicht alle 15 Min in den Vordergrund holen (verhindert Neustart-Loop, Nebenuhr läuft stabiler)
            String alarmActivityWecken = null;
            try {
                alarmActivityWecken = PlatinenDatabaseHelper.getInstance(context).getConfigValue("anlage_alarm_activity_wecken");
            } catch (Exception e) { /* Fallback: wecken */ }
            if ("aus".equalsIgnoreCase(alarmActivityWecken != null ? alarmActivityWecken.trim() : "")) {
                android.util.Log.d(sourceFileName, "Alarm: Activity-Wecken deaktiviert – nur Timer zurückgesetzt");
                StartTurmtechnikService.touchHeartbeat();
                StartTurmtechnikService.setTimeTurmtechnik(90);
                return;
            }
            // Keinen Doppelstart: Activity bereits im Vordergrund → nicht erneut starten (verhindert Absturz)
            if (StartTurmtechnikService.isTurmtechnikUiVisible(context)) {
                android.util.Log.d(sourceFileName, "TurmtechnikActivity bereits im Vordergrund, Alarm-Start übersprungen");
                StartTurmtechnikService.setTimeTurmtechnik(60);
                return;
            }
            workContext = context;
            // Kein touchHeartbeat() hier – wir zeigen nur Notification/Full-Screen-Intent. Wenn die Activity nicht öffnet,
            // soll der Service nach 90 s erneut versuchen (Absturz-Wiederherstellung), bis die App wirklich läuft.
            StartTurmtechnikService.launchTurmtechnikActivityFromBackground(context);
        }
    }

    public boolean isPowerConnected(Context context) {
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null) return false;
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        return plugged == BatteryManager.BATTERY_PLUGGED_AC
                || plugged == BatteryManager.BATTERY_PLUGGED_USB;
    }

    private int getBatteryLevel(Context context) {
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null) return 0;
        return intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
    }

}

