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

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.crash.FirebaseCrash;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cat.ereza.customactivityoncrash.config.CaocConfig;
import projekt.substratum.activities.crash.SubstratumCrash;
import projekt.substratum.common.Broadcasts;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.services.binder.AndromedaBinderService;
import projekt.substratum.services.binder.InterfacerBinderService;

import static android.support.v7.app.AppCompatDelegate.MODE_NIGHT_AUTO;
import static android.support.v7.app.AppCompatDelegate.MODE_NIGHT_NO;
import static android.support.v7.app.AppCompatDelegate.MODE_NIGHT_YES;
import static projekt.substratum.BuildConfig.DEBUG;
import static projekt.substratum.common.References.APP_THEME;
import static projekt.substratum.common.References.AUTO_THEME;
import static projekt.substratum.common.References.DARK_THEME;
import static projekt.substratum.common.References.DEFAULT_THEME;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_O_ANDROMEDA;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_O_ROOTED;
import static projekt.substratum.common.References.RUNTIME_RESOURCE_OVERLAY_N_ROOTED;
import static projekt.substratum.common.Systems.checkOreo;
import static projekt.substratum.common.Systems.isAndromedaDevice;
import static projekt.substratum.common.Systems.isBinderInterfacer;

public class Substratum extends Application {

    private static final String BINDER_TAG = "BinderService";
    private static final FinishReceiver finishReceiver = new FinishReceiver();
    private static int initialPackageCount = 0;
    private static int initialOverlayCount = 0;
    public static Thread currentThread;
    private static Substratum substratum;
    private static boolean isWaiting;
    private static boolean shouldStopThread = false;

    /**
     * Get the current instance of the substratum application
     *
     * @return Returns the instance
     */
    public static Substratum getInstance() {
        return substratum;
    }

    /**
     * Alerts the application that we are waiting for overlays to be installed
     */
    public static void startWaitingInstall() {
        isWaiting = true;
    }

    /**
     * Asks the application whether we are waiting for overlays to be installed
     *
     * @return True, if the application is waiting
     */
    public static boolean isWaitingInstall() {
        return isWaiting;
    }

    /**
     * Asks whether the current system needs to wait for install
     *
     * @return True, if the application needs to wait
     */
    public static boolean needToWaitInstall() {
        int system = Systems.checkThemeSystemModule(getInstance());
        // system on root, old interfacer and andromeda need this
        return (system == OVERLAY_MANAGER_SERVICE_O_ANDROMEDA) ||
                (system == OVERLAY_MANAGER_SERVICE_O_ROOTED) ||
                (system == RUNTIME_RESOURCE_OVERLAY_N_ROOTED);
    }

    /**
     * Stop the ongoing package detection on Samsung
     */
    public static void stopSamsungPackageMonitor() {
        Log.d("Substratum",
                "The overlay package refresher for Samsung devices is now stopping!");
        shouldStopThread = true;
    }

