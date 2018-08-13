/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.util.tabs;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import com.google.android.material.snackbar.Snackbar;
import org.zeroturnaround.zip.FileSource;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.tabs.BootAnimationsManager;
import projekt.substratum.util.views.Lunchbar;

import javax.crypto.Cipher;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static projekt.substratum.common.Internal.BOOTANIMATION;
import static projekt.substratum.common.Internal.BOOTANIMATION_BU;
import static projekt.substratum.common.Internal.BOOTANIMATION_CACHE;
import static projekt.substratum.common.Internal.BOOTANIMATION_CREATION_CACHE;
import static projekt.substratum.common.Internal.BOOTANIMATION_DESCRIPTOR;
import static projekt.substratum.common.Internal.BOOT_ANIMATION_APPLIED;
import static projekt.substratum.common.Internal.BYTE_ACCESS_RATE;
import static projekt.substratum.common.Internal.ENCRYPTED_FILE_EXTENSION;
import static projekt.substratum.common.Internal.SHUTDOWNANIMATION;
import static projekt.substratum.common.Internal.SHUTDOWN_ANIMATION_APPLIED;
import static projekt.substratum.common.Internal.SYSTEM_ADDON_DIR;
import static projekt.substratum.common.References.EXTERNAL_STORAGE_CACHE;

public class BootAnimationUtils {

    private static final String TAG = "BootAnimationUtils";
    private static final String DATA_SYSTEM = "/data/system/theme/";
    private static final String SYSTEM_MEDIA = "/system/media/";
    private static final String BACKUP_SCRIPT = "81-subsboot.sh";

    /**
     * Apply the boot animation
     *
     * @param view              The view of the caller
     * @param arguments         Arguments to pass
     * @param context           Self explanatory, bud
     * @param themePid          Theme's package name
     * @param encrypted         Encrypted status
     * @param shutdownAnimation Shutdown animation or not
     * @param cipher            Encryption handshake
     */
    public static void execute(View view,
                               String arguments,
                               Context context,
                               String themePid,
                               boolean encrypted,
                               boolean shutdownAnimation,
                               Cipher cipher) {
        new BootAnimationHandlerAsync(
                view,
                context,
                themePid,
                encrypted,
                shutdownAnimation,
                cipher
        ).execute(arguments);
    }

    /**
     * Main function to apply the bootanimation on the device
     */
    private static class BootAnimationHandlerAsync extends AsyncTask<String, Integer, String> {

        @SuppressLint("StaticFieldLeak")
        private final Context context;
        @SuppressLint("StaticFieldLeak")
        private final View view;
        private final String themePid;
        private final SharedPreferences prefs = Substratum.getPreferences();
        private final boolean encrypted;
        private final Cipher cipher;
        private final boolean shutdownAnimation;
        private ProgressDialog progress;
        private boolean hasFailed;

        BootAnimationHandlerAsync(View view,
                                  Context context,
                                  String themePid,
                                  boolean encrypted,
                                  boolean shutdownAnimation,
                                  Cipher cipher) {
            super();
            this.context = context;
            this.view = view;
            this.themePid = themePid;
            this.encrypted = encrypted;
            this.cipher = cipher;
            this.shutdownAnimation = shutdownAnimation;
        }

