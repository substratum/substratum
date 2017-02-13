package projekt.substratum.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.util.Set;

import projekt.substratum.config.References;

public class ThemeUninstallDetector extends BroadcastReceiver {

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
                    References.delete(context, context.getCacheDir().getAbsolutePath() +
                            "/SubstratumBuilder/" + package_name + "/");

                    final SharedPreferences.Editor editor = prefs.edit();
                    if (prefs.getString("sounds_applied", "").equals(package_name)) {
                        References.clearSounds(context);
                        editor.remove("sounds_applied");
                    }
                    if (prefs.getString("fonts_applied", "").equals(package_name)) {
                        References.clearFonts(context);
                        editor.remove("fonts_applied");
                    }
                    if (prefs.getString("bootanimation_applied", "").equals(package_name)) {
                        References.clearBootAnimation(context);
                        editor.remove("bootanimation_applied");
                    }
                    if (prefs.getString("home_wallpaper_applied", "").equals(package_name)) {
                        try {
                            References.clearWallpaper(context, "home");
                            editor.remove("home_wallpaper_applied");
                        } catch (IOException e) {
                            Log.e("ThemeUninstallDetector", "Failed to restore home screen " +
                                    "wallpaper!");
                        }
                    }
                    if (prefs.getString("lock_wallpaper_applied", "").equals(package_name)) {
                        try {
                            References.clearWallpaper(context, "lock");
                        } catch (IOException e) {
                            Log.e("ThemeUninstallDetector", "Failed to restore lock screen " +
                                    "wallpaper!");
                        }
                    }
                    editor.apply();
                }
            }
        }
    }
}