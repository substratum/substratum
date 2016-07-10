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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import projekt.substratum.R;

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
                        if (appInfo.metaData.getString("Substratum_Theme") != null) {
                            String parse1_themeName = appInfo.metaData.getString("Substratum_Theme")
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
                    if (appInfo.metaData.getString("Substratum_Theme") != null) {
                        if (appInfo.metaData.getString("Substratum_Author") != null) {
                            installed_themes.add(package_name);
                        }
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                // Exception
            }
        }

        private void checkOverlayIntegrity(Context context, String package_name) {
            // Check whether all overlay packages installed matches the current device's information
            try {
                ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                        package_name, PackageManager.GET_META_DATA);
                if (appInfo.metaData != null) {
                    if (appInfo.metaData.getString("Substratum_ID") != null) {
                        if (appInfo.metaData.getString("Substratum_ID").equals(Settings.
                                Secure.getString(context.getContentResolver(),
                                Settings.Secure.ANDROID_ID))) {
                            if (appInfo.metaData.getString("Substratum_IMEI") != null) {
                                if (appInfo.metaData.getString("Substratum_IMEI").equals("!" +
                                        getDeviceIMEI())) {
                                    if (appInfo.metaData.getString("Substratum_Parent") != null) {
                                        if (!findOverlayParent(context, appInfo.metaData.getString
                                                ("Substratum_Parent"))) {
                                            Log.d("OverlayVerification", package_name + " " +
                                                    "unauthorized to be used on this device.");
                                            unauthorized_packages.add(package_name);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Exception
            }
        }

        private boolean isPackageInstalled(String package_name) {
            PackageManager pm = mContext.getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(
                    PackageManager.GET_META_DATA);
            for (ApplicationInfo packageInfo : packages) {
                if (packageInfo.packageName.equals(package_name)) {
                    return true;
                }
            }
            return false;
        }

        private class PurgeUnauthorizedOverlays extends AsyncTask<String, Integer, String> {

            @Override
            protected void onPreExecute() {
                Log.d("SubstratumAntiPiracy", "The device has found unauthorized overlays created" +
                        " by " +
                        "another device.");
                Toast toast = Toast.makeText(mContext.getApplicationContext(),
                        mContext.getString(R.string
                                .antipiracy_toast),
                        Toast.LENGTH_LONG);
                toast.show();
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                Toast toast = Toast.makeText(mContext.getApplicationContext(),
                        mContext.getString(R.string
                                .antipiracy_toast_complete),
                        Toast.LENGTH_LONG);
                toast.show();
            }

            @Override
            protected String doInBackground(String... sUrl) {
                ArrayList<String> final_commands_array = new ArrayList<>();
                for (int i = 0; i < unauthorized_packages.size(); i++) {
                    final_commands_array.add(unauthorized_packages.get(i));
                }
                if (isPackageInstalled("projekt.substratum.helper")) {
                    Intent runCommand = new Intent();
                    runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    runCommand.setAction("projekt.substratum.helper.COMMANDS");
                    runCommand.putStringArrayListExtra("pm-uninstall-specific",
                            final_commands_array);
                    mContext.sendBroadcast(runCommand);
                } else {
                    for (int i = 0; i < unauthorized_packages.size(); i++) {
                        Root.runCommand("pm uninstall " + unauthorized_packages.get(i));
                    }
                }
                return null;
            }
        }
    }
}