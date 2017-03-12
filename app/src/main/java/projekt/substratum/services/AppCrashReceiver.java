package projekt.substratum.services;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.config.References;
import projekt.substratum.config.ThemeManager;

import static android.content.Context.NOTIFICATION_SERVICE;

public class AppCrashReceiver extends BroadcastReceiver {

    public final static String APP_CRASH_LOG_TAG = "AppCrashReceiver";
    public final static int NOTIFICATION_ID = 2476;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!References.checkOMS(context)) {
            return;
        }

        String packageName =
                intent.getStringExtra("projekt.substratum.EXTRA_PACKAGE_NAME");
        String exceptionClass =
                intent.getStringExtra("projekt.substratum.EXTRA_EXCEPTION_CLASS_NAME");
        boolean repeating =
                intent.getBooleanExtra("projekt.substratum.EXTRA_CRASH_REPEATING", false);

        if (repeating) {
            Log.e(APP_CRASH_LOG_TAG, "\'" + packageName + "\' stopped unexpectedly...");
            Log.e(APP_CRASH_LOG_TAG, "Now disabling all overlays for \'" + packageName + "\'...");

            List<String> overlays = ThemeManager.listEnabledOverlaysForTarget(packageName);

            if (overlays.size() > 0) {
                try {
                    ApplicationInfo applicationInfo = context.getPackageManager()
                            .getApplicationInfo(packageName, 0);
                    packageName = context.getPackageManager()
                            .getApplicationLabel(applicationInfo).toString();
                } catch (PackageManager.NameNotFoundException e) {
                    // Suppress warning
                }
                String app_crash_title =
                        String.format(context.getString(R.string.app_crash_title), packageName);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                builder.setSmallIcon(R.drawable.notification_overlay_corruption);
                builder.setContentTitle(app_crash_title);
                builder.setContentText(context.getString(R.string.app_crash_content));
                builder.setOngoing(true);
                builder.setPriority(NotificationCompat.PRIORITY_MAX);
                builder.setCategory(NotificationCompat.CATEGORY_SERVICE);
                NotificationManager mNotifyMgr =
                        (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                mNotifyMgr.notify(NOTIFICATION_ID, builder.build());

                ThemeManager.disableOverlay(context, new ArrayList<>(overlays));
            }
        } else {
            Log.e(APP_CRASH_LOG_TAG, packageName + " stopped unexpectedly, not repeating...");
        }
    }
}