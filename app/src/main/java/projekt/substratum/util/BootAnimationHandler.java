package projekt.substratum.util;

import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

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
import java.util.Locale;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import projekt.substratum.R;
import projekt.substratum.config.MasqueradeService;
import projekt.substratum.config.References;

public class BootAnimationHandler {

    private Context mContext;
    private ProgressDialog progress;
    private Boolean has_failed;
    private String theme_pid;
    private SharedPreferences prefs;
    private View view;

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

    public void execute(View view, String arguments, Context context, String theme_pid) {
        this.mContext = context;
        this.theme_pid = theme_pid;
        this.view = view;
        new BootAnimationHandlerAsync().execute(arguments);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private class BootAnimationHandlerAsync extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(mContext, R.style.AppTheme_DialogAlert);
            progress.setMessage(mContext.getString(R.string.bootanimation_dialog_apply_text));
            progress.setIndeterminate(false);
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected void onPostExecute(String result) {
            progress.dismiss();

            if (!has_failed) {
                Snackbar.make(view,
                        mContext.getString(R.string.bootanimation_dialog_apply_success),
                        Snackbar.LENGTH_LONG)
                        .show();
            } else {
                Snackbar.make(view,
                        mContext.getString(R.string.bootanimation_dialog_apply_failed),
                        Snackbar.LENGTH_LONG)
                        .show();
            }
            References.mountROData();
            References.mountRO();
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
                References.delete(mContext.getCacheDir().getAbsolutePath() +
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
                            " assets of this theme!");
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
                            int prevent_lint_w = dm.widthPixels;
                            int prevent_lint_h = dm.heightPixels;
                            if (dm.widthPixels > dm.heightPixels) {
                                scaledWidth = prevent_lint_h;
                                scaledHeight = prevent_lint_w;
                            } else {
                                scaledWidth = dm.widthPixels;
                                scaledHeight = dm.heightPixels;
                            }

                            int width = Integer.parseInt(info[0]);
                            int height = Integer.parseInt(info[1]);

                            int prevent_lint = scaledWidth;
                            if (width == height) {
                                scaledHeight = prevent_lint;
                            } else {
                                // adjust scaledHeight to retain original aspect ratio
                                float scale = (float) scaledWidth / (float) width;
                                int newHeight = (int) ((float) height * scale);
                                if (newHeight < scaledHeight)
                                    scaledHeight = newHeight;
                            }

                            CRC32 crc32 = new CRC32();
                            int size = 0;
                            ByteBuffer buffer = ByteBuffer.wrap(bytes);
                            line = String.format(Locale.getDefault(),
                                    "%d %d %s\n", scaledWidth, scaledHeight, info[2]);
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
                if (References.checkOMS(mContext)) {
                    if (References.checkMasquerade(mContext) <= 20) {
                        if (getDeviceEncryptionStatus() <= 1) {
                            Log.d("BootAnimationHandler", "Data partition on the current device " +
                                    "is decrypted, using dedicated theme bootanimation slot...");
                            themeDirectory = new File("/data/system/theme/");
                            if (!themeDirectory.exists()) {
                                References.mountRWData();
                                References.createNewFolder("/data/system/theme/");
                            }
                        } else {
                            Log.d("BootAnimationHandler", "Data partition on the current device " +
                                    "is encrypted, using dedicated encrypted bootanimation " +
                                    "slot...");
                            is_encrypted = true;
                            themeDirectory = new File("/system/media/");
                        }
                    } else {
                        themeDirectory = null;
                    }
                } else {
                    Log.d("BootAnimationHandler", "Current device is on substratum legacy, " +
                            "using system bootanimation slot...");
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

                if (References.checkMasquerade(mContext) >= 21) {
                    References.move((mContext.getCacheDir()
                            .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/" + "scaled-"
                            + bootanimation + ".zip"), Environment.getExternalStorageDirectory()
                            .getAbsolutePath() + "/.substratum/bootanimation.zip");
                }

                if (!has_failed && (is_encrypted || !References.checkOMS(mContext))) {
                    References.mountRW();
                    File backupScript = new File("/system/addon.d/81-subsboot.sh");

                    if (!backupScript.exists()) {
                        AssetManager assetManager = mContext.getAssets();
                        String backupScriptPath = mContext.getFilesDir().getAbsolutePath() +
                                "/81-subsboot.sh";
                        OutputStream out = null;
                        InputStream in = null;
                        try {
                            out = new FileOutputStream(backupScriptPath);
                            in = assetManager.open("81-subsboot.sh");
                            byte[] buffer = new byte[1024];
                            int read;
                            while ((read = in.read(buffer)) != -1) {
                                out.write(buffer, 0, read);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            if (in != null) {
                                try {
                                    in.close();
                                } catch (IOException e) {
                                    // Suppress warning
                                }
                            }
                            if (out != null) {
                                try {
                                    out.close();
                                } catch (IOException e) {
                                    // Suppress warning
                                }
                            }
                        }
                        References.copy(mContext.getFilesDir().getAbsolutePath() +
                                "/81-subsboot.sh", backupScript.getAbsolutePath());
                        References.setPermissions(755, backupScript.getAbsolutePath());
                    }

                    File backupDirectory = new File(themeDirectory.getAbsolutePath() +
                            "/bootanimation-backup.zip");
                    if (!backupDirectory.exists()) {
                        References.move(themeDirectory.getAbsolutePath()
                                + "/bootanimation.zip", backupDirectory.getAbsolutePath());
                    }

                    File bootAnimationCheck = new File(themeDirectory.getAbsolutePath() +
                            "/bootanimation.zip");

                    if (backupDirectory.exists() && backupScript.exists()) {
                        Log.d("BootAnimationHandler", "Old bootanimation is backed up, ready to " +
                                "go!");
                    } else if (!bootAnimationCheck.exists() && !backupDirectory.exists()) {
                        Log.d("BootAnimationHandler", "There is no predefined bootanimation on " +
                                "this device, injecting a brand new default bootanimation...");
                    } else {
                        Log.e("BootAnimationHandler", "Failed to backup bootanimation!");
                        has_failed = true;
                    }
                }

                if (!has_failed) {
                    if (References.checkMasquerade(mContext) >= 21) {
                        Log.d("BootAnimationHandler",
                                "No-root option has been enabled with the inclusion of " +
                                        "masquerade v21+...");
                        MasqueradeService.setBootAnimation(mContext,
                                Environment.getExternalStorageDirectory().getAbsolutePath() +
                                        "/.substratum/bootanimation.zip");
                    } else {
                        References.mountRW();
                        References.setPermissions(755, themeDirectory.getAbsolutePath());
                        References.mountRW();
                        References.move(mContext.getCacheDir()
                                        .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/"
                                        + "scaled-" + bootanimation + ".zip",
                                themeDirectory.getAbsolutePath() + "/bootanimation.zip");
                        References.mountRW();
                        References.setPermissions(644, themeDirectory.getAbsolutePath() +
                                "/bootanimation.zip");
                        References.mountRWData();
                        References.setContext(themeDirectory.getAbsolutePath());
                    }
                }
            }

            if (!has_failed) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("bootanimation_applied", theme_pid);
                editor.apply();
                Log.d("BootAnimationHandler", "Boot animation installed!");
                // TODO: Remove rm -rf call to delete our own cache file
                References.delete(mContext.getCacheDir().getAbsolutePath() +
                        "/BootAnimationCache/AnimationCreator/");
            } else {
                Log.e("BootAnimationHandler", "Boot animation installation aborted!");
                References.delete(mContext.getCacheDir().getAbsolutePath() +
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