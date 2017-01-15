package projekt.substratum.services;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import static projekt.substratum.fragments.ProfileFragment.SCHEDULED_PROFILE_TYPE_EXTRA;

public class ScheduledProfileReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final String extra = intent.getStringExtra(SCHEDULED_PROFILE_TYPE_EXTRA);
        Log.d("ScheduledProfile", extra + " profile will be applied.");
        Intent service = new Intent(context, ScheduledProfileService.class);
        service.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, extra);
        startWakefulService(context, service);
    }
}
