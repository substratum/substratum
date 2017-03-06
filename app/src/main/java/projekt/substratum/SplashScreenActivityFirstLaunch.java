package projekt.substratum;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import projekt.substratum.config.References;

public class SplashScreenActivityFirstLaunch extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splashscreen_layout);

        // Load the ImageView that will host the animation and
        // set its background to our AnimationDrawable XML resource.

        try {
            ImageView img = (ImageView) findViewById(R.id.splashscreen_image);
            img.setImageDrawable(getDrawable(R.drawable.splashscreen_intro));

            // Get the background, which has been compiled to an AnimationDrawable object.
            AnimationDrawable frameAnimation = (AnimationDrawable) img.getDrawable();

            // Start the animation
            frameAnimation.setOneShot(true);
            frameAnimation.run();
        } catch (OutOfMemoryError oome) {
            Log.e(References.SUBSTRATUM_LOG, "The VM has been blown up and the rendering of " +
                    "the splash screen animated icon has been cancelled.");
        }

        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            Intent intent = new Intent(SplashScreenActivityFirstLaunch.this, AppIntroActivity
                    .class);
            startActivity(intent);
            finish();
        }, 2300);
    }
}