package tom.turmtechnik;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class IsOnline {
    Context myContext;

    public IsOnline(Context context) {
        myContext = context;
    }

    public boolean isOnlineOK() {
        // myWifiManager = (WifiManager)getBaseContext().getSystemService(Context.WIFI_SERVICE);

        ConnectivityManager cm =
                (ConnectivityManager) myContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }
}
