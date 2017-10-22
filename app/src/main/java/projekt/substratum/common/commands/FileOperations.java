/*
 * Copyright (c) 2016-2017 Projekt Substratum
 * This file is part of Substratum.
 *
 * Substratum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Substratum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Substratum.  If not, see <http://www.gnu.org/licenses/>.
 */

package projekt.substratum.common.commands;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;

import projekt.substratum.common.platform.ThemeInterfacerService;
import projekt.substratum.util.files.Root;

import static projekt.substratum.common.References.ENABLE_DIRECT_ASSETS_LOGGING;
import static projekt.substratum.common.Systems.checkThemeInterfacer;

public enum FileOperations {
    ;

    private static final String COPY_LOG = "SubstratumCopy";
    private static final String COPYDIR_LOG = "SubstratumCopyDir";
    private static final String CREATE_LOG = "SubstratumCreate";
    private static final String DELETE_LOG = "SubstratumDelete";
    private static final String MOVE_LOG = "SubstratumMove";
    private static final String DA_LOG = "DirectAssets";
    private static final String ENCRYPTION_EXTENSION = ".enc";

    public static void adjustContentProvider(final String uri,
                                             final String topic, final String fileName) {
        Root.runCommand("content insert --uri " + uri + ' ' +
                "--bind name:s:" + topic + " --bind value:s:" + fileName);
    }

    public static void setContext(final String foldername) {
        Root.runCommand("chcon -R u:object_r:system_file:s0 " + foldername);
    }

    public static void setPermissions(final int permission, final String foldername) {
        Root.runCommand("chmod " + permission + ' ' + foldername);
    }

    public static void setPermissionsRecursively(final int permission, final String foldername) {
        Root.runCommand("chmod -R " + permission + ' ' + foldername);
    }

    @SuppressWarnings("SameParameterValue")
    public static void setProp(final String propName, final String propValue) {
        Root.runCommand("setprop " + propName + ' ' + propValue);
    }

    public static void symlink(final String source, final String destination) {
        Root.runCommand("ln -s " + source + ' ' + destination);
    }

