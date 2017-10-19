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
import android.media.AudioAttributes;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.crash.FirebaseCrash;

import cat.ereza.customactivityoncrash.config.CaocConfig;
import projekt.substratum.activities.crash.SubstratumCrash;
import projekt.substratum.common.Broadcasts;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.services.binder.AndromedaBinderService;
import projekt.substratum.services.binder.InterfacerBinderService;
import projekt.substratum.services.system.SamsungPackageService;

import static projekt.substratum.BuildConfig.DEBUG;
import static projekt.substratum.common.Systems.isAndromedaDevice;
import static projekt.substratum.common.Systems.isBinderInterfacer;

public class Substratum extends Application {

    private static final String BINDER_TAG = "BinderService";
    private static final FinishReceiver finishReceiver = new FinishReceiver();
    private static Substratum substratum;
    private static boolean isWaiting;

    public static Substratum getInstance() {
        return substratum;
    }

    public static void startWaitingInstall() {
        isWaiting = true;
    }

    public static boolean isWaitingInstall() {
        return isWaiting;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        substratum = this;

        // Firebase debug checks
        try {
            FirebaseApp.initializeApp(this.getApplicationContext());
            FirebaseCrash.setCrashCollectionEnabled(!DEBUG);
        } catch (final IllegalStateException ise) {
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

        // Samsung refresher
        if (Systems.isSamsungDevice(getApplicationContext())) {
            startService(new Intent(getBaseContext(), SamsungPackageService.class));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        final NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        final NotificationChannel mainChannel = new NotificationChannel(
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

        final NotificationChannel compileChannel = new NotificationChannel(
                References.ONGOING_NOTIFICATION_CHANNEL_ID,
                this.getString(R.string.notification_channel_ongoing),
                NotificationManager.IMPORTANCE_LOW);
        mainChannel.setDescription(
                this.getString(R.string.notification_channel_ongoing_description));
        notificationManager.createNotificationChannel(compileChannel);

        final NotificationChannel andromedaChannel = new NotificationChannel(
                References.ANDROMEDA_NOTIFICATION_CHANNEL_ID,
                this.getString(R.string.notification_channel_andromeda),
                NotificationManager.IMPORTANCE_NONE);
        andromedaChannel.setDescription(
                this.getString(R.string.notification_channel_andromeda_description));
        notificationManager.createNotificationChannel(andromedaChannel);
    }

    public boolean startBinderService(final Class className) {
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
                    final Intent i = new Intent(this.getApplicationContext(),
                            InterfacerBinderService.class);
                    this.startService(i);
                }
            }
            return true;
        } catch (final Exception e) {
            // Suppress warnings
        }
        return false;
    }

    private boolean checkServiceActivation(final Class<?> serviceClass) {
        final ActivityManager manager = (ActivityManager) this.getSystemService(Context
                .ACTIVITY_SERVICE);
        assert manager != null;
        for (final ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void registerFinishReceiver() {
        final IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addDataScheme("package");
        this.registerReceiver(finishReceiver, filter);
    }

    public void unregisterFinishReceiver() {
        try {
            this.unregisterReceiver(finishReceiver);
        } catch (final IllegalArgumentException e) {
            // Already unregistered
        }
    }

    private static class FinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent.getData() != null) {
                final String packageName = intent.getData().getEncodedSchemeSpecificPart();
                // Check whether the installed package is made by substratum
                final String check = Packages.getOverlayParent(context, packageName);
                if (check != null) {
                    isWaiting = false;
                    Log.d("Substratum", "PACKAGE_ADDED: " + packageName);
                }
            }
        }
    }
}