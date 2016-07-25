package projekt.substratum.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import projekt.substratum.R;
import projekt.substratum.config.References;
import projekt.substratum.util.AntiPiracyCheck;
import projekt.substratum.util.Root;
import projekt.substratum.util.SubstratumThemeUpdater;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class ThemeDetector extends Service {

    private static Runnable runnable = null;
    private static Runnable runnable2 = null;
    private Context context = this;
    private Handler handler = null;
    private Handler handler2 = null;
    private String new_theme_name;
    private Boolean new_theme = false;
    private int CONFIG_TIME_PIRACY_CHECKER = 60000; // 1 sec == 1000ms
    private int CONFIG_TIME_THEME_CHECKER = 2500; // 1 sec == 1000ms
    private Boolean new_setup = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        handler = new Handler();
        runnable = new Runnable() {
            public void run() {
                MainFunction mainFunction = new MainFunction();
                mainFunction.execute("");
                handler.postDelayed(runnable, CONFIG_TIME_THEME_CHECKER);
            }
        };
        handler.postDelayed(runnable, CONFIG_TIME_THEME_CHECKER);
        handler2 = new Handler();
        runnable2 = new Runnable() {
            public void run() {
                new AntiPiracyCheck().AntiPiracyCheck(context);
                handler2.postDelayed(runnable2, CONFIG_TIME_PIRACY_CHECKER);
            }
        };
        handler2.postDelayed(runnable2, CONFIG_TIME_PIRACY_CHECKER);
    }

    private class MainFunction extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPostExecute(String result) {
            // Everything below will only run as long as the PackageManager changes
            if (new_theme && !new_setup) {
                new SubstratumThemeUpdater().initialize(
                        getApplicationContext(), new_theme_name, false);
                new_theme = false;
            } else {
                new_theme = false;
                new_setup = false;
            }
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            PackageManager packageManager = getPackageManager();
            List<ApplicationInfo> list = packageManager.getInstalledApplications(PackageManager
                    .GET_META_DATA);
            List<String> installed = new ArrayList<>();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences
                    (getApplicationContext());

            for (ApplicationInfo packageInfo : list) {
                try {
                    ApplicationInfo appInfo = getPackageManager().getApplicationInfo(
                            packageInfo.packageName, PackageManager.GET_META_DATA);
                    if (appInfo.metaData != null) {
                        if (References.checkOMS()) {
                            if (appInfo.metaData.getString("Substratum_Theme") != null) {
                                if (appInfo.metaData.getString("Substratum_Author") != null) {
                                    installed.add(packageInfo.packageName);
                                }
                            }
                        } else {
                            if (appInfo.metaData.getString("Substratum_Theme") != null) {
                                if (appInfo.metaData.getString("Substratum_Author") != null) {
                                    if (appInfo.metaData.getBoolean("Substratum_Legacy", false)) {
                                        installed.add(packageInfo.packageName);
                                    } else {
                                        Log.d("SubstratumCacher", "Device is non-OMS, while an " +
                                                "OMS theme " +
                                                "is installed, aborting operation!");

                                        Intent showIntent = new Intent();
                                        PendingIntent contentIntent = PendingIntent.getActivity(
                                                context, 0, showIntent, 0);

                                        String parse = String.format(context.getString(
                                                R.string.failed_to_install_text_notification),
                                                appInfo.metaData.getString("Substratum_Theme"));

                                        NotificationManager notificationManager =
                                                (NotificationManager) context.getSystemService(
                                                        Context.NOTIFICATION_SERVICE);
                                        NotificationCompat.Builder mBuilder =
                                                new NotificationCompat.Builder(context)
                                                        .setContentIntent(contentIntent)
                                                        .setAutoCancel(true)
                                                        .setSmallIcon(
                                                                R.drawable
                                                                        .notification_warning_icon)
                                                        .setContentTitle(context.getString(
                                                                R.string.failed_to_install_title_notification))
                                                        .setContentText(parse);
                                        Notification notification = mBuilder.build();
                                        notificationManager.notify(
                                                References.notification_id, notification);

                                        String final_commands = "pm uninstall " +
                                                packageInfo.packageName;

                                        if (References.isPackageInstalled(context,
                                                "masquerade.substratum")) {
                                            Intent runCommand = new Intent();
                                            runCommand.addFlags(Intent
                                                    .FLAG_INCLUDE_STOPPED_PACKAGES);
                                            runCommand.setAction("masquerade.substratum.COMMANDS");
                                            runCommand.putExtra("om-commands", final_commands);
                                            context.sendBroadcast(runCommand);
                                        } else {
                                            Root.runCommand(final_commands);
                                        }
                                    }
                                }
                            }
                        }

                    }

                } catch (PackageManager.NameNotFoundException e) {
                    Log.e("SubstratumLogger", "Unable to find package identifier (INDEX OUT OF " +
                            "BOUNDS)");
                }
            }
            // Check for SharedPreferences Set and sort it
            if (!prefs.contains("installed_themes")) new_setup = true;
            Set<String> setString = prefs.getStringSet("installed_themes", new HashSet<String>());
            Set<String> setStringSorted = new TreeSet<>();
            Iterator<String> it = setString.iterator();
            while (it.hasNext()) {
                setStringSorted.add(it.next());
            }

            // Check for current installed set created just now and sort it
            Set<String> installed_set = new HashSet<>();
            installed_set.addAll(installed);
            Set<String> installed_setStringSorted = new TreeSet<>();
            Iterator<String> it2 = installed_set.iterator();
            while (it2.hasNext()) {
                installed_setStringSorted.add(it2.next());
            }

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