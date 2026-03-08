package tom.turmtechnik;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.StrictMode;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import androidx.core.content.FileProvider;
//import android.support.annotation.RequiresApi;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.graphics.Color;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsoluteLayout;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.awr_technology.nmea_gps_clock.NMEA_gps_clock;
import com.pras.SpreadSheet;
import com.pras.WorkSheet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import java.text.SimpleDateFormat;
import java.util.Date;

import jxl.read.biff.BiffException;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;



public class TurmtechnikActivity extends Activity {


    public static Context turmtechnikContext;

    /** Aktuelle Activity-Instanz (für Web-UI: Automatik/Key an App übergeben). In onCreate gesetzt, in onDestroy auf null. */
    public static TurmtechnikActivity turmtechnikActivityInstance;

    /** True, wenn die Activity im Vordergrund ist (onResume wurde aufgerufen). Verhindert Doppelstart durch Alarm/Service. */
    public static volatile boolean isInForeground = false;

    private final static String sourceFileName = "TurmtechnikActivity";

    //private SoundGlocke soundGlocke = null ;

    public static int usableWith;
    public static int usableHeight;

    private final int SPALTE_C_MELODIE_NAME = 2;
    private final int SPALTE_B_FUNKTION = 1;

    private boolean onCreateFlag = true;
    /** True direkt nach onCreate – nur dann „Neustart App gestartet“ loggen (echter Neustart), nicht bei jedem onStart (z. B. Alarm). */
    private boolean activityJustCreated = false;
    /** Einmal pro Prozess „Neustart“ ins Crash-Log (LogCrash), damit nicht bei jeder Activity-Erstellung. */
    private static boolean crashLogNeustartAlreadyLoggedThisProcess = false;

    private NMEA_gps_clock nmea_gps_clock = null;

    AbsoluteLayout layoutCreated = null;

    private static Context context;

    private String excell_system;


    private int toggleJonsonTest = 0;

    static int layoutButtonsSize;

    private PowerManager pm;

    public static final int SET_BIG_CLOCK_TIME =   // 20 * 60 ;

            ((20) /* sekunden */ * 60) /* minuten */ * 15; // 15 Minuten nichts tun
    // 20 mal 50 ms 					// = start big clock

    //private int bigClocktimeOut = SET_BIG_CLOCK_TIME ;


    //public static final int RELAIS_COUNT = 4 * 8 * 4; // 4 bytes fuer Relais * 8
    public static final int RELAIS_COUNT = 256; // 28.07.16 auf 256 Relais[Integer] erweitert
    /** Max. Anzahl Tasten-Slots aus beschriftung_tasten (Layout/Web-UI). Später auf 96 o. ä. erweiterbar. */
    public static final int BESCHRIFTUNG_TASTEN_MAX_SLOTS = 48;
    /** Anzahl Slots auf „Seite 1“ (Offset für Seite 2). Entspricht get_counted_relais_total() bei 8×3-Grid. */
    public static final int BESCHRIFTUNG_TASTEN_SLOTS_PAGE1 = 24;
    // bites = 32 relais
    // 13.2.13 * 4 fuer 4
    // ip-adressen
    // das 5.te byte = pic
    // adresse
    // 12.2.13 -- nicht
    // mehr, reserve

    private final int BLUETOOTH_CONFIG_SHEET_NUMBER_NEW = 14;
    private final int VORSCHWINGEN_SHEET = 9;

    //	public static final int BESCHRIFTUNG_GLOCKEN_SHEET = 10;
    // wegen Fernwartung uber server wieder eine eigene Datei Beschriftung-Tasten.xls
    public static final int BESCHRIFTUNG_GLOCKEN_SHEET_NEW = 0;
    private String passwordExitNormal = "abc";
    private String passwordExitAndDeleteBeschriftungTasten = "abc";


    WindowManager.LayoutParams layoutParams;

    public static String sdCardPath;

    private WifiManager myWifiManager;

    private boolean wifiOk;
    
    // Web-Server für Konfiguration
    private static ConfigWebServer configWebServer;
    private static String webServerIpAddress = null; // IP-Adresse des Web-Servers

    private int batteryLevel;

    public static TextView outputView;
    private LinearLayout parentLayout;

    public static String beschriftungTastenFileString;
    public static String systemFileString;
    private int index;
    private static Seite1Layout layout;

    // private AvrNetIoThread avrnetiothread ;
    private static Serial_IoThread serial_iothread; // kann bluetooth oder carambola

    private UhrThread uhr_thread;
    private AusgangHeizungThreadNew2 ausgangHeizungThreadNew2 = null;
    // private GoogleDriveUpdateThread google_Drive_Update_Thread ;
    // wird alles im GoogleDriveThread gesteuert 3.8.13

    public static Integer[] relaisNumber = new Integer[RELAIS_COUNT]; // neu ab
    // 9.2.2013
    // --
    // Turmtechnik
    // protokoll
    // mit 5 Byte fuer Relais
    public static Integer[] hammerZeit = new Integer[RELAIS_COUNT];
    //public static Integer[] fernwartungID = new Integer[RELAIS_COUNT];
    public static String[] buttonId = new String[RELAIS_COUNT];
    public static boolean flagAutomaticOnOff = true;

    //public static String stringInfoText = "AUTOMATIK AUSGESCHALTET";
    public static String androidErrorText = "";


    private Handler handler;

    /** Bildschirmschoner: Verzögerung in ms (wird aus Anlagen-Config gelesen, Standard 5 Min). Konstante nur Fallback. */
    private static final long SCREENSAVER_DELAY_MS_DEFAULT = 5 * 60 * 1000L;
    private static final long SCREENSAVER_TOUCH_GRACE_MS = 2500L;
    /** Request-Code für Bildschirmschoner-Alarm (gleichbleibend, damit Alarm bei neuem Timer ersetzt wird). */
    private static final int SCREENSAVER_ALARM_REQUEST_CODE = 9002;
    /** Intent-Extra: Bildschirmschoner als Overlay anzeigen (Activity bleibt im Vordergrund, App wird nicht beendet). */
    public static final String EXTRA_SHOW_SCREENSAVER_OVERLAY = "show_screensaver_overlay";
    /** Broadcast-Action: Bewegungserkennung meldet „Schoner aus“ – Overlay ausblenden, kein Activity-Neustart. */
    public static final String ACTION_DISMISS_SCREENSAVER = "tom.turmtechnik.DISMISS_SCREENSAVER";
    /** Extra für Intent: nach Passwort-Dialog aus anderer Activity – Wert "beenden", "15min" oder "delete_beenden". */
    public static final String EXTRA_EXIT_ACTION = "exit_after_password";
    /** Nach 1 Stunde Inaktivität: KEEP_SCREEN_ON entfernen, damit der Bildschirm nach System-Timeout ausgeht. */
    private static final long SCREEN_OFF_DELAY_MS = 60 * 60 * 1000L;
    /** Heartbeat an StartTurmtechnikService alle 5 s – bei Absturz fehlt er, Service startet Activity nach ~15 s neu. */
    private static final long HEARTBEAT_INTERVAL_MS = 5 * 1000L;
    private Handler screensaverHandler = new Handler(Looper.getMainLooper());
    private Handler heartbeatHandler = new Handler(Looper.getMainLooper());
    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            Intent i = new Intent(TurmtechnikActivity.this, StartTurmtechnikService.class);
            i.setAction(StartTurmtechnikService.ACTION_HEARTBEAT);
            startService(i);
            heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
        }
    };
    private View screensaverOverlay;
    /** Content-Root (android.R.id.content), um beim Schoner-Ende das Hauptlayout wieder in den Vordergrund zu holen. */
    private ViewGroup screensaverContentRoot;
    private long screensaverShownAtMs = 0L;
    /** True, wenn die Activity nur zum Anzeigen des Bildschirmschoners gestartet wurde – dann keine WebUiActivity (app-seite1) starten, sonst überdeckt sie den Schoner. */
    private boolean activityStartedForScreensaver = false;
    private int screensaverPreviousSystemUiVisibility = 0;
    private int screensaverPreviousNavigationBarColor = 0;
    private int screensaverPreviousStatusBarColor = 0;
    private boolean screensaverActionBarWasVisible = false;
    private BroadcastReceiver dismissScreensaverReceiver = null;

    /** Blendet den Bildschirmschoner-Overlay aus (Tipp oder turmt://layout1 aus WebView). */
    private void dismissScreensaverOverlay() {
        Log.i("Screensaver", "Bildschirmschoner ausgeschaltet (Tipp)");
        applyScreensaverSystemUi(false);
        if (screensaverOverlay != null) {
            screensaverOverlay.setVisibility(View.GONE);
        }
        bringMainContentToFront();
        resetScreensaverTimer();
        if (activityStartedForScreensaver) {
            activityStartedForScreensaver = false;
            openWebUiInBrowser(this, "/app-seite1.html");
        }
    }

    /** Registriert Broadcast „Schoner aus“ von Bewegungserkennung – dann nur Overlay ausblenden, kein Activity-Neustart. */
    private void registerDismissScreensaverReceiver() {
        unregisterDismissScreensaverReceiver();
        dismissScreensaverReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_DISMISS_SCREENSAVER.equals(intent != null ? intent.getAction() : null)) {
                    dismissScreensaverOverlay();
                    bringMainContentToFront();
                }
            }
        };
        IntentFilter filter = new IntentFilter(ACTION_DISMISS_SCREENSAVER);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(dismissScreensaverReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(dismissScreensaverReceiver, filter);
        }
    }

    private void unregisterDismissScreensaverReceiver() {
        if (dismissScreensaverReceiver != null) {
            try {
                unregisterReceiver(dismissScreensaverReceiver);
            } catch (Exception ignored) { }
            dismissScreensaverReceiver = null;
        }
    }


    /** Bewegungserkennung/Kamera ausgebaut – Service wird nicht mehr gestartet (machte nur Probleme). */
    private void startOrStopMotionDetectionService() {
        try {
            stopService(new Intent(this, MotionDetectionService.class));
        } catch (Exception e) {
            Log.w("TurmtechnikActivity", "MotionDetectionService stop: " + e.getMessage());
        }
    }

    private static final String SCREENSAVER_URL = "http://127.0.0.1:8080/screensaver.html";

    /** Zeigt den Bildschirmschoner als Overlay (WebView mit screensaver.html – gleiche Ansicht wie Web-UI, keine Analoguhr). */
    private void showScreensaverOverlayNow() {
        Log.w("Screensaver", "showScreensaverOverlayNow aufgerufen, overlay=" + (screensaverOverlay != null));
        if (screensaverOverlay == null) return;
        screensaverShownAtMs = System.currentTimeMillis();
        applyScreensaverSystemUi(true);
        if (screensaverContentRoot != null) {
            screensaverContentRoot.bringChildToFront(screensaverOverlay);
        }
        screensaverOverlay.bringToFront();
        screensaverOverlay.setVisibility(View.VISIBLE);
        screensaverOverlay.requestLayout();
        ensureWebServerStarted(this);
        final WebView wv = screensaverOverlay.findViewById(R.id.screensaver_webview);
        if (wv != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                wv.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null);
            }
            wv.setBackgroundColor(0xFF000000);
            Runnable loadScreensaver = () -> {
                if (screensaverOverlay != null && screensaverOverlay.getVisibility() == View.VISIBLE && wv.getParent() != null) {
                    wv.loadUrl(SCREENSAVER_URL);
                }
            };
            // Mehrere Ladeversuche: WebView braucht Layout, Server ggf. Bind-Zeit
            screensaverHandler.postDelayed(loadScreensaver, 300);
            screensaverHandler.postDelayed(loadScreensaver, 1000);
            screensaverHandler.postDelayed(loadScreensaver, 3000);
            screensaverHandler.postDelayed(loadScreensaver, 6000);
        }
        setKeepScreenOn(true);
        screensaverHandler.removeCallbacks(screenOffRunnable);
        long screenOffMs = getScreenOffDelayMs();
        if (screenOffMs > 0) {
            screensaverHandler.postDelayed(screenOffRunnable, screenOffMs);
        }
        Log.i("Screensaver", "Bildschirmschoner als Overlay (Web) eingeschaltet (App bleibt aktiv)");
    }

    /** Bildschirmschoner als Overlay in dieser Activity starten – keine zweite Activity, damit die App nicht in den Hintergrund geht und vom System beendet wird. */
    private Runnable startScreensaverRunnable = new Runnable() {
        @Override
        public void run() {
            if (screensaverOverlay != null) {
                showScreensaverOverlayNow();
                return;
            }
            try {
                Intent i = new Intent(TurmtechnikActivity.this, WebUiActivity.class);
                i.putExtra(WebUiActivity.EXTRA_PATH, "screensaver.html");
                i.putExtra(WebUiActivity.EXTRA_USE_LOCALHOST, true);
                startActivity(i);
                Log.w("Screensaver", "Bildschirmschoner als WebUiActivity (Overlay war null)");
            } catch (Exception e) {
                Log.w("Screensaver", "WebUiActivity fehlgeschlagen, Fallback ScreenSaverActivity", e);
                try {
                    startActivity(new Intent(TurmtechnikActivity.this, ScreenSaverActivity.class));
                } catch (Exception e2) {
                    if (screensaverOverlay != null) {
                        screensaverShownAtMs = System.currentTimeMillis();
                        applyScreensaverSystemUi(true);
                        screensaverOverlay.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    };

    private Runnable screenOffRunnable = new Runnable() {
        @Override
        public void run() {
            setKeepScreenOn(false);
        }
    };

    private void resetScreensaverTimer() {
        screensaverHandler.removeCallbacks(startScreensaverRunnable);
        screensaverHandler.removeCallbacks(screenOffRunnable);
        long delayMs = getScreensaverDelayMs();
        Log.w("Screensaver", "resetScreensaverTimer: Schoner in " + (delayMs / 60000) + " Min (Delay " + delayMs + " ms)");
        // Handler-Fallback: auch ohne AlarmManager auslösen (AlarmManager wird auf manchen Geräten verzögert/unterdrückt)
        screensaverHandler.postDelayed(startScreensaverRunnable, delayMs);
        // AlarmManager startet WebUiActivity (Schoner) – NICHT TurmtechnikActivity, sonst läuft onCreate neu → Serial_IoThread neu → alle Relais aus
        Intent screensaverIntent = new Intent(getApplicationContext(), WebUiActivity.class);
        screensaverIntent.putExtra(WebUiActivity.EXTRA_PATH, "screensaver.html");
        screensaverIntent.putExtra(WebUiActivity.EXTRA_USE_LOCALHOST, true);
        screensaverIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), SCREENSAVER_ALARM_REQUEST_CODE,
                screensaverIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            long triggerAt = SystemClock.elapsedRealtime() + delayMs;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
            } else {
                am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
            }
            Log.w("Screensaver", "AlarmManager gesetzt, triggerAt in " + (delayMs / 1000) + " s");
        } else {
            Log.w("Screensaver", "AlarmManager null – nur Handler-Fallback aktiv");
        }
        long screenOffMs = getScreenOffDelayMs();
        if (screenOffMs > 0) {
            screensaverHandler.postDelayed(screenOffRunnable, screenOffMs);
        }
        setKeepScreenOn(true);
    }

    /** Schoner-Alarm beim Verlassen der App abbrechen (gleicher Intent wie in resetScreensaverTimer: WebUiActivity). */
    private void cancelScreensaverAlarm() {
        screensaverHandler.removeCallbacks(startScreensaverRunnable);
        Intent screensaverIntent = new Intent(getApplicationContext(), WebUiActivity.class);
        screensaverIntent.putExtra(WebUiActivity.EXTRA_PATH, "screensaver.html");
        screensaverIntent.putExtra(WebUiActivity.EXTRA_USE_LOCALHOST, true);
        screensaverIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), SCREENSAVER_ALARM_REQUEST_CODE,
                screensaverIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.cancel(pi);
        }
        Log.w("Screensaver", "Schoner-Alarm abgebrochen");
    }

    /** Verzögerung bis Bildschirm dunkel (im Bildschirmschoner) in ms. Aus Anlagen-Config (anlage_bildschirm_aus_minuten), Standard 60 Min. 0 = aus. */
    private long getScreenOffDelayMs() {
        try {
            PlatinenDatabaseHelper db = PlatinenDatabaseHelper.getInstance(this);
            String v = db != null ? db.getConfigValue("anlage_bildschirm_aus_minuten") : null;
            if (v != null && !v.trim().isEmpty()) {
                int min = Integer.parseInt(v.trim());
                if (min <= 0) return 0L;
                if (min <= 240) return min * 60 * 1000L;
            }
        } catch (Exception e) {
            // Fallback
        }
        return 60 * 60 * 1000L;
    }

    /** Verzögerung bis Bildschirmschoner (Analoguhr) in ms. Aus Anlagen-Config (anlage_bildschirmschoner_verzoegerung_minuten), Standard 5 Min. */
    private long getScreensaverDelayMs() {
        try {
            PlatinenDatabaseHelper db = PlatinenDatabaseHelper.getInstance(this);
            String v = db != null ? db.getConfigValue("anlage_bildschirmschoner_verzoegerung_minuten") : null;
            if (v != null && !v.trim().isEmpty()) {
                int min = Integer.parseInt(v.trim());
                if (min >= 1 && min <= 120) {
                    return min * 60 * 1000L;
                }
            }
        } catch (Exception e) {
            // Fallback
        }
        return SCREENSAVER_DELAY_MS_DEFAULT;
    }

    /** Wie getScreensaverDelayMs(), für WebUiActivity (statisch, mit Context). */
    public static long getScreensaverDelayMsStatic(Context context) {
        if (context == null) return SCREENSAVER_DELAY_MS_DEFAULT;
        try {
            PlatinenDatabaseHelper db = PlatinenDatabaseHelper.getInstance(context);
            String v = db != null ? db.getConfigValue("anlage_bildschirmschoner_verzoegerung_minuten") : null;
            if (v != null && !v.trim().isEmpty()) {
                int min = Integer.parseInt(v.trim());
                if (min >= 1 && min <= 120) return min * 60 * 1000L;
            }
        } catch (Exception e) { }
        return SCREENSAVER_DELAY_MS_DEFAULT;
    }

    /**
     * Beim Schoner: Navigationsleiste ausblenden und schwarz färben (Analoguhr volle Fläche / einheitlich schwarz).
     * Beim Beenden: vorherige System-UI und Leistenfarben wiederherstellen.
     */
    private void applyScreensaverSystemUi(boolean enableScreensaverMode) {
        View decorView = getWindow().getDecorView();
        Window window = getWindow();
        android.app.ActionBar actionBar = getActionBar();
        if (enableScreensaverMode) {
            screensaverPreviousSystemUiVisibility = decorView.getSystemUiVisibility();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                screensaverPreviousNavigationBarColor = window.getNavigationBarColor();
                screensaverPreviousStatusBarColor = window.getStatusBarColor();
            }
            screensaverActionBarWasVisible = (actionBar != null && actionBar.isShowing());
            if (actionBar != null) actionBar.hide();
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            final int flags = View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY : 0);
            decorView.setSystemUiVisibility(flags);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.setNavigationBarColor(Color.TRANSPARENT);
                window.setStatusBarColor(Color.TRANSPARENT);
            }
            decorView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (screensaverOverlay != null && screensaverOverlay.getVisibility() == View.VISIBLE) {
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                        getWindow().getDecorView().setSystemUiVisibility(flags);
                    }
                }
            }, 100);
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            if (actionBar != null && screensaverActionBarWasVisible) actionBar.show();
            decorView.setSystemUiVisibility(screensaverPreviousSystemUiVisibility);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.setNavigationBarColor(screensaverPreviousNavigationBarColor);
                window.setStatusBarColor(screensaverPreviousStatusBarColor);
            }
        }
    }

    /** Holt das Hauptlayout (Layoutseite 1) wieder in den Vordergrund, nachdem der Schoner ausgeblendet wurde. */
    private void bringMainContentToFront() {
        if (screensaverContentRoot == null) return;
        if (screensaverContentRoot.getChildCount() < 2) return;
        View mainContent = screensaverContentRoot.getChildAt(0);
        if (mainContent != null) {
            mainContent.bringToFront();
            mainContent.setVisibility(View.VISIBLE);
            screensaverContentRoot.requestLayout();
            screensaverContentRoot.invalidate();
        }
    }

    public static Boolean[] globalOn = new Boolean[RELAIS_COUNT];
    public static Boolean[] verknuepfteTastenOn = new Boolean[RELAIS_COUNT];
    /** Lock für Lese-/Schreibzugriffe auf verknuepfteTastenOn, damit UhrThread („nächstes Programm“) immer aktuelle Werte sieht. */
    public static final Object VERKNUEPFTE_TASTEN_LOCK = new Object();

    public static String benutzerMelodienFileString;
    public static String normalprogrammFileString;
    public static String nebenUhrFileString;
    public static String schlagwerkZeitenFileString;
    public static String bluetoothConfigFileString;

    // protected static String festeFesttageFileString ;
    protected static String newFesteFesttageFileString;
    protected static final int FESTE_FESTTAGE_SHEET_NUMBER = 13;
    // protected static String variableFesttageFileString ;
    protected static String newVariableFesttageFileString;
    protected static final int VARIABLE_FESTTAGE_SHEET_NUMBER = 12;

    private File checkfile;

    public static Vector<String> normalProgrammblockFileNamen = new Vector<String>();
    public static Vector<String> festeFesttageTagtypFileNamen = new Vector<String>();
    public static Vector<String> variableFesttageTagtypFileNamen = new Vector<String>();

    private boolean doRun = true; // fuer text anzeige handler thread
    private boolean doRunInternetThread = false;

    private PowerManager.WakeLock wl = null;

    private boolean file_ok;

    private boolean carambola_io_ok;

    private String sdCardFilename;

    private int automatikTasteIndex;
    private int schlagwerkTasteIndex;


    private Calendar calendar;

    private Intent launchIntent;

    private Integer countInfo;
    private Message msg;
    private boolean changeText;

    public static HeizungManualThread heizungManualThread;

    private static ArrayList<Integer> globalOffOhneMelodieIndex = new ArrayList<Integer>();
    private static ArrayList<Integer> globalOffOhneMelodieRelaisNumber = new ArrayList<Integer>();

    static int clrAll = 0;
    // private BroadcastReceiver WifiStateChangedReceiver;

    @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isInForeground = true; // Sofort setzen, damit StartTurmtechnikService keine zweite Activity startet (onCreate kann lange dauern)
        StartTurmtechnikService.touchHeartbeat(); // Sofort, ohne Intent – Service-Thread sieht es sofort (Intent kommt erst nach onCreate)
        try {
            Intent hb = new Intent(this, StartTurmtechnikService.class);
            hb.setAction(StartTurmtechnikService.ACTION_HEARTBEAT);
            startService(hb);
        } catch (Exception e) {
            android.util.Log.w("Screensaver", "Heartbeat in onCreate: " + (e != null ? e.getMessage() : ""));
        }

        StaticVariable.serialNumber = Build.SERIAL;

        onCreateFlag = true;
        activityJustCreated = true;

        Log.e("onCreate", "durchlaufen");

        turmtechnikContext = getApplicationContext();
        turmtechnikActivityInstance = this;

        context = this;

        // „App beenden“ oder Passwort 5644: Tablet soll normal laufen. Wenn Nutzer uns gezielt aus dem Launcher startet → Phase beenden und App normal starten.
        boolean exitForSettingsActive = getExitForSettingsUntilMillis(this) > System.currentTimeMillis();
        boolean background5644Active = getBackgroundAllowedUntilMillis(this) > System.currentTimeMillis();
        boolean startedFromLauncher = getIntent() != null && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER);

        if (exitForSettingsActive || background5644Active) {
            if (startedFromLauncher) {
                // Nutzer hat Turmtechnik gezielt geöffnet → Phasen beenden, App normal laufen lassen
                setExitForSettingsUntilMillis(this, 0);
                getSharedPreferences("Turmtechnik", 0).edit().putLong(PREF_BACKGROUND_ALLOWED_UNTIL_MILLIS, 0).commit();
            } else {
                // Automatischer Start (z. B. HOME-Taste) oder Alarm → nicht übernehmen, Launcher/Einstellungen zeigen damit Tablet normal nutzbar ist
                launchTabletNormalAndFinish();
                return;
            }
        }

        // Einschalt-Schwelle = Ausschalt + 1 %, damit kein Dauerstart bis Akku 1 % unter Ausschalt
        // batteryLevel wird in isPowerConnected() gesetzt; 0 = oft „noch nicht gelesen“ nach Installation → Start erlauben
        boolean powerOrBattOk = (isPowerConnected(getBaseContext()) ||
                batteryLevel >= StaticVariable.getBattEinschaltLevel() || batteryLevel == 0);

        if (!powerOrBattOk) {
            finish();
        }


        // Unbehandelte Java-Exceptions loggen (hilft bei nativen Crashes: oft geht ein Java-Fehler voraus)
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            String exErrorString = ex != null ? ex.toString() : "null";
            Log.e("Turmtechnik", "UncaughtException in " + (thread != null ? thread.getName() : "?") + ": " + exErrorString, ex);
            if (turmtechnikContext != null) {
                try {
                    new LogExcelError(-1, -1, "Android Error: ", -1, exErrorString, -1);
                    LogTurmtechnik2.appendCrashLog("Fehler (Exception): " + exErrorString);
                } catch (Throwable ignored) { }
            }
            if (defaultHandler != null) defaultHandler.uncaughtException(thread, ex);
        });

        //StaticVariable.sendUserProgrammRemote = true ;

        // setContentView(R.layout.text_layout);

        //killFashionClock();

        // startet Turmtechnik periodisch
        if (!StartTurmtechnikService.isServiceCreated()) {
            startService(new Intent(this, StartTurmtechnikService.class));
            Log.e("START", "Service NEU gestartet");
            savePowerOffWithPassword(false);
            StaticVariable.sendUserProgrammRemote2 = true;
        } else {
            Log.e("SERVCE", "laeuft schon!");
            //StaticVariable.sendUserProgrammRemote2 = false ;
            // schaltet sich selber ab!
        }

        if (!crashLogNeustartAlreadyLoggedThisProcess) {
            LogTurmtechnik2.appendCrashLog("Neustart");
            crashLogNeustartAlreadyLoggedThisProcess = true;
        }

        // service vor allem starten
        // wegen timeout der app (warten/beenden)

        setContentView(R.layout.text_layout);


        DisplayMetrics DISPLAY_METRICS = this.getResources().getDisplayMetrics();

        usableWith = DISPLAY_METRICS.widthPixels;
        usableHeight = DISPLAY_METRICS.heightPixels - (getStatusBarHeight() * 2);
        outputView = (TextView) this.findViewById(R.id.textView1);

        parentLayout = (LinearLayout) this.findViewById(R.id.main_layout);

        // Bildschirmschoner als Overlay mit WebView (screensaver.html) – gleiche Ansicht wie Web-UI, keine Analoguhr, App bleibt aktiv
        ViewGroup contentRoot = (ViewGroup) findViewById(android.R.id.content);
        if (contentRoot != null) {
            screensaverOverlay = LayoutInflater.from(this).inflate(R.layout.activity_screensaver_web, contentRoot, false);
            screensaverOverlay.setVisibility(View.GONE);
            screensaverOverlay.setClickable(true);
            WebView screensaverWebView = screensaverOverlay.findViewById(R.id.screensaver_webview);
            if (screensaverWebView != null) {
                WebSettings ws = screensaverWebView.getSettings();
                ws.setJavaScriptEnabled(true);
                ws.setDomStorageEnabled(true);
                final int[] screensaverLoadRetries = { 0 };
                screensaverWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        String url = request != null && request.getUrl() != null ? request.getUrl().toString() : null;
                        if (url != null && url.startsWith("turmt://layout1")) {
                            runOnUiThread(() -> dismissScreensaverOverlay());
                            return true;
                        }
                        return false;
                    }
                    @Override
                    @SuppressWarnings("deprecation")
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        if (url != null && url.startsWith("turmt://layout1")) {
                            runOnUiThread(() -> dismissScreensaverOverlay());
                            return true;
                        }
                        return false;
                    }
                    @Override
                    @SuppressWarnings("deprecation")
                    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                        if (failingUrl != null && failingUrl.contains("screensaver") && screensaverLoadRetries[0] < 3 && screensaverOverlay != null && screensaverOverlay.getVisibility() == View.VISIBLE) {
                            screensaverLoadRetries[0]++;
                            screensaverHandler.postDelayed(() -> view.loadUrl(SCREENSAVER_URL), 1500);
                        }
                    }
                });
                // URL wird erst in showScreensaverOverlayNow() geladen, wenn Overlay sichtbar ist und Server bereit
            }
            screensaverOverlay.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (System.currentTimeMillis() - screensaverShownAtMs >= SCREENSAVER_TOUCH_GRACE_MS) {
                        dismissScreensaverOverlay();
                    }
                }
            });
            screensaverContentRoot = contentRoot;
            contentRoot.addView(screensaverOverlay, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            Log.w("Screensaver", "Bildschirmschoner-Overlay eingehängt, starte Timer");
            resetScreensaverTimer(); // Timer sofort starten (onResume setzt ihn erneut zurück)
            if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_SHOW_SCREENSAVER_OVERLAY, false)) {
                getIntent().removeExtra(EXTRA_SHOW_SCREENSAVER_OVERLAY);
                activityStartedForScreensaver = true;
                // Schoner in WebUiActivity anzeigen statt Overlay (vermeidet Crash bei Alarm-Start aus Hintergrund)
                Log.w("Screensaver", "onCreate: Start mit Schoner-Extra → öffne WebUiActivity screensaver.html");
                ensureWebServerStarted(this);
                try {
                    Intent i = new Intent(this, WebUiActivity.class);
                    i.putExtra(WebUiActivity.EXTRA_PATH, "screensaver.html");
                    i.putExtra(WebUiActivity.EXTRA_USE_LOCALHOST, true);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                } catch (Exception e) {
                    Log.w("Screensaver", "WebUiActivity starten fehlgeschlagen, Fallback Overlay in 800 ms", e);
                    screensaverHandler.postDelayed(this::showScreensaverOverlayNow, 800);
                }
            }
        } else {
            Log.w("Screensaver", "contentRoot null, Bildschirmschoner nicht initialisiert");
        }

        if (!activityStartedForScreensaver) {
            infoToast("APP Start");
        }

        // Web-Server immer starten (wichtig bei fehlenden Beschriftungstasten-Daten: Nutzer kann per Web-UI konfigurieren, bevor Absturz)
        startConfigWebServer();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);

        layoutParams = getWindow().getAttributes();


        // den lock screen automatisch ausschalten
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        // Ab API 27: Beim Aufwecken App über Sperrbildschirm anzeigen, Bildschirm einschalten – dann reicht E-Taste/Aufwecken für Layout
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }



        // Automatik-Status und verknüpfte Tasten einmal beim Start laden (nicht in der Schleife)
        loadVerknuepfteTasten_and_automatic_taste();
        
        for (int i = 0; i < RELAIS_COUNT; i++) {
            globalOn[i] = false; // ??!! stuerzt sonnst ab??
        }

        getDisplayParameter();

        loadVersionString();

        if (StaticConstants.DEBUG) {
            // Log.i("NACH" , "getDisplayParameter") ;
        }

        Log.i("wait", "to SD card");
        requestStoragePermissionsIfNeeded();
        requestBluetoothConnectPermissionIfNeeded();
        waitToSDcard();

        sdCardPath = (Environment.getExternalStorageDirectory().getPath());
        
        // Prüfe beim Start, ob alle Tagtypen Programme in der Datenbank haben
        // Wenn nicht, werden sie automatisch aus Excel migriert
        try {
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
            dbHelper.ensureAllTagtypenHaveProgramme();
        } catch (Exception e) {
            Log.e("TurmtechnikActivity", "Fehler bei Prüfung der Tagtypen-Programme", e);
        }


        // startet Turmtechnik periodisch
        if (!StartTurmtechnikService.isServiceCreated()) {
            startService(new Intent(this, StartTurmtechnikService.class));
            Log.e("START", "Service NEU gestartet");
            StaticVariable.sendUserProgrammRemote2 = true;
        } else {
            Log.e("SERVCE", "laeuft schon!");
            //StaticVariable.sendUserProgrammRemote2 = false ;
            // schaltet sich selber ab!
        }


        getFernsteuernSpeicherOrt();

        StaticVariable.makeBeschriftungTasten = false;

        String beschriftungTastenString = sdCardPath + "/Turmtechnik/Config/Beschriftung-Tasten.xls";


        checkfile = new File(beschriftungTastenString);
        if (!checkfile.exists()) {
            // Nur Datenbank: Excel-Datei optional. Wenn DB Tasten hat, reicht das. Sonst beim Start Import-Dialog anbieten.
            if (hasNoBeschriftungTasten()) {
                file_ok = true; // App startet trotzdem; Tasten-Import-Dialog wird in der Startabfrage angezeigt
            } else {
                StaticVariable.makeBeschriftungTasten = false;
                file_ok = true;
            }
            if (StaticVariable.makeBeschriftungTasten == true) {
                StaticVariable.makeBeschriftungTasten = false;
                StaticVariable.sendUserProgrammRemote2 = true;
            }
        } else {
            if (StaticVariable.makeBeschriftungTasten == true) {
                StaticVariable.makeBeschriftungTasten = false;
                StaticVariable.sendUserProgrammRemote2 = true;
            }
            file_ok = true;
        }

        // outputView = (TextView) this.findViewById(R.id.textView1);
        // printInfo("\n\n");

        // ExcelRead checkfile = new ExcelRead() ;

        // String beschriftungGlocken = "/Turmtechnik/Beschriftung Glocken.xls";

        // String beschriftungGlocken =
        // StaticConstants.beschriftungGlockenString ;
        // ab 7.6.13 ist das in System.xls sheet 10
        excell_system = StaticConstants.excellSystemString;

        // inputFileString =
        // (Environment.getExternalStorageDirectory().getPath()+beschriftungGlocken);
        // neu wegen System.xls multisheet 7.6.13
        systemFileString = (Environment.getExternalStorageDirectory().getPath() + excell_system);
        beschriftungTastenFileString = (Environment.getExternalStorageDirectory().getPath() +
                "/Turmtechnik/Config/Beschriftung-Tasten.xls");

        ///// !!!!!!! neu 28.08.2014

        //checkFilesAndInit();  // wir dann wegen ANR im UI Thread ausgefuehrt

        ///// *******

        // Fashion Clock entfernt – Bildschirmschoner ist Web-UI

        // initThread(); // das blockiert lande den bildschirm

        // Datenbank-Konfiguration prüfen (keine Excel-Abfrage). Fehlendes auflisten, „Weiter“ startet immer.
        if (hasAnyMissingConfig()) {
            ensureWebServerStarted(this);
            showConfigCheckDialog();
        } else {
            startAppAfterConfigCheck();
        }


        //handler = new Handler();
        //startUpdateInfoText();

        System.gc();
    } // ende von onCreate

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private void savePowerOffWithPassword(boolean flag) {
        SharedPreferences pref = getSharedPreferences("Turmtechnik", 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("powerOffPassword", flag);
        editor.commit();
        Log.e("powerOffPassword", "=" + flag);
    }

    /** Passwort 5644 = Beenden mit 15 Minuten Hintergrund (App darf 15 Min im Hintergrund bleiben, danach wieder in den Vordergrund). */
    private static final String PASSWORD_EXIT_15MIN_BACKGROUND = "5644";
    private static final String PREF_BACKGROUND_ALLOWED_UNTIL_MILLIS = "background_allowed_until_millis";
    /** Passwort 5644: Minuten im Hintergrund erlaubt. */
    private static final int BACKGROUND_ALLOWED_MINUTES = 15;

    /** „App beenden für Einstellungen“: Bis zu diesem Zeitpunkt (ms) startet die App nicht wieder (Service/Alarm/Boot); bei erneutem Start (z. B. HOME) → Einstellungen öffnen. */
    private static final String PREF_EXIT_FOR_SETTINGS_UNTIL_MILLIS = "exit_for_settings_until_millis";

    /** Liefert bis wann (Timestamp ms) „App beenden für Einstellungen“ aktiv ist. 0 = inaktiv. */
    public static long getExitForSettingsUntilMillis(android.content.Context context) {
        if (context == null) return 0;
        return context.getSharedPreferences("Turmtechnik", 0).getLong(PREF_EXIT_FOR_SETTINGS_UNTIL_MILLIS, 0);
    }

    private static void setExitForSettingsUntilMillis(android.content.Context context, long untilMillis) {
        if (context == null) return;
        context.getSharedPreferences("Turmtechnik", 0).edit().putLong(PREF_EXIT_FOR_SETTINGS_UNTIL_MILLIS, untilMillis).commit();
    }

    /** Liefert bis wann (Timestamp in ms) die App im Hintergrund bleiben darf. 0 = keine Einschränkung (immer in den Vordergrund). */
    public static long getBackgroundAllowedUntilMillis(android.content.Context context) {
        if (context == null) return 0;
        return context.getSharedPreferences("Turmtechnik", 0).getLong(PREF_BACKGROUND_ALLOWED_UNTIL_MILLIS, 0);
    }

    private void saveBackgroundAllowedUntilMillis(long untilMillis) {
        getSharedPreferences("Turmtechnik", 0).edit().putLong(PREF_BACKGROUND_ALLOWED_UNTIL_MILLIS, untilMillis).commit();
        Log.e("beenden", "Hintergrund erlaubt bis " + untilMillis + " (15 Min)");
    }

    /**
     * Zeigt den System-Launcher oder Einstellungen und beendet diese Activity, damit das Tablet normal genutzt werden kann
     * (nach „App beenden“ oder Passwort 5644). Wird aufgerufen, wenn die App z. B. per HOME-Taste wieder in den Vordergrund geholt wird.
     */
    private void launchTabletNormalAndFinish() {
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            List<ResolveInfo> homes = getPackageManager().queryIntentActivities(home, 0);
            String ourPackage = getPackageName();
            for (ResolveInfo ri : homes) {
                if (ri.activityInfo != null && !ourPackage.equals(ri.activityInfo.packageName)) {
                    Intent launcher = new Intent(Intent.ACTION_MAIN);
                    launcher.addCategory(Intent.CATEGORY_HOME);
                    launcher.setPackage(ri.activityInfo.packageName);
                    launcher.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(launcher);
                    finish();
                    return;
                }
            }
        } catch (Exception e) {
            Log.w("TurmtechnikActivity", "Anderen Launcher starten: " + (e != null ? e.getMessage() : ""));
        }
        startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }

    /** Prüft, ob keine Tastenkonfiguration in der DB ist (Beschriftung Tasten leer). */
    private boolean hasNoBeschriftungTasten() {
        if (turmtechnikContext == null) return true;
        try {
            java.util.List<PlatinenDatabaseHelper.BeschriftungTastenRow> rows = PlatinenDatabaseHelper.getInstance(turmtechnikContext).getBeschriftungTasten();
            return rows == null || rows.isEmpty();
        } catch (Exception e) {
            return true;
        }
    }

    /** Prüft anhand der Datenbank, welche Konfiguration fehlt. Keine Excel-Abfragen. */
    private java.util.List<String> getMissingConfigFromDb() {
        java.util.List<String> missing = new java.util.ArrayList<>();
        if (turmtechnikContext == null) {
            missing.add("Datenbank nicht bereit");
            return missing;
        }
        try {
            PlatinenDatabaseHelper db = PlatinenDatabaseHelper.getInstance(turmtechnikContext);
            if (hasNoBeschriftungTasten()) missing.add("Beschriftung Tasten");
            if (hasNoProgrammeForNormalprogramm()) missing.add("Programme (Normalprogramm)");
            java.util.List<PlatinenDatabaseHelper.Tagtyp> tagtypen = db.getAllTagtypen();
            if (tagtypen == null || tagtypen.isEmpty()) missing.add("Tagtypen");
            java.util.List<Platine> platinen = db.getAllPlatinen();
            if (platinen == null || platinen.isEmpty()) missing.add("Platinen-Konfiguration");
            java.util.List<PlatinenDatabaseHelper.Melodie> melodien = db.getAllMelodien();
            if (melodien == null || melodien.isEmpty()) missing.add("Melodien");
        } catch (Exception e) {
            Log.e("TurmtechnikActivity", "Fehler bei Konfigurationsprüfung", e);
            missing.add("Fehler beim Prüfen: " + (e.getMessage() != null ? e.getMessage() : ""));
        }
        return missing;
    }

    /** Gibt true zurück, wenn mindestens ein Konfigurationseintrag fehlt. */
    private boolean hasAnyMissingConfig() {
        return !getMissingConfigFromDb().isEmpty();
    }

    /** Dialog: Fehlende Konfiguration aus DB auflisten, mit „Weiter“ trotzdem starten. Keine Excel-Abfrage. */
    private void showConfigCheckDialog() {
        try {
            if (isFinishing()) return;
            ensureWebServerStarted(this);
            String ipVal = getWebServerIpAddress();
            if (ipVal == null || ipVal.isEmpty()) {
                try { ipVal = getLocalIpAddress(); } catch (Exception e) { Log.w("ConfigCheck", "IP", e); }
            }
            final String ip = ipVal;
            java.util.List<String> missingList = getMissingConfigFromDb();
            StringBuilder sb = new StringBuilder();
            if (missingList.isEmpty()) {
                sb.append("Alle geprüften Konfigurationen sind vorhanden.");
            } else {
                sb.append("Fehlende oder leere Konfiguration (Datenbank):\n\n");
                for (int i = 0; i < missingList.size(); i++) {
                    if (i > 0) sb.append("\n");
                    sb.append("• ").append(missingList.get(i));
                }
                sb.append("\n\nKonfiguration über Web-UI (Server) oder „Sicherung laden“ ergänzen. Mit „Weiter“ trotzdem starten.");
            }
            final String msg = sb.toString();
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setTitle("Konfiguration prüfen");
            b.setMessage(msg);
            b.setPositiveButton("Weiter", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    startAppAfterConfigCheck();
                }
            });
            b.setNeutralButton((ip != null && !ip.isEmpty()) ? ("Server: " + ip + ":8080") : "Web-UI", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String u = (ip != null && !ip.isEmpty()) ? ("http://" + ip + ":8080") : null;
                    if (u != null) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(u));
                            android.content.pm.ResolveInfo resolve = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
                            if (resolve != null && resolve.activityInfo != null && resolve.activityInfo.exported) {
                                intent.setComponent(new ComponentName(resolve.activityInfo.packageName, resolve.activityInfo.name));
                            }
                            startActivity(intent);
                        } catch (Exception e) { Log.e("ConfigCheck", "Browser", e); }
                    }
                    dialog.dismiss();
                    showConfigCheckDialog();
                }
            });
            b.setNegativeButton("Sicherung laden", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    showAnlageImportFilePicker();
                    showConfigCheckDialog();
                }
            });
            b.setCancelable(false);
            b.show();
        } catch (Exception e) {
            Log.e("ConfigCheck", "Dialog", e);
            startAppAfterConfigCheck();
        }
    }

    /** Nach Konfigurationsprüfung: App starten (keine Excel-Abfrage mehr). */
    private void startAppAfterConfigCheck() {
        new LongOperation().execute("");
    }

    /** Prüft, ob für Normalprogramm keine Programme in der DB sind (z. B. nach fehlgeschlagener Migration). */
    private boolean hasNoProgrammeForNormalprogramm() {
        if (turmtechnikContext == null) return false;
        try {
            java.util.List<Programm> list = PlatinenDatabaseHelper.getInstance(turmtechnikContext).getProgrammeByTagtyp("Normalprogramm");
            return list == null || list.isEmpty();
        } catch (Exception e) {
            return true;
        }
    }

    public void errorDialog(String errorString) {
        Log.e("ERROR", "Dialog aufgerufen");
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(TurmtechnikActivity.this);

        alertDialogBuilder.setTitle("ERROR");
        alertDialogBuilder.setMessage(errorString);


        alertDialogBuilder.setPositiveButton
                ("O.K.",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int wich) {
                                dialog.dismiss();
                            }
                        }
                );

        AlertDialog alertDialog = alertDialogBuilder.create();
        Log.e("alertDialog", "show aufgerufen");
        alertDialog.show();
    }

    public boolean isPowerConnected(Context context) {
        if (context == null) return true;
        Intent intent = null;
        try {
            intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        } catch (Exception e) {
            Log.w("Turmtechnik", "Battery-Intent nicht verfügbar: " + (e != null ? e.getMessage() : ""), e);
        }
        if (intent == null) return true; // Nach Installation/Emulator oft null – Start erlauben
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        return plugged == BatteryManager.BATTERY_PLUGGED_AC
                || plugged == BatteryManager.BATTERY_PLUGGED_USB;
    }

    /** Nicht mehr verwendet – Fashion Clock entfernt, Bildschirmschoner ist Web-UI. */
    private void killFashionClock() { }

    private void infoToast(String infoText) {

        //Toast toast = new Toast(getApplicationContext()) ;
        //toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
        //toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        //toast.setDuration(Toast.LENGTH_LONG);

        SpannableStringBuilder biggerText = new SpannableStringBuilder(infoText);
        biggerText.setSpan(new RelativeSizeSpan(2.35f), 0, infoText.length(), 0);

        //toast.setText(biggerText);
        //toast.setText(infoText);


        //ooper.prepare();
        Toast.makeText(context, biggerText, Toast.LENGTH_LONG).show();


    }



    private class LongOperation extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            // Hintergrund-Thread: kein Looper.prepare() – führt zu "Only one Looper may be created per thread"

            if (params != null && params.length > 0 && "excel_import".equals(params[0])) {
                try {
                    PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(turmtechnikContext);
                    dbHelper.runFullExcelImport();
                } catch (Exception e) {
                    Log.e("LongOperation", "Fehler beim Excel-Import", e);
                }
            }

            Log.e("VOR", "checkFilesAndInit");
            checkFilesAndInit();
            Log.e("NACH", "checkFilesAndInit");


            // machen: files extra pruefen!!! 3.12.2014
            // also:
            //filesCheck() ; // schreiben nur ueber handler!!!!

            // file_ok = true ;  // muss von filesCheck kommen

            if (file_ok) {
                //layout = new Seite1Layout(beschriftungTastenFileString, getApplicationContext()) ;

                //layoutCreated = (layout.initLayout()) ;


                // Starte TimeSyncThread (verwaltet selbst, dass nur ein Thread läuft)
                TimeSyncThread.startInstance(context);
                Log.e("serialGPS", "onOff=" + StaticVariable.serialGPS_OnOff);
                Log.e("serialIP", "=" + StaticVariable.serialGPS_IP);
                Log.e("serialPort", "=" + StaticVariable.serialGPS_port);
                if (StaticVariable.serialGPS_OnOff) {
                    Log.e("serialGPS", "O.K.");
                    if (nmea_gps_clock != null) {
                        nmea_gps_clock.endNmeaThread();
                    }
                    nmea_gps_clock = new NMEA_gps_clock();
                    nmea_gps_clock.startClientThread();
                }

                StaticVariable.heizungRelaisNumberManual2 = TagesSuche.getRelaisNumber("Heizung"); //
                //Log.e("Heizung Relais gesucht" , "=" + StaticVariable.heizungRelaisNumberManual) ;

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            if (isFinishing()) return;
                            if (outputView != null) outputView.setText("");
                            if (activityStartedForScreensaver) {
                                return;
                            }
                            // Nach Bewegung (woke_by_motion): nur natives Layout anzeigen, keine Web-UI (vermeidet schwarzen Bildschirm)
                            boolean wokeByMotion = getIntent() != null && getIntent().getBooleanExtra("woke_by_motion", false);
                            if (wokeByMotion) {
                                if (layoutCreated != null && parentLayout != null) {
                                    parentLayout.addView(layoutCreated);
                                }
                            } else {
                                PlatinenDatabaseHelper db = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.this);
                                String wv = db != null ? db.getConfigValue(CONFIG_WEB_UI_VOLLBILD_TEST) : null;
                                boolean webUiVollbildTest = wv == null || !"0".equals(wv.trim());
                                // Web-UI anzeigen wenn gewünscht oder wenn Layout nicht gebaut wurde (damit nach Init immer etwas kommt)
                                if (webUiVollbildTest || layoutCreated == null) {
                                    ensureWebServerStarted(TurmtechnikActivity.this);
                                    openWebUiInBrowser(TurmtechnikActivity.this, "/app-seite1.html");
                                } else if (parentLayout != null) {
                                    parentLayout.addView(layoutCreated);
                                }
                            }
                            handler = new Handler();
                            // Schwere Arbeit nach dem nächsten Frame ausführen, damit Layout 1 nicht einfriert (Skipped N frames)
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if (isFinishing()) return;
                                        allMelodieRelaisOffVorbereiten2();
                                        startUpdateInfoText();
                                    } catch (Exception e) {
                                        Log.e("TurmtechnikActivity", "Fehler nach Layout-Anzeige", e);
                                    }
                                }
                            }, 100);
                        } catch (Exception e) {
                            Log.e("TurmtechnikActivity", "Fehler beim Anzeigen des Layouts", e);
                        }
                    }
                });
            }

            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            Log.e("postExecute", "result=" + result);


            //if(file_ok==true)
            //{
            //	parentLayout.addView(layoutCreated);

            //	handler = new Handler();

            //    startUpdateInfoText();

            //    if(StaticVariable.timeServerEinAus.equals("EIN"))
            //    {
            //    	SntpThread sntpThread = new SntpThread(context) ;
            //    	sntpThread.start();
            //startActivity(new Intent(android.provider.Settings.ACTION_DATE_SETTINGS)) ;
            //   }
            //}
        }


        // ab 3.12.2014 veraltet, blockiert den Bildschirm
        private void initThread() {
            new Thread() {
                @Override
                public void run() {
                    try {
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                // TODO Auto-generated method stub
                                // wegen ANR Dialog max 5 Sekunden init in thread geben

                                //try {
                                //	Thread.sleep(30000);
                                //} catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                //	e.printStackTrace();
                                //}

                                checkFilesAndInit();  // wir wegen ANR im UI Thread ausgefuehrt

                                Log.e("F I L E", "O.K. 3 = " + file_ok + " no start infoText");

                                if (file_ok == true) {
                                    handler = new Handler();

                                    startUpdateInfoText();



                                    // Starte TimeSyncThread (verwaltet selbst, dass nur ein Thread läuft)
                                    TimeSyncThread.startInstance(context);
                                }
                            }
                        });
                    } catch (final Exception ex) {

                    }
                }
            }.start();
        }
    } // ende von long operation

    private void setKeepScreenOn(boolean keepScreenOn) {
        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }


    private void initNebenUhr() {
        int minutenTemp = getMinuten();
        StaticVariable.uhrA_angezeigteZeit = minutenTemp;
        StaticVariable.uhrA_calendarZeit = minutenTemp;
        StaticVariable.uhrB_angezeigteZeit = minutenTemp;
        StaticVariable.uhrB_calendarZeit = minutenTemp;
        StaticVariable.uhrC_angezeigteZeit = minutenTemp;
        StaticVariable.uhrC_calendarZeit = minutenTemp;
    }

    private int getMinuten() {
        calendar = Calendar.getInstance();
        calendar.get(Calendar.MINUTE);

        int minutenTemp = (calendar.get(Calendar.HOUR) * 60)
                + calendar.get(Calendar.MINUTE);

        calendar = null;
        return minutenTemp;
    }


    // das braucht einen thread in main thread ... wegen Aenderungen in der View
    public void startUpdateInfoText() {
        Runnable runnable = new Runnable() {
            //runOnUiThread(new Runnable() {


            public int datumText = 0;
            // private int powerOffCounter = 0 ;
            private int wifiOnCounter = 0;

            private int infoTextWait = 0;
            /** Zähler für „langsame“ Prüfungen: nur alle ~1 s ausführen, um unnötige Abfragen zu vermeiden. */
            private int slowTick = 0;

            @Override
            public void run() {
                while (doRun) {
                    try {
                        infoTextWait++;
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    final boolean runSlowChecks = (++slowTick >= 20);
                    if (runSlowChecks) {
                        slowTick = 0;
                    }
                    handler.post(new Runnable() {
                        private int MINIMUM_;

                        @Override
                        public void run() {
                            // TODO Auto-generated method stub

                            // Schwere/teure Prüfungen nur alle ~1 s (20 * 50 ms), nicht alle 50 ms
                            if (runSlowChecks) {
                                if (StaticVariable.errorCountToReboot > 10) {
                                    if (StaticVariable.flagRebootOk) {
                                        if (isPowerConnected(context)) {
                                            beendenUndReboot();
                                        }
                                    } else {
                                        StaticVariable.errorCountToReboot = 0;
                                    }
                                }

                                if (StaticVariable.serialGPS_OnOff && (StaticVariable.nmeaGpsNoData > 20)) {
                                    StaticVariable.nmeaGpsNoData = 0;
                                    StaticVariable.timeServerOk = false;
                                    if (nmea_gps_clock != null) {
                                        nmea_gps_clock.endNmeaThread();
                                        try {
                                            Thread.sleep(500);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    Log.e("serialGPS", "ERROR --> neustart");
                                    nmea_gps_clock = new NMEA_gps_clock();
                                    nmea_gps_clock.startClientThread();
                                }

                                checkReboot();

                                // Netzwerk-Check nur alle ~1 s (Ergebnis wird derzeit nicht genutzt)
                                ConnectivityManager cm = (ConnectivityManager) getBaseContext()
                                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                                NetworkInfo netInfo = cm.getActiveNetworkInfo();
                                if (netInfo != null && netInfo.isConnected()) {
                                    // O.K. Internetverbindung vorhanden
                                } else {
                                    // Log.i("WARTE" , "AUF'S INTERNET...") ;
                                }
                            }

                            if (StaticVariable.saveBenutzerProgrammSofort == true) {
                                saveBenutzerTastenAndDateAndTime();
                                StaticVariable.saveBenutzerProgrammSofort = false;
                            }

                            if (StaticVariable.minutenTakt > 0) {
                                StaticVariable.minutenTakt = 0;
                                saveNebenuhr();   // 7.2.14 alle Minuten die nebenuhr sichern

                            }

                            if (StaticVariable.bigClockTimeout > 0) {
                                StaticVariable.bigClockTimeout--; // alle 50 ms decrement
                                //Log.e("decrement" , "bigClockTimeout") ;
                                //Log.e("nach decrement" , "=" + StaticVariable.bigClockTimeout) ;

                                //	if (bigClocktimeOut % ( 20 ) == 0)
                                //	{
                                //		Log.e("bigClock" , "= " + bigClocktimeOut ) ;
                                //	}

                                if (StaticVariable.bigClockTimeout == 0) {
                                    // Fashion Clock entfernt – Bildschirmschoner ist Web-UI (screensaver.html)
                                }
                            }

                            datumText++;
                            if ((datumText & 01) == 0) // einmal Datum einmal
                            // Text anzeigen
                            {
                                if (StaticVariable.infoToastText.length() > 0) {
                                    infoToast(StaticVariable.infoToastText);
                                    StaticVariable.infoToastText = "";
                                }

                                // Sofort Anzeige erzwingen, wenn Verknüpfte-Taste umgeschaltet wurde
                                if (StaticVariable.refreshInfoTextVerknuepfteTaste || StaticVariable.infoTextRefreshCount > 0) {
                                    infoTextWait = 11; // erzwingt Update im nächsten Durchlauf
                                    if (StaticVariable.infoTextRefreshCount > 0) {
                                        StaticVariable.infoTextRefreshCount--;
                                    }
                                }
                                // Aktualisierung alle ~0,5 s (statt 2 s), damit „nächstes Programm“ nach Verknüpfte-Taste schnell wechselt
                                if (infoTextWait > 10) {
                                    infoTextWait = 0;


                                    if (layout != null) {
                                        //if ((UhrThread.blockReady != 0) || (!Serial_IoThread.getSerialIoStatus()))
                                        if ((UhrThread.blockReady != 0) || (! zehnmalErrorIoStatusOk()))
                                        {
                                            layout.infoTextRed();

                                        } else {
                                            //if (UhrThread.getNormalProgrammFlag())
                                            //{
                                            layout.infoTextBlack();
                                            //}
                                            //else
                                            //{
                                            //	layout.infoTextGreen();
                                            //}
                                        }

                                        if (!(androidErrorText.equals(""))) {
                                            layout.infoTextRed();
                                            layout.infoText.setText(androidErrorText);
                                            // Meteor Code entfernt
                                        } else {
                                            if (flagAutomaticOnOff == false) {
                                                //StaticVariable.stringInfoTextField[0] = "Automatik ausgeschaltet";
                                                StaticVariable.stringInfoTextField[0] = StaticVariable.getUebersetzung(0) ;
                                            }

                                            //if (StaticVariable.serial_io_status == true) {

                                            // Meteor Code entfernt
                                            //} else {
                                            //    layout.infoTextRed();
                                            //    String achtungVerbindung = "Achtung keine Verbindung zu den Schaltrelais!";
                                            //    StaticVariable.stringInfoTextField[1] = (achtungVerbindung);
                                            //}

                                            StaticVariable.stringInfoTextField[2] = StaticVariable.infoStringHeizung;

                                            incrementInfoTextIndex();
                                            while (StaticVariable.stringInfoTextField[StaticVariable.infoTextIndex].equals("")) {
                                                incrementInfoTextIndex();
                                            }

                                            layout.infoText.setText("[" + (StaticVariable.infoTextIndex + 1) + "] " + StaticVariable.stringInfoTextField[StaticVariable.infoTextIndex]);
                                            layout.infoText.setText(StaticVariable.stringInfoTextField[StaticVariable.infoTextIndex]);
                                        }
                                    } else if (StaticVariable.stringInfoTextField != null && StaticVariable.stringInfoTextField.length > 0) {
                                        // Web-UI-Modus (layout == null): stringInfoTextField für /api/info-text und Programmsuche trotzdem pflegen
                                        if (!(androidErrorText.equals(""))) {
                                            StaticVariable.stringInfoTextField[0] = androidErrorText;
                                        } else {
                                            if (flagAutomaticOnOff == false) {
                                                StaticVariable.stringInfoTextField[0] = StaticVariable.getUebersetzung(0);
                                            }
                                            StaticVariable.stringInfoTextField[2] = StaticVariable.infoStringHeizung;
                                            incrementInfoTextIndex();
                                            while (StaticVariable.stringInfoTextField[StaticVariable.infoTextIndex].equals("")) {
                                                incrementInfoTextIndex();
                                            }
                                        }
                                    }


                                }
                                // layout.infoText.setText("BatterieLevel " +
                                // batteryLevel ) ;
                                beleuchteTastenAutomatik(); // im thread
                                // abhaengig von
                                // static variable
                                // globalON

                                if (isPowerConnected(getBaseContext()) ||
                                        batteryLevel >= StaticVariable.getBattEinschaltLevel()) {
                                    // Bildschirm wird nie dunkel!
                                    if (wl == null) {
                                        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                                        //wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "tom.turmtechnik:Turmtechnik");
                                        //wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "tom.turmtechnik:Turmtechnik");
                                        wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "tom.turmtechnik:Turmtechnik");
                                        wl.acquire(); // ab jetzt bleibt der
                                        // Bildschirm an
                                        // 14.11.13 das war veraltet -- jetzt bleibt cpu on
                                        setKeepScreenOn(true); // und nun auch der Bildschirm
                                        // Wiedereinschalten: Helligkeit wieder hell, sonst kann man nichts einstellen
                                        runOnUiThread(() -> {
                                            android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();
                                            lp.screenBrightness = 0.85f;
                                            getWindow().setAttributes(lp);
                                        });
                                    }
                                    // layoutParams.screenBrightness = 0.6f ;
                                    // getWindow().setAttributes(layoutParams);
                                    // powerOffCounter = 0 ;
                                    // }
                                    // else
                                    // {
                                    // Log.e("Battery" , "level=" +
                                    // batteryLevel) ;

                                    // System.exit(0);
                                    // finish();

                                    // }

                                    // powerOffCounter ++ ;
                                    // Log.e("powerOffCounter" , "=" +
                                    // powerOffCounter);

                                    // if(powerOffCounter > ( (20) * 60) * 10 )
                                    // // 50ms *2 10 Minuten
                                    // {
                                    // layoutParams.screenBrightness = 0.01f ;
                                    // // komplett dunkel
                                    // getWindow().setAttributes(layoutParams);
                                    // }
                                    // else
                                    // {
                                    // layoutParams.screenBrightness = 0.08f ;
                                    // // sehr dunkel
                                    // getWindow().setAttributes(layoutParams);
                                    // }
                                } else // wenn die Stromversorgung weg ist:
                                {
                                    if (wl != null) {
                                        try {
                                            //wl.release(); // wieder auf normal
                                            // modus schalten
                                            pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                                            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "tom.turmtechnik:Turmtechnik");
                                            wl.acquire(); // ab jetzt bleibt der


                                            wl = null; // ist jetzt wie flag
                                            // cpu nicht in stand by gehen
                                            // aber Bildschirm abdunkeln wenn keine Stromversorgung
                                            // oder accu unter 50 %
                                            setKeepScreenOn(false);
                                            LogTurmtechnik2 logTemp =
                                                    new LogTurmtechnik2("Strom weg und Akku unter soll%", 0, 0, 0, 0);
                                            logTemp = null;
                                        } catch (Throwable th) {
                                            // ignoring this exeption
                                            // probably wakeLock was already
                                            // released
                                        }
                                    }
                                    if (batteryLevel < StaticVariable.batt_shut_down_level2) // bei battery < soll%	// beenden
                                    {
                                        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                                        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "tom.turmtechnik:Turmtechnik");
                                        wl.acquire(); // Bildschirm dunkel

                                        doRun = false;

                                        wl.release();
                                        wl = null;
                                        // Log.e("BEENDET" , "!!!") ;
                                        // saveNebenuhr() ; // wird in beenden
                                        // erledigt
                                        //beenden();


                                        //loadNebenuhr();
                                        beendenButServiceRun();
                                    }
                                }

                            } else {
                                // Nur wenn natives Layout angezeigt wird (nicht im Web-UI-Vollbildmodus)
                                if (layout != null) {
                                    Seite1Layout.printDatum();
                                    Seite1Layout.printUhr();
                                    Seite1Layout.printMondAnzeige();
                                }
                                if (StaticVariable.seiteZweiIsRunning) {
                                    Seite2Layout.printUhr();
                                    Seite2Layout.printDatum();
                                }
                                if (StaticVariable.manuelerStartActivityIsRunning) {
                                    ManuelerStartLayout.printUhr();
                                    ManuelerStartLayout.printDatum();
                                }

                                // neu: wenn kein Strom wifi nicht aktiviern --
                                // 5.6.13
                                //if (isPowerConnected(getBaseContext()))
                                // neu: 25.08.14
                                // ueberhaupt nicht mehr aus und ein schalten
                                // am 17.03.16 -- Thomas ist auf der Baustelle
                                // mit kindle fire 7, doch wieder einschalten!!!
                                //if (false) {
                                if (true) {
                                    wifiOnCounter++;
                                    // if(wifiOnCounter > 150) // 10 * 100
                                    // ms(50ms*2) = alle 15 sekunden
                                    // zu kurz 12.8.2013
                                    // auf wunsch von Thomas am 5.11.2013 nochmal verlaengert
                                    // wegen dropbox synchronisation
                                    //if (wifiOnCounter > 300)
                                    //if (wifiOnCounter > 600) // jetzt 30 sekunden
                                    //Log.e("wifi" , "Oncounter=" + wifiOnCounter) ;
                                    if (wifiOnCounter > 6000) // auf Wunsch von Thomas auf 5 Minuten 7.2.14
                                    {
                                        wifiOnCounter = 0;
                                        myWifiManager = (WifiManager) // getBaseContext()
                                             context.getSystemService(
                                                        Context.WIFI_SERVICE);

                                        int tempWifiState = myWifiManager.getWifiState();

                                        if (tempWifiState != WifiManager.WIFI_STATE_ENABLED) {
                                            myWifiManager.setWifiEnabled(true);

                                        } else
                                        {
                                            //if ((!Serial_IoThread.getSerialIoStatus()) && (StaticVariable.bluetoothMode == false))

                                            if ((! zehnmalErrorIoStatusOk()) && (StaticVariable.bluetoothMode == false))
                                            {
                                                myWifiManager.setWifiEnabled(false);

                                            }
                                        else
                                        {
                                                if (!StaticVariable.bluetoothMode) {
                                                    myWifiManager.setWifiEnabled(true);
                                                }
                                            }
                                        }
                                        myWifiManager = null;
                                        System.gc();
                                    }


                                }
                            }
                        }
                    });
                }
            }
        };
        new Thread(runnable).start();

    }


    public static void incrementInfoTextIndex() {
        StaticVariable.infoTextIndex++;
        if (StaticVariable.infoTextIndex >= StaticVariable.stringInfoTextField.length) {
            //Log.e("stringInfoTextField" , "length=" + StaticVariable.stringInfoTextField) ;
            StaticVariable.infoTextIndex = 0;
        }
    }

    private void getMemoryInfo() {
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        MemoryInfo mi = new MemoryInfo();
        activityManager.getMemoryInfo(mi);
        Log.e("Verfuegbarer", "Speicher = " + mi.availMem);
    }

    private void checkReboot() {

        calendar = Calendar.getInstance();

        int sekunde = calendar.get(Calendar.SECOND);
        int minute = calendar.get(Calendar.MINUTE);
        int stunde = calendar.get(Calendar.HOUR_OF_DAY);

        if ((sekunde == 45) && (minute == 0) && (stunde == 03)) {
            //saveNebenuhr();
            //saveVerknuepfteTasten();

            //rebootSU();
            setRestartTimer(5);
            restartTurmtechnik();
        } else {
            calendar = null;
        }

    }

    private static int errorIoStatusCount ;

    public static boolean zehnmalErrorIoStatusOk()
    {
        if(  Serial_IoThread.getSerialIoStatus2())
        {
            errorIoStatusCount = 0 ;
            return true ;
        }
        else
        {
            errorIoStatusCount ++ ;
            {
                if(errorIoStatusCount > 10)
                {
                    return false ;
                }
                else
                {
                    return true ;
                }
            }
        }
    }

    private void beleuchteTastenAutomatik() {
        if (layout == null) return;

        for (int i = 0; i < layout.buttons.size(); i++) {
            if (relaisNumber[i] < StaticConstants.LIMIT_1000_100) {

                if (globalOn[i] == true) {
                    if (UhrThread.blockReady != 0) {
                        //if (Serial_IoThread.getSerialIoStatus())
                        if(zehnmalErrorIoStatusOk())
                        {
                            layout.buttonOnRed(i);
                        } else {
                            layout.buttonOnErrorRed(i);
                        }
                    } else {
                        //if (Serial_IoThread.getSerialIoStatus())
                        if(zehnmalErrorIoStatusOk())
                        {
                            layout.buttonOnOK(i);
                        } else {
                            layout.buttonOnError(i);
                        }
                    }

                } else {
                    layout.buttonOff(i);
                }

            } else {

                if (relaisNumber[i] == StaticConstants.STOP) // Taste Stop ?
                {
                    layout.buttonOnStop(i); // wird aus System.xls gelesen
                }
                if (relaisNumber[i] != null && (relaisNumber[i] == StaticConstants.AUTOMATIC || relaisNumber[i] == 101)) // Taste Automatik
                {
                    if (flagAutomaticOnOff == false) {
                        layout.buttonOff(automatikTasteIndex);
                    } else {
                        layout.buttonOnOK(automatikTasteIndex);
                    }
                }
                if (relaisNumber[i] == StaticConstants.SCHLAGWERK_ON_OFF) // Taste Schlagwerk on/off ?
                {
                    if (StaticVariable.flagSchlagwerkOnOff == false) {
                        layout.buttonOff(schlagwerkTasteIndex); // setAutomaticRed();
                    } else {
                        layout.buttonOnOK(schlagwerkTasteIndex);
                    }
                }
                if ((relaisNumber[i] > 2000) && (relaisNumber[i] < 3000)) // verknuepfte Taste ?
                {
                    int verknuepftOffset = relaisNumber[i] - 2001;
                    //Log.e("verkn.Offset", "=" + verknuepftOffset + "[]=" + verknuepfteTastenOn[verknuepftOffset]) ;
                    if (verknuepftOffset >= 0 && verknuepftOffset < verknuepfteTastenOn.length && Boolean.TRUE.equals(verknuepfteTastenOn[verknuepftOffset])) {
                        layout.buttonOnOK(i);
                    } else {
                        layout.buttonOff(i);
                    }
                }
                if (TurmtechnikActivity.relaisNumber[i] > 3000) {
                    if ((StaticVariable.sofortStartPopupFlag)
                            && (TurmtechnikActivity.relaisNumber[i] == StaticVariable.sofortStartPopupRelaisNummerAktiv)) {
                        layout.buttonOnOK(i);
                    } else {
                        layout.buttonOff(i);
                    }
                }

            }
        }
    }

    private int checkMelodienNormalprogramm() {
        // Prüfe zuerst, ob Programme in der Datenbank existieren
        boolean programmeInDB = false;
        ExcelRead excelread = null; // Deklariere außerhalb, damit es im gesamten Funktionsbereich verfügbar ist
        
        try {
            android.content.Context context = TurmtechnikActivity.turmtechnikContext;
            if (context != null) {
                PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
                java.util.List<Programm> programme = dbHelper.getProgrammeByTagtyp("Normalprogramm");
                if (programme != null && !programme.isEmpty()) {
                    programmeInDB = true;
                    Log.d("checkMelodien", "Programme in DB gefunden für Normalprogramm (" + programme.size() + " Programme), verwende DB statt Excel");
                    // Lade Melodien aus DB-Programmen
                    normalProgrammblockFileNamen.clear();
                    for (Programm p : programme) {
                        if (p.getFunktion() != null && p.getFunktion().equals("Melodie") && p.getMelodieName() != null && !p.getMelodieName().trim().isEmpty()) {
                            normalProgrammblockFileNamen.add("/Turmtechnik/Melodien/" + p.getMelodieName());
                        }
                    }
                    // Prüfe Dateien wie vorher (Code weiter unten)
                }
            }
        } catch (Exception e) {
            Log.e("checkMelodien", "Fehler beim Prüfen der DB", e);
        }
        
        // Fallback zu Excel nur wenn keine Programme in DB
        if (!programmeInDB) {
            excelread = new ExcelRead();
            Log.e("checkMelodien", "Normal=" + normalprogrammFileString + " (Excel-Fallback)");
            try {
                excelread.openXls_save(normalprogrammFileString);
            } catch (BiffException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                Log.e("Biff", "error");
                new LogExcelError(0, 0, normalprogrammFileString, -1, sourceFileName, 1627);

            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                Log.e("IO", "error");
            }

            // Log.i("file open" , "vor excelread") ;

            // Log.i("lese" , "1ten Filenamen" + (excelread.getCellString(1, 5 ))) ;

            normalProgrammblockFileNamen.clear();

            int zeile = 3;
            //Log.e("SUCHE" , "BLOECKE:") ;

            try {
                while (!(excelread.getCellString(SPALTE_C_MELODIE_NAME, zeile).equals(""))) // noch ein Block Name vorhanden?
                {
                    String melodieTemp = excelread.getCellString(SPALTE_B_FUNKTION, zeile);
                    if (melodieTemp.equals("Melodie")) {
                        normalProgrammblockFileNamen.add("/Turmtechnik/Melodien/"
                                + excelread.getCellString(SPALTE_C_MELODIE_NAME, zeile) + ".xls"); // lese Block
                        //Log.e("fname=", "" + normalProgrammblockFileNamen.lastElement());
                    }
                    zeile++;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // Prüfe Dateien oder DB: Melodie gilt als vorhanden, wenn Datei existiert ODER in DB
        File checkfile;

        for (int i = 0; i < normalProgrammblockFileNamen.size(); i++) {
            String element = normalProgrammblockFileNamen.elementAt(i);
            String blockFileString = Environment.getExternalStorageDirectory().getPath() + element;
            checkfile = new File(blockFileString);
            if (!checkfile.exists()) {
                File checkfileXls = new File(blockFileString + ".xls");
                if (checkfileXls.exists()) {
                    blockFileString = blockFileString + ".xls";
                    normalProgrammblockFileNamen.set(i, blockFileString);
                    checkfile = null;
                    System.gc();
                    continue;
                }
                blockFileString = Environment.getDataDirectory().getPath() + element;
                checkfile = new File(blockFileString);
                if (!checkfile.exists()) {
                    checkfileXls = new File(blockFileString + ".xls");
                    if (checkfileXls.exists()) {
                        blockFileString = blockFileString + ".xls";
                        normalProgrammblockFileNamen.set(i, blockFileString);
                        checkfile = null;
                        System.gc();
                        continue;
                    }
                }
            }
            if (!checkfile.exists()) {
                // Prüfen, ob Melodie in der DB existiert (dann keine .xls-Datei nötig)
                String melodieName = element;
                if (melodieName.contains("/")) melodieName = melodieName.substring(melodieName.lastIndexOf('/') + 1);
                if (melodieName.toLowerCase().endsWith(".xls")) melodieName = melodieName.substring(0, melodieName.length() - 4);
                melodieName = melodieName.trim().replaceAll("\\s+", " ");
                boolean inDb = false;
                if (turmtechnikContext != null && !melodieName.isEmpty()) {
                    try {
                        PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(turmtechnikContext);
                        if (dbHelper.getMelodieByNormalizedName(melodieName) != null) {
                            inDb = true;
                        }
                    } catch (Exception e) { }
                }
                if (inDb) {
                    checkfile = null;
                    System.gc();
                    continue;
                }
                if (!programmeInDB && excelread != null) {
                    excelread.closeWorkbook();
                }
                checkfile = null;
                System.gc();
                return i;
            } else {
                normalProgrammblockFileNamen.set(i, blockFileString);
                checkfile = null;
                System.gc();
            }
        }
        //Log.e("nach", "for schleife alle file sind vorhanden");

        if (excelread != null) {
            excelread.closeWorkbook();
        }

        return -1; // alle file sind vorhanden
    }

    /** Lädt feste Festtage-Tagtypen aus der Datenbank (kein System.xls Sheet 13 mehr). */
    private int checkFesteFeiertage()
    {
        festeFesttageTagtypFileNamen.clear();
        try {
            android.content.Context context = TurmtechnikActivity.turmtechnikContext;
            if (context != null) {
                PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
                java.util.List<PlatinenDatabaseHelper.Tagtyp> tagtypen = dbHelper.getTagtypenByProgrammTyp("festtag_fest");
                if (tagtypen != null) {
                    for (PlatinenDatabaseHelper.Tagtyp tagtyp : tagtypen) {
                        String name = tagtyp.name != null ? tagtyp.name.trim() : "";
                        if (name.isEmpty()) continue;
                        if (!name.toLowerCase().endsWith(".xls")) name = name + ".xls";
                        String fullPath = sdCardPath + "/Turmtechnik/Programmtage/" + name;
                        festeFesttageTagtypFileNamen.add(fullPath);
                    }
                    Log.d("checkFesteFeiertage", "Feste Festtage aus DB: " + festeFesttageTagtypFileNamen.size() + " Tagtypen");
                }
            }
        } catch (Exception e) {
            Log.e("checkFesteFeiertage", "Fehler beim Laden aus DB: " + e.getMessage(), e);
        }
        return -1; // Erfolg (keine fehlende Datei-Prüfung mehr)
    }

    private String checkFeiertag(String fileName) {
        ExcelRead excelread = new ExcelRead();
        try {
            excelread.openXls_save(fileName);
        } catch (BiffException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return fileName;
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return fileName;
        }

        int zeile = 3; // suche ab 5te Zeile
        int zeilen = excelread.getCellZeilen();
        String retString = "";

        while (zeile < zeilen) // noch ein Melodie Name vorhanden?
        {
            try {
                if (!excelread.getCellString(1, zeile).equals("")) {
                    String melodie = sdCardPath + "/Turmtechnik/Melodien/"
                            + excelread.getCellString(1, zeile) + ".xls"; // lese
                    // Block
                    checkfile = new File(melodie);
                    if (!checkfile.exists()) {
                        retString = melodie;
                        excelread.closeWorkbook();
                        return retString;
                    }
                } else {
                    excelread.closeWorkbook(); // bei der ersten leerzeile beenden
                    return retString;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            zeile++;
        }
        excelread.closeWorkbook();
        return retString;
    }

    private String checkFeiertagNormal() {
        return checkFeiertag(sdCardPath
                + "/Turmtechnik/Programmtage/Feiertag normal.xls");
    }

    private String checkVorfeiertagNormal() {
        return checkFeiertag(sdCardPath
                + "/Turmtechnik/Programmtage/Vorfeiertag normal.xls");

    }

    // 9.6.13 Pfingsten ist wahrscheinlich ein alter nicht benutzter rest
    /*
     * private String checkPfingsten() { return checkFeiertag(sdCardPath +
	 * "/Turmtechnik/Programmtage/Pfingsten.xls") ; }
	 */

    /** Lädt variable Festtage-Tagtypen aus der Datenbank (kein System.xls Sheet 12 mehr). */
    private int checkVariableFeiertage() {
        variableFesttageTagtypFileNamen.clear();
        try {
            android.content.Context context = TurmtechnikActivity.turmtechnikContext;
            if (context != null) {
                PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
                java.util.List<PlatinenDatabaseHelper.Tagtyp> tagtypen = dbHelper.getTagtypenByProgrammTyp("festtag_variabel");
                if (tagtypen != null) {
                    for (PlatinenDatabaseHelper.Tagtyp tagtyp : tagtypen) {
                        String name = tagtyp.name != null ? tagtyp.name.trim() : "";
                        if (name.isEmpty()) continue;
                        if (!name.toLowerCase().endsWith(".xls")) name = name + ".xls";
                        String fullPath = sdCardPath + "/Turmtechnik/Programmtage/" + name;
                        variableFesttageTagtypFileNamen.add(fullPath);
                    }
                    Log.d("checkVariableFeiertage", "Variable Festtage aus DB: " + variableFesttageTagtypFileNamen.size() + " Tagtypen");
                }
            }
        } catch (Exception e) {
            Log.e("checkVariableFeiertage", "Fehler beim Laden aus DB: " + e.getMessage(), e);
        }
        return -1; // Erfolg (keine fehlende Datei-Prüfung mehr)
    }

    private void changeAutomatic() {
        if (flagAutomaticOnOff == true) {
            MelodieThreadNew.doRunOff();
            StaticVariable.stopBetaetigt = true;
            allMelodieRelaisOffAusfuehren();

            flagAutomaticOnOff = false;
            if (layout != null) layout.buttonOff(automatikTasteIndex); // setAutomaticRed();
            //StaticVariable.stringInfoTextField[0] = "Automatik ausgeschaltet";
            StaticVariable.stringInfoTextField[0] = StaticVariable.getUebersetzung(0) ;
            StaticVariable.stringInfoTextField[2] = "";
            // WICHTIG: Speichere sofort, damit Status nicht verloren geht
            saveVerknuepfteTasten_and_automaticTaste();
        } else {
            flagAutomaticOnOff = true;
            // layout.setAutomaticGreen();
            if (layout != null) layout.buttonOnOK(automatikTasteIndex);
            //StaticVariable.stringInfoTextField[0] = "suche nächsten automatik Start...";
            StaticVariable.stringInfoTextField[0] = StaticVariable.getUebersetzung(17);
            StaticVariable.stringInfoTextField[2] = "";
            StaticVariable.sofortStartPopupGefunden = false;
            UhrThread.newSearchAutomaticStart = true;
            // Sofort Programmabfrage und Anzeige „nächstes Programm“, nicht erst bei nächster Sekunde
            StaticVariable.refreshInfoTextVerknuepfteTaste = true;
            StaticVariable.infoTextRefreshCount = 5;
            // WICHTIG: Speichere sofort, damit Status nicht verloren geht
            saveVerknuepfteTasten_and_automaticTaste();
        }
    }

    private void changeSchlagwerkFlag() {
        if (StaticVariable.flagSchlagwerkOnOff == true) {
            MelodieThreadNew.doRunOff();
            StaticVariable.stopBetaetigt = true;
            StaticVariable.pathStoppedByUser = (StaticVariable.pathAndFileNameNextMelodie != null ? StaticVariable.pathAndFileNameNextMelodie : "");
            allMelodieRelaisOffAusfuehren();

            StaticVariable.flagSchlagwerkOnOff = false;
            if (layout != null) layout.buttonOff(schlagwerkTasteIndex); // setAutomaticRed();

        } else {
            StaticVariable.flagSchlagwerkOnOff = true;
            // layout.setAutomaticGreen();
            if (layout != null) layout.buttonOnOK(schlagwerkTasteIndex);
        }
    }

    // Meteor Code entfernt: changeAutomaticMeteor()

    private int sucheAutomaticTaste() {
        int index;
        for (index = 0; index < RELAIS_COUNT; index++) {
            if (relaisNumber[index] != null && (relaisNumber[index] == StaticConstants.AUTOMATIC || relaisNumber[index] == 101)) {
                break; // automatik taste gefunden (1101 oder alte Konfiguration 101)
            }
        }
        return index;
    }

    private int sucheSchlagwerkTaste() {
        int index;
        for (index = 0; index < RELAIS_COUNT; index++) {
            if (relaisNumber[index] == StaticConstants.SCHLAGWERK_ON_OFF) //104) // Schlagwerk Taste?
            {
                break; // automatik taste gefunden
            }
        }
        return index;
    }

    private void allRelaisOffNeu() {
        if (serial_iothread == null) return;
        int btnCount = layout != null ? layout.buttons.size() : layoutButtonsSize;
        for (int ix = 0; ix < RELAIS_COUNT; ix++) {
            globalOn[ix] = false;
            serial_iothread.changeRelais(ix + 1, false);
            if (ix < btnCount) {
                if ((relaisNumber[ix] < StaticConstants.LIMIT_1000_100)) // verknuepfte Tasten usw
                // auslassen
                { // nur Tasten die ein Relais haben ausschalten
                    if (layout != null) layout.buttonOff(ix);
                }
            }
        }
    }

    public void allRelaisOffInternetNeu() {
        if (serial_iothread == null) return;
        int btnCount = layout != null ? layout.buttons.size() : layoutButtonsSize;
        for (int ix = 0; ix < RELAIS_COUNT; ix++) {
            globalOn[ix] = false;
            serial_iothread.changeRelais(ix + 1, false);
            if (ix < btnCount) {
                if ((relaisNumber[ix] < StaticConstants.LIMIT_1000_100)) // verknuepfte Tasten usw
                // auslassen
                { // nur Tasten die ein Relais haben ausschalten
                    // layout.buttonOff(ix); // das stuerzt im dem Thread ab ??
                    // bildschirm?
                }
            }
        }
    }

    /**
     * Von der Web-UI (POST /api/control/stop) aufgerufen: wie native Stop-Taste – Sounds stoppen, Melodie-Thread beenden, alle Relais aus.
     */
    public static void allRelaisOffFromWeb(android.content.Context context) {
        final TurmtechnikActivity act = turmtechnikActivityInstance != null ? turmtechnikActivityInstance : (context instanceof TurmtechnikActivity ? (TurmtechnikActivity) context : null);
        if (act == null) {
            if (serial_iothread != null) {
                for (int ix = 0; ix < RELAIS_COUNT; ix++) {
                    globalOn[ix] = false;
                    serial_iothread.changeRelais(ix + 1, false);
                }
            }
            return;
        }
        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                act.performStopFromWeb();
            }
        });
    }

    /** Wie native Stop-Taste: alle Glockensounds stoppen, Melodie-Thread aus, Relais aus. Auf UI-Thread aufrufen. */
    private void performStopFromWeb() {
        stopAllGlockenSounds();
        MelodieThreadNew.doRunOff();
        StaticVariable.stopBetaetigt = true;
        StaticVariable.pathStoppedByUser = (StaticVariable.pathAndFileNameNextMelodie != null ? StaticVariable.pathAndFileNameNextMelodie : "");
        StaticVariable.schlagwerkTriggerSyncTime2 = true;
        allMelodieRelaisOffAusfuehren();
        UhrThread.blockReady = 0;
        UhrThread.newSearchAutomaticStart = true;
        StaticVariable.sofortStartPopupFlag = false;
        StaticVariable.sofortStartPopupGefunden = false;
        if (StaticVariable.stringInfoTextField != null && StaticVariable.stringInfoTextField.length > 0) {
            StaticVariable.stringInfoTextField[0] = "Stop betätigt";
        }
        if (serial_iothread != null) {
            for (int ix = 0; ix < RELAIS_COUNT; ix++) {
                globalOn[ix] = false;
                serial_iothread.changeRelais(ix + 1, false);
            }
        }
    }

    /**
     * Schaltet alle Melodie-Relais aus.
     * WICHTIG: Diese Methode sollte nur aufgerufen werden, wenn die Melodie explizit gestoppt werden soll
     * (z.B. durch STOP-Taste, Automatic-AUS, Schlagwerk-AUS).
     * Beim Seitenwechsel sollte diese Methode NICHT aufgerufen werden, damit die Melodie weiterläuft.
     */
    public static synchronized void allMelodieRelaisOffAusfuehren() {
        // Prüfe, ob eine Melodie aktiv ist
        boolean melodieAktiv = MelodieThreadNew.isRunning();
        
        if (melodieAktiv) {
            Log.e("ausfuehren", "WARNUNG: allMelodieRelaisOffAusfuehren() aufgerufen, während Melodie läuft!");
            Log.e("ausfuehren", "Melodie sollte nur durch STOP-Taste, Automatic-AUS oder Schlagwerk-AUS gestoppt werden.");
        }

        StaticVariable.vorschwingenStartzeitenMotorRelais.clear();
        StaticVariable.nextMelodieStartSekunden2 = 0;

        int globalOffOhneMelodieIndexSize = globalOffOhneMelodieIndex.size();
        int globalOffOhneMelodieRelaisNumberSize = globalOffOhneMelodieRelaisNumber.size();


        Log.e("ausfuehren", "allMelodieRelaisOff");
        Log.e("ausfuehren", "globalOffOhneMelodienIndex.size=" + globalOffOhneMelodieIndexSize);
        Log.e("ausfuehren", "globalOffOhneMelodieRelaisNummer.size=" + globalOffOhneMelodieRelaisNumberSize);

        if (serial_iothread != null) {
            for (int i = 0; i < globalOffOhneMelodieRelaisNumberSize; i++) {
                serial_iothread.changeRelais(globalOffOhneMelodieRelaisNumber.get(i), false);
                Log.e("ausfuehren", "i=" + i + "relaisNumber=" + globalOffOhneMelodieRelaisNumber.get(i));
            }
            // Auch alle Vorschwing-Motor-Relais ausschalten
            int vorschwingenMotorRelaisSize = StaticVariable.vorschwingenMotorRelaisNeu.size();
            Log.e("ausfuehren", "Vorschwing-Motor-Relais ausschalten, Anzahl=" + vorschwingenMotorRelaisSize);
            for (int i = 0; i < vorschwingenMotorRelaisSize; i++) {
                int vorschwingRelais = StaticVariable.vorschwingenMotorRelaisNeu.get(i);
                if (vorschwingRelais > 0 && vorschwingRelais <= TurmtechnikActivity.RELAIS_COUNT) {
                    serial_iothread.changeRelais(vorschwingRelais, false);
                    Log.e("ausfuehren", "Vorschwing-Motor-Relais " + vorschwingRelais + " AUS geschaltet");
                }
            }
        }

        for (int i = 0; i < globalOffOhneMelodieIndexSize; i++) {
            Log.e("ausfuehren", "send tasten status i=" + i + "index=" + globalOffOhneMelodieIndex.get(i));
            // Meteor Code entfernt
        }

        for (int i = 0; i < globalOffOhneMelodieIndexSize; i++) {
            globalOn[globalOffOhneMelodieIndex.get(i)] = false;
            //layout.buttonOff(globalOffOhneMelodieIndex.get(i)); // Stuerzt ab !!!!!!!!! 2.7.15

            Log.e("ausfuehren", "i=" + i + "index=" + globalOffOhneMelodieIndex.get(i));
        }


        //sendTastenStatusSeite1();
    }

    public static void allMelodieRelaisOffVorbereiten2() {

        Log.e("aktuell", "allMelodieRelaisOff Vorbereiten");

        ExcelRead excelreadx = new ExcelRead();
        try {
            excelreadx.openXlsSheet(beschriftungTastenFileString, BESCHRIFTUNG_GLOCKEN_SHEET_NEW);
        } catch (BiffException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        int zeilenx = excelreadx.getCellZeilen() - 1;
        int aktuelleZeileOhneLeerx = 0;
        for (int ix = 0; ix < zeilenx; ix++) {
            try {
                if (!((excelreadx.getCellString(2, ix + 1).equals("null"))
                        || (excelreadx.getCellString(2, ix + 1).equals("NULL")))
                        )                                                     // relais
                // vorhanden?
                {
                    String funktionTemp = excelreadx.getCellString(1, ix + 1).trim();
                    String nameTemp = excelreadx.getCellString(2, ix + 1).trim();


                    Log.e("vorbereiten funktion", "=" + funktionTemp);
                    Log.e("vorbereiten name", "=" + nameTemp);
                    if (!(nameTemp.equals("leer"))) {

                        Log.e("vorbereiten Zeile", "ohne leerX=" + aktuelleZeileOhneLeerx + " funktiontemp=" + funktionTemp + " ix" + ix);
                        if ((funktionTemp.equals("Melodie") || funktionTemp.equals(("Schwingen")))) {
                            Log.e("vorbereiten", "Melodie oder Schwingen");

                            //globalOn[aktuelleZeileOhneLeerx] = false;
                            //layout.buttonOff(aktuelleZeileOhneLeerx);

                            globalOffOhneMelodieIndex.add(aktuelleZeileOhneLeerx);

                            int platineNummer = 1;
                            try {
                                platineNummer = (int) Long.parseLong(excelreadx.getCellString(5, ix + 1));
                            } catch (Exception e) {

                            }
                            Log.e("stop platineNr", "=" + platineNummer);

                            int relaisNumber = 0;
                            try {
                                relaisNumber = (int) Long.parseLong(excelreadx.getCellString(3, ix + 1));
                            } catch (Exception e) {

                            }
                            relaisNumber = relaisNumber + ((platineNummer - 1) * 32);
                            Log.e("stop relaisNr", "=" + relaisNumber);
                            //serial_iothread.changeRelais(relaisNumber, false);
                            globalOffOhneMelodieRelaisNumber.add(relaisNumber);
                        }

                    }
                    aktuelleZeileOhneLeerx++;
                }
                //else {
                //    aktuelleZeileOhneLeerx++;
                //}

            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(3, ix + 1, beschriftungTastenFileString, BESCHRIFTUNG_GLOCKEN_SHEET_NEW, sourceFileName, 2005);
            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(3, ix + 1, beschriftungTastenFileString, BESCHRIFTUNG_GLOCKEN_SHEET_NEW, sourceFileName, 2009);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(3, ix + 1, beschriftungTastenFileString, BESCHRIFTUNG_GLOCKEN_SHEET_NEW, sourceFileName, 2013);
            }
        }
        excelreadx.closeWorkbook();
        //sendTastenStatusSeite1();
    }

    private static void allMelodieRelaisOffInternetOld() {
        Log.e("aktuell", "allMelodieRelaisOffInternet");

        StaticVariable.vorschwingenStartzeitenMotorRelais.clear();
        StaticVariable.nextMelodieStartSekunden2 = 0;

        ExcelRead excelread2 = new ExcelRead();
        try {
            excelread2.openXlsSheet(beschriftungTastenFileString, BESCHRIFTUNG_GLOCKEN_SHEET_NEW);
        } catch (BiffException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }


        int zeilen = excelread2.getCellZeilen() - 1;
        int aktuelleZeileOhneLeer = 0;

        for (int i = 0; i < zeilen; i++) {

            try {
                if (!((excelread2.getCellString(3, i + 1).equals("null"))
                        || (excelread2.getCellString(3, i + 1).equals("NULL")))
                        )                                                     // relais
                // vorhanden?
                {
                    String funktionTemp = excelread2.getCellString(1, i + 1).trim();
                    String nameTemp = excelread2.getCellString(2, i + 1).trim();

                    Log.e("stop funktionTemp", "=" + funktionTemp);
                    Log.e("stop nameTemp", "=" + nameTemp);
                    if (!(nameTemp.equals("leer"))) {

                        Log.e("aktuelleZeile", "=" + aktuelleZeileOhneLeer + " ft=" + funktionTemp);
                        if ((funktionTemp.equals("Melodie") || funktionTemp.equals(("Schwingen")))) {
                            Log.e("Melodie", "oder Schwingen");

                            globalOn[aktuelleZeileOhneLeer] = false;
                            if (layout != null) layout.buttonOff(aktuelleZeileOhneLeer);

                            int platineNummer = 1;
                            try {
                                platineNummer = (int) Long.parseLong(excelread2.getCellString(5, i + 1));
                            } catch (Exception e) {

                            }

                            Log.e("stop platineNr", "=" + platineNummer);


                            int relaisNumber = 0;
                            try {
                                relaisNumber = (int) Long.parseLong(excelread2.getCellString(3, i + 1));
                            } catch (Exception e) {

                            }
                            relaisNumber = relaisNumber + ((platineNummer - 1) * 32);
                            Log.e("stop relaisNr", "=" + relaisNumber);
                            if (serial_iothread != null) serial_iothread.changeRelais(relaisNumber, false);

                        }
                        aktuelleZeileOhneLeer++;
                    }

                } else {
                    aktuelleZeileOhneLeer++;
                }


            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(3, i + 1, beschriftungTastenFileString, BESCHRIFTUNG_GLOCKEN_SHEET_NEW, sourceFileName, 2049);
            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(3, i + 1, beschriftungTastenFileString, BESCHRIFTUNG_GLOCKEN_SHEET_NEW, sourceFileName, 2053);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(3, i + 1, beschriftungTastenFileString, BESCHRIFTUNG_GLOCKEN_SHEET_NEW, sourceFileName, 2057);
            }
        }
        excelread2.closeWorkbook();

    }


    private static int displayWidth;
    private static int displayHeight;

    private void getDisplayParameter() {
        Display display = this.getWindowManager().getDefaultDisplay();
        displayWidth = display.getWidth();
        displayHeight = display.getHeight();
        if (StaticConstants.DEBUG) {
            // Log.i("displayWidth" , "=" + displayWidth) ;
            // Log.i("displayHeight" , "=" + displayHeight) ;
        }
    }

    public static int getDisplayWith() {
        return displayWidth;
    }

    public static int getDisplayHeight() {
        return displayHeight;
    }

    @Override
    public void onRestart() {
        //setRestartTimer(SET_BIG_CLOCK_TIME);
        super.onRestart();
        //new LogTurmtechnik("onRestart" , 0 , 0 , 0 , 0  ) ;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Heartbeat sofort, damit StartTurmtechnikService nicht nach 15 s eine zweite Activity startet (onResume kann spät kommen)
        Intent hb = new Intent(this, StartTurmtechnikService.class);
        hb.setAction(StartTurmtechnikService.ACTION_HEARTBEAT);
        startService(hb);
        // Nur bei echtem Neustart (nach onCreate) loggen – nicht bei „Bewegung: Schoner aus / Layout 1“ (dann kein Neustart ins Log)
        if (activityJustCreated) {
            activityJustCreated = false;
            boolean wokeByMotion = getIntent() != null && getIntent().getBooleanExtra("woke_by_motion", false);
            if (!wokeByMotion) {
                new LogTurmtechnik2("onStart NEU GESTARTET!!!!", 0, 0, 0, 0);
                try { LogTurmtechnik2.setDeviceIpForNebenuhrLog(getLocalIpAddress()); } catch (Exception e) { }
                LogTurmtechnik2.appendNebenuhrRelaisLogNeustart();
            }
        }
        // hier Turmuhrzeiger neu laden
        // loadNebenuhr(); // im thread laeuft die Nebenuhr weiter!!! auch bei on Stop

//		Log.i("serial_io" , "thread gestoppt") ;
//		if (serial_iothread != null)
//		{
//			serial_iothread.endSerialThread();
//		}
    }

    @Override
    public void onResume() {
        super.onResume();
        applyExitActionFromIntent(getIntent());
        isInForeground = true;
        // „Turmtechnik starten“-Notification aufheben, wenn die App wieder im Vordergrund ist (Full-Screen-Intent oder Tipp)
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(StartTurmtechnikService.NOTIFICATION_ID_OPEN_APP);
        } catch (Exception ignored) { }
        // Web-Server wieder starten, falls Activity z. B. nach Bildschirmschoner neu erstellt wurde (onDestroy hatte ihn gestoppt)
        startConfigWebServer();
        loadAnlageLogAndRebootFromDb(this);
        Log.w("Screensaver", "onResume: setze Schoner-Timer zurück");
        resetScreensaverTimer();
        // Heartbeat starten: Service setzt Neustart-Timer zurück; bei Absturz fehlt Heartbeat → Service startet Activity neu
        heartbeatHandler.removeCallbacks(heartbeatRunnable);
        heartbeatRunnable.run();
        if (getIntent() != null && getIntent().getBooleanExtra("woke_by_motion", false)) {
            getIntent().removeExtra("woke_by_motion");
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.screenBrightness = 0.9f;
            getWindow().setAttributes(lp);
            if (screensaverOverlay != null && screensaverOverlay.getVisibility() == View.VISIBLE) {
                screensaverOverlay.setVisibility(View.GONE);
                bringMainContentToFront();
                resetScreensaverTimer();
            }
        }
        // Nach Bildschirm aus/an (Hardware-Taste): Overlay ausblenden und Hauptlayout in den Vordergrund,
        // damit nicht erst schwarzer Bildschirm und später Schoner angezeigt werden.
        if (screensaverOverlay != null && screensaverOverlay.getVisibility() == View.VISIBLE) {
            screensaverOverlay.setVisibility(View.GONE);
        }
        bringMainContentToFront(); // Zeichenreihenfolge nach Screen-on wiederherstellen
        startOrStopMotionDetectionService();
        registerDismissScreensaverReceiver();
        //new LogTurmtechnik("onResume" , 0, 0, 0, 0) ;
        if (onCreateFlag == true) {
            onCreateFlag = false;
            // initThread();
        }

        if (ausgangHeizungThreadNew2 == null) {
            ausgangHeizungThreadNew2 = new AusgangHeizungThreadNew2();
            ausgangHeizungThreadNew2.start();

        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getBooleanExtra("woke_by_motion", false)) {
            intent.removeExtra("woke_by_motion");
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.screenBrightness = 0.9f;
            getWindow().setAttributes(lp);
            // Nur Overlay ausblenden und Layout 1 zeigen – NICHT dismissScreensaverOverlay(), da das bei activityStartedForScreensaver die Web-UI (app-seite1) öffnet → schwarze Seite mit weißem Rand
            if (screensaverOverlay != null && screensaverOverlay.getVisibility() == View.VISIBLE) {
                screensaverOverlay.setVisibility(View.GONE);
                bringMainContentToFront();
                resetScreensaverTimer();
            }
        }
        if (intent != null && intent.getBooleanExtra(EXTRA_SHOW_SCREENSAVER_OVERLAY, false)) {
            intent.removeExtra(EXTRA_SHOW_SCREENSAVER_OVERLAY);
            Log.w("Screensaver", "onNewIntent: Schoner-Extra → öffne WebUiActivity screensaver.html");
            try {
                Intent i = new Intent(this, WebUiActivity.class);
                i.putExtra(WebUiActivity.EXTRA_PATH, "screensaver.html");
                i.putExtra(WebUiActivity.EXTRA_USE_LOCALHOST, true);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } catch (Exception e) {
                Log.w("Screensaver", "WebUiActivity starten fehlgeschlagen", e);
                if (screensaverOverlay != null) showScreensaverOverlayNow();
            }
        }
        applyExitActionFromIntent(intent);
    }

    /** Verarbeitet EXTRA_EXIT_ACTION (von showExitPasswordDialogFrom aus anderer Activity). */
    private void applyExitActionFromIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getStringExtra(EXTRA_EXIT_ACTION);
        if (action == null) return;
        intent.removeExtra(EXTRA_EXIT_ACTION);
        if ("15min".equals(action)) {
            beendenMit15MinHintergrund();
        } else if ("beenden".equals(action)) {
            beenden();
        } else if ("delete_beenden".equals(action)) {
            deleteBeschriftungTasten();
            beenden();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isInForeground = false;
        unregisterDismissScreensaverReceiver();
        screensaverHandler.removeCallbacks(startScreensaverRunnable);
        screensaverHandler.removeCallbacks(screenOffRunnable);
        // Alarm NICHT abbrechen: App muss im Hintergrund weiterlaufen (z. B. WebUiActivity sichtbar). Nach X Min feuert der Alarm und holt TurmtechnikActivity mit Schoner in den Vordergrund. Alarm wird nur in onDestroy abgebrochen.
        //new LogTurmtechnik("onPause" ,0,0,0,0) ;

        // startTurmtechnik();
        // wl.release();
        // allGlockenOff();
        // avrnetiothread.stopAvrFlag();
        // avrnetiothread.stop();
        // this.finish();
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        // Nur bei echtem Tippen (ACTION_DOWN) Timer zurücksetzen – nicht bei ACTION_MOVE/UP,
        // sonst würden z. B. ständige UI-Updates/Animationen den Schoner verhindern
        if (isInForeground && ev.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            resetScreensaverTimer();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onStop() {
        super.onStop();
        new LogTurmtechnik2("onStop", 0, 0, 0, 0);
        // saveNebenuhr();  // der uhr thread laueft weiter !!!

        // allGlockenOff();
        // avrnetiothread.stopAvrFlag();
        // avrnetiothread.stop();
        // this.finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isInForeground = false;
        turmtechnikActivityInstance = null;
        heartbeatHandler.removeCallbacks(heartbeatRunnable);
        // Kein LogExcelError – onDestroy bei Beenden ist normal, würde sonst LogError.txt mit "Fehler" füllen
        android.util.Log.d("TurmtechnikActivity", "onDestroy");
        // Nur bei echtem Beenden (z. B. Back/Exit): Thread stoppen, Alarm abbuchen, Beenden-Logik. Bei Zerstörung durch System (WebUiActivity im Vordergrund) nichts davon – Melodie/Relais laufen weiter.
        if (isFinishing()) {
            LogTurmtechnik2.appendCrashLog("Beenden");
            cancelScreensaverAlarm();
            StaticVariable.serial_io_ThreadsRun = false;
            TimeSyncThread.stopInstance();
            if (StaticVariable.makeBeschriftungTasten == false) {
                beendenButServiceRun();
            }
        }
        // Stoppe Web-Server (immer, damit nur eine Activity ihn nutzt)
        stopConfigWebServer();
        //Intent intent = new Intent(TurmtechnikActivity.this, TurmtechnikActivity.class) ;
        //TurmtechnikActivity.this.startActivity(intent) ;
    }

    private void initRelais() {
        for (int i = 0; i < RELAIS_COUNT; i++) {
            relaisNumber[i] = 0;
            hammerZeit[i] = 0;
            buttonId[i] = "null";
        }
        // Alle Slots aus DB (beschriftung_tasten) befüllen – unabhängig von layout, später auf 96 o. ä. erweiterbar
        try {
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(this);
            java.util.List<PlatinenDatabaseHelper.BeschriftungTastenRow> rows = dbHelper.getBeschriftungTasten();
            if (rows != null && !rows.isEmpty()) {
                int end = Math.min(BESCHRIFTUNG_TASTEN_MAX_SLOTS, rows.size());
                for (int i = 0; i < end; i++) {
                    PlatinenDatabaseHelper.BeschriftungTastenRow r = rows.get(i);
                    String c2 = (r.c2 != null ? r.c2 : "").trim();
                    if ("leer".equalsIgnoreCase(c2) || "NULL".equals(c2) || "null".equals(c2)) {
                        relaisNumber[i] = 0;
                        hammerZeit[i] = 0;
                        buttonId[i] = "null";
                        continue;
                    }
                    String c1 = (r.c1 != null ? r.c1 : "").trim();
                    if (c2.isEmpty() && !isSystemTasteRow(r)) {
                        relaisNumber[i] = 0;
                        hammerZeit[i] = 0;
                        buttonId[i] = "null";
                        continue;
                    }
                    String c3 = (r.c3 != null ? r.c3 : "").trim();
                    String c5 = (r.c5 != null ? r.c5 : "1").trim();
                    int relNumber = 0;
                    if (r.sonderId != null) {
                        relNumber = r.sonderId;
                    } else if (!c3.isEmpty() && !"null".equalsIgnoreCase(c3) && !"leer".equalsIgnoreCase(c3)) {
                        try {
                            relNumber = Integer.parseInt(c3);
                        } catch (NumberFormatException e) {
                            relNumber = 0;
                        }
                    }
                    int platNumber = 1;
                    if (!c5.isEmpty() && !"null".equalsIgnoreCase(c5) && !"leer".equalsIgnoreCase(c5)) {
                        try {
                            platNumber = (int) (Double.parseDouble(c5));
                            if (platNumber < 1) platNumber = 1;
                        } catch (NumberFormatException e) {
                            platNumber = 1;
                        }
                    }
                    relaisNumber[i] = relNumber + ((platNumber - 1) * 32);
                    buttonId[i] = r.c13 != null ? r.c13 : "null";
                    int hammertemp = 0;
                    String c4 = r.c4 != null ? r.c4 : "";
                    if (!c4.equals("null") && !c4.equals("NULL") && !c4.trim().isEmpty() && !"leer".equalsIgnoreCase(c4.trim())) {
                        try {
                            hammertemp = (int) (Double.parseDouble(c4.trim()) * 10);
                        } catch (NumberFormatException e) {
                            hammertemp = 0;
                        }
                    }
                    hammerZeit[i] = hammertemp;
                    if (hammerZeit[i] > 0) {
                        StaticVariable.schlagWerkVariable1 = hammerZeit[i];
                    }
                }
            }
            // Verknüpfte-Tasten-Namen für programmOkFromDatabase / UhrThread (auch im Web-UI-Modus ohne Seite1Layout)
            UhrThread.verknuepfteTastenString.clear();
            if (rows != null && !rows.isEmpty()) {
                int verknuepftIndex = 0;
                for (PlatinenDatabaseHelper.BeschriftungTastenRow r : rows) {
                    String c1 = r.c1 != null ? r.c1.trim() : "";
                    if ("verknuepft".equalsIgnoreCase(c1) || "Verknüpft".equals(c1)) {
                        String name = r.c2 != null ? r.c2.trim() : "";
                        if (name.isEmpty()) name = "Verknüpft " + verknuepftIndex;
                        UhrThread.verknuepfteTastenString.add(name);
                        verknuepftIndex++;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(sourceFileName, "Relais aus DB (beschriftung_tasten) befüllen fehlgeschlagen", e);
        }
    }

    /** Prüft, ob eine Beschriftung-Zeile eine Systemtaste ist (Stop, Automatik, Verknüpft, …). Für initRelais Seite-2. */
    private boolean isSystemTasteRow(PlatinenDatabaseHelper.BeschriftungTastenRow r) {
        if (r == null) return false;
        String c1 = (r.c1 != null ? r.c1 : "").trim();
        return "Stop".equalsIgnoreCase(c1) || "Automatik".equalsIgnoreCase(c1)
                || "Verknüpft".equals(c1) || "verknuepft".equalsIgnoreCase(c1)
                || "Sofort Start".equalsIgnoreCase(c1) || "Hammer".equalsIgnoreCase(c1)
                || "Zweite Seite".equalsIgnoreCase(c1) || "Programmeingeben".equalsIgnoreCase(c1)
                || "Nebenuhr Stellen".equalsIgnoreCase(c1);
    }

    private int checkBluetooth() // return 1 = O.K.
    // 2 = keine bluetooth hardware
    // unterstuetzung
    // 3 = keine verbindung

    {
        BluetoothSerial_io btSerial = new BluetoothSerial_io();
        if (btSerial.adapterOk()) {
            if (!btSerial.isEnabled()) {
                //Intent enableBtIntent = new Intent(
                //        BluetoothAdapter.ACTION_REQUEST_ENABLE);
                //startActivityForResult(enableBtIntent, 1);

            }
        }

        StaticVariable.bluetoothStatus = 0;

        CheckBlueToothThread check_bluetooth = new CheckBlueToothThread();
        check_bluetooth.start();

        while (StaticVariable.bluetoothStatus == 0) {
            // warten auf Ergebnis von check_bluetooth

        }

        return StaticVariable.bluetoothStatus;
    }

    private int checkCarambola() {

        // myWifiManager.setWifiEnabled(false);
        wifiOk = false;

        // zur Sicherheit  wifi einschalten 25.4.2013

        if (!StaticVariable.bluetoothMode)
            myWifiManager.setWifiEnabled(true);

        if (!StaticVariable.bluetoothMode) {
            while (wifiOk == false) {
                int tempWifiState = myWifiManager.getWifiState();
                if (tempWifiState == WifiManager.WIFI_STATE_ENABLED) {
                    wifiOk = true;
                }
            }
        }
        StaticVariable.carambolaStatus = -1;
        CheckCarambolaThread checkCarambolaThread = new CheckCarambolaThread();
        checkCarambolaThread.start();

        while (StaticVariable.carambolaStatus == -1) {
            // warten bis thread fertig ist
        }

        return StaticVariable.carambolaStatus;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) // frist speicher ??
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (StaticConstants.DEBUG) {
                // Log.i("key", "back");
            }
            // Zuerst Schoner ausblenden, damit die aktuelle Layoutseite sichtbar ist (nicht schwarz), dann Passwort-Dialog
            if (screensaverOverlay != null && screensaverOverlay.getVisibility() == View.VISIBLE) {
                dismissScreensaverOverlay();
                bringMainContentToFront();
            }
            checkPasswordAndExit();
            return true;
        }

        if ((keyCode == KeyEvent.KEYCODE_HOME)) {
            // Log.i("key", "home");
            allRelaisOffNeu();
            return true;
        }
        // Beliebige Taste (z. B. E): Schoner-Overlay ausblenden, dann Layout sichtbar – ohne Sperrbildschirm (setShowWhenLocked/setTurnScreenOn)
        if (screensaverOverlay != null && screensaverOverlay.getVisibility() == View.VISIBLE) {
            dismissScreensaverOverlay();
            bringMainContentToFront();
            return true;
        }
        return true;

    }

    private void beendenUndReboot() {
        stopService(new Intent(this, StartTurmtechnikService.class));
        savePowerOffWithPassword(true);

        saveBenutzerTastenAndDateAndTime();

        //stopService(new Intent(this,StartTurmtechnikService.class)) ;
        //Log.e("SERVICE" , "GESTOPPT!") ;

        triggerBigClockTimer();
        killFashionClock(); // No-Op (Fashion Clock entfernt)

        //if ((StaticVariable.bt_io_ok || StaticVariable.carambola_io_ok)
        //		&& file_ok) {
        if (file_ok) {
            saveNebenuhr();
            saveVerknuepfteTasten_and_automaticTaste();

            if (serial_iothread != null) {
                allRelaisOffInternetNeu();
                //serial_iothread.endSerialThread();
                StaticVariable.serial_io_ThreadsRun = false;
            }
        }
        if (file_ok) {


            // google_Drive_Update_Thread.end();
            // googleDriveThread.end();
            // 4.8.13 hier den MyUiThread beenden !!!!
        }

        // layout = null; // gibt 5 MB frei!!  das stuerzt ab wegen infoThread
        doRun = false;
        doRunInternetThread = false;
        StaticVariable.ddpThreadRun = false;
        // StaticVariable.sntpThreadRun = false; // Nicht mehr benötigt, TimeSyncThread verwendet doRun-Flag

        //StaticVariable.nmeaThreadRun = false ;
        if (nmea_gps_clock != null) {
            nmea_gps_clock.stopNmea();
            nmea_gps_clock.endNmeaThread();
        }
        // avrnetiothread = null ;

        if (uhr_thread != null) {
            uhr_thread.endUhrThread();
            uhr_thread = null;
        }

        if (ausgangHeizungThreadNew2 != null) {
            ausgangHeizungThreadNew2.endHeizungThread();
            ausgangHeizungThreadNew2 = null;
        }

        rebootSU();
    }


    private void beenden()
    {
        stopService(new Intent(this, StartTurmtechnikService.class));
        //StaticVariable.ausgeschaltetMitPasswort = true ;
        savePowerOffWithPassword(true);
        //Log.e("SERVICE" , "GESTOPPT!") ;
        beendenButServiceRun();
    }

    /** Verhindert mehrfaches Ausführen von beendenButServiceRun in kurzer Folge (z. B. mehrfaches onDestroy). */
    private static volatile boolean beendenButServiceRunInProgress = false;

    private void beendenButServiceRun() {
        if (beendenButServiceRunInProgress) {
            return;
        }
        beendenButServiceRunInProgress = true;

        if (!StaticVariable.autostartDerApp) {
            stopService(new Intent(this, StartTurmtechnikService.class));
            savePowerOffWithPassword(true);
        }

        saveBenutzerTastenAndDateAndTime();

        //stopService(new Intent(this,StartTurmtechnikService.class)) ;
        //Log.e("SERVICE" , "GESTOPPT!") ;

        triggerBigClockTimer();
        killFashionClock(); // No-Op (Fashion Clock entfernt)

        //if ((StaticVariable.bt_io_ok || StaticVariable.carambola_io_ok)
        //		&& file_ok) {
        if (file_ok) {
            saveNebenuhr();
            saveVerknuepfteTasten_and_automaticTaste();

            if (serial_iothread != null) {
                allRelaisOffInternetNeu();
                //serial_iothread.endSerialThread();
                StaticVariable.serial_io_ThreadsRun = false;
            }
        }
        if (file_ok) {


            // google_Drive_Update_Thread.end();
            // googleDriveThread.end();
            // 4.8.13 hier den MyUiThread beenden !!!!
        }

        // layout = null; // gibt 5 MB frei!!  das stuerzt ab wegen infoThread
        doRun = false;
        doRunInternetThread = false;
        StaticVariable.ddpThreadRun = false;
        // StaticVariable.sntpThreadRun = false; // Nicht mehr benötigt, TimeSyncThread verwendet doRun-Flag

        //StaticVariable.nmeaThreadRun = false ;
        if (nmea_gps_clock != null) {
            nmea_gps_clock.stopNmea();
            nmea_gps_clock.endNmeaThread();
        }
        // avrnetiothread = null ;

        if (uhr_thread != null) {
            uhr_thread.endUhrThread();
            uhr_thread = null;
        }

        if (ausgangHeizungThreadNew2 != null) {
            ausgangHeizungThreadNew2.endHeizungThread();
            ausgangHeizungThreadNew2 = null;
        }

        System.gc();

        // w1.release(); // bildschirm lok ausschalten

        System.exit(0);

        finish();
    }

    /**
     * Beenden mit Passwort 5644: App darf 15 Minuten im Hintergrund bleiben.
     * Service läuft weiter und holt die App nach 15 Min wieder in den Vordergrund.
     * Kein System.exit(0), Service wird nicht gestoppt.
     */
    private void beendenMit15MinHintergrund() {
        saveBackgroundAllowedUntilMillis(System.currentTimeMillis() + BACKGROUND_ALLOWED_MINUTES * 60L * 1000L);

        saveBenutzerTastenAndDateAndTime();
        triggerBigClockTimer();
        killFashionClock(); // No-Op (Fashion Clock entfernt)

        if (file_ok) {
            saveNebenuhr();
            saveVerknuepfteTasten_and_automaticTaste();
            if (serial_iothread != null) {
                allRelaisOffInternetNeu();
                StaticVariable.serial_io_ThreadsRun = false;
            }
        }

        doRun = false;
        doRunInternetThread = false;
        StaticVariable.ddpThreadRun = false;
        if (nmea_gps_clock != null) {
            nmea_gps_clock.stopNmea();
            nmea_gps_clock.endNmeaThread();
        }
        if (uhr_thread != null) {
            uhr_thread.endUhrThread();
            uhr_thread = null;
        }
        if (ausgangHeizungThreadNew2 != null) {
            ausgangHeizungThreadNew2.endHeizungThread();
            ausgangHeizungThreadNew2 = null;
        }

        System.gc();
        finish();
    }

    //private void startHomeActivity()
    //{
    //	Intent startHome = new Intent(Intent.ACTION_MAIN) ;
    //	startHome.addCategory(Intent.CATEGORY_HOME) ;
    //	startHome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)  ;
    //	startActivity(startHome);
    //}


    /**
     * Startet die App neu (von Web-Config aus aufrufbar).
     * Führt restartTurmtechnik() auf dem UI-Thread aus.
     */
    public static void requestAppRestart() {
        final TurmtechnikActivity act = turmtechnikActivityInstance;
        if (act != null) {
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    act.restartTurmtechnik();
                }
            });
        }
    }

    /**
     * Öffnet den System-Install-Dialog für eine APK-Datei (von Web-UI „APK hochladen“).
     * Muss auf dem UI-Thread laufen; wird von ConfigWebServer nach APK-Upload aufgerufen.
     * Auf dem Gerät muss jemand „Installieren“ bestätigen.
     */
    public static void requestOpenInstallDialog(final java.io.File apkFile) {
        final TurmtechnikActivity act = turmtechnikActivityInstance;
        if (act == null || apkFile == null || !apkFile.exists() || !apkFile.canRead()) {
            return;
        }
        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Uri uri = FileProvider.getUriForFile(act, act.getPackageName() + ".fileprovider", apkFile);
                        intent.setDataAndType(uri, "application/vnd.android.package-archive");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } else {
                        intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
                    }
                    act.startActivity(intent);
                } catch (Exception e) {
                    android.util.Log.e("TurmtechnikActivity", "Install-Dialog öffnen fehlgeschlagen", e);
                }
            }
        });
    }

    /** RequestCode für den Einmal-Alarm „App direkt wieder öffnen nach Exit für Einstellungen“ (anders als 0 = 15-Min-Alarm). */
    private static final int PENDING_INTENT_REQUEST_WAKE_AFTER_EXIT = 123;

    /**
     * App beenden für Einstellungen: Für die nächsten {@code minutes} Minuten startet die App nicht automatisch;
     * bei erneutem Start (z. B. HOME-Taste) wird direkt die System-Einstellungen geöffnet.
     * Service und 15-Min-Alarm laufen weiter. Ein Einmal-Alarm direkt nach Ablauf startet die Activity wieder über
     * ein Activity-PendingIntent, damit Android den Rückweg nicht als verbotenen Background-Start blockiert.
     */
    public static void requestAppExitForSettings(final int minutes) {
        final TurmtechnikActivity act = turmtechnikActivityInstance;
        if (act == null) return;
        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                android.content.Context ctx = act.getApplicationContext();
                long until = System.currentTimeMillis() + Math.max(1, minutes) * 60 * 1000L;
                setExitForSettingsUntilMillis(ctx, until);
                // Einmal-Alarm: direkt nach Ablauf der Phase die Activity wieder öffnen.
                try {
                    AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
                    if (am != null) {
                        Intent restartIntent = StartTurmtechnikService.createTurmtechnikLaunchIntent(ctx);
                        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            flags |= PendingIntent.FLAG_IMMUTABLE;
                        }
                        PendingIntent pi = PendingIntent.getActivity(ctx, PENDING_INTENT_REQUEST_WAKE_AFTER_EXIT, restartIntent, flags);
                        long triggerAt = until + 2000L; // 2 s nach Ende der Phase
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                        } else {
                            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                        }
                        android.util.Log.i("TurmtechnikActivity", "Exit für Einstellungen geplant bis " + until + ", direkter Activity-Restart um " + triggerAt);
                    }
                } catch (Exception e) {
                    android.util.Log.w("TurmtechnikActivity", "Einmal-Alarm nach Exit für Einstellungen: " + (e != null ? e.getMessage() : ""));
                }
                act.finishAffinity();
            }
        });
    }

    private void restartTurmtechnik() {

        //if ((StaticVariable.bt_io_ok || StaticVariable.carambola_io_ok)
        //		&& file_ok) {
        if (file_ok) {
            saveNebenuhr();
            saveVerknuepfteTasten_and_automaticTaste();

            if (serial_iothread != null) {
                allRelaisOffInternetNeu();
                //serial_iothread.endSerialThread();
                StaticVariable.serial_io_ThreadsRun = false;
            }
        }
        if (file_ok) {
            if (uhr_thread != null) {
                uhr_thread.endUhrThread();
                uhr_thread = null;

                // google_Drive_Update_Thread.end();
                // googleDriveThread.end();
                // 4.8.13 hier den MyUiThread beenden !!!!
            }

            //layout = null; // gibt 5 MB frei!!
            doRun = false;
            doRunInternetThread = false;

            // service nicht beenden -- muss turmtechnik neu starten!!!
            //stopService(new Intent(this,StartTurmtechnikService.class)) ;

            // avrnetiothread = null ;

        }

        // avrnetiothread = null ;

        System.gc();

        // w1.release(); // bildschirm lok ausschalten

        System.exit(0);

        finish();

    }


    String stringNameNebenuhr = "nebenuhr.txt";
    /** Log-Datei für Start-Check: gespeicherte Werte vs. geladene Werte (Nebenuhr etc.). */
    private static final String START_WERTE_CHECK_LOG = "start_werte_check.log";

    /** io_config-Key: Web-UI im Vollbild beim App-Start anzeigen (Testmodus). "1" = an, sonst aus. */
    public static final String CONFIG_WEB_UI_VOLLBILD_TEST = "web_ui_vollbild_test";

    public SpreadSheet spreadSheet = null;
    private static int internetPolling;

    public static int getServerPingTime() {
        return internetPolling;
    }

    /**
     * Startet den Web-Server für die Konfiguration.
     */
    private void startConfigWebServer() {
        try {
            if (configWebServer == null) {
                configWebServer = new ConfigWebServer(8080, getApplicationContext());
                configWebServer.start();
                Log.i("ConfigWebServer", "Web-Server gestartet auf Port 8080");
                // IP wie beim Server-Bind (WLAN zuerst), für Anzeige und Erreichbarkeit
                String ipAddress = getWifiIpAddressStatic(this);
                if (ipAddress == null) ipAddress = getLocalIpAddress();
                webServerIpAddress = ipAddress;
                if (ipAddress != null) {
                    Log.i("ConfigWebServer", "Web-UI erreichbar unter: http://" + ipAddress + ":8080");
                    Log.i("ConfigWebServer", "Oder lokal: http://localhost:8080");
                } else {
                    Log.w("ConfigWebServer", "IP-Adresse konnte nicht ermittelt werden (WLAN später prüfen)");
                }
            }
        } catch (IOException e) {
            Log.e("ConfigWebServer", "Fehler beim Starten des Web-Servers", e);
        }
    }

    /**
     * Stellt sicher, dass der Web-Server läuft (z. B. wenn WebUiActivity nach Prozess-Neustart zuerst geöffnet wird).
     * Kann von WebUiActivity aufgerufen werden.
     */
    /**
     * Lädt den Platinen-Demo-Modus aus der DB in StaticVariable (wird vor Serial_IoThread-Start benötigt).
     */
    public static void loadPlatinenDemoModusFromDb() {
        if (turmtechnikContext == null) return;
        try {
            PlatinenDatabaseHelper db = PlatinenDatabaseHelper.getInstance(turmtechnikContext);
            StaticVariable.platinenDemoModus = db.getPlatinenDemoModus();
        } catch (Exception e) {
            android.util.Log.e("TurmtechnikActivity", "loadPlatinenDemoModusFromDb", e);
        }
    }

    /**
     * Lädt ipList und portList aus der Platinen-Datenbank (nur Datenbank, kein System.xls mehr).
     * Wird beim Start (vor Serial_IoThread) und nach Speichern in der Platinen-Web-UI aufgerufen.
     */
    public static void loadPlatinenIpListFromDb() {
        if (turmtechnikContext == null) return;
        PlatinenDatabaseHelper db = PlatinenDatabaseHelper.getInstance(turmtechnikContext);
        loadPlatinenDemoModusFromDb();
        StaticVariable.ipList.clear();
        StaticVariable.portList.clear();
        List<Platine> platinen = db.getAllPlatinen();
        java.util.Set<String> addedKeys = new java.util.HashSet<>();
        for (Platine p : platinen) {
            if (p.aktiv && p.ipAdresse != null && !p.ipAdresse.trim().isEmpty() && !p.ipAdresse.equals("0")) {
                String ip = p.ipAdresse.trim();
                int port = p.port;
                if (port < 1 || port > 65535) port = 50210;
                String key = ip + ":" + port;
                if (!addedKeys.contains(key)) {
                    addedKeys.add(key);
                    StaticVariable.ipList.add(ip);
                    StaticVariable.portList.add(port);
                }
            }
        }
        android.util.Log.d("TurmtechnikActivity", "loadPlatinenIpListFromDb: ipList.size=" + StaticVariable.ipList.size());
    }

    /** Stoppt den laufenden Serial_IoThread und startet einen neuen (z. B. nach Platinen-Config-Änderung). */
    private static void restartSerialIoThread() {
        StaticVariable.serial_io_ThreadsRun = false;
        if (serial_iothread != null && serial_iothread.isAlive()) {
            try {
                serial_iothread.join(2500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // Warten, bis alte Carambola-Threads das Flag sehen und beenden (verhindert „Keine Antwort“ und „Impuls wird nicht gezählt“ durch Doppel-Threads)
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        serial_iothread = new Serial_IoThread();
        serial_iothread.start();
    }

    /**
     * Wird aufgerufen, wenn in der Web-UI Platinen-Konfiguration oder Modus gespeichert wurde.
     * Lädt ipList/portList aus der DB und startet den Serial_IoThread neu, damit WLAN-Steuerung die neuen Platinen nutzt.
     */
    public static void onPlatinenConfigSaved() {
        if (context == null || !(context instanceof Activity)) return;
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadPlatinenIpListFromDb();
                restartSerialIoThread();
            }
        });
    }

    public static void ensureWebServerStarted(android.content.Context context) {
        if (context == null) return;
        try {
            if (configWebServer == null) {
                configWebServer = new ConfigWebServer(8080, context.getApplicationContext());
                configWebServer.start();
                Log.i("ConfigWebServer", "Web-Server gestartet auf Port 8080");
                String ipAddress = getWifiIpAddressStatic(context);
                if (ipAddress == null) ipAddress = getLocalIpAddressStatic();
                if (ipAddress != null) {
                    webServerIpAddress = ipAddress;
                    Log.i("ConfigWebServer", "Web-UI erreichbar unter: http://" + ipAddress + ":8080");
                }
            }
        } catch (Exception e) {
            Log.e("ConfigWebServer", "Fehler beim Starten des Web-Servers", e);
        }
    }

    /**
     * Stoppt den Web-Server.
     */
    private void stopConfigWebServer() {
        if (configWebServer != null) {
            configWebServer.stop();
            configWebServer = null;
            Log.i("ConfigWebServer", "Web-Server gestoppt");
        }
    }

    /**
     * Gibt die IP-Adresse des Web-Servers zurück (wie beim Server-Bind, damit Anzeige zur Erreichbarkeit passt).
     * Wenn beim Start noch null (z. B. WLAN nicht bereit), wird beim Abruf erneut ermittelt.
     */
    public static String getWebServerIpAddress() {
        if (webServerIpAddress != null && !webServerIpAddress.isEmpty()) {
            return webServerIpAddress;
        }
        if (turmtechnikContext != null) {
            String ip = getWifiIpAddressStatic(turmtechnikContext);
            if (ip != null) {
                webServerIpAddress = ip;
                return ip;
            }
            ip = getLocalIpAddressStatic();
            if (ip != null) {
                webServerIpAddress = ip;
                return ip;
            }
        }
        return webServerIpAddress;
    }

    /**
     * Ermittelt die lokale IP-Adresse des Geräts (z. B. für Web-UI im LAN).
     * Bevorzugt Schnittstellen, die „up“ sind (z. B. WLAN) – relevant für Android 5.
     */
    private String getLocalIpAddress() {
        return getLocalIpAddressStatic();
    }

    /** Statische Variante für getLocalIpAddress (für getWebServerIpAddress). */
    private static String getLocalIpAddressStatic() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces =
                java.net.NetworkInterface.getNetworkInterfaces();
            String fallback = null;
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface ni = interfaces.nextElement();
                boolean up = false;
                try { up = ni.isUp(); } catch (Exception ignored) { }
                java.util.Enumeration<java.net.InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address instanceof java.net.Inet4Address) {
                        String ip = address.getHostAddress();
                        if (fallback == null) fallback = ip;
                        if (up) return ip;
                    }
                }
            }
            if (fallback != null) return fallback;
        } catch (Exception e) {
            Log.e("ConfigWebServer", "Fehler beim Ermitteln der IP-Adresse", e);
        }
        return null;
    }

    /**
     * Liefert die vollständige Web-UI-URL für den optionalen Pfad (z. B. für WebUiActivity).
     * @param path Optionaler Pfad, z. B. "/benutzerprogramme.html". Null oder leer = Startseite.
     */
    public static String getWebUiUrlForPath(String path) {
        String ip = getLocalIpAddressStatic();
        String base = (ip != null && !ip.isEmpty()) ? ("http://" + ip + ":8080") : "http://127.0.0.1:8080";
        return (path != null && !path.isEmpty()) ? (base + (path.startsWith("/") ? path : "/" + path)) : base;
    }

    /**
     * Ermittelt die WLAN-IP (wie beim Server-Bind). Damit Anzeige und Server dieselbe Adresse nutzen.
     */
    private static String getWifiIpAddressStatic(Context context) {
        if (context == null) return null;
        try {
            Object service = context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (service == null) return null;
            android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) service;
            android.net.wifi.WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo == null) return null;
            int ip = wifiInfo.getIpAddress();
            if (ip == 0) return null;
            return (ip & 0xff) + "." + ((ip >> 8) & 0xff) + "." + ((ip >> 16) & 0xff) + "." + ((ip >> 24) & 0xff);
        } catch (Exception e) {
            Log.w("ConfigWebServer", "WLAN-IP konnte nicht ermittelt werden", e);
            return null;
        }
    }

    /** Dialog-Fenster oben platzieren und bei geöffneter Tastatur sichtbar halten (nicht überdecken). Von SetNebenuhrActivity genutzt. */
    public static void applyDialogAboveKeyboard(android.app.AlertDialog dialog) {
        if (dialog == null) return;
        Window w = dialog.getWindow();
        if (w != null) {
            w.setGravity(Gravity.TOP);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    /**
     * Dialog: Passwort abfragen, dann .db-Dateien aus dem Turmtechnik-Ordner zur Auswahl anzeigen und Anlage importieren.
     * Wird nach 5 Sekunden Logo-Druck aufgerufen.
     */
    private void showAnlageImportWithPasswordDialog() {
        final EditText passwordInput = new EditText(this);
        passwordInput.setHint(getString(R.string.anlage_import_passwort_hinweis));
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.anlage_import_dialog_titel)
                .setMessage(R.string.anlage_import_passwort_hinweis)
                .setView(passwordInput)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String entered = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";
                        String expected = getString(R.string.anlage_import_passwort);
                        if (!expected.equals(entered)) {
                            Toast.makeText(TurmtechnikActivity.this, getString(R.string.anlage_import_fehler), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        showAnlageAdminMenu();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        applyDialogAboveKeyboard(dialog);
        dialog.show();
    }

    /** Menü nach Passworteingabe: Import / Export / Web-UI / Debloat-Skript. */
    private void showAnlageAdminMenu() {
        final String[] items = new String[]{
                getString(R.string.anlage_menue_import),
                getString(R.string.anlage_menue_export),
                getString(R.string.anlage_menue_webui),
                getString(R.string.anlage_menue_debloat)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.anlage_menue_titel)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            showAnlageImportFilePicker();
                        } else if (which == 1) {
                            doAnlageExport();
                        } else if (which == 2) {
                            openWebUiInBrowser();
                        } else if (which == 3) {
                            startActivity(new Intent(TurmtechnikActivity.this, DebloatHelperActivity.class));
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Öffnet die Web-UI im Vollbild (WebUiActivity mit Zurück-Taste). Optionaler Pfad z. B. /benutzerprogramme.html. */
    private void openWebUiInBrowser() {
        openWebUiInBrowser(this, null);
    }

    /**
     * Öffnet die Web-UI im Vollbild in einer in-app WebView (WebUiActivity) mit Zurück-Taste.
     * @param context Activity-Kontext (für startActivity / Toast)
     * @param path Optionaler Pfad, z. B. "/benutzerprogramme.html". Null oder leer = Startseite.
     */
    public static void openWebUiInBrowser(android.content.Context context, String path) {
        if (context == null) return;
        try {
            Intent intent = new Intent(context, WebUiActivity.class);
            if (path != null && !path.isEmpty()) {
                intent.putExtra(WebUiActivity.EXTRA_PATH, path);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e("WebUI", "Web-UI konnte nicht geöffnet werden", e);
            Toast.makeText(context, "Web-UI konnte nicht geöffnet werden.", Toast.LENGTH_LONG).show();
        }
    }

    /** Exportiert die aktuelle Datenbank als Backup in den Turmtechnik-Ordner. */
    private void doAnlageExport() {
        try {
            File dbFile = getDatabasePath("turmtechnik_config.db");
            if (!dbFile.exists()) {
                Toast.makeText(this, getString(R.string.anlage_export_fehler), Toast.LENGTH_SHORT).show();
                return;
            }
            String basePath = sdCardPath != null ? sdCardPath : Environment.getExternalStorageDirectory().getPath();
            File turmtechnikDir = new File(basePath + "/Turmtechnik");
            File backupDir = new File(turmtechnikDir, "Backup");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            String datePart = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.GERMANY).format(new Date());
            String exportName = "turmtechnik_config_Export_" + datePart + ".db";
            File destFile = new File(backupDir, exportName);
            java.io.InputStream in = new FileInputStream(dbFile);
            java.io.OutputStream out = new FileOutputStream(destFile);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            Toast.makeText(this, getString(R.string.anlage_export_erfolg) + " " + destFile.getName(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e("AnlageExport", "Export fehlgeschlagen", e);
            Toast.makeText(this, getString(R.string.anlage_export_fehler) + " " + (e.getMessage() != null ? e.getMessage() : ""), Toast.LENGTH_LONG).show();
        }
    }

    /** Sammelt .db-Dateien im Turmtechnik-Ordner (und Unterordner Backup) und zeigt Auswahl-Dialog. */
    private void showAnlageImportFilePicker() {
        String basePath = sdCardPath != null ? sdCardPath : Environment.getExternalStorageDirectory().getPath();
        final java.util.List<File> dbFiles = new ArrayList<>();
        File turmtechnikDir = new File(basePath + "/Turmtechnik");
        if (turmtechnikDir.exists() && turmtechnikDir.isDirectory()) {
            File[] files = turmtechnikDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && f.getName().toLowerCase().endsWith(".db")) {
                        dbFiles.add(f);
                    }
                }
            }
            File backupDir = new File(turmtechnikDir, "Backup");
            if (backupDir.exists() && backupDir.isDirectory()) {
                File[] backupFiles = backupDir.listFiles();
                if (backupFiles != null) {
                    for (File f : backupFiles) {
                        if (f.isFile() && f.getName().toLowerCase().endsWith(".db")) {
                            dbFiles.add(f);
                        }
                    }
                }
            }
        }
        if (dbFiles.isEmpty()) {
            Toast.makeText(this, getString(R.string.anlage_import_keine_dateien), Toast.LENGTH_LONG).show();
            return;
        }
        final String[] items = new String[dbFiles.size()];
        for (int i = 0; i < dbFiles.size(); i++) {
            items[i] = dbFiles.get(i).getName();
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.anlage_import_datei_auswaehlen)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final File selected = dbFiles.get(which);
                        new AlertDialog.Builder(TurmtechnikActivity.this)
                                .setTitle(R.string.anlage_import_dialog_titel)
                                .setMessage("Anlage aus \"" + selected.getName() + "\" importieren? App wird neu gestartet.")
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface d2, int w2) {
                                        doAnlageImport(selected);
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, null)
                                .show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void doAnlageImport(File sourceFile) {
        try {
            PlatinenDatabaseHelper.closeInstanceForRestore();
            File dbFile = getDatabasePath("turmtechnik_config.db");
            java.io.InputStream in = new java.io.FileInputStream(sourceFile);
            java.io.OutputStream out = new FileOutputStream(dbFile);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            Toast.makeText(this, getString(R.string.anlage_import_erfolg), Toast.LENGTH_LONG).show();
            // App neu starten
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        } catch (Exception e) {
            Log.e("AnlageImport", "Import fehlgeschlagen", e);
            Toast.makeText(this, getString(R.string.anlage_import_fehler) + " " + (e.getMessage() != null ? e.getMessage() : ""), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Ermittelt den zu speichernden angezeigte_zeit-Wert für die Minutenspeicherung.
     * Verhindert, dass der Fortschritt des Impuls-Threads (Aufholen) durch eine ältere Lesung von StaticVariable überschrieben wird.
     * Siehe docs/NEBENUHR_ARCHITEKTUR.md Abschnitt „Schreibzugriffe und Races“.
     */
    private static int nebenuhrEffectiveAngezeigteZeitForSave(int staticVarZeit, int dbZeit) {
        if (staticVarZeit < dbZeit && dbZeit - staticVarZeit <= 30) {
            return dbZeit; // DB ist wenige Minuten voraus (Impuls-Thread war schneller)
        }
        return staticVarZeit;
    }

    public void saveNebenuhr() {
        //Log.e("nebenUhrSave", "StaticVariable.uhrA=" + StaticVariable.uhrA_angezeigteZeit);
        //Log.e("nebenUhrSave", "StaticVariable.uhrB=" + StaticVariable.uhrB_angezeigteZeit);
        //Log.e("nebenUhrSave", "StaticVariable.uhrB=" + StaticVariable.uhrC_angezeigteZeit);

        String uhrA = String.valueOf(StaticVariable.uhrA_angezeigteZeit) + "\n";
        String uhrB = String.valueOf(StaticVariable.uhrB_angezeigteZeit) + "\n";
        String uhrC = String.valueOf(StaticVariable.uhrC_angezeigteZeit) + "\n";

        //Log.e("nebenuhrSave", "String uhrA=" + uhrA + "B=" + uhrB + "C=" + uhrC);

        new LogTurmtechnik2("save Nebenuhr", 0, 0, 0, 0);

        int uhrTemp = StaticVariable.uhrA_angezeigteZeit | StaticVariable.uhrB_angezeigteZeit | StaticVariable.uhrC_angezeigteZeit;

        if (uhrTemp != 0) {
            try {
                FileOutputStream out = this.openFileOutput(stringNameNebenuhr,
                        MODE_PRIVATE);
                OutputStreamWriter writer = new OutputStreamWriter(out);

                writer.write(uhrA);
                writer.write(uhrB);
                writer.write(uhrC);
                writer.flush();
                writer.close();
            } catch (FileNotFoundException fnfe) {
                // was tuen?
            } catch (IOException ioe) {
                // was tuen?
            }

            // last_relais_a für jede Nebenuhr (A, B, C) extra in der DB speichern
            // Hinweis: Minutenspeicherung – schreibt aktuellen Stand, nicht „nach Impuls“;
            // angezeigte_zeit: siehe nebenuhrEffectiveAngezeigteZeitForSave (Races vermeiden).
            try {
                android.util.Log.d("nebenuhrSave", "Minutenspeicherung: aktueller last_relais_a-Stand (A=" + StaticVariable.uhrA_lastRelaisA + " B=" + StaticVariable.uhrB_lastRelaisA + " C=" + StaticVariable.uhrC_lastRelaisA + ")");
                PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(turmtechnikContext);
                for (int zeile = 3; zeile <= 5; zeile++) {
                    PlatinenDatabaseHelper.NebenuhrConfig config = dbHelper.getNebenuhrByZeile(zeile);
                    if (config != null) {
                        int svZeit;
                        boolean svLastRelaisA;
                        if (zeile == 3) {
                            svZeit = StaticVariable.uhrA_angezeigteZeit;
                            svLastRelaisA = StaticVariable.uhrA_lastRelaisA;
                        } else if (zeile == 4) {
                            svZeit = StaticVariable.uhrB_angezeigteZeit;
                            svLastRelaisA = StaticVariable.uhrB_lastRelaisA;
                        } else {
                            svZeit = StaticVariable.uhrC_angezeigteZeit;
                            svLastRelaisA = StaticVariable.uhrC_lastRelaisA;
                        }
                        config.angezeigteZeit = nebenuhrEffectiveAngezeigteZeitForSave(svZeit, config.angezeigteZeit);
                        config.lastRelaisA = svLastRelaisA;
                        dbHelper.saveNebenuhr(config);
                    }
                }
            } catch (Exception e) {
                Log.e("nebenuhrSave", "DB last_relais_a: " + e.getMessage(), e);
            }
        } else {
            Log.e("nebenuhrSave", "nicht gespeichert");
        }
    }

    /**
     * Lädt Nebenuhr-Status (ob Datei vorhanden) und die eigentlichen Werte ausschließlich aus der DB.
     * Kein Fallback auf die Datei für angezeigteZeit/lastRelaisA – verhindert veraltetes „Ist“ und zu viele Nachhol-Impulse.
     */
    public boolean loadNebenuhr() {
        boolean nebenuhrStatus = true;
        try {
            FileInputStream in = this.openFileInput(stringNameNebenuhr);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String uhrA = reader.readLine();
            String uhrB = reader.readLine();
            String uhrC = reader.readLine();
            reader.close();
            if (uhrA == null) nebenuhrStatus = false;
            if (uhrB == null) nebenuhrStatus = false;
            if (uhrC == null) nebenuhrStatus = false;
            Log.e("nebenuhrLoad", "Datei vorhanden, uhrA=" + uhrA + " uhrB=" + uhrB + " uhrC=" + uhrC);
        } catch (FileNotFoundException fnfe) {
            // Datei fehlt (z. B. erster Start): Werte nur aus DB
            Log.e("nebenuhrLoad", "Datei fehlt, lade nur aus DB");
            nebenuhrStatus = false;
        } catch (IOException ioe) {
            Log.e("nebenuhrLoad", "Datei lesen: " + (ioe != null ? ioe.getMessage() : ""));
        }

        // AngezeigteZeit und lastRelaisA ausschließlich aus DB – kein Fallback auf Datei
        loadNebenuhrLastRelaisAndAnzeigeFromDb();

        Log.e("nebenUhrLoad", "StaticVariable.uhrA=" + StaticVariable.uhrA_angezeigteZeit);
        Log.e("nebenUhrLoad", "StaticVariable.uhrB=" + StaticVariable.uhrB_angezeigteZeit);
        Log.e("nebenUhrLoad", "StaticVariable.uhrC=" + StaticVariable.uhrC_angezeigteZeit);
        new LogTurmtechnik2("load Nebenuhr", 0, 0, 0, 0);
        return nebenuhrStatus;
    }

    /** Aktuelle Echtzeit in Minuten (24h, 0–1439). */
    private static int getCurrentCalendarMinuten24() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
    }

    /** Aktuelle Echtzeit in Minuten (12h, 0–719). */
    private static int getCurrentCalendarMinuten12() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.HOUR) * 60 + cal.get(Calendar.MINUTE);
    }

    /**
     * Lädt last_relais_a und angezeigte_zeit (bzw. mondphaseIst für D) für Nebenuhr A, B, C und D ausschließlich aus der DB.
     * Soll (calendarZeit) wird sofort auf aktuelle RTC gesetzt.
     * Wenn für eine Uhr kein DB-Eintrag existiert: Warnung ins Logfile + Log.w, sicherer Init (A/B/C: Ist=RTC; D: Phase=0).
     */
    private void loadNebenuhrLastRelaisAndAnzeigeFromDb() {
        try {
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(turmtechnikContext);
            int min12 = getCurrentCalendarMinuten12();
            StaticVariable.uhrA_calendarZeit = min12;
            StaticVariable.uhrB_calendarZeit = min12;
            StaticVariable.uhrC_calendarZeit = min12;
            for (int zeile = 3; zeile <= 6; zeile++) {
                String label = (zeile == 3) ? "A" : (zeile == 4) ? "B" : (zeile == 5) ? "C" : "D";
                PlatinenDatabaseHelper.NebenuhrConfig config = dbHelper.getNebenuhrByZeile(zeile);
                if (config == null) {
                    if (zeile <= 5) {
                        Log.w("nebenuhrLoad", "Nebenuhr " + label + ": kein DB-Eintrag – setze Ist auf aktuelle RTC (" + min12 + "), keine Impulsflut");
                        LogTurmtechnik2.appendNebenuhrRelaisLogWarnung(label, "kein DB-Eintrag – Ist auf RTC gesetzt");
                        if (zeile == 3) {
                            StaticVariable.uhrA_angezeigteZeit = min12;
                            StaticVariable.uhrA_lastRelaisA = false;
                        } else if (zeile == 4) {
                            StaticVariable.uhrB_angezeigteZeit = min12;
                            StaticVariable.uhrB_lastRelaisA = false;
                        } else {
                            StaticVariable.uhrC_angezeigteZeit = min12;
                            StaticVariable.uhrC_lastRelaisA = false;
                        }
                    } else {
                        // Monduhr D: dasselbe – nur aus DB, bei fehlendem Eintrag Warnung + sicherer Init
                        Log.w("nebenuhrLoad", "Nebenuhr D (Mond): kein DB-Eintrag – setze Phase auf 0");
                        LogTurmtechnik2.appendNebenuhrRelaisLogWarnung("D", "kein DB-Eintrag – Phase auf 0 gesetzt");
                        StaticVariable.uhrD_mondphaseIst = 0;
                        StaticVariable.uhrD_lastRelaisA = false;
                    }
                    continue;
                }
                if (zeile == 6) {
                    StaticVariable.uhrD_lastRelaisA = config.lastRelaisA;
                    StaticVariable.uhrD_mondphaseIst = config.mondphaseIst;
                    continue;
                }
                int angezeigteZeit = config.angezeigteZeit;
                if (config.impulsAusstehend) {
                    angezeigteZeit = config.angezeigteZeit - 1;
                    if (angezeigteZeit < 0) angezeigteZeit = 719;
                    config.angezeigteZeit = angezeigteZeit;
                    config.impulsAusstehend = false;
                    dbHelper.saveNebenuhr(config);
                }
                if (zeile == 3) {
                    StaticVariable.uhrA_lastRelaisA = config.lastRelaisA;
                    StaticVariable.uhrA_angezeigteZeit = angezeigteZeit;
                } else if (zeile == 4) {
                    StaticVariable.uhrB_lastRelaisA = config.lastRelaisA;
                    StaticVariable.uhrB_angezeigteZeit = angezeigteZeit;
                } else {
                    StaticVariable.uhrC_lastRelaisA = config.lastRelaisA;
                    StaticVariable.uhrC_angezeigteZeit = angezeigteZeit;
                }
            }
            StaticVariable.nebenuhrWaitFirstFullMinute = true;
            capNebenuhrAnzeigeToSollAndSave();
            NebenUhrThread.recomputeWartenLaufen(turmtechnikContext);
        } catch (Exception e) {
            Log.e("nebenuhrLoad", "last_relais_a/angezeigte_zeit aus DB: " + e.getMessage(), e);
        }
    }

    /**
     * Nebenuhr-Anzeige (Ist) wird beim Laden nicht mehr auf Soll gesetzt.
     * Ist darf nur durch einen erfolgreichen Impuls geändert werden – sonst nur Aufholen oder Warten.
     */
    private void capNebenuhrAnzeigeToSollAndSave() {
        // Kein Ist = Soll beim Laden: Wert der Nebenuhren darf nur ein erfolgreicher Impuls verändern.
    }

    /**
     * Hängt eine Zeile mit Zeitstempel an die Start-Check-Logdatei an (zum Testen, ob geladene Werte = gespeicherte).
     */
    private void appendToStartCheckLog(String msg) {
        try {
            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMAN).format(new Date());
            FileOutputStream out = openFileOutput(START_WERTE_CHECK_LOG, MODE_APPEND);
            OutputStreamWriter w = new OutputStreamWriter(out);
            w.write(ts + " " + msg + "\n");
            w.flush();
            w.close();
        } catch (Exception e) {
            Log.e("StartCheckLog", "append fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    /**
     * Prüft beim Start: Stimmen die geladenen Nebenuhr-Werte (StaticVariable) mit den gespeicherten (Datei + DB)?
     * Schreibt das Ergebnis in start_werte_check.log zum Testen.
     */
    private void checkSavedValuesAndLog() {
        appendToStartCheckLog("--- Start Werte-Check ---");
        // Datei nebenuhr.txt erneut lesen (gespeicherte Werte)
        int fileA = 0, fileB = 0, fileC = 0;
        boolean fileOk = true;
        try {
            FileInputStream in = openFileInput(stringNameNebenuhr);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String la = reader.readLine();
            String lb = reader.readLine();
            String lc = reader.readLine();
            reader.close();
            if (la != null) fileA = Integer.parseInt(la.trim());
            else fileOk = false;
            if (lb != null) fileB = Integer.parseInt(lb.trim());
            else fileOk = false;
            if (lc != null) fileC = Integer.parseInt(lc.trim());
            else fileOk = false;
        } catch (FileNotFoundException e) {
            appendToStartCheckLog("Nebenuhr Datei fehlt (nebenuhr.txt)");
            fileOk = false;
        } catch (Exception e) {
            appendToStartCheckLog("Nebenuhr Datei lesen: " + e.getMessage());
            fileOk = false;
        }
        if (fileOk) {
            boolean matchA = (fileA == StaticVariable.uhrA_angezeigteZeit);
            boolean matchB = (fileB == StaticVariable.uhrB_angezeigteZeit);
            boolean matchC = (fileC == StaticVariable.uhrC_angezeigteZeit);
            appendToStartCheckLog("Nebenuhr Datei: A gespeichert=" + fileA + " geladen=" + StaticVariable.uhrA_angezeigteZeit + (matchA ? " OK" : " MISMATCH"));
            appendToStartCheckLog("Nebenuhr Datei: B gespeichert=" + fileB + " geladen=" + StaticVariable.uhrB_angezeigteZeit + (matchB ? " OK" : " MISMATCH"));
            appendToStartCheckLog("Nebenuhr Datei: C gespeichert=" + fileC + " geladen=" + StaticVariable.uhrC_angezeigteZeit + (matchC ? " OK" : " MISMATCH"));
        }
        // DB nebenuhr_config (last_relais_a, angezeigte_zeit) mit StaticVariable vergleichen
        try {
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(turmtechnikContext);
            for (int zeile = 3; zeile <= 5; zeile++) {
                PlatinenDatabaseHelper.NebenuhrConfig config = dbHelper.getNebenuhrByZeile(zeile);
                String label = (zeile == 3) ? "A" : (zeile == 4) ? "B" : "C";
                if (config == null) {
                    appendToStartCheckLog("Nebenuhr DB " + label + ": kein Eintrag");
                    continue;
                }
                int svZeit = (zeile == 3) ? StaticVariable.uhrA_angezeigteZeit : (zeile == 4) ? StaticVariable.uhrB_angezeigteZeit : StaticVariable.uhrC_angezeigteZeit;
                boolean svRelais = (zeile == 3) ? StaticVariable.uhrA_lastRelaisA : (zeile == 4) ? StaticVariable.uhrB_lastRelaisA : StaticVariable.uhrC_lastRelaisA;
                boolean matchZeit = (config.angezeigteZeit == svZeit);
                boolean matchRelais = (config.lastRelaisA == svRelais);
                appendToStartCheckLog("Nebenuhr DB " + label + ": angezeigte_zeit gespeichert=" + config.angezeigteZeit + " geladen=" + svZeit + (matchZeit ? " OK" : " MISMATCH"));
                appendToStartCheckLog("Nebenuhr DB " + label + ": last_relais_a gespeichert=" + config.lastRelaisA + " geladen=" + svRelais + (matchRelais ? " OK" : " MISMATCH"));
            }
        } catch (Exception e) {
            appendToStartCheckLog("Nebenuhr DB: " + e.getMessage());
        }
        appendToStartCheckLog("--- Ende Werte-Check ---");
    }
    
    /**
     * Lädt Vorschwingen-Konfiguration aus der Datenbank.
     * @return true wenn Daten geladen wurden, false wenn keine Daten vorhanden sind
     */
    private boolean loadVorschwingenFromDatabase() {
        try {
            // Leere Arrays und Map zuerst
            StaticVariable.vorschwingenMotorRelaisNeu.clear();
            StaticVariable.laeutenKloeppelRelais.clear();
            StaticVariable.vorschwingZeitSekunden.clear();
            StaticVariable.vorschwingZeitSekundenByRelais.clear();

            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(turmtechnikContext);
            List<PlatinenDatabaseHelper.VorschwingenConfig> vorschwingen = dbHelper.getAllVorschwingen();
            
            if (vorschwingen == null || vorschwingen.isEmpty()) {
                Log.d("VorschwingenLoad", "Keine Vorschwingen-Daten in Datenbank gefunden");
                return false;
            }
            
            // Nur aktive Einträge verwenden
            for (PlatinenDatabaseHelper.VorschwingenConfig config : vorschwingen) {
                if (!config.aktiv) {
                    continue; // Überspringe inaktive Einträge
                }
                
                // Läuten-Relais zuerst berechnen (wird ggf. auch für Vorschwingen genutzt)
                int laeutenRelaisBerechnet = config.laeutenRelaisBerechnet;
                if (laeutenRelaisBerechnet == 0) {
                    laeutenRelaisBerechnet = config.laeutenRelais + ((config.laeutenPlatine - 1) * 32);
                }
                
                // Vorschwingen-Relais: 0 = identisch mit Läuten, -1 = Kein Vorschwing Relais (0 in Liste)
                int vorschwingenRelaisBerechnet = config.vorschwingenRelaisBerechnet;
                if (config.vorschwingenRelais == -1) {
                    vorschwingenRelaisBerechnet = 0;
                } else if (config.vorschwingenRelais == 0) {
                    vorschwingenRelaisBerechnet = laeutenRelaisBerechnet;
                } else if (vorschwingenRelaisBerechnet == 0) {
                    vorschwingenRelaisBerechnet = config.vorschwingenRelais + ((config.vorschwingenPlatine - 1) * 32);
                }
                
                StaticVariable.vorschwingenMotorRelaisNeu.add(vorschwingenRelaisBerechnet);
                StaticVariable.laeutenKloeppelRelais.add(laeutenRelaisBerechnet);
                StaticVariable.vorschwingZeitSekunden.add(config.zeitSekunden);
                StaticVariable.vorschwingZeitSekundenByRelais.put(laeutenRelaisBerechnet, config.zeitSekunden);

                Log.e("vorschwingen", "Motor Relais=" + vorschwingenRelaisBerechnet + 
                      (config.vorschwingenRelais == -1 ? " (Kein Vorschwing Relais)" : 
                       (config.vorschwingenRelais == 0 ? " (identisch mit Läuten)" : 
                        " (Original: " + config.vorschwingenRelais + ", Platine: " + config.vorschwingenPlatine + ")")));
                Log.e("setvorschwingen", "Kloeppel Relais=" + laeutenRelaisBerechnet + 
                      " (Original: " + config.laeutenRelais + ", Platine: " + config.laeutenPlatine + ")");
                Log.e("vorschwingen", "Zeit Sekunden=" + config.zeitSekunden);
            }
            
            Log.d("VorschwingenLoad", "Vorschwingen-Daten aus Datenbank geladen: " + 
                  StaticVariable.vorschwingenMotorRelaisNeu.size() + " Einträge");
            return true;
            
        } catch (Exception e) {
            Log.e("VorschwingenLoad", "Fehler beim Laden der Vorschwingen-Daten aus Datenbank", e);
            return false;
        }
    }

    /**
     * Lädt Vorschwingen aus der Datenbank neu in die StaticVariable-Listen (vorschwingenMotorRelaisNeu,
     * laeutenKloeppelRelais, vorschwingZeitSekunden). Wird z. B. nach Änderungen in der Vorschwingen-Config (Web-UI)
     * aufgerufen, damit die laufende App die neuen Werte verwendet.
     * @param context Application-Context; bei null wird nichts gemacht.
     */
    public static void reloadVorschwingenFromDatabase(android.content.Context context) {
        if (context == null) return;
        try {
            StaticVariable.vorschwingenMotorRelaisNeu.clear();
            StaticVariable.laeutenKloeppelRelais.clear();
            StaticVariable.vorschwingZeitSekunden.clear();
            StaticVariable.vorschwingZeitSekundenByRelais.clear();
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
            List<PlatinenDatabaseHelper.VorschwingenConfig> vorschwingen = dbHelper.getAllVorschwingen();
            if (vorschwingen == null || vorschwingen.isEmpty()) return;
            for (PlatinenDatabaseHelper.VorschwingenConfig config : vorschwingen) {
                if (!config.aktiv) continue;
                int laeutenRelaisBerechnet = config.laeutenRelaisBerechnet;
                if (laeutenRelaisBerechnet == 0) {
                    laeutenRelaisBerechnet = config.laeutenRelais + ((config.laeutenPlatine - 1) * 32);
                }
                int vorschwingenRelaisBerechnet = config.vorschwingenRelaisBerechnet;
                if (config.vorschwingenRelais == -1) {
                    vorschwingenRelaisBerechnet = 0;
                } else if (config.vorschwingenRelais == 0) {
                    vorschwingenRelaisBerechnet = laeutenRelaisBerechnet;
                } else if (vorschwingenRelaisBerechnet == 0) {
                    vorschwingenRelaisBerechnet = config.vorschwingenRelais + ((config.vorschwingenPlatine - 1) * 32);
                }
                StaticVariable.vorschwingenMotorRelaisNeu.add(vorschwingenRelaisBerechnet);
                StaticVariable.laeutenKloeppelRelais.add(laeutenRelaisBerechnet);
                StaticVariable.vorschwingZeitSekunden.add(config.zeitSekunden);
                StaticVariable.vorschwingZeitSekundenByRelais.put(laeutenRelaisBerechnet, config.zeitSekunden);
            }
            Log.d("VorschwingenLoad", "Vorschwingen aus DB neu geladen: " + StaticVariable.vorschwingZeitSekunden.size() + " Einträge");
        } catch (Exception e) {
            Log.e("VorschwingenLoad", "Fehler beim Neuladen der Vorschwingen", e);
        }
    }

    /**
     * Stellt sicher, dass Vorschwingen-Listen (vorschwingZeitSekunden, vorschwingenMotorRelaisNeu, laeutenKloeppelRelais)
     * befüllt sind. Wird bei Bedarf von TagesSuche (Sofortstart, Manuel, Benutzer) aufgerufen, falls der normale
     * App-Start die Daten noch nicht geladen hat.
     * @param context Application-Context (z. B. TurmtechnikActivity.turmtechnikContext); bei null wird nichts gemacht.
     */
    public static void ensureVorschwingenLoaded(android.content.Context context) {
        if (context == null) return;
        if (!StaticVariable.vorschwingZeitSekunden.isEmpty()) return;
        try {
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
            List<PlatinenDatabaseHelper.VorschwingenConfig> vorschwingen = dbHelper.getAllVorschwingen();
            if (vorschwingen == null || vorschwingen.isEmpty()) return;
            for (PlatinenDatabaseHelper.VorschwingenConfig config : vorschwingen) {
                if (!config.aktiv) continue;
                int laeutenRelaisBerechnet = config.laeutenRelaisBerechnet;
                if (laeutenRelaisBerechnet == 0) {
                    laeutenRelaisBerechnet = config.laeutenRelais + ((config.laeutenPlatine - 1) * 32);
                }
                int vorschwingenRelaisBerechnet = config.vorschwingenRelaisBerechnet;
                if (config.vorschwingenRelais == -1) {
                    vorschwingenRelaisBerechnet = 0;
                } else if (config.vorschwingenRelais == 0) {
                    vorschwingenRelaisBerechnet = laeutenRelaisBerechnet;
                } else if (vorschwingenRelaisBerechnet == 0) {
                    vorschwingenRelaisBerechnet = config.vorschwingenRelais + ((config.vorschwingenPlatine - 1) * 32);
                }
                StaticVariable.vorschwingenMotorRelaisNeu.add(vorschwingenRelaisBerechnet);
                StaticVariable.laeutenKloeppelRelais.add(laeutenRelaisBerechnet);
                StaticVariable.vorschwingZeitSekunden.add(config.zeitSekunden);
                StaticVariable.vorschwingZeitSekundenByRelais.put(laeutenRelaisBerechnet, config.zeitSekunden);
            }
            Log.d("ensureVorschwingen", "Vorschwingen bei Bedarf geladen: " + StaticVariable.vorschwingZeitSekunden.size() + " Einträge");
        } catch (Exception e) {
            Log.e("ensureVorschwingen", "Fehler beim Nachladen der Vorschwingen", e);
        }
    }

	/*
	 * das wird im GoogleThread gemacht
	 * 
	 * 
	 * public void saveGoogleUri() { // String uhrA =
	 * String.valueOf(StaticVariable.uhrA_angezeigteZeit) + "\n" ; // String
	 * uhrB = String.valueOf(StaticVariable.uhrB_angezeigteZeit) + "\n" ; //
	 * String uhrC = String.valueOf(StaticVariable.uhrC_angezeigteZeit) + "\n" ;
	 * 
	 * if(StaticConstants.DEBUG) { //Log.i("String uhrA" , "=" + uhrA) ; }
	 * 
	 * try { FileOutputStream out = this.openFileOutput(stringNameGoogleUri,
	 * MODE_PRIVATE) ; OutputStreamWriter writer = new OutputStreamWriter(out) ;
	 * 
	 * GoogleDrive googleDrive = new GoogleDrive() ;
	 * 
	 * googleDrive.makeNewDriveFile
	 * 
	 * String download_uri = googleDrive.makeNewDriveFile() ; // liest Filenamen
	 * aus Fernsteuern-Speicher-Ort.xls // und Speichert ein File mit diesem
	 * Namen // der Inhalt ist die Datei mit diesem Namen auf der SD Karte //
	 * macht auch die Speicherung writer.write(download_uri) ;
	 * 
	 * // writer.write(uhrA); // writer.write(uhrB); // writer.write(uhrC);
	 * 
	 * writer.close() ; } catch (FileNotFoundException fnfe) { // was tuen? }
	 * catch (IOException ioe) { // was tuen? } }
	 */

    // das ist hinfaellig 1.8.13
	/*
	 * public void makeGoogleDriveNewSpreadsheet() { GoogleDriveThread
	 * googleDriveThread = new
	 * GoogleDriveThread(StaticConstants.GOOGLE_DRIVE_NEW_FILE,
	 * getBaseContext()); googleDriveThread.start() ; }
	 */

    public boolean nebenuhrVorhanden() {
        File test = getFileStreamPath(stringNameNebenuhr);
        boolean tempTest = (test.exists());
        // Log.e("tempTest" , "=" + tempTest) ;
        return tempTest;
    }


    private void checkPasswordAndStartIntent(final int relais_number,
                                             final String excell_password) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        // alert.setTitle("Title");
        alert.setMessage("Passwort eingeben:");
        // alert.setMessage("Dies ist nun ein Roman\nÜber mehrere\nZeilen") ;

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Editable value = input.getText();
                String value_string = value.toString();
                // Do something with value!
                if (StaticConstants.DEBUG) {
                    // Log.i("value_string", "=" + value + ":") ;
                    // Log.i("password" , "=" + excell_password + ":") ;
                }
                if (excell_password.equals(value_string)) {

                    // Log.i("password" , "OK") ;
                    startSeite(relais_number);
                } else {
                    // Log.i("password" , "ERROR") ;
                }
            }
        });

        alert.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.

                    }
                });

        AlertDialog d = alert.create();
        applyDialogAboveKeyboard(d);
        d.show();
    }

    private String readPassword(int relais_number) {
        String relais_number_string = String.valueOf(relais_number);
        String return_string = "";

        ExcelRead excelread = new ExcelRead();
        try {
            excelread.openXlsSheet(beschriftungTastenFileString, BESCHRIFTUNG_GLOCKEN_SHEET_NEW);
        } catch (BiffException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        int max_zeilen = excelread.getCellZeilen();
        for (int i = 1; i < max_zeilen; i++) {
            try {
                if (excelread.getCellString(3, i).equals(relais_number_string)) {
                    return_string = excelread.getCellString(1, i).trim();
                    break;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(3, i, beschriftungTastenFileString, BESCHRIFTUNG_GLOCKEN_SHEET_NEW, sourceFileName, 2581);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                new LogExcelError(3, i, beschriftungTastenFileString, BESCHRIFTUNG_GLOCKEN_SHEET_NEW, sourceFileName, 2586);
            }
        }

        excelread.closeWorkbook();
        // Log.i("password", "=" + return_string) ;
        return return_string;
    }

    private void startSeite2() {
        setRestartTimer(60 * 15); // 15 Minuten
        Intent testSeite2 = new Intent(this, Seite2Activity.class);
        this.startActivity(testSeite2);
    }

    /** Öffnet die Web-UI (Benutzerprogramme) statt der nativen App-Seite – einheitliche Oberfläche mit Tablet/PC. */
    private void startBenutzerMelodien() {
        setRestartTimer(60 * 15); // 15 Minuten
        openWebUiInBrowser(this, "/benutzerprogramme.html");
    }

    private void startProgrammKontrolle() {
        setRestartTimer(60 * 15); // 15 Minuten
        Intent startKontrolle = new Intent(TurmtechnikActivity.this,
                ProgrammKontrolleActivity.class);
        TurmtechnikActivity.this.startActivity(startKontrolle);
    }

    private void startSetNebenuhr() {
        setRestartTimer(60 * 15); // 15 Minuten
        Intent activitySetNebenuhr = new Intent(TurmtechnikActivity.this,
                SetNebenuhrActivity.class);
        TurmtechnikActivity.this.startActivity(activitySetNebenuhr);
    }

    /**
     * Nur bei fehlenden gespeicherten Werten (Neuinstallation): Nebenuhr-Fenster öffnen und auf Eingabe warten.
     * UhrThread wird erst in onActivityResult gestartet, nachdem der Nutzer die Zeit gesetzt und gespeichert hat.
     */
    private void startSetNebenuhrForFirstTime() {
        waitingForNebenuhrFirstTime = true;
        Intent activitySetNebenuhr = new Intent(TurmtechnikActivity.this, SetNebenuhrActivity.class);
        startActivityForResult(activitySetNebenuhr, REQUEST_NEBENUHR_FIRST_TIME);
    }

    private void startManuelerStart() {
        setRestartTimer(60 * 15); // 15 Minuten
        Log.e("START", "ManuelerStart");
        Intent activityManuelerStart = new Intent(TurmtechnikActivity.this,
                ManuelerStartActivity.class);
        TurmtechnikActivity.this.startActivity(activityManuelerStart);
    }

    private void startTurmtechnik() {
        Intent activityTurmtechnik = new Intent(TurmtechnikActivity.this,
                TurmtechnikActivity.class);
        activityTurmtechnik.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        TurmtechnikActivity.this.startActivity(activityTurmtechnik);
    }

    /** Nicht mehr verwendet – Fashion Clock entfernt, Bildschirmschoner ist Web-UI. */
    protected void startFashionClock() { }

    /** Nicht mehr verwendet – Fashion Clock entfernt. */
    protected void stopFashionClock() { }

    public synchronized void triggerBigClockTimer() {
        Log.e("trigger", "BigClockTimer");
        StaticVariable.bigClockTimeout = SET_BIG_CLOCK_TIME; // Uhr aufziehen
    }

    public synchronized void setRestartTimer(int restartTime) {
        StartTurmtechnikService.setTimeTurmtechnik(restartTime);
    }

    private void startHelpSeite() // 17.9.13 experiment mit Fashion Clock
    {
        setRestartTimer(60 * 15); // 15 Minuten

        //startFashionClock(); // das war nur zum Test

        Log.e("start", "HELP");

        String helpFileName = "";
        // lese Help File Name
        ExcelRead excelRead = new ExcelRead();
        try {
            excelRead.openXlsSheet(sdCardPath + "/Turmtechnik/Config/System.xls",
                    BESCHRIFTUNG_GLOCKEN_SHEET_NEW);
        } catch (BiffException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        int max_zeilen = excelRead.getCellZeilen();
        for (int i = 1; i < max_zeilen; i++) {
            try {
                if (excelRead.getCellString(3, i).equals(StaticConstants.HELP)) ;    //("103"))
                {
                    // wenn die Help Taste gefunden ist:
                    // Log.i("103" , "gefunden") ;
                    // Log.i("index i" , "=" + i) ;
                    helpFileName = (excelRead.getCellString(1, i));
                    // Log.i("helpFileName" , "=" + helpFileName) ;
                    break;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        excelRead.closeWorkbook();

        Intent activityHelpSeite = new Intent(this, HelpSeiteActivity.class);

        Log.e("helpFileName", "=" + helpFileName);
        activityHelpSeite.putExtra("help_file_name", helpFileName);

        TurmtechnikActivity.this.startActivity(activityHelpSeite);
    }

    private void startSeite(int seiten_nummer) // entspricht der Relaisnummer
    {
        //Log.e("startSeite" , "="  + seiten_nummer) ;
        switch (seiten_nummer) {
            case StaticConstants.ZWEITE_SEITE:
                startSeite2();
                break;

            case StaticConstants.PROGRAMM_EINGEBEN:
                startBenutzerMelodien();
                break;

            case StaticConstants.PROGRAMM_ABFRAGEN:
                startProgrammKontrolle();
                break;

            case StaticConstants.SET_NEBENUHR:
                startSetNebenuhr();
                break;

            case StaticConstants.MANUELER_START:
                startManuelerStart();
        }
    }

    //private void checkPasswordAndExit(final String exit_password) {
    private void checkPasswordAndExit() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        // alert.setTitle("Title");
        alert.setMessage("Passwort zum beenden eingeben:");
        // alert.setMessage("Dies ist nun ein Roman\nÜber mehrere\nZeilen") ;

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Editable value = input.getText();
                String value_string = value.toString();
                // Do something with value!
                if (StaticConstants.DEBUG) {
                    // Log.i("value_string", "=" + value + ":") ;
                    // Log.i("password" , "=" + excell_password + ":") ;
                }
                Log.e("exit passowrd normal", "=" + passwordExitNormal);
                Log.e("exit password mit clr", "=" + passwordExitAndDeleteBeschriftungTasten);
                String trimmed = value_string != null ? value_string.trim() : "";
                if (PASSWORD_EXIT_15MIN_BACKGROUND.equals(trimmed)) {
                    Log.e("password", "5644 – Beenden mit 15 Min Hintergrund");
                    beendenMit15MinHintergrund();
                } else if (passwordExitNormal.equals(value_string)) {
                    Log.e("password", "OK");
                    beenden();
                } else {
                    if (passwordExitAndDeleteBeschriftungTasten.equals(value_string)) {
                        Log.e("clear", "Beschriftung-Tasten und exit");
                        deleteBeschriftungTasten();
                        beenden();
                    } else {
                        Log.e("password", "ERROR");
                    }
                }
            }
        });

        alert.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.

                    }
                });

        AlertDialog d = alert.create();
        applyDialogAboveKeyboard(d);
        d.show();
    }

    /**
     * Zeigt den Passwort-zum-Beenden-Dialog in einer beliebigen Activity (z. B. Seite2, SetNebenuhr).
     * Beim ersten BACK in der Navigationsleiste so aufrufen – Dialog erscheint über der aktuellen Layoutseite, nicht über schwarzem Bildschirm.
     */
    public static void showExitPasswordDialogFrom(final android.app.Activity activity) {
        if (activity == null) return;
        final String pwdNormal;
        final String pwdDelete;
        try {
            PlatinenDatabaseHelper db = PlatinenDatabaseHelper.getInstance(activity);
            String p = db.getConfigValue("password_exit_normal");
            String pd = db.getConfigValue("password_exit_and_delete");
            pwdNormal = (p != null) ? p : "";
            pwdDelete = (pd != null) ? pd : "";
        } catch (Exception e) {
            android.util.Log.w("Turmtechnik", "Passwörter für Beenden-Dialog nicht geladen", e);
            return;
        }
        final String pwd15 = "5644";
        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
        alert.setMessage("Passwort zum beenden eingeben:");
        final EditText input = new EditText(activity);
        alert.setView(input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = (input.getText() != null) ? input.getText().toString() : "";
                String trimmed = value.trim();
                String action = null;
                if (pwd15.equals(trimmed)) action = "15min";
                else if (pwdNormal.equals(value)) action = "beenden";
                else if (pwdDelete.equals(value)) action = "delete_beenden";
                if (action != null) {
                    Intent i = new Intent(activity, TurmtechnikActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    i.putExtra(EXTRA_EXIT_ACTION, action);
                    activity.startActivity(i);
                    activity.finish();
                }
            }
        });
        alert.setNegativeButton("Cancel", null);
        AlertDialog d = alert.create();
        applyDialogAboveKeyboard(d);
        d.show();
    }

    private void deleteBeschriftungTasten() {
        File file = new File(beschriftungTastenFileString);
        file.delete();
    }

    private void initSound() {
        ExcelRead excelread = new ExcelRead();
        try {
            excelread.openXlsSheet(beschriftungTastenFileString, BESCHRIFTUNG_GLOCKEN_SHEET_NEW);
        } catch (BiffException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        int maxzeilen = excelread.getCellZeilen();
        String soundFileName1 = "NULL";
        String soundFileName2 = "NULL";

        for (int i = 1; i < maxzeilen; i++) {
            String temp = "NULL";
            try {
                temp = excelread.getCellString(11, i);
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            {
                if (!((temp.equals("null")) || ((temp.equals("NULL"))))) {
                    if (soundFileName1.equals("NULL")) {
                        soundFileName1 = temp;
                    } else {
                        soundFileName2 = temp;
                    }
                }
            }
        }

        // Log.i("soundFileName1" , "=" + soundFileName1) ;
        // Log.i("soundFileName2" , "=" + soundFileName2) ;

        Sound.initSound(soundFileName1, soundFileName2);
        excelread.closeWorkbook();
    }

    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    private static final int REQUEST_BLUETOOTH_CONNECT = 1002;
    /** Erstinstallation: Nebenuhr-Fenster war geöffnet, UhrThread starten erst nach Rückkehr mit gespeichertem Wert. */
    private static final int REQUEST_NEBENUHR_FIRST_TIME = 1003;
    private static final int REQUEST_CAMERA_MOTION = 1004;
    private static boolean waitingForNebenuhrFirstTime = false;

    /** Fordert Speicher-Berechtigung an (Android 6+), damit System.xls und Config gelesen werden können. */
    private void requestStoragePermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    REQUEST_STORAGE_PERMISSION
                );
            }
        }
    }

    /** Fordert BLUETOOTH_CONNECT an (Android 12 / API 31+), damit Bluetooth-Adapter und Verbindung genutzt werden können. */
    private void requestBluetoothConnectPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_CONNECT && grantResults != null && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            StaticVariable.myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Log.i("TurmtechnikActivity", "BLUETOOTH_CONNECT erteilt – Bluetooth-Adapter initialisiert.");
        }
        // Bewegungserkennung/Kamera ausgebaut – kein Service-Start mehr
        // if (requestCode == REQUEST_CAMERA_MOTION && ...) { startService(...); }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_NEBENUHR_FIRST_TIME) {
            waitingForNebenuhrFirstTime = false;
            if (resultCode == RESULT_OK && nebenuhrVorhanden()) {
                loadNebenuhr();
            } else {
                loadNebenuhrLastRelaisAndAnzeigeFromDb();
            }
            if (uhr_thread == null) {
                uhr_thread = new UhrThread();
                uhr_thread.start();
                Log.e("nach", "startUhrThread (nach Nebenuhr Erstinstallation)");
            }
        }
    }

    private void waitToSDcard() {
        //printInfo("\n\n");
        //printInfo(" wait to SDCARD") ;
        String state;
        int errorCounter = 0;
        do {
            errorCounter++;
            if (errorCounter > 100000) {
                printInfo("\n\n");
                printInfo("sdcard not ready");
                break;
            }
            state = Environment.getExternalStorageState();
            // warten bis sd card gemountet ist ...
            // Log.i("WAIT to" , "SD card mounted") ;
        } while (!(Environment.MEDIA_MOUNTED.equals(state)));

    }

/*	//am 27.2.2014 google drive excel version ausgebaut.
 
	private class MyUiTask extends AsyncTask {

		Dialog dialog;
		private boolean internetBlockiert = false ;

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			// dialog = new Dialog(GSSAct.this);
			// dialog.setTitle("Please wait");
			// TextView tv = new TextView(GSSAct.this.getApplicationContext());
			// tv.setText("Featching SpreadSheet list from your account...");
			// dialog.setContentView(tv);
			// dialog.show();
		}

		@Override
		protected Object doInBackground(Object... params) {

			doRunInternetThread = true;
			boolean onLineOk = false;

			// hier auf Internet connection warten
			while (doRunInternetThread == true && onLineOk == false) {
				ConnectivityManager cm = (ConnectivityManager) getBaseContext()
						.getSystemService(Context.CONNECTIVITY_SERVICE);

				NetworkInfo netInfo = cm.getActiveNetworkInfo();
				if (netInfo != null && netInfo.isConnectedOrConnecting()) {
					onLineOk = true;
				} else {
					// Log.i("WARTE" , "AUF'S INTERNET...") ;
					sleepTime(internetPolling); // 10 sekunden warten...
				}
			}

			// Looper.myLooper().prepare();

			// do
			// {
			// Filename aus der Datei Fernsteuern-Speicher-Ort.xls lesen
			
			// mAccountName = "alfredrosenauer@gmail.com" ; so geht es
			Log.i("sdCardFilename", "=" + sdCardFilename);
			Log.i("googleFilename", "=" + googleFilename);
			// Log.i("Konto name", "=" + mAccountName); // Google Account Code entfernt

			SpreadSheetFactory factory = SpreadSheetFactory
					.getInstance(new AndroidAuthenticator(
							TurmtechnikActivity.this));

			Looper.myLooper().prepare();

			// gibts das SpreadSheet schon?
			ArrayList<SpreadSheet> spstemp = factory.getSpreadSheet(
					googleFilename, true);
			if (spstemp != null && spstemp.size() > 0) {
				spreadSheet = spstemp.get(0);

				String rid = spreadSheet.getResourceID();

				Log.i("spreadsheet", "vorhanden id=" + rid);
				// alles merken ??!!
				StaticVariable.googleFileId = rid;
				ArrayList<WorkSheet> wstemp = spreadSheet.getAllWorkSheets();

			} else {
				Log.i("spradsheet", "nicht vorhanden... ein neues erzeugen");
				factory.createSpreadSheet(googleFilename);

				spstemp = factory.getSpreadSheet(googleFilename, true);
				if (spstemp != null && spstemp.size() > 0) {
					spreadSheet = spstemp.get(0);

					StaticVariable.googleFileId = spreadSheet.getResourceID();
					// ToDo und jetzt die Worksheet's erzeugen:
					generateWorksheets();
				}
			}

			// }
			// while(spreadSheet == null) ;

			String getNowStart = getNOW("---");
			stringInfoText = "Internet O.K.";
			StaticVariable.changeInternetVerknuepfteTasten = 1; // nach Start
																// immer
																// internet
																// updaten
			// initSonderprogrammeVectoren() ;
			StaticVariable.changeInternetBenutzerprogramme = 90; // achtung wegen
			//StaticVariable.changeInternetBenutzerprogramme = 1 ;												// Vectoren
			// 101 = erster Start, nur on_off auf 0 stellen

			while (doRunInternetThread) {
				if (StaticVariable.changeInternetVerknuepfteTasten > 0) {
					StaticVariable.changeInternetVerknuepfteTasten--; // decrement
					doUpdateVerknuepfteTastenInternet();
					Log.e("change" , "Verknuepft=" + StaticVariable.changeInternetVerknuepfteTasten) ;
					sendActionTime();
					sleepTime(internetPolling); // google zeit lassen
					String getNowTemp = getNOW(getNowStart);
					getNowStart = getNowTemp; // verhindert zurueck lesen bevor
												// update
												// eigene Aenderung!!

				}
				if (StaticVariable.changeInternetBenutzerprogramme > 0) {
					StaticVariable.changeInternetBenutzerprogramme--; // decrement
					doUpdateBenutzerProgrammeInternet();
					Log.e("change" , "Benutzer=" + StaticVariable.changeInternetBenutzerprogramme) ;
					sendActionTime();
					sleepTime(internetPolling); // google zeit lassen
					String getNowTemp = getNOW(getNowStart);
					getNowStart = getNowTemp; // verhindert zurueck lesen bevor
												// update
												// eigene Aenderung!!
				}

				//Log.i("jetzt", "sleep" + internetPolling);
				sleepTime(internetPolling);

				// check datum auf veraenderungen
				String getNowTemp = getNOW(getNowStart);

				if (getNowTemp.equals(getNowStart)) {
					// Log.i("Tabelle" , "nicht veraendert") ;
				} else 
				{
					Log.i("TABELLE", "veraendert!");

					getNowStart = getNowTemp;
					
					if(sendSelf >0)
					{
						sendSelf -- ;
					}
					else
					{
						// 28.1.2013  --- NEU MACHEN !!!
					changeVerknuepfteTasten(); // in Fernsteuern Tabelle ==
												// Programmschalter();
					changeBenutzerProgramme(); // nur on/off auf off stellen!
												// Datum und Zeit lassen!

					Log.e("in" , "change gesendet") ;
					sendActionTime();
					sendSelf ++ ;
					
					StaticVariable.internetChanged = true;
					}
				}

			}

			return null;
		}

		@Override
		protected void onPostExecute(Object result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);

		}
	}
*/		

/*	
		private String getNOW(String oldString) {
			ConnectivityManager cm = (ConnectivityManager) getBaseContext()
					.getSystemService(Context.CONNECTIVITY_SERVICE);

			NetworkInfo netInfo = cm.getActiveNetworkInfo();
			if (!(netInfo != null && netInfo.isConnectedOrConnecting())) {
				Log.i("KEIN", "INTERNET");
				return oldString; // simmuliere keine Aenderung
									// wenn keine Internet Verbindung ist
			}

			String dataReturn = oldString;
			if (spreadSheet != null) {
				ArrayList<WorkSheet> wks = spreadSheet.getWorkSheet(
						"Status", true);
				if (wks != null && wks.size() > 0) {
					WorkSheet testSheet = wks.get(0);
					if (testSheet != null) {
						ArrayList<Record> records = testSheet
								.getRecords(spreadSheet.getKey());
						// Log.i("records" , "size=" + records.size()) ;
						if (records != null && records.size() > 0)
						{
							Record r = records.get(0);
							HashMap<String, String> data = r.getData();
							Log.i("der erste record", "Data:" + data);
							dataReturn = data.get("Sp-B");
							Log.i("der return string", "=" + dataReturn);
						}
					}
				}
			}

			return dataReturn;
		}

		private void sendActionTime()
		{
			Log.e("SEND" , "ACTION TIME") ;
			if (spreadSheet != null)
			{
				ArrayList<WorkSheet> wks = spreadSheet.getWorkSheet(
						"Status", true);
				if (wks != null && wks.size() > 0)
				{
					WorkSheet testSheet = wks.get(0);
					if (testSheet != null)
					{
						ArrayList<Record> records = testSheet
								.getRecords(spreadSheet.getKey());
						// Log.i("records" , "size=" + records.size()) ;
						if (records != null && records.size() > 0)
						{
							Calendar calendar2 = Calendar.getInstance();
							SimpleDateFormat formater = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss" , Locale.GERMAN);
							String datumPlusZeit = "0" ;
							if((formater != null) && (calendar2 != null) )
							{
								datumPlusZeit = (formater.format(calendar2.getTime())) ;  // 1.10.13 10:03
							}														
							Record r2 = records.get(1);
																			
							r2.addData("Sp-B", datumPlusZeit);
							testSheet.updateRecord(r2);
						}
					}
				}
			}

		}
		
		private void changeVerknuepfteTasten()
		{
					
			ArrayList<WorkSheet> wks = spreadSheet.getWorkSheet(
					"VERKNUEPFTE TASTEN", true);
			if (wks != null && wks.size() > 0) {
				WorkSheet testSheet = wks.get(0);
				if (testSheet != null) {
					ArrayList<Record> records = testSheet
							.getRecords(spreadSheet.getKey());
					int recordsAnzahl = records.size();
					Log.i("records", "size=" + recordsAnzahl);
					for (int i = 1; i < recordsAnzahl; i++) {
						Record r = records.get(i);
						HashMap<String, String> data = r.getData();
						String on_off = data.get("Sp-B");
						//String sofort_Start = data.get("Sp-E");
						String funktionsName = data.get("Sp-A");
						Log.i("name=" + funktionsName, "on_off=" + on_off);

						if (funktionsName.equals("Stop"))
						{
							if (!on_off.equals("0"))
							{
								MelodieThread.doRunOff();
								UhrThread.blockReady = 0; // block fertig ,
															// wieder ready
								stringInfoText = "suche nächsten Start...";
								UhrThread.newSearchAutomaticStart = true;
								allRelaisOffInternet();
															
								StaticVariable.changeInternetBenutzerprogramme++ ;
								StaticVariable.changeInternetVerknuepfteTasten++ ;
								LogTurmtechnik logTemp =
								new LogTurmtechnik("Stop vom Internet empfangen ", 0, 0, 0, 0) ;
								logTemp = null ;
								System.gc();
								
								//setRestartTimer(5);   // restart am 28.1.14 wieder ausgebaut
								// restartTurmtechnik();
								
							}
						} else if (funktionsName.equals("Automatic")) {
							// noch machen:
							// changeAutomatic();
							changeAutomaticInternet(on_off);
						} else if (!funktionsName.equals("leer"))
						  {
							
							 //   if (sofort_Start.equals("1") )
							//	{
							//		Log.e("index" , "i=" + i) ;
							//		StaticVariable.benutzerMelodienIndex = index ;
							//		r.addData("Sp-E", "0");
							//		testSheet.updateRecord(r);
							//		doSofortStartBenuzerMelodien(i-1);
							//		sleepTime(2*60*1000); // mindestens 2 Minuten warten
														  // bis das Programm startet !!!
									//internetBlockiert = true ;  //!!!!!!
							//	}
							 
								
										if(on_off.equals("0"))
										{
											synchronized (VERKNUEPFTE_TASTEN_LOCK) { verknuepfteTastenOn[i-1] = false; }
										}
						    	 		else 
						    	 		{
						    	 			synchronized (VERKNUEPFTE_TASTEN_LOCK) { verknuepfteTastenOn[i-1] = true; }
						    	 		}
				  		
						  }
					// das macht jetzt beleuchteTastenAutomatic
					// showVerknuepfteTastenSeite1();
				}
			}
		}
	}

		private void changeBenutzerProgramme() {
			ArrayList<WorkSheet> wks = spreadSheet.getWorkSheet(
					"BENUTZERPROGRAMME", true);
			if (wks != null && wks.size() > 0) {
				WorkSheet testSheet = wks.get(0);
				if (testSheet != null) {
					ArrayList<Record> records = testSheet
							.getRecords(spreadSheet.getKey());
					int recordsAnzahl = records.size();
					Log.i("records", "size=" + recordsAnzahl);
					for (int i = 1; i < recordsAnzahl; i++) {
						Record r = records.get(i);
						HashMap<String, String> data = r.getData();
						String on_off = data.get("Sp-C");
						Log.i("on_off", "= " + on_off);
						String funktionsName = data.get("Sp-A");
						Log.i("name", "=" + funktionsName);
						String tabelleDatum = data.get("Sp-D");
						Log.i("tabelle Datum", "=" + tabelleDatum);
						String tabelleZeit = data.get("Sp-E");
						Log.i("tabelle Zeit", "=" + tabelleZeit);
						String sofort_Start = data.get("Sp-B");

						// noch machen: die Werte in die Felder schreiben
						// 8.8.2013
						boolean tastenFlag;
						if (on_off.equals("0")) {
							tastenFlag = false;
						} else {
							tastenFlag = true;
						}
						Log.i("tastenFlag", "=" + tastenFlag);
						if (StaticVariable.firstStartMelodie == true) {
							StaticVariable.benutzerMelodieTasteOn.add(false);
							StaticVariable.sofortStartButtonOn.add(false);
							StaticVariable.stunden.add(-1);
							StaticVariable.minuten.add(-1);
							StaticVariable.tage.add(-1);
							StaticVariable.monate.add(-1);
							StaticVariable.jahre.add(-1);
						}

						StaticVariable.benutzerMelodieTasteOn
								.set(i-1, tastenFlag);

						if (sofort_Start.equals("1") )
						{
								Log.e("index" , "i=" + i) ;
								StaticVariable.benutzerMelodienIndex = index ;
								r.addData("Sp-B", "0");  // auf google drive gleich wieder auf 0 stellen
								testSheet.updateRecord(r);
								doSofortStartBenuzerMelodien(i-1);
								StaticVariable.changeInternetBenutzerprogramme++; 
								//sleepTime(2*60*1000); // mindestens 2 Minuten warten
													  // bis das Programm startet !!!
								//internetBlockiert fuer 2 Minuten  //!!!!!!
						}
						else
						{
							// hier dann stunden und minuten einfuegen
							String[] zeitSplit = tabelleZeit.split("\\:");
							if (zeitSplit.length == 3)
							{
								StaticVariable.minuten.set(i,
										Integer.parseInt(zeitSplit[1]));
								StaticVariable.stunden.set(i,
										Integer.parseInt(zeitSplit[0]));
							}
							String[] datumSplit = tabelleDatum.split("\\.");
							if (datumSplit.length == 3)
							{
								StaticVariable.jahre.set(i,
										Integer.parseInt(datumSplit[2]));
								StaticVariable.tage.set(i,
										Integer.parseInt(datumSplit[0]));
								StaticVariable.monate.set(i,
										(Integer.parseInt(datumSplit[1]) - 1));
							}
						}
					}

					StaticVariable.firstStartMelodie = false;
					Log.i("am ende", "der Schleife");
				}
			}
		}

		private void doUpdateVerknuepfteTastenInternet() {
			if (spreadSheet == null) {
				return;
			}
			ArrayList<WorkSheet> wks = spreadSheet.getWorkSheet(
					"VERKNUEPFTE TASTEN", true);
			if (wks != null && wks.size() > 0) {
				WorkSheet testSheet = wks.get(0);
				if (testSheet != null) {
					ArrayList<Record> records = testSheet
							.getRecords(spreadSheet.getKey());
					int recordsAnzahl = records.size();
					Log.i("records", "size=" + recordsAnzahl);
					for (int i = 1; i < recordsAnzahl; i++) {
						Record r = records.get(i);
						HashMap<String, String> data = r.getData();
						String on_off_worksheet = data.get("Sp-B");
						String on_off;
						String funktionsName = data.get("Sp-A");
						if (!funktionsName.equals("leer")) {
							if (funktionsName.equals("Automatic")) {
								if (automaticOn == false) {
									on_off = "0";
								} else {
									on_off = "1";
								}
								if (!on_off_worksheet.equals(on_off)) { // nur
																		// auf
																		// internet
																		// schreiben
																		// bei
																		// !=
									r.addData("Sp-B", on_off);
									testSheet.updateRecord(r);
								}
							} else {
								if (i > 0 && i - 1 < verknuepfteTastenOn.length && Boolean.TRUE.equals(verknuepfteTastenOn[i - 1])) {
									on_off = "1";
								} else {
									on_off = "0";
								}
								if (!on_off_worksheet.equals(on_off)) { // nur
																		// auf
																		// internet
																		// schreiben
																		// bei
																		// !=
									r.addData("Sp-B", on_off);
									testSheet.updateRecord(r);
								}
							}
						}
					}
				}
			}
		}

		private void doUpdateBenutzerProgrammeInternet() {
			Log.i("do update", "BENUTZERPROGRAMME Internet");
			if (spreadSheet == null) {
				return;
			}
			Log.e("bin in" , "doUpdateBenutzerprogramme") ;
			ArrayList<WorkSheet> wks = spreadSheet.getWorkSheet(
					"BENUTZERPROGRAMME", true);
			if (wks != null && wks.size() > 0) {
				WorkSheet testSheet = wks.get(0);
				if (testSheet != null) {
					ArrayList<Record> records = testSheet
							.getRecords(spreadSheet.getKey());
					int recordsAnzahl = records.size();
					Log.i("records", "size=" + recordsAnzahl);
					for (int i = 1; i < recordsAnzahl; i++) {
						Record r = records.get(i);
						HashMap<String, String> data = r.getData();

						String on_off;
						if (StaticVariable.benutzerMelodieTasteOn.get(i-1) == true) {
							on_off = "1";
						} else {
							on_off = "0";
						}

						StringBuilder tabletDatum = new StringBuilder();

						if ((StaticVariable.tage.get(i-1) == -1)
								|| StaticVariable.monate.get(i-1) == -1) {
							tabletDatum.append("1.2.2050");
						} else {
							tabletDatum.append(StaticVariable.tage.get(i-1)
									.toString());
							Integer monatTemp = StaticVariable.monate.get(i-1); // bereits 1-12 in DB
							tabletDatum.append("." + monatTemp.toString());
							tabletDatum.append("."
									+ StaticVariable.jahre.get(i-1).toString());

						}

						StringBuilder tabletZeit = new StringBuilder();
						if (StaticVariable.stunden.get(i-1) == -1
								|| StaticVariable.minuten.get(i-1) == -1) {
							tabletZeit.append("14:00:00");
						} else {
							tabletZeit.append(StaticVariable.stunden.get(i-1)
									.toString());
							tabletZeit.append(":"
									+ StaticVariable.minuten.get(i-1).toString());
							tabletZeit.append(":00");
						}
						Log.e("updateBenutzer" , "on_off=" + on_off) ;
						r.addData("Sp-C", on_off);

						if (StaticVariable.changeInternetBenutzerprogramme < 90)
						{
						   	r.addData("Sp-D", tabletDatum.toString());
						    r.addData("Sp-E", tabletZeit.toString());
						}

						testSheet.updateRecord(r);
						StaticVariable.changeInternetBenutzerprogramme = 0;
					} // ende der for schleife

					
				}
			}
		}
		
*/

// 27.2.2014 neu Fernbedienung ueber Webserver implementiert

    private class MyUiTask extends AsyncTask {

        //Dialog dialog;
        //private boolean internetBlockiert = false ;

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            // dialog = new Dialog(GSSAct.this);
            // dialog.setTitle("Please wait");
            // TextView tv = new TextView(GSSAct.this.getApplicationContext());
            // tv.setText("Featching SpreadSheet list from your account...");
            // dialog.setContentView(tv);
            // dialog.show();
        }

        @Override
        protected Object doInBackground(Object... params) {

            doRunInternetThread = true;
            boolean onLineOk = false;

            // hier auf Internet connection warten
            while (doRunInternetThread == true && onLineOk == false) {
                ConnectivityManager cm = (ConnectivityManager) getBaseContext()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);

                NetworkInfo netInfo = cm.getActiveNetworkInfo();
                if (netInfo != null && netInfo.isConnectedOrConnecting()) {
                    onLineOk = true;
                } else {
                    // Log.i("WARTE" , "AUF'S INTERNET...") ;
                    sleepTime(internetPolling); // 10 sekunden warten...
                }
            }

            Looper.myLooper().prepare();

            //String getNowStart = getNOW("---");
            //stringInfoText = "Internet O.K.";
            StaticVariable.changeInternetVerknuepfteTasten = 1; // nach Start
            // immer
            // internet
            // updaten

            StaticVariable.changeInternetBenutzerprogramme = 90; // achtung wegen
            //StaticVariable.changeInternetBenutzerprogramme = 1 ;												// Vectoren
            // 101 = erster Start, nur on_off auf 0 stellen

            //jetzt momentanen Tastenstatus seite1 senden

            sendTastenStatusSeite1();
				
				/*
                String url_server_status =
						 "http://app.turmtechnik.com/json.php?tokenid=" + StaticVariable.fernwartungServerId
						  + "&tokenpw=" + StaticVariable.fernwartungServerPassword
						  + "&reqtype=status" ;
				
				String url_server_status_tasten_lesen = url_server_status + "&reqtable=tasten" ;
				*/

            while (doRunInternetThread) {

                if (StaticVariable.changeInternetVerknuepfteTasten > 0) {
                    StaticVariable.changeInternetVerknuepfteTasten--; // decrement
                    //doUpdateVerknuepfteTastenInternet();
                    //doUpdateTastenStatusServer();
                    Log.e("change", "Verknuepft=" + StaticVariable.changeInternetVerknuepfteTasten);
                }
                if (StaticVariable.changeInternetBenutzerprogramme > 0) {
                    StaticVariable.changeInternetBenutzerprogramme--; // decrement
                    //doUpdateBenutzerProgrammeInternet();
                    //doUpdateTastenStatusServer();
                    Log.e("change", "Benutzer=" + StaticVariable.changeInternetBenutzerprogramme);
                }

                StaticVariable.gelesenVonJsonRequest = "";
                // server nach status fragen:
                //jsonReadThread = new JsonReadThread(url_server_status);
                //jsonReadThread.start();

                sleepTime(internetPolling);

                //Log.e("Server Status String" , "=" + StaticVariable.gelesenVonJsonRequest) ;
                // check datum auf veraenderungen
                // am 27.2.2014 geaendert auf Statusabfrage beim Server
                if (!(StaticVariable.gelesenVonJsonRequest.equals(""))) // ist was angekommen?
                {
                    try {
                        JSONArray jsonArray = new JSONArray(StaticVariable.gelesenVonJsonRequest);
                        JSONObject jsonObjekt = jsonArray.getJSONObject(0); // das erste object holen
                        String serverAntwort =
                                jsonObjekt.getString("o");  // string key o , Antwort = 0 = offline
                        // 				Antwort = 1 = online
                        // 				Antwort = 2 = jemand hat eine Taste
                        //								  im browser gedrueckt
                        //Log.e("JSON Object" , "key o = " + serverAntwort) ;

                        if (serverAntwort.equals("2")) {
                            Log.e("es hat jemand", "eine Taste im Browser angeklickt");

                            StaticVariable.gelesenVonJsonRequest = "";
                            //Log.e("url_status_tasten" , "=" + url_server_status_tasten_lesen) ;
                            // server nach status tasten fragen:
                            //jsonReadThread = new JsonReadThread(url_server_status_tasten_lesen);
                            //jsonReadThread.start();
                            sleepTime(internetPolling);
                            //		tastenVerarbeiten();
                            sleepTime(internetPolling);
                        }

                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    StaticVariable.internetChanged = true;
                }
            }// while loop
            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);

        }
    }// ende von classe myUiTask

    private void sleepTime(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    private void generateWorksheets() {
        ExcelRead excelread = new ExcelRead();

        String[] alfredsSpaltenUeberschriften = {"Sp-A", "Sp-B", "Sp-C",
                "Sp-D", "Sp-E",};

        String[] ueberschriftDatum = {"Sp-A", "Sp-B"};

        try {
            excelread.openXlsSheet(sdCardFilename, 0);
        } catch (BiffException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        } catch (IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        } // zuerst sheet 0
        int zeilen = excelread.getCellZeilen();
        int spalten = excelread.getCellSpalten();

        // ArrayList<WorkSheet> wks = sp.getWorkSheet("AWR 1" , true);
        // WorkSheet testSheet = wks.get(0);
        // Log.e("testSheet" , "=" + testSheet.getTitle()) ;

        // testSheet.addRecord(sp.getKey(), etwas1);
        // Log.i("nach" , "add record") ;

        spreadSheet.addWorkSheet("BENUTZERPROGRAMME", "tabellen orientiert",
                zeilen, alfredsSpaltenUeberschriften);
        ArrayList<WorkSheet> wks = spreadSheet.getWorkSheet("BENUTZERPROGRAMME",
                true);

        WorkSheet tempSheet1 = wks.get(0);
        Log.i("tempSheet1", "=" + tempSheet1.getTitle());
        Log.i("zeilen", "=" + zeilen);

//		String[] benutzerProgrammeBezeichnungen = { "Programme", "Sofortstart", "on/off", "Datum", "Uhrzeit" };

//		HashMap<String, String> benutzerProgrammBeschreibung = new HashMap<String, String>();

//		for (int i = 0 ; i < 5 ; i++)
//		{
//			benutzerProgrammBeschreibung.put(alfredsSpaltenUeberschriften[i],benutzerProgrammeBezeichnungen[i]);
//		}
//		tempSheet1.addRecord(spreadSheet.getKey(), benutzerProgrammBeschreibung);


        // HashMap<String, String> ersteZeile = new HashMap<String, String>();
        // ersteZeile.put("Sp-A", "=NOW()") ; // wegen datum und zeit
        // veraenderung per firefox
        // ersteZeile.put("Sp-A", "---") ;
        // ersteZeile.put("Sp-B", "---") ;
        // ersteZeile.put("Sp-C", "---") ;
        // ersteZeile.put("Sp-D", "---") ;
        // ersteZeile.put("Sp-E", "---") ;
        // ersteZeile.put("Sp-F", "---") ;
        // tempSheet1.addRecord(spreadSheet.getKey(), ersteZeile) ;

        for (int i = 0; i < zeilen; i++) {
            HashMap<String, String> eineZeile = new HashMap<String, String>();
            for (int j = 0; j < spalten; j++) {
                // "key" , "value"
                String temp = "";
                try {
                    temp = excelread.getCellString(j, i);
                } catch (ArrayIndexOutOfBoundsException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                // schreibt in Zelle Datum und Uhrzeit, Uhrzeit wegschneiden!!
                if (j == 4) {
                    Log.i("temp", "=" + temp);
                    if (temp.length() > 10) {
                        try {
                            temp = temp.substring(0, 10);
                        } catch (Exception e) {
                            Log.e("Exception", "" + e.toString());
                        }
                    }
                }
                eineZeile.put(alfredsSpaltenUeberschriften[j], temp);
            }
            tempSheet1.addRecord(spreadSheet.getKey(), eineZeile);
        }

        try {
            excelread.openXlsSheet(sdCardFilename, 1);
        } catch (BiffException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } // und dann sheet 1
        int zeilen2 = excelread.getCellZeilen();
        int spalten2 = excelread.getCellSpalten();

        spreadSheet.addWorkSheet("VERKNUEPFTE TASTEN", "tabellen orientiert",
                zeilen2, alfredsSpaltenUeberschriften);
        ArrayList<WorkSheet> wks2 = spreadSheet.getWorkSheet(
                "VERKNUEPFTE TASTEN", true);
        WorkSheet tempSheet2 = wks2.get(0);

//		String[] programmschalterBezeichnungen = { "--", "--", "Name", "on/off", "sofort Start", "--" };

//		HashMap<String, String> programmschalterBeschreibung = new HashMap<String, String>();


//		for (int i = 0 ; i < 6 ; i++)
//		{
//			programmschalterBeschreibung.put(alfredsSpaltenUeberschriften[i],programmschalterBezeichnungen[i]);
//		}
//		tempSheet2.addRecord(spreadSheet.getKey(), programmschalterBeschreibung);

        for (int i = 0; i < zeilen2; i++) {
            HashMap<String, String> eineZeile2 = new HashMap<String, String>();
            for (int j = 0; j < spalten2; j++) {
                // "key" , "value"
                try {
                    eineZeile2.put(alfredsSpaltenUeberschriften[j],
                            excelread.getCellString(j, i));
                } catch (ArrayIndexOutOfBoundsException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            tempSheet2.addRecord(spreadSheet.getKey(), eineZeile2);
        }

        // drittes Blatt, neu 28.1.2013
        spreadSheet.addWorkSheet("Status", "tabellen orientiert", 3,
                ueberschriftDatum);
        ArrayList<WorkSheet> wks0 = spreadSheet.getWorkSheet("Status",
                true);
        WorkSheet tempSheet0 = wks0.get(0);

        HashMap<String, String> tabellenDatum = new HashMap<String, String>();
        tabellenDatum.put("Sp-A", "Letzte Änderung");
        tabellenDatum.put("Sp-B", "NOW()");
        tempSheet0.addRecord(spreadSheet.getKey(), tabellenDatum);


        HashMap<String, String> tabellenDatum2 = new HashMap<String, String>();
        tabellenDatum2.put("Sp-A", "Steuerung:");
        tabellenDatum2.put("Sp-B", "0");
        tempSheet0.addRecord(spreadSheet.getKey(), tabellenDatum2);


        ArrayList<WorkSheet> wks3 = spreadSheet.getWorkSheet("Sheet 1", false);
        WorkSheet tempSheet3 = wks3.get(0);
        spreadSheet.deleteWorkSheet(tempSheet3);

        excelread.closeWorkbook();
    }

    /** Wendet die geladene Fernsteuern-Konfiguration auf die App an (internetPolling + StaticVariable). */
    private void applyFernsteuernConfig(PlatinenDatabaseHelper.FernsteuernConfig c) {
        if (c == null) return;
        internetPolling = c.internetPollingMs;
        StaticVariable.timeServerEinAus = c.timeServerEinAus != null ? c.timeServerEinAus : "";
        StaticVariable.timeServerIp = c.timeServerIp != null ? c.timeServerIp : "";
        StaticVariable.timeServerMaxOffsetMinuten = c.timeServerMaxOffsetMinuten != null ? c.timeServerMaxOffsetMinuten : "";
        StaticVariable.timeServerAbfrageIntervallMs = c.timeServerAbfrageIntervallMs != null ? c.timeServerAbfrageIntervallMs : "";
        StaticVariable.serialGPS_OnOff = c.serialGPSOnOff;
        StaticVariable.serialGPS_IP = c.serialGPSIp != null ? c.serialGPSIp : "";
        StaticVariable.serialGPS_port = c.serialGPSPort;
    }

    /**
     * Lädt Fernsteuern-Konfiguration aus der Datenbank (io_config).
     * Wenn noch keine Einträge in der DB: einmalig aus Fernsteuern-Speicher-Ort.xls lesen und in DB speichern.
     * Sonst Defaults in DB schreiben.
     */
    private void getFernsteuernSpeicherOrt() {
        PlatinenDatabaseHelper dbHelper = turmtechnikContext != null ? PlatinenDatabaseHelper.getInstance(turmtechnikContext) : null;
        if (dbHelper != null && dbHelper.hasFernsteuernConfigInDb()) {
            PlatinenDatabaseHelper.FernsteuernConfig c = dbHelper.getFernsteuernConfig();
            applyFernsteuernConfig(c);
            return;
        }
        String fileNameFernsteuernSpeicherOrt = TurmtechnikActivity.sdCardPath + StaticConstants.FERNSTEUERN_SPEICHER_ORT_STRING;
        java.io.File excelFile = new java.io.File(fileNameFernsteuernSpeicherOrt);
        if (excelFile.exists()) {
            ExcelRead excelread = new ExcelRead();
            try {
                excelread.openXlsSheet(fileNameFernsteuernSpeicherOrt, 1);
                PlatinenDatabaseHelper.FernsteuernConfig c = new PlatinenDatabaseHelper.FernsteuernConfig();
                try {
                    String s = excelread.getCellString(2, 4);
                    c.internetPollingMs = (s != null && !s.trim().isEmpty()) ? Integer.parseInt(s.trim()) : PlatinenDatabaseHelper.DEFAULT_FERNSTEUERN_INTERNET_POLLING_MS;
                } catch (Exception e) {
                    c.internetPollingMs = PlatinenDatabaseHelper.DEFAULT_FERNSTEUERN_INTERNET_POLLING_MS;
                }
                try {
                    String v = excelread.getCellString(2, 8);
                    c.timeServerEinAus = (v != null) ? v.trim() : "";
                    v = excelread.getCellString(2, 9);
                    c.timeServerIp = (v != null) ? v.trim() : "";
                    v = excelread.getCellString(2, 10);
                    c.timeServerMaxOffsetMinuten = (v != null) ? v.trim() : "";
                    v = excelread.getCellString(2, 11);
                    c.timeServerAbfrageIntervallMs = (v != null) ? v.trim() : "";
                } catch (Exception e) { }
                try {
                    String temp = excelread.getCellString(2, 12);
                    c.serialGPSOnOff = (temp != null && "EIN".equals(temp.trim()));
                    String ip = excelread.getCellString(2, 13);
                    c.serialGPSIp = (ip != null) ? ip.trim() : "";
                    String portStr = excelread.getCellString(2, 14);
                    c.serialGPSPort = (portStr != null && !portStr.trim().isEmpty()) ? Integer.parseInt(portStr.trim()) : 0;
                } catch (ArrayIndexOutOfBoundsException e) {
                    new LogExcelError(1, 2, fileNameFernsteuernSpeicherOrt, 1, sourceFileName, 3441);
                } catch (Exception e) {
                    new LogExcelError(1, 2, fileNameFernsteuernSpeicherOrt, 1, sourceFileName, 3444);
                }
                try { excelread.closeWorkbook(); } catch (Exception ignored) { }
                if (dbHelper != null) {
                    dbHelper.saveFernsteuernConfig(c);
                    android.util.Log.i(sourceFileName, "Fernsteuern-Konfiguration aus Excel in Datenbank übernommen.");
                }
                applyFernsteuernConfig(c);
                return;
            } catch (BiffException e2) {
                android.util.Log.w(sourceFileName, "Fernsteuern-Speicher-Ort.xls nicht lesbar (Biff): " + e2.getMessage());
            } catch (IOException e2) {
                android.util.Log.w(sourceFileName, "Fernsteuern-Speicher-Ort.xls nicht lesbar (Permission/Datei): " + e2.getMessage());
            }
        }
        PlatinenDatabaseHelper.FernsteuernConfig c = new PlatinenDatabaseHelper.FernsteuernConfig();
        if (dbHelper != null) dbHelper.saveFernsteuernConfig(c);
        applyFernsteuernConfig(c);
    }

    /**
     * Lädt Anlagenstandort aus DB; falls keine Einträge, einmalig aus Anlagenstandort.xls lesen und in DB speichern.
     * Für UhrThread und ProgrammKontrolleActivity.
     */
    public static PlatinenDatabaseHelper.AnlagenstandortConfig loadAnlagenstandortConfig(android.content.Context context) {
        PlatinenDatabaseHelper dbHelper = context != null ? PlatinenDatabaseHelper.getInstance(context) : null;
        if (dbHelper != null && dbHelper.hasAnlagenstandortConfigInDb()) {
            return dbHelper.getAnlagenstandortConfig();
        }
        String path = sdCardPath + "/Turmtechnik/Config/Anlagenstandort.xls";
        java.io.File f = new java.io.File(path);
        if (f.exists()) {
            ExcelRead excelread = new ExcelRead();
            try {
                excelread.openXls(path);
                PlatinenDatabaseHelper.AnlagenstandortConfig c = new PlatinenDatabaseHelper.AnlagenstandortConfig();
                c.breitengrad = excelread.getCellString(0, 1); if (c.breitengrad == null) c.breitengrad = "";
                c.laengengrad = excelread.getCellString(1, 1); if (c.laengengrad == null) c.laengengrad = "";
                c.anpassungSonnenaufgangMinuten = excelread.getCellString(2, 1); if (c.anpassungSonnenaufgangMinuten == null) c.anpassungSonnenaufgangMinuten = "0";
                c.anpassungSonnenuntergangMinuten = excelread.getCellString(3, 1); if (c.anpassungSonnenuntergangMinuten == null) c.anpassungSonnenuntergangMinuten = "0";
                c.rundenMinuten = excelread.getCellString(4, 1); if (c.rundenMinuten == null) c.rundenMinuten = "5";
                excelread.closeWorkbook();
                if (dbHelper != null) { dbHelper.saveAnlagenstandortConfig(c); android.util.Log.i("TurmtechnikActivity", "Anlagenstandort aus Excel in DB übernommen."); }
                return c;
            } catch (Exception e) {
                try { excelread.closeWorkbook(); } catch (Exception ignored) { }
            }
        }
        PlatinenDatabaseHelper.AnlagenstandortConfig c = new PlatinenDatabaseHelper.AnlagenstandortConfig();
        if (dbHelper != null) dbHelper.saveAnlagenstandortConfig(c);
        return c;
    }

    private void initSonderprogrammeVectoren() // == Benutzer Melodien
    {
        // Excel-Datei wird nicht mehr benötigt – Benutzerprogramme kommen aus DB/Web-UI
        File excelFile = new File(benutzerMelodienFileString);
        if (!excelFile.exists()) {
            ensureBenutzerprogrammeSize(BENUTZERPROGRAMME_MAX);
            StaticVariable.firstStartMelodie = false;
            return;
        }

        String textTemp = "";
        ExcelRead excelread = new ExcelRead();
        try {
            excelread.openXls(benutzerMelodienFileString);
        } catch (BiffException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        int zeilen = excelread.getCellZeilen();
        Log.d("initSonderprogramm", "zeilen vorher=" + zeilen);

        for (int i = 0; i < zeilen; i++) {
            String tempText = "";
            try {
                tempText = excelread.getCellString(0, i);
            } catch (ArrayIndexOutOfBoundsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if ((tempText.equals("") || tempText.equals("null"))) {
                zeilen = i;
                break;
            }
        }
        Log.d("initSonderprogramm", "zeilen nachher=" + zeilen);

        calendar = Calendar.getInstance();

        int minute = calendar.get(Calendar.MINUTE);  // minuten takt von der Echtzeit uhr
        int hour_of_day = calendar.get(Calendar.HOUR_OF_DAY);
        int day_of_month = calendar.get(Calendar.DAY_OF_MONTH);
        int month = (calendar.get((Calendar.MONTH)));
        int year = (calendar.get((Calendar.YEAR)));

        SharedPreferences pref = getSharedPreferences("Turmtechnik", 0);


        final int maxBenutzerprogramme = 20; // wie BENUTZERPROGRAMME_MAX / Web-UI
        for (int i = 4; i < zeilen; i++) {
            if (StaticVariable.benutzerMelodieTasteOn2.size() >= maxBenutzerprogramme) break;
            try {
                textTemp = excelread.getCellString(0, i);
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (textTemp == null || textTemp.equals("")) {
                break;
            }

            StaticVariable.benutzerMelodieTasteOn2.add(pref.getBoolean("bmtOn" + (i - 4), false));
            StaticVariable.sofortStartButtonOn.add(false);


            StaticVariable.stunden.add(pref.getInt("stunden" + (i - 4), hour_of_day));
            StaticVariable.minuten.add(pref.getInt("minuten" + (i - 4), minute));
            StaticVariable.tage.add(pref.getInt("tage" + (i - 4), day_of_month));
            StaticVariable.monate.add(pref.getInt("monate" + (i - 4), month));
            StaticVariable.jahre.add(pref.getInt("jahre" + (i - 4), year));

            String melodieStr = "";
            try {
                melodieStr = excelread.getCellString(2, i);
            } catch (Exception e) {
                // Spalte 2 = Melodiename in Benutzermelodien.xls
            }
            StaticVariable.benutzerMelodieName.add(melodieStr != null ? melodieStr : "");
        }
        Log.d("initSonderVectoren", "benutzerMelodieTasteOn2 size=" + StaticVariable.benutzerMelodieTasteOn2.size());


        //int sizeTemp = StaticVariable.jahre.size() ;

        //for(int i = 0 ; i < sizeTemp; i ++)
        //{
        //	initDateAndTime(i);
        //}

        StaticVariable.firstStartMelodie = false;
        excelread.closeWorkbook();
    }

    private int getMinuten(String excelZeit) {
        if (excelZeit == null || (excelZeit = excelZeit.trim()).isEmpty()) {
            return 0;
        }
        String[] zeitSplit = excelZeit.split(":");
        if (zeitSplit.length < 2) {
            return 0;
        }
        try {
            int stunden = Integer.parseInt(zeitSplit[0].trim());
            int minuten = Integer.parseInt(zeitSplit[1].trim());
            return (stunden * 60) + minuten;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Lädt Schlagwerk-Zeiten und Optionen aus der DB (schlagwerk_config Typ 1 und 2).
     * @return true wenn beide Typen in der DB vorhanden und mit gültigen Zeiten befüllt sind, sonst false.
     */
    private boolean loadSchlagwerkFromDB() {
        try {
            Context ctx = TurmtechnikActivity.turmtechnikContext;
            if (ctx == null) return false;
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(ctx);
            PlatinenDatabaseHelper.SchlagwerkConfig c1 = dbHelper.getSchlagwerkConfigByTyp(1);
            PlatinenDatabaseHelper.SchlagwerkConfig c2 = dbHelper.getSchlagwerkConfigByTyp(2);
            if (c1 == null || c2 == null) return false;
            if (c1.startZeit == null || c1.startZeit.trim().isEmpty() || c1.endeZeit == null || c1.endeZeit.trim().isEmpty())
                return false;
            if (c2.startZeit == null || c2.startZeit.trim().isEmpty() || c2.endeZeit == null || c2.endeZeit.trim().isEmpty())
                return false;
            StaticVariable.beginnSchlagwerk1 = getMinuten(c1.startZeit);
            StaticVariable.endeSchlagwerk1 = getMinuten(c1.endeZeit);
            StaticVariable.beginnSchlagwerk2 = getMinuten(c2.startZeit);
            StaticVariable.endeSchlagwerk2 = getMinuten(c2.endeZeit);
            StaticVariable.schlagwerkWaehrendMelodieLeuten = c1.schlagenWennMelodieLaeuft;
            return true;
        } catch (Exception e) {
            Log.e("TurmtechnikActivity", "Schlagwerk aus DB laden: " + e.getMessage(), e);
            return false;
        }
    }

    /** Gibt 0 zurück bei null, leerem String oder ungültiger Zahl (für Stunden/Minuten aus Excel). */
    private static int parseStundeOderMinuteOrZero(String s) {
        if (s == null || (s = s.trim()).isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void loadVersionString() {

        StaticVariable.versionString = (getResources().getString(R.string.title_activity_turmtechnik));
    }


    public void doSofortStartBenuzerMelodien(int index) {
        StaticVariable.benutzerMelodienIndex = index;
        initDateAndTime(index); // momentanes Datum, und momentane Zeit + 1 Minute
        StaticVariable.benutzerMelodieTasteOn2.set(index, true);

    }


    private void initDateAndTime(int index) {
        calendar = Calendar.getInstance();

        int minute = calendar.get(Calendar.MINUTE);  // minuten takt von der Echtzeit uhr
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        minute++;
        if (minute > 59) {
            minute = 0;
            hour++;
            if (hour > 23) {
                hour = 0;
            }
        }

        StaticVariable.minuten.set(index, minute);
        StaticVariable.stunden.set(index, hour);

        StaticVariable.tage.set(index, (calendar.get(Calendar.DAY_OF_MONTH)));
        StaticVariable.monate.set(index, (calendar.get(Calendar.MONTH) + 1)); // 1-12 (Januar=1)
        StaticVariable.jahre.set(index, (calendar.get((Calendar.YEAR))));

    }

    // muss man vor initialisieren wegen sofort Start vom Internet (max 20 wie BENUTZERPROGRAMME_MAX)
    private void initBenutzerMelodieTasteOn2() {
        final int maxBenutzerprogramme = 20;
        for (int i = 0; i < maxBenutzerprogramme; i++) {
            StaticVariable.benutzerMelodieTasteOn2.add(false);
            StaticVariable.sofortStartButtonOn.add(false);
        }
    }
	    
	 
/* diese Version funktioniert nicht!
	 private void reBoot()
	 {
		 Log.e("Achtung" , "REBOOT") ;
		    try 
		 	{
			    
		    	Runtime.getRuntime().exec("su");
			    Runtime.getRuntime().exec("reboot");
			    Log.e("su", "reboot") ;
    		}
		 	catch (IOException e)
		 	{
		 		Log.e("leider", "fehlgeschlagen");
		 	}               
	 }
*/
	 
/* funktioniert leider auch nicht
	 private void reBoot()
	 {
		 Log.e("Achtung" , "REBOOT") ;
		 try
		 {
			    Process p = Runtime.getRuntime().exec("su");
			    OutputStream os = p.getOutputStream();                                       
			    os.write("reboot\n\r".getBytes());
			    os.flush();
			    Log.e("su", "reboot") ;
		 }
		 catch(IOException ioe)
		 {
			 Log.e("leider", "fehlgeschlagen");
		 }
	 }
*/

/*  //funktioniert auch nicht
  
	 private void rebootSU()
	 {
		 Log.e("Achtung" , "REBOOT") ;
		 try {
			 Runtime.getRuntime().exec(new String[]{"/system/bin/su","-c","reboot now"});
			 
			 Log.e("su", "reboot") ;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			 Log.e("leider", "fehlgeschlagen");
		}
	 }
*/
	 
/*
	 public static void rebootSU()
	 {
            Runtime runtime = Runtime.getRuntime();
	        Process proc = null;
	        OutputStreamWriter osw = null;
	        StringBuilder sbstdOut = new StringBuilder();
	        StringBuilder sbstdErr = new StringBuilder();

	        String command="/system/bin/reboot";

	        try
	        { // Run Script

	        	proc = runtime.exec("su");
	            osw = new OutputStreamWriter(proc.getOutputStream());
	            osw.write(command);
                osw.flush();
	            osw.close();

	        }
	        catch (IOException ex)
	        {
	            ex.printStackTrace();
	        }
        	finally
        	{
	            if (osw != null)
	            {
	                try
	                {
	                    osw.close();
	                }
	                catch (IOException e)
	                {
	                    e.printStackTrace();                    
	                }
	            }
	        }
	        try 
	        {
	        	if (proc != null)
	        	{
	                proc.waitFor();
	        	}
	        }
	        catch (InterruptedException e)
	        {
	            e.printStackTrace();
	        }
	 } ;
*/

    private void rebootSU() {
        try {
            Process proc = Runtime.getRuntime()
                    .exec(new String[]{"su", "-c", "reboot"});


            proc.waitFor();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void saveVerknuepfteTasten_and_automaticTaste() {
        saveVerknuepfteTastenFromBackground(this);
    }

    /** Kann von MelodieThreadNew (nach einmaligem Ausschalten der verknüpften Taste) aufgerufen werden – speichert Zustand in DB und SharedPreferences. */
    public static void saveVerknuepfteTastenFromBackground(android.content.Context ctx) {
        if (ctx == null) ctx = turmtechnikContext;
        if (ctx == null) return;
        try {
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(ctx);
            for (int i = 0; i < RELAIS_COUNT; i++) {
                boolean on;
                synchronized (VERKNUEPFTE_TASTEN_LOCK) { on = Boolean.TRUE.equals(verknuepfteTastenOn[i]); }
                dbHelper.setConfigValue("verknuepft_" + i, on ? "1" : "0");
            }
            dbHelper.setConfigValue("automatic", flagAutomaticOnOff ? "1" : "0");
            Log.d("TurmtechnikActivity", "Tastenzustände und Automatik in DB gespeichert: automatic=" + flagAutomaticOnOff);
        } catch (Exception e) {
            Log.e("TurmtechnikActivity", "Fehler beim Speichern der Tastenzustände in DB", e);
        }
        android.content.SharedPreferences pref = ctx.getSharedPreferences("Turmtechnik", 0);
        android.content.SharedPreferences.Editor editor = pref.edit();
        for (int i = 0; i < RELAIS_COUNT; i++) {
            boolean on;
            synchronized (VERKNUEPFTE_TASTEN_LOCK) { on = Boolean.TRUE.equals(verknuepfteTastenOn[i]); }
            editor.putBoolean("verknuepft_" + i, on);
        }
        editor.putBoolean("automatic", flagAutomaticOnOff);
        editor.apply();
    }

    /**
     * Setzt den Zustand einer Verknüpften Taste von außen (z. B. Web-UI) und synchronisiert DB, SharedPreferences und UhrThread.
     * Index = Verknüpft-Index (0 = erste Verknüpft-Taste im Grid, 1 = zweite, …).
     */
    public static void setVerknuepfteTasteFromWeb(android.content.Context ctx, int index, boolean on) {
        if (ctx == null) ctx = turmtechnikContext;
        if (ctx == null || index < 0 || index >= RELAIS_COUNT) return;
        synchronized (VERKNUEPFTE_TASTEN_LOCK) {
            verknuepfteTastenOn[index] = on;
        }
        try {
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(ctx);
            dbHelper.setConfigValue("verknuepft_" + index, on ? "1" : "0");
        } catch (Exception e) {
            android.util.Log.e("TurmtechnikActivity", "setVerknuepfteTasteFromWeb: DB-Fehler", e);
        }
        android.content.SharedPreferences pref = ctx.getSharedPreferences("Turmtechnik", 0);
        pref.edit().putBoolean("verknuepft_" + index, on).apply();
        UhrThread.newSearchAutomaticStart = true;
        StaticVariable.refreshInfoTextVerknuepfteTaste = true;
    }

    /**
     * Löst eine Hammer-Taste von der Web-UI aus (wie Tipp in der App).
     * gridIndex = Zeilen-Index im Tastengrid (0–47 bzw. 0 bis RELAIS_COUNT-1).
     */
    public static void triggerHammerFromWeb(android.content.Context ctx, int gridIndex) {
        if (gridIndex < 0 || gridIndex >= RELAIS_COUNT) return;
        if (hammerZeit[gridIndex] == null || hammerZeit[gridIndex] <= 0) return;
        Integer relNr = relaisNumber != null && gridIndex < relaisNumber.length ? relaisNumber[gridIndex] : null;
        if (relNr == null || relNr <= 0) return;
        long delay = hammerZeit[gridIndex].longValue();
        HammerManualThread t = new HammerManualThread(gridIndex, relNr, delay);
        t.start();
    }

    /**
     * Setzt den Automatik-Zustand von der Web-UI (umschaltbar wie in der App).
     * Führt die gleiche Logik wie changeAutomatic() aus, aber auf den gewünschten Zustand on.
     */
    public static void setAutomaticFromWeb(android.content.Context ctx, final boolean on) {
        final TurmtechnikActivity act = turmtechnikActivityInstance != null ? turmtechnikActivityInstance : (ctx instanceof TurmtechnikActivity ? (TurmtechnikActivity) ctx : null);
        if (act == null) return;
        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                act.applyAutomaticFromWeb(on);
            }
        });
    }

    /**
     * Wird auf UI-Thread ausgeführt: setzt Automatik auf on und aktualisiert Relais/UI.
     */
    private void applyAutomaticFromWeb(boolean on) {
        if (flagAutomaticOnOff == on) return;
        flagAutomaticOnOff = on;
        if (!on) {
            MelodieThreadNew.doRunOff();
            StaticVariable.stopBetaetigt = true;
            allMelodieRelaisOffAusfuehren();
            if (layout != null && automatikTasteIndex >= 0 && automatikTasteIndex < layout.buttons.size()) {
                layout.buttonOff(automatikTasteIndex);
            }
            StaticVariable.stringInfoTextField[0] = StaticVariable.getUebersetzung(0);
            StaticVariable.stringInfoTextField[2] = "";
        } else {
            if (layout != null && automatikTasteIndex >= 0 && automatikTasteIndex < layout.buttons.size()) {
                layout.buttonOnOK(automatikTasteIndex);
            }
            StaticVariable.stringInfoTextField[0] = StaticVariable.getUebersetzung(17);
            StaticVariable.stringInfoTextField[2] = "";
            StaticVariable.sofortStartPopupGefunden = false;
            UhrThread.newSearchAutomaticStart = true;
            StaticVariable.refreshInfoTextVerknuepfteTaste = true;
            StaticVariable.infoTextRefreshCount = 5;
        }
        // Automatik ist nur Software-Zustand (ein/aus), es wird kein Relais geschaltet.
        saveVerknuepfteTasten_and_automaticTaste();
    }

    /**
     * Löst eine Melodie- oder Schwingen-Taste von der Web-UI aus (wie Tipp in der App:
     * Relais an, Glockensound starten, Button-Anzeige auf „an“).
     */
    public static void triggerKeyFromWeb(android.content.Context ctx, final int gridIndex) {
        TurmtechnikActivity act = turmtechnikActivityInstance != null ? turmtechnikActivityInstance : (ctx instanceof TurmtechnikActivity ? (TurmtechnikActivity) ctx : null);
        if (act == null) return;
        if (gridIndex < 0 || gridIndex >= RELAIS_COUNT) return;
        Integer relNr = relaisNumber != null && gridIndex < relaisNumber.length ? relaisNumber[gridIndex] : null;
        /* Fallback: Eine Platine (24 oder 32 Relais), Taste noch nicht zugewiesen → Grid 0–23 = Relais 1–24 */
        if ((relNr == null || relNr <= 0) && ctx != null && gridIndex < 24) {
            try {
                Platine p = PlatinenDatabaseHelper.getInstance(ctx).getPlatine(1);
                if (p != null && p.relaisAnzahl >= 24) {
                    relNr = gridIndex + 1;
                }
            } catch (Exception e) { /* ignorieren */ }
        }
        if (relNr == null || relNr <= 0 || relNr >= StaticConstants.LIMIT_1000_100) return;
        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                act.applyKeyPressFromWeb(gridIndex);
            }
        });
    }

    /**
     * Wird auf UI-Thread ausgeführt: gleiche Aktion wie Tastendruck auf Melodie/Schwingen.
     */
    private void applyKeyPressFromWeb(int gridIndex) {
        if (serial_iothread == null) return;
        if (gridIndex < 0 || gridIndex >= RELAIS_COUNT) return;
        Integer rnObj = relaisNumber != null && gridIndex < relaisNumber.length ? relaisNumber[gridIndex] : null;
        if (rnObj == null && getApplicationContext() != null && gridIndex < 24) {
            try {
                Platine p = PlatinenDatabaseHelper.getInstance(this).getPlatine(1);
                if (p != null && p.relaisAnzahl >= 24) rnObj = gridIndex + 1;
            } catch (Exception e) { /* ignorieren */ }
        }
        if (rnObj == null) return;
        int rn = rnObj;
        if (rn <= 0 || rn >= StaticConstants.LIMIT_1000_100) return;
        // Toggle wie in der App: war an → aus, war aus → an
        if (Boolean.TRUE.equals(globalOn[gridIndex])) {
            globalOn[gridIndex] = false;
            stopGlockenSound(gridIndex);
            if (layout != null && gridIndex < layout.buttons.size()) {
                layout.buttonOff(gridIndex);
            }
            serial_iothread.changeRelais(rn, false);
        } else {
            globalOn[gridIndex] = true;
            startGlockenSound(gridIndex, StaticConstants.PLAY_SOUND_IMMMER);
            if (layout != null && gridIndex < layout.buttons.size()) {
                layout.buttonOnOK(gridIndex);
            }
            serial_iothread.changeRelais(rn, true);
        }
    }

    private void loadVerknuepfteTasten_and_automatic_taste() {
        try {
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(this);
            boolean fromDb = false;
            for (int i = 0; i < RELAIS_COUNT; i++) {
                String v = dbHelper.getConfigValue("verknuepft_" + i);
                if (v != null) {
                    synchronized (VERKNUEPFTE_TASTEN_LOCK) { verknuepfteTastenOn[i] = "1".equals(v) || "true".equalsIgnoreCase(v); }
                    fromDb = true;
                }
            }
            String auto = dbHelper.getConfigValue("automatic");
            if (auto != null) {
                flagAutomaticOnOff = "1".equals(auto) || "true".equalsIgnoreCase(auto);
                fromDb = true;
            }
            if (fromDb) {
                Log.d("TurmtechnikActivity", "Tastenzustände und Automatik aus DB geladen: automatic=" + flagAutomaticOnOff);
                return;
            }
        } catch (Exception e) {
            Log.e("TurmtechnikActivity", "Fehler beim Laden der Tastenzustände aus DB, nutze SharedPreferences", e);
        }
        SharedPreferences pref = getSharedPreferences("Turmtechnik", 0);
        for (int i = 0; i < RELAIS_COUNT; i++) {
            boolean on = pref.getBoolean("verknuepft_" + i, false);
            synchronized (VERKNUEPFTE_TASTEN_LOCK) { verknuepfteTastenOn[i] = on; }
        }
        flagAutomaticOnOff = pref.getBoolean("automatic", true);
        Log.d("TurmtechnikActivity", "Automatik-Status aus SharedPreferences geladen: " + flagAutomaticOnOff);
    }


    private void saveBenutzerTastenAndDateAndTime() {
        Log.e("SAVE", "BenutzerTasten");
        SharedPreferences pref = getSharedPreferences("Turmtechnik", 0);
        SharedPreferences.Editor editor = pref.edit();
        int zeilen = StaticVariable.benutzerMelodieTasteOn2.size();

        for (int i = 0; i < zeilen; i++) {
            editor.putBoolean("bmtOn" + i, (StaticVariable.benutzerMelodieTasteOn2.get(i)));
            editor.putInt("stunden" + i, (StaticVariable.stunden.get(i)));
            editor.putInt("minuten" + i, (StaticVariable.minuten.get(i)));
            editor.putInt("tage" + i, (StaticVariable.tage.get(i)));
            editor.putInt("monate" + i, (StaticVariable.monate.get(i)));
            editor.putInt("jahre" + i, (StaticVariable.jahre.get(i)));
        }
        editor.commit();

        try {
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(this);
            for (int i = 0; i < zeilen && i < 20; i++) {
                String key = "benutzer_" + i + "_";
                dbHelper.setConfigValue(key + "on", StaticVariable.benutzerMelodieTasteOn2.get(i) ? "1" : "0");
                dbHelper.setConfigValue(key + "stunde", String.valueOf(StaticVariable.stunden.get(i)));
                dbHelper.setConfigValue(key + "minute", String.valueOf(StaticVariable.minuten.get(i)));
                dbHelper.setConfigValue(key + "tag", String.valueOf(StaticVariable.tage.get(i)));
                dbHelper.setConfigValue(key + "monat", String.valueOf(StaticVariable.monate.get(i)));
                dbHelper.setConfigValue(key + "jahr", String.valueOf(StaticVariable.jahre.get(i)));
                dbHelper.setConfigValue(key + "melodie", getBenutzerMelodieNameForSlot(i));
            }
        } catch (Exception e) {
            Log.e("TurmtechnikActivity", "Fehler beim Speichern der Benutzerprogramme in DB", e);
        }
    }

    /** Max. Anzahl Benutzerprogramm-Slots (wie Web-UI / ConfigWebServer.BENUTZERPROGRAMME_MAX). */
    public static final int BENUTZERPROGRAMME_MAX = 20;

    /**
     * Stellt sicher, dass die Benutzerprogramm-Vektoren mindestens minSize, aber maximal BENUTZERPROGRAMME_MAX (20) Einträge haben.
     */
    public static void ensureBenutzerprogrammeSize(int minSize) {
        if (minSize <= 0) return;
        int cap = Math.min(minSize, BENUTZERPROGRAMME_MAX);
        int year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        while (StaticVariable.benutzerMelodieTasteOn2.size() < cap) {
            StaticVariable.benutzerMelodieTasteOn2.add(false);
            StaticVariable.sofortStartButtonOn.add(false);
        }
        while (StaticVariable.benutzerMelodieTasteOn2.size() > BENUTZERPROGRAMME_MAX) {
            StaticVariable.benutzerMelodieTasteOn2.remove(StaticVariable.benutzerMelodieTasteOn2.size() - 1);
            if (StaticVariable.sofortStartButtonOn.size() > BENUTZERPROGRAMME_MAX)
                StaticVariable.sofortStartButtonOn.remove(StaticVariable.sofortStartButtonOn.size() - 1);
        }
        while (StaticVariable.stunden.size() < cap) StaticVariable.stunden.add(12);
        while (StaticVariable.stunden.size() > BENUTZERPROGRAMME_MAX) StaticVariable.stunden.remove(StaticVariable.stunden.size() - 1);
        while (StaticVariable.minuten.size() < cap) StaticVariable.minuten.add(0);
        while (StaticVariable.minuten.size() > BENUTZERPROGRAMME_MAX) StaticVariable.minuten.remove(StaticVariable.minuten.size() - 1);
        while (StaticVariable.tage.size() < cap) StaticVariable.tage.add(1);
        while (StaticVariable.tage.size() > BENUTZERPROGRAMME_MAX) StaticVariable.tage.remove(StaticVariable.tage.size() - 1);
        while (StaticVariable.monate.size() < cap) StaticVariable.monate.add(1);
        while (StaticVariable.monate.size() > BENUTZERPROGRAMME_MAX) StaticVariable.monate.remove(StaticVariable.monate.size() - 1);
        while (StaticVariable.jahre.size() < cap) StaticVariable.jahre.add(year);
        while (StaticVariable.jahre.size() > BENUTZERPROGRAMME_MAX) StaticVariable.jahre.remove(StaticVariable.jahre.size() - 1);
        if (StaticVariable.benutzerMelodieName != null) {
            while (StaticVariable.benutzerMelodieName.size() < cap) StaticVariable.benutzerMelodieName.add("");
            while (StaticVariable.benutzerMelodieName.size() > BENUTZERPROGRAMME_MAX) StaticVariable.benutzerMelodieName.remove(StaticVariable.benutzerMelodieName.size() - 1);
        }
        if (StaticVariable.benutzerTagtypName != null) {
            while (StaticVariable.benutzerTagtypName.size() < cap) StaticVariable.benutzerTagtypName.add("");
            while (StaticVariable.benutzerTagtypName.size() > BENUTZERPROGRAMME_MAX) StaticVariable.benutzerTagtypName.remove(StaticVariable.benutzerTagtypName.size() - 1);
        }
    }

    /**
     * Speichert die Benutzerprogramme aus StaticVariable in SharedPreferences und DB.
     * Kann von BenutzerMelodienActivity (z. B. neue Tabellen-UI) aufgerufen werden.
     */
    public static void saveBenutzerprogrammeToDb(android.content.Context context) {
        if (context == null) return;
        SharedPreferences pref = context.getSharedPreferences("Turmtechnik", 0);
        SharedPreferences.Editor editor = pref.edit();
        int zeilen = StaticVariable.benutzerMelodieTasteOn2.size();
        for (int i = 0; i < zeilen; i++) {
            editor.putBoolean("bmtOn" + i, StaticVariable.benutzerMelodieTasteOn2.get(i));
            editor.putInt("stunden" + i, i < StaticVariable.stunden.size() ? StaticVariable.stunden.get(i) : 12);
            editor.putInt("minuten" + i, i < StaticVariable.minuten.size() ? StaticVariable.minuten.get(i) : 0);
            editor.putInt("tage" + i, i < StaticVariable.tage.size() ? StaticVariable.tage.get(i) : 1);
            editor.putInt("monate" + i, i < StaticVariable.monate.size() ? StaticVariable.monate.get(i) : 1);
            editor.putInt("jahre" + i, i < StaticVariable.jahre.size() ? StaticVariable.jahre.get(i) : java.util.Calendar.getInstance().get(java.util.Calendar.YEAR));
        }
        editor.commit();
        try {
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
            for (int i = 0; i < zeilen && i < 20; i++) {
                String key = "benutzer_" + i + "_";
                dbHelper.setConfigValue(key + "on", StaticVariable.benutzerMelodieTasteOn2.get(i) ? "1" : "0");
                dbHelper.setConfigValue(key + "stunde", String.valueOf(i < StaticVariable.stunden.size() ? StaticVariable.stunden.get(i) : 12));
                dbHelper.setConfigValue(key + "minute", String.valueOf(i < StaticVariable.minuten.size() ? StaticVariable.minuten.get(i) : 0));
                dbHelper.setConfigValue(key + "tag", String.valueOf(i < StaticVariable.tage.size() ? StaticVariable.tage.get(i) : 1));
                dbHelper.setConfigValue(key + "monat", String.valueOf(i < StaticVariable.monate.size() ? StaticVariable.monate.get(i) : 1));
                dbHelper.setConfigValue(key + "jahr", String.valueOf(i < StaticVariable.jahre.size() ? StaticVariable.jahre.get(i) : java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)));
                String melodie = (StaticVariable.benutzerMelodieName != null && i < StaticVariable.benutzerMelodieName.size()) ? (StaticVariable.benutzerMelodieName.get(i) != null ? StaticVariable.benutzerMelodieName.get(i).trim() : "") : "";
                dbHelper.setConfigValue(key + "melodie", melodie);
                String tagtyp = (StaticVariable.benutzerTagtypName != null && i < StaticVariable.benutzerTagtypName.size()) ? (StaticVariable.benutzerTagtypName.get(i) != null ? StaticVariable.benutzerTagtypName.get(i).trim() : "") : "";
                dbHelper.setConfigValue(key + "tagtyp", tagtyp);
            }
        } catch (Exception e) {
            Log.e("TurmtechnikActivity", "Fehler beim Speichern der Benutzerprogramme in DB", e);
        }
    }

    /** Lädt Benutzerprogramme aus der DB (io_config), falls vorhanden. Nach initSonderVectoren aufrufen. */
    private void loadBenutzerprogrammeFromDb() {
        loadBenutzerprogrammeFromDb(this);
    }

    /**
     * Lädt Benutzerprogramme aus der DB (io_config) in die StaticVariables.
     * Kann von ProgrammKontrolleActivity aufgerufen werden, damit die Programmabfrage aktuelle Benutzerprogramme anzeigt.
     * Vektoren (benutzerMelodieTasteOn2, tage, monate, jahre, stunden, minuten, benutzerMelodieName) müssen bereits die richtige Größe haben (z. B. durch initSonderVectoren).
     */
    public static void loadBenutzerprogrammeFromDb(android.content.Context context) {
        if (context == null) return;
        try {
            // Immer 20 Slots sicherstellen, damit alle Vektoren (inkl. benutzerTagtypName) die richtige Größe haben
            // und Tagtyp-Werte aus der DB eingelesen werden können.
            ensureBenutzerprogrammeSize(BENUTZERPROGRAMME_MAX);
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
            dbHelper.cleanupAlteBenutzerprogramme();
            int size = StaticVariable.benutzerMelodieTasteOn2.size();
            for (int i = 0; i < size && i < 20; i++) {
                String on = dbHelper.getConfigValue("benutzer_" + i + "_on");
                if (on != null) {
                    StaticVariable.benutzerMelodieTasteOn2.set(i, "1".equals(on) || "true".equalsIgnoreCase(on));
                }
                String v = dbHelper.getConfigValue("benutzer_" + i + "_stunde");
                if (v != null && !v.isEmpty()) StaticVariable.stunden.set(i, Integer.parseInt(v));
                v = dbHelper.getConfigValue("benutzer_" + i + "_minute");
                if (v != null && !v.isEmpty()) StaticVariable.minuten.set(i, Integer.parseInt(v));
                v = dbHelper.getConfigValue("benutzer_" + i + "_tag");
                if (v != null && !v.isEmpty()) StaticVariable.tage.set(i, Integer.parseInt(v));
                v = dbHelper.getConfigValue("benutzer_" + i + "_monat");
                if (v != null && !v.isEmpty()) {
                    int monatVal = Integer.parseInt(v);
                    if (monatVal == 0) monatVal = 1;
                    else if (monatVal < 1 || monatVal > 12) monatVal = 1;
                    StaticVariable.monate.set(i, monatVal);
                }
                v = dbHelper.getConfigValue("benutzer_" + i + "_jahr");
                if (v != null && !v.isEmpty()) StaticVariable.jahre.set(i, Integer.parseInt(v));
                v = dbHelper.getConfigValue("benutzer_" + i + "_melodie");
                if (StaticVariable.benutzerMelodieName != null && i < StaticVariable.benutzerMelodieName.size()) {
                    StaticVariable.benutzerMelodieName.set(i, (v != null && !v.isEmpty()) ? v.trim() : "");
                }
                v = dbHelper.getConfigValue("benutzer_" + i + "_tagtyp");
                if (StaticVariable.benutzerTagtypName != null && i < StaticVariable.benutzerTagtypName.size()) {
                    StaticVariable.benutzerTagtypName.set(i, (v != null && !v.isEmpty()) ? v.trim() : "");
                }
            }
        } catch (Exception e) {
            Log.e("TurmtechnikActivity", "Fehler beim Laden der Benutzerprogramme aus DB", e);
        }
    }

    /**
     * Liefert den Programmtag (Tagtyp) für ein Datum, wenn ein aktives Benutzerprogramm auf dieses Datum fällt.
     * Wird für die Tagesumschaltung genutzt: an diesem Tag gilt dann der gewählte Tagtyp (z. B. Feiertag).
     * @param tag Tag im Monat (1–31)
     * @param monat Calendar.MONTH (0–11)
     * @param jahr Jahr
     * @return Tagtyp-Name (z. B. "Normalprogramm", "Heiligabend") oder null/leer wenn kein Benutzerprogramm für dieses Datum oder kein Tagtyp gesetzt
     */
    public static String getBenutzerprogrammTagtypForDate(int tag, int monat, int jahr) {
        if (StaticVariable.benutzerMelodieTasteOn2 == null || StaticVariable.tage == null || StaticVariable.monate == null || StaticVariable.jahre == null) return null;
        int monat1based = monat + 1; // Calendar 0–11 -> 1–12
        int size = Math.min(StaticVariable.benutzerMelodieTasteOn2.size(), BENUTZERPROGRAMME_MAX);
        for (int i = 0; i < size; i++) {
            if (!Boolean.TRUE.equals(StaticVariable.benutzerMelodieTasteOn2.get(i))) continue;
            if (i >= StaticVariable.tage.size() || i >= StaticVariable.monate.size() || i >= StaticVariable.jahre.size()) continue;
            if (StaticVariable.tage.get(i) != tag) continue;
            if (StaticVariable.monate.get(i) != monat1based) continue;
            if (StaticVariable.jahre.get(i) != jahr) continue;
            String tt = (StaticVariable.benutzerTagtypName != null && i < StaticVariable.benutzerTagtypName.size()) ? StaticVariable.benutzerTagtypName.get(i) : null;
            if (tt != null && !tt.trim().isEmpty()) return tt.trim();
        }
        return null;
    }

    /**
     * Lädt Log- und Reboot-Optionen aus dem Anlagenformular (io_config).
     * Überschreibt die aus Excel gelesenen Werte, wenn in der DB gesetzt.
     * Reboot: wenn BL oder WLAN länger nicht funktioniert (errorCountToReboot).
     */
    public static void loadAnlageLogAndRebootFromDb(android.content.Context context) {
        if (context == null) return;
        try {
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
            String v = dbHelper.getConfigValue("anlage_log_abgelaufene_melodien");
            if (v != null && "aus".equalsIgnoreCase(v.trim())) {
                StaticVariable.logAbgelaufeneMelodien = false;
            } else {
                // "ein" oder Key fehlt/null -> Log für ausgeführte Melodien aktiv (Standard: ein)
                StaticVariable.logAbgelaufeneMelodien = true;
            }
            v = dbHelper.getConfigValue("anlage_log_fehler_crashes");
            if (v != null && "ein".equalsIgnoreCase(v.trim())) {
                StaticVariable.logFehlerCrashes = true;
            } else if (v != null && "aus".equalsIgnoreCase(v.trim())) {
                StaticVariable.logFehlerCrashes = false;
            }
            v = dbHelper.getConfigValue("reboot_ok_flag");
            if (v != null && ("1".equals(v.trim()) || "ein".equalsIgnoreCase(v.trim()))) {
                StaticVariable.flagRebootOk = true;
            } else if (v != null && ("0".equals(v.trim()) || "aus".equalsIgnoreCase(v.trim()))) {
                StaticVariable.flagRebootOk = false;
            }
            v = dbHelper.getConfigValue("logfile_on_off");
            if (v != null && "ein".equalsIgnoreCase(v.trim())) {
                StaticVariable.logOnOff = true;
            } else if (v != null && "aus".equalsIgnoreCase(v.trim())) {
                StaticVariable.logOnOff = false;
            }
            int powerOffPct = dbHelper.getPowerOffAkkuProzent();
            StaticVariable.batt_shut_down_level2 = powerOffPct;
        } catch (Exception e) {
            Log.e("TurmtechnikActivity", "Fehler beim Laden der Anlagen-Log/Reboot-Flags aus DB", e);
        }
    }

    /** Liefert den Melodienamen für Benutzerprogramm-Slot i (aus DB/StaticVariable oder Excel-Fallback). */
    private String getBenutzerMelodieNameForSlot(int i) {
        if (StaticVariable.benutzerMelodieName != null && i < StaticVariable.benutzerMelodieName.size()) {
            String m = StaticVariable.benutzerMelodieName.get(i);
            if (m != null && !m.trim().isEmpty()) return m;
        }
        if (BenutzerMelodienActivity.filenameSondermelodien != null && i < BenutzerMelodienActivity.filenameSondermelodien.size()) {
            return BenutzerMelodienActivity.filenameSondermelodien.get(i);
        }
        return "";
    }

     /*
	 private void testWriteJson()
	 {
		 JsonSendAndRead jsonSendAndRead = new JsonSendAndRead() ;
		 jsonSendAndRead.testWriteJson();
		 
	 }
	 private void testReadJson()
	 {
		 JsonReadThread jsonReadThread = new JsonReadThread(url_read_status) ;
		 jsonReadThread.start();
	 }
	 */
	 
/*
	 private void testReadJsonAndi()
	 {
		 JsonSendAndRead jsonSendAndRead = new JsonSendAndRead() ;
		 StaticVariable.gelesenVonJsonRequest = jsonSendAndRead.testReadJsonAndi();
	 }
*/	 


/* wurde durch ddp/meteor ersetzt	 29.06.2014	 
	 private void sendTastenStatusJson(int id, Boolean on_off)
	 {
		 String url_update_tasten =
				 "http://app.turmtechnik.com/json.php?tokenid=" + StaticVariable.fernwartungServerId
				  + "&tokenpw=" + StaticVariable.fernwartungServerPassword
				  + "&reqtype=update&status=tablet&reqtable=tasten" ;
		 JsonSendThread jsonSendThread = new JsonSendThread(url_update_tasten, createJsonArrayFromID(id, on_off) ) ;
		 jsonSendThread.start() ;
	 }
	 private void testSendMuster()
	 {
		 String url_update_tasten =
		 "http://app.turmtechnik.com/json.php?tokenid=" + StaticVariable.fernwartungServerId
		  + "&tokenpw=" + StaticVariable.fernwartungServerPassword
		  + "&reqtype=update&status=tablet&reqtable=tasten" ;
		 
		 JsonSendThread jsonSendThread = new JsonSendThread(url_update_tasten, createTestJsonArray() ) ;
		 jsonSendThread.start() ;
	 }
	 
	 private JSONArray createJsonArrayFromID(int id, Boolean on_off)
	 {
		 JSONArray jsonArray = new JSONArray() ;
		 
		 JSONObject jsonObjekt = new JSONObject() ;
		 try
		 {
			 Random r = new Random();
			 int status = 0 ;
			 if(on_off == true)
			 {
				 status++ ;
			 }	
								
			 jsonObjekt.put("id" , id);
			 jsonObjekt.put("status", status);
		 }
		 catch (JSONException e)
		 {
			// TODO Auto-generated catch block
			e.printStackTrace();
		 }
		jsonArray.put(jsonObjekt);
	 
		return jsonArray ;
	 }
	 
	 private void sendTastenStatusSeite1()
	 {
		 String url_update_tasten =
				 "http://app.turmtechnik.com/json.php?tokenid=" + StaticVariable.fernwartungServerId
				  + "&tokenpw=" + StaticVariable.fernwartungServerPassword
				  + "&reqtype=update&status=tablet&reqtable=tasten" ;
		 
		 Log.e("sendTastenStatusSeite1" , "url=" + url_update_tasten);
		 
		 JsonSendThread jsonSendThread = new JsonSendThread(url_update_tasten, createTastenStatusSeite1()) ;
		 jsonSendThread.start() ;
	 }
	 
	 private JSONArray createTastenStatusSeite1()
	 {
		 JSONArray jsonArray = new JSONArray();
		 int btnCount = layout != null ? layout.buttons.size() : layoutButtonsSize;
		 for (int i = 0; i < btnCount; i++)
		 {
			 	//JSONObject jsonObject = new JSONObject() ;
			    if (relaisNumber[i] < 100) 
				{
					int status = 0 ;
			    	if (globalOn[i] == true)
			    	{
			    		status = 1;
			    	}
			    	//try {
						//jsonObject.put("id", fernwartungID[i]);
						//jsonObject.put("status" , status) ;
					//} catch (JSONException e) {
						// TODO Auto-generated catch block
					//	e.printStackTrace();
					//}
			   	}
			    else
			    {
					if (relaisNumber[i] == 100) // Taste Stop ?
					{
						int status = 0 ; // Taste Stop immer inaktiv zurueckschicken
						//try 
						//{
							//jsonObject.put("id", fernwartungID[i]);
							//jsonObject.put("status" , status) ;
						//}
						//catch (JSONException e)
						//{
							// TODO Auto-generated catch block
						//	e.printStackTrace();
						//}
					}
					if (relaisNumber[i] == 101) // Taste Automatik ?
					{
						int status = 0 ;
						if(automaticOn == true)
						{
							status = 1 ;
						}
						//try {
							//jsonObject.put("id", fernwartungID[i]);
							//jsonObject.put("status" , status) ;
						//} catch (JSONException e) {
							// TODO Auto-generated catch block
						//	e.printStackTrace();
						//}
					}
					if (relaisNumber[i] > 2000) // verknuepfte Taste ?
					{
						int status = 0 ;
						int verknuepftOffset = relaisNumber[i] - 2001;
						//Log.e("verkn.Offset", "=" + verknuepftOffset + "[]=" + verknuepfteTastenOn[verknuepftOffset]) ;
						if (verknuepftOffset >= 0 && verknuepftOffset < verknuepfteTastenOn.length && Boolean.TRUE.equals(verknuepfteTastenOn[verknuepftOffset]))
						{
							status = 1 ;
						}
						//try {
							//jsonObject.put("id", fernwartungID[i]);
							//jsonObject.put("status" , status) ;
						//} catch (JSONException e) {
							// TODO Auto-generated catch block
						//	e.printStackTrace();
						//}
					}
				}
			    //jsonArray.put(jsonObject);
		 }
		 Log.e("jsonArray" , "=" + jsonArray.toString()) ;
		 return jsonArray ;
	 }


	 private JSONArray createTestJsonArray()
	 {
		 
		 JSONArray jsonArray = new JSONArray() ;
		 
		 //for (int i=0;i<48;i++)
		 for (int i=0; i<4; i++)
		 {
			 JSONObject jsonObject = new JSONObject() ;
			 try
			 {
				 Random r = new Random();
				 int status = r.nextInt(2);
							
				 jsonObject.put("id" , fernwartungID[i]);
				 jsonObject.put("status", status);
			 }
			 catch (JSONException e)
			 {
				// TODO Auto-generated catch block
				e.printStackTrace();
			 }
			jsonArray.put(jsonObject);
		 }
		 return jsonArray ;
	 }


	 private void tastenVerarbeiten()
	 {
		 if (serial_iothread == null) return;
		 Boolean clrAll = false ;
		 
		 Log.e("tastenVerarbeiten" , "JSON String=" + StaticVariable.gelesenVonJsonRequest) ;
		 try 
		 {
			JSONArray jsonArray = new JSONArray(StaticVariable.gelesenVonJsonRequest) ;
			
			for (int i = 0; i<jsonArray.length();i++)
			{	
				JSONObject jsonObjekt = jsonArray.getJSONObject(fernwartungID[i]); // das erste object holen
				
				String stringObject = 
				jsonObjekt.getString("status");  // string key o , Antwort = 0 = offline
				
				//hier fragen ob verknuepft, oder automatic oder stop
				// --> TO DO
				if (relaisNumber[i] < 100)  // keine
										    // Taste
											// stop (
											// relais#
											// 100 ) ?
											// also normale relais nummer?
				{
					if(stringObject.equals("1"))
					{
						globalOn[i] = true;
						serial_iothread.changeRelais(relaisNumber[i],true);
					}
					else
					{
						globalOn[i] = false;
						serial_iothread.changeRelais(relaisNumber[i],false);
					}
					LogTurmtechnik logTemp =
					new LogTurmtechnik("Glocke/Kloeppel Fernsteuerung", 0, 0, 0, 0) ;
					logTemp = null ;
				}
				else // relais nummer >= 100
				{
					// 2.3.2014 --> da kommt immer 1 zurueck fuer die Taste Stop!
					if( (relaisNumber[i] == 100) && (stringObject.equals("1")) ) // Stop Taste?
					{
						clrAll = true ;
					}
					
					else
					{	
						if (relaisNumber[i] > 2000) // ist
															// es
															// eine
															// verknuepfte
															// Taste?
						{
							int offset = (relaisNumber[i] - 2001);
							offset = Math.max(offset, 0);
							offset = Math.min(RELAIS_COUNT, offset);	
							Log.e("offset verkn." , "=" + offset);
							if (stringObject.equals("0") )
							{
								synchronized (VERKNUEPFTE_TASTEN_LOCK) { verknuepfteTastenOn[offset] = false; }
							
							}
							else
							{
								synchronized (VERKNUEPFTE_TASTEN_LOCK) { verknuepfteTastenOn[offset] = true; }
							}
						
							LogTurmtechnik logTemp =
									new LogTurmtechnik("verknuepfte Taste Fernsteuerung", 0, 0, 0, 0) ;
							logTemp = null ;
							StaticVariable.changeInternetVerknuepfteTasten++;
						}
					}
			
				}
			}	

			
		 }
		 catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		 }
		 
		 if(clrAll)
		 {
				Log.e("CLR" , "ALL");
			    MelodieThread.doRunOff();
				UhrThread.blockReady = 0; // block fertig ,
											// wieder ready
				stringInfoText = "suche nächsten Start...";
				UhrThread.newSearchAutomaticStart = true;
				allMelodieRelaisOffInternet();
											
				//StaticVariable.changeInternetBenutzerprogramme++ ;
				//StaticVariable.changeInternetVerknuepfteTasten++ ;
				LogTurmtechnik logTemp =
				new LogTurmtechnik("Stop vom Internet empfangen ", 0, 0, 0, 0) ;
				logTemp = null ;
				System.gc();
				sendTastenStatusSeite1();
		 }
	 }
*/


    // private Drive getDriveService(GoogleAccountCredential credential) {
    // return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new
    // GsonFactory(), credential)
    // .build();
    // }

    // @Override
    // public boolean onCreateOptionsMenu(Menu menu) {
    // getMenuInflater().inflate(R.menu.activity_turmtechnik, menu);
    // return true;
    // }

    private static void sendTastenStatusSeite1() {

        boolean statusFlag = false;

        int totalButtons = BESCHRIFTUNG_TASTEN_SLOTS_PAGE1;

        //for (int i = 0; i < totalButtons ; i++)
        for (int i = 0; i < RELAIS_COUNT; i++) {

            if ((relaisNumber[i] < StaticConstants.LIMIT_1000_100) && (relaisNumber[i] != 0)) {
                if (globalOn[i] == true) {
                    statusFlag = true;
                } else {
                    statusFlag = false;
                }
            }
            if (relaisNumber[i] == StaticConstants.STOP) // Taste Stop ?
            {
                statusFlag = false; // Taste Stop immer inaktiv zurueckschicken
            }
            if (relaisNumber[i] != null && (relaisNumber[i] == StaticConstants.AUTOMATIC || relaisNumber[i] == 101)) // Taste Automatik
            {
                if (flagAutomaticOnOff == true) {
                    statusFlag = true;
                } else {
                    statusFlag = false;
                }
            }
            if (relaisNumber[i] == StaticConstants.SCHLAGWERK_ON_OFF) // 104) // Taste Schlagwerk on/off ?
            {
                if (StaticVariable.flagSchlagwerkOnOff == true) {
                    statusFlag = true;
                } else {
                    statusFlag = false;
                }
            }
            if ((relaisNumber[i] > 2000) && (relaisNumber[i] < 3000)) // verknuepfte Taste ?
            {
                statusFlag = false;
                int verknuepftOffset = relaisNumber[i] - 2001;
                //Log.e("verkn.Offset", "=" + verknuepftOffset + "[]=" + verknuepfteTastenOn[verknuepftOffset]) ;
                if (verknuepftOffset >= 0 && verknuepftOffset < verknuepfteTastenOn.length && Boolean.TRUE.equals(verknuepfteTastenOn[verknuepftOffset])) {
                    statusFlag = true;
                }
            }

            //if( TurmtechnikActivity.relaisNumber[i] > 3000 )
            //{
            //    if((StaticVariable.sofortStartPopupFlag)
            //            && (TurmtechnikActivity.relaisNumber[i]==StaticVariable.sofortStartPopupRelaisNummerAktiv)) {
            //        layout.buttonOnOK(i);
            //        statusFlag=true;
            //    }
            //    else
            //    {
            //        layout.buttonOff(i);
            //        statusFlag=false ;
            //    }
            //}

            // Meteor Code entfernt
        }


    }

    public static int getRelaisOffsetForVerknuepfteTaste(int verknuepftOffset) {
        for (int i = 0; i < RELAIS_COUNT; i++) {
            if (relaisNumber[i] == (2001 + verknuepftOffset)) {
                return i;
            }
        }
        return -1;
    }

    // Meteor Code entfernt: sendTastenStatusMeteor()

    // Meteor Code entfernt: makeMapObject()

    // Meteor Code entfernt: changeButtonsMeteor()

    public static void changeDateAndTimeBenutzer(int index, String dateAndTime) {
        StaticVariable.benutzerMelodienIndex = index;

        String[] dateAndTimeSplit = dateAndTime.split(" "); // auf 2014-09-25
        // und 12:02 spliten

        String[] dateSplit = dateAndTimeSplit[0].split("-");
        String[] timeSplit = dateAndTimeSplit[1].split(":");

        Log.e("dateAndTimeSplit", "=" + dateAndTimeSplit[0] + "#" + dateAndTimeSplit[1]);
        Log.e("dateSplit", "=" + dateSplit[0] + "#" + dateSplit[1] + "#" + dateSplit[2]);
        Log.e("timeSplit", "=" + timeSplit[0] + "@" + timeSplit[1]);

        try {
            StaticVariable.jahre.set(index, Integer.parseInt(dateSplit[0]));
            StaticVariable.monate.set(index, (Integer.parseInt(dateSplit[1]) - 1));

            StaticVariable.tage.set(index, Integer.parseInt(dateSplit[2]));
            StaticVariable.stunden.set(index, Integer.parseInt(timeSplit[0]));
            StaticVariable.minuten.set(index, Integer.parseInt(timeSplit[1]));
        } catch (NumberFormatException nfe) {
            // wenn das nicht geht nichts tuen
        }

        BenutzerMelodienActivity.printDateAndTime();
    }

    // Meteor Code entfernt: changeButtonsMeteorBenutzerprogramm()
    // Meteor Code entfernt: changeButtonsMeteorGlocken()


    private static int sucheTasteOffsetGlocken(String rowId) {
        int tasteIndex = 0;

        for (int i = 0; i < TurmtechnikActivity.RELAIS_COUNT; i++) {
            if (buttonId[i].equals(rowId)) {
                return tasteIndex;
            }
            tasteIndex++;
        }

        return tasteIndex;
        //return -1 ;  // wenn es keine Taste Glocken, Kloeppel ... also Seite 1 oder 2 war
        // mit -1 , nicht gefunden melden
    }

    private static int sucheTasteOffsetBenutzerprogramm(String rowId) {
        int tasteIndex = 0;
        int zeilen = StaticVariable.userProgramIds.size();

        for (int i = 0; i < zeilen; i++) {
            if (StaticVariable.userProgramIds.get(i).equals(rowId)) {
                return tasteIndex;
            }
            tasteIndex++;
        }
        return tasteIndex;
    }

    public static String getButtonId(int index) {
        return buttonId[index];
    }

    public Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d("Message", "Test=" + msg.toString());
            //String vonMessage = (String) msg.obj ;

            outputView.append((String) msg.obj);
        }
    };

    private void printInfo(String infoText) {
        msg = Message.obtain();
        msg.obj = infoText;

        myHandler.sendMessage(msg);
    }

    private void printCountInfo() {
        countInfo++;

        String temp = countInfo.toString();
        printInfo(" " + temp);
    }


    private void checkFilesAndInit() {
        countInfo = 0;

        if (file_ok == true) {
            checkfile = new File(systemFileString);
            if (checkfile.exists()) {
                file_ok = true;
            } else {
                beschriftungTastenFileString = (Environment.getDataDirectory().getPath() + excell_system);
                checkfile = new File(systemFileString);
                if (checkfile.exists()) {
                    file_ok = true;
                }
            }

            if (file_ok == false) {
                // setContentView(R.layout.text_layout);
                // outputView = (TextView) this.findViewById(R.id.textView1);
                printInfo("\n\n");
                printInfo("\"" + excell_system
                        + "\" \n\n   nicht vorhanden \n\n");
            }
        }


        if (file_ok == true) {
            // Log.i("check", "programmliste");
            file_ok = false;

            // Prüfe zuerst, ob Programme in der Datenbank existieren
            boolean programmeInDB = false;
            try {
                android.content.Context context = TurmtechnikActivity.turmtechnikContext;
                if (context != null) {
                    PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
                    java.util.List<Programm> programme = dbHelper.getProgrammeByTagtyp("Normalprogramm");
                    if (programme != null && !programme.isEmpty()) {
                        programmeInDB = true;
                        file_ok = true;
                        Log.d("TurmtechnikActivity", "Normalprogramm in DB gefunden (" + programme.size() + " Programme), Excel-Datei nicht erforderlich");
                    }
                }
            } catch (Exception e) {
                Log.e("TurmtechnikActivity", "Fehler beim Prüfen der DB für Normalprogramm", e);
            }
            
            // Nur Excel-Datei prüfen, wenn keine Programme in DB sind
            if (!programmeInDB) {
                normalprogrammFileString = (Environment
                        .getExternalStorageDirectory().getPath() + (StaticConstants.normalprogrammString));
                // Log.i("prgListString" , "=" + normalprogrammFileString) ;
                checkfile = new File(normalprogrammFileString);
                if (checkfile.exists()) {
                    // Log.i("prog sd", "liste vorhanden");
                    file_ok = true;
                } else {
                    normalprogrammFileString = (Environment.getDataDirectory()
                            .getPath() + (StaticConstants.normalprogrammString));
                    checkfile = new File(normalprogrammFileString);
                    if (checkfile.exists()) {
                        // Log.i("prog tel", "liste vorhanden");
                        file_ok = true;
                    }
                }
                // Log.i("file_ok" , "=" + file_ok ) ;
                if (file_ok == false) {
                    // setContentView(R.layout.text_layout);
                    // outputView = (TextView) this.findViewById(R.id.textView1);
                    printInfo("\n\n");

                    printInfo("\"" + normalprogrammFileString
                            + "\" \n\n   nicht vorhanden \n\n");
                }
            }
        }

        if (file_ok == true) {
            // Log.i("check", "benutzermelodien");
            file_ok = false;

            benutzerMelodienFileString = (Environment
                    .getExternalStorageDirectory().getPath() + (StaticConstants.benutzerMelodienString));
            // Log.i("prgListString" , "=" + benutzerMelodienFileString) ;
            checkfile = new File(benutzerMelodienFileString);
            if (checkfile.exists()) {
                // Log.i("prog sd", "benutzer Melodien vorhanden");
                file_ok = true;
            } else {
                normalprogrammFileString = (Environment.getDataDirectory()
                        .getPath() + (StaticConstants.benutzerMelodienString));
                checkfile = new File(benutzerMelodienFileString);
                if (checkfile.exists()) {
                    // Log.i("prog tel", "benutzer Melodien vorhanden");
                    file_ok = true;
                }
            }
            // Benutzermelodien kommen aus DB/Web-UI – Excel wird nicht mehr benötigt
            if (file_ok == false) {
                try {
                    android.content.Context ctx = TurmtechnikActivity.turmtechnikContext;
                    if (ctx != null) {
                        PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(ctx);
                        String v = dbHelper.getConfigValue("benutzer_0_on");
                        if (v != null || dbHelper.getConfigValue("benutzer_0_melodie") != null) {
                            file_ok = true;
                        }
                    }
                } catch (Exception e) {
                    Log.e("TurmtechnikActivity", "Fehler beim Prüfen der DB für Benutzermelodien", e);
                }
            }
            if (file_ok == false) {
                file_ok = true; // Start trotzdem: loadBenutzerprogrammeFromDb() legt 20 Slots an und lädt aus DB
            }
        }

        // 9.6.13 umgebaut auf Config/System.xls sheet

        newFesteFesttageFileString = (sdCardPath + StaticConstants.excellSystemString);
        newVariableFesttageFileString = (sdCardPath + StaticConstants.excellSystemString);


        bluetoothConfigFileString = (Environment
                .getExternalStorageDirectory().getPath() + (StaticConstants.excellSystemString));
        // 7.6.13. das ist jetzt in system.xls sheet 14

			/*
			 * if (file_ok==true) { Log.i("check", "Bluetooth_config");
			 * file_ok=false;
			 * 
			 * bluetoothConfigFileString =
			 * (Environment.getExternalStorageDirectory
			 * ().getPath()+(StaticConstants.bluetoothConfigString)) ;
			 * Log.i("relaisAnsteuerung" , "=" + normalprogrammFileString) ;
			 * checkfile = new File(bluetoothConfigFileString); if
			 * (checkfile.exists()) { Log.i("prog sd", "config vorhanden");
			 * file_ok = true; } else { normalprogrammFileString =
			 * (Environment.getDataDirectory
			 * ().getPath()+(StaticConstants.bluetoothConfigString)); checkfile
			 * = new File(bluetoothConfigFileString); if (checkfile.exists()) {
			 * Log.i("prog tel", "config vorhanden"); file_ok = true ; } }
			 * Log.i("file_ok" , "=" + file_ok ) ; if (file_ok==false) { //
			 * setContentView(R.layout.text_layout); // outputView = (TextView)
			 * this.findViewById(R.id.textView1); printInfo("\n\n");
			 * 
			 * 
			 * printInfo("\"" + normalprogrammFileString +
			 * "\" \n\n   nicht vorhanden\n\n"); } }
			 */

        // 10.6.2013 nebenuhr wir in /Config/System.xls sheet 15 abgefragt
			/*
			 * if (file_ok==true) { Log.i("check", "Nebenuhr"); file_ok=false;
			 * 
			 * nebenUhrFileString =
			 * (Environment.getExternalStorageDirectory().getPath
			 * ()+(StaticConstants.nebenUhrString)) ;
			 * 
			 * checkfile = new File(nebenUhrFileString); if (checkfile.exists())
			 * { Log.i("prog sd", "Nebenuhr vorhanden"); file_ok = true; } else
			 * { nebenUhrFileString =
			 * (Environment.getDataDirectory().getPath()+(
			 * StaticConstants.nebenUhrString)); checkfile = new
			 * File(nebenUhrFileString); if (checkfile.exists()) {
			 * Log.i("speicher tel", "Nebenuhr vorhanden"); file_ok = true ; } }
			 * if (file_ok==false) { // setContentView(R.layout.text_layout); //
			 * outputView = (TextView) this.findViewById(R.id.textView1);
			 * printInfo("\n\n");
			 * 
			 * printInfo("\"" + nebenUhrFileString +
			 * "\" \n\n   nicht vorhanden\n\n"); } }
			 */
			
			/*
			 * if (file_ok==true) { Log.i("check", "Feste Festtage");
			 * file_ok=false;
			 * 
			 * festeFesttageFileString = (sdCardPath +
			 * (StaticConstants.festeFesttageString)) ; checkfile = new
			 * File(festeFesttageFileString); if (checkfile.exists()) { file_ok
			 * = true; } else { printInfo("\n\n\"" +
			 * festeFesttageFileString + "\" \n\n   nicht vorhanden\n\n"); } }
			 * 
			 * if (file_ok==true) { Log.i("check", "variable Festtage");
			 * file_ok=false;
			 * 
			 * variableFesttageFileString = (sdCardPath +
			 * (StaticConstants.variableFesttageString)) ; checkfile = new
			 * File(variableFesttageFileString); if (checkfile.exists()) {
			 * file_ok = true; } else { printInfo("\n\n\"" +
			 * variableFesttageFileString + "\" \n\n   nicht vorhanden\n\n"); }
			 * }
			 */
        if (file_ok == true) {
            // Schlagwerk: zuerst aus DB laden, sonst aus Schlagwerkzeiten.xls
            file_ok = false;
            boolean schlagwerkAusDB = loadSchlagwerkFromDB();
            if (schlagwerkAusDB) {
                file_ok = true;
                Log.d("TurmtechnikActivity", "Schlagwerk aus DB geladen");
            }
            if (!schlagwerkAusDB) {
                schlagwerkZeitenFileString = (Environment
                        .getExternalStorageDirectory().getPath() + (StaticConstants.schlagwerkString));

                checkfile = new File(schlagwerkZeitenFileString);
                if (checkfile.exists()) {
                    file_ok = true;
                } else {
                    schlagwerkZeitenFileString = (Environment
                            .getDataDirectory().getPath() + (StaticConstants.schlagwerkString));
                    checkfile = new File(schlagwerkZeitenFileString);
                    if (checkfile.exists()) {
                        file_ok = true;
                    }
                }
            }

            if (!schlagwerkAusDB && file_ok == false) {
                printInfo("\n\n");
                printInfo("\"" + schlagwerkZeitenFileString
                        + "\" \n\n   nicht vorhanden \n\n");
            } else if (!schlagwerkAusDB && file_ok == true) {
                // 26.9.2013 im schlagwerk thread ist das zu langsam
                ExcelRead swExcelread = new ExcelRead();
                boolean schlagwerkExcelOk = false;
                try {
                    swExcelread.openXls(TurmtechnikActivity.schlagwerkZeitenFileString);
                    schlagwerkExcelOk = true;
                } catch (BiffException e1) {
                    e1.printStackTrace();
                    new LogExcelError(-1, -1, schlagwerkZeitenFileString, -1, sourceFileName, 540);
                } catch (IOException e1) {
                    android.util.Log.w("TurmtechnikActivity", "Schlagwerkzeiten.xls nicht lesbar: " + e1.getMessage());
                    new LogExcelError(-1, -1, schlagwerkZeitenFileString, -1, sourceFileName, 544);
                }
                if (!schlagwerkExcelOk) {
                    // Datei nicht geöffnet (Permission/Fehler) – Schlagwerk-Standards belassen
                } else {
                Log.e("excel", "geoeffnet");
                String excelZeit = "0";

                try {
                    excelZeit = swExcelread.getCellString(0, 2);
                } catch (ArrayIndexOutOfBoundsException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    new LogExcelError(0, 2, TurmtechnikActivity.schlagwerkZeitenFileString, 0, sourceFileName, 554);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    new LogExcelError(0, 2, TurmtechnikActivity.schlagwerkZeitenFileString, 0, sourceFileName, 558);
                } // beginn schlagwerk 1
                //	Log.e("excelZeit" , "=" + excelZeit) ;
                StaticVariable.beginnSchlagwerk1 = getMinuten(excelZeit);

                try {
                    excelZeit = swExcelread.getCellString(1, 2);
                } catch (ArrayIndexOutOfBoundsException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    new LogExcelError(1, 2, TurmtechnikActivity.schlagwerkZeitenFileString, 0, sourceFileName, 568);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    new LogExcelError(1, 2, TurmtechnikActivity.schlagwerkZeitenFileString, 0, sourceFileName, 572);
                } // ende schlagwerk 2
                Log.e("excelZeit", "=" + excelZeit);
                StaticVariable.endeSchlagwerk1 = getMinuten(excelZeit);

                try {
                    excelZeit = swExcelread.getCellString(0, 3);
                } catch (ArrayIndexOutOfBoundsException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    new LogExcelError(0, 3, TurmtechnikActivity.schlagwerkZeitenFileString, 0, sourceFileName, 582);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    new LogExcelError(0, 3, TurmtechnikActivity.schlagwerkZeitenFileString, 0, sourceFileName, 586);
                } // beginn schlagwerk 1
                Log.e("excelZeit", "=" + excelZeit);
                StaticVariable.beginnSchlagwerk2 = getMinuten(excelZeit);

                try {
                    excelZeit = swExcelread.getCellString(1, 3);
                } catch (ArrayIndexOutOfBoundsException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    new LogExcelError(1, 3, TurmtechnikActivity.schlagwerkZeitenFileString, 0, sourceFileName, 596);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    new LogExcelError(1, 3, TurmtechnikActivity.schlagwerkZeitenFileString, 0, sourceFileName, 600);
                } // ende schlagwerk 2
                Log.e("excelZeit", "=" + excelZeit);
                StaticVariable.endeSchlagwerk2 = getMinuten(excelZeit);

                try {
                    if (swExcelread.getCellString(1, 4).equals("EIN")) {
                        StaticVariable.schlagwerkWaehrendMelodieLeuten = true;
                    } else {
                        StaticVariable.schlagwerkWaehrendMelodieLeuten = false;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                Log.e("Schlagwerk", "Ein/Aus =" + StaticVariable.schlagwerkWaehrendMelodieLeuten);

                swExcelread.closeWorkbook();
                }
            }
        }

        if (file_ok == true) {
            file_ok = false;

            String textSizeConfig = sdCardPath
                    + StaticConstants.FERNSTEUERN_SPEICHER_ORT_STRING;
            checkfile = new File(textSizeConfig);
            if (checkfile.exists()) {
                file_ok = true;
            }
            if (file_ok == false) {
                printInfo("\n\n\"" + textSizeConfig
                        + "\" \n\n   nicht vorhanden \n\n");

            }
        }

        if (file_ok == true) {

            file_ok = false;
            file_ok = true;  // 2.7.2014 nicht mehr in Funktion
				/*		
				ExcelRead excelread = new ExcelRead();
				Log.i("vor", "excel read open");
				try {
					excelread.openXlsSheet(sdCardPath
							+ StaticConstants.FERNSTEUERN_SPEICHER_ORT_STRING, 1);
				} catch (BiffException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} // sheet
																				// 1
																		// !!!
				Log.i("nach", "excel read open");
				String textSizeConfig = "" ;
				try {
					textSizeConfig = (sdCardPath + "/Turmtechnik/Config/"
							+ excelread.getCellString(2, 1) + ".xls");
				} catch (ArrayIndexOutOfBoundsException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} // get Filename
				Log.i("nach", "getCellString");
				excelread.closeWorkbook();
			//	Log.i("File name von", "Tabelle=" + textSizeConfig);
				checkfile = new File(textSizeConfig);
				if (checkfile.exists()) {
					file_ok = true;
				} else {
					printInfo("\n\n\"" + textSizeConfig
							+ "\" \n\n   nicht vorhanden 6 \n\n");
				}
				*/
        }

        if (file_ok == true) {
            file_ok = false;
            String textSizeConfig = (sdCardPath + "/Turmtechnik/Config/TextSize_config.xls");
            checkfile = new File(textSizeConfig);
            if (checkfile.exists()) {
                file_ok = true;
            }
            if (file_ok == false) {
                printInfo("\n\n\"" + textSizeConfig
                        + "\" \n\n   nicht vorhanden \n\n");
            }
        }

        if (file_ok == true) {
            file_ok = false;
            Log.e("check", "Anlagenstandort");
            PlatinenDatabaseHelper dbHelperAnlage = (turmtechnikContext != null) ? PlatinenDatabaseHelper.getInstance(turmtechnikContext) : null;
            if (dbHelperAnlage != null && dbHelperAnlage.hasAnlagenstandortConfigInDb()) {
                Log.e("check", "Anlagenstandort aus DB");
                file_ok = true;
            } else {
                String anlagestandortConfig = (sdCardPath + "/Turmtechnik/Config/Anlagenstandort.xls");
                checkfile = new File(anlagestandortConfig);
                if (checkfile.exists()) {
                    Log.e("check", "Anlagestandort exists (Excel)");
                    file_ok = true;
                } else {
                    file_ok = false;
                    Log.e("check", "Anlagenstandort nicht vorhanden");
                    printInfo("\n\n\"" + anlagestandortConfig
                            + "\" \n\n   nicht vorhanden \n\n");
                }
            }
        }

        // 28.11.16  Sprachdatei.xls eingebaut
        if (file_ok == true)
        {
            file_ok = false;
            String sprachdateiPathAndFileName = (TurmtechnikActivity.sdCardPath + "/Turmtechnik/Grafiken/Sprachdatei.xls");
            checkfile = new File(sprachdateiPathAndFileName);
            if (checkfile.exists())
            {
                Log.e("check", "Sprachdatei.xls vorhanden");
                file_ok = true;

                ExcelRead excelread = new ExcelRead();
                Log.e("load", "Sprachdatei=" + sprachdateiPathAndFileName);
                try
                {
                    excelread.openXls(sprachdateiPathAndFileName);

                    StaticVariable.uebersetzteTexte = new ArrayList<>();

                    int zeilenAnzahl = excelread.getCellZeilen();

                    // Schleife: i geht von 0 bis zeilenAnzahl-1, aber wir lesen Zeile (i+2)
                    // Daher muss (i+2) < zeilenAnzahl sein, also i < zeilenAnzahl-2
                    for (int i = 0; i < zeilenAnzahl - 2 && (i + 2) < zeilenAnzahl; i++)
                    {
                        String loadTempString = excelread.getCellString(1, (i + 2));
                        Log.e("loadTexte", "=" + loadTempString);
                        StaticVariable.uebersetzteTexte.add(loadTempString);
                    }

                } catch (BiffException e1)
                {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                    Log.e("Biff", "error");
                    new LogExcelError(0, 0, sprachdateiPathAndFileName, -1, sourceFileName, 5130);

                } catch (IOException e1)
                {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                    Log.e("IO", "error");
                } catch (Exception e)
                {
                    e.printStackTrace();
                }


            }
            else
            {
                file_ok = false;
                Log.e("check", "Sprachdatei nicht vorhanden");
                printInfo("\n\n\"" + sprachdateiPathAndFileName
                        + "\" \n\n   nicht vorhanden \n\n");
            }
        }

        if (file_ok == true) {
            int index = (checkMelodienNormalprogramm());
            // Log.e("index" , "checkMelodienNormalprogramm=" + index) ;
            if (index != -1) {
                // Log.i("melodie Normalprogramm" , "fehlfile zeile " + index);
                file_ok = false; // file nicht gefunden
                // setContentView(R.layout.text_layout);
                // outputView = (TextView) this.findViewById(R.id.textView1);
                printInfo("\n\n");
                printInfo("\""
                        + normalProgrammblockFileNamen.elementAt(index)
                        + "\" \n\n nicht vorhanden \n\n");
            }
            // sonst file_ok true lassen
        }


        if (file_ok == true) {
            // Prüfe, ob Festtage aus DB verwendet werden können (Excel-Check überspringen)
            boolean festtageInDB = false;
            try {
                android.content.Context context = TurmtechnikActivity.turmtechnikContext;
                if (context != null) {
                    PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
                    // Entweder feste Feiertage in DB ODER Tagtypen (z. B. Normalprogramm) → Programmauswahl aus DB, Excel nicht nötig
                    java.util.List<PlatinenDatabaseHelper.FesterFeiertag> feste = dbHelper.getAllFesteFeiertage();
                    if (feste != null && !feste.isEmpty()) {
                        festtageInDB = true;
                        Log.d("TurmtechnikActivity", "Feste Feiertage in DB (" + feste.size() + "), Excel-Prüfung übersprungen");
                    } else {
                        java.util.List<PlatinenDatabaseHelper.Tagtyp> tagtypen = dbHelper.getAllTagtypen();
                        if (tagtypen != null && !tagtypen.isEmpty()) {
                            for (PlatinenDatabaseHelper.Tagtyp tagtyp : tagtypen) {
                                if (tagtyp.programmTyp != null && tagtyp.programmTyp.equals("festtag_fest")) {
                                    java.util.List<Programm> programme = dbHelper.getProgrammeByTagtyp(tagtyp.name);
                                    if (programme != null && !programme.isEmpty()) {
                                        festtageInDB = true;
                                        Log.d("TurmtechnikActivity", "Feste Festtage in DB gefunden für " + tagtyp.name);
                                        break;
                                    }
                                }
                            }
                            if (!festtageInDB && tagtypen.size() > 0) {
                                // Tagtypen vorhanden (z. B. Normalprogramm), alle Feiertage/Vortage auf Normalprogramm → Excel-Check nicht nötig
                                festtageInDB = true;
                                Log.d("TurmtechnikActivity", "Tagtypen in DB, Feiertage nutzen Normalprogramm – Excel-Prüfung feste Feiertage übersprungen");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("TurmtechnikActivity", "Fehler beim Prüfen der DB für feste Festtage", e);
            }
            
            // Nur Excel-Dateien prüfen, wenn keine Festtage aus DB verwendet werden
            if (!festtageInDB) {
                int index = (checkFesteFeiertage());
                if (index != -1) {
                    // Log.i("feste Feiertage" , "fehlfile zeile " + index);
                    file_ok = false; // file nicht gefunden
                    // setContentView(R.layout.text_layout);
                    // outputView = (TextView) this.findViewById(R.id.textView1);
                    printInfo("\n\n");
                    printInfo("\""
                            + festeFesttageTagtypFileNamen.elementAt(index)
                            + "\" \n\n nicht vorhanden \n\n");
                }
            } else {
                // Festtage in DB gefunden, Excel-Prüfung überspringen
                Log.d("TurmtechnikActivity", "Feste Festtage in DB gefunden, Excel-Prüfung übersprungen");
            }
            // sonst file_ok true lassen
        }


        if (file_ok == true) {
            // Prüfe zuerst, ob Tagtypen in der Datenbank existieren
            // Wenn ja, überspringe Excel-Prüfung komplett (App verwendet DB)
            boolean skipExcelCheck = false;
            try {
                android.content.Context context = TurmtechnikActivity.turmtechnikContext;
                if (context != null) {
                    PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(context);
                    java.util.List<PlatinenDatabaseHelper.Tagtyp> tagtypen = dbHelper.getAllTagtypen();
                    if (tagtypen != null && !tagtypen.isEmpty()) {
                        // Wenn es überhaupt Tagtypen in der DB gibt, verwende DB statt Excel
                        skipExcelCheck = true;
                        Log.d("TurmtechnikActivity", "Tagtypen in DB gefunden (" + tagtypen.size() + "), Excel-Prüfung für variable Festtage übersprungen");
                    }
                }
            } catch (Exception e) {
                Log.e("TurmtechnikActivity", "Fehler beim Prüfen der DB für variable Festtage", e);
            }
            
            // Nur Excel-Dateien prüfen, wenn keine Tagtypen in DB sind
            if (!skipExcelCheck) {
                int index = (checkVariableFeiertage());
                //  index = -1 ;
                if (index != -1) {
                    // Log.i("fehlfile" , "zeile " + index);
                    file_ok = false; // file nicht gefunden
                    // setContentView(R.layout.text_layout);
                    // outputView = (TextView) this.findViewById(R.id.textView1);
                    printInfo("\n\n");
                    printInfo("\""
                            + variableFesttageTagtypFileNamen.elementAt(index)
                            + "\" \n\n nicht vorhanden \n\n");
                }
                // sonst file_ok true lassen
            } else {
                // Tagtypen in DB gefunden, Excel-Prüfung überspringen
                Log.d("TurmtechnikActivity", "Tagtypen in DB gefunden, Excel-Prüfung für variable Festtage übersprungen");
            }
        }

        //if (file_ok == true) {
        //	String answer = checkFeiertagNormal();
        //	if ( ! answer.equals("")) {
        //		file_ok = false;
        //		printInfo("\n\n\"" + answer + "\" " + "nicht vorhanden 11 \n\n" );
        //	}
        //	else
        //	{
        //		printCountInfo();
        //	}
        //}

        //if (file_ok == true) {
        //	String answer = checkVorfeiertagNormal();
        //	if (!answer.equals("")) {
        //		file_ok = false;
        //		printInfo("\n\n\"" + answer + "\" " + "nicht vorhanden 12 \n\n");
        //	}
        //	else
        //	{
        //		printCountInfo();
        //	}
        //}

        // 9.6.13 pfingsten wird schon lange nicht verwendet
			/*
			 * if (file_ok == true) { String answer = checkPfingsten() ; if ( !
			 * answer.equals("") ) { file_ok = false ; printInfo("\n\n\"" +
			 * answer ); } }
			 */

        // hier checken ob bluetooth oder carambola moeglich

        Log.e("alle", "files geprueft");

        if (file_ok) {
            printInfo("\nalle Files vorhanden");
        }

        if (file_ok) {
            // WICHTIG: Vorschwingen-Daten werden jetzt aus der Datenbank geladen, nicht mehr aus Excel!
            // Excel wird nur noch als Fallback verwendet, wenn Datenbank leer ist
            boolean vorschwingenGeladen = loadVorschwingenFromDatabase();
            
            if (!vorschwingenGeladen) {
                Log.w("loadBluetoothConfig", "Vorschwingen-Daten nicht in Datenbank. Nutzen Sie 'Import alter Anlagen' (Web-UI) für Excel-Import.");
            }

            StaticVariable.schlagwerkTriggerSyncTime2 = true;
        }

        StaticVariable.bt_io_ok = false;
        StaticVariable.carambola_io_ok = false;

        if (file_ok) {
            PlatinenDatabaseHelper dbHelper = PlatinenDatabaseHelper.getInstance(getApplicationContext());
            boolean bluetoothConfigOpened = true;
            try {
                String dbModus = dbHelper.getModus();
                StaticVariable.bluetoothMode = "bluetooth".equals(dbModus != null ? dbModus : "wifi");
                if (IOConfig.bluetoothMode != StaticVariable.bluetoothMode) {
                    IOConfig.bluetoothMode = StaticVariable.bluetoothMode;
                }
                android.util.Log.d("TurmtechnikActivity", "Modus aus DB: " + dbModus + " -> bluetoothMode=" + StaticVariable.bluetoothMode);
            } catch (Exception e1) {
                android.util.Log.w("loadBluetoothConfig", "Modus aus DB nicht geladen, Standard WLAN", e1);
                StaticVariable.bluetoothMode = false;
            }
            loadPlatinenIpListFromDb();
            printInfo("\nmomentanes exit passort = abc");
            try {
                String pwd = dbHelper.getConfigValue("password_exit_normal");
                passwordExitNormal = (pwd != null) ? pwd : "";
                String pwdDel = dbHelper.getConfigValue("password_exit_and_delete");
                passwordExitAndDeleteBeschriftungTasten = (pwdDel != null) ? pwdDel : "";
            } catch (Exception e2) {
                passwordExitNormal = "";
                passwordExitAndDeleteBeschriftungTasten = "";
            }

            if (bluetoothConfigOpened) {
                // ipList/portList bereits aus loadPlatinenIpListFromDb() – kein System.xls mehr
            }

            // ScanRelais nur aus DB
            try {
                int dbScanRelais = dbHelper.getScanRelaisMS();
                StaticVariable.scanRelaisMS = (dbScanRelais >= 100 && dbScanRelais <= 10000) ? dbScanRelais : 1000;
            } catch (Exception e) { StaticVariable.scanRelaisMS = 1000; }

            //12.1.2015 Power off Akku % aus DB
            try {
                StaticVariable.batt_shut_down_level2 = dbHelper.getPowerOffAkkuProzent();
            } catch (Exception e) {
                StaticVariable.batt_shut_down_level2 = 75;
            }

            loadAnlageLogAndRebootFromDb(this);

            try {
                String internetOn = dbHelper.getConfigValue("internet_on_zeit");
                String internetOff = dbHelper.getConfigValue("internet_off_zeit");
                if (internetOn != null && internetOn.contains(":")) {
                    String[] z = internetOn.split(":");
                    StaticVariable.internetOnStunden = parseStundeOderMinuteOrZero(z.length > 0 ? z[0] : null);
                    StaticVariable.internetOnMinuten = parseStundeOderMinuteOrZero(z.length > 1 ? z[1] : null);
                } else { StaticVariable.internetOnStunden = 0; StaticVariable.internetOnMinuten = 0; }
                if (internetOff != null && internetOff.contains(":")) {
                    String[] z2 = internetOff.split(":");
                    StaticVariable.internetOffStunden = parseStundeOderMinuteOrZero(z2.length > 0 ? z2[0] : null);
                    StaticVariable.internetOffMinuten = parseStundeOderMinuteOrZero(z2.length > 1 ? z2[1] : null);
                } else { StaticVariable.internetOffStunden = 0; StaticVariable.internetOffMinuten = 0; }
            } catch (Exception e) {
                StaticVariable.internetOnStunden = 0; StaticVariable.internetOnMinuten = 0;
                StaticVariable.internetOffStunden = 0; StaticVariable.internetOffMinuten = 0;
            }
            Boolean autostart = dbHelper.getAutostartDerApp();
            StaticVariable.autostartDerApp = (autostart != null) ? autostart : true;

            Log.e("autostart", "StaticVariable.autostartDerApp=" + StaticVariable.autostartDerApp);
            Log.e("serial", "Number=" + StaticVariable.serialNumber);


            if (!StaticVariable.bluetoothMode) {
                myWifiManager = (WifiManager) getBaseContext().getApplicationContext().getSystemService(
                        Context.WIFI_SERVICE);
                myWifiManager.setWifiEnabled(true);
            }


            // Ab Android 12 (API 31) BLUETOOTH_CONNECT zur Laufzeit nötig; vor getDefaultAdapter() prüfen
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (TurmtechnikActivity.context != null && ActivityCompat.checkSelfPermission(TurmtechnikActivity.context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.w("TurmtechnikActivity", "BLUETOOTH_CONNECT nicht erteilt – Bluetooth-Adapter wird nicht initialisiert.");
                    StaticVariable.myBluetoothAdapter = null;
                } else {
                    StaticVariable.myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                }
            } else {
                StaticVariable.myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }
            // im thread = absturz

            // bt_io_ok/carambola_io_ok an Modus anpassen (Modus kommt aus DB, siehe loadBluetoothConfig)
            if (StaticVariable.bluetoothMode == true) {
                StaticVariable.bt_io_ok = true; // simmuliert bt = O.K. 18.09.2013
            } else {
                StaticVariable.bt_io_ok = false;
                StaticVariable.carambola_io_ok = true;

                // ausgebaut 5.6.2013 == bei wifi modus wird beim start das wifi
                // nicht kontrolliert

					/*
					 * int checkTemp = checkCarambola(); Log.e("checkTemp" , "=" +
					 * checkTemp) ; if(checkTemp > 0) // bei null alles O.K. // bei
					 * > 0 gibt ersten Fehlversuch bei der Verbindung zurueck
					 * 
					 * {
					 * 
					 * printInfo("\n\n");
					 * printInfo("Carambola echo error:\nPlatine Nummer " +
					 * StaticVariable.carambolaStatus + " IP = " +
					 * StaticVariable.ipList.get(StaticVariable.carambolaStatus-1))
					 * ; StaticVariable.carambola_io_ok = false ; }
					 */
            }
        }

			/*
			 * avrNetIo ausgebaut: 10.2.2013 Log.i("vor avr", " check");
			 * 
			 * AvrNetIo avr1=new AvrNetIo("192.168.1.51",50290); if
			 * (!avr1.connect()) { //file_ok=false ; net_io_ok = false ;
			 * Toast.makeText(this, "AVR-Net-IO 1: keine Verbindung",
			 * Toast.LENGTH_LONG).show(); avr1.disconnect(); } else {
			 * Toast.makeText(this, "AVR-Net-IO 1: Verbindung O.K.",
			 * Toast.LENGTH_LONG).show(); avr1.disconnect(); net_io_ok = true ; }
			 * 
			 * avr1 = null ; System.gc();
			 * 
			 * Log.i("nach avr","check");
			 */
        Log.e("F I L E", "O.K. 1 = " + file_ok);

//			if (file_ok && (StaticVariable.bt_io_ok == true || StaticVariable.carambola_io_ok == true))
        if (file_ok) {
            PlatinenDatabaseHelper dbForLayout = PlatinenDatabaseHelper.getInstance(TurmtechnikActivity.this);
            // Neuinstallation: Config ist nicht gesetzt → neues Web-UI-Layout (Seite 1/2). Nur bei explizit "0" altes Android-Layout.
            // Bei woke_by_motion (Bewegungserkennung) immer natives Layout bauen, damit kein schwarzer Bildschirm (Web-UI) erscheint.
            boolean wokeByMotion = getIntent() != null && getIntent().getBooleanExtra("woke_by_motion", false);
            String webUiConfig = dbForLayout != null ? dbForLayout.getConfigValue(CONFIG_WEB_UI_VOLLBILD_TEST) : null;
            boolean webUiVollbildTest = webUiConfig == null || !"0".equals(webUiConfig.trim());
            if (!webUiVollbildTest || wokeByMotion) {
                printInfo("\nlayout wir aufgebaut...");
                layout = new Seite1Layout(beschriftungTastenFileString, getApplicationContext());
                layout.setOnAnlageImportRequestedListener(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showAnlageImportWithPasswordDialog();
                            }
                        });
                    }
                });
                layoutCreated = (layout.initLayout());
            } else {
                layout = null;
                layoutCreated = null;
            }

            //Log.e("layoutReady" , "=" + StaticVariable.layoutReady) ;
            //while(StaticVariable.layoutReady == false)
            //{
            //	Log.e("warte" , "auf seite1 Layout") ;
            //}

            //outputView.setText("");
            //printInfo("jetzt wuerde parent addView kommen");
            // parentLayout.addView(layoutTemp);

            //setContentView(layoutTemp) ; // 2.12.2014 neu, erst wenn fertig laden

            boolean nu_vorhanden_temp = nebenuhrVorhanden();
            // Log.e("Nebenuhr" , "=" + nu_vorhanden_temp ) ;

            // Log.e("SAVE" , "nebenuhr") ;
            // saveNebenuhr();
            // Log.e("LOAD" , "nebenuhr") ;
            // loadNebenuhr() ;


            if (nu_vorhanden_temp == false) {
                // Keine gespeicherten Werte (z. B. Neuinstallation): nur Nebenuhr-Fenster öffnen, auf Eingabe warten (nur einmal nötig)
                initNebenUhr();
                startSetNebenuhrForFirstTime();
            } else {
                boolean nebenuhrOK = loadNebenuhr();
                if (nebenuhrOK == false) {
                    initNebenUhr();
                    loadNebenuhrLastRelaisAndAnzeigeFromDb();
                    startSetNebenuhr();
                }
            }

            Log.e("nach", "load Nebenuhr");

            // das wird alles im Google Drive thread gemacht:
				/*
				 * boolean google_url_vorhanden = fileGoogleSpreadsheetVorhanden() ;
				 * if(google_url_vorhanden == false) {
				 * makeGoogleDriveNewSpreadsheet(); // erzeugt auch das url file }
				 * else { loadGoogleUrl(); }
				 */

            // outputView = null ;
            // checkfile = null ;

            System.gc();

            Log.e("load", "Nebenuhr");

            // loadNebenuhr() wurde bereits oben (nu_vorhanden_temp) aufgerufen – kein zweiter Aufruf
            checkSavedValuesAndLog();

            //11.11.14 startet zu bald!
            //uhr_thread = new UhrThread();
            //uhr_thread.start();

            // wird auch im googleDriveThread gestartet
				/*
				 * google_Drive_Update_Thread = new
				 * GoogleDriveUpdateThread(getBaseContext());
				 * //google_Drive_Update_Thread = new GoogleDriveUpdateThread();
				 * google_Drive_Update_Thread.start();
				 */

            //if (StaticVariable.bt_io_ok || StaticVariable.carambola_io_ok)
            //{
            // Serial_IoThread nur neu starten, wenn noch keiner läuft (z. B. echter App-Start). Nach Bildschirmschoner-Rückkehr Thread wiederverwenden, damit Melodie/Relais nicht aus gehen.
            if (serial_iothread == null || !serial_iothread.isAlive()) {
                StaticVariable.serial_io_ThreadsRun = false;
                loadPlatinenDemoModusFromDb();
                loadPlatinenIpListFromDb();
                int ipCount = (StaticVariable.ipList != null && StaticVariable.portList != null)
                        ? Math.min(StaticVariable.ipList.size(), StaticVariable.portList.size()) : 0;
                Log.w("Serial_IoThread", "Start Relais-Thread: bt_io_ok=" + StaticVariable.bt_io_ok + ", Platinen (IP/Port)=" + ipCount + ", DemoModus=" + StaticVariable.platinenDemoModus);
                serial_iothread = new Serial_IoThread();
                serial_iothread.start();
            }
            //}



            // outputView = null ; // hilft nichts wegen speicherverbrauch
            System.gc();

            // layout = new Seite1Layout(beschriftungTastenFileString, getApplicationContext());


            //setContentView(layout.initLayout());  // loest ANR Dialog aus???
            // in den Thread verschoben 28.8.14

            // 3.12.2014 wird weiter vorne geladen

            if (layout != null) {
                layoutButtonsSize = layout.buttons.size();
            } else {
                layoutButtonsSize = BESCHRIFTUNG_TASTEN_MAX_SLOTS;
            }

            StaticVariable.firstStartMelodie = false; // wegen Vector in
            // SonderMelodien
            initSonderprogrammeVectoren();
            loadBenutzerprogrammeFromDb();
            // Benutzerprogramme sind aus DB geladen – UhrThread/Heizung-Thread müssen nicht warten (kein 60‑s-Timeout mehr).
            StaticVariable.helpForStartBenutzermelodien = false;

            initSound();

            // Benutzerprogramm-Seite nicht mehr automatisch beim App-Start öffnen;
            // Nutzer bleibt auf Layoutseite 1. Öffnung weiterhin über Taste „Programm eingeben“.

            // Bei Erstinstallation (keine Nebenuhr-Werte): UhrThread erst in onActivityResult nach SetNebenuhr-Eingabe starten
            if (!waitingForNebenuhrFirstTime) {
                uhr_thread = new UhrThread();
                uhr_thread.start();
            }


            //ausgangHeizungThreadNew2 = new AusgangHeizungThreadNew2() ;
            //ausgangHeizungThreadNew2.start();

            Log.e("nach", "startUhrThread");

            InternetOnOffThread internetOnOffThread = new InternetOnOffThread() ;
            internetOnOffThread.start();

            //testReadJsonAndi();


            //initBenutzerMelodieTasteOn();
				
				/*
				 * // wir durch einen ui thread ersetzt SpreadSheetFactory factory =
				 * SpreadSheetFactory.getInstance(new
				 * AndroidAuthenticator(TurmtechnikActivity.this));
				 * factory.createSpreadSheet("test spreadSheet") ; Activity
				 * turmtechnik = TurmtechnikActivity.this ; googleDriveThread = new
				 * GoogleDriveThread(StaticConstants.GOOGLE_DRIVE_NEW_FILE,
				 * getBaseContext(), turmtechnik) ; googleDriveThread.start();
				 */


            //if(GOOGLE_EXCEL_ENABLE == true)
            //{
//				runOnUiThread(new Runnable() {
            //
//					@Override
//					public void run() {
//						// TODO Auto-generated method stub
//						// new MyTask().execute(null);
//						new MyUiTask().execute("");
//					}
//				});
            //}


            // AccountManager am = AccountManager.get(getBaseContext());
            // am.getAuthToken(am.getAccounts()[0],
            // "ouath2:" + DriveScopes.DRIVE,
            // new Bundle(),
            // true,
            // new OnTokenAcquired(),
            // null);
            //

            // Bildschirm wird nie dunkel!
            // auf Wunsch vom Thomas am 5.6.13 deaktiviert
            // app stellt sich nach einigen tagen ab.
            // am 4.11.13 wieder eingebaut

            // 14.11.13 wird nach beleuchte Tasten durchgefuehrt
				/*
				  PowerManager pm = (PowerManager)
				  getSystemService(Context.POWER_SERVICE); wl =
				  pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "tom.turmtechnik:Turmtechnik");
				  wl.acquire(); // ab jetzt bleibt der Bildschirm an
				*/

            //layout.printDatum();

            initRelais();
            automatikTasteIndex = sucheAutomaticTaste();
            schlagwerkTasteIndex = sucheSchlagwerkTaste();

            // Automatik ist nur Software-Zustand, kein Relais.
            if (flagAutomaticOnOff) {
                UhrThread.newSearchAutomaticStart = true;
            }

            // layout.closeWorkbook(); // braucht man fuer Tastenwechsel
            // (farbe..)

            // 12.3.13 wird durch "HOME" und "SEITE" ersetzt
				/*
				 * layout.pfeilRechtsButton.setOnClickListener(new OnClickListener()
				 * {
				 * 
				 * @Override public void onClick(View v) { // TODO Auto-generated
				 * method stub startSeite2(); } });
				 */

				/*
				 * 4.4.13 Stop und Automatik Taste sind ins normale Tastenlayout
				 * integriert
				 * 
				 * layout.stopButton.setOnClickListener(new OnClickListener() {
				 * 
				 * @Override public void onClick(View v) { BlockThread.doRunOff() ;
				 * UhrThread.blockReady = 0 ; // block fertig , wieder ready
				 * stringInfoText = "suche nächsten Start..." ;
				 * UhrThread.newSearchAutomaticStart = true ; allRelaisOff(); } });
				 * 
				 * layout.automaticButton.setOnClickListener(new OnClickListener() {
				 * 
				 * @Override public void onClick(View v) { { changeAutomatic(); } }
				 * });
				 */


            // 28.2.14 jetzt Fernwartungsthread starten
            // 30.6.14 Fernsteuerung umgebaut auf DDP und Meteor
            // also runOnUiThread auskommentiert
            //runOnUiThread(new Runnable() {
            //@Override
            //public void run() {
            //new  MyUiTask().execute("");
            //}
            //});

            // 11.6.13 die gesamte logik wegen der Tasten

            printInfo("\nbuttons werden initialisiert");
            int buttonsSize = (layout != null && layout.buttons != null) ? layout.buttons.size() : 0;
            Log.e("seite1", "layout.buttons.size()=" + buttonsSize);

            for (int ix = 0; ix < buttonsSize; ix++) {
                final int buttonIndex = ix;
                // Tag (Slot-Index 0..23) wird in initGlockenButton gesetzt – nicht überschreiben

                layout.buttons.elementAt(ix).setOnClickListener(
                        new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                int localIndex = (v.getTag() instanceof Integer) ? (Integer) v.getTag() : buttonIndex;
                                if (localIndex < 0 || localIndex >= relaisNumber.length) return;

                                // nur fuer kurzen Test 8.2.14
                                //toggleJonsonTest++ ;
                                //if(  (toggleJonsonTest & 01) == 0 )
                                //{
                                //	testReadJson();
                                //}
                                //else
                                //{
                                //testWriteJson();
                                //testSendMuster() ;
                                //}


                                triggerBigClockTimer();
                                setRestartTimer(60 * 15); // 15 Minuten restart Turmtechnik ueber Service

                                if (relaisNumber[localIndex] < StaticConstants.LIMIT_1000_100) // keine
                                // Taste
                                // stop (
                                // relais#
                                // 100 ) ?
                                {
                                    Log.e("Manual Relais", "=" + relaisNumber[localIndex]
                                            + " StaticVariable.heizungRelaisNumberManual=" + StaticVariable.heizungRelaisNumberManual2);
                                    if (globalOn[localIndex] == true) {
                                        globalOn[localIndex] = false;
                                        stopGlockenSound(localIndex);
                                        layout.buttonOff(localIndex);
                                        // Log.i("locIndex"+localIndex,"rel "+relaisNumber[localIndex]);
                                        LogTurmtechnik2 logTurmtechnik =
                                                new LogTurmtechnik2("Manual Relais " + relaisNumber[localIndex], 0, 0, 0, 0);
                                        logTurmtechnik = null;
                                        if (serial_iothread != null)
                                            serial_iothread.changeRelais(
                                                    relaisNumber[localIndex],
                                                    false);

                                        //sendTastenStatusJson(fernwartungID[localIndex], false);
                                        // Meteor Code entfernt
                                        if (relaisNumber[localIndex] == StaticVariable.heizungRelaisNumberManual2) {
                                            if (automatikHeizungAktiv()) {
                                                new SaveAndLoadHeizung().clearHeizung();
                                            }
                                            if (heizungManualThread != null) {
                                                heizungManualThread.endHeizungManualThread();
                                                Log.e("Heizung", "timer auf 0 gestellt");
                                                StaticVariable.heizungOnTimer = 0;
                                            }
                                        }

                                    } else {
                                        // Log.i("Hammer" , "zeit=" +
                                        // hammerZeit[localIndex]) ;
                                        if (hammerZeit[localIndex] > 0) // ist
                                        // es
                                        // ein
                                        // Hammer
                                        // ?
                                        {
                                            checkHammer(localIndex);
                                        }
                                        else
                                        {
                                            if (relaisNumber[localIndex] == StaticVariable.heizungRelaisNumberManual2)
                                            {
                                                if (StaticVariable.heizungOnTimer > 0) {
                                                    // thread laeft noch
                                                    Log.e("Heizung", "endHeizungManualThread");
                                                    heizungManualThread.endHeizungManualThread();

                                                }

                                                // jetzt einen neuen starten
                                                heizungManualThread = new HeizungManualThread();
                                                heizungManualThread.start();
                                                Log.e("Heizung", "heizungThrad neu gestartet");

                                                //Calendar calendar = Calendar.getInstance();

                                                //long momentanMsDurchTausend = (calendar.getTimeInMillis()) / 1000L;

                                                //long momentanPlusAchtStunden = momentanMsDurchTausend + (60 * 60 * 8); // acht Stunden

                                                //new SaveAndLoadHeizung().saveHeizung(momentanMsDurchTausend, momentanPlusAchtStunden);
                                            }


                                            LogTurmtechnik2 logTemp = new LogTurmtechnik2("manul", 0, 0, 0, 0);
                                            logTemp = null;

                                            globalOn[localIndex] = true;
                                            startGlockenSound(localIndex, StaticConstants.PLAY_SOUND_IMMMER);
                                            layout.buttonOnOK(localIndex);
                                            if (serial_iothread != null)
                                                serial_iothread.changeRelais(
                                                        relaisNumber[localIndex],
                                                        true);
                                            //sendTastenStatusJson(fernwartungID[localIndex], true);
                                            // Meteor Code entfernt
                                        }
                                    }
                                } else // relais nummer > 100
                                {
                                    if (TurmtechnikActivity.relaisNumber[localIndex] > 3000) {
                                        int offset = (TurmtechnikActivity.relaisNumber[localIndex] - 3001);
                                        StaticVariable.sofortStartPopupRelaisNummerAktiv = (TurmtechnikActivity.relaisNumber[localIndex]);
                                        StartMelodieSofort startMelodieSofort = new StartMelodieSofort(TurmtechnikActivity.this);
                                        startMelodieSofort.setStartMelodieSofort(offset);

                                    }


                                    if ((relaisNumber[localIndex] > 2000) && (relaisNumber[localIndex] < 3000))
                                    // ist
                                    // es
                                    // eine
                                    // normale verknuepfte
                                    // Taste?

                                    {
                                        int offset = (relaisNumber[localIndex] - 2001);
                                        offset = Math.max(offset, 0);
                                        offset = Math.min(RELAIS_COUNT, offset);
                                        Log.e("offset verkn.", "=" + offset);
                                        if (offset < verknuepfteTastenOn.length && Boolean.TRUE.equals(verknuepfteTastenOn[offset])) 							{
								synchronized (VERKNUEPFTE_TASTEN_LOCK) { verknuepfteTastenOn[offset] = false; }
								layout.buttonOff(localIndex);
                                            //sendTastenStatusJson(fernwartungID[localIndex], false) ;
                                            // Meteor Code entfernt
                                        } else 							{
								synchronized (VERKNUEPFTE_TASTEN_LOCK) { verknuepfteTastenOn[offset] = true; }
								layout.buttonOnOK(localIndex);
                                            //sendTastenStatusJson(fernwartungID[localIndex], true) ;
                                            // Meteor Code entfernt
                                        }
                                        LogTurmtechnik2 logTemp =
                                                new LogTurmtechnik2("verknuepfte Taste manual", 0, 0, 0, 0);
                                        logTemp = null;
                                        StaticVariable.changeInternetVerknuepfteTasten++;
                                        StaticVariable.refreshInfoTextVerknuepfteTaste = true; // UhrThread führt sofort Programmabfrage aus
                                        StaticVariable.infoTextRefreshCount = 5; // UI zeigt „nächstes Programm“ sofort an (5× ~50 ms)
                                    }
                                    if ((relaisNumber[localIndex] > StaticConstants.LIMIT_1000_200)
                                            && (relaisNumber[localIndex] < StaticConstants.LIMIT_1000_300)) // ist
                                    // es eine "SEITE"-Taste?
                                    {
                                        // Zweite Seite: ohne Passwortabfrage öffnen (Excel-Passwort oft nicht mehr genutzt)
                                        if (relaisNumber[localIndex] == StaticConstants.ZWEITE_SEITE) {
                                            startSeite(relaisNumber[localIndex]);
                                        } else {
                                            String password = (readPassword(relaisNumber[localIndex]));
                                            if (!((password.equals("null")) || password.equals("NULL")) && password != null && !password.trim().isEmpty()) {
                                                checkPasswordAndStartIntent(
                                                        relaisNumber[localIndex],
                                                        password);
                                            } else {
                                                startSeite(relaisNumber[localIndex]);
                                            }
                                        }
                                    }

                                    if (relaisNumber[localIndex] == StaticConstants.STOP) // Taste
                                    // stop
                                    // ?
                                    {
                                        stopAllGlockenSounds();
                                        MelodieThreadNew.doRunOff();
                                        StaticVariable.stopBetaetigt = true;
                                        StaticVariable.pathStoppedByUser = (StaticVariable.pathAndFileNameNextMelodie != null ? StaticVariable.pathAndFileNameNextMelodie : "");
                                        // neu 7.9.16 bei stop Taste nmea uhr sync
                                        StaticVariable.schlagwerkTriggerSyncTime2 = true;
                                        Log.e("nmea", "STOP");

                                        TagesSuche tagesSuche = new TagesSuche();

                                        allMelodieRelaisOffAusfuehren();

                                        UhrThread.blockReady = 0; // block
                                        // fertig ,
                                        // wieder
                                        // ready

                                        UhrThread.newSearchAutomaticStart = true;
                                        StaticVariable.sofortStartPopupFlag = false;
                                        StaticVariable.sofortStartPopupGefunden = false;
                                        StaticVariable.stringInfoTextField[0] = "Stop betätigt";
                                        // allRelaisOff();
                                        StaticVariable.changeInternetBenutzerprogramme++;
                                        StaticVariable.changeInternetVerknuepfteTasten++;
                                        LogTurmtechnik2 logTemp =
                                                new LogTurmtechnik2("Stop gedrueckt", 0, 0, 0, 0);
                                        logTemp = null;
                                        System.gc();
                                        //sendTastenStatusSeite1();
                                        //setRestartTimer(5);  // am 18.1.14 wieder ausgebaut
                                        //restartTurmtechnik();

                                    }

                                    if (relaisNumber[localIndex] == StaticConstants.AUTOMATIC || relaisNumber[localIndex] == 101) // Taste Automatik ein/aus (101 = alte Konfiguration)
                                    {
                                        stopAllGlockenSounds();
                                        changeAutomatic();
                                        StaticVariable.changeInternetVerknuepfteTasten++;
                                        //sendTastenStatusJson(fernwartungID[localIndex], automaticOn) ;
                                        // Meteor Code entfernt
                                    }

                                    if (relaisNumber[localIndex] == StaticConstants.HELP)  //103) // Help
                                    // Taste?
                                    {

                                        startHelpSeite();
                                    }

                                    if (relaisNumber[localIndex] == StaticConstants.SCHLAGWERK_ON_OFF)  //104) // Schlagwerk
                                    {
                                        startGlockenSound(localIndex, StaticConstants.PLAY_SOUND_EINMALIG);
                                        changeSchlagwerkFlag();
                                        // Meteor Code entfernt
                                    }
                                }
                            }
                        });
            }

            // thread fuer update infotext erzeugen
            //handler = new Handler();
            //startUpdateInfoText();

            //sendTastenStatusSeite1();

        } // ende von if file_ok
        Log.e("F I L E", "O.K. 2 = " + file_ok);

        // Immer weitermachen (Nutzer hat bereits „Weiter“ gewählt) – sonst bleibt Bildschirm nach Init stehen
        file_ok = true;

        // Sound-Init nicht hier blockierend ausführen, sondern auf UI-Thread mit Verzögerung
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            initSoundPool();
                            StaticVariable.soundIndexList = new ArrayList<>();
                            StaticVariable.soundIDsList = new ArrayList<>();
                            StaticVariable.streamIDsList = new ArrayList<>();
                            StaticVariable.soundGlocke = new SoundGlocke();
                        } catch (Exception e) {
                            Log.e("TurmtechnikActivity", "Sound-Init nach Ende Init", e);
                        }
                    }
                }, 300);
            }
        });

    } // ende von checkFilesAndInit

    public void initSoundPool() {


        //sound = new SoundPool(10, AudioManager.STREAM_MUSIC, 0 );


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            createNewSoundPool();
        } else {
            createOldSoundPool();
        }


        //Log.i("SOUND INIT" , "=" + filename1 ) ;
        //Log.i("SOUND INIT" , "=" + filename2) ;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected void createNewSoundPool() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)

                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        StaticVariable.soundPool2 = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .setMaxStreams(25)
                .build();
    }

    @SuppressWarnings("deprecation")
    protected void createOldSoundPool() {
        StaticVariable.soundPool2 = new SoundPool(25, AudioManager.STREAM_MUSIC, 0);
    }

    public static void startGlockenSound(int index, int wiederholung)
    {
        String soundName = null;
        if (turmtechnikContext != null) {
            try {
                java.util.List<PlatinenDatabaseHelper.BeschriftungTastenRow> rows = PlatinenDatabaseHelper.getInstance(turmtechnikContext).getBeschriftungTasten();
                if (rows != null && index >= 0 && index < rows.size()) {
                    PlatinenDatabaseHelper.BeschriftungTastenRow r = rows.get(index);
                    if (r.sound != null && !r.sound.trim().isEmpty()) {
                        soundName = r.sound.trim();
                    }
                }
            } catch (Exception e) {
                Log.e("startGlockenSound", "DB-Lesen Sound", e);
            }
        }
        if (soundName == null) {
            ExcelRead excelread = new ExcelRead();
            try {
                excelread.openXls(beschriftungTastenFileString);
                soundName = excelread.getCellString(11, (index + 1));
            } catch (BiffException e1) {
                e1.printStackTrace();
                new LogExcelError(0, 0, beschriftungTastenFileString, -1, sourceFileName, 1627);
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try { excelread.closeWorkbook(); } catch (Exception ignored) { }
            }
        }
        Log.e("soundGlocke", "name=" + soundName);
        if (StaticVariable.soundGlocke != null) {
            StaticVariable.soundGlocke.playGlockeSound(soundName, index, wiederholung);
        }

        //String pathAndFilename = TurmtechnikActivity.sdCardPath + "/Turmtechnik/Sound/" + soundName ;
        //File file = new File(pathAndFilename) ;
        //MediaPlayer momentanSound = MediaPlayer.create(TurmtechnikActivity.this,Uri.fromFile(file)) ;
        //momentanSound.start();


    }

    private void stopGlockenSound(int index) {
        if (StaticVariable.soundGlocke != null) {
            StaticVariable.soundGlocke.stopGlockenSound(index);
        }
    }

    private void stopAllGlockenSounds() {
        if (StaticVariable.streamIDsList == null || StaticVariable.soundPool2 == null) return;
        for (int i = 0; i < StaticVariable.streamIDsList.size(); i++) {
            Log.e("sound", "streamIdList= " + StaticVariable.streamIDsList.get(i));
            StaticVariable.soundPool2.stop(StaticVariable.streamIDsList.get(i));
        }
        if (StaticVariable.stopGlockenSoundPending != null) {
            StaticVariable.stopGlockenSoundPending.clear();
        }
    }

    private static boolean automatikHeizungAktiv() {

        boolean retBoolean = false;

        Calendar calendar = Calendar.getInstance();

        long momentanMsDurchTausend = (calendar.getTimeInMillis()) / 1000L;

        SaveAndLoadHeizung saveAndLoadHeizung = new SaveAndLoadHeizung();

        long[] onOffHeizung = saveAndLoadHeizung.loadHeizungZeiten();

        if (momentanMsDurchTausend < onOffHeizung[1]) {
            if (momentanMsDurchTausend > onOffHeizung[0]) {
                retBoolean = true;
            }
        }

        return retBoolean;
    }

    private static void checkHammer(int localIndex) {
        if (hammerZeit[localIndex] > 0) // ist
        // es
        // ein
        // Hammer
        // ?
        {
            if (StaticVariable.hammerThreadLaeuft == false) {
                StaticVariable.hammerDelayTime2 = (hammerZeit[localIndex]);
                StaticVariable.hammerSound = 1;
                for (int i = localIndex + 1; i < RELAIS_COUNT; i++) {
                    if (hammerZeit[i] > 0) {
                        StaticVariable.hammerSound = 0;
                        break;
                    }
                }
                LogTurmtechnik2 logTurmtechnik = new LogTurmtechnik2("Hammer manual", 0, 0, 0, 0);
                logTurmtechnik = null;

                int relNr = (relaisNumber[localIndex] != null) ? relaisNumber[localIndex] : 0;
                long delay = (hammerZeit[localIndex] != null) ? hammerZeit[localIndex] : 50;
                HammerManualThread hammermanualthread = new HammerManualThread(localIndex, relNr, delay);
                hammermanualthread.start();
            }
        }
    }

    public class SendTastenStatusSeite1Thread extends Thread
    {
        public void run()
        {
            Log.e("StatusSeite1Thread" , "vor 5000") ;
            sleepTime(5000);
            sendTastenStatusSeite1();
            Log.e("StatusSeite1Thread" , "nach 5000") ;
        }

        private void sleepTime(long time) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }


}  // ende von allem
