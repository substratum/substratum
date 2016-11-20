package projekt.substratum.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import projekt.substratum.R;
import projekt.substratum.config.References;

public class FontHandler {

    private Context mContext;
    private ProgressDialog progress;
    private String theme_pid;
    private SharedPreferences prefs;

    public void execute(String arguments, Context context, String theme_pid) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.mContext = context;
        this.theme_pid = theme_pid;
        new FontHandlerAsync().execute(arguments);
    }

    private boolean checkChangeConfigurationPermissions() {
        String permission = "android.permission.CHANGE_CONFIGURATION";
        int res = mContext.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    private class FontHandlerAsync extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            if (!checkChangeConfigurationPermissions()) {
                Log.e("FontHandler", "Substratum was not granted " +
                        "CHANGE_CONFIGURATION permissions, allowing now...");
                References.grantPermission("projekt.substratum",
                        "android.permission.CHANGE_CONFIGURATION");
            } else {
                Log.d("FontHandler", "Substratum was granted CHANGE_CONFIGURATION permissions!");
            }
            progress = new ProgressDialog(mContext, R.style.AppTheme_DialogAlert);
            progress.setMessage(mContext.getString(R.string.font_dialog_apply_text));
            progress.setIndeterminate(false);
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected void onPostExecute(String result) {
            progress.dismiss();
            if (result == null) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("fonts_applied", theme_pid);
                editor.apply();
                Toast toast = Toast.makeText(mContext,
                        mContext.getString(R.string.font_dialog_apply_success), Toast.LENGTH_LONG);
                toast.show();
            } else {
                Toast toast = Toast.makeText(mContext,
                        mContext.getString(R.string.font_dialog_apply_failed), Toast.LENGTH_LONG);
                toast.show();
            }

            if (result == null) {
                // Finally, refresh the window
                String final_commands = "pkill -f com.android.systemui";
                if (References.checkOMS(mContext)) {
                    try {
                        float fontSize = Float.valueOf(Settings.System.getString(
                                mContext.getContentResolver(), Settings.System.FONT_SCALE));
                        Settings.System.putString(mContext.getContentResolver(),
                                Settings.System.FONT_SCALE, String.valueOf(fontSize + 0.0000001));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (References.checkOMSVersion(mContext) == 3) {
                        if (!prefs.getBoolean("systemui_recreate", false)) {
                            final_commands = " && pkill -f com.android.systemui";
                        } else {
                            final_commands = "";
                        }
                        if (References.isPackageInstalled(mContext, "masquerade.substratum")) {
                            Intent runCommand = new Intent();
                            runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                            runCommand.setAction("masquerade.substratum.COMMANDS");
                            runCommand.putExtra("om-commands", References.refreshWindows() +
                                    final_commands);
                            mContext.sendBroadcast(runCommand);
                        } else {
                            new References.ThreadRunner().execute(final_commands);
                        }
                    } else {
                        if (References.checkOMSVersion(mContext) == 7) {
                            new References.ThreadRunner().execute(final_commands);
                        }
                    }
                } else {
                    final AlertDialog.Builder alertDialogBuilder =
                            new AlertDialog.Builder(mContext);
                    alertDialogBuilder.setTitle(mContext.getString(
                            R.string.legacy_dialog_soft_reboot_title));
                    alertDialogBuilder.setMessage(mContext.getString(
                            R.string.legacy_dialog_soft_reboot_text));
                    alertDialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface
                            .OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            References.reboot();
                        }
                    });
                    alertDialogBuilder.setNegativeButton(
                            R.string.remove_dialog_later, new DialogInterface
                                    .OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialogBuilder.setCancelable(false);
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {

            try {
                // Move the file from assets folder to a new working area
                Log.d("FontHandler", "Copying over the selected fonts to working " +
                        "directory...");

                File cacheDirectory = new File(mContext.getCacheDir(), "/FontCache/");
                if (!cacheDirectory.exists()) {
                    boolean created = cacheDirectory.mkdirs();
                    if (created) Log.d("FontHandler", "Successfully created cache folder!");
                }
                File cacheDirectory2 = new File(mContext.getCacheDir(), "/FontCache/" +
                        "FontCreator/");
                if (!cacheDirectory2.exists()) {
                    boolean created = cacheDirectory2.mkdirs();
                    if (created) Log.d("FontHandler", "Successfully created cache folder work " +
                            "directory!");
                } else {
                    References.delete(mContext.getCacheDir().getAbsolutePath() +
                            "/FontCache/FontCreator/");
                    boolean created = cacheDirectory2.mkdirs();
                    if (created) Log.d("FontHandler", "Successfully recreated cache folder work " +
                            "directory!");
                }

                // Copy the font.zip from assets/fonts of the theme's assets
                String source = sUrl[0] + ".zip";

                try {
                    Context otherContext = mContext.createPackageContext(theme_pid, 0);
                    AssetManager am = otherContext.getAssets();
                    try (InputStream inputStream = am.open("fonts/" + source);
                         OutputStream outputStream = new FileOutputStream(mContext.getCacheDir()
                                 .getAbsolutePath() + "/FontCache/" + source)) {
                        copyStream(inputStream, outputStream);
                    }
                } catch (Exception e) {
                    Log.e("FontHandler", "There is no fonts.zip found within the assets " +
                            "of this theme!");
                }

                // Unzip the fonts to get it prepared for the preview
                unzip(mContext.getCacheDir().getAbsolutePath() +
                                "/FontCache/" + source,
                        mContext.getCacheDir().getAbsolutePath() +
                                "/FontCache/FontCreator/");

                // Copy all the system fonts to /data/system/theme/fonts
                File dataSystemThemeDir = new File("/data/system/theme");
                if (!dataSystemThemeDir.exists()) {
                    References.mountRWData();
                    References.createNewFolder("/data/system/theme/");
                }
                File dataSystemThemeFontsDir = new File("/data/system/theme/fonts");
                if (!dataSystemThemeFontsDir.exists()) {
                    References.mountRWData();
                    References.createNewFolder("/data/system/theme/fonts");
                } else {
                    References.delete("/data/system/theme/fonts/");
                    References.mountRWData();
                    References.createNewFolder("/data/system/theme/fonts");
                }

                // Copy font configuration file (fonts.xml) to the working directory
                File fontsConfig = new File(mContext.getCacheDir().getAbsolutePath() +
                        "/FontCache/FontCreator/fonts.xml");
                if (!fontsConfig.exists()) copyAssets();
                References.copy("/system/fonts/*", "/data/system/theme/fonts/");

                // Copy all the files from work directory to /data/system/theme/fonts
                References.copy(mContext.getCacheDir().getAbsolutePath() +
                        "/FontCache/FontCreator/*", "/data/system/theme/fonts/");

                // Check for correct permissions and system file context integrity.
                References.mountRWData();
                References.setPermissions(755, "/data/system/theme/");
                References.setPermissionsRecursively(747, "/data/system/theme/fonts/");
                References.setPermissions(775, "/data/system/theme/fonts/");
                References.mountROData();
                References.setContext("/data/system/theme");
                References.setProp("sys.refresh_theme", "1");
            } catch (Exception e) {
                e.printStackTrace();
                return "failed";
            }
            return null;
        }

        private void copyAssets() {
            AssetManager assetManager = mContext.getAssets();
            final String filename = "fonts.xml";
            try (InputStream in = assetManager.open(filename);
                 OutputStream out = new FileOutputStream(mContext.getCacheDir().getAbsolutePath() +
                         "/FontCache/FontCreator/" + filename)) {
                copyFile(in, out);
            } catch (IOException e) {
                Log.e("FontHandler", "Failed to move font configuration file to working " +
                        "directory!");
            }
        }

        private void copyFile(InputStream in, OutputStream out) throws IOException {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }

        private void unzip(String source, String destination) {
            try (ZipInputStream inputStream = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(source)))) {
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
                    try (FileOutputStream outputStream = new FileOutputStream(file)) {
                        while ((count = inputStream.read(buffer)) != -1)
                            outputStream.write(buffer, 0, count);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("FontHandler",
                        "An issue has occurred while attempting to decompress this archive.");
            }
        }

        private void copyStream(InputStream Input, OutputStream Output) throws IOException {
            byte[] buffer = new byte[5120];
            int length = Input.read(buffer);
            while (length > 0) {
                Output.write(buffer, 0, length);
                length = Input.read(buffer);
            }
        }
    }
}