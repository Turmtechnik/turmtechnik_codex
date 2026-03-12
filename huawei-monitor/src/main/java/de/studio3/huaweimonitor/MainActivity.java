package de.studio3.huaweimonitor;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "huawei_monitor_prefs";
    private static final String PREF_HOST = "host";
    private static final String PREF_PORT = "port";
    private static final String PREF_UNIT_ID = "unit_id";
    private static final String PREF_REGISTER = "register";
    private static final String PREF_INTERVAL = "interval";
    private static final String PREF_INVERT_SIGN = "invert_sign";

    private static final int DEFAULT_PORT = 502;
    private static final int DEFAULT_UNIT_ID = 1;
    private static final int DEFAULT_REGISTER = 32278;
    private static final int DEFAULT_INTERVAL = 5;
    private static final int MODBUS_TIMEOUT_MS = 3000;
    private static final int DISCOVERY_TIMEOUT_MS = 180;
    private static final int SMART_METER_BLOCK_START = 32260;
    private static final int SMART_METER_BLOCK_LENGTH = 105;

    private EditText hostEditText;
    private EditText portEditText;
    private EditText unitIdEditText;
    private EditText registerEditText;
    private EditText intervalEditText;
    private CheckBox invertSignCheckBox;
    private TextView headlineTextView;
    private TextView powerTextView;
    private TextView detailsTextView;
    private TextView statusTextView;
    private TextView lastUpdateTextView;
    private TextView phaseVoltagesTextView;
    private TextView phaseCurrentsTextView;
    private TextView powerBreakdownTextView;
    private TextView energyCountersTextView;
    private Button startButton;
    private Button stopButton;
    private Button smartMeterPresetButton;
    private Button inverterPresetButton;
    private Button findHostsButton;
    private Button toggleFullscreenButton;
    private MaterialCardView settingsCardView;
    private MaterialCardView powerCardView;

    private final ModbusTcpClient modbusTcpClient = new ModbusTcpClient();
    private final NetworkDiscovery networkDiscovery = new NetworkDiscovery();
    private ScheduledExecutorService scheduler;
    private boolean monitoringActive;
    private boolean fullscreenMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hostEditText = findViewById(R.id.edit_host);
        portEditText = findViewById(R.id.edit_port);
        unitIdEditText = findViewById(R.id.edit_unit_id);
        registerEditText = findViewById(R.id.edit_register);
        intervalEditText = findViewById(R.id.edit_interval);
        invertSignCheckBox = findViewById(R.id.checkbox_invert_sign);
        headlineTextView = findViewById(R.id.text_headline);
        powerTextView = findViewById(R.id.text_power);
        detailsTextView = findViewById(R.id.text_details);
        statusTextView = findViewById(R.id.text_status);
        lastUpdateTextView = findViewById(R.id.text_last_update);
        phaseVoltagesTextView = findViewById(R.id.text_phase_voltages);
        phaseCurrentsTextView = findViewById(R.id.text_phase_currents);
        powerBreakdownTextView = findViewById(R.id.text_power_breakdown);
        energyCountersTextView = findViewById(R.id.text_energy_counters);
        startButton = findViewById(R.id.button_start);
        stopButton = findViewById(R.id.button_stop);
        smartMeterPresetButton = findViewById(R.id.button_preset_smart_meter);
        inverterPresetButton = findViewById(R.id.button_preset_inverter);
        findHostsButton = findViewById(R.id.button_find_hosts);
        toggleFullscreenButton = findViewById(R.id.button_toggle_fullscreen);
        settingsCardView = findViewById(R.id.card_settings);
        powerCardView = findViewById(R.id.card_power);

        loadPreferences();
        updateDisplay(0, "Bereit", getString(R.string.details_placeholder));
        clearDashboard();
        updateRunningState(false);

        smartMeterPresetButton.setOnClickListener(v -> {
            registerEditText.setText(String.valueOf(DEFAULT_REGISTER));
            detailsTextView.setText(R.string.smart_meter_hint);
        });

        inverterPresetButton.setOnClickListener(v -> {
            registerEditText.setText(String.valueOf(32080));
            detailsTextView.setText(R.string.inverter_hint);
        });

        startButton.setOnClickListener(v -> startMonitoring());
        stopButton.setOnClickListener(v -> stopMonitoring(getString(R.string.status_stopped)));
        findHostsButton.setOnClickListener(v -> startDiscovery());
        toggleFullscreenButton.setOnClickListener(v -> toggleFullscreenMode());
    }

    @Override
    protected void onDestroy() {
        stopScheduler();
        super.onDestroy();
    }

    private void startMonitoring() {
        String host = trim(hostEditText.getText().toString());
        if (TextUtils.isEmpty(host)) {
            hostEditText.setError(getString(R.string.error_required));
            return;
        }

        int port;
        int unitId;
        int register;
        int intervalSeconds;
        try {
            port = parseInteger(portEditText, DEFAULT_PORT, 1, 65535, R.string.error_invalid_port);
            unitId = parseInteger(unitIdEditText, DEFAULT_UNIT_ID, 0, 247, R.string.error_invalid_unit_id);
            register = parseInteger(registerEditText, DEFAULT_REGISTER, 0, 65535, R.string.error_invalid_register);
            intervalSeconds = parseInteger(intervalEditText, DEFAULT_INTERVAL, 1, 3600, R.string.error_invalid_interval);
        } catch (RuntimeException e) {
            return;
        }

        boolean invertSign = invertSignCheckBox.isChecked();

        savePreferences(host, port, unitId, register, intervalSeconds, invertSign);
        stopScheduler();
        monitoringActive = true;
        updateRunningState(true);
        setStatusText(getString(R.string.status_connecting));

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(
                () -> pollRegister(host, port, unitId, register, invertSign),
                0,
                intervalSeconds,
                TimeUnit.SECONDS
        );
    }

    private void startDiscovery() {
        int port;
        try {
            port = parseInteger(portEditText, DEFAULT_PORT, 1, 65535, R.string.error_invalid_port);
        } catch (RuntimeException e) {
            return;
        }

        setStatusText(getString(R.string.status_searching));
        detailsTextView.setText(R.string.status_searching);
        findHostsButton.setEnabled(false);

        networkDiscovery.findModbusHosts(this, port, DISCOVERY_TIMEOUT_MS, new NetworkDiscovery.Callback() {
            @Override
            public void onResult(List<String> hosts) {
                runOnUiThread(() -> {
                    findHostsButton.setEnabled(true);
                    if (hosts == null || hosts.isEmpty()) {
                        setStatusText(getString(R.string.status_no_hosts_found, port));
                        detailsTextView.setText(R.string.details_placeholder);
                        return;
                    }
                    String joinedHosts = TextUtils.join(", ", hosts);
                    hostEditText.setText(hosts.get(0));
                    setStatusText(getString(R.string.status_hosts_found, hosts.get(0)));
                    detailsTextView.setText(getString(R.string.details_found_hosts, joinedHosts));
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    findHostsButton.setEnabled(true);
                    setStatusText(getString(R.string.status_error_pattern, message));
                });
            }
        });
    }

    private void stopMonitoring(String status) {
        monitoringActive = false;
        stopScheduler();
        updateRunningState(false);
        setStatusText(status);
    }

    private void stopScheduler() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void pollRegister(String host, int port, int unitId, int register, boolean invertSign) {
        try {
            int[] smartMeterRegisters = modbusTcpClient.readHoldingRegisters(
                    host,
                    port,
                    unitId,
                    SMART_METER_BLOCK_START,
                    SMART_METER_BLOCK_LENGTH,
                    MODBUS_TIMEOUT_MS
            );
            SmartMeterSnapshot snapshot = SmartMeterSnapshot.fromRegisterBlock(SMART_METER_BLOCK_START, smartMeterRegisters);
            int rawWatts = snapshot.activePowerWatts;
            if (register != SMART_METER_BLOCK_START + (32278 - SMART_METER_BLOCK_START)) {
                try {
                    rawWatts = modbusTcpClient.readSignedInt32(host, port, unitId, register, MODBUS_TIMEOUT_MS);
                } catch (Exception ignored) {
                    // Falls ein benutzerdefiniertes Zusatzregister nicht lesbar ist, bleiben die Smart-Meter-Werte sichtbar.
                }
            }
            final int watts = invertSign ? (rawWatts * -1) : rawWatts;
            final SmartMeterSnapshot uiSnapshot = snapshot;
            String details = getString(R.string.details_pattern, register, unitId, host, port);
            runOnUiThread(() -> {
                if (!monitoringActive || isFinishing()) {
                    return;
                }
                updateDisplay(watts, getString(R.string.status_connected), details);
                updateDashboard(uiSnapshot, invertSign);
                lastUpdateTextView.setText(getString(R.string.last_update_pattern, getNowText()));
            });
        } catch (Exception e) {
            String message = e.getMessage();
            if (TextUtils.isEmpty(message)) {
                message = e.getClass().getSimpleName();
            }
            String status = getString(R.string.status_error_pattern, message);
            runOnUiThread(() -> {
                if (!isFinishing()) {
                    setStatusText(status);
                    clearDashboard();
                }
            });
        }
    }

    private void updateDisplay(int watts, String status, String details) {
        int absoluteWatts = Math.abs(watts);
        String formattedPower = String.format(Locale.GERMANY, "%,d W", absoluteWatts);

        if (watts > 0) {
            headlineTextView.setText(R.string.feed_in_title);
            powerTextView.setText(formattedPower);
            powerTextView.setTextColor(Color.parseColor("#157347"));
        } else if (watts < 0) {
            headlineTextView.setText(R.string.grid_import_title);
            powerTextView.setText(formattedPower);
            powerTextView.setTextColor(Color.parseColor("#B02A37"));
        } else {
            headlineTextView.setText(R.string.neutral_title);
            powerTextView.setText(R.string.zero_power);
            powerTextView.setTextColor(Color.parseColor("#0F172A"));
        }

        detailsTextView.setText(details);
        setStatusText(status);
    }

    private void setStatusText(String status) {
        statusTextView.setText(getString(R.string.status_label_pattern, status));
    }

    private void updateDashboard(SmartMeterSnapshot snapshot, boolean invertSign) {
        if (isSnapshotAllZero(snapshot)) {
            setStatusText(getString(R.string.status_zero_data));
            detailsTextView.setText(R.string.details_zero_data);
        }
        phaseVoltagesTextView.setText(getString(
                R.string.dashboard_voltages_pattern,
                formatDeci(snapshot.voltageL1DeciVolts),
                formatDeci(snapshot.voltageL2DeciVolts),
                formatDeci(snapshot.voltageL3DeciVolts)
        ));
        phaseCurrentsTextView.setText(getString(
                R.string.dashboard_currents_pattern,
                formatMilli(snapshot.currentL1MilliAmps),
                formatMilli(snapshot.currentL2MilliAmps),
                formatMilli(snapshot.currentL3MilliAmps)
        ));

        int activePower = invertSign ? (snapshot.activePowerWatts * -1) : snapshot.activePowerWatts;
        int reactivePower = invertSign ? (snapshot.reactivePowerVar * -1) : snapshot.reactivePowerVar;
        int apparentPower = Math.abs(snapshot.apparentPowerVa);
        double powerFactor = snapshot.powerFactorMilli / 1000.0d;

        powerBreakdownTextView.setText(getString(
                R.string.dashboard_power_pattern,
                formatInt(activePower),
                formatInt(reactivePower),
                formatInt(apparentPower),
                formatDecimal(powerFactor)
        ));
        energyCountersTextView.setText(getString(
                R.string.dashboard_energy_pattern,
                formatHundredths(snapshot.importEnergyHundredthsKwh),
                formatHundredths(snapshot.exportEnergyHundredthsKwh)
        ));
    }

    private void clearDashboard() {
        phaseVoltagesTextView.setText(R.string.dashboard_voltages_placeholder);
        phaseCurrentsTextView.setText(R.string.dashboard_currents_placeholder);
        powerBreakdownTextView.setText(R.string.dashboard_power_placeholder);
        energyCountersTextView.setText(R.string.dashboard_energy_placeholder);
    }

    private boolean isSnapshotAllZero(SmartMeterSnapshot snapshot) {
        return snapshot.activePowerWatts == 0
                && snapshot.reactivePowerVar == 0
                && snapshot.apparentPowerVa == 0
                && snapshot.powerFactorMilli == 0
                && snapshot.voltageL1DeciVolts == 0
                && snapshot.voltageL2DeciVolts == 0
                && snapshot.voltageL3DeciVolts == 0
                && snapshot.currentL1MilliAmps == 0
                && snapshot.currentL2MilliAmps == 0
                && snapshot.currentL3MilliAmps == 0
                && snapshot.importEnergyHundredthsKwh == 0
                && snapshot.exportEnergyHundredthsKwh == 0;
    }

    private void updateRunningState(boolean running) {
        startButton.setEnabled(!running);
        stopButton.setEnabled(running);
        hostEditText.setEnabled(!running);
        portEditText.setEnabled(!running);
        unitIdEditText.setEnabled(!running);
        registerEditText.setEnabled(!running);
        intervalEditText.setEnabled(!running);
        invertSignCheckBox.setEnabled(!running);
        smartMeterPresetButton.setEnabled(!running);
        inverterPresetButton.setEnabled(!running);
        findHostsButton.setEnabled(!running);
    }

    private void loadPreferences() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        hostEditText.setText(preferences.getString(PREF_HOST, ""));
        portEditText.setText(String.valueOf(preferences.getInt(PREF_PORT, DEFAULT_PORT)));
        unitIdEditText.setText(String.valueOf(preferences.getInt(PREF_UNIT_ID, DEFAULT_UNIT_ID)));
        registerEditText.setText(String.valueOf(preferences.getInt(PREF_REGISTER, DEFAULT_REGISTER)));
        intervalEditText.setText(String.valueOf(preferences.getInt(PREF_INTERVAL, DEFAULT_INTERVAL)));
        invertSignCheckBox.setChecked(preferences.getBoolean(PREF_INVERT_SIGN, false));
    }

    private void savePreferences(String host, int port, int unitId, int register, int intervalSeconds, boolean invertSign) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        preferences.edit()
                .putString(PREF_HOST, host)
                .putInt(PREF_PORT, port)
                .putInt(PREF_UNIT_ID, unitId)
                .putInt(PREF_REGISTER, register)
                .putInt(PREF_INTERVAL, intervalSeconds)
                .putBoolean(PREF_INVERT_SIGN, invertSign)
                .apply();
    }

    private int parseInteger(EditText editText, int fallback, int min, int max, int errorRes) {
        String value = trim(editText.getText().toString());
        if (value.isEmpty()) {
            editText.setText(String.valueOf(fallback));
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < min || parsed > max) {
                editText.setError(getString(errorRes));
                throw new IllegalArgumentException("Ungueltiger Bereich");
            }
            return parsed;
        } catch (NumberFormatException e) {
            editText.setError(getString(errorRes));
            throw e;
        }
    }

    private String getNowText() {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, Locale.GERMANY).format(new Date());
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String formatDeci(int value) {
        return String.format(Locale.GERMANY, "%.1f", value / 10.0d);
    }

    private String formatMilli(int value) {
        return String.format(Locale.GERMANY, "%.3f", value / 1000.0d);
    }

    private String formatHundredths(long value) {
        return String.format(Locale.GERMANY, "%.2f", value / 100.0d);
    }

    private String formatDecimal(double value) {
        return String.format(Locale.GERMANY, "%.3f", value);
    }

    private String formatInt(int value) {
        return String.format(Locale.GERMANY, "%,d", value);
    }

    private void toggleFullscreenMode() {
        fullscreenMode = !fullscreenMode;
        settingsCardView.setVisibility(fullscreenMode ? View.GONE : View.VISIBLE);
        toggleFullscreenButton.setText(fullscreenMode ? R.string.exit_fullscreen_button : R.string.fullscreen_button);

        if (fullscreenMode) {
            powerCardView.post(() -> {
                powerTextView.setTextSize(64f);
                enterImmersiveMode();
            });
        } else {
            powerTextView.setTextSize(42f);
            exitImmersiveMode();
        }
    }

    private void enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }

    private void exitImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }
}
