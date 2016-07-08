package projekt.substratum.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.util.concurrent.ThreadLocalRandom;

import projekt.substratum.R;
import projekt.substratum.util.CacheCreator;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class ThemeCacher extends BroadcastReceiver {

    private Context mContext;
    private String packageName;
    private SharedPreferences prefs;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.mContext = context;
        Uri packageName = intent.getData();
        this.packageName = packageName.toString().substring(8);

        if (checkSubstratumReady(this.packageName)) {
            prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            prefs.edit().putBoolean("is_updating", true).apply();
            new SubstratumThemeUpdate().execute("");
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

    private String getThemeName(String package_name) {
        // Simulate the Layers Plugin feature by filtering all installed apps and their metadata
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (appInfo.metaData.getString("Substratum_Theme") != null) {
                    if (appInfo.metaData.getString("Substratum_Author") != null) {
                        return appInfo.metaData.getString("Substratum_Theme");
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("SubstratumLogger", "Unable to find package identifier (INDEX OUT OF BOUNDS)");
        }
        return null;
    }

    private class SubstratumThemeUpdate extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPostExecute(String result) {

            final int id = ThreadLocalRandom.current().nextInt(0, 1000);
            final int notification_priority = 2; // PRIORITY_MAX == 2

            // This is the time when the notification should be shown on the user's screen
            NotificationManager mNotifyManager =
                    (NotificationManager) mContext.getSystemService(
                            Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder
                    (mContext);
            mBuilder.getNotification().flags |= Notification.FLAG_AUTO_CANCEL;
            mBuilder.setContentTitle(getThemeName(packageName) + " " + mContext.getString(
                    R.string.notification_theme_updated))
                    .setContentText(mContext.getString(R.string
                            .notification_theme_updated_content))
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.notification_updated)
                    .setLargeIcon(BitmapFactory.decodeResource(
                            mContext.getResources(), R.mipmap
                                    .restore_launcher))
                    .setPriority(notification_priority);
            mNotifyManager.notify(id, mBuilder.build());

            prefs.edit().putBoolean("is_updating", false).apply();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            new CacheCreator().initializeCache(mContext, packageName);
            return null;
        }
    }
}