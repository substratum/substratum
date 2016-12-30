package projekt.substratum.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import projekt.substratum.config.References;

public class AntiPiracyCheck {

    private Context mContext;
    private List<String> installed_themes;
    private ArrayList<String> unauthorized_packages;
    private SharedPreferences prefs;

    public void execute(Context context) {
        this.mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(
                mContext);
        new AntiPiracyChecker().execute("");
    }

    private class AntiPiracyChecker extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            installed_themes = new ArrayList<>();
            unauthorized_packages = new ArrayList<>();
        }

        @Override
        protected void onPostExecute(String result) {
            if (unauthorized_packages.size() > 0) {
                PurgeUnauthorizedOverlays purgeUnauthorizedOverlays = new
                        PurgeUnauthorizedOverlays();
                purgeUnauthorizedOverlays.execute("");
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            PackageManager packageManager = mContext.getPackageManager();
            List<ApplicationInfo> list = packageManager.getInstalledApplications(PackageManager
                    .GET_META_DATA);

            for (ApplicationInfo packageInfo : list) {
                getSubstratumPackages(mContext, packageInfo.packageName);
            }

            try {
                for (ApplicationInfo packageInfo : list) {
                    checkOverlayIntegrity(mContext, packageInfo.packageName);
                }
            } catch (Exception e) {
                // Exception
            }
            Set<String> installed = new HashSet<>();
            installed.addAll(installed_themes);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putStringSet("installed_themes", installed);
            edit.apply();
            return null;
        }

        private void getSubstratumPackages(Context context, String package_name) {
            // Simulate the Layers Plugin feature by filtering all installed apps and their metadata
            try {
                ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                        package_name, PackageManager.GET_META_DATA);
                if (appInfo.metaData != null) {
                    if (appInfo.metaData.getString(References.metadataName) != null) {
                        if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                            installed_themes.add(package_name);
                        }
                    }
                }
            } catch (Exception e) {
                // Exception
            }
        }

        private void checkOverlayIntegrity(Context context, String package_name) {
            // Check whether all overlay packages installed matches the current device's information
            try {
                ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                        package_name, PackageManager.GET_META_DATA);
                if (appInfo.metaData != null &&
                        appInfo.metaData.getString("Substratum_Device") != null) {
                    String deviceID = appInfo.metaData.getString("Substratum_Device");
                    String actualDeviceID = References.getDeviceID(context);
                    if (deviceID != null && deviceID.equals(actualDeviceID)) {
                        if (appInfo.metaData.getString("Substratum_Parent") != null
                                && !References.isPackageInstalled(context,
                                appInfo.metaData.getString("Substratum_Parent"))) {
                            Log.d("OverlayVerification", package_name + " " +
                                    "unauthorized to be used on this device.");
                            unauthorized_packages.add(package_name);
                        }
                    } else {
                        Log.d("OverlayVerification", package_name + " " +
                                "unauthorized to be used on this device.");
                        unauthorized_packages.add(package_name);
                    }
                }
            } catch (Exception e) {
                // Exception
            }
        }

        private class PurgeUnauthorizedOverlays extends AsyncTask<String, Integer, String> {

            @Override
            protected String doInBackground(String... sUrl) {
                ArrayList<String> final_commands_array = new ArrayList<>();
                if (unauthorized_packages.size() > 0) {
                    for (int i = 0; i < unauthorized_packages.size(); i++) {
                        final_commands_array.add(unauthorized_packages.get(i));
                    }
                    if (References.checkOMS(mContext)) {
                        if (References.isPackageInstalled(mContext, "masquerade.substratum")) {
                            Intent runCommand = new Intent();
                            runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                            runCommand.setAction("masquerade.substratum.COMMANDS");
                            runCommand.putStringArrayListExtra("pm-uninstall-specific",
                                    final_commands_array);
                            mContext.sendBroadcast(runCommand);
                        } else {
                            for (int i = 0; i < unauthorized_packages.size(); i++) {
                                References.uninstallOverlay(unauthorized_packages.get(i));
                            }
                        }
                    } else {
                        References.mountRW();
                        for (int i = 0; i < final_commands_array.size(); i++) {
                            References.delete(References.getInstalledDirectory(mContext,
                                    final_commands_array.get(i)));
                        }
                        References.mountRO();
                    }
                }
                return null;
            }
        }
    }
}