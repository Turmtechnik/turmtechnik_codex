package tom.turmtechnik;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
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
import java.util.List;

/**
 * Zeigt die Web-UI (z. B. Benutzerprogramme) im Vollbild in einer WebView.
 */
public class WebUiActivity extends Activity {
    private static volatile WebUiActivity activeInstance;

    public static final String EXTRA_PATH = "path";
    /** Wenn true: URL = http://127.0.0.1:8080/<path> (immer funktionsfähig beim Bildschirmschoner vom gleichen Gerät). */
    public static final String EXTRA_USE_LOCALHOST = "use_localhost";
    private static final int RETRY_DELAY_MS = 2000;
    private static final int MAX_RETRIES = 5;
    private static final String SCREENSAVER_URL = "http://127.0.0.1:8080/screensaver.html";
    private static final String RUSTDESK_PACKAGE = "com.carriez.flutter_hbb";
    private static final long HEARTBEAT_INTERVAL_MS = 60_000L;
    private static final String SCHEME_BACK_TO_APP = "turmt://layout1";
    private static final String DEFAULT_PAGE_AFTER_SCREENSAVER = "http://127.0.0.1:8080/app-seite1.html";

    private WebView webView;
    private String loadUrl;
    private int retryCount;
    private String pageBeforeScreensaver;
    private boolean showingScreensaver;
    private final Handler screenOffHandler = new Handler(Looper.getMainLooper());
    private Runnable screenDimRunnable;
    private View screenDimOverlay;
    private final Handler screensaverHandler = new Handler(Looper.getMainLooper());
    private Runnable screensaverRunnable;
    private final Handler heartbeatHandler = new Handler(Looper.getMainLooper());
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
            if (parent != null) {
                parent.removeView(screenDimOverlay);
            }
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
        if (maybeReleaseTabletInsteadOfKioskStart(getIntent())) {
            return;
        }
        activeInstance = this;
        TurmtechnikActivity.scheduleNeustartNotificationsOnce(getApplicationContext());
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

