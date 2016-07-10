package projekt.substratum.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import projekt.substratum.util.SubstratumThemeUpdater;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class ThemeCacher extends BroadcastReceiver {

    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.mContext = context;
        Uri packageName = intent.getData();

        if (checkSubstratumReady(packageName.toString().substring(8))) {
            new SubstratumThemeUpdater().initialize(context,
                    packageName.toString().substring(8), true);
        }
    }

    private boolean checkSubstratumReady(String package_name) {
        // Simulate the Layers Plugin feature by filtering all installed apps and their metadata
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (appInfo.metaData.getString("Substratum_Theme") != null) {
                    if (appInfo.metaData.getString("Substratum_Author") != null) {
                        Log.d("SubstratumCacher", "Re-caching assets from \"" +
                                package_name + "\"");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // Exception
        }
        return false;
    }
}