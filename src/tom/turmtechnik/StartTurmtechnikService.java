package tom.turmtechnik;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.util.List;

public class StartTurmtechnikService extends Service {

    private final static String sourceFileName = "StartTurmtechnikService";

    /** Intent-Action: Activity meldet sich lebend – Timer wird zurückgesetzt. Bei Absturz fehlt das Signal, dann startet der Service die App nach RESTART_AFTER_CRASH_SEC. */
    public static final String ACTION_HEARTBEAT = "tom.turmtechnik.HEARTBEAT";
    /** Sekunden ohne Heartbeat, danach Activity neu starten (Absturz-Erkennung). 90 s verhindert Neustart-Loop bei langer onCreate. */
    private static final int RESTART_AFTER_CRASH_SEC = 90;
    /** Mindestens so viele ms ohne Heartbeat, erst dann Activity starten (verhindert Doppelstart trotz touchHeartbeat-Race). */
    private static final long MIN_NO_HEARTBEAT_MS = 90_000L;

    public static StartTurmtechnikService instance = null;
    private static volatile long lastHeartbeatMs = 0L;

    private static final int NOTIFICATION_ID = 9001;
    private static final String CHANNEL_ID = "turmtechnik_service";

    private PowerManager pm;
    private PowerManager.WakeLock wl = null;

    private boolean doRun = true;
    private Intent intent;
    /** Startwert groß genug, damit die Activity onCreate/onResume und ersten Heartbeat schafft (sonst Neustart-Loop). */
    private static int restartTimeTurmtechnik = 60;

    private boolean powerLostFlag = false;

    public static boolean isServiceCreated() {
        return instance != null;
    }

    /** SharedPreferences-Key: Anzahl Clocksaver-Crashes (nach 3 wird Bildschirmschoner deaktiviert, bis Heartbeat vom Hauptbildschirm). */
    private static final String PREF_CLOCKSAVER_CRASH_COUNT = "clocksaver_crash_count";
    /** SharedPreferences-Key: Zuletzt wurde WebUiActivity (Screensaver) gestartet – wenn danach kein Heartbeat, zählt als Crash. */
    private static final String PREF_LAST_STARTED_SCREENSAVER = "last_started_screensaver";
    private static final int CLOCKSAVER_CRASH_THRESHOLD = 3;
    private static final String PREF_NAME = "Turmtechnik";

    /** Von TurmtechnikActivity/WebUiActivity aufrufen – Timer wird zurückgesetzt. Setzt auch Clocksaver-Crash-Zähler zurück, damit Schoner wieder aktiviert werden kann. */
    public static void touchHeartbeat() {
        lastHeartbeatMs = System.currentTimeMillis();
        resetClocksaverCrashCount();
    }

    /** Setzt den Clocksaver-Crash-Zähler zurück (z. B. nach Heartbeat von Hauptbildschirm), damit der Bildschirmschoner wieder gestartet wird. */
    public static void resetClocksaverCrashCount() {
        try {
            Context ctx = instance != null ? instance.getApplicationContext() : null;
            if (ctx != null) {
                SharedPreferences p = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                p.edit().putInt(PREF_CLOCKSAVER_CRASH_COUNT, 0).putBoolean(PREF_LAST_STARTED_SCREENSAVER, false).apply();
            }
        } catch (Exception e) {
            Log.w(sourceFileName, "resetClocksaverCrashCount: " + (e != null ? e.getMessage() : ""));
        }
    }

    public static synchronized void setTimeTurmtechnik(int restartTime) {
        restartTimeTurmtechnik = restartTime;
    }


