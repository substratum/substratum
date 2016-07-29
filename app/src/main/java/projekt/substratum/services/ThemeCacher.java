package projekt.substratum.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import projekt.substratum.R;
import projekt.substratum.config.References;
import projekt.substratum.util.Root;
import projekt.substratum.util.SubstratumThemeUpdater;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class ThemeCacher extends BroadcastReceiver {

    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.mContext = context;
        Uri packageName = intent.getData();

        if (checkSubstratumReady(packageName.toString().substring(8))) {
            new SubstratumThemeUpdater().initialize(context,
                    packageName.toString().substring(8), true);
        }
    }

    private boolean checkSubstratumReady(String package_name) {
        // Simulate the Layers Plugin feature by filtering all installed apps and their metadata
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (!References.checkOMS()) {
                    if (appInfo.metaData.getString(References.metadataName) != null) {
                        if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                            if (appInfo.metaData.getBoolean(References.metadataLegacy, false)) {
                                Log.d("SubstratumCacher", "Re-caching assets from \"" +
                                        package_name + "\"");
                                return true;
                            } else {
                                Log.e("SubstratumCacher", "Device is non-OMS, while an OMS theme " +
                                        "is installed, aborting operation!");

                                Intent showIntent = new Intent();
                                PendingIntent contentIntent = PendingIntent.getActivity(
                                        mContext, 0, showIntent, 0);

                                String parse = String.format(mContext.getString(
                                        R.string.failed_to_install_text_notification),
                                        appInfo.metaData.getString(References.metadataName));

                                NotificationManager notificationManager =
                                        (NotificationManager) mContext.getSystemService(
                                                Context.NOTIFICATION_SERVICE);
                                NotificationCompat.Builder mBuilder =
                                        new NotificationCompat.Builder(mContext)
                                                .setContentIntent(contentIntent)
                                                .setAutoCancel(true)
                                                .setSmallIcon(R.drawable.notification_warning_icon)
                                                .setContentTitle(mContext.getString(
                                                        R.string.failed_to_install_title_notification))
                                                .setContentText(parse);
                                Notification notification = mBuilder.build();
                                notificationManager.notify(
                                        References.notification_id, notification);

                                String final_commands = "pm uninstall " + package_name;

                                if (References.isPackageInstalled(mContext,
                                        "masquerade.substratum")) {
                                    Intent runCommand = new Intent();
                                    runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                                    runCommand.setAction("masquerade.substratum.COMMANDS");
                                    runCommand.putExtra("om-commands", final_commands);
                                    mContext.sendBroadcast(runCommand);
                                } else {
                                    Root.runCommand(final_commands);
                                }

                                return false;
                            }
                        }
                    }
                } else {
                    if (appInfo.metaData.getString(References.metadataName) != null) {
                        if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                            Log.d("SubstratumCacher", "Re-caching assets from \"" +
                                    package_name + "\"");
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
}