    /**
     * Start the ongoing package detection on Samsung
     *
     * @param context Context!
     */
    public static void startSamsungPackageMonitor(Context context) {
        Log.d("Substratum",
                "The overlay package refresher for Samsung devices has been fully loaded.");
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> currentApps =
                pm.getInstalledApplications(PackageManager.GET_META_DATA);
        initialPackageCount = currentApps.size();
        if (checkOreo()) initialOverlayCount = ThemeManager.listAllOverlays(context).size();
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (shouldStopThread || Substratum.getInstance() == null) {
                    cancel();
                    try {
                        currentThread.interrupt();
                    } catch (Exception ignored) {
                    }
                    currentThread = null;
                }
                List<ApplicationInfo> currentApps =
                        pm.getInstalledApplications(PackageManager.GET_META_DATA);
                List<String> listOfThemes = new ArrayList<>();
                if (checkOreo()) listOfThemes = ThemeManager.listAllOverlays(context);
                if (initialPackageCount != currentApps.size() ||
                        (checkOreo() &&
                                initialOverlayCount >= 1 &&
                                initialOverlayCount != listOfThemes.size())) {
                    if (checkOreo())
                        initialOverlayCount = ThemeManager.listAllOverlays(context).size();
                    initialPackageCount = currentApps.size();
                    Broadcasts.sendOverlayRefreshMessage(context);
                }
            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, 1000);
        currentThread = new Thread(timerTask);
        currentThread.start();
    }

    /**
     * Restart the application after a change that requires a full exit.
     *
     * @param context Duh
     */
    public static void restartSubstratum(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent startActivity = pm.getLaunchIntentForPackage(context.getPackageName());

        if (startActivity != null) startActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Create a pending intent so the application is restarted after System.exit(0) was called.
        // We use an AlarmManager to call this intent in 10ms
        PendingIntent mPendingIntent =
                PendingIntent.getActivity(context,
                        0, startActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (mgr != null) mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 10, mPendingIntent);

        // Kill the application
        System.exit(0);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        substratum = this;

        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        String selectedTheme = prefs.getString(APP_THEME, DEFAULT_THEME);
        if (!getApplicationContext().getResources().getBoolean(R.bool.forceAppDarkTheme)) {
            switch (selectedTheme) {
                case AUTO_THEME:
                    AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_AUTO);
                    break;
                case DARK_THEME:
                    AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES);
                    break;
                case DEFAULT_THEME:
                    AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO);
                    break;
            }
        } else {
            AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES);
            prefs.edit().putString("app_theme", DARK_THEME).apply();
        }

        // Firebase
        try {
            FirebaseApp.initializeApp(this.getApplicationContext());
            FirebaseCrash.setCrashCollectionEnabled(!DEBUG);
        } catch (IllegalStateException ignored) {
        }

        // Dynamically check which theme engine is running at the moment
        if (isAndromedaDevice(this.getApplicationContext())) {
            boolean startBinderService = this.startBinderService(AndromedaBinderService.class);
            Log.d(BINDER_TAG, "Successful to start the Andromeda binder service: " +
                    (startBinderService ? "Success!" : "Failed"));
            if (!startBinderService) {
                this.stopService(
                        new Intent(this.getApplicationContext(), AndromedaBinderService.class));
            }
        } else if (isBinderInterfacer(this.getApplicationContext())) {
            boolean startBinderService = this.startBinderService(InterfacerBinderService.class);
            Log.d(BINDER_TAG, "Successful to start the Interfacer binder service: " +
                    (startBinderService ? "Success!" : "Failed"));
            if (!startBinderService) {
                this.stopService(
                        new Intent(this.getApplicationContext(), InterfacerBinderService.class));
            }
        }

        // Implicit broadcasts must be declared
        Broadcasts.registerBroadcastReceivers(this);

        // If the device is Android Oreo, create a persistent notification
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            this.createNotificationChannel();
        }

        // Custom Activity on Crash initialization
        CaocConfig.Builder.create()
                .backgroundMode(CaocConfig.BACKGROUND_MODE_SHOW_CUSTOM)
                .enabled(true)
                .showErrorDetails(true)
                .showRestartButton(true)
                .trackActivities(true)
                .minTimeBetweenCrashesMs(1)
                .errorActivity(SubstratumCrash.class)
                .apply();
    }

    /**
     * For Android Oreo and above, we need to ensure our notification channels are properly
     * configured so that we do not get killed off with the new background service limiter.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel mainChannel = new NotificationChannel(
                References.DEFAULT_NOTIFICATION_CHANNEL_ID,
                this.getString(R.string.notification_channel_default),
                NotificationManager.IMPORTANCE_DEFAULT);
        mainChannel.setDescription(
                this.getString(R.string.notification_channel_default_description));
        mainChannel.setSound(null, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build());
        assert notificationManager != null;
        notificationManager.createNotificationChannel(mainChannel);

        NotificationChannel compileChannel = new NotificationChannel(
                References.ONGOING_NOTIFICATION_CHANNEL_ID,
                this.getString(R.string.notification_channel_ongoing),
                NotificationManager.IMPORTANCE_LOW);
        mainChannel.setDescription(
                this.getString(R.string.notification_channel_ongoing_description));
        notificationManager.createNotificationChannel(compileChannel);

        NotificationChannel andromedaChannel = new NotificationChannel(
                References.ANDROMEDA_NOTIFICATION_CHANNEL_ID,
                this.getString(R.string.notification_channel_andromeda),
                NotificationManager.IMPORTANCE_NONE);
        andromedaChannel.setDescription(
                this.getString(R.string.notification_channel_andromeda_description));
        notificationManager.createNotificationChannel(andromedaChannel);
    }

    /**
     * A consolidated function that launches a specific binder class
     *
     * @param className Binder service to be started
     * @return Returns true if the service has been started
     */
    public boolean startBinderService(Class className) {
        try {
            if (className.equals(AndromedaBinderService.class)) {
                if (this.checkServiceActivation(AndromedaBinderService.class)) {
                    Log.d(BINDER_TAG,
                            "This session will utilize the connected Andromeda Binder service!");
                } else {
                    Log.d(BINDER_TAG,
                            "Substratum is now connecting to the Andromeda Binder service...");
                    this.startService(new Intent(this.getApplicationContext(),
                            AndromedaBinderService.class));
                }
            } else if (className.equals(InterfacerBinderService.class)) {
                if (this.checkServiceActivation(InterfacerBinderService.class)) {
                    Log.d(BINDER_TAG, "This session will utilize the connected Binder service!");
                } else {
                    Log.d(BINDER_TAG, "Substratum is now connecting to the Binder service...");
                    Intent i = new Intent(this.getApplicationContext(),
                            InterfacerBinderService.class);
                    this.startService(i);
                }
            }
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Check whether the {@link #startBinderService(Class)} has been loaded before
     *
     * @param serviceClass Specific class to check whether activated
     * @return True, the specified class is active
     */
    private boolean checkServiceActivation(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) this.getSystemService(Context
                .ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Registers the finish receiver for PACKAGE_ADDED
     */
    public void registerFinishReceiver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addDataScheme("package");
        this.registerReceiver(finishReceiver, filter);
    }

    /**
     * Unregisters the finish receiver for PACKAGE_ADDED
     */
    public void unregisterFinishReceiver() {
        try {
            this.unregisterReceiver(finishReceiver);
        } catch (IllegalArgumentException ignored) {
            // Already unregistered
        }
    }

    /**
     * A persistent receiver that detects whether an application is installed by Substratum
     */
    private static class FinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getData() != null) {
                String packageName = intent.getData().getEncodedSchemeSpecificPart();
                // Check whether the installed package is made by substratum
                String check = Packages.getOverlayParent(context, packageName);
                if (check != null) {
                    isWaiting = false;
                    Log.d("Substratum", "PACKAGE_ADDED: " + packageName);
                }
            }
        }
    }
}