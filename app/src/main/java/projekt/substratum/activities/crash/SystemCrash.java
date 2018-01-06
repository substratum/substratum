package projekt.substratum.activities.crash;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;

import projekt.substratum.LaunchActivity;
import projekt.substratum.R;
import projekt.substratum.common.Activities;

public class SystemCrash extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.system_crash_dialog);
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler();
        Runnable r = () -> {
            Activities.launchInternalActivity(getApplicationContext(), LaunchActivity.class);
            finishAffinity();
        };
        handler.postDelayed(r, 3000);
    }
}