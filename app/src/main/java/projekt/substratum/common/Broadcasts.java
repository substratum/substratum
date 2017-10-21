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

public class Broadcasts {
    static void sendLocalizedKeyMessage(Context context,
                                        byte[] encryption_key,
                                        byte[] iv_encrypt_key) {
        Log.d("KeyRetrieval",
                "The system has completed the handshake for keys retrieval " +
                        "and is now passing it to the activity...");
        Intent intent = new Intent(KEY_RETRIEVAL);
        intent.putExtra("encryption_key", encryption_key);
        intent.putExtra("iv_encrypt_key", iv_encrypt_key);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void sendKillMessage(Context context) {
        Log.d("SubstratumKiller",
                "A crucial action has been conducted by the user and Substratum is now shutting " +
                        "down!");
        Intent intent = new Intent("MainActivity.KILL");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void sendAndromedaRefreshMessage(Context context) {
        Log.d("AndromedaReceiver",
                "Andromeda has been killed, notifying the MainActivity now!");
        Intent intent = new Intent("AndromedaReceiver.KILL");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void sendRefreshMessage(Context context) {
        Log.d("ThemeFragmentRefresher",
                "A theme has been modified, sending update signal to refresh the list!");
        Intent intent = new Intent("ThemeFragment.REFRESH");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void sendOverlayRefreshMessage(Context context) {
        Log.d("OverlayRefresher",
                "A theme has been modified, sending update signal to refresh the list!");
        Intent intent = new Intent("Overlay.REFRESH");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void sendActivityFinisherMessage(Context context, String package_name) {
        Log.d("ThemeInstaller",
                "A theme has been installed, sending update signal to app for further processing!");
        Intent intent = new Intent(ACTIVITY_FINISHER);
        intent.putExtra("theme_pid", package_name);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void sendRefreshManagerMessage(Context context) {
        Intent intent = new Intent(MANAGER_REFRESH);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

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
                        new OverlayFound(), intentPackageAdded);
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

    public static void registerProfileScreenOffReceiver(Context context) {
        scheduledProfileReceiver = new ScheduledProfileReceiver();
        context.registerReceiver(scheduledProfileReceiver,
                new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    public static void unregisterProfileScreenOffReceiver(Context context) {
        try {
            context.unregisterReceiver(scheduledProfileReceiver);
        } catch (Exception e) {
            // Suppress warning
        }
    }

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

    public static class KeyRetriever extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            sendLocalizedKeyMessage(
                    context,
                    intent.getByteArrayExtra("encryption_key"),
                    intent.getByteArrayExtra("iv_encrypt_key"));
        }
    }
}
