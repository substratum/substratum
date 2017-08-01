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

package projekt.substratum.activities.launch;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.MainActivity;
import projekt.substratum.R;
import projekt.substratum.common.References;
import projekt.substratum.common.analytics.FirebaseAnalytics;
import projekt.substratum.util.files.MD5;

import static projekt.substratum.common.References.PLAY_STORE_PACKAGE_NAME;
import static projekt.substratum.common.References.SST_ADDON_PACKAGE;
import static projekt.substratum.common.Resources.ANDROID_STUDIO_DEBUG_KEYS;
import static projekt.substratum.common.analytics.FirebaseAnalytics.PACKAGES_PREFS;
import static projekt.substratum.common.analytics.PackageAnalytics.isLowEnd;

public class SplashScreenActivity extends Activity {

    private static final int DELAY_LAUNCH_MAIN_ACTIVITY = 600;
    private static final int DELAY_LAUNCH_APP_INTRO = 2300;
    private Intent intent;
    private MaterialProgressBar materialProgressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splashscreen_layout);

        materialProgressBar = findViewById(R.id.splash_progress);

        Intent currentIntent = getIntent();
        Boolean first_run = currentIntent.getBooleanExtra("first_run", false);
        intent = new Intent(SplashScreenActivity.this, MainActivity.class);
        int intent_launch_delay = DELAY_LAUNCH_MAIN_ACTIVITY;

        if (first_run && !isLowEnd()) {
            // Load the ImageView that will host the animation and
            // set its background to our AnimationDrawable XML resource.

            try {
                ImageView img = findViewById(R.id.splashscreen_image);
                img.setImageDrawable(getDrawable(R.drawable.splashscreen_intro));

                // Get the background, which has been compiled to an AnimationDrawable object.
                AnimationDrawable frameAnimation = (AnimationDrawable) img.getDrawable();

                // Start the animation
                frameAnimation.setOneShot(true);
                frameAnimation.run();

                // Finally set the proper launch activity and delay
                intent = new Intent(SplashScreenActivity.this, AppIntroActivity.class);
                intent_launch_delay = DELAY_LAUNCH_APP_INTRO;
            } catch (OutOfMemoryError oome) {
                Log.e(References.SUBSTRATUM_LOG, "The VM has been blown up and the rendering of " +
                        "the splash screen animated icon has been cancelled.");
            }
        }

        final Handler handler = new Handler();
        handler.postDelayed(() -> new CheckSamsung(this).execute(), intent_launch_delay);
    }

    private void launch() {
        startActivity(intent);
        finish();
    }

    static class CheckSamsung extends AsyncTask<Void, Void, Void> {
        private WeakReference<SplashScreenActivity> ref;
        private SharedPreferences prefs;
        private SharedPreferences.Editor editor;
        private KeyRetrieval keyRetrieval;
        private Intent securityIntent;
        private Handler handler = new Handler();
        private Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (securityIntent != null) {
                    handler.removeCallbacks(runnable);
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    handler.postDelayed(this, 100);
                }
            }
        };

        CheckSamsung(SplashScreenActivity activity) {
            ref = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            SplashScreenActivity activity = ref.get();
            if (References.isSamsungDevice(activity) &&
                    References.isPackageInstalled(activity, SST_ADDON_PACKAGE)) {
                activity.materialProgressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Context context = ref.get().getApplicationContext();
            prefs = context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
            editor = prefs.edit();
            editor.clear().apply();

            FirebaseAnalytics.withdrawBlacklistedPackages(context);
            prefs = context.getSharedPreferences(PACKAGES_PREFS, Context.MODE_PRIVATE);
            SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy", Locale.US);
            while (!prefs.contains(dateFormat.format(new Date()))) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (!References.isSamsungDevice(context) ||
                    !References.isPackageInstalled(context, SST_ADDON_PACKAGE)) {
                return null;
            }

            FirebaseAnalytics.withdrawSungstratumFingerprint(context,
                    References.grabAppVersionCode(context, SST_ADDON_PACKAGE));
            while (!prefs.contains("sungstratum_exp_fp")) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            keyRetrieval = new KeyRetrieval();
            IntentFilter filter = new IntentFilter("projekt.substratum.PASS");
            context.getApplicationContext().registerReceiver(keyRetrieval, filter);

            Intent intent = new Intent("projekt.substratum.AUTHENTICATE");
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }

            int counter = 0;
            handler.postDelayed(runnable, 100);
            while (securityIntent == null && counter < 5) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                counter++;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void voids) {
            ref.get().launch();
        }

        @SuppressWarnings("ConstantConditions")
        class KeyRetrieval extends BroadcastReceiver {
            @SuppressWarnings("UnusedAssignment")
            @Override
            public void onReceive(Context context, Intent intent) {
                securityIntent = intent;
                context.getApplicationContext().unregisterReceiver(keyRetrieval);
                if (securityIntent != null) {
                    int hash = securityIntent.getIntExtra("app_hash", 0);
                    boolean debug = securityIntent.getBooleanExtra("app_debug", true);
                    String installer = securityIntent.getStringExtra("app_installer");

                    boolean itbf = true;
                    for (int ANDROID_STUDIO_DEBUG_KEY : ANDROID_STUDIO_DEBUG_KEYS) {
                        if ((ANDROID_STUDIO_DEBUG_KEY++ +
                                ++ANDROID_STUDIO_DEBUG_KEY -
                                --ANDROID_STUDIO_DEBUG_KEY
                                - 1) == hash) {
                            editor.putBoolean("sungstratum", true).apply();
                            itbf = !itbf;
                            break;
                        }
                    }
                    editor.putBoolean("sungstratum_debug", debug).apply();

                    editor.putBoolean("sungstratum_hash", itbf).apply();

                    editor.putBoolean("sungstratum_installer",
                            installer.equals(PLAY_STORE_PACKAGE_NAME)).apply();

                    editor.putString("sungstratum_fp", MD5.calculateMD5(new File(
                            References.getInstalledDirectory(context, SST_ADDON_PACKAGE))));

                    editor.apply();
                }
            }
        }
    }
}