package projekt.substratum.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

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
                if (installed_themes.contains(package_name)) {
                    Log.d("SubstratumLogger", "Now purging caches for \"" + package_name + "\"...");
                    References.delete(context.getCacheDir().getAbsolutePath() +
                            "/SubstratumBuilder/" + package_name + "/");
                }
            }
        }
    }
}