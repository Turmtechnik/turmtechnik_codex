package tom.turmtechnik;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.graphics.Color;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Zeigt die Web-UI (z. B. Benutzerprogramme) im Vollbild in einer WebView.
 * Eine „Zurück“-Taste beendet die Activity und kehrt zur aufrufenden App zurück.
 */
public class WebUiActivity extends Activity {
    private static volatile WebUiActivity activeInstance;

    public static final String EXTRA_PATH = "path";
    /** Wenn true: URL = http://127.0.0.1:8080/&lt;path&gt; (immer funktionsfähig beim Bildschirmschoner vom gleichen Gerät). */
    public static final String EXTRA_USE_LOCALHOST = "use_localhost";
    private static final int RETRY_DELAY_MS = 2000;
    private static final int MAX_RETRIES = 5;
    private static final String SCREENSAVER_URL = "http://127.0.0.1:8080/screensaver.html";
    /** RustDesk Android-Paket (F-Droid/official: flutter_hbb). */
    private static final String RUSTDESK_PACKAGE = "com.carriez.flutter_hbb";

    private WebView webView;
    private String loadUrl;
    private int retryCount;
    /** URL der Seite vor dem Schoner (app-seite1 oder app-seite2), um nach Tipp zurückzukehren. */
    private String pageBeforeScreensaver;
    /** True, wenn wir per Timer auf screensaver.html gewechselt haben (Tipp = zurück zur vorherigen Seite). */
    private boolean showingScreensaver;
    /** Bildschirmschoner: Nach konfigurierter Zeit Schoner anzeigen bzw. Bildschirm abdunkeln. */
    private Handler screenOffHandler = new Handler(Looper.getMainLooper());
    private Runnable screenDimRunnable;
    /** Overlay beim Abdunkeln; beim Wiedereinschalten entfernen und Helligkeit wieder hell. */
    private View screenDimOverlay;
    private Handler screensaverHandler = new Handler(Looper.getMainLooper());
    private Runnable screensaverRunnable;
    /** Heartbeat an StartTurmtechnikService, damit beim Bildschirmschoner die App nicht nach 90 s neu gestartet wird (Relais bleiben an, TurmtechnikActivity wird nicht zerstört). */
    private static final long HEARTBEAT_INTERVAL_MS = 60_000L;
    private Handler heartbeatHandler = new Handler(Looper.getMainLooper());
    private Runnable heartbeatRunnable;

    private void cancelScreenDimState() {
        if (screenDimRunnable != null) {
            screenOffHandler.removeCallbacks(screenDimRunnable);
        }
        try {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.screenBrightness = 0.85f;
            getWindow().setAttributes(lp);
        } catch (Exception e) {
            Log.w("WebUiActivity", "ScreenDim-Reset: " + (e != null ? e.getMessage() : ""));
        }
        if (screenDimOverlay != null) {
            ViewGroup parent = (ViewGroup) screenDimOverlay.getParent();
            if (parent != null) parent.removeView(screenDimOverlay);
            screenDimOverlay = null;
        }
    }

    private void refreshScreensaverFromUserInteraction() {
        if (isFinishing()) return;
        StartTurmtechnikService.touchHeartbeat();
        if (showingScreensaver) return;
        cancelScreenDimState();
        if (webView != null) {
            String currentUrl = webView.getUrl();
            if (currentUrl != null && !currentUrl.contains("screensaver.html")) {
                pageBeforeScreensaver = currentUrl;
            }
        }
        startScreensaverTimer();
    }

    public static void notifyUserInteraction() {
        final WebUiActivity activity = activeInstance;
        if (activity == null) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.refreshScreensaverFromUserInteraction();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activeInstance = this;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setGravity(Gravity.TOP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        hideSystemUi();
        setContentView(R.layout.activity_web_ui);

        String path = null;
        boolean useLocalhost = false;
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(EXTRA_PATH)) path = intent.getStringExtra(EXTRA_PATH);
            if (intent.hasExtra(EXTRA_USE_LOCALHOST)) useLocalhost = intent.getBooleanExtra(EXTRA_USE_LOCALHOST, false);
        }
        if (useLocalhost && path != null && !path.isEmpty()) {
            loadUrl = "http://127.0.0.1:8080/" + (path.startsWith("/") ? path.substring(1) : path);
        } else {
            loadUrl = TurmtechnikActivity.getWebUiUrlForPath(path);
        }

        // Server ggf. starten (wichtig nach Prozess-Neustart, wenn WebUiActivity vor TurmtechnikActivity läuft)
        TurmtechnikActivity.ensureWebServerStarted(getApplicationContext());

        // Heartbeat sofort: Service soll nicht nach 90 s eine zweite Activity starten (verhindert Neustart + „alle Relais aus“ beim Zurückkehren)
        StartTurmtechnikService.touchHeartbeat();

        webView = findViewById(R.id.web_ui_webview);
        setupWebView(webView);
        retryCount = 0;
        webView.loadUrl(loadUrl);

