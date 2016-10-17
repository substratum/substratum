package projekt.substratum.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import projekt.substratum.R;
import projekt.substratum.config.References;
import projekt.substratum.services.NotificationUpgradeReceiver;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class CacheCreator {

    // Extract the files from the assets folder of the target theme

    private Context mContext;
    private int id = References.notification_id_upgrade;
    private NotificationManager mNotifyManager;

    public boolean initializeCache(Context context, String package_identifier) {
        mContext = context;

        try {
            return unzip(package_identifier);
        } catch (IOException ioe) {
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
                if (appInfo.metaData.getString(References.metadataName) != null) {
                    if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                        return appInfo.metaData.getString(References.metadataName);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("SubstratumLogger", "Unable to find package identifier (INDEX OUT OF BOUNDS)");
        }
        return null;
    }

    private String getThemeVersion(String package_name) {
        try {
            PackageInfo pinfo = mContext.getPackageManager().getPackageInfo(package_name, 0);
            return pinfo.versionName;
        } catch (Exception e) {
            // Exception
        }
        return null;
    }

    private int checkCurrentThemeSelectionLocation(String packageName) {
        try {
            mContext.getPackageManager().getApplicationInfo(packageName, 0);
            File directory1 = new File("/data/app/" + packageName + "-1/base.apk");
            if (directory1.exists()) {
                return 1;
            } else {
                File directory2 = new File("/data/app/" + packageName + "-2/base.apk");
                if (directory2.exists()) {
                    return 2;
                } else {
                    return 0;
                }
            }
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean wipeCache(Context context, String package_identifier) {
        mContext = context;
        File myDir2 = new File(mContext.getCacheDir().getAbsoluteFile() +
                "/SubstratumBuilder/" + package_identifier);
        if (myDir2.exists()) {
            Root.runCommand("rm -r " + myDir2.getAbsolutePath());
            return myDir2.mkdir();
        }
        return false;
    }

    private boolean unzip(String package_identifier) throws IOException {
        // First, extract the APK as a zip so we don't have to access the APK multiple times

        int folder_abbreviation = checkCurrentThemeSelectionLocation(package_identifier);
        if (folder_abbreviation != 0) {
            String source = "/data/app/" + package_identifier + "-" +
                    folder_abbreviation + "/base.apk";
            File myDir = new File(mContext.getCacheDir(), "SubstratumBuilder");
            if (!myDir.exists()) {
                boolean created = myDir.mkdir();
            }
            File myDir2 = new File(mContext.getCacheDir().getAbsoluteFile() +
                    "/SubstratumBuilder/" + package_identifier);
            if (!myDir2.exists()) {
                boolean created = myDir2.mkdir();
            } else {
                Root.runCommand("rm -r " + myDir2.getAbsolutePath());
                boolean created = myDir2.mkdir();
            }
            String destination = mContext.getCacheDir().getAbsolutePath() + "/SubstratumBuilder/"
                    + package_identifier;

            // Initialize Notification

            int notification_priority = 2; // PRIORITY_MAX == 2

            // Create an Intent for the BroadcastReceiver
            Intent buttonIntent = new Intent(mContext, NotificationUpgradeReceiver.class);

            // Create the PendingIntent
            PendingIntent btPendingIntent = PendingIntent.getBroadcast(
                    mContext, 0, buttonIntent, 0);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(
                    mContext, 0, new Intent(), 0);

            // Now count the amount of files needed to be extracted
            int files = 0;
            try {
                Context otherContext = mContext.createPackageContext(package_identifier, 0);
                try (ZipFile zipFile = new ZipFile(otherContext.getPackageCodePath())) {
                    Enumeration zipEntries = zipFile.entries();
                    while (zipEntries.hasMoreElements()) {
                        String fileName = ((ZipEntry) zipEntries.nextElement()).getName();
                        files += 1;
                    }
                }
                Log.d("SubstratumCacher", "Extracting \"" + package_identifier +
                        "\" with " + files + " files...");
            } catch (Exception e) {
                Log.e("SubstratumCacher",
                        "CacheCreator was not able to extract this theme's assets.");
            }

            mNotifyManager =
                    (NotificationManager) mContext.getSystemService(
                            Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext);

            // Buffer proper English and parse it (no 's after a name that ends with s)
            String theme = getThemeName(package_identifier);
            if (theme.substring(theme.length() - 1).equals("s")) {
                theme = theme + "\'";
            } else {
                theme = theme + "\'s";
            }
            String theme_name = String.format(
                    mContext.getString(R.string.notification_initial_title_upgrade), theme);

            mBuilder.setContentTitle(theme_name)
                    .setProgress(files, 0, true)
                    .addAction(android.R.color.transparent, mContext.getString(R.string
                            .notification_hide_upgrade), btPendingIntent)
                    .setSmallIcon(android.R.drawable.ic_popup_sync)
                    .setPriority(notification_priority)
                    .setContentIntent(resultPendingIntent)
                    .setOngoing(true);
            mNotifyManager.notify(id, mBuilder.build());

            try (ZipInputStream inputStream = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(source)))) {
                ZipEntry zipEntry;
                int count;
                byte[] buffer = new byte[65536];
                int file_count = 0;
                while ((zipEntry = inputStream.getNextEntry()) != null &&
                        checkNotificationVisibility()) {
                    File file = new File(destination, zipEntry.getName());
                    File dir = zipEntry.isDirectory() ? file : file.getParentFile();
                    if (!dir.isDirectory() && !dir.mkdirs())
                        throw new FileNotFoundException("Failed to ensure directory: " +
                                dir.getAbsolutePath());
                    if (zipEntry.isDirectory())
                        continue;
                    try (FileOutputStream outputStream = new FileOutputStream(file)) {
                        while ((count = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, count);
                        }
                    }
                    file_count += 1;
                    if (file_count % 50 == 0) {
                        mBuilder.setProgress(files, file_count, false);
                        mNotifyManager.notify(id, mBuilder.build());
                    }
                }
                if (checkNotificationVisibility()) {
                    createVersioningPlaceholderFile(package_identifier, package_identifier);
                    mNotifyManager.cancel(id);
                    Log.d("SubstratumCacher", "The theme's assets have been successfully " +
                            "expanded to the work area!");
                    return true;
                }
                return false;
            }
        } else {
            Log.e("SubstratumLogger",
                    "There is no valid package name under this abbreviated folder " +
                            "count.");
        }
        return false;
    }

    private boolean checkNotificationVisibility() {
        StatusBarNotification[] notifications = mNotifyManager.getActiveNotifications();
        for (StatusBarNotification notification : notifications) {
            if (notification.getId() == id) {
                return true;
            }
        }
        return false;
    }

    // Create a version.xml to take note of whether the cache needs to be rebuilt

    private void createVersioningPlaceholderFile(String package_identifier, String package_name) {
        File root = new File(mContext.getCacheDir().getAbsoluteFile() +
                "/SubstratumBuilder/" + package_name + "/substratum.xml");
        try {
            root.createNewFile();
            FileWriter fw = new FileWriter(root);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);
            String manifest = getThemeVersion(package_identifier);
            pw.write((manifest == null) ? "" : manifest);
            pw.close();
            bw.close();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
