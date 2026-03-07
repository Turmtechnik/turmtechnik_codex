package tom.turmtechnik;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

public class BootUpReceiver extends BroadcastReceiver {

    private final static String sourceFileName = "BootUpReciver";
    /** Nach Stromausfall/Akku-Entladung melden manche Geräte Akku erst verzögert – nach dieser Zeit (ms) Activity starten, falls beim Boot nicht gestartet. */
    private static final long DELAYED_START_AFTER_BOOT_MS = 60_000L;

    @Override
    public void onReceive(Context context, Intent intent) {
        Context appContext = context.getApplicationContext();

        try {
            KeyguardManager myKeyGuard = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            if (myKeyGuard != null) {
                KeyguardLock myLock = myKeyGuard.newKeyguardLock(null);
                if (myLock != null) myLock.disableKeyguard();
            }
        } catch (Exception e) {
            Log.w(sourceFileName, "Keyguard nicht deaktivierbar (z. B. nach Stromausfall)", e);
        }

        try {
        Boolean autostartFromDb = null;
        try {
            autostartFromDb = PlatinenDatabaseHelper.getInstance(appContext).getAutostartDerApp();
        } catch (Exception e) {
            Log.w(sourceFileName, "Autostart aus DB fehlgeschlagen, Fallback Excel", e);
            StaticVariable.autostartDerApp = true; // Nach Stromausfall/DB-Problemen: App starten
        }
        if (autostartFromDb != null) {
            StaticVariable.autostartDerApp = autostartFromDb;
        } else {
            // Nicht in DB: Standard = ein (App soll immer laufen). Kein Excel mehr – nur Datenbank.
            StaticVariable.autostartDerApp = true;
        }

        Log.e("autostart", "StaticVariable.autostartDerApp=" + StaticVariable.autostartDerApp);

        // Akku-Schwellwert aus DB (Power-off bei %), damit Service und Boot dieselbe Grenze nutzen
        try {
            int schwellwert = PlatinenDatabaseHelper.getInstance(appContext).getPowerOffAkkuProzent();
            StaticVariable.batt_shut_down_level2 = schwellwert;
        } catch (Exception e) {
            Log.w(sourceFileName, "Power-off-Akku % aus DB nicht lesbar, behalte Standard", e);
        }

        // App soll laufen, außer: Akku sicher zu niedrig UND keine Netzversorgung. Direkt nach Stromausfall/Boot melden manche Geräte 0 % oder Intent noch nicht bereit – dann starten wir trotzdem.
        boolean powerConnected = isPowerConnected(context);
        int batteryLevel = getBatteryLevel(context);
        int einschaltLevel = StaticVariable.getBattEinschaltLevel();
        boolean batteryUncertain = (batteryLevel == 0 && !powerConnected); // 0 % ohne Netz = oft „noch nicht gelesen“ nach Boot
        boolean laufenErlaubt = powerConnected || batteryLevel >= einschaltLevel || batteryUncertain;
        if (!laufenErlaubt) {
            Log.w(sourceFileName, "Boot: Akku " + batteryLevel + "% unter Einschalt-Schwelle " + einschaltLevel + "% (Ausschalt " + StaticVariable.batt_shut_down_level2 + "%) und kein Netz – starte nur Service, keine Activity");
        }

        context.startService(new Intent(appContext, StartTurmtechnikService.class));

        // Bewegungserkennung/Kamera ausgebaut – MotionDetectionService wird nicht mehr gestartet

        // Passwort 5644: 15 Min Hintergrund – in dieser Zeit nach Boot Activity nicht starten
        long backgroundUntil = TurmtechnikActivity.getBackgroundAllowedUntilMillis(context);
        if (backgroundUntil > 0 && System.currentTimeMillis() < backgroundUntil) {
            Log.d(sourceFileName, "Noch in Hintergrundphase (5644), starte nach Boot keine Activity");
            return;
        }

        // „App beenden für Einstellungen“: In dieser Zeit nach Boot Activity nicht starten
        if (TurmtechnikActivity.getExitForSettingsUntilMillis(context) > System.currentTimeMillis()) {
            Log.d(sourceFileName, "Exit für Einstellungen aktiv – starte nach Boot keine Activity");
            return;
        }

        if (StaticVariable.autostartDerApp && laufenErlaubt) {
            startTurmtechnikActivity(appContext);
        } else if (StaticVariable.autostartDerApp && !laufenErlaubt) {
            // Nach Stromausfall/Akku: Beim Boot evtl. Akku noch 0 % – nach 60 s erneut prüfen und Activity starten
            final Context ctx = appContext;
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean powerNow = isPowerConnected(ctx);
                        int levelNow = getBatteryLevel(ctx);
                        int einschalt = StaticVariable.getBattEinschaltLevel();
                        if (powerNow || levelNow >= einschalt) {
                            Log.i(sourceFileName, "Verzögerter Start nach Boot: Akku " + levelNow + "%, Netz=" + powerNow);
                            startTurmtechnikActivity(ctx);
                        }
                    } catch (Exception e) {
                        Log.w(sourceFileName, "Verzögerter Start nach Boot: " + (e != null ? e.getMessage() : ""), e);
                    }
                }
            }, DELAYED_START_AFTER_BOOT_MS);
        }
        } catch (Exception e) {
            Log.e(sourceFileName, "Boot nach Stromausfall/Akku fehlgeschlagen – starte Service und App trotzdem", e);
            context.startService(new Intent(appContext, StartTurmtechnikService.class));
            StaticVariable.autostartDerApp = true;
            startTurmtechnikActivity(appContext);
        }
    }

    private static void startTurmtechnikActivity(Context appContext) {
        StartTurmtechnikService.touchHeartbeat();
        StartTurmtechnikService.setTimeTurmtechnik(90);
        Intent i = new Intent(appContext, TurmtechnikActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        appContext.startActivity(i);
    }

    private boolean isPowerConnected(Context context) {
        try {
            Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (intent == null) return false;
            int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
        } catch (Exception e) {
            Log.w(sourceFileName, "Netzversorgung nicht lesbar", e);
            return false;
        }
    }

    private int getBatteryLevel(Context context) {
        try {
            Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (intent == null) return 0;
            return intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        } catch (Exception e) {
            Log.w(sourceFileName, "Akku-Stand nicht lesbar", e);
            return 0;
        }
    }
}