    private static String checkBox(final String mountType) {
        Process process = null;
        // default style is "toybox" style, because aosp has toybox not toolbox
        String result = mountType + ",remount";
        try {
            final Runtime rt = Runtime.getRuntime();
            process = rt.exec(new String[]{"readlink", "/system/bin/mount"});
            try (BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(process.getInputStream()))) {
                // if it has toolbox instead of toybox, handle
                if ("toolbox".equals(stdInput.readLine())) {
                    result = "remount," + mountType;
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return result;
    }

    public static void mountRW() {
        Root.runCommand("mount -o " + checkBox("rw") + " /system");
    }

    public static void mountRWData() {
        Root.runCommand("mount -o " + checkBox("rw") + " /data");
    }

    public static void mountRWVendor() {
        Root.runCommand("mount -o " + checkBox("rw") + " /vendor");
    }

    public static void mountRO() {
        Root.runCommand("mount -o " + checkBox("ro") + " /system");
    }

    public static void mountROData() {
        Root.runCommand("mount -o " + checkBox("ro") + " /data");
    }

    public static void mountROVendor() {
        Root.runCommand("mount -o " + checkBox("ro") + " /vendor");
    }

    public static void createNewFolder(final Context context, final String destination) {
        final String dataDir = context.getDataDir().getAbsolutePath();
        final String externalDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        final boolean needRoot = (
                !destination.startsWith(dataDir) && !destination.startsWith(externalDir) &&
                        !destination.startsWith("/system")) || (!destination.startsWith(dataDir) &&
                !destination.startsWith(externalDir) && !destination.startsWith("/system"));
        if (checkThemeInterfacer(context) && needRoot) {
            ThemeInterfacerService.createNewFolder(context, destination);
        } else {
            createNewFolder(destination);
        }
    }

    public static void createNewFolder(final String foldername) {
        Log.d(CREATE_LOG, "Using rootless operation to create " + foldername);
        final File folder = new File(foldername);
        if (!folder.exists()) {
            Log.d(CREATE_LOG, "Operation " + (folder.mkdirs() ? "succeeded" : "failed"));
            if (!folder.exists()) {
                Log.d(CREATE_LOG, "Using rooted operation to create " + foldername);
                Root.runCommand("mkdir " + foldername);
            }
        } else {
            Log.d("SubstratumCreate", "Folder already exists!");
        }
    }

    public static void copy(final Context context, final String source, final String destination) {
        final String dataDir = context.getDataDir().getAbsolutePath();
        final String externalDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        final boolean needRoot = (!source.startsWith(dataDir) && !source.startsWith(externalDir) &&
                !source.startsWith("/system")) || (!destination.startsWith(dataDir) &&
                !destination.startsWith(externalDir) && !destination.startsWith("/system"));
        if (checkThemeInterfacer(context) && needRoot) {
            Log.d(COPY_LOG,
                    "Using theme interface operation to copy " + source + " to " + destination);
            ThemeInterfacerService.copy(context, source, destination);

            // Wait until copy succeeds
            try {
                int retryCount = 0;
                final File file = new File(destination);
                while (!file.exists() && (retryCount < 5)) {
                    Thread.sleep(1000L);
                    retryCount++;
                }
                if (retryCount == 5) Log.d(COPY_LOG, "Operation timed out!");
                Log.d(COPY_LOG, "Operation " + (file.exists() ? "succeeded" : "failed"));
            } catch (final InterruptedException e) {
                Thread.interrupted();
            }
        } else {
            copy(source, destination);
        }
    }

    private static void copy(final String source, final String destination) {
        Log.d(COPY_LOG,
                "Using rootless operation to copy " + source + " to " + destination);
        final File out = new File(destination);
        try {
            final File in = new File(source);
            FileUtils.copyFile(in, out);
        } catch (final IOException e) {
            // Suppress warning
        }
        if (!out.exists()) {
            Log.d(COPY_LOG,
                    "Rootless operation failed, falling back to rooted mode...");
            Root.runCommand("cp -f " + source + ' ' + destination);
        }
        Log.d(COPY_LOG, "Operation " + (out.exists() ? "succeeded" : "failed"));
    }

    public static void copyDir(final Context context, final String source, final String
            destination) {
        final String dataDir = context.getDataDir().getAbsolutePath();
        final String externalDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        final boolean needRoot = (!source.startsWith(dataDir) && !source.startsWith(externalDir) &&
                !source.startsWith("/system")) || (!destination.startsWith(dataDir) &&
                !destination.startsWith(externalDir) && !destination.startsWith("/system"));
        if (checkThemeInterfacer(context) && needRoot) {
            copy(context, source, destination);
        } else {
            copyDir(source, destination);
        }
    }

    private static void copyDir(final String source, final String destination) {
        Log.d(COPYDIR_LOG,
                "Using rootless operation to copy " + source + " to " + destination);
        final File out = new File(destination);
        try {
            final File in = new File(source);
            FileUtils.copyDirectory(in, out);
        } catch (final IOException e) {
            // Suppress warning
        }
        if (!out.exists()) {
            Log.d(COPY_LOG,
                    "Rootless operation failed, falling back to rooted mode...");
            Root.runCommand("cp -rf " + source + ' ' + destination);
        }
        Log.d(COPYDIR_LOG, "Operation " + (out.exists() ? "succeeded" : "failed"));
    }

    public static void bruteforceDelete(final String directory) {
        Root.runCommand("rm -rf " + directory);
    }

    public static void delete(final Context context, final String directory) {
        delete(context, directory, true);
    }

    public static void delete(final Context context, final String directory, final boolean
            deleteParent) {
        final String dataDir = context.getDataDir().getAbsolutePath();
        final String externalDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        final boolean needRoot = (!directory.startsWith(dataDir) && !directory.startsWith
                (externalDir) &&
                !directory.startsWith("/system"));
        if (checkThemeInterfacer(context) && needRoot) {
            Log.d(DELETE_LOG, "Using theme interfacer operation to delete " + directory);
            ThemeInterfacerService.delete(context, directory, deleteParent);

            // Wait until delete success
            try {
                int retryCount = 0;
                final File file = new File(directory);
                final boolean notDone = deleteParent ? file.exists() : file.list().length == 0;
                while (notDone && (retryCount < 5)) {
                    Thread.sleep(1000L);
                    retryCount++;
                }
                if (retryCount == 5) Log.d(DELETE_LOG, "Operation timed out!");
                Log.d(DELETE_LOG, "Operation " + (!file.exists() ? "succeeded" : "failed"));
            } catch (final InterruptedException e) {
                Thread.interrupted();
            }
        } else {
            delete(directory, deleteParent);
        }
    }

    private static void delete(final String directory, final boolean deleteParent) {
        Log.d(DELETE_LOG, "Using rootless operation to delete " + directory);
        final File dir = new File(directory);
        try {
            if (deleteParent) {
                FileUtils.forceDelete(dir);
            } else {
                FileUtils.cleanDirectory(dir);
            }
        } catch (final FileNotFoundException e) {
            Log.d(DELETE_LOG, "File already " + (deleteParent ? "deleted." : "cleaned."));
        } catch (final IOException e) {
            // Suppress warning
        }
        if (dir.exists()) {
            Log.d(DELETE_LOG,
                    "Rootless operation failed, falling back to rooted mode...");
            if (deleteParent) {
                Root.runCommand("rm -rf " + directory);
            } else {
                final StringBuilder command = new StringBuilder("rm -rf ");
                if (dir.isDirectory()) {
                    for (final File child : dir.listFiles()) {
                        command.append(child.getAbsolutePath()).append(' ');
                    }
                    Root.runCommand(command.toString());
                } else {
                    Root.runCommand(command + directory);
                }
            }
        }
        Log.d(DELETE_LOG, "Operation " + (!dir.exists() ? "succeeded" : "failed"));
    }

    public static void move(final Context context, final String source, final String destination) {
        final String dataDir = context.getDataDir().getAbsolutePath();
        final String externalDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        final boolean needRoot = (!source.startsWith(dataDir) && !source.startsWith(externalDir) &&
                !source.startsWith("/system")) || (!destination.startsWith(dataDir) &&
                !destination.startsWith(externalDir) && !destination.startsWith("/system"));
        if (checkThemeInterfacer(context) && needRoot) {
            Log.d(MOVE_LOG,
                    "Using theme interfacer operation to move " + source + " to " + destination);
            ThemeInterfacerService.move(context, source, destination);

            // Wait until move success
            try {
                int retryCount = 0;
                final File file = new File(destination);
                while (!file.exists() && (retryCount < 5)) {
                    Thread.sleep(1000L);
                    retryCount++;
                }
                if (retryCount == 5) Log.d(MOVE_LOG, "Operation timed out");
                Log.d(MOVE_LOG, "Operation " + (file.exists() ? "succeeded" : "failed"));
            } catch (final InterruptedException e) {
                Thread.interrupted();
            }
        } else {
            move(source, destination);
        }
    }

    private static void move(final String source, final String destination) {
        Log.d(MOVE_LOG, "Using rootless operation to move " + source + " to " +
                destination);
        final File out = new File(destination);
        try {
            final File in = new File(source);
            if (in.isFile()) {
                FileUtils.moveFile(in, out);
            } else if (in.isDirectory()) {
                FileUtils.moveDirectory(in, out);
            }
        } catch (final Exception e) {
            Log.d(MOVE_LOG,
                    "Rootless operation failed, falling back to rooted mode...");
            Root.runCommand("mv -f " + source + ' ' + destination);
        }
        Log.d(MOVE_LOG, "Operation " + (out.exists() ? "succeeded" : "failed"));
    }

    public static long getFileSize(final File source) {
        long size = 0L;
        if (source.isDirectory()) {
            for (final File file : source.listFiles()) {
                size += getFileSize(file);
            }
        } else {
            size = source.length();
        }
        return size;
    }

    /**
     * EncryptedAssets InputStream
     *
     * @param assetManager take the asset manager context from the theme package
     * @param filePath     the expected list directory inside the assets folder
     * @param cipherKey    the decryption key for the Cipher object
     */
    public static InputStream getInputStream(
            @NonNull final AssetManager assetManager,
            @NonNull final String filePath,
            @Nullable final Cipher cipherKey) throws IOException {
        final InputStream inputStream = assetManager.open(filePath);
        if ((cipherKey != null) && filePath.endsWith(ENCRYPTION_EXTENSION)) {
            return new CipherInputStream(inputStream, cipherKey);
        }
        return inputStream;
    }

    /**
     * DirectAssets Mode Functions
     *
     * @param assetManager take the asset manager context from the theme package
     * @param listDir      the expected list directory inside the assets folder
     * @param destination  output directory on where we should be caching
     * @param remember     should be the same as listDir, so we strip out the unnecessary prefix
     *                     so it
     *                     only extracts to a specified folder without the asset manager's list
     *                     structure.
     */
    public static boolean copyFileOrDir(final AssetManager assetManager, final String listDir,
                                        final String destination, final String remember, final
                                        Cipher cipher) {
        if (ENABLE_DIRECT_ASSETS_LOGGING) Log.d(DA_LOG, "Source: " + listDir);
        if (ENABLE_DIRECT_ASSETS_LOGGING) Log.d(DA_LOG, "Destination: " + destination);
        try {
            final String[] assets = assetManager.list(listDir);
            if (assets.length == 0) {
                // When asset[] is empty, it is not iterable, hence it is a file
                if (ENABLE_DIRECT_ASSETS_LOGGING)
                    Log.d(DA_LOG, "This is a file object, directly copying...");
                if (ENABLE_DIRECT_ASSETS_LOGGING) Log.d(DA_LOG, listDir);
                final boolean copied = copyFile(assetManager, listDir, destination, remember,
                        cipher);
                if (ENABLE_DIRECT_ASSETS_LOGGING) Log.d(DA_LOG, "File operation status: " +
                        ((copied) ? "Success!" : "Failed"));
            } else {
                // This will be a folder if the size is greater than 0
                final String fullPath = (destination + '/' + listDir.substring(remember.length()))
                        .replaceAll("\\s+", "");
                final File dir = new File(fullPath);
                if (!dir.exists()) {
                    Log.d(DA_LOG, "Attempting to copy: " + dir.getAbsolutePath() + '/');
                    Log.d(DA_LOG, "File operation status: " +
                            ((dir.mkdir()) ? "Success!" : "Failed"));
                }
                for (final String asset : assets) {
                    copyFileOrDir(assetManager, listDir + '/' + asset, destination, remember,
                            cipher);
                }
            }
            return true;
        } catch (final IOException ex) {
            if (ENABLE_DIRECT_ASSETS_LOGGING)
                Log.e(DA_LOG, "An IOException has been reached: " + ex.getMessage());
        }
        return false;
    }

    private static boolean copyFile(final AssetManager assetManager, final String filename,
                                    final String destination, final String remember, final Cipher
                                            cipher) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = assetManager.open(filename);
            if ((cipher != null) && filename.endsWith(".enc")) {
                inputStream = new CipherInputStream(inputStream, cipher);
            } else if ((cipher == null) && filename.endsWith(".enc")) {
                return false;
            }
            final String destinationFile = destination + filename.replaceAll("\\s+", "")
                    .substring(remember.replaceAll("\\s+", "").length());
            outputStream = new FileOutputStream(
                    ((cipher != null) ?
                            destinationFile.substring(0, destinationFile.length() - 4) :
                            destinationFile));

            final byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            return true;
        } catch (final Exception e) {
            e.printStackTrace();
            if (ENABLE_DIRECT_ASSETS_LOGGING)
                Log.e(DA_LOG, "An exception has been reached: " + e.getMessage());
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
            } catch (final IOException e) {
                // Suppress warning
            }
        }
        return false;
    }

    public static void copyFromAsset(final Context ctx, final String fileName, final String
            targetPath) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = ctx.getAssets().open(fileName);

            out = new FileOutputStream(targetPath);

            final byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

        } catch (final IOException e) {
            Log.e("tag", "Failed to copy asset file: ", e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }

                if (out != null) {
                    out.flush();
                    out.close();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }
}