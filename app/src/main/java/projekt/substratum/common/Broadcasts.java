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

package projekt.substratum.common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import projekt.substratum.services.crash.AppCrashReceiver;
import projekt.substratum.services.packages.OverlayFound;
import projekt.substratum.services.packages.OverlayUpdater;
import projekt.substratum.services.packages.PackageModificationDetector;
import projekt.substratum.services.profiles.ScheduledProfileReceiver;
import projekt.substratum.services.system.InterfacerAuthorizationReceiver;

import static projekt.substratum.common.Internal.ENCRYPTION_KEY_EXTRA;
import static projekt.substratum.common.Internal.IV_ENCRYPTION_KEY_EXTRA;
import static projekt.substratum.common.Internal.MAIN_ACTIVITY_RECEIVER;
import static projekt.substratum.common.Internal.OVERLAY_REFRESH;
import static projekt.substratum.common.Internal.THEME_FRAGMENT_REFRESH;
import static projekt.substratum.common.References.ACTIVITY_FINISHER;
import static projekt.substratum.common.References.APP_CRASHED;
import static projekt.substratum.common.References.INTERFACER_PACKAGE;
import static projekt.substratum.common.References.KEY_RETRIEVAL;
import static projekt.substratum.common.References.MANAGER_REFRESH;
import static projekt.substratum.common.References.PACKAGE_ADDED;
import static projekt.substratum.common.References.PACKAGE_FULLY_REMOVED;
import static projekt.substratum.common.References.SUBSTRATUM_LOG;
import static projekt.substratum.common.References.TEMPLATE_RECEIVE_KEYS;
import static projekt.substratum.common.References.scheduledProfileReceiver;

public enum Broadcasts {
    ;

    /**
     * Send a localized key message for encryption to take place
     *
     * @param context       Context
     * @param encryptionKey Encryption key
     * @param ivEncryptKey  IV encryption key
     */
    static void sendLocalizedKeyMessage(Context context,
                                        byte[] encryptionKey,
                                        byte[] ivEncryptKey) {
        Log.d("KeyRetrieval",
                "The system has completed the handshake for keys retrieval " +
                        "and is now passing it to the activity...");
        Intent intent = new Intent(KEY_RETRIEVAL);
        intent.putExtra(ENCRYPTION_KEY_EXTRA, encryptionKey);
        intent.putExtra(IV_ENCRYPTION_KEY_EXTRA, ivEncryptKey);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Close Substratum as a whole
     *
     * @param context Context
     */
    public static void sendKillMessage(Context context) {
        Log.d("SubstratumKiller",
                "A crucial action has been conducted by the user and " +
                        "Substratum is now shutting down!");
        Intent intent = new Intent(MAIN_ACTIVITY_RECEIVER);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * A package was installed, refresh the ThemeFragment
     *
     * @param context Context
     */
    public static void sendRefreshMessage(Context context) {
        Log.d("ThemeFragmentRefresher",
                "A theme has been modified, sending update signal to refresh the list!");
        Intent intent = new Intent(THEME_FRAGMENT_REFRESH);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * A package was installed, refresh the Overlays tab
     *
     * @param context Context
     */
    public static void sendOverlayRefreshMessage(Context context) {
        Log.d("OverlayRefresher",
                "A theme has been modified, sending update signal to refresh the list!");
        Intent intent = new Intent(OVERLAY_REFRESH);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Activity finisher when a theme was updated
     *
     * @param context     Context
     * @param packageName Package of theme to close
     */
    public static void sendActivityFinisherMessage(Context context,
                                                   String packageName) {
        Log.d("ThemeInstaller",
                "A theme has been installed, sending update signal to app for further processing!");
        Intent intent = new Intent(ACTIVITY_FINISHER);
        intent.putExtra(Internal.THEME_PID, packageName);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * A package was installed, refresh the ManagerFragment
     *
     * @param context Context
     */
    public static void sendRefreshManagerMessage(Context context) {
        Intent intent = new Intent(MANAGER_REFRESH);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Register the implicit intent broadcast receivers
     *
     * @param context Context
     */
    public static void registerBroadcastReceivers(Context context) {
        try {
            IntentFilter intentPackageAdded = new IntentFilter(PACKAGE_ADDED);
            intentPackageAdded.addDataScheme("package");
            IntentFilter intentPackageFullyRemoved = new IntentFilter(PACKAGE_FULLY_REMOVED);
            intentPackageFullyRemoved.addDataScheme("package");

            if (Systems.checkOMS(context)) {
                IntentFilter intentAppCrashed = new IntentFilter(APP_CRASHED);
                context.getApplicationContext().registerReceiver(
                        new AppCrashReceiver(), intentAppCrashed);
                context.getApplicationContext().registerReceiver(
                        new OverlayUpdater(), intentPackageAdded);
            }

            if (Systems.checkThemeInterfacer(context)) {
                IntentFilter interfacerAuthorize = new IntentFilter(
                        INTERFACER_PACKAGE + ".CALLER_AUTHORIZED");
                context.getApplicationContext().registerReceiver(
                        new InterfacerAuthorizationReceiver(), interfacerAuthorize);
            }

            context.getApplicationContext().registerReceiver(
                    new OverlayFound(), intentPackageAdded);
            context.getApplicationContext().registerReceiver(
                    new PackageModificationDetector(), intentPackageAdded);
            context.getApplicationContext().registerReceiver(
                    new PackageModificationDetector(), intentPackageFullyRemoved);

            Log.d(SUBSTRATUM_LOG,
                    "Successfully registered broadcast receivers for Substratum functionality!");
        } catch (Exception e) {
            Log.e(SUBSTRATUM_LOG,
                    "Failed to register broadcast receivers for Substratum functionality...");
        }
    }

    /**
     * Register the profile screen off receiver
     *
     * @param context Context
     */
    public static void registerProfileScreenOffReceiver(Context context) {
        scheduledProfileReceiver = new ScheduledProfileReceiver();
        context.registerReceiver(scheduledProfileReceiver,
                new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    /**
     * Unload the profile screen off receiver
     *
     * @param context Context
     */
    public static void unregisterProfileScreenOffReceiver(Context context) {
        try {
            context.unregisterReceiver(scheduledProfileReceiver);
        } catch (Exception ignored) {
        }
    }

    /**
     * Start the key retrieval receiver to obtain the key from the theme
     *
     * @param context Context
     */
    public static void startKeyRetrievalReceiver(Context context) {
        try {
            IntentFilter intentGetKeys = new IntentFilter(TEMPLATE_RECEIVE_KEYS);
            context.getApplicationContext().registerReceiver(
                    new KeyRetriever(), intentGetKeys);

            Log.d(SUBSTRATUM_LOG, "Successfully registered key retrieval receiver!");
        } catch (Exception e) {
            Log.e(SUBSTRATUM_LOG, "Failed to register key retrieval receiver...");
        }
    }

    /**
     * Key Retriever Receiver
     */
    public static class KeyRetriever extends BroadcastReceiver {

        @Override
        public void onReceive(Context context,
                              Intent intent) {
            sendLocalizedKeyMessage(
                    context,
                    intent.getByteArrayExtra(ENCRYPTION_KEY_EXTRA),
                    intent.getByteArrayExtra(IV_ENCRYPTION_KEY_EXTRA));
        }
    }
}