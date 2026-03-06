package tom.turmtechnik;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ScreenReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context arg0, Intent arg1) {
        Log.e("ScreenReceiver", "aufgerufen");
        if (arg1.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            StaticVariable.screenIsOff = true;
            Log.e("ScreenReceiver", "true");
        }
        if (arg1.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            StaticVariable.screenIsOff = false;
            Log.e("ScreenReceiver", "false");
        }

    }

}
