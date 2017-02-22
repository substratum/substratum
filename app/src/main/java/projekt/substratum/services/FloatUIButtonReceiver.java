package projekt.substratum.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class FloatUIButtonReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                context);
        if (prefs.getBoolean("floatui_show_android_system_overlays", true)) {
            prefs.edit().putBoolean("floatui_show_android_system_overlays", false).apply();
        } else {
            prefs.edit().putBoolean("floatui_show_android_system_overlays", true).apply();
        }
        Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.getApplicationContext().sendBroadcast(closeDialog);
        context.stopService(new Intent(context, SubstratumFloatInterface.class));
        context.startService(new Intent(context, SubstratumFloatInterface.class));
    }
}