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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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
    private static volatile String lastVisibleActivityClassName = "";
    private static volatile long lastVisibleActivityMs = 0L;

    private static final int NOTIFICATION_ID = 9001;
    /** Notification-ID für „App starten“ / Full-Screen-Intent. Öffentlich, damit TurmtechnikActivity sie beim Öffnen aufheben kann. */
    public static final int NOTIFICATION_ID_OPEN_APP = 9002;
    private static final String CHANNEL_ID = "turmtechnik_service";
    private static final String CHANNEL_ID_OPEN = "turmtechnik_open";
    private static final int OPEN_APP_REQUEST_CODE = 2;
    private static final int DIRECT_ACTIVITY_ALARM_REQUEST_CODE = 2002;
    private static final long OPEN_APP_NOTIFICATION_CANCEL_DELAY_MS = 10_000L;
    private static final long OPEN_APP_ACTIVITY_FALLBACK_DELAY_MS = 2_000L;
    private static final int MAX_ACTIVITY_LAUNCH_CHECKS = 3;
    private static final long RECENT_VISIBLE_ACTIVITY_WINDOW_MS = 15_000L;
    private static final int BACKGROUND_TASK_RECHECK_SEC = 300;
    private static final int TURMTECHNIK_ACTIVITY_LAUNCH_FLAGS =
            Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED;

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
        setTimeTurmtechnik(RESTART_AFTER_CRASH_SEC);
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

    public static void reportVisibleActivity(Context context, String activityClassName) {
        lastVisibleActivityClassName = activityClassName != null ? activityClassName : "";
        lastVisibleActivityMs = System.currentTimeMillis();
        touchHeartbeat();
        cancelDirectActivityLaunch(context);
        Log.i(sourceFileName, "Activity sichtbar: " + lastVisibleActivityClassName);
    }

    public static void reportHiddenActivity(String activityClassName) {
        Log.i(sourceFileName, "Activity verborgen: " + (activityClassName != null ? activityClassName : ""));
    }


    @Override
    public void onCreate() {
        //Log.e("SERVICE" , "on Create durchlaufen") ;

        instance = this;
        doRun = true;
        lastHeartbeatMs = System.currentTimeMillis(); // Sofort „virtueller“ Heartbeat, damit nicht sofort Activity gestartet wird
        TurmtechnikActivity.ensureCoreRuntimeStarted(getApplicationContext());
        // Erster Start: 30 s Puffer, damit Activity onCreate/Heartbeat schafft (verhindert Neustart-Loop)
        setTimeTurmtechnik(30);

        // Foreground-Service, damit Android den Service nicht wegen "app idle" beendet
        startForegroundIfSupported();

        // Nach Prozess-Neustart (z. B. nach Absturz mit WebUiActivity) kurz warten, damit Prozess/Chromium sich stabilisieren (vermeidet SIGSEGV in JDWP-Thread)
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!doRun) return;
                RestartTurmtechnik restartTurmtechnik = new RestartTurmtechnik();
                restartTurmtechnik.start();
            }
        }, 3000);

        // neu 19.11.2014 -- Starte turmtechnik unbedingt interval
        AlarmManager alarmManager;
        PendingIntent pendingIntent1;
        Intent alarmIntent = new Intent(StartTurmtechnikService.this, AlarmReceiver.class);
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            piFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        pendingIntent1 = PendingIntent.getBroadcast(StartTurmtechnikService.this, 0, alarmIntent, piFlags);
        alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        //Calendar calendar = Calendar.getInstance();
        //calendar.setTimeInMillis(System.currentTimeMillis()) ;
        //calendar.set(Calendar.HOUR_OF_DAY, 3);
        //calendar.set(Calendar.MINUTE, 5) ;
        //calendar.set(Calendar.SECOND, 0) ;

        //alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 24*60*60*1000, pendingIntent1);
        // alle 15 Minuten starten
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
            Log.d(sourceFileName, "Noch in Hintergrundphase (5644), starte Activity nicht");
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
        TurmtechnikActivity.ensureCoreRuntimeStarted(getApplicationContext());
        // Keinen Doppelstart: Activity bereits im Vordergrund → nicht erneut starten (verhindert Absturz)
        if (isTurmtechnikUiVisible(this)) {
            Log.d(sourceFileName, "Turmtechnik-UI bereits sichtbar, Start übersprungen");
            setTimeTurmtechnik(60);
            return;
        }
        // Wenn der App-Task noch lebt, aber der Launcher vorne ist, nicht permanent aus dem Hintergrund nach vorne zwingen.
        // Die Runtime läuft weiter; sichtbares Nach-vorne-Holen gelingt auf Samsung aus dem Hintergrund oft ohnehin nicht.
        if (hasTurmtechnikTask(this)) {
            Log.d(sourceFileName, "Turmtechnik-Task existiert bereits im Hintergrund – Vordergrundstart übersprungen");
            setTimeTurmtechnik(BACKGROUND_TASK_RECHECK_SEC);
            return;
        }
        // Noch nie Heartbeat → nicht starten (App evtl. gerade am Starten)
        if (lastHeartbeatMs == 0) {
            Log.d(sourceFileName, "Noch kein Heartbeat, Start übersprungen");
            setTimeTurmtechnik(RESTART_AFTER_CRASH_SEC);
            return;
        }
        // Nur starten, wenn wirklich 90 s kein Heartbeat: verhindert Neustart-Loop (Absturz-Erkennung)
        long ago = System.currentTimeMillis() - lastHeartbeatMs;
        if (ago < MIN_NO_HEARTBEAT_MS) {
            Log.d(sourceFileName, "Heartbeat vor " + (ago/1000) + " s (< 90 s), Start übersprungen");
            setTimeTurmtechnik(RESTART_AFTER_CRASH_SEC);
            return;
        }

        Log.w(sourceFileName, "Kein Heartbeat seit " + (ago/1000) + " s – starte Activity neu (Absturz-Wiederherstellung)");
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean lastWasScreensaver = prefs.getBoolean(PREF_LAST_STARTED_SCREENSAVER, false);
        int crashCount = prefs.getInt(PREF_CLOCKSAVER_CRASH_COUNT, 0);
        if (lastWasScreensaver) {
            crashCount = Math.min(CLOCKSAVER_CRASH_THRESHOLD, crashCount + 1);
            prefs.edit().putInt(PREF_CLOCKSAVER_CRASH_COUNT, crashCount).putBoolean(PREF_LAST_STARTED_SCREENSAVER, false).apply();
        }

        if (crashCount >= CLOCKSAVER_CRASH_THRESHOLD) {
            Log.i(getPackageName(), "Clocksaver crasht zu oft (" + CLOCKSAVER_CRASH_THRESHOLD + " Crashes) - DEAKTIVIERT!");
            launchTurmtechnikActivityFromBackground(this);
            return;
        }

        // Haupt-Activity in den Vordergrund holen – per Full-Screen-Intent, damit Android den Start aus dem Hintergrund erlaubt („Background activity start“)
        launchTurmtechnikActivityFromBackground(this);
    }

    /**
     * Startet TurmtechnikActivity aus dem Hintergrund (nach Alarm / Absturz-Wiederherstellung).
     * Per Full-Screen-Intent-Notification, damit Android den Start erlaubt („Background activity start“).
     * Ohne Full-Screen-Intent würde startActivity() blockiert. Beim Tippen auf die Notification öffnet sich die App ebenfalls.
     */
    public static void launchTurmtechnikActivityFromBackground(Context context) {
        if (context == null) return;
        final Intent intent = createTurmtechnikLaunchIntent(context);
        try {
            Log.i(sourceFileName, "launchTurmtechnikActivityFromBackground: starte ohne Open-App-Notification");
            context.startActivity(intent);
        } catch (Exception e) {
            Log.w(sourceFileName, "Direktstart aus Hintergrund fehlgeschlagen: " + (e != null ? e.getMessage() : ""));
        }
        scheduleActivityLaunchFallback(context.getApplicationContext(), intent, 1);
        scheduleOpenAppNotificationCancel(context.getApplicationContext());
    }

    /** Zeigt Notification „Tippen zum Öffnen“ (Fallback ohne Full-Screen-Intent). */
    private static void showOpenAppNotification(Context context, Intent activityIntent, int pendingFlags) {
        try {
            PendingIntent openPi = PendingIntent.getActivity(context, OPEN_APP_REQUEST_CODE, activityIntent, pendingFlags);
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel ch = new NotificationChannel(CHANNEL_ID_OPEN,
                        context.getString(R.string.app_name) + " – Start",
                        NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("Tippen öffnet die Turmtechnik-App");
                ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                ch.enableVibration(true);
                nm.createNotificationChannel(ch);
                Notification n = new Notification.Builder(context, CHANNEL_ID_OPEN)
                        .setContentTitle(" ")
                        .setContentText(" ")
                        .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                        .setContentIntent(openPi)
                        .setAutoCancel(true)
                        .setOngoing(true)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .build();
                nm.notify(NOTIFICATION_ID_OPEN_APP, n);
                // Fenster nach kurzer Zeit entfernen
                scheduleOpenAppNotificationCancel(context.getApplicationContext());
            }
        } catch (Exception e) {
            Log.w(sourceFileName, "Open-App-Notification: " + (e != null ? e.getMessage() : ""));
        }
    }

    /** Notification ohne Full-Screen für API < 26. */
    private static void showOpenAppNotificationLegacy(Context context, Intent activityIntent, int pendingFlags) {
        showOpenAppNotification(context, activityIntent, pendingFlags);
    }

    public static Intent createTurmtechnikLaunchIntent(Context context) {
        Intent intent = new Intent(context, WebUiActivity.class);
        intent.putExtra(WebUiActivity.EXTRA_PATH, "/app-seite1.html");
        intent.putExtra(WebUiActivity.EXTRA_USE_LOCALHOST, true);
        intent.addFlags(TURMTECHNIK_ACTIVITY_LAUNCH_FLAGS);
        // SHOW_WHEN_LOCKED/TURN_SCREEN_ON nicht per Intent-Flags (ab Android 14 nur noch erlaubte Flags), ggf. in der Activity/Manifest setzen
        return intent;
    }

    public static void scheduleDirectActivityLaunch(Context context, long triggerAtMillis, String reason) {
        if (context == null) return;
        try {
            Context appContext = context.getApplicationContext();
            AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.w(sourceFileName, "Direktstart-Alarm nicht gesetzt (" + reason + "): AlarmManager null");
                return;
            }
            Intent intent = createTurmtechnikLaunchIntent(appContext);
            int flags = getImmutableUpdateCurrentPendingIntentFlags();
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    appContext,
                    DIRECT_ACTIVITY_ALARM_REQUEST_CODE,
                    intent,
                    flags);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
            Log.i(sourceFileName, "Direktstart-Alarm gesetzt für " + triggerAtMillis + " (" + reason + ")");
        } catch (Exception e) {
            Log.w(sourceFileName, "Direktstart-Alarm fehlgeschlagen (" + reason + "): " + (e != null ? e.getMessage() : ""));
        }
    }

    public static void cancelDirectActivityLaunch(Context context) {
        if (context == null) return;
        try {
            Context appContext = context.getApplicationContext();
            AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;
            Intent intent = createTurmtechnikLaunchIntent(appContext);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    appContext,
                    DIRECT_ACTIVITY_ALARM_REQUEST_CODE,
                    intent,
                    getImmutableUpdateCurrentPendingIntentFlags());
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        } catch (Exception e) {
            Log.w(sourceFileName, "Direktstart-Alarm abbrechen fehlgeschlagen: " + (e != null ? e.getMessage() : ""));
        }
    }

    private static int getImmutableUpdateCurrentPendingIntentFlags() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return flags;
    }

    private static void scheduleOpenAppNotificationCancel(final Context appContext) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                NotificationManager notificationManager =
                        (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.cancel(NOTIFICATION_ID_OPEN_APP);
                }
            }
        }, OPEN_APP_NOTIFICATION_CANCEL_DELAY_MS);
    }

    public static boolean isTurmtechnikUiVisible(Context context) {
        if (TurmtechnikActivity.isInForeground) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (now - lastVisibleActivityMs <= RECENT_VISIBLE_ACTIVITY_WINDOW_MS
                && lastVisibleActivityClassName != null
                && !lastVisibleActivityClassName.isEmpty()) {
            return true;
        }
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ActivityManager.AppTask foregroundTask = findTurmtechnikAppTask(context);
                if (foregroundTask != null) {
                    ActivityManager.RecentTaskInfo taskInfo = foregroundTask.getTaskInfo();
                    if (taskInfo != null
                            && taskInfo.topActivity != null
                            && taskInfo.id == taskInfo.persistentId) {
                        Log.i(sourceFileName, "Start-Check: App-Task sichtbar mit " + taskInfo.topActivity.getClassName());
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(sourceFileName, "Start-Check AppTask fehlgeschlagen: " + (e != null ? e.getMessage() : ""));
        }
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                List<RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
                if (processes != null) {
                    String packageName = context.getPackageName();
                    for (RunningAppProcessInfo processInfo : processes) {
                        if (processInfo != null
                                && packageName.equals(processInfo.processName)
                                && processInfo.importance <= RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                                && now - lastVisibleActivityMs <= RECENT_VISIBLE_ACTIVITY_WINDOW_MS) {
                            Log.i(sourceFileName, "Start-Check: Vordergrund-Prozess mit letzter sichtbarer Activity " + lastVisibleActivityClassName);
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(sourceFileName, "Start-Check Prozessprüfung fehlgeschlagen: " + (e != null ? e.getMessage() : ""));
        }
        return false;
    }

    private static boolean hasTurmtechnikTask(Context context) {
        return findTurmtechnikAppTask(context) != null;
    }

    private static ActivityManager.AppTask findTurmtechnikAppTask(Context context) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return null;
        }
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager == null) return null;
            List<ActivityManager.AppTask> appTasks = activityManager.getAppTasks();
            if (appTasks == null) return null;
            String packageName = context.getPackageName();
            for (ActivityManager.AppTask appTask : appTasks) {
                ActivityManager.RecentTaskInfo taskInfo = appTask != null ? appTask.getTaskInfo() : null;
                if (taskInfo != null
                        && taskInfo.baseIntent != null
                        && taskInfo.baseIntent.getComponent() != null
                        && packageName.equals(taskInfo.baseIntent.getComponent().getPackageName())) {
                    return appTask;
                }
                if (taskInfo != null
                        && taskInfo.topActivity != null
                        && packageName.equals(taskInfo.topActivity.getPackageName())) {
                    return appTask;
                }
                if (taskInfo != null
                        && taskInfo.baseActivity != null
                        && packageName.equals(taskInfo.baseActivity.getPackageName())) {
                    return appTask;
                }
            }
        } catch (Exception e) {
            Log.w(sourceFileName, "App-Task-Suche fehlgeschlagen: " + (e != null ? e.getMessage() : ""));
        }
        return null;
    }

    private static void scheduleActivityLaunchFallback(final Context appContext, final Intent activityIntent, final int attempt) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isTurmtechnikUiVisible(appContext)) {
                    Log.i(sourceFileName, "Fallback-Start erfolgreich: Turmtechnik-UI sichtbar (" + lastVisibleActivityClassName + ")");
                    return;
                }
                if (hasTurmtechnikTask(appContext)) {
                    Log.i(sourceFileName, "Fallback-Start übersprungen: Turmtechnik-Task lebt bereits im Hintergrund");
                    return;
                }
                if (TurmtechnikActivity.getExitForSettingsUntilMillis(appContext) > System.currentTimeMillis()) {
                    Log.i(sourceFileName, "Fallback-Start übersprungen: Exit für Einstellungen weiterhin aktiv");
                    return;
                }
                if (TurmtechnikActivity.getBackgroundAllowedUntilMillis(appContext) > System.currentTimeMillis()) {
                    Log.i(sourceFileName, "Fallback-Start übersprungen: Hintergrundphase 5644 aktiv");
                    return;
                }
                try {
                    Log.i(sourceFileName, "Fallback-Start Versuch " + attempt + ": starte TurmtechnikActivity explizit per startActivity()");
                    appContext.startActivity(new Intent(activityIntent));
                } catch (Exception e) {
                    Log.w(sourceFileName, "Fallback-Start fehlgeschlagen: " + (e != null ? e.getMessage() : ""));
                }
                if (attempt < MAX_ACTIVITY_LAUNCH_CHECKS) {
                    scheduleActivityLaunchFallback(appContext, activityIntent, attempt + 1);
                } else {
                    Log.w(sourceFileName, "Start-Check: Turmtechnik-UI nach " + MAX_ACTIVITY_LAUNCH_CHECKS + " Versuchen weiterhin nicht sichtbar");
                }
            }
        }, OPEN_APP_ACTIVITY_FALLBACK_DELAY_MS);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Turmtechnik-Service",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
            return new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle(" ")
                    .setContentText(" ")
                    .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                    .setOngoing(true)
                    .build();
        } else {
            return new Notification.Builder(this)
                    .setContentTitle(" ")
                    .setContentText(" ")
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
