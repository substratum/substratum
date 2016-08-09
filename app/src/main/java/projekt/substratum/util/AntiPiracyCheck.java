package projekt.substratum.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import projekt.substratum.config.References;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class AntiPiracyCheck {

    private Context mContext;
    private List<String> installed_themes;
    private ArrayList<String> unauthorized_packages;
    private SharedPreferences prefs;

    public void AntiPiracyCheck(Context context) {
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

        private String getDeviceIMEI() {
            TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context
                    .TELEPHONY_SERVICE);
            return telephonyManager.getDeviceId();
        }

        private boolean findOverlayParent(Context context, String theme_parent) {
            try {
                PackageManager packageManager = mContext.getPackageManager();
                List<ApplicationInfo> pm = packageManager.getInstalledApplications(PackageManager
                        .GET_META_DATA);
                for (int i = 0; i < pm.size(); i++) {
                    ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                            pm.get(i).packageName, PackageManager.GET_META_DATA);
                    if (appInfo.metaData != null) {
                        if (appInfo.metaData.getString(References.metadataName) != null) {
                            String parse1_themeName = appInfo.metaData.getString(References
                                    .metadataName)
                                    .replaceAll("\\s+", "");
                            String parse2_themeName = parse1_themeName.replaceAll
                                    ("[^a-zA-Z0-9]+", "");
                            if (parse2_themeName.equals(theme_parent)) {
                                return true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Exception
            }
            return false;
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
                if (appInfo.metaData != null && appInfo.metaData.getString("Substratum_ID") != null
                        && appInfo.metaData.getString("Substratum_ID")
                        .equals(Settings.Secure.getString(context.getContentResolver(),
                                Settings.Secure.ANDROID_ID))) {
                    if (appInfo.metaData.getString("Substratum_IMEI") != null
                            && appInfo.metaData.getString("Substratum_IMEI").equals("!" +
                            getDeviceIMEI())) {
                        if (appInfo.metaData.getString("Substratum_Parent") != null
                                && !findOverlayParent(context,
                                appInfo.metaData.getString("Substratum_Parent"))) {
                            Log.d("OverlayVerification", package_name + " " +
                                    "unauthorized to be used on this device.");
                            unauthorized_packages.add(package_name);
                        }
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
                    if (References.checkOMS()) {
                        if (References.isPackageInstalled(mContext, "masquerade.substratum")) {
                            Intent runCommand = new Intent();
                            runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                            runCommand.setAction("masquerade.substratum.COMMANDS");
                            runCommand.putStringArrayListExtra("pm-uninstall-specific",
                                    final_commands_array);
                            mContext.sendBroadcast(runCommand);
                        } else {
                            for (int i = 0; i < unauthorized_packages.size(); i++) {
                                Root.runCommand("pm uninstall " + unauthorized_packages.get(i));
                            }
                        }
                    } else {
                        Root.runCommand("mount -o rw,remount /system");
                        for (int i = 0; i < final_commands_array.size(); i++) {
                            Root.runCommand("rm -r " +
                                    References.getInstalledDirectory(mContext,
                                            final_commands_array.get(i)));
                        }
                        Root.runCommand("mount -o ro,remount /system");
                    }
                }
                return null;
            }
        }
    }
}