        private static void CopyStream(InputStream Input, OutputStream Output) throws
                IOException {
            byte[] buffer = new byte[BYTE_ACCESS_RATE];
            int length = Input.read(buffer);
            while (length > 0) {
                Output.write(buffer, 0, length);
                length = Input.read(buffer);
            }
        }

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(context, R.style.AppTheme_DialogAlert);
            progress.setMessage(context.getString(shutdownAnimation ?
                    R.string.shutdownanimation_dialog_apply_text :
                    R.string.bootanimation_dialog_apply_text));
            progress.setIndeterminate(false);
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected void onPostExecute(String result) {
            progress.dismiss();

            if (!hasFailed) {
                Lunchbar.make(view, context.getString(shutdownAnimation ?
                                R.string.shutdownanimation_dialog_apply_success :
                                R.string.bootanimation_dialog_apply_success),
                        Snackbar.LENGTH_LONG)
                        .show();
            } else {
                Lunchbar.make(view, context.getString(shutdownAnimation ?
                                R.string.shutdownanimation_dialog_apply_failed :
                                R.string.bootanimation_dialog_apply_failed),
                        Snackbar.LENGTH_LONG)
                        .show();
            }
            if (!Systems.checkThemeInterfacer(context)) {
                FileOperations.mountROData();
                FileOperations.mountRO();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            hasFailed = false;

            // Move the file from assets folder to a new working area
            Substratum.log(TAG, "Copying over the selected boot animation to working directory...");

            File cacheDirectory = new File(context.getCacheDir(), BOOTANIMATION_CACHE);
            if (!cacheDirectory.exists() && cacheDirectory.mkdirs())
                Substratum.log(TAG, "Bootanimation folder created");

            File cacheDirectory2 = new File(context.getCacheDir(),
                    BOOTANIMATION_CREATION_CACHE);
            if (!cacheDirectory2.exists() && cacheDirectory2.mkdirs()) {
                Substratum.log(TAG, "Bootanimation work folder created");
            } else {
                FileOperations.delete(
                        context,
                        context.getCacheDir().getAbsolutePath() +
                                BOOTANIMATION_CREATION_CACHE);
                boolean created = cacheDirectory2.mkdirs();
                if (created) Substratum.log(TAG, "Bootanimation folder recreated");
            }

            String bootanimation = sUrl[0];

            String directory = (shutdownAnimation ?
                    "shutdownanimation" :
                    "bootanimation");

            // Now let's take out desc.txt from the theme's assets (bootanimation.zip) and parse it
            if (!hasFailed) {
                Substratum.log(TAG, "Analyzing integrity of boot animation descriptor file...");
                if (cipher != null) {
                    try {
                        Context otherContext = context.createPackageContext(this
                                .themePid, 0);
                        AssetManager themeAssetManager = otherContext.getAssets();
                        FileOperations.copyFileOrDir(
                                themeAssetManager,
                                directory + '/' + bootanimation +
                                        (encrypted ?
                                                ".zip" + ENCRYPTED_FILE_EXTENSION : ".zip"),
                                context.getCacheDir().getAbsolutePath() +
                                        BOOTANIMATION_CREATION_CACHE +
                                        bootanimation + ".zip",
                                directory + '/' + bootanimation +
                                        (encrypted ?
                                                ".zip" + ENCRYPTED_FILE_EXTENSION : ".zip"),
                                cipher);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        Context otherContext = context.createPackageContext(this
                                .themePid, 0);
                        AssetManager am = otherContext.getAssets();
                        try (InputStream inputStream = am.open(
                                directory + '/' + bootanimation + ".zip");
                             OutputStream outputStream = new FileOutputStream(
                                     context.getCacheDir().getAbsolutePath() +
                                             BOOTANIMATION_CREATION_CACHE +
                                             bootanimation + ".zip")) {
                            BootAnimationHandlerAsync.CopyStream(inputStream, outputStream);
                        }
                    } catch (Exception ignored) {
                        hasFailed = true;
                        Log.e(TAG,
                                "There is no animation.zip found within the assets " +
                                        "of this theme!");

                    }
                }

                // Rename the file
                File workingDirectory = new File(
                        context.getCacheDir().getAbsolutePath() +
                                BOOTANIMATION_CREATION_CACHE);
                File from = new File(workingDirectory, bootanimation + ".zip");
                bootanimation =
                        bootanimation.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "");
                File to = new File(workingDirectory, bootanimation + ".zip");
                boolean rename = from.renameTo(to);
                if (rename)
                    Substratum.log(TAG, "Boot Animation successfully moved to new directory");
            }

            if (!hasFailed) {
                boolean exists = ZipUtil.containsEntry(
                        new File(context.getCacheDir().getAbsolutePath() +
                                BOOTANIMATION_CREATION_CACHE +
                                bootanimation + ".zip"), BOOTANIMATION_DESCRIPTOR);
                if (exists) {
                    ZipUtil.unpackEntry(
                            new File(context.getCacheDir().getAbsolutePath() +
                                    BOOTANIMATION_CREATION_CACHE +
                                    bootanimation + ".zip"),
                            BOOTANIMATION_DESCRIPTOR,
                            new File(context.getCacheDir().getAbsolutePath() +
                                    BOOTANIMATION_CREATION_CACHE + BOOTANIMATION_DESCRIPTOR));
                } else {
                    Log.e(TAG,
                            "Could not find specified boot animation descriptor file (desc.txt)!");
                    hasFailed = true;
                }
            }

            // Begin parsing of the file (desc.txt) and parse the first line
            if (!hasFailed) {
                Substratum.log(TAG, "Calculating hardware display density metrics " +
                        "and resizing the bootanimation...");
                BufferedReader reader = null;
                try (OutputStream os = new FileOutputStream(
                        context.getCacheDir().getAbsolutePath() +
                                BOOTANIMATION_CREATION_CACHE + "scaled-" + bootanimation + ".zip");
                     ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(os));
                     ZipInputStream bootAni = new ZipInputStream(new BufferedInputStream(new
                             FileInputStream(
                             context.getCacheDir().getAbsolutePath() +
                                     BOOTANIMATION_CREATION_CACHE + bootanimation + ".zip")))) {

                    zos.setMethod(ZipOutputStream.STORED);
                    byte[] bytes = new byte[BYTE_ACCESS_RATE];
                    ZipEntry ze;
                    while ((ze = bootAni.getNextEntry()) != null) {
                        ZipEntry entry = new ZipEntry(ze.getName());
                        entry.setMethod(ZipEntry.STORED);
                        entry.setCrc(ze.getCrc());
                        entry.setSize(ze.getSize());
                        entry.setCompressedSize(ze.getSize());
                        if (!ze.getName().equals(BOOTANIMATION_DESCRIPTOR)) {
                            // just copy this entry straight over into the output zip
                            zos.putNextEntry(entry);
                            int len;
                            while ((len = bootAni.read(bytes)) > 0) {
                                zos.write(bytes, 0, len);
                            }
                        } else {
                            reader = new BufferedReader(new InputStreamReader
                                    (bootAni));
                            String[] info = reader.readLine().split(" ");

                            WindowManager wm = (WindowManager) context.getSystemService
                                    (Context.WINDOW_SERVICE);
                            DisplayMetrics dm = new DisplayMetrics();
                            if (wm != null) {
                                wm.getDefaultDisplay().getRealMetrics(dm);
                            }
                            // just in case the device is in landscape orientation we will
                            // swap the values since most (if not all) animations are portrait
                            int preventLintW = dm.widthPixels;
                            int preventLintH = dm.heightPixels;
                            int scaledHeight;
                            int scaledWidth;
                            if (dm.widthPixels > dm.heightPixels) {
                                scaledWidth = preventLintH;
                                scaledHeight = preventLintW;
                            } else {
                                scaledWidth = dm.widthPixels;
                                scaledHeight = dm.heightPixels;
                            }

                            int width = Integer.parseInt(info[0]);
                            int height = Integer.parseInt(info[1]);

                            if (width == height) {
                                //noinspection SuspiciousNameCombination
                                scaledHeight = scaledWidth;
                            } else {
                                // adjust scaledHeight to retain original aspect ratio
                                float scale = (float) scaledWidth / (float) width;
                                int newHeight = (int) ((float) height * scale);
                                if (newHeight < scaledHeight)
                                    scaledHeight = newHeight;
                            }

                            CRC32 crc32 = new CRC32();
                            ByteBuffer buffer = ByteBuffer.wrap(bytes);
                            String line = String.format(Locale.US,
                                    "%d %d %s\n", scaledWidth, scaledHeight, info[2]);
                            buffer.put(line.getBytes());
                            int size = 0;
                            size += line.getBytes().length;
                            crc32.update(line.getBytes());
                            while ((line = reader.readLine()) != null) {
                                line = String.format("%s\n", line);
                                buffer.put(line.getBytes());
                                size += line.getBytes().length;
                                crc32.update(line.getBytes());
                            }
                            entry.setCrc(crc32.getValue());
                            entry.setSize((long) size);
                            entry.setCompressedSize((long) size);

                            zos.putNextEntry(entry);
                            zos.write(buffer.array(), 0, size);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "The boot animation descriptor file " +
                            "(" + BOOTANIMATION_DESCRIPTOR + ") " +
                            "could not be parsed properly!");
                    hasFailed = true;
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }

            if (!hasFailed) {
                Substratum.log(TAG,
                        "Finalizing the boot animation descriptor file and " +
                                "committing changes to the archive...");

                ZipEntrySource[] addedEntries = {
                        new FileSource(BOOTANIMATION_DESCRIPTOR, new File(
                                context.getCacheDir().getAbsolutePath() +
                                        BOOTANIMATION_CREATION_CACHE + BOOTANIMATION_DESCRIPTOR))
                };
                ZipUtil.addOrReplaceEntries(new File(
                        context.getCacheDir().getAbsolutePath() +
                                BOOTANIMATION_CREATION_CACHE +
                                bootanimation + ".zip"), addedEntries);
            }

            if (!hasFailed) {
                Substratum.log(TAG, "Moving boot animation to theme directory " +
                        "and setting correct contextual parameters...");
                boolean isEncrypted = Systems.getDeviceEncryptionStatus(context) > 1;
                File themeDirectory;
                if (Systems.checkOMS(context)) {
                    if ((!isEncrypted || shutdownAnimation) &&
                            Systems.checkSubstratumFeature(context)) {
                        Substratum.log(TAG, "Data partition on the current device is decrypted, using " +
                                "dedicated theme bootanimation slot...");
                        themeDirectory = new File(DATA_SYSTEM);
                        if (!themeDirectory.exists()) {
                            if (!Systems.checkThemeInterfacer(context)) {
                                FileOperations.mountRWData();
                            }
                            FileOperations.createNewFolder(context, DATA_SYSTEM);
                        }
                    } else {
                        Substratum.log(TAG, "Data partition on the current device is encrypted, using " +
                                "dedicated encrypted bootanimation slot...");
                        themeDirectory = new File(SYSTEM_MEDIA);
                    }
                } else {
                    Substratum.log("BootAnimationUtils",
                            "Current device is on substratum legacy, " +
                                    "using system bootanimation slot...");
                    themeDirectory = new File(SYSTEM_MEDIA);
                }

                File scaledBootAnimCheck = new File(context.getCacheDir()
                        .getAbsolutePath() + BOOTANIMATION_CREATION_CACHE +
                        "scaled-" + bootanimation + ".zip");
                if (scaledBootAnimCheck.exists()) {
                    Substratum.log(TAG, "Scaled boot animation created by Substratum verified!");
                } else {
                    hasFailed = true;
                    Log.e(TAG, "Scaled boot animation created by Substratum NOT verified!");
                }

                // Move created boot animation to working directory
                FileOperations.move(context,
                        scaledBootAnimCheck.getAbsolutePath(),
                        EXTERNAL_STORAGE_CACHE +
                                (shutdownAnimation ?
                                        SHUTDOWNANIMATION : BOOTANIMATION));

                // Inject backup script for encrypted legacy and encrypted OMS devices
                if (!hasFailed && (isEncrypted || !Systems.checkOMS(context)) &&
                        !shutdownAnimation) {
                    FileOperations.mountRW();
                    File backupScript = new File(SYSTEM_ADDON_DIR + BACKUP_SCRIPT);

                    if (Systems.checkSubstratumFeature(context)) {
                        if (!backupScript.exists()) {
                            AssetManager assetManager = context.getAssets();
                            String backupScriptPath =
                                    context.getFilesDir().getAbsolutePath() + '/' +
                                            BACKUP_SCRIPT;
                            try (OutputStream out = new FileOutputStream(backupScriptPath);
                                 InputStream in = assetManager.open(BACKUP_SCRIPT)) {
                                byte[] buffer = new byte[BYTE_ACCESS_RATE];
                                int read;
                                while ((read = in.read(buffer)) != -1) {
                                    out.write(buffer, 0, read);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            FileOperations.copy(context, context.getFilesDir()
                                    .getAbsolutePath() +
                                    '/' + BACKUP_SCRIPT, backupScript.getAbsolutePath());
                            FileOperations.setPermissions(755, backupScript.getAbsolutePath());
                        }
                    }

                    File backupDirectory = new File(themeDirectory.getAbsolutePath() +
                            "/" + BOOTANIMATION_BU);
                    if (!backupDirectory.exists()) {
                        FileOperations.move(context, themeDirectory.getAbsolutePath()
                                + "/" + BOOTANIMATION, backupDirectory.getAbsolutePath());
                    }

                    File bootAnimationCheck = new File(themeDirectory.getAbsolutePath() +
                            "/" + BOOTANIMATION);

                    if (backupDirectory.exists()) {
                        if (backupScript.exists()) {
                            Substratum.log(TAG, "Old bootanimation is backed up, ready to go!");
                        }
                    } else if (!bootAnimationCheck.exists() && !backupDirectory.exists()) {
                        Substratum.log(TAG, "There is no predefined bootanimation on this device, " +
                                "injecting a brand new default bootanimation...");
                    } else {
                        hasFailed = true;
                        Log.e(TAG, "Failed to backup bootanimation!");
                    }
                }

                if (!hasFailed) {
                    BootAnimationsManager.setBootAnimation(context,
                            themeDirectory.getAbsolutePath(), shutdownAnimation);
                }
            }

            if (!hasFailed) {
                SharedPreferences.Editor editor = prefs.edit();
                if (shutdownAnimation) {
                    editor.putString(SHUTDOWN_ANIMATION_APPLIED, themePid);
                } else {
                    editor.putString(BOOT_ANIMATION_APPLIED, themePid);
                }
                editor.apply();
                Substratum.log(TAG, "Boot animation installed!");
                FileOperations.delete(context, context.getCacheDir().getAbsolutePath() +
                        BOOTANIMATION_CREATION_CACHE);
            } else {
                Log.e(TAG, "Boot animation installation aborted!");
                FileOperations.delete(context, context.getCacheDir().getAbsolutePath() +
                        BOOTANIMATION_CREATION_CACHE);
            }
            return null;
        }
    }
}