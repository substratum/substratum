/*
 * Copyright (c) 2016-2017 Projekt Substratum
 * This file is part of Substratum.
 *
 * Substratum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Substratum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Substratum.  If not, see <http://www.gnu.org/licenses/>.
 */

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