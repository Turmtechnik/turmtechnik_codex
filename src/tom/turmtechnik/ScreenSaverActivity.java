package tom.turmtechnik;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * Bildschirmschoner: Zeigt die analoge Uhr fullscreen.
 * Berührung/Klick beendet die Activity und kehrt zur TurmtechnikActivity zurück.
 * Nur Statusleiste wird ausgeblendet (kein immersiver Modus), damit der System-Hinweis
 * „Sie verwenden den Vollbildmodus…“ nicht erscheint.
 */
public class ScreenSaverActivity extends Activity {

    /** Mindestdauer in ms, bevor eine Berührung den Schoner beendet (verhindert sofortiges Beenden). */
    private static final long TOUCH_GRACE_MS = 2500L;
    private long createdAtMs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createdAtMs = System.currentTimeMillis();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUi();
        setContentView(R.layout.activity_screensaver);
        Log.e("Screensaver", "ScreenSaverActivity onCreate – UHR ANZEIGE");

        FrameLayout root = findViewById(R.id.screensaver_root);
        if (root != null) {
            root.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (System.currentTimeMillis() - createdAtMs >= TOUCH_GRACE_MS) {
                        finish();
                    }
                }
            });
        }
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
        if (System.currentTimeMillis() - createdAtMs >= TOUCH_GRACE_MS) {
            super.onBackPressed();
        }
    }

    /**
     * Blendet nur die Statusleiste aus (FULLSCREEN), nicht die Navigation.
     * Ohne HIDE_NAVIGATION / IMMERSIVE erscheint der weiße System-Hinweis „Vollbildmodus“ nicht.
     */
    private void hideSystemUi() {
        View decorView = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(flags);
        }
    }
}
