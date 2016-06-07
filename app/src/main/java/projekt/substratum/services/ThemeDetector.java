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
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import projekt.substratum.InformationActivity;
import projekt.substratum.R;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class ThemeDetector extends Service {

    public static Runnable runnable = null;
    public Context context = this;
    public Handler handler = null;
    private String new_theme_name;
    private Boolean new_theme = false;

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
                handler.postDelayed(runnable, 2500);
            }
        };
        handler.postDelayed(runnable, 2500);
    }

    private class MainFunction extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPostExecute(String result) {
            try {
                int id = 1;
                int notification_priority = 2; // PRIORITY_MAX == 2

                ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo
                        (new_theme_name, 0);
                String packageTitle = getPackageManager().getApplicationLabel
                        (applicationInfo).toString();

                // Everything below will only run as long as the PackageManager changes

                if (new_theme) {
                    Intent notificationIntent = new Intent(ThemeDetector.this, InformationActivity
                            .class);
                    notificationIntent.putExtra("theme_name", packageTitle);
                    notificationIntent.putExtra("theme_pid", new_theme_name);
                    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    PendingIntent intent =
                            PendingIntent.getActivity(ThemeDetector.this, 0, notificationIntent,
                                    PendingIntent.FLAG_CANCEL_CURRENT);

                    // This is the time when the notification should be shown on the user's screen
                    NotificationManager mNotifyManager =
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder
                            (getApplicationContext());
                    mBuilder.getNotification().flags |= Notification.FLAG_AUTO_CANCEL;
                    mBuilder.setContentTitle(packageTitle + " " + getString(R.string
                            .notification_theme_installed))
                            .setContentIntent(intent)
                            .setContentText(getString(R.string
                                    .notification_theme_installed_content))

                            .setAutoCancel(true)
                            .setSmallIcon(R.drawable.notification_icon)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap
                                    .main_launcher))
                            .setPriority(notification_priority);
                    mNotifyManager.notify(id, mBuilder.build());

                    new_theme = false;

                    super.onPostExecute(result);
                }
            } catch (PackageManager.NameNotFoundException nnfe) {
                // We will automatically assume that the service ran and there are no new themes
            }
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
                        if (appInfo.metaData.getString("Layers_Name") != null) {
                            if (appInfo.metaData.getString("Layers_Developer") != null) {
                                if (appInfo.metaData.getString("Substratum_Enabled") != null) {
                                    installed.add(packageInfo.packageName);
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