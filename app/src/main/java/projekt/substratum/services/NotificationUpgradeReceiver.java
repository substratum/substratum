package projekt.substratum.services;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import projekt.substratum.R;
import projekt.substratum.config.References;

public class NotificationUpgradeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(References.notification_id_upgrade);
        SharedPreferences prefs = context.getSharedPreferences(
                "substratum_state", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        Toast toast = Toast.makeText(context, context.getString(R.string
                        .background_updated_toast_cleaning),
                Toast.LENGTH_LONG);
        toast.show();
    }
}