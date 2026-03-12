package tom.turmtechnik;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

public final class KioskModeHelper {
    private static final String TAG = "KioskModeHelper";
    private static final String PREFS = "Turmtechnik";
    private static final String PREF_EXTERNAL_HOME_PACKAGE = "kiosk_external_home_package";
    private static final String PREF_EXTERNAL_HOME_CLASS = "kiosk_external_home_class";

    private KioskModeHelper() {
    }

    public static ComponentName getAdminComponent(Context context) {
        return new ComponentName(context, TurmtechnikDeviceAdminReceiver.class);
    }

    public static ComponentName getHomeAliasComponent(Context context) {
        return new ComponentName(context, "tom.turmtechnik.TurmtechnikHomeActivity");
    }

    public static boolean isDeviceOwner(Context context) {
        if (context == null) return false;
        try {
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            return dpm != null && dpm.isDeviceOwnerApp(context.getPackageName());
        } catch (Exception e) {
            Log.w(TAG, "isDeviceOwner: " + e.getMessage());
            return false;
        }
    }

    public static void applyTurmtechnikAsHome(Activity activity) {
        if (activity == null || !isDeviceOwner(activity)) return;
        DevicePolicyManager dpm = (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm == null) return;
        try {
            ComponentName admin = getAdminComponent(activity);
            IntentFilterBuilder.addPersistentHome(dpm, admin, getHomeAliasComponent(activity));
            dpm.setLockTaskPackages(admin, new String[]{activity.getPackageName()});
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    dpm.setStatusBarDisabled(admin, true);
                } catch (Exception ignored) {
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    dpm.setKeyguardDisabled(admin, true);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "applyTurmtechnikAsHome: " + e.getMessage());
        }
    }

    public static void startKioskIfPossible(Activity activity) {
        if (activity == null || !isDeviceOwner(activity)) return;
        applyTurmtechnikAsHome(activity);
        try {
            activity.startLockTask();
        } catch (Exception e) {
            Log.w(TAG, "startLockTask: " + e.getMessage());
        }
    }

    public static void releaseToExternalHome(Activity activity) {
        if (activity == null) return;
        try {
            ComponentName external = findExternalHome(activity);
            if (isDeviceOwner(activity) && external != null) {
                DevicePolicyManager dpm = (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
                if (dpm != null) {
                    ComponentName admin = getAdminComponent(activity);
                    rememberExternalHome(activity, external);
                    dpm.clearPackagePersistentPreferredActivities(admin, activity.getPackageName());
                    IntentFilterBuilder.addPersistentHome(dpm, admin, external);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            dpm.setStatusBarDisabled(admin, false);
                        } catch (Exception ignored) {
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            dpm.setKeyguardDisabled(admin, false);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "releaseToExternalHome prepare: " + e.getMessage());
        }

        try {
            activity.stopLockTask();
        } catch (Exception ignored) {
        }
    }

    public static ComponentName findExternalHome(Context context) {
        if (context == null) return null;
        String ownPackage = context.getPackageName();
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> homes = context.getPackageManager().queryIntentActivities(home, 0);
        if (homes == null) return null;
        for (ResolveInfo ri : homes) {
            if (ri == null || ri.activityInfo == null) continue;
            if (ownPackage.equals(ri.activityInfo.packageName)) continue;
            return new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
        }
        return getRememberedExternalHome(context);
    }

    private static void rememberExternalHome(Context context, ComponentName component) {
        if (context == null || component == null) return;
        context.getSharedPreferences(PREFS, 0)
                .edit()
                .putString(PREF_EXTERNAL_HOME_PACKAGE, component.getPackageName())
                .putString(PREF_EXTERNAL_HOME_CLASS, component.getClassName())
                .apply();
    }

    private static ComponentName getRememberedExternalHome(Context context) {
        if (context == null) return null;
        String pkg = context.getSharedPreferences(PREFS, 0).getString(PREF_EXTERNAL_HOME_PACKAGE, null);
        String cls = context.getSharedPreferences(PREFS, 0).getString(PREF_EXTERNAL_HOME_CLASS, null);
        if (TextUtils.isEmpty(pkg) || TextUtils.isEmpty(cls)) return null;
        return new ComponentName(pkg, cls);
    }

    private static final class IntentFilterBuilder {
        private IntentFilterBuilder() {
        }

        static void addPersistentHome(DevicePolicyManager dpm, ComponentName admin, ComponentName target) {
            android.content.IntentFilter filter = new android.content.IntentFilter(Intent.ACTION_MAIN);
            filter.addCategory(Intent.CATEGORY_HOME);
            filter.addCategory(Intent.CATEGORY_DEFAULT);
            dpm.addPersistentPreferredActivity(admin, filter, target);
        }
    }
}
