package tom.turmtechnik;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
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
            android.util.Log.d(sourceFileName, "Noch in 15-Min-Hintergrundphase (5644), Alarm startet Activity nicht");
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
            if (TurmtechnikActivity.isInForeground) {
                android.util.Log.d(sourceFileName, "TurmtechnikActivity bereits im Vordergrund, Alarm-Start übersprungen");
                StartTurmtechnikService.setTimeTurmtechnik(60);
                return;
            }
            workContext = context;
            {
                StartTurmtechnikService.touchHeartbeat();
                StartTurmtechnikService.setTimeTurmtechnik(90);  // nicht 2 s (verhinderte Doppelstart 1–2 s nach App-Start)

                Intent intent1 = new Intent(context, TurmtechnikActivity.class);
                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(intent1);
            }
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


