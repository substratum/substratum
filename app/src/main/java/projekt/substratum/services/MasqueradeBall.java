package projekt.substratum.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import projekt.substratum.R;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class MasqueradeBall extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getStringExtra("substratum-check") != null) {
            if (intent.getStringExtra("substratum-check").equals("masquerade-ball")) {
                Toast toast = Toast.makeText(context, context.getString(R.string
                                .masquerade_check_installed),
                        Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }
}
