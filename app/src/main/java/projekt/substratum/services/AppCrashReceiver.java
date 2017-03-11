package projekt.substratum.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.config.References;
import projekt.substratum.config.ThemeManager;

public class AppCrashReceiver extends BroadcastReceiver {

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

        if ((exceptionClass.contains("NotFoundException") ||
                exceptionClass.contains("IllegalArgumentException")) && repeating) {
            List<String> overlays = ThemeManager.listEnabledOverlaysForTarget(packageName);

            if (overlays.size() > 0) {
                try {
                    ApplicationInfo applicationInfo = context.getPackageManager()
                            .getApplicationInfo(packageName, 0);
                    packageName = context.getPackageManager()
                            .getApplicationLabel(applicationInfo).toString();
                } catch (PackageManager.NameNotFoundException e) {
                    // exception
                }

                Toast.makeText(context, context.getString(R.string.app_crash_toast, packageName),
                        Toast.LENGTH_LONG)
                        .show();
                ThemeManager.disableOverlay(context, new ArrayList<>(overlays));
            }
        }
    }
}
