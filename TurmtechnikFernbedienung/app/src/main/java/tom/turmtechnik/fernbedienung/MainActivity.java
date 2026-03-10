package tom.turmtechnik.fernbedienung;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Fernbedienung für Turmtechnik: Einmal Adresse (Tailscale-IP oder Hostname) eingeben,
 * danach startet die App direkt mit „App Seite 1“ (http://&lt;Adresse&gt;:8080/app-seite1.html).
 */
public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "TurmtechnikFernbedienung";
    private static final String KEY_HOST = "host";
    private static final String KEY_PWD_HASH = "pwd_hash";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String LAYOUT_PAGE = "/app-seite1.html";
    private static final int PORT = 8080;
    private static final int RUSTDESK_PORT = 21118;
    private static final int MIN_PASSWORD_LENGTH = 4;

    private View layoutLogin;
    private View layoutSetPassword;
    private View layoutSetup;
    private View layoutWeb;
    private EditText editHost;
    private EditText editLoginPassword;
    private EditText editSetPassword;
    private EditText editSetPasswordRepeat;
    private WebView webView;
    private View bottomBarContainer;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        layoutLogin = findViewById(R.id.layout_login);
        layoutSetPassword = findViewById(R.id.layout_set_password);
        layoutSetup = findViewById(R.id.layout_setup);
        layoutWeb = findViewById(R.id.layout_web);
        editHost = findViewById(R.id.edit_host);
        editLoginPassword = findViewById(R.id.edit_login_password);
        editSetPassword = findViewById(R.id.edit_set_password);
        editSetPasswordRepeat = findViewById(R.id.edit_set_password_repeat);
        webView = findViewById(R.id.webview);
        bottomBarContainer = findViewById(R.id.bottom_bar_container);

        findViewById(R.id.btn_save).setOnClickListener(v -> saveAndOpen());
        findViewById(R.id.btn_change_address).setOnClickListener(v -> showSetup());
        findViewById(R.id.btn_rustdesk).setOnClickListener(v -> launchRustDesk());
        findViewById(R.id.btn_login).setOnClickListener(v -> doLogin());
        findViewById(R.id.btn_set_password).setOnClickListener(v -> doSetPassword());
        findViewById(R.id.btn_logout).setOnClickListener(v -> doLogout());

        findViewById(R.id.btn_menu).setOnClickListener(v -> {
            if (bottomBarContainer != null) {
                boolean visible = bottomBarContainer.getVisibility() == View.VISIBLE;
                bottomBarContainer.setVisibility(visible ? View.GONE : View.VISIBLE);
            }
        });

        findViewById(R.id.btn_page1).setOnClickListener(v -> loadPage("/app-seite1.html"));
        findViewById(R.id.btn_page2).setOnClickListener(v -> loadPage("/app-seite2.html"));
        findViewById(R.id.btn_startpage).setOnClickListener(v -> loadPage("/"));

        setupWebView();

        String pwdHash = prefs.getString(KEY_PWD_HASH, "").trim();
        boolean loggedIn = prefs.getBoolean(KEY_LOGGED_IN, false);

        if (pwdHash.isEmpty()) {
            showOnly(layoutSetPassword);
        } else if (!loggedIn) {
            showOnly(layoutLogin);
        } else {
            onLoginSuccess();
        }
    }

    private void showOnly(View visibleLayout) {
        layoutLogin.setVisibility(View.GONE);
        layoutSetPassword.setVisibility(View.GONE);
        layoutSetup.setVisibility(View.GONE);
        layoutWeb.setVisibility(View.GONE);
        visibleLayout.setVisibility(View.VISIBLE);
    }

    private void doLogin() {
        String input = editLoginPassword != null ? editLoginPassword.getText().toString() : "";
        String hash = hashPassword(input);
        String stored = prefs.getString(KEY_PWD_HASH, "");
        if (hash != null && hash.equals(stored)) {
            prefs.edit().putBoolean(KEY_LOGGED_IN, true).apply();
            if (editLoginPassword != null) editLoginPassword.setText("");
            onLoginSuccess();
        } else {
            Toast.makeText(this, R.string.login_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void doSetPassword() {
        String p1 = editSetPassword != null ? editSetPassword.getText().toString() : "";
        String p2 = editSetPasswordRepeat != null ? editSetPasswordRepeat.getText().toString() : "";
        if (p1.length() < MIN_PASSWORD_LENGTH) {
            Toast.makeText(this, R.string.set_password_too_short, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!p1.equals(p2)) {
            Toast.makeText(this, R.string.set_password_mismatch, Toast.LENGTH_SHORT).show();
            return;
        }
        String hash = hashPassword(p1);
        if (hash != null) {
            prefs.edit().putString(KEY_PWD_HASH, hash).putBoolean(KEY_LOGGED_IN, true).apply();
            if (editSetPassword != null) editSetPassword.setText("");
            if (editSetPasswordRepeat != null) editSetPasswordRepeat.setText("");
            onLoginSuccess();
        }
    }

    private void doLogout() {
        prefs.edit().putBoolean(KEY_LOGGED_IN, false).apply();
        showOnly(layoutLogin);
        if (editLoginPassword != null) editLoginPassword.setText("");
    }

    /** Nach erfolgreicher Anmeldung: Setup oder WebView anzeigen. */
    private void onLoginSuccess() {
        String host = prefs.getString(KEY_HOST, "").trim();
        if (!host.isEmpty()) {
            showWebView(host);
        } else {
            showOnly(layoutSetup);
            editHost.setText("");
            editHost.requestFocus();
        }
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest((password != null ? password : "").getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** Lädt eine Seite im WebView (host aus Prefs). */
    private void loadPage(String path) {
        String host = prefs.getString(KEY_HOST, "").trim();
        if (host.isEmpty()) return;
        String url = "http://" + host + ":" + PORT + path;
        webView.loadUrl(url);
    }

    private void showSetup() {
        layoutWeb.setVisibility(View.GONE);
        layoutLogin.setVisibility(View.GONE);
        layoutSetPassword.setVisibility(View.GONE);
        layoutSetup.setVisibility(View.VISIBLE);
        editHost.setText(prefs.getString(KEY_HOST, ""));
        editHost.requestFocus();
    }

    /** Viewport-Meta per JS auf zoombar setzen (Seite liefert user-scalable=no). */
    private static final String JS_FIX_VIEWPORT =
            "var m=document.querySelector('meta[name=viewport]')||document.getElementById('tt-viewport');"
            + "if(m){ m.setAttribute('content','width=device-width, initial-scale=0.5, minimum-scale=0.25, maximum-scale=4, user-scalable=yes, viewport-fit=cover'); }";
    /** Header ausblenden und oberen Abstand entfernen, damit Tasten bis oben angezeigt werden. */
    private static final String JS_HIDE_HEADER =
            "var h=document.querySelector('header.tt-header'); if(h){ h.style.display='none'; }"
            + "var w=document.querySelector('.app-seite-wrap'); if(w){ w.style.paddingTop='0.25rem'; }"
            + "var m=document.querySelector('.app-page-main'); if(m){ m.style.paddingTop='0.5rem'; }";

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript(JS_FIX_VIEWPORT, null);
                view.evaluateJavascript(JS_HIDE_HEADER, null);
            }
        });
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
    }

    private void saveAndOpen() {
        String host = editHost.getText().toString().trim();
        if (host.isEmpty()) {
            Toast.makeText(this, R.string.hint_enter_address, Toast.LENGTH_SHORT).show();
            return;
        }
        // Optional: "http://" und ":8080" entfernen, wir bauen die URL selbst
        host = host.replaceFirst("^https?://", "").split("/")[0].split(":")[0];
        prefs.edit().putString(KEY_HOST, host).apply();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        showWebView(host);
    }

    private void showWebView(String host) {
        layoutSetup.setVisibility(View.GONE);
        layoutLogin.setVisibility(View.GONE);
        layoutSetPassword.setVisibility(View.GONE);
        layoutWeb.setVisibility(View.VISIBLE);

        TextView topBar = findViewById(R.id.top_bar_title);
        if (topBar != null) {
            topBar.setText(getString(R.string.top_bar_connected, "…"));
        }

        String url = "http://" + host + ":" + PORT + LAYOUT_PAGE;
        webView.loadUrl(url);

        loadBaustelleName(host, topBar);
    }

    /** Ruft /api/anlagendaten ab und zeigt „Verbunden: Baustellenname“ in der oberen Leiste. */
    private void loadBaustelleName(String host, TextView topBar) {
        if (topBar == null) return;
        new Thread(() -> {
            String baustelle = null;
            try {
                URL apiUrl = new URL("http://" + host + ":" + PORT + "/api/anlagendaten");
                HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    JSONObject json = new JSONObject(sb.toString());
                    if (json.has("baustelleName")) {
                        String name = json.optString("baustelleName", "").trim();
                        if (!name.isEmpty()) baustelle = name;
                    }
                }
            } catch (Exception ignored) { }
            final String display = (baustelle != null && !baustelle.isEmpty()) ? baustelle : host;
            runOnUiThread(() -> topBar.setText(getString(R.string.top_bar_connected, display)));
        }).start();
    }

    /** Öffnet RustDesk mit rustdesk://&lt;gespeicherte_Adresse&gt;:21118 für Direktverbindung (z. B. über Tailscale). */
    private void launchRustDesk() {
        String host = prefs.getString(KEY_HOST, "").trim();
        if (host.isEmpty()) {
            Toast.makeText(this, R.string.rustdesk_no_address, Toast.LENGTH_SHORT).show();
            return;
        }
        String rustdeskUri = "rustdesk://" + host + ":" + RUSTDESK_PORT;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(rustdeskUri));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.btn_rustdesk) + ": " + (e.getMessage() != null ? e.getMessage() : "RustDesk nicht installiert?"), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
