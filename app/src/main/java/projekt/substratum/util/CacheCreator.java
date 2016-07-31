package projekt.substratum.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import projekt.substratum.config.References;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class CacheCreator {

    // Extract the files from the assets folder of the target theme

    private Context mContext;

    public void initializeCache(Context context, String package_identifier) {
        mContext = context;

        try {
            unzip(package_identifier);
            Log.d("SubstratumCacher", "The theme's assets have been successfully expanded to the" +
                    " work area!");
        } catch (IOException ioe) {
            // Exception
        }
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

    private void unzip(String package_identifier) throws IOException {
        // First, extract the APK as a zip so we don't have to access the APK multiple times

        int folder_abbreviation = checkCurrentThemeSelectionLocation(package_identifier);
        if (folder_abbreviation != 0) {
            String source = "/data/app/" + package_identifier + "-" +
                    folder_abbreviation + "/base.apk";
            File myDir = new File(mContext.getCacheDir(), "SubstratumBuilder");
            if (!myDir.exists()) {
                boolean created = myDir.mkdir();
            }
            String directory_name = getThemeName(package_identifier).replaceAll("\\s+", "")
                    .replaceAll("[^a-zA-Z0-9]+", "");
            File myDir2 = new File(mContext.getCacheDir().getAbsoluteFile() +
                    "/SubstratumBuilder/" + directory_name);
            if (!myDir2.exists()) {
                boolean created = myDir2.mkdir();
            } else {
                Root.runCommand("rm -r " + myDir2.getAbsolutePath());
                boolean created = myDir2.mkdir();
            }
            String destination = mContext.getCacheDir().getAbsolutePath() + "/SubstratumBuilder/"
                    + directory_name;


            try (ZipInputStream inputStream = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(source)))){
                ZipEntry zipEntry;
                int count;
                byte[] buffer = new byte[8192];
                while ((zipEntry = inputStream.getNextEntry()) != null) {
                    File file = new File(destination, zipEntry.getName());
                    File dir = zipEntry.isDirectory() ? file : file.getParentFile();
                    if (!dir.isDirectory() && !dir.mkdirs())
                        throw new FileNotFoundException("Failed to ensure directory: " +
                                dir.getAbsolutePath());
                    if (zipEntry.isDirectory())
                        continue;
                    try(FileOutputStream outputStream = new FileOutputStream(file)) {
                        while ((count = inputStream.read(buffer)) != -1)
                            outputStream.write(buffer, 0, count);
                    }
                }
                createVersioningPlaceholderFile(package_identifier, directory_name);
            }
        } else {
            Log.e("SubstratumLogger",
                    "There is no valid package name under this abbreviated folder " +
                            "count.");
        }
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