    @Override
    public void onCreate() {
        //Log.e("SERVICE" , "on Create durchlaufen") ;

        instance = this;
        doRun = true;
        lastHeartbeatMs = System.currentTimeMillis(); // Sofort „virtueller“ Heartbeat, damit nicht sofort Activity gestartet wird
        // Erster Start: 30 s Puffer, damit Activity onCreate/Heartbeat schafft (verhindert Neustart-Loop)
        setTimeTurmtechnik(30);

        // Foreground-Service, damit Android den Service nicht wegen "app idle" beendet
        startForegroundIfSupported();

        RestartTurmtechnik restartTurmtechnik = new RestartTurmtechnik();
        restartTurmtechnik.start();

        // neu 19.11.2014 -- Starte turmtechnik unbedingt interval

        AlarmManager alarmManager;
        PendingIntent pendingIntent1;

        Intent alarmIntent = new Intent(StartTurmtechnikService.this, AlarmReceiver.class);
        pendingIntent1 = PendingIntent.getBroadcast(StartTurmtechnikService.this, 0, alarmIntent, 0);
        alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        //Calendar calendar = Calendar.getInstance();
        //calendar.setTimeInMillis(System.currentTimeMillis()) ;
        //calendar.set(Calendar.HOUR_OF_DAY, 3);
        //calendar.set(Calendar.MINUTE, 5) ;
        //calendar.set(Calendar.SECOND, 0) ;

        //alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 24*60*60*1000, pendingIntent1);
        // alle 15 Minuten auf jeden fall starten
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
                15 * 60 * 1000,
                // 5000, // 5 sek fuer test
                pendingIntent1);


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_HEARTBEAT.equals(intent.getAction())) {
            touchHeartbeat();
            setTimeTurmtechnik(RESTART_AFTER_CRASH_SEC);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        instance = null;
        doRun = false;
        super.onDestroy();
    }

    private void startTurmtechnik() // neu nur wenn startAppAutomatisch = EIN
    {
        // „App beenden für Einstellungen“: In dieser Zeit nicht automatisch starten
        if (TurmtechnikActivity.getExitForSettingsUntilMillis(this) > System.currentTimeMillis()) {
            Log.d(sourceFileName, "Exit für Einstellungen aktiv – starte Activity nicht");
            setTimeTurmtechnik(60);
            return;
        }
        // Passwort 5644: App darf 15 Min im Hintergrund bleiben – in dieser Zeit nicht in den Vordergrund holen
        long backgroundUntil = TurmtechnikActivity.getBackgroundAllowedUntilMillis(this);
        if (backgroundUntil > 0 && System.currentTimeMillis() < backgroundUntil) {
            Log.d(sourceFileName, "Noch in 15-Min-Hintergrundphase (5644), starte Activity nicht");
            setTimeTurmtechnik(60);
            return;
        }

        Boolean autostartFromDb = null;
        try {
            autostartFromDb = PlatinenDatabaseHelper.getInstance(this).getAutostartDerApp();
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

        // App soll immer laufen (Start nur verweigert bei niedrigem Akku + kein Netz – das prüft der Aufrufer bereits)
        if (!StaticVariable.autostartDerApp) {
            Log.d(sourceFileName, "Autostart in Config aus – starte Activity nicht");
            return;
        }
        // Keinen Doppelstart: Activity bereits im Vordergrund → nicht erneut starten (verhindert Absturz)
        if (TurmtechnikActivity.isInForeground) {
            Log.d(sourceFileName, "TurmtechnikActivity bereits im Vordergrund, Start übersprungen");
            setTimeTurmtechnik(60);
            return;
        }
        // Noch nie Heartbeat → nicht starten (App evtl. gerade am Starten)
        if (lastHeartbeatMs == 0) {
            Log.d(sourceFileName, "Noch kein Heartbeat, Start übersprungen");
            setTimeTurmtechnik(RESTART_AFTER_CRASH_SEC);
            return;
        }
        // Nur starten, wenn wirklich 90 s kein Heartbeat: verhindert Neustart-Loop
        long ago = System.currentTimeMillis() - lastHeartbeatMs;
        if (ago < MIN_NO_HEARTBEAT_MS) {
            Log.d(sourceFileName, "Heartbeat vor " + (ago/1000) + " s (< 90 s), Start übersprungen");
            setTimeTurmtechnik(RESTART_AFTER_CRASH_SEC);
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean lastWasScreensaver = prefs.getBoolean(PREF_LAST_STARTED_SCREENSAVER, false);
        int crashCount = prefs.getInt(PREF_CLOCKSAVER_CRASH_COUNT, 0);
        if (lastWasScreensaver) {
            crashCount = Math.min(CLOCKSAVER_CRASH_THRESHOLD, crashCount + 1);
            prefs.edit().putInt(PREF_CLOCKSAVER_CRASH_COUNT, crashCount).putBoolean(PREF_LAST_STARTED_SCREENSAVER, false).apply();
        }

        if (crashCount >= CLOCKSAVER_CRASH_THRESHOLD) {
            Log.i(getPackageName(), "Clocksaver crasht zu oft (" + CLOCKSAVER_CRASH_THRESHOLD + " Crashes) - DEAKTIVIERT!");
            intent = new Intent(this, TurmtechnikActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return;
        }

        // Haupt-Activity in den Vordergrund holen (nicht nur WebUiActivity/Schoner), damit Layout und Relais-Steuerung laufen
        intent = new Intent(this, TurmtechnikActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private class RestartTurmtechnik extends Thread {

        private int batteryLevel;

        public void run() {
            while (doRun == true) {
                sleepTime(1000);

                decrementTimeTurmtechnik();
                //Log.e("restartTime" , "=" + restartTimeTurmtechnik) ;
                //Log.e("bigClockTimeout" , "=" + StaticVariable.bigClockTimeout) ;

                //restartTimeTurmtechnik = Math.min(30, restartTimeTurmtechnik) ;
                if (restartTimeTurmtechnik <= 0) {
                        // Akku-Schwellwert aus DB (wie BootUpReceiver)
                        try {
                            int schwellwert = PlatinenDatabaseHelper.getInstance(StartTurmtechnikService.this).getPowerOffAkkuProzent();
                            StaticVariable.batt_shut_down_level2 = schwellwert;
                        } catch (Exception e) {
                            // Standard bleibt
                        }
                    if (isPowerConnected(getBaseContext())
                            || batteryLevel >= StaticVariable.getBattEinschaltLevel())

                    {
                        //Log.e("starte" , "Turmtechnik neu") ;
                        startTurmtechnik();
                        setTimeTurmtechnik(60); // zieht sich selber auf 10 Sekunden auf
                        ; // wegen log File onResume() 12.11.13 nur alle 60 Sekunden

                        // Fashion Clock entfernt – Bildschirmschoner ist jetzt Web-UI (screensaver.html)

                        //if ( (wl == null) || ( StaticVariable.screenIsOff == true) )
                        //{
                        //Log.e("call" , "PowerManager") ;
                        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                        if (!(pm.isScreenOn())) {
                            //wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "tom.turmtechnik:Turmtechnik");
                            //wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "tom.turmtechnik:Turmtechnik");
                            //int wakeFlags = (PowerManager.FULL_WAKE_LOCK |
                            //		PowerManager.ACQUIRE_CAUSES_WAKEUP |
                            //		PowerManager.SCREEN_DIM_WAKE_LOCK  ) ;
                            int wakeFlags = PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP;

                            wl = pm.newWakeLock(wakeFlags, "tom.turmtechnik:Turmtechnik");


                            wl.acquire(); // ab jetzt bleibt der
                        }                                    // Bildschirm an
                        // 14.11.13 das war veraltet -- jetzt bleibt cpu on
                        //setKeepScreenOn(true); // und nun auch der Bildschirm
                        //}

                        //Log.e("bigClockTimeout" , "=" + StaticVariable.bigClockTimeout) ;


                    } else {
                        setTimeTurmtechnik(60); // alle 10 Sekunden testen
                        //Log.e("Service keine" , "Power" ) ;
                        // Fashion Clock entfernt – keine externe App mehr

                        if (wl != null) {
                            //wl.release();
                            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "tom.turmtechnik:Turmtechnik");
                            wl.acquire();
                            wl = null;

                        }
                    }

                }
            }

        } // ende von run

        public boolean isPowerConnected(Context context) {
            Intent intent = context.registerReceiver(null, new IntentFilter(
                    Intent.ACTION_BATTERY_CHANGED));
            int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            batteryLevel = intent.getIntExtra("level", 0);
            return plugged == BatteryManager.BATTERY_PLUGGED_AC
                    || plugged == BatteryManager.BATTERY_PLUGGED_USB;
        }


        public synchronized void decrementTimeTurmtechnik() {
            restartTimeTurmtechnik--;
            //Log.e("Variable" , "restartTimeTurmtechnik=" + restartTimeTurmtechnik) ;
        }


        /** Nicht mehr verwendet – Bildschirmschoner ist Web-UI (screensaver.html). */
        protected void startFashionClock() { }

        /** Nicht mehr verwendet – Fashion Clock entfernt. */
        private boolean checkIfFashionClockIsRunning() { return false; }

        /** Nicht mehr verwendet – Fashion Clock entfernt. */
        private void killFashionClock() { }

        private void sleepTime(long time) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    } // ende der Klasse


    /**
     * Startet den Service im Vordergrund mit einer persistenten Notification.
     * Verhindert, dass das System den Service wegen "app idle" stoppt.
     */
    private void startForegroundIfSupported() {
        try {
            Notification notification = buildServiceNotification();
            if (notification != null) {
                startForeground(NOTIFICATION_ID, notification);
                Log.i(sourceFileName, "Service als Foreground-Service gestartet");
            }
        } catch (Exception e) {
            Log.w(sourceFileName, "startForeground nicht möglich: " + e.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    private Notification buildServiceNotification() {
        String title = getString(R.string.app_name);
        String text = "Kirchturmtechnik – Service aktiv";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Turmtechnik-Service",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
            return new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                    .setOngoing(true)
                    .build();
        } else {
            return new Notification.Builder(this)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                    .setOngoing(true)
                    .build();
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

}
