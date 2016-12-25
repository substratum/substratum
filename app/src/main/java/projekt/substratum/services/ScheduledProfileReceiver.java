package projekt.substratum.services;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import projekt.substratum.config.References;


public class ScheduledProfileReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final String extra = intent.getStringExtra("type");
        Log.d(References.SUBSTRATUM_LOG, extra + " profile will be applied.");
        Intent service = new Intent(context, ScheduledProfileService.class);
        service.putExtra("type", extra);
        startWakefulService(context, service);
    }
}
