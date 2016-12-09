package projekt.substratum.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.config.References;

import static projekt.substratum.config.References.DEBUG;
import static projekt.substratum.config.References.FIRST_WINDOW_REFRESH_DELAY;
import static projekt.substratum.config.References.MAIN_WINDOW_REFRESH_DELAY;
import static projekt.substratum.config.References.SECOND_WINDOW_REFRESH_DELAY;

public class PackageModificationDetector extends BroadcastReceiver {

    private ArrayList<String> to_be_disabled;

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.PACKAGE_ADDED".equals(intent.getAction()) ||
                "android.intent.action.PACKAGE_REPLACED".equals(intent.getAction()) ||
                "android.intent.action.MY_PACKAGE_REPLACED".equals(intent.getAction())) {

            Uri packageName = intent.getData();
            String package_name = packageName.toString().substring(8);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (prefs.contains("installed_icon_pack")) {
                to_be_disabled = new ArrayList<>();
                if (prefs.getString("installed_icon_pack", null).equals(context.getPackageName())) {
                    List<ApplicationInfo> list = context.getPackageManager()
                            .getInstalledApplications(PackageManager.GET_META_DATA);
                    for (ApplicationInfo packageInfo : list) {
                        if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                            getSubstratumPackages(context, packageInfo.packageName);
                        }
                    }
                    String final_commands = References.disableOverlay();
                    for (int i = 0; i < to_be_disabled.size(); i++) {
                        final_commands = final_commands + " " + to_be_disabled.get(i);
                    }
                    if (References.isPackageInstalled(context, "masquerade.substratum")) {
                        if (DEBUG)
                            Log.e(References.SUBSTRATUM_LOG, "Initializing the Masquerade " +
                                    "theme provider...");
                        Intent runCommand = new Intent();
                        runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                        runCommand.setAction("masquerade.substratum.COMMANDS");
                        ArrayList<String> final_array = new ArrayList<>();
                        final_array.add(0, context.getString(R.string.studio_system)
                                .toLowerCase());
                        final_array.add(1, final_commands);
                        final_array.add(2, (final_commands.contains("projekt.substratum") ?
                                String.valueOf(MAIN_WINDOW_REFRESH_DELAY) : String.valueOf(0)));
                        final_array.add(3, String.valueOf(FIRST_WINDOW_REFRESH_DELAY));
                        final_array.add(4, String.valueOf(SECOND_WINDOW_REFRESH_DELAY));
                        final_array.add(5, null);
                        runCommand.putExtra("icon-handler", final_array);
                        context.sendBroadcast(runCommand);
                    }
                    prefs.edit().remove("installed_icon_pack").apply();
                }
            } else if (package_name.equals(References.lp_package_identifier)) {
                SharedPreferences prefsPrivate = context.getSharedPreferences(
                        "filter_state", Context.MODE_PRIVATE);
                prefsPrivate.edit().clear().apply();
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
                                SharedPreferences prefsPrivate = context.getSharedPreferences(
                                        "filter_state", Context.MODE_PRIVATE);
                                prefsPrivate.edit().clear().apply();
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

    private void getSubstratumPackages(Context context, String package_name) {
        // Simulate the Layers Plugin feature by filtering all installed apps and their metadata
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (appInfo.metaData.equals("Substratum_IconPack")) {
                    to_be_disabled.add(package_name);
                }
            }
        } catch (Exception e) {
            // Suppress warnings
        }
    }
}