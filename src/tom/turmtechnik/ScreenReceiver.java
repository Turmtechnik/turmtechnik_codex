package tom.turmtechnik;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class ScreenReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context arg0, Intent arg1) {
        if (arg0 == null || arg1 == null || arg1.getAction() == null) {
            return;
        }

        String action = arg1.getAction();
        Log.e("ScreenReceiver", "aufgerufen: " + action);

        if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            StaticVariable.screenIsOff = true;
            Log.e("ScreenReceiver", "true");
            return;
        }

        if (Intent.ACTION_SCREEN_ON.equals(action)) {
            StaticVariable.screenIsOff = false;
            Log.e("ScreenReceiver", "false");
        }

        if (Intent.ACTION_SCREEN_ON.equals(action)
                || (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && Intent.ACTION_USER_PRESENT.equals(action))
                || Intent.ACTION_USER_PRESENT.equals(action)) {
            tryTriggerRecovery(arg0.getApplicationContext(), action);
        }
    }

    private void tryTriggerRecovery(Context context, String action) {
        if (context == null) return;
        if (TurmtechnikActivity.isInForeground) return;
        if (TurmtechnikActivity.getExitForSettingsUntilMillis(context) > System.currentTimeMillis()) return;
        if (TurmtechnikActivity.getBackgroundAllowedUntilMillis(context) > System.currentTimeMillis()) return;

        Boolean autostartFromDb = null;
        try {
            autostartFromDb = PlatinenDatabaseHelper.getInstance(context).getAutostartDerApp();
        } catch (Exception e) {
            Log.w("ScreenReceiver", "Autostart aus DB fehlgeschlagen bei " + action, e);
        }
        if (autostartFromDb != null) {
            StaticVariable.autostartDerApp = autostartFromDb;
        }
        if (!StaticVariable.autostartDerApp) {
            return;
        }

        try {
            Log.i("ScreenReceiver", "Recovery-Start nach " + action);
            StartTurmtechnikService.launchTurmtechnikActivityFromBackground(context);
        } catch (Exception e) {
            Log.w("ScreenReceiver", "Recovery-Start nach " + action + " fehlgeschlagen", e);
        }
    }

}
