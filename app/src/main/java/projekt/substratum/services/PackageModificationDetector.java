package projekt.substratum.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import projekt.substratum.config.References;

public class PackageModificationDetector extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.PACKAGE_ADDED".equals(intent.getAction()) ||
                "android.intent.action.PACKAGE_REPLACED".equals(intent.getAction()) ||
                "android.intent.action.MY_PACKAGE_REPLACED".equals(intent.getAction())) {

            Uri packageName = intent.getData();
            String package_name = packageName.toString().substring(8);

            if (package_name.equals(References.lp_package_identifier)) {
                SharedPreferences prefs = context.getSharedPreferences(
                        "filter_state", Context.MODE_PRIVATE);
                prefs.edit().clear().apply();
                Log.d(References.SUBSTRATUM_LOG,
                        "The filter cache has been wiped in accordance to intent: " +
                                package_name + " [" + intent.getAction() + "]");
            } else {
                try {
                    ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                            package_name, PackageManager.GET_META_DATA);
                    if (appInfo.metaData != null) {
                        if (appInfo.metaData.getString(References.metadataName) != null) {
                            if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                                SharedPreferences prefs = context.getSharedPreferences(
                                        "filter_state", Context.MODE_PRIVATE);
                                prefs.edit().clear().apply();
                                Log.d(References.SUBSTRATUM_LOG,
                                        "The filter cache has been wiped in accordance to intent:" +
                                                " " + package_name + " [" +
                                                intent.getAction() + "]");
                            }
                        }
                    }
                } catch (Exception e) {
                    // Exception
                }
            }
        }
    }
}