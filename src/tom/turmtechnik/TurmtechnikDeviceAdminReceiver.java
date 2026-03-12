package tom.turmtechnik;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class TurmtechnikDeviceAdminReceiver extends DeviceAdminReceiver {
    private static final String TAG = "TurmtechnikDeviceAdmin";

    @Override
    public void onEnabled(Context context, Intent intent) {
        Log.i(TAG, "Device-Admin aktiviert");
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        Log.i(TAG, "Device-Admin deaktiviert");
    }
}