        String path = "/app-seite1.html";
        boolean useLocalhost = false;
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(EXTRA_PATH)) path = intent.getStringExtra(EXTRA_PATH);
            if (intent.hasExtra(EXTRA_USE_LOCALHOST)) useLocalhost = intent.getBooleanExtra(EXTRA_USE_LOCALHOST, false);
        }
        loadUrl = buildTargetUrl(path, useLocalhost);

        TurmtechnikActivity.ensureCoreRuntimeStarted(getApplicationContext());
        StartTurmtechnikService.touchHeartbeat();

        webView = findViewById(R.id.web_ui_webview);
        setupWebView(webView);
        retryCount = 0;
        webView.loadUrl(loadUrl);

        if ("screensaver.html".equals(path)) {
            scheduleScreenDim();
        } else {
            pageBeforeScreensaver = loadUrl;
            startScreensaverTimer();
        }
    }

    private String buildTargetUrl(String path, boolean useLocalhost) {
        String resolvedPath = (path == null || path.isEmpty()) ? "/app-seite1.html" : path;
        if (useLocalhost) {
            return "http://127.0.0.1:8080/" + (resolvedPath.startsWith("/") ? resolvedPath.substring(1) : resolvedPath);
        }
        return TurmtechnikActivity.getWebUiUrlForPath(resolvedPath);
    }

    private void startScreensaverTimer() {
        if (screensaverRunnable != null) {
            screensaverHandler.removeCallbacks(screensaverRunnable);
        }
        final long delayMs = TurmtechnikActivity.getScreensaverDelayMsStatic(getApplicationContext());
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

    private void scheduleScreenDim() {
        screenDimRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    WindowManager.LayoutParams lp = getWindow().getAttributes();
                    lp.screenBrightness = 0f;
                    getWindow().setAttributes(lp);
                    screenDimOverlay = new View(WebUiActivity.this);
                    screenDimOverlay.setBackgroundColor(0xFF000000);
                    screenDimOverlay.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
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

    private boolean isTemporaryTabletReleaseActive() {
        long now = System.currentTimeMillis();
        return TurmtechnikActivity.getExitForSettingsUntilMillis(this) > now
                || TurmtechnikActivity.getBackgroundAllowedUntilMillis(this) > now;
    }

    private boolean isStartedFromHomeOrLauncher(Intent intent) {
        if (intent == null) return false;
        return intent.hasCategory(Intent.CATEGORY_HOME);
    }

    private void openTabletNormalSurface() {
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            String ourPackage = getPackageName();
            List<ResolveInfo> homes = getPackageManager().queryIntentActivities(home, 0);
            if (homes != null) {
                for (ResolveInfo ri : homes) {
                    if (ri != null && ri.activityInfo != null && !ourPackage.equals(ri.activityInfo.packageName)) {
                        Intent launcher = new Intent(Intent.ACTION_MAIN);
                        launcher.addCategory(Intent.CATEGORY_HOME);
                        launcher.setPackage(ri.activityInfo.packageName);
                        launcher.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(launcher);
                        moveTaskToBack(true);
                        finish();
                        return;
                    }
                }
            }
        } catch (Exception e) {
            Log.w("WebUiActivity", "Externer Launcher fehlgeschlagen: " + (e != null ? e.getMessage() : ""));
        }
        try {
            startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            moveTaskToBack(true);
            finish();
        } catch (Exception e) {
            Log.w("WebUiActivity", "Settings-Fallback fehlgeschlagen: " + (e != null ? e.getMessage() : ""));
        }
    }

    private boolean maybeReleaseTabletInsteadOfKioskStart(Intent intent) {
        if (!isTemporaryTabletReleaseActive()) {
            return false;
        }
        if (!isStartedFromHomeOrLauncher(intent)) {
            return false;
        }
        Log.i("WebUiActivity", "Kiosk-Start unterdrückt: Tablet-Freigabe aktiv");
        openTabletNormalSurface();
        return true;
    }

    private void loadLayoutPage1IfNeeded() {
        if (webView == null) return;
        String currentUrl = webView.getUrl();
        if (currentUrl == null || !currentUrl.contains("app-seite1.html")) {
            showingScreensaver = false;
            cancelScreenDimState();
            pageBeforeScreensaver = DEFAULT_PAGE_AFTER_SCREENSAVER;
            webView.loadUrl(DEFAULT_PAGE_AFTER_SCREENSAVER);
            startScreensaverTimer();
        }
    }

    private void handleScreensaverDismiss() {
        if (webView == null) return;
        showingScreensaver = false;
        cancelScreenDimState();
        String targetUrl = (pageBeforeScreensaver != null && !pageBeforeScreensaver.isEmpty())
                ? pageBeforeScreensaver
                : DEFAULT_PAGE_AFTER_SCREENSAVER;
        webView.loadUrl(targetUrl);
        pageBeforeScreensaver = targetUrl;
        startScreensaverTimer();
    }

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
                            if (!isFinishing()) {
                                hideSystemUi();
                            }
                        }
                    }, 150);
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e("WebUiActivity", "WebView error: " + description + " " + failingUrl);
                if (description != null && description.contains("ERR_CONNECTION_REFUSED") && retryCount < MAX_RETRIES) {
                    retryCount++;
                    Toast.makeText(WebUiActivity.this, "Server startet, bitte warten ... (" + retryCount + "/" + MAX_RETRIES + ")", Toast.LENGTH_SHORT).show();
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
        if (maybeReleaseTabletInsteadOfKioskStart(getIntent())) {
            return;
        }
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
        if (screensaverRunnable != null) {
            screensaverHandler.removeCallbacks(screensaverRunnable);
        }
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && !(showingScreensaver || screenDimOverlay != null)) {
            loadLayoutPage1IfNeeded();
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
            loadLayoutPage1IfNeeded();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (maybeReleaseTabletInsteadOfKioskStart(intent)) {
            return;
        }
        if (webView != null && intent != null && (intent.hasExtra(EXTRA_PATH) || intent.hasCategory(Intent.CATEGORY_HOME) || intent.hasCategory(Intent.CATEGORY_LAUNCHER))) {
            String path = intent.hasExtra(EXTRA_PATH) ? intent.getStringExtra(EXTRA_PATH) : "/app-seite1.html";
            boolean useLocalhost = intent.getBooleanExtra(EXTRA_USE_LOCALHOST, true);
            loadUrl = buildTargetUrl(path, useLocalhost);
            if (!loadUrl.equals(webView.getUrl())) {
                webView.loadUrl(loadUrl);
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
