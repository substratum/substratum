package projekt.substratum.services;

import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import projekt.substratum.config.References;

public class ThemeUninstallDetector extends BroadcastReceiver {

    public static int getDeviceEncryptionStatus(Context context) {
        // 0: ENCRYPTION_STATUS_UNSUPPORTED
        // 1: ENCRYPTION_STATUS_INACTIVE
        // 2: ENCRYPTION_STATUS_ACTIVATING
        // 3: ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY
        // 4: ENCRYPTION_STATUS_ACTIVE
        // 5: ENCRYPTION_STATUS_ACTIVE_PER_USER
        int status = DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED;
        final DevicePolicyManager dpm = (DevicePolicyManager)
                context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm != null) {
            status = dpm.getStorageEncryptionStatus();
        }
        return status;
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    private void copyAssets(Context context) {
        AssetManager assetManager = context.getAssets();
        final String filename = "fonts.xml";
        try (InputStream in = assetManager.open(filename);
             OutputStream out = new FileOutputStream(context.getCacheDir()
                     .getAbsolutePath() +
                     "/FontCache/FontCreator/" + filename)) {
            copyFile(in, out);
        } catch (IOException e) {
            Log.e("FontHandler", "Failed to move font configuration file to working " +
                    "directory!");
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.PACKAGE_REMOVED".equals(intent.getAction())) {

            Uri packageName = intent.getData();
            String package_name = packageName.toString().substring(8);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (prefs.contains("installed_themes")) {
                Set installed_themes = prefs.getStringSet("installed_themes", null);
                if (installed_themes != null && installed_themes.contains(package_name)) {
                    Log.d(References.SUBSTRATUM_LOG, "Now purging caches for \"" + package_name +
                            "\"...");
                    References.delete(context.getCacheDir().getAbsolutePath() +
                            "/SubstratumBuilder/" + package_name + "/");

                    final SharedPreferences.Editor editor = prefs.edit();
                    if (prefs.getString("sounds_applied", "").equals(package_name)) {
                        References.delete("/data/system/theme/audio/ && pkill -f com" +
                                ".android.systemui");
                        editor.remove("sounds_applied");
                    }
                    if (prefs.getString("fonts_applied", "").equals(package_name)) {
                        int version = References.checkOMSVersion(context);
                        if (version == 3) {
                            References.delete("/data/system/theme/fonts/");
                            References.runCommands(References.refreshWindows());
                        } else if (version == 7) {
                            References.delete("/data/system/theme/fonts/");
                            References.mountRWData();
                            References.copyDir("/system/fonts/", "/data/system/theme/");
                            copyAssets(context);
                            References.move(context.getCacheDir().getAbsolutePath() +
                                            "/FontCache/FontCreator/fonts.xml",
                                    "/data/system/theme/fonts/");

                            // Check for correct permissions and system file context integrity.
                            References.setPermissions(755, "/data/system/theme/");
                            References.setPermissionsRecursively(747, "/data/system/theme/fonts/");
                            References.setPermissions(775, "/data/system/theme/fonts/");
                            References.setContext("/data/system/theme");
                            References.setProp("sys.refresh_theme", "1");
                            References.mountROData();
                        } else if (version == 0) {
                            References.delete("/data/system/theme/fonts/");
                        }
                        if (!prefs.getBoolean("systemui_recreate", false)) {
                            if (References.isPackageInstalled(context, "masquerade.substratum")) {
                                Intent runCommand = new Intent();
                                runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                                runCommand.setAction("masquerade.substratum.COMMANDS");
                                runCommand.putExtra("om-commands", "pkill -f com.android.systemui");
                                context.sendBroadcast(runCommand);
                            } else {
                                References.restartSystemUI();
                            }
                        }
                        editor.remove("fonts_applied");
                    }
                    if (prefs.getString("bootanimation_applied", "").equals(package_name)) {
                        if (getDeviceEncryptionStatus(context) <= 1 && References.checkOMS(
                                context)) {
                            References.delete("/data/system/theme/bootanimation.zip");
                        } else {
                            References.mountRW();
                            References.move("/system/media/bootanimation-backup.zip",
                                    "/system/media/bootanimation.zip");
                            References.delete("/system/addon.d/81-subsboot.sh");
                        }
                        editor.remove("bootanimation_applied");
                    }
                    WallpaperManager wm = WallpaperManager.getInstance(
                            context);
                    if (prefs.getString("home_wallpaper_applied", "").equals(package_name)) {
                        try {
                            wm.clear();
                            editor.remove("home_wallpaper_applied");
                        } catch (IOException e) {
                            Log.e("InformationActivity", "Failed to restore home screen " +
                                    "wallpaper!");
                        }
                    }
                    if (prefs.getString("lock_wallpaper_applied", "").equals(package_name)) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                wm.clear(WallpaperManager.FLAG_LOCK);
                                editor.remove("lock_wallpaper_applied");
                            }
                        } catch (IOException e) {
                            Log.e("InformationActivity", "Failed to restore lock screen " +
                                    "wallpaper!");
                        }
                    }
                    editor.apply();
                }
            }
        }
    }
}