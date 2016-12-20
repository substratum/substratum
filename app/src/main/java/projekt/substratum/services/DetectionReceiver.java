package projekt.substratum.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import projekt.substratum.R;
import projekt.substratum.config.References;
import projekt.substratum.util.SubstratumThemeUpdater;

public class DetectionReceiver extends BroadcastReceiver {

    private Context mContext;
    private String new_theme_name;
    private Boolean new_theme = false;
    private Boolean new_setup = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.mContext = context;
        Uri packageName = intent.getData();
        String package_name = packageName.toString().substring(8);
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (!References.checkOMS(mContext)) {
                    if (appInfo.metaData.getBoolean(References.metadataLegacy, false)) {
                        if (appInfo.metaData.getString(References.metadataName) != null) {
                            if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                                Log.d("SubstratumDetector", "Substratum is now initializing: " +
                                        package_name);
                                MainFunction mainFunction = new MainFunction();
                                mainFunction.execute("");
                            }
                        }
                    }
                } else {
                    if (appInfo.metaData.getString(References.metadataName) != null) {
                        if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                            Log.d("SubstratumDetector", "Substratum is now initializing: " +
                                    package_name);
                            MainFunction mainFunction = new MainFunction();
                            mainFunction.execute("");
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Exception
        }
    }

    private class MainFunction extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPostExecute(String result) {
            // Everything below will only run as long as the PackageManager changes
            if (new_theme && !new_setup) {
                new SubstratumThemeUpdater().initialize(mContext, new_theme_name, false);
                new_theme = false;
            } else {
                new_theme = false;
                new_setup = false;
            }
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            PackageManager packageManager = mContext.getPackageManager();
            List<ApplicationInfo> list = packageManager.getInstalledApplications(PackageManager
                    .GET_META_DATA);
            List<String> installed = new ArrayList<>();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

            for (ApplicationInfo packageInfo : list) {
                try {
                    ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                            packageInfo.packageName, PackageManager.GET_META_DATA);
                    if (appInfo.metaData != null) {
                        if (References.checkOMS(mContext)) {
                            if (appInfo.metaData.getString(References.metadataName) != null) {
                                if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                                    installed.add(packageInfo.packageName);
                                }
                            }
                        } else {
                            if (appInfo.metaData.getString(References.metadataName) != null) {
                                if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                                    if (appInfo.metaData.getBoolean(References.metadataLegacy,
                                            false)) {
                                        installed.add(packageInfo.packageName);
                                    } else {
                                        Log.e("SubstratumCacher", "Device is non-OMS, while an " +
                                                "OMS theme is installed, aborting operation!");

                                        Intent showIntent = new Intent();
                                        PendingIntent contentIntent = PendingIntent.getActivity(
                                                mContext, 0, showIntent, 0);

                                        String parse = String.format(mContext.getString(
                                                R.string.failed_to_install_text_notification),
                                                appInfo.metaData.getString(References
                                                        .metadataName));

                                        NotificationManager notificationManager =
                                                (NotificationManager) mContext.getSystemService(
                                                        Context.NOTIFICATION_SERVICE);
                                        NotificationCompat.Builder mBuilder =
                                                new NotificationCompat.Builder(mContext)
                                                        .setContentIntent(contentIntent)
                                                        .setAutoCancel(true)
                                                        .setSmallIcon(
                                                                R.drawable
                                                                        .notification_warning_icon)
                                                        .setContentTitle(mContext.getString(
                                                                R.string.failed_to_install_title_notification))
                                                        .setContentText(parse);
                                        Notification notification = mBuilder.build();
                                        notificationManager.notify(
                                                References.notification_id, notification);

                                        String final_commands = "pm uninstall " +
                                                packageInfo.packageName;

                                        if (References.isPackageInstalled(mContext,
                                                "masquerade.substratum")) {
                                            Intent runCommand = new Intent();
                                            runCommand.addFlags(Intent
                                                    .FLAG_INCLUDE_STOPPED_PACKAGES);
                                            runCommand.setAction("masquerade.substratum.COMMANDS");
                                            runCommand.putExtra("om-commands", final_commands);
                                            mContext.sendBroadcast(runCommand);
                                        } else {
                                            References.runCommands(final_commands);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(References.SUBSTRATUM_LOG, "Unable to find package identifier (INDEX " +
                            "OUT OF " +
                            "BOUNDS)");
                }
            }
            // Check for SharedPreferences Set and sort it
            if (!prefs.contains("installed_themes")) new_setup = true;
            Set<String> setString = prefs.getStringSet("installed_themes", new HashSet<>());
            Set<String> setStringSorted = new TreeSet<>();
            setStringSorted.addAll(setString);

            // Check for current installed set created just now and sort it
            Set<String> installed_set = new HashSet<>();
            installed_set.addAll(installed);
            Set<String> installed_setStringSorted = new TreeSet<>();
            installed_setStringSorted.addAll(installed_set);

            // Compare both lists and if they are different, then show a notification and add it
            // into the list
            if (!setStringSorted.equals(installed_setStringSorted) && !installed_setStringSorted
                    .equals(setStringSorted)) {
                for (int i = 0; i < installed_setStringSorted.size(); i++) {
                    if (!setStringSorted.contains(installed.get(i))) {
                        new_theme = true;
                        new_theme_name = installed.get(i);
                        i = installed_setStringSorted.size();
                        SharedPreferences.Editor edit = prefs.edit();
                        edit.putStringSet("installed_themes", installed_set);
                        edit.apply();
                    }
                }
            }
            return null;
        }
    }
}