        if ("screensaver.html".equals(path)) {
            scheduleScreenDim();
        } else {
            // App-Seite (z. B. app-seite1/2): Schoner nach konfigurierter Zeit in derselben WebView anzeigen
            pageBeforeScreensaver = loadUrl;
            startScreensaverTimer();
        }
    }

    /** Startet den Timer: Nach X Min wechseln wir in der WebView zu screensaver.html. */
    private void startScreensaverTimer() {
        screensaverHandler.removeCallbacks(screensaverRunnable);
        long delayMs = TurmtechnikActivity.getScreensaverDelayMsStatic(getApplicationContext());
        screensaverRunnable = new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || webView == null) return;
                Log.w("Screensaver", "WebUiActivity: Schoner anzeigen (nach " + (delayMs / 60000) + " Min)");
                showingScreensaver = true;
                webView.loadUrl(SCREENSAVER_URL);
                scheduleScreenDim();
            }
        };
        screensaverHandler.postDelayed(screensaverRunnable, delayMs);
    }

    /** Liest aus Config „Bildschirm aus nach (Minuten)“ und startet Timer zum Abdunkeln. */
    private void scheduleScreenDim() {
        screenDimRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    WindowManager.LayoutParams lp = getWindow().getAttributes();
                    lp.screenBrightness = 0f;
                    getWindow().setAttributes(lp);
                    // Zusätzlich schwarzes Vollbild-Overlay, damit der Bildschirm garantiert ganz abgedunkelt ist
                    screenDimOverlay = new View(WebUiActivity.this);
                    screenDimOverlay.setBackgroundColor(0xFF000000);
                    screenDimOverlay.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    screenDimOverlay.setClickable(true);
                    screenDimOverlay.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            handleScreensaverDismiss();
                        }
                    });
                    ViewGroup root = (ViewGroup) getWindow().getDecorView();
                    root.addView(screenDimOverlay);
                } catch (Exception e) {
                    Log.w("WebUiActivity", "Bildschirm abdunkeln: " + (e != null ? e.getMessage() : ""));
                }
            }
        };
        new Thread(new Runnable() {
            @Override
            public void run() {
                int minuten = 0;
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL("http://127.0.0.1:8080/api/anlagendaten").openConnection();
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);
                    if (conn.getResponseCode() == 200) {
                        BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = r.readLine()) != null) sb.append(line);
                        r.close();
                        JSONObject j = new JSONObject(sb.toString());
                        String v = j.optString("bildschirmAusMinuten", "60").trim();
                        if (!v.isEmpty()) minuten = Integer.parseInt(v);
                    }
                } catch (Exception e) {
                    Log.w("WebUiActivity", "Anlagendaten für Bildschirm-Aus: " + (e != null ? e.getMessage() : ""));
                }
                final int delayMin = Math.min(240, Math.max(0, minuten));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (delayMin > 0 && !isFinishing()) {
                            screenOffHandler.postDelayed(screenDimRunnable, delayMin * 60L * 1000L);
                        }
                    }
                });
            }
        }).start();
    }

    /** Custom-URL: Zurück zur App (Layout Seite 1) statt Serverseite. */
    private static final String SCHEME_BACK_TO_APP = "turmt://layout1";

    /** Standard-URL nach Schoner, wenn keine vorherige Seite (wie Wechsel zu Seite 1/2 – kein App-Neustart, Relais bleiben unverändert). */
    private static final String DEFAULT_PAGE_AFTER_SCREENSAVER = "http://127.0.0.1:8080/app-seite1.html";

    /** Schoner-Tipp: Immer nur Seite wechseln (wie bei App Seite 1/2) – keine TurmtechnikActivity starten, damit App nicht neu startet und Relais unverändert bleiben. */
    private void handleScreensaverDismiss() {
        if (webView == null) return;
        showingScreensaver = false;
        cancelScreenDimState();
        String targetUrl = (pageBeforeScreensaver != null && !pageBeforeScreensaver.isEmpty())
                ? pageBeforeScreensaver
                : DEFAULT_PAGE_AFTER_SCREENSAVER;
        webView.loadUrl(targetUrl);
        pageBeforeScreensaver = targetUrl; // für nächsten Schoner-Wechsel
        startScreensaverTimer();
    }

    /** Startet die RustDesk-App, falls installiert (z. B. com.carriez.flutter_hbb). */
    private void launchRustDesk() {
        try {
            PackageManager pm = getPackageManager();
            Intent launch = pm.getLaunchIntentForPackage(RUSTDESK_PACKAGE);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launch);
            } else {
                Toast.makeText(this, "RustDesk nicht installiert", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.w("WebUiActivity", "RustDesk starten: " + (e != null ? e.getMessage() : ""));
            Toast.makeText(this, "RustDesk konnte nicht gestartet werden", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView wv) {
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request != null ? request.getUrl().toString() : null;
                if (url != null && url.startsWith("turmt://")) {
                    if (SCHEME_BACK_TO_APP.equals(url) || url.startsWith("turmt://layout1")) {
                        handleScreensaverDismiss();
                    } else if ("turmt://rustdesk".equals(url)) {
                        launchRustDesk();
                    }
                    return true;
                }
                return false;
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && url.startsWith("turmt://")) {
                    if (SCHEME_BACK_TO_APP.equals(url) || url.startsWith("turmt://layout1")) {
                        handleScreensaverDismiss();
                    } else if ("turmt://rustdesk".equals(url)) {
                        launchRustDesk();
                    }
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (view != null) view.scrollTo(0, 0);
                if (url != null && url.contains("screensaver.html")) {
                    hideSystemUi();
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!isFinishing()) hideSystemUi();
                        }
                    }, 150);
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e("WebUiActivity", "WebView error: " + description + " " + failingUrl);
                if (description != null && description.contains("ERR_CONNECTION_REFUSED") && retryCount < MAX_RETRIES) {
                    retryCount++;
                    Toast.makeText(WebUiActivity.this, "Server startet, bitte warten … (" + retryCount + "/" + MAX_RETRIES + ")", Toast.LENGTH_SHORT).show();
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!isFinishing() && webView != null) {
                                webView.loadUrl(loadUrl);
                            }
                        }
                    }, RETRY_DELAY_MS);
                } else {
                    Toast.makeText(WebUiActivity.this, "Seite konnte nicht geladen werden: " + failingUrl, Toast.LENGTH_SHORT).show();
                }
            }
        });
        WebSettings settings = wv.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        StartTurmtechnikService.reportVisibleActivity(this, getClass().getName());
        StartTurmtechnikService.touchHeartbeat();
        refreshScreensaverFromUserInteraction();
        startHeartbeat();
    }

    @Override
    protected void onPause() {
        StartTurmtechnikService.reportHiddenActivity(getClass().getName());
        stopHeartbeat();
        super.onPause();
    }

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) return;
                StartTurmtechnikService.touchHeartbeat();
                Intent hb = new Intent(WebUiActivity.this, StartTurmtechnikService.class);
                hb.setAction(StartTurmtechnikService.ACTION_HEARTBEAT);
                startService(hb);
                heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
            }
        };
        heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL_MS);
    }

    private void stopHeartbeat() {
        if (heartbeatRunnable != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
            heartbeatRunnable = null;
        }
    }

    @Override
    protected void onDestroy() {
        if (activeInstance == this) {
            activeInstance = null;
        }
        StartTurmtechnikService.reportHiddenActivity(getClass().getName());
        stopHeartbeat();
        screensaverHandler.removeCallbacks(screensaverRunnable);
        if (screenDimRunnable != null) {
            screenOffHandler.removeCallbacks(screenDimRunnable);
        }
        super.onDestroy();
    }

    public static void closeIfOpen() {
        final WebUiActivity activity = activeInstance;
        if (activity == null) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!activity.isFinishing()) {
                    activity.finish();
                }
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (ev != null && ev.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            refreshScreensaverFromUserInteraction();
        }
        return super.dispatchTouchEvent(ev);
    }

    /** Tastendruck (z. B. E) bei Schoner/abgedunkeltem Bildschirm: Aufwecken und direkt Layout-Seite anzeigen, ohne Sperrbildschirm. */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && !(showingScreensaver || screenDimOverlay != null)) {
            onBackPressed();
            return true;
        }
        if (showingScreensaver || screenDimOverlay != null) {
            try {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (pm != null && !pm.isInteractive()) {
                    PowerManager.WakeLock wl = pm.newWakeLock(
                            PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                            "WebUiActivity:KeyWake");
                    wl.acquire(3000);
                    new Handler(Looper.getMainLooper()).postDelayed(wl::release, 800);
                }
            } catch (Exception e) {
                Log.w("WebUiActivity", "WakeLock bei Tastendruck: " + (e != null ? e.getMessage() : ""));
            }
            handleScreensaverDismiss();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUi();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            try {
                TurmtechnikActivity.prepareAppExitForSettings(getApplicationContext(), 30);
                Intent home = new Intent(Intent.ACTION_MAIN);
                home.addCategory(Intent.CATEGORY_HOME);
                home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(home);
                moveTaskToBack(true);
                finish();
            } catch (Exception e) {
                Log.w("WebUiActivity", "Zurück aktiviert Tablet-Freigabe nicht: " + (e != null ? e.getMessage() : ""));
                moveTaskToBack(true);
            }
        }
    }

    private void hideSystemUi() {
        Window window = getWindow();
        View decorView = window.getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController insets = window.getInsetsController();
            if (insets != null) {
                insets.hide(WindowInsets.Type.systemBars());
                insets.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                flags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }
            decorView.setSystemUiVisibility(flags);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            window.setNavigationBarColor(Color.TRANSPARENT);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}
