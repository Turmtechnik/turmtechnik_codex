package tom.turmtechnik;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleService;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private ExecutorService executor;
    private Handler mainHandler;
    private ProcessCameraProvider cameraProvider;
    private long lastWakeMs = 0L;
    private byte[] previousLuma;
    private long lastComparisonMs = 0L;
    private int configIntervalSec = 2;
    private int configSensitivity = 50; // 1–100, höher = weniger empfindlich
    private boolean configOnlyWhenScreenOff = true;

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
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
        startCamera();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        if (cameraProvider != null) {
            try {
                cameraProvider.unbindAll();
            } catch (Exception e) {
                Log.w(TAG, "unbindAll: " + e.getMessage());
            }
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
            configOnlyWhenScreenOff = "ein".equalsIgnoreCase(v != null ? v.trim() : "ein");
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

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider provider = future.get();
                    cameraProvider = provider;
                    bindUseCases(provider);
                } catch (Exception e) {
                    Log.e(TAG, "Kamera starten: " + e.getMessage(), e);
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindUseCases(ProcessCameraProvider provider) {
        try {
            provider.unbindAll();
            // Nur Frontkamera
            CameraSelector selector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build();
            ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build();
            imageAnalysis.setAnalyzer(executor, new MotionAnalyzer());
            provider.bindToLifecycle(this, selector, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "bindUseCases: " + e.getMessage(), e);
        }
    }

    private class MotionAnalyzer implements ImageAnalysis.Analyzer {
        @Override
        public void analyze(@NonNull ImageProxy image) {
            try {
                long now = System.currentTimeMillis();
                if (now - lastComparisonMs < configIntervalSec * 1000L) {
                    image.close();
                    return;
                }
                lastComparisonMs = now;
                if (configOnlyWhenScreenOff && !StaticVariable.screenIsOff) {
                    image.close();
                    return;
                }
                byte[] luma = copyLuma(image);
                image.close();
                if (luma == null) return;
                boolean motion = previousLuma != null && detectMotion(previousLuma, luma, configSensitivity);
                previousLuma = luma;
                if (motion) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            wakeScreenIfDebounce();
                        }
                    });
                }
            } catch (Exception e) {
                Log.w(TAG, "analyze: " + e.getMessage());
            }
        }
    }

    private byte[] copyLuma(ImageProxy image) {
        try {
            ImageProxy.PlaneProxy plane = image.getPlanes()[0];
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
        return avgDiff > threshold;
    }

    private void wakeScreenIfDebounce() {
        long now = System.currentTimeMillis();
        if (now - lastWakeMs < DEBOUNCE_MS) return;
        lastWakeMs = now;
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm == null) return;
        PowerManager.WakeLock wl = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "turmtechnik:motion");
        try {
            wl.acquire(5000);
        } catch (Exception e) {
            Log.w(TAG, "WakeLock: " + e.getMessage());
        }
        Intent intent = new Intent(this, TurmtechnikActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("woke_by_motion", true);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "Activity starten: " + e.getMessage());
        }
        if (wl.isHeld()) {
            try {
                wl.release();
            } catch (Exception ignored) {}
        }
    }
}
