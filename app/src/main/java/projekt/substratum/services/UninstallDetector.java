package projekt.substratum.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import projekt.substratum.util.AntiPiracyCheck;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class UninstallDetector extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("SubstratumAntiPiracy",
                "Detected an application removal, rechecking Substratum system integrity...");
        new AntiPiracyCheck().AntiPiracyCheck(context);
    }
}