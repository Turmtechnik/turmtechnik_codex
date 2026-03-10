package tom.turmtechnik;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleService;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Foreground-Service: Nutzt die Frontkamera zur Bewegungserkennung und weckt bei
 * erkannter Bewegung den Bildschirm (WakeLock + TurmtechnikActivity in den Vordergrund).
 * Konfiguration über anlage_bewegungserkennung_* in der DB.
 */
public class MotionDetectionService extends LifecycleService {

    private static final String TAG = "MotionDetectionService";
    private static final int NOTIFICATION_ID = 9002;
    private static final String CHANNEL_ID = "turmtechnik_motion";
    /** Mindestabstand (ms) zwischen zwei Aufweck-Aktionen. */
    private static final long DEBOUNCE_MS = 30_000L;
    private static final int WAKE_ACTIVITY_FLAGS =
            Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP;

    private Handler mainHandler;
    private volatile boolean stopped = false;
    private CameraDevice cameraDevice;
    private ImageReader imageReader;
    private HandlerThread cameraThread;
    private Handler cameraHandler;
    private long lastWakeMs = 0L;
    private int frameCount = 0;
    private byte[] previousLuma;
    private static final long FRAME_WAIT_LOG_INTERVAL_MS = 15_000L;
    private long lastComparisonMs = 0L;
    private int configIntervalSec = 2;
    private int configSensitivity = 50; // 1–100, höher = weniger empfindlich
    private boolean configOnlyWhenScreenOff = true;

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT < 21) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Keine CAMERA-Berechtigung – Service beenden");
            stopSelf();
            return START_NOT_STICKY;
        }
        loadConfig();
        if (!"ein".equalsIgnoreCase(PlatinenDatabaseHelper.getInstance(this).getConfigValue("anlage_bewegungserkennung_ein"))) {
            stopSelf();
            return START_NOT_STICKY;
        }
        startForegroundWithNotification();
        // Kamera erst nach kurzer Verzögerung starten – auf manchen Geräten (z. B. T830) crasht sofortiger Start im nativen Code
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!stopped) startCamera();
            }
        }, 3000);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopped = true;
        try {
            if (imageReader != null) {
                imageReader.close();
                imageReader = null;
            }
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (cameraThread != null && cameraThread.isAlive()) {
                cameraThread.quitSafely();
            }
        } catch (Exception e) {
            Log.w(TAG, "onDestroy Kamera: " + e.getMessage());
        }
        super.onDestroy();
    }

    private void loadConfig() {
        try {
            PlatinenDatabaseHelper db = PlatinenDatabaseHelper.getInstance(this);
            String v = db.getConfigValue("anlage_bewegungserkennung_intervall_sekunden");
            configIntervalSec = parsePositiveInt(v, 2);
            if (configIntervalSec < 1) configIntervalSec = 1;
            if (configIntervalSec > 10) configIntervalSec = 10;
            v = db.getConfigValue("anlage_bewegungserkennung_empfindlichkeit");
            configSensitivity = parsePositiveInt(v, 50);
            if (configSensitivity < 1) configSensitivity = 1;
            if (configSensitivity > 100) configSensitivity = 100;
            v = db.getConfigValue("anlage_bewegungserkennung_nur_bei_bildschirm_aus");
            configOnlyWhenScreenOff = "ein".equalsIgnoreCase((v != null && !v.trim().isEmpty()) ? v.trim() : "");
        } catch (Exception e) {
            Log.w(TAG, "Config laden: " + e.getMessage());
        }
    }

    private static int parsePositiveInt(String v, int def) {
        if (v == null || v.trim().isEmpty()) return def;
        try {
            int n = Integer.parseInt(v.trim());
            return n > 0 ? n : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @SuppressWarnings("deprecation")
    private void startForegroundWithNotification() {
        try {
            android.app.Notification notification = buildNotification();
            if (notification != null) {
                if (Build.VERSION.SDK_INT >= 34) {
                    startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA);
                } else {
                    startForeground(NOTIFICATION_ID, notification);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "startForeground: " + e.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    private android.app.Notification buildNotification() {
        String title = getString(R.string.app_name);
        String text = "Bewegungserkennung (Frontkamera) aktiv";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    CHANNEL_ID,
                    "Bewegungserkennung",
                    android.app.NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
            return new android.app.Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(android.R.drawable.ic_menu_camera)
                    .setOngoing(true)
                    .build();
        } else {
            return new android.app.Notification.Builder(this)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(android.R.drawable.ic_menu_camera)
                    .setOngoing(true)
                    .build();
        }
    }

    /** Kamera per Camera2-API öffnen (liefert auf manchen Geräten Frames, wo CameraX im Service keine liefert). */
    private void startCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) {
            Log.e(TAG, "CameraManager null");
            return;
        }
        String frontId = null;
        try {
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics cc = manager.getCameraCharacteristics(id);
                Integer facing = cc.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    frontId = id;
                    break;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Kamera-Liste: " + e.getMessage());
            return;
        }
        if (frontId == null) {
            Log.e(TAG, "Keine Frontkamera gefunden");
            return;
        }
        Size size = pickImageSize(manager, frontId);
        if (size == null) {
            Log.e(TAG, "Keine passende Auflösung für YUV_420_888");
            return;
        }
        cameraThread = new HandlerThread("MotionCam");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
        imageReader = ImageReader.newInstance(size.getWidth(), size.getHeight(), ImageFormat.YUV_420_888, 2);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                if (stopped) return;
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image == null) return;
                    frameCount++;
                    if (frameCount <= 5 || frameCount % 50 == 0) {
                        Log.i(TAG, "Kamera: Frame #" + frameCount + " empfangen (Camera2)");
                    }
                    long now = System.currentTimeMillis();
                    if (now - lastComparisonMs < configIntervalSec * 1000L) return;
                    lastComparisonMs = now;
                    byte[] luma = copyLumaFromImage(image);
                    if (luma == null) {
                        if (frameCount <= 3) Log.w(TAG, "Kamera: copyLuma fehlgeschlagen (null)");
                        return;
                    }
                    if (previousLuma == null) {
                        previousLuma = luma;
                        Log.i(TAG, "Kamera: Erster Frame gespeichert – Vergleich ab nächstem Intervall (alle " + configIntervalSec + " s)");
                        return;
                    }
                    boolean motion = detectMotion(previousLuma, luma, configSensitivity);
                    previousLuma = luma;
                    if (motion) {
                        Log.i(TAG, "Kamera: Werteveränderung erkannt (Bewegung)");
                        // Immer: Layout1 (Hauptseite) in den Vordergrund und Bildschirm hell
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                wakeScreenIfDebounce();
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.w(TAG, "onImageAvailable: " + e.getMessage());
                } finally {
                    if (image != null) image.close();
                }
            }
        }, cameraHandler);
        try {
            manager.openCamera(frontId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    if (stopped) {
                        camera.close();
                        return;
                    }
                    cameraDevice = camera;
                    try {
                        final CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        builder.addTarget(imageReader.getSurface());
                        camera.createCaptureSession(Arrays.asList(imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                if (stopped || cameraDevice == null) return;
                                try {
                                    builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
                                    session.setRepeatingRequest(builder.build(), null, cameraHandler);
                                    Log.i(TAG, "Kamera gebunden (Camera2, " + size.getWidth() + "x" + size.getHeight() + "), Bewegungserkennung aktiv");
                                    scheduleFrameWaitLog();
                                } catch (Exception e) {
                                    Log.e(TAG, "setRepeatingRequest: " + e.getMessage());
                                }
                            }
                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) {
                                Log.e(TAG, "createCaptureSession fehlgeschlagen");
                            }
                        }, cameraHandler);
                    } catch (Exception e) {
                        Log.e(TAG, "createCaptureSession: " + e.getMessage());
                    }
                }
                @Override
                public void onDisconnected(CameraDevice camera) {
                    camera.close();
                    if (cameraDevice == camera) cameraDevice = null;
                }
                @Override
                public void onError(CameraDevice camera, int error) {
                    Log.e(TAG, "Kamera onError: " + error);
                    camera.close();
                    if (cameraDevice == camera) cameraDevice = null;
                }
            }, cameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "openCamera: " + e.getMessage());
        }
    }

    private Size pickImageSize(CameraManager manager, String cameraId) {
        try {
            CameraCharacteristics cc = manager.getCameraCharacteristics(cameraId);
            android.hardware.camera2.params.StreamConfigurationMap map = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) return null;
            Size[] sizes = map.getOutputSizes(ImageFormat.YUV_420_888);
            if (sizes == null || sizes.length == 0) return null;
            for (Size s : sizes) {
                if (s.getWidth() == 320 && s.getHeight() == 240) return s;
            }
            // Sonst kleinste verfügbare Auflösung (weniger Daten = schnellere Auswertung)
            Size smallest = sizes[0];
            int minPixels = smallest.getWidth() * smallest.getHeight();
            for (Size s : sizes) {
                int p = s.getWidth() * s.getHeight();
                if (p < minPixels && p >= 160 * 120) {
                    minPixels = p;
                    smallest = s;
                }
            }
            return smallest;
        } catch (Exception e) {
            return null;
        }
    }

    /** Loggt periodisch, ob bereits Frames angekommen sind (zum Debuggen). */
    private void scheduleFrameWaitLog() {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (stopped) return;
                if (frameCount == 0) {
                    Log.w(TAG, "Kamera: bisher keine Frames empfangen – prüfen ob Gerät Frames liefert");
                } else {
                    Log.i(TAG, "Kamera: " + frameCount + " Frames empfangen");
                }
                scheduleFrameWaitLog();
            }
        }, FRAME_WAIT_LOG_INTERVAL_MS);
    }

    private byte[] copyLumaFromImage(Image image) {
        try {
            Image.Plane plane = image.getPlanes()[0];
            ByteBuffer buffer = plane.getBuffer();
            int rowStride = plane.getRowStride();
            int pixelStride = plane.getPixelStride();
            int w = image.getWidth();
            int h = image.getHeight();
            int size = w * h;
            if (size <= 0 || size > 1024 * 1024) return null;
            byte[] out = new byte[size];
            int outIdx = 0;
            for (int y = 0; y < h; y++) {
                int srcIdx = y * rowStride;
                for (int x = 0; x < w; x++) {
                    out[outIdx++] = buffer.get(srcIdx);
                    srcIdx += pixelStride;
                }
            }
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Vergleicht zwei Luma-Puffer; höhere sensitivity = weniger empfindlich (größere Änderung nötig).
     */
    private boolean detectMotion(byte[] prev, byte[] curr, int sensitivity) {
        if (prev.length != curr.length || prev.length == 0) return false;
        long sumDiff = 0;
        int step = Math.max(1, prev.length / (80 * 60)); // max ~4800 Pixel auswerten
        for (int i = 0; i < prev.length; i += step) {
            sumDiff += Math.abs((curr[i] & 0xff) - (prev[i] & 0xff));
        }
        int samples = prev.length / step;
        double avgDiff = (double) sumDiff / samples;
        // sensitivity 1 = sehr empfindlich (schon kleine Änderung), 100 = unempfindlich
        double threshold = 5 + (100 - sensitivity) * 0.5;
        boolean motion = avgDiff > threshold;
        Log.i(TAG, "Kamera-Vergleich: avgDiff=" + String.format("%.1f", avgDiff) + ", Schwellwert=" + String.format("%.1f", threshold) + ", Bewegung=" + (motion ? "ja" : "nein"));
        return motion;
    }

    /** Bildschirm aus? */
    private boolean isScreenOff() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                return !pm.isInteractive();
            }
        } catch (Exception e) {
            Log.w(TAG, "isScreenOff: " + e.getMessage());
        }
        return false;
    }

    private void wakeScreenIfDebounce() {
        if (TurmtechnikActivity.isInForeground) {
            return; // Layoutseite schon offen und sichtbar – nichts tun (Overlay wird in onNewIntent/onResume beim nächsten Mal ausgeblendet)
        }
        long now = System.currentTimeMillis();
        if (now - lastWakeMs < DEBOUNCE_MS) {
            Log.i(TAG, "Schaltung unterdrückt (Debounce) – nächste Schaltung erst nach " + (DEBOUNCE_MS / 1000) + " s");
            return;
        }
        lastWakeMs = now;
        Log.i(TAG, "Schaltung: Bildschirm aufwecken / Activity in den Vordergrund");
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm == null) return;
        // Bildschirm explizit einschalten (z. B. nach Ein/Aus-Taste) – per Reflection, da wakeUp(long) erst in neueren SDK-Stubs
        if (Build.VERSION.SDK_INT >= 21) {
            try {
                java.lang.reflect.Method wakeUp = pm.getClass().getMethod("wakeUp", long.class);
                wakeUp.invoke(pm, SystemClock.uptimeMillis());
            } catch (Exception e) {
                if (e.getCause() != null) Log.w(TAG, "wakeUp: " + e.getCause().getMessage());
            }
        }
        PowerManager.WakeLock wl = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "turmtechnik:motion");
        try {
            wl.acquire(5000);
        } catch (Exception e) {
            Log.w(TAG, "WakeLock: " + e.getMessage());
        }
        // Activity nur starten/holen, wenn App nicht schon im Vordergrund – sonst würde bei jeder Bewegung erneut onResume/Initialisierung laufen
        if (!TurmtechnikActivity.isInForeground) {
            Intent intent = createWakeActivityIntent();
            // TURN_SCREEN_ON nicht per Intent-Flag (ab Android 14 nur erlaubte Flags), Activity/Manifest übernimmt ggf.
            intent.putExtra("woke_by_motion", true);
            try {
                startActivity(intent);
            } catch (Exception e) {
                Log.w(TAG, "Activity starten: " + e.getMessage());
            }
        }
        if (wl.isHeld()) {
            try {
                wl.release();
            } catch (Exception ignored) {}
        }
    }
    private Intent createWakeActivityIntent() {
        Intent intent = new Intent(this, WebUiActivity.class);
        intent.putExtra(WebUiActivity.EXTRA_PATH, "/app-seite1.html");
        intent.putExtra(WebUiActivity.EXTRA_USE_LOCALHOST, true);
        intent.addFlags(WAKE_ACTIVITY_FLAGS);
        return intent;
    }
}
