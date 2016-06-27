package projekt.substratum;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import projekt.substratum.util.Root;

/**
 * @author Nicholas Chum (nicholaschum)
 */
public class SplashScreenActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splashscreen_layout);
        Root.requestRootAccess();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, 600);
    }
}