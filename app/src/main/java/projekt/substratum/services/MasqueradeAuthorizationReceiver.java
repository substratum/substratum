package projekt.substratum.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import projekt.substratum.R;

public class MasqueradeAuthorizationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getBooleanExtra("isCallerAuthorized", false)) {
            Toast toast = Toast.makeText(context,
                    context.getString(R.string.masquerade_not_authorized_toast), Toast.LENGTH_LONG);
            toast.show();
        }
    }

}
