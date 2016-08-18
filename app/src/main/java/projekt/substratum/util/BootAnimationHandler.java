package projekt.substratum.util;

import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import org.zeroturnaround.zip.FileSource;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;

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
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import projekt.substratum.R;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class BootAnimationHandler {

    private Context mContext;
    private ProgressDialog progress;
    private Boolean has_failed;
    private String theme_pid;
    private SharedPreferences prefs;

    private int getDeviceEncryptionStatus() {
        // 0: ENCRYPTION_STATUS_UNSUPPORTED
        // 1: ENCRYPTION_STATUS_INACTIVE
        // 2: ENCRYPTION_STATUS_ACTIVATING
        // 3: ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY
        // 4: ENCRYPTION_STATUS_ACTIVE
        // 5: ENCRYPTION_STATUS_ACTIVE_PER_USER
        int status = DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED;
        final DevicePolicyManager dpm = (DevicePolicyManager)
                mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm != null) {
            status = dpm.getStorageEncryptionStatus();
        }
        return status;
    }

    public void BootAnimationHandler(String arguments, Context context, String theme_pid) {
        this.mContext = context;
        this.theme_pid = theme_pid;
        new BootAnimationHandlerAsync().execute(arguments);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private class BootAnimationHandlerAsync extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(mContext, android.R.style
                    .Theme_DeviceDefault_Dialog_Alert);
            progress.setMessage(mContext.getString(R.string.bootanimation_dialog_apply_text));
            progress.setIndeterminate(false);
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected void onPostExecute(String result) {
            progress.dismiss();
            if (!has_failed) {
                Toast toast = Toast.makeText(mContext,
                        mContext.getString(R.string.bootanimation_dialog_apply_success), Toast
                                .LENGTH_LONG);
                toast.show();
            } else {
                Toast toast = Toast.makeText(mContext,
                        mContext.getString(R.string.bootanimation_dialog_apply_failed), Toast
                                .LENGTH_LONG);
                toast.show();
            }
            Root.runCommand("mount -o ro,remount /");
            Root.runCommand("mount -o ro,remount /data");
            Root.runCommand("mount -o ro,remount /system");
        }

        @Override
        protected String doInBackground(String... sUrl) {

            has_failed = false;

            // Move the file from assets folder to a new working area

            Log.d("BootAnimationHandler", "Copying over the selected boot animation to working " +
                    "directory...");

            File cacheDirectory = new File(mContext.getCacheDir(), "/BootAnimationCache/");
            if (!cacheDirectory.exists()) {
                boolean created = cacheDirectory.mkdirs();
                if (created) Log.d("BootAnimationHandler", "Bootanimation folder created");
            }
            File cacheDirectory2 = new File(mContext.getCacheDir(), "/BootAnimationCache/" +
                    "AnimationCreator/");
            if (!cacheDirectory2.exists()) {
                boolean created = cacheDirectory2.mkdirs();
                if (created) Log.d("BootAnimationHandler", "Bootanimation work folder created");
            } else {
                Root.runCommand(
                        "rm -r " + mContext.getCacheDir().getAbsolutePath() +
                                "/BootAnimationCache/AnimationCreator/");
                boolean created = cacheDirectory2.mkdirs();
                if (created) Log.d("BootAnimationHandler", "Bootanimation folder recreated");
            }

            String bootanimation = sUrl[0];

            // Now let's take out desc.txt from the theme's assets (bootanimation.zip) and parse it

            if (!has_failed) {
                Log.d("BootAnimationHandler", "Analyzing integrity of boot animation descriptor " +
                        "file...");
                try {
                    Context otherContext = mContext.createPackageContext(theme_pid, 0);
                    AssetManager am = otherContext.getAssets();
                    try (InputStream inputStream = am.open("bootanimation/" + bootanimation + "" +
                            ".zip");
                         OutputStream outputStream = new FileOutputStream(mContext.getCacheDir()
                                 .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/" +
                                 bootanimation + ".zip")) {

                        CopyStream(inputStream, outputStream);
                    }
                } catch (Exception e) {
                    Log.e("BootAnimationHandler", "There is no bootanimation.zip found within the" +
                            " " +
                            "assets " +
                            "of this theme!");
                    has_failed = true;
                }

                // Rename the file

                File workingDirectory = new File(mContext.getCacheDir()
                        .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/");
                File from = new File(workingDirectory, bootanimation + ".zip");
                bootanimation = bootanimation.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+",
                        "");
                File to = new File(workingDirectory, bootanimation + ".zip");
                boolean rename = from.renameTo(to);
                if (rename)
                    Log.d("BootAnimationHandler", "Boot Animation successfully moved to new " +
                            "directory");
            }

            if (!has_failed) {
                boolean exists = ZipUtil.containsEntry(new File(mContext.getCacheDir()
                        .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/" +
                        bootanimation + ".zip"), "desc.txt");

                if (exists) {
                    ZipUtil.unpackEntry(new File(mContext.getCacheDir()
                            .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/" +
                            bootanimation + ".zip"), "desc.txt", new File(mContext.getCacheDir()
                            .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/desc.txt"));
                } else {
                    Log.e("BootAnimationHandler", "Could not find specified boot animation " +
                            "descriptor file (desc.txt)!");
                    has_failed = true;
                }
            }

            // Begin parsing of the file (desc.txt) and parse the first line

            if (!has_failed) {
                Log.d("BootAnimationHandler", "Calculating hardware display density metrics and " +
                        "resizing the bootanimation...");
                BufferedReader reader = null;
                try (final OutputStream os = new FileOutputStream(mContext.getCacheDir()
                        .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/" +
                        "scaled-" + bootanimation + ".zip");
                     final ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(os));
                     final ZipInputStream bootAni = new ZipInputStream(new BufferedInputStream(new
                             FileInputStream(mContext.getCacheDir()
                             .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/" +
                             bootanimation + ".zip")))) {
                    ZipEntry ze;

                    zos.setMethod(ZipOutputStream.STORED);
                    final byte[] bytes = new byte[4096];
                    int len;
                    while ((ze = bootAni.getNextEntry()) != null) {
                        ZipEntry entry = new ZipEntry(ze.getName());
                        entry.setMethod(ZipEntry.STORED);
                        entry.setCrc(ze.getCrc());
                        entry.setSize(ze.getSize());
                        entry.setCompressedSize(ze.getSize());
                        if (!ze.getName().equals("desc.txt")) {
                            // just copy this entry straight over into the output zip
                            zos.putNextEntry(entry);
                            while ((len = bootAni.read(bytes)) > 0) {
                                zos.write(bytes, 0, len);
                            }
                        } else {
                            String line;
                            reader = new BufferedReader(new InputStreamReader
                                    (bootAni));
                            final String[] info = reader.readLine().split(" ");

                            int scaledWidth;
                            int scaledHeight;
                            WindowManager wm = (WindowManager) mContext.getSystemService
                                    (Context.WINDOW_SERVICE);
                            DisplayMetrics dm = new DisplayMetrics();
                            wm.getDefaultDisplay().getRealMetrics(dm);
                            // just in case the device is in landscape orientation we will
                            // swap the values since most (if not all) animations are portrait
                            if (dm.widthPixels > dm.heightPixels) {
                                scaledWidth = dm.heightPixels;
                                scaledHeight = dm.widthPixels;
                            } else {
                                scaledWidth = dm.widthPixels;
                                scaledHeight = dm.heightPixels;
                            }

                            int width = Integer.parseInt(info[0]);
                            int height = Integer.parseInt(info[1]);

                            if (width == height)
                                scaledHeight = scaledWidth;
                            else {
                                // adjust scaledHeight to retain original aspect ratio
                                float scale = (float) scaledWidth / (float) width;
                                int newHeight = (int) ((float) height * scale);
                                if (newHeight < scaledHeight)
                                    scaledHeight = newHeight;
                            }

                            CRC32 crc32 = new CRC32();
                            int size = 0;
                            ByteBuffer buffer = ByteBuffer.wrap(bytes);
                            line = String.format("%d %d %s\n", scaledWidth, scaledHeight, info[2]);
                            buffer.put(line.getBytes());
                            size += line.getBytes().length;
                            crc32.update(line.getBytes());
                            while ((line = reader.readLine()) != null) {
                                line = String.format("%s\n", line);
                                buffer.put(line.getBytes());
                                size += line.getBytes().length;
                                crc32.update(line.getBytes());
                            }
                            entry.setCrc(crc32.getValue());
                            entry.setSize(size);
                            entry.setCompressedSize(size);

                            zos.putNextEntry(entry);
                            zos.write(buffer.array(), 0, size);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("BootAnimationHandler", "The boot animation descriptor file (desc.txt) " +
                            "could not be parsed properly!");
                    has_failed = true;
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            //eat it
                        }
                    }
                }
            }

            if (!has_failed) {
                Log.d("BootAnimationHandler", "Finalizing the boot animation descriptor file and " +
                        "committing changes to the archive...");

                ZipEntrySource[] addedEntries = new ZipEntrySource[]{
                        new FileSource("desc.txt", new File(mContext.getCacheDir()
                                .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/desc" +
                                ".txt"))
                };
                ZipUtil.addOrReplaceEntries(new File(mContext.getCacheDir()
                        .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/" +
                        bootanimation + ".zip"), addedEntries);
            }

            if (!has_failed) {
                Log.d("BootAnimationHandler", "Moving boot animation to theme directory " +
                        "and setting correct contextual parameters...");
                boolean is_encrypted = false;
                File themeDirectory;
                if (getDeviceEncryptionStatus() <= 1) {
                    Log.d("BootAnimationHandler", "Data partition on the current device is " +
                            "decrypted, using dedicated theme bootanimation slot...");
                    themeDirectory = new File("/data/system/theme/");
                    if (!themeDirectory.exists()) {
                        Root.runCommand("mount -o rw,remount /data");
                        Root.runCommand("mkdir /data/system/theme/");
                    }
                } else {
                    Log.d("BootAnimationHandler", "Data partition on the current device is " +
                            "encrypted, using dedicated encrypted bootanimation slot...");
                    is_encrypted = true;
                    themeDirectory = new File("/system/media/");
                }

                File scaledBootAnimCheck = new File(mContext.getCacheDir()
                        .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/" + "scaled-"
                        + bootanimation + ".zip");
                if (scaledBootAnimCheck.exists()) {
                    Log.d("BootAnimationHandler", "Scaled boot animation created by Substratum " +
                            "verified!");
                } else {
                    Log.e("BootAnimationHandler", "Scaled boot animation created by Substratum " +
                            "NOT verified!");
                    has_failed = true;
                }

                if (!has_failed) {
                    Root.runCommand("mount -o rw,remount /system");
                    Root.runCommand("chmod 755 " + themeDirectory.getAbsolutePath());

                    Root.runCommand("mount -o rw,remount /system");
                    Root.runCommand(
                            "mv -f " + mContext.getCacheDir()
                                    .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/"
                                    + "scaled-" + bootanimation + ".zip "
                                    + themeDirectory.getAbsolutePath() + ((is_encrypted) ?
                                    "/bootanimation-encrypted.zip" : "/bootanimation.zip"));

                    Root.runCommand("mount -o rw,remount /system");
                    Root.runCommand("chmod 644 "
                            + themeDirectory.getAbsolutePath() + ((is_encrypted) ?
                            "/bootanimation-encrypted.zip" : "/bootanimation.zip"));

                    Root.runCommand("mount -o rw,remount /data");
                    Root.runCommand("chcon -R u:object_r:system_file:s0 " +
                            themeDirectory.getAbsolutePath());
                }
            }

            if (!has_failed) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("bootanimation_applied", theme_pid);
                editor.apply();
                Log.d("BootAnimationHandler", "Boot animation installed!");
                Root.runCommand(
                        "rm -r " + mContext.getCacheDir().getAbsolutePath() +
                                "/BootAnimationCache/AnimationCreator/");
            } else {
                Log.e("BootAnimationHandler", "Boot animation installation aborted!");
                Root.runCommand(
                        "rm -r " + mContext.getCacheDir().getAbsolutePath() +
                                "/BootAnimationCache/AnimationCreator/");
            }
            return null;
        }

        private void CopyStream(InputStream Input, OutputStream Output) throws IOException {
            byte[] buffer = new byte[5120];
            int length = Input.read(buffer);
            while (length > 0) {
                Output.write(buffer, 0, length);
                length = Input.read(buffer);
            }
        }
    }
}