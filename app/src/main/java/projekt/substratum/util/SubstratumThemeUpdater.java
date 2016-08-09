package projekt.substratum.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.ThreadLocalRandom;

import projekt.substratum.MainActivity;
import projekt.substratum.R;
import projekt.substratum.config.References;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class SubstratumThemeUpdater {

    private Context mContext;
    private SharedPreferences prefs;
    private String packageName;
    private boolean showNotification;

    public void initialize(Context context, String packageName, boolean notification) {
        this.mContext = context;
        this.packageName = packageName;
        this.showNotification = notification;

        prefs = context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("is_updating", true).apply();

        new SubstratumThemeUpdate().execute("");
    }

    private String getThemeName(String package_name) {
        // Simulate the Layers Plugin feature by filtering all installed apps and their metadata
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (References.checkOMS()) {
                    if (appInfo.metaData.getString(References.metadataName) != null) {
                        if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                            return appInfo.metaData.getString(References.metadataName);
                        }
                    }
                } else {
                    if (appInfo.metaData.getBoolean(References.metadataLegacy, false)) {
                        if (appInfo.metaData.getString(References.metadataName) != null) {
                            if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                                return appInfo.metaData.getString(References.metadataName);
                            }
                        }
                    }
                }

            }
        } catch (Exception e) {
            Log.e("SubstratumLogger", "Unable to find package identifier (INDEX OUT OF BOUNDS)");
        }
        return null;
    }

    private class SubstratumThemeUpdate extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPostExecute(String result) {

            final int id = ThreadLocalRandom.current().nextInt(0, 1000);
            final int notification_priority = 2; // PRIORITY_MAX == 2

            if (showNotification) {
                Intent notificationIntent;
                PendingIntent intent;
                try {
                    Intent myIntent = new Intent(Intent.ACTION_MAIN);
                    Context otherAppContext = mContext.createPackageContext(
                            packageName, Context.CONTEXT_IGNORE_SECURITY);
                    boolean is_valid = true;

                    // An easy way to check where the SubstratumLauncher class is located
                    String intenter = "";
                    String[] classes = References.getClassesOfPackage(otherAppContext);
                    for (int i = 0; i < classes.length; i++) {
                        if (classes[i].contains("SubstratumLauncher")) intenter = classes[i];
                        if (!References.letUsDance(classes[i],
                                packageName)) {
                            is_valid = false;
                            break;
                        }
                    }
                    if (intenter.length() == 0) {
                        intenter = "substratum.theme.template.SubstratumLauncher";
                    }

                    if (is_valid) {
                        if (!References.checkOMS()) {
                            notificationIntent = new Intent(mContext, MainActivity.class);
                            intent = PendingIntent.getActivity(mContext, 0, notificationIntent,
                                    PendingIntent.FLAG_CANCEL_CURRENT);
                        } else {
                            myIntent.setComponent(ComponentName.unflattenFromString(
                                    packageName + "/" + intenter));
                            myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            intent = PendingIntent.getActivity(mContext, 0, myIntent,
                                    PendingIntent.FLAG_CANCEL_CURRENT);
                        }
                    } else {
                        intent = PendingIntent.getActivity(mContext, 0, myIntent,
                                PendingIntent.FLAG_CANCEL_CURRENT);
                    }

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
                            .setContentIntent(intent)
                            .setSmallIcon(R.drawable.notification_updated)
                            .setLargeIcon(BitmapFactory.decodeResource(
                                    mContext.getResources(), R.mipmap
                                            .restore_launcher))
                            .setPriority(notification_priority);
                    mNotifyManager.notify(id, mBuilder.build());
                } catch (Exception ex) {
                    Toast toast = Toast.makeText(mContext,
                            mContext.getString(R.string
                                    .information_activity_upgrade_toast),
                            Toast.LENGTH_LONG);
                    toast.show();
                }
            } else {
                Intent notificationIntent;
                PendingIntent intent;
                try {
                    Intent myIntent = new Intent(Intent.ACTION_MAIN);
                    Context otherAppContext = mContext.createPackageContext(
                            packageName, Context.CONTEXT_IGNORE_SECURITY);
                    boolean is_valid = true;

                    // An easy way to check where the SubstratumLauncher class is located
                    String intenter = "";
                    String[] classes = References.getClassesOfPackage(otherAppContext);
                    for (int i = 0; i < classes.length; i++) {
                        if (classes[i].contains("SubstratumLauncher")) intenter = classes[i];
                        if (!References.letUsDance(classes[i],
                                packageName)) {
                            is_valid = false;
                            break;
                        }
                    }
                    if (intenter.length() == 0) {
                        intenter = "substratum.theme.template.SubstratumLauncher";
                    }

                    if (is_valid) {
                        if (!References.checkOMS()) {
                            notificationIntent = new Intent(mContext, MainActivity.class);
                            intent = PendingIntent.getActivity(mContext, 0, notificationIntent,
                                    PendingIntent.FLAG_CANCEL_CURRENT);
                        } else {
                            myIntent.setComponent(ComponentName.unflattenFromString(
                                    packageName + "/" + intenter));
                            myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            intent = PendingIntent.getActivity(mContext, 0, myIntent,
                                    PendingIntent.FLAG_CANCEL_CURRENT);
                        }
                    } else {
                        intent = PendingIntent.getActivity(mContext, 0, myIntent,
                                PendingIntent.FLAG_CANCEL_CURRENT);
                    }

                    // This is the time when the notification should be shown on the user's screen
                    NotificationManager mNotifyManager =
                            (NotificationManager) mContext.getSystemService(
                                    Context.NOTIFICATION_SERVICE);
                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder
                            (mContext);
                    mBuilder.getNotification().flags |= Notification.FLAG_AUTO_CANCEL;
                    mBuilder.setContentTitle(getThemeName(packageName) + " " + mContext.getString(
                            R.string.notification_theme_installed))
                            .setContentText(mContext.getString(R.string
                                    .notification_theme_installed_content))
                            .setAutoCancel(true)
                            .setContentIntent(intent)
                            .setSmallIcon(R.drawable.notification_icon)
                            .setLargeIcon(BitmapFactory.decodeResource(
                                    mContext.getResources(), R.mipmap
                                            .main_launcher))
                            .setPriority(notification_priority);
                    mNotifyManager.notify(id, mBuilder.build());
                } catch (Exception ex) {
                    Toast toast = Toast.makeText(mContext,
                            mContext.getString(R.string
                                    .information_activity_upgrade_toast),
                            Toast.LENGTH_LONG);
                    toast.show();
                }
            }
            prefs.edit().putBoolean("is_updating", false).apply();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            new CacheCreator().initializeCache(mContext, packageName);
            return null;
        }
    }
}