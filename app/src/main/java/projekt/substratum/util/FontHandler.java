package projekt.substratum.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import kellinwood.security.zipsigner.ZipSigner;
import projekt.substratum.R;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class FontHandler {

    private Context mContext;
    private ProgressDialog progress;
    private String theme_pid;
    private String final_commands;
    private SharedPreferences prefs;

    public void FontHandler(String arguments, Context context, String theme_pid) {
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

    private boolean isPackageInstalled(Context context, String package_name) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(package_name, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private class FontHandlerAsync extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            if (!checkChangeConfigurationPermissions()) {
                Log.e("FontHandler", "Substratum was not granted " +
                        "CHANGE_CONFIGURATION permissions, allowing now...");
                Root.runCommand("pm grant projekt.substratum " +
                        "android.permission.CHANGE_CONFIGURATION");
            } else {
                Log.d("FontHandler", "Substratum was granted CHANGE_CONFIGURATION permissions!");
            }
            progress = new ProgressDialog(mContext, android.R.style
                    .Theme_DeviceDefault_Dialog_Alert);
            progress.setMessage(mContext.getString(R.string.font_dialog_apply_text));
            progress.setIndeterminate(false);
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected void onPostExecute(String result) {
            progress.dismiss();
            if (result == null) {
                Toast toast = Toast.makeText(mContext,
                        mContext.getString(R.string.font_dialog_apply_success), Toast.LENGTH_LONG);
                toast.show();
            } else {
                Toast toast = Toast.makeText(mContext,
                        mContext.getString(R.string.font_dialog_apply_failed), Toast.LENGTH_LONG);
                toast.show();
            }

            if (final_commands.length() > 0 && result == null) {

                // Reflect back to Settings and updateConfiguration() run on simulated locale change

                try {
                    Class<?> activityManagerNative = Class.forName("android.app" +
                            ".ActivityManagerNative");
                    Object am = activityManagerNative.getMethod("getDefault").invoke
                            (activityManagerNative);
                    Object config = am.getClass().getMethod("getConfiguration").invoke(am);
                    am.getClass().getMethod("updateConfiguration", android.content.res
                            .Configuration.class).invoke(am, config);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Finally, enable/disable the SystemUI dummy overlay

                if (isPackageInstalled(mContext, "projekt.substratum.helper")) {
                    if (!prefs.getBoolean("systemui_recreate", false)) {
                        final_commands = final_commands + " && pkill com.android.systemui";
                    }
                    Intent runCommand = new Intent();
                    runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    runCommand.setAction("masquerade.substratum.COMMANDS");
                    runCommand.putExtra("om-commands", final_commands);
                    mContext.sendBroadcast(runCommand);
                } else {
                    Root.runCommand(final_commands);
                    if (!prefs.getBoolean("systemui_recreate", false)) {
                        Root.runCommand("pkill com.android.systemui");
                    }
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
                    Root.runCommand(
                            "rm -r " + mContext.getCacheDir().getAbsolutePath() +
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
                    InputStream inputStream = am.open("fonts/" + source);
                    OutputStream outputStream = new FileOutputStream(mContext.getCacheDir()
                            .getAbsolutePath() + "/FontCache/" + source);
                    CopyStream(inputStream, outputStream);
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
                    Root.runCommand("mount -o rw,remount /data");
                    Root.runCommand(
                            "mkdir /data/system/theme/");
                }
                File dataSystemThemeFontsDir = new File("/data/system/theme/fonts");
                if (!dataSystemThemeFontsDir.exists()) {
                    Root.runCommand("mount -o rw,remount /data");
                    Root.runCommand(
                            "mkdir /data/system/theme/fonts");
                } else {
                    Root.runCommand(
                            "rm -r /data/system/theme/fonts/");
                    Root.runCommand("mount -o rw,remount /data");
                    Root.runCommand(
                            "mkdir /data/system/theme/fonts");
                }

                // Copy font configuration file (fonts.xml) to the working directory
                File fontsConfig = new File(mContext.getCacheDir().getAbsolutePath() +
                        "/FontCache/FontCreator/fonts.xml");
                if (!fontsConfig.exists()) copyAssets();

                Root.runCommand(
                        "cp /system/fonts/* /data/system/theme/fonts/");

                // Copy all the files from work directory to /data/system/theme/fonts

                Root.runCommand(
                        "cp -F " + mContext.getCacheDir().getAbsolutePath() +
                                "/FontCache/FontCreator/*" + " /data/system/theme/fonts/");

                // Check if substratum_helper overlay installed, if not, create it

                if (!isPackageInstalled(mContext, "substratum.helper")) {
                    Log.e("FontHandler", "Substratum Helper Overlay not installed, compiling and " +
                            "installing now...");

                    File helperDirectory = new File(mContext.getCacheDir(),
                            "/FontCache/helper/");

                    if (!helperDirectory.exists()) {
                        boolean created = helperDirectory.mkdirs();
                        if (created) Log.d("FontHandler", "Helper folder created");
                    }
                    File helperDirectoryRes = new File(mContext.getCacheDir(),
                            "/FontCache/helper/res/");
                    if (!helperDirectoryRes.exists()) {
                        boolean created = helperDirectoryRes.mkdirs();
                        if (created) Log.d("FontHandler", "Helper resources folder created");
                    }
                    File helperDirectoryResDrawable =
                            new File(mContext.getCacheDir(),
                                    "/FontCache/helper/res/drawable-xxhdpi/");
                    if (!helperDirectoryResDrawable.exists()) {
                        boolean created = helperDirectoryResDrawable.mkdirs();
                        if (created) Log.d("FontHandler", "Helper drawable folder created");
                    }
                    File helperDirectoryResValues =
                            new File(mContext.getCacheDir(), "/FontCache/helper/res/values/");
                    if (!helperDirectoryResValues.exists()) {
                        boolean created = helperDirectoryResValues.mkdirs();
                        if (created) Log.d("FontHandler", "Helper values folder created");
                    }

                    // Create new icon in /res/drawable-xxhdpi from Substratum's mipmap-xxhdpi
                    Resources res;
                    Drawable hero = null;
                    try {
                        res = mContext.getPackageManager().getResourcesForApplication(
                                mContext.getApplicationContext().getPackageName());
                        int resourceId = res.getIdentifier(mContext.getApplicationContext()
                                .getPackageName() + ":mipmap/main_launcher", null, null);
                        if (0 != resourceId) {
                            hero = mContext.getPackageManager().getDrawable(mContext
                                    .getApplicationContext().getPackageName(), resourceId, null);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }

                    try {
                        Bitmap bmp = ((BitmapDrawable) hero).getBitmap();
                        FileOutputStream out = null;
                        try {
                            out = new FileOutputStream(new File(mContext.getCacheDir(),
                                    "/FontCache/helper/res/drawable-xxhdpi/app_icon.png"));
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                if (out != null) {
                                    out.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (NullPointerException npe) {
                        Log.e("FontHandler", "Could not create bitmap drawable of app icon!");
                    }

                    // Create new EXISTING boolean file based on MM6.0+ to surpass non-dangerous
                    // overlay

                    File root = new File(mContext.getCacheDir(),
                            "/FontCache/helper/res/values/bools.xml");
                    try {
                        boolean created = root.createNewFile();
                        if (created) Log.d("FontHandler", "Allowing handler overlay take on " +
                                "resources from SystemUI");
                        FileWriter fw = new FileWriter(root);
                        BufferedWriter bw = new BufferedWriter(fw);
                        PrintWriter pw = new PrintWriter(bw);
                        String string =
                                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                        "<resources>\n" +
                                        "        <bool name=\"enable_volume_ui\">true</bool>\n" +
                                        "</resources>";
                        pw.write(string);
                        pw.close();
                        bw.close();
                        fw.close();
                    } catch (Exception e) {
                        Log.e("FontHandler", "There was an exception creating a new bools file!");
                    }

                    // Create dummy AndroidManifest

                    File root2 = new File(mContext.getCacheDir(),
                            "/FontCache/helper/AndroidManifest.xml");
                    try {
                        boolean created = root2.createNewFile();
                        if (created) Log.d("FontHandler", "AndroidManifest file created");
                        FileWriter fw2 = new FileWriter(root2);
                        BufferedWriter bw2 = new BufferedWriter(fw2);
                        PrintWriter pw2 = new PrintWriter(bw2);

                        PackageInfo pInfo = mContext.getPackageManager()
                                .getPackageInfo(mContext.getPackageName(), 0);
                        String version = pInfo.versionName;

                        String manifest =
                                "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n" +
                                        "<manifest xmlns:android=\"http://schemas.android" +
                                        ".com/apk/res/android\" package=\"" +
                                        "substratum.helper" + "\"\n" +
                                        "        android:versionName=\"" + version + "\"> \n" +
                                        "    <overlay android:targetPackage=\"" +
                                        "com.android.systemui" +
                                        "\"/>\n" +
                                        "    <application android:label=\"" +
                                        mContext.getString(R.string.helper_name) + "\"\n" +
                                        "         android:icon=\"@drawable/app_icon\"/>\n" +
                                        "</manifest>\n";
                        pw2.write(manifest);
                        pw2.close();
                        bw2.close();
                        fw2.close();
                    } catch (Exception e) {
                        Log.e("FontHandler", "There was an exception creating a new Manifest " +
                                "file!");
                    }

                    // Begin aapt and compile the temp file

                    try {
                        String commands = "aapt p -M " + mContext.getCacheDir()
                                .getAbsolutePath() +

                                "/FontCache/helper/AndroidManifest.xml " +
                                "-S " + mContext.getCacheDir().getAbsolutePath() +
                                "/FontCache/helper/res/ " +
                                "-I " + "/system/framework/framework-res.apk " +
                                "-F " + mContext.getCacheDir().getAbsolutePath() +
                                "/FontCache/helper/unsigned_helper.apk " +
                                "-f\n";

                        String line;
                        Process nativeApp = Runtime.getRuntime().exec(commands);

                        OutputStream stdin = nativeApp.getOutputStream();
                        InputStream stderr = nativeApp.getErrorStream();
                        InputStream stdout = nativeApp.getInputStream();
                        stdin.write(("ls\n").getBytes());
                        stdin.write("exit\n".getBytes());
                        stdin.flush();
                        stdin.close();

                        BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
                        while ((line = br.readLine()) != null) {
                            Log.d("OverlayOptimizer", line);
                        }
                        br.close();
                        br = new BufferedReader(new InputStreamReader(stderr));
                        while ((line = br.readLine()) != null) {
                            Log.e("SubstratumBuilder", line);
                        }
                        br.close();
                        nativeApp.waitFor();
                    } catch (Exception e) {
                        Log.e("FontHandler", "Unfortunately, there was an exception trying to " +
                                "create the Substratum Overlay Helper " +
                                "APK");
                    }

                    // Sign the helper overlay

                    try {
                        // Sign with the built-in test key/certificate.
                        String sign_source = mContext.getCacheDir().getAbsolutePath() +
                                "/FontCache/helper/unsigned_helper.apk";
                        String sign_destination = Environment.getExternalStorageDirectory()
                                .getAbsolutePath() +
                                "/.substratum/" + "substratum.helper" + "-signed.apk";

                        ZipSigner zipSigner = new ZipSigner();
                        zipSigner.setKeymode("testkey");
                        zipSigner.signZip(sign_source, sign_destination);

                        Log.d("FontHandler", "Substratum Helper Overlay APK successfully signed!");
                    } catch (Throwable t) {
                        t.printStackTrace();
                        Log.e("FontHandler", "Substratum Helper Overlay APK could not be signed. " +
                                t.toString());
                    }

                    // Install the Helper

                    Root.runCommand(
                            "pm install -r " + Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() + "/.substratum/substratum.helper-signed" +
                                    ".apk");

                    // Bruteforce the cache folder out of here due to possible MAJOR access
                    // permissions denial

                    Root.runCommand(
                            "rm -r " + mContext.getCacheDir().getAbsolutePath() +
                                    "/FontCache/");

                    final_commands = "om enable substratum.helper";
                } else {
                    File current_overlays = new File(Environment
                            .getExternalStorageDirectory().getAbsolutePath() +
                            "/.substratum/current_overlays.xml");
                    if (current_overlays.exists()) {
                        Root.runCommand("rm " + Environment
                                .getExternalStorageDirectory().getAbsolutePath() +
                                "/.substratum/current_overlays.xml");
                    }
                    Root.runCommand("cp /data/system/overlays" +
                            ".xml " +
                            Environment
                                    .getExternalStorageDirectory().getAbsolutePath() +
                            "/.substratum/current_overlays.xml");

                    String[] commands0 = {Environment.getExternalStorageDirectory()
                            .getAbsolutePath() +
                            "/.substratum/current_overlays.xml", "4"};
                    String[] commands1 = {Environment.getExternalStorageDirectory()
                            .getAbsolutePath() +
                            "/.substratum/current_overlays.xml", "5"};

                    List<String> state4overlays = ReadOverlaysFile.main(commands0);
                    List<String> state5overlays = ReadOverlaysFile.main(commands1);

                    if (state4overlays.contains("substratum.helper")) {
                        final_commands = "om enable substratum.helper";
                    } else {
                        if (state5overlays.contains("substratum.helper")) {
                            final_commands = "om disable substratum.helper";
                        }
                    }
                }

                // Whether the user has the helper installed, we have to check for correct
                // permissions
                // and system file context integrity.

                Root.runCommand("mount -o rw,remount /system");
                Root.runCommand("chmod 755 /data/system/theme/");

                Root.runCommand("mount -o rw,remount /system");
                Root.runCommand("chmod -R 747 " +
                        "/data/system/theme/fonts/");

                Root.runCommand("mount -o rw,remount /system");
                Root.runCommand("chmod 775 " +
                        "/data/system/theme/fonts/");

                Root.runCommand("mount -o rw,remount /data");
                Root.runCommand("chcon -R u:object_r:system_file:s0 " +
                        "/data/system/theme");

                Root.runCommand("setprop sys.refresh_theme 1");
            } catch (Exception e) {
                e.printStackTrace();
                return "failed";
            }
            return null;
        }

        private void copyAssets() {
            AssetManager assetManager = mContext.getAssets();
            InputStream in;
            OutputStream out;
            String filename = "fonts.xml";
            try {
                in = assetManager.open(filename);
                out = new FileOutputStream(mContext.getCacheDir().getAbsolutePath() +
                        "/FontCache/FontCreator/" + filename);
                copyFile(in, out);
                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;
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
            try {
                ZipInputStream inputStream = new ZipInputStream(
                        new BufferedInputStream(new FileInputStream(source)));
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
                    FileOutputStream outputStream = new FileOutputStream(file);
                    while ((count = inputStream.read(buffer)) != -1)
                        outputStream.write(buffer, 0, count);
                    outputStream.close();
                }
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("FontHandler",
                        "An issue has occurred while attempting to decompress this archive.");
            }
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
