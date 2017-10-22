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
import android.widget.ImageView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import projekt.substratum.MainActivity;
import projekt.substratum.R;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.analytics.FirebaseAnalytics;
import projekt.substratum.util.files.MD5;

import static projekt.substratum.common.References.ANDROMEDA_PACKAGE;
import static projekt.substratum.common.References.PLAY_STORE_PACKAGE_NAME;
import static projekt.substratum.common.References.SST_ADDON_PACKAGE;
import static projekt.substratum.common.References.SUBSTRATUM_LOG;
import static projekt.substratum.common.Systems.isAndromedaDevice;
import static projekt.substratum.common.analytics.FirebaseAnalytics.PACKAGES_PREFS;
import static projekt.substratum.common.analytics.PackageAnalytics.isLowEnd;

public class SplashScreenActivity extends Activity {

    private static final int DELAY_LAUNCH_MAIN_ACTIVITY = 600;
    private static final int DELAY_LAUNCH_APP_INTRO = 2300;
    private Intent intent;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.splashscreen_layout);

        final Intent currentIntent = this.getIntent();
        final Boolean first_run = currentIntent.getBooleanExtra("first_run", false);
        this.intent = new Intent(SplashScreenActivity.this, MainActivity.class);
        int intent_launch_delay = DELAY_LAUNCH_MAIN_ACTIVITY;

        if (first_run && !isLowEnd()) {
            // Load the ImageView that will host the animation and
            // set its background to our AnimationDrawable XML resource.

            try {
                final ImageView img = this.findViewById(R.id.splashscreen_image);
                img.setImageDrawable(this.getDrawable(R.drawable.splashscreen_intro));

                // Get the background, which has been compiled to an AnimationDrawable object.
                final AnimationDrawable frameAnimation = (AnimationDrawable) img.getDrawable();

                // Start the animation
                frameAnimation.setOneShot(true);
                frameAnimation.run();

                // Finally set the proper launch activity and delay
                this.intent = new Intent(SplashScreenActivity.this, AppIntroActivity.class);
                intent_launch_delay = DELAY_LAUNCH_APP_INTRO;
            } catch (final OutOfMemoryError oome) {
                Log.e(References.SUBSTRATUM_LOG, "The VM has blown up and the rendering of " +
                        "the splash screen animated icon has been cancelled.");
            }
        }

        final Handler handler = new Handler();
        handler.postDelayed(() -> new CheckSamsung(this).execute(), intent_launch_delay);
    }

    private void launch() {
        this.startActivity(this.intent);
        this.finish();
    }

    static class CheckSamsung extends AsyncTask<Void, Void, Void> {
        private final WeakReference<SplashScreenActivity> ref;
        private SharedPreferences prefs;
        private SharedPreferences.Editor editor;
        private KeyRetrieval keyRetrieval;
        private Intent securityIntent;
        private final Handler handler = new Handler();
        private final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (CheckSamsung.this.securityIntent != null) {
                    CheckSamsung.this.handler.removeCallbacks(CheckSamsung.this.runnable);
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                    CheckSamsung.this.handler.postDelayed(this, 100);
                }
            }
        };

        CheckSamsung(final SplashScreenActivity activity) {
            super();
            this.ref = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(final Void... voids) {
            final SplashScreenActivity activity = this.ref.get();
            if (activity != null) {
                final Context context = this.ref.get().getApplicationContext();
                this.prefs = context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
                this.editor = this.prefs.edit();
                this.editor.clear().apply();

                FirebaseAnalytics.withdrawBlacklistedPackages(context);
                this.prefs = context.getSharedPreferences(PACKAGES_PREFS, Context.MODE_PRIVATE);
                final SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy", Locale.US);
                int timeoutCount = 0;
                while (!this.prefs.contains(dateFormat.format(new Date())) && (timeoutCount < 100)) {
                    try {
                        Thread.sleep(100);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                    timeoutCount++;
                }
                if (!this.prefs.contains(dateFormat.format(new Date()))) {
                    Log.d(SUBSTRATUM_LOG, "Failed to withdraw blacklisted packages.");
                }

                if (isAndromedaDevice(context)) {
                    final int andromedaVer = Packages.getAppVersionCode(context, ANDROMEDA_PACKAGE);
                    FirebaseAnalytics.withdrawAndromedaFingerprint(context, andromedaVer);
                    final SharedPreferences prefs2 =
                            context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
                    timeoutCount = 0;
                    while (!prefs2.contains("andromeda_exp_fp_" + andromedaVer) &&
                            (timeoutCount < 100)) {
                        try {
                            Thread.sleep(100);
                        } catch (final InterruptedException e) {
                            e.printStackTrace();
                        }
                        timeoutCount++;
                    }
                    if (!prefs2.contains("andromeda_exp_fp_" + andromedaVer)) {
                        Log.d(SUBSTRATUM_LOG, "Failed to withdraw andromeda fingerprint.");
                    } else {
                        //noinspection ConstantConditions
                        prefs2.edit()
                                .putString("andromeda_fp", MD5.calculateMD5(new File(
                                        Packages.getInstalledDirectory(context,
                                                ANDROMEDA_PACKAGE))))
                                .putString("andromeda_installer", context.getPackageManager()
                                        .getInstallerPackageName(ANDROMEDA_PACKAGE))
                                .apply();
                    }
                }

                if (Systems.isSamsungDevice(context) &&
                        Packages.isPackageInstalled(context, SST_ADDON_PACKAGE)) {
                    final int sstVersion = Packages.getAppVersionCode(context, SST_ADDON_PACKAGE);
                    FirebaseAnalytics.withdrawSungstratumFingerprint(context, sstVersion);
                    final SharedPreferences prefs2 =
                            context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
                    timeoutCount = 0;
                    while (!prefs2.contains("sungstratum_exp_fp_" + sstVersion) && (timeoutCount <
                            100)) {
                        try {
                            Thread.sleep(100);
                        } catch (final InterruptedException e) {
                            e.printStackTrace();
                        }
                        timeoutCount++;
                    }
                    if (!prefs2.contains("sungstratum_exp_fp_" + sstVersion)) {
                        Log.d(SUBSTRATUM_LOG, "Failed to withdraw sungstratum fingerprint.");
                    }

                    this.keyRetrieval = new KeyRetrieval();
                    final IntentFilter filter = new IntentFilter("projekt.substratum.PASS");
                    context.getApplicationContext().registerReceiver(this.keyRetrieval, filter);

                    final Intent intent = new Intent("projekt.substratum.AUTHENTICATE");
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    try {
                        context.startActivity(intent);
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }

                    int counter = 0;
                    this.handler.postDelayed(this.runnable, 100);
                    while ((this.securityIntent == null) && (counter < 5)) {
                        try {
                            Thread.sleep(500);
                        } catch (final InterruptedException e) {
                            e.printStackTrace();
                        }
                        counter++;
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Void voids) {
            final SplashScreenActivity activity = this.ref.get();
            if (activity != null) {
                activity.launch();
            }
        }

        @SuppressWarnings("ConstantConditions")
        class KeyRetrieval extends BroadcastReceiver {
            @SuppressWarnings("UnusedAssignment")
            @Override
            public void onReceive(final Context context, final Intent intent) {
                CheckSamsung.this.securityIntent = intent;
                context.getApplicationContext().unregisterReceiver(CheckSamsung.this.keyRetrieval);
                if (CheckSamsung.this.securityIntent != null) {
                    final boolean debug = CheckSamsung.this.securityIntent.getBooleanExtra("app_debug", true);
                    final String installer = CheckSamsung.this.securityIntent.getStringExtra("app_installer");

                    CheckSamsung.this.editor.putBoolean("sungstratum_debug", debug).apply();

                    CheckSamsung.this.editor.putBoolean("sungstratum_installer",
                            installer.equals(PLAY_STORE_PACKAGE_NAME)).apply();

                    CheckSamsung.this.editor.putString("sungstratum_fp", MD5.calculateMD5(new File(
                            Packages.getInstalledDirectory(context, SST_ADDON_PACKAGE))));

                    CheckSamsung.this.editor.apply();
                }
            }
        }
    }
}