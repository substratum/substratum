package projekt.substratum.config;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class MasqueradeService {

    private static final String MASQUERADE_TOKEN = "masquerade_token";
    private static final String JOB_TIME_KEY = "job_time_key";

    public static Intent getMasquerade(Context context) {
        Intent intent = new Intent();
        PendingIntent pending = PendingIntent.getActivity(context, 0, new Intent(), 0);
        intent.putExtra(MASQUERADE_TOKEN, pending);
        intent.putExtra(JOB_TIME_KEY, System.currentTimeMillis());
        return intent;
    }
}