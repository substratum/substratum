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
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.crash.FirebaseCrash;

import java.util.Locale;

import cat.ereza.customactivityoncrash.config.CaocConfig;
import projekt.substratum.activities.crash.SubstratumCrash;
import projekt.substratum.common.Broadcasts;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.services.binder.AndromedaBinderService;
import projekt.substratum.services.binder.InterfacerBinderService;

import static projekt.substratum.BuildConfig.DEBUG;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_O_ANDROMEDA;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_O_ROOTED;
import static projekt.substratum.common.References.RUNTIME_RESOURCE_OVERLAY_N_ROOTED;
import static projekt.substratum.common.Systems.isAndromedaDevice;
import static projekt.substratum.common.Systems.isBinderInterfacer;

public class Substratum extends Application {

    private static final String BINDER_TAG = "BinderService";
    private static final FinishReceiver finishReceiver = new FinishReceiver();
    private static Substratum substratum;
    private static boolean isWaiting;

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
     * Set the locale of the whole app
     *
     * @param forceEnglish Force english?
     */
    public static void setLocale(boolean forceEnglish) {
        Resources resources = Substratum.getInstance().getResources();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        android.content.res.Configuration conf = resources.getConfiguration();
        if (forceEnglish) {
            conf.setLocale(Locale.US);
        } else {
            conf.setLocale(Locale.getDefault());
        }
        resources.updateConfiguration(conf, displayMetrics);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        substratum = this;

        // Set global app theme if on special UI mode
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getInstance());
        boolean bottomBarUi = !prefs.getBoolean("advanced_ui", false);
        if (bottomBarUi) {
            setTheme(R.style.AppTheme_SpecialUI);
        } else {
            setTheme(R.style.AppTheme);
        }

        // Firebase
        try {
            FirebaseApp.initializeApp(this.getApplicationContext());
            FirebaseCrash.setCrashCollectionEnabled(!DEBUG);
        } catch (IllegalStateException ise) {
            // Suppress warning
        }

        // Dynamically check which theme engine is running at the moment
        if (isAndromedaDevice(this.getApplicationContext())) {
            Log.d(BINDER_TAG, "Successful to start the Andromeda binder service: " +
                    (this.startBinderService(AndromedaBinderService.class) ? "Success!" :
                            "Failed"));
        } else if (isBinderInterfacer(this.getApplicationContext())) {
            Log.d(BINDER_TAG, "Successful to start the Interfacer binder service: " +
                    (this.startBinderService(InterfacerBinderService.class) ? "Success!" :
                            "Failed"));
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
    @RequiresApi(api = Build.VERSION_CODES.O)
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
        } catch (Exception e) {
            // Suppress warnings
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
        } catch (IllegalArgumentException e) {
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